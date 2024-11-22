import org.apache.spark.sql.SparkSession

object WordCount {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Word Count")
      .master("local[*]")
      .getOrCreate()

    val stringsRDD = spark.sparkContext.parallelize(Seq("hello world", "spark RDD example", "count words"))
    val wordsRDD = stringsRDD.flatMap(_.split(" "))
    val wordCount = wordsRDD.count()

    println(s"Total words: $wordCount")
    spark.stop()
  }
}
