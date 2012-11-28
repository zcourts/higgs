package info.crlog.higgs.concurrent

import java.util.concurrent._

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object ProducerConsumerDemo {
  val q = new LinkedBlockingQueue[Runnable](10000)
  val consumer = new ThreadPoolExecutor(50, 100, 10, TimeUnit.SECONDS, q)
  val producer = Executors.newSingleThreadExecutor()

  def main(args: Array[String]) {
    var count = 0
    val max = readInt()
    while (count < max) {
      producer.submit(new Producer(consumer, () => {
        new Consumer(() => {
          println("consumer task")
          count += 1
        })
      }))
      //Thread.sleep(0)
    }
    producer.shutdown()
    consumer.shutdown()
    Thread.sleep(2000)
    println("Count:", count, "Queue size:", q.size(), "Consumer pool size:", consumer.getPoolSize())
  }
}
