package io.higgs.ws.sockjs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
@Path("sockjs")
public class SockJSProtocol {

    private Random random = new Random();

    @GET
    public Map<String, Object> info() {
        Map<String, Object> data = new HashMap<>();
        //Are websockets enabled on the server?
        data.put("websocket", true);
        //Do transports need to support cookies (ie: for load balancing purposes.
        data.put("cookie_needed", true);
        data.put("origins", "*:*");
        data.put("entropy", random.nextInt());

        return data;
    }
}
