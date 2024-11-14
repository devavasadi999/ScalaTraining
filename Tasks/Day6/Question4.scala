/*
use implicit method to convert array of students to list of students.
Add filterStudents method in StudentOps class
*/

import scala.language.implicitConversions
import scala.collection.mutable.ListBuffer

case class Student(sno: Int, name: String, score: Int)

//Implicit conversion method to convert Array[Student] to List[Student]
implicit def studentArrayToList(studentArray: Array[Student]): List[Student] = {
    val studentListBuffer: ListBuffer[Student] = ListBuffer()
    for(i <- 0 until studentArray.length) {
        studentListBuffer += studentArray(i)
    }
    studentListBuffer.toList
}

// Implicit conversion method to convert tuple to Student object
implicit def tupleToStudent(tuple: (Int, String, Int)): Student = {
  Student(tuple._1, tuple._2, tuple._3)
}

class StudentOps(val students: List[Student]) {
  // Filter students based on a given filter method
  def filterStudents(filterMethod: Student => Boolean): List[Student] = {
    students.filter(filterMethod)
  }
}

@main def studentOpsFunctionality() = {
  val records: Array[Student] = Array(
    (1, "Alice", 85), (2, "Bob", 92), (3, "Charlie", 78), (4, "David", 66), (5, "Eve", 90),
    (6, "Frank", 73), (7, "Grace", 88), (8, "Hannah", 91), (9, "Isaac", 84), (10, "Judy", 76),
    (11, "Kevin", 82), (12, "Laura", 79), (13, "Mike", 95), (14, "Nina", 70), (15, "Oscar", 89),
    (16, "Paul", 80), (17, "Quinn", 77), (18, "Rachel", 93), (19, "Sam", 85), (20, "Tina", 74),
    (21, "Uma", 69), (22, "Victor", 96), (23, "Wendy", 87), (24, "Xander", 68), (25, "Yara", 94),
    (26, "Zane", 81), (27, "Oliver", 78), (28, "Sophia", 85), (29, "Liam", 90), (30, "Mia", 83),
    (31, "Noah", 88), (32, "Emma", 75), (33, "Ava", 92), (34, "William", 86), (35, "James", 91),
    (36, "Lucas", 72), (37, "Amelia", 79), (38, "Ella", 89), (39, "Mason", 76), (40, "Logan", 95),
    (41, "Ethan", 84), (42, "Charlotte", 82), (43, "Benjamin", 80), (44, "Alexander", 71),
    (45, "Michael", 88), (46, "Isabella", 73), (47, "Daniel", 86), (48, "Elijah", 81),
    (49, "Matthew", 79), (50, "Jackson", 92)
  )

  val studentOps: StudentOps = new StudentOps(records/*.map(tupleToStudent)*/)

  println(s"Filtered records: ${studentOps.filterStudents(student => student.score > 80)}")
}
