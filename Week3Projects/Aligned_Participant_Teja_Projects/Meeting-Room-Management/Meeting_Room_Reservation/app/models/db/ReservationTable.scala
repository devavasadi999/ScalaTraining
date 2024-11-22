package models.db

import models.Reservation
import slick.jdbc.MySQLProfile.api._

class ReservationTable(tag: Tag) extends Table[Reservation](tag, "Reservation") {
  def reservationId = column[Option[Long]]("reservation_id", O.PrimaryKey, O.AutoInc)
  def roomId = column[Int]("room_id")
  def employeeName = column[String]("employee_name")
  def employeeMail = column[String]("employee_email")
  def department = column[String]("department")
  def purpose = column[String]("purpose")
  def startTime = column[String]("start_time")
  def endTime = column[String]("end_time")
  def createdBy = column[Long]("created_by")

  def * = (reservationId, roomId, employeeName, employeeMail, department, purpose, startTime, endTime, createdBy) <> ((Reservation.apply _).tupled, Reservation.unapply)
}
