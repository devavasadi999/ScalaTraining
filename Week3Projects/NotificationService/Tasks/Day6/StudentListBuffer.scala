/* Similar to StudentList question but using ListBuffer because List is immutable.
So if we use List, it results in creating new list for every add operation.
Using ListBuffer would solve this issue because it is mutable.*/

import scala.language.implicitConversions
import scala.collection.mutable.ListBuffer

case class Student(sno: Int, name: String, score: Double)

// implicit conversion method to convert tuple to student object
implicit def tupleToStudent(tuple: (Int, String, Double)): Student = {
    Student(tuple._1, tuple._2, tuple._3)
}

implicit class StudentListOps(var students: ListBuffer[Student]) {
    // Filter students with scores greater than a specified value
    def %>(threshold: Double): ListBuffer[Student] = students.filter(_.score > threshold)

    // Filter students with scores less than a specified value
    def %<(threshold: Double): ListBuffer[Student] = students.filter(_.score < threshold)

    // Filter students whose score lies in range [low, high]
    def scoreWithinRange(low: Double, high: Double): ListBuffer[Student] = students.filter(student => student.score >= low && student.score <= high)

    // add method to add new student to existing student list
    def add(newStudent: Student): ListBuffer[Student] = {
        students += newStudent
    }
}

@main def studentops() = {
    var studentList = ListBuffer(
        Student(1, "Alice", 34),
        Student(2, "Bob", 22),
        Student(3, "Charlie", 75),
        Student(4, "David", 82)
    )

    studentList.add((5, "Deva", 33D))

    println(s"Students with scores > 70: ${studentList %> 70}")
    println(s"Students with scores < 60: ${studentList %< 60}")
    println(s"Student with score within range >= 30 && <= 80: ${studentList.scoreWithinRange(30, 80)}")
}