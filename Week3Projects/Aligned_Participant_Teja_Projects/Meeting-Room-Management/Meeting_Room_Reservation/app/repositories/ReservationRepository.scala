package repositories

import models.{Reservation, Room}
import models.db.ReservationTable
import models.db.RoomTable
import play.api.db.slick._
import slick.jdbc.JdbcProfile

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReservationRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val reservations = TableQuery[ReservationTable]

  private val rooms = TableQuery[RoomTable]

  def createReservation(reservation: Reservation): Future[Long] = {
    val insertQuery = reservations
      .returning(reservations.map(_.reservationId)) += reservation

    db.run(insertQuery).map {
      case Some(reservationId) => reservationId  // Extract reservationId from Option
      case None => throw new Exception("Failed to create reservation")
    }
  }

}
