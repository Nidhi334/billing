package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;

public class HorizontalBarChartPanel extends JPanel {
    private Map<String, Double> chartData;
    private String chartTitle = "Top Selling Products";
    private final SidebarIcon titleIcon = new SidebarIcon("trophy", 16, AppTheme.FOREST_GREEN);
    private int hoveredIndex = -1;

    public HorizontalBarChartPanel() {
        setBackground(Color.WHITE);
        setOpaque(false);
        setPreferredSize(new Dimension(450, 260));
        setMinimumSize(new Dimension(150, 180));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

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
        this.chartTitle = (title != null) ? title.replaceFirst("^[\\p{So}\\p{Cn}\\s]+", "") : "Top Selling Products";
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
        int slotH = chartH / Math.max(1, count);

        int newHover = -1;
        for (int idx = 0; idx < count; idx++) {
            int y = topPad + (idx * slotH);
            if (my >= y && my <= y + slotH) {
                newHover = idx;
                break;
            }
        }

        if (newHover != hoveredIndex) {
            hoveredIndex = newHover;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int r = 36; // Extra rounded modern widget silhouette

        // Subtle ambient drop shadow
        g2.setColor(new Color(0, 50, 30, 6));
        g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);

        // Pure crisp white card surface, NO BORDER!
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);

        // Chart Icon & Title
        titleIcon.paintIcon(this, g2, 22, 14);
        g2.setFont(AppTheme.font(Font.BOLD, 13));
        g2.setColor(AppTheme.TEXT_PRIMARY);
        g2.drawString(chartTitle, 44, 26);

        if (chartData == null || chartData.isEmpty()) {
            g2.setFont(AppTheme.font(Font.ITALIC, 12));
            g2.setColor(AppTheme.TEXT_MUTED);
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
        g2.setFont(AppTheme.font(Font.PLAIN, 11));
        g2.setColor(AppTheme.TEXT_MUTED);
        String sub = String.format("Total Units: %,d", (long) totalUnits);
        int subW = g2.getFontMetrics().stringWidth(sub);
        g2.drawString(sub, w - subW - 22, 26);

        int leftPad = getLeftPadding(w);
        int rightPad = getRightPadding(w);
        int topPad = 42;
        int bottomPad = 20;
        int chartW = Math.max(1, w - leftPad - rightPad);
        int chartH = h - topPad - bottomPad;

        int count = chartData.size();
        int slotH = chartH / Math.max(1, count);
        int barH = Math.max(14, Math.min(26, slotH - 8));

        int idx = 0;
        String tooltipText = null;
        int tooltipX = 0, tooltipY = 0;

        for (Map.Entry<String, Double> entry : chartData.entrySet()) {
            double val = entry.getValue();
            int barWidth = (int) Math.round((val / maxVal) * chartW);
            int y = topPad + (idx * slotH) + (slotH - barH) / 2;
            int x = leftPad;

            boolean isHovered = (idx == hoveredIndex);

            // Label (Product Name)
            g2.setFont(new Font("Segoe UI", isHovered ? Font.BOLD : Font.PLAIN, 11));
            g2.setColor(isHovered ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY);
            String name = fitLabel(g2.getFontMetrics(), entry.getKey(), leftPad - 16);
            int nameW = g2.getFontMetrics().stringWidth(name);
            g2.drawString(name, leftPad - nameW - 10, y + barH - 4);

            // Bar background track
            g2.setColor(AppTheme.BG_CANVAS);
            g2.fill(new RoundRectangle2D.Float(x, y, chartW, barH, 6, 6));

            // Bar Gradient
            Color startColor = isHovered ? AppTheme.FOREST_MID : AppTheme.FOREST_MID;
            Color endColor = isHovered ? AppTheme.ELECTRIC_LIME : AppTheme.FOREST_GREEN;
            GradientPaint gp = new GradientPaint(x, y, startColor, x + barWidth, y, endColor);
            g2.setPaint(gp);
            g2.fill(new RoundRectangle2D.Float(x, y, Math.max(6, barWidth), barH, 6, 6));

            // Value text on right (guaranteed not clipped)
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.setColor(isHovered ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_MUTED);
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

    private int getLeftPadding(int width) {
        return Math.max(52, Math.min(140, width / 4));
    }

    private int getRightPadding(int width) {
        return Math.max(54, Math.min(90, width / 5));
    }

    private String fitLabel(FontMetrics metrics, String label, int maxWidth) {
        if (metrics.stringWidth(label) <= maxWidth) return label;

        String suffix = "..";
        int end = label.length();
        while (end > 0 && metrics.stringWidth(label.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return end > 0 ? label.substring(0, end) + suffix : "";
    }
}
