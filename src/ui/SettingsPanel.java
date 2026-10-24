package ui;

import config.AppSettings;
import config.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * SettingsPanel - Streamlined, uncluttered system configuration panel.
 * Matches Dashboard aesthetics with compact borderless cards (r=20),
 * vector icons, no redundant descriptions, and prominent pill buttons.
 */
public class SettingsPanel extends JPanel {
    private final Runnable onSettingsChangedCallback;

    // Receipt & Printing Settings
    private CustomDropdown cmbReceiptSize;

    // Database Connection Credentials
    private ModernInputField txtDbHost;
    private ModernInputField txtDbPort;
    private ModernInputField txtDbName;
    private ModernInputField txtDbUser;
    private ModernPasswordField txtDbPass;
    private PillButton btnTestConn;

    // Store Details
    private ModernInputField txtStoreName;
    private ModernInputField txtStorePhone;
    private ModernInputField txtStoreGst;
    private ModernInputField txtStoreUpi;

    public SettingsPanel(Runnable onSettingsChangedCallback) {
        this.onSettingsChangedCallback = onSettingsChangedCallback;
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_CANVAS);
        initComponents();
        loadCurrentSettings();
    }

    private void initComponents() {
        // TOP HEADER (Clean title only, no icon and no subtitle)
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topPanel.setOpaque(false);
        topPanel.setBorder(new EmptyBorder(16, 32, 10, 32));

        JLabel lblTitle = new JLabel("System & Hardware Settings");
        lblTitle.setFont(AppTheme.font(Font.BOLD, 22));
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        topPanel.add(lblTitle);

        add(topPanel, BorderLayout.NORTH);

        // CENTER COLUMN (Centered max 720px width, natural compact heights)
        JPanel scrollContent = new JPanel(new GridBagLayout());
        scrollContent.setOpaque(false);

        JPanel centerColumn = new JPanel();
        centerColumn.setLayout(new BoxLayout(centerColumn, BoxLayout.Y_AXIS));
        centerColumn.setOpaque(false);

        // 1. RECEIPT & PRINTING CARD (Just title and custom dropdown below it)
        ModernSettingsCard printCard = new ModernSettingsCard();
        printCard.setHeader("printer", "Receipt & Printing Format");

        cmbReceiptSize = new CustomDropdown(new String[]{
                "2-Inch Thermal (58mm - Mini POS)",
                "3-Inch Thermal (80mm - Supermarket POS)",
                "Standard A4 / Full Page Invoice"
        });

        printCard.setContent(cmbReceiptSize);
        centerColumn.add(printCard);
        centerColumn.add(Box.createVerticalStrut(14));

        // 2. DATABASE CREDENTIALS CARD (Clean 2-column grid)
        ModernSettingsCard dbCard = new ModernSettingsCard();
        dbCard.setHeader("database", "Database Connection");

        JPanel dbBody = new JPanel(new GridLayout(3, 2, 14, 10));
        dbBody.setOpaque(false);

        txtDbHost = new ModernInputField("localhost");
        txtDbPort = new ModernInputField("3306");
        txtDbName = new ModernInputField("billing_system");
        txtDbUser = new ModernInputField("root");
        txtDbPass = new ModernPasswordField();

        btnTestConn = PillButton.secondary("Test Connection", new SidebarIcon("zap", 12, AppTheme.FOREST_GREEN));
        btnTestConn.setPreferredSize(new Dimension(0, 34));
        btnTestConn.addActionListener(e -> testDatabaseConnection());

        dbBody.add(createFieldGroup("Database Host", txtDbHost));
        dbBody.add(createFieldGroup("Port", txtDbPort));
        dbBody.add(createFieldGroup("Database Name", txtDbName));
        dbBody.add(createFieldGroup("Username", txtDbUser));
        dbBody.add(createFieldGroup("Password", txtDbPass));
        dbBody.add(createFieldGroup("Verify Setup", btnTestConn));

        dbCard.setContent(dbBody);
        centerColumn.add(dbCard);
        centerColumn.add(Box.createVerticalStrut(14));

        // 3. SHOP & INVOICE DETAILS CARD (Clean 2-column grid)
        ModernSettingsCard storeCard = new ModernSettingsCard();
        storeCard.setHeader("building", "Shop & Invoice Details");

        JPanel storeBody = new JPanel(new GridLayout(2, 2, 14, 10));
        storeBody.setOpaque(false);

        txtStoreName = new ModernInputField("SmartBilling Supermarket");
        txtStorePhone = new ModernInputField("9876543210");
        txtStoreGst = new ModernInputField("18.0");
        txtStoreUpi = new ModernInputField("bazaarpoint@upi");

        storeBody.add(createFieldGroup("Shop / Store Name", txtStoreName));
        storeBody.add(createFieldGroup("Phone / Support No", txtStorePhone));
        storeBody.add(createFieldGroup("Default GST Rate (%)", txtStoreGst));
        storeBody.add(createFieldGroup("Store UPI ID", txtStoreUpi));

        storeCard.setContent(storeBody);
        centerColumn.add(storeCard);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(4, 20, 20, 20);
        scrollContent.add(centerColumn, gbc);

        JScrollPane scrollPane = new JScrollPane(scrollContent);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // BOTTOM ACTION BAR
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        bottomBar.setOpaque(false);
        bottomBar.setBorder(new EmptyBorder(6, 32, 14, 32));

        PillButton btnResetDefaults = PillButton.subtle("Reset Defaults", new SidebarIcon("refresh", 13, AppTheme.TEXT_SECONDARY));
        btnResetDefaults.setPreferredSize(new Dimension(150, 40));
        btnResetDefaults.addActionListener(e -> resetDefaults());

        PillButton btnSave = PillButton.primary("Save & Apply All Settings", new SidebarIcon("check", 14, Color.WHITE));
        btnSave.setPreferredSize(new Dimension(230, 40));
        btnSave.addActionListener(e -> saveSettings());

        bottomBar.add(btnResetDefaults);
        bottomBar.add(btnSave);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel createFieldGroup(String labelText, JComponent input) {
        JPanel group = new JPanel(new BorderLayout(0, 4));
        group.setOpaque(false);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(AppTheme.font(Font.BOLD, 11));
        lbl.setForeground(AppTheme.TEXT_SECONDARY);
        group.add(lbl, BorderLayout.NORTH);
        group.add(input, BorderLayout.CENTER);
        return group;
    }

    public void loadCurrentSettings() {
        AppSettings.loadSettings();

        // Receipt Size
        String savedSize = AppSettings.getString(AppSettings.KEY_RECEIPT_PRINT_SIZE, "2_INCH");
        if ("3_INCH".equalsIgnoreCase(savedSize)) cmbReceiptSize.setSelectedIndex(1);
        else if ("A4".equalsIgnoreCase(savedSize)) cmbReceiptSize.setSelectedIndex(2);
        else cmbReceiptSize.setSelectedIndex(0);

        // DB Credentials
        Properties dbProps = DBConnection.getProperties();
        txtDbHost.setText(dbProps.getProperty("db.host", "localhost"));
        txtDbPort.setText(dbProps.getProperty("db.port", "3306"));
        txtDbName.setText(dbProps.getProperty("db.name", "billing_system"));
        txtDbUser.setText(dbProps.getProperty("db.user", "root"));
        txtDbPass.setText(dbProps.getProperty("db.password", ""));

        // Store
        txtStoreName.setText(AppSettings.getString(AppSettings.KEY_STORE_NAME, "SmartBilling Supermarket"));
        txtStorePhone.setText(AppSettings.getString(AppSettings.KEY_STORE_PHONE, "9876543210"));
        txtStoreGst.setText(AppSettings.getString(AppSettings.KEY_STORE_GST, "18.0"));
        txtStoreUpi.setText(AppSettings.getString(AppSettings.KEY_STORE_UPI_ID, "bazaarpoint@upi"));
    }

    private void saveSettings() {
        int szIdx = cmbReceiptSize.getSelectedIndex();
        String szCode = szIdx == 1 ? "3_INCH" : (szIdx == 2 ? "A4" : "2_INCH");
        AppSettings.setString(AppSettings.KEY_RECEIPT_PRINT_SIZE, szCode);

        AppSettings.setString(AppSettings.KEY_STORE_NAME, txtStoreName.getText().trim());
        AppSettings.setString(AppSettings.KEY_STORE_PHONE, txtStorePhone.getText().trim());
        AppSettings.setString(AppSettings.KEY_STORE_GST, txtStoreGst.getText().trim());
        AppSettings.setString(AppSettings.KEY_STORE_UPI_ID, txtStoreUpi.getText().trim());

        AppSettings.saveSettings();

        // Save DB Credentials securely to db_config.properties
        DBConnection.saveConfig(
                txtDbHost.getText().trim(),
                txtDbPort.getText().trim(),
                txtDbName.getText().trim(),
                txtDbUser.getText().trim(),
                new String(txtDbPass.getPassword())
        );

        JOptionPane.showMessageDialog(this,
                "Settings and Database Configuration saved successfully!\nCredentials and preferences have been updated.",
                "Settings Saved", JOptionPane.INFORMATION_MESSAGE);

        if (onSettingsChangedCallback != null) {
            onSettingsChangedCallback.run();
        }
    }

    private void testDatabaseConnection() {
        btnTestConn.setEnabled(false);
        try {
            java.sql.Connection conn = DBConnection.getConnection();
            if (conn != null && !conn.isClosed()) {
                JOptionPane.showMessageDialog(this,
                        "Database connection successful!\nConnected to: " + txtDbName.getText().trim(),
                        "Connection Test", JOptionPane.INFORMATION_MESSAGE);
                conn.close();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Connection failed!\nError: " + ex.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            btnTestConn.setEnabled(true);
        }
    }

    private void resetDefaults() {
        int opt = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reset printing and store options to default?",
                "Reset Confirmation", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            cmbReceiptSize.setSelectedIndex(0);
            txtStoreName.setText("SmartBilling Supermarket");
            txtStorePhone.setText("9876543210");
            txtStoreGst.setText("18.0");
            txtStoreUpi.setText("bazaarpoint@upi");

            saveSettings();
        }
    }

    // ── MODERN SETTINGS CARD (r=20, borderless, ambient shadow, 720px width) ──
    private static class ModernSettingsCard extends JPanel {
        public ModernSettingsCard() {
            setLayout(new BorderLayout(0, 14));
            setOpaque(false);
            setBorder(new EmptyBorder(16, 20, 18, 20));
        }

        public void setHeader(String iconName, String title) {
            JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            titleRow.setOpaque(false);

            JLabel icon = new JLabel(new SidebarIcon(iconName, 16, AppTheme.FOREST_GREEN));
            JLabel lblTitle = new JLabel(title);
            lblTitle.setFont(AppTheme.font(Font.BOLD, 14));
            lblTitle.setForeground(AppTheme.TEXT_PRIMARY);

            titleRow.add(icon);
            titleRow.add(lblTitle);
            add(titleRow, BorderLayout.NORTH);
        }

        public void setContent(JComponent comp) {
            add(comp, BorderLayout.CENTER);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(720, d.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int r = 20;

            // Ambient soft drop shadow
            g2.setColor(new Color(0, 50, 30, 6));
            g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);

            // Crisp white card surface - NO BORDER!
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── MODERN ROUNDED INPUT FIELD (r=10, soft sage stroke, forest focus) ────
    private static class ModernInputField extends JTextField {
        private boolean focused = false;

        public ModernInputField(String text) {
            super(text);
            setFont(AppTheme.font(Font.PLAIN, 12));
            setForeground(AppTheme.TEXT_PRIMARY);
            setCaretColor(AppTheme.FOREST_GREEN);
            setOpaque(false);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setPreferredSize(new Dimension(0, 34));

            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int r = 10;

            g2.setColor(new Color(250, 253, 251));
            g2.fillRoundRect(0, 0, w, h, r, r);

            if (focused) {
                g2.setColor(AppTheme.FOREST_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── MODERN ROUNDED PASSWORD FIELD (r=10) ──────────────────────────────────
    private static class ModernPasswordField extends JPasswordField {
        private boolean focused = false;

        public ModernPasswordField() {
            setFont(AppTheme.font(Font.PLAIN, 12));
            setForeground(AppTheme.TEXT_PRIMARY);
            setCaretColor(AppTheme.FOREST_GREEN);
            setOpaque(false);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setPreferredSize(new Dimension(0, 34));

            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int r = 10;

            g2.setColor(new Color(250, 253, 251));
            g2.fillRoundRect(0, 0, w, h, r, r);

            if (focused) {
                g2.setColor(AppTheme.FOREST_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── PILL BUTTON (High-contrast, prominent, stadium rounded arc=h) ─────────
    private static class PillButton extends JButton {
        private final Color bg;
        private final Color hoverBg;
        private final Color fg;
        private final Color hoverFg;
        private final boolean hasBorder;
        private final Color borderColor;
        private boolean hovered = false;

        public PillButton(String text, Icon icon, Color bg, Color hoverBg, Color fg, Color hoverFg, boolean hasBorder, Color borderColor) {
            super(text, icon);
            this.bg = bg;
            this.hoverBg = hoverBg;
            this.fg = fg;
            this.hoverFg = hoverFg;
            this.hasBorder = hasBorder;
            this.borderColor = borderColor;

            setFont(AppTheme.font(Font.BOLD, 12));
            setForeground(fg);
            setIconTextGap(7);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        public static PillButton primary(String text, Icon icon) {
            return new PillButton(text, icon, AppTheme.FOREST_GREEN, AppTheme.FOREST_MID, Color.WHITE, Color.WHITE, false, null);
        }

        public static PillButton secondary(String text, Icon icon) {
            return new PillButton(text, icon, new Color(236, 248, 240), new Color(220, 244, 228), AppTheme.FOREST_GREEN, AppTheme.FOREST_MID, true, AppTheme.BORDER_SAGE);
        }

        public static PillButton subtle(String text, Icon icon) {
            return new PillButton(text, icon, new Color(244, 248, 246), new Color(230, 240, 234), AppTheme.TEXT_SECONDARY, AppTheme.TEXT_PRIMARY, true, AppTheme.BORDER_SAGE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int arc = h; // Perfect pill stadium shape

            boolean enabled = isEnabled();
            Color currentBg = !enabled ? new Color(236, 242, 239) : (hovered ? hoverBg : bg);
            Color currentFg = !enabled ? new Color(160, 175, 168) : (hovered ? hoverFg : fg);

            // Fill background pill
            g2.setColor(currentBg);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            if (hasBorder && enabled) {
                g2.setColor(hovered ? AppTheme.FOREST_MID : (borderColor != null ? borderColor : AppTheme.BORDER_SAGE));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }

            // Draw Icon + Text perfectly centered
            Icon icon = getIcon();
            String text = getText();
            Font font = getFont();
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);

            int textW = (text != null && !text.isEmpty()) ? fm.stringWidth(text) : 0;
            int iconW = (icon != null) ? icon.getIconWidth() : 0;
            int iconH = (icon != null) ? icon.getIconHeight() : 0;
            int gap = (icon != null && text != null && !text.isEmpty()) ? getIconTextGap() : 0;
            int totalW = iconW + gap + textW;
            int startX = (w - totalW) / 2;

            if (icon != null) {
                int iconY = (h - iconH) / 2;
                if (icon instanceof SidebarIcon) {
                    ((SidebarIcon) icon).setOverrideColor(currentFg);
                }
                icon.paintIcon(this, g2, startX, iconY);
            }

            if (text != null && !text.isEmpty()) {
                int textX = startX + iconW + gap;
                int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(currentFg);
                g2.drawString(text, textX, textY);
            }

            g2.dispose();
        }
    }

    // ── CUSTOM DROPDOWN (Comfortable, modern Keyra-styled selector) ───────────
    public static class CustomDropdown extends JButton {
        private final List<String> items;
        private int selectedIndex = 0;
        private boolean hovered = false;
        private boolean popupOpen = false;
        private JPopupMenu popup;

        public CustomDropdown(String[] options) {
            this.items = new ArrayList<>(Arrays.asList(options));
            setFont(AppTheme.font(Font.BOLD, 13));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, 44)); // Comfortable generous height

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
            return (selectedIndex >= 0 && selectedIndex < items.size()) ? items.get(selectedIndex) : "";
        }

        private void showPopup() {
            popup = new JPopupMenu();
            popup.setBackground(Color.WHITE);
            popup.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER_SAGE, 1),
                    new EmptyBorder(6, 6, 6, 6)
            ));

            popup.addPopupMenuListener(new PopupMenuListener() {
                @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    popupOpen = true;
                    repaint();
                }
                @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                    popupOpen = false;
                    repaint();
                }
                @Override public void popupMenuCanceled(PopupMenuEvent e) {
                    popupOpen = false;
                    repaint();
                }
            });

            int menuWidth = Math.max(getWidth(), 360);

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
                            g2.setColor(new Color(236, 252, 242)); // Soft emerald mint
                            g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);
                        } else if (getModel().isArmed() || getModel().isRollover()) {
                            g2.setColor(new Color(0, 90, 61, 14)); // Translucent hover
                            g2.fillRoundRect(2, 2, w - 4, h - 4, arc, arc);
                        }

                        g2.setFont(AppTheme.font(isSelected ? Font.BOLD : Font.PLAIN, 13));
                        g2.setColor(isSelected ? AppTheme.FOREST_DEEP : AppTheme.TEXT_PRIMARY);
                        FontMetrics fm = g2.getFontMetrics();
                        int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                        g2.drawString(text, 16, ty);

                        if (isSelected) {
                            g2.setColor(AppTheme.FOREST_GREEN);
                            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                            Path2D chk = new Path2D.Float();
                            int cx = w - 24;
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
                item.setPreferredSize(new Dimension(menuWidth, 38));
                item.setCursor(new Cursor(Cursor.HAND_CURSOR));
                item.addActionListener(ev -> setSelectedIndex(idx));
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
            int r = 12;

            boolean active = hovered || popupOpen;

            // Fill background
            g2.setColor(active ? new Color(242, 248, 244) : new Color(248, 251, 249));
            g2.fillRoundRect(0, 0, w, h, r, r);

            // Subtle border with forest highlight on active/hover
            g2.setColor(active ? AppTheme.FOREST_GREEN : AppTheme.BORDER_SAGE);
            g2.setStroke(new BasicStroke(active ? 1.5f : 1.0f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);

            // Left icon
            int iconX = 16;
            SidebarIcon icon = new SidebarIcon("printer", 16, AppTheme.FOREST_GREEN);
            icon.paintIcon(this, g2, iconX, (h - 16) / 2);

            // Selected value text
            String val = getSelectedItem();
            g2.setFont(AppTheme.font(Font.BOLD, 13));
            g2.setColor(AppTheme.TEXT_PRIMARY);
            FontMetrics fm = g2.getFontMetrics();
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(val, iconX + 28, ty);

            // Downward vector chevron on right (flips upwards if popup is open)
            g2.setColor(active ? AppTheme.FOREST_DEEP : AppTheme.TEXT_SECONDARY);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D chev = new Path2D.Float();
            int cx = w - 22;
            int cy = h / 2 - 1;
            if (popupOpen) {
                // Pointing UP
                chev.moveTo(cx - 4.5f, cy + 2.5f);
                chev.lineTo(cx, cy - 2.5f);
                chev.lineTo(cx + 4.5f, cy + 2.5f);
            } else {
                // Pointing DOWN
                chev.moveTo(cx - 4.5f, cy - 2.5f);
                chev.lineTo(cx, cy + 2.5f);
                chev.lineTo(cx + 4.5f, cy - 2.5f);
            }
            g2.draw(chev);

            g2.dispose();
        }
    }
}
