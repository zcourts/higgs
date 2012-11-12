package info.crlog.higgs.protocols.boson.v1;

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
public @interface BosonProperty {
    /**
     * Optionally provide a name for this field.
     * If no name is provided, by default the variable name is used
     *
     * @return
     */
    public String value() default "";
}
