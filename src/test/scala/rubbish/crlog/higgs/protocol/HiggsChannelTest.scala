package rubbish.crlog.higgs.protocol

import io.netty.channel.{ChannelStateEvent, ChannelHandlerContext}
import org.junit.{Test, After, Before}
import info.crlog.higgs.agents.{HiggsRadio, HiggsBroadcaster}

/**
 * Courtney Robinson <courtney@crlog.rubbish>
 */
class HiggsChannelTest {
  val broadcaster = new HiggsBroadcaster("localhost",2022)
  val radio = new HiggsRadio("localhost", 2022)

  @Before def setUp {
    broadcaster.bind()
  }

  @After def tearDown {
  }

  @Test def testOnConnected {
//    broadcaster.higgsChannel.onConnected((ctx: ChannelHandlerContext, e: ChannelStateEvent) => {
//      class ConnectedException(msg: String) extends RuntimeException(msg)
//      throw new ConnectedException("Connected")
//    })
//    radio.listen()
//   Thread.sleep(10000)
  }

  @Test def testOnChannelClosed {
  }

  @Test def testOnDisconnected {
  }

  @Test def testOnChildOpen {
  }

  @Test def testOnChildClosed {
  }

  @Test def testOnMessageReceived {
  }

  @Test def testOnChannelBound {
  }

  @Test def testOnExceptionCaught {
  }

  @Test def testOnHandleUpstream {
  }

  @Test def testOnOpen {
  }

  @Test def testOnUnbound {
  }

  @Test def testOnInterestChanged {
  }

  @Test def testOnClosed {
  }
}

