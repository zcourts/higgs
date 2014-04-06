package io.higgs.http.server;

import io.higgs.core.ResolvedFile;
import io.netty.channel.ChannelFuture;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface ManagedWriter {
    ChannelFuture doWrite();

    boolean isDone();

    ResolvedFile getFile();
}
