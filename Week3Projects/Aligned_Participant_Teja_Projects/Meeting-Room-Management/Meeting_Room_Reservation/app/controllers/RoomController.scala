package controllers

import models.Room
import play.api.libs.json._
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.RoomService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RoomController @Inject()(cc: ControllerComponents,
                               roomService: RoomService
                             )(implicit ec: ExecutionContext) extends AbstractController(cc) {
  // Endpoint to check available rooms for a given time range
  def checkAvailability(startTime: String, endTime: String): Action[AnyContent] = Action.async {
    roomService.findAvailableRooms(startTime, endTime).map { availableRooms =>
      Ok(Json.toJson(availableRooms))
    }
  }

  // Endpoint to check availability of a specific room
  def checkRoomAvailability(roomId: Int, startTime: String, endTime: String): Action[AnyContent] = Action.async {
    roomService.getRoomById(roomId).flatMap {
      case Some(_) => // Room exists, proceed to check availability
        roomService.checkRoomAvailability(roomId, startTime, endTime).map { isAvailable =>
          if (isAvailable) {
            Ok(Json.obj("available" -> true))
          } else {
            Conflict(Json.obj("available" -> false, "message" -> "Room is already reserved for this time range."))
          }
        }
      case None => // Room not found
        Future.successful(NotFound(Json.obj("error" -> "Room not found")))
    }
  }

  // Endpoint to add a new room
  def addRoom(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Room].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> "Invalid room data"))),
      room => {
        roomService.addRoom(room).map { roomId =>
          Created(Json.obj("roomId" -> roomId, "message" -> "Room successfully added"))
        }
      }
    )
  }

  // Endpoint to fetch a room by its ID
  def getRoomById(roomId: Int): Action[AnyContent] = Action.async {
    roomService.getRoomById(roomId).map {
      case Some(room) => Ok(Json.toJson(room))
      case None => NotFound(Json.obj("error" -> "Room not found"))
    }
  }

  // Endpoint to update an existing room
  def updateRoom(roomId: Int): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Room].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> "Invalid room data"))),
      room => {
        roomService.getRoomById(roomId).flatMap {
          case Some(existingRoom) =>
            // Proceed to update
            roomService.updateRoom(room.copy(id = Some(roomId))).map { rowsUpdated =>
              if (rowsUpdated > 0) {
                Ok(Json.obj("message" -> "Room updated successfully"))
              } else {
                Conflict(Json.obj("message" -> "No changes made to the room"))
              }
            }
          case None => Future.successful(NotFound(Json.obj("error" -> "Room not found")))
        }
      }
    )
  }

  // Endpoint to fetch all rooms
  def getAllRooms: Action[AnyContent] = Action.async {
    roomService.fetchAllRooms().map { rooms =>
      Ok(Json.toJson(rooms))
    }
  }
}
