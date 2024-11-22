package controllers

import models.{EventPlanRepository, IssueStatus, Role, ServiceTeamRepository, TaskAssignmentRepository, TaskIssue, TaskIssueRepository, TaskTemplateRepository}
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents}
import services.KafkaProducerService

import javax.inject.{Inject, Singleton}
import play.api.libs.typedmap.TypedKey
import security.RequestKeys

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

  private def userHasAccessToServiceTeam(serviceTeamId: Long, roles: Seq[Role.Role]): Future[Boolean] = {
    serviceTeamRepository.find(serviceTeamId).map {
      case Some(serviceTeam) =>
        val normalizedTeamName = serviceTeam.name.replaceAll("\\s", "")
        roles.contains(Role.withName(normalizedTeamName))
      case None => false
    }
  }

  // 5) Add Task Issue - POST
  def addTaskIssue = Action.async(parse.json) { request =>
    val roles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    request.body.validate[TaskIssue].fold(
      errors => Future.successful(BadRequest("Invalid JSON provided")),
      taskIssue => {
        taskAssignmentRepository.find(taskIssue.taskAssignmentId).flatMap {
          case Some((_, _, serviceTeam, _)) =>
            userHasAccessToServiceTeam(serviceTeam.id, roles).flatMap {
              case true =>
                taskIssueRepository.add(taskIssue).flatMap { createdIssue =>
                  kafkaProducer.send("event_manager_topic", Json.toJson(createdIssue).toString)
                  Future.successful(Created(Json.toJson(createdIssue)))
                }
              case false => Future.successful(Forbidden("Access denied"))
            }
          case None => Future.successful(BadRequest("Task assignment not found"))
        }
      }
    )
  }

  // 11) Update Task Issue Status - PATCH
  def updateTaskIssueStatus(taskIssueId: Long) = Action.async(parse.json) { request =>
    val roles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    taskIssueRepository.find(taskIssueId).flatMap {
      case Some((taskIssue, taskAssignment, _, serviceTeam, _)) =>
        userHasAccessToServiceTeam(serviceTeam.id, roles).flatMap {
          case true =>
            val statusOpt = (request.body \ "status").asOpt[IssueStatus.IssueStatus]
            statusOpt match {
              case Some(status) =>
                taskIssueRepository.updateStatus(taskIssueId, status).map {
                  case 1 => Ok("Task issue status updated")
                  case _ => NotFound("Task issue not found")
                }
              case None => Future.successful(BadRequest("Invalid status"))
            }
          case false => Future.successful(Forbidden("Access denied"))
        }
      case None => Future.successful(NotFound("Task issue not found"))
    }
  }

  // 13) View All Task Issues - GET
  def listTaskIssues = Action.async { request =>
    val roles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    if (!roles.contains(Role.EventManager)) {
      Future.successful(Forbidden("Access denied"))
    } else {
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
  }

  def getTaskIssuesByEventPlan(eventPlanId: Long) = Action.async { request =>
    val roles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    if (!roles.contains(Role.EventManager)) {
      Future.successful(Forbidden("Access denied"))
    } else {
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
  }

  def getTaskIssuesByServiceTeam(serviceTeamId: Long) = Action.async { request =>
    val roles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    (if (roles.contains(Role.EventManager))
      Future.successful(true)
    else
      userHasAccessToServiceTeam(serviceTeamId, roles)).flatMap {
      case true =>
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
      case false => Future.successful(Forbidden("Access denied"))
    }
  }

  def getTaskIssuesByTaskAssignment(taskAssignmentId: Long) = Action.async { request =>
    val roles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    taskAssignmentRepository.find(taskAssignmentId).flatMap {
      case Some((_, _, serviceTeam, _)) =>
        (if (roles.contains(Role.EventManager))
          Future.successful(true)
        else
          userHasAccessToServiceTeam(serviceTeam.id, roles)).flatMap {
          case true =>
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
          case false => Future.successful(Forbidden("Access denied"))
        }
      case None => Future.successful(NotFound("Task assignment not found"))
    }
  }

  def getTaskIssueById(id: Long) = Action.async { request =>
    val roles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    taskIssueRepository.find(id).flatMap {
      case Some((taskIssue, taskAssignment, taskTemplate, serviceTeam, eventPlan)) =>
        (if (roles.contains(Role.EventManager))
          Future.successful(true)
        else
          userHasAccessToServiceTeam(serviceTeam.id, roles)).map {
          case true =>
            val response = Json.obj(
              "task_issue" -> Json.toJson(taskIssue),
              "task_assignment" -> Json.toJson(taskAssignment),
              "task_template" -> Json.toJson(taskTemplate),
              "service_team" -> Json.toJson(serviceTeam),
              "event_plan" -> Json.toJson(eventPlan)
            )
            Ok(response)
          case false => Forbidden("Access denied")
        }
      case None => Future.successful(NotFound("Task issue not found"))
    }
  }

}
