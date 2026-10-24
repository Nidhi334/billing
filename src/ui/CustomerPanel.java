package ui;

import dao.CustomerDAO;
import model.Customer;

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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modern, Minimalist Customer Directory & CRM Management Workspace.
 * Adheres strictly to the Keyra Design System:
 * - Minimal, borderless header matching Dashboard style (zero emoji clutter)
 * - "+ Add Customer" in header opens a clean modal popup dialog
 * - Full-width elevated card (arc = 36) with subtle ambient drop shadow
 * - Purchasing sort filters: Newest Customer, Oldest Customer, Most Purchases, Least Purchases
 * - Purchasing metrics displayed per row (Total Spent & Order count)
 * - Diverse, vibrant gradient colors for circular initial avatars (ID omitted below name)
 * - Sleek inline dual action buttons per row: [Edit] and [Delete]
 * - Modern rounded search field with embedded vector magnifying glass & live filtering
 */
public class CustomerPanel extends JPanel {

    private final CustomerDAO customerDAO = new CustomerDAO();

    // Table & Models
    private DefaultTableModel tableModel;
    private JTable table;
    private List<Customer> loadedCustomers = new ArrayList<>();
    private Map<Integer, double[]> customerStats = new HashMap<>();

    // Search & Sort Components
    private ModernSearchField txtSearch;
    private JLabel lblRecordCount;
    private String currentSort = "NEWEST"; // NEWEST, OLDEST, MOST_PURCHASES, LEAST_PURCHASES
    private String searchQuery = "";

    // Sort Filter Chips
    private JButton btnSortNewest;
    private JButton btnSortOldest;
    private JButton btnSortMostPurchases;
    private JButton btnSortLeastPurchases;

    public CustomerPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(AppTheme.BG_CANVAS);
        setBorder(new EmptyBorder(14, 18, 16, 18));

