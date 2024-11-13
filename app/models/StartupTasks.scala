package models

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StartupTasks @Inject()(dbInitializer: DatabaseInitializer)(implicit ec: ExecutionContext) {
  println("Running start up tasks")
  dbInitializer.initialize()
}
