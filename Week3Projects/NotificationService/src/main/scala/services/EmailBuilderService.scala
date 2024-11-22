package services

import services.TimeZoneConversion

import java.time.LocalDateTime
import spray.json._

import java.time.format.DateTimeFormatter

case class Email(
                  toEmails: Seq[String],
                  subject: String,
                  body: String,
                  frequencyType: String,
                  frequencyValues: List[LocalDateTime]
                )

// Custom JSON format for LocalDateTime
object LocalDateTimeJsonProtocol extends DefaultJsonProtocol {
  implicit object LocalDateTimeFormat extends JsonFormat[LocalDateTime] {
    private val formatter = DateTimeFormatter.ISO_DATE_TIME

    def write(dateTime: LocalDateTime): JsValue = JsString(dateTime.format(formatter))
    def read(value: JsValue): LocalDateTime = value match {
      case JsString(str) => LocalDateTime.parse(str, formatter)
      case _ => deserializationError("Expected LocalDateTime as JsString")
    }
  }
}

import spray.json._
import LocalDateTimeJsonProtocol._

object EmailBuilderService {

  def buildEmail(messageJson: String): Email = {
    val message = messageJson.parseJson.asJsObject
    val messageType = message.fields("message_type").convertTo[String]

    // Create subject and body based on messageType
    val (toEmail, subject, body, frequencyType, frequencyValues) = messageType match {
      case "TaskAssignmentNotification" =>
        val taskAssignment = message.fields("task_assignment").asJsObject
        val eventPlan = message.fields("event_plan").asJsObject
        val serviceTeam = message.fields("service_team").asJsObject
        val taskTemplate = message.fields("task_template").asJsObject

        val body = s"""
                      |New Task Assignment:
                      |
                      |Task Details:
                      |Name: ${taskTemplate.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |Description: ${taskTemplate.fields.get("description").map(_.convertTo[String]).getOrElse("N/A")}
                      |Start Time: ${taskAssignment.fields.get("start_time").map(_.convertTo[LocalDateTime]).getOrElse("N/A")}
                      |End Time: ${taskAssignment.fields.get("end_time").map(_.convertTo[LocalDateTime]).getOrElse("N/A")}
                      |Expectations: ${taskAssignment.fields.get("expectations").map(_.convertTo[String]).getOrElse("N/A")}
                      |Special Requirements: ${taskAssignment.fields.get("special_requirements").map(_.convertTo[String]).getOrElse("N/A")}
                      |
                      |Event Details:
                      |Name: ${eventPlan.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |Date: ${eventPlan.fields.get("date").map(_.convertTo[String]).getOrElse("N/A")}
                      |
                      |Service Team: ${serviceTeam.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |
                      |Please prepare accordingly.
          """.stripMargin
        val serviceTeamEmail = serviceTeam.fields.get("email").map(_.convertTo[String]).getOrElse("N/A")

        (serviceTeamEmail, "Task Assignment Notification", body, "Fixed", List(TimeZoneConversion.getCurrentISTLocalDateTime()))

      case "PreparationReminders" =>
        val taskAssignment = message.fields("task_assignment").asJsObject
        val eventPlan = message.fields("event_plan").asJsObject
        val serviceTeam = message.fields("service_team").asJsObject
        val taskTemplate = message.fields("task_template").asJsObject

        val startTime = taskAssignment.fields.get("start_time").map(_.convertTo[LocalDateTime]).getOrElse(LocalDateTime.MIN)
        val body = s"""
                      |Preparation Reminder:
                      |
                      |Task Details:
                      |Name: ${taskTemplate.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |Description: ${taskTemplate.fields.get("description").map(_.convertTo[String]).getOrElse("N/A")}
                      |Start Time: $startTime
                      |End Time: ${taskAssignment.fields.get("end_time").map(_.convertTo[LocalDateTime]).getOrElse("N/A")}
                      |Expectations: ${taskAssignment.fields.get("expectations").map(_.convertTo[String]).getOrElse("N/A")}
                      |Special Requirements: ${taskAssignment.fields.get("special_requirements").map(_.convertTo[String]).getOrElse("N/A")}
                      |
                      |Event Details:
                      |Name: ${eventPlan.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |Date: ${eventPlan.fields.get("date").map(_.convertTo[String]).getOrElse("N/A")}
                      |
                      |Service Team: ${serviceTeam.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |
                      |Please prepare accordingly.
           """.stripMargin
        val serviceTeamEmail = serviceTeam.fields.get("email").map(_.convertTo[String]).getOrElse("N/A")

        (serviceTeamEmail, "Preparation Reminder", body, "Fixed", List(startTime.minusDays(1), startTime.minusHours(2)))

      case "ProgressCheckIn" =>
        val taskAssignment = message.fields("task_assignment").asJsObject
        val eventPlan = message.fields("event_plan").asJsObject
        val serviceTeam = message.fields("service_team").asJsObject
        val taskTemplate = message.fields("task_template").asJsObject

        val startTime = taskAssignment.fields.get("start_time").map(_.convertTo[LocalDateTime]).getOrElse(LocalDateTime.MIN)
        val endTime = taskAssignment.fields.get("end_time").map(_.convertTo[LocalDateTime]).getOrElse(LocalDateTime.MIN)
        val body = s"""
                      |Progress Check-In:
                      |
                      |Task Details:
                      |Name: ${taskTemplate.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |Description: ${taskTemplate.fields.get("description").map(_.convertTo[String]).getOrElse("N/A")}
                      |Start Time: $startTime
                      |End Time: $endTime
                      |Expectations: ${taskAssignment.fields.get("expectations").map(_.convertTo[String]).getOrElse("N/A")}
                      |Special Requirements: ${taskAssignment.fields.get("special_requirements").map(_.convertTo[String]).getOrElse("N/A")}
                      |
                      |Event Details:
                      |Name: ${eventPlan.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |Date: ${eventPlan.fields.get("date").map(_.convertTo[String]).getOrElse("N/A")}
                      |
                      |Service Team: ${serviceTeam.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |
                      |Please update the status of the project.
          """.stripMargin
        val serviceTeamEmail = serviceTeam.fields.get("email").map(_.convertTo[String]).getOrElse("N/A")

        (serviceTeamEmail, "Progress Check-In", body, "Recurring", List(startTime, endTime, startTime.plusHours(1)))

      case "EventDayAlert" =>
        val taskAssignment = message.fields("task_assignment").asJsObject
        val eventPlan = message.fields("event_plan").asJsObject
        val serviceTeam = message.fields("service_team").asJsObject
        val taskTemplate = message.fields("task_template").asJsObject

        val endTime = taskAssignment.fields.get("end_time").map(_.convertTo[LocalDateTime]).getOrElse(LocalDateTime.MIN)
        val body = s"""
                      |Event Day Alert:
                      |
                      |Task Details:
                      |Name: ${taskTemplate.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |Description: ${taskTemplate.fields.get("description").map(_.convertTo[String]).getOrElse("N/A")}
                      |Start Time: ${taskAssignment.fields.get("start_time").map(_.convertTo[LocalDateTime]).getOrElse("N/A")}
                      |End Time: $endTime
                      |Expectations: ${taskAssignment.fields.get("expectations").map(_.convertTo[String]).getOrElse("N/A")}
                      |Special Requirements: ${taskAssignment.fields.get("special_requirements").map(_.convertTo[String]).getOrElse("N/A")}
                      |
                      |Event Details:
                      |Name: ${eventPlan.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |Date: ${eventPlan.fields.get("date").map(_.convertTo[String]).getOrElse("N/A")}
                      |
                      |Service Team: ${serviceTeam.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |
                      |Please make sure that the tasks are completed on time.
          """.stripMargin
        val serviceTeamEmail = serviceTeam.fields.get("email").map(_.convertTo[String]).getOrElse("N/A")

        // Add both service team and event manager emails
        (serviceTeamEmail, "Event Day Alert", body, "Fixed", List(endTime.minusHours(1)))

      case "IssueAlert" =>
        val taskIssue = message.fields("task_issue").asJsObject
        val taskAssignment = message.fields("task_assignment").asJsObject
        val eventPlan = message.fields("event_plan").asJsObject
        val taskTemplate = message.fields("task_template").asJsObject
        val serviceTeam = message.fields("service_team").asJsObject

        val problem = taskIssue.fields.get("problem").map(_.convertTo[String]).getOrElse("N/A")

        val body = s"""
                      |Issue Raised:
                      |Problem: $problem
                      |
                      |Task Details:
                      |Name: ${taskTemplate.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |Description: ${taskTemplate.fields.get("description").map(_.convertTo[String]).getOrElse("N/A")}
                      |Start Time: ${taskAssignment.fields.get("start_time").map(_.convertTo[LocalDateTime]).getOrElse("N/A")}
                      |End Time: ${taskAssignment.fields.get("end_time").map(_.convertTo[LocalDateTime]).getOrElse("N/A")}
                      |Expectations: ${taskAssignment.fields.get("expectations").map(_.convertTo[String]).getOrElse("N/A")}
                      |Special Requirements: ${taskAssignment.fields.get("special_requirements").map(_.convertTo[String]).getOrElse("N/A")}
                      |
                      |Service Team: ${serviceTeam.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |
                      |Event Details:
                      |Name: ${eventPlan.fields.get("name").map(_.convertTo[String]).getOrElse("N/A")}
                      |Date: ${eventPlan.fields.get("date").map(_.convertTo[String]).getOrElse("N/A")}
          """.stripMargin

        // Send only to the event manager
        val eventManagerEmail = System.getProperty("EVENT_MANAGER_EMAIL")
        (eventManagerEmail, "Issue Alert", body, "Fixed", List(TimeZoneConversion.getCurrentISTLocalDateTime()))

      case "OverdueReminder" =>
        // Extract the equipment allocation details
        val equipmentAllocation = message.fields("equipmentAllocation").asJsObject
        val expectedReturnDate = equipmentAllocation.fields.get("expectedReturnDate").map(_.convertTo[String]).getOrElse("N/A")

        // Extract employee details
        val employee = message.fields.get("employee").map(_.asJsObject)
        val employeeName = employee.flatMap(_.fields.get("name")).map(_.convertTo[String]).getOrElse("N/A")

        // Extract equipment details
        val equipment = message.fields.get("equipment").map(_.asJsObject)
        val equipmentCode = equipment.flatMap(_.fields.get("code")).map(_.convertTo[String]).getOrElse("N/A")

        // Extract equipment type details
        val equipmentType = message.fields.get("equipmentType").map(_.asJsObject)
        val equipmentTypeName = equipmentType.flatMap(_.fields.get("name")).map(_.convertTo[String]).getOrElse("N/A")

        val body = s"""Overdue Reminder:
                      |Equipment Code: $equipmentCode
                      |Equipment Type: $equipmentTypeName
                      |Expected Return Date: $expectedReturnDate
                      |Employee: $employeeName
                      |Please return the equipment as soon as possible.""".stripMargin
        val employeeEmail = employee.flatMap(_.fields.get("email")).map(_.convertTo[String]).getOrElse("N/A")

        // Return tuple with message details
        (employeeEmail, "Overdue Reminder", body, "Fixed", List(TimeZoneConversion.getCurrentISTLocalDateTime()))

      case "MaintenanceTeamNotification" =>
        // Extract the equipment repair details
        val equipmentRepair = message.fields("equipmentRepair").asJsObject
        val repairDescription = equipmentRepair.fields.get("repairDescription").map(_.convertTo[String]).getOrElse("N/A")
        val status = equipmentRepair.fields.get("status").map(_.convertTo[String]).getOrElse("N/A")

        // Extract equipment details
        val equipment = message.fields.get("equipment").map(_.asJsObject)
        val equipmentCode = equipment.flatMap(_.fields.get("code")).map(_.convertTo[String]).getOrElse("N/A")

        // Extract equipment type details
        val equipmentType = message.fields.get("equipmentType").map(_.asJsObject)
        val equipmentTypeName = equipmentType.flatMap(_.fields.get("name")).map(_.convertTo[String]).getOrElse("N/A")

        val body = s"""Maintenance Team Notification:
                      |Repair Description: $repairDescription
                      |Status: $status
                      |Equipment Code: $equipmentCode
                      |Equipment Type: $equipmentTypeName
                      |Please review and proceed with the necessary actions.""".stripMargin

        val maintenanceTeamEmail = System.getProperty("MAINTENANCE_TEAM_EMAIL")
        // Return tuple with message details
        (maintenanceTeamEmail, "New Repair Request", body, "Fixed", List(TimeZoneConversion.getCurrentISTLocalDateTime()))

      case "InventoryTeamNotification" =>
        val notificationType = message.fields("notificationType").convertTo[String]
        val equipmentAllocation = message.fields("equipmentAllocation").asJsObject

        // Extract equipment allocation details
        val allocationDate = equipmentAllocation.fields.get("allocationDate").map(_.convertTo[String]).getOrElse("N/A")
        val expectedReturnDate = equipmentAllocation.fields.get("expectedReturnDate").map(_.convertTo[String]).getOrElse("N/A")
        val actualReturnDate = equipmentAllocation.fields.get("actualReturnDate").map(_.convertTo[String]).getOrElse("N/A")

        // Extract employee details
        val employee = message.fields.get("employee").map(_.asJsObject)
        val employeeName = employee.flatMap(_.fields.get("name")).map(_.convertTo[String]).getOrElse("N/A")

        // Extract equipment details
        val equipment = message.fields.get("equipment").map(_.asJsObject)
        val equipmentCode = equipment.flatMap(_.fields.get("code")).map(_.convertTo[String]).getOrElse("N/A")

        // Extract equipment type details
        val equipmentType = message.fields.get("equipmentType").map(_.asJsObject)
        val equipmentTypeName = equipmentType.flatMap(_.fields.get("name")).map(_.convertTo[String]).getOrElse("N/A")

        val (subject, body) = notificationType match {
          case "Equipment Allocated" =>
            (
              "New Equipment Allocation",
              s"""Equipment Allocation Details:
                 |Equipment Code: $equipmentCode
                 |Equipment Type: $equipmentTypeName
                 |Allocation Date: $allocationDate
                 |Expected Return Date: $expectedReturnDate
                 |Allocated to: $employeeName""".stripMargin
            )
          case "Equipment Returned" =>
            (
              "Equipment Returned",
              s"""Equipment Return Details:
                 |Equipment Code: $equipmentCode
                 |Equipment Type: $equipmentTypeName
                 |Actual Return Date: $actualReturnDate
                 |Returned by: $employeeName""".stripMargin
            )
          case _ =>
            (
              "Inventory Team Notification",
              s"""Equipment Notification:
                 |Equipment Code: $equipmentCode
                 |Equipment Type: $equipmentTypeName""".stripMargin
            )
        }

        val inventoryTeamEmail = System.getProperty("INVENTORY_TEAM_EMAIL")
        // Return tuple with message details
        (inventoryTeamEmail, subject, body, "Fixed", List(TimeZoneConversion.getCurrentISTLocalDateTime()))

      case _ => ("user@example.com", "Unknown Notification", "No details available", "Fixed", List(TimeZoneConversion.getCurrentISTLocalDateTime()))
    }

    Email(Seq(toEmail), subject, body, frequencyType, frequencyValues)
  }
}
