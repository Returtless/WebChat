package chat.Client;

import chat.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

class ServerListener implements Runnable {
    SocketChannel channel;
    private final AtomicBoolean isConnected;

    public ServerListener(SocketChannel channel, AtomicBoolean isConnected) {
        this.channel = channel;
        this.isConnected = isConnected;
    }

    public void run() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            while (isConnected.get()) {
                buffer.clear();
                if (channel.read(buffer) > 0) {
                    buffer.flip();
                    String response = new String(buffer.array(), 0, buffer.limit());
                    new Message(response).toChat();
                }
            }
            channel.close();
            channel.socket().close();
            System.out.println("Сервер выключился!");
        } catch (IOException e) {
            System.out.println("Ошибка: " + e.getMessage());
            isConnected.set(false);
        }
    }
}
