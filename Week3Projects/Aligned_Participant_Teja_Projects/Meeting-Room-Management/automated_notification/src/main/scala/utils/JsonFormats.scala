package utils

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
import models.*

object JsonFormats {
  implicit val reservationEncoder: Encoder[Reservation] = deriveEncoder[Reservation]
  implicit val reservationDecoder: Decoder[Reservation] = deriveDecoder[Reservation].emap { reservation =>
    // Add custom validation here if needed
    if (reservation.reservationId <= 0) {
      Left("reservationId must be positive") // Custom error message
    } else {
      println("correct")
      Right(reservation)
    }
  }
}
