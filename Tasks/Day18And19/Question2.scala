import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import scala.util.Random

object SparkCachingExample {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Spark Caching Example")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Generate a sample DataFrame (sales data)
    val salesData = (1 to 1000000).map { _ =>
      val product = s"Product${Random.nextInt(100)}"
      val quantity = Random.nextInt(10) + 1
      val price = Random.nextDouble() * 100
      (product, quantity, price)
    }.toDF("product", "quantity", "price")

    // Without Caching
    val startTimeWithoutCache = System.nanoTime()

    val filteredDF = salesData.filter($"price" > 0).filter($"price" > 1)
      .filter($"price" > 80)
    val filteredCount = filteredDF.count()

    val totalQuantityWithoutCache = filteredDF
      .groupBy("product")
      .agg(sum("quantity").as("total_quantity"))
      .count()

    println(s"Total products (without cache): $totalQuantityWithoutCache")
    val endTimeWithoutCache = System.nanoTime()

    // With Caching
    val startTimeWithCache = System.nanoTime()
    val cachedFilteredDF = salesData.filter($"price" > 0).filter($"price" > 1)
      .filter($"price" > 80).cache()
    val cachedFilteredCount = cachedFilteredDF.count()

    val totalQuantityWithCache = cachedFilteredDF
      .groupBy("product")
      .agg(sum("quantity").as("total_quantity"))
      .count()
    println(s"Total products (with cache): $totalQuantityWithCache")
    val endTimeWithCache = System.nanoTime()

    // Compare execution times
    println(s"Execution time without cache: ${(endTimeWithoutCache - startTimeWithoutCache) / 1e9} seconds")
    println(s"Execution time with cache: ${(endTimeWithCache - startTimeWithCache) / 1e9} seconds")

    // Stop Spark session
    spark.stop()
  }
}
