package actors

import akka.actor.{Actor, Props}
import models.Reservation
import services.EmailService

object ReminderActor {
  def props(emailService: EmailService): Props = Props(new ReminderActor(emailService))
}

class ReminderActor(emailService: EmailService) extends Actor {
  override def receive: Receive = {
    case reservation: Reservation =>
      println(s"Sending reminder for reservation ID: ${reservation.reservationId}")
      emailService.sendReminder(reservation)
  }
}
