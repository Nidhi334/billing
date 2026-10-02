package ui;

import dao.BillingDAO;
import model.Sale;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BillHistoryPanel extends JPanel {
    private BillingDAO billingDAO = new BillingDAO();
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

    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

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

        JButton btnViewPrint = new JButton("🖨️ View & Print Bill");
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

        // 4. Main Bills Table
        String[] cols = {
                "#", "Invoice No", "Date & Time", "Customer Name", "Customer Phone",
                "Mode", "Items", "Qty", "Subtotal (₹)", "GST (₹)", "Total Amount (₹)", "Cashier"
        };
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(new Color(30, 41, 59));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(226, 232, 240));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(35);   // #
        table.getColumnModel().getColumn(1).setPreferredWidth(140);  // Inv
        table.getColumnModel().getColumn(2).setPreferredWidth(125);  // Date
        table.getColumnModel().getColumn(3).setPreferredWidth(140);  // Cust
        table.getColumnModel().getColumn(4).setPreferredWidth(100);  // Phone
        table.getColumnModel().getColumn(5).setPreferredWidth(70);   // Mode
        table.getColumnModel().getColumn(6).setPreferredWidth(50);   // Items
        table.getColumnModel().getColumn(7).setPreferredWidth(45);   // Qty
        table.getColumnModel().getColumn(8).setPreferredWidth(90);   // Subtotal
        table.getColumnModel().getColumn(9).setPreferredWidth(75);   // GST
        table.getColumnModel().getColumn(10).setPreferredWidth(110); // Total
        table.getColumnModel().getColumn(11).setPreferredWidth(90);  // Cashier

        // Renderers: align amounts right
        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(8).setCellRenderer(rightRender);
        table.getColumnModel().getColumn(9).setCellRenderer(rightRender);

        // Grand total column bold colored
        DefaultTableCellRenderer totalRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.RIGHT);
                l.setFont(new Font("Segoe UI", Font.BOLD, 12));
                if (!sel) l.setForeground(new Color(22, 101, 52));
                return l;
            }
        };
        table.getColumnModel().getColumn(10).setCellRenderer(totalRender);

        // Double click to view/print bill
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewSelectedBill();
                }
            }
        });

        // Key Enter to view/print
        table.registerKeyboardAction(
                e -> viewSelectedBill(),
                KeyStroke.getKeyStroke("ENTER"),
                JComponent.WHEN_FOCUSED
        );

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        add(scrollPane, BorderLayout.CENTER);

        // 5. Bottom Status Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        lblTableStatus = new JLabel("Loading orders...");
        lblTableStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTableStatus.setForeground(new Color(71, 85, 105));

        JLabel lblHint = new JLabel("💡 Tip: Double-click any row to view full bill details & re-print receipt");
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
                String cashier = s.getCashierName() != null ? s.getCashierName() : "Admin";

                model.addRow(new Object[]{
                        i + 1,
                        s.getInvoiceNo(),
                        dateStr,
                        cust,
                        phone,
                        s.getPaymentMode() != null ? s.getPaymentMode() : "CASH",
                        s.getItemCount(),
                        s.getTotalUnits(),
                        String.format("₹%.2f", s.getSubtotal()),
                        String.format("₹%.2f", s.getGstAmount()),
                        String.format("₹%.2f", total),
                        cashier
                });
            }

            // Update KPI cards
            lblTotalBillsVal.setText(currentSalesList.size() + " Bills");
            lblTotalRevenueVal.setText(String.format("₹%.2f", totalRev));
            lblTotalCashVal.setText(String.format("₹%.2f", totalCash));
            lblTotalOnlineVal.setText(String.format("₹%.2f", totalOnline));

            lblTableStatus.setText("Showing " + currentSalesList.size() + " bill(s) | Total Amount: " + String.format("₹%.2f", totalRev));

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading bill history: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewSelectedBill() {
        int r = table.getSelectedRow();
        if (r == -1) {
            JOptionPane.showMessageDialog(this, "Please select a bill from the table to view or print.", "Select Bill", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String invNo = (String) model.getValueAt(r, 1);
        try {
            Sale fullSale = billingDAO.getSaleByInvoice(invNo);
            if (fullSale == null) {
                JOptionPane.showMessageDialog(this, "Could not load invoice data for: " + invNo, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            InvoiceDialog dlg = new InvoiceDialog(owner, fullSale);
            dlg.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error fetching bill details: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
