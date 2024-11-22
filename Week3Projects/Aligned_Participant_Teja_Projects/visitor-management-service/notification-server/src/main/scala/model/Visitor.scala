package model

import play.api.libs.functional.syntax._
import play.api.libs.json._



case class Visitor(
                    visitorId: Option[Long],
                    name: String,
                    hostName: String,
                    hostMail: String,
                    building: String,
                    contactNumber: String,
                    email: String,
                    checkInTime: Option[String],
                    checkOutTime: Option[String],
                    status: String
                  )

object Visitor {
  // Combine all the reads
  implicit val visitorReads: Reads[Visitor] = (
    (JsPath \ "visitorId").readNullable[Long] and
      (JsPath \ "name").read[String] and
      (JsPath \ "hostName").read[String] and
      (JsPath \ "hostMail").read[String] and
      (JsPath \ "building").read[String] and
      (JsPath \ "contactNumber").read[String] and
      (JsPath \ "email").read[String] and
      (JsPath \ "checkInTime").readNullable[String] and
      (JsPath \ "checkOutTime").readNullable[String] and
      (JsPath \ "status").read[String]
    )(Visitor.apply _)

  // Use Json.writes to generate Writes automatically
  implicit val visitorWrites: Writes[Visitor] = Json.writes[Visitor]

  // Combine Reads and Writes into Format
  implicit val visitorFormat: Format[Visitor] = Format(visitorReads, visitorWrites)
}
