package com.fillta.higgs.http.server;

import com.fillta.higgs.http.server.params.ResourcePath;

import java.util.Set;

public class DefaultResourceFilter implements ResourceFilter {
	private final HttpServer server;

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
		} else if (request.isPut()) {
			Set<Endpoint> putEndpoints = server.getPutEndpoints();
			for (Endpoint endpoint : putEndpoints) {
				if (endpoint.newPath().matches(request))
					return endpoint;
			}
		} else if (request.isPost()) {
			Set<Endpoint> postEndpoints = server.getPostEndpoints();
			for (Endpoint endpoint : postEndpoints) {
				if (endpoint.newPath().matches(request))
					return endpoint;
			}
		} else if (request.isDelete()) {
			Set<Endpoint> deleteEndpoints = server.getDeleteEndpoints();
			for (Endpoint endpoint : deleteEndpoints) {
				if (endpoint.newPath().matches(request))
					return endpoint;
			}
		} else if (request.isHead()) {
			Set<Endpoint> headEndpoints = server.getHeadEndpoints();
			for (Endpoint endpoint : headEndpoints) {
				if (endpoint.newPath().matches(request))
					return endpoint;
			}
		} else if (request.isOptions()) {
			Set<Endpoint> optionsEndpoints = server.getOptionsEndpoints();
			for (Endpoint endpoint : optionsEndpoints) {
				if (endpoint.newPath().matches(request))
					return endpoint;
			}
		} else {
			request.setUnsupportedMethod(true);
		}
		return null;
	}
}
