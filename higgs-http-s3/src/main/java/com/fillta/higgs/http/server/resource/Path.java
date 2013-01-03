package com.fillta.higgs.http.server.resource;

import com.fillta.higgs.http.server.HttpRequest;
import com.fillta.higgs.http.server.HttpServer;
import com.fillta.higgs.http.server.params.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Path {
	/**
	 * Provide a path at which the annotated type can be reached
	 *
	 * @return
	 */
	public String value();

	/**
	 * The following rules apply on how variable names are mapped in the template.
	 * <pre>
	 * 1) If the response is a Map the map's keys become the variable names in the template and the
	 *    map's respective values are the values to those names
	 *
	 * 2) If the response is any type, other than Map or POJOs the variable name in the template
	 *    is "_response", i.e. if the method returns the number 100 in the template it can be accessed as
	 *    ${_response}. Note, this variable is always available, even for POJOs and Maps.
	 *
	 * 3) For POJOs the field names will become the variable names
	 *
	 *  4) Reserved variable names that will be automatically available in the template include:
	 *  ${_query} ,${_form},${_files},${_session},${_cookies},${_request},${_response},${_server}
	 * <pre>
	 * <table>
	 *     <thead>
	 *         <tr>
	 *             <td><strong>Template Variable name</strong></td>
	 *             <td><strong>Class/Type</strong></td>
	 *         </tr>
	 *     </thead>
	 *         <tr>
	 *             <td>${_query}</td>
	 *             <td>{@link QueryParams}</td>
	 *         </tr>
	 *         <tr>
	 *             <td>${_form}</td>
	 *             <td>{@link FormParams}</td>
	 *         </tr>
	 *         <tr>
	 *             <td>${_files}</td>
	 *             <td>{@link FormFiles}</td>
	 *         </tr>
	 *         <tr>
	 *             <td>${_session}</td>
	 *             <td>{@link HttpSession}</td>
	 *         </tr>
	 *         <tr>
	 *             <td>${_cookies}</td>
	 *             <td>{@link HttpCookies}</td>
	 *         </tr>
	 *         <tr>
	 *             <td>${_request}</td>
	 *             <td>{@link HttpRequest}</td>
	 *         </tr>
	 *         <tr>
	 *             <td>${_response}</td>
	 *             <td>{@link Object} - the object returned by the resource method</td>
	 *         </tr>
	 *         <tr>
	 *             <td>${_server}</td>
	 *             <td>{@link HttpServer}</td>
	 *         </tr>
	 * </table>
	 * </pre>
	 * <p/>
	 * </pre>
	 *
	 * @return The path to a template that the resource's response is mapped to.
	 *         The response can be any collection, POJO, array or primitive (inc strings).
	 */
	public String template() default "";

}
