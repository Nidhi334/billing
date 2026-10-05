package ui;

import dao.CategoryDAO;
import dao.ProductDAO;
import model.Category;
import model.Product;
import util.ProductImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class ProductPanel extends JPanel {
    private ProductDAO productDAO = new ProductDAO();
    private CategoryDAO categoryDAO = new CategoryDAO();

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField txtCode, txtBarcode, txtName, txtPurchasePrice, txtSellingPrice, txtQty, txtMinStock, txtSearch;
    private JLabel lblBarcodePreview;
    private JComboBox<Category> cmbCategory;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnManageCategories;
    private JButton btnViewBarcode, btnPrintBarcode;
    private int selectedProductId = -1;

    // Product Image support
    private String selectedImagePath = null;
    private JLabel lblImagePreview;
    private JButton btnChooseImage, btnRemoveImage;

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

        JLabel lblTitle = new JLabel("📦 Product Management & Barcode Hub");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(30, 41, 59));
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setBackground(new Color(248, 250, 252));
        searchPanel.add(new JLabel("Search (Code/Barcode/Name):"));
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
                new EmptyBorder(12, 14, 12, 14)
        ));
        formCard.setPreferredSize(new Dimension(360, 0));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 5, 4, 5);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        // Code
        g.gridx = 0; g.gridy = 0; formCard.add(new JLabel("Product Code / SKU:"), g);
        txtCode = new JTextField();
        g.gridy = 1; formCard.add(txtCode, g);

        // Barcode (Auto-generate & custom barcode entry)
        JPanel barcodeLabelRow = new JPanel(new BorderLayout());
        barcodeLabelRow.setOpaque(false);
        barcodeLabelRow.add(new JLabel("Barcode (Scan or Type):"), BorderLayout.WEST);

        JButton btnGenBarcode = new JButton("⚡ Auto-Generate");
        btnGenBarcode.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnGenBarcode.setMargin(new Insets(1, 4, 1, 4));
        btnGenBarcode.addActionListener(e -> generateRandomBarcode());
        barcodeLabelRow.add(btnGenBarcode, BorderLayout.EAST);

        g.gridy = 2; formCard.add(barcodeLabelRow, g);

        txtBarcode = new JTextField();
        txtBarcode.setFont(new Font("Monospaced", Font.BOLD, 13));
        txtBarcode.setToolTipText("Enter any custom barcode value (e.g. 8901234567890)");
        g.gridy = 3; formCard.add(txtBarcode, g);

        // Live Barcode Preview Component
        lblBarcodePreview = new JLabel("Barcode Preview", SwingConstants.CENTER);
        lblBarcodePreview.setPreferredSize(new Dimension(320, 60));
        lblBarcodePreview.setOpaque(true);
        lblBarcodePreview.setBackground(new Color(248, 250, 252));
        lblBarcodePreview.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        g.gridy = 4; formCard.add(lblBarcodePreview, g);

        // Name
        g.gridy = 5; formCard.add(new JLabel("Product Name:"), g);
        txtName = new JTextField();
        g.gridy = 6; formCard.add(txtName, g);

        // Category
        g.gridy = 7; formCard.add(new JLabel("Category:"), g);
        cmbCategory = new JComboBox<>();
        g.gridy = 8; formCard.add(cmbCategory, g);

        btnManageCategories = new JButton("+ Manage Categories");
        btnManageCategories.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g.gridy = 9; formCard.add(btnManageCategories, g);

        // Price Row (Buy & Sell)
        JPanel priceRow = new JPanel(new GridLayout(1, 2, 8, 0));
        priceRow.setOpaque(false);
        JPanel pBuy = new JPanel(new BorderLayout(2, 2));
        pBuy.setOpaque(false);
        pBuy.add(new JLabel("Buy Price (₹):"), BorderLayout.NORTH);
        txtPurchasePrice = new JTextField();
        pBuy.add(txtPurchasePrice, BorderLayout.CENTER);

        JPanel pSell = new JPanel(new BorderLayout(2, 2));
        pSell.setOpaque(false);
        pSell.add(new JLabel("Sell Price (₹):"), BorderLayout.NORTH);
        txtSellingPrice = new JTextField();
        pSell.add(txtSellingPrice, BorderLayout.CENTER);

        priceRow.add(pBuy);
        priceRow.add(pSell);
        g.gridy = 10; formCard.add(priceRow, g);

        // Stock Row (Initial Qty & Min Stock)
        JPanel stockRow = new JPanel(new GridLayout(1, 2, 8, 0));
        stockRow.setOpaque(false);
        JPanel pQty = new JPanel(new BorderLayout(2, 2));
        pQty.setOpaque(false);
        pQty.add(new JLabel("Stock Quantity:"), BorderLayout.NORTH);
        txtQty = new JTextField();
        pQty.add(txtQty, BorderLayout.CENTER);

        JPanel pMin = new JPanel(new BorderLayout(2, 2));
        pMin.setOpaque(false);
        pMin.add(new JLabel("Min Alert Level:"), BorderLayout.NORTH);
        txtMinStock = new JTextField("5");
        pMin.add(txtMinStock, BorderLayout.CENTER);

        stockRow.add(pQty);
        stockRow.add(pMin);
        g.gridy = 11; formCard.add(stockRow, g);

        // Product Image Row (Preview + Choose / Remove buttons)
        JPanel imageSection = new JPanel(new BorderLayout(10, 6));
        imageSection.setOpaque(false);
        imageSection.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                "🖼️ Product Image",
                0, 0,
                new Font("Segoe UI", Font.BOLD, 11),
                new Color(71, 85, 105)
        ));

        lblImagePreview = new JLabel();
        lblImagePreview.setPreferredSize(new Dimension(80, 56));
        lblImagePreview.setOpaque(true);
        lblImagePreview.setBackground(new Color(248, 250, 252));
        lblImagePreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagePreview.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        imageSection.add(lblImagePreview, BorderLayout.WEST);

        JPanel imageBtnStack = new JPanel(new GridLayout(2, 1, 0, 4));
        imageBtnStack.setOpaque(false);

        btnChooseImage = new JButton("📁 Choose Image...");
        btnChooseImage.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnChooseImage.setMargin(new Insets(2, 6, 2, 6));
        btnChooseImage.addActionListener(e -> chooseProductImage());

        btnRemoveImage = new JButton("✖ Remove Image");
        btnRemoveImage.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnRemoveImage.setMargin(new Insets(2, 6, 2, 6));
        btnRemoveImage.setForeground(new Color(220, 38, 38));
        btnRemoveImage.setEnabled(false);
        btnRemoveImage.addActionListener(e -> {
            selectedImagePath = null;
            updateImagePreview();
        });

        imageBtnStack.add(btnChooseImage);
        imageBtnStack.add(btnRemoveImage);
        imageSection.add(imageBtnStack, BorderLayout.CENTER);

        g.gridy = 12;
        formCard.add(imageSection, g);

        // Action Buttons Grid
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        btnPanel.setBackground(Color.WHITE);
        btnAdd = new JButton("Add Product");
        btnAdd.setBackground(new Color(16, 185, 129));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnUpdate = new JButton("Update");
        btnUpdate.setBackground(new Color(37, 99, 235));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnDelete = new JButton("Delete");
        btnDelete.setBackground(new Color(239, 68, 68));
        btnDelete.setForeground(Color.WHITE);

        btnClear = new JButton("Clear Form");

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);

        g.gridy = 13;
        g.insets = new Insets(10, 5, 4, 5);
        formCard.add(btnPanel, g);

        // Dedicated Barcode Tools Buttons (View & Print)
        JPanel barcodeToolsRow = new JPanel(new GridLayout(1, 2, 6, 0));
        barcodeToolsRow.setOpaque(false);

        btnViewBarcode = new JButton("🔍 View Barcode");
        btnViewBarcode.setBackground(new Color(79, 70, 229));
        btnViewBarcode.setForeground(Color.WHITE);
        btnViewBarcode.setFont(new Font("Segoe UI", Font.BOLD, 11));

        btnPrintBarcode = new JButton("🖨️ Print Label");
        btnPrintBarcode.setBackground(new Color(13, 148, 136));
        btnPrintBarcode.setForeground(Color.WHITE);
        btnPrintBarcode.setFont(new Font("Segoe UI", Font.BOLD, 11));

        barcodeToolsRow.add(btnViewBarcode);
        barcodeToolsRow.add(btnPrintBarcode);

        g.gridy = 14;
        formCard.add(barcodeToolsRow, g);

        JScrollPane formScroll = new JScrollPane(formCard);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(14);
        add(formScroll, BorderLayout.WEST);

        // CENTER: Products Table (Added Barcode column)
        String[] cols = {"ID", "Code", "Barcode", "Name", "Category", "Buy Price", "Sell Price", "Stock", "Min Stock", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Custom status renderer to highlight low stock
        table.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
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

        // Document Listener on txtBarcode to automatically update live barcode image
        txtBarcode.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateLiveBarcodePreview(); }
            public void removeUpdate(DocumentEvent e) { updateLiveBarcodePreview(); }
            public void changedUpdate(DocumentEvent e) { updateLiveBarcodePreview(); }
        });

        txtCode.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { syncCodeToBarcode(); }
            public void removeUpdate(DocumentEvent e) { syncCodeToBarcode(); }
            public void changedUpdate(DocumentEvent e) { syncCodeToBarcode(); }
        });

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

        btnViewBarcode.addActionListener(e -> viewSelectedBarcode());
        btnPrintBarcode.addActionListener(e -> printSelectedBarcode());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int r = table.getSelectedRow();
                selectedProductId = Integer.parseInt(tableModel.getValueAt(r, 0).toString());
                txtCode.setText(tableModel.getValueAt(r, 1).toString());
                txtBarcode.setText(tableModel.getValueAt(r, 2).toString());
                txtName.setText(tableModel.getValueAt(r, 3).toString());
                String catName = tableModel.getValueAt(r, 4).toString();
                for (int i = 0; i < cmbCategory.getItemCount(); i++) {
                    if (cmbCategory.getItemAt(i).getName().equalsIgnoreCase(catName)) {
                        cmbCategory.setSelectedIndex(i);
                        break;
                    }
                }
                txtPurchasePrice.setText(tableModel.getValueAt(r, 5).toString().replace("₹", "").trim());
                txtSellingPrice.setText(tableModel.getValueAt(r, 6).toString().replace("₹", "").trim());
                txtQty.setText(tableModel.getValueAt(r, 7).toString());
                txtMinStock.setText(tableModel.getValueAt(r, 8).toString());

                try {
                    Product p = productDAO.getProductById(selectedProductId);
                    selectedImagePath = (p != null) ? p.getImagePath() : null;
                } catch (Exception ex) {
                    selectedImagePath = null;
                }
                updateImagePreview();
                updateLiveBarcodePreview();
            }
        });

        cmbCategory.addActionListener(e -> {
            if (selectedImagePath == null) updateImagePreview();
        });

        txtName.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { if (selectedImagePath == null) updateImagePreview(); }
            public void removeUpdate(DocumentEvent e) { if (selectedImagePath == null) updateImagePreview(); }
            public void changedUpdate(DocumentEvent e) { if (selectedImagePath == null) updateImagePreview(); }
        });

        updateImagePreview();
    }

    private void syncCodeToBarcode() {
        if (selectedProductId <= 0 && txtBarcode.getText().trim().isEmpty()) {
            txtBarcode.setText(txtCode.getText().trim());
        }
    }

    private void generateRandomBarcode() {
        // Generates clean standard 12-digit UPC/EAN barcode
        long ts = System.currentTimeMillis() % 10000000000L;
        String val = String.format("890%09d", ts % 1000000000L);
        txtBarcode.setText(val);
        updateLiveBarcodePreview();
    }

    private void updateLiveBarcodePreview() {
        String val = txtBarcode.getText().trim();
        if (val.isEmpty()) {
            val = txtCode.getText().trim();
        }
        if (val.isEmpty()) {
            lblBarcodePreview.setIcon(null);
            lblBarcodePreview.setText("Enter barcode above to generate");
            return;
        }

        try {
            BufferedImage img = BarcodeUtil.generateBarcodeImage(val, 320, 56, true);
            lblBarcodePreview.setText("");
            lblBarcodePreview.setIcon(new ImageIcon(img));
        } catch (Exception ex) {
            lblBarcodePreview.setIcon(null);
            lblBarcodePreview.setText("Error generating barcode");
        }
    }

    private void viewSelectedBarcode() {
        Product p = getSelectedOrBuiltProduct();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Please select a product from the table or fill in barcode/code.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        BarcodeUtil.showBarcodeDialog(owner, p);
    }

    private void printSelectedBarcode() {
        Product p = getSelectedOrBuiltProduct();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Please select a product from the table or fill in barcode/code.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        BufferedImage img = BarcodeUtil.generateBarcodeImage(p.getBarcode(), 300, 100, true);
        BarcodeUtil.printBarcodeLabel(p, img, this);
    }

    private Product getSelectedOrBuiltProduct() {
        if (selectedProductId > 0) {
            try {
                return productDAO.getProductByCode(txtCode.getText().trim());
            } catch (Exception ignored) {}
        }
        return validateAndBuildProduct();
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
                        p.getBarcode(),
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
                        p.getBarcode(),
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
                JOptionPane.showMessageDialog(this, "Product added successfully with generated barcode!", "Success", JOptionPane.INFORMATION_MESSAGE);
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
                JOptionPane.showMessageDialog(this, "Product and barcode updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
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
        String barcode = txtBarcode.getText().trim();
        if (barcode.isEmpty()) barcode = code;
        String name = txtName.getText().trim();
        String buyStr = txtPurchasePrice.getText().trim();
        String sellStr = txtSellingPrice.getText().trim();
        String qtyStr = txtQty.getText().trim();
        String minStr = txtMinStock.getText().trim();

        if (code.isEmpty() || name.isEmpty() || buyStr.isEmpty() || sellStr.isEmpty() || qtyStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all mandatory fields (Code, Name, Prices, Qty).", "Validation", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        try {
            double buy = Double.parseDouble(buyStr);
            double sell = Double.parseDouble(sellStr);
            int qty = Integer.parseInt(qtyStr);
            int minStock = minStr.isEmpty() ? 5 : Integer.parseInt(minStr);

            Category cat = (Category) cmbCategory.getSelectedItem();
            int catId = cat != null ? cat.getId() : 0;

            return new Product(0, code, barcode, name, catId, buy, sell, qty, minStock, selectedImagePath);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Price and Quantity must be valid numbers.", "Validation", JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private void clearForm() {
        selectedProductId = -1;
        selectedImagePath = null;
        txtCode.setText("");
        txtBarcode.setText("");
        lblBarcodePreview.setIcon(null);
        lblBarcodePreview.setText("Barcode Preview");
        txtName.setText("");
        txtPurchasePrice.setText("");
        txtSellingPrice.setText("");
        txtQty.setText("");
        txtMinStock.setText("5");
        updateImagePreview();
        table.clearSelection();
    }

    private void chooseProductImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Product Image");
        chooser.setFileFilter(new FileNameExtensionFilter("Image Files (*.jpg, *.png, *.webp, *.jpeg)", "jpg", "jpeg", "png", "webp"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String code = txtCode.getText().trim();
            if (code.isEmpty()) {
                code = "prod_" + System.currentTimeMillis();
            }
            String savedPath = ProductImageUtil.saveProductImage(file, code);
            if (savedPath != null) {
                selectedImagePath = savedPath;
                updateImagePreview();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save product image.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateImagePreview() {
        String catName = cmbCategory.getSelectedItem() != null ? cmbCategory.getSelectedItem().toString() : "";
        String prodName = txtName != null ? txtName.getText().trim() : "";
        ImageIcon icon = ProductImageUtil.getProductIcon(selectedImagePath, catName, prodName, 76, 52);
        if (lblImagePreview != null) {
            lblImagePreview.setIcon(icon);
            lblImagePreview.setText("");
        }
        if (btnRemoveImage != null) {
            btnRemoveImage.setEnabled(selectedImagePath != null && !selectedImagePath.trim().isEmpty());
        }
    }

    private void manageCategories() {
        CategoryDialog dlg = new CategoryDialog((Frame) SwingUtilities.getWindowAncestor(this));
        dlg.setVisible(true);
        loadCategories();
        loadProductTable();
    }
}
