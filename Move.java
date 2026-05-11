// Tracks a single move in a Connect 4 game.
public class Move {
    private final int player;        // 1 or 2
    private final int column;        // 0-6
    private final long timestamp;    // milliseconds since epoch

    public Move(int player, int column, long timestamp) {
        this.player = player;
        this.column = column;
        this.timestamp = timestamp;
    }

    public int getPlayer() {
        return player;
    }

    public int getColumn() {
        return column;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("Move(player=%d, column=%d, time=%d)", player, column, timestamp);
    }
}
