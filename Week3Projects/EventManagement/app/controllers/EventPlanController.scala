package controllers

import javax.inject._
import models._
import play.api.libs.json._
import play.api.mvc._
import services.KafkaProducerService

import scala.concurrent.{ExecutionContext, Future}
import models.{EventPlan, EventType}
import play.api.libs.typedmap.TypedKey
import security.RequestKeys

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

  private val eventManagerRole = "EventManager"

  // 1) Create Event Plan - POST
  def createEventPlan = Action.async(parse.json) { request =>
    val userRoles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)
    if (!userRoles.contains(Role.EventManager)) {
      Future.successful(Forbidden("Access denied"))
    } else {
      request.body.validate[EventPlan].fold(
        errors => Future.successful(BadRequest("Invalid JSON format or missing fields")),
        eventPlan => {
          eventPlanRepository.add(eventPlan).map { createdEventPlan =>
            Created(Json.toJson(createdEventPlan))
          }
        }
      )
    }
  }

  // 6) View All Event Plans - GET
  def listEventPlans = Action.async { request =>
    val userRoles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)
    if (!userRoles.contains(Role.EventManager)) {
      Future.successful(Forbidden("Access denied"))
    } else {
      eventPlanRepository.list().map { eventPlans =>
        Ok(Json.toJson(eventPlans))
      }
    }
  }

  // 7) View Specific Event Plan by ID - GET
  def getEventPlanWithAssignments(eventPlanId: Long): Action[AnyContent] = Action.async { request =>
    val userRoles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)
    if (!userRoles.contains(Role.EventManager)) {
      Future.successful(Forbidden("Access denied"))
    } else {
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
}
