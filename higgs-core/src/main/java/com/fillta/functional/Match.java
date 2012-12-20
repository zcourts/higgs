package com.fillta.functional;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Match {
	/**
	 * Invokes the given function callback if the given class is assignable from the given object
	 *
	 * @param klass
	 * @param obj
	 * @param function
	 */
	public Match caseAssignableFrom(Class<?> klass, Object obj, Function function) {
		if (obj != null && klass.isAssignableFrom(obj.getClass())) {
			function.apply();
		}
		return this;
	}

	/**
	 * Invokes the given function if obj.getClass().equals(klass)
	 *
	 * @param klass
	 * @param obj
	 * @param function
	 */
	public Match caseEquals(Class<?> klass, Object obj, Function function) {
		if (obj != null && obj.getClass().equals(klass)) {
			function.apply();
		}
		return this;
	}

	/**
	 * Invoke the given function if a==b
	 *
	 * @param a
	 * @param b
	 * @param function
	 * @return
	 */
	public Match caseEquals(Object a, Object b, Function function) {
		if (a == b) {
			function.apply();
		}
		return this;
	}
}
