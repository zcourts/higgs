package info.crlog.higgs.protocols.jrpc

import java.io.Serializable

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
case class RPC(remoteMethodName: String,
                      clientCallbackID: String,
                      remoteParams: Seq[Serializable]) extends Serializable {

}
