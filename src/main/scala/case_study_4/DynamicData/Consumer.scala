package case_study_4.DynamicData

import org.apache.spark.sql.{DataFrame, Dataset, Row, SaveMode, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.protobuf.functions.from_protobuf
import org.apache.spark.sql.streaming.Trigger
import org.apache.spark.storage.StorageLevel

object KafkaProtobufSalesReader {

  val bucketName = "deva_vasadi"

  def main(args: Array[String]): Unit = {
    // Initialize SparkSession
    val spark = SparkSession.builder()
      .appName("KafkaProtobufSalesReader")
      .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
      .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
      .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
      .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Kafka configuration
    val kafkaBootstrapServers = "localhost:9092"
    val topic = "sales-topic"

    // Path to the Protobuf descriptor file
    val descriptorFile = "src/main/scala/case_study_4/DynamicData/descriptor/SalesRecord.desc" // Adjust path
    val messageType = "case_study_4.DynamicData.SalesRecord" // Fully qualified Protobuf type

    // GCS Configuration
    val featuresPath = s"gs://$bucketName/final_project/case_study_4/features.csv"
    val storesPath = s"gs://$bucketName/final_project/case_study_4/stores.csv"
    val trainPath = s"gs://$bucketName/final_project/case_study_4/updated_train.csv"

    // Load from GCS
    val rawFeaturesDF = spark.read
      .option("header", "true") // Adjusted for datasets saved with headers
      .option("inferSchema", "true")
      .csv(featuresPath)

    val rawStoresDF = spark.read
      .option("header", "true") // Adjusted for datasets saved with headers
      .option("inferSchema", "true")
      .csv(storesPath)

    // Validate critical columns for featuresDF and storesDF
    // Cache the features
    val featuresDF = rawFeaturesDF.na.drop("any", Seq("Store", "Date")).cache()
    // Broadcast the stores since it is very small (45 rows)
    val storesDF = broadcast(rawStoresDF.na.drop("any", Seq("Store", "Type", "Size")))

    // Read messages from Kafka
    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrapServers)
      .option("subscribe", topic)
      .option("startingOffsets", "earliest")
      .load()

    // Extract Protobuf binary data and deserialize to DataFrame
    val salesDF = kafkaDF
      .selectExpr("CAST(value AS BINARY) as value") // Extract binary Protobuf data
      .select(from_protobuf($"value", messageType, descriptorFile).alias("salesRecord")) // Deserialize Protobuf
      .select("salesRecord.*") // Flatten the struct for individual fields
      .na.fill(Map(
        "is_holiday" -> false,      // Default boolean value
        "weekly_sales" -> 0.0f      // Default float value
      ))

    // Process new records
    val query = salesDF.writeStream
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .foreachBatch { (batchDF: Dataset[Row], batchId: Long) =>
        println(s"Processing batch: $batchId")
        batchDF.show()

        // Append new records to updated_train.csv in GCS
        batchDF.write.mode("append").option("header", "true").csv(trainPath)

        // Update partitioned enriched data and metrics in GCS
        updateEnrichedAndMetrics(spark, featuresDF, storesDF)

        println(s"Batch $batchId processed successfully")
      }
      .start()

    query.awaitTermination() // Keep the query running
    spark.stop()
  }

  // Function to update enriched data and metrics
  def updateEnrichedAndMetrics(spark: SparkSession, featuresDF: DataFrame, storesDF: DataFrame): Unit = {
    val trainPath = s"gs://$bucketName/final_project/case_study_4/updated_train.csv"
    val enrichedPath = s"gs://$bucketName/final_project/case_study_4/enriched_data"

    val rawTrainDF = spark.read
      .option("header", "true") // Adjusted for datasets saved with headers
      .option("inferSchema", "true")
      .csv(trainPath)

    //Filter out records	where	Weekly_Sales	is	negative.
    // Data validation: Drop rows with missing or invalid values in critical columns
    val trainDF = rawTrainDF.filter("Weekly_Sales >= 0").na.drop("any", Seq("Store", "Dept", "Weekly_Sales", "Date"))

    //Update partitioned enriched data
    val newEnrichedDF = trainDF
      .join(featuresDF, Seq("Store", "Date", "IsHoliday"), "left")
      .join(storesDF, Seq("Store"), "left")

    val partitionedParquetPath = s"gs://$bucketName/final_project/case_study_4/enriched_data"
    val partitionedEnrichedDF = newEnrichedDF
      .repartition(col("Store"), col("Date")) // Partition by Store and Date
      .cache() // Cache for reuse in downstream processes

    // Write partitioned data to Parquet format
    partitionedEnrichedDF.limit(1000).write
      .mode(SaveMode.Overwrite)
      .partitionBy("Store", "Date") // Physically partition the data
      .parquet(partitionedParquetPath)

    //Update store wise metrics
    val storeMetrics = partitionedEnrichedDF.groupBy("Store")
      .agg(
        sum("Weekly_Sales").alias("Total_Weekly_Sales"),
        avg("Weekly_Sales").alias("Average_Weekly_Sales")
      )
      .orderBy(desc("Total_Weekly_Sales")).persist(StorageLevel.MEMORY_ONLY)
    val storeWiseAggregatedMetricsPath = s"gs://deva_vasadi/final_project/case_study_4/aggregated_metrics/store_wise"
    storeMetrics.write.mode("overwrite").json(storeWiseAggregatedMetricsPath)

    //Update department wise metrics
    val departmentMetrics = partitionedEnrichedDF.groupBy("Store", "Dept")
      .agg(
        sum("Weekly_Sales").alias("Total_Sales"),
        avg("Weekly_Sales").alias("Average_Sales")
      ).persist(StorageLevel.MEMORY_AND_DISK_SER)
    val deptWiseAggregatedMetricsPath = s"gs://deva_vasadi/final_project/case_study_4/aggregated_metrics/department_wise"
    departmentMetrics.write.mode("overwrite").json(deptWiseAggregatedMetricsPath)

    //Update holiday vs non holiday sales metrics
    // Persist the holidaySales
    val holidaySales = partitionedEnrichedDF.filter("IsHoliday = true")
      .groupBy("Store", "Dept")
      .agg(sum("Weekly_Sales").alias("Holiday_Sales")).persist(StorageLevel.MEMORY_AND_DISK)

    val nonHolidaySales = partitionedEnrichedDF.filter("IsHoliday = false")
      .groupBy("Store", "Dept")
      .agg(sum("Weekly_Sales").alias("NonHoliday_Sales"))

    val holidayComparison = holidaySales
      .join(nonHolidaySales, Seq("Store", "Dept"), "outer")
      .orderBy(desc("Holiday_Sales"))

    val holidayVsNonHolidayMetricsPath = s"gs://deva_vasadi/final_project/case_study_4/aggregated_metrics/holiday_vs_non_holiday"
    holidayComparison.write
      .mode(SaveMode.Overwrite)
      .json(holidayVsNonHolidayMetricsPath)

    println(s"Updated enriched data at $enrichedPath")
    println(s"Updated metrics")
  }
}
