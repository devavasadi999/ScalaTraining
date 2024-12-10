package case_study_4.DynamicData

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import case_study_4.DynamicData.SalesRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import scalapb.GeneratedMessage

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object SalesProducer extends App {

  // Initialize ActorSystem
  implicit val system: ActorSystem = ActorSystem("KafkaProtobufSalesProducer")

  // Kafka configuration
  val bootstrapServers = "localhost:9092"
  val topic = "sales-topic"

  // Producer settings
  val producerSettings = ProducerSettings(system, new StringSerializer, new ByteArraySerializer)
    .withBootstrapServers(bootstrapServers)

  // Serialize Protobuf to byte array
  def serializeProtobuf[T <: GeneratedMessage](message: T): Array[Byte] = message.toByteArray

  // Function to create SalesRecord messages
  def createSalesRecord(store: String, dept: String, date: String, weeklySales: Float, isHoliday: Boolean): SalesRecord.SalesRecord =
    SalesRecord.SalesRecord(store = store, dept = dept, date = date, weeklySales = weeklySales, isHoliday = isHoliday)

  // Kafka producer source (1 record every 2 seconds)
  val salesRecords = Source.tick(0.seconds, 2.seconds, ())
    .map { _ =>
      buildRecord()
    }

//  val salesRecords = Source(1 to 5).map {
//    i => buildRecord()
//  }

  // Stream records to Kafka
  salesRecords
    .runWith(Producer.plainSink(producerSettings))
    .onComplete { result =>
      println(s"Kafka Producer completed with result: $result")
      system.terminate()
    }

  def buildRecord(): ProducerRecord[String, Array[Byte]] = {
    val store = s"${scala.util.Random.nextInt(10) + 1}" // Random store between Store-1 and Store-10
    val dept = s"${scala.util.Random.nextInt(20) + 1}"   // Random department between Dept-1 and Dept-20
    val date = java.time.LocalDate.now().toString             // Current date
    val weeklySales = 1000 + scala.util.Random.nextFloat() * 1000
    val isHoliday = scala.util.Random.nextBoolean()           // Randomly true or false

    val record = createSalesRecord(store, dept, date, weeklySales.toFloat, isHoliday)
    println(record)
    new ProducerRecord[String, Array[Byte]](topic, record.store, serializeProtobuf(record))
  }
}
