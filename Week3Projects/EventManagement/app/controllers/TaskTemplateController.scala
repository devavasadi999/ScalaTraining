package controllers

import models.{EventPlanRepository, ServiceTeamRepository, TaskAssignmentRepository, TaskIssueRepository, TaskTemplate, TaskTemplateRepository}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, ControllerComponents}
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

  // 8) View Task Templates for a Service Team - GET
  def getTaskTemplates(serviceTeamId: Long) = Action.async {
    taskTemplateRepository.findByServiceTeam(serviceTeamId).map { taskTemplates =>
      Ok(Json.toJson(taskTemplates))
    }
  }
}