import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

public class Test {

    String testStr = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    @org.junit.Test
    public void compressByte() throws IOException {
        byte[] input = testStr.getBytes();
        byte[] op = compress(input);
        System.out.println("original data length " + testStr.length() + ",  compressed data length " + new String(op).length());
        System.out.println("original data length " + input.length + ",  compressed data length " + op.length);
        byte[] org = decompress(op);
        System.out.println(org.length);
        System.out.println(new String(org, StandardCharsets.UTF_8));
    }

    @org.junit.Test
    public void compress() throws IOException {

        String op = compressAndReturnB64(testStr);
        System.out.println("Compressed data b64" + op);
        String org = decompressB64(op);
        System.out.println("Original text" + org);
    }

    public static String compressAndReturnB64(String text) throws IOException {
        return new String(Base64.getEncoder().encode(compress(text)));
    }

    public static String decompressB64(String b64Compressed) throws IOException {
        byte[] decompressedBArray = decompress(Base64.getDecoder().decode(b64Compressed));
        return new String(decompressedBArray, StandardCharsets.UTF_8);
    }

    public static byte[] compress(String text) throws IOException {
        return compress(text.getBytes());
    }

    public static byte[] compress(byte[] bArray) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (DeflaterOutputStream dos = new DeflaterOutputStream(os)) {
            dos.write(bArray);
        }
        return os.toByteArray();
    }

    public static byte[] decompress(byte[] compressedTxt) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (OutputStream ios = new InflaterOutputStream(os)) {
            ios.write(compressedTxt);
        }

        return os.toByteArray();
    }

    @org.junit.Test
    public void test1(){
        ZoneId zoneId = ZoneId.systemDefault();
        System.out.println(zoneId);
    }

}

