object ImplicitExample {

  // Step 1: Define an implicit value
  implicit val defaultTaxRate: Double = 0.2 // 20% tax rate

  // Step 2: Define a function with an implicit parameter
  def calculateTax(amount: Double)(implicit taxRate: Double): Double = {
    amount * taxRate
  }

  def main(args: Array[String]): Unit = {
    // Step 3: Call the function without passing the tax rate
    val amount = 100.0
    val tax = calculateTax(amount)  // Uses the implicit tax rate
    println(s"The tax on $amount is $tax")  // Outputs: The tax on $100.0 is $20.0

    // Step 4: Override the implicit parameter by passing a custom tax rate
    val customTax = calculateTax(amount)(0.15)  // Uses a 15% tax rate explicitly
    println(s"The tax on $amount with a custom rate is $customTax")  // Outputs: The tax on $100.0 with a custom rate is $15.0
  }
}
