package services

import models.Room
import repositories.RoomRepository

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RoomService @Inject()(roomRepository: RoomRepository)(implicit ec: ExecutionContext) {

  def getRoomById(roomId: Int): Future[Option[Room]] = {
    roomRepository.findById(roomId)
  }

  def checkRoomAvailability(roomId: Int, startTime: String, endTime: String): Future[Boolean] = {
    roomRepository.checkRoomAvailability(roomId, startTime, endTime)
  }

  def findAvailableRooms(startTime: String, endTime: String): Future[List[Room]] = {
    roomRepository.findAvailableRooms(startTime, endTime).map(_.toList)
  }

  // Add a new room
  def addRoom(room: Room): Future[Int] = {
    roomRepository.addRoom(room)
  }

  // Fetch all rooms
  def fetchAllRooms(): Future[List[Room]] = {
    roomRepository.fetchAllRooms()
  }

  // Update an existing room
  def updateRoom(room: Room): Future[Int] = {
    roomRepository.updateRoom(room)
  }
}
