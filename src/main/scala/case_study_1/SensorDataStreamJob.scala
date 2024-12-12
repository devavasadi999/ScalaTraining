package case_study_1

import case_study_1.AggregatedSensorMetrics.AggregatedSensorMetrics
import org.apache.spark.sql.functions._
import org.apache.spark.sql.protobuf.functions.{from_protobuf, to_protobuf}
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.sql._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.apache.hadoop.fs.{FileSystem, Path}

object SensorDataAggregation {

  val bucketName = "deva_vasadi"
  val rawDataMessageType = "case_study_1.RawSensorReading"
  val aggregatedDataMessageType = "case_study_1.AggregatedSensorMetrics"

  // Path to the Protobuf descriptor file
  val descriptorFile = "src/main/scala/case_study_1/descriptor/SensorReadings.desc"

  val spark: SparkSession = SparkSession.builder()
    .appName("SensorDataAggregation")
    .config("spark.hadoop.fs.defaultFS", "gs://deva_vasadi/")
    .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
    .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
    .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
    .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._

  def filterRecords(sensorReadingsDF: DataFrame): Dataset[Row] = {
    sensorReadingsDF
      .filter($"temperature".between(-50, 150) && $"humidity".between(0, 100))
  }

  def main(args: Array[String]): Unit = {

    // Kafka configuration
    val kafkaBootstrapServers = "localhost:9092"
    val topic = "sensor-readings"

    // GCS Path Configuration
    val aggregatedDataBasePath = s"gs://$bucketName/aggregated"
    val rawDataBasePath = s"gs://$bucketName/raw/sensor-data"

    // Read messages from Kafka
    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrapServers)
      .option("subscribe", topic)
      .option("startingOffsets", "earliest")
      .load()

    // Extract Protobuf binary data and deserialize to DataFrame
    val sensorReadingsDF = kafkaDF
      .selectExpr("CAST(value AS BINARY) as value")
      .select(from_protobuf($"value", rawDataMessageType, descriptorFile).alias("sensorReading"))
      .select("sensorReading.*")
      .na.fill(Map(
        "timestamp" -> 0L,
        "temperature" -> 0.0f,
        "humidity" -> 0.0f
      ))

    // Data validation: filter out invalid records
    val validatedReadingsDF = filterRecords(sensorReadingsDF)

    // Process new records
    val query = validatedReadingsDF.writeStream
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .foreachBatch { (batchDF: Dataset[Row], batchId: Long) =>
        println(s"Processing batch: $batchId")

        //broadcast it so that later on joins will be optimised and also it only contains 100 rows ( 10 per second X 10 seconds)
        val broadCastedBatchDF = broadcast(batchDF)
        broadCastedBatchDF.show()

        val currentTimestamp = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH")
        val currentHourAggregatedDataFolder = s"$aggregatedDataBasePath/protobuf/${currentTimestamp.format(formatter)}/"
        val previousHourAggregatedDataFolder = s"$aggregatedDataBasePath/protobuf/${currentTimestamp.minusHours(1).format(formatter)}/"
        val jsonAggregatedDataPath = s"$aggregatedDataBasePath/json/${currentTimestamp.format(formatter)}/"
        val currentHourRawDataFolder = s"$rawDataBasePath/${currentTimestamp.format(formatter)}/"

        //Save the received new batch of sensor readings data
        storeRawData(broadCastedBatchDF, currentHourRawDataFolder)

        performIncrementalAggregation(broadCastedBatchDF, currentHourAggregatedDataFolder, previousHourAggregatedDataFolder, jsonAggregatedDataPath, spark)

        println(s"Batch $batchId processed successfully")
      }
      .start()

