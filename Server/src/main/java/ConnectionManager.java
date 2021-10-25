import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectionManager extends Thread {
    private CopyOnWriteArrayList<Client> clients;
    // private ServerSocketChannel serverChannel;
    public static String HOST = "localhost";

    public ConnectionManager() {
        //this.serverChannel = serverChannel;
        this.clients = new CopyOnWriteArrayList<>();
        start();
    }

    @Override
    public void run() {
        try {
            final ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(HOST, 8080));
            while (true) {
                SocketChannel socketChannel = serverChannel.accept();
                clients.add(new Client(socketChannel, this));
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public CopyOnWriteArrayList<Client> getClients() {
        return clients;
    }
}
