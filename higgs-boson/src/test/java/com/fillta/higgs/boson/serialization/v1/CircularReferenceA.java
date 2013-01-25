package com.fillta.higgs.boson.serialization.v1;

public class CircularReferenceA {
    CircularReferenceB b;

    public CircularReferenceA(final CircularReferenceB b) {
        this.b = b;
    }

    public CircularReferenceA() {
        //keep serializer happy
    }

    public String toString() {
        return hashCode() + "-A";
    }
}
