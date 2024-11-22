package actors

import akka.actor.{Actor, ActorSystem, Props}

import scala.concurrent.duration.*

class SchedulerActor(system: ActorSystem) extends Actor {
  import system.dispatcher

  def receive: Receive = {
    case ("scheduleReminder", reservationId: Int) =>
      system.scheduler.scheduleOnce(15.minutes) {
        // Logic to send reminder email
        println(s"Sending reminder for reservation $reservationId")
      }
  }
}

object SchedulerActor {
  def props(system: ActorSystem): Props = Props(new SchedulerActor(system))
}
