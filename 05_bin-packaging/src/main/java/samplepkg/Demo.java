package samplepkg;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.google.common.base.Strings;

public class Demo {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {
        Demo demo = new Demo();
        System.out.println(demo.sha1demo("Hello, World", 10));
    }

    public String sha1demo(String src, int count) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA1", "BC");
        src = Strings.repeat(src, count);
        byte[] output = md.digest(src.getBytes(StandardCharsets.ISO_8859_1));
        StringBuilder sb = new StringBuilder();
        for (byte b : output) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
