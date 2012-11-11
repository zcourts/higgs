package info.crlog.higgs.protocols.jrpc

import java.io.Serializable

/**
 * Represents a remote procedure call, both on the server and client.
 * If the RPC is a request to the server then the isRequest field should be
 * set to true by the client otherwise its false.
 * @param remoteMethodName
 * @param clientCallbackID
 * @param arguments
 * @param response
 * @param error
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class RPC(remoteMethodName: String,
               clientCallbackID: String,
               arguments: Array[Serializable],
               response: Option[Serializable]=None,
               error: Option[Throwable] = None) extends Serializable {
  /**
   * Create an RPC with new arguments specified by args* and copy all other fields
   * from the given RPC instance
   * @param rpc
   * @param args
   */
  def this(rpc: RPC, args: Array[Serializable]) = {
    this(rpc.remoteMethodName, rpc.clientCallbackID, args)
  }
}
