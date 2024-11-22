import akka.actor.{ActorSystem, Props}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import scala.concurrent.Await
import scala.concurrent.duration._

object NotificationService extends App {

  val system = ActorSystem("VisitorNotificationSystem")

  val itSupportProcessor = system.actorOf(Props[ITSupportProcessor], "itSupportProcessor")
  val hostProcessor = system.actorOf(Props[HostProcessor], "hostProcessor")
  val securityProcessor = system.actorOf(Props[SecurityProcessor], "securityProcessor")

  val notificationHandler = system.actorOf(Props(new NotificationHandler(itSupportProcessor, hostProcessor, securityProcessor)), "notificationHandler")

  notificationHandler ! "start-consumer"

  Await.result(system.whenTerminated, Duration.Inf)
}

