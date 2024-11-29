package Day18And19.Question5

import org.apache.spark.sql.SparkSession

import scala.util.Random

object CreateLargeUserDetailsCSV {
  def main(args: Array[String]): Unit = {
    // Initialize SparkSession with GCS configurations
    val spark = SparkSession.builder()
      .appName("Create Large User Details CSV")
      .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
      .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
      .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
      .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
      .master("local[*]") // Adjust for your environment
      .getOrCreate()

    import spark.implicits._

    // Generate 1,000 random user details
    val random = new Random()
    val userDetails = (1 to 1000).map { id =>
      val name = s"User$id" // Generate user name dynamically
      (id, name)
    }

    // Convert to DataFrame
    val userDetailsDF = userDetails.toDF("user_id", "user_name")

    // Output Path on GCS
    val outputPath = "gs://deva_vasadi/config/user_details.csv"

    // Write the DataFrame as a CSV file to GCS
    userDetailsDF.write
      .option("header", "true") // Include header in the CSV file
      .mode("overwrite")        // Overwrite if the file already exists
      .csv(outputPath)

    println(s"User details CSV with 1,000 records successfully written to $outputPath")

    // Stop SparkSession
    spark.stop()
  }
}

import org.apache.spark.sql.functions._

object KafkaStreamingWithBroadcastDF {
  def main(args: Array[String]): Unit = {
    // Initialize SparkSession
    val spark = SparkSession.builder()
      .appName("Kafka Streaming with Broadcast DF")
      .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
      .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
      .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
      .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
      .master("local[*]")
      .getOrCreate()

    // Paths
    val userDetailsPath = "gs://deva_vasadi/config/user_details.csv"
    val outputPath = "gs://deva_vasadi/output/enriched_orders/"
    val kafkaTopic = "orders"
    val kafkaBootstrapServers = "localhost:9092"

    // Step 1: Load the User Details Dataset from GCS
    val userDetailsDF = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv(userDetailsPath)

    userDetailsDF.show(10)

    // Step 2: Read Streaming Data from Kafka
    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrapServers)
      .option("subscribe", kafkaTopic)
      .option("startingOffsets", "earliest")
      .load()

    // Parse Kafka value as JSON and extract relevant fields
    val ordersSchema = new org.apache.spark.sql.types.StructType()
      .add("order_id", "integer")
      .add("user_id", "integer")
      .add("amount", "double")

    val ordersDF = kafkaDF
      .selectExpr("CAST(value AS STRING) as json_string")
      .select(from_json(col("json_string"), ordersSchema).as("data"))
      .select("data.*")

    // Step 3: Enrich Orders with Broadcasted User Details
    val enrichedOrdersDF = ordersDF
      .join(broadcast(userDetailsDF), Seq("user_id"), "left_outer") // Broadcast the DataFrame

    // Step 4: Write Enriched Data Back to GCS
    val query = enrichedOrdersDF.writeStream
      .outputMode("append") // Append mode for continuous data
      .format("json") // Write as JSON
      .option("path", outputPath)
      .option("checkpointLocation", "gs://deva_vasadi/checkpoints/enriched_orders/")
      .start()

    query.awaitTermination()
  }
}
