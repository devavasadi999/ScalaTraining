import org.apache.spark.sql.SparkSession

object JoinRDDs {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Join RDDs")
      .master("local[*]")
      .getOrCreate()

    val namesRDD = spark.sparkContext.parallelize(Seq((1, "Alice"), (2, "Bob"), (3, "Carol")))
    val scoresRDD = spark.sparkContext.parallelize(Seq((1, 95), (2, 85), (3, 75)))

    // Join the RDDs
    val joinedRDD = namesRDD.join(scoresRDD)

    // Transform to (id, name, score)
    val resultRDD = joinedRDD.map { case (id, (name, score)) => (id, name, score) }

    // Collect and print the result
    resultRDD.collect().foreach(println)

    spark.stop()
  }
}
