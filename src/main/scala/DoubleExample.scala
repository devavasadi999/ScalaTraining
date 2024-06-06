import org.apache.spark.sql.SparkSession

object DoubleExample {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Double Numbers")
      .master("local[*]")
      .getOrCreate()

    val numbersRDD = spark.sparkContext.parallelize(Seq(1, 2, 3, 4, 5))
    val doubledRDD = numbersRDD.map(_ * 2)

    doubledRDD.collect().foreach(println)
    spark.stop()
  }
}