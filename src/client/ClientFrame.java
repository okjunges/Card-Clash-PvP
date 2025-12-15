package client;

import common.Message;
import common.ServerInfo;
import common.State;

import javax.swing.*;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Vector;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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

    private State lastMyState;   // 내가 마지막으로 받은 상태
    private boolean specialRoundActive = false;


    private DefaultStyledDocument document = new DefaultStyledDocument(); // 게임화면 채팅에 쓸 Document

    public ClientFrame() {
        super("Card Clash PvP");

        // server.txt에서 IP / PORT 읽기
        serverIP = ServerInfo.getInstance().getIP();
        serverPort = ServerInfo.getInstance().getPORT();

        setLayout(new BorderLayout());
        setSize(1000, 800);
        setResizable(false); // 창 크기 조절 막기
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

                            case Message.MODE_SPECIAL_START:
                                handleSpecialStart(msg);
                                break;

                            case Message.MODE_SPECIAL_RESULT:
                                handleSpecialResult(msg);
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

    private void stopUdpTimerReceiver() {
        // 스레드 종료 조건 깨기
        udpReceiveThread = null;

        // receive() 블로킹을 깨기 위해 소켓을 닫는다
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }

        udpSocket = null;
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
        // ===== 방 이름 중복 fail 처리 =====
        if ("fail".equals(msg.getMessage())) {
            JOptionPane.showMessageDialog(this, "방 생성 실패: 이미 존재하는 방 이름입니다.");
            return;
        }

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

        // 입장 실패 처리(서버가 "fail"을 message로 준다면)
        if ("fail".equals(msg.getMessage())) {
            if (uid != null && uid.equals(userId)) {
                currentRoomName = null; // 롤백
                JOptionPane.showMessageDialog(this, "방 입장 실패");
            }
            return;
        }

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

        // 캐릭터 머리위 닉네임용: 내/상대 UID 세팅
        String enemyUid = getEnemyUid(msg.getP1(), msg.getP2());
        gamePanel.setPlayerNames(uid, enemyUid);


        // 4) 현재 턴 반영
        gamePanel.setTurnOwner(msg.getUserID());

        // 5) 로그
        gamePanel.appendChat("시스템: 게임 시작, 현재 턴 = " + msg.getUserID());

        myHand.clear();
        drawCards(5);
    }

    private void goToGameScreen(String roomName) {
        gamePanel.setRoomName(roomName);
        changeScreen("GAME");

        try {
            if (udpSocket == null || udpSocket.isClosed()) {
                udpSocket = new DatagramSocket(udpPort); // 너가 login에서 정한 udpPort로 바인딩하는 구조면 이걸
                // 혹은 new DatagramSocket(0) 구조면 그 방식 그대로
            }
        } catch (Exception e) {
            System.out.println("UDP 소켓 생성 오류: " + e.getMessage());
        }

        startUdpTimerReceiver();
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

        String actorUid = msg.getUserID();

        // blue = 나(uid), red = 상대 (좌우 고정)
        boolean actorIsBlue = (uid != null && uid.equals(actorUid));

        gamePanel.showUsedCard(used, actorIsBlue);

        // 공격 카드
        if (used instanceof common.CardStrike
                || used instanceof common.CardHeavyBlow
                || used instanceof common.CardPierce
                || used instanceof common.CardWeaknessStrike
                || used instanceof common.CardBonus) {

            gamePanel.playAttackEffect(actorIsBlue);
        }
        // 버프 카드
        else if (used instanceof common.CardSharpEdge) {
            gamePanel.playBuffEffect(actorIsBlue);
        }
        // 방어 카드
        else if (used instanceof common.CardDefend
                || used instanceof common.CardIronWall
                || used instanceof common.CardCounterGuard) {

            gamePanel.playShieldEffect(actorIsBlue);
        }

        // 내 카드면 손패에서 1장 제거(서버가 성공 방송을 보냈을 때만 제거)
        if (uid != null && uid.equals(msg.getUserID()) && used != null) {
            removeOneCardFromMyHand(used);

            // ===== 카드 드로우 효과 (클라이언트 처리) =====
            if (used instanceof common.CardChargeUp) {
                drawCards(1);
                gamePanel.appendBattleLog("시스템: Charge Up 효과 - 카드 1장 드로우");
            }
            else if (used instanceof common.CardAdrenalineRush) {
                drawCards(2);
                gamePanel.appendBattleLog("시스템: Adrenaline Rush 효과 - 카드 2장 드로우");
            }
            else if (used instanceof common.CardBonus) {
                drawCards(1); // 기존 드로우 함수 재사용
                gamePanel.appendBattleLog("시스템: Bonus 효과 - 카드 1장 드로우");
            }

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
        // 내 상태 캐싱
        if (uid != null) {
            if (uid.equals(msg.getP1().getName())) {
                lastMyState = msg.getP1();
            } else if (uid.equals(msg.getP2().getName())) {
                lastMyState = msg.getP2();
            }
        }
        System.out.println("SYNC_STATE 수신: p1Cost=" + msg.getP1().getCost() + ", p2Cost=" + msg.getP2().getCost());

        // 상태가 올 때마다 닉네임도 유지/갱신
        String enemyUid = getEnemyUid(msg.getP1(), msg.getP2());
        gamePanel.setPlayerNames(uid, enemyUid);

        gamePanel.updateState(msg.getP1(), msg.getP2());
        updateCostCache(msg.getP1(), msg.getP2());
    }

    // 턴 종료 방송
    private void handleTurnEnd(Message msg) {
        System.out.println("TURN_END rcv: nextTurnUid=" + msg.getUserID() + ", nextTurn=" + msg.getTurn() + ", myUid=" + uid);

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
        boolean iWin = (uid != null && !uid.equals(loser));

        gamePanel.showGameResult(iWin);
    }

    // 카드풀 초기화 메서드 (샘플용 카드 타입 목록)
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

        if (c instanceof common.CardBonus) return 0;

        return 999; // 알 수 없는 카드면 막기
    }

    // 카드 드로우 메서드
    private void drawCards(int n) {
        if (cardPool.isEmpty()) initCardPool();

        for (int i = 0; i < n; i++) {
            int idx = (int) (Math.random() * cardPool.size());
            common.Card src = cardPool.get(idx);
            common.Card c;

            if (src instanceof common.CardStrike) c = new common.CardStrike();
            else if (src instanceof common.CardHeavyBlow) c = new common.CardHeavyBlow();
            else if (src instanceof common.CardPierce) c = new common.CardPierce();
            else if (src instanceof common.CardSharpEdge) c = new common.CardSharpEdge();
            else if (src instanceof common.CardWeaknessStrike) c = new common.CardWeaknessStrike();

            else if (src instanceof common.CardDefend) c = new common.CardDefend();
            else if (src instanceof common.CardIronWall) c = new common.CardIronWall();
            else if (src instanceof common.CardCounterGuard) c = new common.CardCounterGuard();

            else if (src instanceof common.CardChargeUp) c = new common.CardChargeUp();
            else if (src instanceof common.CardAdrenalineRush) c = new common.CardAdrenalineRush();

            else if (src instanceof common.CardBonus) c = new common.CardBonus();
            else c = src;

            myHand.add(c);
        }
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

        Message m = new Message(Message.MODE_USE_CARD, uid, card);
        m.setRoomName(currentRoomName);
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

    // 내/상대 UID 세팅
    private String getEnemyUid(common.State p1, common.State p2) {
        if (uid == null || p1 == null || p2 == null) return null;
        String p1n = p1.getName();
        String p2n = p2.getName();
        if (uid.equals(p1n)) return p2n;
        if (uid.equals(p2n)) return p1n;
        return null;
    }

    public void requestSurrender() {
        if (currentRoomName == null) return;

        Message m = new Message(Message.MODE_GAME_END, uid); // 패배자 uid를 서버에 알림
        m.setRoomName(currentRoomName);
        sendMessage(m);

        // 중복 클릭 방지용(서버 방송이 올 때까지)
        gamePanel.lockForGameEnd();
    }

    public void clearCurrentRoom() {
        currentRoomName = null;
    }

    public void requestLeaveRoomAfterGame() {
        // 1) UDP 타이머 수신 중지
        stopUdpTimerReceiver();

        // 2) 클라 게임 상태 초기화
        myHand.clear();
        myCostCached = 0;
        enemyCostCached = 0;

        // 3) 게임 UI 초기화
        gamePanel.resetGameUI();

        // 4) 화면 이동
        changeScreen("ROOM_LIST");

        // 5) 방 목록 갱신 요청(기존 있던 방식 유지)
        sendMessage(new Message(Message.MODE_ROOM_LIST));
    }

    private void handleSpecialStart(Message msg) {
        if (msg.getRoomName() != null && currentRoomName != null) {
            if (!currentRoomName.equals(msg.getRoomName())) return;
        }

        specialRoundActive = true;
        int maxCost = myCostCached;

        gamePanel.appendChat("시스템: 보너스 라운드 시작! (60초 내 배팅)");
        gamePanel.enterSpecialRound(maxCost);
    }

    private void handleSpecialResult(Message msg) {
        // 방 필터
        if (msg.getRoomName() != null && currentRoomName != null) {
            if (!currentRoomName.equals(msg.getRoomName())) return;
        }

        specialRoundActive = false;
        gamePanel.exitSpecialRound();

        State winner = msg.getWinner(); // null이면 무승부
        if (winner == null) {
            gamePanel.appendChat("시스템: 보너스 라운드 무승부 - 보너스 카드 없음");
        } else {
            String winName = winner.getName();
            if (uid != null && uid.equals(winName)) {
                // 승자면 보너스 카드 획득(클라 손패에 추가)
                myHand.add(new common.CardBonus());
                gamePanel.setMyHand(myHand);
                gamePanel.appendChat("시스템: 보너스 라운드 승리! 보너스 카드 획득");
            } else {
                gamePanel.appendChat("시스템: 보너스 라운드 패배 - 상대가 보너스 카드 획득");
            }
        }

        String nextTurnUid = msg.getUserID();
        int nextTurn = msg.getTurn();

        // 턴 owner 반영(버튼/손패 enable 갱신)
        gamePanel.setTurnOwner(nextTurnUid);

        if (uid != null && uid.equals(nextTurnUid)) {
            if (nextTurn >= 3) {
                // 보너스가 바로 이어지는 경우가 있어서 약간 지연 후 실행
                new javax.swing.Timer(150, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ((javax.swing.Timer) e.getSource()).stop();

                        // 이 시점에 SPECIAL_START가 왔다면 드로우 금지
                        if (specialRoundActive) return;

                        drawCards(1);
                        gamePanel.appendChat("시스템: 내 턴 시작 - 카드 1장 드로우");
                    }
                }).start();
            }
        }

        // udp 타이머 로그로 확인(임시)
        gamePanel.appendChat("시스템: 다음 턴 = " + nextTurnUid + " (턴 " + nextTurn + ")");
    }

    public void requestSpecialSubmit(int cost) {
        if (currentRoomName == null) return;

        Message m = new Message(Message.MODE_SPECIAL_SUBMIT, cost);
        m.setRoomName(currentRoomName);
        sendMessage(m);

        gamePanel.appendChat("시스템: 보너스 배팅 제출 (" + cost + ")");
    }


    public static void main(String[] args) {
        new ClientFrame();
    }


}
