package example

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import scalapb.GeneratedMessage
import example.employee.Employee

import scala.concurrent.ExecutionContext.Implicits.global


object MainProducer extends App {
  // Initialize ActorSystem
  implicit val system: ActorSystem = ActorSystem("KafkaProtobufProducer")

  // Kafka configuration
  val bootstrapServers = "localhost:9092"
  val topic = "employee-topic"

  // Producer settings
  val producerSettings = ProducerSettings(system, new StringSerializer, new ByteArraySerializer)
    .withBootstrapServers(bootstrapServers)

  // Serialize Protobuf to byte array
  def serializeProtobuf[T <: GeneratedMessage](message: T): Array[Byte] = message.toByteArray

  // Function to create Employee messages
  def createEmployee(id: String, name: String, department: String, salary: Float): Employee =
    Employee(id = id, name = name, department = department, salary = salary)

  // Kafka producer source
  val employeeRecords = Source(1 to 10).map { i =>
    val employee = createEmployee(s"EMP-$i", s"Employee-$i", "Engineering", 50000 + i * 1000)
    new ProducerRecord[String, Array[Byte]](topic, employee.id, serializeProtobuf(employee))
  }

  // Stream records to Kafka
  employeeRecords
    .runWith(Producer.plainSink(producerSettings))
    .onComplete { result =>
      println(s"Kafka Producer completed with result: $result")
      system.terminate()
    }
}
