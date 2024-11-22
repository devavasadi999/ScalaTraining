import org.apache.spark.sql.SparkSession

object SumIntegers {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Sum Integers")
      .master("local[*]")
      .getOrCreate()

    val numbersRDD = spark.sparkContext.parallelize(1 to 100)
    val sum = numbersRDD.reduce(_ + _)

    println(s"Sum: $sum")
    spark.stop()
  }
}
