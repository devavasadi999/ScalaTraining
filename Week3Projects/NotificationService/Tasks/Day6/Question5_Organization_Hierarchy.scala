import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Queue
import scala.io.StdIn.readLine

// Define employee case class
case class Employee (
    serialNo: Int, 
    name: String,
    city: String
) {
    override def toString: String = s"($serialNo, $name, $city)"
}

// Define the TreeNode case class
case class TreeNode (
    departmentName: String,
    employees: ListBuffer[Employee] = ListBuffer(),
    children: ListBuffer[TreeNode] = ListBuffer()
)

class TreeOps {
    val rootNode: TreeNode = TreeNode("Organization")

    def addNode(parentDeptName: String, currentDeptName: String, employees: List[Employee] = List.empty): String = {
        try {
            val currentDepartmentNode: TreeNode = addCurrentDepartment(parentDeptName, currentDeptName)

            // add employees to current department
            currentDepartmentNode.employees ++= employees

            "Succesfully saved"
        } catch {
            case e: Exception => e.getMessage
        }
    }

    private def addCurrentDepartment(parentDeptName: String, currentDeptName: String): TreeNode = {
        // if parent department not found, throw error
        val parentDeptNode: TreeNode = search(node => node.departmentName == parentDeptName).getOrElse(throw new Exception(s"Department $parentDeptName not found"))
        val currentDeptNode: TreeNode = search(node => node.departmentName == currentDeptName).getOrElse(createCurrentDepartment(parentDeptNode, currentDeptName))

        // if current not linked to parent, throw error
        if (!currentLinkedToParent(parentDeptNode, currentDeptNode)) {
            throw new Exception(s"Department $parentDeptName is not the direct parent of $currentDeptName")
        }

        currentDeptNode
    }

    private def createCurrentDepartment(parentDeptNode: TreeNode, currentDeptName: String): TreeNode = {
        val currentDeptNode: TreeNode = TreeNode(currentDeptName)
        parentDeptNode.children += currentDeptNode
        currentDeptNode
    }

    private def currentLinkedToParent(parentDeptNode: TreeNode, currentDeptNode: TreeNode): Boolean = {
        parentDeptNode.children.exists(_ eq currentDeptNode)
    }

    private def search(criterion: (TreeNode) => Boolean): Option[TreeNode] = {
        // search using bfs
        // Initialize the queue using the starting node
        val queue = Queue[TreeNode](rootNode)

        // Process each node in the queue
        while(queue.nonEmpty) {
            // Dequeue the next node to process
            val current = queue.dequeue()

            // Check if the current node matches the search criterion
            if(criterion(current)) {
                return Some(current)
            }

            // Enqueue all children of the current node
            queue.enqueueAll(current.children)
        }

        // Return None if no matching node is found
        None
    }

    def printTree(): Unit = {
        printTreeUtils(rootNode, 0)
    }

    private def printTreeUtils(currNode: TreeNode, depth: Int): Unit = {
        printWithIndentation(depth, currNode.departmentName)
        for(singleEmployee <- currNode.employees) {
            printWithIndentation(depth + 1, singleEmployee)
        }

        for(childNode <- currNode.children) {
            printTreeUtils(childNode, depth + 1)
        }
    }

    private def printWithIndentation(depth: Int, content: Any): Unit = {
        for(i <- 0 until depth) {
            print("  |")
        }
        print("-- ")
        println(content)
    }
}

@main def treeOperations() = {
    startInteraction()
}

def startInteraction(): Unit = {
    val treeOps: TreeOps = new TreeOps()
    var continueLooping: Boolean = true

    while(continueLooping) {
        println("Enter 1 to add record. 2 to print the current organization structure, 3 to exit.")

        val choice = readLine("Enter your choice: ").trim

        choice match {
            case "1" =>
                val (parentDeptName, currentDeptName, employees) = collectDepartmentDetails()
                println(s"${treeOps.addNode(parentDeptName, currentDeptName, employees)} \n")
            case "2" => 
                println()
                treeOps.printTree()
                println()
            case "3" =>
                continueLooping = false
            case _ =>
                println("Invalid choice\n")
        }
    }
}

def collectDepartmentDetails(): (String, String, List[Employee]) = {
    val parentDeptName = readLine("Enter parent department name: ").trim
    val currentDeptName = readLine("Enter current department name: ").trim
    val employees: ListBuffer[Employee] = ListBuffer()

    var continueLooping: Boolean = true
    while(continueLooping) {
        val choice = readLine("\nEnter 1 to add employee. 2 to save: ")

        choice match {
            case "1" =>
                val sNum: Int = readLine("Enter Serial Number: ").trim.toInt
                val empName: String = readLine("Enter Name of Employee: ").trim
                val empCity: String = readLine("Enter city of Employee: ").trim
                employees += Employee(sNum, empName, empCity)
            case "2" =>
                continueLooping = false
            case _ =>
                println("Invalid choice")
        }
    }

    (parentDeptName, currentDeptName, employees.toList)
}