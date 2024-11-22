package services

import models.Reservation
import repositories.{ReservationRepository, RoomRepository}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReservationService @Inject()(reservationRepository: ReservationRepository, roomRepository: RoomRepository)(implicit ec: ExecutionContext) {

  // Create a new reservation with availability check
  def reserveRoom(reservation: Reservation): Future[Option[Reservation]] = {
    // First, check if the room is available
    roomRepository.checkRoomAvailability(reservation.roomId, reservation.startTime, reservation.endTime).flatMap { available =>
      if (available) {
        reservationRepository.createReservation(reservation).map { reservationId =>
          Some(reservation.copy(reservationId = Some(reservationId)))
        }
      } else {
        Future.successful(None) // Return None if the room is not available
      }
    }
  }
}
