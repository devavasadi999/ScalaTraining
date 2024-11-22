package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Employee(employeeId: Option[Long],
                    employeeName: String,
                    organisation: String,
                    building: String,
                    email: String,
                    employeeType: String,
                    contactNo: String)

object Employee {
  // Reads for deserialization
  implicit val employeeReads: Reads[Employee] = (
    (JsPath \ "employeeId").readNullable[Long] and
      (JsPath \ "employeeName").read[String] and
      (JsPath \ "organisation").read[String] and
      (JsPath \ "building").read[String] and
      (JsPath \ "email").read[String] and
      (JsPath \ "employeeType").read[String] and
      (JsPath \ "contactNo").read[String]
    )(Employee.apply _)

  // Writes for serialization
  implicit val employeeWrites: Writes[Employee] = Json.writes[Employee]

  // Combine Reads and Writes into Format
  implicit val employeeFormat: Format[Employee] = Format(employeeReads, employeeWrites)
}
