package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcProfile, JdbcType}

case class EventPlan(id: Long, name: String, description: String, eventType: EventType.EventType)

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

  def * = (id, name, description, eventType) <> ((EventPlan.apply _).tupled, EventPlan.unapply)
}
