package Day15

import org.apache.spark.sql.SparkSession

object RDDPartitioning {
  def main(args: Array[String]): Unit = {
    // Initialize SparkSession
    val spark = SparkSession.builder()
      .appName("RDD Partitioning Exercise")
      .master("local[*]")
      .getOrCreate()

    // Generate a large RDD with random numbers
    val randomNumbersRDD = spark.sparkContext.parallelize(Seq.fill(2000000)(scala.util.Random.nextInt(1000)))
    println(s"Initial number of partitions: ${randomNumbersRDD.getNumPartitions}")

    // Repartition the RDD into 4 partitions
    val repartitionedRDD = randomNumbersRDD.repartition(4)
    println(s"Number of partitions after repartition: ${repartitionedRDD.getNumPartitions}")

    // Helper function to print the first 5 elements from each partition
    def printFirstElementsPerPartition[T](rdd: org.apache.spark.rdd.RDD[T]): Unit = {
      val partitionElements = rdd.mapPartitionsWithIndex {
        case (index, iterator) => Iterator((index, iterator.take(5).toList))
      }.collect()

      partitionElements.foreach { case (partitionIndex, elements) =>
        println(s"Partition $partitionIndex: ${elements.mkString(", ")}")
      }
    }

    // Coalesce the RDD into 2 partitions
    val coalescedRDD = repartitionedRDD.coalesce(2)
    println(s"Number of partitions after coalesce: ${coalescedRDD.getNumPartitions}")

    // Print first 5 elements from each partition after coalesce
    println("\nAfter coalesce (2 partitions):")
    printFirstElementsPerPartition(coalescedRDD)

    scala.io.StdIn.readLine()

    // Stop SparkSession
    spark.stop()
  }
}
