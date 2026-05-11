import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	public static final int PORT = 3000;
	public static void main(String[] args) throws Exception {
		// Initialize persistence manager first.
		PersistenceManager persistence = new PersistenceManager();
		System.out.println("[SERVER] Persistence initialized. ");
		
		// Initialize lobby with persistence.
		LobbyManager lobby = new LobbyManager(persistence);
		System.out.println("[SERVER] Loaded ");
		
		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			System.out.println("Server listening on " + PORT);
			while (true) {
				Socket client = serverSocket.accept();
				System.out.println("Client connected: " + client.getRemoteSocketAddress());
				ClientHandler handler = new ClientHandler(client, lobby);
				new Thread(handler).start();
			}
		}
	}
}
