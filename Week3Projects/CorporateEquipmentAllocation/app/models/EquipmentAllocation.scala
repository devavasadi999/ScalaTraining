package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._
import play.api.libs.json._
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcProfile, JdbcType}

import java.time.LocalDate

case class EquipmentAllocation(
                                id: Long,
                                equipmentId: Long,
                                employeeId: Long,
                                purpose: String,
                                allocationDate: LocalDate,
                                expectedReturnDate: Option[LocalDate],
                                actualReturnDate: Option[LocalDate],
                                status: AllocationStatus.AllocationStatus
                              )

object AllocationStatus extends Enumeration {
  type AllocationStatus = Value
  val Allocated, Returned = Value

  implicit val allocationStatusFormat: Format[AllocationStatus] = Json.formatEnum(this)

  // MappedColumnType for Slick
  implicit val allocationStatusMapper: JdbcType[AllocationStatus] with BaseTypedType[AllocationStatus] =
    MappedColumnType.base[AllocationStatus, String](
      _.toString,
      AllocationStatus.withName
    )
}

object EquipmentAllocation {
  implicit val equipmentAllocationFormat: OFormat[EquipmentAllocation] = Json.format[EquipmentAllocation]
}

class EquipmentAllocationTable(tag: Tag) extends Table[EquipmentAllocation](tag, "equipment_allocation") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def equipmentId = column[Long]("equipment_id")
  def employeeId = column[Long]("employee_id")
  def purpose = column[String]("purpose")
  def allocationDate = column[LocalDate]("allocation_date")
  def expectedReturnDate = column[Option[LocalDate]]("expected_return_date")
  def actualReturnDate = column[Option[LocalDate]]("actual_return_date")
  def status = column[AllocationStatus.AllocationStatus]("status")

  def * = (id, equipmentId, employeeId, purpose, allocationDate, expectedReturnDate, actualReturnDate, status) <> ((EquipmentAllocation.apply _).tupled, EquipmentAllocation.unapply)

  def equipment = foreignKey("equipment_fk", equipmentId, TableQuery[EquipmentTable])(_.id)
  def employee = foreignKey("employee_fk", employeeId, TableQuery[EmployeeTable])(_.id)
}
