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

    // 캐릭터 이미지 라벨
    private JLabel l_p1Avatar = new JLabel("", SwingConstants.CENTER);
    private JLabel l_p2Avatar = new JLabel("", SwingConstants.CENTER);

    private String p1AvatarPath = "/resources/img/blueknight.png";
    private String p2AvatarPath = "/resources/img/redknight.png";

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

        p1Panel.add(l_p1Avatar, BorderLayout.CENTER); // p1 이미지 들어가는 자리
        l_p1Avatar.setOpaque(false);

        JPanel p2Panel = new JPanel(new BorderLayout());
        p2Panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));
        l_p2Name.setHorizontalAlignment(SwingConstants.CENTER);
        p2Panel.add(l_p2Name, BorderLayout.NORTH);

        p2Panel.add(l_p2Avatar, BorderLayout.CENTER); // p2 이미지 들어가는 자리
        l_p2Avatar.setOpaque(false);

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

        l_p1Avatar.setVisible(false);
        l_p2Avatar.setVisible(false);
    }

    // P1 입장 (방장)
    public void enterAsOwner(String ownerName, String roomName) {
        this.isOwner = true;
        this.roomName = roomName;

        l_roomName.setText("방 이름: " + roomName);
        l_p1Name.setText("P1 : " + ownerName);
        l_p2Name.setText("P2 : 대기 중");
        b_start.setEnabled(true);

        applyWaitingRoomAvatars();

        l_p1Avatar.setVisible(true);
        l_p2Avatar.setVisible(false);
        l_p2Avatar.setIcon(null);
        l_p2Avatar.setText("");
    }

    // P2 입장 (손님)
    public void enterAsGuest(String guestName, String roomName) {
        this.isOwner = false;
        this.roomName = roomName;

        l_roomName.setText("방 이름: " + roomName);
        l_p2Name.setText("P2 : " + guestName);
        b_start.setEnabled(false);

        applyWaitingRoomAvatars();

        l_p1Avatar.setVisible(true);
        l_p2Avatar.setVisible(true);
    }

    // 상대 플레이어 닉네임 채우기 (P1·P2 공통)
    public void setOpponentName(String opponentName) {
        if (isOwner) {
            l_p2Name.setText("P2 : " + opponentName);
        } else {
            l_p1Name.setText("P1 : " + opponentName);
        }

        applyWaitingRoomAvatars();
        if (isOwner) {
            l_p2Avatar.setVisible(true);
        } else {
            l_p1Avatar.setVisible(true);
        }
    }

    public String getPlayer1Name(){
        String player1Name = l_p1Name.getText().trim().replace("P1 : ", "");
        return player1Name;
    }

    private ImageIcon loadScaledIconFromResource(String resourcePath, int w, int h) {
        try {
            java.net.URL url = getClass().getResource(resourcePath);
            if (url == null) return null;

            ImageIcon origin = new ImageIcon(url);
            Image scaled = origin.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }


    private void applyWaitingRoomAvatars() {
        int w = 220;
        int h = 280;

        ImageIcon p1 = loadScaledIconFromResource(p1AvatarPath, w, h);
        ImageIcon p2 = loadScaledIconFromResource(p2AvatarPath, w, h);

        // P1 슬롯
        if (p1 != null) l_p1Avatar.setIcon(p1);
        else l_p1Avatar.setText("P1 IMG");

        // P2 슬롯
        if (p2 != null) l_p2Avatar.setIcon(p2);
        else l_p2Avatar.setText("P2 IMG");
    }
}

