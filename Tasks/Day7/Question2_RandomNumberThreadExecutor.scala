import scala.util.{Random, Try, Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}

def RandomNumberThreadExecutor(): Future[String] = {
    // Create a promise
    val promise = Promise[String]()

    // Create a random number generation task
    val task = new Runnable {
        def run(): Unit = {
            while(!promise.isCompleted) {
                // generate random number
                val randomNumber = Random.between(1, 5001) //Generates a random number between 1(inclusive) and 5001(exclusive)

                if(randomNumber == 1567) {
                    // try complete the promise to make sure if it is already completed, won't be doing it again
                    // it also signals other threads to stop because all threads are checking for status of promise continuously
                    promise.trySuccess(s"${Thread.currentThread().getName} has generated 1567")
                }
            }
        }
    }

    // Create three threads that does the above task
    val thread1 = new Thread(task)
    thread1.setName("firstThread")
    val thread2 = new Thread(task)
    thread2.setName("secondThread")
    val thread3 = new Thread(task)
    thread3.setName("thirdThread")

    // Start the threads
    thread1.start()
    thread2.start()
    thread3.start()

    // Return the future associated with the promise
    promise.future
}

@main def execute() = {
    val future: Future[String] = RandomNumberThreadExecutor()

    // print the result after the future gets completed
    try{
        println(Await.result(future, Duration.Inf))
    } catch {
        case ex: Throwable => println(s"Await failed with exceptin: ${ex.getMessage}")
    }
}