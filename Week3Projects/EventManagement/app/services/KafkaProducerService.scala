package services

import javax.inject._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import java.util.Properties

@Singleton
class KafkaProducerService @Inject()() {

  private val props = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  private val producer = new KafkaProducer[String, String](props)

  def send(topic: String, message: String): Unit = {
    val record = new ProducerRecord[String, String](topic, message)
    producer.send(record)
  }
}
