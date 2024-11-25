package Day15

import org.apache.spark.sql.SparkSession

object ExploringDAG {
  def main(args: Array[String]): Unit = {
    // Step 1: Initialize SparkSession
    val spark = SparkSession.builder()
      .appName("Exploring DAG and Spark UI")
      .master("local[*]") // Local mode
      .getOrCreate()

    // Step 2: Create an RDD of integers from 1 to 10,000
    val numbersRDD = spark.sparkContext.parallelize(1 to 10000, numSlices = 4)

    // Step 3: Perform transformations
    // Step 3.1: Filter to keep only even numbers
    val evenNumbersRDD = numbersRDD.filter(_ % 2 == 0)

    // Step 3.2: Map to multiply each number by 10
    val multipliedRDD = evenNumbersRDD.map(_ * 10)

    // Step 3.3: Map to generate a tuple (remainder when dividing by 100, number itself)
    val keyValueRDD = multipliedRDD.map(num => (num % 100, num))

    // Step 3.4: ReduceByKey to sum values for each key
    val reducedRDD = keyValueRDD.reduceByKey(_ + _)

    // Step 4: Perform an action to collect the results
    val results = reducedRDD.collect()

    // Step 5: Display the results
    println("Results (key -> sum):")
    results.foreach { case (key, sum) =>
      println(s"$key -> $sum")
    }

    scala.io.StdIn.readLine()

    // Stop the SparkSession
    spark.stop()
  }
}
