package case_study_1.UnitTests

import case_study_1.SensorDataAggregation
import case_study_1.SensorDataAggregation.{bucketName, descriptorFile, rawDataMessageType}
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.protobuf.functions.from_protobuf
import org.scalatest.funsuite.AnyFunSuite

class RawDataStorageTest extends AnyFunSuite {
  val spark: SparkSession = SparkSession.builder()
    .appName("RawDataStorageTest")
    .config("spark.hadoop.fs.defaultFS", "gs://deva_vasadi/")
    .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
    .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
    .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
    .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._

  test("Raw data should be appended correctly to GCS") {
    val testPath = s"gs://$bucketName/case_study_1/test_data/raw_data"
    val fs = FileSystem.get(spark.sparkContext.hadoopConfiguration)
    if (fs.exists(new Path(testPath))) fs.delete(new Path(testPath), true)

    val rawData = Seq(
      ("Sensor-1", 1672531200000L, 25.5f, 60.0f),
      ("Sensor-2", 1672531300000L, 30.0f, 50.0f)
    ).toDF("sensorId", "timestamp", "temperature", "humidity")

    SensorDataAggregation.storeRawData(rawData, testPath)

    val writtenData = spark.read.format("avro").load(testPath)
      .select(from_protobuf($"value", rawDataMessageType, descriptorFile).alias("data"))
      .select("data.*")

    assert(writtenData.count() == 2)
    assert(writtenData.columns.contains("sensorId"))
  }
}
