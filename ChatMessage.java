// Saves a single chat message in a game room.
public class ChatMessage {
    private final String sender;     // Player name
    private final String message;    // Message content
    private final long timestamp;    // milliseconds since epoch

    public ChatMessage(String sender, String message, long timestamp) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s: %s", timestamp, sender, message);
    }
}
