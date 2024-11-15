name := """play-service-n-app"""
organization := "com.playapp"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.14"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.0" % Test
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.30"
libraryDependencies ++= Seq(
  "org.playframework" %% "play-slick"            % "6.1.0",
  "org.playframework" %% "play-slick-evolutions" % "6.1.0",
  "mysql" % "mysql-connector-java" % "8.0.26",
  "org.postgresql" % "postgresql" % "42.2.23",
  "org.apache.kafka" % "kafka-clients" % "2.8.0" // Add Kafka clients dependency
)
libraryDependencies += ws

// Uncomment if additional Twirl or Routes imports are needed
// TwirlKeys.templateImports += "com.playapp.controllers._"
// play.sbt.routes.RoutesKeys.routesImport += "com.playapp.binders._"
