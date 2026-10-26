package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * StockDistributionChartPanel - A stacked horizontal bar chart showing stock distribution by category.
 * Inspired by modern analytics dashboards with smooth tooltips and high roundness.
 */
public class StockDistributionChartPanel extends JPanel {
    private Map<String, Map<String, Double>> chartData;
    private String chartTitle = "Stock Distribution by Category";
    private final SidebarIcon titleIcon = new SidebarIcon("boxes", 16, AppTheme.FOREST_GREEN);
    
    // Interactive State
    private int hoveredRow = -1;
    private int hoveredSegmentIdx = -1;
    private Point mousePoint = new Point(0, 0);

    public StockDistributionChartPanel() {
        setBackground(Color.WHITE);
        setOpaque(false);
        setPreferredSize(new Dimension(500, 300));
        setMinimumSize(new Dimension(200, 200));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePoint = e.getPoint();
                handleMouseMove(e.getX(), e.getY());
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                hoveredSegmentIdx = -1;
                repaint();
            }
        });
    }

    public void setData(Map<String, Map<String, Double>> data, String title) {
        this.chartData = data;
        if (title != null) {
            this.chartTitle = title.replaceFirst("^[\\p{So}\\p{Cn}\\s]+", "");
        }
        this.hoveredRow = -1;
        this.hoveredSegmentIdx = -1;
        repaint();
    }

    private void handleMouseMove(int mx, int my) {
        if (chartData == null || chartData.isEmpty()) return;

        int w = getWidth();
        int h = getHeight();
        int leftPad = 60;
        int rightPad = 30;
        int topPad = 50;
        int bottomPad = 25;
        
        int chartW = w - leftPad - rightPad;
        int chartH = h - topPad - bottomPad;
        int rowCount = chartData.size();
        int slotH = chartH / Math.max(1, rowCount);
        int barH = Math.min(30, slotH - 10);

        int newHoverRow = -1;
        int newHoverSeg = -1;

        int rIdx = 0;
        for (Map.Entry<String, Map<String, Double>> rowEntry : chartData.entrySet()) {
            int rowY = topPad + (rIdx * slotH) + (slotH - barH) / 2;
            if (my >= rowY && my <= rowY + barH && mx >= leftPad && mx <= leftPad + chartW) {
                newHoverRow = rIdx;
                
                // Find which segment is hovered
                Map<String, Double> dist = rowEntry.getValue();
                double rowTotal = dist.values().stream().mapToDouble(Double::doubleValue).sum();
                if (rowTotal > 0) {
                    double currentX = leftPad;
                    int sIdx = 0;
                    for (Double val : dist.values()) {
                        double segW = (val / rowTotal) * chartW;
                        if (mx >= currentX && mx <= currentX + segW) {
                            newHoverSeg = sIdx;
                            break;
                        }
                        currentX += segW;
                        sIdx++;
                    }
                }
                break;
            }
            rIdx++;
        }

        if (newHoverRow != hoveredRow || newHoverSeg != hoveredSegmentIdx) {
            hoveredRow = newHoverRow;
            hoveredSegmentIdx = newHoverSeg;
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
        int r = 36;

        // Shadow & Card
        g2.setColor(new Color(0, 50, 30, 6));
        g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);

        // Header
        titleIcon.paintIcon(this, g2, 22, 14);
        g2.setFont(AppTheme.font(Font.BOLD, 13));
        g2.setColor(AppTheme.TEXT_PRIMARY);
        g2.drawString(chartTitle, 44, 26);

        if (chartData == null || chartData.isEmpty()) {
            g2.setFont(AppTheme.font(Font.ITALIC, 12));
            g2.setColor(AppTheme.TEXT_MUTED);
            g2.drawString("No stock distribution data available", 40, h / 2);
            g2.dispose();
            return;
        }

        int leftPad = 60;
        int rightPad = 30;
        int topPad = 50;
        int bottomPad = 25;
        int chartW = w - leftPad - rightPad;
        int chartH = h - topPad - bottomPad;

        int rowCount = chartData.size();
        int slotH = chartH / Math.max(1, rowCount);
        int barH = Math.min(30, slotH - 10);

        int rIdx = 0;
        String tooltipText = null;

        for (Map.Entry<String, Map<String, Double>> rowEntry : chartData.entrySet()) {
            String day = rowEntry.getKey();
            Map<String, Double> dist = rowEntry.getValue();
            double rowTotal = dist.values().stream().mapToDouble(Double::doubleValue).sum();
            
            int y = topPad + (rIdx * slotH) + (slotH - barH) / 2;
            
            // Day label
            g2.setFont(AppTheme.font(Font.PLAIN, 11));
            g2.setColor(AppTheme.TEXT_SECONDARY);
            g2.drawString(day, 22, y + barH/2 + 5);

            if (rowTotal > 0) {
                double currentX = leftPad;
                int sIdx = 0;
                List<String> keys = new ArrayList<>(dist.keySet());
                
                for (String cat : keys) {
                    double val = dist.get(cat);
                    double segW = (val / rowTotal) * chartW;
                    
                    boolean isHovered = (rIdx == hoveredRow && sIdx == hoveredSegmentIdx);
                    
                    // Segment color
                    Color baseColor = AppTheme.CHART_PALETTE[sIdx % AppTheme.CHART_PALETTE.length];
                    if (isHovered) {
                        g2.setColor(baseColor.brighter());
                        tooltipText = String.format("%s: %.0f units (%.1f%%)", cat, val, (val/rowTotal)*100);
                    } else {
                        g2.setColor(baseColor);
                    }
                    
                    // Draw segment with slight rounding for the ends
                    Shape segShape;
                    if (sIdx == 0 && sIdx == keys.size() - 1) {
                        segShape = new RoundRectangle2D.Double(currentX, y, segW, barH, 12, 12);
                    } else if (sIdx == 0) {
                        segShape = new RoundRectangle2D.Double(currentX, y, segW + 6, barH, 12, 12);
                        g2.setClip(new Rectangle((int)currentX, y, (int)segW, barH));
                    } else if (sIdx == keys.size() - 1) {
                        segShape = new RoundRectangle2D.Double(currentX - 6, y, segW + 6, barH, 12, 12);
                        g2.setClip(new Rectangle((int)currentX, y, (int)segW, barH));
                    } else {
                        segShape = new Rectangle((int)currentX, y, (int)segW, barH);
                    }
                    
                    g2.fill(segShape);
                    g2.setClip(null);
                    
                    // Category name and Percentage text
                    String label = String.format("%s %.0f%%", cat, (val / rowTotal) * 100);
                    int fontSize = 9;
                    g2.setFont(AppTheme.font(Font.BOLD, fontSize));
                    FontMetrics fm = g2.getFontMetrics();
                    
                    g2.setColor(new Color(10, 46, 33)); // Deep Forest Slate for text
                    String dispText = label;
                    
                    // Smart label fitting with dynamic font sizing
                    if (fm.stringWidth(label) > segW - 4) {
                        // Try smaller font
                        fontSize = 8;
                        g2.setFont(AppTheme.font(Font.BOLD, fontSize));
                        fm = g2.getFontMetrics();
                        
                        if (fm.stringWidth(label) > segW - 4) {
                            dispText = cat;
                            if (fm.stringWidth(dispText) > segW - 4) {
                                dispText = String.format("%.0f%%", (val / rowTotal) * 100);
                                if (fm.stringWidth(dispText) > segW - 4) {
                                    fontSize = 7;
                                    g2.setFont(AppTheme.font(Font.BOLD, fontSize));
                                    fm = g2.getFontMetrics();
                                    if (fm.stringWidth(dispText) > segW - 2) {
                                        dispText = cat.substring(0, 1);
                                    }
                                }
                            }
                        }
                    }
                    
                    if (segW > 4) { // Only draw if there's some space
                        int tx = (int) (currentX + (segW - fm.stringWidth(dispText)) / 2);
                        int ty = y + barH / 2 + 4;
                        g2.drawString(dispText, tx, ty);
                    }
                    
                    currentX += segW;
                    sIdx++;
                }
            } else {
                // Empty track
                g2.setColor(AppTheme.BG_CANVAS);
                g2.fill(new RoundRectangle2D.Float(leftPad, y, chartW, barH, 12, 12));
            }
            
            rIdx++;
        }

        // Draw Interactive Tooltip
        if (tooltipText != null) {
            g2.setFont(AppTheme.font(Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int tW = fm.stringWidth(tooltipText) + 16;
            int tH = 26;
            int tx = Math.max(10, Math.min(mousePoint.x - tW / 2, w - tW - 10));
            int ty = Math.max(10, mousePoint.y - tH - 10);

            g2.setColor(new Color(15, 23, 42, 240));
            g2.fillRoundRect(tx, ty, tW, tH, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawString(tooltipText, tx + 8, ty + 17);
        }

        g2.dispose();
    }
}
