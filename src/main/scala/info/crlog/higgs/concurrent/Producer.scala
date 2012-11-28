package info.crlog.higgs.concurrent

import java.util.concurrent.ExecutorService

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class Producer[T](consumer: ExecutorService, task: () => Consumer[T]) extends Runnable {
  def run() {
    consumer.submit(task())
  }
}
