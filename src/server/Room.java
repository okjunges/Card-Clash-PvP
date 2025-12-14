package server;

import common.Card;
import common.Message;
import common.State;

import java.net.InetSocketAddress;
import java.util.Random;

public class Room {
    private String roomName;
    private Session player1;
    private Session player2;
    private State p1State;
    private State p2State;
    private String currentTurnUid; // 지금 턴인 플레이어 uid
    private int turnNumber;
    private int p1bill;
    private boolean p1Submit;
    private int p2bill;
    private boolean p2Submit;
    private Round round;
    private Random random = new Random();

    // 로그용 플래그 변수
    private int lastLoggedRemain = -1;

    // UDP 통신 타이머 관리
    private TurnTimer turnTimer = new TurnTimer(60);
    private volatile boolean gameRunning;
    private InetSocketAddress p1UdpAddr;
    private InetSocketAddress p2UdpAddr;

    Room(String roomName, Session player1) {
        this.roomName = roomName;
        this.player1 = player1;
        this.p1State = new State(player1.getUid(), 30, 3, 0);
        p1bill = -1;
        p1Submit = false;
        p1UdpAddr = player1.getInetSocketAddress();
    }
    public void enterRoom(Session player2) {
        this.player2 = player2;
        this.p2State = new State(player2.getUid(), 30, 3, 0);
        p2bill = -1;
        p2Submit = false;
        p2UdpAddr = player2.getInetSocketAddress();
    }
    public boolean isReady() {
        return player1 != null && player2 != null;
    }
    public State getStateOf(Session session) {
        if (session == player1) return p1State;
        else if (session == player2) return p2State;
        else return null;
    }
    public State getOpponentStateOf(Session session) {
        if (session == player1) return p2State;
        else if (session == player2) return p1State;
        else return null;
    }
    public boolean applyCard(Card card, Session caster) {
        State me = getStateOf(caster);
        State enemy = getOpponentStateOf(caster);
        return card.executeCard(me, enemy);
    }

    public String getRoomName() { return roomName; }
    public State getP1State() { return p1State; }
    public State getP2State() { return p2State; }
    public Session getPlayer1() { return player1; }
    public Session getPlayer2() { return player2; }
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
        round = Round.NORMAL;
        currentTurnUid = player1.getUid();
        turnNumber = 1;
        lastLoggedRemain = -1;
        turnTimer.startTurnTimer(nowMs);
    }

    public synchronized void endGame() {
        gameRunning = false;
        turnTimer.stopTurnTimer();
    }

    public synchronized Round changeTurn(long nowMs) {
        // 턴이 끝난 플레이어만 보너스 데미지 초기화
        State s = getCurrentTurn();
        if (s == null) {
            System.err.println("현재 턴인 " + currentTurnUid + " 플레이어의 상태가 없습니다");
            return null;
        }
        s.resetBonusDamage();
        turnNumber++;
        round = Round.NORMAL;

        // 스페셜 라운드인지 확인
        if (checkSpecialRound(nowMs)) return round;

        boolean grantCost = (turnNumber >= 3);
        if (currentTurnUid.equals(player1.getUid())) {
            if (grantCost) p2State.addCoat(2);
            currentTurnUid = player2.getUid();
        }
        else if (currentTurnUid.equals(player2.getUid())) {
            if (grantCost) p1State.addCoat(2);
            currentTurnUid = player1.getUid();
        }
        lastLoggedRemain = -1;
        turnTimer.startTurnTimer(nowMs);
        return round;
    }

    public boolean checkSpecialRound(long nowMs) {
        if (turnNumber % 5 != 0) return false;
        int r = random.nextInt(10) + 1;
        if (r > 3) return false;
        lastLoggedRemain = -1;
        turnTimer.startTurnTimer(nowMs);
        round = Round.SPECIAL;
        p1bill = p2bill = -1;
        p1Submit = p2Submit = false;
        return true;
    }

    // 시간 초과로 서버가 강제 종료
    public synchronized void forceTurnEnd(long nowMs) {
        if (!turnTimer.isExpired(nowMs)) return;
        if (round == Round.SPECIAL) {
            if (!p1Submit) {
                p1bill = 0;
                p1Submit = true;
            }
            if (!p2Submit) {
                p2bill = 0;
                p2Submit = true;
            }
            resolveSpecial(nowMs);
            return;
        }
        changeTurn(nowMs);
    }

    public synchronized void submit(Session player, int cost, long nowMs) {
        if (round != Round.SPECIAL) {
            System.err.println("지금은 보너스 라운드가 아닙니다!");
            return;
        }

        State me = getStateOf(player);
        if (cost < 0 || cost > me.getCost()) {
            System.err.println("코스트 부족 문제");
            return;
        }

        if (player == player1 && p1Submit == false) {
            p1bill = cost;
            p1Submit = true;
        }
        else if (player == player2 && p2Submit == false) {
            p2bill = cost;
            p2Submit = true;
        }

        // 둘 다 제출했으면 즉시 결정
        if (p1Submit && p2Submit) { resolveSpecial(nowMs); }
    }

    public synchronized void resolveSpecial(long nowMs) {
        State winner = null;
        if (p1bill > p2bill) {
            winner = p1State;
            p1State.addCoat(p1bill * -1);
        }
        else if (p1bill < p2bill) {
            winner = p2State;
            p2State.addCoat(p2bill * -1);
        }
        changeTurn(nowMs);

        Message msg = new Message(Message.MODE_SPECIAL_RESULT, winner, currentTurnUid, turnNumber);
        broadcasting(msg);
    }

    public void broadcasting(Message msg) {
        if (msg == null) {
            System.err.println(roomName + "방(서버) - 빈객체 방송 요청 오류");
            return;
        }
        if (player1 != null) player1.send(msg);
        if (player2 != null) player2.send(msg);
    }

    public synchronized boolean isGameRunning() { return gameRunning; }
    public synchronized int getRemainingSec(long nowMs) { return turnTimer.remainingSec(nowMs); }
}