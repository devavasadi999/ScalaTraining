import org.apache.spark.sql.SparkSession
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class SimpleDataProcessorTest extends AnyFunSuite with BeforeAndAfter with Matchers {
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

  test("filterAndCountEvenNumbers returns correct counts") {
      // Now accessible due to the shared SparkSession trait

    val input = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val result = SimpleDataProcessor.filterAndCountEvenNumbers(input, spark)
    assert(result == 5, "Expected 5 even numbers")
  }
}
