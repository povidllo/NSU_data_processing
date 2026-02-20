import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class Client {

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 8081);
        String name = "mymeme";
        OutputStream out = socket.getOutputStream();
        out.write(name.getBytes("ASCII"));
        out.write(0);
        out.flush();
    }
}
