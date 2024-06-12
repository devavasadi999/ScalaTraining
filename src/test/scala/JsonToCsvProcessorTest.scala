import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.col
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.BeforeAndAfterAll

class JsonToCsvProcessorTest extends AnyFunSuite with BeforeAndAfterAll {
  var spark: SparkSession = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    spark = SparkSession.builder()
      .master("local[*]")
      .appName("Spark Tests")
      .getOrCreate()
  }

  override def afterAll(): Unit = {
    if (spark != null) {
      spark.stop()
    }
    super.afterAll()
  }

  test("filterUsersByAge should filter out users under the specified age") {

    val spark = this.spark





    // Attempt to import implicits right after SparkSession creation
    import spark.implicits._

    val sampleData = Seq(
      ("Alice", 25, "New York"),
      ("Bob", 35, "Los Angeles"),
      ("Charlie", 40, "Chicago")
    ).toDF("name", "age", "lesson")
    val result = sampleData.filter(col("age") >= 30)

    assert(result.count() === 2)
    assert(result.filter(col("name") === "Bob").count() === 1)
    assert(result.filter(col("name") === "Charlie").count() === 1)
  }
}
