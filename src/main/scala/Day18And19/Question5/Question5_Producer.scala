package Day18And19.Question5

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random

object KafkaContinuousOrdersProducer {
  def main(args: Array[String]): Unit = {
    val isRunning = new AtomicBoolean(true) // Atomic flag to control the loop

    // Kafka configuration
    val topic = "orders"
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
      val orderId = random.nextInt(1000) + 1          // Random order_id between 1 and 1000
      val userId = random.nextInt(1000) + 1          // Random user_id between 1 and 1000
      val amount = BigDecimal(random.nextDouble() * 500).setScale(2, BigDecimal.RoundingMode.HALF_UP) // Random amount rounded to 2 decimals

      // Create a JSON message
      val message = s"""{"order_id": $orderId, "user_id": $userId, "amount": $amount}"""

      // Send the message to Kafka
      val record = new ProducerRecord[String, String](topic, null, message)
      producer.send(record)

      // Get the current timestamp
      val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

      // Print the message with the timestamp
      println(s"Sent at $currentTime: $message")
    }

    // Use a scheduler to send a record every 1 second
    println("Starting Kafka producer. Sending messages every 1 second...")
    Future {
      while (isRunning.get()) {
        sendRecord()
        Thread.sleep(1000) // Wait for 1 second before sending the next message
      }
    }

    // Add shutdown hook to close the producer gracefully
    sys.addShutdownHook {
      println("Shutting down Kafka producer...")
      isRunning.set(false) // Stop the loop
      producer.close()
    }

    // Block the main thread to keep the program running
    while (isRunning.get()) {
      Thread.sleep(100) // Small sleep to avoid excessive CPU usage
    }

    println("Kafka producer stopped.")
  }
}
