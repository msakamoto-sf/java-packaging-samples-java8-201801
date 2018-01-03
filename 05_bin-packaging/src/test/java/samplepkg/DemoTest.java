package samplepkg;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DemoTest {
    @Test
    public void testSha1Demo() throws Exception {
        Demo demo = new Demo();
        assertEquals("aef214d4e978d44c0a7dbd6bd760992cdf04465b", demo.sha1demo("Hello, World", 10));
    }
}
