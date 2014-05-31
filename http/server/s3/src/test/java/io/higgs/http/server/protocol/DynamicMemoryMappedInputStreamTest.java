package io.higgs.http.server.protocol;

import io.higgs.core.func.Function1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DynamicMemoryMappedInputStreamTest {
    //mocked stuff
    protected ChannelHandlerContext ctx;
    protected Channel mockedChannel;
    protected MMappedDecoder mockedDecoder;
    //
    protected Path underlyingFile;
    protected MappedByteBuffer file;
    protected long datumSize = 4, //writing ints which are 4 bytes so the initial buffer size is 4 bytes
            bufferSize = datumSize; //initial buffer size is not 0
    protected InputStream stream;
    protected FileChannel fileChannel;
    protected RandomAccessFile rand;

    @Before
    public void setUp() throws Exception {
        //mock the ctx and channel used in the input stream impl
        ctx = mock(ChannelHandlerContext.class);
        mockedChannel = mock(Channel.class);
        mockedDecoder = mock(MMappedDecoder.class);

        when(ctx.channel()).thenReturn(mockedChannel);
        //most importantly, make sure the isOpen is true for the socket to be considered alive
        when(mockedChannel.isOpen()).thenReturn(true);
        //just as important, return the changing MMapped buffer

        doAnswer(new Answer<Function1>() {
            @Override
            public Function1 answer(InvocationOnMock invocation) throws Throwable {
                synchronized (file) {
                    return ((Function1<Object, Object>) invocation.getArguments()[0]);//.apply(file);
                }
            }
        });
        when(mockedDecoder.syncAndRun(new Function1<MappedByteBuffer, Object>() {
            @Override
            public Object apply(MappedByteBuffer buffer) {
                return null;
            }
        }));
        //
        underlyingFile = Files.createTempFile("hs3-mapped-file-", "-test-run");
        rand = new RandomAccessFile(underlyingFile.toFile(), "rw");
        fileChannel = rand.getChannel();
        stream = new DynamicMemoryMappedInputStream(ctx, mockedDecoder, fileChannel);
    }

    @After
    public void tearDown() throws Exception {
        fileChannel.close();
        underlyingFile.toFile().delete(); //clean up
    }

    private void mapFile(int pos) throws IOException {
        if (file != null) {
            synchronized (file) {
                file.notifyAll();
            }
        }
        file = fileChannel.map(FileChannel.MapMode.READ_WRITE, pos, bufferSize);
    }

    /**
     * To test this, we'll push a number of sequential values into the mapped buffer, each time
     * remapping the buffer to a larger size while a reader thread constantly pulls the numbers out of the
     * input stream, verifying their incremental order.
     * <p/>
     * When there is nothing to pull out the reader should block and shouldn't receive a -1 causing it to terminate
     */
    @Test(timeout = 100000) //shouldn't take 10 seconds to complete this
//    @Test
    public void testReadWriteFromMultipleThreads() throws Exception {
        final int total = randInt(1000, 100000);
        for (int i = 0; i < total; i++) {
            //increase the mapped buffer size by how much we're going to write
            //after the first write, because we've already done map file in setup
            bufferSize += i == 0 ? 0 : datumSize;
            //position is 0 at first but moves to the limit of the previous mapping subsequently
            int pos = i == 0 ? 0 : file.limit();
            mapFile(pos); //remap the file
            file.putInt(i); //add our new int
            if (i == 0) {
                //start reading stream after adding the first value
                readIntStream(total);
            }
        }
    }

    private void readIntStream(final int total) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                do {
                    try {
                        ByteBuf buf = Unpooled.buffer(4);
                        //read 4 bytes from the stream to re-create the int value
                        buf.writeByte(stream.read())
                                .writeByte(stream.read())
                                .writeByte(stream.read())
                                .writeByte(stream.read());
                        int val = buf.readInt();
                        assertEquals(String.format("Had %s runs with matching values", i), i, val);
                    } catch (IOException e) {
                        fail("No IO exception should be raised - " + e.getMessage());
                    }
                } while (i++ < total);
            }
        }).start();
    }

    public static int randInt(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }
}
