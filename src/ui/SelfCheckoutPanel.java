package ui;

import config.AppSettings;
import dao.BillingDAO;
import dao.CategoryDAO;
import dao.CustomerDAO;
import dao.ProductDAO;
import model.*;
import util.ProductImageUtil;
import util.QrCodeGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

/**
 * Self-Checkout Panel for Retail / Supermarket customers.
 * Provides a fast, intuitive, touch-screen friendly self-service billing experience:
 * - USB / Camera Barcode Scanning & Instant Search
 * - Visual Touch Product Catalog with Category Tabs
 * - Interactive Shopping Cart with Large Touch +/- Controls
 * - Dynamic UPI QR Code, Card, and Cash Payment Options
 * - Auto-Receipt Printing & Instant Next Customer Reset
 * - Call Store Assistant Assistance Alert
 */
public class SelfCheckoutPanel extends JPanel {

    private final User checkoutUser;
    private final Runnable exitCallback;

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final BillingDAO billingDAO = new BillingDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();

    // Data models
    private final List<CartItem> cartItems = new ArrayList<>();
    private List<Product> catalogProducts = new ArrayList<>();
    private String selectedCategory = "ALL";

    // UI Components
    private JTextField txtSearchScan;
    private JPanel catalogGridPanel;
    private JPanel categoryTabsPanel;
    private JTable cartTable;
    private DefaultTableModel cartTableModel;
    private JLabel lblCartItemCount;
    private JLabel lblSubtotalValue;
    private JLabel lblGstValue;
    private JLabel lblGrandTotalValue;
    private JButton btnPayNow;
    private JButton btnClearCart;
    private JLabel lblLiveClock;
    private JLabel lblStatusToast;

    // Cart Item Helper Class
    public static class CartItem {
        Product product;
        int quantity;
        double unitPrice;

        public CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
            this.unitPrice = product.getSellingPrice();
        }

