public class GameState {
    private static final int ROWS = 6;
    private static final int COLS = 7;
    private static final int PLAYER1 = 1;  // Red
    private static final int PLAYER2 = 2;  // Yellow
    private static final int EMPTY = 0;
    private static final int WIN_LENGTH = 4;

    private int[][] board;
    private int currentTurn;

    public GameState() {
        this.board = new int[ROWS][COLS];
        this.currentTurn = PLAYER1;
        initializeBoard();
    }

    private void initializeBoard() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                board[row][col] = EMPTY;
            }
        }
    }

    // Attempts to place a piece in the specified column for the player that made the move.
    public synchronized boolean makeMove(int column, int playerId) {
        if (!isValidMove(column)) {
            return false;
        }
        int row = getLowestEmptyRow(column);
        if (row == -1) {
            return false;
        }
        board[row][column] = playerId;
        return true;
    }

    // Checks if a move to the specified column is valid.
    public synchronized boolean isValidMove(int column) {
        if (column < 0 || column >= COLS) {
            return false;
        }
        return board[0][column] == EMPTY;
    }

    // Gets the lowest empty row where a piece would land.
    public synchronized int getLowestEmptyRow(int column) {
        if (column < 0 || column >= COLS) {
            return -1;
        }
        for (int row = ROWS - 1; row >= 0; row--) {
            if (board[row][column] == EMPTY) {
                return row;
            }
        }
        return -1;
    }


    // Checks if the player has won.
    public synchronized boolean checkWin(int playerId) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (board[row][col] == playerId) {
                    if (countDirection(row, col, playerId, 0, 1) >= WIN_LENGTH ||
                        countDirection(row, col, playerId, 1, 0) >= WIN_LENGTH ||
                        countDirection(row, col, playerId, 1, 1) >= WIN_LENGTH ||
                        countDirection(row, col, playerId, 1, -1) >= WIN_LENGTH) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Counts consecutive pieces in a given direction from a starting position.
    private int countDirection(int startRow, int startCol, int playerId, int dRow, int dCol) {
        int count = 1;

        int row = startRow + dRow;
        int col = startCol + dCol;
        while (row >= 0 && row < ROWS && col >= 0 && col < COLS && board[row][col] == playerId) {
            count++;
            row += dRow;
            col += dCol;
        }

        row = startRow - dRow;
        col = startCol - dCol;
        while (row >= 0 && row < ROWS && col >= 0 && col < COLS && board[row][col] == playerId) {
            count++;
            row -= dRow;
            col -= dCol;
        }

        return count;
    }

    // Checks if the board is full (draw).
    public synchronized boolean isBoardFull() {
        for (int col = 0; col < COLS; col++) {
            if (board[0][col] == EMPTY) {
                return false;
            }
        }
        return true;
    }

    // Gets the current player's turn.
    public synchronized int getCurrentTurn() {
        return currentTurn;
    }

    // Switches the turn to the other player.
    public synchronized void switchTurn() {
        currentTurn = (currentTurn == PLAYER1) ? PLAYER2 : PLAYER1;
    }

    // Resets the game state for a new game.
    public synchronized void reset() {
        initializeBoard();
        currentTurn = PLAYER1;
    }

    // Gets the current board state as a JSON string.
    public synchronized String getBoardAsJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{\"board\":[");

        for (int row = 0; row < ROWS; row++) {
            json.append("[");
            for (int col = 0; col < COLS; col++) {
                json.append(board[row][col]);
                if (col < COLS - 1) json.append(",");
            }
            json.append("]");
            if (row < ROWS - 1) json.append(",");
        }

        json.append("],\"rows\":").append(ROWS);
        json.append(",\"cols\":").append(COLS);
        json.append(",\"currentTurn\":").append(currentTurn);
        json.append("}");

        return json.toString();
    }

     // Gets the entire board for internal use.
    public synchronized int[][] getBoard() {
        int[][] copy = new int[ROWS][COLS];
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                copy[row][col] = board[row][col];
            }
        }
        return copy;
    }

    public static int getRows() {
        return ROWS;
    }

    public static int getCols() {
        return COLS;
    }

    public static int getPlayer1() {
        return PLAYER1;
    }

    public static int getPlayer2() {
        return PLAYER2;
    }
}
