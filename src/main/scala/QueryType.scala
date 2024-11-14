import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

// Define Query Types and Messages
sealed trait QueryType
case object BalanceQuery extends QueryType
case object InternetQuery extends QueryType
case object SMSQuery extends QueryType

// Messages for initiating and responding to queries
case class Query(queryType: QueryType, subscriber: ActorRef)
case class Response(message: String)

// Subscriber Actor
class Subscriber(ivrSystem: ActorRef) extends Actor {
  def receive: Receive = {
    case queryType: QueryType =>
      println(s"Subscriber: Initiating a query for $queryType")
      ivrSystem ! Query(queryType, self) // Send query to IVRSystem

    case Response(message) =>
      println(s"Subscriber: Received response - $message")
  }
}

// IVRSystem Actor
class IVRSystem(balanceActor: ActorRef, internetActor: ActorRef, smsActor: ActorRef) extends Actor {
  def receive: Receive = {
    case Query(BalanceQuery, subscriber) =>
      println(s"IVRSystem: Routing Balance Query to BalanceActor")
      balanceActor ! Query(BalanceQuery, subscriber)

    case Query(InternetQuery, subscriber) =>
      println(s"IVRSystem: Routing Internet Query to InternetActor")
      internetActor ! Query(InternetQuery, subscriber)

    case Query(SMSQuery, subscriber) =>
      println(s"IVRSystem: Routing SMS Query to SMSActor")
      smsActor ! Query(SMSQuery, subscriber)
  }
}

// BalanceActor
class BalanceActor extends Actor {
  def receive: Receive = {
    case Query(BalanceQuery, subscriber) =>
      println(s"BalanceActor: Processing balance query")
      subscriber ! Response("Your balance is $10")
  }
}

// InternetActor
class InternetActor extends Actor {
  def receive: Receive = {
    case Query(InternetQuery, subscriber) =>
      println(s"InternetActor: Processing internet query")
      subscriber ! Response("Your remaining data is 2GB")
  }
}

// SMSActor
class SMSActor extends Actor {
  def receive: Receive = {
    case Query(SMSQuery, subscriber) =>
      println(s"SMSActor: Processing SMS query")
      subscriber ! Response("You have 100 SMS remaining")
  }
}

// Main Application
object IVRSystemApp {
  def main(args: Array[String]): Unit = {
    // Create the actor system
    val system = ActorSystem("IVRSystem")

    // Create the query handlers
    val balanceActor = system.actorOf(Props[BalanceActor], "balanceActor")
    val internetActor = system.actorOf(Props[InternetActor], "internetActor")
    val smsActor = system.actorOf(Props[SMSActor], "smsActor")

    // Create the IVR system with references to each query handler
    val ivrSystem = system.actorOf(Props(new IVRSystem(balanceActor, internetActor, smsActor)), "ivrSystem")

    // Create the subscriber actor with a reference to the IVR system
    val subscriber = system.actorOf(Props(new Subscriber(ivrSystem)), "subscriber")

    // Subscriber initiates different queries
    subscriber ! BalanceQuery
    subscriber ! InternetQuery
    subscriber ! SMSQuery

    // Shutdown the system after a delay to allow processing
    system.scheduler.scheduleOnce(5.seconds)(system.terminate())
  }
}
