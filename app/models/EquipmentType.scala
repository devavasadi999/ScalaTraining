package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

case class EquipmentType(id: Long, name: String, description: String)

object EquipmentType {
  implicit val equipmentTypeFormat: OFormat[EquipmentType] = Json.format[EquipmentType]
}

class EquipmentTypeTable(tag: Tag) extends Table[EquipmentType](tag, "equipment_type") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def description = column[String]("description")

  def * = (id, name, description) <> ((EquipmentType.apply _).tupled, EquipmentType.unapply)
}
