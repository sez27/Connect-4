import java.io.*;
import java.nio.file.*;
import java.util.*;

// Handles all file I/O for game state and chat persistence.
public class PersistenceManager {
    private static final String GAMES_DIR = "data/games";
    private static final String CHATS_DIR = "data/chats";

    public PersistenceManager() {
        initializeDirectories();
    }

    // Initializes required directories if they don't exist.
    private void initializeDirectories() {
        try {
            Files.createDirectories(Paths.get(GAMES_DIR));
            Files.createDirectories(Paths.get(CHATS_DIR));
            System.out.println("[PERSISTENCE] Directories initialized: " + GAMES_DIR + ", " + CHATS_DIR);
        } catch (IOException e) {
            System.err.println("[PERSISTENCE] Failed to create directories: " + e.getMessage());
        }
    }

    // Saves a game room's state to JSON file.
    public void saveGameRoom(GameRoom room) {
        if (room == null) return;

        try {
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"roomId\":").append(room.getId()).append(",");
            json.append("\"player1\":\"").append(escapeJson(room.getPlayer1() != null ? room.getPlayer1().getClientName() : "")).append("\",");
            json.append("\"player2\":\"").append(escapeJson(room.getPlayer2() != null ? room.getPlayer2().getClientName() : "")).append("\",");
            json.append("\"createdAt\":").append(room.getCreatedAt()).append(",");
            json.append("\"status\":\"").append(room.isGameStarted() ? "playing" : "waiting").append("\",");
            json.append("\"isPrivate\":").append(room.isPrivate()).append(",");
            
            // Save move history
            json.append("\"moveHistory\":[");
            List<Move> moves = room.getMoveHistory();
            for (int i = 0; i < moves.size(); i++) {
                Move move = moves.get(i);
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"player\":").append(move.getPlayer()).append(",");
                json.append("\"column\":").append(move.getColumn()).append(",");
                json.append("\"timestamp\":").append(move.getTimestamp());
                json.append("}");
            }
            json.append("],");
            
            // Save current board state
            json.append("\"currentBoard\":").append(room.getBoardAsJSON());
            json.append("}");

            String filepath = GAMES_DIR + "/room_" + room.getId() + ".json";
            Files.write(Paths.get(filepath), json.toString().getBytes());
            System.out.println("[PERSISTENCE] Saved game room " + room.getId());
        } catch (Exception e) {
            System.err.println("[PERSISTENCE] Failed to save game room " + room.getId() + ": " + e.getMessage());
        }
    }

    // Escapes special characters in JSON strings.
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    // Loads a specific game room from disk.
    public String loadGameRoom(int roomId) {
        try {
            String filepath = GAMES_DIR + "/room_" + roomId + ".json";
            return new String(Files.readAllBytes(Paths.get(filepath)));
        } catch (Exception e) {
            System.err.println("[PERSISTENCE] Failed to load game room " + roomId + ": " + e.getMessage());
            return null;
        }
    }

    // Loads all saved game rooms from disk. Called on server startup to restore previous games.
    public List<String> loadAllGames() {
        List<String> games = new ArrayList<>();
        try {
            File gamesFolder = new File(GAMES_DIR);
            if (!gamesFolder.exists()) {
                return games;
            }

            File[] files = gamesFolder.listFiles((dir, name) -> name.startsWith("room_") && name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        String content = new String(Files.readAllBytes(file.toPath()));
                        games.add(content);
                        System.out.println("[PERSISTENCE] Loaded game from " + file.getName());
                    } catch (Exception e) {
                        System.err.println("[PERSISTENCE] Skipped corrupted file " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[PERSISTENCE] Error loading games: " + e.getMessage());
        }
        return games;
    }

    // Saves a chat message to the room's chat file.
    public void saveChatMessage(int roomId, String sender, String message) {
        try {
            String filepath = CHATS_DIR + "/room_" + roomId + ".txt";
            long timestamp = System.currentTimeMillis();
            String chatLine = String.format("[%d] %s: %s%n", timestamp, sender, message);
            Files.write(Paths.get(filepath), chatLine.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.err.println("[PERSISTENCE] Failed to save chat message for room " + roomId + ": " + e.getMessage());
        }
    }

    // Loads all chat messages for a specific room.
    public List<ChatMessage> loadChatHistory(int roomId) {
        List<ChatMessage> messages = new ArrayList<>();
        try {
            String filepath = CHATS_DIR + "/room_" + roomId + ".txt";
            if (!Files.exists(Paths.get(filepath))) {
                return messages;  // empty list if file doesn't exist
            }

            List<String> lines = Files.readAllLines(Paths.get(filepath));
            for (String line : lines) {
                try {
                    // format: [timestamp] sender: message
                    int bracketEnd = line.indexOf("]");
                    if (bracketEnd == -1) continue;

                    long timestamp = Long.parseLong(line.substring(1, bracketEnd));
                    String rest = line.substring(bracketEnd + 2);  // Skip "] "
                    int colonPos = rest.indexOf(": ");
                    if (colonPos == -1) continue;

                    String sender = rest.substring(0, colonPos);
                    String text = rest.substring(colonPos + 2);
                    messages.add(new ChatMessage(sender, text, timestamp));
                } catch (Exception e) {
                    System.err.println("[PERSISTENCE] Skipped malformed chat line: " + line);
                }
            }
            System.out.println("[PERSISTENCE] Loaded " + messages.size() + " chat messages for room " + roomId);
        } catch (Exception e) {
            System.err.println("[PERSISTENCE] Failed to load chat history for room " + roomId + ": " + e.getMessage());
        }
        return messages;
    }

    // Deletes a game room's game state and chat history.
    public void deleteGameRoom(int roomId) {
        try {
            Files.deleteIfExists(Paths.get(GAMES_DIR + "/room_" + roomId + ".json"));
            Files.deleteIfExists(Paths.get(CHATS_DIR + "/room_" + roomId + ".txt"));
            System.out.println("[PERSISTENCE] Deleted game room " + roomId);
        } catch (Exception e) {
            System.err.println("[PERSISTENCE] Failed to delete game room " + roomId + ": " + e.getMessage());
        }
    }

}