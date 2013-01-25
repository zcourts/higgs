package com.fillta.higgs.boson.serialization.v1;

import com.fillta.higgs.boson.BosonServer;
import com.fillta.higgs.method;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class CircularReferenceServer {
    @method("circular")
    public CircularReferenceB[] getCircularReferencedObject() {
        final CircularReferenceB[] b = new CircularReferenceB[1];
        for (int i = 0; i < b.length; i++) {
            b[i] = new CircularReferenceB();
            b[i].init();
        }
        return b;
    }

    public static void main(String[] args) {
        BosonServer server = new BosonServer(11000);
        server.bind();
        server.register(new CircularReferenceServer());
    }
}
