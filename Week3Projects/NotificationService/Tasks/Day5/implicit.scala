import scala.language.implicitConversions

case class Amount(val amount: Double)

implicit def intToString(x: Int): String = x.toString
implicit def takeAmount(d: Double): Amount = new Amount(d)

implicit class EnhancedInt(val a: Int){
    def +#(x: Int): Int = x+a+4

    def rangeFinder(): String = "Finding range..."
}

@main def haha() = {

    val a:String  = 456
    val b: Amount = 56.78
    println(a)
    println(b)

    val c: Int = 23 +# 5
    println(c)
    println(c.rangeFinder())
}