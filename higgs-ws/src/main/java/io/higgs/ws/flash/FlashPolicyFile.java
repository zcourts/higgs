package io.higgs.ws.flash;

import java.nio.charset.Charset;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FlashPolicyFile {
    private String policy;

    public FlashPolicyFile(String policy) {
        this.policy = policy;
    }

    public FlashPolicyFile() {
        this("<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE cross-domain-policy SYSTEM \"http://www.macromedia.com/xml/dtds/cross-domain-policy.dtd\">" +
                "\n<cross-domain-policy>\n" +
                "    <allow-access-from domain=\"*\" to-ports=\"*\"/>\n" +
                "</cross-domain-policy>");
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(final String policy) {
        this.policy = policy;
    }

    public byte[] getBytes() {
        return policy.getBytes(Charset.forName("utf-8"));
    }
}
