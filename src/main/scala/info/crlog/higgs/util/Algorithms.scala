package info.crlog.higgs.util

import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Courtney Robinson <courtney@crlog.info>
 */
object Algorithms {
  def md5(str: String): String = {
    var m: MessageDigest = null
    try {
      m = MessageDigest.getInstance("MD5")
    }
    catch {
      case e: NoSuchAlgorithmException => {
      }
    }
    m.update(str.getBytes, 0, str.length)
    return new BigInteger(1, m.digest).toString(16)
  }

  def sha1(str: String): String = {
    var m: MessageDigest = null
    try {
      m = MessageDigest.getInstance("SHA-1")
    }
    catch {
      case e: NoSuchAlgorithmException => {
      }
    }
    m.update(str.getBytes, 0, str.length)
    return new BigInteger(1, m.digest).toString(16)
  }
}


