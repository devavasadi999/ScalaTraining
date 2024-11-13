package controllers

import javax.inject._
import models._
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDate

@Singleton
class EquipmentController @Inject()(
                                     equipmentRepository: EquipmentRepository,
                                     equipmentTypeRepository: EquipmentTypeRepository,
                                     equipmentAllocationRepository: EquipmentAllocationRepository,
                                     equipmentRepairRepository: EquipmentRepairRepository,
                                     cc: ControllerComponents
                                   )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  implicit val equipmentFormat: Format[Equipment] = Json.format[Equipment]
  implicit val equipmentTypeFormat: Format[EquipmentType] = Json.format[EquipmentType]
  implicit val employeeFormat: Format[Employee] = Json.format[Employee]
  implicit val equipmentAllocationFormat: Format[EquipmentAllocation] = Json.format[EquipmentAllocation]
  implicit val equipmentRepairFormat: Format[EquipmentRepair] = Json.format[EquipmentRepair]
  implicit val allocationStatusReads: Reads[AllocationStatus.AllocationStatus] = Reads.enumNameReads(AllocationStatus)
  implicit val repairStatusReads: Reads[RepairStatus.RepairStatus] = Reads.enumNameReads(RepairStatus)

  // 1) Allocate Equipment to Employee - POST
  def allocateEquipment = Action.async(parse.json) { request =>
    val employeeIdOpt = (request.body \ "employee_id").asOpt[Long]
    val equipmentIdOpt = (request.body \ "equipment_id").asOpt[Long]
    val purposeOpt = (request.body \ "purpose").asOpt[String]
    val expectedReturnDateOpt = (request.body \ "expectedReturnDate").asOpt[LocalDate]

    (employeeIdOpt, equipmentIdOpt, purposeOpt) match {
      case (Some(employeeId), Some(equipmentId), Some(purpose)) =>
        // Check if the equipment is currently allocated
        equipmentAllocationRepository.findActiveAllocationByEquipment(equipmentId).flatMap {
          case Some(_) =>
            // Equipment is already allocated
            Future.successful(BadRequest("Equipment is already allocated"))
          case None =>
            // Equipment is available, proceed with allocation
            val allocation = EquipmentAllocation(
              id = 0,
              equipmentId = equipmentId,
              employeeId = employeeId,
              purpose = purpose,
              allocationDate = LocalDate.now(),
              expectedReturnDate = expectedReturnDateOpt,
              actualReturnDate = None,
              status = AllocationStatus.Allocated
            )
            equipmentAllocationRepository.add(allocation).map { createdAllocation =>
              Created(Json.toJson(createdAllocation))
            }
        }
      case _ =>
        Future.successful(BadRequest("Invalid JSON format or missing fields"))
    }
  }

  // 2) Return Equipment - PATCH
  def returnEquipment(equipmentAllocationId: Long) = Action.async(parse.json) { request =>
    val statusOpt = (request.body \ "status").asOpt[AllocationStatus.AllocationStatus]

    statusOpt match {
      case Some(AllocationStatus.Returned) =>
        val actualReturnDate = LocalDate.now()
        equipmentAllocationRepository.updateStatusAndReturnDate(equipmentAllocationId, AllocationStatus.Returned, actualReturnDate).map {
          case 1 => Ok("Equipment successfully returned with updated return date")
          case _ => NotFound("Equipment allocation not found")
        }
      case _ =>
        Future.successful(BadRequest("Invalid status"))
    }
  }

  // 3) Raise Repair Request - POST
  def raiseRepairRequest = Action.async(parse.json) { request =>
    val equipmentIdOpt = (request.body \ "equipment_id").asOpt[Long]
    val serviceDescriptionOpt = (request.body \ "service_description").asOpt[String]

    (equipmentIdOpt, serviceDescriptionOpt) match {
      case (Some(equipmentId), Some(serviceDescription)) =>
        val repairRequest = EquipmentRepair(
          id = 0,
          equipmentId = equipmentId,
          repairDescription = serviceDescription,
          status = RepairStatus.Pending
        )
        equipmentRepairRepository.add(repairRequest).map { createdRepairRequest =>
          Created(Json.toJson(createdRepairRequest))
        }
      case _ =>
        Future.successful(BadRequest("Invalid JSON format or missing fields"))
    }
  }

  // 4) Change Status of Equipment Repair - PATCH
  def updateRepairStatus(equipmentRepairId: Long) = Action.async(parse.json) { request =>
    val statusOpt = (request.body \ "status").asOpt[RepairStatus.RepairStatus]

    statusOpt match {
      case Some(status) =>
        equipmentRepairRepository.updateStatus(equipmentRepairId, status).map {
          case 1 => Ok("Repair status updated successfully")
          case _ => NotFound("Repair request not found")
        }
      case None =>
        Future.successful(BadRequest("Invalid status"))
    }
  }

  // 5) Get Equipments for a Particular Equipment Type - GET
  def getEquipmentsByType(equipmentTypeId: Long) = Action.async {
    equipmentRepository.findByType(equipmentTypeId).map { equipments =>
      Ok(Json.toJson(equipments))
    }
  }

  // 6) Get Equipment Allocation Details for a Particular Equipment Allocation ID - GET
  def getEquipmentAllocationDetails(equipmentAllocationId: Long) = Action.async {
    equipmentAllocationRepository.find(equipmentAllocationId).map {
      case Some(allocation) => Ok(Json.toJson(allocation))
      case None => NotFound
    }
  }

  // 7) Get Equipment Details for a Particular Equipment ID - GET
  def getEquipmentDetails(equipmentId: Long) = Action.async {
    equipmentRepository.find(equipmentId).map {
      case Some(equipment) => Ok(Json.toJson(equipment))
      case None => NotFound
    }
  }

  // 8) Get Repair Request Details for a Particular Equipment Repair ID - GET
  def getRepairRequestDetails(equipmentRepairId: Long) = Action.async {
    equipmentRepairRepository.find(equipmentRepairId).map {
      case Some(repairRequest) => Ok(Json.toJson(repairRequest))
      case None => NotFound
    }
  }
}
