package info.crlog.higgs.protocol

import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import com.codahale.logula.Logging

/**
 * Base client handler which allows clients to push messages back to Higgs etc
 * @author Courtney Robinson <courtney@crlog.info> @ 03/02/12
 */

trait HiggsClientHandler extends SimpleChannelUpstreamHandler with Logging{

}