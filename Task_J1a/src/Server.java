import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Server {

    static ConcurrentHashMap<String, Future<KeyPair>> keys = new ConcurrentHashMap<>();
    static ExecutorService pool;

    public static void main(String[] args) throws IOException {
//        if(args.length < 2) {
//            throw new IllegalArgumentException();
//        }
//        int poolCount = Integer.getInteger(args[0]);
        int poolCount = 4;
        pool = Executors.newFixedThreadPool(poolCount);

        Selector selector = Selector.open();
//        int port = Integer.getInteger(args[1]);
        int port = 8081;
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started with port " + port);

//        String issuerName = args[2];
        String issuerName = "kekes";

        while(true) {
            selector.select();
            Set<SelectionKey> keys = selector.selectedKeys();

            for (var key : keys) {
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
                        processClient(name);
                        buf.clear();
//                        buf.flip();
//                        while (buf.hasRemaining()) {
//                            byte b = buf.get();
//                            if (b == 0) {
//                                int length = buf.position() - 1;
//                                byte[] nameBytes = new byte[length];
//                                buf.position(0);
//                                buf.get(nameBytes);
//                                String name = new String(nameBytes);
//
//                                System.out.println("Name: " + name);
//                                processClient(name);
//                                buf.flip();
//                                buf.clear();
//                            }
//                        }
//
//                        buf.compact();
                    }
                }
            }
        }
    }

    static void processClient(String name) {
        Future<KeyPair> pair = keys.computeIfAbsent(name, k -> pool.submit(() -> {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(8192);
                KeyPair keyPair = kpg.generateKeyPair();
                System.out.println("Finish processing");
                return keyPair;
            })
        );

        try {
            pair.get();
            System.out.println("get pair");

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

}