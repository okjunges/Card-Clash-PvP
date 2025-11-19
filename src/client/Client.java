package client;

import common.Message;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.*;
import java.net.*;

public class Client extends JFrame {
    private DefaultStyledDocument document = new DefaultStyledDocument();
    private JTextPane t_display = new JTextPane(document);
    private JTextField t_input = new JTextField(100);
    private JButton b_select = new JButton("선택하기");
    private JButton b_send = new JButton("보내기");
    private JTextField t_name = new JTextField(7);
    private JTextField t_address = new JTextField(10);
    private JTextField t_port = new JTextField(5);
    private JButton b_connect = new JButton("접속하기");
    private JButton b_disconnect = new JButton("접속 끊기");
    private JButton b_exit = new JButton("종료하기");
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private Thread receiveThread = null;
    private ObjectOutputStream out;
    private String uid;

    public Client(String address, int port) {
        super("Card Clash PvP");
        serverAddress = address;
        serverPort = port;

        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildGUI();
        setVisible(true);
    }
    public void buildGUI() {
        add(createDisplayPanel(), BorderLayout.CENTER);
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0,1));
        panel.add(createInputPanel());
        panel.add(createInfoPanel());
        panel.add(createControlPanel());
        add(panel, BorderLayout.SOUTH);
    }
    public JPanel createDisplayPanel() {
        t_display.setEditable(false); // 편집 불가능한 TextArea로 설정 => 읽기 전용 텍스트
        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new BorderLayout());
        displayPanel.add(new JScrollPane(t_display), BorderLayout.CENTER);
        return displayPanel;
    }
    public JPanel createInputPanel() {
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.add(t_input, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 0));
        buttonPanel.add(b_select);
        buttonPanel.add(b_send);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        b_send.setEnabled(false);
        b_select.setEnabled(false);
        t_input.setEnabled(false);
        t_input.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { sendMessage(); }
        });
        t_input.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                b_send.setEnabled(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (t_input.getText().equals("") || t_input.getText() == null)
                    b_send.setEnabled(false);
            }
        });
        b_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
                b_send.setEnabled(false);
            }
        });
        b_select.addActionListener(new ActionListener() {
            JFileChooser chooser = new JFileChooser();
            @Override
            public void actionPerformed(ActionEvent e) {
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "JPG & GIF & PNG Images",        // 파일 이름에 창에 출력될 문자열
                        "jpg", "gif", "png");           // 파일 필터로 사용되는 확장자
                chooser.setFileFilter(filter);
                int ret = chooser.showOpenDialog(Client.this);   // 해당 컨테이너의 중앙에 배치
                if (ret != JFileChooser.APPROVE_OPTION) {
                    JOptionPane.showMessageDialog(Client.this, "파일을 선택하지 않았습니다.", "경고", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                t_input.setText(chooser.getSelectedFile().getAbsolutePath());   // 선택한 파일의 절대 경로명
                sendImage();
            }
        });
        return inputPanel;
    }
    public JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        try {
            InetAddress local = InetAddress.getLocalHost();
            String addr = local.getHostAddress();
            String[] part = addr.split("\\.");
            t_name.setText("guest" + part[3]);
            t_address.setText(this.serverAddress);
            t_port.setText(String.valueOf(serverPort));
        } catch (IOException e) {
            System.err.println("local address 읽기 오류 > " + e.getMessage());
        }
        infoPanel.add(new JLabel("아이디 : "));
        infoPanel.add(t_name);
        infoPanel.add(new JLabel("서버주소 : "));
        infoPanel.add(t_address);
        infoPanel.add(new JLabel("포트번호 : "));
        infoPanel.add(t_port);
        return infoPanel;
    }
    public JPanel createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(0,3));
        controlPanel.add(b_connect);
        controlPanel.add(b_disconnect);
        controlPanel.add(b_exit);
        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                serverAddress = t_address.getText();
                serverPort = Integer.parseInt(t_port.getText());
                try {
                    connectToServer();
                } catch (IOException err) {
                    System.err.println("클라이언트 접속 오류 > " + err.getMessage());
                    printDisplay("서버와의 연결 오류 : " + err.getMessage());
                }
            }
        });
        b_disconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("접속이 끊어졌습니다");
                disconnect();
            }
        });
        b_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (receiveThread != null) disconnect();
                System.out.println("client 종료");
                System.exit(0);
            }
        });
        b_disconnect.setEnabled(false);
        return controlPanel;
    }
    public void printDisplay(String msg) {
        int len = t_display.getDocument().getLength();
        try {
            document.insertString(len, msg + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        t_display.setCaretPosition(len);
    }
    public void printDisplay(ImageIcon icon) {
        t_display.setCaretPosition(t_display.getDocument().getLength());
        if (icon.getIconWidth() > 400) {
            Image img = icon.getImage();
            Image changeImg = img.getScaledInstance(400, -1, Image.SCALE_SMOOTH);
            icon = new ImageIcon(changeImg);
        }
        t_display.insertIcon(icon);
        printDisplay("");
        t_input.setText("");
    }
    public void connectToServer() throws UnknownHostException, IOException {
        socket = new Socket();
        SocketAddress sa = new InetSocketAddress(serverAddress, serverPort);
        socket.connect(sa, 3000);

        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));

        sendUserId();
        System.out.println("접속되었습니다");
        printDisplay(t_name.getText() + "님 반갑습니다.");
        toggleUI(true);

        receiveThread = new Thread(new Runnable() {
            ObjectInputStream in;

            public void receiveMessage() {
                try {
                    Message inMsg = (Message) in.readObject();
                    if (inMsg == null) {
                        disconnect();
                        printDisplay("서버 연결 끊김");
                        return;
                    }
                    switch (inMsg.getMode()) {
                        case Message.MODE_TX_STRING :
                            printDisplay(inMsg.getUserID() + " : " + inMsg.getMessage());
                            break;
                        case Message.MODE_TX_IMAGE :
                            printDisplay(inMsg.getUserID() + " : " + inMsg.getMessage());
                            printDisplay(inMsg.getImage());
                            break;
                    }
                } catch (IOException e) {
                    printDisplay("연결을 종료했습니다");
                } catch (ClassNotFoundException e) {
                    printDisplay("잘못된 객체가 전달되었습니다");
                }
            }

            @Override
            public void run() {
                try {
                    in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
                } catch (IOException e) {
                    printDisplay("입력 스트림이 열리지 않음");
                }
                while (receiveThread == Thread.currentThread()) { receiveMessage(); } }
        });
        receiveThread.start();
    }
    public void disconnect() {
        send(new Message(uid, Message.MODE_LOGOUT));
        try {
            toggleUI(false);
            receiveThread = null;
            socket.close();
        } catch (IOException err) {
            System.err.println("클라이언트 닫기 오류 > "+err.getMessage());
            System.exit(-1);
        }
    }
    public void toggleUI(boolean state) {
        // connect에 활성화
        b_disconnect.setEnabled(state);
        t_input.setEnabled(state);
        b_select.setEnabled(state);
        // disconnect에 활성화
        b_connect.setEnabled(!state);
        t_name.setEnabled(!state);
        t_address.setEnabled(!state);
        t_port.setEnabled(!state);
    }

    public void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
        } catch (IOException e) {
            System.err.println("클라이언트 일반 전송 오류> " + e.getMessage());
        }
    }

    public void sendMessage() {
        String message = t_input.getText();
        if (message.equals("") || message == null) return;
        send(new Message(uid, Message.MODE_TX_STRING, message));
        t_input.setText("");
    }

    public void sendUserId() {
        uid = t_name.getText();
        send(new Message(uid, Message.MODE_LOGIN));
    }

    public void sendImage() {
        String filename = t_input.getText().strip();
        if (filename.isEmpty()) return;

        File file = new File(filename);
        if (!file.exists()) {
            printDisplay(">> 파일이 존재하지 않습니다 : " + filename);
            return;
        }

        ImageIcon icon = new ImageIcon(filename);
        send(new Message(uid, Message.MODE_TX_IMAGE, file.getName(), icon));
        t_input.setText("");
    }

    public static void main(String[] args) {
        // 추후 작성
    }
}