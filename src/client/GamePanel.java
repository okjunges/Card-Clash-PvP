package client;

import common.State;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GamePanel extends JPanel {

    private ClientFrame clientFrame;

    // 상단 방 정보(필드는 남겨두되, 레이아웃엔 안 올림)
    private JLabel l_roomName = new JLabel("방 이름: ");

    // 가운데 게임 영역(지금은 자리만)
    private JPanel gameArea = new JPanel();

    // 채팅 영역
    private JTextPane t_chat;
    private JTextField t_input = new JTextField(30);
    private JButton b_send = new JButton("보내기");

    // ClientFrame에서 넘겨준 Document 재사용
    private DefaultStyledDocument document;

    public GamePanel(ClientFrame clientFrame, DefaultStyledDocument document) {
        this.clientFrame = clientFrame;
        this.document = document;

        buildGUI();
    }

    private void buildGUI() {
        setLayout(new BorderLayout());

        // 중앙: 게임 화면 자리 (왼쪽 큰 영역)
        gameArea.setLayout(new BorderLayout());
        gameArea.add(
                new JLabel("게임 화면은 나중에 구현 예정입니다.", SwingConstants.CENTER),
                BorderLayout.CENTER
        );
        add(gameArea, BorderLayout.CENTER);

        // 오른쪽: 채팅 영역
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        chatPanel.setPreferredSize(new Dimension(220, 0)); // 오른쪽 폭

        t_chat = new JTextPane(document);
        t_chat.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(t_chat);
        chatPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(t_input, BorderLayout.CENTER);
        inputPanel.add(b_send, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        add(chatPanel, BorderLayout.EAST);

        // 이벤트
        t_input.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendChat();
            }
        });
        b_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendChat();
            }
        });
    }

    private void sendChat() {
        String text = t_input.getText().trim();
        if (text.isEmpty()) return;

        clientFrame.sendChat(text);   // 실제 전송은 ClientFrame이 담당
        t_input.setText("");
    }

    // ClientFrame이 호출해서 방 이름 설정 – 지금은 화면엔 안 보이지만 나중에 쓸 수 있게 유지
    public void setRoomName(String roomName) {
        l_roomName.setText("방 이름: " + roomName);
    }

    // 서버에서 채팅이 왔을 때 ClientFrame이 호출
    public void appendChat(String msg) {
        int len = document.getLength();
        try {
            document.insertString(len, msg + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        t_chat.setCaretPosition(document.getLength());
    }

    // 나중에 hp/cost/shield 상태 갱신용(지금은 빈 껍데기)
    public void updateState(State p1, State p2) {
        // TODO: 카드/체력 UI 붙일 때 사용
    }
}
