package repositories

import models.{Reservation, Room}
import models.db.{RoomTable, ReservationTable}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}


class RoomRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  val rooms = TableQuery[RoomTable]
  val reservations = TableQuery[ReservationTable]

  // Find available rooms based on the specified time range
  def findAvailableRooms(startTime: String, endTime: String): Future[List[Room]] = {
    val availableRoomsQuery = rooms.filterNot { room =>
      reservations
        .filter(reservation => reservation.roomId === room.id)
        .filter(reservation => reservation.startTime < endTime && reservation.endTime > startTime)
        .exists
    }

    db.run(availableRoomsQuery.result).map(_.toList)
  }


  def findById(roomId: Int): Future[Option[Room]] = {
    db.run(rooms.filter(_.id === roomId).result.headOption)
  }

  // Add a new room to the database
  def addRoom(room: Room): Future[Int] = {
    val insertQuery = rooms.returning(rooms.map(_.id)) += room
    db.run(insertQuery).map(_.head) // Return the ID of the newly added room
  }

  // Fetch all rooms from the database
  def fetchAllRooms(): Future[List[Room]] = {
    db.run(rooms.result).map(_.toList)
  }

  // Update a room's details
  def updateRoom(room: Room): Future[Int] = {
    val updateQuery = rooms
      .filter(_.id === room.id)
      .update(room)
    db.run(updateQuery)
  }

  def checkRoomAvailability(roomId: Int, startTime: String, endTime: String): Future[Boolean] = {
    val conflictingReservations = reservations
      .filter(reservation =>
        reservation.roomId === roomId &&
          reservation.startTime < endTime &&
          reservation.endTime > startTime
      )
    db.run(conflictingReservations.exists.result).map(!_)
  }
}
