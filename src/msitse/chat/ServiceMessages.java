package msitse.chat;

/**
 * Created by Valerii Volkov
 */
public enum ServiceMessages {
    CONNECTION_ESTABLISHED("Connection established"),
    CLIENT_QUITED_THE_CHAT("Decided to quit the chat"),
    KICK("The client is kicked out: ");

    private String message;

    ServiceMessages(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
