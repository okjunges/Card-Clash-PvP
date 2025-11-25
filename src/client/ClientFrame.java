package client;

import common.Message;
import common.ServerInfo;

import javax.swing.*;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;

public class ClientFrame extends JFrame {

    // 화면 전환용
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    // 화면 패널
    private TitlePanel titlePanel;
    private RoomListPanel roomListPanel;
    private WaitingRoomPanel waitingRoomPanel;
    private GamePanel gamePanel;

    // 네트워크
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String uid; // 내 닉네임 설정
    private String serverIP;
    private int serverPort;
    private Thread receiveThread;
    private String currentRoomName; // 현재 들어간 방 이름 기억용

    private DefaultStyledDocument document = new DefaultStyledDocument(); // 게임화면 채팅에 쓸 Document

    public ClientFrame() {
        super("Card Clash PvP");

        // server.txt에서 IP / PORT 읽기
        serverIP = ServerInfo.getInstance().getIP();
        serverPort = ServerInfo.getInstance().getPORT();

        setLayout(new BorderLayout());
        setSize(1000, 800); //일단 임시로 2배로 키움. 적절한 크기 찾은 후 고정예정
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 패널 생성 & 등록
        titlePanel = new TitlePanel(this);
        roomListPanel = new RoomListPanel(this);
        waitingRoomPanel = new WaitingRoomPanel(this);
        gamePanel = new GamePanel(this, document);


        mainPanel.add(titlePanel, "TITLE");
        mainPanel.add(roomListPanel, "ROOM_LIST");
        mainPanel.add(waitingRoomPanel, "WAITING");
        mainPanel.add(gamePanel, "GAME");

        add(mainPanel, BorderLayout.CENTER);
        setVisible(true);

        cardLayout.show(mainPanel, "TITLE");
    }

    // 화면 전환
    public void changeScreen(String name) {
        cardLayout.show(mainPanel, name);
    }

    public String getUid() {
        return uid;
    }

    public DefaultStyledDocument getDocument() {
        return document;
    }

