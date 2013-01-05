package com.fillta.higgs.ws.demo;

import com.fillta.higgs.http.server.resource.GET;
import com.fillta.higgs.http.server.resource.POST;
import com.fillta.higgs.http.server.resource.Path;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
@Path("/")
public class Api {
	@Path(value = "/", template = "index")
	@GET
	@POST
	public String test() {
		return "{}";
	}
}
