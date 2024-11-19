import actors.{MessageProcessorActor, NotificationActor}
import akka.actor.{ActorSystem, Props}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.ExecutionContext

// Main application to start Kafka consumers and actors
object KafkaConsumerApp extends App {

  // Load environment variables
  EnvLoader.loadEnv(".env")

  implicit val system: ActorSystem = ActorSystem("KafkaConsumerSystem1")
  implicit val ec: ExecutionContext = system.dispatcher

  // Define actors for processing messages
  val messageProcessor = system.actorOf(Props[MessageProcessorActor], "messageProcessor")
  val notificationActor = system.actorOf(Props[NotificationActor], "notificationActor")

  val kafkaBrokers = System.getProperty("KAFKA_BROKERS")
  println(s"Kafka brokers: ${kafkaBrokers}")

  // Kafka consumer settings
  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(kafkaBrokers)
    .withGroupId("kafkaConsumerAppGroup")  // Adding a unique group ID
    //.withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
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