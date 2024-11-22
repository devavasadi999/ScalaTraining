package security

import models._
import org.apache.pekko.stream.Materializer
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.http.HttpFilters
import play.api.mvc._
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JwtAuthFilter @Inject()(
                               val mat: Materializer, // Explicit declaration
                               implicit val ec: ExecutionContext,
                               protected val dbConfigProvider: DatabaseConfigProvider
                             ) extends Filter with HasDatabaseConfigProvider[JdbcProfile]{

  override def apply(nextFilter: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {
    val publicRoutes = Seq("/login")

    if (publicRoutes.exists(request.path.startsWith)) {
      nextFilter(request) // Allow public routes
    } else {
      val tokenOpt = request.headers.get("Authorization").map(_.replace("Bearer ", ""))
      println(tokenOpt)
      JwtUtil.validateToken(tokenOpt.getOrElse("")) match {
        case Some(userId) =>
          val rolesQuery = TableQuery[AppUserRoleTable].filter(_.appUserId === userId.toLong).map(_.role).result
          db.run(rolesQuery).flatMap { roles =>
            println(roles)
            val updatedRequest = request.addAttr(RequestKeys.Roles, roles)
              .addAttr(RequestKeys.UserId, userId)
            println(updatedRequest.attrs.get(RequestKeys.Roles).getOrElse(Seq.empty))
            nextFilter(updatedRequest)
          }
        case None =>
          Future.successful(Results.Unauthorized("Invalid or missing token"))
      }
    }
  }
}

// Filters Registration
class Filters @Inject()(jwtAuthFilter: JwtAuthFilter) extends HttpFilters {
  override def filters: Seq[EssentialFilter] = Seq(jwtAuthFilter)
}
