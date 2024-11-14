package models

import models.AssignmentStatus.AssignmentStatus

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime
import play.api.libs.json._
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcProfile, JdbcType}

import play.api.libs.json.{Format, JsString, JsResult, JsValue, Json}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

case class TaskAssignment(
                           id: Option[Long] = None, // Make `id` an Option with a default of None
                           eventPlanId: Long,
                           taskTemplateId: Long,
                           serviceTeamId: Long,
                           startTime: LocalDateTime,
                           endTime: LocalDateTime,
                           specialRequirements: Option[String] = None,
                           expectations: Option[String] = None,
                           status: AssignmentStatus
                         )

object TaskAssignment {
  implicit val taskAssignmentFormat: OFormat[TaskAssignment] = Json.format[TaskAssignment]
}

object LocalDateTimeJson {

  // Define an ISO date formatter
  private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

  // Reads and Writes for LocalDateTime
  implicit val localDateTimeFormat: Format[LocalDateTime] = new Format[LocalDateTime] {
    override def reads(json: JsValue): JsResult[LocalDateTime] = json.validate[String].map { dateStr =>
      LocalDateTime.parse(dateStr, dateTimeFormatter)
    }

    override def writes(dateTime: LocalDateTime): JsValue = JsString(dateTime.format(dateTimeFormatter))
  }
}

import LocalDateTimeJson.localDateTimeFormat

object AssignmentStatus extends Enumeration {
  type AssignmentStatus = Value
  val TODO, INPROGRESS, COMPLETED = Value

  implicit val eventTypeFormat: Format[AssignmentStatus] = Json.formatEnum(this)

  // Slick MappedColumnType for EventType
  implicit val eventTypeMapper: JdbcType[AssignmentStatus] with BaseTypedType[AssignmentStatus] =
    MappedColumnType.base[AssignmentStatus, String](
      e => e.toString,
      s => AssignmentStatus.withName(s)
    )
}

class TaskAssignmentTable(tag: Tag) extends Table[TaskAssignment](tag, "task_assignment") {
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def eventPlanId = column[Long]("event_plan_id")
  def taskTemplateId = column[Long]("task_template_id")
  def serviceTeamId = column[Long]("service_team_id")
  def startTime = column[LocalDateTime]("start_time")
  def endTime = column[LocalDateTime]("end_time")
  def specialRequirements = column[Option[String]]("special_requirements")
  def expectations = column[Option[String]]("expectations")
  def status = column[AssignmentStatus.AssignmentStatus]("status")

  def * = (id, eventPlanId, taskTemplateId, serviceTeamId, startTime, endTime, specialRequirements, expectations, status) <> ((TaskAssignment.apply _).tupled, TaskAssignment.unapply)
}
