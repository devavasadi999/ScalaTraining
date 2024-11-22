package controllers

import models.Employee
import play.api.mvc._
import services.EmployeeService
import play.api.libs.json._
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmployeeController @Inject()(
                                   val cc: ControllerComponents,
                                   EmployeeService: EmployeeService
                                 )(implicit ec: ExecutionContext) extends AbstractController(cc) {


  def addEmployee(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Employee] match {
      case JsSuccess(visitor, _) =>
        EmployeeService.create(visitor).map { created =>
          Created(Json.toJson(created))
        }
      case JsError(errors) =>
        Future.successful(BadRequest(Json.obj(
          "message" -> "Invalid visitor data",
          "errors" -> JsError.toJson(errors))))
    }
  }

  def list(): Action[AnyContent] = Action.async{
    EmployeeService.list().map(employees => Ok(Json.toJson(employees)))
  }

  def getEmployeeDetails(EmployeeId: Long): Action[AnyContent] = Action.async{
    EmployeeService.get(EmployeeId).map {
      case Some(employee) => Ok(Json.toJson(employee))
      case None => NotFound(Json.obj("message" -> s"Employee with id $EmployeeId not found"))
    }
  }
}
