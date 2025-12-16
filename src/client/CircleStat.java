package client;

import javax.swing.*;
import java.awt.*;

public class CircleStat extends JComponent {

    private String title = "";
    private String value = "0";

    public CircleStat(String title) {
        this.title = title;
        setPreferredSize(new Dimension(70, 70));
        setMinimumSize(new Dimension(70, 70));
    }

    public void setValue(int v) {
        value = String.valueOf(v);
        repaint();
    }

    //외부 참조
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 원 크기(타이틀 공간 고려)
        int titleH = 16;
        int circleSize = Math.min(w, h - titleH) - 6;
        if (circleSize < 10) return;

        int cx = (w - circleSize) / 2;
        int cy = titleH + (h - titleH - circleSize) / 2;

        // 테두리 원
        g2.setColor(Color.BLACK);
        g2.drawOval(cx, cy, circleSize, circleSize);

        // 타이틀
        g2.setFont(new Font("Dialog", Font.BOLD, 12));
        FontMetrics fmT = g2.getFontMetrics();
        int tx = (w - fmT.stringWidth(title)) / 2;
        g2.drawString(title, tx, 12);

        // 값
        g2.setFont(new Font("Dialog", Font.BOLD, 18));
        FontMetrics fmV = g2.getFontMetrics();
        int vx = (w - fmV.stringWidth(value)) / 2;
        int vy = cy + circleSize / 2 + fmV.getAscent() / 2 - 2;
        g2.drawString(value, vx, vy);
    }
}
