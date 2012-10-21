package info.crlog.higgs.protocols

/**
 * jrpc, short for JSON RPC is a simple protocol which uses
 * annotations to identify listeners who are then subscribed to a topic
 * of either a given name or if no name is given the method name becomes the topic.
 * Methods are analyzed on start up and their parameter types are all matched when a message
 * is received. If the types aren't compatible (i.e. doesn't meet the "is-a" req) an error
 * is sent back to the remote peer that invoked it
 * @author Courtney Robinson <courtney@crlog.info>
 */
package object jrpc {

}
