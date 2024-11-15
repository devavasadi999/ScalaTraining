package actors

import services.KafkaProducerService
import spray.json._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

case class ProcessedMessage(
                             toEmails: Seq[String],
                             subject: String,
                             body: String,
                             frequencyType: String,
                             frequencyValues: List[LocalDateTime]
                           )

object ProcessedMessageJsonProtocol extends DefaultJsonProtocol {
  implicit val processedMessageFormat: RootJsonFormat[ProcessedMessage] = jsonFormat5(ProcessedMessage)
}

import akka.actor.Actor
import spray.json._
import java.time.LocalDateTime

class MessageProcessorActor extends Actor {
  override def receive: Receive = {
    case messageJson: String =>
      // Parse JSON using Spray
      val message = messageJson.parseJson.asJsObject
      println(s"Received at MessageProcessorActor:${message}")
      val messageType = message.fields("message_type").convertTo[String]
      var toEmails = message.fields("to_emails").convertTo[Seq[String]]

      // Create subject and body based on messageType
      val (subject, body, frequencyType, frequencyValues) = messageType match {
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

          ("Task Assignment Notification", body, "Fixed", List(LocalDateTime.now()))

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

          ("Preparation Reminder", body, "Fixed", List(startTime.minusDays(1), startTime.minusHours(2)))

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

          ("Progress Check-In", body, "Recurring", List(startTime, endTime, startTime.plusHours(1)))

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

          // Add both service team and event manager emails
          toEmails = toEmails ++ List("event_manager@example.com") // Add event manager email
          ("Event Day Alert", body, "Fixed", List(endTime.minusHours(1)))

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

          // Send only to the task manager
          ("Issue Alert", body, "Fixed", List(LocalDateTime.now()))

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

          // Return tuple with message details
          ("Overdue Reminder", body, "Fixed", List(LocalDateTime.now()))

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

          // Return tuple with message details
          ("New Repair Request", body, "Fixed", List(LocalDateTime.now()))

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

          // Return tuple with message details
          (subject, body, "Fixed", List(LocalDateTime.now()))

        case _ => ("Unknown Notification", "No details available", "Fixed", List(LocalDateTime.now()))
      }

      println(frequencyValues)

      // Build and send the processed message to Kafka
      val processedMessage = buildProcessedMessage(subject, toEmails, body, frequencyType, frequencyValues)
      new KafkaProducerService().send("processedNotification", processedMessage.compactPrint)

    case _ => // Handle unexpected messages
  }

  private def buildProcessedMessage(
                                     subject: String,
                                     toEmails: Seq[String],
                                     body: String,
                                     frequencyType: String,
                                     frequencyValues: List[LocalDateTime]
                                   ): JsObject = {
    JsObject(
      "toEmails" -> JsArray(toEmails.map(JsString(_)).toVector),
      "subject" -> JsString(subject),
      "body" -> JsString(body),
      "frequencyType" -> JsString(frequencyType),
      "frequencyValues" -> JsArray(frequencyValues.map(date => JsString(date.toString)).toVector)
    )
  }
}

