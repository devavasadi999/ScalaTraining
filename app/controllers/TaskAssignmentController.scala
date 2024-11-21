package controllers

import models.{AssignmentStatus, EventPlanRepository, ServiceTeamRepository, TaskAssignment, TaskAssignmentRepository, TaskIssueRepository, TaskTemplateRepository}
import play.api.libs.json._
import play.api.mvc.{AbstractController, ControllerComponents}
import services.KafkaProducerService

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
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

  // 4) Add Task Assignment - POST
  def addTaskAssignment = Action.async(parse.json) { request =>
    request.body.validate[TaskAssignment].fold(
      errors => {
        println(errors)
        Future.successful(BadRequest("Invalid JSON provided"))
      },
      taskAssignment => {
        val updatedTaskAssignment = taskAssignment.copy(
          status = taskAssignment.status match {
            case AssignmentStatus.UNKNOWN => AssignmentStatus.TODO
            case _ => taskAssignment.status
          }
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

              // Determine the topic based on the service team name
              val serviceTeamTopic = serviceTeam.name.toLowerCase.replace(" ", "_") + "_topic"
              val eventManagerTopic = "event_manager_topic"

              notificationTypes.foreach { notificationType =>
                val message = baseMessage + ("message_type" -> Json.toJson(notificationType))

                // Send EventDayAlert to both topics
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
            Future.successful(
              BadRequest("Failed to fetch necessary details (task template, service team, or event plan).")
            )
        }
      }
    )
  }

  // 10) View Task Assignments for an Event Plan ID - GET
  def getTaskAssignments(eventPlanId: Long) = Action.async {
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

  private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

  // 12) Update Task Assignment Status or End Time - PATCH
  def updateTaskAssignment(taskAssignmentId: Long) = Action.async(parse.json) { request =>
    request.body.validate[TaskAssignment].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> "Invalid JSON format"))),
      taskAssignmentUpdate => {
        // Check whether `status` and `endTime` were provided
        val isStatusProvided = taskAssignmentUpdate.status != AssignmentStatus.UNKNOWN
        val isEndTimeProvided = taskAssignmentUpdate.endTime != LocalDateTime.MIN

        println(isEndTimeProvided)

        if (!isStatusProvided && !isEndTimeProvided) {
          Future.successful(BadRequest(Json.obj("error" -> "Either status or endTime must be provided")))
        } else {
          // Construct options based on whether the fields were provided
          val statusOpt = if (isStatusProvided) Some(taskAssignmentUpdate.status) else None
          val endTimeOpt = if (isEndTimeProvided) Some(taskAssignmentUpdate.endTime) else None

          // Call the repository to perform the update
          taskAssignmentRepository.updatePartial(taskAssignmentId, statusOpt, endTimeOpt).map {
            case Some(updatedTaskAssignment) => Ok(Json.toJson(updatedTaskAssignment)) // Return the updated task assignment
            case None => NotFound(Json.obj("error" -> "Task assignment not found"))
          }
        }
      }
    )
  }

  def getTaskAssignmentsByServiceTeam(serviceTeamId: Long) = Action.async {
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
    }.recover { case ex =>
      InternalServerError(s"An error occurred: ${ex.getMessage}")
    }
  }

  def getTaskAssignmentById(id: Long) = Action.async {
    taskAssignmentRepository.find(id).map {
      case Some((taskAssignment, taskTemplate, serviceTeam, eventPlan)) =>
        val response = Json.obj(
          "task_assignment" -> Json.toJson(taskAssignment),
          "task_template" -> Json.toJson(taskTemplate),
          "service_team" -> Json.toJson(serviceTeam),
          "event_plan" -> Json.toJson(eventPlan)
        )
        Ok(response)
      case None => NotFound("Task assignment not found")
    }
  }

}