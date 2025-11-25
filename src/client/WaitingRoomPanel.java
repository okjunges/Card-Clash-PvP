package client;

import common.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WaitingRoomPanel extends JPanel {

    private ClientFrame parent;

    // UI 컴포넌트
    private JLabel l_roomName = new JLabel("방 이름: ");
    private JLabel l_p1Name   = new JLabel("P1 : -");
    private JLabel l_p2Name   = new JLabel("P2 : -");
    private JButton b_start   = new JButton("시작하기");

    // 상태
    private boolean isOwner = false;
    private String roomName;

    public WaitingRoomPanel(ClientFrame parent) {
        this.parent = parent;
        buildGUI();
    }

    private void buildGUI() {
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 상단: 방 이름
        l_roomName.setHorizontalAlignment(SwingConstants.CENTER);
        l_roomName.setFont(new Font("Dialog", Font.BOLD, 18));
        add(l_roomName, BorderLayout.NORTH);

        // 중앙: P1, P2 박스
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0));

        JPanel p1Panel = new JPanel(new BorderLayout());
        p1Panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));
        l_p1Name.setHorizontalAlignment(SwingConstants.CENTER);
        p1Panel.add(l_p1Name, BorderLayout.NORTH);
        // 나중에 캐릭터 이미지 들어갈 자리
        p1Panel.add(new JLabel(" ", SwingConstants.CENTER), BorderLayout.CENTER);

        JPanel p2Panel = new JPanel(new BorderLayout());
        p2Panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));
        l_p2Name.setHorizontalAlignment(SwingConstants.CENTER);
        p2Panel.add(l_p2Name, BorderLayout.NORTH);
        p2Panel.add(new JLabel(" ", SwingConstants.CENTER), BorderLayout.CENTER);

        centerPanel.add(p1Panel);
        centerPanel.add(p2Panel);

        add(centerPanel, BorderLayout.CENTER);

        // 하단: 시작 버튼
        JPanel bottomPanel = new JPanel(new GridLayout(1, 0, 10, 0));
        bottomPanel.add(b_start);
        add(bottomPanel, BorderLayout.SOUTH);

        // 기본은 비활성화
        b_start.setEnabled(false);

        // 시작 버튼: 클라이언트가 서버에 게임 시작 요청
        b_start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.requestGameStart();
            }
        });
    }

    // P1 입장 (방장)
    public void enterAsOwner(String ownerName, String roomName) {
        this.isOwner = true;
        this.roomName = roomName;

        l_roomName.setText("방 이름: " + roomName);
        l_p1Name.setText("P1 : " + ownerName);
        l_p2Name.setText("P2 : 대기 중");
        b_start.setEnabled(true);
    }

    // P2 입장 (손님)
    public void enterAsGuest(String guestName, String roomName) {
        this.isOwner = false;
        this.roomName = roomName;

        l_roomName.setText("방 이름: " + roomName);
        l_p1Name.setText("P1 : 대기 중");
        l_p2Name.setText("P2 : " + guestName);
        b_start.setEnabled(false);
    }

    // 상대 플레이어 닉네임 채우기 (P1·P2 공통)
    public void setOpponentName(String opponentName) {
        if (isOwner) {
            l_p2Name.setText("P2 : " + opponentName);
        } else {
            l_p1Name.setText("P1 : " + opponentName);
        }
    }
}
