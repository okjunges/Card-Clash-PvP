package common;

import javax.swing.*;
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

    // 카드 코드
    public final static int Strike = 1;
    public final static int HeavyBlow = 2;
    public final static int Pierce = 3;
    public final static int SharpEdge = 4;
    public final static int WeaknessStrike = 5;
    public final static int Defend = 6;
    public final static int IronWall = 7;
    public final static int CounterGuard = 8;
    public final static int ChargeUp = 9;
    public final static int AdrenalineRush = 10;

    private String roomName;
    private String userID;
    private int mode;
    private int cardCode;
    private String message;
    private State p1;
    private State p2;
    private Vector<String> rooms;

    // 방 목록 요청
    public Message(int mode) { this.mode = mode; }
    // 로그인, 게임 종료(패배한 userID), 턴종료(종료한 userID)
    public Message(int mode, String userID) {
        this.mode = mode;
        this.userID = userID;
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
    public Message(int mode, String userID, int cardCode) {
        this.mode = mode;
        this.userID = userID;
        this.cardCode = cardCode;
    }
    // 상태 반환
    public Message(int mode, State p1, State p2) {
        this.mode = mode;
        this.p1 = p1;
        this.p2 = p2;
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

    public int getCardCode() {
        return cardCode;
    }

    public void setCardCode(int cardCode) {
        this.cardCode = cardCode;
    }

    public String getMessage() {
        return message;
    }

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
    public Vector<String> getRoomNames() {
        return rooms;
    }

    // 예전 코드
    public final static int MODE_LOGOUT = 0x2;
    public final static int MODE_TX_STRING = 0x10;
    public final static int MODE_TX_FILE = 0x20;
    public final static int MODE_TX_IMAGE = 0x40;
    private ImageIcon image;
    private long size;

    public ImageIcon getImage() {
        return image;
    }

    public void setImage(ImageIcon image) {
        this.image = image;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Message(String userID, int code, String message, ImageIcon image, long size) {
        this.userID = userID;
        this.mode = code;
        this.message = message;
        this.image = image;
        this.size = size;
    }

    public Message(String userID, int code, String message, ImageIcon image) {
        this(userID, code, message, image, 0);
    }

    public Message(String userID, int code) {
        this(userID, code, null, null);
    }

    public Message(String userID, int code, String message) {
        this(userID, code, message, null);
    }

    public Message(String userID, int code, ImageIcon image) {
        this(userID, code, null, image);
    }

    public Message(String userID, int code, String filename, long size) {
        this(userID, code, filename, null, size);
    }
}