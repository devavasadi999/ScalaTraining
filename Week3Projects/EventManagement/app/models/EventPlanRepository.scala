package models

import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EventPlanRepository @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  val eventPlans = TableQuery[EventPlanTable]
  private val taskAssignments = TableQuery[TaskAssignmentTable]

  def list(): Future[Seq[EventPlan]] = db.run(eventPlans.result)

  def find(id: Long): Future[Option[EventPlan]] = db.run(eventPlans.filter(_.id === id).result.headOption)

  def add(eventPlan: EventPlan): Future[EventPlan] = {
    val action = (eventPlans returning eventPlans.map(_.id)
      into ((plan, id) => plan.copy(id = id))
      ) += eventPlan

    db.run(action)
  }

  def update(eventPlan: EventPlan): Future[Int] = db.run(eventPlans.filter(_.id === eventPlan.id).update(eventPlan))

  def delete(id: Long): Future[Int] = db.run(eventPlans.filter(_.id === id).delete)

  // Fetch an EventPlan along with its TaskAssignments
  def findEventPlanWithAssignments(eventPlanId: Long): Future[Option[(EventPlan, Seq[TaskAssignment])]] = {
    val query = for {
      (eventPlan, taskAssignment) <- eventPlans
        .filter(_.id === eventPlanId) // Filter by the event plan ID
        .joinLeft(taskAssignments)
        .on(_.id === _.eventPlanId) // Left join on eventPlanId
    } yield (eventPlan, taskAssignment)

    db.run(query.result).map { results =>
      if (results.isEmpty) None
      else {
        val (eventPlan, _) = results.head
        val taskAssignments = results.flatMap(_._2) // Extract task assignments, removing `None` values
        Some((eventPlan, taskAssignments))
      }
    }
  }
}