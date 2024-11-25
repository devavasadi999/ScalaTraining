package Day14

import org.apache.spark.sql.SparkSession

object FilterEvenNumbers {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Filter Even Numbers")
      .master("local[*]")
      .getOrCreate()

    val numbersRDD = spark.sparkContext.parallelize(Seq(1, 2, 3, 4, 5, 6))
    val oddNumbersRDD = numbersRDD.filter(_ % 2 != 0)

    oddNumbersRDD.collect().foreach(println)
    spark.stop()
  }
}
