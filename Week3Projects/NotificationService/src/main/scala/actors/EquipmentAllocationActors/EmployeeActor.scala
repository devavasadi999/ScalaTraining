package actors.EquipmentAllocationActors

import akka.actor.Actor
import services.EmailBuilderService

class EmployeeActor(notificationActor: akka.actor.ActorRef) extends Actor {
  override def receive: Receive = {
    case messageJson: String =>
      val email = EmailBuilderService.buildEmail(messageJson)
      notificationActor ! email

    case _ => println("Unexpected message format")
  }
}