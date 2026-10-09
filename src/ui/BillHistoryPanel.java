package ui;

import dao.BillingDAO;
import model.Sale;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * BillHistoryPanel - Redesigned Orders & Billing History view.
 * Cohesive with the Keyra Admin Dashboard design system:
 * - Airy Mint canvas background
 * - Elevated crisp white KPI cards with vector watermarks & ambient drop shadows
 * - Modern search and filter card with stadium pill shortcut filters
 * - Streamlined high-DPI table with custom header, pill badges, and interactive Action buttons
 */
public class BillHistoryPanel extends JPanel {
    private final BillingDAO billingDAO = new BillingDAO();
    private User currentUser;

    private JTable table;
    private DefaultTableModel model;
    private List<Sale> currentSalesList = new ArrayList<>();
    private int hoveredRow = -1;

    // Filters
    private ModernSearchField txtSearch;
    private ModernDateField txtFromDate;
    private ModernDateField txtToDate;
    private ModernDropdown cmbPaymentMode;

    // Quick Date Shortcut Pills
    private DateFilterPillButton btnToday;
    private DateFilterPillButton btnYesterday;
    private DateFilterPillButton btnThisMonth;
    private DateFilterPillButton btnAllTime;

    // KPI Cards
    private JLabel lblTotalBillsVal;
    private JLabel lblTotalRevenueVal;
    private JLabel lblTotalCashVal;
    private JLabel lblTotalOnlineVal;
    private JLabel lblTableStatus;

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public BillHistoryPanel() {
        this(null);
    }

