object Main {
  def main(args: Array[String]) {
    //    val pipeline = new Pipeline[Int, Long] {} >>
    //      ((a) => a.toLong) >>
    //      ((l) => l.toString) >>
    //      ((s) => s) >>
    //      ((s) => Seq(s)) >>
    //      ((s) => s) >>
    //      ((s) => s.map(_.toInt * 10))
    //    println(pipeline)
    val producer = pipeline() >>
      ((a: Int) => a.toLong) >>
      ((l: Long) => l.toString) >>
      ((s: String) => Seq(s)) >>
      ((s: Seq[String]) => s.map(_.toInt * 10))
    val producerPipeline = producer.compose[Int, Seq[Int]]
    println(producerPipeline(10))
  }
}
