package info.crlog.higgs

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
object Event extends Enumeration {
  /**
   * Any event which this handler provides notifications for
   */
  type Event = Value
  val CHANNEL_ACTIVE = Value("channel_active")
  val CHANNEL_INACTIVE = Value("channel_inactive")
  val CHANNEL_REGISTERED = Value("channel_registered")
  val CHANNEL_UNREGISTERED = Value("channel_unregistered")
  val EXCEPTION_CAUGHT = Value("exception_caught")
}
