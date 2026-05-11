import java.util.*;

public class GameRoom {
	private final int id;
	private final LobbyManager lobbyManager;
	private ClientHandler player1;
	private ClientHandler player2;
	private boolean started = false;

	private final boolean isPrivate;
	private final String password; //optional password for private rooms
	
	private GameState gameState;
	private Timer gameTimer;
	private boolean gameEnded;

	// Constructor with optional password.
	public GameRoom(int id, ClientHandler owner, LobbyManager lobbyManager, String password) {
		this.id = id;
		this.lobbyManager = lobbyManager;
		this.player1 = owner;
		if (owner != null) owner.setCurrentRoom(this);
		this.password = (password != null && !password.isEmpty()) ? password : null;
		this.isPrivate = this.password != null;
		this.gameState = new GameState();
		this.gameTimer = new Timer();
		this.gameEnded = false;
	}

    // Gets the room id.
	public int getId() {
		return id;
	}

    // Gets the summary of the room status, player names, and game status.
	public synchronized String getSummary() {
		String p1 = player1 != null ? player1.getClientName() : "(empty)";
		String p2 = player2 != null ? player2.getClientName() : "(empty)";
		String status = started ? "playing" : "waiting";
		return String.format("%s vs %s — %s", p1, p2, status);
	}

    // Checks if the room has two players and is full.
	public synchronized boolean isFull() {
		return player1 != null && player2 != null;
	}

	// Indicates if room is private.
	public boolean isPrivate() {
		return isPrivate;
	}

	// Simple password check.
	public boolean verifyPassword(String attempt) {
		if (!isPrivate) return true;
		return attempt != null && attempt.equals(password);
	}

    // Adds a player to the room, if the room is full, sends a message to the client and does not add them.
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

    // Removes a player from the room, broadcasts a message to remaining player.
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

    // Broadcasts a message to both players in the room, sender is null if it's a system message.
	public synchronized void broadcastMessage(String message, ClientHandler sender) {
		if (player1 != null) player1.sendMessage(message);
		if (player2 != null) player2.sendMessage(message);
	}

    // Starts the game when both players have joined and sends a message about the game starting and roles.
	public synchronized void startGame() {
		if (started) return;
		started = true;
		gameEnded = false;
		broadcastMessage("Game starting in room " + id + "!", null);
		if (player1 != null) player1.sendMessage("You are player 1 (RED)");
		if (player2 != null) player2.sendMessage("You are player 2 (YELLOW)");
		broadcastBoard();
		gameTimer.startTimer(this, GameState.getPlayer1());
	}

	// Processes a move from a player.
	public synchronized boolean makeMove(ClientHandler player, int column) {
		if (gameEnded) {
			player.sendMessage("Game is over. Cannot make moves.");
			return false;
		}

		int playerId;
		if (player.equals(player1)) {
			playerId = GameState.getPlayer1();
		} else if (player.equals(player2)) {
			playerId = GameState.getPlayer2();
		} else {
			player.sendMessage("You are not in this game.");
			return false;
		}

		if (gameState.getCurrentTurn() != playerId) {
			player.sendMessage("It's not your turn.");
			return false;
		}

		gameTimer.cancelTimer();

		if (column < 0 || column >= GameState.getCols()) {
			player.sendMessage("Invalid column number. Use 0-6.");
			gameTimer.startTimer(this, playerId);
			return false;
		}

		if (!gameState.makeMove(column, playerId)) {
			player.sendMessage("That column is full or invalid. Choose another.");
			gameTimer.startTimer(this, playerId);
			return false;
		}

		broadcastMessage(player.getClientName() + " placed a piece in column " + column, null);
		broadcastBoard();

		if (gameState.checkWin(playerId)) {
			endGame(playerId, "WIN");
			return true;
		}

		if (gameState.isBoardFull()) {
			endGame(0, "DRAW");
			return true;
		}

		gameState.switchTurn();
		int nextPlayer = gameState.getCurrentTurn();
		broadcastMessage("It's now player " + nextPlayer + "'s turn.", null);
		gameTimer.startTimer(this, nextPlayer);

		return true;
	}

	// Processes a timeout when the current player doesn't move within 30 seconds.
	public synchronized void processTimeout(int playerId) {
		if (gameEnded) return;
		if (gameState.getCurrentTurn() != playerId) return;

		int column = getRandomValidColumn();

		if (column == -1) {
			endGame(0, "DRAW");
			return;
		}

		String playerName = (playerId == GameState.getPlayer1()) ? player1.getClientName() : player2.getClientName();
		broadcastMessage(playerName + " didn't move in time! Auto-placed piece in column " + column, null);

		if (gameState.makeMove(column, playerId)) {
			broadcastBoard();

			if (gameState.checkWin(playerId)) {
				endGame(playerId, "WIN");
				return;
			}

			if (gameState.isBoardFull()) {
				endGame(0, "DRAW");
				return;
			}

			gameState.switchTurn();
			int nextPlayer = gameState.getCurrentTurn();
			broadcastMessage("It's now player " + nextPlayer + "'s turn.", null);
			gameTimer.startTimer(this, nextPlayer);
		}
	}

	// Ends the game with a result (WIN or DRAW).
	private void endGame(int winnerId, String result) {
		gameEnded = true;
		gameTimer.cancelTimer();

		if ("DRAW".equals(result)) {
			broadcastMessage("GAME OVER: It's a draw! Board is full.", null);
		} else if ("WIN".equals(result)) {
			String winnerName = (winnerId == GameState.getPlayer1()) ?
				(player1 != null ? player1.getClientName() : "Player 1") :
				(player2 != null ? player2.getClientName() : "Player 2");
			broadcastMessage("GAME OVER: " + winnerName + " (Player " + winnerId + ") wins!", null);
		}

		broadcastBoard();
	}

	// Finds a random valid column for timeout move.
	private int getRandomValidColumn() {
		List<Integer> validColumns = new ArrayList<>();
		for (int col = 0; col < GameState.getCols(); col++) {
			if (gameState.isValidMove(col)) {
				validColumns.add(col);
			}
		}

		if (validColumns.isEmpty()) {
			return -1;
		}

		Random rand = new Random();
		return validColumns.get(rand.nextInt(validColumns.size()));
	}

	// Broadcasts the current board state to both players in JSON format for debug.
	private void broadcastBoard() {
		String boardJSON = gameState.getBoardAsJSON();
		broadcastMessage("BOARD:" + boardJSON, null);
	}
}
