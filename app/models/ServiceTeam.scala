package models

import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import slick.jdbc.JdbcProfile

case class ServiceTeam(id: Long, name: String, description: String, email: String)

object ServiceTeam {
  implicit val serviceTeamFormat: OFormat[ServiceTeam] = Json.format[ServiceTeam]
}

class ServiceTeamTable(tag: Tag) extends Table[ServiceTeam](tag, "service_team") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def description = column[String]("description")
  def email = column[String]("email")

  def * = (id, name, description, email) <> ((ServiceTeam.apply _).tupled, ServiceTeam.unapply)
}
