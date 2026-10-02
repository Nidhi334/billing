package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

public class CategoryPieChartPanel extends JPanel {
    private Map<String, Double> chartData;
    private String chartTitle = "🍰 Revenue Breakdown";
    private int hoveredSlice = -1;

    private static final Color[] SLICE_COLORS = {
            new Color(59, 130, 246),
            new Color(16, 185, 129),
            new Color(245, 158, 11),
            new Color(239, 68, 68),
            new Color(139, 92, 246),
            new Color(236, 72, 153),
            new Color(14, 165, 233),
            new Color(100, 116, 139)
    };

    public CategoryPieChartPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(420, 260));
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
                if (hoveredSlice != -1) {
                    hoveredSlice = -1;
                    repaint();
                }
            }
        });
    }

    public void setData(Map<String, Double> data, String title) {
        this.chartData = data;
        this.chartTitle = title;
        this.hoveredSlice = -1;
        repaint();
    }

    public void setData(Map<String, Double> data) {
        setData(data, "🍰 Category-Wise Revenue Share");
    }

    private void handleMouseMove(int mx, int my) {
        if (chartData == null || chartData.isEmpty()) return;

        int w = getWidth();
        int h = getHeight();
        int contentTop = 38;
        int contentH = h - contentTop - 12;
        int contentCenterY = contentTop + contentH / 2;
        int pieDiameter = Math.min(210, Math.min(Math.max(120, (int)(w * 0.42)), contentH - 20));
        int pieX = 20;
        int pieY = contentCenterY - pieDiameter / 2;
        int centerX = pieX + pieDiameter / 2;
        int centerY = pieY + pieDiameter / 2;

        double dist = Math.sqrt(Math.pow(mx - centerX, 2) + Math.pow(my - centerY, 2));
        int outerR = pieDiameter / 2;
        int innerR = (int) (pieDiameter * 0.52 / 2);

        if (dist >= innerR && dist <= outerR) {
            double angle = Math.toDegrees(Math.atan2(-(my - centerY), mx - centerX));
            if (angle < 0) angle += 360;

            double total = 0;
            for (double v : chartData.values()) total += v;

            if (total > 0) {
                double curAngle = 0;
                int idx = 0;
                int found = -1;
                for (double v : chartData.values()) {
                    if (v <= 0) { idx++; continue; }
                    double arc = (v / total) * 360.0;
                    if (angle >= curAngle && angle < curAngle + arc) {
                        found = idx;
                        break;
                    }
                    curAngle += arc;
                    idx++;
                }
                if (found != hoveredSlice) {
                    hoveredSlice = found;
                    repaint();
                    return;
                }
            }
        } else {
            if (hoveredSlice != -1) {
                hoveredSlice = -1;
                repaint();
            }
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
            g2.drawString("No revenue recorded for breakdown", 30, h / 2);
            g2.dispose();
            return;
        }

        double total = 0.0;
        int activeCount = 0;
        for (double v : chartData.values()) {
            if (v > 0) {
                total += v;
                activeCount++;
            }
        }

        if (total <= 0) {
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            g2.setColor(new Color(148, 163, 184));
            g2.drawString("Zero revenue recorded", 30, h / 2);
            g2.dispose();
            return;
        }

        int contentTop = 38;
        int contentH = h - contentTop - 12;
        int contentCenterY = contentTop + contentH / 2;
        int pieDiameter = Math.min(210, Math.min(Math.max(120, (int)(w * 0.42)), contentH - 20));
        int pieX = 20;
        int pieY = contentCenterY - pieDiameter / 2;

        double curAngle = 0;
        int colorIdx = 0;

        int itemH = 26;
        int totalLegendH = Math.max(activeCount * itemH, 40);
        int legendY = contentCenterY - totalLegendH / 2;
        int legendX = pieX + pieDiameter + 20;

        String hoveredText = null;

        for (Map.Entry<String, Double> entry : chartData.entrySet()) {
            double val = entry.getValue();
            if (val <= 0) {
                colorIdx++;
                continue;
            }
            double fraction = val / total;
            int arcAngle = (int) Math.round(fraction * 360.0);

            Color color = SLICE_COLORS[colorIdx % SLICE_COLORS.length];
            boolean isHover = (colorIdx == hoveredSlice);

            if (isHover) {
                hoveredText = String.format("%s: ₹%.2f (%.1f%%)", entry.getKey(), val, fraction * 100);
                g2.setColor(color.darker());
                g2.fillArc(pieX - 2, pieY - 2, pieDiameter + 4, pieDiameter + 4, (int) Math.round(curAngle), arcAngle);
            } else {
                g2.setColor(color);
                g2.fillArc(pieX, pieY, pieDiameter, pieDiameter, (int) Math.round(curAngle), arcAngle);
            }

            curAngle += arcAngle;

            // Draw Legend
            if (legendY + 16 < h && legendX < w - 40) {
                g2.setColor(color);
                g2.fillRoundRect(legendX, legendY + 2, 12, 12, 3, 3);

                g2.setColor(isHover ? new Color(15, 23, 42) : new Color(51, 65, 85));
                g2.setFont(new Font("Segoe UI", isHover ? Font.BOLD : Font.PLAIN, 11));
                String name = entry.getKey();
                int maxChars = Math.max(8, (w - legendX - 50) / 7);
                if (name.length() > maxChars) name = name.substring(0, Math.max(6, maxChars - 2)) + "..";
                String text = String.format("%s (%.0f%%)", name, fraction * 100);
                g2.drawString(text, legendX + 20, legendY + 12);
                legendY += itemH;
            }

            colorIdx++;
        }

        // Sleek Donut Hole
        int holeDiameter = (int) (pieDiameter * 0.52);
        int holeX = pieX + (pieDiameter - holeDiameter) / 2;
        int holeY = pieY + (pieDiameter - holeDiameter) / 2;
        g2.setColor(Color.WHITE);
        g2.fillOval(holeX, holeY, holeDiameter, holeDiameter);

        // Center info in Donut Hole
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.setColor(new Color(100, 116, 139));
        String centerLbl = "TOTAL";
        int cLW = g2.getFontMetrics().stringWidth(centerLbl);
        g2.drawString(centerLbl, holeX + (holeDiameter - cLW) / 2, holeY + holeDiameter / 2 - 4);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.setColor(new Color(30, 41, 59));
        String cVal = String.format("₹%.0f", total);
        int cVW = g2.getFontMetrics().stringWidth(cVal);
        g2.drawString(cVal, holeX + (holeDiameter - cVW) / 2, holeY + holeDiameter / 2 + 12);

        // Tooltip if hovered
        if (hoveredText != null) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int tW = fm.stringWidth(hoveredText) + 14;
            int tH = 22;
            int tx = Math.max(10, Math.min(pieX + pieDiameter / 2 - tW / 2, w - tW - 10));
            int ty = Math.max(26, pieY - 6);

            g2.setColor(new Color(15, 23, 42, 235));
            g2.fillRoundRect(tx, ty, tW, tH, 6, 6);
            g2.setColor(Color.WHITE);
            g2.drawString(hoveredText, tx + 7, ty + 15);
        }

        g2.dispose();
    }
}
