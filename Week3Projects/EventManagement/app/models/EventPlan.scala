package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcProfile, JdbcType}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import play.api.libs.functional.syntax._

case class EventPlan(
                      id: Long,
                      name: String,
                      description: String,
                      eventType: EventType.EventType,
                      date: LocalDate,
                      expectedGuestCount: Int
                    )

object EventType extends Enumeration {
  type EventType = Value
  val Wedding, CorporateEvent, Birthday = Value

  implicit val eventTypeFormat: Format[EventType] = Json.formatEnum(this)

  // Slick MappedColumnType for EventType
  implicit val eventTypeMapper: JdbcType[EventType] with BaseTypedType[EventType] =
    MappedColumnType.base[EventType, String](
      e => e.toString,
      s => EventType.withName(s)
    )
}

class EventPlanTable(tag: Tag) extends Table[EventPlan](tag, "event_plan") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def description = column[String]("description")
  def eventType = column[EventType.EventType]("event_type")
  def date = column[LocalDate]("date")
  def expectedGuestCount = column[Int]("expected_guest_count")

  def * = (id, name, description, eventType, date, expectedGuestCount) <> ((EventPlan.apply _).tupled, EventPlan.unapply)
}

object EventPlan {
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  // Custom Reads for LocalDate
  implicit val localDateReads: Reads[LocalDate] = Reads[LocalDate] { json =>
    json.validate[String].map(LocalDate.parse(_, dateFormatter))
  }

  // Custom Writes for LocalDate
  implicit val localDateWrites: Writes[LocalDate] = Writes[LocalDate] { date =>
    JsString(date.format(dateFormatter))
  }

  // Combined Format for LocalDate
  implicit val localDateFormat: Format[LocalDate] = Format(localDateReads, localDateWrites)

  // Custom Reads for EventPlan
  implicit val eventPlanReads: Reads[EventPlan] = (
    (__ \ "id").read[Long].orElse(Reads.pure(0L)) and
      (__ \ "name").read[String].orElse(Reads.pure("")) and
      (__ \ "description").read[String].orElse(Reads.pure("")) and
      (__ \ "event_type").read[String].map(EventType.withName).orElse(Reads.pure(EventType.Wedding)) and
      (__ \ "date").read[LocalDate].orElse(Reads.pure(LocalDate.now())) and
      (__ \ "expected_guest_count").read[Int].orElse(Reads.pure(0))
    )(EventPlan.apply _)

  // Custom Writes for EventPlan (Snake Case Keys)
  implicit val eventPlanWrites: OWrites[EventPlan] = OWrites[EventPlan] { eventPlan =>
    Json.obj(
      "id" -> eventPlan.id,
      "name" -> eventPlan.name,
      "description" -> eventPlan.description,
      "event_type" -> eventPlan.eventType.toString,
      "date" -> eventPlan.date.format(dateFormatter),
      "expected_guest_count" -> eventPlan.expectedGuestCount
    )
  }

  // Combined Format for EventPlan
  implicit val eventPlanFormat: OFormat[EventPlan] = OFormat(eventPlanReads, eventPlanWrites)
}
