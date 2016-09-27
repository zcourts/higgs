import Pipeline._

object Main {
  def main(args: Array[String]) {
    val p = Pipeline() >>
      ((i: Int) => i.toLong) >>
      ((l: Long) => l.toString) >>
      ((s: String) => Seq(s)) >>
      ((s: Seq[String]) => s.map(_.toInt * 10)) //compose
  }
}
