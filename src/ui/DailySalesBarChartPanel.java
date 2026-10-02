package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;

public class DailySalesBarChartPanel extends JPanel {
    private Map<String, Double> chartData;
    private String chartTitle = "📈 Revenue Trend";
    private int hoveredIndex = -1;

    public DailySalesBarChartPanel() {
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

    public void setData(Map<String, Double> data) {
        setData(data, "📈 Daily Revenue Trend (Last 7 Days)");
    }

    private void handleMouseMove(int mx, int my) {
        if (chartData == null || chartData.isEmpty()) return;

        int w = getWidth();
        int h = getHeight();
        int leftPad = 55;
        int rightPad = 25;
        int topPad = 45;
        int bottomPad = 45;
        int chartW = w - leftPad - rightPad;
        int availH = h - topPad - bottomPad;
        int chartH = Math.min(300, availH);
        int startY = topPad + Math.max(0, (availH - chartH) / 2);

        int count = chartData.size();
        int slotW = chartW / Math.max(1, count);
        int barW = Math.max(14, Math.min(42, slotW - 12));

        int newHover = -1;
        int idx = 0;
        for (double val : chartData.values()) {
            int x = leftPad + (idx * slotW) + (slotW - barW) / 2;
            if (mx >= x && mx <= x + barW && my >= startY && my <= startY + chartH + 20) {
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
            g2.drawString("No revenue data available for this range", 40, h / 2);
            g2.dispose();
            return;
        }

        double maxVal = 100.0;
        double totalRev = 0.0;
        for (double v : chartData.values()) {
            if (v > maxVal) maxVal = v;
            totalRev += v;
        }

        // Subtitle Total
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(100, 116, 139));
        String sub = String.format("Total: ₹%.2f", totalRev);
        int subW = g2.getFontMetrics().stringWidth(sub);
        g2.drawString(sub, w - subW - 16, 22);

        int leftPad = 55;
        int rightPad = 25;
        int topPad = 45;
        int bottomPad = 45;
        int chartW = w - leftPad - rightPad;
        int availH = h - topPad - bottomPad;
        int chartH = Math.min(300, availH);
        int startY = topPad + Math.max(0, (availH - chartH) / 2);

        // Draw horizontal gridlines & Y labels
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        int gridSteps = 4;
        for (int i = 0; i <= gridSteps; i++) {
            int y = startY + (chartH * i / gridSteps);
            g2.setColor(new Color(226, 232, 240));
            g2.drawLine(leftPad, y, leftPad + chartW, y);

            double lblVal = maxVal * (gridSteps - i) / gridSteps;
            g2.setColor(new Color(100, 116, 139));
            String lbl = String.format("₹%.0f", lblVal);
            g2.drawString(lbl, 8, y + 4);
        }

        // Draw Bars
        int count = chartData.size();
        int slotW = chartW / Math.max(1, count);
        int barW = Math.max(14, Math.min(42, slotW - 12));

        int idx = 0;
        int tooltipX = -1, tooltipY = -1;
        String tooltipText = null;

        for (Map.Entry<String, Double> entry : chartData.entrySet()) {
            double val = entry.getValue();
            int barHeight = (int) Math.round((val / maxVal) * chartH);
            int x = leftPad + (idx * slotW) + (slotW - barW) / 2;
            int y = startY + chartH - barHeight;

            boolean isHovered = (idx == hoveredIndex);

            // Bar Gradient
            Color startColor = isHovered ? new Color(99, 102, 241) : new Color(59, 130, 246);
            Color endColor = isHovered ? new Color(79, 70, 229) : new Color(37, 99, 235);
            GradientPaint gp = new GradientPaint(x, y, startColor, x, y + barHeight, endColor);
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(x, y, barW, Math.max(4, barHeight), 6, 6));

            // Top Value Label
            if (val > 0) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, isHovered ? 11 : 10));
                g2.setColor(isHovered ? new Color(30, 41, 59) : new Color(71, 85, 105));
                String vStr = String.format("₹%.0f", val);
                int strW = g2.getFontMetrics().stringWidth(vStr);
                g2.drawString(vStr, x + (barW - strW) / 2, Math.max(startY - 5, y - 5));
            }

            // X-axis label
            g2.setFont(new Font("Segoe UI", isHovered ? Font.BOLD : Font.PLAIN, 10));
            g2.setColor(isHovered ? new Color(30, 41, 59) : new Color(100, 116, 139));
            String dateLabel = entry.getKey();
            int dateW = g2.getFontMetrics().stringWidth(dateLabel);
            g2.drawString(dateLabel, x + (barW - dateW) / 2, startY + chartH + 18);

            if (isHovered) {
                tooltipX = x + barW / 2;
                tooltipY = y - 10;
                tooltipText = String.format("%s: ₹%.2f", entry.getKey(), val);
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
            int ty = Math.max(26, tooltipY - tH);

            g2.setColor(new Color(15, 23, 42, 230));
            g2.fillRoundRect(tx, ty, tW, tH, 6, 6);
            g2.setColor(Color.WHITE);
            g2.drawString(tooltipText, tx + 7, ty + 15);
        }

        g2.dispose();
    }
}
