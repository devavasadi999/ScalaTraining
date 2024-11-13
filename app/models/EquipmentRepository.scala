package models

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import models.AllocationStatus.allocationStatusMapper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EquipmentRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  val equipments = TableQuery[EquipmentTable]
  val equipmentAllocations = TableQuery[EquipmentAllocationTable]

  def list(): Future[Seq[Equipment]] = db.run(equipments.result)

  def find(id: Long): Future[Option[Equipment]] = db.run(equipments.filter(_.id === id).result.headOption)

  def add(equipment: Equipment): Future[Equipment] = {
    val action = (equipments returning equipments.map(_.id)
      into ((eq, id) => eq.copy(id = id))
      ) += equipment

    db.run(action)
  }

  def update(equipment: Equipment): Future[Int] = db.run(equipments.filter(_.id === equipment.id).update(equipment))

  def delete(id: Long): Future[Int] = db.run(equipments.filter(_.id === id).delete)

  // New methods
  def findByType(equipmentTypeId: Long): Future[Seq[Equipment]] =
    db.run(equipments.filter(_.equipmentTypeId === equipmentTypeId).result)

  def findAvailableEquipmentByType(equipmentTypeId: Long): Future[Seq[Equipment]] = {
    // Subquery to get IDs of equipment that are currently allocated
    val allocatedEquipmentIds = equipmentAllocations
      .filter(_.status === AllocationStatus.Allocated)
      .map(_.equipmentId)

    // Main query to find unallocated equipment of the specified type
    val query = equipments
      .filter(e => e.equipmentTypeId === equipmentTypeId && !e.id.in(allocatedEquipmentIds))
      .result

    db.run(query)
  }
}
