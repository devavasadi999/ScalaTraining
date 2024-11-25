package Day14

import org.apache.spark.sql.SparkSession

object UnionDistinct {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Union Distinct")
      .master("local[*]")
      .getOrCreate()

    val rdd1 = spark.sparkContext.parallelize(Seq(1, 2, 3))
    val rdd2 = spark.sparkContext.parallelize(Seq(3, 4, 5))
    val unionRDD = rdd1.union(rdd2).distinct()

    unionRDD.collect().foreach(println)
    spark.stop()
  }
}
