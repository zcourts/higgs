package com.fillta.higgs.boson;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonMessage {
    public Object[] arguments;
    public String method;
    public String callback;
    public short protocolVersion = 0x1;

    public BosonMessage(Object[] arguments, String method, String callback, short protocolVersion) {
        this.arguments = arguments;
        this.method = method;
        this.callback = callback;
        this.protocolVersion = protocolVersion;
    }

    public BosonMessage() {

    }
}
