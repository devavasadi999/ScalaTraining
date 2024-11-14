import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

// Messages to start an activity and signal completion
case class StartActivity(activityName: String, timeLimit: FiniteDuration)
case class ActivityFinished(activityName: String)

// ActivityActor definition
class ActivityActor extends Actor {

  var original:ActorRef = self
  override def receive: Receive = {
    case StartActivity(activityName, timeLimit) =>
      println(s"Starting activity: $activityName with a time limit of $timeLimit")
      context.system.scheduler.scheduleOnce(timeLimit, self, ActivityFinished(activityName))
      original=sender()
    case ActivityFinished(activityName) =>
      println(s"Activity finished: $activityName")
      original ! s"$activityName completed."

    case _ => println("Default case")// Respond to sender when finished
  }
}

object MainApp {
  def main(args: Array[String]): Unit = {
    // Set up the actor system and create the ActivityActor
    val system = ActorSystem("ActivitySystem")
    val activityActor = system.actorOf(Props[ActivityActor], "ActivityActor")

    // Set a timeout for the ask pattern
    implicit val timeout: Timeout = Timeout(10.seconds)  // Should be longer than any activity's time limit

    // Start an activity with a 3-second time limit and wait for the result
    val future = activityActor ? StartActivity("ExampleActivity", 3.seconds)

    // Await and print the result
    val result = Await.result(future, timeout.duration)
    println(s"Received response: $result")

    // Shutdown the system
    system.terminate()
  }
}
