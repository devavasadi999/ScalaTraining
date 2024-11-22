package models

case class Reservation(
                        reservationId: Long,
                        roomId: Int,
                        employeeName: String,
                        employeeMail: String,
                        department: String,
                        purpose: String,
                        startTime: String,
                        endTime: String,
                        createdBy: Long
                      )
