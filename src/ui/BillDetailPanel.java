package ui;

import dao.BillingDAO;
import model.Sale;
import model.SaleItem;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Modern Bill & Order Details Panel.
 * Shows complete itemized bill breakdown, customer details, payment info, tax ledger, and print options.
 */
public class BillDetailPanel extends JPanel {
    private final Window parentWindow;
    private final Sale sale;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public BillDetailPanel(Window parent, Sale sale) {
        this.parentWindow = parent;
        this.sale = sale;

        // Ensure full item details are loaded
        if (sale != null && (sale.getItems() == null || sale.getItems().isEmpty())) {
            try {
                BillingDAO billingDAO = new BillingDAO();
                Sale fullSale = billingDAO.getSaleByInvoice(sale.getInvoiceNo());
                if (fullSale != null && fullSale.getItems() != null) {
                    sale.setItems(fullSale.getItems());
                }
            } catch (Exception ignored) {}
        }

        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(248, 250, 252));
        setBorder(new EmptyBorder(18, 20, 16, 20));

        initComponents();
    }

    private void initComponents() {
        // 1. TOP HEADER & METADATA CARDS
        JPanel topContainer = new JPanel(new BorderLayout(0, 12));
        topContainer.setOpaque(false);

        // Header Title Row
        JPanel titleRow = new JPanel(new BorderLayout(10, 0));
        titleRow.setOpaque(false);

        JPanel invInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        invInfo.setOpaque(false);

        JLabel lblInv = new JLabel(sale != null ? sale.getInvoiceNo() : "INV-UNKNOWN");
        lblInv.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblInv.setForeground(new Color(15, 23, 42));

        JLabel lblPaidBadge = new JLabel("✓ COMPLETED & PAID");
        lblPaidBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblPaidBadge.setForeground(new Color(16, 185, 129));
        lblPaidBadge.setBackground(new Color(236, 253, 245));
        lblPaidBadge.setOpaque(true);
        lblPaidBadge.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(167, 243, 208), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));

        invInfo.add(lblInv);
        invInfo.add(lblPaidBadge);

        JLabel lblDateStr = new JLabel(sale != null && sale.getSaleDate() != null ? "Billed on: " + sdf.format(sale.getSaleDate()) : "");
        lblDateStr.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDateStr.setForeground(new Color(100, 116, 139));

        titleRow.add(invInfo, BorderLayout.WEST);
        titleRow.add(lblDateStr, BorderLayout.EAST);
        topContainer.add(titleRow, BorderLayout.NORTH);

        // 4 Metadata Cards Grid
        JPanel metaGrid = new JPanel(new GridLayout(1, 4, 10, 0));
        metaGrid.setOpaque(false);

        String custName = (sale != null && sale.getCustomerName() != null) ? sale.getCustomerName() : "Walk-in Customer";
        String custPhone = (sale != null && sale.getCustomerPhone() != null && !sale.getCustomerPhone().isEmpty()) ? sale.getCustomerPhone() : "Not provided";
        String payMode = (sale != null && sale.getPaymentMode() != null) ? sale.getPaymentMode() : "CASH";
        String cashier = (sale != null && sale.getCashierName() != null) ? sale.getCashierName() : "Admin";

        int totalItems = (sale != null && sale.getItems() != null) ? sale.getItems().size() : (sale != null ? sale.getItemCount() : 0);
        int totalUnits = 0;
        if (sale != null && sale.getItems() != null) {
            for (SaleItem it : sale.getItems()) totalUnits += it.getQuantity();
        } else if (sale != null) {
            totalUnits = sale.getTotalUnits();
        }

        metaGrid.add(createMetaCard("CUSTOMER NAME", custName, "📞 " + custPhone, new Color(37, 99, 235), new Color(239, 246, 255)));
        metaGrid.add(createMetaCard("PAYMENT MODE", payMode, "Status: Verified", new Color(22, 163, 74), new Color(240, 253, 244)));
        metaGrid.add(createMetaCard("ITEMS & QUANTITY", totalItems + " Unique Items", totalUnits + " Total Units", new Color(147, 51, 234), new Color(250, 245, 255)));
        metaGrid.add(createMetaCard("CASHIER / DESK", cashier, "SmartBilling Station", new Color(217, 119, 6), new Color(254, 243, 199)));

        topContainer.add(metaGrid, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        // 2. CENTER: ITEMIZED PRODUCTS TABLE
        JPanel tablePanel = new JPanel(new BorderLayout(0, 8));
        tablePanel.setOpaque(false);

        JLabel lblItemsTitle = new JLabel("🛒 Itemized Products Purchased in this Order:");
        lblItemsTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblItemsTitle.setForeground(new Color(30, 41, 59));
        tablePanel.add(lblItemsTitle, BorderLayout.NORTH);

        String[] cols = {"#", "Product / Item Name", "Item Code / SKU", "Unit Price (₹)", "Qty", "Item Subtotal (₹)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        if (sale != null && sale.getItems() != null) {
            List<SaleItem> items = sale.getItems();
            for (int i = 0; i < items.size(); i++) {
                SaleItem item = items.get(i);
                String code = item.getProductCode() != null ? item.getProductCode() : "-";
                model.addRow(new Object[]{
                        i + 1,
                        item.getProductName(),
                        code,
                        String.format("₹%.2f", item.getUnitPrice()),
                        item.getQuantity(),
                        String.format("₹%.2f", item.getSubtotal())
                });
            }
        }

        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(241, 245, 249));
        table.getTableHeader().setForeground(new Color(30, 41, 59));
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(226, 232, 240));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(60);
        table.getColumnModel().getColumn(5).setPreferredWidth(120);

        // Align amounts right
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));
        tablePanel.add(scroll, BorderLayout.CENTER);

        add(tablePanel, BorderLayout.CENTER);

        // 3. BOTTOM: FINANCIAL SUMMARY & ACTION BUTTONS
        JPanel bottomContainer = new JPanel(new BorderLayout(0, 12));
        bottomContainer.setOpaque(false);

        // Financial Breakdown Card
        JPanel summaryCard = new JPanel(new BorderLayout(14, 0));
        summaryCard.setBackground(Color.WHITE);
        summaryCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(12, 18, 12, 18)
        ));

        // Left note
        JPanel leftNote = new JPanel(new GridLayout(2, 1, 0, 2));
        leftNote.setOpaque(false);
        JLabel lblGstNote = new JLabel(String.format("Tax Breakdown: GST (%.1f%%)", sale != null ? sale.getGstRate() : 18.0));
        lblGstNote.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblGstNote.setForeground(new Color(100, 116, 139));

        JLabel lblVerified = new JLabel("🔒 Electronically Generated Tax Invoice • Verified & Settled");
        lblVerified.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblVerified.setForeground(new Color(148, 163, 184));
        leftNote.add(lblGstNote);
        leftNote.add(lblVerified);

        // Right totals
        JPanel rightTotals = new JPanel(new GridLayout(3, 2, 16, 4));
        rightTotals.setOpaque(false);

        double subtotal = sale != null ? sale.getSubtotal() : 0.0;
        double gstAmt = sale != null ? sale.getGstAmount() : 0.0;
        double grandTotal = sale != null ? sale.getTotalAmount() : 0.0;

        rightTotals.add(createTotalLabel("Subtotal (Taxable):", false, Color.GRAY));
        rightTotals.add(createTotalValue(String.format("₹%.2f", subtotal), false, new Color(30, 41, 59)));

        rightTotals.add(createTotalLabel("GST Amount:", false, Color.GRAY));
        rightTotals.add(createTotalValue(String.format("₹%.2f", gstAmt), false, new Color(30, 41, 59)));

        rightTotals.add(createTotalLabel("Grand Total:", true, new Color(15, 23, 42)));
        rightTotals.add(createTotalValue(String.format("₹%.2f", grandTotal), true, new Color(22, 163, 74)));

        summaryCard.add(leftNote, BorderLayout.WEST);
        summaryCard.add(rightTotals, BorderLayout.EAST);
        bottomContainer.add(summaryCard, BorderLayout.NORTH);

        // Bottom Action Buttons
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionRow.setOpaque(false);

        JButton btnPrintSlip = new JButton("🖨️ Print Receipt Slip");
        btnPrintSlip.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPrintSlip.setBackground(new Color(37, 99, 235));
        btnPrintSlip.setForeground(Color.WHITE);
        btnPrintSlip.setFocusPainted(false);
        btnPrintSlip.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrintSlip.addActionListener(e -> {
            Frame f = parentWindow instanceof Frame ? (Frame) parentWindow : null;
            InvoiceDialog dlg = new InvoiceDialog(f, sale);
            dlg.setVisible(true);
        });

        JButton btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.setBackground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> {
            if (parentWindow != null) {
                parentWindow.dispose();
            }
        });

        actionRow.add(btnPrintSlip);
        actionRow.add(btnClose);
        bottomContainer.add(actionRow, BorderLayout.SOUTH);

        add(bottomContainer, BorderLayout.SOUTH);
    }

    private JPanel createMetaCard(String title, String mainVal, String subVal, Color accent, Color bg) {
        JPanel p = new JPanel(new GridLayout(3, 1, 0, 2));
        p.setBackground(bg);
        p.setBorder(new CompoundBorder(
                new LineBorder(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70), 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));

        JLabel l1 = new JLabel(title);
        l1.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l1.setForeground(accent.darker());

        JLabel l2 = new JLabel(mainVal);
        l2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l2.setForeground(new Color(15, 23, 42));

        JLabel l3 = new JLabel(subVal);
        l3.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l3.setForeground(new Color(100, 116, 139));

        p.add(l1);
        p.add(l2);
        p.add(l3);
        return p;
    }

    private JLabel createTotalLabel(String text, boolean bold, Color color) {
        JLabel l = new JLabel(text, SwingConstants.RIGHT);
        l.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, bold ? 14 : 12));
        l.setForeground(color);
        return l;
    }

    private JLabel createTotalValue(String text, boolean bold, Color color) {
        JLabel l = new JLabel(text, SwingConstants.RIGHT);
        l.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, bold ? 16 : 12));
        l.setForeground(color);
        return l;
    }
}
