package models

import models.IssueStatus.IssueStatus
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaskIssueRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  val taskIssues = TableQuery[TaskIssueTable]

  def list(): Future[Seq[TaskIssue]] = db.run(taskIssues.result)

  def find(taskAssignmentId: Long): Future[Option[TaskIssue]] = db.run(taskIssues.filter(_.taskAssignmentId === taskAssignmentId).result.headOption)

  def add(taskIssue: TaskIssue): Future[TaskIssue] = {
    val action = (taskIssues returning taskIssues.map(_.id)
      into ((plan, id) => plan.copy(id = id))
      ) += taskIssue

    db.run(action)
  }

  def update(taskIssue: TaskIssue): Future[Int] = db.run(taskIssues.filter(_.taskAssignmentId === taskIssue.taskAssignmentId).update(taskIssue))

  def delete(taskAssignmentId: Long): Future[Int] = db.run(taskIssues.filter(_.taskAssignmentId === taskAssignmentId).delete)

  def updateStatus(taskIssueId: Long, status: IssueStatus): Future[Int] = {
    db.run(taskIssues.filter(_.id === taskIssueId).map(_.status).update(status))
  }
}
