import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window

object DepartmentWithEmployees {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Department Employees JSON Output")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Example DataFrame for Departments
    val departments = Seq(
      (1, "HR"),
      (2, "Engineering"),
      (3, "Marketing")
    ).toDF("department_id", "department_name")

    // Example DataFrame for Employees
    val employees = Seq(
      (101, "Alice", 1),
      (102, "Bob", 1),
      (103, "Charlie", 2),
      (104, "David", 3),
      (105, "Eve", 2)
    ).toDF("employee_id", "employee_name", "department_id")

    // Joining DataFrames on department_id
    val departmentWithEmployees = departments
      .join(employees, "department_id")
      .withColumn("employee", struct("employee_id", "employee_name"))

    // Using Window to collect employees without a groupBy
    val windowSpec = Window.partitionBy("department_id").orderBy("department_id")
    val result = departmentWithEmployees
      .withColumn("employees", collect_list("employee").over(windowSpec))
      .select("department_id", "department_name", "employees")
      .dropDuplicates("department_id")

    // Convert the result to JSON and show
    result.toJSON.show(false)

    // Optional: Write the result to a JSON file (uncomment to use)
    // result.write.json("path/to/save/json_files")

    // Stop the Spark session
    spark.stop()
  }
}
