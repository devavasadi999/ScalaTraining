package actors

import akka.actor.{Actor, Props}
import models.Reservation
import services.EmailService

class RoomPreparationActor(emailService: EmailService) extends Actor {
  def receive: Receive = {
    case reservation: Reservation =>
      emailService.sendRoomPreparationNotification(reservation)
  }
}

object RoomPreparationActor {
  def props(emailService: EmailService): Props = Props(new RoomPreparationActor(emailService))
}
