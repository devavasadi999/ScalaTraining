object BubbleSort extends App {
    def bubbleSort(array: Array[Int]) = {
        for(i <- array.length - 1 to 0 by -1) {
            for(j<- 0 to i - 1) {
                // if adjacent elements are out of order
                if(array(j) > array(j+1)) {
                    // swap them
                    array(j) = array(j) + array(j+1)
                    array(j + 1) = array(j) - array(j + 1)
                    array(j) = array(j) - array(j + 1)
                }
            }
        }
    }

    val array = Array(3, 2, 1)
    bubbleSort(array)
    println(array.mkString(", "))
}