        public double getSubtotal() {
            return unitPrice * quantity;
        }
    }

    public SelfCheckoutPanel(User checkoutUser, Runnable exitCallback) {
        this.checkoutUser = (checkoutUser != null) ? checkoutUser : new User(1, "selfcheckout", "", "Self Checkout Express", "STAFF");
        this.exitCallback = exitCallback;

        setLayout(new BorderLayout());
        setBackground(new Color(241, 245, 249));

        initUI();
        loadCategories();
        loadCatalog();

        // Start live clock
        Timer clockTimer = new Timer(1000, e -> updateClock());
        clockTimer.start();
        updateClock();

        // Auto-focus barcode input
        SwingUtilities.invokeLater(this::focusScanInput);
    }

    private void initUI() {
        // 1. Top Header
        add(createHeader(), BorderLayout.NORTH);

        // 2. Main Content Split Pane (Left: Scanning & Catalog, Right: Live Cart & Summary)
        JPanel centerSplit = new JPanel(new BorderLayout(14, 0));
        centerSplit.setOpaque(false);
        centerSplit.setBorder(new EmptyBorder(12, 14, 12, 14));

        centerSplit.add(createCatalogAndScanSection(), BorderLayout.CENTER);
        centerSplit.add(createCartAndCheckoutSection(), BorderLayout.EAST);

        add(centerSplit, BorderLayout.CENTER);
    }

    // ==========================================
    // 1. HEADER
    // ==========================================
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setBackground(new Color(15, 23, 42)); // Dark Slate
        header.setBorder(new EmptyBorder(12, 18, 12, 18));

        // Brand & Subtitle
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandBox.setOpaque(false);

        JLabel lblLogo = new JLabel("⚡");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblLogo.setForeground(new Color(56, 189, 248));

        JPanel titleTextPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titleTextPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("SmartBilling Self-Checkout");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSubtitle = new JLabel("Touch-Screen Express Lane • Scan, Tap & Pay");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(148, 163, 184));

        titleTextPanel.add(lblTitle);
        titleTextPanel.add(lblSubtitle);

        brandBox.add(lblLogo);
        brandBox.add(titleTextPanel);

        // Live Clock & Status Badge
        JPanel centerInfo = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        centerInfo.setOpaque(false);

        JLabel lblCheckoutActive = new JLabel("🟢 SELF CHECKOUT ACTIVE");
        lblCheckoutActive.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCheckoutActive.setForeground(new Color(34, 197, 94));
        lblCheckoutActive.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(34, 197, 94), 1),
                new EmptyBorder(3, 8, 3, 8)
        ));

        lblLiveClock = new JLabel();
        lblLiveClock.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblLiveClock.setForeground(new Color(226, 232, 240));

        centerInfo.add(lblCheckoutActive);
        centerInfo.add(lblLiveClock);

        // Action Buttons: Call Assistant, Fullscreen, Exit / Back
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBox.setOpaque(false);

        JButton btnCallAssistant = createTouchHeaderButton("🛎️ Call Assistant", new Color(245, 158, 11));
        btnCallAssistant.addActionListener(e -> callStoreAssistant());

        JButton btnFullscreen = createTouchHeaderButton("⛶ Fullscreen", new Color(59, 130, 246));
        btnFullscreen.addActionListener(e -> launchFullscreenWindow());

        JButton btnExit = createTouchHeaderButton("❌ Back to Dashboard", new Color(239, 68, 68));
        btnExit.addActionListener(e -> {
            if (!cartItems.isEmpty()) {
                int res = JOptionPane.showConfirmDialog(this,
                        "Cart has items. Are you sure you want to exit self-checkout?",
                        "Exit Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (res != JOptionPane.YES_NO_OPTION) return;
            }
            if (exitCallback != null) exitCallback.run();
        });

        actionBox.add(btnCallAssistant);
        actionBox.add(btnFullscreen);
        actionBox.add(btnExit);

        header.add(brandBox, BorderLayout.WEST);
        header.add(centerInfo, BorderLayout.CENTER);
        header.add(actionBox, BorderLayout.EAST);

        return header;
    }

    private JButton createTouchHeaderButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ==========================================
    // 2. SCANNING & CATALOG SECTION (LEFT 65%)
    // ==========================================
    private JPanel createCatalogAndScanSection() {
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);

        // Top Search & Barcode Bar
        JPanel searchBarWrapper = new JPanel(new BorderLayout(8, 0));
        searchBarWrapper.setBackground(Color.WHITE);
        searchBarWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(8, 14, 8, 14)
        ));

        JLabel lblScanIcon = new JLabel("🔍");
        lblScanIcon.setFont(new Font("Segoe UI", Font.PLAIN, 18));

        txtSearchScan = new JTextField();
        txtSearchScan.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        txtSearchScan.setBorder(null);
        txtSearchScan.putClientProperty("JTextField.placeholderText", "Scan barcode with handheld scanner or type product name/code...");
        txtSearchScan.addActionListener(e -> handleScanOrSearch());

        JButton btnAddBarcode = new JButton("➕ Add Scanned Item");
        btnAddBarcode.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAddBarcode.setBackground(new Color(37, 99, 235));
        btnAddBarcode.setForeground(Color.WHITE);
        btnAddBarcode.setFocusPainted(false);
        btnAddBarcode.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddBarcode.setBorder(new EmptyBorder(8, 16, 8, 16));
        btnAddBarcode.addActionListener(e -> handleScanOrSearch());

        JButton btnCamera = new JButton("📷 Camera Scan");
        btnCamera.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCamera.setBackground(new Color(79, 70, 229));
        btnCamera.setForeground(Color.WHITE);
        btnCamera.setFocusPainted(false);
        btnCamera.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCamera.setBorder(new EmptyBorder(8, 14, 8, 14));
        btnCamera.addActionListener(e -> openCameraBarcodeScanner());

        JButton btnClearSearch = new JButton("✖");
        btnClearSearch.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnClearSearch.setForeground(new Color(148, 163, 184));
        btnClearSearch.setBorder(new EmptyBorder(4, 8, 4, 8));
        btnClearSearch.setContentAreaFilled(false);
        btnClearSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearSearch.addActionListener(e -> {
            txtSearchScan.setText("");
            loadCatalog();
            focusScanInput();
        });

        JPanel rightSearchActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightSearchActions.setOpaque(false);
        rightSearchActions.add(btnClearSearch);
        rightSearchActions.add(btnCamera);
        rightSearchActions.add(btnAddBarcode);

        searchBarWrapper.add(lblScanIcon, BorderLayout.WEST);
        searchBarWrapper.add(txtSearchScan, BorderLayout.CENTER);
        searchBarWrapper.add(rightSearchActions, BorderLayout.EAST);

        // Toast feedback label
        lblStatusToast = new JLabel("Ready: Scan an item barcode or tap any product card below to add to cart.");
        lblStatusToast.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatusToast.setForeground(new Color(13, 148, 136));
        lblStatusToast.setBorder(new EmptyBorder(0, 4, 0, 4));

        JPanel topArea = new JPanel(new BorderLayout(0, 6));
        topArea.setOpaque(false);
        topArea.add(searchBarWrapper, BorderLayout.NORTH);
        topArea.add(lblStatusToast, BorderLayout.SOUTH);

        // Category Sidebar Panel (WEST) - borderless clean floating sidebar
        JPanel categorySidebarCard = new JPanel(new BorderLayout(0, 8));
        categorySidebarCard.setPreferredSize(new Dimension(165, 0));
        categorySidebarCard.setBackground(Color.WHITE);
        categorySidebarCard.setBorder(new EmptyBorder(12, 10, 12, 10));

        JLabel lblCategoryHeader = new JLabel("🏷️ CATEGORIES");
        lblCategoryHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCategoryHeader.setForeground(new Color(100, 116, 139));
        lblCategoryHeader.setBorder(new EmptyBorder(0, 4, 4, 4));
        categorySidebarCard.add(lblCategoryHeader, BorderLayout.NORTH);

        categoryTabsPanel = new JPanel();
        categoryTabsPanel.setLayout(new BoxLayout(categoryTabsPanel, BoxLayout.Y_AXIS));
        categoryTabsPanel.setBackground(Color.WHITE);

        JScrollPane categoryScroll = new JScrollPane(categoryTabsPanel);
        categoryScroll.setBorder(null);
        categoryScroll.setBackground(Color.WHITE);
        categoryScroll.getViewport().setBackground(Color.WHITE);
        categoryScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        categoryScroll.getVerticalScrollBar().setUnitIncrement(16);
        categorySidebarCard.add(categoryScroll, BorderLayout.CENTER);

        // Visual Catalog Grid (CENTER) - 4 products per row, seamless canvas
        catalogGridPanel = new JPanel(new GridLayout(0, 4, 12, 12));
        catalogGridPanel.setOpaque(false);

        JPanel catalogScrollContent = new JPanel(new BorderLayout());
        catalogScrollContent.setOpaque(false);
        catalogScrollContent.setBorder(new EmptyBorder(2, 4, 8, 4));
        catalogScrollContent.add(catalogGridPanel, BorderLayout.NORTH);

        JScrollPane catalogScroll = new JScrollPane(catalogScrollContent);
        catalogScroll.setBorder(null);
        catalogScroll.setOpaque(false);
        catalogScroll.getViewport().setOpaque(false);
        catalogScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        catalogScroll.getVerticalScrollBar().setUnitIncrement(24);

        // Combine: Sidebar on WEST, Product grid on CENTER
        JPanel catalogWrapper = new JPanel(new BorderLayout(12, 0));
        catalogWrapper.setOpaque(false);
        catalogWrapper.add(categorySidebarCard, BorderLayout.WEST);
        catalogWrapper.add(catalogScroll, BorderLayout.CENTER);

        left.add(topArea, BorderLayout.NORTH);
        left.add(catalogWrapper, BorderLayout.CENTER);

        return left;
    }

    // ==========================================
    // 3. CART & CHECKOUT SUMMARY (RIGHT 35%)
    // ==========================================
    private JPanel createCartAndCheckoutSection() {
        JPanel right = new JPanel(new BorderLayout(0, 10));
        right.setPreferredSize(new Dimension(360, 0));
        right.setBackground(Color.WHITE);
        right.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(14, 14, 14, 14)
        ));

        // Cart Header
        JPanel cartHeader = new JPanel(new BorderLayout(8, 0));
        cartHeader.setOpaque(false);

        JLabel lblCartTitle = new JLabel("🛒 Your Shopping Cart");
        lblCartTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblCartTitle.setForeground(new Color(30, 41, 59));

        lblCartItemCount = new JLabel("(0 items)");
        lblCartItemCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCartItemCount.setForeground(new Color(100, 116, 139));

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleBox.setOpaque(false);
        titleBox.add(lblCartTitle);
        titleBox.add(lblCartItemCount);

        btnClearCart = new JButton("🧹 Clear");
        btnClearCart.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnClearCart.setForeground(new Color(239, 68, 68));
        btnClearCart.setContentAreaFilled(false);
        btnClearCart.setBorder(BorderFactory.createLineBorder(new Color(254, 202, 202), 1));
        btnClearCart.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearCart.addActionListener(e -> clearCart());

        cartHeader.add(titleBox, BorderLayout.WEST);
        cartHeader.add(btnClearCart, BorderLayout.EAST);

        // Cart Table / Item list
        String[] columns = {"Product", "Price", "Qty", "Total", "Action"};
        cartTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        cartTable = new JTable(cartTableModel);
        cartTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cartTable.setRowHeight(44);
        cartTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        cartTable.getTableHeader().setBackground(new Color(248, 250, 252));
        cartTable.getTableHeader().setForeground(new Color(71, 85, 105));
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cartTable.setShowVerticalLines(false);
        cartTable.setGridColor(new Color(241, 245, 249));

        // Column widths
        cartTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(50);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        cartTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        cartTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        cartTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        cartTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        // Double-click or click to adjust quantity
        cartTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = cartTable.getSelectedRow();
                int col = cartTable.getSelectedColumn();
                if (row >= 0 && row < cartItems.size()) {
                    if (col == 4) {
                        // Delete
                        removeCartItem(row);
                    } else if (col == 2) {
                        // Change quantity dialog
                        promptChangeQuantity(row);
                    }
                }
            }
        });

        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        cartScroll.getViewport().setBackground(Color.WHITE);

        // Touch Quantity Adjustment Bar below Cart Table
        JPanel touchQtyBar = new JPanel(new GridLayout(1, 3, 8, 0));
        touchQtyBar.setOpaque(false);
        touchQtyBar.setBorder(new EmptyBorder(6, 0, 6, 0));

        JButton btnQtyMinus = new JButton("➖ Decrease (-1)");
        btnQtyMinus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnQtyMinus.setBackground(new Color(241, 245, 249));
        btnQtyMinus.setForeground(new Color(51, 65, 85));
        btnQtyMinus.setFocusPainted(false);
        btnQtyMinus.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnQtyMinus.addActionListener(e -> {
            int sel = cartTable.getSelectedRow();
            if (sel >= 0) modifyCartItemQty(sel, -1);
            else if (!cartItems.isEmpty()) modifyCartItemQty(cartItems.size() - 1, -1);
        });

        JButton btnQtyPlus = new JButton("➕ Increase (+1)");
        btnQtyPlus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnQtyPlus.setBackground(new Color(241, 245, 249));
        btnQtyPlus.setForeground(new Color(51, 65, 85));
        btnQtyPlus.setFocusPainted(false);
        btnQtyPlus.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnQtyPlus.addActionListener(e -> {
            int sel = cartTable.getSelectedRow();
            if (sel >= 0) modifyCartItemQty(sel, 1);
            else if (!cartItems.isEmpty()) modifyCartItemQty(cartItems.size() - 1, 1);
        });

        JButton btnRemoveSelected = new JButton("🗑️ Remove");
        btnRemoveSelected.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRemoveSelected.setBackground(new Color(254, 242, 242));
        btnRemoveSelected.setForeground(new Color(220, 38, 38));
        btnRemoveSelected.setFocusPainted(false);
        btnRemoveSelected.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRemoveSelected.addActionListener(e -> {
            int sel = cartTable.getSelectedRow();
            if (sel >= 0) removeCartItem(sel);
            else if (!cartItems.isEmpty()) removeCartItem(cartItems.size() - 1);
        });

        touchQtyBar.add(btnQtyMinus);
        touchQtyBar.add(btnQtyPlus);
        touchQtyBar.add(btnRemoveSelected);

        // Order Summary Box - clean divider with soft background
        JPanel summaryBox = new JPanel(new GridLayout(3, 2, 6, 8));
        summaryBox.setBackground(new Color(248, 250, 252));
        summaryBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel lblSubtotal = new JLabel("Subtotal:");
        lblSubtotal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSubtotal.setForeground(new Color(100, 116, 139));
        lblSubtotalValue = new JLabel("₹0.00", SwingConstants.RIGHT);
        lblSubtotalValue.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSubtotalValue.setForeground(new Color(30, 41, 59));

        JLabel lblGst = new JLabel("Tax (GST):");
        lblGst.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblGst.setForeground(new Color(100, 116, 139));
        lblGstValue = new JLabel("₹0.00", SwingConstants.RIGHT);
        lblGstValue.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblGstValue.setForeground(new Color(30, 41, 59));

        JLabel lblGrandTotal = new JLabel("TOTAL TO PAY:");
        lblGrandTotal.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblGrandTotal.setForeground(new Color(15, 23, 42));
        lblGrandTotalValue = new JLabel("₹0.00", SwingConstants.RIGHT);
        lblGrandTotalValue.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblGrandTotalValue.setForeground(new Color(16, 185, 129)); // Vibrant emerald green

        summaryBox.add(lblSubtotal);
        summaryBox.add(lblSubtotalValue);
        summaryBox.add(lblGst);
        summaryBox.add(lblGstValue);
        summaryBox.add(lblGrandTotal);
        summaryBox.add(lblGrandTotalValue);

        // Big Checkout Action Button
        btnPayNow = new JButton("💳 PROCEED TO PAY (₹0.00)");
        btnPayNow.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnPayNow.setBackground(new Color(16, 185, 129));
        btnPayNow.setForeground(Color.WHITE);
        btnPayNow.setFocusPainted(false);
        btnPayNow.setBorder(new EmptyBorder(14, 18, 14, 18));
        btnPayNow.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPayNow.setEnabled(false);
        btnPayNow.addActionListener(e -> startSelfCheckoutPayment());

        // Assemble Right Panel
        JPanel centerCart = new JPanel(new BorderLayout(0, 6));
        centerCart.setOpaque(false);
        centerCart.add(cartScroll, BorderLayout.CENTER);
        centerCart.add(touchQtyBar, BorderLayout.SOUTH);

        JPanel bottomSummary = new JPanel(new BorderLayout(0, 10));
        bottomSummary.setOpaque(false);
        bottomSummary.add(summaryBox, BorderLayout.NORTH);
        bottomSummary.add(btnPayNow, BorderLayout.SOUTH);

        right.add(cartHeader, BorderLayout.NORTH);
        right.add(centerCart, BorderLayout.CENTER);
        right.add(bottomSummary, BorderLayout.SOUTH);

        return right;
    }

    // ==========================================
    // CATALOG & CATEGORY LOADING
    // ==========================================
    private void loadCategories() {
        categoryTabsPanel.removeAll();

        // "All Items" tab
        JButton btnAll = createCategoryTabButton("⭐ All Items", "ALL");
        categoryTabsPanel.add(btnAll);
        categoryTabsPanel.add(Box.createVerticalStrut(6));

        List<Category> categories = new ArrayList<>();
        try {
            categories = categoryDAO.getAllCategories();
        } catch (Exception ignored) {}

        if (categories == null || categories.isEmpty()) {
            categories = new ArrayList<>();
            categories.add(new Category(1, "Dairy", "Dairy products"));
            categories.add(new Category(2, "Electronics", "Electronic gadgets"));
            categories.add(new Category(3, "Stationery", "Office & paper"));
            categories.add(new Category(4, "Fresh", "Fresh fruits & veg"));
        }

        for (Category cat : categories) {
            String emoji = getCategoryEmoji(cat.getName());
            JButton btnCat = createCategoryTabButton(emoji + " " + cat.getName(), cat.getName());
            categoryTabsPanel.add(btnCat);
            categoryTabsPanel.add(Box.createVerticalStrut(6));
        }

        categoryTabsPanel.add(Box.createVerticalGlue());
        categoryTabsPanel.revalidate();
        categoryTabsPanel.repaint();
    }

    private String getCategoryEmoji(String catName) {
        if (catName == null) return "🏷️";
        String lower = catName.toLowerCase().trim();
        if (lower.contains("dairy") || lower.contains("milk")) return "🥛";
        if (lower.contains("fruit") || lower.contains("mango") || lower.contains("veg")) return "🍎";
        if (lower.contains("electronic") || lower.contains("laptop") || lower.contains("tv")) return "💻";
        if (lower.contains("accessories") || lower.contains("keyboard") || lower.contains("mouse")) return "⌨️";
        if (lower.contains("stationery") || lower.contains("paper")) return "📄";
        if (lower.contains("snack") || lower.contains("food") || lower.contains("bev")) return "🍪";
        return "🏷️";
    }

    private JButton createCategoryTabButton(String label, String categoryKey) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(135, 40));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        boolean isSelected = selectedCategory.equalsIgnoreCase(categoryKey);

        Color activeBg = new Color(37, 99, 235);
        Color inactiveBg = new Color(248, 250, 252);

        btn.setBackground(isSelected ? activeBg : inactiveBg);
        btn.setForeground(isSelected ? Color.WHITE : new Color(51, 65, 85));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isSelected ? activeBg : new Color(226, 232, 240), 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            selectedCategory = categoryKey;
            loadCategories(); // refresh active styling
            filterCatalogByCategory(selectedCategory);
            focusScanInput();
        });

        return btn;
    }

    public void loadCatalog() {
        try {
            catalogProducts = productDAO.getAllProducts();
        } catch (Exception ignored) {}

        if (catalogProducts == null || catalogProducts.isEmpty()) {
            catalogProducts = getSampleFallbackProducts();
        }
        renderCatalogGrid(catalogProducts);
    }

    private List<Product> getSampleFallbackProducts() {
        List<Product> list = new ArrayList<>();

        Product p1 = new Product(1, "PRD-001", "8901262260138", "Amul Pure Fresh Milk (1L)", 1, 55.0, 64.0, 45, 10, "data/product_images/PRD-001.png");
        p1.setCategoryName("Dairy");
        list.add(p1);

        Product p2 = new Product(2, "PRD-002", "890873960606", "Wireless Optical Mouse (2.4GHz)", 2, 350.0, 499.0, 28, 5, "data/product_images/PRD-002.png");
        p2.setCategoryName("Electronics");
        list.add(p2);

        Product p3 = new Product(3, "PRD-003", "890879660777", "Mechanical Gaming Keyboard RGB", 2, 1400.0, 1850.0, 15, 3, "data/product_images/PRD-003.png");
        p3.setCategoryName("Electronics");
        list.add(p3);

        Product p4 = new Product(4, "PRD-004", "890704139705", "A4 Copier Paper Ream (500s)", 3, 220.0, 280.0, 60, 10, "data/product_images/PRD-004.png");
        p4.setCategoryName("Stationery");
        list.add(p4);

        Product p5 = new Product(5, "PRD-005", "34567", "Fresh Alphonso Mangoes (1kg)", 4, 280.0, 350.0, 20, 5, "data/product_images/34567.png");
        p5.setCategoryName("Fresh");
        list.add(p5);

        Product p6 = new Product(6, "PRD-006", "233", "Smart 4K Ultra HD LED TV 43 Inch", 2, 18000.0, 24999.0, 8, 2, "data/product_images/tv.png");
        p6.setCategoryName("Electronics");
        list.add(p6);

        Product p7 = new Product(7, "PRD-0015", "PRD-0015", "Dell Inspiron 15 Core i5 Laptop", 2, 45000.0, 52999.0, 10, 2, "data/product_images/PRD-0015.png");
        p7.setCategoryName("Electronics");
        list.add(p7);

        Product p8 = new Product(8, "PRD-008", "890879660777", "5G Android Smartphone (128GB)", 2, 11500.0, 14999.0, 14, 4, "data/product_images/mob.png");
        p8.setCategoryName("Electronics");
        list.add(p8);

        return list;
    }

    private void filterCatalogByCategory(String catName) {
        if ("ALL".equalsIgnoreCase(catName)) {
            renderCatalogGrid(catalogProducts);
            return;
        }

        List<Product> filtered = new ArrayList<>();
        for (Product p : catalogProducts) {
            if (p.getCategoryName() != null && p.getCategoryName().equalsIgnoreCase(catName)) {
                filtered.add(p);
            }
        }
        renderCatalogGrid(filtered);
    }

    private void renderCatalogGrid(List<Product> products) {
        catalogGridPanel.removeAll();

        if (products.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setOpaque(false);
            JLabel lblEmpty = new JLabel("No products found matching your search.", SwingConstants.CENTER);
            lblEmpty.setFont(new Font("Segoe UI", Font.ITALIC, 14));
            lblEmpty.setForeground(new Color(100, 116, 139));
            empty.add(lblEmpty);
            catalogGridPanel.setLayout(new BorderLayout());
            catalogGridPanel.add(empty, BorderLayout.CENTER);
        } else {
            catalogGridPanel.setLayout(new GridLayout(0, 4, 10, 10));
            for (Product p : products) {
                catalogGridPanel.add(createProductCard(p));
            }
        }

        catalogGridPanel.revalidate();
        catalogGridPanel.repaint();
    }

    private JPanel createProductCard(Product p) {
        boolean inStock = p.getQuantity() > 0;

        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        card.setPreferredSize(new Dimension(0, 262));

        // 1. TOP: Full Edge-to-Edge Image Banner (Enlarged Height 138px for bold display)
        JPanel imageBanner = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int w = getWidth();
                int h = getHeight();
                if (w <= 0 || h <= 0) return;

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Clean soft background fill
                g2.setColor(new Color(248, 250, 252));
                g2.fillRect(0, 0, w, h);

                Image raw = ProductImageUtil.getProductRawImage(p);
                if (raw != null) {
                    int imgW = raw.getWidth(null);
                    int imgH = raw.getHeight(null);
                    if (imgW > 0 && imgH > 0) {
                        // Natural aspect-ratio fit: enlarged view without stretching or cropping
                        int padX = 8;
                        int padY = 6;
                        int availW = w - (padX * 2);
                        int availH = h - (padY * 2);
                        if (availW > 0 && availH > 0) {
                            double scale = Math.min((double) availW / imgW, (double) availH / imgH);
                            int drawW = Math.max(1, (int) Math.round(imgW * scale));
                            int drawH = Math.max(1, (int) Math.round(imgH * scale));
                            int drawX = (w - drawW) / 2;
                            int drawY = (h - drawH) / 2;
                            g2.drawImage(raw, drawX, drawY, drawW, drawH, null);
                        }
                    }
                } else {
                    ImageIcon icon = ProductImageUtil.getProductIcon(p, w, h);
                    if (icon != null) {
                        g2.drawImage(icon.getImage(), 0, 0, w, h, null);
                    }
                }

                // Subtle divider line separating image banner from details below
                g2.setColor(new Color(241, 245, 249));
                g2.drawLine(0, h - 1, w, h - 1);

                g2.dispose();
            }
        };
        imageBanner.setPreferredSize(new Dimension(0, 138));
        imageBanner.setOpaque(false);

        card.add(imageBanner, BorderLayout.NORTH);

        // 2. CENTER: Details Section (Padded)
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);
        detailsPanel.setBorder(new EmptyBorder(8, 12, 10, 12));

        // Row 1: Category Tag & Stock Status
        JPanel tagRow = new JPanel(new BorderLayout());
        tagRow.setOpaque(false);
        tagRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        tagRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        String catName = (p.getCategoryName() != null && !p.getCategoryName().trim().isEmpty())
                ? p.getCategoryName().toUpperCase()
                : "GENERAL";
        JLabel lblCat = new JLabel(catName);
        lblCat.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblCat.setForeground(new Color(100, 116, 139));

        JLabel lblStock = new JLabel(inStock ? "● " + p.getQuantity() + " left" : "● Sold Out");
        lblStock.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblStock.setForeground(inStock ? new Color(22, 163, 74) : new Color(220, 38, 38));

        tagRow.add(lblCat, BorderLayout.WEST);
        tagRow.add(lblStock, BorderLayout.EAST);

        // Row 2: Product Name (2 lines fixed height)
        String safeName = (p.getName() != null)
                ? p.getName().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                : "Product";
        JLabel lblName = new JLabel("<html><div style='line-height:16px; font-weight:600; color:#0F172A; font-family:Segoe UI, sans-serif; font-size:12px;'>" + safeName + "</div></html>");
        lblName.setPreferredSize(new Dimension(100, 34));
        lblName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Row 3: Barcode / SKU
        String codeStr = (p.getBarcode() != null && !p.getBarcode().isEmpty()) ? p.getBarcode() : p.getCode();
        JLabel lblCode = new JLabel("#" + codeStr);
        lblCode.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblCode.setForeground(new Color(148, 163, 184));
        lblCode.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Row 4: Price & Touch Add Button
        JPanel bottomBox = new JPanel(new BorderLayout(6, 0));
        bottomBox.setOpaque(false);
        bottomBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        bottomBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPrice = new JLabel(String.format("₹%.2f", p.getSellingPrice()));
        lblPrice.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblPrice.setForeground(new Color(13, 148, 136));

        JButton btnAdd = new JButton(inStock ? "➕ Add" : "Sold Out");
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnAdd.setBackground(inStock ? new Color(37, 99, 235) : new Color(241, 245, 249));
        btnAdd.setForeground(inStock ? Color.WHITE : new Color(148, 163, 184));
        btnAdd.setFocusPainted(false);
        btnAdd.setEnabled(inStock);
        btnAdd.setCursor(inStock ? new Cursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        btnAdd.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(inStock ? new Color(37, 99, 235) : new Color(226, 232, 240), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        btnAdd.addActionListener(e -> addProductToCart(p));

        bottomBox.add(lblPrice, BorderLayout.WEST);
        bottomBox.add(btnAdd, BorderLayout.EAST);

        detailsPanel.add(tagRow);
        detailsPanel.add(Box.createVerticalStrut(4));
        detailsPanel.add(lblName);
        detailsPanel.add(Box.createVerticalStrut(2));
        detailsPanel.add(lblCode);
        detailsPanel.add(Box.createVerticalStrut(6));
        detailsPanel.add(bottomBox);

        card.add(detailsPanel, BorderLayout.CENTER);

        // Card touch click & hover styling (supports both card and image click)
        MouseAdapter cardMouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (inStock) addProductToCart(p);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (inStock) {
                    card.setBorder(BorderFactory.createLineBorder(new Color(37, 99, 235), 1));
                    card.setBackground(new Color(248, 250, 252));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
                card.setBackground(Color.WHITE);
            }
        };

        card.setCursor(inStock ? new Cursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        card.addMouseListener(cardMouseAdapter);
        imageBanner.setCursor(inStock ? new Cursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        imageBanner.addMouseListener(cardMouseAdapter);

        return card;
    }

    // ==========================================
    // 4. SCANNING & CART MANIPULATION
    // ==========================================
    private void handleScanOrSearch() {
        String query = txtSearchScan.getText().trim();
        if (query.isEmpty()) return;

        try {
            // 1. First attempt exact barcode or code lookup
            Product exact = productDAO.getProductByCode(query);
            if (exact != null) {
                addProductToCart(exact);
                txtSearchScan.setText("");
                return;
            }

            // 2. Otherwise search products by keyword
            List<Product> matches = productDAO.searchProducts(query);
            if (matches.size() == 1) {
                addProductToCart(matches.get(0));
                txtSearchScan.setText("");
            } else if (!matches.isEmpty()) {
                renderCatalogGrid(matches);
                setToast("Found " + matches.size() + " products for '" + query + "'. Tap one to add.", false);
            } else {
                playAlertSound();
                setToast("No product found matching barcode/name: " + query, true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            setToast("Search error: " + ex.getMessage(), true);
        }

        focusScanInput();
    }

    public void addProductToCart(Product p) {
        if (p.getQuantity() <= 0) {
            playAlertSound();
            setToast("⚠️ " + p.getName() + " is currently out of stock!", true);
            return;
        }

        // Check if item already exists in cart
        CartItem existing = null;
        for (CartItem ci : cartItems) {
            if (ci.product.getId() == p.getId()) {
                existing = ci;
                break;
            }
        }

        if (existing != null) {
            if (existing.quantity >= p.getQuantity()) {
                playAlertSound();
                setToast("⚠️ Only " + p.getQuantity() + " units available in store for " + p.getName(), true);
                return;
            }
            existing.quantity++;
        } else {
            cartItems.add(new CartItem(p, 1));
        }

        playScanBeep();
        refreshCartTable();
        setToast("✓ Added: " + p.getName() + " (₹" + String.format("%.2f", p.getSellingPrice()) + ")", false);
        focusScanInput();
    }

    private void modifyCartItemQty(int index, int delta) {
        if (index < 0 || index >= cartItems.size()) return;
        CartItem item = cartItems.get(index);
        int newQty = item.quantity + delta;

        if (newQty <= 0) {
            removeCartItem(index);
            return;
        }

        if (newQty > item.product.getQuantity()) {
            playAlertSound();
            setToast("⚠️ Only " + item.product.getQuantity() + " units in stock!", true);
            return;
        }

        item.quantity = newQty;
        playScanBeep();
        refreshCartTable();
        cartTable.setRowSelectionInterval(index, index);
    }

    private void removeCartItem(int index) {
        if (index < 0 || index >= cartItems.size()) return;
        CartItem item = cartItems.remove(index);
        refreshCartTable();
        setToast("Removed " + item.product.getName() + " from cart.", false);
        focusScanInput();
    }

    private void promptChangeQuantity(int index) {
        if (index < 0 || index >= cartItems.size()) return;
        CartItem item = cartItems.get(index);
        String res = JOptionPane.showInputDialog(this,
                "Enter Quantity for " + item.product.getName() + " (Max " + item.product.getQuantity() + "):",
                item.quantity);
        if (res == null || res.trim().isEmpty()) return;

        try {
            int q = Integer.parseInt(res.trim());
            if (q <= 0) {
                removeCartItem(index);
            } else if (q > item.product.getQuantity()) {
                playAlertSound();
                JOptionPane.showMessageDialog(this, "Requested quantity exceeds available stock (" + item.product.getQuantity() + ")", "Stock Limit", JOptionPane.WARNING_MESSAGE);
            } else {
                item.quantity = q;
                refreshCartTable();
            }
        } catch (NumberFormatException e) {
            playAlertSound();
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric quantity.", "Invalid Number", JOptionPane.ERROR_MESSAGE);
        }
        focusScanInput();
    }

    private void clearCart() {
        if (cartItems.isEmpty()) return;
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear your cart?", "Clear Cart", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            cartItems.clear();
            refreshCartTable();
            setToast("Cart cleared. Ready for next item.", false);
            focusScanInput();
        }
    }

    private void refreshCartTable() {
        cartTableModel.setRowCount(0);

        double subtotal = 0.0;
        int totalUnits = 0;

        for (CartItem ci : cartItems) {
            subtotal += ci.getSubtotal();
            totalUnits += ci.quantity;

            cartTableModel.addRow(new Object[]{
                    ci.product.getName(),
                    String.format("₹%.2f", ci.unitPrice),
                    ci.quantity,
                    String.format("₹%.2f", ci.getSubtotal()),
                    "🗑️"
            });
        }

        // Standard GST calculate or 0%
        double gstRate = 0.0; try { gstRate = Double.parseDouble(config.AppSettings.getString(config.AppSettings.KEY_STORE_GST, "18.0")); } catch (Exception ignored) {}
        double gstAmount = subtotal * (gstRate / 100.0);
        double grandTotal = subtotal + gstAmount;

        lblCartItemCount.setText("(" + totalUnits + " items)");
        lblSubtotalValue.setText(String.format("₹%.2f", subtotal));
        lblGstValue.setText(String.format("₹%.2f", gstAmount));
        lblGrandTotalValue.setText(String.format("₹%.2f", grandTotal));

        btnPayNow.setText(String.format("💳 PROCEED TO PAY (₹%.2f)", grandTotal));
        btnPayNow.setEnabled(!cartItems.isEmpty());
        btnClearCart.setEnabled(!cartItems.isEmpty());
    }

    // ==========================================
    // 5. INTERACTIVE PAYMENT FLOW & RECEIPT
    // ==========================================
    private void startSelfCheckoutPayment() {
        if (cartItems.isEmpty()) return;

        double subtotal = 0.0;
        for (CartItem ci : cartItems) subtotal += ci.getSubtotal();
        double gstRate = 0.0; try { gstRate = Double.parseDouble(config.AppSettings.getString(config.AppSettings.KEY_STORE_GST, "18.0")); } catch (Exception ignored) {}
        double gstAmount = subtotal * (gstRate / 100.0);
        double grandTotal = subtotal + gstAmount;

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = (parentWindow instanceof Frame) ? (Frame) parentWindow : null;

        // Open Payment Modal Dialog
        SelfCheckoutPaymentDialog dialog = new SelfCheckoutPaymentDialog(parentFrame, cartItems, subtotal, gstRate, gstAmount, grandTotal, checkoutUser);
        dialog.setVisible(true);

        if (dialog.isCompleted()) {
            // Payment success! Clear cart and prompt next customer
            cartItems.clear();
            refreshCartTable();
            loadCatalog(); // Refresh inventory counts
            setToast("🎉 Payment completed successfully! Thank you for shopping with us.", false);
        }

        focusScanInput();
    }

    private void openCameraBarcodeScanner() {
        String barcode = JOptionPane.showInputDialog(this,
                "Scan Barcode with Camera / Scanner or Type Barcode:",
                "Barcode Scanner Input", JOptionPane.PLAIN_MESSAGE);
        if (barcode != null && !barcode.trim().isEmpty()) {
            txtSearchScan.setText(barcode.trim());
            handleScanOrSearch();
        }
        focusScanInput();
    }

    private void callStoreAssistant() {
        playAlertSound();
        JOptionPane.showMessageDialog(this,
                "🛎️ STORE ASSISTANT NOTIFIED!\n\nAn attendant is on their way to assist you with your checkout.\nPlease wait a moment.",
                "Assistant Called", JOptionPane.INFORMATION_MESSAGE);
        setToast("Store assistant has been summoned to Self-Checkout #1.", false);
        focusScanInput();
    }

    private void launchFullscreenWindow() {
        SelfCheckoutFrame fullscreenFrame = new SelfCheckoutFrame(checkoutUser);
        fullscreenFrame.setVisible(true);
    }

    private void setToast(String message, boolean isError) {
        lblStatusToast.setText((isError ? "⚠️ " : "✓ ") + message);
        lblStatusToast.setForeground(isError ? new Color(220, 38, 38) : new Color(13, 148, 136));
    }

    private void playScanBeep() {
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception ignored) {}
    }

    private void playAlertSound() {
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception ignored) {}
    }

    private void updateClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy • hh:mm:ss a");
        lblLiveClock.setText("🕒 " + sdf.format(new Date()));
    }

    public void focusScanInput() {
        if (txtSearchScan != null) {
            txtSearchScan.requestFocusInWindow();
        }
    }

    // ==========================================
    // 6. SELF-CHECKOUT PAYMENT MODAL DIALOG
    // ==========================================
    public static class SelfCheckoutPaymentDialog extends JDialog {
        private final List<CartItem> items;
        private final double subtotal;
        private final double gstRate;
        private final double gstAmount;
        private final double grandTotal;
        private final User user;

        private boolean completed = false;
        private final BillingDAO billingDAO = new BillingDAO();
        private final CustomerDAO customerDAO = new CustomerDAO();

        private JTextField txtCustomerPhone;
        private JLabel lblUpiQrCanvas;
        private String generatedInvoiceNo;
        private Sale savedSale;

        public SelfCheckoutPaymentDialog(Frame owner, List<CartItem> items, double subtotal, double gstRate, double gstAmount, double grandTotal, User user) {
            super(owner, "Self-Checkout Payment & Invoice", true);
            this.items = items;
            this.subtotal = subtotal;
            this.gstRate = gstRate;
            this.gstAmount = gstAmount;
            this.grandTotal = grandTotal;
            this.user = user;

            try {
                this.generatedInvoiceNo = billingDAO.generateNextInvoiceNo();
            } catch (Exception e) {
                this.generatedInvoiceNo = "INV-" + System.currentTimeMillis();
            }

            setSize(680, 720);
            setLocationRelativeTo(owner);
            setResizable(false);
            initComponents();
        }

        public boolean isCompleted() {
            return completed;
        }

        private void initComponents() {
            JPanel root = new JPanel(new BorderLayout(0, 12));
            root.setBackground(Color.WHITE);
            root.setBorder(new EmptyBorder(16, 20, 16, 20));

            // Header Banner
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(new Color(15, 23, 42));
            header.setBorder(new EmptyBorder(12, 16, 12, 16));

            JLabel lblH1 = new JLabel("💳 Express Self-Checkout Payment");
            lblH1.setFont(new Font("Segoe UI", Font.BOLD, 17));
            lblH1.setForeground(Color.WHITE);

            JLabel lblTotalBadge = new JLabel(String.format("₹%.2f", grandTotal));
            lblTotalBadge.setFont(new Font("Segoe UI", Font.BOLD, 20));
            lblTotalBadge.setForeground(new Color(34, 197, 94));

            header.add(lblH1, BorderLayout.WEST);
            header.add(lblTotalBadge, BorderLayout.EAST);

            // Center: Phone Input + Tabbed Payment Method
            JPanel center = new JPanel(new BorderLayout(0, 12));
            center.setOpaque(false);

            // Optional Customer Phone
            JPanel phoneCard = new JPanel(new BorderLayout(10, 4));
            phoneCard.setBackground(new Color(248, 250, 252));
            phoneCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(226, 232, 240)),
                    new EmptyBorder(10, 14, 10, 14)
            ));

            JLabel lblPhonePrompt = new JLabel("📱 Mobile Number for Digital Receipt & WhatsApp Bill (Optional):");
            lblPhonePrompt.setFont(new Font("Segoe UI", Font.BOLD, 12));
            lblPhonePrompt.setForeground(new Color(71, 85, 105));

            txtCustomerPhone = new JTextField();
            txtCustomerPhone.setFont(new Font("Segoe UI", Font.BOLD, 15));
            txtCustomerPhone.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(203, 213, 225)),
                    new EmptyBorder(6, 10, 6, 10)
            ));

            phoneCard.add(lblPhonePrompt, BorderLayout.NORTH);
            phoneCard.add(txtCustomerPhone, BorderLayout.CENTER);

            center.add(phoneCard, BorderLayout.NORTH);

            // Tabbed Payment Options: UPI QR (Default), Card, Cash
            JTabbedPane tabPayment = new JTabbedPane();
            tabPayment.setFont(new Font("Segoe UI", Font.BOLD, 13));

            tabPayment.addTab("📱 Scan & Pay with UPI QR", createUpiTab());
            tabPayment.addTab("💳 Credit / Debit Card", createCardTab());
            tabPayment.addTab("💵 Cash at Counter", createCashTab());

            center.add(tabPayment, BorderLayout.CENTER);

            // Bottom Cancel / Exit button
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            bottom.setOpaque(false);

            JButton btnCancel = new JButton("Cancel & Back to Cart");
            btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            btnCancel.setForeground(new Color(100, 116, 139));
            btnCancel.setFocusPainted(false);
            btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnCancel.addActionListener(e -> dispose());

            bottom.add(btnCancel);

            root.add(header, BorderLayout.NORTH);
            root.add(center, BorderLayout.CENTER);
            root.add(bottom, BorderLayout.SOUTH);

            setContentPane(root);
        }

        private JPanel createUpiTab() {
            JPanel p = new JPanel(new BorderLayout(0, 10));
            p.setBackground(Color.WHITE);
            p.setBorder(new EmptyBorder(12, 12, 10, 12));

            // Generate UPI Intent & Real QR code
            String storeUpi = AppSettings.getString(AppSettings.KEY_STORE_UPI_ID, "bazaarpoint@upi").trim();
            String storeName = AppSettings.getString(AppSettings.KEY_STORE_NAME, "SmartBilling Store").trim();
            String upiUri = QrCodeGenerator.buildUpiUri(storeUpi, storeName, grandTotal, generatedInvoiceNo);
            BufferedImage qrImg = QrCodeGenerator.generateQrImage(upiUri, 230);

            lblUpiQrCanvas = new JLabel(new ImageIcon(qrImg), SwingConstants.CENTER);

            JPanel instructions = new JPanel(new GridLayout(2, 1, 0, 4));
            instructions.setOpaque(false);
            JLabel lblI1 = new JLabel("1. Open GPay, PhonePe, Paytm, BHIM, or any UPI App", SwingConstants.CENTER);
            lblI1.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblI1.setForeground(new Color(30, 41, 59));

            JLabel lblI2 = new JLabel("2. Scan QR code to pay exact amount: " + String.format("₹%.2f", grandTotal), SwingConstants.CENTER);
            lblI2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblI2.setForeground(new Color(100, 116, 139));

            instructions.add(lblI1);
            instructions.add(lblI2);

            JButton btnUpiConfirm = new JButton("✅ I Have Completed UPI Payment (Confirm & Print)");
            btnUpiConfirm.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btnUpiConfirm.setBackground(new Color(16, 185, 129));
            btnUpiConfirm.setForeground(Color.WHITE);
            btnUpiConfirm.setFocusPainted(false);
            btnUpiConfirm.setBorder(new EmptyBorder(12, 16, 12, 16));
            btnUpiConfirm.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnUpiConfirm.addActionListener(e -> finalizeSale("UPI"));

            p.add(instructions, BorderLayout.NORTH);
            p.add(lblUpiQrCanvas, BorderLayout.CENTER);
            p.add(btnUpiConfirm, BorderLayout.SOUTH);

            return p;
        }

        private JPanel createCardTab() {
            JPanel p = new JPanel(new BorderLayout(0, 14));
            p.setBackground(Color.WHITE);
            p.setBorder(new EmptyBorder(24, 20, 20, 20));

            JPanel center = new JPanel(new GridLayout(3, 1, 0, 10));
            center.setOpaque(false);

            JLabel lblIcon = new JLabel("💳", SwingConstants.CENTER);
            lblIcon.setFont(new Font("Segoe UI", Font.PLAIN, 48));

            JLabel lblMsg = new JLabel("Please Tap, Swipe, or Insert your Card on the POS Terminal", SwingConstants.CENTER);
            lblMsg.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblMsg.setForeground(new Color(30, 41, 59));

            JLabel lblSub = new JLabel("Amount to charge: " + String.format("₹%.2f", grandTotal), SwingConstants.CENTER);
            lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblSub.setForeground(new Color(100, 116, 139));

            center.add(lblIcon);
            center.add(lblMsg);
            center.add(lblSub);

            JButton btnCardConfirm = new JButton("✅ Card Payment Approved (Confirm & Print)");
            btnCardConfirm.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btnCardConfirm.setBackground(new Color(37, 99, 235));
            btnCardConfirm.setForeground(Color.WHITE);
            btnCardConfirm.setFocusPainted(false);
            btnCardConfirm.setBorder(new EmptyBorder(12, 16, 12, 16));
            btnCardConfirm.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnCardConfirm.addActionListener(e -> finalizeSale("CARD"));

            p.add(center, BorderLayout.CENTER);
            p.add(btnCardConfirm, BorderLayout.SOUTH);

            return p;
        }

        private JPanel createCashTab() {
            JPanel p = new JPanel(new BorderLayout(0, 14));
            p.setBackground(Color.WHITE);
            p.setBorder(new EmptyBorder(24, 20, 20, 20));

            JPanel center = new JPanel(new GridLayout(3, 1, 0, 10));
            center.setOpaque(false);

            JLabel lblIcon = new JLabel("💵", SwingConstants.CENTER);
            lblIcon.setFont(new Font("Segoe UI", Font.PLAIN, 48));

            JLabel lblMsg = new JLabel("Pay with Cash at Express Checkout Counter", SwingConstants.CENTER);
            lblMsg.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblMsg.setForeground(new Color(30, 41, 59));

            JLabel lblSub = new JLabel("Please deposit " + String.format("₹%.2f", grandTotal) + " with the attendant.", SwingConstants.CENTER);
            lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblSub.setForeground(new Color(100, 116, 139));

            center.add(lblIcon);
            center.add(lblMsg);
            center.add(lblSub);

            JButton btnCashConfirm = new JButton("✅ Cash Received & Verified (Confirm & Print)");
            btnCashConfirm.setFont(new Font("Segoe UI", Font.BOLD, 14));
            btnCashConfirm.setBackground(new Color(5, 150, 105));
            btnCashConfirm.setForeground(Color.WHITE);
            btnCashConfirm.setFocusPainted(false);
            btnCashConfirm.setBorder(new EmptyBorder(12, 16, 12, 16));
            btnCashConfirm.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnCashConfirm.addActionListener(e -> finalizeSale("CASH"));

            p.add(center, BorderLayout.CENTER);
            p.add(btnCashConfirm, BorderLayout.SOUTH);

            return p;
        }

        private void finalizeSale(String paymentMode) {
            try {
                Sale sale = new Sale();
                sale.setInvoiceNo(generatedInvoiceNo);
                sale.setSubtotal(subtotal);
                sale.setGstRate(gstRate);
                sale.setGstAmount(gstAmount);
                sale.setTotalAmount(grandTotal);
                sale.setPaymentMode(paymentMode);
                sale.setCreatedBy(user != null ? user.getId() : 1);

                // Optional Customer Mobile
                String phone = txtCustomerPhone.getText().trim();
                if (!phone.isEmpty()) {
                    Customer cust = customerDAO.getCustomerByPhone(phone);
                    if (cust == null) {
                        cust = new Customer(0, "Customer " + phone, phone, "", "Self Checkout Express");
                        customerDAO.addCustomer(cust);
                    }
                    if (cust != null && cust.getId() > 0) {
                        sale.setCustomerId(cust.getId());
                        sale.setCustomerName(cust.getName());
                    }
                }

                // Add Items
                for (CartItem ci : items) {
                    SaleItem si = new SaleItem();
                    si.setProductId(ci.product.getId());
                    si.setProductName(ci.product.getName());
                    si.setQuantity(ci.quantity);
                    si.setUnitPrice(ci.unitPrice);
                    si.setSubtotal(ci.getSubtotal());
                    sale.getItems().add(si);
                }

                // Process Sale via BillingDAO
                boolean success = billingDAO.processSale(sale);
                if (success) {
                    this.savedSale = sale;
                    this.completed = true;

                    // Play success sound
                    try { Toolkit.getDefaultToolkit().beep(); } catch (Exception ignored) {}

                    // Open Receipt & Confirmation
                    showSuccessAndReceiptModal(sale);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not process transaction. Please consult assistant.", "Transaction Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Billing Failed", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void showSuccessAndReceiptModal(Sale sale) {
            JDialog dlg = new JDialog((Frame) getOwner(), "🎉 Payment Successful - Order Complete", true);
            dlg.setSize(440, 480);
            dlg.setLocationRelativeTo(getOwner());

            JPanel p = new JPanel(new BorderLayout(0, 16));
            p.setBackground(Color.WHITE);
            p.setBorder(new EmptyBorder(24, 24, 24, 24));

            JPanel center = new JPanel(new GridLayout(5, 1, 0, 6));
            center.setOpaque(false);

            JLabel lblSuccessIcon = new JLabel("🎉", SwingConstants.CENTER);
            lblSuccessIcon.setFont(new Font("Segoe UI", Font.PLAIN, 46));

            JLabel lblTitle = new JLabel("PAYMENT SUCCESSFUL!", SwingConstants.CENTER);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
            lblTitle.setForeground(new Color(16, 185, 129));

            JLabel lblInv = new JLabel("Invoice #: " + sale.getInvoiceNo(), SwingConstants.CENTER);
            lblInv.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblInv.setForeground(new Color(30, 41, 59));

            JLabel lblAmt = new JLabel("Total Paid: ₹" + String.format("%.2f", sale.getTotalAmount()) + " via " + sale.getPaymentMode(), SwingConstants.CENTER);
            lblAmt.setFont(new Font("Segoe UI", Font.BOLD, 15));
            lblAmt.setForeground(new Color(13, 148, 136));

            JLabel lblThanks = new JLabel("Thank you for shopping at SmartBilling!", SwingConstants.CENTER);
            lblThanks.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            lblThanks.setForeground(new Color(100, 116, 139));

            center.add(lblSuccessIcon);
            center.add(lblTitle);
            center.add(lblInv);
            center.add(lblAmt);
            center.add(lblThanks);

            JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
            actions.setOpaque(false);

            JButton btnPrint = new JButton("🖨️ Print Receipt");
            btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnPrint.setBackground(new Color(37, 99, 235));
            btnPrint.setForeground(Color.WHITE);
            btnPrint.setFocusPainted(false);
            btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnPrint.addActionListener(e -> {
                new InvoiceDialog((Frame) getOwner(), sale).setVisible(true);
            });

            JButton btnDone = new JButton("👤 Next Customer");
            btnDone.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnDone.setBackground(new Color(16, 185, 129));
            btnDone.setForeground(Color.WHITE);
            btnDone.setFocusPainted(false);
            btnDone.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnDone.addActionListener(e -> dlg.dispose());

            actions.add(btnPrint);
            actions.add(btnDone);

            p.add(center, BorderLayout.CENTER);
            p.add(actions, BorderLayout.SOUTH);

            dlg.setContentPane(p);
            dlg.setVisible(true);
        }
    }
}
