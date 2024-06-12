import org.apache.spark.sql.SparkSession

object WriteToKeyspaces extends App {

  val spark = SparkSession.builder
    .appName("Secure AWS Keyspaces Connection")
    .master("local[*]")
    .config("spark.cassandra.connection.host","cassandra.ap-south-1.amazonaws.com")
    .config("spark.cassandra.connection.port", "9142")
    .config("spark.cassandra.connection.ssl.enabled", "true")
    .config("spark.cassandra.auth.username", "vinodh-at-961286845363")
    .config("spark.cassandra.auth.password", "jqILOGN6EAPtchyiiPXZHutmaYxZPGO8D8B/upzpvPqrVB5JOkLMe6NCH1k=")
    .config("spark.cassandra.input.consistency.level", "LOCAL_QUORUM")
    .config("spark.cassandra.connection.ssl.trustStore.path", "/Users/vinodh/cassandra_truststore.jks")
    .config("spark.cassandra.connection.ssl.trustStore.password", "vinodh")
    .getOrCreate()

  // Define a case class corresponding to your schema
  case class Person(sno: Int, name: String, city: String)

  import spark.implicits._

  // Create a DataFrame
  val people = Seq(
    Person(3, "Roger", "New York"),
    Person(4, "Jasnon", "Mumbai")
  ).toDF()

  // Write data to AWS Keyspaces
  people.write
    .format("org.apache.spark.sql.cassandra")
    .options(Map("table" -> "people", "keyspace" -> "tutorialkeyspace"))
    .mode("append")
    .save()

  spark.stop()
}
