package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Room(id: Option[Int], roomName: String, capacity: Int)

object Room {
  implicit val roomReads: Reads[Room] = (
      (JsPath \ "id").readNullable[Int] and
      (JsPath \ "room_name").read[String] and
      (JsPath \ "capacity").read[Int]
    )(Room.apply _)

  implicit val roomWrites: Writes[Room] = Json.writes[Room]

  implicit val roomFormat: Format[Room] = Format(roomReads, roomWrites)
}

