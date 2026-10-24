package ui;

import dao.CategoryDAO;
import model.Category;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Modern, Minimalist Modal Dialog for Managing Product Categories.
 * Matches Keyra Design System:
 * - Clean minimal layout without redundant duplicate header box
 * - Sleek, comfortable modern input fields
 * - Inline delete trash icon in each category row for superior UX (no separate "delete selected" button)
 * - Anti-aliased pill action buttons
 */
public class CategoryDialog extends JDialog {
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private DefaultTableModel tableModel;
    private JTable table;
    private ModernInputBox txtName;
    private ModernInputBox txtDesc;
    private JButton btnAdd;
    private JButton btnClose;

    public CategoryDialog(Frame owner) {
        super(owner, "Manage Product Categories", true);
        setSize(560, 480);
        setMinimumSize(new Dimension(500, 420));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(248, 250, 249));

        initComponents();
        loadCategories();
        setupShortcuts();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(new Color(248, 250, 249));
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        // =========================================================================
        // 1. TOP: NEW CATEGORY FORM CARD (Sleek, Minimal)
        // =========================================================================
        JPanel formCard = new JPanel(new BorderLayout(0, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, 16, 16);
                g2.setColor(new Color(230, 238, 234));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
                g2.dispose();
            }
        };
        formCard.setOpaque(false);
        formCard.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel inputsRow = new JPanel(new GridLayout(1, 2, 12, 0));
        inputsRow.setOpaque(false);

        txtName = new ModernInputBox("e.g. Electronics, Beverages...");
        txtDesc = new ModernInputBox("Brief description or note...");

        inputsRow.add(createFieldGroup("Category Name *", txtName));
        inputsRow.add(createFieldGroup("Description", txtDesc));

        formCard.add(inputsRow, BorderLayout.CENTER);

        // Add Category Button Row inside form card
        JPanel formBtnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        formBtnRow.setOpaque(false);
        btnAdd = createPillButton("Add Category", "plus", AppTheme.FOREST_GREEN, null, Color.WHITE);
        btnAdd.setPreferredSize(new Dimension(135, 34));
        btnAdd.addActionListener(e -> addCategory());
        formBtnRow.add(btnAdd);
        formCard.add(formBtnRow, BorderLayout.SOUTH);

        root.add(formCard, BorderLayout.NORTH);

        // =========================================================================
        // 2. CENTER: AVAILABLE CATEGORIES TABLE CARD (with inline delete icon)
        // =========================================================================
        JPanel tableCard = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, 16, 16);
                g2.setColor(new Color(230, 238, 234));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
                g2.dispose();
            }
        };
        tableCard.setOpaque(false);
        tableCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Table Header Label
        JLabel lblListTitle = new JLabel("Available Categories");
        lblListTitle.setFont(AppTheme.font(Font.BOLD, 12));
        lblListTitle.setForeground(AppTheme.TEXT_SECONDARY);
        tableCard.add(lblListTitle, BorderLayout.NORTH);

        // Columns: ID, Category, Description, Action (Delete)
        tableModel = new DefaultTableModel(new String[]{"ID", "Category Name", "Description", "Action"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        setupTableStyle(table);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(45);
        table.getColumnModel().getColumn(0).setMaxWidth(55);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setMaxWidth(80);

        // Cell Renderers
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        centerRender.setFont(AppTheme.font(Font.BOLD, 11));
        centerRender.setForeground(AppTheme.TEXT_MUTED);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        DefaultTableCellRenderer nameRender = new DefaultTableCellRenderer();
        nameRender.setFont(AppTheme.font(Font.BOLD, 12));
        nameRender.setForeground(AppTheme.TEXT_PRIMARY);
        nameRender.setBorder(new EmptyBorder(0, 8, 0, 8));
        table.getColumnModel().getColumn(1).setCellRenderer(nameRender);

        DefaultTableCellRenderer descRender = new DefaultTableCellRenderer();
        descRender.setFont(AppTheme.font(Font.PLAIN, 12));
        descRender.setForeground(AppTheme.TEXT_SECONDARY);
        descRender.setBorder(new EmptyBorder(0, 8, 0, 8));
        table.getColumnModel().getColumn(2).setCellRenderer(descRender);

        // Column 3: Inline Delete Icon Renderer
        table.getColumnModel().getColumn(3).setCellRenderer(new DeleteRowActionRenderer());

        // Setup mouse click and cursor listener for inline delete
        setupTableActionMouseListener();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        tableCard.add(scroll, BorderLayout.CENTER);

        root.add(tableCard, BorderLayout.CENTER);

        // =========================================================================
        // 3. BOTTOM ACTIONS FOOTER (Close Pill Button)
        // =========================================================================
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);

        btnClose = createPillButton("Close", null, Color.WHITE, new Color(226, 232, 240), new Color(71, 85, 105));
        btnClose.setPreferredSize(new Dimension(90, 34));
        btnClose.addActionListener(e -> dispose());
        footer.add(btnClose);

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
        p.add(Box.createVerticalStrut(5));
        p.add(field);
        return p;
    }

    private void setupTableStyle(JTable tbl) {
        tbl.setRowHeight(34);
        tbl.setShowGrid(false);
        tbl.setIntercellSpacing(new Dimension(0, 0));
        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tbl.setSelectionBackground(new Color(237, 247, 242));
        tbl.setSelectionForeground(AppTheme.FOREST_GREEN);
        tbl.setOpaque(false);

        JTableHeader th = tbl.getTableHeader();
        th.setFont(AppTheme.font(Font.BOLD, 11));
        th.setForeground(new Color(100, 116, 139));
        th.setBackground(new Color(248, 250, 252));
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 238, 234)));
        th.setPreferredSize(new Dimension(0, 32));
        th.setReorderingAllowed(false);
        th.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setFont(AppTheme.font(Font.BOLD, 11));
                l.setForeground(new Color(100, 116, 139));
                l.setBackground(new Color(248, 250, 252));
                l.setBorder(new EmptyBorder(0, 8, 0, 8));
                if (column == 0 || column == 3) l.setHorizontalAlignment(SwingConstants.CENTER);
                else l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });
    }

    private void setupTableActionMouseListener() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || col != 3) return;

                Rectangle cellRect = table.getCellRect(row, col, false);
                int relX = e.getX() - cellRect.x;
                int relY = e.getY() - cellRect.y;
                int cellW = cellRect.width;

                int pillW = 28;
                int pillH = 26;
                int pillX = (cellW - pillW) / 2;
                int pillY = (table.getRowHeight() - pillH) / 2;

                if (relX >= pillX && relX <= pillX + pillW && relY >= pillY && relY <= pillY + pillH) {
                    int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
                    String name = tableModel.getValueAt(row, 1).toString();
                    confirmAndDeleteCategory(id, name);
                }
            }
        });

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 3) {
                    Rectangle cellRect = table.getCellRect(row, col, false);
                    int relX = e.getX() - cellRect.x;
                    int cellW = cellRect.width;
                    int pillW = 28;
                    int pillX = (cellW - pillW) / 2;
                    if (relX >= pillX && relX <= pillX + pillW) {
                        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        return;
                    }
                }
                table.setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    /**
     * Renders a soft red circle with trash vector icon in the Action column.
     */
    private static class DeleteRowActionRenderer extends DefaultTableCellRenderer {
        private final SidebarIcon trashIcon = new SidebarIcon("trash", 12, new Color(220, 38, 38));

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object val, boolean isSelected, boolean hasFocus, int r, int c) {
            return new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();

                    if (isSelected) {
                        g2.setColor(new Color(237, 247, 242));
                        g2.fillRect(0, 0, w, h);
                    }

                    int pillW = 28;
                    int pillH = 24;
                    int pillX = (w - pillW) / 2;
                    int pillY = (h - pillH) / 2;

                    g2.setColor(new Color(254, 242, 242));
                    g2.fillRoundRect(pillX, pillY, pillW, pillH, pillH, pillH);
                    g2.setColor(new Color(254, 202, 202));
                    g2.drawRoundRect(pillX, pillY, pillW - 1, pillH - 1, pillH, pillH);

                    int sz = trashIcon.getIconWidth();
                    trashIcon.paintIcon(this, g2, pillX + (pillW - sz) / 2, pillY + (pillH - trashIcon.getIconHeight()) / 2);
                    g2.dispose();
                }
            };
        }
    }

    private void loadCategories() {
        try {
            tableModel.setRowCount(0);
            List<Category> list = categoryDAO.getAllCategories();
            for (Category c : list) {
                tableModel.addRow(new Object[]{c.getId(), c.getName(), c.getDescription() != null ? c.getDescription() : "—", ""});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading categories: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addCategory() {
        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Category name is required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtName.requestFocusInWindow();
            return;
        }
        try {
            if (categoryDAO.addCategory(new Category(0, name, desc))) {
                txtName.setText("");
                txtDesc.setText("");
                loadCategories();
                txtName.requestFocusInWindow();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to create category.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding category: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void confirmAndDeleteCategory(int id, String catName) {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete the category \"" + catName + "\"?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            if (categoryDAO.deleteCategory(id)) {
                loadCategories();
            } else {
                JOptionPane.showMessageDialog(this, "Cannot delete category because it may be linked to products in stock.", "Unable to Delete", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Cannot delete category: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupShortcuts() {
        JRootPane root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDialog");
        root.getActionMap().put("closeDialog", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { dispose(); }
        });

        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addCategory();
                }
            }
        };
        txtName.addKeyListener(enterListener);
        txtDesc.addKeyListener(enterListener);
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

                Color fill = hovered ? (bg.equals(AppTheme.FOREST_GREEN) ? AppTheme.FOREST_MID : (border != null ? new Color(241, 245, 249) : bg.darker())) : bg;
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

    /**
     * Custom text input box with smooth focus border and placeholder support.
     */
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
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int r = 12;

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w, h, r, r);

            if (focused) {
                g2.setColor(AppTheme.FOREST_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);

            super.paintComponent(g);

            if (getText().isEmpty() && !focused && placeholder != null) {
                g2.setColor(new Color(156, 163, 175));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(placeholder, 12, ty);
            }
            g2.dispose();
        }
    }
}
