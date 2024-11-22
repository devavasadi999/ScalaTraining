package consumers

import akka.actor.ActorSystem
import io.circe.parser.decode
import models.Reservation
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.serialization.StringDeserializer
import utils.JsonFormats.*

import java.util.{Collections, Properties}

object RoomPreparationConsumer {
  def startConsumer()(implicit system: ActorSystem): Unit = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "room-preparation-group")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)

    val consumer = new KafkaConsumer[String, String](props)
    consumer.subscribe(Collections.singletonList("meeting-reservation"))

    println("Room Preparation Consumer started")

    while (true) {
      val records = consumer.poll(java.time.Duration.ofMillis(100))
      records.forEach { record =>
        decode[Reservation](record.value()) match {
          case Right(reservation) =>
            system.actorSelection("/user/roomPreparationActor") ! reservation
          case Left(error) =>
            system.log.error(s"Failed to decode JSON to Reservation: ${error.getMessage}")
        }
      }
    }
  }
}
