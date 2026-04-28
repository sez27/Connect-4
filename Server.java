import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	public static final int PORT = 3000;
	public static void main(String[] args) throws Exception {
		LobbyManager lobby = new LobbyManager();
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
