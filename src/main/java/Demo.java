
import info.crlog.higgs.agents.Broadcaster;
import info.crlog.higgs.agents.Radio;
import info.crlog.higgs.protocol.boson.BosonMessage;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Demo {

    static boolean stop = false;

    public static void main(String[] args) throws Exception {
        final Broadcaster server = new Broadcaster("localhost", 2012);
        server.prepare();
        final Radio client = new Radio("localhost", 2012);
        final Radio client2 = new Radio("localhost", 2012);
        client.setFactorySize(10000);
        client2.setFactorySize(10000);
        server.setFactorySize(10000);
        client.tune();
        client2.tune("flush");
        Thread.sleep(5000);
        new Timer().schedule(new TimerTask() {

            @Override
            public void run() {
                server.broadcast("flush", "this");
                server.broadcast("haha", "this");
                stop = true;
                client.shutdown();
                client2.shutdown();
                server.shutdown();
            }
        }, 2000);
        System.out.println("Server started");
        boolean alive = true;
        long total = 1;
        long start = System.currentTimeMillis();
        while (alive) {
            if (stop) {
                alive = false;
            } else {
                total++;
                server.broadcast(new BosonMessage("a"));
            }
        }
        long end = System.currentTimeMillis();
        System.out.println((end - start) + " milliseconds to send " + total);

    }
}
