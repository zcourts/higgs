

/**
  * @author Courtney Robinson <courtney@crlog.info>
  */
class WebSocketClient {

}

case class pipeline() {

  protected case class Def[A, B](f: (A) => B, prev: Option[Def[_, _]]) {
    def >>[I, O](f: (I) => O): Def[I, O] = Def(f, Some(this))

    def compose[X, Y] = compose1[X, Y](this.asInstanceOf[Def[X, Y]])
  }

  def >>[I, O](f: (I) => O): Def[I, O] = Def(f, None)

  def compose1[A, B](d: Def[A, B]): (A) => B = {
    d.prev match {
      case None => d.f
      case Some(o) => compose2(o)
    }
  }

  def compose2[A, B](d: Def[_, _]): (A) => B = compose1(d.asInstanceOf[Def[A, B]])

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
