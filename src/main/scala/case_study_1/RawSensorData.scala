case class RawSensorData(
                       sensorId: String,    // Unique identifier for each sensor
                       timestamp: Long,     // Timestamp for the reading (epoch time in milliseconds)
                       temperature: Float,  // Temperature value between -50 and 150
                       humidity: Float      // Humidity value between 0 and 100
                     )