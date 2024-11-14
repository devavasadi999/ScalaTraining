package controllers

import javax.inject._
import models._
import play.api.libs.json._
import play.api.mvc._
import services.KafkaProducerService

import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Singleton
class EventPlanController @Inject()(
                                     eventPlanRepository: EventPlanRepository,
                                     taskTemplateRepository: TaskTemplateRepository,
                                     serviceTeamRepository: ServiceTeamRepository,
                                     taskAssignmentRepository: TaskAssignmentRepository,
                                     taskIssueRepository: TaskIssueRepository,
                                     cc: ControllerComponents,
                                     kafkaProducer: KafkaProducerService,
                                   )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  implicit val eventPlanFormat: Format[EventPlan] = Json.format[EventPlan]
  implicit val taskTemplateFormat: Format[TaskTemplate] = Json.format[TaskTemplate]
  implicit val serviceTeamFormat: Format[ServiceTeam] = Json.format[ServiceTeam]
  implicit val taskAssignmentFormat: Format[TaskAssignment] = Json.format[TaskAssignment]
  implicit val taskIssueFormat: Format[TaskIssue] = Json.format[TaskIssue]
  implicit val eventTypeReads: Reads[EventType.EventType] = Reads.enumNameReads(EventType)
  implicit val assignmentStatusReads: Reads[AssignmentStatus.AssignmentStatus] = Reads.enumNameReads(AssignmentStatus)
  implicit val issueStatusReads: Reads[IssueStatus.IssueStatus] = Reads.enumNameReads(IssueStatus)

  private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

  // 1) Create Event Plan - POST
  def createEventPlan = Action.async(parse.json) { request =>
    val nameOpt = (request.body \ "name").asOpt[String]
    val descriptionOpt = (request.body \ "description").asOpt[String]
    val eventTypeOpt = (request.body \ "eventType").asOpt[EventType.EventType](eventTypeReads)

    (nameOpt, descriptionOpt, eventTypeOpt) match {
      case (Some(name), Some(description), Some(eventType)) =>
        eventPlanRepository.add(EventPlan(0, name, description, eventType)).map { createdEventPlan =>
          Created(Json.toJson(createdEventPlan))
        }
      case _ =>
        Future.successful(BadRequest("Invalid JSON format or missing fields"))
    }
  }

  // 2) Add Task Template - POST
  def addTaskTemplate = Action.async(parse.json) { request =>
    val nameOpt = (request.body \ "name").asOpt[String]
    val serviceTeamIdOpt = (request.body \ "serviceTeamId").asOpt[Long]
    val descriptionOpt = (request.body \ "description").asOpt[String]

    (nameOpt, serviceTeamIdOpt, descriptionOpt) match {
      case (Some(name), Some(serviceTeamId), Some(description)) =>
        taskTemplateRepository.add(TaskTemplate(0, serviceTeamId, name, description)).map { createdTaskTemplate =>
          Created(Json.toJson(createdTaskTemplate))
        }
      case _ =>
        Future.successful(BadRequest("Invalid JSON format or missing fields"))
    }
  }

  // 3) Add Service Team - POST
  def addServiceTeam = Action.async(parse.json) { request =>
    val nameOpt = (request.body \ "name").asOpt[String]
    val descriptionOpt = (request.body \ "description").asOpt[String]
    val emailOpt = (request.body \ "email").asOpt[String]

    (nameOpt, descriptionOpt, emailOpt) match {
      case (Some(name), Some(description), Some(email)) =>
        serviceTeamRepository.add(ServiceTeam(0, name, description, email)).map { createdServiceTeam =>
          Created(Json.toJson(createdServiceTeam))
        }
      case _ =>
        Future.successful(BadRequest("Invalid JSON format or missing fields"))
    }
  }

  // 4) Add Task Assignment - POST
  def addTaskAssignment = Action.async(parse.json) { request =>
    request.body.validate[TaskAssignment].fold(
      errors => {
        println(errors)
        Future.successful(BadRequest("Invalid JSON provided"))
      },
      taskAssignment => {
        // Retrieve the email for the service team based on serviceTeamId
        serviceTeamRepository.findEmailById(taskAssignment.serviceTeamId).flatMap {
          case Some(email) =>
            // Add task assignment to the repository
            taskAssignmentRepository.add(taskAssignment).map { createdAssignment =>
              // Define your different message types
              val notificationTypes = Seq(
                "TaskAssignmentNotification",
                "PreparationReminders",
                "ProgressCheckIn",
                "EventDayAlert"
              )

              // Create the base message object
              var message = Json.obj(
                "messageType" -> "",  // Placeholder for message type
                "toEmails" -> Json.arr(email),
                "taskAssignment" -> Json.toJson(createdAssignment),
                "taskIssue" -> JsNull
              )

              // Iterate over each notification type and modify `messageType` before sending
              notificationTypes.foreach { notificationType =>
                // Update the `messageType` field
                message = message.as[JsObject] + ("messageType" -> Json.toJson(notificationType))

                // Send the modified message to Kafka
                kafkaProducer.send("rawNotification", message.toString)
              }

              Created(Json.toJson(createdAssignment))
            }
          case None =>
            // If the service team email is not found, return an error response
            Future.successful(BadRequest("Service team email not found"))
        }
      }
    )
  }

  // 5) Add Task Issue - POST
  def addTaskIssue = Action.async(parse.json) { request =>
    request.body.validate[TaskIssue].fold(
      errors => Future.successful(BadRequest("Invalid JSON provided")),
      taskIssue => {
        taskIssueRepository.add(taskIssue).map { createdIssue =>
          val message = Json.obj(
            "messageType" -> "IssueAlert",
            "toEmails" -> Json.arr("event.manager@example.com"),
            "taskAssignment" -> JsNull,
            "taskIssue" -> Json.toJson(createdIssue)
          )
          kafkaProducer.send("rawNotification", message.toString)
          Created(Json.toJson(createdIssue))
        }
      }
    )
  }

  // 6) View All Event Plans - GET
  def listEventPlans = Action.async {
    eventPlanRepository.list().map { eventPlans =>
      Ok(Json.toJson(eventPlans))
    }
  }

  // 7) View Specific Event Plan by ID - GET
  def getEventPlan(id: Long) = Action.async {
    eventPlanRepository.find(id).map {
      case Some(eventPlan) => Ok(Json.toJson(eventPlan))
      case None => NotFound
    }
  }

  // 8) View Task Templates for a Service Team - GET
  def getTaskTemplates(serviceTeamId: Long) = Action.async {
    taskTemplateRepository.findByServiceTeam(serviceTeamId).map { taskTemplates =>
      Ok(Json.toJson(taskTemplates))
    }
  }

  // 9) View All Service Teams - GET
  def listServiceTeams = Action.async {
    serviceTeamRepository.list().map { serviceTeams =>
      Ok(Json.toJson(serviceTeams))
    }
  }

  // 10) View Task Assignments for an Event Plan ID - GET
  def getTaskAssignments(eventPlanId: Long) = Action.async {
    taskAssignmentRepository.findByEventPlan(eventPlanId).map { taskAssignments =>
      Ok(Json.toJson(taskAssignments))
    }
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

  // 12) Update Task Assignment Status or End Time - PATCH
  def updateTaskAssignment(taskAssignmentId: Long) = Action.async(parse.json) { request =>
    val statusOpt = (request.body \ "status").asOpt[AssignmentStatus.AssignmentStatus]
    val endTimeOpt = (request.body \ "endTime").asOpt[String].map(LocalDateTime.parse(_, dateTimeFormatter))

    if (statusOpt.isEmpty && endTimeOpt.isEmpty) {
      Future.successful(BadRequest("Either status or endTime must be provided"))
    } else {
      taskAssignmentRepository.update(taskAssignmentId, statusOpt, endTimeOpt).map {
        case 1 => Ok("Task assignment updated")
        case _ => NotFound("Task assignment not found")
      }
    }
  }

  // 13) View All Task Issues - GET
  def listTaskIssues = Action.async {
    taskIssueRepository.list().map { taskIssues =>
      Ok(Json.toJson(taskIssues))
    }
  }
}
