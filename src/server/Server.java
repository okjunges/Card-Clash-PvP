package server;

import common.Message;

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
    private JButton b_connect = new JButton("서버 시작");
    private JButton b_disconnect = new JButton("서버 종료");
    private JButton b_exit = new JButton("종료");
    private int port;
    private ServerSocket serverSocket;
    private Thread acceptThread = null;
    private Thread clientThread;
    private Vector<ClientHandler> users = new Vector<ClientHandler>();

    public Server(int port) {
        super("Card Clash PvP Server");
        setBounds(510, 0, 500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildGUI();
        setVisible(true);

        this.port = port;
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
        controlPanel.add(b_connect);
        controlPanel.add(b_disconnect);
        controlPanel.add(b_exit);
        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                acceptThread = new Thread(new Runnable() {
                    @Override
                    public void run() { startServer(); }
                });
                acceptThread.start();
                b_disconnect.setEnabled(true);
                b_connect.setEnabled(false);
            }
        });
        b_disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("접속이 끊어졌습니다");
                b_connect.setEnabled(true);
                b_disconnect.setEnabled(false);
                disconnect();
            }
        });
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("server 종료");
                System.exit(0);
            }
        });
        b_disconnect.setEnabled(false);
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
                b_disconnect.setEnabled(false);
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
    public void disconnect() {
        try {
            acceptThread = null;
            serverSocket.close();
        } catch (IOException err) {
            System.err.println("서버 소켓 닫기 오류 > "+err.getMessage());
            System.exit(-1);
        }
    }
    public void printDisplay(String msg) {
        t_display.append(msg + "\n");
        t_display.setCaretPosition(t_display.getDocument().getLength());
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
                        continue;
                    }
                    else if (msg.getMode() == Message.MODE_LOGOUT) {
                        break;
                    }
                    else if (msg.getMode() == Message.MODE_TX_STRING) {
                        message = uid + " : " + msg.getMessage();
                        printDisplay(message);
                        broadcasting(msg);
                    }
                    else if (msg.getMode() == Message.MODE_TX_IMAGE) {
                        printDisplay(uid + " : " + msg.getMessage());
                        broadcasting(msg);
                    }
                }
                printDisplay(uid + "님이 연결을 종료하였습니다");
                printDisplay("현재 참가자 수 : " + users.size());
                users.removeElement(this);
                if (users.size() == 0) { b_disconnect.setEnabled(true); }
            } catch (ClassNotFoundException e) {
                printDisplay("잘못된 객체가 전달되었습니다");
            } catch (IOException e) {
                System.err.println("서버 읽기 오류 > " + e.getMessage());
            }
            finally {
                try {
                    clientSocket.close();
                    printDisplay(uid + "님이 연결을 종료하였습니다");
                    printDisplay("현재 참가자 수 : " + users.size());
                    users.removeElement(this);
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

        public void sendMessage(String message) {
            send(new Message(uid, Message.MODE_TX_STRING, message));
        }
        public void broadcasting(Message msg) {
            for (ClientHandler thread : users) { thread.send(msg); }
        }

        @Override
        public void run() { receiveMessages(clientSocket); }
    }

    public static void main(String[] args) {
        new Server(54321);
    }
}