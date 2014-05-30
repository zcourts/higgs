package io.higgs.http.server.providers;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashSet;
import java.util.Set;

/**
 * Wraps a {@link javax.ws.rs.ext.Provider} to provide convenient ways of determining if it
 * produces or consumes a certain {@link javax.ws.rs.core.MediaType}.
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ProviderContainer<T> {
    protected final T instance;
    protected final Class<?> klass;
    protected Set<MediaType> consumes = new HashSet<>();
    protected Set<MediaType> produces = new HashSet<>();

    public ProviderContainer(T i) {
        if (i == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        this.instance = i;
        klass = i.getClass();
        inspect();
    }

    /**
     * Figure out if the provider wrapped by this container restricts what types it consumes or produces
     */
    private void inspect() {
        Consumes consumesAnnotation = klass.getAnnotation(Consumes.class);
        if (consumesAnnotation != null && consumesAnnotation.value() != null) {
            for (String mediaType : consumesAnnotation.value()) {
                consumes.add(MediaType.valueOf(mediaType));
            }
        } else {
            consumes.add(MediaType.WILDCARD_TYPE); //consumes everything
        }
        Produces producesAnnotation = klass.getAnnotation(Produces.class);
        if (producesAnnotation != null && producesAnnotation.value() != null) {
            for (String mediaType : producesAnnotation.value()) {
                produces.add(MediaType.valueOf(mediaType));
            }
        } else {
            produces.add(MediaType.WILDCARD_TYPE); //produces everything
        }
    }

    public T get() {
        return instance;
    }

    /**
     * Checks if the provider wrapped by this container consumes the given media type
     *
     * @param t the media type to check for compatibility for
     * @return true if the provider can consume the type, false otherwise
     */
    public boolean consumes(MediaType t) {
        if (t == null) {
            return false;
        }
        for (MediaType consumedType : consumes) {
            if (consumedType.isCompatible(t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the provider wrapped by this container produces the given media type
     *
     * @param t the media type to check for compatibility for
     * @return true if the provider can produce the type, false otherwise
     */
    public boolean produces(MediaType t) {
        if (t == null) {
            return false;
        }
        for (MediaType producedType : produces) {
            if (producedType.isCompatible(t)) {
                return true;
            }
        }
        return false;
    }
}
