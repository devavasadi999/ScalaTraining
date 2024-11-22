package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class User(
                 employeeId: Option[Long],
                 employeeName: String,
                 role: String, // Role can be 'AdminStaff', 'RoomService', or 'SystemAdmin'
                 email: String
               )
object User {
  implicit val userReads: Reads[User] = (
    (JsPath \ "employee_id").readNullable[Long] and
      (JsPath \ "employeeName").read[String] and
      (JsPath \ "role").read[String] and
      (JsPath \ "email").read[String]
    )(User.apply _)

  implicit val userWrites: Writes[User] = Json.writes[User]

  implicit val userFormat: Format[User] = Format(userReads, userWrites)
}
