package samplepkg;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Demo {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws Exception {
        Demo demo = new Demo();
        System.out.println(demo.sha1demo2("Hello, World".getBytes(StandardCharsets.ISO_8859_1)));
    }

    public String sha1demo2(byte[] src) throws Exception {
        byte[] output = sha1demo(src);
        StringBuilder sb = new StringBuilder();
        for (byte b : output) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public byte[] sha1demo(byte[] src) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA1", "BC");
        return md.digest(src);
    }
}
