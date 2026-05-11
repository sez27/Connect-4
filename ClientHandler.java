import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;
    private LobbyManager lobbyManager;
    private GameRoom currentRoom;
    private boolean isConnected;

    // initializes a new client connection handler
    public ClientHandler(Socket socket, LobbyManager lobbyManager) {
        this.socket = socket;
        this.lobbyManager = lobbyManager;
        this.isConnected = true;
        // if stream setup fails, close connection
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Error setting up streams: " + e.getMessage());
            closeConnection();
        }
    }

    @Override
    // executes when the handler is started as a thread
    public void run() {
        try {
            out.println("Welcome to Connect 4 Server! Please enter your name:");
            // reads name input
            String name = in.readLine();
            if (name != null && !name.trim().isEmpty()) {
                this.clientName = name.trim();
                out.println("Hello, " + clientName + "! You are now in the lobby.");
                lobbyManager.addClient(this);
            } else {
                out.println("Invalid name. Disconnecting."); // invalid name closes connection
                closeConnection();
                return;
            }

            String command;
            // command loop continously reads commands from client
            while (isConnected && (command = in.readLine()) != null) {
                processCommand(command.trim());
            }
        } catch (IOException e) {
            // logs error message if client disconnects randomly
            System.err.println("Client " + clientName + " disconnected: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void processCommand(String command) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0].toUpperCase();
        String arg = parts.length > 1 ? parts[1] : "";

        // set of commands
        switch (cmd) {
            case "LIST_ROOMS":
                // list available rooms
                out.println("Available rooms: " + lobbyManager.listRooms());
                break;
            case "CREATE_ROOM":
                if (currentRoom == null) {
                    String password = arg.isEmpty() ? null : arg;
                    GameRoom room = lobbyManager.createRoom(this, password);
                    currentRoom = room;
                    out.println("Room created (id " + room.getId() + "). Waiting for another player..." + (room.isPrivate() ? " (private)" : ""));
                } else {
                    out.println("You are already in a room.");
                }
                break;
            case "JOIN_ROOM":
                if (currentRoom == null && !arg.isEmpty()) {
                    // may include id and optional password: "<id> [password]"
                    String[] joinParts = arg.split(" ", 2);
                    try {
                        int roomId = Integer.parseInt(joinParts[0]);
                        String password = joinParts.length > 1 ? joinParts[1] : null;
                        GameRoom room = lobbyManager.joinRoom(roomId, this, password);
                        if (room != null) {
                            currentRoom = room;
                            out.println("Joined room " + roomId + ". Game starting!");
                        } else {
                            if (!lobbyManager.listRooms().contains("[" + roomId + "]")) {
                                out.println("Room not found.");
                            }
                        }
                    } catch (NumberFormatException e) {
                        out.println("Invalid room ID.");
                    }
                } else {
                    out.println("You are already in a room or invalid command.");
                }
                break;
            case "LEAVE_ROOM":
                if (currentRoom != null) {
                    currentRoom.removePlayer(this);
                    currentRoom = null;
                    out.println("You left the room. Back to lobby.");
                } else {
                    out.println("You are not in a room.");
                }
                break;
            case "CHAT":
                if (currentRoom != null && !arg.isEmpty()) {
                    String message = clientName + ": " + arg;
                    currentRoom.broadcastMessage(message, this);
                    currentRoom.recordChatMessage(clientName, arg);
                } else {
                    out.println("You must be in a room to chat.");
                }
                break;
            case "PLAY_AGAIN":
                if (currentRoom != null) {
                    currentRoom.resetForNewGame();
                } else {
                    out.println("Cannot start new game or no room joined.");
                }
                break;
            case "MOVE":
                if (currentRoom != null && !arg.isEmpty()) {
                    try {
                        int column = Integer.parseInt(arg);
                        currentRoom.makeMove(this, column);
                    } catch (NumberFormatException e) {
                        out.println("Invalid column number.");
                    }
                } else {
                    out.println("You must be in a room to make a move.");
                }
                break;
            case "QUIT":
                out.println("Goodbye!");
                closeConnection();
                break;
            case "ALL":
                if (!arg.isEmpty()) {
                    lobbyManager.broadcastToLobby(clientName + ": " + arg);
                } else {
                    out.println("Usage: LOBBY_CHAT <message>");
                }
                break;
            default:
                out.println("Unknown command: " + cmd);
                break;
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public String getClientName() {
        return clientName;
    }

    public GameRoom getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(GameRoom room) {
        this.currentRoom = room;
    }

    private void closeConnection() {
        isConnected = false;
        if (currentRoom != null) {
            currentRoom.removePlayer(this);
        }
        lobbyManager.removeClient(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}
