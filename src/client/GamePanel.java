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

    // 게임 영역
    private JPanel gameArea = new JPanel();

    // 게임영역 UI
    private JLabel l_enemy = new JLabel("상대 HP: - / COST: - / SHIELD: -");
    private JLabel l_timer = new JLabel("턴 정보: -");
    private JLabel l_me = new JLabel("내 HP: - / COST: - / SHIELD: -");

    private JTextArea t_battleLog = new JTextArea();
    private JScrollPane battleScroll;

    private JButton b_endTurn = new JButton("턴 종료");

    private JPanel handPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    private JButton[] b_hand = new JButton[5];

    // ===== 손패(누적, 겹침, 오버 시 돌출) =====
    private JLayeredPane handLayer = new JLayeredPane();
    private JScrollPane handScroll;

    private final int CARD_W = 90;
    private final int CARD_H = 55;
    private final int CARD_OVERLAP = 30;
    private final int CARD_RAISE_Y = 18;

    private boolean isMyTurn = false;

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

        // ===== 왼쪽: 게임영역(상단/중앙/하단) =====
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 상단: 상대 상태 + 턴 정보
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.add(l_enemy);
        topPanel.add(l_timer);
        leftPanel.add(topPanel, BorderLayout.NORTH);

        // 중앙: 전투 로그
        t_battleLog.setEditable(false);
        battleScroll = new JScrollPane(t_battleLog);
        leftPanel.add(battleScroll, BorderLayout.CENTER);

        // 하단: 내 상태 + 턴 종료 + 손패
        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel myStatePanel = new JPanel(new BorderLayout());
        myStatePanel.add(l_me, BorderLayout.CENTER);
        myStatePanel.add(b_endTurn, BorderLayout.EAST);

        bottomPanel.add(myStatePanel, BorderLayout.NORTH);

        // 손패 레이어 세팅(겹치기용)
        handLayer.setLayout(null);
        handLayer.setPreferredSize(new Dimension(300, CARD_H + CARD_RAISE_Y));

        handScroll = new JScrollPane(handLayer,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        handScroll.setPreferredSize(new Dimension(10, CARD_H + CARD_RAISE_Y + 10));

        bottomPanel.add(handScroll, BorderLayout.CENTER);

        leftPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.CENTER);

        // ===== 오른쪽: 채팅 영역 =====
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

        // ===== 이벤트 =====
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

        // 턴 종료 버튼(지금은 동작 없이 로그만)
        b_endTurn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clientFrame.requestEndTurn();
            }
        });
    }

    // 전투로그(미확정)
    public void appendBattleLog(String msg) {
        t_battleLog.append(msg + "\n");
        t_battleLog.setCaretPosition(t_battleLog.getDocument().getLength());
    }

    // === 채팅 관련 메서드들 ===
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

    // udp에서 호출할 함수
    public void updateTurnTimer(int turnNumber, String turnUid, int remainSec) {
        String me = clientFrame.getUid();
        String turnInfo = "턴 " + turnNumber + " / 현재 턴: " + turnUid + " / 남은시간: " + remainSec + "초";
        if (me != null && me.equals(turnUid)) turnInfo += " (내 턴)";
        l_timer.setText(turnInfo);

        setTurnOwner(turnUid);
    }

    // === 카드 및 턴 관련 메서드 ===
    // 턴 소유자 세팅 메서드
    public void setTurnOwner(String turnUid) {
        String me = clientFrame.getUid();
        isMyTurn = (me != null && me.equals(turnUid));
        b_endTurn.setEnabled(isMyTurn);

        // 손패 카드 버튼들도 한꺼번에 on/off
        for (Component c : handLayer.getComponents()) {
            if (c instanceof JButton) {
                c.setEnabled(isMyTurn);
            }
        }
    }

    // 손패 갱신 메서드 추가(누적 대응, 겹침+오버 돌출)
    public void setMyHand(java.util.List<common.Card> handCards) {
        handLayer.removeAll();

        if (handCards == null) handCards = new java.util.ArrayList<>();

        int x = 0;
        for (int i = 0; i < handCards.size(); i++) {
            common.Card card = handCards.get(i);
            JButton b = new JButton(card.getCardName());
            b.setEnabled(isMyTurn);

            int baseX = x;
            int baseY = CARD_RAISE_Y;

            b.setBounds(baseX, baseY, CARD_W, CARD_H);

            // 마우스 오버 시 돌출 + 앞으로
            b.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    b.setLocation(baseX, baseY - CARD_RAISE_Y);
                    handLayer.setComponentZOrder(b, 0);
                    handLayer.repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    b.setLocation(baseX, baseY);
                    handLayer.repaint();
                }
            });

            // 클릭 동작은 6단계에서 서버 전송으로 연결
            b.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    clientFrame.requestUseCard(card);
                }
            });

            handLayer.add(b, Integer.valueOf(i));
            x += CARD_OVERLAP;
        }

        int width = Math.max(300, x + CARD_W);
        handLayer.setPreferredSize(new Dimension(width, CARD_H + CARD_RAISE_Y));
        handLayer.revalidate();
        handLayer.repaint();
    }

    public boolean isMyTurn() {
        return isMyTurn;
    }


    public void updateState(State p1, State p2) {
        if (p1 == null || p2 == null) return;

        String me = clientFrame.getUid();
        State myState = p1;
        State enemyState = p2;

        if (me != null && p2.getName() != null && me.equals(p2.getName())) {
            myState = p2;
            enemyState = p1;
        }

        l_enemy.setText("상대 HP: " + enemyState.getHp() + " / COST: " + enemyState.getCost() + " / SHIELD: " + enemyState.getShield());
        l_me.setText("내 HP: " + myState.getHp() + " / COST: " + myState.getCost() + " / SHIELD: " + myState.getShield());
    }

}
