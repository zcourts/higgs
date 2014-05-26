package io.higgs.http.server.providers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class HiggsProviders implements Providers {
    @Override
    public <T> MessageBodyReader<T> getMessageBodyReader(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return null;
    }

    @Override
    public <T> MessageBodyWriter<T> getMessageBodyWriter(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return null;
    }

    @Override
    public <T extends Throwable> ExceptionMapper<T> getExceptionMapper(Class<T> type) {
        return null;
    }

    @Override
    public <T> ContextResolver<T> getContextResolver(Class<T> contextType, MediaType mediaType) {
        return null;
    }
}
