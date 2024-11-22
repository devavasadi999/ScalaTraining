import scala.collection.Seq

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.15"

lazy val akkaVersion = "2.8.4"
lazy val akkaHttpVersion = "10.2.10" // Compatible with Akka 2.8.x
lazy val akkaStreamKafkaVersion = "2.1.1" // Latest available for Scala 2.13

lazy val root = (project in file("."))
  .settings(
    name := "notification-management-system",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.13",
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.15" % Test,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-kafka" % akkaStreamKafkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "org.apache.kafka" % "kafka-clients" % "3.7.0" ,// Kafka client
      "org.playframework" %% "play-slick"% "6.1.0",    // Enables to work with database
      "org.playframework" %% "play-slick-evolutions" % "6.1.0",    // Support for database migrations, similar to Flyway
      "mysql" % "mysql-connector-java" % "8.0.26",
      "com.sun.mail" % "javax.mail" % "1.6.2",
      "io.circe" %% "circe-core" % "0.14.1",
      "io.circe" %% "circe-generic" % "0.14.1",
      "io.circe" %% "circe-parser" % "0.14.1",
      "org.slf4j" % "slf4j-api" % "2.0.9",        // Ensure you use compatible SLF4J version
      "ch.qos.logback" % "logback-classic" % "1.3.5" // Compatible with SLF4J 2.x
    )
  )
