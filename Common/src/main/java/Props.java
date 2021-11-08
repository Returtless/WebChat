import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Props {
    private static int port;
    private static String host;
    private static int bufferSize;
    public static String FILEPATH = "settings.xml";
    public static String PORT_TAG = "port";
    public static String HOST_TAG = "host";
    public static String BUFFERS_TAG = "bufferSize";


    static {
        Properties settings = new Properties();
        try {
            settings.loadFromXML(new FileInputStream(FILEPATH));
            port = Integer.parseInt(settings.getProperty(PORT_TAG));
            host = settings.getProperty(HOST_TAG);
            bufferSize = Integer.parseInt(settings.getProperty(BUFFERS_TAG));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getPort() {
        return port;
    }

    public static String getHost() {
        return host;
    }

    public static int getBufferSize() {
        return bufferSize;
    }
}
