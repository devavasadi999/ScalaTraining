package actors

import akka.actor.{Actor, Cancellable}
import akka.actor.ActorSystem
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

  case class NotificationMessage(
                                  toEmails: Seq[String],
                                  subject: String,
                                  body: String,
                                  frequencyType: String,
                                  frequencyValues: List[LocalDateTime]
                                )

  implicit val notificationMessageFormat: RootJsonFormat[NotificationMessage] = jsonFormat5(NotificationMessage)
}

import NotificationJsonProtocol._

class NotificationActor extends Actor {
  implicit val ec: ExecutionContext = context.system.dispatcher

  override def receive: Receive = {
    case messageJson: String =>
      // Parse JSON using Spray JSON
      val message = messageJson.parseJson.convertTo[NotificationMessage]
      println(s"Received at NotificationActor:${message}")
      val NotificationMessage(toEmails, subject, body, frequencyType, frequencyValues) = message

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
    val now = LocalDateTime.now()
    val delayInSeconds = ChronoUnit.SECONDS.between(now, targetTime)
    delayInSeconds.seconds
  }

  private def sendEmail(toEmails: Seq[String], subject: String, body: String): Unit = {
    println(s"Sending email to: ${toEmails.mkString(", ")}")
    println(s"Subject: $subject")
    println(s"Body: $body")
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
          val now = LocalDateTime.now()
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
