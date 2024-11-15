import akka.actor.{Actor, ActorRef}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import spray.json.enrichAny
import JsonFormats._

// MessageGatherer Actor (aggregator and producer)
class MessageGatherer(producer: KafkaProducer[String, String]) extends Actor {
  def receive: Receive = {
    case msg: Message =>
      val record = new ProducerRecord[String, String]("consolidated-messages", msg.messageKey, msg.toJson.toString())
      producer.send(record)
      println(s"MessageGatherer consolidated message: $msg")
  }
}

// Listener Actor for each Kafka topic
class CloudListener(messageGatherer: ActorRef) extends Actor {
  def receive: Receive = {
    case msg: Message =>
      messageGatherer ! msg
  }
}

class NetworkListener(messageGatherer: ActorRef) extends Actor {
  def receive: Receive = {
    case msg: Message =>
      messageGatherer ! msg
  }
}

class AppListener(messageGatherer: ActorRef) extends Actor {
  def receive: Receive = {
    case msg: Message =>
      messageGatherer ! msg
  }
}