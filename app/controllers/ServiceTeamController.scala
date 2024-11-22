package controllers

import models.{EventPlanRepository, Role, ServiceTeam, ServiceTeamRepository, TaskAssignmentRepository, TaskIssueRepository, TaskTemplateRepository}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.KafkaProducerService

import javax.inject.{Inject, Singleton}
import play.api.libs.typedmap.TypedKey
import security.RequestKeys

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ServiceTeamController @Inject()(
                                       eventPlanRepository: EventPlanRepository,
                                       taskTemplateRepository: TaskTemplateRepository,
                                       serviceTeamRepository: ServiceTeamRepository,
                                       taskAssignmentRepository: TaskAssignmentRepository,
                                       taskIssueRepository: TaskIssueRepository,
                                       cc: ControllerComponents,
                                       kafkaProducer: KafkaProducerService,
                                     )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val eventManagerRole = "EventManager"

  // Utility function to normalize team names (remove spaces)
  private def getRole(name: String): Role.Role = Role.withName(name.replaceAll("\\s", ""))

  // 3) Add Service Team - POST
  def addServiceTeam = Action.async(parse.json) { request =>
    val userRoles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    if (!userRoles.contains(Role.EventManager)) {
      Future.successful(Forbidden("Access denied"))
    } else {
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
  }

  // 9) View All Service Teams - GET
  def listServiceTeams = Action.async { request =>
    val userRoles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)
    println(userRoles)

    if (!userRoles.contains(Role.EventManager)) {
      Future.successful(Forbidden("Access denied"))
    } else {
      serviceTeamRepository.list().map { serviceTeams =>
        Ok(Json.toJson(serviceTeams))
      }
    }
  }

  // View a specific Service Team by ID - GET
  def getServiceTeam(id: Long) = Action.async { request =>
    val userRoles = request.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty)

    serviceTeamRepository.find(id).flatMap {
      case Some(serviceTeam) =>
        val role = getRole(serviceTeam.name)
        if (userRoles.contains(Role.EventManager) || userRoles.contains(role)) {
          Future.successful(Ok(Json.toJson(serviceTeam)))
        } else {
          Future.successful(Forbidden("Access denied"))
        }
      case None =>
        Future.successful(NotFound(Json.obj("error" -> s"Service team with ID $id not found")))
    }
  }
}
