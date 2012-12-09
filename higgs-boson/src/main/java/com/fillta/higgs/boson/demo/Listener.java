package com.fillta.higgs.boson.demo;

import com.fillta.higgs.method;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Listener {
    @method(optout = true)
    public void ignoredMethod() {
    }

    int c = 0;

    @method("name")
    public void name(int a) {
        c++;
        System.out.println(c + ":" + a);
    }

    @method("age")
    public int age(int a) {
        System.out.println(a);
        return a;
    }
}
