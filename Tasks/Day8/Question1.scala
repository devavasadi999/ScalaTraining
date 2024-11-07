trait GetStarted {
    def prepare(): Unit = {
        println("GetStarted prepare")
    }

    def prepareFood(): Unit
}

trait KeepIngredients extends GetStarted {
    // Here abstract override is not required because parent method is concrete
    override def prepare(): Unit = {
        super.prepare()
        println("KeepIngredients prepare")
    }
}

trait Cook extends KeepIngredients {
    override def prepare(): Unit = {
        super.prepare()
        println("Cook prepare")
    }
}

trait Seasoning {
    def applySeasoning(): Unit = {
        println("Seasoning applySeasoning")
    }
}

class Food extends Cook with Seasoning {
    override def prepareFood(): Unit = {
        prepare()
        applySeasoning()
    }
}

@main def execute() = {
    val food: GetStarted = new Food()
    food.prepareFood()
}