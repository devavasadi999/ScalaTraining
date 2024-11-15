object QuickSort extends App {
    def quickSort(array: Array[Int]): Unit = {
        quickSortHelper(array, 0, array.length - 1)
    }

    def quickSortHelper(array: Array[Int], left: Int, right: Int): Unit = {
        if(left < right){
            val p: Int = partition(array, left, right)
            quickSortHelper(array, left, p)
            quickSortHelper(array, p + 1, right)
        }
    }

    def partition(array: Array[Int], left: Int, right: Int) : Int = {
        var i = left - 1
        var j = right + 1
        val pivot  = array(left)

        while(i < j) {
            i += 1
            while(array(i) < pivot) {
                i += 1
            }

            j = j - 1
            while (array(j) > pivot) {
                j = j - 1
            }

            if(i < j) {
                swap(array, i, j)
            }
        }

        j
    }

    def swap(array: Array[Int], i: Int, j: Int): Unit = {
        array(i) = array(i) + array(j)
        array(j) = array(i) - array(j)
        array(i) = array(i) - array(j)
    }

    val array = Array(3, 2, 1)
    quickSort(array)
    println(array.mkString(", "))
}