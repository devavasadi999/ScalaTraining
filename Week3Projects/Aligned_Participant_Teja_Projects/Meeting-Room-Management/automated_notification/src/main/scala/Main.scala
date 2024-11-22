import actors.{BookingConfirmationActor, RoomPreparationActor, SchedulerActor}
import akka.actor.ActorSystem
import consumers.{BookingConfirmationConsumer, RoomPreparationConsumer}
import services.EmailService

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("NotificationSystem")
  val emailService = new EmailService()

  // Initialize actors with required parameters
  val bookingActor = system.actorOf(BookingConfirmationActor.props(emailService), "bookingConfirmationActor")
  val roomPreparationActor = system.actorOf(RoomPreparationActor.props(emailService), "roomPreparationActor")
  val schedulerActor = system.actorOf(SchedulerActor.props(system), "schedulerActor")

  BookingConfirmationConsumer.startConsumer()
  println("BookingConfirmationConsumer started.")

  RoomPreparationConsumer.startConsumer()
  println("RoomPreparationConsumer started.")

  // Keep the application running
  println("Notification System is running. Press ENTER to stop.")
  scala.io.StdIn.readLine() // Waits for ENTER to terminate
  system.terminate()         // Gracefully shut down the actor system
}
