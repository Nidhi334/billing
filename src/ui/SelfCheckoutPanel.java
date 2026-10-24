package ui;

import config.AppSettings;
import dao.BillingDAO;
import dao.CategoryDAO;
import dao.CustomerDAO;
import dao.ProductDAO;
import model.*;
import util.AppIconUtil;
import util.ProductImageUtil;
import util.QrCodeGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.swing.Timer;

/**
 * Modern, Minimalist Customer Self-Checkout Terminal.
 * Follows the Dashboard Design System:
 * - Elevated crisp white cards (border radius 36, soft ambient shadow, no harsh border)
 * - 16px grid spacing
 * - High-DPI Vector SVG icons (no emoji glyphs)
 * - Stadium pill sidebar navigation matching the main dashboard
 * - High-contrast readable buttons with clear enabled/disabled states
 * - Clutter-free, streamlined self-checkout workflow
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

        setLayout(new BorderLayout(0, 0));
        setBackground(AppTheme.BG_CANVAS);
        setBorder(null);
        setOpaque(true);

        initUI();
        loadCatalog();
        loadCategories();

        // Start live clock
        Timer clockTimer = new Timer(1000, e -> updateClock());
        clockTimer.start();
        updateClock();

        SwingUtilities.invokeLater(this::focusScanInput);
    }

    private void initUI() {
        // 1. Full Left Sidebar (Same 228px width & styling as Admin Portal)
        add(createLeftSidebar(), BorderLayout.WEST);

        // 2. Main Right Container (Header + Content Split)
        JPanel mainContainer = new JPanel(new BorderLayout(0, 14));
        mainContainer.setOpaque(false);
        mainContainer.setBorder(new EmptyBorder(16, 16, 16, 20));

        // Top Header Card
        mainContainer.add(createHeader(), BorderLayout.NORTH);

        // Center Split Pane (Left: Catalog & Scan Section, Right: Cart & Summary)
        JPanel centerSplit = new JPanel(new BorderLayout(16, 0));
        centerSplit.setOpaque(false);

        centerSplit.add(createCatalogAndScanSection(), BorderLayout.CENTER);
        centerSplit.add(createCartAndCheckoutSection(), BorderLayout.EAST);

        mainContainer.add(centerSplit, BorderLayout.CENTER);

        add(mainContainer, BorderLayout.CENTER);
    }

    // ==========================================
    // FULL LEFT SIDEBAR (Exact Match to Admin Portal Layout & Dimensions)
    // ==========================================
    private JPanel createLeftSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(AppTheme.BG_CANVAS);
        sidebar.setPreferredSize(new Dimension(228, 0));
        sidebar.setMinimumSize(new Dimension(228, 0));
        sidebar.setBorder(null);

        // 1. Sidebar Header (Brand Mark & Station Name)
        JPanel sidebarHeader = new JPanel(new BorderLayout());
        sidebarHeader.setOpaque(false);
        sidebarHeader.setBorder(new EmptyBorder(20, 14, 14, 14));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brand.setOpaque(false);
        JLabel icon = new JLabel(AppIconUtil.getSquircleImageIcon(26));

        JPanel brandText = new JPanel();
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        brandText.setOpaque(false);

        JLabel title = new JLabel("SmartBilling Pro");
        title.setFont(AppTheme.font(Font.BOLD, 14));
        title.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Express Checkout");
        subtitle.setFont(AppTheme.font(Font.PLAIN, 10));
        subtitle.setForeground(AppTheme.TEXT_MUTED);

        brandText.add(title);
        brandText.add(Box.createVerticalStrut(1));
        brandText.add(subtitle);

        brand.add(icon);
        brand.add(brandText);
        sidebarHeader.add(brand, BorderLayout.CENTER);
        sidebar.add(sidebarHeader, BorderLayout.NORTH);

        // 2. Center Categories Navigation
        JPanel navContainer = new JPanel(new BorderLayout(0, 6));
        navContainer.setOpaque(false);

        JLabel lblCategoryHeader = new JLabel("CATEGORIES", new SidebarIcon("tag", 13, AppTheme.TEXT_MUTED), SwingConstants.LEFT);
        lblCategoryHeader.setFont(AppTheme.font(Font.BOLD, 11));
        lblCategoryHeader.setForeground(AppTheme.TEXT_MUTED);
        lblCategoryHeader.setIconTextGap(8);
        lblCategoryHeader.setBorder(new EmptyBorder(4, 14, 6, 14));
        navContainer.add(lblCategoryHeader, BorderLayout.NORTH);

        categoryTabsPanel = new JPanel();
        categoryTabsPanel.setLayout(new BoxLayout(categoryTabsPanel, BoxLayout.Y_AXIS));
        categoryTabsPanel.setOpaque(false);
        categoryTabsPanel.setBorder(new EmptyBorder(2, 10, 10, 10));

        JScrollPane navScroll = new JScrollPane(categoryTabsPanel);
        navScroll.setBorder(null);
        navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false);
        navScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.getVerticalScrollBar().setUnitIncrement(16);
        navContainer.add(navScroll, BorderLayout.CENTER);

        sidebar.add(navContainer, BorderLayout.CENTER);

        // 3. Bottom Footer (Matching Dashboard Floating Profile Card with Back to Login)
        JPanel sidebarFooter = new JPanel(new BorderLayout());
        sidebarFooter.setOpaque(false);
        sidebarFooter.setBorder(new EmptyBorder(6, 10, 16, 10));

        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int arc = 28;

                // Ambient shadow
                g2.setColor(new Color(0, 50, 30, 7));
                g2.fillRoundRect(1, 2, w - 2, h - 2, arc, arc);

                // Card surface - Pure crisp white, borderless
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel footerContent = new JPanel();
        footerContent.setLayout(new BoxLayout(footerContent, BoxLayout.Y_AXIS));
        footerContent.setOpaque(false);

        // Row 1: Station Info
        JPanel stationRow = new JPanel(new BorderLayout(8, 0));
        stationRow.setOpaque(false);
        stationRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComponent stationAvatar = createStationAvatar(34);
        stationRow.add(stationAvatar, BorderLayout.WEST);

        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);

        JLabel stationLbl = new JLabel("Self-Checkout");
        stationLbl.setFont(AppTheme.font(Font.BOLD, 12));
        stationLbl.setForeground(AppTheme.TEXT_PRIMARY);

        JPanel subRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        subRow.setOpaque(false);

        JLabel statusDot = new JLabel("●");
        statusDot.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));
        statusDot.setForeground(new Color(34, 197, 94)); // vibrant green dot

        JLabel modeLbl = new JLabel("Express Station #1");
        modeLbl.setFont(AppTheme.font(Font.PLAIN, 10));
        modeLbl.setForeground(AppTheme.TEXT_MUTED);

        subRow.add(statusDot);
        subRow.add(modeLbl);

        details.add(stationLbl);
        details.add(Box.createVerticalStrut(2));
        details.add(subRow);

        stationRow.add(details, BorderLayout.CENTER);
        footerContent.add(stationRow);

        // Inner Hairline Divider
        footerContent.add(Box.createVerticalStrut(10));
        JSeparator sep = new JSeparator() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(210, 234, 198, 120));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setPreferredSize(new Dimension(100, 1));
        footerContent.add(sep);
        footerContent.add(Box.createVerticalStrut(10));

        // Row 2: Back to Login Button
        JButton btnBackToLogin = createBackToLoginButton();
        btnBackToLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        footerContent.add(btnBackToLogin);

        card.add(footerContent, BorderLayout.CENTER);
        sidebarFooter.add(card, BorderLayout.CENTER);
        sidebar.add(sidebarFooter, BorderLayout.SOUTH);

        return sidebar;
    }

    private JComponent createStationAvatar(int size) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(size, size));
                setMinimumSize(new Dimension(size, size));
                setMaximumSize(new Dimension(size, size));
                setOpaque(false);
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int sz = Math.min(w, h);
                int x = (w - sz) / 2;
                int y = (h - sz) / 2;

                GradientPaint gp = new GradientPaint(x, y, AppTheme.FOREST_MID, x + sz, y + sz, AppTheme.FOREST_DEEP);
                g2.setPaint(gp);
                g2.fillOval(x, y, sz, sz);

                g2.setColor(new Color(255, 255, 255, 45));
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(x, y, sz - 1, sz - 1);

                SidebarIcon icon = new SidebarIcon("cart", Math.round(sz * 0.52f), Color.WHITE);
                int ix = x + (sz - icon.getIconWidth()) / 2;
                int iy = y + (sz - icon.getIconHeight()) / 2;
                icon.paintIcon(this, g2, ix, iy);

                g2.dispose();
            }
        };
    }

    private JButton createBackToLoginButton() {
        JButton btn = new JButton("Back to Login", new SidebarIcon("logout", 14, AppTheme.TEXT_SECONDARY)) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        setForeground(AppTheme.STATUS_DANGER);
                        setIcon(new SidebarIcon("logout", 14, AppTheme.STATUS_DANGER));
                        repaint();
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        hovered = false;
                        setForeground(AppTheme.TEXT_SECONDARY);
                        setIcon(new SidebarIcon("logout", 14, AppTheme.TEXT_SECONDARY));
                        repaint();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int arc = 12;
                if (hovered) {
                    g2.setColor(new Color(254, 242, 242));
                    g2.fillRoundRect(0, 0, w, h, arc, arc);
                    g2.setColor(new Color(254, 202, 202));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                } else {
                    g2.setColor(new Color(246, 250, 248));
                    g2.fillRoundRect(0, 0, w, h, arc, arc);
                    g2.setColor(new Color(210, 234, 198, 120));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 11));
        btn.setForeground(AppTheme.TEXT_SECONDARY);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setIconTextGap(8);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(7, 10, 7, 10));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        btn.setPreferredSize(new Dimension(184, 32));
        btn.setToolTipText("Close self-checkout and return to login screen");
        btn.addActionListener(e -> backToLogin());
        return btn;
    }

    public void backToLogin() {
        if (!cartItems.isEmpty()) {
            int res = JOptionPane.showConfirmDialog(this,
                    "Cart contains items. Are you sure you want to exit self-checkout?",
                    "Exit Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (res != JOptionPane.YES_OPTION) return;
        }

        Window currentWindow = SwingUtilities.getWindowAncestor(this);
        if (currentWindow instanceof DashboardFrame && exitCallback != null) {
            exitCallback.run();
            return;
        }

        if (currentWindow != null) {
            currentWindow.dispose();
        }

        // Show existing LoginFrame or launch a new LoginFrame
        boolean foundLoginFrame = false;
        for (Frame frame : Frame.getFrames()) {
            if (frame instanceof LoginFrame && frame.isDisplayable()) {
                frame.setVisible(true);
                frame.toFront();
                frame.requestFocus();
                foundLoginFrame = true;
                break;
            }
        }
        if (!foundLoginFrame) {
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }

    /**
     * Shared Card Builder:
     * Pure crisp white surface, border radius 36, soft ambient shadow, no harsh border line.
     * Matches Dashboard and Billing terminal cards.
     */
    private JPanel createElevatedCard(LayoutManager layout) {
        JPanel card = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = 36; // Exact same border radius as dashboard cards

                // Soft ambient shadow
                g2.setColor(new Color(0, 50, 30, 6));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);

                // Card surface - Pure crisp white
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        return card;
    }

    // ==========================================
    // 1. CLEAN TOP HEADER CARD
    // ==========================================
    private JPanel createHeader() {
        JPanel header = createElevatedCard(new BorderLayout(16, 0));
        header.setBorder(new EmptyBorder(14, 22, 14, 22));

        // Brand & Subtitle
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandBox.setOpaque(false);

        // macOS squircle storefront icon badge
        JComponent iconBadge = AppIconUtil.createBrandBadge(38);

        JPanel titleTextPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titleTextPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Self-Checkout Terminal");
        lblTitle.setFont(AppTheme.font(Font.BOLD, 18));
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel lblSubtitle = new JLabel("Scan barcodes or select products to complete your order");
        lblSubtitle.setFont(AppTheme.font(Font.PLAIN, 12));
        lblSubtitle.setForeground(AppTheme.TEXT_SECONDARY);

        titleTextPanel.add(lblTitle);
        titleTextPanel.add(lblSubtitle);

        brandBox.add(iconBadge);
        brandBox.add(titleTextPanel);

        // Right side: Clean Live Clock (No fullscreen button, No back to dashboard button)
        JPanel rightInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightInfo.setOpaque(false);

        lblLiveClock = new JLabel("Loading clock...", new SidebarIcon("orders", 13, AppTheme.TEXT_SECONDARY), SwingConstants.LEFT);
        lblLiveClock.setFont(AppTheme.font(Font.BOLD, 12));
        lblLiveClock.setForeground(AppTheme.TEXT_SECONDARY);
        lblLiveClock.setIconTextGap(6);
        rightInfo.add(lblLiveClock);

        header.add(brandBox, BorderLayout.WEST);
        header.add(rightInfo, BorderLayout.EAST);

        return header;
    }

    // ==========================================
    // 2. SCANNING & CATALOG SECTION (LEFT COLUMN)
    // ==========================================
    private JPanel createCatalogAndScanSection() {
        JPanel left = new JPanel(new BorderLayout(0, 16));
        left.setOpaque(false);

        // Top Search Bar (Sleek stadium pill card, no harsh border)
        JPanel searchBarWrapper = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = 24;
                g2.setColor(new Color(0, 50, 30, 5));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        searchBarWrapper.setOpaque(false);
        searchBarWrapper.setBorder(new EmptyBorder(10, 18, 10, 14));

        JLabel lblScanIcon = new JLabel(new SidebarIcon("search", 16, AppTheme.TEXT_SECONDARY));

        txtSearchScan = new JTextField();
        txtSearchScan.setFont(AppTheme.font(Font.PLAIN, 14));
        txtSearchScan.setForeground(AppTheme.TEXT_PRIMARY);
        txtSearchScan.setCaretColor(AppTheme.FOREST_GREEN);
        txtSearchScan.setBorder(null);
        txtSearchScan.putClientProperty("JTextField.placeholderText", "Scan barcode or type product name/code to add...");
        txtSearchScan.addActionListener(e -> handleScanOrSearch());

        JButton btnCamera = new JButton("Camera Scan", new SidebarIcon("camera", 13, AppTheme.FOREST_GREEN)) {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hov = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(hov ? AppTheme.LIME_PALE : new Color(248, 250, 252));
                g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.setColor(hov ? AppTheme.FOREST_MID : AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnCamera.setFont(AppTheme.font(Font.BOLD, 12));
        btnCamera.setForeground(AppTheme.FOREST_GREEN);
        btnCamera.setFocusPainted(false);
        btnCamera.setContentAreaFilled(false);
        btnCamera.setBorderPainted(false);
        btnCamera.setOpaque(false);
        btnCamera.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCamera.setIconTextGap(6);
        btnCamera.setBorder(new EmptyBorder(6, 14, 6, 14));
        btnCamera.addActionListener(e -> openCameraBarcodeScanner());

        JButton btnClearSearch = new JButton("✕");
        btnClearSearch.setFont(AppTheme.font(Font.BOLD, 12));
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

        JPanel rightSearchActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightSearchActions.setOpaque(false);
        rightSearchActions.add(btnClearSearch);
        rightSearchActions.add(btnCamera);

        searchBarWrapper.add(lblScanIcon, BorderLayout.WEST);
        searchBarWrapper.add(txtSearchScan, BorderLayout.CENTER);
        searchBarWrapper.add(rightSearchActions, BorderLayout.EAST);

        // Toast feedback label (Subtle and non-intrusive)
        lblStatusToast = new JLabel("");
        lblStatusToast.setFont(AppTheme.font(Font.BOLD, 12));
        lblStatusToast.setForeground(AppTheme.FOREST_GREEN);
        lblStatusToast.setBorder(new EmptyBorder(0, 6, 0, 6));

        JPanel topArea = new JPanel(new BorderLayout(0, 4));
        topArea.setOpaque(false);
        topArea.add(searchBarWrapper, BorderLayout.NORTH);
        topArea.add(lblStatusToast, BorderLayout.SOUTH);

        // 3. Visual Catalog Grid (CENTER)
        catalogGridPanel = new JPanel(new GridLayout(0, 4, 12, 12));
        catalogGridPanel.setOpaque(false);
        catalogGridPanel.addComponentListener(new ComponentAdapter() {
            private int prevCols = 4;
            @Override
            public void componentResized(ComponentEvent e) {
                int cols = getCatalogColumnCount();
                if (cols != prevCols && catalogGridPanel.getLayout() instanceof GridLayout) {
                    prevCols = cols;
                    GridLayout gl = (GridLayout) catalogGridPanel.getLayout();
                    gl.setColumns(cols);
                    catalogGridPanel.revalidate();
                }
            }
        });

        JPanel catalogScrollContent = new JPanel(new BorderLayout());
        catalogScrollContent.setOpaque(false);
        catalogScrollContent.setBorder(new EmptyBorder(0, 0, 8, 0));
        catalogScrollContent.add(catalogGridPanel, BorderLayout.NORTH);

        JScrollPane catalogScroll = new JScrollPane(catalogScrollContent);
        catalogScroll.setBorder(null);
        catalogScroll.setOpaque(false);
        catalogScroll.getViewport().setOpaque(false);
        catalogScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        catalogScroll.getVerticalScrollBar().setUnitIncrement(24);

        left.add(topArea, BorderLayout.NORTH);
        left.add(catalogScroll, BorderLayout.CENTER);

        return left;
    }

    // ==========================================
    // 3. CART & CHECKOUT SECTION (RIGHT COLUMN)
    // ==========================================
    private JPanel createCartAndCheckoutSection() {
        JPanel right = createElevatedCard(new BorderLayout(0, 14));
        right.setPreferredSize(new Dimension(380, 0));
        right.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Cart Header
        JPanel cartHeader = new JPanel(new BorderLayout(8, 0));
        cartHeader.setOpaque(false);

        JLabel lblCartTitle = new JLabel("Shopping Cart", new SidebarIcon("cart", 16, AppTheme.TEXT_PRIMARY), SwingConstants.LEFT);
        lblCartTitle.setFont(AppTheme.font(Font.BOLD, 16));
        lblCartTitle.setForeground(AppTheme.TEXT_PRIMARY);
        lblCartTitle.setIconTextGap(8);

        lblCartItemCount = new JLabel("(0 items)");
        lblCartItemCount.setFont(AppTheme.font(Font.BOLD, 12));
        lblCartItemCount.setForeground(AppTheme.TEXT_MUTED);

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleBox.setOpaque(false);
        titleBox.add(lblCartTitle);
        titleBox.add(lblCartItemCount);

        btnClearCart = new JButton("Clear", new SidebarIcon("trash", 12, new Color(220, 38, 38))) {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) { hov = true; repaint(); } }
                    @Override public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                if (isEnabled()) {
                    g2.setColor(hov ? new Color(254, 226, 226) : new Color(254, 242, 242));
                    g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
                }
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
        btnClearCart.setIconTextGap(4);
        btnClearCart.setBorder(new EmptyBorder(4, 10, 4, 10));
        btnClearCart.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClearCart.setEnabled(false);
        btnClearCart.addActionListener(e -> clearCart());

        cartHeader.add(titleBox, BorderLayout.WEST);
        cartHeader.add(btnClearCart, BorderLayout.EAST);

        // Cart Items Scrollable List
        cartItemsContainer = new JPanel();
        cartItemsContainer.setLayout(new BoxLayout(cartItemsContainer, BoxLayout.Y_AXIS));
        cartItemsContainer.setOpaque(false);

        JScrollPane cartScroll = new JScrollPane(cartItemsContainer);
        cartScroll.setBorder(null);
        cartScroll.setOpaque(false);
        cartScroll.getViewport().setOpaque(false);
        cartScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        cartScroll.getVerticalScrollBar().setUnitIncrement(18);

        // Order Summary Box - Clean Mint Canvas card with no harsh borders
        JPanel summaryBox = new JPanel(new GridLayout(3, 2, 6, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int arc = 20;
                g2.setColor(AppTheme.BG_CANVAS);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        summaryBox.setOpaque(false);
        summaryBox.setBorder(new EmptyBorder(14, 16, 14, 16));

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
        lblGrandTotal.setFont(AppTheme.font(Font.BOLD, 13));
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

        // Big Checkout Action Button (Always high-contrast, crystal clear text & background)
        btnPayNow = new JButton("Add items to checkout") {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) { hov = true; repaint(); } }
                    @Override public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int arc = h;

                if (!isEnabled()) {
                    // Crisp, readable disabled state: soft neutral background
                    g2.setColor(new Color(241, 245, 249));
                    g2.fillRoundRect(0, 0, w, h, arc, arc);
                    g2.setColor(new Color(148, 163, 184)); // High-contrast legible text
                } else {
                    // Active state: Rich forest green
                    g2.setColor(getModel().isPressed() ? AppTheme.FOREST_DEEP : (hov ? AppTheme.FOREST_MID : AppTheme.FOREST_GREEN));
                    g2.fillRoundRect(0, 0, w, h, arc, arc);
                    g2.setColor(Color.WHITE);
                }

                // Explicit text & icon rendering to avoid Swing disabled gray wash
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                String txt = getText();
                int textW = fm.stringWidth(txt);
                int textX = (w - textW) / 2;
                int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(txt, textX, textY);

                g2.dispose();
            }
        };
        btnPayNow.setFont(AppTheme.font(Font.BOLD, 15));
        btnPayNow.setForeground(Color.WHITE);
        btnPayNow.setFocusPainted(false);
        btnPayNow.setContentAreaFilled(false);
        btnPayNow.setBorderPainted(false);
        btnPayNow.setOpaque(false);
        btnPayNow.setPreferredSize(new Dimension(0, 50));
        btnPayNow.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPayNow.setEnabled(false);
        btnPayNow.addActionListener(e -> startSelfCheckoutPayment());

        // Assemble Right Panel
        JPanel centerCart = new JPanel(new BorderLayout());
        centerCart.setOpaque(false);
        centerCart.add(cartScroll, BorderLayout.CENTER);

        JPanel bottomSummary = new JPanel(new BorderLayout(0, 10));
        bottomSummary.setOpaque(false);
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

        // "All Items" tab with count and distinct "boxes" icon
        JButton btnAll = createCategoryTabButton("All Items", "ALL", catalogProducts.size(), "boxes");
        btnAll.setAlignmentX(Component.CENTER_ALIGNMENT);
        categoryTabsPanel.add(btnAll);
        categoryTabsPanel.add(Box.createVerticalStrut(5));

        List<Category> categories = new ArrayList<>();
        try {
            categories = categoryDAO.getAllCategories();
        } catch (Exception ignored) {}

        if (categories == null || categories.isEmpty()) {
            categories = new ArrayList<>();
            categories.add(new Category(1, "Electronics", "Electronic gadgets"));
            categories.add(new Category(2, "Accessories", "Mice, Keyboards, Cables"));
            categories.add(new Category(3, "Stationery", "Office & paper"));
            categories.add(new Category(4, "Storage & Networking", "USB Drives & Adapters"));
        }

        for (Category cat : categories) {
            int count = categoryCounts.getOrDefault(cat.getName().toUpperCase(), 0);
            String iconName = getCategoryIconName(cat.getName());
            JButton btnCat = createCategoryTabButton(cat.getName(), cat.getName(), count, iconName);
            btnCat.setAlignmentX(Component.CENTER_ALIGNMENT);
            categoryTabsPanel.add(btnCat);
            categoryTabsPanel.add(Box.createVerticalStrut(5));
        }

        categoryTabsPanel.add(Box.createVerticalGlue());
        categoryTabsPanel.revalidate();
        categoryTabsPanel.repaint();
    }

    private String getCategoryIconName(String categoryName) {
        if (categoryName == null) return "category";
        String lower = categoryName.trim().toLowerCase();
        if (lower.equals("all") || lower.contains("all item")) return "boxes";
        if (lower.contains("accessori") || lower.contains("headphone") || lower.contains("mouse") || lower.contains("keyboard") || lower.contains("earphone") || lower.contains("gadget")) return "accessories";
        if (lower.contains("electron") || lower.contains("laptop") || lower.contains("monitor") || lower.contains("tv") || lower.contains("screen") || lower.contains("computer") || lower.contains("pc")) return "electronics";
        if (lower.contains("stationer") || lower.contains("paper") || lower.contains("office") || lower.contains("pen") || lower.contains("note") || lower.contains("pencil") || lower.contains("spiral")) return "stationery";
        if (lower.contains("storage") || lower.contains("network") || lower.contains("usb") || lower.contains("drive") || lower.contains("cable") || lower.contains("adapter") || lower.contains("router")) return "network";
        if (lower.contains("grocer") || lower.contains("food") || lower.contains("snack") || lower.contains("fresh") || lower.contains("fruit") || lower.contains("veg")) return "grocery";
        if (lower.contains("dairy") || lower.contains("milk")) return "dairy";
        if (lower.contains("cloth") || lower.contains("apparel") || lower.contains("wear") || lower.contains("fashion") || lower.contains("shirt")) return "clothing";
        if (lower.contains("book")) return "book";
        return "tag";
    }

    /**
     * Category Tab Button: Exact match to the Dashboard SidebarNavItem:
     * - Stadium pill shape
     * - Vector SVG icon
     * - Active: Forest Green background + white text + Electric Lime indicator on the right edge
     * - Inactive: Transparent background, Pine Slate text, soft translucent green hover
     */
    private JButton createCategoryTabButton(String label, String categoryKey, int count, String iconName) {
        boolean isSelected = selectedCategory.equalsIgnoreCase(categoryKey);
        Color activeColor = Color.WHITE;
        Color inactiveColor = AppTheme.TEXT_SECONDARY;

        SidebarIcon vIcon = new SidebarIcon(iconName, 17, isSelected ? activeColor : inactiveColor);

        JButton btn = new JButton(label + " (" + count + ")", vIcon) {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) {
                        hov = true;
                        if (!isSelected) {
                            setForeground(AppTheme.TEXT_PRIMARY);
                            vIcon.setOverrideColor(AppTheme.TEXT_PRIMARY);
                        }
                        repaint();
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        hov = false;
                        if (!isSelected) {
                            setForeground(inactiveColor);
                            vIcon.setOverrideColor(inactiveColor);
                        }
                        repaint();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                float arc = h; // Stadium pill shape

                if (isSelected) {
                    // Dark green stadium pill
                    g2.setColor(AppTheme.FOREST_GREEN);
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));

                    // Keyra Electric Lime Accent pill on active item
                    g2.setColor(AppTheme.ELECTRIC_LIME);
                    g2.fillRoundRect(w - 16, (h - 12) / 2, 4, 12, 4, 4);

                    // Subtle ambient shadow
                    g2.setColor(new Color(0, 0, 0, 20));
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, arc, arc));
                } else if (hov) {
                    // Translucent soft green hover pill
                    g2.setColor(AppTheme.HOVER_SURFACE);
                    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 12));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setHorizontalTextPosition(SwingConstants.RIGHT);
        btn.setMaximumSize(new Dimension(208, 42));
        btn.setPreferredSize(new Dimension(208, 42));
        btn.setMinimumSize(new Dimension(208, 42));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setForeground(isSelected ? activeColor : inactiveColor);
        btn.setIconTextGap(10);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(0, 16, 0, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(label + " (" + count + " items)");

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
            catalogProducts = createDemoProducts();
        }

        filterCatalogByCategory(selectedCategory);
    }

    private List<Product> createDemoProducts() {
        List<Product> list = new ArrayList<>();
        Product p1 = new Product(1, "PRD-001", "890123456789", "Amul Gold Milk (500ml)", 1, 30.0, 33.0, 50, 10, "data/product_images/milk.png");
        p1.setCategoryName("Dairy");
        list.add(p1);

        Product p2 = new Product(2, "PRD-002", "890234567890", "Wireless Optical Mouse", 2, 450.0, 599.0, 25, 5, "data/product_images/mouse.png");
        p2.setCategoryName("Electronics");
        list.add(p2);

        Product p3 = new Product(3, "PRD-003", "890345678901", "Mechanical Keyboard RGB", 2, 1800.0, 2499.0, 15, 3, "data/product_images/keyboard.png");
        p3.setCategoryName("Electronics");
        list.add(p3);

        Product p4 = new Product(4, "PRD-004", "890704139705", "A4 Copier Paper Ream (500s)", 3, 220.0, 280.0, 60, 10, "data/product_images/PRD-004.png");
        p4.setCategoryName("Stationery");
        list.add(p4);

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
            int cols = getCatalogColumnCount();
            catalogGridPanel.setLayout(new GridLayout(0, cols, 12, 12));
            for (Product p : products) {
                catalogGridPanel.add(createProductCard(p));
            }
        }

        catalogGridPanel.revalidate();
        catalogGridPanel.repaint();
    }

    private int getCatalogColumnCount() {
        int avail = catalogGridPanel.getWidth();
        if (avail <= 0 && catalogGridPanel.getParent() != null) {
            avail = catalogGridPanel.getParent().getWidth();
        }
        if (avail <= 0) {
            avail = 720;
        }
        return Math.max(2, (avail - 12) / 185);
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
     * Exact same UI as the Admin Sales / Billing Terminal card:
     * - Top studio image container (clean contain mode, 16px top rounded clip)
     * - Product title
     * - Price on Left & Live Stock Count on Right ("X in stock" / "X left" / "Out")
     * - Bottom action row: Circular stepper [-] [qty] [+] on Left & "+ Add" button on Right
     * - 16px rounded corners, pure white surface, subtle hover border & shadow
     */
    private JPanel createProductCard(Product p) {
        return new ProductCardPanel(p);
    }

    private JButton createCircleStepperBtn(String iconName, ActionListener al) {
        JButton btn = new JButton() {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                boolean en = isEnabled();
                Color bg = !en ? new Color(245, 248, 246) : (hovered ? new Color(228, 242, 234) : new Color(242, 246, 244));
                Color fg = !en ? new Color(195, 205, 200) : AppTheme.TEXT_PRIMARY;

                g2.setColor(bg);
                g2.fillOval(0, 0, w - 1, h - 1);

                SidebarIcon ico = new SidebarIcon(iconName, 10, fg);
                ico.paintIcon(this, g2, (w - ico.getIconWidth()) / 2, (h - ico.getIconHeight()) / 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(22, 22));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(al);
        return btn;
    }

    private class ProductCardPanel extends JPanel {
        private final Product product;
        private int currentQty = 1;
        private boolean isHovered = false;

        private JLabel lblQty;
        private JButton btnDec;
        private JButton btnInc;
        private JButton btnAdd;
        private JLabel lblStock;

        public ProductCardPanel(Product p) {
            this.product = p;
            setLayout(new BorderLayout(0, 0));
            setOpaque(false);
            setPreferredSize(new Dimension(175, 235));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            // 1. TOP: Product Image Container (height: 125px, clean contain mode, no cutoffs)
            JPanel imgContainer = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    int w = getWidth(), h = getHeight();

                    // Same plain background as card
                    g2.setColor(Color.WHITE);
                    g2.fillRect(0, 0, w, h);

                    // Top rounded clip (16px top corners, square bottom)
                    Path2D clipPath = new Path2D.Float();
                    clipPath.moveTo(0, 16);
                    clipPath.quadTo(0, 0, 16, 0);
                    clipPath.lineTo(w - 16, 0);
                    clipPath.quadTo(w, 0, w, 16);
                    clipPath.lineTo(w, h);
                    clipPath.lineTo(0, h);
                    clipPath.closePath();
                    g2.setClip(clipPath);

                    // Clean fit with contain mode on pure white studio background (no cropping or clipping of product!)
                    Image raw = ProductImageUtil.getProductRawImage(product);
                    if (raw != null && raw.getWidth(null) > 0 && raw.getHeight(null) > 0) {
                        int imgW = raw.getWidth(null);
                        int imgH = raw.getHeight(null);
                        double scale = Math.min((double) (w - 12) / imgW, (double) (h - 12) / imgH);
                        int drawW = Math.max(1, (int) Math.round(imgW * scale));
                        int drawH = Math.max(1, (int) Math.round(imgH * scale));
                        int drawX = (w - drawW) / 2;
                        int drawY = (h - drawH) / 2;
                        g2.drawImage(raw, drawX, drawY, drawW, drawH, null);
                    } else {
                        ImageIcon icon = ProductImageUtil.getProductIcon(product, w, h);
                        if (icon != null && icon.getImage() != null) {
                            int drawW = icon.getIconWidth();
                            int drawH = icon.getIconHeight();
                            int drawX = (w - drawW) / 2;
                            int drawY = (h - drawH) / 2;
                            g2.drawImage(icon.getImage(), drawX, drawY, drawW, drawH, null);
                        }
                    }

                    // Floating Category Badge on top-left (same as billing panel)
                    String catName = (product.getCategoryName() != null && !product.getCategoryName().trim().isEmpty())
                            ? product.getCategoryName().toUpperCase()
                            : "GENERAL";
                    g2.setFont(AppTheme.font(Font.BOLD, 9));
                    FontMetrics fm = g2.getFontMetrics();
                    int tagW = fm.stringWidth(catName) + 10;
                    int tagH = 17;
                    g2.setColor(new Color(245, 250, 247, 235));
                    g2.fillRoundRect(8, 8, tagW, tagH, 6, 6);
                    g2.setColor(AppTheme.FOREST_MID);
                    g2.drawString(catName, 13, 8 + fm.getAscent() + (tagH - fm.getHeight()) / 2);

                    g2.dispose();
                }
            };
            imgContainer.setOpaque(false);
            imgContainer.setPreferredSize(new Dimension(0, 125));

            add(imgContainer, BorderLayout.NORTH);

            // 2. MIDDLE & BOTTOM: Details + Stepper + Add Button
            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setOpaque(false);
            body.setBorder(new EmptyBorder(8, 10, 10, 10));

            // Name
            JLabel lblName = new JLabel(product.getName());
            lblName.setFont(AppTheme.font(Font.BOLD, 12));
            lblName.setForeground(AppTheme.TEXT_PRIMARY);
            lblName.setToolTipText(product.getName());
            lblName.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(lblName);
            body.add(Box.createVerticalStrut(4));

            // Rate (Price) on Left, Stock Left on Right
            JLabel lblPrice = new JLabel(AppTheme.formatCurrency(product.getSellingPrice()));
            lblPrice.setFont(AppTheme.font(Font.BOLD, 13));
            lblPrice.setForeground(AppTheme.FOREST_GREEN);

            // Stock Left
            lblStock = new JLabel();
            lblStock.setFont(AppTheme.font(Font.PLAIN, 11));

            JPanel priceStockRow = new JPanel(new BorderLayout());
            priceStockRow.setOpaque(false);
            priceStockRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            priceStockRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            priceStockRow.add(lblPrice, BorderLayout.WEST);
            priceStockRow.add(lblStock, BorderLayout.EAST);
            body.add(priceStockRow);
            body.add(Box.createVerticalStrut(8));

            // Bottom Actions: Quantity Stepper + Add Button
            JPanel actionsRow = new JPanel(new BorderLayout(4, 0));
            actionsRow.setOpaque(false);
            actionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            actionsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

            // Stepper: [-] [qty] [+]
            JPanel stepper = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            stepper.setOpaque(false);

            btnDec = createCircleStepperBtn("minus", e -> {
                if (currentQty > 1) {
                    currentQty--;
                    updateStepperState();
                }
            });

            lblQty = new JLabel(String.valueOf(currentQty), SwingConstants.CENTER);
            lblQty.setFont(AppTheme.font(Font.BOLD, 12));
            lblQty.setForeground(AppTheme.TEXT_PRIMARY);
            lblQty.setPreferredSize(new Dimension(18, 22));

            btnInc = createCircleStepperBtn("plus", e -> {
                int maxAvailable = getRemainingStock();
                if (currentQty < maxAvailable) {
                    currentQty++;
                    updateStepperState();
                } else {
                    Toolkit.getDefaultToolkit().beep();
                    setToast("Cannot add more. Only " + maxAvailable + " units available in stock.", true);
                }
            });

            stepper.add(btnDec);
            stepper.add(lblQty);
            stepper.add(btnInc);
            actionsRow.add(stepper, BorderLayout.WEST);

            // Add button: "+ Add"
            btnAdd = new JButton("+ Add") {
                private boolean hovered = false;
                {
                    addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                        @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                    });
                }
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    boolean en = isEnabled();
                    Color bg = !en ? new Color(242, 246, 244) : (hovered ? AppTheme.FOREST_MID : AppTheme.FOREST_GREEN);
                    Color fg = !en ? new Color(160, 175, 168) : Color.WHITE;

                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, w, h, h, h);

                    g2.setFont(AppTheme.font(Font.BOLD, 11));
                    g2.setColor(fg);
                    FontMetrics fm = g2.getFontMetrics();
                    String text = getText();
                    int tx = (w - fm.stringWidth(text)) / 2;
                    int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(text, tx, ty);
                    g2.dispose();
                }
            };
            btnAdd.setPreferredSize(new Dimension(54, 26));
            btnAdd.setOpaque(false);
            btnAdd.setContentAreaFilled(false);
            btnAdd.setBorderPainted(false);
            btnAdd.setFocusPainted(false);
            btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnAdd.addActionListener(e -> {
                int remaining = getRemainingStock();
                if (remaining <= 0) {
                    playAlertSound();
                    setToast(product.getQuantity() <= 0
                            ? "\"" + product.getName() + "\" is out of stock (0 available)."
                            : "\"" + product.getName() + "\" is already at maximum available stock in cart.", true);
                    return;
                }
                if (currentQty > remaining) {
                    playAlertSound();
                    setToast("Cannot add " + currentQty + " units. Only " + remaining + " unit(s) available in stock.", true);
                    return;
                }
                boolean added = addProductToCart(product, currentQty);
                if (added) {
                    currentQty = 1;
                    updateStepperState();
                }
            });
            actionsRow.add(btnAdd, BorderLayout.EAST);

            body.add(actionsRow);
            add(body, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int remaining = getRemainingStock();
                        if (remaining > 0) {
                            addProductToCart(product, 1);
                            currentQty = 1;
                            updateStepperState();
                        } else {
                            playAlertSound();
                            setToast(product.getQuantity() <= 0
                                    ? "\"" + product.getName() + "\" is out of stock."
                                    : "Maximum available stock already in cart.", true);
                        }
                    }
                }
            });

            updateStepperState();
        }

        private int getRemainingStock() {
            int inCart = getCartQuantityForProduct(product.getId());
            return Math.max(0, product.getQuantity() - inCart);
        }

        public void updateStockDisplay() {
            int remaining = getRemainingStock();
            if (product.getQuantity() <= 0) {
                lblStock.setText("Out");
                lblStock.setForeground(new Color(220, 38, 38));
            } else if (remaining <= 0) {
                lblStock.setText("Max in cart");
                lblStock.setForeground(new Color(217, 119, 6));
            } else if (remaining <= product.getMinStockLevel()) {
                lblStock.setText(remaining + " left");
                lblStock.setForeground(new Color(217, 119, 6));
            } else {
                lblStock.setText(remaining + " in stock");
                lblStock.setForeground(AppTheme.TEXT_MUTED);
            }
        }

        public void updateStepperState() {
            int remaining = getRemainingStock();
            if (remaining <= 0) {
                currentQty = 0;
            } else if (currentQty > remaining) {
                currentQty = remaining;
            } else if (currentQty <= 0) {
                currentQty = 1;
            }

            lblQty.setText(String.valueOf(currentQty));
            btnDec.setEnabled(currentQty > 1 && remaining > 0);
            btnInc.setEnabled(currentQty < remaining && remaining > 0);
            btnAdd.setEnabled(remaining > 0);
            btnAdd.setText(remaining > 0 ? "+ Add" : (product.getQuantity() <= 0 ? "Out" : "Max"));
            updateStockDisplay();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            // Card background: Pure white
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);

            // Subtle ambient shadow on hover
            if (isHovered) {
                g2.setColor(new Color(0, 40, 20, 10));
                g2.fillRoundRect(1, 2, w - 2, h - 2, 16, 16);
            }
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            if (isHovered) {
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(new Color(230, 238, 234));
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
            g2.dispose();
        }
    }

    private JButton createSmallCircleButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hov = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(hov ? AppTheme.LIME_PALE : Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.setColor(hov ? AppTheme.FOREST_GREEN : AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 13));
        btn.setForeground(AppTheme.FOREST_GREEN);
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
            Product exact = productDAO.getProductByCode(query);
            if (exact != null) {
                addProductToCart(exact);
                txtSearchScan.setText("");
                return;
            }

            List<Product> matches = productDAO.searchProducts(query);
            if (matches.size() == 1) {
                addProductToCart(matches.get(0));
                txtSearchScan.setText("");
            } else if (!matches.isEmpty()) {
                renderCatalogGrid(matches);
                setToast("Found " + matches.size() + " products for '" + query + "'.", false);
            } else {
                playAlertSound();
                setToast("No product found for: " + query, true);
            }
        } catch (Exception ex) {
            setToast("Search error: " + ex.getMessage(), true);
        }

        focusScanInput();
    }

    public boolean addProductToCart(Product p, int qty) {
        if (p.getQuantity() <= 0) {
            playAlertSound();
            setToast(p.getName() + " is currently out of stock", true);
            return false;
        }

        CartItem existing = null;
        for (CartItem ci : cartItems) {
            if (ci.product.getId() == p.getId()) {
                existing = ci;
                break;
            }
        }

        int currentInCart = (existing != null) ? existing.quantity : 0;
        if (currentInCart + qty > p.getQuantity()) {
            playAlertSound();
            int remaining = Math.max(0, p.getQuantity() - currentInCart);
            setToast("Only " + remaining + " more units available for " + p.getName(), true);
            return false;
        }

        if (existing != null) {
            existing.quantity += qty;
        } else {
            cartItems.add(new CartItem(p, qty));
        }

        playScanBeep();
        refreshCartDisplay();
        setToast("Added " + (qty > 1 ? (qty + "x ") : "") + p.getName(), false);
        focusScanInput();
        return true;
    }

    public void addProductToCart(Product p) {
        addProductToCart(p, 1);
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
            setToast("Only " + item.product.getQuantity() + " units in stock", true);
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
        setToast("Removed " + item.product.getName() + " from cart", false);
        focusScanInput();
    }

    private void clearCart() {
        if (cartItems.isEmpty()) return;
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear your cart?", "Clear Cart", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            cartItems.clear();
            refreshCartDisplay();
            setToast("Cart cleared", false);
            focusScanInput();
        }
    }

    private void refreshCartDisplay() {
        renderCartItemsList();
        filterCatalogByCategory(selectedCategory);

        double subtotal = 0.0;
        int totalUnits = 0;

        for (CartItem ci : cartItems) {
            subtotal += ci.getSubtotal();
            totalUnits += ci.quantity;
        }

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

        if (cartItems.isEmpty()) {
            btnPayNow.setText("Add items to checkout");
            btnPayNow.setEnabled(false);
            btnClearCart.setEnabled(false);
        } else {
            btnPayNow.setText(String.format("Pay %s  ➔", AppTheme.formatCurrency(grandTotal)));
            btnPayNow.setEnabled(true);
            btnClearCart.setEnabled(true);
        }
        btnPayNow.repaint();
    }

    /**
     * Renders modern interactive cart item cards with inline steppers.
     */
    private void renderCartItemsList() {
        if (cartItemsContainer == null) return;
        cartItemsContainer.removeAll();

        if (cartItems.isEmpty()) {
            // Clean minimal empty state
            JPanel emptyState = new JPanel(new GridBagLayout());
            emptyState.setOpaque(false);
            emptyState.setBorder(new EmptyBorder(40, 20, 40, 20));

            JPanel box = new JPanel();
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setOpaque(false);

            JLabel lblEmptyIcon = new JLabel(new SidebarIcon("cart", 44, new Color(203, 213, 225)));
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
            box.add(Box.createVerticalStrut(10));
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

    private JPanel createCartItemCard(CartItem ci, int index) {
        JPanel card = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(AppTheme.BG_CANVAS);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        card.setPreferredSize(new Dimension(340, 56));
        card.setBorder(new EmptyBorder(6, 12, 6, 10));

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
        lblLineTotal.setPreferredSize(new Dimension(60, 24));

        JButton btnTrash = createSmallCircleButton("✕");
        btnTrash.setFont(AppTheme.font(Font.BOLD, 11));
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

        SelfCheckoutPaymentDialog dialog = new SelfCheckoutPaymentDialog(parentFrame, cartItems, subtotal, gstRate, gstAmount, grandTotal, checkoutUser);
        dialog.setVisible(true);

        if (dialog.isCompleted()) {
            cartItems.clear();
            refreshCartDisplay();
            loadCatalog();
            setToast("Payment completed successfully! Thank you.", false);
        }

        focusScanInput();
    }

    private void openCameraBarcodeScanner() {
        String barcode = JOptionPane.showInputDialog(this,
                "Scan or Enter Product Barcode:",
                "Barcode Scanner", JOptionPane.PLAIN_MESSAGE);
        if (barcode != null && !barcode.trim().isEmpty()) {
            txtSearchScan.setText(barcode.trim());
            handleScanOrSearch();
        }
        focusScanInput();
    }

    private void launchFullscreenWindow() {
        SelfCheckoutFrame fullscreenFrame = new SelfCheckoutFrame(checkoutUser);
        fullscreenFrame.setVisible(true);
    }

    private void setToast(String message, boolean isError) {
        lblStatusToast.setText(message);
        lblStatusToast.setForeground(isError ? AppTheme.STATUS_DANGER : AppTheme.FOREST_GREEN);
    }

    private void playScanBeep() {
        try { Toolkit.getDefaultToolkit().beep(); } catch (Exception ignored) {}
    }

    private void playAlertSound() {
        try { Toolkit.getDefaultToolkit().beep(); } catch (Exception ignored) {}
    }

    private void updateClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a");
        lblLiveClock.setText(sdf.format(new Date()));
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

        public SelfCheckoutPaymentDialog(Frame owner, List<CartItem> items, double subtotal, double gstRate, double gstAmount, double grandTotal, User user) {
            super(owner, "Self-Checkout Payment", true);
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

            setSize(640, 680);
            setLocationRelativeTo(owner);
            setResizable(false);
            initComponents();
        }

        public boolean isCompleted() {
            return completed;
        }

        private void initComponents() {
            JPanel root = new JPanel(new BorderLayout(0, 14));
            root.setBackground(Color.WHITE);
            root.setBorder(new EmptyBorder(18, 22, 18, 22));

            // Header Banner: Forest Green with Electric Lime total
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(AppTheme.FOREST_GREEN);
            header.setBorder(new EmptyBorder(16, 20, 16, 20));

            JLabel lblH1 = new JLabel("Express Payment", new SidebarIcon("credit_card", 18, Color.WHITE), SwingConstants.LEFT);
            lblH1.setFont(AppTheme.font(Font.BOLD, 18));
            lblH1.setForeground(Color.WHITE);
            lblH1.setIconTextGap(8);

            JLabel lblTotalBadge = new JLabel(AppTheme.formatCurrency(grandTotal));
            lblTotalBadge.setFont(AppTheme.font(Font.BOLD, 22));
            lblTotalBadge.setForeground(AppTheme.ELECTRIC_LIME);

            header.add(lblH1, BorderLayout.WEST);
            header.add(lblTotalBadge, BorderLayout.EAST);

            JPanel center = new JPanel(new BorderLayout(0, 12));
            center.setOpaque(false);

            // Optional Customer Phone
            JPanel phoneCard = new JPanel(new BorderLayout(10, 4));
            phoneCard.setBackground(AppTheme.BG_CANVAS);
            phoneCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER_SAGE),
                    new EmptyBorder(10, 14, 10, 14)
            ));

            JLabel lblPhonePrompt = new JLabel("Mobile Number for WhatsApp / Digital Receipt (Optional):");
            lblPhonePrompt.setFont(AppTheme.font(Font.BOLD, 12));
            lblPhonePrompt.setForeground(AppTheme.TEXT_SECONDARY);

            txtCustomerPhone = new JTextField();
            txtCustomerPhone.setFont(AppTheme.font(Font.BOLD, 14));
            txtCustomerPhone.setForeground(AppTheme.TEXT_PRIMARY);
            txtCustomerPhone.setCaretColor(AppTheme.FOREST_GREEN);
            txtCustomerPhone.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER_SAGE),
                    new EmptyBorder(6, 10, 6, 10)
            ));

            phoneCard.add(lblPhonePrompt, BorderLayout.NORTH);
            phoneCard.add(txtCustomerPhone, BorderLayout.CENTER);

            center.add(phoneCard, BorderLayout.NORTH);

            // Tabbed Payment Options: UPI QR, Card, Cash
            JTabbedPane tabPayment = new JTabbedPane();
            tabPayment.setFont(AppTheme.font(Font.BOLD, 12));

            tabPayment.addTab("UPI QR Code", new SidebarIcon("zap", 14, AppTheme.FOREST_GREEN), createUpiTab());
            tabPayment.addTab("Card Payment", new SidebarIcon("credit_card", 14, AppTheme.FOREST_GREEN), createCardTab());
            tabPayment.addTab("Cash at Desk", new SidebarIcon("wallet", 14, AppTheme.FOREST_GREEN), createCashTab());

            center.add(tabPayment, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            bottom.setOpaque(false);

            JButton btnCancel = new JButton("Cancel & Back to Cart");
            btnCancel.setFont(AppTheme.font(Font.PLAIN, 12));
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

            String storeUpi = AppSettings.getString(AppSettings.KEY_STORE_UPI_ID, "bazaarpoint@upi").trim();
            String storeName = AppSettings.getString(AppSettings.KEY_STORE_NAME, "SmartBilling Store").trim();
            String upiUri = QrCodeGenerator.buildUpiUri(storeUpi, storeName, grandTotal, generatedInvoiceNo);
            BufferedImage qrImg = QrCodeGenerator.generateQrImage(upiUri, 220);

            lblUpiQrCanvas = new JLabel(new ImageIcon(qrImg), SwingConstants.CENTER);

            JPanel instructions = new JPanel(new GridLayout(2, 1, 0, 4));
            instructions.setOpaque(false);
            JLabel lblI1 = new JLabel("Scan QR code using GPay, PhonePe, Paytm, or any UPI App", SwingConstants.CENTER);
            lblI1.setFont(AppTheme.font(Font.BOLD, 13));
            lblI1.setForeground(AppTheme.TEXT_PRIMARY);

            JLabel lblI2 = new JLabel("Pay exact amount: " + AppTheme.formatCurrency(grandTotal), SwingConstants.CENTER);
            lblI2.setFont(AppTheme.font(Font.PLAIN, 12));
            lblI2.setForeground(AppTheme.TEXT_MUTED);

            instructions.add(lblI1);
            instructions.add(lblI2);

            JButton btnUpiConfirm = createPaymentConfirmButton("Confirm & Print Receipt");
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

            JLabel lblIcon = new JLabel(new SidebarIcon("credit_card", 44, AppTheme.FOREST_GREEN), SwingConstants.CENTER);

            JLabel lblMsg = new JLabel("Please Tap, Swipe, or Insert your Card on the POS Terminal", SwingConstants.CENTER);
            lblMsg.setFont(AppTheme.font(Font.BOLD, 14));
            lblMsg.setForeground(AppTheme.TEXT_PRIMARY);

            JLabel lblSub = new JLabel("Amount to charge: " + AppTheme.formatCurrency(grandTotal), SwingConstants.CENTER);
            lblSub.setFont(AppTheme.font(Font.PLAIN, 13));
            lblSub.setForeground(AppTheme.TEXT_MUTED);

            center.add(lblIcon);
            center.add(lblMsg);
            center.add(lblSub);

            JButton btnCardConfirm = createPaymentConfirmButton("Card Payment Approved - Complete");
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

            JLabel lblIcon = new JLabel(new SidebarIcon("wallet", 44, AppTheme.FOREST_GREEN), SwingConstants.CENTER);

            JLabel lblMsg = new JLabel("Pay with Cash at Express Checkout Counter", SwingConstants.CENTER);
            lblMsg.setFont(AppTheme.font(Font.BOLD, 14));
            lblMsg.setForeground(AppTheme.TEXT_PRIMARY);

            JLabel lblSub = new JLabel("Please deposit " + AppTheme.formatCurrency(grandTotal) + " with the attendant.", SwingConstants.CENTER);
            lblSub.setFont(AppTheme.font(Font.PLAIN, 13));
            lblSub.setForeground(AppTheme.TEXT_MUTED);

            center.add(lblIcon);
            center.add(lblMsg);
            center.add(lblSub);

            JButton btnCashConfirm = createPaymentConfirmButton("Cash Received - Complete & Print");
            btnCashConfirm.addActionListener(e -> finalizeSale("CASH"));

            p.add(center, BorderLayout.CENTER);
            p.add(btnCashConfirm, BorderLayout.SOUTH);

            return p;
        }

        private JButton createPaymentConfirmButton(String text) {
            JButton btn = new JButton(text, new SidebarIcon("check_circle", 15, Color.WHITE)) {
                private boolean hov = false;
                {
                    addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) { hov = true; repaint(); }
                        @Override public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
                    });
                }
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    g2.setColor(hov ? AppTheme.FOREST_MID : AppTheme.FOREST_GREEN);
                    g2.fillRoundRect(0, 0, w, h, h, h);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(AppTheme.font(Font.BOLD, 13));
            btn.setForeground(Color.WHITE);
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setOpaque(false);
            btn.setIconTextGap(8);
            btn.setPreferredSize(new Dimension(0, 46));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            return btn;
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

                for (CartItem ci : items) {
                    SaleItem si = new SaleItem();
                    si.setProductId(ci.product.getId());
                    si.setProductName(ci.product.getName());
                    si.setQuantity(ci.quantity);
                    si.setUnitPrice(ci.unitPrice);
                    si.setSubtotal(ci.getSubtotal());
                    sale.getItems().add(si);
                }

                boolean success = billingDAO.processSale(sale);
                if (success) {
                    this.completed = true;
                    try { Toolkit.getDefaultToolkit().beep(); } catch (Exception ignored) {}
                    showSuccessAndReceiptModal(sale);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not process transaction. Please consult assistant.", "Transaction Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Billing Failed", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void showSuccessAndReceiptModal(Sale sale) {
            JDialog dlg = new JDialog((Frame) getOwner(), "Payment Successful", true);
            dlg.setSize(420, 380);
            dlg.setLocationRelativeTo(getOwner());

            JPanel p = new JPanel(new BorderLayout(0, 16));
            p.setBackground(Color.WHITE);
            p.setBorder(new EmptyBorder(24, 24, 24, 24));

            JPanel center = new JPanel(new GridLayout(4, 1, 0, 8));
            center.setOpaque(false);

            JLabel lblSuccessIcon = new JLabel(new SidebarIcon("check_circle", 48, AppTheme.FOREST_GREEN), SwingConstants.CENTER);

            JLabel lblTitle = new JLabel("Payment Successful!", SwingConstants.CENTER);
            lblTitle.setFont(AppTheme.font(Font.BOLD, 18));
            lblTitle.setForeground(AppTheme.FOREST_GREEN);

            JLabel lblInv = new JLabel("Invoice #" + sale.getInvoiceNo(), SwingConstants.CENTER);
            lblInv.setFont(AppTheme.font(Font.BOLD, 13));
            lblInv.setForeground(AppTheme.TEXT_PRIMARY);

            JLabel lblAmt = new JLabel("Total: " + AppTheme.formatCurrency(sale.getTotalAmount()) + " (" + sale.getPaymentMode() + ")", SwingConstants.CENTER);
            lblAmt.setFont(AppTheme.font(Font.BOLD, 14));
            lblAmt.setForeground(AppTheme.FOREST_MID);

            center.add(lblSuccessIcon);
            center.add(lblTitle);
            center.add(lblInv);
            center.add(lblAmt);

            JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
            actions.setOpaque(false);

            JButton btnPrint = new JButton("Print Receipt", new SidebarIcon("printer", 13, AppTheme.FOREST_MID)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(241, 245, 249));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btnPrint.setFont(AppTheme.font(Font.BOLD, 12));
            btnPrint.setForeground(AppTheme.TEXT_PRIMARY);
            btnPrint.setFocusPainted(false);
            btnPrint.setContentAreaFilled(false);
            btnPrint.setBorderPainted(false);
            btnPrint.setIconTextGap(6);
            btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnPrint.addActionListener(e -> new InvoiceDialog((Frame) getOwner(), sale).setVisible(true));

            JButton btnDone = new JButton("Done", new SidebarIcon("check", 13, Color.WHITE)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(AppTheme.FOREST_GREEN);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btnDone.setFont(AppTheme.font(Font.BOLD, 12));
            btnDone.setForeground(Color.WHITE);
            btnDone.setFocusPainted(false);
            btnDone.setContentAreaFilled(false);
            btnDone.setBorderPainted(false);
            btnDone.setIconTextGap(6);
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
