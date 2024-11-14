import akka.actor.{ActorRef, ActorSystem, Props}
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import spray.json._
import JsonFormats._
import akka.stream.scaladsl.Sink

// Main application to start the kafka consumers and actors
object KafkaConsumerApp {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("KafkaListenersSystem")

    val producer = KafkaProducerFactory.createProducer()
    val messageGatherer = system.actorOf(Props(new MessageGatherer(producer)), "messageGatherer")

    val cloudListener = system.actorOf(Props(new CloudListener(messageGatherer)), "cloudListener")
    val networkListener = system.actorOf(Props(new NetworkListener(messageGatherer)), "networkListener")
    val appListener = system.actorOf(Props(new AppListener(messageGatherer)), "appListener")

    val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers("localhost:9092")
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    def startListener(topic: String, listener: ActorRef): Unit = {
      Consumer.plainSource(consumerSettings, Subscriptions.topics(topic))
        .map { record => record.value().parseJson.convertTo[Message] }
        .runWith(Sink.actorRef(listener, onCompleteMessage = "complete", onFailureMessage = (x: Throwable) => {}))
    }

    // Start each listener
    startListener("cloud-message", cloudListener)
    startListener("network-message", networkListener)
    startListener("app-message", appListener)

    println("Listeners are active and consuming messages...")
  }
}