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

    // 전투 보드(양쪽 캐릭터, 닉네임)
    private JLabel l_leftChar = new JLabel("", SwingConstants.CENTER);
    private JLabel l_rightChar = new JLabel("", SwingConstants.CENTER);
    private JLabel l_leftName = new JLabel("", SwingConstants.CENTER);
    private JLabel l_rightName = new JLabel("", SwingConstants.CENTER);


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
    private final int CARD_W = 140;
    private final int CARD_H = 190;
    private final int CARD_RAISE_Y = 25;

    private boolean isMyTurn = false;

    // 보너스 배팅 UI 및 스페셜 라운드 잠금
    private JDialog specialDialog;
    private javax.swing.Timer specialTimer;
    private boolean specialSubmitted = false;
    private boolean specialRoundActive = false;

    private static final int MAX_BET_COST = 30; //최대 코스트 제한

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

    // ===== 캐릭터 상태 이미지 경로 =====
    private final String BLUE_IDLE = "/resources/img/blue_idle.png";
    private final String BLUE_ATTACK = "/resources/img/blue_attack.png";
    private final String BLUE_ATTACKED = "/resources/img/blue_attacked.png";
    private final String BLUE_BUFF = "/resources/img/blue_buff.png";
    private final String BLUE_SHIELD = "/resources/img/blue_shildSpell.png";

    private final String RED_IDLE = "/resources/img/red_idle.png";
    private final String RED_ATTACK = "/resources/img/red_attack.png";
    private final String RED_ATTACKED = "/resources/img/red_attacked.png";
    private final String RED_BUFF = "/resources/img/red_buff.png";
    private final String RED_SHIELD = "/resources/img/red_shildSpell.png";

    // 1초 후 idle 복귀용 타이머
    private javax.swing.Timer effectTimer;

    // 카드 오버레이용
    private JPanel usedCardLayer = new JPanel(null);
    private static final int USED_CARD_W = 110;
    private static final int USED_CARD_H = 150;

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
        JPanel topPanel = new JPanel(new BorderLayout());

        // 1줄: GG 버튼 (왼쪽) + 상대 상태 (오른쪽)
        JLabel l_enemyTag = new JLabel("상대");
        l_enemyTag.setOpaque(true);
        l_enemyTag.setBackground(new Color(235, 235, 235));
        l_enemyTag.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel line1 = new JPanel(new BorderLayout());
        line1.add(b_gg, BorderLayout.WEST);
        JPanel enemyStatPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        enemyStatPanel.setOpaque(false);

        enemyStatPanel.add(l_enemyTag);
        enemyStatPanel.add(c_enemyCost);
        enemyStatPanel.add(c_enemyHp);
        enemyStatPanel.add(c_enemySh);
        line1.add(enemyStatPanel, BorderLayout.EAST);

        // 2줄: 턴 정보 (가운데)
        JPanel line2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        line2.add(l_timer);
        l_timer.setFont(new Font("Dialog", Font.BOLD, 18));
        line2.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        topPanel.add(line1, BorderLayout.NORTH);
        topPanel.add(line2, BorderLayout.SOUTH);

        leftPanel.add(topPanel, BorderLayout.NORTH);

        // 중앙: 전투 보드(캐릭터 2명)
        JPanel arena = new JPanel(new BorderLayout());
        arena.setOpaque(false);

        // 좌 캐릭터(닉+이미지)
        JPanel leftWrap = new JPanel(new BorderLayout());
        leftWrap.setOpaque(false);
        l_leftName.setFont(new Font("Dialog", Font.BOLD, 16));
        l_leftName.setForeground(Color.WHITE);
        leftWrap.add(l_leftName, BorderLayout.NORTH);
        leftWrap.add(l_leftChar, BorderLayout.CENTER);
        leftWrap.setPreferredSize(new Dimension(260, 0));

        // 우 캐릭터(닉+이미지)
        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.setOpaque(false);
        l_rightName.setFont(new Font("Dialog", Font.BOLD, 16));
        l_rightName.setForeground(Color.WHITE);
        rightWrap.add(l_rightName, BorderLayout.NORTH);
        rightWrap.add(l_rightChar, BorderLayout.CENTER);
        rightWrap.setPreferredSize(new Dimension(260, 0));

        arena.add(leftWrap, BorderLayout.WEST);
        arena.add(rightWrap, BorderLayout.EAST);


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

        usedCardLayer.setOpaque(false);

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
        //카드 오버레이
        centerLayer.add(usedCardLayer, JLayeredPane.POPUP_LAYER);
        // 결과 오버레이
        centerLayer.add(l_resultOverlay, JLayeredPane.PALETTE_LAYER);

        centerLayer.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = centerLayer.getWidth();
                int h = centerLayer.getHeight();

                backgroundPanel.setBounds(0, 0, w, h);
                baseGamePanel.setBounds(0, 0, w, h);
                usedCardLayer.setBounds(0, 0, w, h);
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
        JLabel l_meTag = new JLabel("나");
        l_meTag.setOpaque(true);
        l_meTag.setBackground(new Color(235, 235, 235));
        l_meTag.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel myStatePanel = new JPanel(new BorderLayout());
        JPanel myStatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        myStatPanel.setOpaque(false);

        myStatPanel.add(l_meTag);
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
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        handScroll.setPreferredSize(new Dimension(10, CARD_H + CARD_RAISE_Y + 10));

        bottomPanel.add(handScroll, BorderLayout.CENTER);
       // handScroll.setBorder(null);
        //handScroll.setViewportBorder(null);

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

    // 전투로그(초반 확인용 및 예비용 로그)
    public void appendBattleLog(String msg) {
        t_battleLog.append(msg + "\n");
        t_battleLog.setCaretPosition(t_battleLog.getDocument().getLength());
    }

    // 캐릭터와 화면 동기화
    public void setPlayerNames(String myUid, String enemyUid) {
        // 내 화면 기준: 왼쪽=나(블루), 오른쪽=상대(레드)
        l_leftName.setText(myUid == null ? "" : myUid);
        l_rightName.setText(enemyUid == null ? "" : enemyUid);
    }


    // === 채팅 관련 메서드들 ===
    private void sendChat() {
        String text = t_input.getText().trim();
        if (text.isEmpty()) return;

        clientFrame.sendChat(text);   // 실제 전송은 ClientFrame이 담당
        t_input.setText("");
    }

    // ClientFrame이 호출해서 방 이름 설정
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

        // 보너스 라운드면 턴 종료/손패는 잠금
        if (specialRoundActive) {
            b_endTurn.setEnabled(false);
            for (Component c : handLayer.getComponents()) {
                if (c instanceof JButton) c.setEnabled(false);
            }
            return; // 여기서 끝 (슬라이더/제출은 enterSpecialRound에서 따로 enable)
        }

        //평소 턴 로직
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

        int n = handCards.size();

        // 동적 step 계산
        int gap = 5; // 카드가 겹치지 않을 때 카드 사이 간격(원하는 값)
        int minStep = 20; // 너무 많이 겹치면 클릭이 힘드니 최소 이동폭(원하는 값)

        int avail = 300;
        if (handScroll != null && handScroll.getViewport() != null) {
            avail = handScroll.getViewport().getWidth();
            if (avail <= 0) avail = 300; // 초기 0 방어
        }

        int step; // 다음 카드로 갈 때 x 증가량
        if (n <= 1) {
            step = CARD_W + gap;
        } else {
            int noOverlapWidth = n * CARD_W + (n - 1) * gap;
            if (noOverlapWidth <= avail) {
                step = CARD_W + gap; // 공간 충분할 시 안 겹침
            } else {
                // 공간 부족 → avail 안에 들어오도록 step 줄임(=많이 겹침)
                step = (avail - CARD_W) / (n - 1);
                if (step < minStep) step = minStep; // 너무 과도한 겹침 방지
            }
        }

        int x = 0;
        final int HOVER_LAYER = 9999; // 항상 최상단
        for (int i = 0; i < handCards.size(); i++) {
            common.Card card = handCards.get(i);
            CardButton b = new CardButton(card, cardBgImage);
            b.setEnabled(isMyTurn);

            int baseX = x;
            int baseY = CARD_RAISE_Y;
            b.setBounds(baseX, baseY, CARD_W, CARD_H);

            final int originalLayer = i;  // 오른쪽 카드가 더 위에 보이도록: i가 클수록 layer가 크다

            // 마우스 오버 시 돌출
            b.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    b.setLocation(baseX, baseY - CARD_RAISE_Y);

                    handLayer.setLayer(b, HOVER_LAYER);
                    handLayer.moveToFront(b);

                    handLayer.repaint();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    b.setLocation(baseX, baseY);
                    handLayer.setLayer(b, originalLayer);
                    handLayer.moveToFront(b);
                    handLayer.repaint();
                }
            });

            b.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    clientFrame.requestUseCard(card);
                }
            });

            handLayer.add(b, Integer.valueOf(originalLayer));
            x += step;
        }

        int width = Math.max(avail, x + CARD_W);
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
                clientFrame.requestLeaveRoomAfterGame();
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


    public void enterSpecialRound(int maxCost) {
        // 이미 떠있으면 재호출 방지
        if (specialDialog != null && specialDialog.isShowing()) return;

        // 네트워크 수신 스레드에서 UI 띄우면 안됨 → EDT로 넘김
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                showSpecialDialog(maxCost);
            }
        });
    }

    // 구글링을 통해 공부하며 코드 작성
    private void showSpecialDialog(int maxCost) {
        specialRoundActive = true;

        // 행동 잠금(턴 종료/손패)
        b_endTurn.setEnabled(false);
        for (Component c : handLayer.getComponents()) {
            if (c instanceof JButton) c.setEnabled(false);
        }

        specialSubmitted = false;

        specialDialog = new JDialog(SwingUtilities.getWindowAncestor(this), "보너스 라운드 배팅",
                Dialog.ModalityType.MODELESS);   // 모달 금지
        specialDialog.setLayout(new BorderLayout());
        specialDialog.setSize(620, 300);
        specialDialog.setLocationRelativeTo(this);

        JLabel l_info = new JLabel("코스트를 얼마나 낼지 선택", SwingConstants.CENTER);
        specialDialog.add(l_info, BorderLayout.NORTH);

        int upper = Math.min(maxCost, MAX_BET_COST);
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, upper, 0);
        // 눈금 설정
        slider.setMajorTickSpacing(1);     // 모든 숫자 표시
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setPreferredSize(new Dimension(520, 100)); //가로 길이 늘림
        slider.setSnapToTicks(true); // 스냅. 정확한 값 선택
        slider.setFont(new Font("Dialog", Font.PLAIN, 13));

        specialDialog.add(slider, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        JLabel l_time = new JLabel("남은시간: 60초", SwingConstants.CENTER);
        JButton b_submit = new JButton("제출");
        bottom.add(l_time, BorderLayout.CENTER);
        bottom.add(b_submit, BorderLayout.EAST);
        specialDialog.add(bottom, BorderLayout.SOUTH);

        b_submit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (specialSubmitted) return;
                specialSubmitted = true;

                int cost = slider.getValue();
                clientFrame.requestSpecialSubmit(cost);

                if (specialTimer != null) specialTimer.stop();
                specialDialog.dispose();
            }
        });

        specialDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (!specialSubmitted) {
                    specialSubmitted = true;
                    clientFrame.requestSpecialSubmit(0);
                }
                if (specialTimer != null) specialTimer.stop();
            }
        });

        final int[] remain = {60};
        specialTimer = new javax.swing.Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                remain[0]--;
                l_time.setText("남은시간: " + remain[0] + "초");
                if (remain[0] <= 0) {
                    specialTimer.stop();
                    if (!specialSubmitted) {
                        specialSubmitted = true;
                        clientFrame.requestSpecialSubmit(0);
                    }
                    specialDialog.dispose();
                }
            }
        });
        specialTimer.start();

        specialDialog.setVisible(true);
    }

    public void exitSpecialRound() {
        specialRoundActive = false;
        try {
            if (specialTimer != null) specialTimer.stop();
            if (specialDialog != null && specialDialog.isShowing()) specialDialog.dispose();
        } catch (Exception ignored) {}
    }

    private void setLeftChar(String path) {
        leftCharPath = path;
        applyGameCharacters();
    }

    private void setRightChar(String path) {
        rightCharPath = path;
        applyGameCharacters();
    }

    private void resetBothToIdle() {
        setLeftChar(BLUE_IDLE);
        setRightChar(RED_IDLE);
    }


    public void playAttackEffect(boolean attackerIsBlue) {
        // Swing UI는 EDT에서만 변경
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> playAttackEffect(attackerIsBlue));
            return;
        }

        // 기존 타이머 있으면 끊고 새로 시작
        if (effectTimer != null) effectTimer.stop();

        if (attackerIsBlue) {
            setLeftChar(BLUE_ATTACK);
            setRightChar(RED_ATTACKED);
        } else {
            setRightChar(RED_ATTACK);
            setLeftChar(BLUE_ATTACKED);
        }

        effectTimer = new javax.swing.Timer(1000, e -> resetBothToIdle());
        effectTimer.setRepeats(false);
        effectTimer.start();
    }

    public void playBuffEffect(boolean blueSide) {
        // Swing UI는 EDT에서만 변경
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> playBuffEffect(blueSide));
            return;
        }

        if (effectTimer != null) effectTimer.stop();

        if (blueSide) setLeftChar(BLUE_BUFF);
        else setRightChar(RED_BUFF);

        effectTimer = new javax.swing.Timer(1000, e -> resetBothToIdle());
        effectTimer.setRepeats(false);
        effectTimer.start();
    }

    public void playShieldEffect(boolean blueSide) {
        // Swing UI는 EDT에서만 변경
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> playShieldEffect(blueSide));
            return;
        }

        if (effectTimer != null) effectTimer.stop();

        if (blueSide) setLeftChar(BLUE_SHIELD);
        else setRightChar(RED_SHIELD);

        effectTimer = new javax.swing.Timer(1000, e -> resetBothToIdle());
        effectTimer.setRepeats(false);
        effectTimer.start();
    }


    public void showUsedCard(common.Card usedCard, boolean actorIsBlue) {
        if (usedCard == null) return;

        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showUsedCard(usedCard, actorIsBlue));
            return;
        }

        // 카드 컴포넌트 생성 (기존 CardButton 재사용)
        CardButton view = new CardButton(usedCard, cardBgImage);
        view.setEnabled(false);
        view.setFocusable(false);
        view.setCursor(Cursor.getDefaultCursor());
        view.setSize(USED_CARD_W, USED_CARD_H);

        int layerW = usedCardLayer.getWidth();
        int layerH = usedCardLayer.getHeight();

        // 중앙 기준, 내 카드(파랑=actorIsBlue)는 약간 왼쪽 / 상대(빨강)는 약간 오른쪽
        int baseX = (layerW - USED_CARD_W) / 2 + (actorIsBlue ? -120 : 120);
        int baseY = (layerH - USED_CARD_H) / 2 + 40;

        // 같은 쪽에서 연속 사용하면 조금씩 중앙으로 밀기(겹침 완화)
        int sameSideCount = 0;
        for (Component c : usedCardLayer.getComponents()) {
            if (c instanceof JComponent) {
                Object side = ((JComponent) c).getClientProperty("side");
                if (side != null && side.equals(actorIsBlue ? "L" : "R")) sameSideCount++;
            }
        }
        int gap = 25; // 카드가 겹치는 정도
        int x = baseX + (actorIsBlue ? (sameSideCount * gap) : -(sameSideCount * gap));
        int y = baseY;

        x = Math.max(10, Math.min(x, layerW - USED_CARD_W - 10)); // 화면 밖으로 안나가게 보정

        view.putClientProperty("side", actorIsBlue ? "L" : "R");
        view.setLocation(x, y);

        usedCardLayer.add(view, 0);
        usedCardLayer.repaint();

        // 3초 뒤 이 카드만 제거
        javax.swing.Timer t = new javax.swing.Timer(3000, e -> {
            usedCardLayer.remove(view);
            usedCardLayer.revalidate();
            usedCardLayer.repaint();
        });
        t.setRepeats(false);
        t.start();
    }


}
