package models

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaskAssignmentRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  val taskAssignments = TableQuery[TaskAssignmentTable]

  def list(): Future[Seq[TaskAssignment]] = db.run(taskAssignments.result)

  def find(id: Long): Future[Option[TaskAssignment]] = db.run(taskAssignments.filter(_.id === id).result.headOption)

  def add(eventPlan: TaskAssignment): Future[TaskAssignment] = {
    val action = (taskAssignments returning taskAssignments.map(_.id)
      into ((plan, id) => plan.copy(id = id))
      ) += eventPlan

    db.run(action)
  }

  def update(taskAssignment: TaskAssignment): Future[Int] = db.run(taskAssignments.filter(_.id === taskAssignment.id).update(taskAssignment))

  def delete(id: Long): Future[Int] = db.run(taskAssignments.filter(_.id === id).delete)

  def findByEventPlan(eventPlanId: Long): Future[Seq[TaskAssignment]] = db.run(taskAssignments.filter(_.eventPlanId === eventPlanId).result)

  def update(taskAssignmentId: Long, status: Option[AssignmentStatus.AssignmentStatus], endTime: Option[LocalDateTime]): Future[Int] = {
    val query = taskAssignments.filter(_.id === taskAssignmentId)
    val updateAction = query.map(ta => (ta.status, ta.endTime)).update((status.get, endTime.get))
    db.run(updateAction)
  }

}
