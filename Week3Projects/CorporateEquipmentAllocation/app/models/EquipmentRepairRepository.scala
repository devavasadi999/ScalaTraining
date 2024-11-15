package models

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EquipmentRepairRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  val equipmentRepairs = TableQuery[EquipmentRepairTable]
  val equipmentAllocations = TableQuery[EquipmentAllocationTable]
  val equipments = TableQuery[EquipmentTable]
  val equipmentTypes = TableQuery[EquipmentTypeTable]

  // List all repair requests
  def list(): Future[Seq[EquipmentRepair]] = db.run(equipmentRepairs.result)

  // Find a repair request by repair ID
  def find(id: Long): Future[Option[(EquipmentRepair, EquipmentAllocation, Equipment, EquipmentType)]] = {
    val query = for {
      repair <- equipmentRepairs if repair.id === id
      allocation <- equipmentAllocations if repair.equipmentId === allocation.equipmentId
      equipment <- equipments if allocation.equipmentId === equipment.id
      equipmentType <- equipmentTypes if equipment.equipmentTypeId === equipmentType.id
    } yield (repair, allocation, equipment, equipmentType)

    db.run(query.result.headOption)
  }

  // List all repair requests for a specific equipment item
  def listByEquipment(equipmentId: Long): Future[Seq[EquipmentRepair]] =
    db.run(equipmentRepairs.filter(_.equipmentId === equipmentId).result)

  // List all repair requests by status
  def listByStatus(status: RepairStatus.RepairStatus): Future[Seq[EquipmentRepair]] =
    db.run(equipmentRepairs.filter(_.status === status).result)

  // Add a new repair request
  def add(repair: EquipmentRepair): Future[EquipmentRepair] = {
    val action = (equipmentRepairs returning equipmentRepairs.map(_.id)
      into ((repair, id) => repair.copy(id = id))
      ) += repair

    db.run(action)
  }

  // Update repair request details
  def update(repair: EquipmentRepair): Future[Int] =
    db.run(equipmentRepairs.filter(_.id === repair.id).update(repair))

  // Update the status of a repair request by repair ID
  def updateStatus(id: Long, status: RepairStatus.RepairStatus): Future[Int] =
    db.run(equipmentRepairs.filter(_.id === id).map(_.status).update(status))

  // Delete a repair request by ID
  def delete(id: Long): Future[Int] = db.run(equipmentRepairs.filter(_.id === id).delete)
}
