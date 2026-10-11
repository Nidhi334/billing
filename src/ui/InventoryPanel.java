package ui;

import config.DBConnection;
import dao.ProductDAO;
import dao.PurchaseDAO;
import dao.ReportDAO;
import dao.SupplierDAO;
import model.Product;
import model.Supplier;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Inventory & Stock Management Hub adhering to Keyra Admin Design System.
 * Features:
 * - Elevated Header Strip with pill badge and fast actions
 * - 4 Interactive KPI Metric Cards (Total SKUs, Low Stock, Critical Out of Stock, Stock Valuation)
 * - Left Inward Stock-In Entry card with live product calculation & preview widget
 * - Right Multi-View Workspace with modern segmented tabs:
 *     1. Stock Reorder Alerts (Low & Out-of-Stock)
 *     2. Stock Movement History (Audit Trail)
 *     3. Complete Inventory Ledger
 * - Fast Search & Filter Chips across all views
 * - Instant "⚡ Restock" quick-action shortcut from tables to form
 */
public class InventoryPanel extends JPanel {

    // DAOs
    private final ProductDAO productDAO = new ProductDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final PurchaseDAO purchaseDAO = new PurchaseDAO();
    private final ReportDAO reportDAO = new ReportDAO();

    // Data Caches
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> lowStockProducts = new ArrayList<>();
    private List<Supplier> allSuppliers = new ArrayList<>();
    private List<String[]> stockHistoryList = new ArrayList<>();

    // State
    private String currentTab = "ALERTS"; // "ALERTS", "HISTORY", "LEDGER"
    private String alertsFilter = "ALL";  // "ALL", "OUT_OF_STOCK", "LOW_STOCK"
    private String historyFilter = "ALL"; // "ALL", "IN", "OUT"
    private String ledgerFilter = "ALL";  // "ALL", "HEALTHY", "REORDER"
    private String searchQuery = "";
    private Product selectedProduct = null;

    // KPI Summary Labels
    private JLabel lblKpiTotalSkus;
    private JLabel lblKpiLowStock;
    private JLabel lblKpiOutOfStock;
    private JLabel lblKpiValuation;

    // Segmented Tab Buttons
    private JButton btnTabAlerts;
    private JButton btnTabHistory;
    private JButton btnTabLedger;

    // Table Cards & Models
    private CardLayout tableCardLayout;
    private JPanel tableCardContainer;

    private JTable alertsTable;
    private DefaultTableModel alertsModel;

    private JTable historyTable;
    private DefaultTableModel historyModel;

    private JTable ledgerTable;
    private DefaultTableModel ledgerModel;

    // Search & Filter Components
    private ModernSearchField txtSearch;
    private JPanel filterChipsPanel;
    private JLabel lblRecordCount;

    // Stock-In Form Components
    private JComboBox<Product> cmbProducts;
    private JComboBox<Supplier> cmbSuppliers;
    private ModernInputField txtQty;
    private ModernInputField txtCostPrice;
    private JButton btnAddStock;
    private JButton btnClearForm;

    // Live Product Preview & Math Widget
    private JPanel pnlProductPreview;
    private JLabel lblPreviewName;
    private JLabel lblPreviewMeta;
    private JLabel lblPreviewStockPill;
    private JLabel lblPreviewMath;
    private JLabel lblPreviewInvestment;

    public InventoryPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(AppTheme.BG_CANVAS);
        setBorder(new EmptyBorder(12, 14, 12, 14));

