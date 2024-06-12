import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.sql.functions._
object JsonToCsvProcessor {

  def filterUsersByAge(df: DataFrame, minAge: Int): DataFrame = {
    import df.sparkSession.implicits._

    df.filter($"age" >= minAge)
  }

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("JSON to CSV Processor")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Example JSON path and output CSV path
    val jsonPath = "/Users/vinodh/datasets/people.json"
    val outputPath = "/Users/vinodh/datasets/csvoutput"

    // Read JSON data
    val userData = spark.read.json(jsonPath)

    // Filter data
    val filteredData = filterUsersByAge(userData, 30)

    // Write to CSV
    filteredData.write.option("header", "true").csv(outputPath)

    spark.stop()
  }
}
