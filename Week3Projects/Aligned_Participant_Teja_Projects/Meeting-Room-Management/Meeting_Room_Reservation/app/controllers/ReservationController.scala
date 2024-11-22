package controllers

import models.{Reservation, Room}
import play.api.libs.json._
import play.api.mvc._
import services.{ReservationService, RoomService, UserService}
import utils.KafkaProducerUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReservationController @Inject()(
                                       cc: ControllerComponents,
                                       reservationService: ReservationService,
                                       roomService: RoomService,
                                       userService: UserService
                                     )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def showReservationForm: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.ReservationForm())
  }

  // Endpoint to reserve a room, restricted to AdminStaff role
  def reserveRoom: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Reservation].fold(
      errors => {
        Future.successful(BadRequest(Json.obj("error" -> "Invalid reservation data", "details" -> JsError.toJson(errors))))
      },
      reservation => {
        // Check if the user has the AdminStaff role
        userService.getUserById(reservation.createdBy).flatMap {
          case Some(user) if user.role == "AdminStaff" =>
            reservationService.reserveRoom(reservation).map {
              case Some(savedReservation) =>
                // Trigger Kafka event after successful reservation creation
                val reservationData = Json.toJson(savedReservation).toString()
                KafkaProducerUtil.sendMessage("meeting-reservation", savedReservation.reservationId.toString, reservationData)
                Created(Json.toJson(savedReservation))
              case None =>
                Conflict(Json.obj("error" -> "Room is unavailable for the selected time"))
            }
          case Some(_) =>
            Future.successful(Forbidden(Json.obj("error" -> "Only Admin Staff can make reservations")))
          case None =>
            Future.successful(NotFound(Json.obj("error" -> "User not found")))
        }
      }
    )
  }
}
