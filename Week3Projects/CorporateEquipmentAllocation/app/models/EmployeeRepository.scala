package models

import javax.inject.{Inject, Singleton}
import models.Employee
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmployeeRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private val employees = TableQuery[EmployeeTable]

  // Method to find employee's email by their ID
  def findEmailById(employeeId: Long): Future[Option[String]] = {
    db.run(employees.filter(_.id === employeeId).map(_.email).result.headOption)
  }

  def find(id: Long): Future[Option[Employee]] = db.run(employees.filter(_.id === id).result.headOption)
}
