import spray.json.{DefaultJsonProtocol, RootJsonFormat}

// Define the Message case class
case class Message(messageType: String, message: String, messageKey: String)

// JSON formatting for Message
object JsonFormats extends DefaultJsonProtocol {
  implicit val messageFormat: RootJsonFormat[Message] = jsonFormat3(Message)
}