        initComponents();
        loadCustomerTable();
    }

    private void initComponents() {
        // =========================================================================
        // 1. TOP HEADER: Minimal Dashboard Style
        // =========================================================================
        JPanel headerPanel = new JPanel(new BorderLayout(16, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(4, 4, 16, 4));

        JLabel lblTitle = new JLabel("Customers");
        lblTitle.setFont(AppTheme.font(Font.BOLD, 24));
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        // Header Right: "+ Add Customer" Primary Pill CTA & Smooth Circular Refresh
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        JButton btnAddCustomer = createPrimaryActionButton("Add Customer", "plus");
        btnAddCustomer.addActionListener(e -> openCustomerDialog(null));

        JButton btnRefresh = createCircularRefreshButton();

        headerRight.add(btnAddCustomer);
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

        // Top Toolbar: Sort Chips + Search
        JPanel tableHeaderBox = new JPanel();
        tableHeaderBox.setLayout(new BoxLayout(tableHeaderBox, BoxLayout.Y_AXIS));
        tableHeaderBox.setOpaque(false);

        JPanel row1 = new JPanel(new BorderLayout(12, 0));
        row1.setOpaque(false);

        JPanel tabPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabPills.setOpaque(false);

        btnSortNewest = createSortChip("Newest Customer", "NEWEST");
        btnSortOldest = createSortChip("Oldest Customer", "OLDEST");
        btnSortMostPurchases = createSortChip("Most Purchases", "MOST_PURCHASES");
        btnSortLeastPurchases = createSortChip("Least Purchases", "LEAST_PURCHASES");

        tabPills.add(btnSortNewest);
        tabPills.add(btnSortOldest);
        tabPills.add(btnSortMostPurchases);
        tabPills.add(btnSortLeastPurchases);
        row1.add(tabPills, BorderLayout.WEST);

        lblRecordCount = new JLabel("0 customers registered");
        lblRecordCount.setFont(AppTheme.font(Font.BOLD, 12));
        lblRecordCount.setForeground(AppTheme.TEXT_MUTED);
        lblRecordCount.setBorder(new EmptyBorder(4, 6, 4, 6));
        row1.add(lblRecordCount, BorderLayout.EAST);

        tableHeaderBox.add(row1);
        tableHeaderBox.add(Box.createVerticalStrut(10));

        // Row 2: Search Field
        JPanel row2 = new JPanel(new BorderLayout(12, 0));
        row2.setOpaque(false);

        txtSearch = new ModernSearchField("Search customers by name, phone, email...");
        txtSearch.setPreferredSize(new Dimension(360, 34));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchQuery = txtSearch.getText().trim(); applyFilter(); }
            public void removeUpdate(DocumentEvent e) { searchQuery = txtSearch.getText().trim(); applyFilter(); }
            public void changedUpdate(DocumentEvent e) { searchQuery = txtSearch.getText().trim(); applyFilter(); }
        });
        row2.add(txtSearch, BorderLayout.WEST);

        tableHeaderBox.add(row2);
        tableCard.add(tableHeaderBox, BorderLayout.NORTH);

        // Directory Table Setup
        String[] cols = {"#", "Customer Profile", "Phone Number", "Email Address", "Purchases & Orders", "Billing / Shipping Address", "Actions"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        setupTableStyle(table);

        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(0).setMinWidth(45);

        table.getColumnModel().getColumn(1).setPreferredWidth(260); // Profile (Avatar + Name)
        table.getColumnModel().getColumn(2).setPreferredWidth(130); // Phone
        table.getColumnModel().getColumn(3).setPreferredWidth(180); // Email
        table.getColumnModel().getColumn(4).setPreferredWidth(160); // Purchases (Spent + Orders)
        table.getColumnModel().getColumn(5).setPreferredWidth(220); // Address

        table.getColumnModel().getColumn(6).setPreferredWidth(150); // Actions ([Edit] [Delete])
        table.getColumnModel().getColumn(6).setMaxWidth(160);
        table.getColumnModel().getColumn(6).setMinWidth(140);

        // Column 0: Index centered
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        centerRender.setFont(AppTheme.font(Font.BOLD, 12));
        centerRender.setForeground(AppTheme.TEXT_MUTED);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        // Column 1: Vibrant Avatar + Customer Details (No redundant ID)
        table.getColumnModel().getColumn(1).setCellRenderer(new CustomerProfileCellRenderer());

        // Column 2, 3, 5: Standard padded clean text
        DefaultTableCellRenderer paddedLeft = new DefaultTableCellRenderer();
        paddedLeft.setFont(AppTheme.font(Font.PLAIN, 12));
        paddedLeft.setBorder(new EmptyBorder(0, 10, 0, 10));
        table.getColumnModel().getColumn(2).setCellRenderer(paddedLeft);
        table.getColumnModel().getColumn(3).setCellRenderer(paddedLeft);
        table.getColumnModel().getColumn(5).setCellRenderer(paddedLeft);

        // Column 4: Purchases & Orders renderer
        table.getColumnModel().getColumn(4).setCellRenderer(new PurchasesCellRenderer());

        // Column 6: Dual Actions ([Edit] and [Delete])
        table.getColumnModel().getColumn(6).setCellRenderer(new DualActionCellRenderer());

        // Action Clicking & Hover Handling
        setupTableActionMouseListener();

        tableCard.add(createTableScrollPane(table), BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);
    }

    public void openCustomerDialog(Customer customer) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        CustomerDialog dialog = new CustomerDialog(parent, customer, this::loadCustomerTable);
        dialog.setVisible(true);
    }

    private void handleDeleteCustomer(Customer c) {
        if (c == null) return;
        String displayName = c.getName() != null && !c.getName().isEmpty() ? c.getName() : "Customer #" + c.getId();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete customer profile '" + displayName + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (customerDAO.deleteCustomer(c.getId())) {
                    loadCustomerTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not delete customer.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Cannot delete customer (may have billing order history): " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void setupTableActionMouseListener() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || col != 6) return;

                Object val = table.getValueAt(row, 1);
                if (!(val instanceof Customer)) return;
                Customer target = (Customer) val;

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
                        openCustomerDialog(target);
                    } else if (relX >= delX1 && relX <= delX2) {
                        handleDeleteCustomer(target);
                    }
                }
            }
        });

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 6) {
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
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        t.setFillsViewportHeight(true);

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
                    String msg = "No customers registered or matching your search.";

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
                    loadCustomerTable();
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
        btn.setToolTipText("Refresh customers list");
        return btn;
    }

    private JButton createSortChip(String text, String sortKey) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = h;
                boolean active = sortKey.equals(currentSort);

                if (active) {
                    g2.setColor(AppTheme.FOREST_GREEN);
                    g2.fillRoundRect(0, 0, w, h, r, r);
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, w, h, r, r);
                    g2.setColor(AppTheme.BORDER_SAGE);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                    g2.setColor(AppTheme.TEXT_SECONDARY);
                }

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 11));
        int strW = btn.getFontMetrics(btn.getFont()).stringWidth(text);
        btn.setPreferredSize(new Dimension(strW + 24, 28));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            currentSort = sortKey;
            btnSortNewest.repaint();
            btnSortOldest.repaint();
            btnSortMostPurchases.repaint();
            btnSortLeastPurchases.repaint();
            applyFilter();
        });
        return btn;
    }

    // =========================================================================
    // DATA MANAGEMENT & FILTERING / SORTING
    // =========================================================================
    public void loadCustomerTable() {
        try {
            loadedCustomers = customerDAO.getAllCustomers();
            if (loadedCustomers == null) loadedCustomers = new ArrayList<>();
            customerStats = customerDAO.getAllCustomerStats();
            if (customerStats == null) customerStats = new HashMap<>();
            applyFilter();
        } catch (Exception e) {
            System.err.println("Notice: Could not load customers: " + e.getMessage());
        }
    }

    private void applyFilter() {
        tableModel.setRowCount(0);
        String q = searchQuery.toLowerCase();

        List<Customer> filtered = new ArrayList<>();
        for (Customer c : loadedCustomers) {
            if (!q.isEmpty()) {
                boolean match = (c.getName() != null && c.getName().toLowerCase().contains(q))
                        || (c.getPhone() != null && c.getPhone().toLowerCase().contains(q))
                        || (c.getEmail() != null && c.getEmail().toLowerCase().contains(q))
                        || (c.getAddress() != null && c.getAddress().toLowerCase().contains(q));
                if (!match) continue;
            }
            filtered.add(c);
        }

        // Apply Purchasing / Date Sorting
        filtered.sort((c1, c2) -> {
            switch (currentSort) {
                case "OLDEST": {
                    if (c1.getCreatedAt() != null && c2.getCreatedAt() != null) {
                        int cmp = c1.getCreatedAt().compareTo(c2.getCreatedAt());
                        if (cmp != 0) return cmp;
                    }
                    return Integer.compare(c1.getId(), c2.getId());
                }
                case "MOST_PURCHASES": {
                    double s1 = customerStats.getOrDefault(c1.getId(), new double[]{0, 0})[1];
                    double s2 = customerStats.getOrDefault(c2.getId(), new double[]{0, 0})[1];
                    int cmp = Double.compare(s2, s1);
                    return cmp != 0 ? cmp : Integer.compare(c2.getId(), c1.getId());
                }
                case "LEAST_PURCHASES": {
                    double s1 = customerStats.getOrDefault(c1.getId(), new double[]{0, 0})[1];
                    double s2 = customerStats.getOrDefault(c2.getId(), new double[]{0, 0})[1];
                    int cmp = Double.compare(s1, s2);
                    return cmp != 0 ? cmp : Integer.compare(c1.getId(), c2.getId());
                }
                case "NEWEST":
                default: {
                    if (c1.getCreatedAt() != null && c2.getCreatedAt() != null) {
                        int cmp = c2.getCreatedAt().compareTo(c1.getCreatedAt());
                        if (cmp != 0) return cmp;
                    }
                    return Integer.compare(c2.getId(), c1.getId());
                }
            }
        });

        int rowIdx = 1;
        for (Customer c : filtered) {
            boolean hasPhone = c.getPhone() != null && !c.getPhone().trim().isEmpty();
            boolean hasEmail = c.getEmail() != null && !c.getEmail().trim().isEmpty();
            double[] stats = customerStats.getOrDefault(c.getId(), new double[]{0.0, 0.0});

            tableModel.addRow(new Object[]{
                    c.getId(), // ID directly in column 0
                    c,         // Customer entity for avatar renderer (no ID in subtitle)
                    hasPhone ? c.getPhone() : "—",
                    hasEmail ? c.getEmail() : "—",
                    stats,     // Purchases stats: [total_orders, total_spent]
                    c.getAddress() != null && !c.getAddress().isEmpty() ? c.getAddress() : "—",
                    ""         // DualActionCellRenderer renders [Edit] and [Delete]
            });
            rowIdx++;
        }
        lblRecordCount.setText(filtered.size() + " customers registered");
    }

    // =========================================================================
    // CUSTOM REUSABLE TABLE RENDERERS (Colorful Avatars + Purchases + Actions)
    // =========================================================================

    /**
     * Renders a 36x36 circular initial avatar with curated vibrant gradient color
     * palettes deterministically assigned per customer. Subtitle shows registration date
     * or active client status (omitting redundant ID).
     */
    private static class CustomerProfileCellRenderer extends DefaultTableCellRenderer {
        private static final Color[][] AVATAR_PALETTES = {
                { new Color(16, 185, 129), new Color(5, 150, 105) },   // Emerald / Forest
                { new Color(139, 92, 246), new Color(109, 40, 217) }, // Royal Violet / Purple
                { new Color(244, 63, 94), new Color(225, 29, 72) },   // Warm Coral / Rose
                { new Color(245, 158, 11), new Color(217, 119, 6) },  // Vibrant Amber / Orange
                { new Color(99, 102, 241), new Color(67, 56, 202) },  // Deep Indigo / Blue
                { new Color(20, 184, 166), new Color(13, 148, 136) }, // Ocean Teal
                { new Color(239, 68, 68), new Color(185, 28, 28) },   // Crimson / Red
                { new Color(217, 70, 239), new Color(162, 28, 175) }, // Electric Fuchsia
                { new Color(14, 165, 233), new Color(2, 132, 199) },   // Sky Sapphire
                { new Color(234, 88, 12), new Color(194, 65, 12) }    // Sunset Rust
        };

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");

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

                    Customer cust = (val instanceof Customer) ? (Customer) val : null;
                    String name = (cust != null && cust.getName() != null) ? cust.getName() : "Customer";
                    int id = (cust != null) ? cust.getId() : 0;

                    // 1. Draw circular gradient avatar with unique curated color per customer
                    int avSz = 36;
                    int avX = 12;
                    int avY = (h - avSz) / 2;

                    int hash = (name + "_" + id).hashCode();
                    int colorIdx = Math.abs(hash) % AVATAR_PALETTES.length;
                    Color topCol = AVATAR_PALETTES[colorIdx][0];
                    Color botCol = AVATAR_PALETTES[colorIdx][1];

                    GradientPaint gp = new GradientPaint(avX, avY, topCol, avX + avSz, avY + avSz, botCol);
                    g2.setPaint(gp);
                    g2.fillOval(avX, avY, avSz, avSz);

                    // Inner border ring
                    g2.setColor(new Color(255, 255, 255, 80));
                    g2.drawOval(avX, avY, avSz - 1, avSz - 1);

                    // Avatar Initials
                    String initials = getInitials(name);
                    g2.setColor(Color.WHITE);
                    g2.setFont(AppTheme.font(Font.BOLD, 12));
                    FontMetrics fmAvi = g2.getFontMetrics();
                    int tx = avX + (avSz - fmAvi.stringWidth(initials)) / 2;
                    int ty = avY + ((avSz - fmAvi.getHeight()) / 2) + fmAvi.getAscent();
                    g2.drawString(initials, tx, ty);

                    // 2. Text Layout beside Avatar
                    int textX = avX + avSz + 12;

                    // Line 1: Customer Name
                    g2.setFont(AppTheme.font(Font.BOLD, 12));
                    g2.setColor(AppTheme.TEXT_PRIMARY);
                    FontMetrics fmName = g2.getFontMetrics();
                    int nameY = avY + 15;

                    int maxW = w - textX - 8;
                    String dispName = name;
                    if (fmName.stringWidth(dispName) > maxW && maxW > 20) {
                        while (dispName.length() > 3 && fmName.stringWidth(dispName + "...") > maxW) {
                            dispName = dispName.substring(0, dispName.length() - 1);
                        }
                        dispName = dispName + "...";
                    }
                    g2.drawString(dispName, textX, nameY);

                    // Line 2: Customer Subtitle (Registration Date or Active Client - NO ID)
                    g2.setFont(AppTheme.font(Font.PLAIN, 10));
                    g2.setColor(AppTheme.TEXT_MUTED);
                    String sub;
                    if (cust != null && cust.getCreatedAt() != null) {
                        sub = "Joined " + dateFormat.format(cust.getCreatedAt());
                    } else {
                        sub = "Verified Client";
                    }
                    int subY = avY + 30;
                    g2.drawString(sub, textX, subY);

                    g2.dispose();
                }
            };
        }

        private String getInitials(String name) {
            if (name == null || name.trim().isEmpty()) return "C";
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
            }
            return name.substring(0, Math.min(2, name.length())).toUpperCase();
        }
    }

    /**
     * Renders customer purchasing summary: Total Spent (bold currency) & Orders count.
     */
    private static class PurchasesCellRenderer extends DefaultTableCellRenderer {
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

                    double[] stats = (val instanceof double[]) ? (double[]) val : new double[]{0, 0};
                    int orders = (int) stats[0];
                    double spent = stats[1];

                    int py = (h - 32) / 2;

                    // Line 1: Total Spent
                    g2.setFont(AppTheme.font(Font.BOLD, 12));
                    g2.setColor(spent > 0 ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_MUTED);
                    String spentStr = String.format("₹%,.2f", spent);
                    g2.drawString(spentStr, 12, py + 14);

                    // Line 2: Orders Count
                    g2.setFont(AppTheme.font(Font.PLAIN, 10));
                    g2.setColor(orders > 0 ? AppTheme.FOREST_MID : AppTheme.TEXT_MUTED);
                    String ordersStr = orders == 1 ? "1 invoice" : (orders + " invoices");
                    g2.drawString(ordersStr, 12, py + 28);

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
