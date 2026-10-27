package ui;

import model.Product;
import dao.ProductDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class BarcodeScannerDialog extends JDialog {
    private BillingPanel billingPanel;
    private ProductDAO productDAO = new ProductDAO();
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> filteredProducts = new ArrayList<>();

    private JTextField txtScanInput;
    private JTextField txtSearch;
    private JSpinner spinQty;
    private JCheckBox chkAutoClose;
    private JLabel lblStatus;
    private JLabel lblBarcodeImg;
    private JLabel lblSelectedInfo;

    private DefaultTableModel tableModel;
    private JTable productTable;

    public BarcodeScannerDialog(Frame owner, BillingPanel billingPanel) {
        super(owner, "🏷️ Barcode Scanner & Quick Product Add", true);
        this.billingPanel = billingPanel;

        setSize(780, 560);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(241, 245, 249));

        loadProducts();
        initComponents();
        setupShortcuts();
    }

    private void loadProducts() {
        try {
            allProducts = productDAO.getAllProducts();
        } catch (Exception e) {
            allProducts = new ArrayList<>();
        }
        filteredProducts = new ArrayList<>(allProducts);
    }

    private void initComponents() {
        // TOP: Fast Scanner Bar
        JPanel topContainer = new JPanel(new BorderLayout(8, 8));
        topContainer.setBorder(new EmptyBorder(12, 14, 6, 14));
        topContainer.setOpaque(false);

        // Header info
        JPanel headerTextPanel = new JPanel(new BorderLayout());
        headerTextPanel.setOpaque(false);
        JLabel lblTitle = new JLabel("⚡ Product Barcode Scanner (Ready for Scan / Type)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(new Color(30, 41, 59));
        JLabel lblSubtitle = new JLabel("Point your barcode scanner here or enter barcode/product code to add directly to billing cart.");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(100, 116, 139));
        headerTextPanel.add(lblTitle, BorderLayout.NORTH);
        headerTextPanel.add(lblSubtitle, BorderLayout.SOUTH);
        topContainer.add(headerTextPanel, BorderLayout.NORTH);

        // Scan Input Row
        JPanel scanRow = new JPanel(new BorderLayout(8, 0));
        scanRow.setBackground(Color.WHITE);
        scanRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(37, 99, 235), 2, true),
                new EmptyBorder(6, 10, 6, 10)
        ));

        JLabel lblScanIcon = new JLabel("📷 BARCODE: ");
        lblScanIcon.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblScanIcon.setForeground(new Color(37, 99, 235));
        scanRow.add(lblScanIcon, BorderLayout.WEST);

        txtScanInput = new JTextField();
        txtScanInput.setFont(new Font("Segoe UI", Font.BOLD, 15));
        txtScanInput.setToolTipText("Scan barcode or type value and press Enter");
        scanRow.add(txtScanInput, BorderLayout.CENTER);

        JPanel scanRightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        scanRightActions.setOpaque(false);

        JLabel lblQty = new JLabel("Qty:");
        lblQty.setFont(new Font("Segoe UI", Font.BOLD, 12));
        spinQty = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        spinQty.setPreferredSize(new Dimension(55, 28));

        JButton btnScanAdd = new JButton("⚡ Scan & Add ↵");
        btnScanAdd.setBackground(new Color(16, 185, 129));
        btnScanAdd.setForeground(Color.WHITE);
        btnScanAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnScanAdd.setFocusPainted(false);

        chkAutoClose = new JCheckBox("Close after add", false);
        chkAutoClose.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        chkAutoClose.setOpaque(false);

        scanRightActions.add(lblQty);
        scanRightActions.add(spinQty);
        scanRightActions.add(btnScanAdd);
        scanRightActions.add(chkAutoClose);
        scanRow.add(scanRightActions, BorderLayout.EAST);

        topContainer.add(scanRow, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        // CENTER: Product Table & Barcode Preview
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.68);
        splitPane.setBorder(new EmptyBorder(0, 14, 0, 14));
        splitPane.setOpaque(false);

        // Left: Search & Table
        JPanel leftTablePanel = new JPanel(new BorderLayout(6, 6));
        leftTablePanel.setOpaque(false);

        JPanel searchRow = new JPanel(new BorderLayout(6, 0));
        searchRow.setOpaque(false);
        JLabel lblSearch = new JLabel("Filter Products: ");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtSearch = new JTextField();
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchRow.add(lblSearch, BorderLayout.WEST);
        searchRow.add(txtSearch, BorderLayout.CENTER);
        leftTablePanel.add(searchRow, BorderLayout.NORTH);

        String[] cols = {"#", "Barcode", "Code", "Product Name", "Price (₹)", "Stock"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        productTable = new JTable(tableModel);
        productTable.setRowHeight(28);
        productTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        productTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        productTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        productTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        productTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        productTable.getColumnModel().getColumn(3).setPreferredWidth(160);
        productTable.getColumnModel().getColumn(4).setPreferredWidth(75);
        productTable.getColumnModel().getColumn(5).setPreferredWidth(55);

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        productTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        productTable.getColumnModel().getColumn(5).setCellRenderer(centerRender);

        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
        productTable.getColumnModel().getColumn(4).setCellRenderer(rightRender);

        refreshTableRows();
        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        leftTablePanel.add(scrollPane, BorderLayout.CENTER);
        splitPane.setLeftComponent(leftTablePanel);

        // Right: Selected Product & Live Barcode Image Preview
        JPanel rightPreviewCard = new JPanel(new BorderLayout(8, 8));
        rightPreviewCard.setBackground(Color.WHITE);
        rightPreviewCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel lblPreviewTitle = new JLabel("Barcode Label Preview", SwingConstants.CENTER);
        lblPreviewTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPreviewTitle.setForeground(new Color(30, 41, 59));
        rightPreviewCard.add(lblPreviewTitle, BorderLayout.NORTH);

        JPanel barcodeCenter = new JPanel(new BorderLayout(6, 6));
        barcodeCenter.setOpaque(false);

        lblBarcodeImg = new JLabel("Select a product to view barcode", SwingConstants.CENTER);
        lblBarcodeImg.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblBarcodeImg.setForeground(new Color(148, 163, 184));
        barcodeCenter.add(lblBarcodeImg, BorderLayout.CENTER);

        lblSelectedInfo = new JLabel("No product selected", SwingConstants.CENTER);
        lblSelectedInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        barcodeCenter.add(lblSelectedInfo, BorderLayout.SOUTH);
        rightPreviewCard.add(barcodeCenter, BorderLayout.CENTER);

        JButton btnAddSelected = new JButton("⚡ Add Selected to Bill");
        btnAddSelected.setBackground(new Color(37, 99, 235));
        btnAddSelected.setForeground(Color.WHITE);
        btnAddSelected.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAddSelected.setFocusPainted(false);
        rightPreviewCard.add(btnAddSelected, BorderLayout.SOUTH);

        splitPane.setRightComponent(rightPreviewCard);
        add(splitPane, BorderLayout.CENTER);

        // BOTTOM: Status bar & Close buttons
        JPanel bottomBar = new JPanel(new BorderLayout(10, 0));
        bottomBar.setBorder(new EmptyBorder(8, 14, 12, 14));
        bottomBar.setOpaque(false);

        lblStatus = new JLabel("Ready. Scan barcode or pick product from table.");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(new Color(71, 85, 105));
        bottomBar.add(lblStatus, BorderLayout.WEST);

        JPanel bottomActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomActions.setOpaque(false);

        JButton btnPayDirect = new JButton("💳 Pay & Print (Ctrl+Enter)");
        btnPayDirect.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPayDirect.setBackground(new Color(16, 185, 129));
        btnPayDirect.setForeground(Color.WHITE);

        JButton btnClose = new JButton("Close (Esc)");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        bottomActions.add(btnPayDirect);
        bottomActions.add(btnClose);
        bottomBar.add(bottomActions, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);

        // Event listeners
        btnScanAdd.addActionListener(e -> executeBarcodeScan());
        txtScanInput.addActionListener(e -> executeBarcodeScan());

        btnAddSelected.addActionListener(e -> addSelectedTableRow());
        productTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                updateSelectedPreview();
                if (e.getClickCount() == 2) {
                    addSelectedTableRow();
                }
            }
        });
        productTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedPreview();
            }
        });

        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterProducts(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterProducts(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterProducts(); }
        });

        btnPayDirect.addActionListener(e -> {
            dispose();
            billingPanel.triggerCheckout();
        });

        btnClose.addActionListener(e -> dispose());

        // Auto focus scan input when dialog appears
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                txtScanInput.requestFocusInWindow();
            }
            @Override
            public void windowClosed(WindowEvent e) {
                billingPanel.focusBarcodeField();
            }
        });
    }

    private void setupShortcuts() {
        JRootPane rp = getRootPane();
        InputMap im = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rp.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDialog");
        am.put("closeDialog", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "payDirect");
        am.put("payDirect", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dispose();
                billingPanel.triggerCheckout();
            }
        });
    }

    private void filterProducts() {
        String q = txtSearch.getText().trim().toLowerCase();
        filteredProducts.clear();
        for (Product p : allProducts) {
            String b = p.getBarcode() != null ? p.getBarcode().toLowerCase() : "";
            String c = p.getCode() != null ? p.getCode().toLowerCase() : "";
            String n = p.getName() != null ? p.getName().toLowerCase() : "";
            if (b.contains(q) || c.contains(q) || n.contains(q)) {
                filteredProducts.add(p);
            }
        }
        refreshTableRows();
    }

    private void refreshTableRows() {
        tableModel.setRowCount(0);
        for (int i = 0; i < filteredProducts.size(); i++) {
            Product p = filteredProducts.get(i);
            tableModel.addRow(new Object[]{
                    i + 1,
                    p.getBarcode(),
                    p.getCode(),
                    p.getName(),
                    String.format("%.2f", p.getSellingPrice()),
                    p.getQuantity()
            });
        }
        if (productTable.getRowCount() > 0) {
            productTable.setRowSelectionInterval(0, 0);
            updateSelectedPreview();
        }
    }

    private void updateSelectedPreview() {
        int r = productTable.getSelectedRow();
        if (r < 0 || r >= filteredProducts.size()) {
            lblBarcodeImg.setIcon(null);
            lblBarcodeImg.setText("Select a product to view barcode");
            lblSelectedInfo.setText("No product selected");
            return;
        }

        Product p = filteredProducts.get(r);
        String barcodeVal = p.getBarcode() != null && !p.getBarcode().trim().isEmpty() ? p.getBarcode() : p.getCode();
        try {
            BufferedImage img = BarcodeUtil.generateBarcodeImage(barcodeVal, 220, 70, true);
            lblBarcodeImg.setIcon(new ImageIcon(img));
            lblBarcodeImg.setText("");
            lblSelectedInfo.setText("<html><center><b>" + p.getName() + "</b><br>Code: " + p.getCode() + " | Rate: ₹" + String.format("%.2f", p.getSellingPrice()) + " | Stock: " + p.getQuantity() + "</center></html>");
        } catch (Exception ex) {
            lblBarcodeImg.setIcon(null);
            lblBarcodeImg.setText("Barcode: " + barcodeVal);
            lblSelectedInfo.setText(p.getName());
        }
    }

    private void executeBarcodeScan() {
        String val = txtScanInput.getText().trim();
        if (val.isEmpty()) return;

        // Clean scanner noise
        val = val.replaceAll("[\\r\\n\\t]", "").trim();

        int qty = (int) spinQty.getValue();
        if (qty <= 0) qty = 1;

        Product match = null;
        for (Product p : allProducts) {
            if (val.equalsIgnoreCase(p.getBarcode())
                    || val.equalsIgnoreCase(p.getCode())
                    || val.equalsIgnoreCase(p.getName())) {
                match = p;
                break;
            }
        }

        if (match == null) {
            try {
                match = productDAO.getProductByCode(val);
            } catch (Exception ignored) {}
        }

        if (match == null) {
            Toolkit.getDefaultToolkit().beep();
            lblStatus.setText("❌ Product not found for barcode: " + val);
            lblStatus.setForeground(new Color(239, 68, 68));
            txtScanInput.selectAll();
            return;
        }

        // Add to billing cart
        boolean ok = billingPanel.addProductToCart(match, qty);
        if (ok) {
            lblStatus.setText("✓ Added [" + match.getName() + "] x" + qty + " to bill!");
            lblStatus.setForeground(new Color(16, 185, 129));
            txtScanInput.setText("");
            if (chkAutoClose.isSelected()) {
                dispose();
            } else {
                txtScanInput.requestFocusInWindow();
            }
        }
    }

    private void addSelectedTableRow() {
        int r = productTable.getSelectedRow();
        if (r < 0 || r >= filteredProducts.size()) {
            JOptionPane.showMessageDialog(this, "Select a product from the table first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Product p = filteredProducts.get(r);
        int qty = (int) spinQty.getValue();
        if (qty <= 0) qty = 1;

        boolean ok = billingPanel.addProductToCart(p, qty);
        if (ok) {
            lblStatus.setText("✓ Added [" + p.getName() + "] x" + qty + " to bill!");
            lblStatus.setForeground(new Color(16, 185, 129));
            if (chkAutoClose.isSelected()) {
                dispose();
            }
        }
    }
}
