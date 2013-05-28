package io.higgs.events.demo;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class RandomObject {
    private final int val;

    public RandomObject(int i) {
        this.val = i;
    }

    @Override
    public String toString() {
        return "RandomObject{" +
                "val=" + val +
                '}';
    }
}
