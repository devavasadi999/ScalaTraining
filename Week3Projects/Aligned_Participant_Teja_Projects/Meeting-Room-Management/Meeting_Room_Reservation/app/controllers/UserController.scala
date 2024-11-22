package controllers

import models.User
import play.api.libs.json._
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.UserService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()(cc: ControllerComponents,
                               userService: UserService
                              )(implicit ec: ExecutionContext) extends AbstractController(cc) {
  // Endpoint to add a new user
  def addEmployee(): Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[User].fold(
      errors => Future.successful(BadRequest(Json.obj("error" -> "Invalid user data"))),
      user => {
        userService.addUser(user).map { userId =>
          Created(Json.obj("userId" -> userId, "message" -> "User successfully added"))
        }
      }
    )
  }

  // Endpoint to fetch a user by its ID
  def getUserById(userId: Long): Action[AnyContent] = Action.async {
    userService.getUserById(userId).map {
      case Some(user) => Ok(Json.toJson(user))
      case None => NotFound(Json.obj("error" -> "User not found"))
    }
  }

  // Endpoint to fetch all users
  def getAllUsers: Action[AnyContent] = Action.async {
    userService.getAllUsers.map { users =>
      Ok(Json.toJson(users))
    }
  }

}
