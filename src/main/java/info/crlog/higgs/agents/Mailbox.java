package info.crlog.higgs.agents;

/**
 * Receive messages from a Postman. A Postman -> Mailbox agent has much stronger
 * guarantee on message delivery. If a Postman is unable to deliver a message to
 * a Mailbox it will return false from its delivery methods, user applications
 * can then pro-actively retry message delivery
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Mailbox {
}
