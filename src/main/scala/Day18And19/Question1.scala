import org.apache.spark.sql.SparkSession

object GenerateAndUploadDataToGCS {
  def main(args: Array[String]): Unit = {
    // Initialize SparkSession with GCS configurations
    val spark = SparkSession.builder()
      .appName("Generate and Upload Data to GCS")
      .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
      .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
      .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
      .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Generate User Details (100,000 records)
    val userDetails = (1 to 100000).map(id => (id, s"User$id"))
    val userDetailsDF = spark.createDataFrame(userDetails).toDF("user_id", "name")

    // Generate Transaction Logs (1,000 records)
    val random = new scala.util.Random
    val transactionLogs = (1 to 1000).map { _ =>
      val userId = random.nextInt(100000) + 1
      val transactionType = if (random.nextBoolean()) "Purchase" else "Refund"
      val amount = 1.0 + random.nextDouble() * (1000.0 - 1.0)
      (userId, transactionType, amount)
    }
    val transactionLogsDF = spark.createDataFrame(transactionLogs).toDF("user_id", "transaction_type", "amount")

    // Paths to GCS
    val userDetailsPath = "gs://deva_vasadi/day18_19_tasks/user_details.csv"
    val transactionLogsPath = "gs://deva_vasadi/day18_19_tasks/tasks_transaction_logs.csv"

//    userDetailsDF.show(10)
//    transactionLogsDF.show(10)

    // Write data to GCS
    userDetailsDF.write
      .option("header", "true")
      .csv(userDetailsPath)

    transactionLogsDF.write
      .option("header", "true")
      .csv(transactionLogsPath)

    println(s"Data successfully written to GCS at $userDetailsPath and $transactionLogsPath")

    // Stop SparkSession
    spark.stop()
  }
}

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.broadcast
import scala.util.Random

object SparkBroadcastJoin {
  def main(args: Array[String]): Unit = {
    // Initialize SparkSession with GCS configurations
    val spark = SparkSession.builder()
      .appName("Broadcast Join with GCS")
      .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
      .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
      .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
      .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
      .master("local[*]") // Adjust master for your cluster
      .getOrCreate()

    val bucketName = "deva_vasadi"
    val userDetailsPath = s"gs://$bucketName/day18_19_tasks/user_details.csv"
    val transactionLogsPath = s"gs://$bucketName/day18_19_tasks/tasks_transaction_logs.csv"

    // Load userDetails and transactionLogs from GCS
    val userDetails = spark.read
      .option("header", "true") // Adjusted for datasets saved with headers
      .option("inferSchema", "true")
      .csv(userDetailsPath)
      .toDF("user_id", "name") // Rename columns for consistency if necessary

    val transactionLogs = spark.read
      .option("header", "true") // Adjusted for datasets saved with headers
      .option("inferSchema", "true")
      .csv(transactionLogsPath)
      .toDF("user_id", "transaction_type", "amount")

//    userDetails.show(10)
//    transactionLogs.show(10)

    // Perform Broadcast Join
    val joinedDF = transactionLogs
      .join(broadcast(userDetails), Seq("user_id"))

    // Show results (top 10 rows)
    joinedDF.show(10)

    // Optionally save the joined DataFrame to GCS for further use
//    val joinedDataPath = s"gs://$bucketName/day18_19_tasks/joined_data.csv"
//    joinedDF.write
//      .option("header", "true")
//      .csv(joinedDataPath)

//    println(s"Joined data successfully written to GCS at $joinedDataPath")

    // Stop Spark session
    spark.stop()
  }
}

