package samplepkg;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class DemoTest {
    @Test
    public void testSha1Demo() throws Exception {
        Demo demo = new Demo();
        assertEquals("907d14fb3af2b0d4f18c2d46abe8aedce17367bd",
                demo.sha1demo2("Hello, World".getBytes(StandardCharsets.ISO_8859_1)));
    }
}
