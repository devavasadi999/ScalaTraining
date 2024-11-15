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
        // Ensure the status is set to TODO if not provided
        val updatedTaskAssignment = taskAssignment.copy(
          status = taskAssignment.status match {
            case AssignmentStatus.UNKNOWN => AssignmentStatus.TODO
            case _ => taskAssignment.status
          }
        )

        // Fetch the service team email, task template, service team, and event plan
        val fetchDetails = for {
          emailOpt <- serviceTeamRepository.findEmailById(updatedTaskAssignment.serviceTeamId)
          taskTemplateOpt <- taskTemplateRepository.find(updatedTaskAssignment.taskTemplateId)
          serviceTeamOpt <- serviceTeamRepository.find(updatedTaskAssignment.serviceTeamId)
          eventPlanOpt <- eventPlanRepository.find(updatedTaskAssignment.eventPlanId)
        } yield (emailOpt, taskTemplateOpt, serviceTeamOpt, eventPlanOpt)

        fetchDetails.flatMap {
          case (Some(email), Some(taskTemplate), Some(serviceTeam), Some(eventPlan)) =>
            // Add the task assignment to the repository
            taskAssignmentRepository.add(updatedTaskAssignment).map { createdAssignment =>
              // Define your different message types
              val notificationTypes = Seq(
                "TaskAssignmentNotification",
                "PreparationReminders",
                "ProgressCheckIn",
                "EventDayAlert"
              )

              // Create the base message object
              val baseMessage = Json.obj(
                "to_emails" -> Json.arr(email),
                "task_assignment" -> Json.toJson(createdAssignment),
                "task_template" -> Json.toJson(taskTemplate),
                "service_team" -> Json.toJson(serviceTeam),
                "event_plan" -> Json.toJson(eventPlan)
              )

              // Iterate over each notification type and modify `messageType` before sending
              notificationTypes.foreach { notificationType =>
                val message = baseMessage + ("message_type" -> Json.toJson(notificationType))

                // Send the modified message to Kafka
                kafkaProducer.send("rawNotification", message.toString)
              }

              Created(Json.toJson(createdAssignment))
            }

          case _ =>
            Future.successful(
              BadRequest("Failed to fetch necessary details (email, task template, service team, or event plan).")
            )
        }
      }
    )
  }

  // 10) View Task Assignments for an Event Plan ID - GET
  def getTaskAssignments(eventPlanId: Long) = Action.async {
    taskAssignmentRepository.findByEventPlan(eventPlanId).map { taskAssignments =>
      Ok(Json.toJson(taskAssignments))
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
    taskAssignmentRepository.findByServiceTeamId(serviceTeamId).map { taskAssignments =>
      Ok(Json.toJson(taskAssignments)) // Serializes the result to JSON
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