package Day15

import org.apache.spark.sql.SparkSession

object NarrowWideTransformations {
  def main(args: Array[String]): Unit = {
    // Step 1: Initialize SparkSession
    val spark = SparkSession.builder()
      .appName("Narrow vs Wide Transformations")
      .master("local[*]")
      .getOrCreate()

    // Step 2: Create an RDD of numbers from 1 to 1000
    val numbersRDD = spark.sparkContext.parallelize(1 to 1000, numSlices = 4)

    // Step 3: Apply narrow transformations
    val mappedRDD = numbersRDD.map(_ * 2) // Multiply each number by 2
    val filteredRDD = mappedRDD.filter(_ % 3 == 0) // Keep only numbers divisible by 3

    // Step 4: Apply a wide transformation
    val pairedRDD = filteredRDD.map(num => (num % 10, num)) // Key: num % 10, Value: num
    val groupedRDD = pairedRDD.groupByKey() // Group by key (causes a shuffle)

    // Step 5: Save the results to a text file
    val outputPath = "output/grouped_result"
    groupedRDD.saveAsTextFile(outputPath)

    // Step 6: Load the saved data back
    val loadedRDD = spark.sparkContext.textFile(outputPath)

    // Print loaded data for reference
    println("\n--- Loaded Data ---")
    loadedRDD.take(10).foreach(println)

    // Step 7: Perform map and filter transformations
    val mappedAgainRDD = loadedRDD.map(line => line.toUpperCase) // Example: Convert lines to uppercase
    val filteredAgainRDD = mappedAgainRDD.filter(line => line.contains("5")) // Keep lines containing '5'

    // Print the results of the transformations
    println("\n--- Mapped Again Results ---")
    mappedAgainRDD.take(10).foreach(println)

    println("\n--- Filtered Again Results ---")
    filteredAgainRDD.take(10).foreach(println)

    scala.io.StdIn.readLine()

    // Stop the SparkSession
    spark.stop()
  }
}
