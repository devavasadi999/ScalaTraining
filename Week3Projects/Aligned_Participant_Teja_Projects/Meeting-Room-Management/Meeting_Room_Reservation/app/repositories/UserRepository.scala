package repositories

import models.User
import models.db.UserTable
import play.api.db.slick._
import slick.jdbc.JdbcProfile
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject
import play.api.db.slick._

@Singleton
class UserRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig._
  import profile.api._

  private val users = TableQuery[UserTable]

  def findById(userId: Long): Future[Option[User]] = {
    db.run(users.filter(_.employeeId === userId).result.headOption)
  }

  // Add a new user
  def addUser(user: User): Future[Long] = {
    val insertQuery = users.returning(users.map(_.employeeId)) += user
    db.run(insertQuery).map(_.getOrElse(throw new Exception("Failed to add user")))
  }

  // Fetch all users
  def getAllUsers: Future[List[User]] = {
    db.run(users.result).map(_.toList)
  }
}
