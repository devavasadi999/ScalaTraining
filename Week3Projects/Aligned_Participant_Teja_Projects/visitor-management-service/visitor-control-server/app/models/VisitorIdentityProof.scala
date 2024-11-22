package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class VisitorIdentityProof(
                    id: Option[Long] = None,
                    visitorId: Long,
                    identityProof: Array[Byte]
                  )

// JSON reads and writes for Visitor
object VisitorIdentityProof {
  implicit val visitorReads: Reads[VisitorIdentityProof] = (
      (JsPath \ "id").readNullable[Long] and
      (JsPath \ "visitorId").read[Long] and
      (JsPath \ "identityProof").read[Array[Byte]]
    )(VisitorIdentityProof.apply _)

  implicit val visitorWrites: Writes[VisitorIdentityProof] = Json.writes[VisitorIdentityProof]

  implicit val visitorFormat: Format[VisitorIdentityProof] = Format(visitorReads, visitorWrites)
}