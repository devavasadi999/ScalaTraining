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
  "org.apache.kafka" % "kafka-clients" % "2.8.0", // Add Kafka clients dependency
  "com.auth0" % "java-jwt" % "4.3.0", // Java JWT library
  "org.mindrot" % "jbcrypt" % "0.4"
)
libraryDependencies += ws

libraryDependencies += "org.postgresql" % "postgresql" % "42.2.23"





// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.playapp.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.playapp.binders._"
