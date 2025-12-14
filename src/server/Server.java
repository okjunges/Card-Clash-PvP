package server;

import common.Message;
import common.ServerInfo;
import common.State;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Server extends JFrame {
    private JTextArea t_display = new JTextArea("");
    private JButton b_exit = new JButton("종료");
    private int port;
    private ServerSocket serverSocket;
    private Thread acceptThread = null;
    private Thread clientThread;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();
    private Vector<Room> rooms = new Vector<Room>();
    private Logger log = new ServerLog();

    // UDP 통신
    private DatagramSocket udpSendSocket;
    // 서버 시간 스케줄러(내부적으로 스레드 존재)
    private ScheduledExecutorService timerExec;
    // 0.3초에 1번씩 턴 시간 보내기
    private static final long TIMER_TICK_MS = 300;
    private UdpTimerDispatcher udpDispatcher;

    public Server(int port) {
        super("Card Clash PvP Server");
        setBounds(1400, 0, 500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildGUI();
        setVisible(true);

        this.port = port;
        acceptThread = new Thread(new Runnable() {
            @Override
            public void run() { startServer(); }
        });
        acceptThread.start();
        initUdpTimerSystem();
    }
    public void buildGUI() {
        add(createDisplayPanel(), BorderLayout.CENTER);
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0,1));
        panel.add(createControlPanel());
        add(panel, BorderLayout.SOUTH);
    }
    public JPanel createDisplayPanel() {
        t_display.setEditable(false);
        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new BorderLayout());
        displayPanel.add(new JScrollPane(t_display), BorderLayout.CENTER);
        return displayPanel;
    }
    public JPanel createControlPanel() {
        JPanel controlPanel = new JPanel(new GridLayout(1,0));
        controlPanel.add(b_exit);
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("server 종료");
                timerExec.shutdown();
                udpSendSocket.close();
                System.exit(0);
            }
        });
        return controlPanel;
    }
    String getLocalAddr() {
        String localAddr = "";
        try {
            localAddr =  InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return localAddr;
    }

    // 구글링을 통해 공부하며 코드 작성 (0.5초마다 서버에서 클라이언트에게 턴 시간 전송)
    // UDP 소켓을 먼저 만들고, 스케줄러를 만들고, 그 스케줄러는 TIMER_TICK_MS 초마다 tickTimersAndBroadcast() 이 함수를 실행
    private void initUdpTimerSystem() {
        try {
            udpSendSocket = new DatagramSocket();
            udpDispatcher = new UdpTimerDispatcher(udpSendSocket);
            timerExec = Executors.newSingleThreadScheduledExecutor();

            timerExec.scheduleAtFixedRate(() -> {
                try {
                    tickTimersAndBroadcast();
                } catch (Exception e) {
                    // 타이머 스레드 죽지 않게 잡아두기
                    System.err.println("UDP Timer loop unexpected exception : " + e.getMessage());
                }
            }, 0, TIMER_TICK_MS, TimeUnit.MILLISECONDS); // 첫번쨰 인자(0) : 서버 시작하자마자를 의미

        } catch (SocketException e) {
            throw new RuntimeException("UDP 송신 소켓 생성 실패", e);
        }
    }
    private void tickTimersAndBroadcast() {
        long nowMs = System.currentTimeMillis();

        Vector<Room> snapshot;
        synchronized (rooms) {
            snapshot = new Vector<>(rooms);
        }
        for (Room room : snapshot) {
            if (!room.isGameRunning()) continue;

            room.forceTurnEnd(nowMs);

            if (!room.isGameRunning()) continue;

            int remain = room.getRemainingSec(nowMs);
            if (remain == 60 || remain == 50 || remain == 30 || remain == 10) {
                if (room.getLastLoggedRemain() != remain) {
                    printDisplay(room.getRoomName() + "방에서 턴 수 : " + room.getTurnNumber() + ", " + room.getCurrentTurnUid() + "의 턴 남은 시간 : " + remain + "  [60, 50, 30, 10]");
                    room.setLastLoggedRemain(remain);
                }
            }
            udpDispatcher.send(room, remain);
        }
    }
    public void startServer() {
        Socket clientSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            printDisplay("서버가 시작되었습니다 : " + getLocalAddr());
            // 나에대한 참조값이 같을 동안에만 반복
            while (acceptThread == Thread.currentThread()) {
                clientSocket = serverSocket.accept();
                t_display.append("클라이언트가 연결되었습니다 : " + clientSocket.getInetAddress().getHostAddress() + "\n");
                clientThread = new ClientHandler(clientSocket);
                clientThread.start();
                users.add((ClientHandler) clientThread);

                b_exit.setEnabled(false);
            }
        } catch (SocketException e) {
            System.err.println("서버 소켓 종료 > " + e.getMessage());
            printDisplay("서버 소켓 종료");
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally { // 현재 연결된 클라이언트 소켓을 닫는 일
            try {
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                System.err.println("서버 닫기 오류 > " + e.getMessage());
                System.exit(-1);
            }
        }
    }
    public void printDisplay(String msg) {
        // Swing은 스레드-세이프가 아니기 때문에 EDT(이벤트 디스패치 스레드)에서만 UI를 만져야 문제 발생 X
        // 스케줄러 스레드와 같은 다른 스레드에서 호출할 경우 랜덤하여 UI가 꼬이거나 멈추는 경우가 생길 수 있어 해당 부분을 방지하기 위해 처리
        SwingUtilities.invokeLater(() -> {
            t_display.append(msg + "\n");
            t_display.setCaretPosition(t_display.getDocument().getLength());
        });
    }
    private class ServerLog implements Logger {
        @Override
        public void display(String text) { printDisplay(text); }
    }
    public void printRoomPlayersState(Room room) {
        printDisplay(room.getRoomName() + "방에서 " + room.getP1State().getName() + "의 (hp, cost, shield) : (" + room.getP1State().getHp() + ", " + room.getP1State().getCost() + ", " + room.getP1State().getShield() + ")");
        printDisplay(room.getRoomName() + "방에서 " + room.getP2State().getName() + "의 (hp, cost, shield) : (" + room.getP2State().getHp() + ", " + room.getP2State().getCost() + ", " + room.getP2State().getShield() + ")");
    }

    private Room findRoomByName(String roomName) {
        if (roomName == null) return null;

        synchronized(rooms) {
            for (Room r : rooms) {
                if (r.getRoomName().equals(roomName)) return r;
            }
        }
        return null;
    }

    private Room findRoomByUser(String uid) {
        if (uid == null) return null;

        synchronized (rooms) {
            for (Room r : rooms) {
                if ((r.getPlayer1() != null && r.getPlayer1().getUid().equals(uid)) ||
                        (r.getPlayer2() != null && r.getPlayer2().getUid().equals(uid))) {
                    return r;
                }
            }
        }
        return null;
    }

    public class ClientHandler extends Thread implements Session {
        private Socket clientSocket;
        private ObjectOutputStream out;
        private String uid;
        private int udpPort;

        public ClientHandler(Socket clientSocket) { this.clientSocket = clientSocket; }

        public void receiveMessages(Socket socket) {
            try {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                Message msg;
                while ((msg = (Message)in.readObject()) != null) {
                    if (msg.getMode() == Message.MODE_LOGIN) {
                        login(msg);
                    }
                    else if (msg.getMode() == Message.MODE_CREATE_ROOM) {
                        createRoom(msg);
                    }
                    else if (msg.getMode() == Message.MODE_ENTER_ROOM) {
                        enterRoom(msg);
                    }
                    else if (msg.getMode() == Message.MODE_GAME_START) {
                        gameStart(msg);
                    }
                    else if (msg.getMode() == Message.MODE_CHAT) {
                        chat(msg);
                    }
                    else if (msg.getMode() == Message.MODE_USE_CARD) {
                        useCard(msg);
                    }
                    else if (msg.getMode() == Message.MODE_TURN_END) {
                        turnEnd(msg);
                    }
                    else if (msg.getMode() == Message.MODE_GAME_END) {
                        gameEnd(msg);
                    }
                    else if (msg.getMode() == Message.MODE_ROOM_LIST) {
                        sendRoomList();
                    }
                    else if (msg.getMode() == Message.MODE_SPECIAL_SUBMIT) {
                        submit(msg);
                    }
                }
            } catch (ClassNotFoundException e) {
                printDisplay("잘못된 객체가 전달되었습니다");
            } catch (IOException e) {
                System.err.println("서버 읽기 오류 > " + e.getMessage());
            }
            finally {
                try {
                    clientSocket.close();
                    Room room = findRoomByUser(uid);
                    if (room != null) {
                        // 플레이어가 강제 종료했을 때 해당 플레이어의 항복 처리 이후 게임 방 삭제
                        room.broadcasting(new Message(Message.MODE_GAME_END, uid));
                        printDisplay(room.getRoomName() + "에서 " + uid + "가 항복");
                        finishGame(room);
                    }
                    users.removeElement(this);
                    printDisplay(uid + "님이 연결을 종료하였습니다");
                    printDisplay("현재 참가자 수 : " + users.size());
                    // 접속한 플레이어가 없을 경우엔 서버 종료 가능
                    if (users.isEmpty()) { b_exit.setEnabled(true); }
                } catch (IOException e) {
                    System.err.println("서버 닫기 오류 > " + e.getMessage());
                    System.exit(-1);
                }
            }
        }

        @Override
        public void send(Message msg) {
            try {
                if (msg == null) {
                    System.err.println("서버 빈객체 전송 요청 요류");
                    return;
                }
                synchronized (out) {
                    // Java 직렬화에서 같은 State 객체를 반복 전송했을 때
                    // ObjectOutputStream 캐시 때문에 클라이언트가 업데이트를 못 받는 경우를 방지하기 위해서 캐시 삭제
                    out.reset();    // 캐시 삭제
                    out.writeObject(msg);
                    out.flush();
                }
            } catch (IOException e) {
                System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
            }
        }

        public void broadcasting(Message msg) {
            synchronized(users) {
                for (ClientHandler thread : users) {
                    thread.send(msg);
                }
            }
        }

        private void login(Message msg) {
            String id = msg.getUserID();
            boolean success = false;
            udpPort = msg.getUdpPort();

            // 중복되는 id의 사용자가 존재하는 것을 방지하기 위해 synchronized 키워드를 이용한 임계구역 설정 및 동시성 제어
            synchronized (users) {
                boolean duplicate = false;
                for (ClientHandler user : users) {
                    String otherId = user.getUid();
                    if (otherId != null && otherId.equals(id)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    uid = id;
                    success = true;
                }
            }
            if (success) {
                printDisplay("새 참가자 : " + uid + "의 UDP Port : " + udpPort);
                printDisplay("현재 참가자 수 : " + users.size());
            } else {
                msg.setMessage("fail");
                printDisplay("(로그인 실패) 이미 존재하는 ID : " + id);
            }
            send(msg);
        }

        private void createRoom(Message msg) {
            String name = msg.getRoomName();
            boolean success = false;

            // 중복되는 방 이름을 없애기 위해 synchronized 키워드를 이용한 임계구역 설정 및 동시성 제어
            synchronized (rooms) {
                boolean duplicate = false;
                for (Room room : rooms) {
                    if (room.getRoomName().equals(name)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    Room room = new Room(name, this, log);
                    rooms.add(room);
                    success = true;
                }
            }

            if (success) {
                printDisplay(uid + " 가 방 생성 : " + name);
                broadcasting(msg);
            } else {
                msg.setMessage("fail");
                printDisplay("(방 생성 실패) 이미 존재하는 방 : " + name);
                send(msg);
            }
        }

        private void enterRoom(Message msg) {
            Room room = findRoomByName(msg.getRoomName());
            synchronized (rooms) {
                if (room != null && !room.isReady()) {
                    send(new Message(Message.MODE_CREATE_ROOM, room.getPlayer1().getUid(), room.getRoomName()));
                    room.enterRoom(this);
                    printDisplay(uid + " 가 방 입장 : " + msg.getRoomName());
                    broadcasting(msg);
                } else {
                    printDisplay(msg.getRoomName() + " 방이 없습니다.");
                }
            }
        }

        private void gameStart(Message msg) {
            Room room = findRoomByUser(uid);
            if (room == null) {
                printDisplay("게임 시작 실패 : 방을 찾을 수 없음 - " + uid);
                return;
            }
            if (!room.isReady()) {
                printDisplay("게임 시작 실패 : " + room.getRoomName() + "방 인원 부족 - " + room.getRoomName());
                return;
            }

            long nowMs = System.currentTimeMillis();
            room.startGame(nowMs);

            printDisplay(room.getRoomName() + "방에서 게임을 시작했습니다");
            Message stateMsg = new Message(Message.MODE_GAME_START, room.getCurrentTurnUid(), room.getTurnNumber(), room.getP1State(), room.getP2State());
            room.broadcasting(stateMsg);
            printRoomPlayersState(room);
        }

        private void chat(Message msg) {
            Room room = findRoomByUser(uid);
            if (room == null) {
                printDisplay("채팅 실패 : 방을 찾을 수 없음 - " + uid);
                return;
            }
            String message = msg.getMessage();
            printDisplay(room.getRoomName() + "방에서 " + uid + "의 메세지 : " + message);
            room.broadcasting(msg);
        }

        private void useCard(Message msg) {
            // 플레이어가 속한 방 찾기
            Room room = findRoomByUser(uid);
            if (room == null) {
                printDisplay("카드 사용 실패 : 방을 찾을 수 없음 - " + uid);
                return;
            }
            if (!room.getCurrentTurnUid().equals(msg.getUserID())) {
                printDisplay("카드 사용 무시: 현재 턴 = " + room.getCurrentTurnUid() + ", 요청자 = " + msg.getUserID());
                return;
            }

            // 해당 카드 효과를 방에 적용
            Message stateMsg = null;
            synchronized (room) {
                boolean state = room.applyCard(msg.getCard(), this);
                if (!state) {
                    printDisplay(room.getRoomName() + "방에서 " + uid + "가 " + msg.getCard().getCardName() + " 카드 사용 실패");
                    msg.setMessage("fail");
                    msg.setCard(null);
                    send(msg);
                    return;
                }
                printDisplay(room.getRoomName() + "방에서 " + uid + "가 " + msg.getCard().getCardName() + " 카드 사용");
                room.broadcasting(msg);

                // 변경된 상태를 모든 플레이어에게 방송
                stateMsg = new Message(Message.MODE_SYNC_STATE, room.getP1State(), room.getP2State());
            }

            printRoomPlayersState(room);
            room.broadcasting(stateMsg);

            if (room.getP1State().getHp() <= 0) {
                printDisplay(room.getRoomName() + "에서 " + room.getPlayer1().getUid() + "의 hp가 0으로 패배");
                Message endMsg = new Message(Message.MODE_GAME_END, room.getPlayer1().getUid());
                room.broadcasting(endMsg);
                finishGame(room);
            }
            else if (room.getP2State().getHp() <= 0) {
                printDisplay(room.getRoomName() + "에서 " + room.getPlayer2().getUid() + "의 hp가 0으로 패배");
                Message endMsg = new Message(Message.MODE_GAME_END, room.getPlayer2().getUid());
                room.broadcasting(endMsg);
                finishGame(room);
            }
        }

        private void turnEnd(Message msg) {
            Room room = findRoomByUser(uid);
            if (room == null) {
                printDisplay("턴 종료 실패 : 방을 찾을 수 없음 - " + uid);
                return;
            }
            synchronized (room) {
                if (!uid.equals(msg.getUserID())) {
                    printDisplay("턴 종료 무시: uid = " + uid + ", 요청자 = " + msg.getUserID());
                    return;
                }
                // 현재 턴인 유저가 턴 종료한 것이 맞는지 확인
                if (!room.getCurrentTurnUid().equals(msg.getUserID())) {
                    printDisplay("턴 종료 무시: 현재 턴 = " + room.getCurrentTurnUid() + ", 요청자 = " + msg.getUserID());
                    return;
                }

                long nowMs = System.currentTimeMillis();
                Round nowRound = room.changeTurn(nowMs);
                if (nowRound == Round.NORMAL) {
                    printDisplay(room.getRoomName() + "방에서 " + room.getTurnNumber() + "턴의 " + room.getCurrentTurnUid() + " 시작");

                    Message endMsg = new Message(Message.MODE_TURN_END, room.getCurrentTurnUid(), room.getTurnNumber());
                    room.broadcasting(endMsg);

                    // 변경된 상태를 모든 플레이어에게 방송
                    Message stateMsg = new Message(Message.MODE_SYNC_STATE, room.getP1State(), room.getP2State());
                    room.broadcasting(stateMsg);
                    printRoomPlayersState(room);
                }
                else if (nowRound == Round.SPECIAL) {
                    printDisplay(room.getRoomName() + "방에서 보너스 라운드 경매 시작!");
                    Message m = new Message(Message.MODE_SPECIAL_START);
                    room.broadcasting(m);
                }
            }
        }

        private void gameEnd(Message msg) {
            Room room = findRoomByUser(msg.getUserID());
            if (room == null) {
                printDisplay("게임 종료 실패 : 방을 찾을 수 없음 - " + uid);
                return;
            }
            printDisplay(room.getRoomName() + "에서 " + msg.getUserID() + "가 항복");
            room.broadcasting(msg);
            finishGame(room);
        }

        private void sendRoomList() {
            Vector<String> list = new Vector<>();
            // 현재 서버에 있는 방 중에 player2가 아직 들어가지 않은(즉, 현재 플레이어가 들어갈 수 있는) 방 목록 반환
            synchronized (rooms) {
                for (Room r : rooms) { if (r.getPlayer2() == null) { list.add(r.getRoomName()); } }
            }
            Message returnMsg = new Message(Message.MODE_ROOM_LIST, list);
            send(returnMsg);
        }

        private void submit(Message msg) {
            Room room = findRoomByUser(uid);
            if (room == null) {
                printDisplay("배팅 실패 : 방을 찾을 수 없음 - " + uid);
                return;
            }

            synchronized (room) {
                int bill = msg.getCost();
                printDisplay(uid + "가 보너스 라운드에서 " + bill + " 배팅 완료");
                room.submit(this, bill, System.currentTimeMillis());
            }
        }

        @Override
        public InetSocketAddress getInetSocketAddress() {
            return new InetSocketAddress(clientSocket.getInetAddress(), udpPort);
        }

        @Override
        public void run() { receiveMessages(clientSocket); }

        @Override
        public String getUid() { return uid; }
        public void finishGame(Room room) {
            printDisplay(room.getRoomName() + " 게임 종료");
            room.endGame();
            // 서버에서 방만 삭제
            synchronized (rooms) { rooms.remove(room); }
        }
    }



    public static void main(String[] args) {
        int port = ServerInfo.getInstance().getPORT();
        Server server = new Server(port);
    }
}