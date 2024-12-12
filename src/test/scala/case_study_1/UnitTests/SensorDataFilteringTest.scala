package case_study_1.UnitTests

import case_study_1.SensorDataAggregation
import org.apache.spark.sql.SparkSession
import org.scalatest.funsuite.AnyFunSuite

class SensorDataFilteringTest extends AnyFunSuite {
  val spark: SparkSession = SparkSession.builder()
    .appName("SensorDataFilteringTest")
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._

  test("Sensor data should be filtered properly") {
    // Simulated Kafka input data
    val kafkaInput = Seq(
      ("Sensor-1", 1672531200000L, 25.5f, 60.0f), // Valid data
      ("Sensor-2", 1672531300000L, 30.0f, 50.0f), // Valid data
      ("Sensor-3", 1672531400000L, -100.0f, 110.0f) // Invalid data
    ).toDF("sensorId", "timestamp", "temperature", "humidity")

    // Expected output based on filtering logic since only first two records are valid
    val expectedOutput = kafkaInput.limit(2)

    // Simulated consumer processing (actual logic being tested)
    val result = SensorDataAggregation.filterRecords(kafkaInput)

    // Compare the result with the hardcoded expected output
    assert(result.collect() sameElements expectedOutput.collect(), "Filtered data does not match the expected output")
  }
}
