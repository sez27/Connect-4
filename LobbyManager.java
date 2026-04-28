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
			client.sendMessage("You are in the lobby. Use LIST_ROOMS, CREATE_ROOM, or JOIN_ROOM <id>");
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
			sb.append(String.format("[%d] %s\n", r.getId(), r.getSummary()));
		}
		return sb.toString().trim();
	}

	// create a new room and the client who makes the room is the first player
	public synchronized GameRoom createRoom(ClientHandler roomOwner) {
		int id = nextRoomId.getAndIncrement();
		GameRoom room = new GameRoom(id, roomOwner, this);
		rooms.put(id, room);
		return room;
	}

	// join an existing room by id
	public synchronized GameRoom joinRoom(int roomId, ClientHandler joiningPlayer) {
		GameRoom room = rooms.get(roomId);
		if (room == null) return null;
		synchronized (room) {
			if (room.isFull()) return null;
			room.addPlayer(joiningPlayer);
			return room;
		}
	}

	// remove a room from the lobby
	public void removeRoom(int roomId) {
		rooms.remove(roomId);
	}
}
