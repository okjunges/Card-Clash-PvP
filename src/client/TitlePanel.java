package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TitlePanel extends JPanel {

    private ClientFrame parent;
    private JTextField t_name = new JTextField(12);
    private JButton b_connect = new JButton("게임 참가");

    private Image backgroundImage; // 배경이미지

    // 타이틀 오버레이 색상 정의
    private final Color overlayColor = new Color(0, 0, 0, 120);

    public TitlePanel(ClientFrame parent) {
        this.parent = parent;

        try {
            java.net.URL bgUrl = getClass().getResource("/resources/img/title.jpg");
            if (bgUrl != null) {
                backgroundImage = new ImageIcon(bgUrl).getImage();
            } else {
                System.out.println("title.jpg 로드 실패");
            }
        } catch (Exception e) {
            System.out.println("title.jpg 로드 실패");
        }

        // 전체 레이아웃: 위/중앙/아래
        setLayout(new BorderLayout());

        // ===== CENTER: 타이틀(여백 줄이고 글씨 키우기) =====
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(220, 0, 40, 0)); // 위/아래 여백만 적당히

        JLabel title1 = new JLabel("Card Clash", SwingConstants.CENTER);
        JLabel title2 = new JLabel("PvP", SwingConstants.CENTER);

        title1.setAlignmentX(Component.CENTER_ALIGNMENT);
        title2.setAlignmentX(Component.CENTER_ALIGNMENT);

        title1.setFont(new Font("Dialog", Font.BOLD, 80));
        title1.setForeground(Color.WHITE);
        title2.setFont(new Font("Dialog", Font.BOLD, 72));
        title2.setForeground(new Color(230, 230, 230));

        centerPanel.add(title1);
        centerPanel.add(Box.createVerticalStrut(20)); // 두 줄 간격
        centerPanel.add(title2);

        add(centerPanel, BorderLayout.CENTER);

        // ===== SOUTH: 닉네임 입력 =====
        JPanel bottomPanel = new JPanel(new BorderLayout(12, 0));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(16, 24, 24, 24));

        JLabel l_name = new JLabel("닉네임:");
        l_name.setFont(new Font("Dialog", Font.BOLD, 18));

        t_name.setFont(new Font("Dialog", Font.PLAIN, 18));
        t_name.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        l_name.setForeground(Color.WHITE);
        t_name.setPreferredSize(new Dimension(420, 44));
        t_name.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        b_connect.setFont(new Font("Dialog", Font.BOLD, 18));
        b_connect.setPreferredSize(new Dimension(140, 44));

        JPanel namePanel = new JPanel(new BorderLayout(10, 0));
        namePanel.add(l_name, BorderLayout.WEST);
        namePanel.add(t_name, BorderLayout.CENTER);

        bottomPanel.add(namePanel, BorderLayout.CENTER);
        bottomPanel.add(b_connect, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        centerPanel.setOpaque(false);
        bottomPanel.setOpaque(false);
        namePanel.setOpaque(false);


        // 버튼 동작
        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doConnect();
            }
        });

        // 닉네임 입력칸 Enter 처리
        t_name.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doConnect();
            }
        });
    }

    private void doConnect() {
        String user = t_name.getText().trim();
        if (user.isEmpty()) {
            JOptionPane.showMessageDialog(TitlePanel.this, "닉네임을 입력하세요.");
            return;
        }

        boolean ok = parent.connectToServer(user);
        if (ok) {
            parent.changeScreen("ROOM_LIST");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 1) 배경 이미지
        if (backgroundImage != null) {
            g.drawImage(
                    backgroundImage,
                    0, 0,
                    getWidth(), getHeight(),
                    this
            );
        }

        // 2) 반투명 오버레이
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(overlayColor);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }


}
