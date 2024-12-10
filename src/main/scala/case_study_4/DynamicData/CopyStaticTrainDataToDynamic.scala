import org.apache.spark.sql.functions.col
import org.apache.spark.sql.{SaveMode, SparkSession}

object CopyStaticTrainDataToDynamic {
  val bucketName = "deva_vasadi"

  def main(args: Array[String]): Unit = {
    // Initialize SparkSession
    val spark = SparkSession.builder()
      .appName("CopyStaticTrainDataToDynamic")
      .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
      .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
      .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
      .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
      .master("local[*]")
      .getOrCreate()

    val updatedTrainPath = s"gs://$bucketName/final_project/case_study_4/updated_train.csv"

    val initialTrainPath = s"gs://$bucketName/final_project/case_study_4/train.csv"
    val initialTrainDF = spark.read
      .option("header", "true") // Adjusted for datasets saved with headers
      .option("inferSchema", "true")
      .csv(initialTrainPath)
    println(initialTrainDF.count())

    initialTrainDF.write.mode(SaveMode.Overwrite).option("header", "true").csv(updatedTrainPath)

//    val updatedTrainDF = spark.read
//      .option("header", "true") // Adjusted for datasets saved with headers
//      .option("inferSchema", "true")
//      .csv(updatedTrainPath)
//    println(updatedTrainDF.count())
//    println(updatedTrainDF.collect().takeRight(5).foreach(println))
//    updatedTrainDF.show(10)

//    val storeWiseAggregatedMetricsPath = s"gs://deva_vasadi/final_project/case_study_4/aggregated_metrics/store_wise"
//    val storeMetrics = spark.read
//      .option("inferSchema", "true")
//      .json(storeWiseAggregatedMetricsPath)

//    val departmentWiseAggregatedMetricsPath = s"gs://deva_vasadi/final_project/case_study_4/aggregated_metrics/department_wise"
//    val departmentMetrics = spark.read
//      .option("inferSchema", "true")
//      .json(departmentWiseAggregatedMetricsPath)

//    val holidayVsNonHolidayMetricsPath = s"gs://deva_vasadi/final_project/case_study_4/aggregated_metrics/holiday_vs_non_holiday"
//    val holidayComparison = spark.read
//      .option("inferSchema", "true")
//      .json(holidayVsNonHolidayMetricsPath)

    spark.stop()
  }
}