import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	public static final int PORT = 3000;

	public static void main(String[] args) {
		System.out.println("Starting server on port " + PORT);
		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			System.out.println("Waiting for a client...");
			try (Socket client = serverSocket.accept()) {
				System.out.println("Client connected from " + client.getRemoteSocketAddress());
				BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				PrintWriter out = new PrintWriter(client.getOutputStream(), true);
				out.println("Hello from server");
				String line = in.readLine();
				System.out.println("Received: " + line);
			}
		} catch (IOException e) {
			System.err.println("Server error: " + e.getMessage());
			e.printStackTrace();
		}
		System.out.println("Server stopped.");
	}
}
