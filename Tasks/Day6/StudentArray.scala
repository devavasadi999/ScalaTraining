/* Create an array of students.
Define implicit class on array of students.
Add filterRecords operation in that class which returns filtered students based on received filter method */

case class Student(sno: Int, name: String, score: Double)

implicit class StudentArrayOps(students: Array[Student]) {
    // Filter students based on given filter method
    def filterRecords(filterMethod: (Student) => Boolean): Array[Student] = {
        students.filter(filterMethod)
    }
}

@main def studentArray() = {
    val students: Array[Student] = Array(
        Student(1, "Alice", 34),
        Student(2, "Bob", 45),
        Student(3, "Charlie", 65),
        Student(4, "Deva", 64)
    )

    println(students.filterRecords(student => student.score > 50).mkString(", "))
}