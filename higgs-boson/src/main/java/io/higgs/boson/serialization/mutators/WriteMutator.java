package io.higgs.boson.serialization.mutators;

/**
 * A write mutator provides an interface for setting fields on objects without reflection
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface WriteMutator extends ClassMutator {

    /**
     * Sets the given field to the value provided on the instance of class T
     *
     * @param instance the instance to set the value on
     * @param field    the field to be set
     * @param value    the value to set the field to
     * @return true if successfully set
     *         TODO: if false is returned then an attempt will be made to set the field via reflection
     */
    <T> boolean set(T instance, String field, Object value);

    /**
     * Given the Fully qualified class name, return an instance of the said class
     *
     * @param className the FQN
     * @return a new instance
     */
    Object newInstance(String className);

    /**
     * Checks if this mutator can create instances of the given class name
     *
     * @param className The fully qualified class name
     * @return true if it can create new instances, false otherwise.
     */
    boolean canCreate(String className);
}
