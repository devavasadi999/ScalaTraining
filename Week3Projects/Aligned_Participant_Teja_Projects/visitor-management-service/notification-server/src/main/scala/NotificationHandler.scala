import akka.actor.{Actor, ActorRef, Props}
import io.circe.generic.auto.exportDecoder
import io.circe.parser.decode
import model.Visitor
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.ConsumerRecord

class NotificationHandler(itSupportProcessor: ActorRef, hostProcessor: ActorRef, securityProcessor: ActorRef) extends Actor {

  // Kafka Consumer configuration (same as before)
  private val consumerConfig = new java.util.Properties()
  consumerConfig.put("bootstrap.servers", "localhost:9092")
  consumerConfig.put("group.id", "visitor-notification-group")
  consumerConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  consumerConfig.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")

  val kafkaConsumer = new KafkaConsumer[String, String](consumerConfig)
  kafkaConsumer.subscribe(java.util.Collections.singletonList("visitor-checkin-topic"))

  override def receive: Receive = {
    case "start-consumer" =>
      consumeMessages()

    case message: String =>
      // Deserialize the message into a Visitor object
      decode[Visitor](message) match {
        case Right(visitor) =>
          visitor.status match {
            case "Waiting" =>
              // For pending status, notify the host for verification
              hostProcessor ! visitor

            case "Approved" =>
              // For check-in status, notify IT Support, Host, and Security
              itSupportProcessor ! visitor
              hostProcessor ! visitor
              securityProcessor ! visitor

            case "Checked-out" =>
              // For check-out status, notify IT Support, Host, and Security
              itSupportProcessor ! visitor
              hostProcessor ! visitor
              securityProcessor ! visitor

            case _ =>
              println(s"Unknown status: ${visitor.status}")
          }

        case Left(error) =>
          println(s"Failed to decode message: $error")
      }
  }

  def consumeMessages(): Unit = {
    new Thread(new Runnable {
      override def run(): Unit = {
        while (true) {
          val records: ConsumerRecords[String, String] = kafkaConsumer.poll(10000)

          // Use forEach to avoid any issues with Scala-specific iteration
          records.forEach { record: ConsumerRecord[String, String] =>
            val visitorJson = record.value()
            self ! visitorJson // Send the JSON string to the actor for deserialization
          }
        }
      }
    }).start()
  }
}
