package io.higgs.events;

import java.util.Arrays;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class EventMessage {
    private final Object[] params;
    private final String name;
    private final String str;

    public EventMessage(String event, Object[] params) {
        this.name = event;
        this.params = params;
        str = "Event{" +
                "name='" + name + '\'' +
                ", params=" + Arrays.toString(params) +
                '}';
    }

    /**
     * @return the name of this event
     */
    public String name() {
        return name;
    }

    /**
     * @return the set of arguments emitted with this event
     */
    public Object[] params() {
        return params;
    }

    @Override
    public String toString() {
        return str;
    }
}
