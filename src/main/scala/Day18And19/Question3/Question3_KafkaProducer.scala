package Day18And19.Question3

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

object KafkaContinuousProducer {
  def main(args: Array[String]): Unit = {
    val isRunning = new AtomicBoolean(true) // Atomic flag to control the loop

    // Kafka configuration
    val topic = "transactions"
    val kafkaBootstrapServers = "localhost:9092"

    // Kafka producer properties
    val props = new Properties()
    props.put("bootstrap.servers", kafkaBootstrapServers)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](props)

    // Random data generator
    val random = new Random()

    // Function to send a message
    def sendRecord(): Unit = {
      val transactionId = s"txn${random.nextInt(10) + 1}" // Random transactionId between txn1 and txn10
      val userId = s"user${random.nextInt(5) + 1}"       // Random userId between user1 and user5
      val amount = random.nextDouble() * 500            // Random amount between 0 and 500

      // Create a JSON message
      val message = s"""{"transactionId": "$transactionId", "userId": "$userId", "amount": $amount}"""

      // Send the message to Kafka
      val record = new ProducerRecord[String, String](topic, null, message)
      producer.send(record)

      // Get the current timestamp
      val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

      // Print the message with the timestamp
      println(s"Sent at $currentTime: $message")
    }

    // Use a scheduler to send a record every second
    println("Starting Kafka producer. Sending messages every 4 seconds...")
    Future {
      while (isRunning.get()) {
        sendRecord()
        Thread.sleep(4000) // Wait for 4 seconds before sending the next message
      }
    }

    // Add shutdown hook to close the producer gracefully
    sys.addShutdownHook {
      println("Shutting down Kafka producer...")
      producer.close()
    }

    // Block the main thread to keep the program running
    while (isRunning.get()) {
      Thread.sleep(100) // Small sleep to avoid excessive CPU usage
    }

    println("Kafka producer stopped.")
  }
}

