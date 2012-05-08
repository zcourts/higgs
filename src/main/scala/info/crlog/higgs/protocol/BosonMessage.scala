package info.crlog.higgs.protocol

import info.crlog.higgs.util.StringUtilities
import info.crlog.higgs.{ByteConvertible, Message}

/**
 * Courtney Robinson <courtney@crlog.info>
 */
abstract class BosonMessage[K <: ByteConvertible, V <: ByteConvertible] extends Message[K, V] {
  protected var properties = scala.collection.mutable.Map.empty[K, V]
  private var util = new StringUtilities
}

