package services

import models.Visitor
import javax.inject._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import java.util.Properties
import play.api.libs.json._

@Singleton
class KafkaProducerService @Inject()() {

  private val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  private val producer = new KafkaProducer[String, String](props)

  def sendToKafka(visitor: Visitor): Unit = {
    // Serialize the Visitor object to JSON
    val jsonMessage: String = Json.stringify(Json.toJson(visitor))

    // Send the message to Kafka
    val record = new ProducerRecord[String, String]("visitor-checkin-topic", "key", jsonMessage)
    producer.send(record)
  }
}
