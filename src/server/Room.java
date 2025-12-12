package server;

import common.Card;
import common.Message;
import common.State;

import java.net.InetSocketAddress;

public class Room {
    private String roomName;
    private Server.ClientHandler player1;
    private Server.ClientHandler player2;
    private State p1State;
    private State p2State;
    private String currentTurnUid; // 지금 턴인 플레이어 uid
    private int turnNumber;
    // 로그용 플래그 변수
    private int lastLoggedRemain = -1;

    // UDP 통신 타이머 관리
    private TurnTimer turnTimer = new TurnTimer(60);
    private volatile boolean gameRunning;
    private InetSocketAddress p1UdpAddr;
    private InetSocketAddress p2UdpAddr;

    Room(String roomName, Server.ClientHandler player1) {
        this.roomName = roomName;
        this.player1 = player1;
        this.p1State = new State(player1.getUid(), 30, 3, 0);
        p1UdpAddr = new InetSocketAddress(player1.getClientSocket().getInetAddress(), player1.getUdpPort());
    }
    public void enterRoom(Server.ClientHandler player2) {
        this.player2 = player2;
        this.p2State = new State(player2.getUid(), 30, 3, 0);
        p2UdpAddr = new InetSocketAddress(player2.getClientSocket().getInetAddress(), player2.getUdpPort());
    }
    public boolean isReady() {
        return player1 != null && player2 != null;
    }
    public State getStateOf(Server.ClientHandler handler) {
        if (handler == player1) return p1State;
        else if (handler == player2) return p2State;
        else return null;
    }
    public State getOpponentStateOf(Server.ClientHandler handler) {
        if (handler == player1) return p2State;
        else if (handler == player2) return p1State;
        else return null;
    }
    public boolean applyCard(Card card, Server.ClientHandler caster) {
        State me = getStateOf(caster);
        State enemy = getOpponentStateOf(caster);
        return card.executeCard(me, enemy);
    }

    public String getRoomName() { return roomName; }
    public State getP1State() { return p1State; }
    public State getP2State() { return p2State; }
    public Server.ClientHandler getPlayer1() { return player1; }
    public Server.ClientHandler getPlayer2() { return player2; }
    public String getCurrentTurnUid() { return currentTurnUid; }
    public int getTurnNumber() { return turnNumber; }
    public State getCurrentTurn() {
        State nowState = null;
        if (currentTurnUid.equals(player1.getUid())) {
            nowState = p1State;
        }
        else if (currentTurnUid.equals(player2.getUid())) {
            nowState = p2State;
        }
        return nowState;
    }
    public InetSocketAddress getP1Udp() { return p1UdpAddr; }
    public InetSocketAddress getP2Udp() { return p2UdpAddr; }
    public void setLastLoggedRemain(int lastLog) { this.lastLoggedRemain = lastLog; }
    public int getLastLoggedRemain() { return lastLoggedRemain; }

    public synchronized void startGame(long nowMs) {
        gameRunning = true;
        currentTurnUid = player1.getUid();
        turnNumber = 1;
        lastLoggedRemain = -1;
        turnTimer.startTurnTimer(nowMs);
    }

    public synchronized void endGame() {
        gameRunning = false;
        turnTimer.stopTurnTimer();
    }

    public synchronized void changeTurn(long nowMs) {
        // 턴이 끝난 플레이어만 보너스 데미지 초기화
        State s = getCurrentTurn();
        if (s == null) {
            System.err.println("현재 턴인 " + currentTurnUid + " 플레이어의 상태가 없습니다");
            return;
        }
        s.resetBonusDamage();
        if (currentTurnUid.equals(player1.getUid())) {
            currentTurnUid = player2.getUid();
        }
        else if (currentTurnUid.equals(player2.getUid())) {
            currentTurnUid = player1.getUid();
        }
        turnNumber++;
        lastLoggedRemain = -1;
        turnTimer.startTurnTimer(nowMs);
    }

    // 시간 초과로 서버가 강제 종료
    public synchronized void forceTurnEnd(long nowMs) {
        if (!turnTimer.isExpired(nowMs)) return;
        changeTurn(nowMs);
        // 다음 시작할 사람 시작
        Message msg = new Message(Message.MODE_TURN_END, currentTurnUid, turnNumber);
        broadcasting(msg);
    }

    public void broadcasting(Message msg) {
        if (player1 != null) player1.send(msg);
        if (player2 != null) player2.send(msg);
    }

    public synchronized boolean isGameRunning() { return gameRunning; }
    public synchronized int getRemainingSec(long nowMs) { return turnTimer.remainingSec(nowMs); }
}