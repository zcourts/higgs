

/**
  * @author Courtney Robinson <courtney@crlog.info>
  */
class WebSocketClient {

}

case class Pipeline(p: Seq[(_) => _] = Nil) {
  def >>[A, B](f: (A) => B) = Seq(f)
}

object Pipeline {

  protected case class Def[A, B](s: Seq[(A) => B])

  implicit def seq2pipeline[A, B](s: Seq[(A) => B]): Pipeline = Pipeline(s.asInstanceOf[Seq[(_) => _]])

  implicit def seq2composition[A, B](s: Seq[(_) => _]): (A) => B = ???

  //  protected case class Def[A,B](p: Seq[(A) => B]) {
  //    def >>[I, O](f: (I) => O) = Def[I,O]((p ++ Seq(f)).asInstanceOf[Def[A,B]])
  //
  //    def compose[X, Y] = (in:X) => {
  //
  //    }
  //  }
  //
  //  def >>[I, O](f: (I) => O): Def = Def(Seq(f))

  //def |(atom: A)
}

//trait Pipeline[A, B] {
//
//  private case class Def[T, U, V](f: (T) => U) extends Pipeline[B, V]
//
//  def >>[C](f: (A) => B): Pipeline[B, C] = Def(f)
//
//  //def |(atom: A)
//}
