package io.higgs.s3.spi;

import io.higgs.common.annotations.tests.SpiAnnotation;
import io.higgs.common.annotations.tests.SpiClass;
import io.higgs.common.annotations.tests.SpiInterface;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class SpiTests {
    /**
     * {@link io.higgs.s3.spi.SpiImpl1} and {@link io.higgs.s3.spi.SpiImpl2}
     * uses {@link io.higgs.common.annotations.tests.SpiAnnotation},
     * {@link io.higgs.common.annotations.tests.SpiInterface} and
     * {@link io.higgs.common.annotations.tests.SpiClass}.
     * <p/>
     * A file should be generated for each.
     */
    @Test
    public void testIfSpiFileIsGenerated() {
        InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(SpiAnnotation.class.getName());
        //if the file was found then we're good to go
        assertNotNull(in);

        in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(SpiInterface.class.getName());
        assertNotNull(in);

        in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(SpiClass.class.getName());
        assertNotNull(in);
    }
}
