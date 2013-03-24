package io.higgs.boson.serialization.mutators;

/**
 * A read write mutator simply merges the {@link ReadMutator} and {@link WriteMutator}
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ReadWriteMutator extends ReadMutator, WriteMutator {
}
