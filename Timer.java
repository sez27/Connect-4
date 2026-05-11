import java.util.concurrent.*;

public class Timer {
    private static final int TIMEOUT_SECONDS = 30;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> currentTask;
    private GameRoom gameRoom;
    private int currentPlayer;
    private long startTime;
    private boolean isRunning;

    public Timer() {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.isRunning = false;
    }

    // Starts a new timer for the current player's turn.
    public synchronized void startTimer(GameRoom room, int playerId) {
        cancelTimer();

        this.gameRoom = room;
        this.currentPlayer = playerId;
        this.startTime = System.currentTimeMillis();
        this.isRunning = true;

        // Timer fires after 30 seconds
        currentTask = scheduler.schedule(() -> {
            handleTimeout();
        }, TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    // Cancels the current timer.
    public synchronized void cancelTimer() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(false);
        }
        isRunning = false;
    }

    // Handles event when player runs out of time.
    private void handleTimeout() {
        isRunning = false;
        if (gameRoom != null) {
            gameRoom.processTimeout(currentPlayer);
        }
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    public synchronized int getCurrentPlayer() {
        return currentPlayer;
    }

    public synchronized long getElapsedTime() {
        if (!isRunning) {
            return 0;
        }
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    public synchronized long getRemainingTime() {
        if (!isRunning) {
            return 0;
        }
        long elapsed = getElapsedTime();
        long remaining = Math.max(0, TIMEOUT_SECONDS - elapsed);
        return remaining;
    }

    public void shutdown() {
        cancelTimer();
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }
}
