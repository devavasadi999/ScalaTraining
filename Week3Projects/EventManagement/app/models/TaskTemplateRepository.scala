package models

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaskTemplateRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  val taskTemplates = TableQuery[TaskTemplateTable]

  def list(): Future[Seq[TaskTemplate]] = db.run(taskTemplates.result)

  def find(id: Long): Future[Option[TaskTemplate]] = db.run(taskTemplates.filter(_.id === id).result.headOption)

  def add(taskTemplate: TaskTemplate): Future[TaskTemplate] = {
    val action = (taskTemplates returning taskTemplates.map(_.id)
      into ((plan, id) => plan.copy(id = id))
      ) += taskTemplate

    db.run(action)
  }

  def update(taskTemplate: TaskTemplate): Future[Int] = db.run(taskTemplates.filter(_.id === taskTemplate.id).update(taskTemplate))

  def delete(id: Long): Future[Int] = db.run(taskTemplates.filter(_.id === id).delete)

  def findByServiceTeam(serviceTeamId: Long): Future[Seq[TaskTemplate]] = db.run(taskTemplates.filter(_.serviceTeamId === serviceTeamId).result)

}
