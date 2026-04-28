import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) throws Exception {
        String host = args.length>0?args[0]:"localhost";
        int port = args.length>1?Integer.parseInt(args[1]):3000;
        Socket s = new Socket(host, port);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

        // reader thread
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) System.out.println("[SERVER] " + line);
            } catch (IOException e) { System.out.println("Disconnected from server."); }
        }).start();

        // send user input
        String input;
        while ((input = stdin.readLine()) != null) {
            out.println(input);
            if (input.equalsIgnoreCase("QUIT")) break;
        }
        s.close();
    }
}