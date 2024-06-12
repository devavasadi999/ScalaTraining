import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql.types._

object KafkaStreamToCSV {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .appName("KafkaStreamToCSV")
      .master("local[2]")  // Use local mode with 2 threads, adjust as needed
      .getOrCreate()

    // Important for using DataFrame operations like select
    import spark.implicits._

    // Define the schema of the JSON data
    val schema = new StructType()
      .add("sno", IntegerType)
      .add("product", IntegerType)
      .add("rating", StringType)

    // Kafka configuration
    val kafkaTopic = "ratings"
    val kafkaBootstrapServers = "localhost:9092"  // Update as necessary

    // Create DataFrame representing the stream of input lines from Kafka
    val df = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrapServers)
      .option("failOnDataLoss","false")
      .option("subscribe", kafkaTopic)
      .option("startingOffsets", "earliest")  // From the beginning of the topic
      .load()

    // Convert the key and value from Kafka into string types, and then parse the JSON
    val parsedData = df
      .selectExpr("CAST(value AS STRING) as json_string")
      .select(from_json(col("json_string"), schema).as("data"))
      .select("data.*")

    // Define the path to save the CSV files
    val outputPath = "/Users/vinodh/datasets/todaynew.csv"

    // Start the streaming query, writing the result to the given location in append mode
    val query = parsedData
      .writeStream
      .outputMode("append")
      .format("csv")
      .option("path", outputPath)
      .option("checkpointLocation", "/Users/vinodh/datasets/checkpoint2")
      .trigger(Trigger.ProcessingTime("1 minute"))  // Trigger the write every minute
      .start()

    query.awaitTermination()
  }
}