    public BillHistoryPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout(0, 14));
        setBackground(AppTheme.BG_CANVAS);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        initUI();
        loadBillHistory();
    }

    private void initUI() {
        // =========================================================================
        // 1. TOP SECTION: Header & Actions + KPI Summary Cards + Filter Bar
        // =========================================================================
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setOpaque(false);

        // --- 1.1 Header & Actions ---
        JPanel topContainer = new JPanel(new BorderLayout(12, 0));
        topContainer.setOpaque(false);
        topContainer.setBorder(new EmptyBorder(0, 2, 6, 2));

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);

        JLabel lblTitle = new JLabel("Orders & Billing History");
        lblTitle.setFont(AppTheme.font(Font.BOLD, 22));
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        titleRow.add(lblTitle);

        JLabel lblSub = new JLabel("Complete ledger of completed customer bills, sales history, search, and reprint receipts");
        lblSub.setFont(AppTheme.font(Font.PLAIN, 12));
        lblSub.setForeground(AppTheme.TEXT_SECONDARY);

        headerPanel.add(titleRow);
        headerPanel.add(Box.createVerticalStrut(3));
        headerPanel.add(lblSub);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topActions.setOpaque(false);

        // Circular Refresh Button matching Dashboard style
        JButton btnRefresh = new JButton() {
            private boolean hovered = false;
            private final SidebarIcon refreshIcon = new SidebarIcon("refresh", 16, AppTheme.FOREST_GREEN);
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
                addActionListener(e -> loadBillHistory());
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int sz = Math.min(getWidth(), getHeight()) - 2;
                int x = (getWidth() - sz) / 2;
                int y = (getHeight() - sz) / 2;

                g2.setColor(hovered ? AppTheme.BG_CANVAS : Color.WHITE);
                g2.fillOval(x, y, sz, sz);
                g2.setColor(hovered ? AppTheme.FOREST_MID : AppTheme.BORDER_SAGE);
                g2.setStroke(new BasicStroke(hovered ? 1.5f : 1.1f));
                g2.drawOval(x, y, sz, sz);

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                refreshIcon.paintIcon(this, g2, cx - refreshIcon.getIconWidth() / 2, cy - refreshIcon.getIconHeight() / 2);
                g2.dispose();
            }
        };
        btnRefresh.setPreferredSize(new Dimension(38, 38));
        btnRefresh.setFocusPainted(false);
        btnRefresh.setContentAreaFilled(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setOpaque(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.setToolTipText("Refresh Order Records");

        // Primary View Details Button
        JButton btnViewPrint = new JButton("View Bill Details") {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = h;

                // Ambient shadow
                g2.setColor(new Color(0, 50, 30, hovered ? 22 : 12));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);

                // Button fill
                g2.setColor(hovered ? AppTheme.FOREST_MID : AppTheme.FOREST_GREEN);
                g2.fillRoundRect(0, 0, w, h, r, r);

                // Icon & Text
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                String text = "👁️ View Bill Details";
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(text, tx, ty);

                g2.dispose();
            }
        };
        btnViewPrint.setFont(AppTheme.font(Font.BOLD, 12));
        btnViewPrint.setForeground(Color.WHITE);
        btnViewPrint.setFocusPainted(false);
        btnViewPrint.setContentAreaFilled(false);
        btnViewPrint.setBorderPainted(false);
        btnViewPrint.setOpaque(false);
        btnViewPrint.setPreferredSize(new Dimension(160, 38));
        btnViewPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewPrint.addActionListener(e -> viewSelectedBill());

        topActions.add(btnRefresh);
        topActions.add(btnViewPrint);

        topContainer.add(headerPanel, BorderLayout.WEST);
        topContainer.add(topActions, BorderLayout.EAST);
        northPanel.add(topContainer);
        northPanel.add(Box.createVerticalStrut(12));

        // --- 1.2 KPI Summary Cards (4 Cards) ---
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiGrid.setOpaque(false);

        kpiGrid.add(createKeyraKpiCard("ORDERS", "TOTAL ORDERS", "0 Bills", AppTheme.FOREST_GREEN, "billing", (lbl) -> lblTotalBillsVal = lbl));
        kpiGrid.add(createKeyraKpiCard("REVENUE", "TOTAL REVENUE", "₹0", Color.decode("#00875a"), "trending_up", (lbl) -> lblTotalRevenueVal = lbl));
        kpiGrid.add(createKeyraKpiCard("CASH", "CASH COLLECTED", "₹0", Color.decode("#d97706"), "wallet", (lbl) -> lblTotalCashVal = lbl));
        kpiGrid.add(createKeyraKpiCard("DIGITAL", "UPI & CARDS", "₹0", Color.decode("#7e22ce"), "zap", (lbl) -> lblTotalOnlineVal = lbl));

        northPanel.add(kpiGrid);
        northPanel.add(Box.createVerticalStrut(12));

        // --- 1.3 Modern Filter Bar ---
        JPanel filterCard = new JPanel(new BorderLayout(12, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = 20;

                // Ambient shadow
                g2.setColor(new Color(0, 50, 30, 5));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);

                // White surface
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);

                // Sage border
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        filterCard.setOpaque(false);
        filterCard.setBorder(new EmptyBorder(10, 14, 10, 14));

        // Left Filters Row
        JPanel leftFilterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        leftFilterRow.setOpaque(false);

        txtSearch = new ModernSearchField("Search invoice no, customer, mobile...", 19);
        txtSearch.setToolTipText("Search by invoice no, customer name, mobile, or cashier");
        txtSearch.addActionListener(e -> loadBillHistory());
        Timer searchDebounceTimer = new Timer(220, e -> loadBillHistory());
        searchDebounceTimer.setRepeats(false);
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { searchDebounceTimer.restart(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { searchDebounceTimer.restart(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { searchDebounceTimer.restart(); }
        });
        leftFilterRow.add(txtSearch);

        txtFromDate = new ModernDateField("From Date", 9);
        txtFromDate.setToolTipText("Format: YYYY-MM-DD");
        txtFromDate.addActionListener(e -> loadBillHistory());
        leftFilterRow.add(txtFromDate);

        txtToDate = new ModernDateField("To Date", 9);
        txtToDate.setToolTipText("Format: YYYY-MM-DD");
        txtToDate.addActionListener(e -> loadBillHistory());
        leftFilterRow.add(txtToDate);

        cmbPaymentMode = new ModernDropdown("Payment", "wallet", new String[]{"ALL", "CASH", "UPI", "CARD", "CREDIT"});
        cmbPaymentMode.setPreferredSize(new Dimension(165, 34));
        cmbPaymentMode.addActionListener(e -> loadBillHistory());
        leftFilterRow.add(cmbPaymentMode);

        JButton btnFilter = new JButton("Filter") {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = h;

                g2.setColor(hovered ? AppTheme.FOREST_MID : AppTheme.FOREST_GREEN);
                g2.fillRoundRect(0, 0, w, h, r, r);

                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                String text = "Filter";
                int tx = (w - fm.stringWidth(text)) / 2;
                int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(text, tx, ty);
                g2.dispose();
            }
        };
        btnFilter.setFont(AppTheme.font(Font.BOLD, 11));
        btnFilter.setForeground(Color.WHITE);
        btnFilter.setFocusPainted(false);
        btnFilter.setContentAreaFilled(false);
        btnFilter.setBorderPainted(false);
        btnFilter.setOpaque(false);
        btnFilter.setPreferredSize(new Dimension(72, 34));
        btnFilter.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnFilter.addActionListener(e -> loadBillHistory());
        leftFilterRow.add(btnFilter);

        // Right Filter Shortcuts (Stadium Pills)
        JPanel rightFilterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        rightFilterRow.setOpaque(false);

        btnToday = new DateFilterPillButton("Today");
        btnToday.addActionListener(e -> {
            setActiveDatePill(btnToday);
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            txtFromDate.setText(today);
            txtToDate.setText(today);
            loadBillHistory();
        });

        btnYesterday = new DateFilterPillButton("Yesterday");
        btnYesterday.addActionListener(e -> {
            setActiveDatePill(btnYesterday);
            String yest = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            txtFromDate.setText(yest);
            txtToDate.setText(yest);
            loadBillHistory();
        });

        btnThisMonth = new DateFilterPillButton("This Month");
        btnThisMonth.addActionListener(e -> {
            setActiveDatePill(btnThisMonth);
            LocalDate now = LocalDate.now();
            txtFromDate.setText(now.withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
            txtToDate.setText(now.format(DateTimeFormatter.ISO_LOCAL_DATE));
            loadBillHistory();
        });

        btnAllTime = new DateFilterPillButton("All Time");
        btnAllTime.setActive(true);
        btnAllTime.addActionListener(e -> {
            setActiveDatePill(btnAllTime);
            txtSearch.setText("");
            txtFromDate.setText("");
            txtToDate.setText("");
            cmbPaymentMode.setSelectedIndex(0);
            loadBillHistory();
        });

        rightFilterRow.add(btnToday);
        rightFilterRow.add(btnYesterday);
        rightFilterRow.add(btnThisMonth);
        rightFilterRow.add(btnAllTime);

        filterCard.add(leftFilterRow, BorderLayout.WEST);
        filterCard.add(rightFilterRow, BorderLayout.EAST);

        northPanel.add(filterCard);
        add(northPanel, BorderLayout.NORTH);

        // =========================================================================
        // 2. CENTER SECTION: Elevated Table Card Container
        // =========================================================================
        JPanel tableCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = 20;

                // Ambient shadow
                g2.setColor(new Color(0, 50, 30, 6));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);

                // Card surface
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);

                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = 20;
                Shape clip = new RoundRectangle2D.Float(0, 0, w, h, r, r);
                g2.clip(clip);
                super.paintChildren(g2);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.dispose();
            }
        };
        tableCard.setOpaque(false);
        tableCard.setBorder(new EmptyBorder(1, 1, 1, 1));

        String[] cols = {
                "#", "Invoice No", "Date & Time", "Customer Name", "Customer Phone",
                "Payment Mode", "Total Amount (₹)", "Action"
        };
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7; // Only Action column is interactive
            }
        };

        table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    if (row == hoveredRow) {
                        c.setBackground(new Color(242, 252, 246)); // Soft mint hover
                    } else if (row % 2 == 1) {
                        c.setBackground(new Color(251, 254, 252)); // Very faint alternating mint
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                } else {
                    c.setBackground(new Color(230, 248, 238)); // Selected mint
                }
                return c;
            }
        };

        table.setRowHeight(44);
        table.setFont(AppTheme.font(Font.PLAIN, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(230, 248, 238));
        table.setSelectionForeground(AppTheme.TEXT_PRIMARY);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(241, 246, 243));

        // Header Styling
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 42));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setFont(AppTheme.font(Font.BOLD, 12));
                l.setForeground(AppTheme.TEXT_PRIMARY);
                l.setBackground(new Color(240, 250, 244)); // Fresh soft mint
                l.setOpaque(true);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_SAGE),
                        new EmptyBorder(0, 12, 0, 12)
                ));
                if (c == 0 || c == 2 || c == 4 || c == 5 || c == 7) {
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (c == 6) {
                    l.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return l;
            }
        });

        // Column Widths
        table.getColumnModel().getColumn(0).setPreferredWidth(45);   // #
        table.getColumnModel().getColumn(1).setPreferredWidth(175);  // Inv No
        table.getColumnModel().getColumn(2).setPreferredWidth(140);  // Date
        table.getColumnModel().getColumn(3).setPreferredWidth(165);  // Customer
        table.getColumnModel().getColumn(4).setPreferredWidth(125);  // Phone
        table.getColumnModel().getColumn(5).setPreferredWidth(110);  // Mode
        table.getColumnModel().getColumn(6).setPreferredWidth(135);  // Total Amount
        table.getColumnModel().getColumn(7).setPreferredWidth(125);  // Action Button

        // Alignments & Column Renderers
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        centerRender.setForeground(AppTheme.TEXT_MUTED);
        centerRender.setFont(AppTheme.font(Font.BOLD, 11));
        table.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        DefaultTableCellRenderer dateRender = new DefaultTableCellRenderer();
        dateRender.setHorizontalAlignment(SwingConstants.CENTER);
        dateRender.setForeground(AppTheme.TEXT_SECONDARY);
        dateRender.setFont(AppTheme.font(Font.PLAIN, 12));
        table.getColumnModel().getColumn(2).setCellRenderer(dateRender);

        DefaultTableCellRenderer phoneRender = new DefaultTableCellRenderer();
        phoneRender.setHorizontalAlignment(SwingConstants.CENTER);
        phoneRender.setForeground(AppTheme.TEXT_MUTED);
        phoneRender.setFont(AppTheme.font(Font.PLAIN, 12));
        table.getColumnModel().getColumn(4).setCellRenderer(phoneRender);

        // Invoice No Column renderer (Clean bold Forest Slate with pill style)
        DefaultTableCellRenderer invRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setFont(AppTheme.font(Font.BOLD, 12));
                l.setForeground(AppTheme.FOREST_MID);
                l.setBorder(new EmptyBorder(0, 14, 0, 8));
                return l;
            }
        };
        table.getColumnModel().getColumn(1).setCellRenderer(invRender);

        // Customer Name renderer
        DefaultTableCellRenderer custRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                String name = val != null ? val.toString() : "-";
                l.setFont(AppTheme.font(Font.PLAIN, 12));
                if ("Walk-in Customer".equalsIgnoreCase(name)) {
                    l.setForeground(AppTheme.TEXT_MUTED);
                } else {
                    l.setForeground(AppTheme.TEXT_PRIMARY);
                }
                l.setBorder(new EmptyBorder(0, 12, 0, 8));
                return l;
            }
        };
        table.getColumnModel().getColumn(3).setCellRenderer(custRender);

        // Payment Mode pill badge renderer
        table.getColumnModel().getColumn(5).setCellRenderer(new PaymentBadgeRenderer());

        // Total Amount column bold currency renderer
        DefaultTableCellRenderer totalRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setFont(AppTheme.font(Font.BOLD, 13));
                l.setForeground(AppTheme.TEXT_PRIMARY);
                l.setBorder(new EmptyBorder(0, 8, 0, 16));
                return l;
            }
        };
        table.getColumnModel().getColumn(6).setCellRenderer(totalRender);

        // Action Column: Sleek Keyra Stadium Pill Button
        table.getColumnModel().getColumn(7).setCellRenderer(new ActionButtonRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new ActionButtonEditor());

        // Mouse listeners: Track hover and handle click
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r != hoveredRow) {
                    hoveredRow = r;
                    table.repaint();
                }
                int c = table.columnAtPoint(e.getPoint());
                if (c == 7) {
                    table.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    table.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                table.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                int c = table.columnAtPoint(e.getPoint());
                if (r >= 0 && r < currentSalesList.size()) {
                    if (c == 7 || e.getClickCount() == 2) {
                        viewBillAt(r);
                    }
                }
            }
        });

        // Key Enter to view
        table.registerKeyboardAction(
                e -> viewSelectedBill(),
                KeyStroke.getKeyStroke("ENTER"),
                JComponent.WHEN_FOCUSED
        );

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setColumnHeaderView(table.getTableHeader());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(205, 230, 215);
                this.trackColor = new Color(248, 252, 249);
            }
            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isThumbRollover() ? AppTheme.FOREST_MID : thumbColor);
                g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 2, thumbBounds.width - 2, thumbBounds.height - 4, 6, 6);
                g2.dispose();
            }
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(trackColor);
                g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                g2.dispose();
            }
        });

        tableCard.add(scrollPane, BorderLayout.CENTER);

        // --- Bottom Status Bar inside Table Card ---
        JPanel bottomBar = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(248, 252, 249));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        bottomBar.setOpaque(false);
        bottomBar.setBorder(new EmptyBorder(10, 16, 10, 16));

        lblTableStatus = new JLabel("Loading orders...");
        lblTableStatus.setFont(AppTheme.font(Font.BOLD, 12));
        lblTableStatus.setForeground(AppTheme.TEXT_SECONDARY);

        JLabel lblHint = new JLabel("💡 Tip: Click '👁️ Details' or double-click any row to view full bill items & reprint");
        lblHint.setFont(AppTheme.font(Font.PLAIN, 11));
        lblHint.setForeground(AppTheme.TEXT_MUTED);

        bottomBar.add(lblTableStatus, BorderLayout.WEST);
        bottomBar.add(lblHint, BorderLayout.EAST);
        tableCard.add(bottomBar, BorderLayout.SOUTH);

        add(tableCard, BorderLayout.CENTER);
    }

    private void setActiveDatePill(DateFilterPillButton activeBtn) {
        if (btnToday != null) btnToday.setActive(btnToday == activeBtn);
        if (btnYesterday != null) btnYesterday.setActive(btnYesterday == activeBtn);
        if (btnThisMonth != null) btnThisMonth.setActive(btnThisMonth == activeBtn);
        if (btnAllTime != null) btnAllTime.setActive(btnAllTime == activeBtn);
    }

    // =========================================================================
    // KPI CARD FACTORY: Keyra Elevated White Card with Vector Watermark
    // =========================================================================
    private JPanel createKeyraKpiCard(String tagText, String title, String initialVal, Color accentColor, String iconName, java.util.function.Consumer<JLabel> labelConsumer) {
        SidebarIcon bgIcon = new SidebarIcon(iconName, 52, accentColor);

        JPanel card = new JPanel(new BorderLayout()) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = 24;

                // Ambient drop shadow
                g2.setColor(new Color(0, 50, 30, hovered ? 12 : 5));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);

                // Card surface - Pure crisp white
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);

                // Delicate sage border
                g2.setColor(hovered ? AppTheme.FOREST_MID : AppTheme.BORDER_SAGE);
                g2.setStroke(new BasicStroke(hovered ? 1.3f : 1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);

                // Watermark icon on the right
                Shape clip = new RoundRectangle2D.Float(0, 0, w, h, r, r);
                g2.setClip(clip);

                int iconSz = 52;
                int iconX = w - iconSz - 12;
                int iconY = (h - iconSz) / 2;

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hovered ? 0.16f : 0.08f));
                bgIcon.paintIcon(this, g2, iconX, iconY);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 18, 14, 18));
        card.setPreferredSize(new Dimension(200, 94));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Header Tag + Category Row
        JPanel tagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tagRow.setOpaque(false);
        tagRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTag = new JLabel(tagText) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 26));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblTag.setFont(AppTheme.font(Font.BOLD, 9));
        lblTag.setForeground(accentColor);
        lblTag.setBorder(new EmptyBorder(2, 6, 2, 6));

        JLabel lblTitle = new JLabel("  " + title);
        lblTitle.setFont(AppTheme.font(Font.BOLD, 10));
        lblTitle.setForeground(AppTheme.TEXT_MUTED);

        tagRow.add(lblTag);
        tagRow.add(lblTitle);

        // Big Value Label
        JLabel lblVal = new JLabel(initialVal);
        lblVal.setFont(AppTheme.font(Font.BOLD, 22));
        lblVal.setForeground(AppTheme.TEXT_PRIMARY);
        lblVal.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelConsumer.accept(lblVal);

        content.add(tagRow);
        content.add(Box.createVerticalStrut(6));
        content.add(lblVal);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // DATA LOADING & QUERYING
    // =========================================================================
    public void loadBillHistory() {
        String kw = txtSearch != null ? txtSearch.getText().trim() : "";
        String from = txtFromDate != null ? txtFromDate.getText().trim() : "";
        String to = txtToDate != null ? txtToDate.getText().trim() : "";
        String mode = cmbPaymentMode != null ? cmbPaymentMode.getSelectedItem() : "ALL";

        try {
            currentSalesList = billingDAO.searchSales(kw, from, to, mode);
            model.setRowCount(0);

            double totalRev = 0.0;
            double totalCash = 0.0;
            double totalOnline = 0.0;

            for (int i = 0; i < currentSalesList.size(); i++) {
                Sale s = currentSalesList.get(i);
                double total = s.getTotalAmount();
                totalRev += total;
                if ("CASH".equalsIgnoreCase(s.getPaymentMode())) {
                    totalCash += total;
                } else {
                    totalOnline += total;
                }

                String dateStr = s.getSaleDate() != null ? sdf.format(s.getSaleDate()) : "-";
                String cust = s.getCustomerName() != null ? s.getCustomerName() : "Walk-in Customer";
                String phone = s.getCustomerPhone() != null && !s.getCustomerPhone().isEmpty() ? s.getCustomerPhone() : "-";

                model.addRow(new Object[]{
                        i + 1,
                        s.getInvoiceNo(),
                        dateStr,
                        cust,
                        phone,
                        s.getPaymentMode() != null ? s.getPaymentMode() : "CASH",
                        AppTheme.formatCurrency(total),
                        "👁️ Details"
                });
            }

            // Update KPI cards
            lblTotalBillsVal.setText(currentSalesList.size() + " Bills");
            lblTotalRevenueVal.setText(AppTheme.formatCurrency(totalRev));
            lblTotalCashVal.setText(AppTheme.formatCurrency(totalCash));
            lblTotalOnlineVal.setText(AppTheme.formatCurrency(totalOnline));

            lblTableStatus.setText("Showing " + currentSalesList.size() + " bill(s) | Total Amount: " + AppTheme.formatCurrency(totalRev));

        } catch (Exception ex) {
            System.err.println("Notice: Could not load bill history from database: " + ex.getMessage());
            lblTableStatus.setText("⚠️ MySQL Disconnected. (Start MySQL service to sync live records)");

            if (currentSalesList.isEmpty()) {
                loadOfflineDemoSales();
            }
        }
    }

    private void loadOfflineDemoSales() {
        currentSalesList.clear();
        model.setRowCount(0);

        String[][] demoData = {
                {"INV-20261006-0001", "06/10/2026 14:48", "Walk-in Customer", "-", "UPI", "61124.00"},
                {"INV-20261005-0008", "05/10/2026 23:02", "Walk-in Customer", "-", "UPI", "118401.20"},
                {"INV-20261005-0007", "05/10/2026 23:00", "Walk-in Customer", "-", "UPI", "377.60"},
                {"INV-20261005-0006", "05/10/2026 19:15", "Walk-in Customer", "-", "UPI", "401.20"},
                {"INV-20261005-0005", "05/10/2026 17:10", "Walk-in Customer", "-", "UPI", "59259.60"},
                {"INV-20261005-0004", "05/10/2026 16:45", "Walk-in Customer", "-", "UPI", "59778.80"},
                {"INV-20261005-0003", "05/10/2026 16:42", "Walk-in Customer", "-", "CASH", "377.60"},
                {"INV-20261005-0002", "05/10/2026 16:41", "Walk-in Customer", "-", "UPI", "59401.20"},
                {"INV-20261005-0001", "05/10/2026 13:58", "Walk-in Customer", "-", "UPI", "118424.80"},
                {"INV-20261004-0002", "05/10/2026 02:18", "Walk-in Customer", "-", "UPI", "118493.24"},
                {"INV-20261004-0001", "05/10/2026 02:17", "Walk-in Customer", "-", "UPI", "177047.20"},
                {"INV-20261003-0001", "04/10/2026 02:25", "Paras", "9876543210", "UPI", "23.60"}
        };

        double totalRev = 0;
        double totalCash = 0;
        double totalOnline = 0;

        for (int i = 0; i < demoData.length; i++) {
            String[] d = demoData[i];
            double amt = Double.parseDouble(d[5]);
            totalRev += amt;
            if ("CASH".equalsIgnoreCase(d[4])) totalCash += amt;
            else totalOnline += amt;

            Sale s = new Sale();
            s.setId(i + 1);
            s.setInvoiceNo(d[0]);
            s.setCustomerName(d[2]);
            s.setCustomerPhone(d[3]);
            s.setPaymentMode(d[4]);
            s.setTotalAmount(amt);
            s.setSubtotal(amt / 1.18);
            s.setGstAmount(amt - (amt / 1.18));
            s.setGstRate(18.0);
            s.setCashierName("System Administrator");

            List<model.SaleItem> items = new ArrayList<>();
            items.add(new model.SaleItem(1, "ITM-01", "Retail Merchandise Item " + (i + 1), 1, amt / 1.18, amt / 1.18));
            s.setItems(items);

            currentSalesList.add(s);

            model.addRow(new Object[]{
                    i + 1,
                    d[0],
                    d[1],
                    d[2],
                    d[3],
                    d[4],
                    AppTheme.formatCurrency(amt),
                    "👁️ Details"
            });
        }

        lblTotalBillsVal.setText(currentSalesList.size() + " Bills");
        lblTotalRevenueVal.setText(AppTheme.formatCurrency(totalRev));
        lblTotalCashVal.setText(AppTheme.formatCurrency(totalCash));
        lblTotalOnlineVal.setText(AppTheme.formatCurrency(totalOnline));
        lblTableStatus.setText("Showing " + currentSalesList.size() + " bill(s) | Total Amount: " + AppTheme.formatCurrency(totalRev));
    }

    private void viewSelectedBill() {
        int r = table.getSelectedRow();
        if (r == -1) {
            JOptionPane.showMessageDialog(this, "Please select an order from the table to view details.", "Select Order", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        viewBillAt(r);
    }

    private void viewBillAt(int row) {
        if (row < 0 || row >= currentSalesList.size()) return;
        Sale s = currentSalesList.get(row);
        try {
            Sale fullSale = billingDAO.getSaleByInvoice(s.getInvoiceNo());
            if (fullSale == null) fullSale = s;

            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            BillDetailDialog dlg = new BillDetailDialog(owner, fullSale);
            dlg.setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching bill details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // CUSTOM FILTER CONTROLS & COMPONENTS
    // =========================================================================

    /**
     * ModernSearchField - Crisp rounded search input with magnifying glass prefix.
     */
    private static class ModernSearchField extends JTextField {
        private final String placeholder;
        private boolean focused = false;

        public ModernSearchField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
            setFont(AppTheme.font(Font.PLAIN, 12));
            setOpaque(false);
            setBorder(new EmptyBorder(7, 30, 7, 10));
            setForeground(AppTheme.TEXT_PRIMARY);

            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int arc = 16;

            // Background
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            // Border
            if (focused) {
                g2.setColor(AppTheme.FOREST_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            } else {
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }

            // Magnifying glass icon
            g2.setColor(focused ? AppTheme.FOREST_GREEN : AppTheme.TEXT_MUTED);
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = 13, cy = h / 2 - 1, r = 4;
            g2.drawOval(cx - r, cy - r, r * 2, r * 2);
            g2.drawLine(cx + 3, cy + 3, cx + 7, cy + 7);

            super.paintComponent(g);

            // Placeholder text
            if (getText().isEmpty() && !focused) {
                g2.setColor(AppTheme.TEXT_MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(placeholder, 30, ty);
            }

            g2.dispose();
        }
    }

    /**
     * ModernDateField - Crisp rounded date input with calendar prefix.
     */
    private static class ModernDateField extends JTextField {
        private final String placeholder;
        private boolean focused = false;

        public ModernDateField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
            setFont(AppTheme.font(Font.PLAIN, 12));
            setOpaque(false);
            setBorder(new EmptyBorder(7, 26, 7, 8));
            setForeground(AppTheme.TEXT_PRIMARY);

            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int arc = 16;

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            if (focused) {
                g2.setColor(AppTheme.FOREST_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            } else {
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }

            // Calendar icon
            g2.setColor(focused ? AppTheme.FOREST_GREEN : AppTheme.TEXT_MUTED);
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = 9, cy = h / 2 - 5;
            g2.drawRoundRect(cx, cy, 11, 10, 2, 2);
            g2.drawLine(cx, cy + 3, cx + 11, cy + 3);

            super.paintComponent(g);

            if (getText().isEmpty() && !focused) {
                g2.setColor(AppTheme.TEXT_MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(placeholder, 26, ty);
            }

            g2.dispose();
        }
    }

    /**
     * DateFilterPillButton - Stadium capsule button for quick date filtering.
     */
    private static class DateFilterPillButton extends JButton {
        private boolean active = false;
        private boolean hovered = false;

        public DateFilterPillButton(String text) {
            super(text);
            setFont(AppTheme.font(Font.BOLD, 11));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(6, 12, 6, 12));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        public void setActive(boolean a) {
            this.active = a;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int r = h;

            if (active) {
                g2.setColor(AppTheme.FOREST_GREEN);
                g2.fillRoundRect(0, 0, w, h, r, r);
                setForeground(Color.WHITE);
            } else if (hovered) {
                g2.setColor(new Color(232, 246, 238));
                g2.fillRoundRect(0, 0, w, h, r, r);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                setForeground(AppTheme.FOREST_GREEN);
            } else {
                g2.setColor(new Color(244, 250, 246));
                g2.fillRoundRect(0, 0, w, h, r, r);
                g2.setColor(new Color(218, 236, 224));
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                setForeground(AppTheme.TEXT_SECONDARY);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // =========================================================================
    // PAYMENT MODE PILL BADGE RENDERER
    // =========================================================================
    private static class PaymentBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String mode = value != null ? value.toString().toUpperCase() : "CASH";
            return new PaymentPillComponent(mode);
        }
    }

    private static class PaymentPillComponent extends JComponent {
        private final String mode;

        public PaymentPillComponent(String mode) {
            this.mode = mode;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int pillH = 24;
            int pillW = Math.min(w - 16, 78);
            int px = (w - pillW) / 2;
            int py = (h - pillH) / 2;

            Color bg, border, text;
            if (mode.contains("UPI")) {
                bg = new Color(244, 235, 255);
                border = new Color(216, 180, 254);
                text = new Color(126, 34, 206);
            } else if (mode.contains("CARD")) {
                bg = new Color(239, 246, 255);
                border = new Color(191, 219, 254);
                text = new Color(29, 78, 216);
            } else if (mode.contains("CREDIT")) {
                bg = new Color(254, 243, 199);
                border = new Color(253, 230, 138);
                text = new Color(180, 83, 9);
            } else { // CASH
                bg = new Color(236, 253, 243);
                border = new Color(167, 243, 208);
                text = new Color(2, 122, 72);
            }

            g2.setColor(bg);
            g2.fillRoundRect(px, py, pillW, pillH, pillH, pillH);
            g2.setColor(border);
            g2.drawRoundRect(px, py, pillW - 1, pillH - 1, pillH, pillH);

            g2.setColor(text);
            g2.setFont(AppTheme.font(Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            int tx = px + (pillW - fm.stringWidth(mode)) / 2;
            int ty = py + ((pillH - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(mode, tx, ty);

            g2.dispose();
        }
    }

    // =========================================================================
    // ACTION BUTTON RENDERER & EDITOR FOR JTABLE
    // =========================================================================
    private static class ActionButtonRenderer extends JButton implements TableCellRenderer {
        public ActionButtonRenderer() {
            super("👁️ Details");
            setFont(AppTheme.font(Font.BOLD, 11));
            setForeground(AppTheme.FOREST_GREEN);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int btnW = Math.min(w - 16, 104);
            int btnH = 28;
            int btnX = (w - btnW) / 2;
            int btnY = (h - btnH) / 2;
            int arc = btnH;

            // Soft mint pill button
            g2.setColor(new Color(240, 250, 244));
            g2.fillRoundRect(btnX, btnY, btnW, btnH, arc, arc);

            g2.setColor(AppTheme.BORDER_SAGE);
            g2.drawRoundRect(btnX, btnY, btnW - 1, btnH - 1, arc, arc);

            g2.setColor(AppTheme.FOREST_GREEN);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String text = "👁️ Details";
            int tx = btnX + (btnW - fm.stringWidth(text)) / 2;
            int ty = btnY + ((btnH - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(text, tx, ty);

            g2.dispose();
        }
    }

    private class ActionButtonEditor extends DefaultCellEditor {
        private final JButton btn;
        private int clickedRow;

        public ActionButtonEditor() {
            super(new JCheckBox());
            btn = new JButton("👁️ Details") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    int btnW = Math.min(w - 16, 104);
                    int btnH = 28;
                    int btnX = (w - btnW) / 2;
                    int btnY = (h - btnH) / 2;
                    int arc = btnH;

                    // Active / Clicked Forest Green State
                    g2.setColor(AppTheme.FOREST_GREEN);
                    g2.fillRoundRect(btnX, btnY, btnW, btnH, arc, arc);

                    g2.setColor(Color.WHITE);
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    String text = "👁️ Details";
                    int tx = btnX + (btnW - fm.stringWidth(text)) / 2;
                    int ty = btnY + ((btnH - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(text, tx, ty);
                    g2.dispose();
                }
            };
            btn.setFont(AppTheme.font(Font.BOLD, 11));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setOpaque(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                fireEditingStopped();
                viewBillAt(clickedRow);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.clickedRow = row;
            return btn;
        }

        @Override
        public Object getCellEditorValue() {
            return "👁️ Details";
        }
    }
}
