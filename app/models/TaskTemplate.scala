package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcProfile, JdbcType}

case class TaskTemplate(id: Long, serviceTeamId: Long, name: String, description: String)

object TaskTemplate {
  implicit val taskTemplateFormat: OFormat[TaskTemplate] = Json.format[TaskTemplate]
}

class TaskTemplateTable(tag: Tag) extends Table[TaskTemplate](tag, "task_template") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def serviceTeamId = column[Long]("service_team_id")
  def name = column[String]("name")
  def description = column[String]("description")

  def * = (id, serviceTeamId, name, description) <> ((TaskTemplate.apply _).tupled, TaskTemplate.unapply)
}
