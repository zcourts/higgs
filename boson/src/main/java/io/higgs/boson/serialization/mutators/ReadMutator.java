package io.higgs.boson.serialization.mutators;

import java.util.List;

/**
 * A write mutator provides an interface for setting fields on objects without reflection
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ReadMutator extends ClassMutator {
    /**
     * @param klass the class to check
     * @param obj   the instance of the class to check
     * @return true if this mutator can read fields from the given class or object either or both can be used
     *         to determine true or false
     */
    boolean canReadFields(Class<?> klass, Object obj);

    /**
     * Gets the value of the given field from the provided instance
     * Preferably without using the Class object
     *
     * @param instance the instance to get the value from
     * @param klass the class of the instance
     *@param field    the field to get  @return The value of the given field
     */
    Object get(Class<?> klass, Object instance, String field);

    /**
     * Try to determine a list of fields to be serialized. Preferably without using the Class object
     * @return A list of all modifiable fields on the given class, only these fields will be serialized
     */
    <T> List<String> fields(Class<?> klass, Object obj);
}