    // TitlePanel에서 호출: 서버 접속 + 로그인
    public boolean connectToServer(String userID) {
        try {
            uid = userID; // 닉네임 저장

            socket = new Socket();
            socket.connect(new InetSocketAddress(serverIP, serverPort), 3000);

            // 출력 스트림만 여기서 만든다
            out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.flush();   // 헤더 전송

            // 수신 스레드 시작 (안에서 ObjectInputStream 생성)
            startReceiveThread();

            // 로그인 메시지 전송
            sendMessage(new Message(Message.MODE_LOGIN, uid));

            // 로그인 후 현재 방 목록 요청
            sendMessage(new Message(Message.MODE_ROOM_LIST));

            System.out.println("서버 접속 완료: " + serverIP + ":" + serverPort);
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "서버 연결 실패: " + e.getMessage());
            return false;
        }
    }

    // 수신 스레드 메서드
    private void startReceiveThread() {
        receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 여기서 입력 스트림 생성
                    in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

                    while (receiveThread == Thread.currentThread()) {
                        Message msg = (Message) in.readObject();
                        if (msg == null) {
                            System.out.println("서버로부터 null 수신, 종료");
                            break;
                        }

                        // 모드에 따라 분기
                        switch (msg.getMode()) {
                            case Message.MODE_ENTER_ROOM:
                                handleEnterRoom(msg);
                                break;

                            case Message.MODE_GAME_START:
                                handleGameStart(msg);
                                break;

                            case Message.MODE_CHAT:
                                handleChat(msg);
                                break;

                            case Message.MODE_ROOM_LIST:                   // ← 추가
                                handleRoomList(msg);
                                break;

                            default:
                                System.out.println("알 수 없는 모드 수신: " + msg.getMode());
                        }
                    }
                } catch (IOException e) {
                    System.out.println("서버와의 연결 종료: " + e.getMessage());
                } catch (ClassNotFoundException e) {
                    System.out.println("잘못된 객체 수신: " + e.getMessage());
                } finally {
                    try {
                        if (socket != null && !socket.isClosed()) {
                            socket.close();
                        }
                    } catch (IOException e) {
                        System.out.println("소켓 닫기 오류: " + e.getMessage());
                    }
                }
            }
        });
        receiveThread.start();
    }

    // 공용 전송 메서드
    public void sendMessage(Message msg) {
        if (out == null) {
            System.err.println("아직 서버에 연결되지 않았습니다.");
            return;
        }
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("클라이언트 전송 오류 > " + e.getMessage());
        }
    }

    // RoomListPanel에서 사용할 헬퍼 메서드들
    // 방만들기 요청
    public void requestCreateRoom(String roomName) {
        // 1) 서버에 방 만들기 요청 보내기
        sendMessage(new Message(Message.MODE_CREATE_ROOM, uid, roomName));
        System.out.println("방 만들기 요청: " + roomName);

        sendMessage(new Message(Message.MODE_ROOM_LIST));

        // ⚠ 절대 여기서 currentRoomName을 설정하지 말 것!
        // 서버가 MODE_ENTER_ROOM 응답을 보내줄 때만 설정해야 동기화가 맞는다.
        // currentRoomName = roomName;

    }

    // 방 들어가기 요청
    public void requestEnterRoom(String roomName) {
        sendMessage(new Message(Message.MODE_ENTER_ROOM, uid, roomName));
        System.out.println("방 들어가기 요청: " + roomName);
        currentRoomName = roomName;   // 들어간 방 기억

        sendMessage(new Message(Message.MODE_ROOM_LIST));
    }

    // (임시) 종료 시 스레드/소켓 정리용 메서드
    public void disconnectFromServer() {
        try {
            if (receiveThread != null) {
                receiveThread = null;
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("클라이언트 닫기 오류 > " + e.getMessage());
        }
    }

    // 방 입장 처리
    private void handleEnterRoom(Message msg) {
        String roomName = msg.getRoomName();
        String userId = msg.getUserID();

        if (userId.equals(uid)) {
            // 서버가 "너 입장 성공"을 보내면, 그 때 화면 전환 + P1/P2 UI 갱신
            waitingRoomPanel.enterAsOwner(uid, roomName);
            changeScreen("WAITING");
        } else {
            // 나중에 P2가 들어왔을 때 서버가 방송해줄 때 사용
            waitingRoomPanel.enterAsGuest(userId, roomName);
        }
    }

    // P1이 "시작하기" 눌렀을 때 호출
    public void requestGameStart() {
        if (currentRoomName == null) return;

        // 서버에 게임시작 요청
        sendMessage(new Message(Message.MODE_GAME_START, uid, currentRoomName));

        // 2) 화면 전환은 서버가 MODE_GAME_START 방송(for all players)을 보내면
        //    handleGameStart() -> goToGameScreen()에서 처리
    }

    // 채팅 전송용
    public void sendChat(String text) {
        if (text == null || text.trim().isEmpty()) return;
        sendMessage(new Message(Message.MODE_CHAT, uid, text, true));
        // 에코는 서버에서 MODE_CHAT 방송으로 온 걸 handleChat에서 처리
    }

    // 서버에서 "이 방 게임 시작" 방송
    private void handleGameStart(Message msg) {
        String roomName = msg.getRoomName();
        if (roomName != null) {
            currentRoomName = roomName;
        }
        // 서버가 방송을 보내면 두 플레이어 모두 같은 타이밍에 게임 화면으로 전환됨
        goToGameScreen(currentRoomName);
    }

    // 실제 화면 전환 + 방 이름 세팅
    private void goToGameScreen(String roomName) {
        gamePanel.setRoomName(roomName);
        changeScreen("GAME");
        gamePanel.appendChat("시스템: 게임이 시작되었습니다.");
    }

    // 서버에서 채팅 방송
    private void handleChat(Message msg) {
        String line = msg.getUserID() + " : " + msg.getMessage();
        gamePanel.appendChat(line);
    }

    // 서버에서 방 목록 방송/응답 받았을 때
    private void handleRoomList(Message msg) {
        Vector<String> rooms = msg.getRoomNames();
        if (rooms == null) return;

        // RoomListPanel이 List<String> 기준이니까 변환해서 넘김
        java.util.List<String> list = new ArrayList<>(rooms);

        SwingUtilities.invokeLater(() -> roomListPanel.updateRoomList(list));
    }

    public static void main(String[] args) {
        new ClientFrame();
    }


}
