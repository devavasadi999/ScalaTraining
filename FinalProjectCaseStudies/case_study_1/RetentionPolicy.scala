import org.apache.spark.sql.SparkSession
import org.apache.hadoop.fs.{FileSystem, Path}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import akka.actor.ActorSystem
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object RetentionPolicy {

  def main(args: Array[String]): Unit = {
    // Initialize Akka system
    implicit val system: ActorSystem = ActorSystem("RetentionPolicyScheduler")
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    // Schedule the task to run once a day
    val interval = 1.days
    val initialDelay = 0.seconds
    system.scheduler.scheduleWithFixedDelay(initialDelay, interval) { () =>
      runRetentionPolicy()
    }
  }

  def runRetentionPolicy(): Unit = {
    // Initialize SparkSession
    val spark = SparkSession.builder()
      .appName("RetentionPolicy")
      .config("spark.hadoop.fs.defaultFS", "gs://deva_vasadi/")
      .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
      .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
      .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
      .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
      .master("local[*]") // Use local for testing
      .getOrCreate()

    // Raw data base path
    val rawDataBasePath = "gs://deva_vasadi/raw/sensor-data"

    // Get the current time
    val currentTime = LocalDateTime.now()

    // Calculate cutoff time (7 days ago)
    val cutoffTime = currentTime.minus(7, ChronoUnit.DAYS)

    // Format for directories (yyyy/MM/dd/HH)
    val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd/HH")

    // Hadoop FileSystem
    val fs = FileSystem.get(spark.sparkContext.hadoopConfiguration)

    // List all year directories
    val yearPaths = fs.listStatus(new Path(rawDataBasePath)).map(_.getPath)

    // Loop through directories to check their timestamps
    yearPaths.foreach { yearPath =>
      fs.listStatus(yearPath).foreach { monthPath =>
        fs.listStatus(monthPath.getPath).foreach { dayPath =>
          fs.listStatus(dayPath.getPath).foreach { hourPath =>
            val fullPath = s"${yearPath.getName}/${monthPath.getPath.getName}/${dayPath.getPath.getName}/${hourPath.getPath.getName}"
            val folderTimestamp = LocalDateTime.parse(fullPath, formatter)

            if (folderTimestamp.isBefore(cutoffTime)) {
              println(s"Deleting folder: ${hourPath.getPath}")
              fs.delete(hourPath.getPath, true) // Recursive delete
            }
          }
        }
      }
    }

    spark.stop()
  }
}
