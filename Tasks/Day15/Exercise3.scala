package Day15

import org.apache.spark.sql.SparkSession

object TaskExecutorAnalysis {
  def main(args: Array[String]): Unit = {
    // Step 1: Initialize SparkSession with 2 executors
    val spark = SparkSession.builder()
      .appName("Task Executor Analysis")
      .master("local[*]") // Simulates multiple executors in local mode
      .config("spark.executor.instances", "2") // Set 2 executors
      .getOrCreate()

    // Step 2: Create an RDD of strings with 1 million lines
    val textRDD = spark.sparkContext.parallelize(
      Seq.fill(1000000)("lorem ipsum dolor sit amet consectetur adipiscing elit"), numSlices = 8
    )

    // Step 3: Transformation pipeline
    // Split each line into words
    val wordsRDD = textRDD.flatMap(line => line.split(" "))

    // Map each word to a tuple (word, 1)
    val wordPairsRDD = wordsRDD.map(word => (word, 1))

    // Reduce by key to count word occurrences
    val wordCountsRDD = wordPairsRDD.reduceByKey(_ + _)

    // Step 4: Trigger the computation and collect the results
    wordCountsRDD.collect().foreach { case (word, count) =>
      println(s"$word: $count")
    }

    scala.io.StdIn.readLine()

    // Stop the SparkSession
    spark.stop()
  }
}
