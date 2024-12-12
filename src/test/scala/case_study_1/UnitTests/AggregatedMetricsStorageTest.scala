package case_study_1.UnitTests

import case_study_1.SensorDataAggregation
import case_study_1.SensorDataAggregation.bucketName
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.SparkSession
import org.scalatest.funsuite.AnyFunSuite

class AggregatedMetricsStorageTest extends AnyFunSuite {
  val spark: SparkSession = SparkSession.builder()
    .appName("AggregatedMetricsStorageTest")
    .config("spark.hadoop.fs.defaultFS", "gs://deva_vasadi/")
    .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
    .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
    .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
    .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._

  test("Aggregated metrics should be saved in Protobuf and JSON formats") {
    val aggregatedData = Seq(
      ("Sensor-1", 27.5f, 62.5f, 25.5f, 30.0f, 60.0f, 65.0f, 4),
      ("Sensor-3", 29.0f, 63.0f, 19.5f, 32.0f, 61.5f, 67.0f, 4)
    ).toDF("sensorId", "avgTemperature", "avgHumidity", "minTemperature", "maxTemperature", "minHumidity", "maxHumidity", "dataCount")

    val protobufPath = s"gs://$bucketName/case_study_1/test_data/aggregated_data/protobuf"
    val jsonPath = s"gs://$bucketName/case_study_1/test_data/aggregated_data/json"

    val fs = FileSystem.get(spark.sparkContext.hadoopConfiguration)
    if (fs.exists(new Path(protobufPath))) fs.delete(new Path(protobufPath), true)
    if (fs.exists(new Path(jsonPath))) fs.delete(new Path(jsonPath), true)

    SensorDataAggregation.storeAggregatedMetrics(aggregatedData, protobufPath, jsonPath)

    // Verify Protobuf
    assert(fs.exists(new Path(protobufPath)))
    val protobufData = spark.read.format("avro").load(protobufPath)
    assert(protobufData.count() == 2)

    // Verify JSON
    assert(fs.exists(new Path(jsonPath)))
    val jsonData = spark.read.format("json").load(jsonPath)
    assert(jsonData.count() == 2)
  }
}
