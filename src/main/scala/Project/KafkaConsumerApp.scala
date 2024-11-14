package Project

import Project.{MessageProcessorActor, NotificationActor}
import akka.actor.{Actor, ActorSystem, Props}
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import spray.json._

import scala.concurrent.ExecutionContext

// Main application to start Kafka consumers and actors
object KafkaConsumerApp extends App {
  implicit val system: ActorSystem = ActorSystem("KafkaConsumerSystem1")
  implicit val ec: ExecutionContext = system.dispatcher

  // Define actors for processing messages
  val messageProcessor = system.actorOf(Props[MessageProcessorActor], "messageProcessor")
  val notificationActor = system.actorOf(Props[NotificationActor], "notificationActor")

  // Kafka consumer settings
  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers("localhost:9092")
    .withGroupId("kafkaConsumerAppGroup")  // Adding a unique group ID
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000")
    .withProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000")

  // Start a consumer for the "RawNotification" and send messages to messageProcessor actor
  Consumer
    .plainSource(consumerSettings, Subscriptions.topics("rawNotification"))
    .map(record => record.value()) // Extract message value from Kafka record
    .runWith(Sink.foreach(messageProcessor ! _)) // Send each message to the messageProcessor actor

  // Start a consumer for the "ProcessedNotification" topic and send messages to notificationActor
  Consumer
    .plainSource(consumerSettings, Subscriptions.topics("processedNotification"))
    .map(record => record.value()) // Extract message value from Kafka record
    .runWith(Sink.foreach(notificationActor ! _)) // Send each message to the notificationActor

  println("Kafka Consumers are running and listening for messages...")

  // Add a shutdown hook for graceful termination
  sys.addShutdownHook {
    system.terminate()
  }
}
