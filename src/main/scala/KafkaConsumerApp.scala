import actors.{MessageProcessorActor, NotificationActor}
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
    .withGroupId("kafkaConsumerAppGroup4") // Adding a unique group ID
    //.withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000")
    .withProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "300000")

  // Start a consumer for the "rawNotification" topic
  val rawNotificationControl = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("rawNotification"))
    .map(record => record.value()) // Extract message value from Kafka record
    .toMat(Sink.foreach(messageProcessor ! _))(Keep.both)
    .mapMaterializedValue(DrainingControl[Done])
    .run()

  // Start a consumer for the "processedNotification" topic
  val processedNotificationControl = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("processedNotification"))
    .map(record => record.value()) // Extract message value from Kafka record
    .toMat(Sink.foreach(notificationActor ! _))(Keep.both)
    .mapMaterializedValue(DrainingControl[Done])
    .run()

  println("Kafka Consumers are running and listening for messages...")

  // Graceful shutdown hook
  sys.addShutdownHook {
    println("Shutting down Kafka consumers...")
    val shutdownFuture: Future[Done] = for {
      _ <- rawNotificationControl.drainAndShutdown()
      _ <- processedNotificationControl.drainAndShutdown()
    } yield Done

    shutdownFuture.onComplete { _ =>
      println("Kafka consumers stopped. Terminating the ActorSystem...")
      system.terminate()
    }
  }
}
