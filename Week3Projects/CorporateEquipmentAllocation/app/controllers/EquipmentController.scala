package controllers

import javax.inject._
import models._
import play.api.libs.json._
import play.api.mvc._
import services.KafkaProducerService

import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDate

@Singleton
class EquipmentController @Inject()(
                                     equipmentRepository: EquipmentRepository,
                                     equipmentTypeRepository: EquipmentTypeRepository,
                                     equipmentAllocationRepository: EquipmentAllocationRepository,
                                     equipmentRepairRepository: EquipmentRepairRepository,
                                     cc: ControllerComponents,
                                     kafkaProducer: KafkaProducerService,
                                     employeeRepository: EmployeeRepository
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
            equipmentAllocationRepository.add(allocation).flatMap { createdAllocation =>
              // Fetch Equipment, EquipmentType, and Employee details
              for {
                equipmentOpt <- equipmentRepository.find(equipmentId)
                employeeOpt <- employeeRepository.find(employeeId)
              } yield {
                (equipmentOpt, employeeOpt) match {
                  case (Some((equipment, equipmentType)), Some(employee)) =>
                    // Create the notification message
                    val message = Json.obj(
                      "message_type" -> "InventoryTeamNotification",
                      "to_emails" -> Json.arr("inventory_team@example.com"), // Replace with actual emails
                      "notificationType" -> "Equipment Allocated",
                      "equipmentAllocation" -> Json.toJson(createdAllocation),
                      "equipment" -> Json.toJson(equipment),
                      "equipmentType" -> Json.toJson(equipmentType),
                      "employee" -> Json.toJson(employee)
                    )
                    // Send the notification to Kafka
                    kafkaProducer.send("rawNotification", message.toString)
                    Created(Json.toJson(createdAllocation))

                  case _ =>
                    InternalServerError("Failed to fetch related equipment or employee details")
                }
              }
            }
        }
      case _ =>
        Future.successful(BadRequest("Invalid JSON format or missing fields"))
    }
  }

  // 2) Return Equipment - PATCH
  def changeStatus(equipmentAllocationId: Long) = Action.async {
    // Fetch the equipment allocation details
    equipmentAllocationRepository.find(equipmentAllocationId).flatMap {
      case Some((allocation, equipment, equipmentType, employee)) =>
        if (allocation.status == AllocationStatus.Returned) {
          // If already returned, throw an error
          Future.successful(BadRequest("Equipment is already returned."))
        } else {
          // Set the status to RETURNED and update the actual return date
          val actualReturnDate = LocalDate.now()
          equipmentAllocationRepository.updateStatusAndReturnDate(equipmentAllocationId, AllocationStatus.Returned, actualReturnDate).flatMap {
            case Some((updatedAllocation, equipment, equipmentType, employee)) =>
              // Create the notification message
              val message = Json.obj(
                "message_type" -> "InventoryTeamNotification",
                "to_emails" -> Json.arr("inventory_team@example.com"), // Replace with actual emails
                "notificationType" -> "Equipment Returned",
                "equipmentAllocation" -> Json.toJson(updatedAllocation),
                "equipment" -> Json.toJson(equipment),
                "equipmentType" -> Json.toJson(equipmentType),
                "employee" -> Json.toJson(employee)
              )
              // Send the notification to Kafka
              kafkaProducer.send("rawNotification", message.toString)
              Future.successful(Ok(Json.toJson(updatedAllocation))) // Return the updated allocation object in the response

            case None => Future.successful(NotFound("Equipment allocation not found"))
          }
        }

      case None =>
        // If the equipment allocation is not found
        Future.successful(NotFound("Equipment allocation not found"))
    }
  }

  // 3) Raise Repair Request - POST
  def raiseRepairRequest = Action.async(parse.json) { request =>
    val equipmentIdOpt = (request.body \ "equipment_id").asOpt[Long]
    val serviceDescriptionOpt = (request.body \ "service_description").asOpt[String]

    (equipmentIdOpt, serviceDescriptionOpt) match {
      case (Some(equipmentId), Some(serviceDescription)) =>
        // Fetch Equipment and EquipmentType details before saving the repair request
        equipmentRepository.find(equipmentId).flatMap {
          case Some((equipment, equipmentType)) =>
            val repairRequest = EquipmentRepair(
              id = 0,
              equipmentId = equipmentId,
              repairDescription = serviceDescription,
              status = RepairStatus.Pending
            )

            // Save the repair request in the repository
            equipmentRepairRepository.add(repairRequest).map { createdRepairRequest =>
              // Send a maintenance notification to Kafka
              val message = Json.obj(
                "message_type" -> "MaintenanceTeamNotification",
                "to_emails" -> Json.arr("maintenance_team@example.com"), // Update with actual team emails
                "equipmentRepair" -> Json.toJson(createdRepairRequest),
                "equipment" -> Json.toJson(equipment),
                "equipmentType" -> Json.toJson(equipmentType)
              )
              kafkaProducer.send("rawNotification", message.toString)

              // Return the created repair request in the response
              Created(Json.toJson(createdRepairRequest))
            }

          case None =>
            Future.successful(NotFound("Equipment not found"))
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
    equipmentRepository.findByType(equipmentTypeId).map { equipmentWithTypes =>
      if (equipmentWithTypes.nonEmpty) {
        val response = Json.toJson(equipmentWithTypes.map {
          case (equipment, equipmentType) =>
            Json.obj(
              "equipment" -> Json.toJson(equipment),
              "equipmentType" -> Json.toJson(equipmentType)
            )
        })
        Ok(response)
      } else {
        NotFound(Json.obj("error" -> s"No equipment found for type ID $equipmentTypeId"))
      }
    }
  }

  // 6) Get Equipment Allocation Details for a Particular Equipment Allocation ID - GET
  def getEquipmentAllocationDetails(equipmentAllocationId: Long) = Action.async {
    equipmentAllocationRepository.find(equipmentAllocationId).map {
      case Some((allocation, equipment, equipmentType, employee)) =>
        val response = Json.obj(
          "equipmentAllocation" -> Json.toJson(allocation),
          "equipment" -> Json.toJson(equipment),
          "equipmentType" -> Json.toJson(equipmentType),
          "employee" -> Json.toJson(employee)
        )
        Ok(response)

      case None =>
        NotFound(Json.obj("error" -> s"No equipment allocation found for ID $equipmentAllocationId"))
    }
  }

  // 7) Get Equipment Details for a Particular Equipment ID - GET
  def getEquipmentDetails(equipmentId: Long) = Action.async {
    equipmentRepository.find(equipmentId).map {
      case Some((equipment, equipmentType)) =>
        val response = Json.obj(
          "equipment" -> Json.toJson(equipment),
          "equipmentType" -> Json.toJson(equipmentType)
        )
        Ok(response)
      case None =>
        NotFound(Json.obj("error" -> s"Equipment with ID $equipmentId not found"))
    }
  }

  // 8) Get Repair Request Details for a Particular Equipment Repair ID - GET
  def getEquipmentRepairDetails(repairId: Long) = Action.async {
    equipmentRepairRepository.find(repairId).map {
      case Some((repair, allocation, equipment, equipmentType)) =>
        val response = Json.obj(
          "equipmentRepair" -> Json.toJson(repair),
          "equipmentAllocation" -> Json.toJson(allocation),
          "equipment" -> Json.toJson(equipment),
          "equipmentType" -> Json.toJson(equipmentType)
        )
        Ok(response)

      case None =>
        NotFound(Json.obj("error" -> s"No equipment repair found for ID $repairId"))
    }
  }

  // Get All Employees
  def getAllEmployees = Action.async {
    employeeRepository.list().map { employees =>
      Ok(Json.toJson(employees))
    }
  }

  //Get Employee by ID
  def getEmployeeById(id: Long) = Action.async {
    employeeRepository.find(id).map {
      case Some(employee) => Ok(Json.toJson(employee))
      case None => NotFound("Employee not found")
    }
  }

  //Get All Equipment Types
  def getAllEquipmentTypes = Action.async {
    equipmentTypeRepository.list().map { equipmentTypes =>
      Ok(Json.toJson(equipmentTypes))
    }
  }

  //Get Equipment Allocations for an Employee
  def getEquipmentAllocationsForEmployee(employeeId: Long) = Action.async {
    equipmentAllocationRepository.findByEmployee(employeeId).map { allocations =>
      val response = allocations.map { case (allocation, equipment, equipmentType) =>
        Json.obj(
          "equipment_allocation" -> Json.toJson(allocation),
          "equipment" -> Json.toJson(equipment),
          "equipment_type" -> Json.toJson(equipmentType)
        )
      }
      Ok(Json.toJson(response))
    }
  }

  //Get Available Equipment for an Equipment Type
  def getAvailableEquipments(equipmentTypeId: Long) = Action.async {
    equipmentRepository.findAvailableByType(equipmentTypeId).map { equipments =>
      val response = equipments.map { case (equipment, equipmentType) =>
        Json.obj(
          "equipment" -> Json.toJson(equipment),
          "equipment_type" -> Json.toJson(equipmentType)
        )
      }
      Ok(Json.toJson(response))
    }
  }

  //Get Equipment Allocations for an Equipment ID
  def getEquipmentAllocationsForEquipment(equipmentId: Long) = Action.async {
    equipmentAllocationRepository.findByEquipment(equipmentId).map { allocations =>
      val response = allocations.map { case (allocation, equipmentType, employee) =>
        Json.obj(
          "equipment_allocation" -> Json.toJson(allocation),
          "equipment_type" -> Json.toJson(equipmentType),
          "employee" -> Json.toJson(employee)
        )
      }
      Ok(Json.toJson(response))
    }
  }

  //Get equipment repairs for an equipment ID
  def getEquipmentRepairsForEquipment(equipmentId: Long) = Action.async {
    equipmentRepairRepository.findByEquipmentId(equipmentId).map { repairs =>
      Ok(Json.toJson(repairs))
    }
  }

  def getAllEquipmentRepairs = Action.async {
    equipmentRepairRepository.findAllWithDetails.map { repairs =>
      val response = repairs.map {
        case (repair, equipment, equipmentType) =>
          Json.obj(
            "equipment_repair" -> Json.toJson(repair),
            "equipment" -> Json.toJson(equipment),
            "equipment_type" -> Json.toJson(equipmentType)
          )
      }
      Ok(Json.toJson(response))
    }
  }

}
