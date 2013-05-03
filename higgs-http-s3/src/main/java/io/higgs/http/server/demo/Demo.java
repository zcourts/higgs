package io.higgs.http.server.demo;

import io.higgs.core.HiggsServer;
import io.higgs.http.server.HttpTemplate;
import io.higgs.http.server.config.ErrorConfig;
import io.higgs.http.server.config.HttpConfig;
import io.higgs.http.server.protocol.HttpProtocolConfiguration;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Demo {
    private Demo() {
    }

    public static void main(String... args) {
        Constructor constructor = new Constructor(HttpConfig.class); //HttpConfig.class is root
        TypeDescription errorDesc = new TypeDescription(ErrorConfig.class);
        errorDesc.putListPropertyType("templates", HttpTemplate.class);
        constructor.addTypeDescription(errorDesc);

        HttpProtocolConfiguration http = new HttpProtocolConfiguration();

        HiggsServer<HttpConfig> server = new HiggsServer<>("config.yml", HttpConfig.class, constructor);
        server.registerProtocol(http);
        //
        server.registerPackage(Api.class.getPackage());
//        server.registerClass(Api.class);
//        server.registerObject(new Api());
        server.start();
    }
}
