/* Problem Statement:
Create a case class Student
create an implicit class on List[Student] for %> and %< operators to work and to return filtered lists
create add method in that class which add a new student to existing list
define an implicit conversion method to convert tuple to student object so that add(tuple) would work */

import scala.language.implicitConversions

case class Student(sno: Int, name: String, score: Double)

// implicit conversion method to convert tuple to student object
implicit def tupleToStudent(tuple: (Int, String, Double)): Student = {
    Student(tuple._1, tuple._2, tuple._3)
}

implicit class StudentListOps(var students: List[Student]) {
    // Filter students with scores greater than a specified value
    def %>(threshold: Double): List[Student] = students.filter(_.score > threshold)

    // Filter students with scores less than a specified value
    def %<(threshold: Double): List[Student] = students.filter(_.score < threshold)

    // Filter students whose score lies in range [low, high]
    def scoreWithinRange(low: Double, high: Double) = students.filter(student => student.score >= low && student.score <= high)

    // add method to add new student to existing student list
    def add(newStudent: Student): List[Student] = students :+ newStudent
}

@main def studentops() = {
    var studentList = List(
        Student(1, "Alice", 34),
        Student(2, "Bob", 22),
        Student(3, "Charlie", 75),
        Student(4, "David", 82)
    )

    studentList = studentList.add((5, "Deva", 33D))

    println(s"Students with scores > 70: ${studentList %> 70}")
    println(s"Students with scores < 60: ${studentList %< 60}")
    println(s"Student with score within range >= 30 && <= 80: ${studentList.scoreWithinRange(30, 80)}")
}