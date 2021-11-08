import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

public class ConnectionManager extends Thread {
    private static final Logger LOG = LogManager.getLogger(ConnectionManager.class);

    private final ConcurrentSkipListSet<String> listOfNames = new ConcurrentSkipListSet<>();
    private Set<SocketChannel> clientChannels;
    private ServerSocketChannel serverSocket = null;
    private Selector selector = null;

    public static int EXIT_STATUS = 1;

    public ConnectionManager() {
        LOG.info("Инициализация сервера");
        try {
            clientChannels = new HashSet<>();
            serverSocket = ServerSocketChannel.open();
            serverSocket.configureBlocking(false);
            serverSocket.socket().bind(new InetSocketAddress(Props.getHost(), Props.getPort()));
            selector = Selector.open();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        } catch (Exception e) {
            LOG.error("Отключение сервера : {}", e.getMessage());
            System.exit(EXIT_STATUS);
        }
        LOG.info("Сервер запущен");
        start();
    }

    @Override
    public void run() {
        while (true) try {
            selector.select();
            for (Iterator i = selector.selectedKeys().iterator(); i.hasNext(); i.remove()) {
                SelectionKey key = (SelectionKey) i.next();
                if (key.isAcceptable()) {
                    accept(key);
                }
                if (key.isReadable()) {
                    read(key);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private void accept(SelectionKey key) throws IOException {
        SocketChannel sc = serverSocket.accept();
        clientChannels.add(sc);
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        if (!socketChannel.isOpen()) {
            return;
        }
        StringBuilder request = new StringBuilder();
        try {
            ByteBuffer buffer = ByteBuffer.allocate(Props.getBufferSize());
            buffer.clear();
            while (socketChannel.read(buffer) > 0) {
                buffer.flip();
                request.append(new String(buffer.array(), buffer.position(),
                        buffer.limit(), StandardCharsets.UTF_8));
                buffer.clear();
            }

            Message message = processRequest(socketChannel, new Message(request.toString()));

            assert message != null;

            if (message.getType() == Commands.ERROR || (message.getType() == Commands.LOGIN && message.getLogin() == null)) {
                sendMessage(socketChannel, message);
            } else {
                sendMessages(message, socketChannel);
            }
        } catch (Exception exc) {
            clientChannels.remove(socketChannel);
            LOG.error("Ошибка при чтении: {}", exc.getMessage());
            try {
                socketChannel.close();
                socketChannel.socket().close();
            } catch (Exception e) {
                LOG.error("Ошибка закрытия сокета: {}", e.getMessage());
            }
        }
    }

    private void sendMessages(Message message, SocketChannel mySocketChannel) {
        for (Iterator<SocketChannel> i = clientChannels.iterator(); i.hasNext(); )
            try {
                SocketChannel client = i.next();
                if ((!mySocketChannel.equals(client) || message.getLogin().isBlank()) && client.isConnected()) {
                    client.write(message.toBuffer());
                }
            } catch (IOException e) {
                LOG.error("Ошибка при отправке: {}", e.getMessage());
                i.remove();
            }
    }

    private void sendMessage(SocketChannel client, Message message) throws IOException {
        if (client.isConnected()) {
            client.write(message.toBuffer());
        }
    }

    private Message processRequest(SocketChannel socketChannel, Message message) throws IOException {
        final String login = message.getLogin();
        switch (message.getType()) {
            case LOGIN -> {
                if (listOfNames.contains(login)) {
                    return new Message(Commands.ERROR, "Данный логин уже занят!");
                }
                listOfNames.add(login);
                LOG.info("Новый пользователь: {} подключен", login);
                return new Message(Commands.SEND, login + " вошел в чат");
            }
            case SEND -> {
                LOG.info("{}: {}", login, message.getText());
                return message;
            }
            case LOGOUT -> {
                clientChannels.remove(socketChannel);
                listOfNames.remove(login);
                socketChannel.close();
                LOG.info("{} вышел из чата", login);
                return new Message(Commands.SEND, login + " вышел из чата");
            }
            default -> {
                LOG.warn("Неизвестная команда \"{}\"", message.getType());
                return null;
            }
        }
    }
}
