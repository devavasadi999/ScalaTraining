import org.apache.spark.sql.SparkSession
import org.scalatest.funsuite.AnyFunSuite
import SalesAnalysis._
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers

class SalesAnalysisTest extends AnyFunSuite with BeforeAndAfter with Matchers {
  var spark: SparkSession = _

  // Set up SparkSession before each test
  before {
    spark = SparkSession.builder()
      .master("local[*]")
      .appName("Spark Test")
      .getOrCreate()
  }

  // Tear down SparkSession after each test
  after {
    if (spark != null) {
      spark.stop()
    }
  }

  test("filterHighValueSales should filter sales above a given threshold") {
    val spark= this.spark
    import spark.implicits._
    val testData = Seq(
      (1, "Electronics", 400.0),
      (2, "Books", 550.0),
      (3, "Electronics", 800.0)
    ).toDF("id", "category", "amount")

    val result = filterHighValueSales(spark, testData, 500)
    val expectedData = Seq(
      (2, "Books", 550.0),
      (3, "Electronics", 800.0)
    ).toDF("id", "category", "amount")

    assert(result.collect() === expectedData.collect())
  }

  test("aggregateSalesData should return correct total and average sales per category") {
    val spark= this.spark
    import spark.implicits._
    val testData = Seq(
      (1, "Electronics", 1000.0),
      (2, "Books", 550.0),
      (3, "Electronics", 500.0)
    ).toDF("id", "category", "amount")

    val result = aggregateSalesData(testData, "category")
    val expectedData = Seq(
      ("Electronics", 1500.0, 750.0),  // Total sales, Avg sales
      ("Books", 550.0, 550.0)
    ).toDF("category", "total_sales", "avg_sales").sort($"total_sales".desc)

    assert(result.collect() === expectedData.collect())
  }
}
