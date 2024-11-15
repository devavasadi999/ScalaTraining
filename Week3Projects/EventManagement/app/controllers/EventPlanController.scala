package controllers

import javax.inject._
import models._
import play.api.libs.json._
import play.api.mvc._
import services.KafkaProducerService

import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import models.{EventPlan, EventType}

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

  //implicit val eventPlanFormat: Format[EventPlan] = Json.format[EventPlan]
//  implicit val taskTemplateFormat: Format[TaskTemplate] = Json.format[TaskTemplate]
//  implicit val serviceTeamFormat: Format[ServiceTeam] = Json.format[ServiceTeam]
  //implicit val taskAssignmentFormat: Format[TaskAssignment] = Json.format[TaskAssignment]
//  implicit val taskIssueFormat: Format[TaskIssue] = Json.format[TaskIssue]
//  implicit val eventTypeReads: Reads[EventType.EventType] = Reads.enumNameReads(EventType)
//  implicit val assignmentStatusReads: Reads[AssignmentStatus.AssignmentStatus] = Reads.enumNameReads(AssignmentStatus)
//  implicit val issueStatusReads: Reads[IssueStatus.IssueStatus] = Reads.enumNameReads(IssueStatus)

  // 1) Create Event Plan - POST
  def createEventPlan = Action.async(parse.json) { request =>
    // Use the EventPlan Reads from its companion object
    request.body.validate[EventPlan].fold(
      errors => Future.successful(BadRequest("Invalid JSON format or missing fields")),
      eventPlan => {
        eventPlanRepository.add(eventPlan).map { createdEventPlan =>
          Created(Json.toJson(createdEventPlan)) // Serialize response using the EventPlan Writes
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
  def getEventPlanWithAssignments(eventPlanId: Long): Action[AnyContent] = Action.async {
    eventPlanRepository.findEventPlanWithAssignments(eventPlanId).map {
      case Some((eventPlan, taskAssignments)) =>
        Ok(Json.obj(
          "eventPlan" -> Json.toJson(eventPlan),
          "task_assignments" -> Json.toJson(taskAssignments)
        ))
      case None => NotFound("EventPlan not found")
    }
  }
}
