package info.crlog.higgs.agents

/**
 * A post man agent so-called because of its similarity with real world post men;
 * will deliver messages to a given list of listeners. Using this agent ensures delivery of the messages to the
 * specified listeners. If the post man is unable to deliver a message it optionally buffers messages, once the buffer is
 * full it throws an exception (MessageDeliveryException). If buffering is disabled then the postman throws an exception
 * as soon as it is unable to deliver the first message.
 * Courtney Robinson <courtney@crlog.info>
 */
class HiggsPostman {
}