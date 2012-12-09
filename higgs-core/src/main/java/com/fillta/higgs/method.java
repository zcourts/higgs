package com.fillta.higgs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers a class or method to receive messages
 * If applied to a class then all methods within that class are registered as listeners
 * unless a method specificly applies the annotation setting "optout=true"
 * In which case the method will be opted out of receiving messages.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface method {
	/**
	 * Optionally provide a name for the "topic" to which a method is subscribed.
	 * If no name is provided, by default and preferably, the fully qualified name of the
	 * method is used i.e. com.domain.class.method
	 *
	 * @return
	 */
	public String value() default "";

	/**
	 * If provided the method this is applied to will NOT be registered to receive messages.
	 * NOTE: Applying this at class level is meaningless and is ignored. Only applies to methods
	 *
	 * @return
	 */
	public boolean optout() default false;
}
