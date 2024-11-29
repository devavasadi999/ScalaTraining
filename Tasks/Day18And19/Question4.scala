import org.apache.spark.sql.SparkSession
import scala.util.Random

object WriteLargeParquetToGCS {
  def main(args: Array[String]): Unit = {
    // Initialize SparkSession with GCS configurations
    val spark = SparkSession.builder()
      .appName("Write Large Parquet to GCS")
      .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
      .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
      .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
      .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
      .master("local[*]") // Adjust as per your environment
      .getOrCreate()

    import spark.implicits._

    // Generate 100,000 rows dynamically
    val random = new Random()
    val data = (1 to 100000).map { id =>
      val name = s"User$id"
      val status = if (random.nextBoolean()) "completed" else "pending"
      val amount = random.nextDouble() * 1000 // Random amount between 0 and 1000
      (id, name, status, amount)
    }

    // Create a DataFrame with the schema
    val df = data.toDF("id", "name", "status", "amount")

    // Output Path on GCS
    val outputPath = "gs://deva_vasadi/input/data.parquet"

    // Write DataFrame as a Parquet file to GCS
    df.write
      .mode("overwrite") // Overwrite existing data
      .parquet(outputPath)

    println(s"Parquet file with 100,000 rows successfully written to $outputPath")

    // Stop SparkSession
    spark.stop()
  }
}

import org.apache.spark.sql.SparkSession

object GCSParquetProcessing {
  def main(args: Array[String]): Unit = {
    // Initialize SparkSession with GCS configurations
    val spark = SparkSession.builder()
      .appName("GCS Parquet Processing")
      .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
      .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
      .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
      .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
      .master("local[*]") // Adjust for your environment
      .getOrCreate()

    import spark.implicits._

    // Paths to input and output files
    val inputPath = "gs://deva_vasadi/input/data.parquet"
    val outputPath = "gs://deva_vasadi/output/processed_data.parquet"

    // Read Parquet file from GCS
    val inputData = spark.read.parquet(inputPath)
    inputData.show(10)

    // Process data: Filter rows where status = "completed"
    val processedData = inputData.filter($"status" === "completed")
    processedData.show(10)

    // Write the processed data back to GCS in Parquet format
    processedData.write.mode("overwrite").parquet(outputPath)

    println(s"Processed data successfully written to $outputPath")

    // Stop SparkSession
    spark.stop()
  }
}
