import scala.io.Source

//Define a case class to represent an Employee
case class Employee(sno: Int, name: String, city: String, salary: Double, department: String)

@main def execute() = {
    // Read the CSV file and skip the header and convert all of them into a list
    val employees = Source.fromFile("employees.csv").getLines().drop(1).map { line => 
        val Array(sno, name, city, salary, department) = line.split(",").map(_.trim)
        Employee(sno.toInt, name, city, salary.toDouble, department)
    }.toList

    //Filter employees by salary
    val highEarners = employees.filter(_.salary > 55000)
    println("Employees with salary > 55000:")
    highEarners.foreach(println)

    //Filter employees by department
    val marketingEmployees = employees.filter(_.department == "Marketing")
    println("\nEmployees in Marketing Department:")
    marketingEmployees.foreach(println)

    // Map operation to produce a formatted report
    val report = employees.map(emp => s"${emp.name}, ${emp.city}, ${emp.salary}, ${emp.department}")
    println("\nFormatted Report:")
    report.foreach(println)

    // Reduce operations

    // Total salary
    val totalSalary = employees.map(_.salary).sum
    println(s"\nTotal Salary: $$$totalSalary")

    // Average salary
    val averageSalary = totalSalary/employees.size
    println(s"Average Salary: $$$averageSalary")

    val employeesByDept = employees.groupBy(_.department)
    
    // Count of employees per department
    val employeeCountByDept = employeesByDept.mapValues(_.size)

    println("\nNumber of Employees Department-wise:")
    employeeCountByDept.foreach {
        case(dept, count) => println(s"$dept: $count")
    }

    // Sum of salaries department wise
    val totalSalaryByDept = employeesByDept.mapValues(_.map(_.salary).sum)
    
    println("\nTotal Salary Department-wise:")
    totalSalaryByDept.foreach {
        case (dept, total) => println(s"$dept: $$$total")
    }
}