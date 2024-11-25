package Day14

import org.apache.spark.sql.SparkSession

object CharacterFrequency {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Character Frequency")
      .master("local[*]")
      .getOrCreate()

    val stringsRDD = spark.sparkContext.parallelize(Seq("hello", "spark", "rdd"))
    val charRDD = stringsRDD.flatMap(_.toCharArray)
    val charFrequency = charRDD.map(char => (char, 1)).reduceByKey(_ + _)

    charFrequency.collect().foreach(println)
    spark.stop()
  }
}
