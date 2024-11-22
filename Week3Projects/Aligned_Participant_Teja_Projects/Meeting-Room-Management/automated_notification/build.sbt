ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.4"

lazy val root = (project in file("."))
  .settings(
    name := "Automated_Notifications"
  )

// Dependencies
libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % "3.0.0",
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.19",
  "com.typesafe.akka" %% "akka-stream" % "2.6.19",
  "com.typesafe.play" %% "play-json" % "2.9.2" cross CrossVersion.for3Use2_13,
  "ch.qos.logback" % "logback-classic" % "1.2.10"
)

// Resolvers
resolvers += "Akka Snapshot Repository" at "https://repo.akka.io/snapshots/"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % "0.14.1",
  "io.circe" %% "circe-generic" % "0.14.1",
  "io.circe" %% "circe-parser" % "0.14.1",
  "com.sun.mail" % "javax.mail" % "1.6.2",
)


