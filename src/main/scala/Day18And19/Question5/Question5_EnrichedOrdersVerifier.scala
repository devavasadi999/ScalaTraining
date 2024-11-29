package Day18And19.Question5

import org.apache.spark.sql.SparkSession

object ReadEnrichedOrdersFromGCS {
  def main(args: Array[String]): Unit = {
    // Initialize SparkSession with GCS configurations
    val spark = SparkSession.builder()
      .appName("Read Enriched Orders from GCS")
      .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
      .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
      .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
      .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
      .master("local[*]") // Adjust for your environment
      .getOrCreate()

    // Path to the enriched orders output in GCS
    val enrichedOrdersPath = "gs://deva_vasadi/output/enriched_orders/"

    // Read the JSON files from the specified GCS path
    val enrichedOrdersDF = spark.read
      .json(enrichedOrdersPath)

    // Show the data
    enrichedOrdersDF.show(10)

    println("Enriched orders successfully fetched.")
  }
}
