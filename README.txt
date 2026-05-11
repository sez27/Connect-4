CONNECT 4 JAVA MULTIPLAYER GAME

PROJECT OVERVIEW
----------------
This project is a multiplayer Connect 4 game written in Java. The game uses a client-server structure, which means one program runs as the server and multiple players connect to it using a client program.

Players can enter a lobby, create or join game rooms, play Connect 4 against another player, send chat messages, and play again after a game ends. The server controls the actual game logic, including turns, move validation, win checking, room management, and the 30-second turn timer. The client provides the graphical interface that players use to interact with the game.

The Connect 4 board has 6 rows and 7 columns. Player 1 uses red pieces and Player 2 uses yellow pieces. Player 1 always goes first. A player wins by getting four of their pieces in a row horizontally, vertically, or diagonally.


MAIN FEATURES
-------------
- Multiplayer client-server connection
- Java Swing graphical user interface
- Lobby system
- Public and private rooms
- Two-player Connect 4 gameplay
- Clickable game board
- Turn-based movement
- 30-second move timer
- Automatic move if a player runs out of time
- In-game chat
- Lobby chat
- Win tracking
- Play again option

CLASS OVERVIEW
--------------

Server.java
The Server class starts the server program. It creates the PersistenceManager, creates the LobbyManager, opens a ServerSocket on port 3000, and waits for clients to connect. Every time a client connects, the server creates a new ClientHandler and runs it on its own thread.

ClientHandler.java
The ClientHandler class represents one connected player on the server. It reads commands from that client, such as CREATE_ROOM, JOIN_ROOM, MOVE, CHAT, PLAY_AGAIN, and LEAVE_ROOM. It then sends those commands to the correct class, usually LobbyManager or GameRoom.

LobbyManager.java
The LobbyManager class manages the lobby and all active game rooms. It stores connected clients, creates new rooms, lets players join rooms, lists available rooms, removes empty rooms, and broadcasts lobby chat messages.

GameRoom.java
The GameRoom class controls one Connect 4 match. It stores player 1 and player 2, starts the game when two players join, processes moves, broadcasts board updates, handles chat messages, checks for wins or draws, manages the timer, tracks wins, and resets the game when players choose to play again.

GameState.java
The GameState class stores the actual Connect 4 board and game rules. It keeps the 6 by 7 board array, tracks whose turn it is, places pieces in the lowest available row, checks if a move is valid, checks for four in a row, checks for draws, switches turns, and creates the board JSON sent to the clients.

Timer.java
The Timer class handles the server-side 30-second turn timer. When a player's turn starts, the timer begins counting down. If the player does not move in time, the timer tells the GameRoom to process a timeout, which automatically places a piece in a random valid column.

GameClient.java
The GameClient class is the main graphical client program. It connects to the server, asks the user for their name, displays the lobby screen, displays the game screen, sends commands to the server, receives messages from the server, updates the board, shows chat, shows game logs, and displays player information.

Client.java
The Client class is a simpler console-based client. It connects to the server and allows a user to type commands manually in the terminal instead of using the Swing graphical interface.

PersistenceManager.java
The PersistenceManager class handles saving and loading game data. It creates the data folders, saves game room information as JSON, saves chat messages to text files, and can load previous saved game files.

Move.java
The Move class is a small data class that stores one move. It records which player moved, which column they chose, and the timestamp of the move.

ChatMessage.java
The ChatMessage class is a small data class that stores one chat message. It records the sender, message text, and timestamp.


HOW THE GAME FLOWS
1. The server starts and waits for clients.
2. A player opens GameClient.
3. The player enters a name.
4. The client connects to the server.
5. The player enters the lobby.
6. The player can create a room or join an existing room.
7. When two players are in the same room, the game starts.
8. Player 1 is red and goes first.
9. Players take turns clicking a column to place a piece.
10. The server checks if the move is valid.
11. The server updates the board.
12. The server checks for a win or draw.
13. The updated board is sent to both clients.
14. The next player's turn begins.
15. The game continues until someone wins or the board is full.
16. After the game ends, players can play again or leave the room.


PLAYER INSTRUCTIONS
-------------------

Starting the Server
Before players can connect, the server must be running.

Run:

    java Server

The server listens on port 3000.


Starting the GUI Client
Each player should run:

    java GameClient

When the window opens, enter your player name. After connecting, you will enter the lobby.


Lobby Controls
In the lobby, players can create rooms, join rooms, list rooms, and send lobby chat messages.

List Rooms:
Click the "List Rooms" button or type:

    /LIST_ROOMS

Create a Public Room:
Click "Create Room" and leave the password blank, or type:

    /CREATE_ROOM

Create a Private Room:
Click "Create Room" and enter a password, or type:

    /CREATE_ROOM passwordHere

Join a Public Room:
Click "Join Room" and enter the room ID, or type:

    /JOIN_ROOM roomId

Example:

    /JOIN_ROOM 1

Join a Private Room:
Click "Join Room" and enter the room ID and password, or type:

    /JOIN_ROOM roomId passwordHere

Example:

    /JOIN_ROOM 1 mypassword

Lobby Chat:

    /ALL message

Type a normal message in the lobby chat box. The client sends it as a lobby message to other connected lobby users.


Game Rules
----------
- The board has 6 rows and 7 columns.
- Columns are numbered from 0 to 6.
- Player 1 is red.
- Player 2 is yellow.
- Player 1 goes first.
- Players take turns placing pieces.
- A piece falls to the lowest open space in the chosen column.
- A player wins by connecting four pieces in a row.
- Four in a row can be horizontal, vertical, or diagonal.
- If the board fills with no winner, the game ends in a draw.
- Each player has 30 seconds to move.
- If a player runs out of time, the server automatically places a piece in a random valid column.


Making a Move
During your turn, click a column on the board to place your piece.

You can also type the move command:

    /MOVE columnNumber

Example:

    /MOVE 3

Valid columns are:

    0, 1, 2, 3, 4, 5, 6


Game Chat
During a game, type a normal message in the chat box and press Send. The message is sent only to the players in the current room.

You can also use:

    /CHAT message


Playing Again
After a game ends, players can start another game in the same room by typing:

    /PLAY_AGAIN

This resets the board and starts a new match if both players are still in the room.
*This works even if the game is not finished.


Leaving a Room
To leave the current game room and return to the lobby, type:

    /LEAVE_ROOM


Quitting
To quit the program, type:

    /QUIT

or close the game window.


COMMAND SUMMARY
LIST_ROOMS
    Shows all available rooms.

CREATE_ROOM [password]
    Creates a new room. The password is optional.

JOIN_ROOM <roomId> [password]
    Joins an existing room. Password is only needed for private rooms.

MOVE <column>
    Places a piece in the selected column.

CHAT <message>
    Sends a message to the current game room.

ALL <message>
    Sends a message to all lobby users.

PLAY_AGAIN
    Starts a new game in the current room.

LEAVE_ROOM
    Leaves the current room and returns to the lobby.

QUIT
    Disconnects from the server.


TROUBLESHOOTING
---------------
If the client cannot connect:
- Make sure the server is running first.
- Make sure the server is using port 3000.

If images do not appear:
- Make sure the images folder exists.
- Make sure image file names match exactly.
- Expected paths:
    images/red_piece.png
    images/yellow_piece.png
    images/trophy.png

If a player cannot move:
- Try /MOVE <column>
- Make sure it is that player's turn.
- Make sure the selected column is not full.
- Make sure the player is currently inside a game room.

