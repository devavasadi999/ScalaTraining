case class JobRunner (
    name: String,
    time_in_seconds: Int
)

object JobRunner {
    def apply(name: String, time_in_seconds: Int)(logic: String => Unit): JobRunner = {
        println("About to sleep..")
        Thread.sleep(time_in_seconds * 1000)
        logic(name)
        new JobRunner(name, time_in_seconds)
    }
}

@main def jobRunnerFunction() = {
    val jb: JobRunner = JobRunner("Job1", 5) { name =>
        println(s"Executing logic... for $name")
    }
}