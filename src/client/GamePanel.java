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

    private Image backgroundImage;
    private JPanel backgroundPanel;

    // 상단 방 정보(필드는 남겨두되, 레이아웃엔 안 올림)
    private JLabel l_roomName = new JLabel("방 이름: ");

    // 상단 왼쪽 GG 버튼
    private JButton b_gg = new JButton("GG");

    // 채팅 입력칸/보내기 영역을 통째로 갈아끼우기 위한 패널
    private JPanel chatBottomPanel = new JPanel(new BorderLayout());
    private JButton b_leaveRoom = new JButton("방 나가기");

    // 게임 영역
    private JPanel gameArea = new JPanel();

    // 전투 보드(양쪽 캐릭터)
    private JLabel l_leftChar = new JLabel("", SwingConstants.CENTER);
    private JLabel l_rightChar = new JLabel("", SwingConstants.CENTER);

    // 리소스 경로
    private String rightCharPath = "/resources/img/red_idle.png";
    private String leftCharPath = "/resources/img/blue_idle.png";

    // 게임영역 UI
    private JLabel l_enemy = new JLabel("상대 HP: - / COST: - / SHIELD: -");
    private JLabel l_timer = new JLabel("턴 정보: -");
    private JLabel l_me = new JLabel("내 HP: - / COST: - / SHIELD: -");

    private JTextArea t_battleLog = new JTextArea();
    private JScrollPane battleScroll;

    private JButton b_endTurn = new JButton("턴 종료");

    private JPanel handPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    private JButton[] b_hand = new JButton[5];

    // 상대 스탯 원형 UI
    private CircleStat c_enemyCost = new CircleStat("COST");
    private CircleStat c_enemyHp   = new CircleStat("HP");
    private CircleStat c_enemySh   = new CircleStat("SHIELD");

    // 내 스탯 원형 UI
    private CircleStat c_meSh   = new CircleStat("SHIELD");
    private CircleStat c_meHp   = new CircleStat("HP");
    private CircleStat c_meCost = new CircleStat("COST");

    // ===== 손패(누적, 겹침, 오버 시 돌출) =====
    private JLayeredPane handLayer = new JLayeredPane();
    private JScrollPane handScroll;

    // 카드 이미지(배경)
    private Image cardBgImage;

    // 카드 크기
    private final int CARD_W = 120;
    private final int CARD_H = 160;
    private final int CARD_OVERLAP = 35;
    private final int CARD_RAISE_Y = 25;

    private boolean isMyTurn = false;

    // 게임 종료 오버레이
    private JLayeredPane centerLayer = new JLayeredPane();
    private JPanel baseGamePanel = new JPanel(new BorderLayout());
    private JLabel l_resultOverlay = new JLabel("", SwingConstants.CENTER);

    private boolean gameEnded = false;

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

        // ===== 상단 패널 =====
        JPanel topPanel = new JPanel(new GridLayout(2, 1));

        // 1줄: GG 버튼 (왼쪽) + 상대 상태 (오른쪽)
        JPanel line1 = new JPanel(new BorderLayout());
        line1.add(b_gg, BorderLayout.WEST);
        JPanel enemyStatPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        enemyStatPanel.setOpaque(false);
        enemyStatPanel.add(c_enemyCost);
        enemyStatPanel.add(c_enemyHp);
        enemyStatPanel.add(c_enemySh);

        line1.add(enemyStatPanel, BorderLayout.EAST);


        // 2줄: 턴 정보 (가운데)
        JPanel line2 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        line2.add(l_timer);

        topPanel.add(line1);
        topPanel.add(line2);

        leftPanel.add(topPanel, BorderLayout.NORTH);

        // 중앙: 전투 보드(캐릭터 2명)
        JPanel arena = new JPanel(new BorderLayout());
        arena.setOpaque(false);


        // 좌/우 캐릭터 영역 폭 고정(원하면 조절)
        l_leftChar.setPreferredSize(new Dimension(260, 0));
        l_rightChar.setPreferredSize(new Dimension(260, 0));

        arena.add(l_leftChar, BorderLayout.WEST);
        arena.add(l_rightChar, BorderLayout.EAST);

        // 중앙은 이펙트 자리로 비워둠
        JPanel centerStage = new JPanel();
        centerStage.setOpaque(false);
        arena.add(centerStage, BorderLayout.CENTER);

        try {
            java.net.URL bgUrl = getClass().getResource("/resources/img/backGroundImg.jpg");
            System.out.println("bgUrl = " + bgUrl);
            if (bgUrl != null) {
                backgroundImage = new ImageIcon(bgUrl).getImage();
            }
        } catch (Exception e) {
            System.out.println("배경 이미지 로드 실패");
        }

        backgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(
                            backgroundImage,
                            0, 0,
                            getWidth(), getHeight(),
                            this
                    );
                }
            }
        };
        backgroundPanel.setOpaque(false);

        // ===== CENTER를 레이어로 구성(오버레이용) =====
        baseGamePanel.setLayout(new BorderLayout());
        baseGamePanel.setOpaque(false);
        baseGamePanel.add(arena, BorderLayout.CENTER);

        // 캐릭터 이미지 1회 세팅
        applyGameCharacters();

        centerLayer.setLayout(null);
        centerLayer.setOpaque(false);

        // 배경 (제일 아래)
        centerLayer.add(backgroundPanel, JLayeredPane.DEFAULT_LAYER);
        // 캐릭터/전투
        centerLayer.add(baseGamePanel, JLayeredPane.MODAL_LAYER);
        // 결과 오버레이
        centerLayer.add(l_resultOverlay, JLayeredPane.PALETTE_LAYER);

        centerLayer.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = centerLayer.getWidth();
                int h = centerLayer.getHeight();

                backgroundPanel.setBounds(0, 0, w, h);
                baseGamePanel.setBounds(0, 0, w, h);
                l_resultOverlay.setBounds(0, 0, w, h);

                centerLayer.revalidate();
                centerLayer.repaint();
            }
        });

        // 오버레이 기본 설정(처음엔 숨김)
        l_resultOverlay.setVisible(false);
        l_resultOverlay.setOpaque(true);
        l_resultOverlay.setBackground(new Color(220, 220, 220));
        l_resultOverlay.setFont(new Font("Arial", Font.BOLD, 80));

        // leftPanel CENTER에는 battleScroll이 아니라 centerLayer를 붙임
        leftPanel.add(centerLayer, BorderLayout.CENTER);

        // 하단: 내 상태 + 턴 종료 + 손패
        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel myStatePanel = new JPanel(new BorderLayout());
        JPanel myStatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        myStatPanel.setOpaque(false);
        myStatPanel.add(c_meSh);
        myStatPanel.add(c_meHp);
        myStatPanel.add(c_meCost);

        myStatePanel.add(myStatPanel, BorderLayout.CENTER);

        myStatePanel.add(b_endTurn, BorderLayout.EAST);

        bottomPanel.add(myStatePanel, BorderLayout.NORTH);

        // 손패 레이어 세팅(겹치기용)
        handLayer.setLayout(null);
        handLayer.setPreferredSize(new Dimension(300, CARD_H + CARD_RAISE_Y));

        try {
            java.net.URL cardUrl = getClass().getResource("/resources/img/card.png");
            if (cardUrl != null) {
                cardBgImage = new ImageIcon(cardUrl).getImage();
            } else {
                System.out.println("card.png 로드 실패");
            }
        } catch (Exception e) {
            System.out.println("card.png 로드 실패");
        }


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

        // ===== 채팅 하단 영역(교체 가능 패널) =====
        chatBottomPanel.setLayout(new BorderLayout());
        chatBottomPanel.removeAll();
        chatBottomPanel.add(t_input, BorderLayout.CENTER);
        chatBottomPanel.add(b_send, BorderLayout.EAST);

        // SOUTH에는 항상 chatBottomPanel만 붙인다
        chatPanel.add(chatBottomPanel, BorderLayout.SOUTH);

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

        b_gg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameEnded) return;
                clientFrame.requestSurrender();
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
            CardButton b = new CardButton(card);
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

        // 내/상대 매핑: clientFrame.getUid() 기준
        String my = clientFrame.getUid();

        common.State me;
        common.State enemy;

        if (my != null && my.equals(p1.getName())) {
            me = p1; enemy = p2;
        } else {
            me = p2; enemy = p1;
        }

        // 상대
        c_enemyHp.setValue(enemy.getHp());
        c_enemyCost.setValue(enemy.getCost());
        c_enemySh.setValue(enemy.getShield());

        // 나
        c_meHp.setValue(me.getHp());
        c_meCost.setValue(me.getCost());
        c_meSh.setValue(me.getShield());
    }

    public void lockForGameEnd() {
        // 중복 입력 방지
        b_gg.setEnabled(false);
        b_endTurn.setEnabled(false);
        for (Component c : handLayer.getComponents()) {
            if (c instanceof JButton) c.setEnabled(false);
        }
    }

    public void showGameResult(boolean iWin) {
        gameEnded = true;

        // 오버레이 표시
        l_resultOverlay.setText(iWin ? "Victory" : "Defeat");
        l_resultOverlay.setVisible(true);

        // 입력 잠금
        lockForGameEnd();

        // 채팅 입력칸 제거 → 방 나가기 버튼으로 교체
        chatBottomPanel.removeAll();
        chatBottomPanel.add(b_leaveRoom, BorderLayout.CENTER);
        chatBottomPanel.revalidate();
        chatBottomPanel.repaint();

        b_leaveRoom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clientFrame.requestLeaveRoomAfterGame(); // 아래에서 추가할 메서드
            }
        });
    }

    public void resetGameUI() {
        gameEnded = false;

        // 오버레이 제거
        l_resultOverlay.setVisible(false);
        l_resultOverlay.setText("");

        // 전투 로그 비우기
        t_battleLog.setText("");

        // 채팅 로그 비우기(원하면 유지 가능)
        t_chat.setText("");

        // 손패 비우기
        handLayer.removeAll();
        handLayer.revalidate();
        handLayer.repaint();

        // 버튼/입력 복구
        b_gg.setEnabled(true);
        b_endTurn.setEnabled(false); // 기본은 꺼두고, 내 턴되면 켜짐

        // 채팅 하단 입력칸 복구
        chatBottomPanel.removeAll();
        chatBottomPanel.add(t_input, BorderLayout.CENTER);
        chatBottomPanel.add(b_send, BorderLayout.EAST);
        chatBottomPanel.revalidate();
        chatBottomPanel.repaint();
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

    private void applyGameCharacters() {
        int w = 240;
        int h = 320;

        ImageIcon left = loadScaledIconFromResource(leftCharPath, w, h);
        ImageIcon right = loadScaledIconFromResource(rightCharPath, w, h);

        l_leftChar.setIcon(left);
        l_rightChar.setIcon(right);

        l_leftChar.setText(left == null ? "RED" : "");
        l_rightChar.setText(right == null ? "BLUE" : "");
    }

    private static class CardMeta {
        int cost;
        String topLine;    // 예: "ATK 4" / "DEF 8" / "DRAW 2"
        String effectLine; // 짧은 설명(2줄 정도로)
        CardMeta(int cost, String topLine, String effectLine) {
            this.cost = cost;
            this.topLine = topLine;
            this.effectLine = effectLine;
        }
    }

    private CardMeta getMeta(common.Card c) {
        if (c == null) return new CardMeta(0, "-", "-");

        // 공격
        if (c instanceof common.CardStrike) return new CardMeta(1, "ATK 4", "적에게 데미지 4");
        if (c instanceof common.CardHeavyBlow) return new CardMeta(2, "ATK 8", "적에게 데미지 8");
        if (c instanceof common.CardPierce) return new CardMeta(2, "ATK 5", "데미지 5, 방어 무시");
        if (c instanceof common.CardSharpEdge) return new CardMeta(3, "BUFF", "이번 턴 공격 +2");
        if (c instanceof common.CardWeaknessStrike) return new CardMeta(5, "ATK 6", "데미지 6, 취약 1턴");

        // 방어
        if (c instanceof common.CardDefend) return new CardMeta(2, "DEF 5", "방어 +5");
        if (c instanceof common.CardIronWall) return new CardMeta(3, "DEF 8", "방어 +8");
        if (c instanceof common.CardCounterGuard) return new CardMeta(4, "DEF 6", "다음 피격 시 3 반사");

        // 전략
        if (c instanceof common.CardChargeUp) return new CardMeta(0, "COST +1", "이번 턴 코스트 +1\n손패 1 드로우");
        if (c instanceof common.CardAdrenalineRush) return new CardMeta(1, "DRAW 2", "손패 2 드로우\n대신 HP -2");

        // 나머지(혹시 Bonus 같은 게 있으면 여기 추가)
        return new CardMeta(999, "?", "정의되지 않음");
    }

    private class CardButton extends JButton {
        private final common.Card card;
        private final CardMeta meta;

        CardButton(common.Card card) {
            this.card = card;
            this.meta = getMeta(card);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setMargin(new Insets(0, 0, 0, 0));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 배경 카드 이미지
            if (cardBgImage != null) {
                g2.drawImage(cardBgImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                g2.setColor(new Color(230, 230, 230));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }

            // 텍스트(카드명)
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Dialog", Font.BOLD, 14));
            String name = (card == null) ? "CARD" : card.getCardName();
            drawCentered(g2, name, 0, 20, getWidth(), 30);

            // 코스트(좌상단)
            g2.setFont(new Font("Dialog", Font.BOLD, 18));
            g2.drawString(String.valueOf(meta.cost), 12, 20);

            // ATK/DEF/DRAW 같은 핵심 수치
            g2.setFont(new Font("Dialog", Font.BOLD, 14));
            drawCentered(g2, meta.topLine, 0, 55, getWidth(), 20);

            // 효과(아래쪽 2줄 정도)
            g2.setFont(new Font("Dialog", Font.PLAIN, 12));
            drawMultiline(g2, meta.effectLine, 12, getHeight() - 55, getWidth() - 24, 14);

            g2.dispose();
            super.paintComponent(g); // 클릭/호버 이벤트는 유지
        }

        private void drawCentered(Graphics2D g2, String s, int x, int y, int w, int h) {
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (w - fm.stringWidth(s)) / 2;
            int ty = y + (h + fm.getAscent()) / 2 - 2;
            g2.drawString(s, tx, ty);
        }

        private void drawMultiline(Graphics2D g2, String text, int x, int y, int w, int lineH) {
            if (text == null) return;
            String[] lines = text.split("\n");
            int cy = y;
            for (String line : lines) {
                if (line == null) continue;
                g2.drawString(line, x, cy);
                cy += lineH;
            }
        }
    }

}
