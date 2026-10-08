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
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

/**
 * Self-Checkout Panel for Retail / Supermarket customers.
 * Ultra-modern, touch-screen friendly, card-elevated POS terminal:
 * - Floating rounded cards on Airy Mint Canvas (#ecfcf0)
 * - 3-Column spacious visual product cards with live on-card steppers
 * - Floating Top Header Card with Lane status and live clock
 * - Category sidebar with real-time item counts & Kiosk Guide
 * - Interactive Shopping Cart list of mini-cards with inline touch controls
 * - Contactless trust badge & elevated order summary card
 * - UPI QR Code, Card, and Cash express payment modals
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
    private JPanel cartItemsContainer;
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
        setBackground(AppTheme.BG_CANVAS);
        setOpaque(true);

        initUI();
        loadCatalog();
        loadCategories();

        // Start live clock
        Timer clockTimer = new Timer(1000, e -> updateClock());
        clockTimer.start();
        updateClock();

        // Auto-focus barcode input
        SwingUtilities.invokeLater(this::focusScanInput);
    }

    private void initUI() {
        // 1. Floating Top Header Card
        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.setOpaque(false);
        headerWrapper.setBorder(new EmptyBorder(12, 14, 0, 14));
        headerWrapper.add(createHeader(), BorderLayout.CENTER);
        add(headerWrapper, BorderLayout.NORTH);

        // 2. Main Content Split Pane (Left: Scanning & Catalog, Right: Live Cart & Summary)
        JPanel centerSplit = new JPanel(new BorderLayout(14, 0));
        centerSplit.setOpaque(false);
        centerSplit.setBorder(new EmptyBorder(12, 14, 14, 14));

        centerSplit.add(createCatalogAndScanSection(), BorderLayout.CENTER);
        centerSplit.add(createCartAndCheckoutSection(), BorderLayout.EAST);

        add(centerSplit, BorderLayout.CENTER);
    }

    // ==========================================
    // 1. FLOATING HEADER CARD
    // ==========================================
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = 16;
                // Ambient drop shadow
                g2.setColor(new Color(0, 50, 30, 8));
                g2.fillRoundRect(1, 2, w - 2, h - 2, arc, arc);
                // Card background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(10, 18, 10, 18));

        // Brand & Subtitle
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandBox.setOpaque(false);

        // Forest Green icon badge with Electric Lime accent
        JPanel iconBadge = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.FOREST_GREEN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBadge.setPreferredSize(new Dimension(42, 42));
        iconBadge.setOpaque(false);

        JLabel lblLogo = new JLabel("⚡");
        lblLogo.setFont(AppTheme.font(Font.BOLD, 22));
        lblLogo.setForeground(AppTheme.ELECTRIC_LIME);
        iconBadge.add(lblLogo);

        JPanel titleTextPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titleTextPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("SmartBilling Self-Checkout");
        lblTitle.setFont(AppTheme.font(Font.BOLD, 18));
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel lblSubtitle = new JLabel("Touch-Screen Express Kiosk • Lane #01 • Scan, Tap & Pay");
        lblSubtitle.setFont(AppTheme.font(Font.PLAIN, 12));
        lblSubtitle.setForeground(AppTheme.TEXT_SECONDARY);

        titleTextPanel.add(lblTitle);
        titleTextPanel.add(lblSubtitle);

        brandBox.add(iconBadge);
        brandBox.add(titleTextPanel);

        // Live Clock & Status Badge
        JPanel centerInfo = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        centerInfo.setOpaque(false);

        JLabel lblCheckoutActive = new JLabel("● EXPRESS LANE ACTIVE") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.BG_CANVAS);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblCheckoutActive.setFont(AppTheme.font(Font.BOLD, 11));
        lblCheckoutActive.setForeground(AppTheme.FOREST_GREEN);
        lblCheckoutActive.setOpaque(false);
        lblCheckoutActive.setBorder(new EmptyBorder(5, 14, 5, 14));

        lblLiveClock = new JLabel();
        lblLiveClock.setFont(AppTheme.font(Font.BOLD, 12));
        lblLiveClock.setForeground(AppTheme.TEXT_SECONDARY);

        centerInfo.add(lblCheckoutActive);
        centerInfo.add(lblLiveClock);

        // Action Buttons: Call Assistant, Fullscreen, Exit / Back
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBox.setOpaque(false);

        JButton btnCallAssistant = createPillButton("🛎️ Call Assistant", AppTheme.STATUS_WARNING, new Color(180, 83, 9), Color.WHITE, null);
        btnCallAssistant.addActionListener(e -> callStoreAssistant());

        JButton btnFullscreen = createPillButton("⛶ Fullscreen", AppTheme.BG_CANVAS, AppTheme.HOVER_SURFACE, AppTheme.FOREST_GREEN, AppTheme.BORDER_SAGE);
        btnFullscreen.addActionListener(e -> launchFullscreenWindow());

        JButton btnExit = createPillButton("❌ Back to Dashboard", new Color(254, 242, 242), new Color(254, 226, 226), new Color(220, 38, 38), new Color(254, 202, 202));
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

    // ==========================================
    // 2. SCANNING & CATALOG SECTION (LEFT 65%)
    // ==========================================
    private JPanel createCatalogAndScanSection() {
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);

        // Top Search & Barcode Bar (Clean White Rounded Card with Sage Border)
        JPanel searchBarWrapper = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = 16;
                // Soft drop shadow
                g2.setColor(new Color(0, 50, 30, 6));
                g2.fillRoundRect(1, 2, w - 2, h - 2, arc, arc);
                // Card background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        searchBarWrapper.setOpaque(false);
        searchBarWrapper.setBorder(new EmptyBorder(8, 16, 8, 14));

        JLabel lblScanIcon = new JLabel("🔍");
        lblScanIcon.setFont(AppTheme.font(Font.PLAIN, 18));
        lblScanIcon.setForeground(AppTheme.FOREST_GREEN);

        txtSearchScan = new JTextField();
        txtSearchScan.setFont(AppTheme.font(Font.PLAIN, 14));
        txtSearchScan.setForeground(AppTheme.TEXT_PRIMARY);
        txtSearchScan.setCaretColor(AppTheme.FOREST_GREEN);
        txtSearchScan.setBorder(null);
        txtSearchScan.putClientProperty("JTextField.placeholderText", "Scan barcode with handheld scanner or search by product name/code...");
        txtSearchScan.addActionListener(e -> handleScanOrSearch());

        JButton btnAddBarcode = createPillButton("➕ Add Item", AppTheme.FOREST_GREEN, AppTheme.FOREST_MID, Color.WHITE, null);
        btnAddBarcode.setFont(AppTheme.font(Font.BOLD, 12));
        btnAddBarcode.setBorder(new EmptyBorder(7, 16, 7, 16));
        btnAddBarcode.addActionListener(e -> handleScanOrSearch());

        JButton btnCamera = createPillButton("📷 Camera Scan", AppTheme.FOREST_MID, AppTheme.FOREST_DEEP, Color.WHITE, null);
        btnCamera.setFont(AppTheme.font(Font.BOLD, 12));
        btnCamera.setBorder(new EmptyBorder(7, 14, 7, 14));
        btnCamera.addActionListener(e -> openCameraBarcodeScanner());

        JButton btnClearSearch = new JButton("✖");
        btnClearSearch.setFont(AppTheme.font(Font.BOLD, 13));
        btnClearSearch.setForeground(AppTheme.TEXT_MUTED);
        btnClearSearch.setBorder(new EmptyBorder(4, 8, 4, 8));
        btnClearSearch.setContentAreaFilled(false);
        btnClearSearch.setFocusPainted(false);
        btnClearSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearSearch.addActionListener(e -> {
            txtSearchScan.setText("");
            loadCatalog();
            focusScanInput();
        });

        JPanel rightSearchActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightSearchActions.setOpaque(false);
        rightSearchActions.add(btnClearSearch);
        rightSearchActions.add(btnCamera);
        rightSearchActions.add(btnAddBarcode);

        searchBarWrapper.add(lblScanIcon, BorderLayout.WEST);
        searchBarWrapper.add(txtSearchScan, BorderLayout.CENTER);
        searchBarWrapper.add(rightSearchActions, BorderLayout.EAST);

        // Toast feedback label with soft mint pill background
        lblStatusToast = new JLabel("Ready: Scan an item barcode or tap any product card below to add to cart.") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblStatusToast.setFont(AppTheme.font(Font.BOLD, 12));
        lblStatusToast.setForeground(AppTheme.FOREST_GREEN);
        lblStatusToast.setBorder(new EmptyBorder(3, 10, 3, 10));

        JPanel topArea = new JPanel(new BorderLayout(0, 6));
        topArea.setOpaque(false);
        topArea.add(searchBarWrapper, BorderLayout.NORTH);
        topArea.add(lblStatusToast, BorderLayout.SOUTH);

        // Category Sidebar Panel (WEST) - Clean white card with Sage border
        JPanel categorySidebarCard = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = 16;
                g2.setColor(new Color(0, 50, 30, 6));
                g2.fillRoundRect(1, 2, w - 2, h - 2, arc, arc);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        categorySidebarCard.setOpaque(false);
        categorySidebarCard.setPreferredSize(new Dimension(175, 0));
        categorySidebarCard.setBorder(new EmptyBorder(14, 10, 12, 10));

        JLabel lblCategoryHeader = new JLabel("🏷️ CATEGORIES");
        lblCategoryHeader.setFont(AppTheme.font(Font.BOLD, 11));
        lblCategoryHeader.setForeground(AppTheme.TEXT_MUTED);
        lblCategoryHeader.setBorder(new EmptyBorder(0, 6, 6, 6));
        categorySidebarCard.add(lblCategoryHeader, BorderLayout.NORTH);

        categoryTabsPanel = new JPanel();
        categoryTabsPanel.setLayout(new BoxLayout(categoryTabsPanel, BoxLayout.Y_AXIS));
        categoryTabsPanel.setOpaque(false);

        JScrollPane categoryScroll = new JScrollPane(categoryTabsPanel);
        categoryScroll.setBorder(null);
        categoryScroll.setOpaque(false);
        categoryScroll.getViewport().setOpaque(false);
        categoryScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        categoryScroll.getVerticalScrollBar().setUnitIncrement(16);
        categorySidebarCard.add(categoryScroll, BorderLayout.CENTER);

        // Add Quick Guide / Kiosk Assistance Card at the bottom of the sidebar
        categorySidebarCard.add(createKioskHelpCard(), BorderLayout.SOUTH);

        // Visual Catalog Grid (CENTER) - 3 spacious product cards per row
        catalogGridPanel = new JPanel(new GridLayout(0, 3, 14, 14));
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

    /**
     * Kiosk Help & Quick Instruction Card at the bottom of the sidebar.
     */
    private JPanel createKioskHelpCard() {
        JPanel card = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(AppTheme.LIME_PALE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel lblTitle = new JLabel("💡 Quick Guide");
        lblTitle.setFont(AppTheme.font(Font.BOLD, 11));
        lblTitle.setForeground(AppTheme.FOREST_GREEN);

        JLabel lblStep1 = new JLabel("1. Scan or tap item");
        lblStep1.setFont(AppTheme.font(Font.PLAIN, 10));
        lblStep1.setForeground(AppTheme.TEXT_SECONDARY);

        JLabel lblStep2 = new JLabel("2. Adjust qty in cart");
        lblStep2.setFont(AppTheme.font(Font.PLAIN, 10));
        lblStep2.setForeground(AppTheme.TEXT_SECONDARY);

        JLabel lblStep3 = new JLabel("3. Scan UPI QR & pay");
        lblStep3.setFont(AppTheme.font(Font.PLAIN, 10));
        lblStep3.setForeground(AppTheme.TEXT_SECONDARY);

        JPanel content = new JPanel(new GridLayout(4, 1, 0, 3));
        content.setOpaque(false);
        content.add(lblTitle);
        content.add(lblStep1);
        content.add(lblStep2);
        content.add(lblStep3);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ==========================================
    // 3. CART & CHECKOUT SUMMARY (RIGHT 35%)
    // ==========================================
    private JPanel createCartAndCheckoutSection() {
        JPanel right = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = 16;
                // Soft shadow
                g2.setColor(new Color(0, 50, 30, 8));
                g2.fillRoundRect(1, 2, w - 2, h - 2, arc, arc);
                // Pure white surface
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(380, 0));
        right.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Cart Header
        JPanel cartHeader = new JPanel(new BorderLayout(8, 0));
        cartHeader.setOpaque(false);

        JLabel lblCartTitle = new JLabel("🛒 Your Shopping Cart");
        lblCartTitle.setFont(AppTheme.font(Font.BOLD, 16));
        lblCartTitle.setForeground(AppTheme.TEXT_PRIMARY);

        lblCartItemCount = new JLabel("(0 items)") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.LIME_PALE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblCartItemCount.setFont(AppTheme.font(Font.BOLD, 11));
        lblCartItemCount.setForeground(AppTheme.FOREST_GREEN);
        lblCartItemCount.setBorder(new EmptyBorder(2, 8, 2, 8));

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleBox.setOpaque(false);
        titleBox.add(lblCartTitle);
        titleBox.add(lblCartItemCount);

        btnClearCart = new JButton("🧹 Clear") {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) { hov = true; repaint(); } }
                    @Override public void mouseExited(MouseEvent e) { hov = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = h;
                g2.setColor(hov ? new Color(254, 226, 226) : new Color(254, 242, 242));
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.setColor(new Color(254, 202, 202));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnClearCart.setFont(AppTheme.font(Font.BOLD, 11));
        btnClearCart.setForeground(new Color(220, 38, 38));
        btnClearCart.setFocusPainted(false);
        btnClearCart.setContentAreaFilled(false);
        btnClearCart.setBorderPainted(false);
        btnClearCart.setOpaque(false);
        btnClearCart.setBorder(new EmptyBorder(4, 12, 4, 12));
        btnClearCart.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearCart.setEnabled(false);
        btnClearCart.addActionListener(e -> clearCart());

        cartHeader.add(titleBox, BorderLayout.WEST);
        cartHeader.add(btnClearCart, BorderLayout.EAST);

        // Modern Touch Cart Container (Item Cards with inline steppers)
        cartItemsContainer = new JPanel();
        cartItemsContainer.setLayout(new BoxLayout(cartItemsContainer, BoxLayout.Y_AXIS));
        cartItemsContainer.setOpaque(false);

        JScrollPane cartScroll = new JScrollPane(cartItemsContainer);
        cartScroll.setBorder(null);
        cartScroll.setOpaque(false);
        cartScroll.getViewport().setOpaque(false);
        cartScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        cartScroll.getVerticalScrollBar().setUnitIncrement(18);

        // Trust & Contactless Badge Banner
        JPanel trustBanner = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(AppTheme.LIME_PALE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        trustBanner.setOpaque(false);
        trustBanner.setBorder(new EmptyBorder(4, 8, 4, 8));
        JLabel lblTrust = new JLabel("🛡️ 100% Contactless & Express Self-Checkout");
        lblTrust.setFont(AppTheme.font(Font.BOLD, 10));
        lblTrust.setForeground(AppTheme.FOREST_MID);
        trustBanner.add(lblTrust);

        // Order Summary Box - Mint Canvas container with Sage border
        JPanel summaryBox = new JPanel(new GridLayout(3, 2, 6, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = 14;
                g2.setColor(AppTheme.BG_CANVAS);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        summaryBox.setOpaque(false);
        summaryBox.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel lblSubtotal = new JLabel("Subtotal:");
        lblSubtotal.setFont(AppTheme.font(Font.PLAIN, 13));
        lblSubtotal.setForeground(AppTheme.TEXT_SECONDARY);
        lblSubtotalValue = new JLabel("₹0.00", SwingConstants.RIGHT);
        lblSubtotalValue.setFont(AppTheme.font(Font.BOLD, 14));
        lblSubtotalValue.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel lblGst = new JLabel("Tax (GST):");
        lblGst.setFont(AppTheme.font(Font.PLAIN, 13));
        lblGst.setForeground(AppTheme.TEXT_SECONDARY);
        lblGstValue = new JLabel("₹0.00", SwingConstants.RIGHT);
        lblGstValue.setFont(AppTheme.font(Font.BOLD, 14));
        lblGstValue.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel lblGrandTotal = new JLabel("TOTAL TO PAY:");
        lblGrandTotal.setFont(AppTheme.font(Font.BOLD, 14));
        lblGrandTotal.setForeground(AppTheme.TEXT_PRIMARY);
        lblGrandTotalValue = new JLabel("₹0.00", SwingConstants.RIGHT);
        lblGrandTotalValue.setFont(AppTheme.font(Font.BOLD, 22));
        lblGrandTotalValue.setForeground(AppTheme.FOREST_GREEN);

        summaryBox.add(lblSubtotal);
        summaryBox.add(lblSubtotalValue);
        summaryBox.add(lblGst);
        summaryBox.add(lblGstValue);
        summaryBox.add(lblGrandTotal);
        summaryBox.add(lblGrandTotalValue);

        // Big Checkout Action Button (Forest Green stadium pill)
        btnPayNow = new JButton("💳 PROCEED TO PAY (₹0.00)") {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) { hov = true; repaint(); } }
                    @Override public void mouseExited(MouseEvent e) { hov = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = h;
                if (!isEnabled()) {
                    g2.setColor(new Color(226, 232, 240));
                } else if (getModel().isPressed()) {
                    g2.setColor(AppTheme.FOREST_DEEP);
                } else if (hov) {
                    g2.setColor(AppTheme.FOREST_MID);
                } else {
                    g2.setColor(AppTheme.FOREST_GREEN);
                }
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnPayNow.setFont(AppTheme.font(Font.BOLD, 15));
        btnPayNow.setForeground(Color.WHITE);
        btnPayNow.setFocusPainted(false);
        btnPayNow.setContentAreaFilled(false);
        btnPayNow.setBorderPainted(false);
        btnPayNow.setOpaque(false);
        btnPayNow.setPreferredSize(new Dimension(0, 52));
        btnPayNow.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPayNow.setEnabled(false);
        btnPayNow.addActionListener(e -> startSelfCheckoutPayment());

        // Assemble Right Panel
        JPanel centerCart = new JPanel(new BorderLayout());
        centerCart.setOpaque(false);
        centerCart.add(cartScroll, BorderLayout.CENTER);

        JPanel bottomSummary = new JPanel(new BorderLayout(0, 8));
        bottomSummary.setOpaque(false);
        bottomSummary.add(trustBanner, BorderLayout.NORTH);
        bottomSummary.add(summaryBox, BorderLayout.CENTER);
        bottomSummary.add(btnPayNow, BorderLayout.SOUTH);

        right.add(cartHeader, BorderLayout.NORTH);
        right.add(centerCart, BorderLayout.CENTER);
        right.add(bottomSummary, BorderLayout.SOUTH);

        renderCartItemsList();

        return right;
    }

    // ==========================================
    // CATALOG & CATEGORY LOADING
    // ==========================================
    private void loadCategories() {
        categoryTabsPanel.removeAll();

        // Calculate count per category
        Map<String, Integer> categoryCounts = new HashMap<>();
        categoryCounts.put("ALL", catalogProducts.size());
        for (Product p : catalogProducts) {
            String cat = (p.getCategoryName() != null && !p.getCategoryName().trim().isEmpty())
                    ? p.getCategoryName().trim()
                    : "General";
            categoryCounts.put(cat.toUpperCase(), categoryCounts.getOrDefault(cat.toUpperCase(), 0) + 1);
        }

        // "All Items" tab with count
        JButton btnAll = createCategoryTabButton("⭐ All Items (" + catalogProducts.size() + ")", "ALL");
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
            int count = categoryCounts.getOrDefault(cat.getName().toUpperCase(), 0);
            JButton btnCat = createCategoryTabButton(emoji + " " + cat.getName() + " (" + count + ")", cat.getName());
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

    /**
     * Category Tab Button matching the Keyra Sidebar Nav Item:
     * - Stadium capsule shape
     * - Shows category emoji + name, and item count badge
     * - Active: Forest Green background + Electric Lime dot indicator on the right edge!
     * - Inactive: Transparent background, Pine Slate text, soft translucent green hover.
     */
    private JButton createCategoryTabButton(String label, String categoryKey) {
        boolean isSelected = selectedCategory.equalsIgnoreCase(categoryKey);
        JButton btn = new JButton(label) {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hov = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hov = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = h; // stadium pill shape

                if (isSelected) {
                    // Dark green stadium pill
                    g2.setColor(AppTheme.FOREST_GREEN);
                    g2.fillRoundRect(0, 0, w, h, arc, arc);

                    // Keyra Electric Lime Accent pill on active item
                    g2.setColor(AppTheme.ELECTRIC_LIME);
                    g2.fillRoundRect(w - 14, (h - 14) / 2, 4, 14, 4, 4);
                } else if (hov) {
                    // Translucent soft green hover pill
                    g2.setColor(AppTheme.HOVER_SURFACE);
                    g2.fillRoundRect(0, 0, w, h, arc, arc);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.font(isSelected ? Font.BOLD : Font.PLAIN, 12));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setPreferredSize(new Dimension(160, 38));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setForeground(isSelected ? Color.WHITE : AppTheme.TEXT_SECONDARY);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(0, 12, 0, 18));
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
            lblEmpty.setFont(AppTheme.font(Font.ITALIC, 14));
            lblEmpty.setForeground(AppTheme.TEXT_MUTED);
            empty.add(lblEmpty);
            catalogGridPanel.setLayout(new BorderLayout());
            catalogGridPanel.add(empty, BorderLayout.CENTER);
        } else {
            catalogGridPanel.setLayout(new GridLayout(0, 3, 14, 14));
            for (Product p : products) {
                catalogGridPanel.add(createProductCard(p));
            }
        }

        catalogGridPanel.revalidate();
        catalogGridPanel.repaint();
    }

    private int getCartQuantityForProduct(int productId) {
        for (CartItem ci : cartItems) {
            if (ci.product.getId() == productId) {
                return ci.quantity;
            }
        }
        return 0;
    }

    /**
     * Interactive Product Card:
     * - Top image banner with floating category badge & stock status
     * - Multi-line product title with Deep Forest Slate text
     * - Product SKU
     * - Selling Price
     * - Interactive Add / In-Cart Stepper ([ - ]  qty  [ + ]) right on the card!
     */
    private JPanel createProductCard(Product p) {
        boolean inStock = p.getQuantity() > 0;
        int qtyInCart = getCartQuantityForProduct(p.getId());

        JPanel card = new JPanel(new BorderLayout(0, 0)) {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { if (inStock) { hov = true; repaint(); } }
                    @Override public void mouseExited(MouseEvent e) { hov = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = 16;

                // Subtle ambient drop shadow
                g2.setColor(new Color(0, 50, 30, hov ? 14 : 5));
                g2.fillRoundRect(1, 2, w - 2, h - 2, arc, arc);

                // Card surface - Pure crisp white
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

                // Border: Forest Green on hover, Forest Mid when in cart, Sage Border normally
                g2.setColor(hov ? AppTheme.FOREST_GREEN : (qtyInCart > 0 ? AppTheme.FOREST_MID : AppTheme.BORDER_SAGE));
                g2.setStroke(new BasicStroke(hov || qtyInCart > 0 ? 1.5f : 1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(0, 280));

        // 1. TOP: Image Banner with Overlay Badges
        String catName = (p.getCategoryName() != null && !p.getCategoryName().trim().isEmpty())
                ? p.getCategoryName().toUpperCase()
                : "GENERAL";

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

                // Clip top corners so image doesn't bleed past rounded card border
                Shape oldClip = g2.getClip();
                g2.clip(new RoundRectangle2D.Float(0, 0, w, h + 16, 16, 16));

                // Clean soft mint background fill
                g2.setColor(new Color(244, 251, 246));
                g2.fillRect(0, 0, w, h);

                Image raw = ProductImageUtil.getProductRawImage(p);
                if (raw != null) {
                    int imgW = raw.getWidth(null);
                    int imgH = raw.getHeight(null);
                    if (imgW > 0 && imgH > 0) {
                        int padX = 14;
                        int padY = 8;
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

                // Subtle bottom divider line
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawLine(0, h - 1, w, h - 1);

                // Top-Left Floating Badge: Category Tag
                g2.setFont(AppTheme.font(Font.BOLD, 9));
                FontMetrics fm = g2.getFontMetrics();
                int catTextW = fm.stringWidth(catName);
                int tagW = catTextW + 14;
                int tagH = 18;
                g2.setColor(new Color(255, 255, 255, 230));
                g2.fillRoundRect(8, 8, tagW, tagH, 8, 8);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(8, 8, tagW - 1, tagH - 1, 8, 8);
                g2.setColor(AppTheme.FOREST_MID);
                g2.drawString(catName, 15, 8 + fm.getAscent() + (tagH - fm.getHeight()) / 2);

                // Top-Right Floating Badge: Stock Indicator
                String stockText = inStock ? (p.getQuantity() <= 5 ? "● Low: " + p.getQuantity() : "● In Stock") : "● Sold Out";
                int stockTextW = fm.stringWidth(stockText);
                int stockW = stockTextW + 14;
                g2.setColor(new Color(255, 255, 255, 230));
                g2.fillRoundRect(w - stockW - 8, 8, stockW, tagH, 8, 8);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(w - stockW - 8, 8, stockW - 1, tagH - 1, 8, 8);
                g2.setColor(inStock ? (p.getQuantity() <= 5 ? AppTheme.STATUS_WARNING : AppTheme.STATUS_SUCCESS) : AppTheme.STATUS_DANGER);
                g2.drawString(stockText, w - stockW - 8 + 7, 8 + fm.getAscent() + (tagH - fm.getHeight()) / 2);

                g2.setClip(oldClip);
                g2.dispose();
            }
        };
        imageBanner.setPreferredSize(new Dimension(0, 142));
        imageBanner.setOpaque(false);

        card.add(imageBanner, BorderLayout.NORTH);

        // 2. CENTER: Details Section
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);
        detailsPanel.setBorder(new EmptyBorder(10, 14, 12, 14));

        // Row 1: Product Name (Wrapped cleanly to 2 lines)
        String safeName = (p.getName() != null)
                ? p.getName().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                : "Product";
        JLabel lblName = new JLabel("<html><div style='width:190px; line-height:16px; font-weight:700; color:#0a2e21; font-family:Segoe UI, sans-serif; font-size:13px;'>" + safeName + "</div></html>");
        lblName.setPreferredSize(new Dimension(190, 36));
        lblName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Row 2: Barcode / SKU
        String codeStr = (p.getBarcode() != null && !p.getBarcode().isEmpty()) ? p.getBarcode() : p.getCode();
        JLabel lblCode = new JLabel("#" + codeStr);
        lblCode.setFont(AppTheme.font(Font.PLAIN, 10));
        lblCode.setForeground(AppTheme.TEXT_MUTED);
        lblCode.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Row 3: Price & Action Stepper
        JPanel bottomBox = new JPanel(new BorderLayout(6, 0));
        bottomBox.setOpaque(false);
        bottomBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        bottomBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPrice = new JLabel(AppTheme.formatCurrency(p.getSellingPrice()));
        lblPrice.setFont(AppTheme.font(Font.BOLD, 16));
        lblPrice.setForeground(AppTheme.FOREST_GREEN);

        JComponent actionWidget;
        if (!inStock) {
            JLabel lblSoldOut = new JLabel("Sold Out", SwingConstants.CENTER);
            lblSoldOut.setFont(AppTheme.font(Font.BOLD, 11));
            lblSoldOut.setForeground(AppTheme.TEXT_MUTED);
            lblSoldOut.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                    new EmptyBorder(4, 10, 4, 10)
            ));
            actionWidget = lblSoldOut;
        } else if (qtyInCart > 0) {
            // Interactive Stepper on Card: [ - ]  q in cart  [ + ]
            JPanel stepper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
            stepper.setOpaque(false);

            JButton btnMinus = createSmallCircleButton("-");
            btnMinus.addActionListener(e -> modifyCartItemQtyByProduct(p, -1));

            JLabel lblQtyBadge = new JLabel(String.valueOf(qtyInCart), SwingConstants.CENTER);
            lblQtyBadge.setFont(AppTheme.font(Font.BOLD, 13));
            lblQtyBadge.setForeground(AppTheme.FOREST_GREEN);
            lblQtyBadge.setPreferredSize(new Dimension(22, 24));

            JButton btnPlus = createSmallCircleButton("+");
            btnPlus.addActionListener(e -> modifyCartItemQtyByProduct(p, 1));

            stepper.add(btnMinus);
            stepper.add(lblQtyBadge);
            stepper.add(btnPlus);
            actionWidget = stepper;
        } else {
            // Standard Add Button
            JButton btnAdd = new JButton("➕ Add") {
                private boolean hov = false;
                {
                    addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) { hov = true; repaint(); }
                        @Override public void mouseExited(MouseEvent e) { hov = false; repaint(); }
                    });
                }
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int bw = getWidth();
                    int bh = getHeight();
                    int arc = bh;
                    g2.setColor(hov ? AppTheme.FOREST_MID : AppTheme.FOREST_GREEN);
                    g2.fillRoundRect(0, 0, bw, bh, arc, arc);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btnAdd.setFont(AppTheme.font(Font.BOLD, 11));
            btnAdd.setForeground(Color.WHITE);
            btnAdd.setFocusPainted(false);
            btnAdd.setContentAreaFilled(false);
            btnAdd.setBorderPainted(false);
            btnAdd.setOpaque(false);
            btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnAdd.setBorder(new EmptyBorder(4, 14, 4, 14));
            btnAdd.addActionListener(e -> addProductToCart(p));
            actionWidget = btnAdd;
        }

        bottomBox.add(lblPrice, BorderLayout.WEST);
        bottomBox.add(actionWidget, BorderLayout.EAST);

        detailsPanel.add(lblName);
        detailsPanel.add(Box.createVerticalStrut(2));
        detailsPanel.add(lblCode);
        detailsPanel.add(Box.createVerticalStrut(8));
        detailsPanel.add(bottomBox);

        card.add(detailsPanel, BorderLayout.CENTER);

        // Clicking image banner adds to cart
        imageBanner.setCursor(inStock ? new Cursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        imageBanner.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (inStock) addProductToCart(p);
            }
        });

        return card;
    }

    private JButton createSmallCircleButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hov = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hov = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(hov ? AppTheme.HOVER_SURFACE : AppTheme.BG_CANVAS);
                g2.fillRoundRect(0, 0, w, h, 8, 8);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 13));
        btn.setForeground(AppTheme.TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(0, 0, 0, 0));
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
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
        refreshCartDisplay();
        setToast("✓ Added: " + p.getName() + " (" + AppTheme.formatCurrency(p.getSellingPrice()) + ")", false);
        focusScanInput();
    }

    private void modifyCartItemQtyByProduct(Product p, int delta) {
        for (int i = 0; i < cartItems.size(); i++) {
            if (cartItems.get(i).product.getId() == p.getId()) {
                modifyCartItemQty(i, delta);
                return;
            }
        }
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
        refreshCartDisplay();
    }

    private void removeCartItem(int index) {
        if (index < 0 || index >= cartItems.size()) return;
        CartItem item = cartItems.remove(index);
        refreshCartDisplay();
        setToast("Removed " + item.product.getName() + " from cart.", false);
        focusScanInput();
    }

    private void clearCart() {
        if (cartItems.isEmpty()) return;
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear your cart?", "Clear Cart", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            cartItems.clear();
            refreshCartDisplay();
            setToast("Cart cleared. Ready for next item.", false);
            focusScanInput();
        }
    }

    /**
     * Refreshes the shopping cart item cards, calculations, and catalog buttons.
     */
    private void refreshCartDisplay() {
        renderCartItemsList();
        renderCatalogGrid(catalogProducts); // sync on-card stepper buttons

        double subtotal = 0.0;
        int totalUnits = 0;

        for (CartItem ci : cartItems) {
            subtotal += ci.getSubtotal();
            totalUnits += ci.quantity;
        }

        // Standard GST calculate or 0%
        double gstRate = 0.0;
        try {
            gstRate = Double.parseDouble(config.AppSettings.getString(config.AppSettings.KEY_STORE_GST, "18.0"));
        } catch (Exception ignored) {}

        double gstAmount = subtotal * (gstRate / 100.0);
        double grandTotal = subtotal + gstAmount;

        lblCartItemCount.setText("(" + totalUnits + " items)");
        lblSubtotalValue.setText(AppTheme.formatCurrency(subtotal));
        lblGstValue.setText(AppTheme.formatCurrency(gstAmount));
        lblGrandTotalValue.setText(AppTheme.formatCurrency(grandTotal));

        btnPayNow.setText(String.format("💳 PROCEED TO PAY (%s)", AppTheme.formatCurrency(grandTotal)));
        btnPayNow.setEnabled(!cartItems.isEmpty());
        btnClearCart.setEnabled(!cartItems.isEmpty());
    }

    /**
     * Renders modern interactive cart item cards with inline steppers.
     */
    private void renderCartItemsList() {
        if (cartItemsContainer == null) return;
        cartItemsContainer.removeAll();

        if (cartItems.isEmpty()) {
            // Friendly Empty Cart Graphic
            JPanel emptyState = new JPanel(new GridBagLayout());
            emptyState.setOpaque(false);
            emptyState.setBorder(new EmptyBorder(40, 20, 40, 20));

            JPanel box = new JPanel();
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setOpaque(false);

            JLabel lblEmptyIcon = new JLabel("🛍️");
            lblEmptyIcon.setFont(AppTheme.font(Font.PLAIN, 48));
            lblEmptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblEmptyTitle = new JLabel("Your cart is empty");
            lblEmptyTitle.setFont(AppTheme.font(Font.BOLD, 15));
            lblEmptyTitle.setForeground(AppTheme.TEXT_PRIMARY);
            lblEmptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblEmptySub = new JLabel("<html><div style='text-align:center;'>Scan an item barcode or tap any<br>product from the catalog to begin.</div></html>");
            lblEmptySub.setFont(AppTheme.font(Font.PLAIN, 12));
            lblEmptySub.setForeground(AppTheme.TEXT_MUTED);
            lblEmptySub.setAlignmentX(Component.CENTER_ALIGNMENT);

            box.add(lblEmptyIcon);
            box.add(Box.createVerticalStrut(8));
            box.add(lblEmptyTitle);
            box.add(Box.createVerticalStrut(4));
            box.add(lblEmptySub);

            emptyState.add(box);
            cartItemsContainer.add(emptyState);
        } else {
            for (int i = 0; i < cartItems.size(); i++) {
                final int idx = i;
                CartItem ci = cartItems.get(i);
                cartItemsContainer.add(createCartItemCard(ci, idx));
                cartItemsContainer.add(Box.createVerticalStrut(8));
            }
        }

        cartItemsContainer.revalidate();
        cartItemsContainer.repaint();
    }

    /**
     * Modern touch-friendly card for an item in the cart.
     */
    private JPanel createCartItemCard(CartItem ci, int index) {
        JPanel card = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(AppTheme.BG_CANVAS);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        card.setPreferredSize(new Dimension(340, 58));
        card.setBorder(new EmptyBorder(6, 12, 6, 10));

        // Left info: Product name & Unit price
        JPanel infoBox = new JPanel(new GridLayout(2, 1, 0, 2));
        infoBox.setOpaque(false);

        JLabel lblName = new JLabel(ci.product.getName());
        lblName.setFont(AppTheme.font(Font.BOLD, 12));
        lblName.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel lblUnitPrice = new JLabel(AppTheme.formatCurrency(ci.unitPrice) + " each");
        lblUnitPrice.setFont(AppTheme.font(Font.PLAIN, 11));
        lblUnitPrice.setForeground(AppTheme.TEXT_MUTED);

        infoBox.add(lblName);
        infoBox.add(lblUnitPrice);

        // Right Stepper & Total
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        actionBox.setOpaque(false);

        JButton btnMinus = createSmallCircleButton("-");
        btnMinus.addActionListener(e -> modifyCartItemQty(index, -1));

        JLabel lblQty = new JLabel(String.valueOf(ci.quantity), SwingConstants.CENTER);
        lblQty.setFont(AppTheme.font(Font.BOLD, 13));
        lblQty.setForeground(AppTheme.TEXT_PRIMARY);
        lblQty.setPreferredSize(new Dimension(20, 24));

        JButton btnPlus = createSmallCircleButton("+");
        btnPlus.addActionListener(e -> modifyCartItemQty(index, 1));

        JLabel lblLineTotal = new JLabel(AppTheme.formatCurrency(ci.getSubtotal()), SwingConstants.RIGHT);
        lblLineTotal.setFont(AppTheme.font(Font.BOLD, 13));
        lblLineTotal.setForeground(AppTheme.FOREST_GREEN);
        lblLineTotal.setPreferredSize(new Dimension(58, 24));

        JButton btnTrash = createSmallCircleButton("✕");
        btnTrash.setFont(AppTheme.font(Font.BOLD, 12));
        btnTrash.setForeground(new Color(220, 38, 38));
        btnTrash.addActionListener(e -> removeCartItem(index));

        actionBox.add(btnMinus);
        actionBox.add(lblQty);
        actionBox.add(btnPlus);
        actionBox.add(lblLineTotal);
        actionBox.add(btnTrash);

        card.add(infoBox, BorderLayout.CENTER);
        card.add(actionBox, BorderLayout.EAST);

        return card;
    }

    // ==========================================
    // 5. INTERACTIVE PAYMENT FLOW & RECEIPT
    // ==========================================
    private void startSelfCheckoutPayment() {
        if (cartItems.isEmpty()) return;

        double subtotal = 0.0;
        for (CartItem ci : cartItems) subtotal += ci.getSubtotal();
        double gstRate = 0.0;
        try {
            gstRate = Double.parseDouble(config.AppSettings.getString(config.AppSettings.KEY_STORE_GST, "18.0"));
        } catch (Exception ignored) {}

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
            refreshCartDisplay();
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
        lblStatusToast.setForeground(isError ? AppTheme.STATUS_DANGER : AppTheme.FOREST_GREEN);
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

    /**
     * Shared Pill Button Utility matching Keyra aesthetic.
     */
    public static JButton createPillButton(String text, Color bg, Color hoverBg, Color fg, Color borderColor) {
        JButton btn = new JButton(text) {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) { hov = true; repaint(); } }
                    @Override public void mouseExited(MouseEvent e) { hov = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                int arc = h;
                if (!isEnabled()) {
                    g2.setColor(new Color(241, 245, 249));
                } else if (hov) {
                    g2.setColor(hoverBg);
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, w, h, arc, arc);
                if (borderColor != null) {
                    g2.setColor(borderColor);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(8, 14, 8, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
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

            // Header Banner: Forest Green with Electric Lime amount badge
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(AppTheme.FOREST_GREEN);
            header.setBorder(new EmptyBorder(14, 18, 14, 18));

            JLabel lblH1 = new JLabel("💳 Express Self-Checkout Payment");
            lblH1.setFont(AppTheme.font(Font.BOLD, 17));
            lblH1.setForeground(Color.WHITE);

            JLabel lblTotalBadge = new JLabel(AppTheme.formatCurrency(grandTotal));
            lblTotalBadge.setFont(AppTheme.font(Font.BOLD, 20));
            lblTotalBadge.setForeground(AppTheme.ELECTRIC_LIME);

            header.add(lblH1, BorderLayout.WEST);
            header.add(lblTotalBadge, BorderLayout.EAST);

            // Center: Phone Input + Tabbed Payment Method
            JPanel center = new JPanel(new BorderLayout(0, 12));
            center.setOpaque(false);

            // Optional Customer Phone
            JPanel phoneCard = new JPanel(new BorderLayout(10, 4));
            phoneCard.setBackground(AppTheme.BG_CANVAS);
            phoneCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER_SAGE),
                    new EmptyBorder(10, 14, 10, 14)
            ));

            JLabel lblPhonePrompt = new JLabel("📱 Mobile Number for Digital Receipt & WhatsApp Bill (Optional):");
            lblPhonePrompt.setFont(AppTheme.font(Font.BOLD, 12));
            lblPhonePrompt.setForeground(AppTheme.TEXT_SECONDARY);

            txtCustomerPhone = new JTextField();
            txtCustomerPhone.setFont(AppTheme.font(Font.BOLD, 15));
            txtCustomerPhone.setForeground(AppTheme.TEXT_PRIMARY);
            txtCustomerPhone.setCaretColor(AppTheme.FOREST_GREEN);
            txtCustomerPhone.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER_SAGE),
                    new EmptyBorder(6, 10, 6, 10)
            ));

            phoneCard.add(lblPhonePrompt, BorderLayout.NORTH);
            phoneCard.add(txtCustomerPhone, BorderLayout.CENTER);

            center.add(phoneCard, BorderLayout.NORTH);

            // Tabbed Payment Options: UPI QR (Default), Card, Cash
            JTabbedPane tabPayment = new JTabbedPane();
            tabPayment.setFont(AppTheme.font(Font.BOLD, 13));

            tabPayment.addTab("📱 Scan & Pay with UPI QR", createUpiTab());
            tabPayment.addTab("💳 Credit / Debit Card", createCardTab());
            tabPayment.addTab("💵 Cash at Counter", createCashTab());

            center.add(tabPayment, BorderLayout.CENTER);

            // Bottom Cancel / Exit button
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            bottom.setOpaque(false);

            JButton btnCancel = new JButton("Cancel & Back to Cart");
            btnCancel.setFont(AppTheme.font(Font.PLAIN, 13));
            btnCancel.setForeground(AppTheme.TEXT_MUTED);
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
            lblI1.setFont(AppTheme.font(Font.BOLD, 13));
            lblI1.setForeground(AppTheme.TEXT_PRIMARY);

            JLabel lblI2 = new JLabel("2. Scan QR code to pay exact amount: " + AppTheme.formatCurrency(grandTotal), SwingConstants.CENTER);
            lblI2.setFont(AppTheme.font(Font.PLAIN, 12));
            lblI2.setForeground(AppTheme.TEXT_MUTED);

            instructions.add(lblI1);
            instructions.add(lblI2);

            JButton btnUpiConfirm = createPillButton("✅ I Have Completed UPI Payment (Confirm & Print)", AppTheme.FOREST_GREEN, AppTheme.FOREST_MID, Color.WHITE, null);
            btnUpiConfirm.setFont(AppTheme.font(Font.BOLD, 14));
            btnUpiConfirm.setPreferredSize(new Dimension(0, 46));
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
            lblIcon.setFont(AppTheme.font(Font.PLAIN, 48));

            JLabel lblMsg = new JLabel("Please Tap, Swipe, or Insert your Card on the POS Terminal", SwingConstants.CENTER);
            lblMsg.setFont(AppTheme.font(Font.BOLD, 15));
            lblMsg.setForeground(AppTheme.TEXT_PRIMARY);

            JLabel lblSub = new JLabel("Amount to charge: " + AppTheme.formatCurrency(grandTotal), SwingConstants.CENTER);
            lblSub.setFont(AppTheme.font(Font.PLAIN, 13));
            lblSub.setForeground(AppTheme.TEXT_MUTED);

            center.add(lblIcon);
            center.add(lblMsg);
            center.add(lblSub);

            JButton btnCardConfirm = createPillButton("✅ Card Payment Approved (Confirm & Print)", AppTheme.FOREST_GREEN, AppTheme.FOREST_MID, Color.WHITE, null);
            btnCardConfirm.setFont(AppTheme.font(Font.BOLD, 14));
            btnCardConfirm.setPreferredSize(new Dimension(0, 46));
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
            lblIcon.setFont(AppTheme.font(Font.PLAIN, 48));

            JLabel lblMsg = new JLabel("Pay with Cash at Express Checkout Counter", SwingConstants.CENTER);
            lblMsg.setFont(AppTheme.font(Font.BOLD, 15));
            lblMsg.setForeground(AppTheme.TEXT_PRIMARY);

            JLabel lblSub = new JLabel("Please deposit " + AppTheme.formatCurrency(grandTotal) + " with the attendant.", SwingConstants.CENTER);
            lblSub.setFont(AppTheme.font(Font.PLAIN, 13));
            lblSub.setForeground(AppTheme.TEXT_MUTED);

            center.add(lblIcon);
            center.add(lblMsg);
            center.add(lblSub);

            JButton btnCashConfirm = createPillButton("✅ Cash Received & Verified (Confirm & Print)", AppTheme.FOREST_GREEN, AppTheme.FOREST_MID, Color.WHITE, null);
            btnCashConfirm.setFont(AppTheme.font(Font.BOLD, 14));
            btnCashConfirm.setPreferredSize(new Dimension(0, 46));
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
            lblSuccessIcon.setFont(AppTheme.font(Font.PLAIN, 46));

            JLabel lblTitle = new JLabel("PAYMENT SUCCESSFUL!", SwingConstants.CENTER);
            lblTitle.setFont(AppTheme.font(Font.BOLD, 18));
            lblTitle.setForeground(AppTheme.FOREST_GREEN);

            JLabel lblInv = new JLabel("Invoice #: " + sale.getInvoiceNo(), SwingConstants.CENTER);
            lblInv.setFont(AppTheme.font(Font.BOLD, 14));
            lblInv.setForeground(AppTheme.TEXT_PRIMARY);

            JLabel lblAmt = new JLabel("Total Paid: " + AppTheme.formatCurrency(sale.getTotalAmount()) + " via " + sale.getPaymentMode(), SwingConstants.CENTER);
            lblAmt.setFont(AppTheme.font(Font.BOLD, 15));
            lblAmt.setForeground(AppTheme.FOREST_MID);

            JLabel lblThanks = new JLabel("Thank you for shopping at SmartBilling!", SwingConstants.CENTER);
            lblThanks.setFont(AppTheme.font(Font.ITALIC, 12));
            lblThanks.setForeground(AppTheme.TEXT_MUTED);

            center.add(lblSuccessIcon);
            center.add(lblTitle);
            center.add(lblInv);
            center.add(lblAmt);
            center.add(lblThanks);

            JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
            actions.setOpaque(false);

            JButton btnPrint = createPillButton("🖨️ Print Receipt", AppTheme.FOREST_MID, AppTheme.FOREST_DEEP, Color.WHITE, null);
            btnPrint.setFont(AppTheme.font(Font.BOLD, 13));
            btnPrint.addActionListener(e -> {
                new InvoiceDialog((Frame) getOwner(), sale).setVisible(true);
            });

            JButton btnDone = createPillButton("👤 Next Customer", AppTheme.FOREST_GREEN, AppTheme.FOREST_MID, Color.WHITE, null);
            btnDone.setFont(AppTheme.font(Font.BOLD, 13));
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
