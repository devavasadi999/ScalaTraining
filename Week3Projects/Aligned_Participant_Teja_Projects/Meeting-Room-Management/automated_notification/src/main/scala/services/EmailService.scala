package services

import utils.EmailUtils
import models.Reservation

class EmailService {
  def sendBookingConfirmation(reservation: Reservation): Unit = {
    println(s"Sending booking confirmation to ${reservation.employeeName} for reservation reservationId ${reservation.reservationId}")
    val subject = s"Meeting Room has been confirmed"
    val body =
      s"""
         |Dear ${reservation.employeeName},
         |
         |Welcome! Your Request for Meeting Room reservation from ${reservation.startTime} to ${reservation.endTime} is confirmed
         |
         |Thank you for reservation.
         |
         |Best regards,
         |Meeting Room Service
         |""".stripMargin

    // Send Wi-Fi details email
    EmailUtils.sendEmail(reservation.employeeMail, subject, body)
  }

  def sendRoomPreparationNotification(reservation: Reservation): Unit = {
    println(s"Sending room preparation notification for reservation reservationId ${reservation.reservationId}")
    val subject = s"Room preparation notification"
    val body =
      s"""
         |Dear Room Service Team,
         |
         |There is a Meeting reserved from ${reservation.startTime} to ${reservation.endTime} for ${reservation.roomId}.
         |
         |Please make sure the meeting requirements are met.
         |
         |For Further details contact ${reservation.employeeName} through ${reservation.employeeMail}
         |
         |Best regards,
         |Room Service
         |""".stripMargin

    // Send Wi-Fi details email
    EmailUtils.sendEmail("tejakumar023@gmail.com", subject, body)
  }

  def sendReminder(reservation: Reservation): Unit = {
    println(s"Sending reminder to ${reservation.employeeName} for reservation reservationId ${reservation.reservationId}")
    val subject = s"Meeting Remainder"
    val body =
      s"""
         |Dear ${reservation.employeeName},
         |
         |Welcome! Your Meeting is reserved from ${reservation.startTime} to ${reservation.endTime}.
         |
         |Reminding Your Reservation.
         |
         |Thank you for reservation.
         |
         |Best regards,
         |Meeting Room Service
         |""".stripMargin

    // Send Wi-Fi details email
    EmailUtils.sendEmail(reservation.employeeMail, subject, body)
  }

  def sendReleaseNotification(reservation: Reservation): Unit = {
    println(s"Room ${reservation.roomId} was not used for reservation reservationId ${reservation.reservationId}. Notification sent to admin staff.")
    val subject = s"Meeting Release Notification"
    val body =
      s"""
         |Dear Room Service Team,
         |
         |The Meeting reserved from ${reservation.startTime} to ${reservation.endTime} has been released.
         |
         |Please Make sure the meeting room with Id ${reservation.roomId} be available for next meeting.
         |
         |Thank you.
         |
         |Best regards,
         |Room Service
         |""".stripMargin

    // Send Wi-Fi details email
    EmailUtils.sendEmail("tejakumar023@gmail.com", subject, body)
  }
}
