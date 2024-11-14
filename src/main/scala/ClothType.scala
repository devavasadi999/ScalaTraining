import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

// Message Types
sealed trait ClothType
case object UnwashedCloth extends ClothType
case object WashedCloth extends ClothType
case object TornCloth extends ClothType

// Messages for Processing
case class ProcessCloth(clothType: ClothType, customer: ActorRef)
case class CleanedCloth(clothType: ClothType, customer: ActorRef)
case class PressedCloth(clothType: ClothType, customer: ActorRef)
case class RepairedCloth(clothType: ClothType, customer: ActorRef)
case class ClothReady(clothType: ClothType)

// Customer Actor
class Customer(washingMachine: ActorRef, pressingMachine: ActorRef, stitchingMachine: ActorRef) extends Actor {
  def receive: Receive = {
    case clothType: ClothType =>
      println(s"Customer: Received $clothType cloth for processing")
      clothType match {
        case UnwashedCloth =>
          washingMachine ! ProcessCloth(UnwashedCloth, self)
        case WashedCloth =>
          pressingMachine ! ProcessCloth(WashedCloth, self)
        case TornCloth =>
          stitchingMachine ! ProcessCloth(TornCloth, self)
      }

    case ClothReady(clothType) =>
      println(s"Customer: Received processed $clothType cloth back")
  }
}

// WashingMachine Actor
class WashingMachine(pressingMachine: ActorRef) extends Actor {
  def receive: Receive = {
    case ProcessCloth(UnwashedCloth, customer) =>
      println(s"WashingMachine: Washing unwashed cloth")
      pressingMachine ! CleanedCloth(UnwashedCloth, customer)

    case RepairedCloth(TornCloth, customer) =>
      println(s"WashingMachine: Washing repaired cloth")
      pressingMachine ! CleanedCloth(TornCloth, customer)
  }
}

// PressingMachine Actor
class PressingMachine extends Actor {
  def receive: Receive = {
    case CleanedCloth(clothType, customer) =>
      println(s"PressingMachine: Pressing cleaned $clothType cloth")
      customer ! ClothReady(clothType)

    case ProcessCloth(WashedCloth, customer) =>
      println(s"PressingMachine: Pressing washed cloth")
      customer ! ClothReady(WashedCloth)
  }
}

// StitchingMachine Actor
class StitchingMachine(washingMachine: ActorRef) extends Actor {
  def receive: Receive = {
    case ProcessCloth(TornCloth, customer) =>
      println(s"StitchingMachine: Repairing torn cloth")
      washingMachine ! RepairedCloth(TornCloth, customer)
  }
}

// Main Application
object LaundryProcessingApp {
  def main(args: Array[String]): Unit = {
    // Create the actor system
    val system = ActorSystem("LaundryProcessingSystem")

    // Create the PressingMachine actor
    val pressingMachine = system.actorOf(Props[PressingMachine], "pressingMachine")

    // Create the WashingMachine actor, with a reference to the PressingMachine
    val washingMachine = system.actorOf(Props(new WashingMachine(pressingMachine)), "washingMachine")

    // Create the StitchingMachine actor, with a reference to the WashingMachine
    val stitchingMachine = system.actorOf(Props(new StitchingMachine(washingMachine)), "stitchingMachine")

    // Create the Customer actor, with references to all machines
    val customer = system.actorOf(Props(new Customer(washingMachine, pressingMachine, stitchingMachine)), "customer")

    // Start the process by sending different types of cloth
    customer ! UnwashedCloth

    customer ! WashedCloth

    customer ! TornCloth

    // Shutdown the system after a delay to allow processing
    system.scheduler.scheduleOnce(5.seconds)(system.terminate())
  }
}
