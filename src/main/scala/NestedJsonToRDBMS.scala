import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object NestedJsonToRDBMS {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Nested JSON to RDBMS")
      .master("local[*]")
      .getOrCreate()

    // For implicit conversions like converting RDDs to DataFrames
    import spark.implicits._

    // Load the JSON data
    val jsonPath = "/Users/vinodh/datasets/nested.json"
    val jsonData = spark.read.json(jsonPath)

    // Flatten the nested JSON data using implicits
    val flattenedData = jsonData.select(
      $"customer_id",
      $"name",
      $"email",
      explode($"orders").alias("order")
    ).select(
      $"customer_id",
      $"name",
      $"email",
      $"order.order_id".alias("order_id"),
      $"order.order_date".alias("order_date"),
      $"order.amount".alias("amount")
    )

    // Show the results
    flattenedData.show()

    // Define database connection parameters

  }
}
