package models

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EquipmentAllocationRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  val equipmentAllocations = TableQuery[EquipmentAllocationTable]

  // List all allocations
  def list(): Future[Seq[EquipmentAllocation]] = db.run(equipmentAllocations.result)

  // Find an allocation by allocation ID
  def find(id: Long): Future[Option[EquipmentAllocation]] = db.run(equipmentAllocations.filter(_.id === id).result.headOption)

  // Find an allocation by equipment and employee
  def findByEquipmentAndEmployee(equipmentId: Long, employeeId: Long): Future[Option[EquipmentAllocation]] =
    db.run(equipmentAllocations.filter(ea => ea.equipmentId === equipmentId && ea.employeeId === employeeId).result.headOption)

  // List all active allocations (status = Allocated)
  def listActiveAllocations(): Future[Seq[EquipmentAllocation]] =
    db.run(equipmentAllocations.filter(_.status === AllocationStatus.Allocated).result)

  // Add a new allocation
  def add(allocation: EquipmentAllocation): Future[EquipmentAllocation] = {
    val action = (equipmentAllocations returning equipmentAllocations.map(_.id)
      into ((allocation, id) => allocation.copy(id = id))
      ) += allocation

    db.run(action)
  }

  // Update allocation details
  def update(allocation: EquipmentAllocation): Future[Int] =
    db.run(equipmentAllocations.filter(_.id === allocation.id).update(allocation))

  // Update the status of an allocation by allocation ID
  def updateStatusAndReturnDate(id: Long, status: AllocationStatus.AllocationStatus, actualReturnDate: LocalDate): Future[Int] = {
    db.run(
      equipmentAllocations
        .filter(_.id === id)
        .map(a => (a.status, a.actualReturnDate))
        .update((status, Some(actualReturnDate)))
    )
  }


  // Delete an allocation by ID
  def delete(id: Long): Future[Int] = db.run(equipmentAllocations.filter(_.id === id).delete)

  // List all allocations for a specific employee
  def listByEmployee(employeeId: Long): Future[Seq[EquipmentAllocation]] =
    db.run(equipmentAllocations.filter(_.employeeId === employeeId).result)

  // List all allocations for a specific piece of equipment
  def listByEquipment(equipmentId: Long): Future[Seq[EquipmentAllocation]] =
    db.run(equipmentAllocations.filter(_.equipmentId === equipmentId).result)

  def findActiveAllocationByEquipment(equipmentId: Long): Future[Option[EquipmentAllocation]] = {
    db.run {
      equipmentAllocations
        .filter(allocation => allocation.equipmentId === equipmentId && allocation.status === AllocationStatus.Allocated)
        .result
        .headOption
    }
  }
}
