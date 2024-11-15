import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, entity, path, post}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

import JsonFormats._

//Define the WebServer Application
object WebServerApp {
  def main(args: Array[String]): Unit = {
    //Create the actor system
    implicit val system: ActorSystem = ActorSystem("MessageProcessingSystem")

    //Create a Kafka producer instance
    val producer = KafkaProducerFactory.createProducer()

    //Create message processors
    val networkProcessor = system.actorOf(Props(new NetworkMessageProcessor(producer)), "networkProcessor")
    val cloudProcessor = system.actorOf(Props(new CloudMessageProcessor(producer)), "cloudProcessor")
    val appProcessor = system.actorOf(Props(new AppMessageProcessor(producer)), "appProcessor")

    //Create the MessageHandler with references to each processor
    val messageHandler = system.actorOf(Props(new MessageHandler(networkProcessor, cloudProcessor, appProcessor)), "messageHandler")

    //Define the HTTP route
    val route = post {
      path("process-Message") {
        entity(as[Message]) { message =>
          messageHandler ! message
          complete(StatusCodes.OK, s"Message processed: $message")
        }
      }
    }

    //Start the HTTP server
    Http().newServerAt("0.0.0.0", 8080).bind(route)
    println("Server online at http://0.0.0.0:8080/")
  }
}