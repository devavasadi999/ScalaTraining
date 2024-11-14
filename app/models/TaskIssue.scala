package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcProfile, JdbcType}

case class TaskIssue(id: Option[Long], taskAssignmentId: Long, problem: String, status: IssueStatus.IssueStatus)

object IssueStatus extends Enumeration {
  type IssueStatus = Value
  val Pending, Resolved = Value

  // JSON formatter for IssueStatus Enumeration
  implicit val issueStatusFormat: Format[IssueStatus] = Json.formatEnum(this)

  // Slick MappedColumnType for IssueStatus Enumeration
  implicit val issueStatusMapper: JdbcType[IssueStatus] with BaseTypedType[IssueStatus] =
    MappedColumnType.base[IssueStatus, String](
      e => e.toString,
      s => IssueStatus.withName(s)
    )
}

object TaskIssue {
  // JSON formatter for TaskIssue case class
  implicit val taskIssueFormat: OFormat[TaskIssue] = Json.format[TaskIssue]
}

// Slick Table Definition for TaskIssue
class TaskIssueTable(tag: Tag) extends Table[TaskIssue](tag, "task_issue") {
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def taskAssignmentId = column[Long]("task_assignment_id")
  def problem = column[String]("problem")
  def status = column[IssueStatus.IssueStatus]("status")

  def * = (id, taskAssignmentId, problem, status) <> ((TaskIssue.apply _).tupled, TaskIssue.unapply)
}
