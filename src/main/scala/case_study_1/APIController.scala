package case_study_1

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.{ActorMaterializer, Materializer}
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.{DataFrame, SparkSession}

import java.time.{LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}
import org.apache.spark.sql.functions._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import akka.http.scaladsl.model.HttpMethods._

object AggregatedDataApi {

  implicit val system: ActorSystem = ActorSystem("AggregatedDataApi")
  implicit val materializer: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val aggregatedDataJsonPath = "gs://deva_vasadi/aggregated/json"

  val spark = SparkSession.builder()
    .appName("AggregatedDataApi")
    .config("spark.hadoop.fs.defaultFS", "gs://deva_vasadi/")
    .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
    .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
    .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
    .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
    .master("local[*]")
    .getOrCreate()

  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.server.Route

  val corsSettings = CorsSettings.defaultSettings
    .withAllowedOrigins(_ => true) // Allow all origins
    .withAllowedMethods(scala.collection.immutable.Seq(GET, POST, PUT, DELETE, OPTIONS)) // Allow required HTTP methods
    .withAllowCredentials(false) // If cookies/auth headers are not required, keep false
    .withExposedHeaders(scala.collection.immutable.Seq("Content-Type", "Authorization")) // Expose necessary headers

  val route: Route = cors(corsSettings) {
    options {
      complete(StatusCodes.OK) // Respond OK to preflight requests
    } ~
      concat(
        path("api" / "aggregated-data") {
          get {
            complete(HttpEntity(ContentTypes.`application/json`, fetchAllAggregatedData(spark)))
          }
        },
        path("api" / "aggregated-data" / Segment) { sensorId =>
          get {
            complete(HttpEntity(ContentTypes.`application/json`, fetchAggregatedDataBySensorId(spark, sensorId)))
          }
        }
      )
  }

  def main(args: Array[String]): Unit = {

    Http().newServerAt("localhost", 8080).bind(route).onComplete {
      case Success(binding) =>
        println(s"Server started at ${binding.localAddress}")
      case Failure(exception) =>
        println(s"Failed to bind server: ${exception.getMessage}")
        system.terminate()
    }
  }

  // Fetch aggregated data for all folders (only the latest hour folder)
  def fetchAllAggregatedData(spark: SparkSession): String = {
    val fs = FileSystem.get(spark.sparkContext.hadoopConfiguration)
    val latestFolderPath = getOptimizedLatestFolder(spark)

    latestFolderPath match {
      case Some(folderPath) =>
        loadFolderData(spark, folderPath)
          .toJSON
          .collect()
          .mkString("[", ",", "]")
      case None =>
        "[]" // Return empty JSON array if no folder exists
    }
  }

  // Fetch aggregated data for a specific sensorId (only the latest hour folder)
  def fetchAggregatedDataBySensorId(spark: SparkSession, sensorId: String): String = {
    import spark.implicits._

    val latestFolderPath = getOptimizedLatestFolder(spark)

    latestFolderPath match {
      case Some(folderPath) =>
        loadFolderData(spark, folderPath)
          .filter($"sensorId" === sensorId)
          .toJSON
          .collect()
          .mkString("[", ",", "]")
      case None =>
        "[]" // Return empty JSON array if no folder exists
    }
  }

  // Optimized function to get the latest folder path
  def getOptimizedLatestFolder(spark: SparkSession): Option[String] = {
    val fs = FileSystem.get(spark.sparkContext.hadoopConfiguration)

    val basePathObj = new Path(aggregatedDataJsonPath)
    if (!fs.exists(basePathObj)) return None

    // Step-by-step traversal
    val yearDirs = fs.listStatus(basePathObj).map(_.getPath)
    if (yearDirs.isEmpty) return None
    val maxYear = yearDirs.map(_.getName.toInt).max

    val monthDirs = fs.listStatus(new Path(s"$aggregatedDataJsonPath/$maxYear")).map(_.getPath)
    if (monthDirs.isEmpty) return None
    val maxMonth = monthDirs.map(_.getName.toInt).max

    val dayDirs = fs.listStatus(new Path(s"$aggregatedDataJsonPath/$maxYear/${f"$maxMonth%02d"}")).map(_.getPath)
    if (dayDirs.isEmpty) return None
    val maxDay = dayDirs.map(_.getName.toInt).max

    val hourDirs = fs.listStatus(new Path(s"$aggregatedDataJsonPath/$maxYear/${f"$maxMonth%02d"}/${f"$maxDay%02d"}")).map(_.getPath)
    if (hourDirs.isEmpty) return None
    val maxHour = hourDirs.map(_.getName.toInt).max

    Some(s"$aggregatedDataJsonPath/$maxYear/${f"$maxMonth%02d"}/${f"$maxDay%02d"}/${f"$maxHour%02d"}")
  }

  // Load data from a specific folder
  def loadFolderData(spark: SparkSession, folderPath: String): DataFrame = {
    spark.read.format("json").load(folderPath)
  }

}
