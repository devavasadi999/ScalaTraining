package controllers

import models.{EventPlanRepository, Role, ServiceTeamRepository, TaskAssignmentRepository, TaskIssueRepository, TaskTemplate, TaskTemplateRepository}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import play.api.libs.typedmap.TypedKey
import security.RequestKeys
import services.KafkaProducerService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaskTemplateController @Inject()(
                                        eventPlanRepository: EventPlanRepository,
                                        taskTemplateRepository: TaskTemplateRepository,
                                        serviceTeamRepository: ServiceTeamRepository,
                                        taskAssignmentRepository: TaskAssignmentRepository,
                                        taskIssueRepository: TaskIssueRepository,
                                        cc: ControllerComponents,
                                        kafkaProducer: KafkaProducerService,
                                      )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val eventManagerRole = "EventManager"

  private def userHasAccessToServiceTeam(serviceTeamId: Long, roles: Seq[Role.Role]): Future[Boolean] = {
    serviceTeamRepository.find(serviceTeamId).map {
      case Some(serviceTeam) =>
        val normalizedTeamName = serviceTeam.name.replaceAll("\\s", "")
        roles.contains(Role.withName(normalizedTeamName))
      case None => false
    }
  }

  // 2) Add Task Template - POST
  def addTaskTemplate = Action.async(parse.json) { request =>
    val roles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    if (!roles.contains(Role.EventManager)) {
      Future.successful(Forbidden("Access denied"))
    } else {
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
  }

  // 8) View Task Templates for a Service Team - GET
  def getTaskTemplates(serviceTeamId: Long) = Action.async { request =>
    val roles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    if (!roles.contains(Role.EventManager)) {
      Future.successful(Forbidden("Access denied"))
    } else {
      taskTemplateRepository.findByServiceTeam(serviceTeamId).map { taskTemplates =>
        Ok(Json.toJson(taskTemplates))
      }
    }
  }
}
