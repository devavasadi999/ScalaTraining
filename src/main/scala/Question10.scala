import org.apache.spark.sql.SparkSession

object GroupByKeySum {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Group By Key Sum")
      .master("local[*]")
      .getOrCreate()

    val keyValueRDD = spark.sparkContext.parallelize(Seq(("a", 1), ("b", 2), ("a", 3), ("b", 4)))
    val groupedSumRDD = keyValueRDD.reduceByKey(_ + _)

    groupedSumRDD.collect().foreach(println)
    spark.stop()
  }
}
