package io.higgs.http.server.jaxrs;

import io.higgs.http.server.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ResponseBuilder extends Response.ResponseBuilder {
    HttpResponse response = new HttpResponse();
    JaxRsResponse jaxRsResponse = new JaxRsResponse(response);

    @Override
    public Response build() {
        return jaxRsResponse;
    }

    @Override
    public Response.ResponseBuilder clone() {
        return this;
    }

    @Override
    public Response.ResponseBuilder status(int status) {
        response.setStatus(HttpResponseStatus.valueOf(status));
        return this;
    }

    @Override
    public Response.ResponseBuilder entity(Object entity) {
        jaxRsResponse.setEntity(entity);
        return this;
    }

    @Override
    public Response.ResponseBuilder entity(Object entity, Annotation[] annotations) {
        jaxRsResponse.setEntity(entity);
        return this;
    }

    @Override
    public Response.ResponseBuilder allow(String... methods) {
        return this;
    }

    @Override
    public Response.ResponseBuilder allow(Set<String> methods) {
        return this;
    }

    @Override
    public Response.ResponseBuilder cacheControl(CacheControl cacheControl) {
        return this;
    }

    @Override
    public Response.ResponseBuilder encoding(String encoding) {
        return this;
    }

    @Override
    public Response.ResponseBuilder header(String name, Object value) {
        response.headers().set(name, value);
        return this;
    }

    @Override
    public Response.ResponseBuilder replaceAll(MultivaluedMap<String, Object> headers) {
        return this;
    }

    @Override
    public Response.ResponseBuilder language(String language) {
        return this;
    }

    @Override
    public Response.ResponseBuilder language(Locale language) {
        return this;
    }

    @Override
    public Response.ResponseBuilder type(MediaType type) {
        return this;
    }

    @Override
    public Response.ResponseBuilder type(String type) {
        return this;
    }

    @Override
    public Response.ResponseBuilder variant(Variant variant) {
        return this;
    }

    @Override
    public Response.ResponseBuilder contentLocation(URI location) {
        return this;
    }

    @Override
    public Response.ResponseBuilder cookie(NewCookie... cookies) {
        return this;
    }

    @Override
    public Response.ResponseBuilder expires(Date expires) {
        return this;
    }

    @Override
    public Response.ResponseBuilder lastModified(Date lastModified) {
        return this;
    }

    @Override
    public Response.ResponseBuilder location(URI location) {
        return this;
    }

    @Override
    public Response.ResponseBuilder tag(EntityTag tag) {
        return this;
    }

    @Override
    public Response.ResponseBuilder tag(String tag) {
        return this;
    }

    @Override
    public Response.ResponseBuilder variants(Variant... variants) {
        return this;
    }

    @Override
    public Response.ResponseBuilder variants(List<Variant> variants) {
        return this;
    }

    @Override
    public Response.ResponseBuilder links(Link... links) {
        return this;
    }

    @Override
    public Response.ResponseBuilder link(URI uri, String rel) {
        return this;
    }

    @Override
    public Response.ResponseBuilder link(String uri, String rel) {
        return this;
    }
}
