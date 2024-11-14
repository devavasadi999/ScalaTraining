package models

import services.KafkaProducerService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._

import java.time.{Duration, LocalDateTime, LocalTime}
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

class StartupTasks @Inject()(dbInitializer: DatabaseInitializer,
                             equipmentAllocationRepository: EquipmentAllocationRepository,
                             kafkaProducer: KafkaProducerService,
                             employeeRepository: EmployeeRepository)(implicit ec: ExecutionContext) {
  println("Running start up tasks")
  dbInitializer.initialize()

  // Set the specific time for the daily check (e.g., 8:00 AM)
  private val dailyCheckTime: LocalTime = LocalTime.of(17, 55)
  private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
  startDailyOverdueCheck()

  def startDailyOverdueCheck(): Unit = {
    val initialDelay = calculateInitialDelay(dailyCheckTime)

    // Schedule the task to run daily at the specified time
    scheduler.scheduleAtFixedRate(
      new Runnable {
        override def run(): Unit = {
          checkForOverdueAllocations()
        }
      },
      initialDelay,
      TimeUnit.DAYS.toSeconds(1), // Repeat every 24 hours
      TimeUnit.SECONDS
    )
  }

  // Calculate the delay until the next occurrence of the target time
  private def calculateInitialDelay(targetTime: LocalTime): Long = {
    val now = LocalDateTime.now()
    val targetDateTime = if (now.toLocalTime.isBefore(targetTime)) {
      now.toLocalDate.atTime(targetTime) // Today at target time
    } else {
      now.toLocalDate.plusDays(1).atTime(targetTime) // Tomorrow at target time
    }
    Duration.between(now, targetDateTime).getSeconds // Return delay in seconds
  }

  // Method to check overdue equipment allocations and send reminders

  private def checkForOverdueAllocations(): Unit = {
    val currentDate = LocalDateTime.now().toLocalDate

    // Retrieve overdue allocations as a Future[Seq[EquipmentAllocation]]
    equipmentAllocationRepository.findOverdueAllocations(currentDate).flatMap { overdueAllocations =>
      Future.sequence(
        overdueAllocations.map { allocation =>
          // Retrieve the employee email associated with this equipment allocation
          employeeRepository.findEmailById(allocation.employeeId).flatMap {
            case Some(employeeEmail) =>
              // Create and send an overdue reminder message to the employee's email
              val message = Json.obj(
                "messageType" -> "OverdueReminder",
                "toEmails" -> Json.arr(employeeEmail),
                "equipmentAllocation" -> Json.toJson(allocation)
              )
              kafkaProducer.send("rawNotification", message.toString)
              println(s"Overdue reminder sent to $employeeEmail for equipment allocation: $allocation")
              Future.successful(())

            case None =>
              println(s"Failed to send overdue reminder: No email found for employeeId ${allocation.employeeId}")
              Future.successful(())
          }
        }
      )
    }.recover {
      case ex: Exception =>
        println(s"Failed to check overdue allocations: ${ex.getMessage}")
    }
  }

}
