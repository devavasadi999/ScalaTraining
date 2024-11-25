package Day15

import org.apache.spark.sql.SparkSession

import java.io.{BufferedWriter, FileWriter}
import scala.util.Random

object PartitioningImpact {

  def main(args: Array[String]): Unit = {

    // Configuration: Number of rows and file path
    val numRows = 1000000  // Adjust to create more rows
    val inputFilePath = "large_dataset.csv"

    // Generate the CSV file
    generateLargeCSV(numRows, inputFilePath)

    println(s"Large CSV file with $numRows rows created at: $inputFilePath")

    def generateLargeCSV(numRows: Int, filePath: String): Unit = {
      val random = new Random()
      val writer = new BufferedWriter(new FileWriter(filePath))

      try {

        // Generate random rows
        for (i <- 1 to numRows) {
          val name = s"Name${random.alphanumeric.take(5).mkString}"
          val age = random.nextInt(60) + 20  // Random age between 20 and 80
          val salary = random.nextDouble() * 100000  // Random salary up to 100,000
          writer.write(s"$name,$age,$salary\n")
        }
      } finally {
        writer.close()
      }
    }

    // Step 1: Initialize SparkSession
    val spark = SparkSession.builder()
      .appName("Partitioning Impact on Performance")
      .master("local[*]") // Local mode for simulation
      .getOrCreate()

    // Step 2: Load a large dataset into an RDD
    // Replace with the path to your large CSV or JSON file
    val rdd = spark.sparkContext.textFile(inputFilePath)
    println(s"Initial number of partitions: ${rdd.getNumPartitions}")

    // Step 3: Define a function to perform tasks on different partition sizes
    def processWithPartitions(partitions: Int): Unit = {
      println(s"\n--- Processing with $partitions partitions ---")

      // Repartition the RDD
      val partitionedRDD = rdd.repartition(partitions)

      // Count the number of rows in the RDD
      val rowCount = partitionedRDD.count()
      println(s"Row count: $rowCount")

      // Sort the data (wide transformation)
      val sortedRDD = partitionedRDD.sortBy(line => line) // Sorting lexicographically

      // Write the sorted data back to disk
      val outputPath = s"output/sorted_data_$partitions"
      sortedRDD.saveAsTextFile(outputPath)
    }

    // Step 4: Process the RDD with different partition sizes
    processWithPartitions(2)  // Process with 2 partitions
    processWithPartitions(4)  // Process with 4 partitions
    processWithPartitions(8)  // Process with 8 partitions

    scala.io.StdIn.readLine()

    // Stop the SparkSession
    spark.stop()
  }
}
