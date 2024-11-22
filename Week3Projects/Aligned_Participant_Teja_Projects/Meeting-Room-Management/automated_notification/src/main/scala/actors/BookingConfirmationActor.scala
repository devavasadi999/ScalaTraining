package actors

import akka.actor.{Actor, Props}
import models.Reservation
import services.EmailService

class BookingConfirmationActor(emailService: EmailService) extends Actor {
  def receive: Receive = {
    case reservation: Reservation =>
      emailService.sendBookingConfirmation(reservation)
  }
}

object BookingConfirmationActor {
  def props(emailService: EmailService): Props = Props(new BookingConfirmationActor(emailService))
}
