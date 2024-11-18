package controllers

import models.{EventPlanRepository, ServiceTeam, ServiceTeamRepository, TaskAssignmentRepository, TaskIssueRepository, TaskTemplateRepository}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
import services.KafkaProducerService

import javax.inject.{Inject, Singleton}
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

  // 9) View All Service Teams - GET
  def listServiceTeams = Action.async {
    serviceTeamRepository.list().map { serviceTeams =>
      Ok(Json.toJson(serviceTeams))
    }
  }

  def getServiceTeam(id: Long) = Action.async {
    serviceTeamRepository.find(id).map {
      case Some(serviceTeam) => Ok(Json.toJson(serviceTeam))
      case None              => NotFound(Json.obj("error" -> s"Service team with ID $id not found"))
    }
  }
}