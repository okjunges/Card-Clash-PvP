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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;


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

    // UDP 타이머 수신용
    private DatagramSocket udpSocket;
    private int udpPort;
    private Thread udpReceiveThread;

    // ===== 내 손패 관리 =====
    private ArrayList<common.Card> myHand = new ArrayList<>();
    private ArrayList<common.Card> cardPool = new ArrayList<>();

    // ===== 내 현재 코스트(서버 상태 기준 캐시) =====
    private int myCostCached = 0;
    private int enemyCostCached = 0;

    private DefaultStyledDocument document = new DefaultStyledDocument(); // 게임화면 채팅에 쓸 Document

    public ClientFrame() {
        super("Card Clash PvP");

        // server.txt에서 IP / PORT 읽기
        serverIP = ServerInfo.getInstance().getIP();
        serverPort = ServerInfo.getInstance().getPORT();

        setLayout(new BorderLayout());
        setSize(700, 500); //일단 임시로 2배로 키움. 적절한 크기 찾은 후 고정예정
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

            // UDP 소켓 생성(0이면 OS가 비어있는 포트 자동 할당)
            udpSocket = new DatagramSocket(0);
            udpPort = udpSocket.getLocalPort();

            // 로그인 메시지 전송(udpPort 포함)
            sendMessage(new Message(Message.MODE_LOGIN, udpPort, uid));

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
                            case Message.MODE_LOGIN:
                                handleLogin(msg);
                                break;

                            case Message.MODE_CREATE_ROOM:
                                requestRoomList();
                                responseEnterRoom(msg);
                                break;

                            case Message.MODE_ENTER_ROOM:
                                requestRoomList();
                                handleEnterRoom(msg);
                                break;

                            case Message.MODE_GAME_START:
                                handleGameStart(msg);
                                break;

                            case Message.MODE_CHAT:
                                handleChat(msg);
                                break;

                            case Message.MODE_USE_CARD:
                                handleUseCard(msg);
                                break;

                            case Message.MODE_SYNC_STATE:
                                handleSyncState(msg);
                                break;

                            case Message.MODE_TURN_END:
                                handleTurnEnd(msg);
                                break;

                            case Message.MODE_GAME_END:
                                handleGameEnd(msg);
                                break;

                            case Message.MODE_ROOM_LIST:
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

    private void startUdpTimerReceiver() {
        if (udpSocket == null) return;
        if (udpReceiveThread != null) return; // 중복 시작 방지

        udpReceiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buf = new byte[256];
                    while (udpReceiveThread == Thread.currentThread()) {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length);
                        udpSocket.receive(packet);

                        String payload = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                        // 형식: TIMER|roomName|turnNumber|turnUid|remainSec
                        String[] parts = payload.split("\\|");
                        if (parts.length != 5) continue;
                        if (!"TIMER".equals(parts[0])) continue;

                        String roomName = parts[1];
                        int turnNumber;
                        String turnUid = parts[3];
                        int remainSec;
                        try {
                            turnNumber = Integer.parseInt(parts[2]);
                            remainSec = Integer.parseInt(parts[4]);
                        } catch (Exception e) {
                            continue;
                        }

                        // 현재 방이 아니면 무시
                        if (currentRoomName == null) continue;
                        if (!currentRoomName.equals(roomName)) continue;

                        // Swing UI 갱신
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                gamePanel.updateTurnTimer(turnNumber, turnUid, remainSec);
                            }
                        });
                    }
                } catch (SocketException e) {
                    // 소켓 close 시 여기로 빠질 수 있음(정상)
                } catch (Exception e) {
                    System.out.println("UDP 수신 오류: " + e.getMessage());
                }
            }
        });
        udpReceiveThread.start();
    }


    // 로그인 응답 처리
    private void handleLogin(Message msg) {
        // 서버가 fail을 보내면(닉네임 중복 등) 접속을 유지하면 이후 로직이 다 꼬일 수 있어서
        // 여기서 즉시 안내 후 연결을 끊고, 타이틀 화면으로 돌린다.
        if (msg.getMessage() != null && msg.getMessage().equals("fail")) {
            JOptionPane.showMessageDialog(this, "로그인 실패: 닉네임이 중복되었거나 사용할 수 없습니다.");
            disconnectFromServer();
            changeScreen("TITLE");
            return;
        }

        // 성공 응답인 경우(현재는 별도 처리 없이 진행)
        System.out.println("로그인 성공: " + uid);
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
        currentRoomName = roomName;   // 들어간 방 기억
        sendMessage(new Message(Message.MODE_ENTER_ROOM, uid, roomName));
        System.out.println("방 들어가기 요청: " + roomName);

        sendMessage(new Message(Message.MODE_ROOM_LIST));
    }

    // 방 만들기 응답 처리
    private void responseEnterRoom(Message msg) {
        if (uid.equals(msg.getUserID())){
            currentRoomName = msg.getRoomName();
            waitingRoomPanel.enterAsOwner(uid, msg.getRoomName());
            changeScreen("WAITING");
        }
        else if(currentRoomName == null) return;
        else if(currentRoomName.equals(msg.getRoomName())){
            waitingRoomPanel.enterAsOwner(msg.getUserID(), msg.getRoomName());
            changeScreen("WAITING");
        }
    }

    // (임시) 종료 시 스레드/소켓 정리용 메서드
    public void disconnectFromServer() {
        try {
            if (receiveThread != null) {
                receiveThread = null;
            }
            if (udpReceiveThread != null) {
                udpReceiveThread = null;
            }
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
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

        if (currentRoomName == null || !currentRoomName.equals(roomName)) return;

        if (uid.equals(userId)) {
            waitingRoomPanel.enterAsGuest(userId, roomName);
            changeScreen("WAITING");
        }
        else {
            waitingRoomPanel.setOpponentName(userId);
            changeScreen("WAITING");
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
        // 1) 서버가 보낸 방 이름으로 최종 확정
        if (msg.getRoomName() != null) {
            currentRoomName = msg.getRoomName();
        }

        // 2) 화면 전환 + UDP 수신 시작
        goToGameScreen(currentRoomName);

        // 3) 초기 상태 표시
        gamePanel.updateState(msg.getP1(), msg.getP2());
        updateCostCache(msg.getP1(), msg.getP2());

        // 4) 현재 턴 반영
        gamePanel.setTurnOwner(msg.getUserID());

        // 5) 로그
        gamePanel.appendChat("시스템: 게임 시작, 현재 턴 = " + msg.getUserID());

        myHand.clear();
        drawCards(5);
    }

    // 실제 화면 전환 + 방 이름 세팅
    private void goToGameScreen(String roomName) {
        gamePanel.setRoomName(roomName);
        changeScreen("GAME");
        gamePanel.appendChat("시스템: 게임이 시작되었습니다.");

        startUdpTimerReceiver(); // 추가
    }

    // 서버에서 채팅 방송
    private void handleChat(Message msg) {
        String line = msg.getUserID() + " : " + msg.getMessage();
        gamePanel.appendChat(line);
    }

    // 서버에 방 목록 요청
    private void requestRoomList() {
        sendMessage(new Message(Message.MODE_ROOM_LIST));
    }

    // 서버에서 방 목록 방송/응답 받았을 때
    private void handleRoomList(Message msg) {
        Vector<String> rooms = msg.getRoomNames();
        if (rooms == null) return;

        roomListPanel.updateRoomList(rooms);
    }

    // 카드 사용 방송(이펙트/로그용)
    private void handleUseCard(Message msg) {
        // 방 정보가 있으면 같은 방일 때만 반영
        if (msg.getRoomName() != null && currentRoomName != null) {
            if (!currentRoomName.equals(msg.getRoomName())) return;
        }
        // 서버 fail은 해당 클라에게만 온다
        if ("fail".equals(msg.getMessage())) {
            if (uid != null && uid.equals(msg.getUserID())) {
                gamePanel.appendBattleLog("시스템: 카드 사용 실패(서버) - 코스트 부족");
            }
            return;
        }

        // 성공이면 전체 브로드캐스트로 오므로, 로그(=이펙트 트리거) 먼저 처리
        common.Card used = msg.getCard();
        String cardName = (used == null) ? "알수없는카드" : used.getCardName();
        gamePanel.appendBattleLog(msg.getUserID() + " 가 [" + cardName + "] 사용");

        // 내 카드면 손패에서 1장 제거(서버가 성공 방송을 보냈을 때만 제거)
        if (uid != null && uid.equals(msg.getUserID()) && used != null) {
            removeOneCardFromMyHand(used);
            gamePanel.setMyHand(myHand);
        }
    }

    private void removeOneCardFromMyHand(common.Card used) {
        // 같은 이름 카드가 여러 장 있을 수 있으니 1장만 제거
        for (int i = 0; i < myHand.size(); i++) {
            if (myHand.get(i).getCardName().equals(used.getCardName())) {
                myHand.remove(i);
                return;
            }
        }
    }

    // 상태 동기화
    private void handleSyncState(Message msg) {
        if (msg.getRoomName() != null && currentRoomName != null) {
            if (!currentRoomName.equals(msg.getRoomName())) return;
        }
        System.out.println("SYNC_STATE 수신: p1Cost=" + msg.getP1().getCost() + ", p2Cost=" + msg.getP2().getCost());
        gamePanel.updateState(msg.getP1(), msg.getP2());

        gamePanel.updateState(msg.getP1(), msg.getP2());
        updateCostCache(msg.getP1(), msg.getP2());
    }

    // 턴 종료 방송 - 상세 로직은 7단계에서 구현
    private void handleTurnEnd(Message msg) {
        if (msg.getRoomName() != null && currentRoomName != null) {
            if (!currentRoomName.equals(msg.getRoomName())) return;
        }

        String nextTurnUid = msg.getUserID();
        int nextTurn = msg.getTurn();

        gamePanel.appendChat("시스템: 턴이 변경되었습니다.");
        gamePanel.setTurnOwner(nextTurnUid);

        if (uid != null && uid.equals(nextTurnUid)) {
            // 3턴부터만 드로우 (1턴=p1, 2턴=p2는 초기세팅만)
            if (nextTurn >= 3) {
                drawCards(1);
                gamePanel.appendChat("시스템: 내 턴 시작 - 카드 1장 드로우");
            }
        }
    }

    // 게임 종료 방송 - 상세 로직은 7단계에서 구현
    private void handleGameEnd(Message msg) {
        if (msg.getRoomName() != null && currentRoomName != null) {
            if (!currentRoomName.equals(msg.getRoomName())) return;
        }
        String loser = msg.getUserID();
        if (uid != null && uid.equals(loser)) {
            JOptionPane.showMessageDialog(this, "패배했습니다.");
        } else {
            JOptionPane.showMessageDialog(this, "승리했습니다.");
        }
        // 게임이 끝나면 방/게임 상태를 초기화하고 목록으로 복귀(일단은)
        currentRoomName = null;
        changeScreen("ROOM_LIST");
    }

    // 카드풀 초기화 메서드
    private void initCardPool() {
        cardPool.clear();
        cardPool.add(new common.CardStrike());
        cardPool.add(new common.CardHeavyBlow());
        cardPool.add(new common.CardPierce());
        cardPool.add(new common.CardSharpEdge());
        cardPool.add(new common.CardWeaknessStrike());

        cardPool.add(new common.CardDefend());
        cardPool.add(new common.CardIronWall());
        cardPool.add(new common.CardCounterGuard());

        cardPool.add(new common.CardChargeUp());
        cardPool.add(new common.CardAdrenalineRush());
    }

    // 카드 코스트 매핑 메서드
    private int getCardCost(common.Card c) {
        if (c instanceof common.CardStrike) return 1;
        if (c instanceof common.CardHeavyBlow) return 2;
        if (c instanceof common.CardPierce) return 2;
        if (c instanceof common.CardSharpEdge) return 3;
        if (c instanceof common.CardWeaknessStrike) return 5;

        if (c instanceof common.CardDefend) return 2;
        if (c instanceof common.CardIronWall) return 3;
        if (c instanceof common.CardCounterGuard) return 4;

        if (c instanceof common.CardChargeUp) return 0;
        if (c instanceof common.CardAdrenalineRush) return 1;

        return 999; // 알 수 없는 카드면 막기
    }

    // 카드 드로우 메서드
    private void drawCards(int n) {
        if (cardPool.isEmpty()) initCardPool();

        for (int i = 0; i < n; i++) {
            int idx = (int) (Math.random() * cardPool.size());
            // 카드 객체를 그대로 공유하면 안 될 수도 있으니 새 객체로 넣는 게 안전
            // (각 카드가 상태를 가진다면 특히)
            common.Card c = cardPool.get(idx);
            myHand.add(c);
        }
        //카드가 내부 상태를 갖는 구조면 “복제 생성”이 필요할 수 있는데,
        // 지금 카드 클래스들 보통은 상수값만 들고 있어서 일단 이 수준으로 가고,
        // 문제 생기면 그때 카드 생성 방식을 조정.

        gamePanel.setMyHand(myHand);
    }

    // 카드 사용 요청 메서드
    public void requestUseCard(common.Card card) {
        if (card == null) return;

        // 내 턴이 아닐 때는 서버가 어차피 무시하지만, UI 단계에서 막아주기
        if (gamePanel != null && !gamePanel.isMyTurn()) {
            gamePanel.appendBattleLog("시스템: 내 턴이 아닙니다.");
            return;
        }

        // 클라 1차 코스트 체크(서버가 최종 판단, 클라는 UX용)
        int need = getCardCost(card);
        if (myCostCached < need) {
            gamePanel.appendBattleLog("시스템: 코스트 부족(" + myCostCached + "/" + need + ")");
            return;
        }

        // 서버 전송
        Message m = new Message(Message.MODE_USE_CARD, uid, card);
        sendMessage(m);
    }

    public void requestEndTurn() {
        // 내 턴이 아닐 때는 막기
        if (gamePanel != null && !gamePanel.isMyTurn()) {
            gamePanel.appendBattleLog("시스템: 내 턴이 아닙니다.");
            return;
        }
        if (currentRoomName == null) return;

        Message m = new Message(Message.MODE_TURN_END, uid);
        m.setRoomName(currentRoomName);  // Message에 setRoomName이 없다면 생성자/필드 방식에 맞춰서 수정 필요
        sendMessage(m);

        gamePanel.appendBattleLog("시스템: 턴 종료 요청 전송");
    }


    // 코스트 캐시 갱신
    private void updateCostCache(common.State p1, common.State p2) {
        if (p1 == null || p2 == null) return;
        if (uid == null) return;

        if (uid.equals(p1.getName())) {
            myCostCached = p1.getCost();
            enemyCostCached = p2.getCost();
        } else if (uid.equals(p2.getName())) {
            myCostCached = p2.getCost();
            enemyCostCached = p1.getCost();
        }
    }




    public static void main(String[] args) {
        new ClientFrame();
    }


}
