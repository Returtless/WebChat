import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientManager {
    private final Logger LOG = LogManager.getLogger(ClientManager.class);
    private SocketChannel channel;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private String nickname;

    public static String EXIT = "/exit";
    public static int TIMEOUT = 200;
    public static int EXIT_STATUS = 1;

    public void start() {
        this.connect();
        this.login();
        this.read();
    }

    public void connect() {
        String host = Props.getHost();
        int port = Props.getPort();
        try {
            if (isConnected.get()) {
                return;
            }
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            if (!channel.isOpen()) {
                channel = SocketChannel.open();
            }
            channel.connect(new InetSocketAddress(host, port));
            System.out.printf("Подключение к серверу %s:%d%n", host, port);
            LOG.info("Подключение к серверу {}:{}", host, port);
            while (!channel.finishConnect()) {
                try {
                    Thread.sleep(TIMEOUT);
                } catch (Exception exc) {
                    return;
                }
            }
            isConnected.set(true);
            System.out.println("Подключение прошло успешно!");
            LOG.info("Подключение к серверу прошло успешно");
        } catch (IOException exc) {
            LOG.error("Ошибка подключения к серверу {}, {}",
                    host, exc.getLocalizedMessage());
            System.exit(EXIT_STATUS);
        }
    }

    public void login() {
        try {
            BufferedReader inputMessage = new BufferedReader((new InputStreamReader(System.in)));
            ByteBuffer buffer = ByteBuffer.allocate(Props.getBufferSize());
            boolean isErrorLogged = false;
            while (true) {
                if (isErrorLogged) {
                    LOG.warn("Ошибка выбора логина");
                    System.out.println("Указанный логин занят!");
                }
                LOG.warn("Выбор логина");
                System.out.println("Введите логин");
                nickname = inputMessage.readLine();
                sendMessage(new Message(Commands.LOGIN, nickname, ""));
                while (true) {
                    if (channel.read(buffer) > 0) {
                        buffer.flip();
                        String response = new String(buffer.array(), 0, buffer.limit());
                        buffer.clear();
                        isErrorLogged = new Message(response).getType().equals(Commands.ERROR);
                        break;
                    }
                }
                if (!isErrorLogged) {
                    new Thread(new ServerListener(channel, isConnected)).start();
                    LOG.info("Подключение к серверу с логином {} прошло успешно", nickname);
                    System.out.println("Вы вошли в чат");
                    break;
                }
            }
        } catch (IOException exc) {
            System.out.printf("Ошибка авторизации при подключении к серверу %s, %s%n",
                    Props.getHost(), exc.getLocalizedMessage());
            LOG.error("Ошибка авторизации при подключении к серверу {}, {}",
                    Props.getHost(), exc.getLocalizedMessage());
            System.exit(EXIT_STATUS);
        }
    }

    public void read() {
        try {
            BufferedReader inputMessage = new BufferedReader((new InputStreamReader(System.in)));
            String msg;
            while (true) {
                msg = inputMessage.readLine();
                if (msg != null) {
                    if (EXIT.equals(msg)) {
                        disconnect();
                        break;
                    }
                    final Message message = new Message(Commands.SEND, nickname, msg);
                    message.toLog(LOG);
                    sendMessage(message);
                }
            }
        } catch (IOException exc) {
            System.out.printf("Ошибка в работе клиента: %s%n",
                    exc.getLocalizedMessage());
            LOG.error("Ошибка в работе клиента: {}",
                    exc.getLocalizedMessage());
            System.exit(EXIT_STATUS);
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
            LOG.error("Ошибка отключения от сервера {}", Props.getHost());
            System.out.printf("Ошибка отключения %s%n", Props.getHost());
            System.exit(EXIT_STATUS);
        }
    }

    private void sendMessage(Message message) throws IOException {
        if (channel.isConnected()) {
            channel.write(message.toBuffer());
        }
    }
}

