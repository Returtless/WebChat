import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Message {
    private Commands type;
    private String login;
    private String text;

    public static String DELIMETER = "@#!";

    public Message(Commands type, String login, String text) {
        this.type = type;
        this.login = login;
        this.text = text;
    }

    public Message(Commands type, String text) {
        this.type = type;
        this.login = "";
        this.text = text;
    }

    public Message(String text) {
        try {
            String[] splittedMsg = text.split(DELIMETER);
            this.type = Commands.getValue(splittedMsg[0]);
            this.login = splittedMsg[1];
            if (splittedMsg.length == 3) {
                this.text = splittedMsg[2];
            }
        } catch (Exception e) {
            System.out.println("Ошибка разбора пришедшего сообщения");
        }
    }

    @Override
    public String toString() {
        return type + DELIMETER + login + DELIMETER + text;
    }

    public ByteBuffer toBuffer() {
        return ByteBuffer.wrap(toString().getBytes(StandardCharsets.UTF_8));
    }

    public void toChat() {
        System.out.println(login.isBlank() ? text : login + ": " + text);
    }

    public Commands getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String getLogin() {
        return login;
    }
}
