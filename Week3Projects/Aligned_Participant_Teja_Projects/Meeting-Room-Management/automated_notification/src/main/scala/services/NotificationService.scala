package services

import models.Reservation

class NotificationService(emailService: EmailService) {

  def sendBookingConfirmation(reservation: Reservation): Unit = {
    emailService.sendBookingConfirmation(reservation)
  }

  def sendRoomPreparationNotification(reservation: Reservation): Unit = {
    emailService.sendRoomPreparationNotification(reservation)
  }
}
