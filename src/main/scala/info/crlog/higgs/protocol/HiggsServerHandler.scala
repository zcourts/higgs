package info.crlog.higgs.protocol

import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import com.codahale.logula.Logging

/**
 * Allows server handlers to push messages back up to Higgs etc.
 * @author Courtney Robinson <courtney@crlog.info> @ 03/02/12
 */

trait HiggsServerHandler extends SimpleChannelUpstreamHandler with Logging {

}