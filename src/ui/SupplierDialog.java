package ui;

import dao.SupplierDAO;
import model.Supplier;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Modern, Minimalist Modal Dialog for Adding and Editing Suppliers.
 * Used for both adding new vendors and updating existing vendor profiles.
 */
public class SupplierDialog extends JDialog {

    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final Supplier existingSupplier;
    private final Runnable onSaved;

    private ModernInputBox txtName;
    private ModernInputBox txtCompany;
    private ModernInputBox txtPhone;
    private ModernInputBox txtEmail;
    private ModernInputBox txtAddress;

    public SupplierDialog(Window owner, Supplier supplier, Runnable onSaved) {
        super(owner, supplier == null ? "Add Supplier" : "Edit Supplier", ModalityType.APPLICATION_MODAL);
        this.existingSupplier = supplier;
        this.onSaved = onSaved;

        setSize(480, 520);
        setResizable(false);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        initComponents();
        populateFieldsIfEditing();
        setupShortcuts();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Color.WHITE);

        // =========================================================================
        // 1. HEADER (Minimal squircle icon + title & subtitle)
        // =========================================================================
        JPanel headerPanel = new JPanel(new BorderLayout(14, 0));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(235, 245, 240)),
                new EmptyBorder(16, 22, 16, 22)
        ));

        // Squircle Icon
        JPanel iconContainer = new JPanel() {
            private final SidebarIcon boxIcon = new SidebarIcon("suppliers", 16, AppTheme.FOREST_GREEN);
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(234, 247, 238));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                int sz = boxIcon.getIconWidth();
                boxIcon.paintIcon(this, g2, (w - sz) / 2, (h - sz) / 2);
                g2.dispose();
            }
        };
        iconContainer.setPreferredSize(new Dimension(38, 38));
        iconContainer.setOpaque(false);
        headerPanel.add(iconContainer, BorderLayout.WEST);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);

        boolean isEdit = existingSupplier != null;
        JLabel lblTitle = new JLabel(isEdit ? "Edit Supplier Details" : "Add New Supplier");
        lblTitle.setFont(AppTheme.font(Font.BOLD, 15));
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel lblSub = new JLabel(isEdit ? "Update vendor contact details and billing location" 
                                           : "Register a verified supplier for inward purchases");
        lblSub.setFont(AppTheme.font(Font.PLAIN, 11));
        lblSub.setForeground(AppTheme.TEXT_MUTED);

        headerText.add(lblTitle);
        headerText.add(Box.createVerticalStrut(2));
        headerText.add(lblSub);
        headerPanel.add(headerText, BorderLayout.CENTER);

        root.add(headerPanel, BorderLayout.NORTH);

        // =========================================================================
        // 2. FORM BODY
        // =========================================================================
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(18, 24, 14, 24));

        txtName = new ModernInputBox("e.g. Ramesh Kumar");
        txtCompany = new ModernInputBox("e.g. Om Logistics Pvt Ltd");
        txtPhone = new ModernInputBox("e.g. +91 98765 43210");
        txtEmail = new ModernInputBox("e.g. vendor@domain.com");
        txtAddress = new ModernInputBox("e.g. Warehouse 4B, Industrial Area");

        body.add(createFieldGroup("Contact Person Name *", txtName));
        body.add(Box.createVerticalStrut(10));
        body.add(createFieldGroup("Company / Vendor Name *", txtCompany));
        body.add(Box.createVerticalStrut(10));
        body.add(createFieldGroup("Phone Number", txtPhone));
        body.add(Box.createVerticalStrut(10));
        body.add(createFieldGroup("Email Address", txtEmail));
        body.add(Box.createVerticalStrut(10));
        body.add(createFieldGroup("Address / Location", txtAddress));

        root.add(body, BorderLayout.CENTER);

        // =========================================================================
        // 3. BOTTOM ACTIONS FOOTER
        // =========================================================================
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(248, 252, 249));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(235, 245, 240)),
                new EmptyBorder(14, 22, 14, 22)
        ));

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightButtons.setOpaque(false);

        JButton btnCancel = createPillButton("Cancel", null, new Color(248, 250, 252), new Color(226, 232, 240), new Color(71, 85, 105));
        btnCancel.setPreferredSize(new Dimension(85, 36));
        btnCancel.addActionListener(e -> dispose());

        String btnLabel = isEdit ? "Update Supplier" : "Add Supplier";
        String iconKey = isEdit ? "check" : "plus";
        JButton btnSubmit = createPillButton(btnLabel, iconKey, AppTheme.FOREST_GREEN, null, Color.WHITE);
        btnSubmit.setPreferredSize(new Dimension(145, 36));
        btnSubmit.addActionListener(e -> saveSupplier());

        rightButtons.add(btnCancel);
        rightButtons.add(btnSubmit);
        footer.add(rightButtons, BorderLayout.EAST);

        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel createFieldGroup(String labelText, JComponent field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(AppTheme.font(Font.BOLD, 11));
        lbl.setForeground(new Color(51, 65, 85));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(field);
        return p;
    }

    private void populateFieldsIfEditing() {
        if (existingSupplier == null) return;
        txtName.setText(existingSupplier.getName() != null ? existingSupplier.getName() : "");
        txtCompany.setText(existingSupplier.getCompanyName() != null ? existingSupplier.getCompanyName() : "");
        txtPhone.setText(existingSupplier.getPhone() != null ? existingSupplier.getPhone() : "");
        txtEmail.setText(existingSupplier.getEmail() != null ? existingSupplier.getEmail() : "");
        txtAddress.setText(existingSupplier.getAddress() != null ? existingSupplier.getAddress() : "");
    }

    private void saveSupplier() {
        String name = txtName.getText().trim();
        String company = txtCompany.getText().trim();
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();
        String address = txtAddress.getText().trim();

        if (name.isEmpty() && company.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter at least Contact Person Name or Company Name.", "Validation Required", JOptionPane.WARNING_MESSAGE);
            txtName.requestFocusInWindow();
            return;
        }

        try {
            boolean success;
            if (existingSupplier == null) {
                Supplier newSupplier = new Supplier(0, name, company, phone, email, address);
                success = supplierDAO.addSupplier(newSupplier);
            } else {
                Supplier updatedSupplier = new Supplier(existingSupplier.getId(), name, company, phone, email, address);
                success = supplierDAO.updateSupplier(updatedSupplier);
            }

            if (success) {
                if (onSaved != null) onSaved.run();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save supplier to database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupShortcuts() {
        JRootPane root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDialog");
        root.getActionMap().put("closeDialog", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { dispose(); }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submitDialog");
        root.getActionMap().put("submitDialog", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { saveSupplier(); }
        });
    }

    private JButton createPillButton(String text, String iconKey, Color bg, Color border, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            private final SidebarIcon icon = (iconKey != null) ? new SidebarIcon(iconKey, 12, fg) : null;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(java.awt.event.MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = h;

                Color fill = hovered ? (bg.equals(AppTheme.FOREST_GREEN) ? AppTheme.FOREST_MID : new Color(241, 245, 249)) : bg;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w, h, r, r);

                if (border != null) {
                    g2.setColor(border);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                }

                int iconSz = (icon != null) ? icon.getIconWidth() : 0;
                int gap = (icon != null && !getText().isEmpty()) ? 6 : 0;

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(getText());
                int contentW = iconSz + gap + textW;

                int curX = (w - contentW) / 2;
                if (icon != null) {
                    icon.paintIcon(this, g2, curX, (h - icon.getIconHeight()) / 2);
                    curX += iconSz + gap;
                }

                g2.setColor(fg);
                int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), curX, ty);

                g2.dispose();
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private static class ModernInputBox extends JTextField {
        private final String placeholder;
        private boolean focused = false;

        public ModernInputBox(String placeholder) {
            this.placeholder = placeholder;
            setFont(AppTheme.font(Font.PLAIN, 12));
            setForeground(AppTheme.TEXT_PRIMARY);
            setOpaque(false);
            setBorder(new EmptyBorder(0, 12, 0, 12));
            setPreferredSize(new Dimension(0, 36));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

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
            int r = 14;

            g2.setColor(new Color(248, 252, 249));
            g2.fillRoundRect(0, 0, w, h, r, r);

            if (focused) {
                g2.setColor(AppTheme.FOREST_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);

            super.paintComponent(g);

            if (getText().isEmpty() && !focused && placeholder != null) {
                g2.setColor(AppTheme.TEXT_MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(placeholder, 12, ty);
            }
            g2.dispose();
        }
    }
}
