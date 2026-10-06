package ui;

import dao.BillingDAO;
import model.Sale;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BillHistoryPanel extends JPanel {
    private final BillingDAO billingDAO = new BillingDAO();
    private User currentUser;

    private JTable table;
    private DefaultTableModel model;
    private List<Sale> currentSalesList = new ArrayList<>();

    // Filters
    private JTextField txtSearch;
    private JTextField txtFromDate;
    private JTextField txtToDate;
    private JComboBox<String> cmbPaymentMode;

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
        setLayout(new BorderLayout(12, 12));
        setBackground(new Color(248, 250, 252));
        setBorder(new EmptyBorder(16, 18, 16, 18));

        initUI();
        loadBillHistory();
    }

    private void initUI() {
        // TOP: Header + Action buttons
        JPanel topContainer = new JPanel(new BorderLayout(10, 10));
        topContainer.setOpaque(false);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        JLabel lblTitle = new JLabel("📜 Total Billing & Orders History");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblSub = new JLabel("Complete ledger of all customer bills, sales history, search, and reprint receipts");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(100, 116, 139));
        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSub, BorderLayout.SOUTH);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topActions.setOpaque(false);

        JButton btnViewPrint = new JButton("👁️ View Bill Details");
        btnViewPrint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnViewPrint.setBackground(new Color(37, 99, 235));
        btnViewPrint.setForeground(Color.WHITE);
        btnViewPrint.setFocusPainted(false);
        btnViewPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewPrint.addActionListener(e -> viewSelectedBill());

        JButton btnRefresh = new JButton("🔄 Refresh");
        btnRefresh.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefresh.setBackground(Color.WHITE);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefresh.addActionListener(e -> loadBillHistory());

        topActions.add(btnViewPrint);
        topActions.add(btnRefresh);

        topContainer.add(headerPanel, BorderLayout.WEST);
        topContainer.add(topActions, BorderLayout.EAST);

        // 2. KPI Summary Cards
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiGrid.setOpaque(false);

        kpiGrid.add(createKpiCard("TOTAL ORDERS / BILLS", "0 Bills", new Color(37, 99, 235), new Color(239, 246, 255), (lbl) -> lblTotalBillsVal = lbl));
        kpiGrid.add(createKpiCard("TOTAL REVENUE", "₹0.00", new Color(22, 163, 74), new Color(240, 253, 244), (lbl) -> lblTotalRevenueVal = lbl));
        kpiGrid.add(createKpiCard("CASH COLLECTED", "₹0.00", new Color(217, 119, 6), new Color(254, 243, 199), (lbl) -> lblTotalCashVal = lbl));
        kpiGrid.add(createKpiCard("UPI / ONLINE / CARD", "₹0.00", new Color(147, 51, 234), new Color(250, 245, 255), (lbl) -> lblTotalOnlineVal = lbl));

        JPanel northPanel = new JPanel(new BorderLayout(0, 12));
        northPanel.setOpaque(false);
        northPanel.add(topContainer, BorderLayout.NORTH);
        northPanel.add(kpiGrid, BorderLayout.CENTER);

        // 3. Filter Bar
        JPanel filterCard = new JPanel(new BorderLayout(8, 8));
        filterCard.setBackground(Color.WHITE);
        filterCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterRow.setOpaque(false);

        filterRow.add(new JLabel("Search:"));
        txtSearch = new JTextField(14);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtSearch.setToolTipText("Search by invoice no, customer name, mobile, or cashier");
        txtSearch.addActionListener(e -> loadBillHistory());
        filterRow.add(txtSearch);

        filterRow.add(new JLabel("From:"));
        txtFromDate = new JTextField(9);
        txtFromDate.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtFromDate.setToolTipText("YYYY-MM-DD");
        filterRow.add(txtFromDate);

        filterRow.add(new JLabel("To:"));
        txtToDate = new JTextField(9);
        txtToDate.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtToDate.setToolTipText("YYYY-MM-DD");
        filterRow.add(txtToDate);

        filterRow.add(new JLabel("Payment:"));
        cmbPaymentMode = new JComboBox<>(new String[]{"ALL", "CASH", "UPI", "CARD", "CREDIT"});
        cmbPaymentMode.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbPaymentMode.addActionListener(e -> loadBillHistory());
        filterRow.add(cmbPaymentMode);

        JButton btnFilter = new JButton("Filter");
        btnFilter.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnFilter.setBackground(new Color(37, 99, 235));
        btnFilter.setForeground(Color.WHITE);
        btnFilter.addActionListener(e -> loadBillHistory());
        filterRow.add(btnFilter);

        // Quick date shortcut buttons
        JButton btnToday = new JButton("Today");
        btnToday.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnToday.addActionListener(e -> {
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            txtFromDate.setText(today);
            txtToDate.setText(today);
            loadBillHistory();
        });
        filterRow.add(btnToday);

        JButton btnYesterday = new JButton("Yesterday");
        btnYesterday.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnYesterday.addActionListener(e -> {
            String yest = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
            txtFromDate.setText(yest);
            txtToDate.setText(yest);
            loadBillHistory();
        });
        filterRow.add(btnYesterday);

        JButton btnThisMonth = new JButton("This Month");
        btnThisMonth.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnThisMonth.addActionListener(e -> {
            LocalDate now = LocalDate.now();
            txtFromDate.setText(now.withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
            txtToDate.setText(now.format(DateTimeFormatter.ISO_LOCAL_DATE));
            loadBillHistory();
        });
        filterRow.add(btnThisMonth);

        JButton btnReset = new JButton("All Time");
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            txtFromDate.setText("");
            txtToDate.setText("");
            cmbPaymentMode.setSelectedIndex(0);
            loadBillHistory();
        });
        filterRow.add(btnReset);

        filterCard.add(filterRow, BorderLayout.CENTER);
        northPanel.add(filterCard, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);

        // 4. Main Clean Bills Table (8 Streamlined Columns with Interactive View Button)
        String[] cols = {
                "#", "Invoice No", "Date & Time", "Customer Name", "Customer Phone",
                "Payment Mode", "Total Amount (₹)", "Action"
        };
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 7; // Only Action column is editable/clickable
            }
        };

        table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(new Color(30, 41, 59));
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(241, 245, 249));
        table.setShowVerticalLines(false);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(45);   // #
        table.getColumnModel().getColumn(1).setPreferredWidth(170);  // Inv No
        table.getColumnModel().getColumn(2).setPreferredWidth(135);  // Date
        table.getColumnModel().getColumn(3).setPreferredWidth(160);  // Customer
        table.getColumnModel().getColumn(4).setPreferredWidth(120);  // Phone
        table.getColumnModel().getColumn(5).setPreferredWidth(105);  // Mode
        table.getColumnModel().getColumn(6).setPreferredWidth(130);  // Total Amount
        table.getColumnModel().getColumn(7).setPreferredWidth(130);  // Action Button

        // Alignments & Styles
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRender);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRender);

        // Invoice No Column renderer (bold blue link style)
        DefaultTableCellRenderer invRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setFont(new Font("Segoe UI", Font.BOLD, 12));
                if (!sel) l.setForeground(new Color(29, 78, 216));
                return l;
            }
        };
        table.getColumnModel().getColumn(1).setCellRenderer(invRender);

        // Payment Mode badge renderer
        DefaultTableCellRenderer modeRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String m = val != null ? val.toString().toUpperCase() : "CASH";
                if (!sel) {
                    if (m.contains("UPI")) {
                        l.setForeground(new Color(126, 34, 206)); // Purple
                    } else if (m.contains("CARD")) {
                        l.setForeground(new Color(37, 99, 235)); // Blue
                    } else {
                        l.setForeground(new Color(22, 101, 52)); // Green
                    }
                }
                return l;
            }
        };
        table.getColumnModel().getColumn(5).setCellRenderer(modeRender);

        // Grand total column bold colored
        DefaultTableCellRenderer totalRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setFont(new Font("Segoe UI", Font.BOLD, 13));
                if (!sel) l.setForeground(new Color(22, 101, 52));
                return l;
            }
        };
        table.getColumnModel().getColumn(6).setCellRenderer(totalRender);

        // ACTION COLUMN: Interactive "👁️ View Details" Button Renderer & Editor
        table.getColumnModel().getColumn(7).setCellRenderer(new ActionButtonRenderer());
        table.getColumnModel().getColumn(7).setCellEditor(new ActionButtonEditor());

        // Mouse listeners: Click on Action col or double-click anywhere to view bill
        table.addMouseListener(new MouseAdapter() {
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

        // Hover hand cursor on Action column
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int c = table.columnAtPoint(e.getPoint());
                if (c == 7) {
                    table.setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    table.setCursor(Cursor.getDefaultCursor());
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
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // 5. Bottom Status Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        lblTableStatus = new JLabel("Loading orders...");
        lblTableStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTableStatus.setForeground(new Color(71, 85, 105));

        JLabel lblHint = new JLabel("💡 Tip: Click '👁️ View Details' button or double-click any row to view full bill details");
        lblHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblHint.setForeground(new Color(100, 116, 139));

        bottomBar.add(lblTableStatus, BorderLayout.WEST);
        bottomBar.add(lblHint, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel createKpiCard(String title, String initialVal, Color accentColor, Color bgColor, java.util.function.Consumer<JLabel> labelConsumer) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTitle.setForeground(accentColor.darker());

        JLabel lblVal = new JLabel(initialVal);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblVal.setForeground(new Color(15, 23, 42));
        labelConsumer.accept(lblVal);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblVal, BorderLayout.CENTER);
        return card;
    }

    public void loadBillHistory() {
        String kw = txtSearch != null ? txtSearch.getText().trim() : "";
        String from = txtFromDate != null ? txtFromDate.getText().trim() : "";
        String to = txtToDate != null ? txtToDate.getText().trim() : "";
        String mode = cmbPaymentMode != null ? (String) cmbPaymentMode.getSelectedItem() : "ALL";

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
                        String.format("₹%.2f", total),
                        "👁️ View Details"
                });
            }

            // Update KPI cards
            lblTotalBillsVal.setText(currentSalesList.size() + " Bills");
            lblTotalRevenueVal.setText(String.format("₹%.2f", totalRev));
            lblTotalCashVal.setText(String.format("₹%.2f", totalCash));
            lblTotalOnlineVal.setText(String.format("₹%.2f", totalOnline));

            lblTableStatus.setText("Showing " + currentSalesList.size() + " bill(s) | Total Amount: " + String.format("₹%.2f", totalRev));

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
                    String.format("₹%.2f", amt),
                    "👁️ View Details"
            });
        }

        lblTotalBillsVal.setText(currentSalesList.size() + " Bills");
        lblTotalRevenueVal.setText(String.format("₹%.2f", totalRev));
        lblTotalCashVal.setText(String.format("₹%.2f", totalCash));
        lblTotalOnlineVal.setText(String.format("₹%.2f", totalOnline));
        lblTableStatus.setText("Showing " + currentSalesList.size() + " bill(s) | Total Amount: " + String.format("₹%.2f", totalRev));
    }

    private void viewSelectedBill() {
        int r = table.getSelectedRow();
        if (r == -1) {
            JOptionPane.showMessageDialog(this, "Please select a bill from the table to view details.", "Select Bill", JOptionPane.INFORMATION_MESSAGE);
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
    // ACTION BUTTON RENDERER & EDITOR FOR JTABLE
    // =========================================================================
    private static class ActionButtonRenderer extends JButton implements TableCellRenderer {
        public ActionButtonRenderer() {
            super("👁️ View Details");
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setForeground(Color.WHITE);
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

            int w = getWidth();
            int h = getHeight();
            int btnW = Math.min(w - 12, 112);
            int btnH = 26;
            int btnX = (w - btnW) / 2;
            int btnY = (h - btnH) / 2;

            // Pill Button Shape
            g2.setColor(new Color(37, 99, 235)); // Modern Blue
            g2.fillRoundRect(btnX, btnY, btnW, btnH, 8, 8);

            // Subtle border
            g2.setColor(new Color(29, 78, 216));
            g2.drawRoundRect(btnX, btnY, btnW, btnH, 8, 8);

            // Button Text
            g2.setColor(Color.WHITE);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String text = "👁️ View Details";
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
            btn = new JButton("👁️ View Details") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth();
                    int h = getHeight();
                    int btnW = Math.min(w - 12, 112);
                    int btnH = 26;
                    int btnX = (w - btnW) / 2;
                    int btnY = (h - btnH) / 2;
                    g2.setColor(new Color(29, 78, 216));
                    g2.fillRoundRect(btnX, btnY, btnW, btnH, 8, 8);
                    g2.setColor(Color.WHITE);
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    String text = "👁️ View Details";
                    int tx = btnX + (btnW - fm.stringWidth(text)) / 2;
                    int ty = btnY + ((btnH - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(text, tx, ty);
                    g2.dispose();
                }
            };
            btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
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
            return "👁️ View Details";
        }
    }
}
