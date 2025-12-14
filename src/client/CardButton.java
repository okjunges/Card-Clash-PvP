package client;

import javax.swing.*;
import java.awt.*;

public class CardButton extends JButton {

    private final common.Card card;
    private final Image cardBgImage;
    private final CardMeta meta;

    public CardButton(common.Card card, Image cardBgImage) {
        this.card = card;
        this.cardBgImage = cardBgImage;
        this.meta = getMeta(card);

        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private static CardMeta getMeta(common.Card c) {
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

        // 특수(보너스 카드)
        if (c instanceof common.CardBonus) return new CardMeta(0, "BONUS", "손패 1 드로우\n데미지 3, 방어 +3");

        return new CardMeta(999, "?", "정의되지 않음");
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

        // 카드명
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Dialog", Font.BOLD, 14));
        String name = (card == null) ? "CARD" : card.getCardName();
        drawCentered(g2, name, 0, 20, getWidth(), 30);

        // 코스트
        g2.setFont(new Font("Dialog", Font.BOLD, 18));
        g2.drawString(String.valueOf(meta.cost), 12, 20);

        // 핵심 수치
        g2.setFont(new Font("Dialog", Font.BOLD, 14));
        drawCentered(g2, meta.topLine, 0, 55, getWidth(), 20);

        // 효과
        g2.setFont(new Font("Dialog", Font.PLAIN, 12));
        drawMultiline(g2, meta.effectLine, 12, getHeight() - 55, getWidth() - 24, 14);

        g2.dispose();
        super.paintComponent(g);
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
