import akka.actor.Actor
case class RememberMessage(content: String)
case object RetrieveMessage

class LearnerActor extends Actor {
  // Variable to store the last received message
  private var lastMessage: Option[String] = None

  override def receive: Receive = {
    case RememberMessage(content) =>
      lastMessage = Some(content) // Store the message content
      println(s"LearnerActor remembered message: $content")

    case RetrieveMessage =>
      sender() ! lastMessage.getOrElse("No message remembered") // Reply with the last message
  }
}
