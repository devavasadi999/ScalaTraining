package models

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ServiceTeamRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  val serviceTeams = TableQuery[ServiceTeamTable]

  def list(): Future[Seq[ServiceTeam]] = db.run(serviceTeams.result)

  def find(id: Long): Future[Option[ServiceTeam]] = db.run(serviceTeams.filter(_.id === id).result.headOption)

  def add(serviceTeam: ServiceTeam): Future[ServiceTeam] = {
    val action = (serviceTeams returning serviceTeams.map(_.id)
      into ((plan, id) => plan.copy(id = id))
      ) += serviceTeam

    db.run(action)
  }

  def update(serviceTeam: ServiceTeam): Future[Int] = db.run(serviceTeams.filter(_.id === serviceTeam.id).update(serviceTeam))

  def delete(id: Long): Future[Int] = db.run(serviceTeams.filter(_.id === id).delete)
}
