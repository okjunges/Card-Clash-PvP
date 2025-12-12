package server;

public class TurnTimer {
    private final int turnTimeSec;
    private long deadlineMs;
    private volatile boolean running;

    public TurnTimer(int turnTimeSec) { this.turnTimeSec = turnTimeSec; }
    public void startTurnTimer(long nowMS) {
        running = true;
        deadlineMs = nowMS + turnTimeSec * 1000L;
    }
    public void stopTurnTimer() { running = false; }
    public boolean isRunning() { return running; }
    public long remainingMs(long nowMs) {
        if (!running) return 0;
        return Math.max(0, deadlineMs - nowMs);
    }
    public boolean isExpired(long nowMs) { return running && nowMs >= deadlineMs; }
    public int remainingSec(long nowMs) { return (int) Math.ceil(remainingMs(nowMs) / 1000.0); }
}