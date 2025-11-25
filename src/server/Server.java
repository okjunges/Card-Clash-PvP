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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;

public class Server  extends JFrame {
    private JTextArea t_display = new JTextArea("");
    private JButton b_exit = new JButton("종료");
    private int port;
    private ServerSocket serverSocket;
    private Thread acceptThread = null;
    private Thread clientThread;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();
    private Vector<Room> rooms = new Vector<Room>();

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
        t_display.append(msg + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
    }
    public void printRoomPlayersState(Room room) {
        printDisplay(room.roomName + "방에서 " + room.p1State.getName() + "의 (hp, cost, shield) : (" + room.p1State.getHp() + ", " + room.p1State.getCost() + ", " + room.p1State.getShield() + ")");
        printDisplay(room.roomName + "방에서 " + room.p2State.getName() + "의 (hp, cost, shield) : (" + room.p2State.getHp() + ", " + room.p2State.getCost() + ", " + room.p2State.getShield() + ")");
    }

    private Room findRoomByName(String roomName) {
        if (roomName == null) return null;

        for (Room r : rooms) {
            if (r.roomName.equals(roomName)) return r;
        }
        return null;
    }

    private Room findRoomByUser(String uid) {
        if (uid == null) return null;

        for (Room r : rooms) {
            if ((r.player1 != null && r.player1.getUid().equals(uid)) ||
                    (r.player2 != null && r.player2.getUid().equals(uid))) {
                return r;
            }
        }
        return null;
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private ObjectOutputStream out;
        private String uid;

        public ClientHandler(Socket clientSocket) { this.clientSocket = clientSocket; }

        public void receiveMessages(Socket socket) {
            try {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                String message;
                Message msg;
                while ((msg = (Message)in.readObject()) != null) {
                    if (msg.getMode() == Message.MODE_LOGIN) {
                        uid = msg.getUserID();
                        printDisplay("새 참가자 : " + uid);
                        printDisplay("현재 참가자 수 : " + users.size());
                    }
                    else if (msg.getMode() == Message.MODE_CREATE_ROOM) {
                        Room room = new Room(msg.getRoomName(), this);
                        rooms.add(room);
                        printDisplay(uid + " 가 방 생성 : " + msg.getRoomName());
                        broadcasting(msg);
                    }
                    else if (msg.getMode() == Message.MODE_ENTER_ROOM) {
                        Room room = findRoomByName(msg.getRoomName());
                        if (room != null && !room.isReady()) {
                            send(new Message(Message.MODE_CREATE_ROOM, room.player1.getUid(), room.roomName));
                            room.enterRoom(this);
                            printDisplay(uid + " 가 방 입장 : " + msg.getRoomName());
                            broadcasting(msg);
                        }
                        else { printDisplay(msg.getRoomName() + " 방이 없습니다."); }
                    }
                    else if (msg.getMode() == Message.MODE_GAME_START) {
                        Room room = findRoomByUser(uid);
                        if (room == null) {
                            printDisplay("게임 시작 실패 : 방을 찾을 수 없음 - " + uid);
                            continue;
                        }
                        if (!room.isReady()) {
                            printDisplay("게임 시작 실패 : " + room.roomName + "방 인원 부족 - " + room.roomName);
                            continue;
                        }
                        printDisplay(room.roomName + "방에서 게임을 시작했습니다");
                        Message stateMsg = new Message(Message.MODE_GAME_START, room.p1State, room.p2State);
                        room.broadcasting(stateMsg);
                        printRoomPlayersState(room);
                    }
                    else if (msg.getMode() == Message.MODE_CHAT) {
                        Room room = findRoomByUser(uid);
                        if (room == null) {
                            printDisplay("채팅 실패 : 방을 찾을 수 없음 - " + uid);
                            continue;
                        }
                        message = msg.getMessage();
                        printDisplay(room.roomName + "방에서 " + uid + "의 메세지 : " + message);
                        room.broadcasting(msg);
                    }
                    else if (msg.getMode() == Message.MODE_USE_CARD) {
                        // 플레이어가 속한 방 찾기
                        Room room = findRoomByUser(uid);
                        if (room == null) {
                            printDisplay("카드 사용 실패 : 방을 찾을 수 없음 - " + uid);
                            continue;
                        }
                        room.broadcasting(msg);
                        printDisplay(room.roomName + "방에서 " + uid + "가 " + msg.getCardCode() + "번 카드 사용");

                        // 해당 카드 효과를 방에 적용
                        room.applyCard(msg.getCardCode(), this);

                        // 변경된 상태를 모든 플레이어에게 방송
                        Message stateMsg = new Message(Message.MODE_SYNC_STATE, room.p1State, room.p2State);

                        printRoomPlayersState(room);
                        room.broadcasting(stateMsg);

                        if (room.p1State.getHp() <= 0) {
                            printDisplay(room.roomName + "에서 " + room.player1.getUid() + "의 hp가 0으로 패배");
                            Message endMsg = new Message(Message.MODE_GAME_END, room.player1.getUid());
                            room.broadcasting(endMsg);
                            finishGame(room);
                        }
                        else if (room.p2State.getHp() <= 0) {
                            printDisplay(room.roomName + "에서 " + room.player2.getUid() + "의 hp가 0으로 패배");
                            Message endMsg = new Message(Message.MODE_GAME_END, room.player2.getUid());
                            room.broadcasting(endMsg);
                            finishGame(room);
                        }
                    }
                    else if (msg.getMode() == Message.MODE_TURN_END) {
                        Room room = findRoomByUser(uid);
                        if (room == null) {
                            printDisplay("턴 종료 실패 : 방을 찾을 수 없음 - " + uid);
                            continue;
                        }
                        printDisplay(room.roomName + "에서 " + msg.getUserID() + "의 턴 종료");
                        room.broadcasting(msg);
                    }
                    else if (msg.getMode() == Message.MODE_GAME_END) {
                        Room room = findRoomByUser(msg.getUserID());
                        if (room == null) {
                            printDisplay("게임 종료 실패 : 방을 찾을 수 없음 - " + uid);
                            continue;
                        }
                        printDisplay(room.roomName + "에서 " + msg.getUserID() + "가 항복");
                        room.broadcasting(msg);
                        finishGame(room);
                    }
                    else if (msg.getMode() == Message.MODE_ROOM_LIST) {
                        Vector<String> list = new Vector<>();
                        // 현재 서버에 있는 방 중에 player2가 아직 들어가지 않은(즉, 현재 플레이어가 들어갈 수 있는) 방 목록 반환
                        for (Room r : rooms) { if (r.player2 == null) { list.add(r.roomName); } }
                        Message returnMsg = new Message(Message.MODE_ROOM_LIST, list);
                        send(returnMsg);
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
                        printDisplay(room.roomName + "에서 " + uid + "가 항복");
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

        public void send(Message msg) {
            try {
                out.writeObject(msg);
                out.flush();
            } catch (IOException e) {
                System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
            }
        }

        public void broadcasting(Message msg) { for (ClientHandler thread : users) { thread.send(msg); } }

        @Override
        public void run() { receiveMessages(clientSocket); }

        public String getUid() { return uid; }
        public void finishGame(Room room) {
            printDisplay(room.roomName + " 게임 종료");
            // 서버에서 방만 삭제
            rooms.remove(room);
        }
    }

    public class Room {
        String roomName;
        ClientHandler player1;
        ClientHandler player2;
        State p1State;
        State p2State;

        Room(String roomName, ClientHandler player1) {
            this.roomName = roomName;
            this.player1 = player1;
            this.p1State = new State(player1.getUid(), 30, 3, 0);
        }
        public void enterRoom(ClientHandler player2) {
            this.player2 = player2;
            this.p2State = new State(player2.getUid(), 30, 3, 0);
        }
        public boolean isReady() {
            return player1 != null && player2 != null;
        }
        public State getStateOf(ClientHandler handler) {
            if (handler == player1) return p1State;
            else if (handler == player2) return p2State;
            else return null;
        }
        public State getOpponentStateOf(ClientHandler handler) {
            if (handler == player1) return p2State;
            else if (handler == player2) return p1State;
            else return null;
        }
        public void applyCard(int cardCode, ClientHandler caster) {
            State me = getStateOf(caster);
            State enemy = getOpponentStateOf(caster);

            switch (cardCode) {
                case Message.Strike:
                    break;
            }
        }

        public void broadcasting(Message msg) {
            if (player1 != null) player1.send(msg);
            if (player2 != null) player2.send(msg);
        }
    }

    public static void main(String[] args) {
        int port = ServerInfo.getInstance().getPORT();
        Server server = new Server(port);
    }
}