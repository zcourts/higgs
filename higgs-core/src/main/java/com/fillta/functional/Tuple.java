package com.fillta.functional;

/**
 * A simple immutable Tuple
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Tuple<K, V> {
	public final K key;
	public final V value;

	/**
	 * Create a new tuple with the given key and value
	 *
	 * @param key
	 * @param value
	 */
	public Tuple(K key, V value) {
		this.key = key;
		this.value = value;
	}
}
