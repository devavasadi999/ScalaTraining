package actors.EventManagementActors

import akka.actor.Actor
import services.{Email, EmailBuilderService}

class EventManagerActor(notificationActor: akka.actor.ActorRef) extends Actor {
  override def receive: Receive = {
    case messageJson: String =>
      val email = EmailBuilderService.buildEmail(messageJson)
      val eventManagerEmail = System.getProperty("EVENT_MANAGER_EMAIL")
      notificationActor ! Email(Seq(eventManagerEmail), email.subject, email.body, email.frequencyType, email.frequencyValues)

    case _ => println("Unexpected message format")
  }
}