package utils

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.util.Properties

object KafkaProducerUtil {
  val props: Properties = new Properties()
  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  val producer = new KafkaProducer[String, String](props)

  def sendMessage(topic: String, key: String, message: String): Unit = {
    val record = new ProducerRecord[String, String](topic, key, message)
    producer.send(record)
  }

  def closeProducer(): Unit = {
    producer.close()
  }
}
