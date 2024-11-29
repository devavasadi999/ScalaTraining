ThisBuild / scalaVersion := "2.13.15"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "kafka-protobuf-scala",
    resolvers ++= Seq(
      "Akka Repository" at "https://repo.akka.io/maven/",
      "Confluent" at "https://packages.confluent.io/maven/"
    ),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.6.20",
      "com.typesafe.akka" %% "akka-stream" % "2.6.20",
      "com.typesafe.akka" %% "akka-stream-kafka" % "2.1.0",
      "io.confluent" % "kafka-protobuf-serializer" % "7.7.1",
      "com.thesamet.scalapb" %% "scalapb-runtime" % "0.11.6" // Needed for runtime
    )
  )
