import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.protobuf.functions.from_protobuf

object RetrieveAndDisplay {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("RetrieveAndDisplayProtobufData")
      .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem")
      .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS")
      .config("spark.hadoop.google.cloud.auth.service.account.enable", "true")
      .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", "/Users/devavasadi/Documents/gcp-final-key.json")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Path to Avro data and Protobuf descriptor file
    val avroDataPath = "gs://deva_vasadi/raw/sensor-data"
    val descriptorFile = "src/main/scala/case_study_1/descriptor/SensorReadings.desc"
    val rawDataMessageType = "case_study_1.RawSensorReading"

    // Read Avro data
    val avroDF = spark.read
      .format("avro")
      .load(avroDataPath)

    // Deserialize Protobuf data from the "value" column
    val deserializedDF = avroDF
      .select(
        from_protobuf($"value", rawDataMessageType, descriptorFile).alias("sensorReading")
      )
      .select("sensorReading.*") // Flatten the struct to display individual fields

    // Display the data
    deserializedDF.show()
  }
}
