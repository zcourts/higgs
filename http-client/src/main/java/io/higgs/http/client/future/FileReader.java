package io.higgs.http.client.future;

import io.higgs.core.func.Function2;
import io.higgs.http.client.Response;
import io.netty.buffer.ByteBuf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class FileReader extends Reader<File> {
    protected File file;
    protected FileOutputStream out;

    public FileReader() throws IOException {
        this(null);
    }

    public FileReader(Function2<File, Response> function) throws IOException {
        if (function != null) {
            listen(function);
        }
        file = Files.createTempFile("higgs-http-client-temp-" + new Double(Math.random()).longValue(), ".tmp").toFile();
        out = new FileOutputStream(file);
    }

    @Override
    public void data(ByteBuf data) {
        byte[] tmp = new byte[data.readableBytes()];
        data.readBytes(tmp);
        try {
            out.write(tmp);
        } catch (IOException e) {
            log.warn("Error writing data to file", e);
        }
    }

    @Override
    public void done() {
        for (Function2<File, Response> function : functions) {
            function.apply(file, response);
        }
    }
}
