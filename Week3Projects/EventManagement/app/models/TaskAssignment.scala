package models

import models.AssignmentStatus.AssignmentStatus

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcProfile, JdbcType}

import java.time.{LocalDate, LocalDateTime}
import java.time.format.DateTimeFormatter
import play.api.libs.functional.syntax._

case class TaskAssignment(
                           id: Long,
                           eventPlanId: Long,
                           taskTemplateId: Long,
                           serviceTeamId: Long,
                           startTime: LocalDateTime,
                           endTime: LocalDateTime,
                           specialRequirements: Option[String],
                           expectations: Option[String],
                           status: AssignmentStatus
                         )

object TaskAssignment {
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

  // Custom Reads for LocalDateTime
  implicit val localDateTimeReads: Reads[LocalDateTime] = Reads[LocalDateTime] { json =>
    json.validate[String].map(LocalDateTime.parse(_, dateTimeFormatter))
  }

  // Custom Writes for LocalDateTime
  implicit val localDateTimeWrites: Writes[LocalDateTime] = Writes[LocalDateTime] { dateTime =>
    JsString(dateTime.format(dateTimeFormatter))
  }

  // Combined Format for LocalDateTime
  implicit val localDateTimeFormat: Format[LocalDateTime] = Format(localDateTimeReads, localDateTimeWrites)

  // Custom Reads for TaskAssignment with Default Values
  implicit val taskAssignmentReads: Reads[TaskAssignment] = (
    (__ \ "id").read[Long].orElse(Reads.pure(0L)) and
      (__ \ "event_plan_id").read[Long].orElse(Reads.pure(0L)) and
      (__ \ "task_template_id").read[Long].orElse(Reads.pure(0L)) and
      (__ \ "service_team_id").read[Long].orElse(Reads.pure(0L)) and
      (__ \ "start_time").read[LocalDateTime].orElse(Reads.pure(LocalDateTime.MIN)) and
      (__ \ "end_time").read[LocalDateTime].orElse(Reads.pure(LocalDateTime.MIN)) and
      (__ \ "special_requirements").readNullable[String].orElse(Reads.pure(None)) and
      (__ \ "expectations").readNullable[String].orElse(Reads.pure(None)) and
      (__ \ "status").read[String].map(AssignmentStatus.withName).orElse(Reads.pure(AssignmentStatus.UNKNOWN))
    )(TaskAssignment.apply _)

  // Custom Writes for TaskAssignment (Snake Case Keys)
  implicit val taskAssignmentWrites: OWrites[TaskAssignment] = OWrites[TaskAssignment] { taskAssignment =>
    Json.obj(
      "id" -> taskAssignment.id,
      "event_plan_id" -> taskAssignment.eventPlanId,
      "task_template_id" -> taskAssignment.taskTemplateId,
      "service_team_id" -> taskAssignment.serviceTeamId,
      "start_time" -> taskAssignment.startTime.format(dateTimeFormatter),
      "end_time" -> taskAssignment.endTime.format(dateTimeFormatter),
      "special_requirements" -> taskAssignment.specialRequirements,
      "expectations" -> taskAssignment.expectations,
      "status" -> taskAssignment.status.toString
    )
  }

  // Combined Format for TaskAssignment
  implicit val taskAssignmentFormat: OFormat[TaskAssignment] = OFormat(taskAssignmentReads, taskAssignmentWrites)
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
  val TODO, INPROGRESS, COMPLETED, UNKNOWN = Value //UNKNOWN acts as sentinal value

  implicit val assignmentStatusFormat: Format[AssignmentStatus] = Json.formatEnum(this)

  // Slick MappedColumnType for EventType
  implicit val assignmentStatusMapper: JdbcType[AssignmentStatus] with BaseTypedType[AssignmentStatus] =
    MappedColumnType.base[AssignmentStatus, String](
      e => e.toString,
      s => AssignmentStatus.withName(s)
    )
}

class TaskAssignmentTable(tag: Tag) extends Table[TaskAssignment](tag, "task_assignment") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def eventPlanId = column[Long]("event_plan_id")
  def taskTemplateId = column[Long]("task_template_id")
  def serviceTeamId = column[Long]("service_team_id")
  def startTime = column[LocalDateTime]("start_time")
  def endTime = column[LocalDateTime]("end_time")
  def specialRequirements = column[Option[String]]("special_requirements")
  def expectations = column[Option[String]]("expectations")
  def status = column[AssignmentStatus.AssignmentStatus]("status")

  // Define foreign key constraints
  def eventPlanFK = foreignKey("event_plan_fk", eventPlanId, TableQuery[EventPlanTable])(_.id)
  def taskTemplateFK = foreignKey("task_template_fk", taskTemplateId, TableQuery[TaskTemplateTable])(_.id)
  def serviceTeamFK = foreignKey("service_team_fk", serviceTeamId, TableQuery[ServiceTeamTable])(_.id)

  def * = (id, eventPlanId, taskTemplateId, serviceTeamId, startTime, endTime, specialRequirements, expectations, status) <> ((TaskAssignment.apply _).tupled, TaskAssignment.unapply)
}
