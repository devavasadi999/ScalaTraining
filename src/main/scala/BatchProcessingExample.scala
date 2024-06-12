import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object BatchProcessingExample {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder
      .appName("BatchProcessingExample")
      .master("local[2]")  // Use local mode with 2 threads, adjust as needed
      .getOrCreate()

    // Load sales data
    val sales = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("/Users/vinodh/datasets/sales.csv")

    // Load product data
    val products = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("/Users/vinodh/datasets/products.csv")

    // Load customer data
    val customers = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("/Users/vinodh/datasets/customers.csv")

    // Aggregate sales by productId and customerId
    val salesAggregated = sales.groupBy("productId", "customerId")
      .agg(sum("amount").alias("totalSales"))

    // Join aggregated sales with product data
    val productSales = salesAggregated
      .join(products, "productId")

    // Join with customer data
    val detailedSalesReport = productSales
      .join(customers, "customerId")

    // Show the result in the console
    detailedSalesReport.show()

    // Write the result to JSON
  /*  detailedSalesReport.write
      .json("/Users/vinodh/datasets/outputjson")*/
  }
}
