package info.crlog.higgs.concurrent

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class Consumer[T](task: () => T) extends Runnable {
  def run() {
    task()
  }
}
