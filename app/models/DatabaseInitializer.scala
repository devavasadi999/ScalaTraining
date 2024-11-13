package models

import javax.inject.Inject
import play.api.inject.ApplicationLifecycle
import play.api.{Environment, Logging}
import slick.jdbc.JdbcProfile
import play.api.db.slick.DatabaseConfigProvider

import scala.concurrent.{ExecutionContext, Future}

class DatabaseInitializer @Inject()(
                                     dbConfigProvider: DatabaseConfigProvider,
                                     lifecycle: ApplicationLifecycle
                                   )(implicit ec: ExecutionContext) extends Logging {

  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  // Import the table queries from each model
  private val equipmentTypes = TableQuery[EquipmentTypeTable]
  private val equipments = TableQuery[EquipmentTable]
  private val employees = TableQuery[EmployeeTable]
  private val equipmentAllocations = TableQuery[EquipmentAllocationTable]
  private val equipmentRepairs = TableQuery[EquipmentRepairTable]

  // Combine all table schemas
  private val schemas = equipmentTypes.schema ++ equipments.schema ++ employees.schema ++ equipmentAllocations.schema ++ equipmentRepairs.schema

  // Run the schema creation on application startup
  lifecycle.addStopHook { () =>
    Future.successful(db.close()) // Close the database connection when the app stops
  }

  def initialize(): Future[Unit] = {
    db.run(schemas.createIfNotExists).map(_ => println("Database tables created successfully"))
      .recover {
        case ex: Exception =>
          println("Error creating tables", ex)
      }
  }
}
