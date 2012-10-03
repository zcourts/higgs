package info.crlog.higgs.agents.msgpack.commands

import info.crlog.higgs.agents.msgpack.Interaction

/**
 * Sole purpose of this class is to provide a pivot in the hierarchy of interactions.
 * This means a generic method can be used to listen for all commands and within that
 * method a match can be done for specific commands
 * @author Courtney Robinson <courtney@crlog.info>
 */
class Command extends Interaction {

}
