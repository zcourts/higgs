package io.higgs.http.server.jaxrs;

import io.higgs.http.server.HttpResponse;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class JaxRsResponse extends Response {
    private final HttpResponse response;
    private Object entity;

    public JaxRsResponse(HttpResponse response) {
        this.response = response;
    }

    @Override
    public int getStatus() {
        return response.getStatus().code();
    }

    @Override
    public StatusType getStatusInfo() {
        return new StatusType() {
            @Override
            public int getStatusCode() {
                return response.getStatus().code();
            }

            @Override
            public Status.Family getFamily() {
                return Status.Family.familyOf(response.getStatus().code());
            }

            @Override
            public String getReasonPhrase() {
                return response.getStatus().reasonPhrase();
            }
        };
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public <T> T readEntity(Class<T> entityType) {
        return null;
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType) {
        return null;
    }

    @Override
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
        return null;
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
        return null;
    }

    @Override
    public boolean hasEntity() {
        return false;
    }

    @Override
    public boolean bufferEntity() {
        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.WILDCARD_TYPE;
    }

    @Override
    public Locale getLanguage() {
        return Locale.getDefault();
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public Set<String> getAllowedMethods() {
        return new HashSet<String>() {
            {
                add("GET");
                add("PUT");
                add("POST");
                add("DELETE");
                add("HEAD");
            }
        };
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return new HashMap<>();
    }

    @Override
    public EntityTag getEntityTag() {
        return new EntityTag(String.valueOf(Math.random()));
    }

    @Override
    public Date getDate() {
        return new Date();
    }

    @Override
    public Date getLastModified() {
        return new Date();
    }

    @Override
    public URI getLocation() {
        try {
            return new URI("http://ignored");
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public Set<Link> getLinks() {
        return new HashSet<>();
    }

    @Override
    public boolean hasLink(String relation) {
        return false;
    }

    @Override
    public Link getLink(String relation) {
        return null;
    }

    @Override
    public Link.Builder getLinkBuilder(String relation) {
        return null;
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return null;
    }

    @Override
    public String getHeaderString(String name) {
        return null;
    }

    public void setEntity(Object entity) {
        this.entity = entity;
    }
}
