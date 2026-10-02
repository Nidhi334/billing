package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;

public class HorizontalBarChartPanel extends JPanel {
    private Map<String, Double> chartData;
    private String chartTitle = "🏆 Top Selling Products";
    private int hoveredIndex = -1;

    public HorizontalBarChartPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(450, 260));
        setMinimumSize(new Dimension(150, 180));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMove(e.getX(), e.getY());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredIndex != -1) {
                    hoveredIndex = -1;
                    repaint();
                }
            }
        });
    }

    public void setData(Map<String, Double> data, String title) {
        this.chartData = data;
        this.chartTitle = title;
        this.hoveredIndex = -1;
        repaint();
    }

    private void handleMouseMove(int mx, int my) {
        if (chartData == null || chartData.isEmpty()) return;

        int h = getHeight();
        int topPad = 42;
        int bottomPad = 20;
        int chartH = h - topPad - bottomPad;
        int count = chartData.size();
        int slotH = Math.min(46, Math.max(26, chartH / Math.max(1, count)));
        int totalBarsH = count * slotH;
        int startY = topPad + Math.max(0, (chartH - totalBarsH) / 2);

        int newHover = -1;
        int idx = 0;
        for (double val : chartData.values()) {
            int y = startY + (idx * slotH);
            if (my >= y && my <= y + slotH) {
                newHover = idx;
                break;
            }
            idx++;
        }

        if (newHover != hoveredIndex) {
            hoveredIndex = newHover;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Chart Title
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(new Color(30, 41, 59));
        g2.drawString(chartTitle, 16, 22);

        if (chartData == null || chartData.isEmpty()) {
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            g2.setColor(new Color(148, 163, 184));
            g2.drawString("No product sales data recorded yet", 40, h / 2);
            g2.dispose();
            return;
        }

        double maxVal = 1.0;
        double totalUnits = 0.0;
        for (double v : chartData.values()) {
            if (v > maxVal) maxVal = v;
            totalUnits += v;
        }

        // Subtitle Total
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(100, 116, 139));
        String sub = String.format("Total Units: %.0f", totalUnits);
        int subW = g2.getFontMetrics().stringWidth(sub);
        g2.drawString(sub, w - subW - 16, 22);

        int leftPad = Math.max(90, Math.min(140, w / 4));
        int rightPad = Math.max(65, Math.min(90, w / 5));
        int topPad = 42;
        int bottomPad = 20;
        int chartW = Math.max(40, w - leftPad - rightPad);
        int chartH = h - topPad - bottomPad;

        int count = chartData.size();
        int slotH = Math.min(46, Math.max(26, chartH / Math.max(1, count)));
        int barH = Math.min(22, Math.max(14, slotH - 14));
        int totalBarsH = count * slotH;
        int startY = topPad + Math.max(0, (chartH - totalBarsH) / 2);

        int idx = 0;
        String tooltipText = null;
        int tooltipX = 0, tooltipY = 0;

        for (Map.Entry<String, Double> entry : chartData.entrySet()) {
            double val = entry.getValue();
            int barWidth = (int) Math.round((val / maxVal) * chartW);
            int y = startY + (idx * slotH) + (slotH - barH) / 2;
            int x = leftPad;

            boolean isHovered = (idx == hoveredIndex);

            // Label (Product Name)
            g2.setFont(new Font("Segoe UI", isHovered ? Font.BOLD : Font.PLAIN, 11));
            g2.setColor(isHovered ? new Color(30, 41, 59) : new Color(71, 85, 105));
            String name = entry.getKey();
            int maxChars = Math.max(8, (leftPad - 16) / 7);
            if (name.length() > maxChars) name = name.substring(0, Math.max(6, maxChars - 2)) + "..";
            int nameW = g2.getFontMetrics().stringWidth(name);
            g2.drawString(name, leftPad - nameW - 10, y + barH - 4);

            // Bar background track
            g2.setColor(new Color(241, 245, 249));
            g2.fill(new RoundRectangle2D.Float(x, y, chartW, barH, 6, 6));

            // Bar Gradient
            Color startColor = isHovered ? new Color(16, 185, 129) : new Color(13, 148, 136);
            Color endColor = isHovered ? new Color(5, 150, 105) : new Color(15, 118, 110);
            GradientPaint gp = new GradientPaint(x, y, startColor, x + barWidth, y, endColor);
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(x, y, Math.max(6, barWidth), barH, 6, 6));

            // Value text on right (guaranteed not clipped)
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(isHovered ? new Color(15, 23, 42) : new Color(100, 116, 139));
            String vStr = String.format("%.0f units", val);
            int vStrW = g2.getFontMetrics().stringWidth(vStr);
            int vx = x + barWidth + 8;
            if (vx + vStrW > w - 8) {
                vx = Math.max(x + 6, w - vStrW - 10);
                if (barWidth > vStrW + 16) {
                    g2.setColor(Color.WHITE);
                }
            }
            g2.drawString(vStr, vx, y + barH - 4);

            if (isHovered) {
                tooltipX = x + barWidth / 2;
                tooltipY = y;
                tooltipText = String.format("%s: %.0f units sold", entry.getKey(), val);
            }

            idx++;
        }

        // Draw Interactive Tooltip on hover
        if (tooltipText != null) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int tW = fm.stringWidth(tooltipText) + 14;
            int tH = 22;
            int tx = Math.max(leftPad, Math.min(tooltipX - tW / 2, w - tW - 10));
            int ty = Math.max(26, tooltipY - tH - 4);

            g2.setColor(new Color(15, 23, 42, 230));
            g2.fillRoundRect(tx, ty, tW, tH, 6, 6);
            g2.setColor(Color.WHITE);
            g2.drawString(tooltipText, tx + 7, ty + 15);
        }

        g2.dispose();
    }
}
