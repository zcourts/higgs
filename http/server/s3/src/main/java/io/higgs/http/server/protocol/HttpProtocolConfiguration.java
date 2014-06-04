package io.higgs.http.server.protocol;

import io.higgs.core.HiggsServer;
import io.higgs.core.MethodProcessor;
import io.higgs.core.ProtocolConfiguration;
import io.higgs.core.ProtocolDetectorFactory;
import io.higgs.core.reflect.dependency.DependencyProvider;
import io.higgs.http.server.auth.HiggsSecurityManager;
import io.higgs.http.server.config.HttpConfig;
import io.higgs.http.server.providers.ProviderContainer;
import io.higgs.http.server.providers.filters.HiggsFilter;
import io.higgs.http.server.util.Util;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.cliffc.high_scale_lib.NonBlockingHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.util.Set;

/**
 * JAX-RS has three main types of providers.
 * Entity, Context and Exception, see https://jsr311.java.net/nonav/releases/1.1/spec/spec3.html#x3-390004
 * Auto discoverable and manually configurable
 */
public class HttpProtocolConfiguration implements ProtocolConfiguration {
    //Providers
    protected final Set<ProviderContainer<MessageBodyWriter>> writers = new NonBlockingHashSet<>();
    protected final Set<ProviderContainer<MessageBodyReader>> readers = new NonBlockingHashSet<>();
    protected final Set<ProviderContainer<ContextResolver>> contextProviders = new NonBlockingHashSet<>();
    protected final Set<ProviderContainer<ExceptionMapper>> exceptionMappers = new NonBlockingHashSet<>();
    protected final Set<HiggsFilter> filters = new NonBlockingHashSet<>();
    //
    protected SecurityManager securityManager;
    protected HiggsServer server;
    protected Logger log = LoggerFactory.getLogger(getClass());

    public HiggsServer getServer() {
        return server;
    }

    public void setServer(HiggsServer server) {
        this.server = server;
    }

    @Override
    public ProtocolDetectorFactory getProtocol() {
        return new HttpDetectorFactory(this);
    }

    @Override
    public MethodProcessor getMethodProcessor() {
        return new HttpMethodProcessor(this);
    }

    @Override
    public void initialize(HiggsServer server) {
        this.server = server;
        String path = ((HttpConfig) server.getConfig()).security_config_path;
        IniSecurityManagerFactory factory = new IniSecurityManagerFactory(path);
        securityManager = factory.getInstance();

        if (securityManager instanceof DefaultSecurityManager) {
            HiggsSecurityManager.configure(server, (DefaultSecurityManager) securityManager);
        }
        //todo make auto discovery configurable, allowing users to disable it
        log.debug("Attempting to discover providers");
        Set providers = Util.getServices(Provider.class, DependencyProvider.global());
        for (Object o : providers) {
            if (o instanceof MessageBodyReader) {
                readers.add(new ProviderContainer<>((MessageBodyReader) o));
            } else if (o instanceof MessageBodyWriter) {
                writers.add(new ProviderContainer<>((MessageBodyWriter) o));
            } else if (o instanceof ContextResolver) {
                contextProviders.add(new ProviderContainer<>((ContextResolver) o));
            } else if (o instanceof ExceptionMapper) {
                exceptionMappers.add(new ProviderContainer<>((ExceptionMapper) o));
            } else if (o instanceof HiggsFilter) {
                filters.add((HiggsFilter) o);
            } else {
                log.warn(String.format("Discovered unsupported Provider type %s, only MessageBodyReader," +
                                "MessageBodyWriter,ContextResolver and ExceptionMapper implementations are valid.",
                        o.getClass().getName()
                ));
            }
        }
//        filters.addAll(Util.getServices(HiggsFilter.class));
////todo add config option to enable discovery of any implementation of the following (wouldn't be jsr-311 compliant)
        for (ContextResolver m : Util.getServices(ContextResolver.class, DependencyProvider.global())) {
            contextProviders.add(new ProviderContainer<>(m));
        }
//        for (MessageBodyReader m : Util.getServices(MessageBodyReader.class, DependencyProvider.global())) {
//            readers.add(new ProviderContainer<>(m));
//        }
//        for (MessageBodyWriter m : Util.getServices(MessageBodyWriter.class)) {
//            writers.add(new ProviderContainer<>(m));
//        }
//        for (ExceptionMapper m : Util.getServices(ExceptionMapper.class)) {
//            exceptionMappers.add(new ProviderContainer<>(m));
//        }
    }

    public Set<HiggsFilter> getFilters() {
        return filters;
    }

    public Set<ProviderContainer<MessageBodyWriter>> getWriters() {
        return writers;
    }

    public Set<ProviderContainer<MessageBodyReader>> getReaders() {
        return readers;
    }

    public Set<ProviderContainer<ContextResolver>> getContextProviders() {
        return contextProviders;
    }

    public Set<ProviderContainer<ExceptionMapper>> getExceptionMappers() {
        return exceptionMappers;
    }

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        if (securityManager == null) {
            throw new IllegalArgumentException("Security manager cannot be null");
        }
        this.securityManager = securityManager;
    }
}
