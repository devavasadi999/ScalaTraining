import akka.actor.{ActorSystem, Props}

object Exec {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("SimpleMemorySystem")
    val learner = system.actorOf(Props[LearnerActor], "LearnerActor")

    // Send a message to remember
    learner ! RememberMessage("Hello, Akka!")

    // Retrieve the remembered message
    import akka.pattern.ask
    import akka.util.Timeout
    import scala.concurrent.duration._
    import scala.concurrent.Await

    implicit val timeout: Timeout = Timeout(5.seconds)
    val future = learner ? RetrieveMessage
    val result = Await.result(future, timeout.duration)

    println(s"Retrieved message from LearnerActor: $result")
  }
}