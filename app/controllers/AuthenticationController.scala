package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}
import models._
import org.mindrot.jbcrypt.BCrypt
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import security.JwtUtil
import slick.jdbc.JdbcProfile

@Singleton
class AuthenticationController @Inject()(
                                          cc: ControllerComponents,
                                          protected val dbConfigProvider: DatabaseConfigProvider
                                        )(implicit ec: ExecutionContext)
  extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] {

  // Login API
  def login() = Action.async(parse.json) { implicit request =>
    val usernameOpt = (request.body \ "username").asOpt[String]
    val passwordOpt = (request.body \ "password").asOpt[String]

    (usernameOpt, passwordOpt) match {
      case (Some(username), Some(password)) =>
        val query = TableQuery[AppUserTable].filter(_.username === username).result.headOption
        db.run(query).flatMap {
          case Some(user) if BCrypt.checkpw(password, user.password) =>
            val rolesQuery = TableQuery[AppUserRoleTable].filter(_.appUserId === user.id).map(_.role).result
            db.run(rolesQuery).map { roles =>
              val token = JwtUtil.generateToken(user.id.toString)
              Ok(Json.obj("token" -> token, "roles" -> roles))
            }
          case _ =>
            Future.successful(Unauthorized(Json.obj("error" -> "Invalid username or password")))
        }
      case _ =>
        Future.successful(BadRequest(Json.obj("error" -> "Invalid request format")))
    }
  }

  // Logout API
  def logout() = Action {
    Ok("Logged out").discardingCookies(DiscardingCookie("Authorization"))
  }

  // Helper to fetch roles of a user by user ID
  def getUserRoles(userId: Long): Future[Seq[Role.Role]] = {
    val rolesQuery = TableQuery[AppUserRoleTable].filter(_.appUserId === userId).map(_.role).result
    db.run(rolesQuery)
  }
}