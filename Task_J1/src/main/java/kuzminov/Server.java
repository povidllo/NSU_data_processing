package kuzminov;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class Server {
    private static class Keys {
        KeyPair keys;
        X509Certificate cert;

        Keys(KeyPair keys, X509Certificate cert) {
            this.keys = keys;
            this.cert = cert;
        }
    }
    static ConcurrentHashMap<String, Future<Keys>> keys = new ConcurrentHashMap<>();
    static ExecutorService pool;
    static X500Name issuerName;
    static PrivateKey issuerPrivateKey;

    public static void main(String[] args) throws IOException {
        if(args.length < 3) {
            throw new IllegalArgumentException("Enter <poolCount> <port> <issuerName>");
        }
        System.out.println(Arrays.toString(args));
        int poolCount = Integer.parseInt(args[0]);
//        int poolCount = 4;
        pool = Executors.newFixedThreadPool(poolCount);

        Selector selector = Selector.open();
        int port = Integer.parseInt(args[1]);
//        int port = 8081;
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started with port " + port);

        issuerName = new X500Name("CN="+args[2]);
//        issuerName = new X500Name("CN=kekes");
        try {
            byte[] bytes = Files.readAllBytes(new File("issuerKey.key").toPath());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            issuerPrivateKey = kf.generatePrivate(spec);
            System.out.println("load key");
        } catch (Exception e) {
            throw new IOException("Can't load private key");
        }

        while(true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();

            Iterator<SelectionKey> iter = keys.iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                if (key.isAcceptable()) {
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    if (socketChannel != null) {
                        socketChannel.configureBlocking(false);
                        ByteBuffer buf = ByteBuffer.allocate(2048);
                        socketChannel.register(selector, SelectionKey.OP_READ, buf);
                    }
                } else if(key.isReadable()) {
                    SocketChannel client = (SocketChannel)key.channel();
                    ByteBuffer buf = (ByteBuffer) key.attachment();
                    int read = client.read(buf);
                    if(read == -1) {
                        client.close();
                    } else if(read > 0) {
//                        добавить обработку фрагментированных сообщений
                        buf.flip();
                        byte[] byteBuf = new byte[buf.remaining()];
                        buf.get(byteBuf);
                        String name = new String(byteBuf);
                        processClient(name, client);
                        buf.clear();
                    }
                }
                iter.remove();
            }
        }
    }

    static void processClient(String name, SocketChannel client) {
        Future<Keys> pair = keys.computeIfAbsent(name, k -> pool.submit(() -> {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(8192);
                KeyPair keyPair = kpg.generateKeyPair();

                System.out.println("Finish processing");
                X500Name subjectName = new X500Name("CN="+name);
                Date notBefore = new Date(System.currentTimeMillis());
                Date notAfter = new Date(System.currentTimeMillis() + 60 * 60 * 1000);
                BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());

                X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                        issuerName,
                        serialNumber,
                        notBefore,
                        notAfter,
                        subjectName,
                        SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded())
                );
                ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuerPrivateKey);
                X509CertificateHolder certificateHolder = certBuilder.build(signer);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert = (X509Certificate) cf.generateCertificate(
                        new ByteArrayInputStream(certificateHolder.getEncoded())
                );
            System.out.println("Created certificate");
            return new Keys(keyPair, cert);
            })
        );

        try {
            pool.submit(() -> {
                try {
                    sendToClient(pair, client);
                } catch (Exception e) {
                    System.out.println("Can't send keys");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void sendToClient(Future<Keys> pair, SocketChannel client) throws Exception {
        Keys result = pair.get();
        System.out.println("get pair");
        byte[] certBytes = result.cert.getEncoded();
        byte[] privateKeyBytes = result.keys.getPrivate().getEncoded();

        ByteBuffer buffer = ByteBuffer.allocate(8 + certBytes.length + privateKeyBytes.length);
        buffer.putInt(certBytes.length);
        buffer.put(certBytes);
        buffer.putInt(privateKeyBytes.length);
        buffer.put(privateKeyBytes);
        buffer.flip();

        client.write(buffer);
        System.out.println("send");

    }
}