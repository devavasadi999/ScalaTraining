package actors

import akka.actor.{Actor, Cancellable}
import akka.actor.ActorSystem
import services.{Email, EmailService, TimeZoneConversion}
import spray.json._

import scala.concurrent.duration._
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext

// Define JSON format for the Notification message structure
object NotificationJsonProtocol extends DefaultJsonProtocol {
  implicit val localDateTimeFormat: JsonFormat[LocalDateTime] = new JsonFormat[LocalDateTime] {
    private val formatter = DateTimeFormatter.ISO_DATE_TIME
    def write(dateTime: LocalDateTime): JsValue = JsString(dateTime.format(formatter))
    def read(value: JsValue): LocalDateTime = value match {
      case JsString(str) => LocalDateTime.parse(str, formatter)
      case _ => deserializationError("Expected LocalDateTime as JsString")
    }
  }

  implicit val notificationMessageFormat: RootJsonFormat[Email] = jsonFormat5(Email)
}

import NotificationJsonProtocol._

class NotificationActor extends Actor {
  implicit val ec: ExecutionContext = context.system.dispatcher

  override def receive: Receive = {
    case message: Email =>
      println(s"Received at NotificationActor:${message}")
      val Email(toEmails, subject, body, frequencyType, frequencyValues) = message

      frequencyType match {
        case "Fixed" =>
          frequencyValues.foreach { time =>
            val delay = calculateDelay(time)
            context.system.scheduler.scheduleOnce(delay) {
              sendEmail(toEmails, subject, body)
            }
          }

        case "Recurring" =>
          if (frequencyValues.length == 3) {
            val startTime = frequencyValues.head
            val endTime = frequencyValues(1)
            val interval = ChronoUnit.HOURS.between(startTime, frequencyValues(2)).hours
            scheduleRecurringEmail(toEmails, subject, body, startTime, endTime, interval)
          }

        case _ => println("Unknown frequency type")
      }

    case _ => // Handle unexpected messages
  }

  private def calculateDelay(targetTime: LocalDateTime): FiniteDuration = {
    val now = TimeZoneConversion.getCurrentISTLocalDateTime()
    val delayInSeconds = ChronoUnit.SECONDS.between(now, targetTime)
    delayInSeconds.seconds
  }

  private def sendEmail(toEmails: Seq[String], subject: String, body: String): Unit = {
    println(s"Sending email to: ${toEmails.mkString(", ")}")
    println(s"Subject: $subject")
    println(s"Body: $body")

    EmailService.sendEmail(
      toEmails = toEmails,
      subject = subject,
      bodyText = body
    )
  }

  private def scheduleRecurringEmail(
                                      toEmails: Seq[String],
                                      subject: String,
                                      body: String,
                                      startTime: LocalDateTime,
                                      endTime: LocalDateTime,
                                      interval: FiniteDuration
                                    ): Unit = {
    val initialDelay = calculateDelay(startTime)

    var cancellable: Option[Cancellable] = None

    cancellable = Some(context.system.scheduler.scheduleWithFixedDelay(initialDelay, interval) {
      new Runnable {
        def run(): Unit = {
          val now = TimeZoneConversion.getCurrentISTLocalDateTime()
          if (now.isBefore(endTime) || now.isEqual(endTime)) {
            sendEmail(toEmails, subject, body)
          } else {
            cancellable.foreach(_.cancel()) // Cancel the schedule if the end time is reached
          }
        }
      }
    })
  }
}
