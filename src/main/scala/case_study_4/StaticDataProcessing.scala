import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.SaveMode
import org.apache.spark.storage.StorageLevel

object StaticDataProcessing {
  // Initialize SparkSession with GCS configurations
  val spark = SparkSession.builder()
    .appName("Static Data Processing")
    .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
    .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
    .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
    .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._

  def main(args: Array[String]): Unit = {

    // Set log level to WARN to reduce logging verbosity
    spark.sparkContext.setLogLevel("WARN")

    val bucketName = "deva_vasadi"
    val featuresPath = s"gs://$bucketName/final_project/case_study_4/features.csv"
    val trainPath = s"gs://$bucketName/final_project/case_study_4/train.csv"
    val storesPath = s"gs://$bucketName/final_project/case_study_4/stores.csv"

    //Data Preparation

    // Load from GCS
    val featuresDF = spark.read
      .option("header", "true") // Adjusted for datasets saved with headers
      .option("inferSchema", "true")
      .csv(featuresPath)

    val trainDF = spark.read
      .option("header", "true") // Adjusted for datasets saved with headers
      .option("inferSchema", "true")
      .csv(trainPath)

    val storesDF = spark.read
      .option("header", "true") // Adjusted for datasets saved with headers
      .option("inferSchema", "true")
      .csv(storesPath)

//    featuresDF.show(10)
//    trainDF.show(10)
//    storesDF.show(10)

    //Data Validation and Enrichment

    //Filter out records	where	Weekly_Sales	is	negative.
    // Data validation: Drop rows with missing or invalid values in critical columns
    val validatedTrainDF = trainDF.filter($"Weekly_Sales" >= 0).na.drop("any", Seq("Store", "Dept", "Weekly_Sales", "Date"))

    // Validate critical columns for featuresDF and storesDF
    val validatedFeaturesDF = featuresDF.na.drop("any", Seq("Store", "Date"))
    val validatedStoresDF = storesDF.na.drop("any", Seq("Store", "Type", "Size"))

    // Cache the validated DataFrames for repeated use
    val cachedFeaturesDF = validatedFeaturesDF.cache()
    // Broadcast the StoresDF since it is very small (45 rows)
    val broadcastedStoresDF = broadcast(validatedStoresDF)

    // Perform	joins	with	features.csv	and	stores.csv	on	relevant	keys.
    val enrichedDF = validatedTrainDF
      .join(cachedFeaturesDF, Seq("Store", "Date", "IsHoliday"), "left")
      .join(broadcastedStoresDF, Seq("Store"), "left")

    // Show enriched data for verification
    enrichedDF.show(10)

    // Partitioned Storage in Parquet Format
    val partitionedParquetPath = s"gs://$bucketName/final_project/case_study_4/enriched_data"

    val partitionedEnrichedDF = enrichedDF
      .repartition(col("Store"), col("Date")) // Partition by Store and Date
      .cache() // Cache for reuse in downstream processes

    partitionedEnrichedDF.show(10)

    // Write partitioned data to Parquet format
    partitionedEnrichedDF.write
      .mode(SaveMode.Overwrite)
      .partitionBy("Store", "Date") // Physically partition the data
      .parquet(partitionedParquetPath)

    //Aggregation
    // Call the method to compute aggregations
    computeSalesMetrics(partitionedEnrichedDF)

    // Stop Spark session
    spark.stop()
  }

  def computeSalesMetrics(partitionedEnrichedDF: DataFrame): Unit = {
    // Store-Level Metrics
    val storeMetrics = partitionedEnrichedDF.groupBy("Store")
      .agg(
        sum("Weekly_Sales").alias("Total_Weekly_Sales"),
        avg("Weekly_Sales").alias("Average_Weekly_Sales"),
        count("Store").alias("Data_Count")
      )

    // Cache store-level metrics for reuse, MEMORY ONLY will be sufficient for small datasets (45 rows)
    val cachedStoreMetrics = storeMetrics.persist(StorageLevel.MEMORY_ONLY)
    println("Store-Level Metrics:")
    cachedStoreMetrics.show(10)

    // Store Wise Aggregated Metrics Storage in JSON Format
    val storeWiseAggregatedMetricsPath = s"gs://deva_vasadi/final_project/case_study_4/aggregated_metrics/store_wise"
    cachedStoreMetrics.write
      .mode(SaveMode.Overwrite)
      .json(storeWiseAggregatedMetricsPath)

    // Top-Performing Stores (assuming "performance" is based on total weekly sales)
    val topStores = cachedStoreMetrics.orderBy(desc("Total_Weekly_Sales")).limit(5)
    println("Top-Performing Stores:")
    topStores.show()

    // Department-Level Metrics
    val departmentMetrics = partitionedEnrichedDF.groupBy("Store", "Dept")
      .agg(
        sum("Weekly_Sales").alias("Total_Weekly_Sales"),
        avg("Weekly_Sales").alias("Average_Weekly_Sales"),
        count("Store").alias("Data_Count")
      )

    // Cache department-level metrics for reuse
    val cachedDepartmentMetrics = departmentMetrics.persist(StorageLevel.MEMORY_AND_DISK_SER)
    println("Department-Level Metrics:")
    cachedDepartmentMetrics.show(10)

    // Department Wise Aggregated Metrics Storage in JSON Format
    val deptWiseAggregatedMetricsPath = s"gs://deva_vasadi/final_project/case_study_4/aggregated_metrics/department_wise"
    cachedDepartmentMetrics.write
      .mode(SaveMode.Overwrite)
      .json(deptWiseAggregatedMetricsPath)

    //Weekly Trends
    // Add a window partitioned by Store and Dept, ordered by Date
    val windowSpec = Window.partitionBy("Store", "Dept").orderBy("Date")

    // Calculate weekly trends (difference in sales from the previous week)
    val weeklyTrendsDF = partitionedEnrichedDF
      .withColumn("Previous_Weekly_Sales", lag("Weekly_Sales", 1).over(windowSpec))
      .withColumn("Weekly_Trend", col("Weekly_Sales") - col("Previous_Weekly_Sales"))
      .select("Store", "Dept", "Date", "Weekly_Sales", "IsHoliday", "Previous_Weekly_Sales" ,"Weekly_Trend")

    weeklyTrendsDF.show(10)

    // Additional Insights: Holiday vs. Non-Holiday Sales
    // Persist the holidaySales
    val holidaySales = partitionedEnrichedDF.filter("IsHoliday = true")
      .groupBy("Store", "Dept")
      .agg(sum("Weekly_Sales").alias("Holiday_Sales")).persist(StorageLevel.MEMORY_AND_DISK)

    val nonHolidaySales = partitionedEnrichedDF.filter("IsHoliday = false")
      .groupBy("Store", "Dept")
      .agg(sum("Weekly_Sales").alias("NonHoliday_Sales"))

    println("Holiday vs. Non-Holiday Sales:")
    val holidayComparison = holidaySales
      .join(nonHolidaySales, Seq("Store", "Dept"), "outer")
      .orderBy(desc("Holiday_Sales"))

    holidayComparison.show(10)

    // Department Wise Holiday vs Non-Holiday Sales Comparison Metrics Storage in JSON Format
    val holidayVsNonHolidayMetricsPath = s"gs://deva_vasadi/final_project/case_study_4/aggregated_metrics/holiday_vs_non_holiday"
    holidayComparison.write
      .mode(SaveMode.Overwrite)
      .json(holidayVsNonHolidayMetricsPath)
  }
}