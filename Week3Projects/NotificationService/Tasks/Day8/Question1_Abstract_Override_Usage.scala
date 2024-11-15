trait A {
    def f(): Unit
}

trait B extends A{
    // Here abstract override is required because parent f() is abstract and so it expects some class in the hierarchy to have concrete f() method
    abstract override def f(): Unit = {
        super.f()
        println("Inside B's f()")
    }
}

trait C extends A {
    def f(): Unit = {
        println("Inside C's f()")
    }
}

@main def execute() = {
    val obj = new C with B
    obj.f()
}