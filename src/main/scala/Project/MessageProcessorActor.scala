package Project

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
      val messageType = message.fields("messageType").convertTo[String]
      var toEmails = message.fields("toEmails").convertTo[Seq[String]]

      // Create subject and body based on messageType
      val (subject, body, frequencyType, frequencyValues) = messageType match {
        case "TaskAssignmentNotification" =>
          val taskAssignment = message.fields("taskAssignment").asJsObject
          val body = s"Task Details:\n" +
            s"Expectations: ${taskAssignment.fields.get("expectations").map(_.convertTo[String]).getOrElse("N/A")}\n" +
            s"Special Requirements: ${taskAssignment.fields.get("specialRequirements").map(_.convertTo[String]).getOrElse("N/A")}"
          ("Task Assignment Notification", body, "Fixed", List(LocalDateTime.now()))

        case "PreparationReminders" =>
          val taskAssignment = message.fields("taskAssignment").asJsObject
          val startTime = taskAssignment.fields("startTime").convertTo[LocalDateTime]
          val body = s"Preparation Reminder:\n" +
            s"Start Time: $startTime\n" +
            s"Expectations: ${taskAssignment.fields.get("expectations").map(_.convertTo[String]).getOrElse("N/A")}"
          ("Preparation Reminder", body, "Fixed", List(startTime.minusDays(1), startTime.minusHours(2)))

        case "ProgressCheckIn" =>
          val taskAssignment = message.fields("taskAssignment").asJsObject
          val startTime = taskAssignment.fields("startTime").convertTo[LocalDateTime]
          val endTime = taskAssignment.fields("endTime").convertTo[LocalDateTime]
          val body = s"Progress Check-In:\n" +
            s"Start Time: $startTime\nEnd Time: $endTime\n" +
            s"Please update your progress."
          ("Progress Check-In", body, "Recurring", List(startTime, endTime, startTime.plusHours(1)))

        case "EventDayAlert" =>
          val taskAssignment = message.fields("taskAssignment").asJsObject
          val endTime = taskAssignment.fields("endTime").convertTo[LocalDateTime]
          val body = s"Event Day Alert:\n" +
            s"End Time: $endTime\n" +
            s"Make final preparations for the event."

          // Add the event manager's email to `toEmails`
          toEmails = toEmails :+ "event_manager@example.com"  // Replace with actual event manager email

          ("Event Day Alert", body, "Fixed", List(endTime.minusHours(1)))

        case "IssueAlert" =>
          val taskIssue = message.fields("taskIssue").asJsObject
          val body = s"Issue Alert:\n" +
            s"Problem: ${taskIssue.fields.get("problem").map(_.convertTo[String]).getOrElse("N/A")}\n" +
            s"Status: ${taskIssue.fields.get("status").map(_.convertTo[String]).getOrElse("N/A")}"
          ("Issue Alert", body, "Fixed", List(LocalDateTime.now()))

        case "OverdueReminder" =>
          // Extract the equipment allocation details
          val equipmentAllocation = message.fields("equipmentAllocation").asJsObject
          val expectedReturnDate = equipmentAllocation.fields.get("expectedReturnDate").map(_.convertTo[String]).getOrElse("N/A")

          val body = s"Overdue Reminder:\n" +
            s"Expected Return Date: $expectedReturnDate\n" +
            s"Please return the equipment as soon as possible."

          // Return tuple with message details
          ("Overdue Reminder", body, "Fixed", List(LocalDateTime.now()))

        case "MaintenanceTeamNotification" =>
          // Extract the equipment repair details
          val equipmentRepair = message.fields("equipmentRepair").asJsObject
          val repairDescription = equipmentRepair.fields.get("repairDescription").map(_.convertTo[String]).getOrElse("N/A")
          val status = equipmentRepair.fields.get("status").map(_.convertTo[String]).getOrElse("N/A")

          val body = s"Maintenance Team Notification:\n" +
            s"Repair Description: $repairDescription\n" +
            s"Status: $status\n" +
            s"Please review and proceed with the necessary actions."

          // Return tuple with message details
          ("New Repair Request", body, "Fixed", List(LocalDateTime.now()))

        case "InventoryTeamNotification" =>
          val notificationType = message.fields("notificationType").convertTo[String]
          val equipmentAllocation = message.fields("equipmentAllocation").asJsObject

          // Extract equipment allocation details
          val allocationDate = equipmentAllocation.fields.get("allocationDate").map(_.convertTo[String]).getOrElse("N/A")
          val expectedReturnDate = equipmentAllocation.fields.get("expectedReturnDate").map(_.convertTo[String]).getOrElse("N/A")
          val actualReturnDate = equipmentAllocation.fields.get("actualReturnDate").map(_.convertTo[String]).getOrElse("N/A")

          // Customize subject and body based on notification type
          val (subject, body) = notificationType match {
            case "Equipment Allocated" =>
              (
                "New Equipment Allocation",
                s"Equipment has been allocated to Employee on $allocationDate. Expected return date: $expectedReturnDate."
              )
            case "Equipment Returned" =>
              (
                "Equipment Returned",
                s"Equipment has been returned by Employee on $actualReturnDate."
              )
            case _ =>
              (
                "Inventory Team Notification",
                s"Notification regarding Equipment."
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

