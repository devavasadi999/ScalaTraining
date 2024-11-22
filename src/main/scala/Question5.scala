import org.apache.spark.sql.SparkSession

object AverageScore {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Average Score")
      .master("local[*]")
      .getOrCreate()

    val scoresRDD = spark.sparkContext.parallelize(Seq((1, 90), (2, 80), (3, 70)))
    val (totalScore, count) = scoresRDD.map(_._2).aggregate((0, 0))(
      (acc, value) => (acc._1 + value, acc._2 + 1),
      (acc1, acc2) => (acc1._1 + acc2._1, acc1._2 + acc2._2)
    )

    val average = totalScore.toDouble / count
    println(s"Average score: $average")
    spark.stop()
  }
}
