package io.higgs.http.server;

import io.higgs.http.server.params.ResourcePath;

import java.util.Set;

public class DefaultResourceFilter implements ResourceFilter {
    private final HttpServer server;
    private final long id = Double.doubleToLongBits(Math.random());

    public DefaultResourceFilter(final HttpServer server) {
        this.server = server;
    }

    public Endpoint getEndpoint(final HttpRequest request) {
        if (request.isGet()) {
            Set<Endpoint> gets = server.getGetEndpoints();
            for (Endpoint endpoint : gets) {
                ResourcePath path = endpoint.newPath();
                if (path.matches(request)) {
                    return endpoint;
                }
            }
        } else {
            if (request.isPut()) {
                Set<Endpoint> putEndpoints = server.getPutEndpoints();
                for (Endpoint endpoint : putEndpoints) {
                    if (endpoint.newPath().matches(request)) {
                        return endpoint;
                    }
                }
            } else {
                if (request.isPost()) {
                    Set<Endpoint> postEndpoints = server.getPostEndpoints();
                    for (Endpoint endpoint : postEndpoints) {
                        if (endpoint.newPath().matches(request)) {
                            return endpoint;
                        }
                    }
                } else {
                    if (request.isDelete()) {
                        Set<Endpoint> deleteEndpoints = server.getDeleteEndpoints();
                        for (Endpoint endpoint : deleteEndpoints) {
                            if (endpoint.newPath().matches(request)) {
                                return endpoint;
                            }
                        }
                    } else {
                        if (request.isHead()) {
                            Set<Endpoint> headEndpoints = server.getHeadEndpoints();
                            for (Endpoint endpoint : headEndpoints) {
                                if (endpoint.newPath().matches(request)) {
                                    return endpoint;
                                }
                            }
                        } else {
                            if (request.isOptions()) {
                                Set<Endpoint> optionsEndpoints = server.getOptionsEndpoints();
                                for (Endpoint endpoint : optionsEndpoints) {
                                    if (endpoint.newPath().matches(request)) {
                                        return endpoint;
                                    }
                                }
                            } else {
                                request.setUnsupportedMethod(true);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public int compareTo(ResourceFilter that) {
        return this.priority() < that.priority() ? -1 :
                (this.priority() == that.priority() ? 0 : 1);
    }

    @Override
    public int priority() {
        return 1; //static resource priority is 0 so this is used before it
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DefaultResourceFilter)) {
            return false;
        }
        DefaultResourceFilter that = (DefaultResourceFilter) o;
        if (Double.compare(that.id, id) != 0) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
