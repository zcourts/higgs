package io.higgs.boson;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Accelerator {
    /**
     * A set of Beams and their IDs that are currently active within this {@link Accelerator}
     */
    protected Map<Integer, Beam> beams = new HashMap<>();
}
