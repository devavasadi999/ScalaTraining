object HeapSort extends App {
    def heapSort(array: Array[Int]): Unit = {
        for(i <- (array.length - 1)/2 to 0) {
            heapify(array, i, array.length)
        }

        for(i <- array.length - 1 to 1 by -1) {
            swap(array, 0, i)
            heapify(array, 0, i)
        }
    }

    def heapify(array: Array[Int], current: Int, heapLength: Int): Unit = {
        var currentIndex = current

        while(currentIndex < heapLength) {
            val left = currentIndex * 2 + 1
            val right = currentIndex * 2 + 2

            var maxIndex = currentIndex

            if(left < heapLength && array(left) > array(maxIndex)) {
                maxIndex = left
            }

            if(right < heapLength && array(right) > array(maxIndex)) {
                maxIndex = right
            }

            if(maxIndex != currentIndex) {
                swap(array, maxIndex, currentIndex)
                currentIndex = maxIndex
            } else {
                return
            }
        }
    }

    def swap(array: Array[Int], i: Int, j: Int): Unit = {
        array(i) = array(i) + array(j)
        array(j) = array(i) - array(j)
        array(i) = array(i) - array(j)
    }

    val array = Array(3, 2, 1)
    heapSort(array)
    println(array.mkString(", "))
}