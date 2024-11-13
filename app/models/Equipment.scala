package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

case class Equipment(id: Long, code: String, equipmentTypeId: Long)

object Equipment {
  implicit val equipmentFormat: OFormat[Equipment] = Json.format[Equipment]
}

class EquipmentTable(tag: Tag) extends Table[Equipment](tag, "equipment") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def code = column[String]("code")
  def equipmentTypeId = column[Long]("equipment_type_id")

  def * = (id, code, equipmentTypeId) <> ((Equipment.apply _).tupled, Equipment.unapply)

  def equipmentType = foreignKey("equipment_type_fk", equipmentTypeId, TableQuery[EquipmentTypeTable])(_.id)
}
