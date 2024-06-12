import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.sql.functions._

object SalesAnalysis {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Sales Analysis")
      .master("local[*]")
      .getOrCreate()

    val salesData = loadSalesData(spark, "/Users/vinodh/datasets/salesdata.csv")
    val filteredData = filterHighValueSales(spark, salesData, 500)
    val aggregatedData = aggregateSalesData(filteredData, "category")
    aggregatedData.show()
  }

  def loadSalesData(spark: SparkSession, path: String): DataFrame = {
    spark.read.option("header", "true").option("inferSchema", "true").csv(path)
  }

  def filterHighValueSales(spark: SparkSession, df: DataFrame, threshold: Double): DataFrame = {
    import spark.implicits._
    df.filter($"amount" > threshold)
  }

  def aggregateSalesData(df: DataFrame, areaClean: String): DataFrame = {
    import df.sparkSession.implicits._  // Ensure implicits are available for DataFrame transformations
    df.groupBy(areaClean)
      .agg(sum("amount").alias("total_sales"), avg("amount").alias("avg_sales"))
      .sort(desc("total_sales"))
  }
}
