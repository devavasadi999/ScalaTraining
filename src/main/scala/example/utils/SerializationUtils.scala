package example.utils

import scalapb.GeneratedMessage

object SerializationUtils {
  def serializeProtobuf[T <: GeneratedMessage](message: T): Array[Byte] = message.toByteArray
}
