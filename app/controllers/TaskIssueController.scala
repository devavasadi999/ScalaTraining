package controllers

import models.{EventPlanRepository, IssueStatus, ServiceTeamRepository, TaskAssignmentRepository, TaskIssue, TaskIssueRepository, TaskTemplateRepository}
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents}
import services.KafkaProducerService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaskIssueController @Inject()(
                                          eventPlanRepository: EventPlanRepository,
                                          taskTemplateRepository: TaskTemplateRepository,
                                          serviceTeamRepository: ServiceTeamRepository,
                                          taskAssignmentRepository: TaskAssignmentRepository,
                                          taskIssueRepository: TaskIssueRepository,
                                          cc: ControllerComponents,
                                          kafkaProducer: KafkaProducerService,
                                        )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // 5) Add Task Issue - POST
  def addTaskIssue = Action.async(parse.json) { request =>
    request.body.validate[TaskIssue].fold(
      errors => Future.successful(BadRequest("Invalid JSON provided")),
      taskIssue => {
        // Add the task issue to the repository
        taskIssueRepository.add(taskIssue).flatMap { createdIssue =>
          // Fetch related TaskAssignment along with TaskTemplate, ServiceTeam, and EventPlan
          taskAssignmentRepository.find(taskIssue.taskAssignmentId).flatMap {
            case Some((taskAssignment, taskTemplate, serviceTeam, eventPlan)) =>
              // Create the message with TaskIssue and related details
              val message = Json.obj(
                "message_type" -> "IssueAlert",
                "to_emails" -> Json.arr("event.manager@example.com"), // Replace with actual email(s)
                "task_issue" -> Json.toJson(createdIssue),
                "task_assignment" -> Json.toJson(taskAssignment),
                "task_template" -> Json.toJson(taskTemplate),
                "service_team" -> Json.toJson(serviceTeam),
                "event_plan" -> Json.toJson(eventPlan)
              )
              // Send the notification to Kafka
              kafkaProducer.send("rawNotification", message.toString)
              Future.successful(Created(Json.toJson(createdIssue)))

            case None =>
              // Handle case where TaskAssignment is not found
              Future.successful(BadRequest("Task assignment not found for the task issue"))
          }
        }
      }
    )
  }

  // 11) Update Task Issue Status - PATCH
  def updateTaskIssueStatus(taskIssueId: Long) = Action.async(parse.json) { request =>
    val statusOpt = (request.body \ "status").asOpt[IssueStatus.IssueStatus]

    statusOpt match {
      case Some(status) =>
        taskIssueRepository.updateStatus(taskIssueId, status).map {
          case 1 => Ok("Task issue status updated")
          case _ => NotFound("Task issue not found")
        }
      case None => Future.successful(BadRequest("Invalid status"))
    }
  }

  // 13) View All Task Issues - GET
  def listTaskIssues = Action.async {
    taskIssueRepository.listWithDetails().map { taskIssues =>
      val jsonResponse = taskIssues.map { case (issue, assignment, template, team, plan) =>
        Json.obj(
          "task_issue" -> Json.toJson(issue),
          "task_assignment" -> Json.toJson(assignment),
          "task_template" -> Json.toJson(template),
          "service_team" -> Json.toJson(team),
          "event_plan" -> Json.toJson(plan)
        )
      }
      Ok(Json.toJson(jsonResponse))
    }
  }

  def getTaskIssuesByEventPlan(eventPlanId: Long) = Action.async {
    taskIssueRepository.findByEventPlan(eventPlanId).map { issues =>
      val response = issues.map { case (issue, assignment, template, team, plan) =>
        Json.obj(
          "task_issue" -> Json.toJson(issue),
          "task_assignment" -> Json.toJson(assignment),
          "task_template" -> Json.toJson(template),
          "service_team" -> Json.toJson(team),
          "event_plan" -> Json.toJson(plan)
        )
      }
      Ok(Json.toJson(response))
    }
  }

  def getTaskIssuesByServiceTeam(serviceTeamId: Long) = Action.async {
    taskIssueRepository.findByServiceTeam(serviceTeamId).map { issues =>
      val response = issues.map { case (issue, assignment, template, team, plan) =>
        Json.obj(
          "task_issue" -> Json.toJson(issue),
          "task_assignment" -> Json.toJson(assignment),
          "task_template" -> Json.toJson(template),
          "service_team" -> Json.toJson(team),
          "event_plan" -> Json.toJson(plan)
        )
      }
      Ok(Json.toJson(response))
    }
  }

  def getTaskIssuesByTaskAssignment(taskAssignmentId: Long) = Action.async {
    taskIssueRepository.findByTaskAssignment(taskAssignmentId).map { issues =>
      val response = issues.map { case (issue, assignment, template, team, plan) =>
        Json.obj(
          "task_issue" -> Json.toJson(issue),
          "task_assignment" -> Json.toJson(assignment),
          "task_template" -> Json.toJson(template),
          "service_team" -> Json.toJson(team),
          "event_plan" -> Json.toJson(plan)
        )
      }
      Ok(Json.toJson(response))
    }
  }

  def getTaskIssueById(id: Long) = Action.async {
    taskIssueRepository.find(id).map {
      case Some((taskIssue, taskAssignment, taskTemplate, serviceTeam, eventPlan)) =>
        val response = Json.obj(
          "task_issue" -> Json.toJson(taskIssue),
          "task_assignment" -> Json.toJson(taskAssignment),
          "task_template" -> Json.toJson(taskTemplate),
          "service_team" -> Json.toJson(serviceTeam),
          "event_plan" -> Json.toJson(eventPlan)
        )
        Ok(response)
      case None => NotFound("Task issue not found")
    }
  }

}