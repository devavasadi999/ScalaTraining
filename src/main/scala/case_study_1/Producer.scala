package case_study_1

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import case_study_1.RawSensorReading
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import scalapb.GeneratedMessage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object SensorReadingProducer extends App {

  // Initialize ActorSystem
  implicit val system: ActorSystem = ActorSystem("KafkaProtobufSensorProducer")

  // Kafka configuration
  val bootstrapServers = "localhost:9092"
  val topic = "sensor-readings"

  // Producer settings
  val producerSettings = ProducerSettings(system, new StringSerializer, new ByteArraySerializer)
    .withBootstrapServers(bootstrapServers)

  // Serialize Protobuf to byte array
  def serializeProtobuf[T <: GeneratedMessage](message: T): Array[Byte] = message.toByteArray

  // Function to create SensorReading messages
  def createSensorReading(sensorId: String, timestamp: Long, temperature: Float, humidity: Float): RawSensorReading.RawSensorReading =
    RawSensorReading.RawSensorReading(sensorId = sensorId, timestamp = timestamp, temperature = temperature, humidity = humidity)

  // Kafka producer source (1 record every 100 milliseconds)
//  val sensorReadings = Source.tick(0.seconds, 100.milliseconds, ())
//    .map { _ =>
//      buildRecord()
//    }

  val sensorReadings = Source(1 to 5)
    .map { _ =>
      buildRecord()
    }

  // Stream records to Kafka
  sensorReadings
    .runWith(Producer.plainSink(producerSettings))
    .onComplete { result =>
      println(s"Kafka Producer completed with result: $result")
      system.terminate()
    }

  def buildRecord(): ProducerRecord[String, Array[Byte]] = {
    val sensorId = s"Sensor-${scala.util.Random.nextInt(100) + 1}" // Random sensor ID
    val timestamp = System.currentTimeMillis()                    // Current timestamp
    val temperature = -50 + scala.util.Random.nextFloat() * 200   // Temperature between -50 and 150
    val humidity = scala.util.Random.nextFloat() * 100            // Humidity between 0 and 100

    val record = createSensorReading(sensorId, timestamp, temperature, humidity)
    println(record) // Print the record for debugging
    new ProducerRecord[String, Array[Byte]](topic, record.sensorId, serializeProtobuf(record))
  }
}
