import org.apache.spark.sql.SparkSession

object SimpleDataProcessor {
  def filterAndCountEvenNumbers(numbers: Seq[Int], spark: SparkSession): Long = {
    import spark.implicits._
    val nums = spark.sparkContext.parallelize(numbers)
    nums.filter(_ % 2 == 0).count()
  }
}
