package info.crlog.higgs.util

import java.net.URLDecoder
import java.util.{ArrayList, HashMap, List, Map}

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object HttpUtil {

  /**
   * Parse the query string portion of a string containing a URI or URL.
   * @author https://gist.github.com/3504765
   * @see http://stackoverflow.com/a/2969715/400048  for discussion
   */
  def parseUriParameters(qs: String): Map[String, List[String]] = {
    val params = new HashMap[String, List[String]]()
    val uri = if (qs.contains("?")) qs else "?" + qs
    val query = uri.substring(uri.indexOf('?')+1)
    query split "&" map {
      param =>
        val pair = param split "="
        val key = URLDecoder.decode(pair(0), "UTF-8")
        val value = pair.length match {
          case l if l > 1 => URLDecoder.decode(pair(1), "UTF-8")
          case _ => ""
        }
        val values = Option(params get key) match {
          case Some(values) => values

          case None =>
            val v = new ArrayList[String]()
            params.put(key, v)
            v
        }
        values add value // NB: The methodName may be an empty string.
    }
    params
  }

  def main(arg: Array[String]) {
    println(parseUriParameters("access_token=AAAC9iVp3fpoBAFZADH3bycmuL2Dob0msL9QtJNwpWxC9n5JNBc9H2sDsQr8YN3oY312lM5wOAR7gBObZAOyNdhFwkn2q4SSQYtXCzGuQZDZD&expires=5149074"))
  }
}
