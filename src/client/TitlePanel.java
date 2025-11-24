package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TitlePanel extends JPanel {

    private ClientFrame parent;
    private JTextField t_name = new JTextField(12);
    private JButton b_connect = new JButton("게임 참가");

    public TitlePanel(ClientFrame parent) {
        this.parent = parent;

        // 전체 레이아웃: 위/중앙/아래
        setLayout(new BorderLayout());

        // ===== 센터: 4줄 중 2,3줄에 타이틀 배치 =====
        JPanel centerPanel = new JPanel(new GridLayout(4, 1));

        JLabel emptyTop = new JLabel();
        JLabel title1 = new JLabel("Card Clash", SwingConstants.CENTER);
        JLabel title2 = new JLabel("PvP", SwingConstants.CENTER);
        JLabel emptyBottom = new JLabel();

        title1.setFont(new Font("Dialog", Font.BOLD, 40));
        title2.setFont(new Font("Dialog", Font.BOLD, 36));

        centerPanel.add(emptyTop);     // 1번째 줄 (빈칸)
        centerPanel.add(title1);       // 2번째 줄
        centerPanel.add(title2);       // 3번째 줄
        centerPanel.add(emptyBottom);  // 4번째 줄 (빈칸)

        add(centerPanel, BorderLayout.CENTER);

        // 하단패널
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JLabel l_name = new JLabel("닉네임: ");
        l_name.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel namePanel = new JPanel(new BorderLayout(5, 0));
        namePanel.add(l_name, BorderLayout.WEST);
        namePanel.add(t_name, BorderLayout.CENTER);

        bottomPanel.add(namePanel, BorderLayout.CENTER);
        bottomPanel.add(b_connect, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);


        // ===== 버튼 동작 =====
        b_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
        });
    }
}
