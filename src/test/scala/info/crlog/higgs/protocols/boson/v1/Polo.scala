package info.crlog.higgs.protocols.boson.v1

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
class Polo {
  //"model" is ignored as a field but array_polo has it as a property so it is serialized
  @BosonProperty(ignore = true)
  var model = new Model()
  //all numeric types
  var byte: Byte = 0
  var short: Short = 0
  var int = 0
  var long = 0L
  var float = 0.0F
  var double = 0.0D
  //string ...ish
  var char: Char = 'a'
  var string = ""
  var nulled: String = null
  //arrays
  var array_byte = Array[Byte](1, 2, 3)
  var array_short = Array[Short](4, 5, 6)
  var array_int = Array[Int](7, 8, 9)
  var array_long = Array[Long](1, 0, 1)
  var array_float = Array[Float](1, 1, 2)
  var array_double = Array[Double](1, 3, 1)
  var array_polo = Array(1, model)
  //lists
  var list_byte = List[Byte](1, 2, 3)
  var list_short = List[Short](4, 5, 6)
  var list_int = List[Int](7, 8, 9)
  var list_long = List[Long](1, 0, 1)
  var list_float = List[Float](1, 1, 2)
  var list_double = List[Double](1, 3, 1)
  var list_polo = List(1, model)
  //maps

  //var map_byte = Map(1 -> 2, 3->4)   //can't do byte literals,so can't be bothered!
  var map_short = List[Short](4, 5, 6)
  var map_int = List[Int](7, 8, 9)
  var map_long = List[Long](1, 0, 1)
  var map_float = List[Float](1, 1, 2)
  var map_double = List[Double](1, 3, 1)
  var map_polo = List(1, model)
  //polo
  var polo = new Model()
  var polo_nested = new NestedModel()
}

class Model {
  var name = "Test"
}

class NestedModel {
  var name = "Nested Test"
  var model = new Model()
}