    query.awaitTermination() // Keep the query running
    spark.stop()
  }

  def storeRawData(newData: DataFrame, currentHourRawDataFolder: String): Unit = {

    // Serialize new raw data to Protobuf
    val serializedNewRawData = newData.withColumn(
      "value",
      to_protobuf(struct(newData.columns.map(col): _*), rawDataMessageType, descriptorFile)
    ).select($"value")

    // Append new raw data to GCS
    serializedNewRawData.write
      .mode(SaveMode.Append)
      .format("avro") // Save in Avro format
      .save(currentHourRawDataFolder)
  }

  def computeNewAggregatedMetrics(newRawDataBatch: DataFrame, existingAggregatedData: DataFrame): DataFrame = {
    val dataToBeSaved = if (existingAggregatedData != null) {
      val newAggregatedData = newRawDataBatch.groupBy("sensorId")
        .agg(
          avg("temperature").alias("avgTemperature_new"),
          avg("humidity").alias("avgHumidity_new"),
          min("temperature").alias("minTemperature_new"),
          max("temperature").alias("maxTemperature_new"),
          min("humidity").alias("minHumidity_new"),
          max("humidity").alias("maxHumidity_new"),
          count("sensorId").alias("dataCount_new")
        )

      // Join with existing data and calculate updated metrics
      newAggregatedData.join(
        existingAggregatedData,
        Seq("sensorId"),
        "outer"
      ).select(
        coalesce($"sensorId", $"sensorId").alias("sensorId"),
        (((coalesce($"avgTemperature", lit(0.0f)) * coalesce($"dataCount", lit(0))) +
          (coalesce($"avgTemperature_new", lit(0.0f)) * coalesce($"dataCount_new", lit(0)))) /
          (coalesce($"dataCount", lit(0)) + coalesce($"dataCount_new", lit(0)))).cast("float").alias("avgTemperature"),
        (((coalesce($"avgHumidity", lit(0.0f)) * coalesce($"dataCount", lit(0))) +
          (coalesce($"avgHumidity_new", lit(0.0f)) * coalesce($"dataCount_new", lit(0)))) /
          (coalesce($"dataCount", lit(0)) + coalesce($"dataCount_new", lit(0)))).cast("float").alias("avgHumidity"),
        least($"minTemperature", $"minTemperature_new").alias("minTemperature"),
        greatest($"maxTemperature", $"maxTemperature_new").alias("maxTemperature"),
        least($"minHumidity", $"minHumidity_new").alias("minHumidity"),
        greatest($"maxHumidity", $"maxHumidity_new").alias("maxHumidity"),
        (coalesce($"dataCount", lit(0)) + coalesce($"dataCount_new", lit(0))).cast("int").alias("dataCount")
      )
    } else {
      // If no existing aggregated data, directly save new aggregated metrics
      newRawDataBatch.groupBy("sensorId")
        .agg(
          avg("temperature").cast("float").alias("avgTemperature"),
          avg("humidity").cast("float").alias("avgHumidity"),
          min("temperature").alias("minTemperature"),
          max("temperature").alias("maxTemperature"),
          min("humidity").alias("minHumidity"),
          max("humidity").alias("maxHumidity"),
          count("sensorId").cast("int").alias("dataCount")
        )
    }

    dataToBeSaved
  }

  // Function to perform incremental aggregation for the current hour
  def performIncrementalAggregation(newRawDataBatch: DataFrame, currentHourAggregatedDataFolder: String, previousHourAggregatedDataFolder: String, jsonAggregatedDataPath: String, spark: SparkSession): Unit = {
    import spark.implicits._

    val fs = FileSystem.get(spark.sparkContext.hadoopConfiguration)

    val existingAggregatedData = if (fs.exists(new Path(currentHourAggregatedDataFolder))) {

      // If current hour folder exists, read data
      // Cache the data because the number of rows will be small (equal to number of distinct sensors)
      // Caching improves downstream operations
      spark.read.format("avro")
        .load(currentHourAggregatedDataFolder)
        .select(from_protobuf($"value", aggregatedDataMessageType, descriptorFile).alias("data"))
        .select("data.*")
        .cache()
    } else if (fs.exists(new Path(previousHourAggregatedDataFolder))) {
      // If current hour folder does not exist but previous hour folder exists, read data
      spark.read.format("avro")
        .load(previousHourAggregatedDataFolder)
        .select(from_protobuf($"value", aggregatedDataMessageType, descriptorFile).alias("data"))
        .select("data.*")
        .cache()
    } else {
      // If neither current nor previous hour folder exists, skip reading
      null
    }

    // Perform incremental aggregation
    // Cache the new aggregated metrics to optimize downstream operations
    val newAggregatedMetrics = computeNewAggregatedMetrics(newRawDataBatch, existingAggregatedData).cache()
    newAggregatedMetrics.show()

    // Save the new aggregated metrics
    storeAggregatedMetrics(newAggregatedMetrics, currentHourAggregatedDataFolder, jsonAggregatedDataPath)
  }

  def storeAggregatedMetrics(newAggregatedMetrics: DataFrame, currentHourAggregatedDataFolder: String, jsonAggregatedDataPath: String): Unit = {
    // Serialize updated aggregated data back to Protobuf
    val serializedAggregatedData = newAggregatedMetrics.withColumn(
      "value",
      to_protobuf(struct(newAggregatedMetrics.columns.map(col): _*), aggregatedDataMessageType, descriptorFile)
    ).select("value")

    // Write updated aggregated data to GCS
    serializedAggregatedData.write
      .mode(SaveMode.Overwrite)
      .format("avro")
      .save(currentHourAggregatedDataFolder)

    // Write JSON representation for dashboard consumption
    newAggregatedMetrics.write
      .mode(SaveMode.Overwrite)
      .json(jsonAggregatedDataPath)
  }
}
