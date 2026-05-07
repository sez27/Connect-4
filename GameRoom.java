import java.util.*;

public class GameRoom {
	private final int id;
	private final LobbyManager lobbyManager;
	private ClientHandler player1;
	private ClientHandler player2;
	private boolean started = false;

	private final boolean isPrivate;
	private final String password; //optional password for private rooms

	// constructor with optional password
	public GameRoom(int id, ClientHandler owner, LobbyManager lobbyManager, String password) {
		this.id = id;
		this.lobbyManager = lobbyManager;
		this.player1 = owner;
		if (owner != null) owner.setCurrentRoom(this);
		this.password = (password != null && !password.isEmpty()) ? password : null;
		this.isPrivate = this.password != null;
	}

    // returns the room id
	public int getId() {
		return id;
	}

    // returns a summary of the room status, player names, and game status
	public synchronized String getSummary() {
		String p1 = player1 != null ? player1.getClientName() : "(empty)";
		String p2 = player2 != null ? player2.getClientName() : "(empty)";
		String status = started ? "playing" : "waiting";
		return String.format("%s vs %s — %s", p1, p2, status);
	}

    // checks if the room has two players and is full
	public synchronized boolean isFull() {
		return player1 != null && player2 != null;
	}

	// indicates if room is private
	public boolean isPrivate() {
		return isPrivate;
	}

	// simple password check
	public boolean verifyPassword(String attempt) {
		if (!isPrivate) return true;
		return attempt != null && attempt.equals(password);
	}

    // adds a player to the room, if the room is full, sends a message to the client and does not add them
	public synchronized void addPlayer(ClientHandler c) {
		if (c == null) return;
		if (player1 == null) {
			player1 = c;
			c.setCurrentRoom(this);
			broadcastMessage(c.getClientName() + " joined the room", null);
		} else if (player2 == null) {
			player2 = c;
			c.setCurrentRoom(this);
			broadcastMessage(c.getClientName() + " joined the room", null);
		} else {
			c.sendMessage("Room is full.");
			return;
		}

		if (isFull()) startGame();
	}

    // removes a player from the room, broadcasts a message to remaining player
	public synchronized void removePlayer(ClientHandler c) {
		if (c == null) return;
		boolean wasParticipant = false;
		if (c.equals(player1)) {
			player1 = null;
			wasParticipant = true;
		} else if (c.equals(player2)) {
			player2 = null;
			wasParticipant = true;
		}
		if (wasParticipant) {
			broadcastMessage(c.getClientName() + " left the room", null);
			c.setCurrentRoom(null);
		}

		// if room empty, remove it from lobby
		if (player1 == null && player2 == null) {
			lobbyManager.removeRoom(id);
		}
	}

    // broadcasts a message to both players in the room, sender is null if it's a system message
	public synchronized void broadcastMessage(String message, ClientHandler sender) {
		if (player1 != null) player1.sendMessage(message);
		if (player2 != null) player2.sendMessage(message);
	}

    //starts the game when both players have joined and sends a message about the game starting and roles
	public synchronized void startGame() {
		if (started) return;
		started = true;
		broadcastMessage("Game starting in room " + id + "!", null);
		if (player1 != null) player1.sendMessage("You are player 1");
		if (player2 != null) player2.sendMessage("You are player 2");
	}
}
