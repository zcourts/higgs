package com.fillta.higgs.http.server.resource;

/**
 * Defines the media types that the methods of a resource class can accept. If
 * not specified, a container will assume that any media type is acceptable.
 * Method level annotations override a class level annotation. A container
 * is responsible for ensuring that the method invoked is capable of consuming
 * the media type of the HTTP request entity body. If no such method is
 * available the container must respond with a HTTP "415 Unsupported Media Type"
 * as specified by RFC 2616.
 */
public @interface Consumes {
	public String[] value() default MediaType.WILDCARD;
}
