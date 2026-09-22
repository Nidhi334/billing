package ui;

import dao.CategoryDAO;
import dao.ProductDAO;
import model.Category;
import model.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ProductPanel extends JPanel {
    private ProductDAO productDAO = new ProductDAO();
    private CategoryDAO categoryDAO = new CategoryDAO();

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtCode, txtName, txtPurchasePrice, txtSellingPrice, txtQty, txtMinStock, txtSearch;
    private JComboBox<Category> cmbCategory;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnManageCategories;
    private int selectedProductId = -1;

    public ProductPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(248, 250, 252));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        initComponents();
        loadCategories();
        loadProductTable();
    }

    private void initComponents() {
        // TOP: Title & Search bar
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(new Color(248, 250, 252));

        JLabel lblTitle = new JLabel("📦 Product Management");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(30, 41, 59));
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setBackground(new Color(248, 250, 252));
        searchPanel.add(new JLabel("Search:"));
        txtSearch = new JTextField(18);
        searchPanel.add(txtSearch);
        JButton btnSearch = new JButton("Find");
        searchPanel.add(btnSearch);
        JButton btnRefresh = new JButton("Refresh");
        searchPanel.add(btnRefresh);
        topPanel.add(searchPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // LEFT: Form Card
        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));
        formCard.setPreferredSize(new Dimension(340, 0));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        // Code
        g.gridx = 0; g.gridy = 0; formCard.add(new JLabel("Product Code:"), g);
        txtCode = new JTextField();
        g.gridy = 1; formCard.add(txtCode, g);

        // Name
        g.gridy = 2; formCard.add(new JLabel("Product Name:"), g);
        txtName = new JTextField();
        g.gridy = 3; formCard.add(txtName, g);

        // Category
        g.gridy = 4; formCard.add(new JLabel("Category:"), g);
        cmbCategory = new JComboBox<>();
        g.gridy = 5; formCard.add(cmbCategory, g);

        btnManageCategories = new JButton("+ Manage Categories");
        btnManageCategories.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g.gridy = 6; formCard.add(btnManageCategories, g);

        // Purchase Price & Selling Price
        g.gridy = 7; formCard.add(new JLabel("Purchase Price (₹):"), g);
        txtPurchasePrice = new JTextField();
        g.gridy = 8; formCard.add(txtPurchasePrice, g);

        g.gridy = 9; formCard.add(new JLabel("Selling Price (₹):"), g);
        txtSellingPrice = new JTextField();
        g.gridy = 10; formCard.add(txtSellingPrice, g);

        // Quantity & Min Stock
        g.gridy = 11; formCard.add(new JLabel("Initial Quantity:"), g);
        txtQty = new JTextField();
        g.gridy = 12; formCard.add(txtQty, g);

        g.gridy = 13; formCard.add(new JLabel("Min Stock Alert Level:"), g);
        txtMinStock = new JTextField("5");
        g.gridy = 14; formCard.add(txtMinStock, g);

        // Action Buttons Grid
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setBackground(Color.WHITE);
        btnAdd = new JButton("Add");
        btnAdd.setBackground(new Color(16, 185, 129));
        btnAdd.setForeground(Color.WHITE);

        btnUpdate = new JButton("Update");
        btnUpdate.setBackground(new Color(37, 99, 235));
        btnUpdate.setForeground(Color.WHITE);

        btnDelete = new JButton("Delete");
        btnDelete.setBackground(new Color(239, 68, 68));
        btnDelete.setForeground(Color.WHITE);

        btnClear = new JButton("Clear");

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);

        g.gridy = 15;
        g.insets = new Insets(15, 6, 6, 6);
        formCard.add(btnPanel, g);

        add(formCard, BorderLayout.WEST);

        // CENTER: Products Table
        String[] cols = {"ID", "Code", "Name", "Category", "Buy Price", "Sell Price", "Stock", "Min Stock", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom status renderer to highlight low stock
        table.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                String val = String.valueOf(value);
                if ("OUT OF STOCK".equals(val)) {
                    label.setForeground(new Color(220, 38, 38));
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                } else if ("LOW STOCK".equals(val)) {
                    label.setForeground(new Color(217, 119, 6));
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                } else {
                    label.setForeground(new Color(22, 163, 74));
                }
                return label;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        add(scrollPane, BorderLayout.CENTER);

        // Listeners
        btnAdd.addActionListener(e -> addProduct());
        btnUpdate.addActionListener(e -> updateProduct());
        btnDelete.addActionListener(e -> deleteProduct());
        btnClear.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> searchProducts());
        txtSearch.addActionListener(e -> searchProducts());
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            loadProductTable();
        });
        btnManageCategories.addActionListener(e -> manageCategories());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int r = table.getSelectedRow();
                selectedProductId = Integer.parseInt(tableModel.getValueAt(r, 0).toString());
                txtCode.setText(tableModel.getValueAt(r, 1).toString());
                txtName.setText(tableModel.getValueAt(r, 2).toString());
                String catName = tableModel.getValueAt(r, 3).toString();
                for (int i = 0; i < cmbCategory.getItemCount(); i++) {
                    if (cmbCategory.getItemAt(i).getName().equalsIgnoreCase(catName)) {
                        cmbCategory.setSelectedIndex(i);
                        break;
                    }
                }
                txtPurchasePrice.setText(tableModel.getValueAt(r, 4).toString().replace("₹", "").trim());
                txtSellingPrice.setText(tableModel.getValueAt(r, 5).toString().replace("₹", "").trim());
                txtQty.setText(tableModel.getValueAt(r, 6).toString());
                txtMinStock.setText(tableModel.getValueAt(r, 7).toString());
            }
        });
    }

    public void loadCategories() {
        try {
            cmbCategory.removeAllItems();
            List<Category> categories = categoryDAO.getAllCategories();
            for (Category c : categories) {
                cmbCategory.addItem(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadProductTable() {
        try {
            tableModel.setRowCount(0);
            List<Product> products = productDAO.getAllProducts();
            for (Product p : products) {
                String status = p.isOutOfStock() ? "OUT OF STOCK" : (p.isLowStock() ? "LOW STOCK" : "IN STOCK");
                tableModel.addRow(new Object[]{
                        p.getId(),
                        p.getCode(),
                        p.getName(),
                        p.getCategoryName() != null ? p.getCategoryName() : "General",
                        String.format("₹%.2f", p.getPurchasePrice()),
                        String.format("₹%.2f", p.getSellingPrice()),
                        p.getQuantity(),
                        p.getMinStockLevel(),
                        status
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to load products: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchProducts() {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) {
            loadProductTable();
            return;
        }
        try {
            tableModel.setRowCount(0);
            List<Product> products = productDAO.searchProducts(query);
            for (Product p : products) {
                String status = p.isOutOfStock() ? "OUT OF STOCK" : (p.isLowStock() ? "LOW STOCK" : "IN STOCK");
                tableModel.addRow(new Object[]{
                        p.getId(),
                        p.getCode(),
                        p.getName(),
                        p.getCategoryName() != null ? p.getCategoryName() : "General",
                        String.format("₹%.2f", p.getPurchasePrice()),
                        String.format("₹%.2f", p.getSellingPrice()),
                        p.getQuantity(),
                        p.getMinStockLevel(),
                        status
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Search error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addProduct() {
        try {
            Product p = validateAndBuildProduct();
            if (p == null) return;

            if (productDAO.addProduct(p)) {
                JOptionPane.showMessageDialog(this, "Product added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadProductTable();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add product.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding product: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateProduct() {
        if (selectedProductId <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a product from the table first.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            Product p = validateAndBuildProduct();
            if (p == null) return;
            p.setId(selectedProductId);

            if (productDAO.updateProduct(p)) {
                JOptionPane.showMessageDialog(this, "Product updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadProductTable();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update product.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating product: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteProduct() {
        if (selectedProductId <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a product from the table to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this product?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (productDAO.deleteProduct(selectedProductId)) {
                    JOptionPane.showMessageDialog(this, "Product deleted successfully!", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                    clearForm();
                    loadProductTable();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not delete product.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Delete failed (it may be referenced by existing sales/purchases): " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private Product validateAndBuildProduct() {
        String code = txtCode.getText().trim();
        String name = txtName.getText().trim();
        String buyStr = txtPurchasePrice.getText().trim();
        String sellStr = txtSellingPrice.getText().trim();
        String qtyStr = txtQty.getText().trim();
        String minStr = txtMinStock.getText().trim();

        if (code.isEmpty() || name.isEmpty() || buyStr.isEmpty() || sellStr.isEmpty() || qtyStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all mandatory fields.", "Validation", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        try {
            double buy = Double.parseDouble(buyStr);
            double sell = Double.parseDouble(sellStr);
            int qty = Integer.parseInt(qtyStr);
            int minStock = minStr.isEmpty() ? 5 : Integer.parseInt(minStr);

            Category cat = (Category) cmbCategory.getSelectedItem();
            int catId = cat != null ? cat.getId() : 0;

            return new Product(0, code, name, catId, buy, sell, qty, minStock);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Price and Quantity must be valid numbers.", "Validation", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private void clearForm() {
        selectedProductId = -1;
        txtCode.setText("");
        txtName.setText("");
        txtPurchasePrice.setText("");
        txtSellingPrice.setText("");
        txtQty.setText("");
        txtMinStock.setText("5");
        table.clearSelection();
    }

    private void manageCategories() {
        CategoryDialog dlg = new CategoryDialog((Frame) SwingUtilities.getWindowAncestor(this));
        dlg.setVisible(true);
        loadCategories();
        loadProductTable();
    }
}

