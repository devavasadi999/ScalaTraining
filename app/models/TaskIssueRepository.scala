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
  val taskAssignments = TableQuery[TaskAssignmentTable]
  val eventPlans = TableQuery[EventPlanTable]
  val serviceTeams = TableQuery[ServiceTeamTable]
  val taskTemplates = TableQuery[TaskTemplateTable]

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

  def listWithDetails(): Future[Seq[(TaskIssue, TaskAssignment, TaskTemplate, ServiceTeam, EventPlan)]] = {
    val query = for {
      issue <- taskIssues
      assignment <- taskAssignments if assignment.id === issue.taskAssignmentId
      taskTemplate <- taskTemplates if taskTemplate.id === assignment.taskTemplateId
      serviceTeam <- serviceTeams if serviceTeam.id === assignment.serviceTeamId
      eventPlan <- eventPlans if eventPlan.id === assignment.eventPlanId
    } yield (issue, assignment, taskTemplate, serviceTeam, eventPlan)

    db.run(query.result)
  }

  def findByEventPlan(eventPlanId: Long): Future[Seq[(TaskIssue, TaskAssignment, TaskTemplate, ServiceTeam, EventPlan)]] = {
    val query = for {
      issue <- taskIssues
      assignment <- taskAssignments if assignment.id === issue.taskAssignmentId && assignment.eventPlanId === eventPlanId
      taskTemplate <- taskTemplates if taskTemplate.id === assignment.taskTemplateId
      serviceTeam <- serviceTeams if serviceTeam.id === assignment.serviceTeamId
      eventPlan <- eventPlans if eventPlan.id === assignment.eventPlanId
    } yield (issue, assignment, taskTemplate, serviceTeam, eventPlan)

    db.run(query.result)
  }


  def findByServiceTeam(serviceTeamId: Long): Future[Seq[(TaskIssue, TaskAssignment, TaskTemplate, ServiceTeam, EventPlan)]] = {
    val query = for {
      issue <- taskIssues
      assignment <- taskAssignments if assignment.id === issue.taskAssignmentId && assignment.serviceTeamId === serviceTeamId
      taskTemplate <- taskTemplates if taskTemplate.id === assignment.taskTemplateId
      serviceTeam <- serviceTeams if serviceTeam.id === assignment.serviceTeamId
      eventPlan <- eventPlans if eventPlan.id === assignment.eventPlanId
    } yield (issue, assignment, taskTemplate, serviceTeam, eventPlan)

    db.run(query.result)
  }

  def findByTaskAssignment(taskAssignmentId: Long): Future[Seq[(TaskIssue, TaskAssignment, TaskTemplate, ServiceTeam, EventPlan)]] = {
    val query = for {
      issue <- taskIssues if issue.taskAssignmentId === taskAssignmentId
      assignment <- taskAssignments if assignment.id === issue.taskAssignmentId
      taskTemplate <- taskTemplates if taskTemplate.id === assignment.taskTemplateId
      serviceTeam <- serviceTeams if serviceTeam.id === assignment.serviceTeamId
      eventPlan <- eventPlans if eventPlan.id === assignment.eventPlanId
    } yield (issue, assignment, taskTemplate, serviceTeam, eventPlan)

    db.run(query.result)
  }

  def find(id: Long): Future[Option[(TaskIssue, TaskAssignment, TaskTemplate, ServiceTeam, EventPlan)]] = {
    val query = for {
      taskIssue <- taskIssues if taskIssue.id === id
      taskAssignment <- taskAssignments if taskIssue.taskAssignmentId === taskAssignment.id
      taskTemplate <- taskTemplates if taskAssignment.taskTemplateId === taskTemplate.id
      serviceTeam <- serviceTeams if taskAssignment.serviceTeamId === serviceTeam.id
      eventPlan <- eventPlans if taskAssignment.eventPlanId === eventPlan.id
    } yield (taskIssue, taskAssignment, taskTemplate, serviceTeam, eventPlan)

    db.run(query.result.headOption)
  }

}
