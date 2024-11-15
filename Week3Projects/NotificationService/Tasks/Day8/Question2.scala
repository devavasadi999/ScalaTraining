trait Task {
    def doTask(): Unit = {
        println("Task: Performing task")
    }

    def doActivity(): Unit
}

trait Cook extends Task{
    override def doTask(): Unit = {
        super.doTask()
        println("Cook: Cooking task")
    }
}

trait Garnish extends Task {
    override def doTask(): Unit = {
        super.doTask()
        println("Garnish: Garnishing task")
    }
}

trait Pack extends Task {
    override def doTask(): Unit = {
        super.doTask()
        println("Pack: Packing task")
    }
}

class Activity extends Task {
    override def doActivity(): Unit = {
        println("Activity: Starting activity")
        doTask()
    }

    override def doTask(): Unit = {
        println("Activity task")
    }
}

@main def execute() = {
    var activity: Task = new Activity with Cook with Garnish with Pack
    activity.doActivity()
    println()

    activity = new Activity with Pack with Cook with Garnish
    activity.doActivity()
    println()

    activity = new Activity with Garnish with Pack with Cook
    activity.doActivity()
}