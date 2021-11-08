
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

class ServerListener implements Runnable {
    private final Logger LOG = LogManager.getLogger(ServerListener.class);
    private final SocketChannel channel;
    private final AtomicBoolean isConnected;

    public ServerListener(SocketChannel channel, AtomicBoolean isConnected) {
        this.channel = channel;
        this.isConnected = isConnected;
    }

    public void run() {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(Props.getBufferSize());
            while (isConnected.get()) {
                buffer.clear();
                if (channel.read(buffer) > 0) {
                    buffer.flip();
                    final String response = new String(buffer.array(), 0, buffer.limit());
                    final Message message = new Message(response);
                    message.toChat();
                    message.toLog(LOG);
                }
            }
            channel.close();
            channel.socket().close();
            System.out.println("Вы отключились!");
            LOG.info("Вы отключились от сервера");
        } catch (IOException e) {
            System.out.println("Ошибка получения данных от сервера: " + e.getMessage());
            LOG.error("Ошибка получения данных от сервера: " + e.getMessage());
            isConnected.set(false);
        }
    }
}
