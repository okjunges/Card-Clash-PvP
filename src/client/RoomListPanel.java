package client;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class RoomListPanel extends JPanel {

    private ClientFrame parent;

    // 최대 6개 방(3행 2열)
    private JButton[] roomButtons = new JButton[6];
    private String selectedRoomName = null;

    public RoomListPanel(ClientFrame parent) {
        this.parent = parent;

        setLayout(new BorderLayout());

        // ===== 방 리스트 영역 (3x2 격자) =====
        JPanel gridPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int i = 0; i < roomButtons.length; i++) {
            JButton b = new JButton();
            b.setFocusPainted(false);
            b.setBackground(Color.WHITE); // 기본: 빈 방(흰색)
            final int idx = i;

            b.addActionListener(e -> handleRoomClick(idx));

            roomButtons[i] = b;
            gridPanel.add(b);
        }

        add(gridPanel, BorderLayout.CENTER);

        // ===== 하단 버튼 줄 =====
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2));

        JButton b_create = new JButton("방 만들기");
        JButton b_enter = new JButton("선택한 방 들어가기");

        bottomPanel.add(b_create);
        bottomPanel.add(b_enter);

        add(bottomPanel, BorderLayout.SOUTH);

        // ===== 버튼 동작 =====
        // 방 만들기
        b_create.addActionListener(e -> {
            String roomName = JOptionPane.showInputDialog(
                    RoomListPanel.this, "방 이름을 입력하세요.");
            if (roomName == null) return;        // 취소
            roomName = roomName.trim();
            if (roomName.isEmpty()) return;

            parent.requestCreateRoom(roomName);  // 서버에 요청

        });

        // 선택한 방 들어가기
        b_enter.addActionListener(e -> {
            if (selectedRoomName == null) {
                JOptionPane.showMessageDialog(RoomListPanel.this,
                        "먼저 방을 선택하세요.");
                return;
            }
            parent.requestEnterRoom(selectedRoomName);
        });

    }

    // 방 버튼 클릭 시 처리
    private void handleRoomClick(int index) {
        String name = roomButtons[index].getText();
        if (name == null || name.trim().isEmpty()) {
            return; // 빈 칸이면 무시
        }

        selectedRoomName = name;

        // 색 다시 칠하기
        for (int i = 0; i < roomButtons.length; i++) {
            JButton b = roomButtons[i];
            String text = b.getText();
            if (text == null || text.trim().isEmpty()) {
                b.setBackground(Color.WHITE); // 빈 칸
            } else if (i == index) {
                b.setBackground(new Color(180, 210, 255)); // 선택된 방(조금 진한 파랑)
            } else {
                b.setBackground(new Color(220, 235, 255)); // 일반 방(연한 스카이블루)
            }
        }
    }

    // 서버에서 전체 방 목록을 줄 때 쓸 수 있는 함수
    public void updateRoomList(Vector<String> rooms) {
        for (int i = 0; i < roomButtons.length; i++) {
            JButton b = roomButtons[i];
            if (i < rooms.size()) {
                String name = rooms.get(i);
                b.setText(name);
                b.setEnabled(true);
                b.setBackground(new Color(220, 235, 255)); // 방 있음
            } else {
                b.setText("");
                b.setEnabled(false);
                b.setBackground(Color.WHITE); // 빈 칸
            }
        }
        selectedRoomName = null;
    }
}
