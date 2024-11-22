package services

class RoomService {
  def checkRoomOccupancy(roomId: Int, startTime: String): Boolean = {
    // Implement logic to check if the room is currently occupied
    // Here, return false as a placeholder to simulate an unoccupied room
    false // Placeholder for testing; assumes the room is unoccupied
  }

  def releaseRoom(roomId: Int): Unit = {
    println(s"Room $roomId has been released and is available for new bookings.")
  }
}
