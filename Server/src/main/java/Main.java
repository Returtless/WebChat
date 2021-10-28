
import java.io.IOException;

public class Main {
    public static String HOST = "localhost";

    public static void main(String[] args) throws IOException {
        Props props = new Props();
        props.loadSettings();
        new ConnectionManager();
    }
}
