public class Message {
    private int type;
    private String text;

    public static String DELIMETER = "@#!";

    public Message(int type, String text) {
        this.type = type;
        this.text = text;
    }

    public Message(String text) {
        try {
            String[] splittedMsg = text.split(DELIMETER);
            this.type = Integer.parseInt(splittedMsg[0]);
            this.text = splittedMsg[1];
        } catch (Exception e) {
            System.out.println("Ошибка разбора пришедшего сообщения");
        }
    }

    @Override
    public String toString() {
        return type + DELIMETER + text;
    }

    public int getType() {
        return type;
    }

    public String getText() {
        return text;
    }
}
