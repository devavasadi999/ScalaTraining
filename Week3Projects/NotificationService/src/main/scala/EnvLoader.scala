import scala.io.Source

object EnvLoader {
  def loadEnv(filePath: String): Unit = {
    val source = Source.fromFile(filePath)
    for (line <- source.getLines()) {
      val parts = line.split("=", 2)
      if (parts.length == 2) {
        val key = parts(0).trim
        val value = parts(1).trim
        System.setProperty(key, value)
      }
    }
    source.close()
  }
}
