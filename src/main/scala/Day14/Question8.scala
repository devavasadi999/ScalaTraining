package Day14

import org.apache.spark.sql.SparkSession

object FilterAge {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Filter Age")
      .master("local[*]")
      .getOrCreate()

    // Read the CSV file into an RDD
    val csvFilePath = "Data.csv" // Replace with your CSV file name
    val csvRDD = spark.sparkContext.textFile(csvFilePath)

    // Filter rows based on age >= 18
    val filteredRDD = csvRDD.filter(row => {
      val age = row.split(",")(2).toInt
      age >= 18
    })

    // Collect and print the filtered rows
    filteredRDD.collect().foreach(println)

    spark.stop()
  }
}
