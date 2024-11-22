package models.db
import models.User
import slick.jdbc.MySQLProfile.api._

class UserTable(tag: Tag) extends Table[User](tag, "User") {
  def employeeId = column[Option[Long]]("employee_id", O.PrimaryKey, O.AutoInc)
  def employeeName = column[String]("employeeName")
  def role = column[String]("role")
  def email = column[String]("email")
  def * = (employeeId, employeeName, role, email) <> ((User.apply _).tupled, User.unapply)
}
