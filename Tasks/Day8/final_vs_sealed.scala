// final keyword is such that final methods cannot be overridden and final classes cannot be extended

final class Animal {}

//class Dog extends Animal {} // error: cannot extend final class

class A{
    final def f(): Unit = {}
}

class B extends A{
    //def f(): Unit = {} // error: cannot override final method
}

// sealed class allows other classes in the same file to extend it but disallows any other classes from extending it

sealed class C{}

class D extends C{} // class in the same file can extend it

@main def execute() = {
}