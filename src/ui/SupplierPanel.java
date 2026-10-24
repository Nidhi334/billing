package ui;

import dao.SupplierDAO;
import model.Supplier;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern, Minimalist Supplier & Vendor Management Workspace.
 * Adheres strictly to the Keyra Design System:
 * - Minimal, borderless header matching Dashboard style (zero emoji clutter)
 * - "+ Add Supplier" in header opens a clean modal popup dialog
 * - Full-width elevated card (arc = 36) with subtle ambient drop shadow
 * - Sleek inline dual action buttons per row: [Edit] and [Delete]
 * - Clicking [Edit] opens popup pre-filled with supplier data for fast updating
 * - Modern rounded search field with embedded vector magnifying glass & live filtering
 */
public class SupplierPanel extends JPanel {

    private final SupplierDAO supplierDAO = new SupplierDAO();

    // Table & Models
    private DefaultTableModel tableModel;
    private JTable table;
    private List<Supplier> cachedSuppliers = new ArrayList<>();

    // Search & Feedback
    private ModernSearchField txtSearch;
    private JLabel lblRecordCount;
    private String searchQuery = "";

    public SupplierPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(AppTheme.BG_CANVAS);
        setBorder(new EmptyBorder(14, 18, 16, 18));

        initComponents();
        loadSupplierTable();
    }

    private void initComponents() {
        // =========================================================================
        // 1. TOP HEADER: Minimal Dashboard Style
        // =========================================================================
        JPanel headerPanel = new JPanel(new BorderLayout(16, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(4, 4, 16, 4));

        JLabel lblTitle = new JLabel("Suppliers & Vendors");
        lblTitle.setFont(AppTheme.font(Font.BOLD, 24));
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        // Header Right: "+ Add Supplier" Primary Pill CTA & Smooth Circular Refresh
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        JButton btnAddSupplier = createPrimaryActionButton("Add Supplier", "plus");
        btnAddSupplier.addActionListener(e -> openSupplierDialog(null));

        JButton btnRefresh = createCircularRefreshButton();

        headerRight.add(btnAddSupplier);
        headerRight.add(btnRefresh);
        headerPanel.add(headerRight, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // =========================================================================
        // 2. MAIN FULL-WIDTH ELEVATED CARD WORKSPACE
        // Matching Dashboard card styling: borderless, subtle drop shadow, arc = 36
        // =========================================================================
        JPanel tableCard = createElevatedCard(36);
        tableCard.setLayout(new BorderLayout(0, 12));
        tableCard.setBorder(new EmptyBorder(20, 22, 20, 22));

        // Top Toolbar: Title, Count & Modern Pill Search Field
        JPanel toolbarRow = new JPanel(new BorderLayout(12, 0));
        toolbarRow.setOpaque(false);

        JPanel titleAndCount = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleAndCount.setOpaque(false);

        JLabel lblDirTitle = new JLabel("All Registered Suppliers & Vendors");
        lblDirTitle.setFont(AppTheme.font(Font.BOLD, 15));
        lblDirTitle.setForeground(AppTheme.TEXT_PRIMARY);

        lblRecordCount = new JLabel("0 suppliers registered");
        lblRecordCount.setFont(AppTheme.font(Font.BOLD, 12));
        lblRecordCount.setForeground(AppTheme.TEXT_MUTED);
        lblRecordCount.setBorder(new EmptyBorder(2, 6, 2, 6));

        titleAndCount.add(lblDirTitle);
        titleAndCount.add(lblRecordCount);
        toolbarRow.add(titleAndCount, BorderLayout.WEST);

        // Modern Pill Search Field with Real-Time Document Listener
        txtSearch = new ModernSearchField("Search suppliers by vendor name, contact person, phone, or email...");
        txtSearch.setPreferredSize(new Dimension(380, 34));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchQuery = txtSearch.getText().trim(); applyFilter(); }
            public void removeUpdate(DocumentEvent e) { searchQuery = txtSearch.getText().trim(); applyFilter(); }
            public void changedUpdate(DocumentEvent e) { searchQuery = txtSearch.getText().trim(); applyFilter(); }
        });
        toolbarRow.add(txtSearch, BorderLayout.EAST);

        tableCard.add(toolbarRow, BorderLayout.NORTH);

        // Directory Table Setup
        String[] cols = {"#", "Supplier & Vendor Details", "Phone Number", "Email Address", "Address / Location", "Actions"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        setupTableStyle(table);

        table.getColumnModel().getColumn(0).setPreferredWidth(45);  // #
        table.getColumnModel().getColumn(1).setPreferredWidth(300); // Vendor & Contact
        table.getColumnModel().getColumn(2).setPreferredWidth(140); // Phone
        table.getColumnModel().getColumn(3).setPreferredWidth(180); // Email
        table.getColumnModel().getColumn(4).setPreferredWidth(220); // Address
        table.getColumnModel().getColumn(5).setPreferredWidth(155); // Actions ([Edit] [Delete])

        // Column 0: Index centered
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        centerRender.setFont(AppTheme.font(Font.BOLD, 12));
        centerRender.setForeground(AppTheme.TEXT_MUTED);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        // Column 1: Squircle Icon + Vendor & Contact details
        table.getColumnModel().getColumn(1).setCellRenderer(new SupplierDetailsCellRenderer());

        // Column 2, 3, 4: Standard padded clean text
        DefaultTableCellRenderer paddedLeft = new DefaultTableCellRenderer();
        paddedLeft.setFont(AppTheme.font(Font.PLAIN, 12));
        paddedLeft.setBorder(new EmptyBorder(0, 10, 0, 10));
        table.getColumnModel().getColumn(2).setCellRenderer(paddedLeft);
        table.getColumnModel().getColumn(3).setCellRenderer(paddedLeft);
        table.getColumnModel().getColumn(4).setCellRenderer(paddedLeft);

        // Column 5: Dual Actions ([Edit] and [Delete])
        table.getColumnModel().getColumn(5).setCellRenderer(new DualActionCellRenderer());

        // Action Clicking & Hover Handling
        setupTableActionMouseListener();

        tableCard.add(createTableScrollPane(table), BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);
    }

    public void openSupplierDialog(Supplier supplier) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        SupplierDialog dialog = new SupplierDialog(parent, supplier, this::loadSupplierTable);
        dialog.setVisible(true);
    }

    private void handleDeleteSupplier(Supplier s) {
        if (s == null) return;
        String displayName = (s.getCompanyName() != null && !s.getCompanyName().isEmpty()) 
                ? s.getCompanyName() : s.getName();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete supplier '" + displayName + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (supplierDAO.deleteSupplier(s.getId())) {
                    loadSupplierTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not delete supplier.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Cannot delete supplier: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void setupTableActionMouseListener() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || col != 5) return;

                Object val = table.getValueAt(row, 1);
                if (!(val instanceof Supplier)) return;
                Supplier target = (Supplier) val;

                Rectangle cellRect = table.getCellRect(row, col, false);
                int relX = e.getX() - cellRect.x;
                int relY = e.getY() - cellRect.y;
                int cellW = cellRect.width;

                int editW = 58;
                int delW = 68;
                int gap = 8;
                int totalW = editW + gap + delW;
                int startX = (cellW - totalW) / 2;

                int editX1 = startX;
                int editX2 = startX + editW;
                int delX1 = startX + editW + gap;
                int delX2 = startX + totalW;

                int pillH = 26;
                int btnY = (table.getRowHeight() - pillH) / 2;

                if (relY >= btnY - 2 && relY <= btnY + pillH + 2) {
                    if (relX >= editX1 && relX <= editX2) {
                        openSupplierDialog(target);
                    } else if (relX >= delX1 && relX <= delX2) {
                        handleDeleteSupplier(target);
                    }
                }
            }
        });

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 5) {
                    Rectangle cellRect = table.getCellRect(row, col, false);
                    int relX = e.getX() - cellRect.x;
                    int relY = e.getY() - cellRect.y;
                    int cellW = cellRect.width;

                    int editW = 58;
                    int delW = 68;
                    int gap = 8;
                    int totalW = editW + gap + delW;
                    int startX = (cellW - totalW) / 2;

                    int editX1 = startX;
                    int editX2 = startX + editW;
                    int delX1 = startX + editW + gap;
                    int delX2 = startX + totalW;

                    int pillH = 26;
                    int btnY = (table.getRowHeight() - pillH) / 2;

                    if (relY >= btnY - 2 && relY <= btnY + pillH + 2 &&
                            ((relX >= editX1 && relX <= editX2) || (relX >= delX1 && relX <= delX2))) {
                        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        return;
                    }
                }
                table.setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    private void setupTableStyle(JTable t) {
        t.setRowHeight(50);
        t.setFont(AppTheme.font(Font.PLAIN, 12));
        t.setShowVerticalLines(false);
        t.setGridColor(new Color(241, 246, 243));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setSelectionBackground(new Color(230, 248, 238));
        t.setSelectionForeground(AppTheme.TEXT_PRIMARY);

        JTableHeader header = t.getTableHeader();
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, focus, r, c);
                l.setFont(AppTheme.font(Font.BOLD, 11));
                l.setForeground(AppTheme.TEXT_PRIMARY);
                l.setBackground(new Color(245, 250, 247));
                l.setOpaque(true);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_SAGE),
                        new EmptyBorder(0, 10, 0, 10)
                ));
                if (c == 0 || c == tbl.getColumnCount() - 1) {
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return l;
            }
        });
    }

    private JScrollPane createTableScrollPane(JTable table) {
        JScrollPane scroll = new JScrollPane();

        JViewport customViewport = new JViewport() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (table.getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth(), h = getHeight();
                    String msg = "No suppliers registered or matching your search.";

                    g2.setFont(AppTheme.font(Font.BOLD, 13));
                    g2.setColor(AppTheme.TEXT_MUTED);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (w - fm.stringWidth(msg)) / 2;
                    int ty = h / 2;
                    g2.drawString(msg, tx, ty);
                    g2.dispose();
                }
            }
        };
        customViewport.setOpaque(false);
        customViewport.setView(table);
        scroll.setViewport(customViewport);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(205, 230, 215);
                this.trackColor = new Color(248, 252, 249);
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });
        return scroll;
    }

    private JPanel createElevatedCard(int arc) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Subtle ambient drop shadow matching dashboard
                g2.setColor(new Color(0, 50, 30, 6));
                g2.fillRoundRect(1, 2, w - 2, h - 2, arc, arc);
                // Card surface - Pure crisp white, NO BORDER!
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        return card;
    }

    private JButton createPrimaryActionButton(String text, String iconName) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                if (iconName != null) {
                    setIcon(new SidebarIcon(iconName, 14, Color.WHITE));
                }
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(hovered ? AppTheme.FOREST_MID : AppTheme.FOREST_GREEN);
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(145, 36));
        btn.setIconTextGap(8);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createCircularRefreshButton() {
        JButton btn = new JButton() {
            private boolean hovered = false;
            private double currentAngle = 0.0;
            private javax.swing.Timer animTimer = null;
            private final SidebarIcon refreshIcon = new SidebarIcon("refresh", 16, AppTheme.FOREST_GREEN);
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
                addActionListener(e -> {
                    startRotateAnimation();
                    loadSupplierTable();
                });
            }

            private void startRotateAnimation() {
                if (animTimer != null && animTimer.isRunning()) return;
                final long startTime = System.currentTimeMillis();
                final long duration = 400;
                final double startA = currentAngle;

                animTimer = new javax.swing.Timer(16, evt -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed >= duration) {
                        currentAngle = startA + Math.PI;
                        animTimer.stop();
                        repaint();
                    } else {
                        double progress = (double) elapsed / duration;
                        currentAngle = startA + (Math.PI * (1.0 - Math.pow(1.0 - progress, 2)));
                        repaint();
                    }
                });
                animTimer.start();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                g2.setColor(hovered ? new Color(230, 248, 238) : Color.WHITE);
                g2.fillOval(0, 0, w - 1, h - 1);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawOval(0, 0, w - 1, h - 1);

                int cx = w / 2;
                int cy = h / 2;
                g2.rotate(currentAngle, cx, cy);
                int sz = refreshIcon.getIconWidth();
                refreshIcon.paintIcon(this, g2, cx - sz / 2, cy - sz / 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(36, 36));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Refresh suppliers list");
        return btn;
    }

    // =========================================================================
    // DATA MANAGEMENT & FILTERING
    // =========================================================================
    public void loadSupplierTable() {
        try {
            cachedSuppliers = supplierDAO.getAllSuppliers();
            if (cachedSuppliers == null) cachedSuppliers = new ArrayList<>();
            applyFilter();
        } catch (Exception e) {
            System.err.println("Notice: Could not load suppliers: " + e.getMessage());
        }
    }

    private void applyFilter() {
        tableModel.setRowCount(0);
        String q = searchQuery.toLowerCase();
        int rowIdx = 1;

        for (Supplier s : cachedSuppliers) {
            if (!q.isEmpty()) {
                boolean match = (s.getName() != null && s.getName().toLowerCase().contains(q))
                        || (s.getCompanyName() != null && s.getCompanyName().toLowerCase().contains(q))
                        || (s.getPhone() != null && s.getPhone().toLowerCase().contains(q))
                        || (s.getEmail() != null && s.getEmail().toLowerCase().contains(q))
                        || (s.getAddress() != null && s.getAddress().toLowerCase().contains(q));
                if (!match) continue;
            }

            tableModel.addRow(new Object[]{
                    rowIdx++,
                    s, // Store Supplier instance for renderer & click handling
                    s.getPhone() != null && !s.getPhone().isEmpty() ? s.getPhone() : "—",
                    s.getEmail() != null && !s.getEmail().isEmpty() ? s.getEmail() : "—",
                    s.getAddress() != null && !s.getAddress().isEmpty() ? s.getAddress() : "—",
                    "" // DualActionCellRenderer renders [Edit] and [Delete]
            });
        }
        lblRecordCount.setText(tableModel.getRowCount() + " suppliers registered");
    }

    // =========================================================================
    // CUSTOM REUSABLE TABLE RENDERERS
    // =========================================================================

    /**
     * Renders a 36x36 squircle icon + Company/Vendor Name (bold) + Contact Person Name (muted).
     */
    private static class SupplierDetailsCellRenderer extends DefaultTableCellRenderer {
        private final SidebarIcon buildingIcon = new SidebarIcon("building", 16, AppTheme.FOREST_GREEN);

        @Override
        public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
            return new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth(), h = getHeight();

                    if (sel) {
                        g2.setColor(new Color(230, 248, 238));
                        g2.fillRect(0, 0, w, h);
                    }

                    Supplier s = (val instanceof Supplier) ? (Supplier) val : null;
                    String company = (s != null && s.getCompanyName() != null && !s.getCompanyName().isEmpty()) 
                            ? s.getCompanyName() : "Vendor";
                    String contact = (s != null && s.getName() != null && !s.getName().isEmpty()) 
                            ? s.getName() : "—";
                    int id = (s != null) ? s.getId() : 0;

                    // 1. Draw rounded squircle icon container
                    int iconSz = 36;
                    int iconX = 12;
                    int iconY = (h - iconSz) / 2;

                    g2.setColor(new Color(234, 247, 238));
                    g2.fillRoundRect(iconX, iconY, iconSz, iconSz, 10, 10);
                    g2.setColor(new Color(210, 238, 222));
                    g2.drawRoundRect(iconX, iconY, iconSz - 1, iconSz - 1, 10, 10);

                    int sz = buildingIcon.getIconWidth();
                    buildingIcon.paintIcon(this, g2, iconX + (iconSz - sz) / 2, iconY + (iconSz - sz) / 2);

                    // 2. Text Layout beside squircle icon
                    int textX = iconX + iconSz + 12;

                    // Line 1: Company / Vendor Name
                    g2.setFont(AppTheme.font(Font.BOLD, 12));
                    g2.setColor(AppTheme.TEXT_PRIMARY);
                    FontMetrics fmName = g2.getFontMetrics();
                    int nameY = iconY + 15;

                    int maxW = w - textX - 8;
                    String dispCompany = company;
                    if (fmName.stringWidth(dispCompany) > maxW && maxW > 20) {
                        while (dispCompany.length() > 3 && fmName.stringWidth(dispCompany + "...") > maxW) {
                            dispCompany = dispCompany.substring(0, dispCompany.length() - 1);
                        }
                        dispCompany = dispCompany + "...";
                    }
                    g2.drawString(dispCompany, textX, nameY);

                    // Line 2: Contact Person • ID
                    g2.setFont(AppTheme.font(Font.PLAIN, 10));
                    g2.setColor(AppTheme.TEXT_MUTED);
                    String sub = "Contact: " + contact + "  •  ID: #" + id;
                    int subY = iconY + 30;
                    g2.drawString(sub, textX, subY);

                    g2.dispose();
                }
            };
        }
    }

    /**
     * Dual Action Cell Renderer: Displays [Edit] and [Delete] inline pill buttons.
     */
    private static class DualActionCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
            return new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth(), h = getHeight();

                    if (sel) {
                        g2.setColor(new Color(230, 248, 238));
                        g2.fillRect(0, 0, w, h);
                    }

                    int editW = 58;
                    int delW = 68;
                    int gap = 8;
                    int totalW = editW + gap + delW;
                    int pillH = 26;
                    int py = (h - pillH) / 2;
                    int startX = (w - totalW) / 2;

                    // 1. "Edit" Button Pill
                    int editX = startX;
                    g2.setColor(new Color(248, 250, 252));
                    g2.fillRoundRect(editX, py, editW, pillH, pillH, pillH);
                    g2.setColor(new Color(226, 232, 240));
                    g2.drawRoundRect(editX, py, editW - 1, pillH - 1, pillH, pillH);

                    // Small pencil icon
                    g2.setColor(new Color(71, 85, 105));
                    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int iconX = editX + 11;
                    int iconY = py + 7;
                    g2.drawLine(iconX, iconY + 8, iconX + 8, iconY);
                    g2.drawLine(iconX - 1, iconY + 11, iconX + 2, iconY + 11);

                    g2.setFont(AppTheme.font(Font.BOLD, 11));
                    g2.setColor(new Color(51, 65, 85));
                    FontMetrics fm = g2.getFontMetrics();
                    int editTy = py + ((pillH - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString("Edit", editX + 24, editTy);

                    // 2. "Delete" Button Pill
                    int delX = startX + editW + gap;
                    g2.setColor(new Color(254, 242, 242));
                    g2.fillRoundRect(delX, py, delW, pillH, pillH, pillH);
                    g2.setColor(new Color(254, 202, 202));
                    g2.drawRoundRect(delX, py, delW - 1, pillH - 1, pillH, pillH);

                    // Small trash icon
                    g2.setColor(new Color(220, 38, 38));
                    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int tx1 = delX + 11;
                    int ty1 = py + 8;
                    g2.drawLine(tx1, ty1 + 1, tx1 + 8, ty1 + 1); // lid
                    g2.drawRect(tx1 + 1, ty1 + 2, 6, 7);         // bin

                    g2.setFont(AppTheme.font(Font.BOLD, 11));
                    g2.setColor(new Color(220, 38, 38));
                    fm = g2.getFontMetrics();
                    int delTy = py + ((pillH - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString("Delete", delX + 23, delTy);

                    g2.dispose();
                }
            };
        }
    }

    /**
     * Modern pill search field with Java2D vector magnifying glass.
     */
    private static class ModernSearchField extends JTextField {
        private final String placeholder;
        private boolean focused = false;

        public ModernSearchField(String placeholder) {
            this.placeholder = placeholder;
            setFont(AppTheme.font(Font.PLAIN, 12));
            setForeground(AppTheme.TEXT_PRIMARY);
            setOpaque(false);
            setBorder(new EmptyBorder(6, 32, 6, 12));

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
            int r = h;

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

            // Vector Magnifying Glass
            g2.setColor(focused ? AppTheme.FOREST_GREEN : AppTheme.TEXT_MUTED);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cy = h / 2 - 1;
            g2.drawOval(11, cy - 4, 8, 8);
            g2.drawLine(17, cy + 3, 22, cy + 8);

            super.paintComponent(g);

            if (getText().isEmpty() && !focused && placeholder != null) {
                g2.setColor(AppTheme.TEXT_MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(placeholder, 34, ty);
            }
            g2.dispose();
        }
    }
}
