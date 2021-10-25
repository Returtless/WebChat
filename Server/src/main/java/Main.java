import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class Main {
    public static String HOST = "localhost";

    public static void main(String[] args) throws IOException {
        Props props = new Props();
        props.loadSettings();
        new ConnectionManager();
    }

    public static void connectionManager(){

    }
}
