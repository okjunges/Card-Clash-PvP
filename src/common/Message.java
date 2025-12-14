package common;

import java.io.Serializable;
import java.util.Vector;

public class Message implements Serializable {
    // 모드
    public final static int MODE_LOGIN = 0x1; // 로그인
    public final static int MODE_CREATE_ROOM = 0x2; // 방 만들기
    public final static int MODE_ENTER_ROOM = 0x4; // 방 들어가기
    public final static int MODE_GAME_START = 0x8; // 게임시작(카드 랜덤 5장 뽑기, 코스트 등 기본 세팅)
    public final static int MODE_CHAT = 0x10; // 문자열 채팅
    public final static int MODE_USE_CARD = 0x20; // 카드 사용 – 카드 코드로 맞는 로직 진행
    public final static int MODE_SYNC_STATE = 0x40; // 전체 상태 전달
    public final static int MODE_TURN_END = 0x80; // 턴 종료
    public final static int MODE_GAME_END = 0x100; // 게임 종료
    public final static int MODE_ROOM_LIST = 0x200; // 방 목록 조회
    public final static int MODE_SPECIAL_START = 0x400;
    public final static int MODE_SPECIAL_SUBMIT = 0x800;
    public final static int MODE_SPECIAL_RESULT = 0x1000;

    private Card card;
    private String roomName;
    private String userID;
    private int mode;
    private String message;
    private State p1;
    private State p2;
    private State winner;
    private Vector<String> rooms;
    private int udpPort;
    private int turn;
    private int cost;

    // 방 목록 요청, 배팅 시작
    public Message(int mode) { this.mode = mode; }
    // 게임 종료(패배한 userID), (클라)턴 종료 알림(종료한 userID)
    public Message(int mode, String userID) {
        this.mode = mode;
        this.userID = userID;
    }
    // 로그인
    public Message(int mode, int udpPort, String userID) {
        this.mode = mode;
        this.userID = userID;
        this.udpPort = udpPort;
    }
    // (서버)턴종료 - 다음 턴 사람의 id, 다음에 시작된 턴 수
    public Message(int mode, String userID, int nextTurn) {
        this.mode = mode;
        this.userID = userID;
        this.turn = nextTurn;
    }
    // 방만들기, 들어가기
    public Message(int mode, String userID, String roomName) {
        this.mode = mode;
        this.userID = userID;
        this.roomName = roomName;
    }
    // 채팅, isChat = true
    public Message(int mode, String userID, String message, boolean isChat) {
        this.mode = mode;
        this.userID = userID;
        this.message = message;
    }
    // 카드 사용
    public Message(int mode, String userID, Card card) {
        this.mode = mode;
        this.userID = userID;
        this.card = card;
    }
    // 게임 시작
    public Message(int mode, String turnUid, int nextTurn, State p1, State p2) {
        this.turn = nextTurn;
        this.userID = turnUid;
        this.mode = mode;
        this.p1 = p1;
        this.p2 = p2;
    }
    // 상태 반환
    public Message(int mode, State p1, State p2) {
        this.mode = mode;
        this.p1 = p1;
        this.p2 = p2;
    }
    // 배팅 완료(클라 -> 서버)
    public Message(int mode, int cost) {
        this.mode = mode;
        this.cost = cost;
    }
    // 특별 라운드 결과 방송, 배팅 이긴 사람의 코스트만 배팅한 만큼 감소
    public Message(int mode, State winner, String turnUid, int nextTurn) {
        this.mode = mode;
        this.winner = winner;
        this.userID = turnUid;
        this.turn = nextTurn;
    }
    // 방 목록 반환
    public Message(int mode, Vector<String> rooms) {
        this.mode = mode;
        this.rooms = rooms;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getMessage() { return message; }

    public void setMessage(String message) {
        this.message = message;
    }

    public State getP1() {
        return p1;
    }

    public void setP1(State p1) {
        this.p1 = p1;
    }

    public State getP2() {
        return p2;
    }

    public void setP2(State p2) {
        this.p2 = p2;
    }

    public void setCard(Card card) { this.card = card; }

    public Card getCard() { return card; }

    public void setTurn(int turn) { this.turn = turn; }

    public int getTurn() { return turn; }

    public void setUdpPort(int udpPort) { this.udpPort = udpPort; }

    public int getUdpPort() { return udpPort; }

    public void setCost(int cost) { this.cost = cost; }

    public int getCost() { return cost; }

    public void setWinner(State winner) { this.winner = winner; }

    public State getWinner() { return winner; }

    public void setRooms(Vector<String> rooms) { this.rooms = rooms; }

    public Vector<String> getRoomNames() {
        return rooms;
    }
}