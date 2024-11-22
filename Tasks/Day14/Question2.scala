import org.apache.spark.sql.SparkSession

object CartesianProduct {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Cartesian Product")
      .master("local[*]")
      .getOrCreate()

    val rdd1 = spark.sparkContext.parallelize(Seq(1, 2, 3))
    val rdd2 = spark.sparkContext.parallelize(Seq(4, 5, 6))
    val cartesianProduct = rdd1.cartesian(rdd2)

    cartesianProduct.collect().foreach(println)
    spark.stop()
  }
}
