package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._
import play.api.libs.json._
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcProfile, JdbcType}

case class EquipmentRepair(
                            id: Long,
                            equipmentId: Long,
                            repairDescription: String,
                            status: RepairStatus.RepairStatus
                          )

object RepairStatus extends Enumeration {
  type RepairStatus = Value
  val Pending, InProgress, Completed = Value

  implicit val repairStatusFormat: Format[RepairStatus] = Json.formatEnum(this)

  implicit val repairStatusMapper: JdbcType[RepairStatus] with BaseTypedType[RepairStatus] =
    MappedColumnType.base[RepairStatus, String](
      _.toString,
      RepairStatus.withName
    )
}

object EquipmentRepair {
  implicit val equipmentRepairFormat: OFormat[EquipmentRepair] = Json.format[EquipmentRepair]
}

class EquipmentRepairTable(tag: Tag) extends Table[EquipmentRepair](tag, "equipment_repair") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def equipmentId = column[Long]("equipment_id")
  def repairDescription = column[String]("repair_description")
  def status = column[RepairStatus.RepairStatus]("status")

  def * = (id, equipmentId, repairDescription, status) <> ((EquipmentRepair.apply _).tupled, EquipmentRepair.unapply)

  def equipment = foreignKey("equipment_fk", equipmentId, TableQuery[EquipmentTable])(_.id)
}
