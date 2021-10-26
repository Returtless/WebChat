package chat;

public enum Commands {
    LOGIN("1"),
    SEND("2"),
    LOGOUT("3"),
    ERROR("4");

    private final String type;

    Commands(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static Commands getValue(String value) {
        for(Commands e: Commands.values()) {
            if(e.type.equals(value)) {
                return e;
            }
        }
        return null;
    }
}
