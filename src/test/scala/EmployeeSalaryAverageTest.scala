import org.apache.spark.sql.SparkSession
import org.scalatest.funsuite.AnyFunSuite
import EmployeeSalaryAverage.computeAverageSalaryByDepartment

class EmployeeSalaryAverageTest extends AnyFunSuite {
  val spark: SparkSession = SparkSession.builder()
    .appName("Employee Average Salary Test")
    .master("local[*]")
    .getOrCreate()

  import spark.implicits._

  test("computeAverageSalaryByDepartment should return correct averages") {
    // Prepare sample data
    val employeeData = Seq(
      ("Alice", "Engineering", 50000),
      ("Bob", "Engineering", 55000),
      ("Charlie", "HR", 40000),
      ("David", "HR", 45000),
      ("Eve", "IT", 60000)
    ).toDF("name", "department", "salary")

    // Perform transformation
    val result = computeAverageSalaryByDepartment(employeeData)

    // Expected DataFrame
    val expectedData = Seq(
      ("IT", 60000.0),
      ("Engineering", 52500.0),
      ("HR", 42500.0)
    ).toDF("department", "average_salary")

    // Assertion to check if the transformation result matches expected result
    assert(result.collect() sameElements expectedData.collect(), "The results do not match the expected output.")
  }
}
