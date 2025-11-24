package client;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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
        JPanel bottomPanel = new JPanel(new GridLayout(1, 3));

        JButton b_create = new JButton("방 만들기");
        JButton b_enter = new JButton("선택한 방 들어가기");
        JButton b_title = new JButton("타이틀로");

        bottomPanel.add(b_create);
        bottomPanel.add(b_enter);
        bottomPanel.add(b_title);

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

            //------아래 코드는 임시 로컬용. 이후 삭제해야됌--------
            addRoom(roomName);                   // 일단 로컬에서도 추가(임시코드 삭제)
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

        // 타이틀로
        b_title.addActionListener(e -> parent.changeScreen("TITLE"));
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

    // 새 방을 하나 추가 (현재 로컬 상태에서만) -----> 로컬테스트용 함수로 삭제해야됌!!!
    public void addRoom(String roomName) {
        List<String> current = new ArrayList<>();
        for (JButton b : roomButtons) {
            String text = b.getText();
            if (text != null && !text.trim().isEmpty()) {
                current.add(text);
            }
        }
        current.add(roomName);

        updateRoomList(current);
    } //--------------------------------------------------------------

    // 서버에서 전체 방 목록을 줄 때 쓸 수 있는 함수 (나중 확장용)
    public void updateRoomList(List<String> rooms) {
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
