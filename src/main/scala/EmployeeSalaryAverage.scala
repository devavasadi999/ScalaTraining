import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.DataFrame

object EmployeeSalaryAverage {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Employee Average Salary")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Sample data
    val employeeData = Seq(
      ("Alice", "Engineering", 50000),
      ("Bob", "Engineering", 55000),
      ("Charlie", "HR", 40000),
      ("David", "HR", 45000),
      ("Eve", "IT", 60000)
    ).toDF("name", "department", "salary")

    val result = computeAverageSalaryByDepartment(employeeData)
    result.show()
  }

  def computeAverageSalaryByDepartment(df: DataFrame): DataFrame = {
    df.groupBy("department")
      .agg(avg("salary").alias("average_salary"))
      .sort(desc("average_salary"))
  }
}
