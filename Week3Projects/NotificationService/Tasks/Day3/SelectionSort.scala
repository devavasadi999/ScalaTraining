object SelectionSort extends App {
    def selectionSort(array: Array[Int]): Unit = {
        for(i <- 0 to array.length - 2) {
            var minimumIndex = i
            for(j <- i to array.length - 1) {
                if(array(j) < array(minimumIndex)) {
                    minimumIndex = j
                }
            }

            swap(array, i, minimumIndex)
        }
    }

    def swap(array: Array[Int], i: Int, j: Int): Unit = {
        if(i != j) {
            array(i) = array(i) + array(j)
            array(j) = array(i) - array(j)
            array(i) = array(i) - array(j)
        }
    }

    val array = Array(3, 2, 1)
    selectionSort(array)
    println(array.mkString(", "))
}