import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

public class LobbyManager {
	private final ConcurrentMap<Integer, GameRoom> rooms = new ConcurrentHashMap<>();
	private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    
    // creates unique room ID using atomic integer to avoid race conditions
	private final AtomicInteger nextRoomId = new AtomicInteger(1);

	public LobbyManager() {}

    // connected clients are added to the lobby and sent instructions
    public void addClient(ClientHandler client) {
        if (client != null) {
            clients.addIfAbsent(client);
            client.sendMessage("You are in the lobby. Use LIST_ROOMS, CREATE_ROOM [password], or JOIN_ROOM <id> [password]");
        }
    }

	// remove a client from the lobby
	public void removeClient(ClientHandler client) {
		if (client != null) {
			clients.remove(client);
		}
	}

    // returns a list of rooms
    public String listRooms() {
        if (rooms.isEmpty()) return "(no rooms)";
        StringBuilder sb = new StringBuilder();
        for (GameRoom r : rooms.values()) {
            String privateTag = r.isPrivate() ? " (private)" : "";
            sb.append(String.format("[%d] %s%s\n", r.getId(), r.getSummary(), privateTag));
        }
        return sb.toString().trim();
    }

    // create a new room and the client who makes the room is the first player
    public synchronized GameRoom createRoom(ClientHandler roomOwner) {
        return createRoom(roomOwner, null);
    }

    // create a new room with optional password
    public synchronized GameRoom createRoom(ClientHandler roomOwner, String password) {
        int id = nextRoomId.getAndIncrement();
        GameRoom room = new GameRoom(id, roomOwner, this, password);
        rooms.put(id, room);
        return room;
    }

    // join an existing room by id (public)
    public synchronized GameRoom joinRoom(int roomId, ClientHandler joiningPlayer) {
        return joinRoom(roomId, joiningPlayer, null);
    }

    // join an existing room by id with optional password check
    public synchronized GameRoom joinRoom(int roomId, ClientHandler joiningPlayer, String password) {
        GameRoom room = rooms.get(roomId);
        if (room == null) {
            return null;
        }
        synchronized (room) {
            if (room.isFull()) {
                joiningPlayer.sendMessage("Room is full.");
                return null;
            }
            if (room.isPrivate() && !room.verifyPassword(password)) {
                joiningPlayer.sendMessage("Incorrect password for room " + roomId);
                return null;
            }
            room.addPlayer(joiningPlayer);
            return room;
        }
    }

	// remove a room from the lobby
	public void removeRoom(int roomId) {
		rooms.remove(roomId);
	}

    // broadcast a message to all clients in the lobby
	public void broadcastToLobby(String message) {
    for (ClientHandler c : clients) {
        c.sendMessage(message);
    }
	}
}
