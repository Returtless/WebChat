import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class Client extends Thread {
    private final SocketChannel socketChannel;
    private final ConnectionManager connectionManager;

    public Client(SocketChannel socketChannel, ConnectionManager cm) {
        this.socketChannel = socketChannel;
        this.connectionManager = cm;
        start();
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message message = readMessage();
                switch (message.getType()) {
                    //установка имени
                    case 1 -> {
                        System.out.println("Пришло имя");
                        setName(message.getText());
                    }
                    //сообщение
                    case 2 -> sendMessageToClients(new Message(2, getName() + ": " + message.getText()));
                    //выход
                    case 3 -> {
                        System.out.println("Закрываемся");
                        socketChannel.finishConnect();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Закрываемся!");

    }

    private Message readMessage() throws IOException {
        String msg = null;
        while (msg == null) {
            ByteBuffer inputBuffer = ByteBuffer.allocate(2 << 10);
            int bytesCount = socketChannel.read(inputBuffer);
            if (bytesCount == -1) {
                throw new IOException("Ошибка чтения сообщения");
            }
            msg = new String(inputBuffer.array(), 0, bytesCount, StandardCharsets.UTF_8);
            inputBuffer.clear();
            System.out.println(msg);
        }
        return new Message(msg);
    }

    public void sendMessage(Message message) throws IOException {
        socketChannel.write(ByteBuffer.wrap((message.toString()).getBytes(StandardCharsets.UTF_8)));
    }

    private void sendMessageToClients(Message message) throws IOException {
        for (Client client : this.connectionManager.getClients()) {
            //if (!client.equals(this)) {
            client.sendMessage(message);
            //}
        }
    }

}
