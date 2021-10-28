public class Main {

    public static void main(String[] args) {
        ClientManager clientManager = new ClientManager();
        clientManager.connect();
        clientManager.login();
        clientManager.read();
    }
}

