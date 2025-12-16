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

    // 내 화면 기준 표시용
    private String p1NameCached;
    private String p2NameCached;


    // 캐릭터 이미지 라벨
    private JLabel l_p1Avatar = new JLabel("", SwingConstants.CENTER);
    private JLabel l_p2Avatar = new JLabel("", SwingConstants.CENTER);

    private String p1AvatarPath = "/resources/img/blueknight.png"; // 내 캐릭터(항상 BLUE)
    private String p2AvatarPath = "/resources/img/redknight.png";  // 상대 캐릭터(항상 RED)

    // 상태
    private boolean isOwner = false;
    private String roomName;

    // 배경
    private Image bgImage;


    public WaitingRoomPanel(ClientFrame parent) {
        this.parent = parent;
        buildGUI();
    }

    private void buildGUI() {
        setLayout(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        bgImage = new ImageIcon(getClass().getResource("/resources/img/title.jpg")).getImage();

        // 상단: 방 이름
        l_roomName.setHorizontalAlignment(SwingConstants.CENTER);
        l_roomName.setFont(new Font("SansSerifl", Font.BOLD, 30));
        l_roomName.setForeground(Color.WHITE);
        add(l_roomName, BorderLayout.NORTH);

        // 중앙: P1, P2 박스
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        centerPanel.setOpaque(false);
        JPanel p1Panel = new JPanel(new BorderLayout());
        //p1Panel.setOpaque(false);
        p1Panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));
        l_p1Name.setHorizontalAlignment(SwingConstants.CENTER);
        p1Panel.add(l_p1Name, BorderLayout.NORTH);

        p1Panel.add(l_p1Avatar, BorderLayout.CENTER); // p1 이미지 들어가는 자리
        l_p1Avatar.setOpaque(false);

        JPanel p2Panel = new JPanel(new BorderLayout());
        //p2Panel.setOpaque(false);
        p2Panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true));
        l_p2Name.setHorizontalAlignment(SwingConstants.CENTER);
        p2Panel.add(l_p2Name, BorderLayout.NORTH);

        p2Panel.add(l_p2Avatar, BorderLayout.CENTER); // p2 이미지 들어가는 자리
        l_p2Avatar.setOpaque(false);

        // 닉네임 폰트 크기설정
        l_p1Name.setFont(new Font("Dialog", Font.BOLD, 22));
        l_p2Name.setFont(new Font("Dialog", Font.BOLD, 22));

        centerPanel.add(p1Panel);
        centerPanel.add(p2Panel);

        add(centerPanel, BorderLayout.CENTER);

        // 하단: 시작 버튼
        JPanel bottomPanel = new JPanel(new GridLayout(1, 0, 10, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 20, 40));
        bottomPanel.setOpaque(false);
        b_start.setFont(new Font("Dialog", Font.BOLD, 22));
        b_start.setPreferredSize(new Dimension(0, 70));
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

        p1NameCached = ownerName;
        p2NameCached = null;

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

        p2NameCached = guestName; // 캐시만 채움

        l_roomName.setText("방 이름: " + roomName);
        l_p2Name.setText("P2 : " + guestName);
        b_start.setEnabled(false);

        applyWaitingRoomAvatars();

        l_p1Avatar.setVisible(true);
        l_p2Avatar.setVisible(true);
    }

    // 상대 플레이어 닉네임과 아바타 채우기 (P1·P2 공통)
    public void setOpponentName(String opponentName) {
        if (isOwner) {
            p2NameCached = opponentName;
            l_p2Name.setText("P2 : " + opponentName);
            l_p2Avatar.setVisible(true);
        } else {
            p1NameCached = opponentName;
            l_p1Name.setText("P1 : " + opponentName);
            l_p1Avatar.setVisible(true);
        }

        applyWaitingRoomAvatars();
    }


    public String getPlayer1Name(){
        String player1Name = l_p1Name.getText().trim().replace("P1 : ", "");
        return player1Name;
    }

    // 외부참조
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

        // 기본: P1=BLUE, P2=RED
        String leftPath = "/resources/img/blueknight.png";
        String rightPath = "/resources/img/redknight.png";

        // 내가 P2면(손님이면) 내 슬롯이 오른쪽이니까,
        // 오른쪽(P2)이 BLUE가 되도록 swap
        if (!isOwner) {
            leftPath = "/resources/img/redknight.png";
            rightPath = "/resources/img/blueknight.png";
        }

        ImageIcon left = loadScaledIconFromResource(leftPath, w, h);
        ImageIcon right = loadScaledIconFromResource(rightPath, w, h);

        if (left != null) {
            l_p1Avatar.setIcon(left);
            l_p1Avatar.setText("");
        } else {
            l_p1Avatar.setIcon(null);
            l_p1Avatar.setText("P1 IMG");
        }

        if (right != null) {
            l_p2Avatar.setIcon(right);
            l_p2Avatar.setText("");
        } else {
            l_p2Avatar.setIcon(null);
            l_p2Avatar.setText("P2 IMG");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        }
    }


}

