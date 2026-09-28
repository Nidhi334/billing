package ui;

import dao.BillingDAO;
import model.Sale;
import model.SaleItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ProductReturnDialog extends JDialog {
    private JTextField txtInvoiceSearch;
    private JTable itemsTable;
    private DefaultTableModel itemsModel;
    private JSpinner spinReturnQty;
    private JTextField txtReason;
    private JLabel lblRefundAmount;
    private JButton btnProcessReturn;
    private BillingDAO billingDAO = new BillingDAO();
    private Sale currentSale;

    public ProductReturnDialog(Frame owner) {
        super(owner, "🔄 Product Return & Stock Refund", true);
        setSize(720, 520);
        setLocationRelativeTo(owner);
        initComponents();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(15, 15, 15, 15));
        root.setBackground(new Color(248, 250, 252));

        // Top Search Bar for Invoice No
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBackground(Color.WHITE);
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblInv = new JLabel("Invoice Number: ");
        lblInv.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchBar.add(lblInv, BorderLayout.WEST);

        txtInvoiceSearch = new JTextField();
        txtInvoiceSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtInvoiceSearch.setToolTipText("Enter Invoice No (e.g. INV-2026...)");
        searchBar.add(txtInvoiceSearch, BorderLayout.CENTER);

        JButton btnSearch = new JButton("🔍 Find Bill");
        btnSearch.setBackground(new Color(37, 99, 235));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchBar.add(btnSearch, BorderLayout.EAST);

        root.add(searchBar, BorderLayout.NORTH);

        // Center Table of items in this invoice
        String[] cols = {"Item ID", "Product Code", "Product Name", "Sold Qty", "Unit Price (₹)", "Subtotal (₹)"};
        itemsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        itemsTable = new JTable(itemsModel);
        itemsTable.setRowHeight(26);
        itemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setOpaque(false);
        JLabel lblItems = new JLabel("Select Item to Return:");
        lblItems.setFont(new Font("Segoe UI", Font.BOLD, 12));
        centerPanel.add(lblItems, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(itemsTable), BorderLayout.CENTER);

        root.add(centerPanel, BorderLayout.CENTER);

        // Bottom Return Form
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JPanel inputsGrid = new JPanel(new GridLayout(2, 4, 10, 8));
        inputsGrid.setOpaque(false);

        inputsGrid.add(new JLabel("Return Quantity:"));
        spinReturnQty = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        inputsGrid.add(spinReturnQty);

        inputsGrid.add(new JLabel("Calculated Refund:"));
        lblRefundAmount = new JLabel("₹0.00");
        lblRefundAmount.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblRefundAmount.setForeground(new Color(220, 38, 38));
        inputsGrid.add(lblRefundAmount);

        inputsGrid.add(new JLabel("Return Reason:"));
        txtReason = new JTextField("Defective / Customer Exchange");
        inputsGrid.add(txtReason);

        inputsGrid.add(new JLabel("")); // spacer
        inputsGrid.add(new JLabel("")); // spacer

        bottomPanel.add(inputsGrid, BorderLayout.CENTER);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBtns.setOpaque(false);

        btnProcessReturn = new JButton("✅ Approve Return & Restock");
        btnProcessReturn.setBackground(new Color(16, 185, 129));
        btnProcessReturn.setForeground(Color.WHITE);
        btnProcessReturn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnProcessReturn.setEnabled(false);

        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(e -> dispose());

        actionBtns.add(btnProcessReturn);
        actionBtns.add(btnClose);
        bottomPanel.add(actionBtns, BorderLayout.SOUTH);

        root.add(bottomPanel, BorderLayout.SOUTH);

        add(root);

        // Listeners
        btnSearch.addActionListener(e -> searchInvoice());
        txtInvoiceSearch.addActionListener(e -> searchInvoice());

        itemsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                calculateRefund();
            }
        });

        spinReturnQty.addChangeListener(e -> calculateRefund());

        btnProcessReturn.addActionListener(e -> executeReturn());
    }

    private void searchInvoice() {
        String inv = txtInvoiceSearch.getText().trim();
        if (inv.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an invoice number to search.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            currentSale = billingDAO.getSaleByInvoice(inv);
            itemsModel.setRowCount(0);
            if (currentSale == null) {
                JOptionPane.showMessageDialog(this, "No sale record found for invoice: " + inv, "Not Found", JOptionPane.INFORMATION_MESSAGE);
                btnProcessReturn.setEnabled(false);
                return;
            }

            List<SaleItem> items = currentSale.getItems();
            if (items == null || items.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No items found in this invoice.", "Empty Bill", JOptionPane.INFORMATION_MESSAGE);
                btnProcessReturn.setEnabled(false);
                return;
            }

            for (SaleItem it : items) {
                itemsModel.addRow(new Object[]{
                        it.getProductId(),
                        it.getProductId(),
                        it.getProductName(),
                        it.getQuantity(),
                        it.getUnitPrice(),
                        it.getSubtotal()
                });
            }

            if (itemsTable.getRowCount() > 0) {
                itemsTable.setRowSelectionInterval(0, 0);
            }
            btnProcessReturn.setEnabled(true);
            calculateRefund();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error fetching bill: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calculateRefund() {
        int row = itemsTable.getSelectedRow();
        if (row < 0) {
            lblRefundAmount.setText("₹0.00");
            return;
        }

        int soldQty = (int) itemsModel.getValueAt(row, 3);
        double rate = (double) itemsModel.getValueAt(row, 4);

        int returnQty = (int) spinReturnQty.getValue();
        if (returnQty > soldQty) {
            spinReturnQty.setValue(soldQty);
            returnQty = soldQty;
        }

        double refund = returnQty * rate;
        lblRefundAmount.setText(String.format("₹%.2f", refund));
    }

    private void executeReturn() {
        int row = itemsTable.getSelectedRow();
        if (row < 0 || currentSale == null) {
            JOptionPane.showMessageDialog(this, "Please select an item from the table to return.", "Select Item", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int productId = (int) itemsModel.getValueAt(row, 0);
        String prdName = (String) itemsModel.getValueAt(row, 2);
        int returnQty = (int) spinReturnQty.getValue();
        double rate = (double) itemsModel.getValueAt(row, 4);
        double refund = returnQty * rate;
        String reason = txtReason.getText().trim();

                int confirm = JOptionPane.showConfirmDialog(this,
                "Confirm Return: Product: " + prdName + " | Return Qty: " + returnQty + " | Refund: ₹" + String.format("%.2f", refund) + " | Restock: +" + returnQty,
                "Confirm Return & Refund",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = billingDAO.processReturn(currentSale.getInvoiceNo(), productId, returnQty, refund, reason);
            if (ok) {
                JOptionPane.showMessageDialog(this,
                        "Return Processed Successfully! Restocked +" + returnQty + " units. Refund of ₹" + String.format("%.2f", refund) + " completed.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error processing return: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
