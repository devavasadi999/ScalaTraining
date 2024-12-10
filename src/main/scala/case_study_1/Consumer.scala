package case_study_1

import org.apache.spark.sql.{DataFrame, Dataset, Row, SaveMode, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.protobuf.functions.from_protobuf
import org.apache.spark.sql.streaming.Trigger

object SensorReadingValidator {

  val bucketName = "deva_vasadi"
  val rawDataMessageType = "case_study_1.RawSensorReading"

  // Path to the Protobuf descriptor file
  val descriptorFile = "src/main/scala/case_study_1/descriptor/SensorReadings.desc"


  def main(args: Array[String]): Unit = {
    // Initialize SparkSession
    val spark = SparkSession.builder()
      .appName("KafkaProtobufSensorReader")
      .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
      .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
      .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
      .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
      .master("local[*]") // Use local for testing
      .getOrCreate()

    import spark.implicits._

    // Kafka configuration
    val kafkaBootstrapServers = "localhost:9092"
    val topic = "sensor-readings"

    // GCS Path Configuration
    val rawSensorDataPath = s"gs://$bucketName/raw/sensor-data"

    // Read messages from Kafka
    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrapServers)
      .option("subscribe", topic)
      .option("startingOffsets", "earliest")
      .load()

    // Extract Protobuf binary data and deserialize to DataFrame
    val sensorReadingsDF = kafkaDF
      .selectExpr("CAST(value AS BINARY) as value") // Extract binary Protobuf data
      .select(from_protobuf($"value", rawDataMessageType, descriptorFile).alias("sensorReading")) // Deserialize Protobuf
      .select("sensorReading.*") // Flatten the struct for individual fields

    // Data validation: filter out invalid records
    val validatedReadingsDF = sensorReadingsDF
      .filter($"temperature".between(-50, 150) && $"humidity".between(0, 100)) // Filter valid ranges

    // Process new records
    val query = validatedReadingsDF.writeStream
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .foreachBatch { (batchDF: Dataset[Row], batchId: Long) =>
        println(s"Processing batch: $batchId")
        batchDF.show()

        // Serialize valid records into Protobuf format and write to GCS
        writeValidatedDataToGCS(batchDF, rawSensorDataPath, spark)
        println(s"Batch $batchId processed successfully")
      }
      .start()

    query.awaitTermination() // Keep the query running
    spark.stop()
  }

  // Function to serialize and write validated data to GCS
  def writeValidatedDataToGCS(df: DataFrame, basePath: String, spark: SparkSession): Unit = {
    import org.apache.spark.sql.protobuf.functions.to_protobuf
    import spark.implicits._

    // Serialize to Protobuf format
    val serializedDF = df.withColumn("value", to_protobuf(struct(df.columns.map(col): _*), rawDataMessageType, descriptorFile))

    // Partition data by timestamp (yyyy/MM/dd/HH)
    val partitionedDF = serializedDF.withColumn("year", year(from_unixtime($"timestamp" / 1000)))
      .withColumn("month", month(from_unixtime($"timestamp" / 1000)))
      .withColumn("day", dayofmonth(from_unixtime($"timestamp" / 1000)))
      .withColumn("hour", hour(from_unixtime($"timestamp" / 1000)))

    // Write the protobuf data to GCS in avro format (widely used format for serialized data)
    partitionedDF.select("value", "year", "month", "day", "hour")
      .write
      .mode(SaveMode.Append)
      .partitionBy("year", "month", "day", "hour")
      .format("avro")
      .save(basePath)
  }
}
