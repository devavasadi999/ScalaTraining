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
  val taskTemplates = TableQuery[TaskTemplateTable]
  val serviceTeams = TableQuery[ServiceTeamTable]
  val eventPlans = TableQuery[EventPlanTable]

  def list(): Future[Seq[TaskAssignment]] = db.run(taskAssignments.result)

  def find(id: Long): Future[Option[(TaskAssignment, TaskTemplate, ServiceTeam, EventPlan)]] = {
    val query = for {
      taskAssignment <- taskAssignments if taskAssignment.id === id
      taskTemplate <- taskTemplates if taskAssignment.taskTemplateId === taskTemplate.id
      serviceTeam <- serviceTeams if taskAssignment.serviceTeamId === serviceTeam.id
      eventPlan <- eventPlans if taskAssignment.eventPlanId === eventPlan.id
    } yield (taskAssignment, taskTemplate, serviceTeam, eventPlan)

    db.run(query.result.headOption)
  }

  def add(eventPlan: TaskAssignment): Future[TaskAssignment] = {
    val action = (taskAssignments returning taskAssignments.map(_.id)
      into ((plan, id) => plan.copy(id = id))
      ) += eventPlan

    db.run(action)
  }

  def update(taskAssignment: TaskAssignment): Future[Int] = db.run(taskAssignments.filter(_.id === taskAssignment.id).update(taskAssignment))

  def delete(id: Long): Future[Int] = db.run(taskAssignments.filter(_.id === id).delete)

  def findByEventPlan(eventPlanId: Long): Future[Seq[(TaskAssignment, TaskTemplate, ServiceTeam, EventPlan)]] = {
    val query = for {
      taskAssignment <- taskAssignments if taskAssignment.eventPlanId === eventPlanId
      taskTemplate <- taskTemplates if taskAssignment.taskTemplateId === taskTemplate.id
      serviceTeam <- serviceTeams if taskAssignment.serviceTeamId === serviceTeam.id
      eventPlan <- eventPlans if taskAssignment.eventPlanId === eventPlan.id
    } yield (taskAssignment, taskTemplate, serviceTeam, eventPlan)

    db.run(query.result)
  }

  def findByServiceTeamId(serviceTeamId: Long): Future[Seq[(TaskAssignment, TaskTemplate, ServiceTeam, EventPlan)]] = {
    val query = for {
      taskAssignment <- taskAssignments if taskAssignment.serviceTeamId === serviceTeamId
      taskTemplate <- taskTemplates if taskAssignment.taskTemplateId === taskTemplate.id
      serviceTeam <- serviceTeams if taskAssignment.serviceTeamId === serviceTeam.id
      eventPlan <- eventPlans if taskAssignment.eventPlanId === eventPlan.id
    } yield (taskAssignment, taskTemplate, serviceTeam, eventPlan)

    db.run(query.result)
  }

  def updatePartial(
                     id: Long,
                     statusOpt: Option[AssignmentStatus.AssignmentStatus],
                     endTimeOpt: Option[LocalDateTime]
                   ): Future[Option[TaskAssignment]] = {
    val query = taskAssignments.filter(_.id === id)

    // Dynamically construct the update query
    val updateActions = Seq(
      statusOpt.map(status => query.map(_.status).update(status)),
      endTimeOpt.map(endTime => query.map(_.endTime).update(endTime))
    ).flatten

    // Perform the updates sequentially
    val combinedUpdate = DBIO.sequence(updateActions)

    db.run(combinedUpdate).flatMap { results =>
      if (results.exists(_ > 0)) {
        db.run(query.result.headOption) // Fetch the updated task assignment
      } else {
        Future.successful(None)
      }
    }
  }

}
