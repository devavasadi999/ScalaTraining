package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

case class Employee(id: Long, name: String, email: String, department: String)

object Employee {
  implicit val employeeFormat: OFormat[Employee] = Json.format[Employee]
}

class EmployeeTable(tag: Tag) extends Table[Employee](tag, "employee") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def email = column[String]("email")
  def department = column[String]("department")

  def * = (id, name, email, department) <> ((Employee.apply _).tupled, Employee.unapply)
}
