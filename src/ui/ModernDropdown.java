package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ModernDropdown - Elegant, accessible filter dropdown component for Keyra-themed analytics.
 * Replaces legacy OS-dependent JComboBox with a crisp, rounded pill trigger and smooth popup menu.
 */
public class ModernDropdown extends JButton {

    private final String categoryPrefix;
    private final String iconName;
    private final List<String> items;
    private int selectedIndex = 0;
    private final List<ActionListener> selectionListeners = new ArrayList<>();
    private boolean hovered = false;
    private JPopupMenu popup;

    public ModernDropdown(String categoryPrefix, String iconName, String[] options) {
        this.categoryPrefix = categoryPrefix;
        this.iconName = iconName;
        this.items = new ArrayList<>(Arrays.asList(options));

        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        int maxLen = 0;
        for (String s : options) {
            maxLen = Math.max(maxLen, s.length());
        }
        int btnWidth = Math.max(220, 85 + (int)(maxLen * 6.5f));
        setPreferredSize(new Dimension(btnWidth, 34));
        setMinimumSize(new Dimension(170, 34));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
        });

        addActionListener(e -> showPopup());
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int idx) {
        if (idx >= 0 && idx < items.size()) {
            this.selectedIndex = idx;
            repaint();
        }
    }

    public String getSelectedItem() {
        return (selectedIndex >= 0 && selectedIndex < items.size()) ? items.get(selectedIndex) : null;
    }

    @Override
    public void addActionListener(ActionListener l) {
        super.addActionListener(l);
        selectionListeners.add(l);
    }

    private void fireSelectionChanged() {
        ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getSelectedItem());
        for (ActionListener l : selectionListeners) {
            l.actionPerformed(ev);
        }
    }

    private void showPopup() {
        popup = new JPopupMenu();
        popup.setBackground(Color.WHITE);
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER_SAGE, 1),
                new EmptyBorder(5, 5, 5, 5)
        ));

        int menuWidth = Math.max(getWidth(), 230);

        for (int i = 0; i < items.size(); i++) {
            final int idx = i;
            final String text = items.get(i);
            final boolean isSelected = (i == selectedIndex);

            JMenuItem item = new JMenuItem(text) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    int arc = 8;

                    if (isSelected) {
                        g2.setColor(new Color(236, 252, 240)); // Fresh soft mint
                        g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);
                    } else if (getModel().isArmed() || getModel().isRollover()) {
                        g2.setColor(new Color(0, 90, 61, 14)); // Translucent forest hover
                        g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);
                    }

                    // Item text
                    g2.setFont(AppTheme.font(isSelected ? Font.BOLD : Font.PLAIN, 12));
                    g2.setColor(isSelected ? AppTheme.FOREST_DEEP : AppTheme.TEXT_PRIMARY);
                    FontMetrics fm = g2.getFontMetrics();
                    int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(text, 12, ty);

                    // Checkmark for selected item
                    if (isSelected) {
                        g2.setColor(AppTheme.FOREST_GREEN);
                        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        Path2D chk = new Path2D.Float();
                        int cx = w - 20;
                        int cy = h / 2 - 1;
                        chk.moveTo(cx, cy);
                        chk.lineTo(cx + 3.5f, cy + 3.5f);
                        chk.lineTo(cx + 8.5f, cy - 3.5f);
                        g2.draw(chk);
                    }
                    g2.dispose();
                }
            };
            item.setOpaque(false);
            item.setContentAreaFilled(false);
            item.setBorderPainted(false);
            item.setPreferredSize(new Dimension(menuWidth, 32));
            item.setCursor(new Cursor(Cursor.HAND_CURSOR));
            item.addActionListener(ev -> {
                setSelectedIndex(idx);
                fireSelectionChanged();
            });
            popup.add(item);
        }

        popup.show(this, 0, getHeight() + 4);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int arc = 12;

        // Ambient drop shadow
        g2.setColor(new Color(0, 50, 30, hovered ? 10 : 5));
        g2.fillRoundRect(1, 2, w - 2, h - 2, arc, arc);

        // Button background
        g2.setColor(hovered ? new Color(250, 254, 251) : Color.WHITE);
        g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

        // Delicate sage border
        g2.setColor(hovered ? AppTheme.FOREST_MID : AppTheme.BORDER_SAGE);
        g2.setStroke(new BasicStroke(hovered ? 1.3f : 1.0f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

        // Left vector icon
        int iconX = 10;
        int iconY = (h - 15) / 2;
        SidebarIcon icon = new SidebarIcon(iconName, 15, AppTheme.FOREST_GREEN);
        icon.paintIcon(this, g2, iconX, iconY);

        // Category Prefix (e.g. "Timeline:")
        g2.setFont(AppTheme.font(Font.BOLD, 11));
        g2.setColor(AppTheme.TEXT_MUTED);
        FontMetrics fmCat = g2.getFontMetrics();
        String cat = categoryPrefix + ":";
        int catX = iconX + 20;
        int ty = (h + fmCat.getAscent() - fmCat.getDescent()) / 2;
        g2.drawString(cat, catX, ty);

        // Selected Value
        String val = getSelectedItem();
        if (val != null) {
            g2.setFont(AppTheme.font(Font.BOLD, 12));
            g2.setColor(AppTheme.TEXT_PRIMARY);
            int valX = catX + fmCat.stringWidth(cat) + 6;
            Shape origClip = g2.getClip();
            g2.setClip(valX, 0, w - valX - 22, h);
            g2.drawString(val, valX, ty);
            g2.setClip(origClip);
        }

        // Downward vector chevron
        g2.setColor(hovered ? AppTheme.FOREST_DEEP : AppTheme.TEXT_SECONDARY);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D chev = new Path2D.Float();
        int cx = w - 15;
        int cy = h / 2 - 1;
        chev.moveTo(cx - 3.5f, cy - 2);
        chev.lineTo(cx, cy + 2);
        chev.lineTo(cx + 3.5f, cy - 2);
        g2.draw(chev);

        g2.dispose();
    }
}
