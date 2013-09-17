package io.higgs.hmq.jmzq;


import org.zeromq.ZMQ;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class psenvpub {

    public static void main(String[] args) throws Exception {
        // Prepare our context and publisher
        org.zeromq.ZMQ.Context context = ZMQ.context(1);
        org.zeromq.ZMQ.Socket publisher = context.socket(ZMQ.PUB);

        publisher.bind("tcp://*:5563");
        while (!Thread.currentThread().isInterrupted()) {
            // Write two messages, each with an envelope and content
            publisher.sendMore("A");
            publisher.send("We don't want to see this");
            publisher.sendMore("B");
            publisher.send("We would like to see this");
        }
        publisher.close();
        context.term();
    }
}