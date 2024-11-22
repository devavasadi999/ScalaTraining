package models.db

import models.Room
import slick.jdbc.MySQLProfile.api._

class RoomTable(tag: Tag) extends Table[Room](tag, "Room") {
  def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
  def roomName = column[String]("room_name")
  def capacity = column[Int]("capacity")

  def * = (id, roomName, capacity) <> ((Room.apply _).tupled, Room.unapply)
}
