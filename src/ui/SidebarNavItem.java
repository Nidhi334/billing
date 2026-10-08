package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom Pill-shaped Navigation Button for the Modern Dashboard Sidebar.
 * Supports:
 * - Left-aligned text in expanded mode
 * - Icon-only centered view in collapsed mode with tooltips
 * - Stadium pill shape with Dark Green (#114a2a) active highlight & white text/icon
 * - Smooth hover pill highlight on inactive state
 */
public class SidebarNavItem extends JButton {

    public static final Color COLOR_ACTIVE_BG    = AppTheme.FOREST_GREEN;       // #005a3d Deep Forest Green
    public static final Color COLOR_ACTIVE_TEXT  = AppTheme.TEXT_WHITE;
    public static final Color COLOR_INACTIVE_TEXT= AppTheme.TEXT_SECONDARY;     // #335e4e Pine Slate
    public static final Color COLOR_HOVER_BG     = AppTheme.HOVER_SURFACE;      // subtle translucent green
    public static final Color COLOR_HOVER_TEXT   = AppTheme.TEXT_PRIMARY;       // #0a2e21 High contrast slate

    private final String title;
    private final SidebarIcon vectorIcon;
    private boolean active = false;
    private boolean hovered = false;
    private boolean collapsed = false;

    public SidebarNavItem(String title, String iconName) {
        super(title);
        this.title = title;
        this.vectorIcon = new SidebarIcon(iconName, 18);
        setIcon(vectorIcon);

        setFont(AppTheme.font(Font.BOLD, 13));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setIconTextGap(10);

        updateLayoutMode();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    public String getTitle() {
        return title;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        updateColors();
        repaint();
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        updateLayoutMode();
        revalidate();
        repaint();
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    private void updateLayoutMode() {
        if (collapsed) {
            setText("");
            setHorizontalAlignment(SwingConstants.CENTER);
            setHorizontalTextPosition(SwingConstants.CENTER);
            setBorder(new EmptyBorder(0, 0, 0, 0));
            setPreferredSize(new Dimension(44, 42));
            setMaximumSize(new Dimension(44, 42));
            setMinimumSize(new Dimension(44, 42));
            setToolTipText(title);
        } else {
            setText(title);
            setHorizontalAlignment(SwingConstants.LEFT);
            setHorizontalTextPosition(SwingConstants.RIGHT);
            setBorder(new EmptyBorder(0, 16, 0, 14));
            setPreferredSize(new Dimension(204, 42));
            setMaximumSize(new Dimension(204, 42));
            setMinimumSize(new Dimension(204, 42));
            setToolTipText(null);
        }
        updateColors();
    }

    private void updateColors() {
        if (active) {
            setForeground(COLOR_ACTIVE_TEXT);
            vectorIcon.setOverrideColor(COLOR_ACTIVE_TEXT);
        } else if (hovered) {
            setForeground(COLOR_HOVER_TEXT);
            vectorIcon.setOverrideColor(COLOR_HOVER_TEXT);
        } else {
            setForeground(COLOR_INACTIVE_TEXT);
            vectorIcon.setOverrideColor(COLOR_INACTIVE_TEXT);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();
        float arc = h; // stadium pill shape

        if (active) {
            // Dark green stadium pill
            g2.setColor(COLOR_ACTIVE_BG);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));

            // Keyra Electric Lime Accent pill on active item in expanded mode
            if (!collapsed && w > 100) {
                g2.setColor(AppTheme.ELECTRIC_LIME);
                g2.fillRoundRect(w - 16, (h - 12) / 2, 4, 12, 4, 4);
            }

            // Subtle bottom glow/shadow on active pill
            g2.setColor(new Color(0, 0, 0, 20));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, arc, arc));
        } else if (hovered) {
            // Translucent soft green hover pill
            g2.setColor(COLOR_HOVER_BG);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));
        }

        g2.dispose();
        updateColors();
        super.paintComponent(g);
    }
}
