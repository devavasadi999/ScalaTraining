package services

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

object TimeZoneConversion {

  def getCurrentISTLocalDateTime(): LocalDateTime = {
    // Get the current ZonedDateTime of the machine
    val machineZonedDateTime: ZonedDateTime = ZonedDateTime.now()

    // Convert the ZonedDateTime to IST (Asia/Kolkata) ZonedDateTime
    val istZonedDateTime: ZonedDateTime = machineZonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Kolkata"))

    // Get the LocalDateTime for IST
    val istLocalDateTime: LocalDateTime = istZonedDateTime.toLocalDateTime

    // Print results
    println(s"Machine ZonedDateTime: $machineZonedDateTime")
    println(s"IST ZonedDateTime: $istZonedDateTime")
    println(s"IST LocalDateTime: $istLocalDateTime")
    istLocalDateTime
  }
}