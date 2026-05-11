import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.*;

public class GameClient extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String clientName;
    private int playerNumber;
    private boolean inGame;

    private ImageIcon redPiece;
    private ImageIcon yellowPiece;

    private CardLayout cardLayout;
    private JPanel mainPanel;

    private LobbyPanel lobbyPanel;
    private GamePanel gamePanel;

    public GameClient() {
        setTitle("Connect 4 Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 700);
        setLocationRelativeTo(null);
        setResizable(false);

        // Load and scale piece images to fit buttons
        ImageIcon redOriginal = new ImageIcon("images/red_piece.png");
        Image redImg = redOriginal.getImage();
        Image redScaled = redImg.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
        redPiece = new ImageIcon(redScaled);
        
        ImageIcon yellowOriginal = new ImageIcon("images/yellow_piece.png");
        Image yellowImg = yellowOriginal.getImage();
        Image yellowScaled = yellowImg.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
        yellowPiece = new ImageIcon(yellowScaled);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        showLoginDialog();
    }

    private void showLoginDialog() {
        String name = JOptionPane.showInputDialog(this, "Enter your name:", "Connect 4 Login", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) {
            System.exit(0);
        }
        this.clientName = name.trim();
        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 3000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String welcomeMessage = in.readLine();
            System.out.println("[SERVER] " + welcomeMessage);

            out.println(clientName);
            String confirmMessage = in.readLine();
            System.out.println("[SERVER] " + confirmMessage);

            this.inGame = false;
            this.playerNumber = 0;

            lobbyPanel = new LobbyPanel(this);
            gamePanel = new GamePanel(this);

            mainPanel.add(lobbyPanel, "LOBBY");
            mainPanel.add(gamePanel, "GAME");
            add(mainPanel);

            cardLayout.show(mainPanel, "LOBBY");
            setVisible(true);

            new Thread(this::readServerMessages).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void readServerMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[SERVER] " + message);

                // Skip timer warning messages (no need in GUI)
                if (message.startsWith("WARNING:") || message.contains("10 seconds") || message.contains("30 seconds")) {
                    continue;
                }

                // Check specific message types FIRST, before generic checks
                if (message.startsWith("BOARD:")) {
                    String boardJson = message.substring(6);
                    System.out.println("DEBUG: Received board JSON: " + boardJson);
                    gamePanel.updateBoard(boardJson);
                } else if (message.startsWith("YOUR_TURN:")) {
                    gamePanel.setYourTurn(true);
                    gamePanel.logMessage(">>> Your turn! Type /MOVE <column> or use text input");
                } else if (message.startsWith("GAME OVER:")) {
                    gamePanel.setYourTurn(false);
                    gamePanel.logMessage(message);
                } else if (message.startsWith("You are player")) {
                    // Handle "You are player X" message
                    if (message.contains("player 1")) {
                        this.playerNumber = 1;
                    } else {
                        this.playerNumber = 2;
                    }
                    gamePanel.setPlayerInfo(message);
                    gamePanel.logMessage(message);
                } else if (message.contains("Game starting") || message.contains("game starting")) {
                    // Switch to game panel and start game
                    this.inGame = true;
                    cardLayout.show(mainPanel, "GAME");
                    gamePanel.logMessage("Game started!");
                } else if (message.startsWith("It's now player")) {
                    // Check if this message is about your turn by comparing player numbers
                    boolean isYourTurn = false;
                    if (playerNumber == 1 && message.contains("player 1")) {
                        isYourTurn = true;
                    } else if (playerNumber == 2 && message.contains("player 2")) {
                        isYourTurn = true;
                    }
                    gamePanel.setYourTurn(isYourTurn);
                    gamePanel.logMessage(message);
                } else if (message.contains("didn't move in time")) {
                    gamePanel.logMessage(message);
                } else if (message.contains("placed a piece")) {
                    gamePanel.logMessage(message);
                } else if (message.contains("Back to Lobby") || message.equals("You left the room. Back to lobby.")) {
                    // Switch back to lobby when you leave
                    this.inGame = false;
                    cardLayout.show(mainPanel, "LOBBY");
                    gamePanel.setYourTurn(false);
                    lobbyPanel.displayServerMessage("You have returned to the lobby.");
                } else if (message.contains("joined the room") || message.contains("left the room")) {
                    gamePanel.logMessage(message);
                } else if (message.contains("Invalid") || message.contains("invalid")) {
                    gamePanel.logMessage("[ERROR] " + message);
                } else if (message.startsWith("Available rooms:")) {
                    // Display room list in lobby
                    if (!inGame) {
                        lobbyPanel.displayServerMessage(message);
                    }
                } else if (message.startsWith("PLAYER_WINS:")) {
                    // Handle player win count update
                    try {
                        int wins = Integer.parseInt(message.substring(12));
                        gamePanel.setPlayerWins(wins);
                        gamePanel.logMessage("Your wins: " + wins);
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing PLAYER_WINS message: " + e.getMessage());
                    }
                } else if (message.startsWith("OPPONENT_WINS:")) {
                    // Handle opponent win count update (could display if desired)
                    try {
                        int wins = Integer.parseInt(message.substring(14));
                        gamePanel.logMessage("Opponent wins: " + wins);
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing OPPONENT_WINS message: " + e.getMessage());
                    }
                } else if (message.contains(":") && inGame) {
                    // Chat message format: "name: message" - only in game
                    gamePanel.logChat(message);
                } else {
                    // Default: display in appropriate panel
                    if (!inGame) {
                        if (!message.isEmpty() && !message.equals("You are now in the lobby.")) {
                            lobbyPanel.displayServerMessage(message);
                        }
                    } else {
                        if (!message.isEmpty()) {
                            gamePanel.logMessage(message);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Disconnected from server: " + e.getMessage());
        }
    }

    public void sendCommand(String command) {
        if (out != null) {
            if (command.startsWith("QUIT") || command.startsWith("DISCONNECT")) {
                out.println(command);
                System.exit(0);  // Close immediately
            }
            // Handle password-protected commands
            if (command.startsWith("CREATE_ROOM")) {
                String[] parts = command.split(" ", 2);
                String password = (parts.length > 1 && !parts[1].trim().isEmpty()) ? parts[1].trim() : null;
                
                // Only prompt if no password was already provided by the button
                if (password == null) {
                    password = JOptionPane.showInputDialog(this, "Enter password (leave empty for public room):", "");
                }
                if (password != null) {
                    if (password.isEmpty()) {
                        out.println("CREATE_ROOM");
                    } else {
                        out.println("CREATE_ROOM " + password);
                    }
                }
            } else if (command.startsWith("JOIN_ROOM")) {
                String[] parts = command.split(" ", 3);  // Split into id, password, and anything else
                if (parts.length < 2) {
                    JOptionPane.showMessageDialog(this, "Usage: /JOIN_ROOM <room_id>", "Invalid Input", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                String roomId = parts[1].trim();
                String password = (parts.length > 2 && !parts[2].trim().isEmpty()) ? parts[2].trim() : null;
                
                // Only prompt if no password was already provided by the button
                if (password == null) {
                    password = JOptionPane.showInputDialog(this, "Enter password (leave empty for public room):", "");
                }
                if (password != null) {
                    if (password.isEmpty()) {
                        out.println("JOIN_ROOM " + roomId);
                    } else {
                        out.println("JOIN_ROOM " + roomId + " " + password);
                    }
                }
            } else {
                out.println(command);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameClient());
    }

    // ==================== LOBBY PANEL ====================
    private class LobbyPanel extends JPanel {
        private GameClient client;
        private JTextArea messageArea;

        public LobbyPanel(GameClient client) {
            this.client = client;
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Header
            JLabel headerLabel = new JLabel("Connect 4 Lobby - Welcome, " + clientName);
            headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
            add(headerLabel, BorderLayout.NORTH);

            // Message area
            messageArea = new JTextArea();
            messageArea.setEditable(false);
            messageArea.setLineWrap(true);
            messageArea.setWrapStyleWord(true);
            messageArea.setText("Welcome to the lobby!\n\nType chat messages or use buttons:\n");
            JScrollPane scrollPane = new JScrollPane(messageArea);
            add(scrollPane, BorderLayout.CENTER);

            // Bottom panel with command input + buttons
            JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));

            // Command input section
            JPanel commandPanel = new JPanel(new BorderLayout(5, 5));
            commandPanel.setBorder(BorderFactory.createTitledBorder("Chat (use /all for lobby chat)"));
            JTextField commandInput = new JTextField();
            JButton sendBtn = new JButton("Send");
            commandPanel.add(commandInput, BorderLayout.CENTER);
            commandPanel.add(sendBtn, BorderLayout.EAST);

            Runnable sendCommand = () -> {
                String input = commandInput.getText().trim();
                if (!input.isEmpty()) {
                    if (input.startsWith("/")) {
                        String cmd = input.substring(1).toUpperCase();
                        if (!cmd.isEmpty()) {
                            client.sendCommand(cmd);
                        } else {
                            displayServerMessage("[Invalid] Empty command after /");
                        }
                    } else {
                        // No "/" prefix - treat as /ALL chat message
                        client.sendCommand("ALL " + input);
                    }
                    commandInput.setText("");
                }
            };

            sendBtn.addActionListener(e -> sendCommand.run());
            commandInput.addActionListener(e -> sendCommand.run());

            bottomPanel.add(commandPanel, BorderLayout.NORTH);

            // Button panel below chat
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            buttonPanel.setBorder(BorderFactory.createTitledBorder("Room Commands"));

            JButton listBtn = new JButton("List Rooms");
            listBtn.addActionListener(e -> client.sendCommand("LIST_ROOMS"));

            JButton createBtn = new JButton("Create Room");
            createBtn.addActionListener(e -> {
                String password = JOptionPane.showInputDialog(this,
                    "Enter password (leave blank for public room):",
                    "Create Room", JOptionPane.PLAIN_MESSAGE);
                if (password != null) {
                    client.sendCommand("CREATE_ROOM " + (password.isEmpty() ? "" : password));
                }
            });

            JButton joinBtn = new JButton("Join Room");
            joinBtn.addActionListener(e -> {
                String roomId = JOptionPane.showInputDialog(this,
                    "Enter room ID:", "Join Room", JOptionPane.PLAIN_MESSAGE);
                if (roomId != null && !roomId.isEmpty()) {
                    try {
                        int id = Integer.parseInt(roomId);
                        String password = JOptionPane.showInputDialog(this,
                            "Enter password (if room is private):",
                            "Join Room", JOptionPane.PLAIN_MESSAGE);
                        if (password != null) {
                            client.sendCommand("JOIN_ROOM " + id + (password.isEmpty() ? "" : " " + password));
                        }
                    } catch (NumberFormatException ex) {
                        displayServerMessage("[Error] Invalid room ID");
                    }
                }
            });

            buttonPanel.add(listBtn);
            buttonPanel.add(createBtn);
            buttonPanel.add(joinBtn);

            bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
            add(bottomPanel, BorderLayout.SOUTH);
        }

        public void displayServerMessage(String message) {
            messageArea.append(message + "\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        }
    }

    // ==================== GAME PANEL ====================
    private class GamePanel extends JPanel {
        private GameClient client;
        private BoardPanel boardPanel;
        private TimerPanel timerPanel;
        private ChatPanel chatPanel;
        private GameLogPanel gameLogPanel;
        private InfoPanel infoPanel;

        public GamePanel(GameClient client) {
            this.client = client;
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // Top: Info and Timer
            JPanel topPanel = new JPanel(new BorderLayout(10, 0));
            infoPanel = new InfoPanel();
            timerPanel = new TimerPanel();
            topPanel.add(infoPanel, BorderLayout.WEST);
            topPanel.add(timerPanel, BorderLayout.EAST);
            add(topPanel, BorderLayout.NORTH);

            // Center: Board (gets remaining space)
            boardPanel = new BoardPanel(client);
            add(boardPanel, BorderLayout.CENTER);

            // Bottom: Chat and Game Log with fixed height
            JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 10, 0));
            bottomPanel.setPreferredSize(new Dimension(800, 200));  // Fixed height
            chatPanel = new ChatPanel(client);
            gameLogPanel = new GameLogPanel();
            bottomPanel.add(chatPanel);
            bottomPanel.add(gameLogPanel);
            add(bottomPanel, BorderLayout.SOUTH);
        }

        public void updateBoard(String boardJson) {
            boardPanel.updateBoard(boardJson);
        }

        public void setYourTurn(boolean isYourTurn) {
            boardPanel.setEnabled(isYourTurn);
            if (isYourTurn) {
                timerPanel.startCountdown();
            } else {
                timerPanel.stopCountdown();
            }
        }

        public void setPlayerInfo(String info) {
            infoPanel.setGameInfo(info);
            infoPanel.setPlayerIcon(playerNumber, info);
        }

        public void setPlayerWins(int wins) {
            infoPanel.setWins(wins);
        }

        public void logMessage(String message) {
            gameLogPanel.logMessage(message);
        }

        public void logChat(String message) {
            chatPanel.logChat(message);
        }
    }

    // ==================== BOARD PANEL ====================
    private class BoardPanel extends JPanel {
        private GameClient client;
        private JButton[][] buttons;
        private int[][] board;
        private static final int ROWS = 6;
        private static final int COLS = 7;

        public BoardPanel(GameClient client) {
            this.client = client;
            this.board = new int[ROWS][COLS];
            setLayout(new GridLayout(ROWS, COLS, 2, 2));
            setBorder(BorderFactory.createTitledBorder("Game Board"));
            setBackground(new Color(100, 150, 200));

            buttons = new JButton[ROWS][COLS];
            // Add buttons in ROW-MAJOR order for GridLayout
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    final int column = col;
                    JButton button = new JButton();
                    button.setBackground(Color.WHITE);
                    button.setFocusPainted(false);
                    button.setFont(new Font("Arial", Font.BOLD, 14));
                    button.addActionListener(e -> client.sendCommand("MOVE " + column));
                    buttons[row][col] = button;
                    add(button);
                }
            }
            setEnabled(true);
        }
        // Updates board based on parsed JSON
        public void updateBoard(String boardJson) {
            try {
                System.out.println("DEBUG: BoardPanel.updateBoard() called with: " + boardJson);
                board = parseBoardJson(boardJson);
                System.out.println("DEBUG: Board parsed successfully");
                for (int row = 0; row < ROWS; row++) {
                    for (int col = 0; col < COLS; col++) {
                        JButton button = buttons[row][col];
                        int piece = board[row][col];
                        if (piece == 0) {
                            button.setBackground(Color.WHITE);
                            button.setText("");
                            button.setIcon(null);
                        } else if (piece == 1) {
                            // Player 1 (Red)
                            button.setBackground(new Color(255, 100, 100));
                            button.setIcon(redPiece);
                            button.setFont(new Font("Arial", Font.BOLD, 24));
                            button.setForeground(Color.RED);
                        } else if (piece == 2) {
                            // Player 2 (Yellow)
                            button.setBackground(new Color(255, 255, 100));
                            button.setIcon(yellowPiece);
                            button.setFont(new Font("Arial", Font.BOLD, 24));
                            button.setForeground(new Color(200, 150, 0));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error parsing board: " + e.getMessage());
                e.printStackTrace();
            }
            repaint();  // Force repaint of board
        }

        private int[][] parseBoardJson(String json) {
            int[][] parsedBoard = new int[ROWS][COLS];
            try {
                // Extract the board array from JSON: {"board":[...], "rows":...}
                int boardStart = json.indexOf("\"board\":[");
                int boardEnd = json.indexOf(",\"rows\"");
                
                if (boardStart == -1 || boardEnd == -1) {
                    System.err.println("ERROR: Invalid board JSON format");
                    return parsedBoard;
                }
                
                // Extract the 2D array part
                String boardArrayStr = json.substring(boardStart + 9, boardEnd);  // Skip "\"board\":["
                System.out.println("DEBUG: Board array extracted: " + boardArrayStr);
                
                // Remove outer brackets
                boardArrayStr = boardArrayStr.substring(1, boardArrayStr.length() - 1);
                
                // Split by row separator: ],[ 
                String[] rows = boardArrayStr.split("\\],\\[");
                
                for (int row = 0; row < ROWS && row < rows.length; row++) {
                    // Clean up any remaining brackets from each row
                    String rowStr = rows[row].replaceAll("[\\[\\]]", "");
                    String[] cols = rowStr.split(",");
                    
                    for (int col = 0; col < COLS && col < cols.length; col++) {
                        parsedBoard[row][col] = Integer.parseInt(cols[col].trim());
                    }
                }
                System.out.println("DEBUG: Board parsed successfully");
            } catch (Exception e) {
                System.err.println("Parse error: " + e.getMessage());
                e.printStackTrace();
            }
            return parsedBoard;
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    buttons[row][col].setEnabled(enabled);
                }
            }
        }
    }

    // ==================== TIMER PANEL ====================
    private class TimerPanel extends JPanel {
        private JLabel timerLabel;
        private int remainingTime = 30;
        private javax.swing.Timer timer;

        public TimerPanel() {
            setLayout(new FlowLayout(FlowLayout.RIGHT));
            setBorder(BorderFactory.createTitledBorder("Timer"));

            timerLabel = new JLabel("30s");
            timerLabel.setFont(new Font("Arial", Font.BOLD, 20));
            timerLabel.setForeground(Color.BLACK);
            add(timerLabel);
        }

        public void startCountdown() {
            remainingTime = 30;
            if (timer != null) {
                timer.stop();
            }
            timer = new javax.swing.Timer(1000, e -> {
                remainingTime--;
                if (remainingTime <= 10) {
                    timerLabel.setForeground(new Color(255, 100, 100));
                } else {
                    timerLabel.setForeground(Color.BLACK);
                }
                timerLabel.setText(remainingTime + "s");
                if (remainingTime <= 0) {
                    ((javax.swing.Timer) e.getSource()).stop();
                }
            });
            timer.start();
        }

        public void stopCountdown() {
            if (timer != null) {
                timer.stop();
            }
            timerLabel.setText("Waiting...");
            timerLabel.setForeground(Color.GRAY);
        }
    }

    // ==================== CHAT PANEL ====================
    private class ChatPanel extends JPanel {
        private GameClient client;
        private JTextArea chatArea;
        private JTextField inputField;

        public ChatPanel(GameClient client) {
            this.client = client;
            setLayout(new BorderLayout(5, 5));
            setBorder(BorderFactory.createTitledBorder("Chat"));

            chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setWrapStyleWord(true);
            DefaultCaret caret = (DefaultCaret) chatArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            JScrollPane scrollPane = new JScrollPane(chatArea);
            add(scrollPane, BorderLayout.CENTER);

            JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
            inputField = new JTextField();
            JButton sendButton = new JButton("Send");
            sendButton.addActionListener(e -> sendChat());
            inputField.addActionListener(e -> sendChat());
            inputPanel.add(inputField, BorderLayout.CENTER);
            inputPanel.add(sendButton, BorderLayout.EAST);
            add(inputPanel, BorderLayout.SOUTH);
        }

        private void sendChat() {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                if (message.startsWith("/")) {
                    // Parse as command (uppercase for consistency)
                    String cmd = message.substring(1).toUpperCase();
                    if (cmd.isEmpty()) {
                        chatArea.append("[Invalid] Empty command after /\n");
                    } else {
                        client.sendCommand(cmd);
                    }
                } else {
                    // Parse as chat message
                    client.sendCommand("CHAT " + message);
                }
                inputField.setText("");
            }
        }

        public void logChat(String message) {
            chatArea.append(message + "\n");
        }
    }

    // ==================== GAME LOG PANEL ====================
    private class GameLogPanel extends JPanel {
        private JTextArea logArea;

        public GameLogPanel() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder("Game Log"));

            logArea = new JTextArea();
            logArea.setEditable(false);
            logArea.setLineWrap(true);
            logArea.setWrapStyleWord(true);
            logArea.setText("Game started!\nUse /MOVE <column#> (0-6) to place your piece\nYou have 30 seconds per turn\n\n");
            DefaultCaret caret = (DefaultCaret) logArea.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            JScrollPane scrollPane = new JScrollPane(logArea);
            add(scrollPane, BorderLayout.CENTER);
        }

        public void logMessage(String message) {
            logArea.append(message + "\n");
        }
    }

    // ==================== INFO PANEL ====================
    private class InfoPanel extends JPanel {
        private JLabel playerInfoLabel;
        private JLabel winsLabel;
        private ImageIcon redSmall;
        private ImageIcon yellowSmall;
        private ImageIcon trophySmall;

        public InfoPanel() {
            setLayout(new BorderLayout(10, 10));
            setBorder(BorderFactory.createTitledBorder("Game Info"));

            // Load small icons
            redSmall = resizeIcon("images/red_piece.png", 20, 20);
            yellowSmall = resizeIcon("images/yellow_piece.png", 20, 20);
            trophySmall = resizeIcon("images/trophy.png", 20, 20);

            // Player info with color emoji
            playerInfoLabel = new JLabel("Waiting for game to start...");
            playerInfoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            add(playerInfoLabel, BorderLayout.WEST);

            // Trophy/wins display
            winsLabel = new JLabel("Wins: 0");
            winsLabel.setIcon(trophySmall);
            winsLabel.setFont(new Font("Arial", Font.BOLD, 16));
            add(winsLabel, BorderLayout.EAST);
        }

        public void setPlayerIcon(int playerNum, String opponentName) {
            ImageIcon playerIcon = (playerNum == 1) ? redSmall : yellowSmall;
            playerInfoLabel.setIcon(playerIcon);
        }

        public void setWins(int wins) {
            winsLabel.setIcon(trophySmall);
            winsLabel.setText(" Wins: " + wins);
        }

        public void setGameInfo(String info) {
            playerInfoLabel.setText(info);
        }

        private ImageIcon resizeIcon(String path, int width, int height) {
        Image img = new ImageIcon(path).getImage();
        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
        }
    }
}
