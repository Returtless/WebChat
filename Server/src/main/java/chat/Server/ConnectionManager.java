package chat.Server;

import chat.Commands;
import chat.Message;
import chat.Props;

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

    private final ConcurrentSkipListSet<String> listOfNames = new ConcurrentSkipListSet<>();
    private Set<SocketChannel> clientChannels;
    private ServerSocketChannel serverSocket = null;
    private Selector selector = null;

    public ConnectionManager() {
        clientChannels = new HashSet<>();
        try {
            clientChannels = new HashSet<>();
            serverSocket = ServerSocketChannel.open();
            serverSocket.configureBlocking(false);
            Props properties = new Props();
            properties.loadSettings();
            serverSocket.socket().bind(new InetSocketAddress(properties.getHost(), properties.getPort()));
            selector = Selector.open();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        } catch (Exception exc) {
            System.out.println("Сервер выключен");
            exc.printStackTrace();
            System.exit(1);
        }
        System.out.println("Сервер запущен");
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
        SocketChannel cc = (SocketChannel) key.channel();
        if (!cc.isOpen()) {
            return;
        }
        StringBuilder request = new StringBuilder();
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.clear();
            while (cc.read(buffer) > 0) {
                buffer.flip();
                request.append(new String(buffer.array(), buffer.position(),
                        buffer.limit(), StandardCharsets.UTF_8));
                buffer.clear();
            }

            Message message = processRequest(cc, new Message(request.toString()));

            assert message != null;
            if (message.getType() != Commands.ERROR || !(message.getType() == Commands.LOGIN && message.getLogin() == null)) {
                sendMessages(message, cc);
            } else {
                sendMessage(cc, message);
            }
        } catch (Exception exc) {
            clientChannels.remove(cc);
            System.out.println("Ошибка при чтении: " + exc.getMessage());
            try {
                cc.close();
                cc.socket().close();
            } catch (Exception e) {
            }
        }
    }

    private void sendMessages(Message message,  SocketChannel mySocketChannel) {
        for (Iterator<SocketChannel> i = clientChannels.iterator(); i.hasNext(); )
            try {
                SocketChannel client = i.next();
                if ((!mySocketChannel.equals(client) || message.getLogin().isBlank()) && client.isConnected()) {
                    client.write(message.toBuffer());
                }
            } catch (IOException e) {
                System.out.println("Ошибка при отправке: " + e.getMessage());
                i.remove();
            }
    }

    private void sendMessage(SocketChannel client, Message message) throws IOException {
        if (client.isConnected()) {
            client.write(message.toBuffer());
        }
    }

    private Message processRequest(SocketChannel cc, Message message) throws IOException {
        switch (message.getType()) {
            case LOGIN -> {
                if (listOfNames.contains(message.getLogin())) {
                    return new Message(Commands.ERROR, "Данный логин уже занят!");
                }
                System.out.printf("Новый пользователь: %s подключен%n", message.getLogin());
                return new Message(Commands.SEND, message.getLogin() + " вошел в чат");
            }
            case SEND -> {
                System.out.printf("%s: %s%n", message.getLogin(), message.getText());
                return message;
            }
            case LOGOUT -> {
                clientChannels.remove(cc);
                cc.close();
                System.out.printf("%s вышел из чата%n", message.getLogin());
                return new Message(Commands.SEND, message.getLogin() + " вышел из чата");
            }
            default -> {
                System.out.printf("Неизвестная команда \"%s\"", message.getType());
                return null;
            }
        }
    }
}
