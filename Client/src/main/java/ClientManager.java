import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientManager {

    private final Props properties = new Props();
    private SocketChannel channel;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private String nickname;


    public void connect() {
        properties.loadSettings();
        try {
            if (isConnected.get()) {
                return;
            }
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            if (!channel.isOpen()) {
                channel = SocketChannel.open();
            }
            channel.connect(new InetSocketAddress(properties.getHost(), properties.getPort()));
            System.out.printf("Подключение к серверу %s:%d%n", properties.getHost(), properties.getPort());
            while (!channel.finishConnect()) {
                try {
                    Thread.sleep(200);
                } catch (Exception exc) {
                    return;
                }
            }
            isConnected.set(true);
            System.out.println("Подключение прошло успешно!");
        } catch (IOException exc) {
            System.out.printf("Ошибка подключения к серверу %s, %s%n",
                    properties.getHost(), exc.getLocalizedMessage());
            System.exit(1);
        }
    }

    public void login() {
        try {
            BufferedReader inputMessage = new BufferedReader((new InputStreamReader(System.in)));
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            boolean isErrorLogged = false;
            while (true) {
                if (isErrorLogged) {
                    System.out.println("Указанный логин занят!");
                }
                System.out.println("Введите логин");
                nickname = inputMessage.readLine();
                sendMessage(new Message(Commands.LOGIN, nickname, ""));
                while (true) {
                    if (channel.read(buffer) > 0) {
                        buffer.flip();
                        String response = new String(buffer.array(), 0, buffer.limit());
                        buffer.clear();
                        if (!new Message(response).getType().equals(Commands.ERROR)) {
                            break;
                        } else {
                            isErrorLogged = true;
                        }
                    }
                }
                if (!isErrorLogged){
                    new Thread(new ServerListener(channel, isConnected)).start();
                    break;
                }
            }
        } catch (IOException exc) {
            System.out.printf("Ошибка авторизации при подключении к серверу %s, %s%n",
                    properties.getHost(), exc.getLocalizedMessage());
            System.exit(1);
        }
    }

    public void read() {
        try {
            BufferedReader inputMessage = new BufferedReader((new InputStreamReader(System.in)));
            String msg = null;
            System.out.println("Вы вошли в чат");
            while (true) {
                msg = inputMessage.readLine();
                if (msg != null) {
                    if ("exit".equals(msg)) {
                        disconnect();
                        break;
                    }
                    sendMessage(new Message(Commands.SEND, nickname, msg));
                }
            }
        } catch (IOException exc) {
            System.out.printf("Ошибка в работе клиента: %s%n",
                    exc.getLocalizedMessage());
            System.exit(1);
        }
    }

    private void disconnect() {
        if (!isConnected.get()) {
            return;
        }
        try {
            sendMessage(new Message(Commands.LOGOUT, nickname, ""));
            isConnected.set(false);
        } catch (Exception exc) {
            System.out.printf("Ошибка отключения %s%n", properties.getHost());
            System.exit(3);
        }
    }

    private void sendMessage(Message message) throws IOException {
        if (channel.isConnected()) {
            channel.write(message.toBuffer());
        }
    }


}

