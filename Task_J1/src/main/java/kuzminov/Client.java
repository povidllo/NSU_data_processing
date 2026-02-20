package kuzminov;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

public class Client {

    public static void main(String[] args) throws Exception {
        if(args.length < 4) {
            throw new IllegalArgumentException("Enter <port> <subjectName> <delay> <abort>");
        }
        System.out.println(Arrays.toString(args));
        Socket socket = new Socket("localhost", Integer.parseInt(args[0]));
//        String name = "mymeme";
        String name = args[1];
        OutputStream out = socket.getOutputStream();
        out.write(name.getBytes("ASCII"));
        out.write(0);
        out.flush();

        boolean abort = Boolean.parseBoolean(args[3]);
        if(abort) {
            socket.close();
            return;
        }

        long sleepTime = Long.parseLong(args[2]);
        if(sleepTime > 0) {
            Thread.sleep(sleepTime * 1000L);
        }

        InputStream input = socket.getInputStream();

        byte[] idBuffer = new byte[4];
        read(input, idBuffer, 4);
        int id = ByteBuffer.wrap(idBuffer).getInt();


        byte[] certLengthBuffer = new byte[4];
        read(input, certLengthBuffer, 4);
        int certLength = ByteBuffer.wrap(certLengthBuffer).getInt();

        byte[] certBytes = new byte[certLength];
        read(input, certBytes, certLength);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
        Files.write(Paths.get("subjectCert_" + id + ".cert"), cert.getEncoded());
//        System.out.println(cert);

        byte[] privateKeyLengthBuffer = new byte[4];
        read(input, privateKeyLengthBuffer, 4);
        int privateKeyLength = ByteBuffer.wrap(privateKeyLengthBuffer).getInt();

        byte[] privateKeyBytes = new byte[privateKeyLength];
        read(input, privateKeyBytes, privateKeyLength);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(keySpec);

        Files.write(Paths.get("subjectPrivateKey_" + id + ".key"), privateKey.getEncoded());
    }

    public static void read(InputStream input, byte[] buffer, int length) throws IOException {
        int pos = 0;
        while(pos < length) {
            int read = input.read(buffer, pos, length - pos);
            if(read == -1) {
                throw new IOException("Can't read socket");
            }
            pos+= read;
        }
    }
}
