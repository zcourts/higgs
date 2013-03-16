package io.higgs.ws.demo;

import io.higgs.http.server.resource.GET;
import io.higgs.http.server.resource.POST;
import io.higgs.http.server.resource.Path;

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
