package server;

// 턴 시작 시점을 기준으로 종료 시각을 계산해 두고, 현재 시각을 넣으면 만료 여부와 남은 시간을 계산해 주는 타이머 클래스로 각 게임 방마다 존재하며 턴 진행 시간을 계산한다
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