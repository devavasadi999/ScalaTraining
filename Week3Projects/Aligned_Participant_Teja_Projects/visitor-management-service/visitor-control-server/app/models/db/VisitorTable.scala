package models.db

import models.Visitor
import slick.jdbc.MySQLProfile.api._

class VisitorTable(tag: Tag) extends Table[Visitor](tag, "visitors") {
  def visitorId = column[Option[Long]]("visitor_id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def hostName = column[String]("host_name")
  def hostMail = column[String]("host_email")
  def building = column[String]("building")
  def email = column[String]("email")
  def contactNumber = column[String]("contact_number")
  def checkInTime = column[String]("check_in_time")
  def checkOutTime = column[Option[String]]("check_out_time")
  def status = column[String]("status")

  def * = (visitorId, name, hostName, hostMail, building, email, contactNumber, checkInTime, checkOutTime, status) <> ((Visitor.apply _).tupled, Visitor.unapply)
}