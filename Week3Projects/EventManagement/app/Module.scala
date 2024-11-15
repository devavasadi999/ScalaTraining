import play.api.inject.{SimpleModule, _}
import models.{DatabaseInitializer, StartupTasks}

class Module extends SimpleModule(bind[StartupTasks].toSelf.eagerly())
