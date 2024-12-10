import scala.collection.immutable.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

// Ensure compatibility with Spark 3.2.x
scalaVersion := "2.12.13"

// Define Spark version
val sparkVersion = "3.5.1"

// Define Akka version
val akkaVersion = "2.6.20"

// Define Akka Stream Kafka version
val akkaKafkaVersion = "2.1.0"

// Define ScalaPB version
val scalaPBVersion = "0.11.12"

libraryDependencies ++= Seq(
  // Spark dependencies
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion,
  "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkVersion,
  "org.apache.spark" %% "spark-avro" % sparkVersion,

  // Akka dependencies
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % akkaKafkaVersion,

  // Kafka dependencies
  "org.apache.kafka" % "kafka-clients" % "3.4.0",

  // Spark Protobuf module
  "org.apache.spark" %% "spark-protobuf" % sparkVersion,

  // Protobuf runtime dependency
  "com.google.protobuf" % "protobuf-java" % "3.24.3",

  // ScalaPB dependencies
  "com.thesamet.scalapb" %% "scalapb-runtime" % scalaPBVersion, // ScalaPB runtime
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalaPBVersion, // For gRPC support

  // Other dependencies
  "org.scalatest" %% "scalatest" % "3.2.17" % Test, // Updated to the latest version
  "com.datastax.spark" %% "spark-cassandra-connector" % "3.0.1",
  "com.github.jnr" % "jnr-posix" % "3.1.7",
  "joda-time" % "joda-time" % "2.12.5", // Updated version
  "com.google.cloud.bigdataoss" % "gcs-connector" % "hadoop3-2.2.5"
)

dependencyOverrides ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0"
)
