package chat;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Props {
    private int port;
    private String host;
    public static String FILEPATH = "settings.xml";
    public static String PORT_TAG = "port";
    public static String HOST_TAG = "host";


    public void saveSettings() {
        Properties saveProps = new Properties();
        saveProps.setProperty(PORT_TAG, Integer.toString(8080));
        saveProps.setProperty(HOST_TAG, "localhost");
        try {
            saveProps.storeToXML(new FileOutputStream(FILEPATH), "");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void loadSettings() {
        Properties loadProps = new Properties();
        try {
            loadProps.loadFromXML(new FileInputStream(FILEPATH));
            port = Integer.parseInt(loadProps.getProperty(PORT_TAG));
            host = loadProps.getProperty(HOST_TAG);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }
}
