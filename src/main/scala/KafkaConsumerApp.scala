import actors.EquipmentAllocationActors.{EmployeeActor, InventoryTeamActor, MaintenanceTeamActor}
import actors.EventManagementActors.{CateringTeamActor, DecorationsTeamActor, EntertainmentTeamActor, EventManagerActor, LogisticsTeamActor}
import actors.NotificationActor
import akka.Done
import akka.actor.{ActorSystem, Props}
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Keep, Sink}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.{ExecutionContext, Future}

// Main application to start Kafka consumers and actors
object KafkaConsumerApp extends App {

  // Load environment variables
  EnvLoader.loadEnv(".env")

  implicit val system: ActorSystem = ActorSystem("KafkaConsumerSystem")
  implicit val ec: ExecutionContext = system.dispatcher

  // NotificationActor for sending notifications
  val notificationActor = system.actorOf(Props[NotificationActor], "notificationActor")

  val kafkaBrokers = System.getProperty("KAFKA_BROKERS")
  println(s"Kafka brokers: $kafkaBrokers")

  // Kafka consumer settings
  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(kafkaBrokers)
    .withGroupId("kafkaConsumerAppGroup") // Unique group ID
    .withProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000")
    .withProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000")

  // Define topics for service teams
  val allTopics = Map(
    "catering_team_topic" -> system.actorOf(Props(new CateringTeamActor(notificationActor)), "cateringTeamActor"),
    "entertainment_team_topic" -> system.actorOf(Props(new EntertainmentTeamActor(notificationActor)), "entertainmentTeamActor"),
    "decorations_team_topic" -> system.actorOf(Props(new DecorationsTeamActor(notificationActor)), "decorationsTeamActor"),
    "logistics_team_topic" -> system.actorOf(Props(new LogisticsTeamActor(notificationActor)), "logisticsTeamActor"),
    "event_manager_topic" -> system.actorOf(Props(new EventManagerActor(notificationActor)), "eventManagerActor"),
    "employee_topic" -> system.actorOf(Props(new EmployeeActor(notificationActor)), "employeeActor"),
    "inventory_team_topic" -> system.actorOf(Props(new InventoryTeamActor(notificationActor)), "inventoryTeamActor"),
    "maintenance_team_topic" -> system.actorOf(Props(new MaintenanceTeamActor(notificationActor)), "maintenanceTeamActor")
  )

  // Start consumers for each service team topic
  val controls = allTopics.map { case (topic, actor) =>
    Consumer
      .plainSource(consumerSettings, Subscriptions.topics(topic))
      .map(record => record.value()) // Extract message value
      .toMat(Sink.foreach(actor ! _))(Keep.both)
      .mapMaterializedValue(DrainingControl[Done])
      .run()
  }

  println("Kafka Consumers are running and listening for messages...")

  // Graceful shutdown hook
  sys.addShutdownHook {
    println("Shutting down Kafka consumers...")
    val shutdownFutures = controls.map(_.drainAndShutdown())
    Future.sequence(shutdownFutures).onComplete { _ =>
      println("Kafka consumers stopped. Terminating the ActorSystem...")
      system.terminate()
    }
  }
}
