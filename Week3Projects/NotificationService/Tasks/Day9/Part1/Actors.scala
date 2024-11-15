import akka.actor.{Actor, ActorRef}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import spray.json.{enrichAny}
import JsonFormats._

// ProcessMessage wrapper for flexibility
case class ProcessMessage(msg: Message)

//Network Message Processor Actor
class NetworkMessageProcessor(producer: KafkaProducer[String, String]) extends Actor {
  def receive: Receive = {
    case ProcessMessage(message) =>
      val record = new ProducerRecord[String, String]("network-message", message.messageKey, message.toJson.toString())
      producer.send(record)
      println(s"NetworkMessageProcessor sent message: $message")
  }
}

//Cloud Message Processor Actor
class CloudMessageProcessor(producer: KafkaProducer[String, String]) extends Actor {
  def receive: Receive = {
    case ProcessMessage(message) =>
      val record = new ProducerRecord[String, String]("cloud-message", message.messageKey, message.toJson.toString())
      producer.send(record)
      println(s"CloudMessageProcessor sent message: $message")
  }
}

//App Message Processor Actor
class AppMessageProcessor(producer: KafkaProducer[String, String]) extends Actor {
  def receive: Receive = {
    case ProcessMessage(message) =>
      val record = new ProducerRecord[String, String]("app-message", message.messageKey, message.toJson.toString())
      producer.send(record)
      println(s"AppMessageProcessor sent message: $message")
  }
}

//Message Handler Actor to route messages to the appropriate processor
class MessageHandler(networkProcessor: ActorRef, cloudProcessor: ActorRef, appProcessor: ActorRef) extends Actor{
  def receive: Receive = {
    case msg: Message =>
      msg.messageType match {
        case "NetworkMessage" => networkProcessor ! ProcessMessage(msg)
        case "CloudMessage" => cloudProcessor ! ProcessMessage(msg)
        case "AppMessage" => appProcessor ! ProcessMessage(msg)
      }
  }
}