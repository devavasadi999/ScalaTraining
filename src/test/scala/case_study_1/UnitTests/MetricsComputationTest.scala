package case_study_1.UnitTests

import case_study_1.SensorDataAggregation
import org.apache.spark.sql.SparkSession
import org.scalatest.funsuite.AnyFunSuite

class MetricsComputationTest extends AnyFunSuite {
  val spark: SparkSession = SparkSession.builder()
    .appName("AggregatedMetricsTest")
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._

  test("Metrics should be aggregated correctly") {
    val rawData = Seq(
      ("Sensor-1", 1672531200000L, 18.5f, 60.0f),
      ("Sensor-1", 1672531200100L, 30.0f, 85.0f)
    ).toDF("sensorId", "timestamp", "temperature", "humidity")

    val existingAggregatedData = Seq(
      ("Sensor-1", 27.0f, 62.0f, 20.0f, 30.0f, 55.0f, 70.0f, 2)
    ).toDF("sensorId", "avgTemperature", "avgHumidity", "minTemperature", "maxTemperature", "minHumidity", "maxHumidity", "dataCount")

    val expectedAggregatedData = Seq(
      ("Sensor-1", 25.625f, 67.25f, 18.5f, 30.0f, 55.0f, 85.0f, 4)
    ).toDF("sensorId", "avgTemperature", "avgHumidity", "minTemperature", "maxTemperature", "minHumidity", "maxHumidity", "dataCount")

    val result = SensorDataAggregation.computeNewAggregatedMetrics(rawData, existingAggregatedData)
    assert(result.collect() sameElements expectedAggregatedData.collect())
  }
}
