package controllers

import models.{AssignmentStatus, EventPlanRepository, Role, ServiceTeamRepository, TaskAssignment, TaskAssignmentRepository, TaskIssueRepository, TaskTemplateRepository}
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents}
import services.KafkaProducerService

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import play.api.libs.typedmap.TypedKey
import security.RequestKeys

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaskAssignmentController @Inject()(
                                          eventPlanRepository: EventPlanRepository,
                                          taskTemplateRepository: TaskTemplateRepository,
                                          serviceTeamRepository: ServiceTeamRepository,
                                          taskAssignmentRepository: TaskAssignmentRepository,
                                          taskIssueRepository: TaskIssueRepository,
                                          cc: ControllerComponents,
                                          kafkaProducer: KafkaProducerService,
                                        )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  // Utility function to fetch and validate service team access
  private def userHasAccessToServiceTeam(serviceTeamId: Long, roles: Seq[Role.Role]): Future[Boolean] = {
    serviceTeamRepository.find(serviceTeamId).map {
      case Some(serviceTeam) =>
        val normalizedTeamName = serviceTeam.name.replaceAll("\\s", "")
        roles.contains(Role.withName(normalizedTeamName)) // Compare normalized team name
      case None => false
    }
  }

  // 4) Add Task Assignment - POST
  def addTaskAssignment = Action.async(parse.json) { request =>
    val roles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    if (!roles.contains(Role.EventManager)) {
      Future.successful(Forbidden("Access denied"))
    } else {
      request.body.validate[TaskAssignment].fold(
        errors => Future.successful(BadRequest("Invalid JSON provided")),
        taskAssignment => {
          val updatedTaskAssignment = taskAssignment.copy(
            status = if (taskAssignment.status == AssignmentStatus.UNKNOWN) AssignmentStatus.TODO else taskAssignment.status
          )

          val fetchDetails = for {
            taskTemplateOpt <- taskTemplateRepository.find(updatedTaskAssignment.taskTemplateId)
            serviceTeamOpt <- serviceTeamRepository.find(updatedTaskAssignment.serviceTeamId)
            eventPlanOpt <- eventPlanRepository.find(updatedTaskAssignment.eventPlanId)
          } yield (taskTemplateOpt, serviceTeamOpt, eventPlanOpt)

          fetchDetails.flatMap {
            case (Some(taskTemplate), Some(serviceTeam), Some(eventPlan)) =>
              taskAssignmentRepository.add(updatedTaskAssignment).map { createdAssignment =>
                val notificationTypes = Seq(
                  "TaskAssignmentNotification",
                  "PreparationReminders",
                  "ProgressCheckIn",
                  "EventDayAlert"
                )

                val baseMessage = Json.obj(
                  "task_assignment" -> Json.toJson(createdAssignment),
                  "task_template" -> Json.toJson(taskTemplate),
                  "service_team" -> Json.toJson(serviceTeam),
                  "event_plan" -> Json.toJson(eventPlan)
                )

                val serviceTeamTopic = serviceTeam.name.toLowerCase.replace(" ", "_") + "_topic"
                val eventManagerTopic = "event_manager_topic"

                notificationTypes.foreach { notificationType =>
                  val message = baseMessage + ("message_type" -> Json.toJson(notificationType))
                  if (notificationType == "EventDayAlert") {
                    kafkaProducer.send(serviceTeamTopic, message.toString)
                    kafkaProducer.send(eventManagerTopic, message.toString)
                  } else {
                    kafkaProducer.send(serviceTeamTopic, message.toString)
                  }
                }

                Created(Json.toJson(createdAssignment))
              }
            case _ =>
              Future.successful(BadRequest("Failed to fetch necessary details (task template, service team, or event plan)."))
          }
        }
      )
    }
  }

  // 10) View Task Assignments for an Event Plan ID - GET
  def getTaskAssignments(eventPlanId: Long) = Action.async { request =>
    val roles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    if (!roles.contains(Role.EventManager)) {
      Future.successful(Forbidden("Access denied"))
    } else {
      taskAssignmentRepository.findByEventPlanWithDetails(eventPlanId).map { taskAssignments =>
        val response = taskAssignments.map {
          case (taskAssignment, taskTemplate, serviceTeam, eventPlan) =>
            Json.obj(
              "task_assignment" -> Json.toJson(taskAssignment),
              "task_template" -> Json.toJson(taskTemplate),
              "service_team" -> Json.toJson(serviceTeam),
              "event_plan" -> Json.toJson(eventPlan)
            )
        }
        Ok(Json.toJson(response))
      }
    }
  }

  // 12) Update Task Assignment Status or End Time - PATCH
  def updateTaskAssignment(taskAssignmentId: Long) = Action.async(parse.json) { request =>
    val roles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    taskAssignmentRepository.find(taskAssignmentId).flatMap {
      case Some((taskAssignment, _, serviceTeam, _)) =>
        userHasAccessToServiceTeam(serviceTeam.id, roles).flatMap {
          case true =>
            request.body.validate[TaskAssignment].fold(
              errors => Future.successful(BadRequest(Json.obj("error" -> "Invalid JSON format"))),
              taskAssignmentUpdate => {
                val isStatusProvided = taskAssignmentUpdate.status != AssignmentStatus.UNKNOWN
                val isEndTimeProvided = taskAssignmentUpdate.endTime != LocalDateTime.MIN

                if (!isStatusProvided && !isEndTimeProvided) {
                  Future.successful(BadRequest(Json.obj("error" -> "Either status or endTime must be provided")))
                } else {
                  val statusOpt = if (isStatusProvided) Some(taskAssignmentUpdate.status) else None
                  val endTimeOpt = if (isEndTimeProvided) Some(taskAssignmentUpdate.endTime) else None

                  taskAssignmentRepository.updatePartial(taskAssignmentId, statusOpt, endTimeOpt).map {
                    case Some(updatedTaskAssignment) => Ok(Json.toJson(updatedTaskAssignment))
                    case None => NotFound(Json.obj("error" -> "Task assignment not found"))
                  }
                }
              }
            )
          case false => Future.successful(Forbidden("Access denied"))
        }
      case None => Future.successful(NotFound("Task assignment not found"))
    }
  }

  def getTaskAssignmentsByServiceTeam(serviceTeamId: Long) = Action.async { request =>
    val roles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    (if (roles.contains(Role.EventManager))
      Future.successful(true)
     else
      userHasAccessToServiceTeam(serviceTeamId, roles))
    .flatMap {
        case true =>
          taskAssignmentRepository.findByServiceTeamWithDetails(serviceTeamId).map { taskAssignments =>
            val response = taskAssignments.map {
              case (taskAssignment, taskTemplate, serviceTeam, eventPlan) =>
                Json.obj(
                  "task_assignment" -> Json.toJson(taskAssignment),
                  "task_template" -> Json.toJson(taskTemplate),
                  "service_team" -> Json.toJson(serviceTeam),
                  "event_plan" -> Json.toJson(eventPlan)
                )
            }
            Ok(Json.toJson(response))
          }
        case false => Future.successful(Forbidden("Access denied"))
      }
  }

  def getTaskAssignmentById(id: Long) = Action.async { request =>
    val roles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    taskAssignmentRepository.find(id).flatMap {
      case Some((taskAssignment, _, serviceTeam, _)) =>
        (if (roles.contains(Role.EventManager))
          Future.successful(true)
        else
          userHasAccessToServiceTeam(serviceTeam.id, roles)).map {
          case true =>
            val response = Json.obj(
              "task_assignment" -> Json.toJson(taskAssignment),
              "service_team" -> Json.toJson(serviceTeam)
            )
            Ok(response)
          case false => Forbidden("Access denied")
        }
      case None => Future.successful(NotFound("Task assignment not found"))
    }
  }
}
