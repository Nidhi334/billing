package ui;

import dao.ProductDAO;
import dao.PurchaseDAO;
import dao.ReportDAO;
import dao.SupplierDAO;
import model.Product;
import model.Supplier;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class InventoryPanel extends JPanel {
    private ProductDAO productDAO = new ProductDAO();
    private SupplierDAO supplierDAO = new SupplierDAO();
    private PurchaseDAO purchaseDAO = new PurchaseDAO();
    private ReportDAO reportDAO = new ReportDAO();

    private JTable lowStockTable, historyTable;
    private DefaultTableModel lowStockModel, historyModel;
    private JComboBox<Product> cmbProducts;
    private JComboBox<Supplier> cmbSuppliers;
    private JTextField txtQty, txtCostPrice;
    private JButton btnAddStock, btnRefresh;

    public InventoryPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(248, 250, 252));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        initComponents();
        loadDropdownData();
        refreshData();
    }

    private void initComponents() {
        // Top Banner
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(248, 250, 252));

        JLabel title = new JLabel("📊 Inventory & Stock Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(30, 41, 59));
        topPanel.add(title, BorderLayout.WEST);

        btnRefresh = new JButton("Refresh Inventory");
        topPanel.add(btnRefresh, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Center split: Stock-In Form on top, Tabs (Low Stock Alerts & Transaction History) below
        JPanel centerPanel = new JPanel(new BorderLayout(12, 12));
        centerPanel.setBackground(new Color(248, 250, 252));

        // Card: Stock-In (Purchase Quick Entry)
        JPanel stockInCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        stockInCard.setBackground(Color.WHITE);
        stockInCard.setBorder(BorderFactory.createTitledBorder("📦 Quick Stock-In / Purchase Entry (Supplier → Purchase → Stock Increases)"));

        stockInCard.add(new JLabel("Product:"));
        cmbProducts = new JComboBox<>();
        cmbProducts.setPreferredSize(new Dimension(200, 32));
        stockInCard.add(cmbProducts);

        stockInCard.add(new JLabel("Supplier:"));
        cmbSuppliers = new JComboBox<>();
        cmbSuppliers.setPreferredSize(new Dimension(200, 32));
        stockInCard.add(cmbSuppliers);

        stockInCard.add(new JLabel("Quantity:"));
        txtQty = new JTextField(6);
        stockInCard.add(txtQty);

        stockInCard.add(new JLabel("Unit Cost (₹):"));
        txtCostPrice = new JTextField(8);
        stockInCard.add(txtCostPrice);

        btnAddStock = new JButton("+ Add to Stock");
        btnAddStock.setBackground(new Color(16, 185, 129));
        btnAddStock.setForeground(Color.WHITE);
        btnAddStock.setFont(new Font("Segoe UI", Font.BOLD, 12));
        stockInCard.add(btnAddStock);

        centerPanel.add(stockInCard, BorderLayout.NORTH);

        // Tabs: Low Stock Alert vs Stock Movement Log
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: Low Stock Products
        JPanel lowStockPanel = new JPanel(new BorderLayout());
        String[] lowCols = {"Product Code", "Product Name", "Category", "Current Stock", "Min Alert Level", "Unit Sell Price"};
        lowStockModel = new DefaultTableModel(lowCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        lowStockTable = new JTable(lowStockModel);
        lowStockTable.setRowHeight(26);

        // Highlight stock
        lowStockTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setForeground(new Color(220, 38, 38));
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                return lbl;
            }
        });

        lowStockPanel.add(new JScrollPane(lowStockTable), BorderLayout.CENTER);
        tabs.addTab("⚠️ Low-Stock & Out-of-Stock Alerts", lowStockPanel);

        // Tab 2: Stock Transactions Log
        JPanel historyPanel = new JPanel(new BorderLayout());
        String[] histCols = {"Date & Time", "Product Code", "Product Name", "Type", "Qty", "Ref / Invoice", "Notes"};
        historyModel = new DefaultTableModel(histCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        historyTable.setRowHeight(24);

        // Format Type column (IN vs OUT)
        historyTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String t = String.valueOf(value);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                if ("IN".equalsIgnoreCase(t)) {
                    lbl.setForeground(new Color(16, 185, 129));
                    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                } else if ("OUT".equalsIgnoreCase(t)) {
                    lbl.setForeground(new Color(220, 38, 38));
                    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                }
                return lbl;
            }
        });

        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        tabs.addTab("📋 Stock Movement History (Audit Log)", historyPanel);

        centerPanel.add(tabs, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Listeners
        btnAddStock.addActionListener(e -> processStockIn());
        btnRefresh.addActionListener(e -> {
            loadDropdownData();
            refreshData();
        });
        cmbProducts.addActionListener(e -> {
            Product p = (Product) cmbProducts.getSelectedItem();
            if (p != null) {
                txtCostPrice.setText(String.format("%.2f", p.getPurchasePrice()));
            }
        });
    }

    public void loadDropdownData() {
        try {
            cmbProducts.removeAllItems();
            List<Product> products = productDAO.getAllProducts();
            for (Product p : products) {
                cmbProducts.addItem(p);
            }

            cmbSuppliers.removeAllItems();
            List<Supplier> suppliers = supplierDAO.getAllSuppliers();
            for (Supplier s : suppliers) {
                cmbSuppliers.addItem(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshData() {
        // 1. Load Low stock
        try {
            lowStockModel.setRowCount(0);
            List<Product> lowStock = productDAO.getLowStockProducts();
            for (Product p : lowStock) {
                lowStockModel.addRow(new Object[]{
                        p.getCode(),
                        p.getName(),
                        p.getCategoryName() != null ? p.getCategoryName() : "General",
                        p.getQuantity(),
                        p.getMinStockLevel(),
                        String.format("₹%.2f", p.getSellingPrice())
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. Load History
        try {
            historyModel.setRowCount(0);
            List<String[]> list = reportDAO.getStockHistory(100);
            for (String[] row : list) {
                historyModel.addRow(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processStockIn() {
        Product selectedProd = (Product) cmbProducts.getSelectedItem();
        Supplier selectedSupp = (Supplier) cmbSuppliers.getSelectedItem();
        String qtyStr = txtQty.getText().trim();
        String costStr = txtCostPrice.getText().trim();

        if (selectedProd == null || qtyStr.isEmpty() || costStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a product, quantity, and cost price.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int qty = Integer.parseInt(qtyStr);
            double cost = Double.parseDouble(costStr);
            if (qty <= 0 || cost < 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be positive and cost valid.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String invoiceNo = purchaseDAO.generateNextPurchaseInvoiceNo();
            int supplierId = selectedSupp != null ? selectedSupp.getId() : 0;

            boolean success = purchaseDAO.recordPurchase(invoiceNo, supplierId, selectedProd.getId(), qty, cost);
            if (success) {
                JOptionPane.showMessageDialog(this, "Stock added successfully! Invoice: " + invoiceNo, "Success", JOptionPane.INFORMATION_MESSAGE);
                txtQty.setText("");
                refreshData();
                loadDropdownData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update stock.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number format for Quantity or Cost.", "Validation", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

