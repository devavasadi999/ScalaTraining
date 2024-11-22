package services

import models.User
import repositories.UserRepository

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserService @Inject()(userRepository: UserRepository)(implicit ec: ExecutionContext) {

  def getUserById(userId: Long): Future[Option[User]] = {
    userRepository.findById(userId)
  }

  // Add a new user
  def addUser(user: User): Future[Long] = {
    userRepository.addUser(user)
  }

  // Get all users
  def getAllUsers: Future[List[User]] = {
    userRepository.getAllUsers
  }
}