        initComponents();
        loadDropdownData();
        refreshData();
    }

    private void initComponents() {
        // =========================================================================
        // TOP SECTION: Elevated Header Strip + 4 Interactive KPI Summary Cards
        // =========================================================================
        JPanel topContainer = new JPanel(new BorderLayout(0, 10));
        topContainer.setOpaque(false);

        // 1. Elevated Header Strip
        JPanel headerStrip = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = 18;
                g2.setColor(new Color(0, 50, 30, 5));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        headerStrip.setOpaque(false);
        headerStrip.setBorder(new EmptyBorder(8, 14, 8, 14));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerLeft.setOpaque(false);

        JLabel lblTitle = new JLabel("📦 Inventory & Stock Control Hub");
        lblTitle.setFont(AppTheme.font(Font.BOLD, 15));
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel lblBadge = new JLabel(" SUPPLY & WAREHOUSE ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                int h = getHeight();
                g2.fillRoundRect(0, 0, getWidth(), h, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblBadge.setFont(AppTheme.font(Font.BOLD, 9));
        lblBadge.setOpaque(false);
        lblBadge.setBackground(new Color(230, 245, 236));
        lblBadge.setForeground(AppTheme.FOREST_DEEP);
        lblBadge.setBorder(new EmptyBorder(3, 8, 3, 8));

        JLabel lblSubtitle = new JLabel("Monitor warehouse stock levels, receive supplier shipments & track audit trails");
        lblSubtitle.setFont(AppTheme.font(Font.PLAIN, 11));
        lblSubtitle.setForeground(AppTheme.TEXT_MUTED);

        headerLeft.add(lblTitle);
        headerLeft.add(lblBadge);
        headerLeft.add(lblSubtitle);
        headerStrip.add(headerLeft, BorderLayout.WEST);

        // Header Right Action Buttons
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerRight.setOpaque(false);

        JButton btnQuickRestock = createPillButton("➕ Inward Stock-In", AppTheme.FOREST_GREEN, AppTheme.FOREST_GREEN, Color.WHITE);
        btnQuickRestock.setFont(AppTheme.font(Font.BOLD, 11));
        btnQuickRestock.addActionListener(e -> {
            if (txtQty != null) txtQty.requestFocusInWindow();
        });

        JButton btnHeaderRefresh = createPillButton("🔄 Refresh Data", Color.WHITE, AppTheme.BORDER_SAGE, AppTheme.TEXT_SECONDARY);
        btnHeaderRefresh.setFont(AppTheme.font(Font.BOLD, 11));
        btnHeaderRefresh.addActionListener(e -> {
            loadDropdownData();
            refreshData();
        });

        headerRight.add(btnQuickRestock);
        headerRight.add(btnHeaderRefresh);
        headerStrip.add(headerRight, BorderLayout.EAST);

        topContainer.add(headerStrip, BorderLayout.NORTH);

        // 2. Row of 4 Elevated KPI Summary Metric Cards
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 10, 0));
        kpiRow.setOpaque(false);
        kpiRow.setPreferredSize(new Dimension(0, 76));

        lblKpiTotalSkus = new JLabel("0");
        lblKpiLowStock = new JLabel("0");
        lblKpiOutOfStock = new JLabel("0");
        lblKpiValuation = new JLabel("₹0.00");

        JPanel cardTotal = createKpiCard("TOTAL INVENTORY SKUs", lblKpiTotalSkus, "Active catalog products", AppTheme.FOREST_MID, "products", () -> {
            switchTab("LEDGER");
        });
        JPanel cardLowStock = createKpiCard("LOW STOCK ALERTS", lblKpiLowStock, "Needs replenishment", new Color(234, 88, 12), "alert_triangle", () -> {
            switchTab("ALERTS");
            alertsFilter = "LOW_STOCK";
            rebuildFilterChips();
            applyActiveFilters();
        });
        JPanel cardOutOfStock = createKpiCard("OUT OF STOCK (CRITICAL)", lblKpiOutOfStock, "Depleted inventory (0)", new Color(220, 38, 38), "zap", () -> {
            switchTab("ALERTS");
            alertsFilter = "OUT_OF_STOCK";
            rebuildFilterChips();
            applyActiveFilters();
        });
        JPanel cardValuation = createKpiCard("STOCK VALUATION", lblKpiValuation, "Total warehouse cost asset", AppTheme.FOREST_DEEP, "wallet", () -> {
            refreshData();
        });

        kpiRow.add(cardTotal);
        kpiRow.add(cardLowStock);
        kpiRow.add(cardOutOfStock);
        kpiRow.add(cardValuation);

        topContainer.add(kpiRow, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // =========================================================================
        // CENTER: 2-COLUMN WORKSPACE (Left Form Card + Right Intelligence Workspace)
        // =========================================================================
        JPanel centerSplit = new JPanel(new BorderLayout(12, 0));
        centerSplit.setOpaque(false);

        // -------------------------------------------------------------------------
        // LEFT COLUMN: Elevated Inward Stock-In Card (380px)
        // -------------------------------------------------------------------------
        JPanel formCard = createElevatedCard(20);
        formCard.setPreferredSize(new Dimension(380, 0));
        formCard.setLayout(new BorderLayout(0, 10));
        formCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Form Card Header
        JPanel formHeader = new JPanel(new BorderLayout(8, 0));
        formHeader.setOpaque(false);

        JPanel formTitleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        formTitleRow.setOpaque(false);

        JLabel lblFormTitle = new JLabel("📥 Inward Stock-In Entry");
        lblFormTitle.setFont(AppTheme.font(Font.BOLD, 14));
        lblFormTitle.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel lblFormBadge = new JLabel("PURCHASE INWARD") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                int h = getHeight();
                g2.fillRoundRect(0, 0, getWidth(), h, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblFormBadge.setFont(AppTheme.font(Font.BOLD, 9));
        lblFormBadge.setOpaque(false);
        lblFormBadge.setBackground(new Color(230, 248, 238));
        lblFormBadge.setForeground(AppTheme.FOREST_GREEN);
        lblFormBadge.setBorder(new EmptyBorder(2, 6, 2, 6));

        formTitleRow.add(lblFormTitle);
        formTitleRow.add(lblFormBadge);
        formHeader.add(formTitleRow, BorderLayout.WEST);

        btnClearForm = createPillButton("Reset", new Color(248, 250, 252), new Color(226, 232, 240), AppTheme.TEXT_MUTED);
        btnClearForm.setFont(AppTheme.font(Font.PLAIN, 11));
        btnClearForm.setPreferredSize(new Dimension(64, 26));
        btnClearForm.addActionListener(e -> clearForm());
        formHeader.add(btnClearForm, BorderLayout.EAST);

        formCard.add(formHeader, BorderLayout.NORTH);

        // Form Fields Container
        JPanel formBody = new JPanel();
        formBody.setLayout(new BoxLayout(formBody, BoxLayout.Y_AXIS));
        formBody.setOpaque(false);
        formBody.setBorder(new EmptyBorder(0, 0, 8, 2));

        // Dynamic Live Product Calculation & Preview Hero Card
        pnlProductPreview = createProductPreviewHeroWidget();
        formBody.add(pnlProductPreview);
        formBody.add(Box.createVerticalStrut(10));

        // Field 1: Product Selection
        JPanel prodRow = new JPanel(new BorderLayout(0, 4));
        prodRow.setOpaque(false);
        prodRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        prodRow.add(createFieldLabel("Select Target Product:"), BorderLayout.NORTH);

        cmbProducts = new JComboBox<>();
        styleComboBox(cmbProducts);
        cmbProducts.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBorder(new EmptyBorder(4, 8, 4, 8));
                if (value instanceof Product) {
                    Product p = (Product) value;
                    String stockStatus = p.getQuantity() <= 0 ? " (OUT OF STOCK)" : " (Stock: " + p.getQuantity() + ")";
                    l.setText(p.getName() + " [" + p.getCode() + "]" + stockStatus);
                } else if (value == null) {
                    l.setText("— Choose a Product to Restock —");
                }
                return l;
            }
        });
        cmbProducts.addActionListener(e -> onProductSelected());
        prodRow.add(cmbProducts, BorderLayout.CENTER);
        formBody.add(prodRow);
        formBody.add(Box.createVerticalStrut(8));

        // Field 2: Supplier Selection
        JPanel suppRow = new JPanel(new BorderLayout(0, 4));
        suppRow.setOpaque(false);
        suppRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        suppRow.add(createFieldLabel("Select Vendor / Supplier:"), BorderLayout.NORTH);

        cmbSuppliers = new JComboBox<>();
        styleComboBox(cmbSuppliers);
        cmbSuppliers.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBorder(new EmptyBorder(4, 8, 4, 8));
                if (value instanceof Supplier) {
                    Supplier s = (Supplier) value;
                    String comp = s.getCompanyName() != null && !s.getCompanyName().isEmpty() ? " (" + s.getCompanyName() + ")" : "";
                    l.setText(s.getName() + comp);
                } else if (value == null) {
                    l.setText("— Direct Purchase / No Vendor —");
                }
                return l;
            }
        });
        suppRow.add(cmbSuppliers, BorderLayout.CENTER);
        formBody.add(suppRow);
        formBody.add(Box.createVerticalStrut(8));

        // Field 3 & 4: Quantity & Unit Cost Price in dual column
        JPanel dualRow = new JPanel(new GridLayout(1, 2, 8, 0));
        dualRow.setOpaque(false);
        dualRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

        JPanel qtyCol = new JPanel(new BorderLayout(0, 4));
        qtyCol.setOpaque(false);
        qtyCol.add(createFieldLabel("Inward Qty (Units):"), BorderLayout.NORTH);
        txtQty = new ModernInputField("e.g. 20");
        txtQty.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateMathPreview(); }
            public void removeUpdate(DocumentEvent e) { updateMathPreview(); }
            public void changedUpdate(DocumentEvent e) { updateMathPreview(); }
        });
        qtyCol.add(txtQty, BorderLayout.CENTER);

        JPanel costCol = new JPanel(new BorderLayout(0, 4));
        costCol.setOpaque(false);
        costCol.add(createFieldLabel("Unit Cost Price (₹):"), BorderLayout.NORTH);
        txtCostPrice = new ModernInputField("e.g. 250.00");
        txtCostPrice.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateMathPreview(); }
            public void removeUpdate(DocumentEvent e) { updateMathPreview(); }
            public void changedUpdate(DocumentEvent e) { updateMathPreview(); }
        });
        costCol.add(txtCostPrice, BorderLayout.CENTER);

        dualRow.add(qtyCol);
        dualRow.add(costCol);
        formBody.add(dualRow);
        formBody.add(Box.createVerticalStrut(12));

        // Inward Notes / Help Card
        JPanel helpCard = new JPanel(new BorderLayout());
        helpCard.setBackground(new Color(245, 252, 248));
        helpCard.setOpaque(true);
        helpCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        helpCard.setPreferredSize(new Dimension(0, 54));
        helpCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppTheme.BORDER_SAGE, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        JLabel lblHelp = new JLabel("<html><body style='color:#047857; font-size:10px; font-family:Segoe UI; line-height:1.2;'>"
                + "<b>✓ Audit Guarantee:</b> Inward entries automatically raise stock, generate purchases &amp; log audit trails.</body></html>");
        helpCard.add(lblHelp, BorderLayout.CENTER);
        formBody.add(helpCard);
        formBody.add(Box.createVerticalGlue());

        formCard.add(formBody, BorderLayout.CENTER);

        // Form Bottom Action Bar
        JPanel formBottom = new JPanel(new BorderLayout(0, 0));
        formBottom.setOpaque(false);

        btnAddStock = createPillButton("➕ Record Inward Stock", AppTheme.FOREST_GREEN, AppTheme.FOREST_GREEN, Color.WHITE);
        btnAddStock.setFont(AppTheme.font(Font.BOLD, 12));
        btnAddStock.setPreferredSize(new Dimension(0, 38));
        btnAddStock.addActionListener(e -> processStockIn());
        formBottom.add(btnAddStock, BorderLayout.CENTER);

        formCard.add(formBottom, BorderLayout.SOUTH);
        centerSplit.add(formCard, BorderLayout.WEST);

        // -------------------------------------------------------------------------
        // RIGHT COLUMN: Multi-View Intelligence & Audit Card (Flex width)
        // -------------------------------------------------------------------------
        JPanel tableCard = createElevatedCard(20);
        tableCard.setLayout(new BorderLayout(0, 8));
        tableCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Right Card Top: Segmented Tab Bar + Search & Filters
        JPanel tableHeaderBox = new JPanel();
        tableHeaderBox.setLayout(new BoxLayout(tableHeaderBox, BoxLayout.Y_AXIS));
        tableHeaderBox.setOpaque(false);

        // Row 1: Segmented Pill Tab Switcher
        JPanel tabSwitcherRow = new JPanel(new BorderLayout());
        tabSwitcherRow.setOpaque(false);

        JPanel tabPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabPills.setOpaque(false);

        btnTabAlerts = createTabButton("⚠️ Stock Alerts (Low & Out)", "ALERTS");
        btnTabHistory = createTabButton("📋 Movement History (Audit Log)", "HISTORY");
        btnTabLedger = createTabButton("📦 Full Inventory Ledger", "LEDGER");

        tabPills.add(btnTabAlerts);
        tabPills.add(btnTabHistory);
        tabPills.add(btnTabLedger);
        tabSwitcherRow.add(tabPills, BorderLayout.WEST);

        lblRecordCount = new JLabel("0 records");
        lblRecordCount.setFont(AppTheme.font(Font.BOLD, 11));
        lblRecordCount.setForeground(AppTheme.TEXT_MUTED);
        lblRecordCount.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppTheme.BORDER_SAGE, 1, true),
                new EmptyBorder(3, 10, 3, 10)
        ));
        tabSwitcherRow.add(lblRecordCount, BorderLayout.EAST);

        tableHeaderBox.add(tabSwitcherRow);
        tableHeaderBox.add(Box.createVerticalStrut(10));

        // Row 2: Search Bar + Filter Chips
        JPanel toolbarRow = new JPanel(new BorderLayout(10, 0));
        toolbarRow.setOpaque(false);

        txtSearch = new ModernSearchField("Search by product code, name, category, or invoice ref...");
        txtSearch.setPreferredSize(new Dimension(320, 32));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchQuery = txtSearch.getText().trim(); applyActiveFilters(); }
            public void removeUpdate(DocumentEvent e) { searchQuery = txtSearch.getText().trim(); applyActiveFilters(); }
            public void changedUpdate(DocumentEvent e) { searchQuery = txtSearch.getText().trim(); applyActiveFilters(); }
        });
        toolbarRow.add(txtSearch, BorderLayout.WEST);

        filterChipsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filterChipsPanel.setOpaque(false);
        toolbarRow.add(filterChipsPanel, BorderLayout.EAST);

        tableHeaderBox.add(toolbarRow);
        tableCard.add(tableHeaderBox, BorderLayout.NORTH);

        // Center: CardLayout containing 3 Views
        tableCardLayout = new CardLayout();
        tableCardContainer = new JPanel(tableCardLayout);
        tableCardContainer.setOpaque(false);

        // View 1: Stock Alerts Table
        initAlertsTable();
        tableCardContainer.add(createTableScrollPane(alertsTable, "alerts"), "ALERTS");

        // View 2: Stock History Table
        initHistoryTable();
        tableCardContainer.add(createTableScrollPane(historyTable, "history"), "HISTORY");

        // View 3: Complete Inventory Ledger Table
        initLedgerTable();
        tableCardContainer.add(createTableScrollPane(ledgerTable, "ledger"), "LEDGER");

        tableCard.add(tableCardContainer, BorderLayout.CENTER);
        centerSplit.add(tableCard, BorderLayout.CENTER);

        add(centerSplit, BorderLayout.CENTER);

        // Build default filter chips
        rebuildFilterChips();
    }

    // =========================================================================
    // DYNAMIC PRODUCT PREVIEW & MATH HERO WIDGET
    // =========================================================================
    private JPanel createProductPreviewHeroWidget() {
        JPanel hero = new JPanel(new BorderLayout(8, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = 14;
                g2.setColor(new Color(245, 252, 248));
                g2.fillRoundRect(0, 0, w, h, r, r);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        hero.setOpaque(false);
        hero.setBorder(new EmptyBorder(8, 10, 8, 10));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 98));

        JPanel topRow = new JPanel(new BorderLayout(6, 0));
        topRow.setOpaque(false);

        lblPreviewName = new JLabel("Select a product to view stock");
        lblPreviewName.setFont(AppTheme.font(Font.BOLD, 12));
        lblPreviewName.setForeground(AppTheme.TEXT_PRIMARY);
        topRow.add(lblPreviewName, BorderLayout.WEST);

        lblPreviewStockPill = new JLabel("No Selection") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                int h = getHeight();
                g2.fillRoundRect(0, 0, getWidth(), h, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblPreviewStockPill.setFont(AppTheme.font(Font.BOLD, 9));
        lblPreviewStockPill.setOpaque(false);
        lblPreviewStockPill.setBackground(new Color(241, 245, 249));
        lblPreviewStockPill.setForeground(AppTheme.TEXT_MUTED);
        lblPreviewStockPill.setBorder(new EmptyBorder(2, 6, 2, 6));
        topRow.add(lblPreviewStockPill, BorderLayout.EAST);
        hero.add(topRow, BorderLayout.NORTH);

        JPanel midRow = new JPanel(new BorderLayout(4, 0));
        midRow.setOpaque(false);

        lblPreviewMeta = new JLabel("Code: — • Category: — • Min Alert: —");
        lblPreviewMeta.setFont(AppTheme.font(Font.PLAIN, 10));
        lblPreviewMeta.setForeground(AppTheme.TEXT_MUTED);
        midRow.add(lblPreviewMeta, BorderLayout.WEST);
        hero.add(midRow, BorderLayout.CENTER);

        JPanel bottomBox = new JPanel(new GridLayout(2, 1, 0, 2));
        bottomBox.setOpaque(false);

        lblPreviewMath = new JLabel("➔ Projected Stock: —");
        lblPreviewMath.setFont(AppTheme.font(Font.BOLD, 11));
        lblPreviewMath.setForeground(AppTheme.FOREST_MID);

        lblPreviewInvestment = new JLabel("💰 Purchase Investment: ₹0.00");
        lblPreviewInvestment.setFont(AppTheme.font(Font.PLAIN, 10));
        lblPreviewInvestment.setForeground(AppTheme.TEXT_SECONDARY);

        bottomBox.add(lblPreviewMath);
        bottomBox.add(lblPreviewInvestment);
        hero.add(bottomBox, BorderLayout.SOUTH);

        return hero;
    }

    private void onProductSelected() {
        Object item = cmbProducts.getSelectedItem();
        if (item instanceof Product) {
            selectedProduct = (Product) item;
            lblPreviewName.setText(selectedProduct.getName());
            lblPreviewMeta.setText("Code: " + selectedProduct.getCode() + " • Cat: " 
                    + (selectedProduct.getCategoryName() != null ? selectedProduct.getCategoryName() : "General")
                    + " • Min Alert: " + selectedProduct.getMinStockLevel());

            int stock = selectedProduct.getQuantity();
            if (stock <= 0) {
                lblPreviewStockPill.setText("🔴 OUT OF STOCK (0)");
                lblPreviewStockPill.setBackground(new Color(254, 226, 226));
                lblPreviewStockPill.setForeground(new Color(185, 28, 28));
            } else if (stock <= selectedProduct.getMinStockLevel()) {
                lblPreviewStockPill.setText("🟠 LOW STOCK (" + stock + ")");
                lblPreviewStockPill.setBackground(new Color(254, 243, 199));
                lblPreviewStockPill.setForeground(new Color(180, 83, 9));
            } else {
                lblPreviewStockPill.setText("🟢 IN STOCK (" + stock + ")");
                lblPreviewStockPill.setBackground(new Color(220, 252, 231));
                lblPreviewStockPill.setForeground(new Color(21, 128, 61));
            }

            txtCostPrice.setText(String.format("%.2f", selectedProduct.getPurchasePrice()));
            updateMathPreview();
        } else {
            selectedProduct = null;
            lblPreviewName.setText("Select a product to view stock");
            lblPreviewMeta.setText("Code: — • Category: — • Min Alert: —");
            lblPreviewStockPill.setText("No Selection");
            lblPreviewStockPill.setBackground(new Color(241, 245, 249));
            lblPreviewStockPill.setForeground(AppTheme.TEXT_MUTED);
            lblPreviewMath.setText("➔ Projected Stock: —");
            lblPreviewInvestment.setText("💰 Purchase Investment: ₹0.00");
        }
    }

    private void updateMathPreview() {
        if (selectedProduct == null) {
            lblPreviewMath.setText("➔ Projected Stock: —");
            lblPreviewInvestment.setText("💰 Purchase Investment: ₹0.00");
            return;
        }

        int currStock = selectedProduct.getQuantity();
        int addQty = 0;
        try {
            String qStr = txtQty.getText().trim();
            if (!qStr.isEmpty()) addQty = Integer.parseInt(qStr);
        } catch (Exception ignored) {}

        double cost = 0.0;
        try {
            String cStr = txtCostPrice.getText().trim();
            if (!cStr.isEmpty()) cost = Double.parseDouble(cStr);
        } catch (Exception ignored) {}

        int projected = currStock + addQty;
        double investment = addQty * cost;

        if (addQty > 0) {
            lblPreviewMath.setText("➔ Current: " + currStock + " + Inward: " + addQty + " ➔ Projected: " + projected + " units");
            lblPreviewInvestment.setText(String.format("💰 Purchase Investment: ₹%,.2f (%d units @ ₹%.2f)", investment, addQty, cost));
        } else {
            lblPreviewMath.setText("➔ Current Stock: " + currStock + " units");
            lblPreviewInvestment.setText("💰 Purchase Investment: ₹0.00");
        }
    }

    // =========================================================================
    // TABLES SETUP & RENDERING
    // =========================================================================
    private void initAlertsTable() {
        String[] cols = {"#", "Product Details", "Stock Status", "Current Stock", "Min Alert Level", "Unit Sell Price", "Action"};
        alertsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        alertsTable = new JTable(alertsModel);
        setupTableStyle(alertsTable);

        alertsTable.getColumnModel().getColumn(0).setPreferredWidth(35);  // #
        alertsTable.getColumnModel().getColumn(1).setPreferredWidth(210); // Product
        alertsTable.getColumnModel().getColumn(2).setPreferredWidth(125); // Status
        alertsTable.getColumnModel().getColumn(3).setPreferredWidth(95);  // Current
        alertsTable.getColumnModel().getColumn(4).setPreferredWidth(95);  // Min
        alertsTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Price
        alertsTable.getColumnModel().getColumn(6).setPreferredWidth(85);  // Action

        // Column 1: HTML Dual-line Product Details
        alertsTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                String raw = String.valueOf(val != null ? val : "");
                String[] parts = raw.split("###");
                String name = parts[0];
                String sub = parts.length > 1 ? parts[1] : "";
                l.setText("<html><body style='font-family:Segoe UI;'><b style='color:#111827; font-size:11px;'>" 
                        + escapeHtml(name) + "</b><br><span style='color:#6b7280; font-size:9px;'>" 
                        + escapeHtml(sub) + "</span></body></html>");
                l.setBorder(new EmptyBorder(2, 8, 2, 6));
                return l;
            }
        });

        // Column 2: Stock Status Pill Badge
        alertsTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                String status = String.valueOf(val != null ? val : "");
                if (status.contains("OUT")) {
                    l.setText("🔴 Out of Stock");
                    l.setForeground(new Color(185, 28, 28));
                    l.setFont(AppTheme.font(Font.BOLD, 10));
                } else {
                    l.setText("🟠 Low Stock");
                    l.setForeground(new Color(194, 65, 12));
                    l.setFont(AppTheme.font(Font.BOLD, 10));
                }
                return l;
            }
        });

        // Column 3 & 4: Bold Centered Counts
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        centerRender.setFont(AppTheme.font(Font.BOLD, 11));
        alertsTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        alertsTable.getColumnModel().getColumn(3).setCellRenderer(centerRender);
        alertsTable.getColumnModel().getColumn(4).setCellRenderer(centerRender);

        // Column 5: Currency
        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
        rightRender.setFont(AppTheme.font(Font.PLAIN, 11));
        rightRender.setBorder(new EmptyBorder(0, 4, 0, 10));
        alertsTable.getColumnModel().getColumn(5).setCellRenderer(rightRender);

        // Column 6: Action "⚡ Restock"
        alertsTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(AppTheme.font(Font.BOLD, 11));
                l.setForeground(AppTheme.FOREST_GREEN);
                l.setText("⚡ Restock");
                return l;
            }
        });

        alertsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = alertsTable.getSelectedRow();
                if (row != -1) {
                    handleQuickRestockFromRow(alertsTable, row, 1);
                }
            }
        });
    }

    private void initHistoryTable() {
        String[] cols = {"#", "Date & Time", "Product", "Movement", "Quantity", "Ref / Invoice", "Audit Notes"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        setupTableStyle(historyTable);

        historyTable.getColumnModel().getColumn(0).setPreferredWidth(35);  // #
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(135); // Date
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(190); // Product
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Movement
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(75);  // Qty
        historyTable.getColumnModel().getColumn(5).setPreferredWidth(135); // Ref
        historyTable.getColumnModel().getColumn(6).setPreferredWidth(170); // Notes

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        historyTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        historyTable.getColumnModel().getColumn(1).setCellRenderer(centerRender);
        historyTable.getColumnModel().getColumn(4).setCellRenderer(centerRender);

        // Column 3: Movement Pill (IN vs OUT)
        historyTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                String tStr = String.valueOf(val != null ? val : "");
                l.setFont(AppTheme.font(Font.BOLD, 11));
                if ("IN".equalsIgnoreCase(tStr) || tStr.contains("IN")) {
                    l.setText("⬇️ IN (Inward)");
                    l.setForeground(AppTheme.FOREST_GREEN);
                } else {
                    l.setText("⬆️ OUT (Sale)");
                    l.setForeground(new Color(220, 38, 38));
                }
                return l;
            }
        });

        // Column 5: Reference Monospaced
        historyTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(new Font("Monospaced", Font.BOLD, 11));
                l.setForeground(AppTheme.TEXT_PRIMARY);
                l.setText(String.valueOf(val != null ? val : "—"));
                return l;
            }
        });
    }

    private void initLedgerTable() {
        String[] cols = {"#", "Code", "Product Name", "Category", "Stock", "Cost Valuation", "Sell Price", "Status", "Action"};
        ledgerModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        ledgerTable = new JTable(ledgerModel);
        setupTableStyle(ledgerTable);

        ledgerTable.getColumnModel().getColumn(0).setPreferredWidth(35);  // #
        ledgerTable.getColumnModel().getColumn(1).setPreferredWidth(75);  // Code
        ledgerTable.getColumnModel().getColumn(2).setPreferredWidth(175); // Name
        ledgerTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Category
        ledgerTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Stock
        ledgerTable.getColumnModel().getColumn(5).setPreferredWidth(105); // Cost Valuation
        ledgerTable.getColumnModel().getColumn(6).setPreferredWidth(90);  // Sell Price
        ledgerTable.getColumnModel().getColumn(7).setPreferredWidth(100); // Status
        ledgerTable.getColumnModel().getColumn(8).setPreferredWidth(75);  // Action

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        ledgerTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        ledgerTable.getColumnModel().getColumn(1).setCellRenderer(centerRender);
        ledgerTable.getColumnModel().getColumn(4).setCellRenderer(centerRender);

        // Column 7: Status Pill
        ledgerTable.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(AppTheme.font(Font.BOLD, 10));
                String s = String.valueOf(val != null ? val : "");
                if (s.contains("OUT")) {
                    l.setText("🔴 Out of Stock");
                    l.setForeground(new Color(185, 28, 28));
                } else if (s.contains("LOW")) {
                    l.setText("🟠 Low Stock");
                    l.setForeground(new Color(194, 65, 12));
                } else {
                    l.setText("🟢 Healthy");
                    l.setForeground(AppTheme.FOREST_GREEN);
                }
                return l;
            }
        });

        // Column 8: Restock Action
        ledgerTable.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(AppTheme.font(Font.BOLD, 11));
                l.setForeground(AppTheme.FOREST_GREEN);
                l.setText("⚡ Restock");
                return l;
            }
        });

        ledgerTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = ledgerTable.getSelectedRow();
                if (row != -1) {
                    handleQuickRestockFromRow(ledgerTable, row, 1);
                }
            }
        });
    }

    private void setupTableStyle(JTable t) {
        t.setRowHeight(42);
        t.setFont(AppTheme.font(Font.PLAIN, 12));
        t.setShowVerticalLines(false);
        t.setGridColor(new Color(241, 246, 243));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setSelectionBackground(new Color(230, 248, 238));
        t.setSelectionForeground(AppTheme.TEXT_PRIMARY);

        JTableHeader header = t.getTableHeader();
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, focus, r, c);
                l.setFont(AppTheme.font(Font.BOLD, 11));
                l.setForeground(AppTheme.TEXT_PRIMARY);
                l.setBackground(new Color(240, 250, 244));
                l.setOpaque(true);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_SAGE),
                        new EmptyBorder(0, 8, 0, 8)
                ));
                if (c == 0 || c == tbl.getColumnCount() - 1) {
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return l;
            }
        });
    }

    private JScrollPane createTableScrollPane(JTable table, String type) {
        JScrollPane scroll = new JScrollPane();

        // Custom Viewport with empty state watermark
        JViewport customViewport = new JViewport() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (table.getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth(), h = getHeight();
                    String msg;
                    if ("alerts".equals(type)) {
                        msg = "🎉 Excellent! Zero low-stock or out-of-stock alerts found.";
                    } else if ("history".equals(type)) {
                        msg = "No inventory movements recorded in audit log.";
                    } else {
                        msg = "No products found matching query.";
                    }

                    g2.setFont(AppTheme.font(Font.BOLD, 13));
                    g2.setColor(AppTheme.TEXT_MUTED);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (w - fm.stringWidth(msg)) / 2;
                    int ty = h / 2;
                    g2.drawString(msg, tx, ty);
                    g2.dispose();
                }
            }
        };
        customViewport.setOpaque(false);
        customViewport.setView(table);
        scroll.setViewport(customViewport);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(205, 230, 215);
                this.trackColor = new Color(248, 252, 249);
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 1, thumbBounds.width - 2, thumbBounds.height - 2, 6, 6);
                g2.dispose();
            }
        });
        return scroll;
    }

    private void handleQuickRestockFromRow(JTable table, int row, int codeColIndex) {
        Object codeVal = table.getValueAt(row, codeColIndex);
        String codeStr = "";
        if (codeVal != null) {
            String raw = codeVal.toString();
            if (raw.contains("Code: ")) {
                int idx = raw.indexOf("Code: ");
                codeStr = raw.substring(idx + 6).split("•")[0].trim();
            } else {
                codeStr = raw.trim();
            }
        }

        if (!codeStr.isEmpty()) {
            for (int i = 0; i < cmbProducts.getItemCount(); i++) {
                Product p = cmbProducts.getItemAt(i);
                if (p != null && (p.getCode().equalsIgnoreCase(codeStr) || p.getName().equalsIgnoreCase(codeStr))) {
                    cmbProducts.setSelectedIndex(i);
                    txtQty.requestFocusInWindow();
                    break;
                }
            }
        }
    }

    // =========================================================================
    // DATA LOADING & FILTERING
    // =========================================================================
    public void loadDropdownData() {
        try {
            cmbProducts.removeAllItems();
            List<Product> products = productDAO.getAllProducts();
            if (products != null) {
                this.allProducts = products;
                for (Product p : products) {
                    cmbProducts.addItem(p);
                }
            }

            cmbSuppliers.removeAllItems();
            List<Supplier> suppliers = supplierDAO.getAllSuppliers();
            if (suppliers != null) {
                this.allSuppliers = suppliers;
                for (Supplier s : suppliers) {
                    cmbSuppliers.addItem(s);
                }
            }
        } catch (Exception e) {
            System.err.println("Notice: Could not load dropdowns: " + e.getMessage());
        }
    }

    public void refreshData() {
        // 1. Fetch products & calculate stats
        try {
            allProducts = productDAO.getAllProducts();
        } catch (Exception e) {
            System.err.println("Notice: Could not load all products: " + e.getMessage());
        }

        try {
            lowStockProducts = productDAO.getLowStockProducts();
        } catch (Exception e) {
            System.err.println("Notice: Could not load low stock: " + e.getMessage());
        }

        try {
            stockHistoryList = reportDAO.getStockHistory(150);
        } catch (Exception e) {
            System.err.println("Notice: Could not load stock history: " + e.getMessage());
        }

        updateKpiStats();
        applyActiveFilters();
    }

    private void updateKpiStats() {
        int totalSkus = allProducts != null ? allProducts.size() : 0;
        int lowStock = 0;
        int outOfStock = 0;
        double totalValuation = 0.0;

        if (allProducts != null) {
            for (Product p : allProducts) {
                int q = p.getQuantity();
                if (q <= 0) {
                    outOfStock++;
                } else if (q <= p.getMinStockLevel()) {
                    lowStock++;
                }
                totalValuation += (q * p.getPurchasePrice());
            }
        }

        if (lblKpiTotalSkus != null) lblKpiTotalSkus.setText(totalSkus + " SKUs");
        if (lblKpiLowStock != null) lblKpiLowStock.setText(lowStock + " Items");
        if (lblKpiOutOfStock != null) lblKpiOutOfStock.setText(outOfStock + " Items");
        if (lblKpiValuation != null) lblKpiValuation.setText(String.format("₹%,.2f", totalValuation));

        // Update tab pill badge labels
        if (btnTabAlerts != null) {
            btnTabAlerts.setText("⚠️ Stock Alerts (" + (lowStock + outOfStock) + ")");
        }
        if (btnTabHistory != null) {
            int histCount = stockHistoryList != null ? stockHistoryList.size() : 0;
            btnTabHistory.setText("📋 Movement History (" + histCount + ")");
        }
        if (btnTabLedger != null) {
            btnTabLedger.setText("📦 Full Inventory (" + totalSkus + ")");
        }
    }

    private void applyActiveFilters() {
        String q = searchQuery.toLowerCase();

        if ("ALERTS".equals(currentTab)) {
            alertsModel.setRowCount(0);
            int idx = 1;
            for (Product p : lowStockProducts) {
                boolean isOut = p.getQuantity() <= 0;
                if ("OUT_OF_STOCK".equals(alertsFilter) && !isOut) continue;
                if ("LOW_STOCK".equals(alertsFilter) && isOut) continue;

                if (!q.isEmpty()) {
                    boolean match = p.getName().toLowerCase().contains(q)
                            || p.getCode().toLowerCase().contains(q)
                            || (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(q));
                    if (!match) continue;
                }

                String meta = "Code: " + p.getCode() + " • " + (p.getCategoryName() != null ? p.getCategoryName() : "General");
                alertsModel.addRow(new Object[]{
                        idx++,
                        p.getName() + "###" + meta,
                        isOut ? "OUT_OF_STOCK" : "LOW_STOCK",
                        p.getQuantity() + " units",
                        p.getMinStockLevel() + " units",
                        String.format("₹%.2f", p.getSellingPrice()),
                        "⚡ Restock"
                });
            }
            lblRecordCount.setText(alertsModel.getRowCount() + " alerts");

        } else if ("HISTORY".equals(currentTab)) {
            historyModel.setRowCount(0);
            int idx = 1;
            for (String[] row : stockHistoryList) {
                if (row.length < 7) continue;
                String type = row[3];
                if ("IN".equals(historyFilter) && !"IN".equalsIgnoreCase(type)) continue;
                if ("OUT".equals(historyFilter) && !"OUT".equalsIgnoreCase(type)) continue;

                if (!q.isEmpty()) {
                    boolean match = row[1].toLowerCase().contains(q)
                            || row[2].toLowerCase().contains(q)
                            || row[5].toLowerCase().contains(q)
                            || row[6].toLowerCase().contains(q);
                    if (!match) continue;
                }

                historyModel.addRow(new Object[]{
                        idx++,
                        row[0],
                        row[2] + " (" + row[1] + ")",
                        type,
                        row[4] + " units",
                        row[5],
                        row[6]
                });
            }
            lblRecordCount.setText(historyModel.getRowCount() + " records");

        } else if ("LEDGER".equals(currentTab)) {
            ledgerModel.setRowCount(0);
            int idx = 1;
            for (Product p : allProducts) {
                boolean isOut = p.getQuantity() <= 0;
                boolean isLow = p.getQuantity() <= p.getMinStockLevel();

                if ("HEALTHY".equals(ledgerFilter) && isLow) continue;
                if ("REORDER".equals(ledgerFilter) && !isLow) continue;

                if (!q.isEmpty()) {
                    boolean match = p.getName().toLowerCase().contains(q)
                            || p.getCode().toLowerCase().contains(q)
                            || (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(q));
                    if (!match) continue;
                }

                String status = isOut ? "OUT" : (isLow ? "LOW" : "HEALTHY");
                double costValuation = p.getQuantity() * p.getPurchasePrice();

                ledgerModel.addRow(new Object[]{
                        idx++,
                        p.getCode(),
                        p.getName(),
                        p.getCategoryName() != null ? p.getCategoryName() : "General",
                        p.getQuantity() + " units",
                        String.format("₹%,.2f", costValuation),
                        String.format("₹%.2f", p.getSellingPrice()),
                        status,
                        "⚡ Restock"
                });
            }
            lblRecordCount.setText(ledgerModel.getRowCount() + " products");
        }
    }

    private void switchTab(String tabKey) {
        this.currentTab = tabKey;
        tableCardLayout.show(tableCardContainer, tabKey);
        btnTabAlerts.repaint();
        btnTabHistory.repaint();
        btnTabLedger.repaint();
        rebuildFilterChips();
        applyActiveFilters();
    }

    private void rebuildFilterChips() {
        filterChipsPanel.removeAll();

        if ("ALERTS".equals(currentTab)) {
            filterChipsPanel.add(createFilterChip("All Alerts", "ALL", alertsFilter, k -> { alertsFilter = k; applyActiveFilters(); }));
            filterChipsPanel.add(createFilterChip("🔴 Out of Stock (0)", "OUT_OF_STOCK", alertsFilter, k -> { alertsFilter = k; applyActiveFilters(); }));
            filterChipsPanel.add(createFilterChip("🟠 Low Stock (>0)", "LOW_STOCK", alertsFilter, k -> { alertsFilter = k; applyActiveFilters(); }));
        } else if ("HISTORY".equals(currentTab)) {
            filterChipsPanel.add(createFilterChip("All Movements", "ALL", historyFilter, k -> { historyFilter = k; applyActiveFilters(); }));
            filterChipsPanel.add(createFilterChip("⬇️ Stock IN", "IN", historyFilter, k -> { historyFilter = k; applyActiveFilters(); }));
            filterChipsPanel.add(createFilterChip("⬆️ Stock OUT", "OUT", historyFilter, k -> { historyFilter = k; applyActiveFilters(); }));
        } else if ("LEDGER".equals(currentTab)) {
            filterChipsPanel.add(createFilterChip("All Products", "ALL", ledgerFilter, k -> { ledgerFilter = k; applyActiveFilters(); }));
            filterChipsPanel.add(createFilterChip("🟢 Healthy Stock", "HEALTHY", ledgerFilter, k -> { ledgerFilter = k; applyActiveFilters(); }));
            filterChipsPanel.add(createFilterChip("⚠️ Needs Reorder", "REORDER", ledgerFilter, k -> { ledgerFilter = k; applyActiveFilters(); }));
        }

        filterChipsPanel.revalidate();
        filterChipsPanel.repaint();
    }

    // =========================================================================
    // INWARD PURCHASE TRANSACTION LOGIC
    // =========================================================================
    private void processStockIn() {
        Product prod = (Product) cmbProducts.getSelectedItem();
        Supplier supp = (Supplier) cmbSuppliers.getSelectedItem();
        String qtyStr = txtQty.getText().trim();
        String costStr = txtCostPrice.getText().trim();

        if (prod == null || qtyStr.isEmpty() || costStr.isEmpty()) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Please select a product and enter valid quantity and cost price.", "Inward Validation", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        try {
            int qty = Integer.parseInt(qtyStr);
            double cost = Double.parseDouble(costStr);
            if (qty <= 0 || cost < 0) {
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(this, "Quantity must be positive and cost cannot be negative.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                }
                return;
            }

            String invoiceNo = purchaseDAO.generateNextPurchaseInvoiceNo();
            int supplierId = supp != null ? supp.getId() : 0;

            boolean success = purchaseDAO.recordPurchase(invoiceNo, supplierId, prod.getId(), qty, cost);
            if (success) {
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(this,
                            "Stock updated successfully!\n"
                            + "Purchase Invoice: " + invoiceNo + "\n"
                            + "Product: " + prod.getName() + " (+" + qty + " units)\n"
                            + "Total Inward Cost: ₹" + String.format("%.2f", (qty * cost)),
                            "Inward Success", JOptionPane.INFORMATION_MESSAGE);
                }
                txtQty.setText("");
                refreshData();
                loadDropdownData();
            } else {
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(this, "Failed to update warehouse stock in database.", "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException ex) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Invalid number format for Quantity or Cost Price.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearForm() {
        if (cmbProducts.getItemCount() > 0) cmbProducts.setSelectedIndex(0);
        if (cmbSuppliers.getItemCount() > 0) cmbSuppliers.setSelectedIndex(0);
        txtQty.setText("");
        txtCostPrice.setText("");
        onProductSelected();
    }

    // =========================================================================
    // UI BUILDER HELPERS & DESIGN SYSTEM COMPONENTS
    // =========================================================================
    private JPanel createElevatedCard(int arc) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Ambient drop shadow
                g2.setColor(new Color(0, 50, 30, 6));
                g2.fillRoundRect(1, 2, w - 2, h - 2, arc, arc);
                // Surface
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                // Border
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        return card;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, String subtitle, Color accentColor, String iconName, Runnable onClick) {
        SidebarIcon bgIcon = new SidebarIcon(iconName, 44, accentColor);
        JPanel card = new JPanel(new BorderLayout()) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                    @Override public void mouseClicked(MouseEvent e) { if (onClick != null) onClick.run(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int r = 18;
                int w = getWidth(), h = getHeight();

                g2.setColor(new Color(0, 50, 30, hovered ? 10 : 5));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.setColor(hovered ? AppTheme.FOREST_MID : AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);

                Shape clip = new RoundRectangle2D.Float(0, 0, w, h, r, r);
                g2.setClip(clip);

                // Watermark icon
                int iconSz = 44;
                int iconX = w - iconSz - 10;
                int iconY = (h - iconSz) / 2;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hovered ? 0.22f : 0.12f));
                bgIcon.paintIcon(this, g2, iconX, iconY);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setBorder(new EmptyBorder(8, 14, 8, 14));
        card.setPreferredSize(new Dimension(160, 76));

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(AppTheme.font(Font.BOLD, 10));
        lblTitle.setForeground(AppTheme.TEXT_MUTED);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(AppTheme.font(Font.BOLD, 18));
        valueLabel.setForeground(AppTheme.TEXT_PRIMARY);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(AppTheme.font(Font.PLAIN, 10));
        lblSub.setForeground(AppTheme.TEXT_SECONDARY);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        textCol.add(lblTitle);
        textCol.add(Box.createVerticalStrut(2));
        textCol.add(valueLabel);
        textCol.add(Box.createVerticalStrut(2));
        textCol.add(lblSub);

        card.add(textCol, BorderLayout.CENTER);
        return card;
    }

    private JButton createPillButton(String text, Color bg, Color border, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = h;

                if (hovered) {
                    if (bg.equals(AppTheme.FOREST_GREEN)) {
                        g2.setColor(AppTheme.FOREST_MID);
                    } else {
                        g2.setColor(new Color(Math.max(0, bg.getRed() - 8), Math.max(0, bg.getGreen() - 8), Math.max(0, bg.getBlue() - 8)));
                    }
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, w, h, r, r);

                if (border != null) {
                    g2.setColor(border);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                }

                g2.setColor(fg);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createTabButton(String text, String tabKey) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
                addActionListener(e -> switchTab(tabKey));
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = h;
                boolean active = currentTab.equals(tabKey);

                if (active) {
                    g2.setColor(AppTheme.FOREST_GREEN);
                    g2.fillRoundRect(0, 0, w, h, r, r);
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(hovered ? new Color(240, 252, 244) : Color.WHITE);
                    g2.fillRoundRect(0, 0, w, h, r, r);
                    g2.setColor(AppTheme.BORDER_SAGE);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                    g2.setColor(hovered ? AppTheme.FOREST_GREEN : AppTheme.TEXT_SECONDARY);
                }

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 11));
        btn.setPreferredSize(new Dimension(195, 30));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createFilterChip(String text, String chipKey, String activeKey, java.util.function.Consumer<String> onSelect) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = h;
                boolean active = chipKey.equals(activeKey);

                if (active) {
                    g2.setColor(AppTheme.FOREST_GREEN);
                    g2.fillRoundRect(0, 0, w, h, r, r);
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, w, h, r, r);
                    g2.setColor(AppTheme.BORDER_SAGE);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                    g2.setColor(AppTheme.TEXT_SECONDARY);
                }

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 10));
        int strW = btn.getFontMetrics(btn.getFont()).stringWidth(text);
        btn.setPreferredSize(new Dimension(strW + 24, 26));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            onSelect.accept(chipKey);
            rebuildFilterChips();
        });
        return btn;
    }

    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppTheme.font(Font.BOLD, 11));
        l.setForeground(AppTheme.TEXT_PRIMARY);
        return l;
    }

    private void styleComboBox(JComboBox<?> box) {
        box.setFont(AppTheme.font(Font.PLAIN, 12));
        box.setBackground(Color.WHITE);
        box.setForeground(AppTheme.TEXT_PRIMARY);
        box.setPreferredSize(new Dimension(0, 32));
        box.setFocusable(false);
        box.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(AppTheme.BORDER_SAGE, 1, true),
                new EmptyBorder(2, 6, 2, 4)
        ));
        box.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int w = getWidth(), h = getHeight();
                        g2.setColor(new Color(245, 252, 248));
                        g2.fillRect(0, 0, w, h);
                        g2.setColor(AppTheme.FOREST_MID);
                        int cx = w / 2, cy = h / 2;
                        java.awt.geom.Path2D p = new java.awt.geom.Path2D.Float();
                        p.moveTo(cx - 4, cy - 2);
                        p.lineTo(cx + 4, cy - 2);
                        p.lineTo(cx, cy + 3);
                        p.closePath();
                        g2.fill(p);
                        g2.dispose();
                    }
                };
                btn.setBorder(BorderFactory.createEmptyBorder());
                btn.setContentAreaFilled(false);
                btn.setFocusPainted(false);
                btn.setPreferredSize(new Dimension(24, 0));
                return btn;
            }
        });
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    // =========================================================================
    // MODERN INPUT & SEARCH FIELDS
    // =========================================================================
    private static class ModernInputField extends JTextField {
        private final String placeholder;
        private boolean focused = false;

        public ModernInputField(String placeholder) {
            this.placeholder = placeholder;
            setFont(AppTheme.font(Font.PLAIN, 12));
            setForeground(AppTheme.TEXT_PRIMARY);
            setOpaque(false);
            setBorder(new EmptyBorder(6, 10, 6, 10));
            setPreferredSize(new Dimension(0, 32));

            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int r = 12;

            g2.setColor(new Color(250, 253, 251));
            g2.fillRoundRect(0, 0, w, h, r, r);

            if (focused) {
                g2.setColor(AppTheme.FOREST_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
            super.paintComponent(g);

            if (getText().isEmpty() && !focused && placeholder != null) {
                g2.setColor(AppTheme.TEXT_MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(placeholder, 10, ty);
            }
            g2.dispose();
        }
    }

    private static class ModernSearchField extends JTextField {
        private final String placeholder;
        private boolean focused = false;

        public ModernSearchField(String placeholder) {
            this.placeholder = placeholder;
            setFont(AppTheme.font(Font.PLAIN, 12));
            setForeground(AppTheme.TEXT_PRIMARY);
            setOpaque(false);
            setBorder(new EmptyBorder(6, 30, 6, 10));

            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int r = h;

            g2.setColor(new Color(248, 252, 249));
            g2.fillRoundRect(0, 0, w, h, r, r);

            if (focused) {
                g2.setColor(AppTheme.FOREST_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);

            // Search magnifying glass icon
            g2.setColor(focused ? AppTheme.FOREST_GREEN : AppTheme.TEXT_MUTED);
            g2.setStroke(new BasicStroke(1.6f));
            int cy = h / 2 - 1;
            g2.drawOval(10, cy - 4, 8, 8);
            g2.drawLine(16, cy + 3, 21, cy + 8);

            super.paintComponent(g);

            if (getText().isEmpty() && !focused && placeholder != null) {
                g2.setColor(AppTheme.TEXT_MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(placeholder, 32, ty);
            }
            g2.dispose();
        }
    }
}
