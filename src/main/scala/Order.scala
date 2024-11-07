import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

// Define Messages
case class Order(product: String, customer: ActorRef)         // Order from Customer to SalesPerson
case class ApprovedOrder(product: String, customer: ActorRef) // Approved order from SalesPerson to Dispatcher
case class Dispatch(product: String)                          // Final dispatch notification to Customer

// Customer Actor
class Consumer(salesPerson: ActorRef) extends Actor {
  def receive: Receive = {
    case "placeOrder" =>
      println(s"Customer: Placing order for 'Laptop'")
      salesPerson ! Order("Laptop", self) // Send order to SalesPerson

    case Dispatch(product) =>
      println(s"Customer: Received dispatched product '$product'")
  }
}

// SalesPerson Actor
class SalesPerson(dispatcher: ActorRef) extends Actor {
  def receive: Receive = {
    case Order(product, customer) =>
      println(s"SalesPerson: Approving order for '$product'")
      dispatcher ! ApprovedOrder(product, customer) // Send approved order to Dispatcher
  }
}

// Dispatcher Actor
class Dispatcher extends Actor {
  def receive: Receive = {
    case ApprovedOrder(product, customer) =>
      println(s"Dispatcher: Dispatching product '$product' to Customer")
      customer ! Dispatch(product) // Notify the customer of dispatch
  }
}

// Main Application
object OrderProcessingApp {
  def main(args: Array[String]): Unit = {
    // Create the actor system
    val system = ActorSystem("OrderProcessingSystem")

    // Create the Dispatcher actor
    val dispatcher = system.actorOf(Props[Dispatcher], "dispatcher")

    // Create the SalesPerson actor, with a reference to the Dispatcher
    val salesPerson = system.actorOf(Props(new SalesPerson(dispatcher)), "salesPerson")

    // Create the Customer actor, with a reference to the SalesPerson
    val customer = system.actorOf(Props(new Consumer(salesPerson)), "customer")

    // Start the order process
    customer ! "placeOrder"

    // Shutdown the system after a delay to allow processing
    system.scheduler.scheduleOnce(5.seconds)(system.terminate())
  }
}
