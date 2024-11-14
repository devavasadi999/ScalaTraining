object InsertionSort extends App {
    def insertionSort(array: Array[Int]) = {
        for(i <- 0 to array.length - 1) {
            val currentNum = array(i)
            var j = i - 1

            while (j >= 0 && array(j) > currentNum) {
                // swap j and j + 1
                array(j) = array(j) + array(j+1)
                array(j + 1) = array(j) - array(j + 1)
                array(j) = array(j) - array(j + 1)
                j = j - 1
            }

            array(j + 1) = currentNum
        }
    }

    val array = Array(3, 2, 1)
    insertionSort(array)
    println(array.mkString(", "))
}