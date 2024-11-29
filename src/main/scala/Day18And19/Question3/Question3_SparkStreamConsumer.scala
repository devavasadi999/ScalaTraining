package Day18And19.Question3

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.types._

object KafkaStreamAggregation {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .appName("Kafka Streaming Aggregation")
      .master("local[2]") // Adjust for your cluster setup
      .getOrCreate()

    // Define schema for the incoming JSON data
    val schema = new StructType()
      .add("transactionId", StringType)
      .add("userId", StringType)
      .add("amount", DoubleType)

    // Kafka configuration
    val kafkaTopic = "transactions"
    val kafkaBootstrapServers = "localhost:9092" // Update with your Kafka server

    // Read data from Kafka
    val df = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrapServers)
      .option("subscribe", kafkaTopic)
      .option("startingOffsets", "earliest")
      .load()

    // Parse JSON and add a processing time-based timestamp
    val parsedData = df
      .selectExpr("CAST(value AS STRING) as json_string")
      .select(from_json(col("json_string"), schema).as("data"))
      .select("data.*")
      .withColumn("timestamp", current_timestamp()) // Add current processing time as timestamp

    // Perform a 10-second windowed aggregation
    val aggregatedData = parsedData
      .groupBy(
        window(col("timestamp"), "10 seconds") // Group by 10-second time window
      )
      .agg(
        sum("amount").as("total_amount") // Sum, the amount for all transactionIds
      )

    // Write the aggregated data to the console
    val query = aggregatedData
      .writeStream
      .outputMode("update")
      .format("console")
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .start()

    query.awaitTermination()
  }
}