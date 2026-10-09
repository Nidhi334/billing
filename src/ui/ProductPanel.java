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
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Keyra-Themed Product Management & Barcode Hub
 */
public class ProductPanel extends JPanel {
    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    // Table & Models
    private JTable table;
    private DefaultTableModel tableModel;
    private List<Product> loadedProducts = new ArrayList<>();

    // Form inputs
    private JTextField txtCode, txtBarcode, txtName, txtPurchasePrice, txtSellingPrice, txtQty, txtMinStock;
    private ModernSearchField txtSearch;
    private JLabel lblBarcodePreview;
    private JComboBox<Category> cmbCategory;
    private JComboBox<String> cmbCategoryFilter;
    private JLabel lblMarginBadge;
    private JLabel lblFormTitle, lblFormBadge;

    // Buttons
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnCancelEdit, btnManageCategories;
    private JButton btnGenBarcode, btnAutoSku, btnViewBarcode, btnPrintBarcode;
    private JButton btnFilterAll, btnFilterInStock, btnFilterLowStock, btnFilterOutOfStock;

    // KPI Summary Labels
    private JLabel lblKpiTotal, lblKpiInStock, lblKpiLowStock, lblKpiValuation;
    private JLabel lblResultCount;

    // State
    private int selectedProductId = -1;
    private String currentStatusFilter = "ALL"; // ALL, IN_STOCK, LOW_STOCK, OUT_OF_STOCK
    private String selectedImagePath = null;
    private JLabel lblImagePreview;
    private JButton btnChooseImage, btnRemoveImage;
    private JPanel editButtonsRow;

    public ProductPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(AppTheme.BG_CANVAS);
        setBorder(new EmptyBorder(12, 14, 12, 14));
        initComponents();
        loadCategories();
        loadProductTable();
    }

    private void initComponents() {
        // TOP CONTAINER: Header Strip & KPI Metric Cards
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

        JLabel lblTitle = new JLabel("📦 Product Catalog & Barcode Hub");
        lblTitle.setFont(AppTheme.font(Font.BOLD, 15));
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel lblBadge = new JLabel(" CATALOG MANAGER ") {
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

        JLabel lblSubtitle = new JLabel("Inventory stock, retail pricing margins & automated barcode generation");
        lblSubtitle.setFont(AppTheme.font(Font.PLAIN, 11));
        lblSubtitle.setForeground(AppTheme.TEXT_MUTED);

        headerLeft.add(lblTitle);
        headerLeft.add(lblBadge);
        headerLeft.add(lblSubtitle);
        headerStrip.add(headerLeft, BorderLayout.WEST);

        // Header Right Action Buttons
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerRight.setOpaque(false);

        btnManageCategories = createPillButton("🏷️ Categories", new Color(240, 250, 244), AppTheme.BORDER_SAGE, AppTheme.FOREST_GREEN);
        btnManageCategories.setFont(AppTheme.font(Font.BOLD, 11));
        btnManageCategories.addActionListener(e -> manageCategories());

        JButton btnHeaderRefresh = createPillButton("🔄 Refresh", Color.WHITE, AppTheme.BORDER_SAGE, AppTheme.TEXT_SECONDARY);
        btnHeaderRefresh.setFont(AppTheme.font(Font.BOLD, 11));
        btnHeaderRefresh.addActionListener(e -> {
            if (txtSearch != null) txtSearch.setText("");
            currentStatusFilter = "ALL";
            updateFilterButtonsUI();
            loadProductTable();
        });

        headerRight.add(btnManageCategories);
        headerRight.add(btnHeaderRefresh);
        headerStrip.add(headerRight, BorderLayout.EAST);

        topContainer.add(headerStrip, BorderLayout.NORTH);

        // 2. Row of 4 Elevated KPI Summary Metric Cards
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 10, 0));
        kpiRow.setOpaque(false);

        lblKpiTotal = new JLabel("0");
        lblKpiInStock = new JLabel("0");
        lblKpiLowStock = new JLabel("0");
        lblKpiValuation = new JLabel("₹0.00");

        JPanel cardTotal = createKpiCard("TOTAL PRODUCTS", lblKpiTotal, "Active in catalog", AppTheme.FOREST_MID, "products", () -> {
            currentStatusFilter = "ALL";
            updateFilterButtonsUI();
            applyFilter();
        });
        JPanel cardInStock = createKpiCard("IN STOCK", lblKpiInStock, "Healthy inventory", AppTheme.STATUS_SUCCESS, "check_circle", () -> {
            currentStatusFilter = "IN_STOCK";
            updateFilterButtonsUI();
            applyFilter();
        });
        JPanel cardLowStock = createKpiCard("LOW / OUT OF STOCK", lblKpiLowStock, "Needs reorder", AppTheme.STATUS_WARNING, "alert_triangle", () -> {
            currentStatusFilter = "LOW_STOCK";
            updateFilterButtonsUI();
            applyFilter();
        });
        JPanel cardValuation = createKpiCard("INVENTORY VALUATION", lblKpiValuation, "Total retail stock", AppTheme.FOREST_DEEP, "wallet", () -> {
            currentStatusFilter = "ALL";
            updateFilterButtonsUI();
            applyFilter();
        });

        kpiRow.add(cardTotal);
        kpiRow.add(cardInStock);
        kpiRow.add(cardLowStock);
        kpiRow.add(cardValuation);

        topContainer.add(kpiRow, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // =========================================================================
        // CENTER: 2-COLUMN WORKSPACE (Left Form Card + Right Catalog Table Card)
        // =========================================================================
        JPanel centerSplit = new JPanel(new BorderLayout(12, 0));
        centerSplit.setOpaque(false);

        // -------------------------------------------------------------------------
        // LEFT COLUMN: Elevated Product Form Card (370px)
        // -------------------------------------------------------------------------
        JPanel formCard = createElevatedCard(20);
        formCard.setPreferredSize(new Dimension(370, 0));
        formCard.setLayout(new BorderLayout(0, 8));
        formCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Form Card Top Header
        JPanel formHeader = new JPanel(new BorderLayout(8, 0));
        formHeader.setOpaque(false);

        JPanel formTitleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        formTitleRow.setOpaque(false);

        lblFormTitle = new JLabel("✨ Add New Product");
        lblFormTitle.setFont(AppTheme.font(Font.BOLD, 14));
        lblFormTitle.setForeground(AppTheme.TEXT_PRIMARY);

        lblFormBadge = new JLabel("") {
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
        lblFormBadge.setVisible(false);

        formTitleRow.add(lblFormTitle);
        formTitleRow.add(lblFormBadge);
        formHeader.add(formTitleRow, BorderLayout.WEST);

        btnClear = createPillButton("Clear Form", new Color(248, 250, 252), new Color(226, 232, 240), AppTheme.TEXT_MUTED);
        btnClear.setFont(AppTheme.font(Font.PLAIN, 11));
        btnClear.setPreferredSize(new Dimension(84, 26));
        btnClear.addActionListener(e -> clearForm());
        formHeader.add(btnClear, BorderLayout.EAST);

        formCard.add(formHeader, BorderLayout.NORTH);

        // Form Fields Container
        JPanel formBody = new JPanel();
        formBody.setLayout(new BoxLayout(formBody, BoxLayout.Y_AXIS));
        formBody.setOpaque(false);
        formBody.setBorder(new EmptyBorder(0, 0, 0, 4));

        // Row 1: Code & Barcode
        JPanel codeBarcodeRow = new JPanel(new GridLayout(1, 2, 8, 0));
        codeBarcodeRow.setOpaque(false);

        // SKU / Code Column
        JPanel colCode = new JPanel(new BorderLayout(0, 3));
        colCode.setOpaque(false);
        JPanel codeLabelRow = new JPanel(new BorderLayout());
        codeLabelRow.setOpaque(false);
        codeLabelRow.add(createFieldLabel("Product SKU:"), BorderLayout.WEST);
        btnAutoSku = createSmallTextButton("⚡ Auto", e -> generateRandomSku());
        codeLabelRow.add(btnAutoSku, BorderLayout.EAST);
        colCode.add(codeLabelRow, BorderLayout.NORTH);
        txtCode = new ModernInputField("e.g. PRD-101");
        colCode.add(txtCode, BorderLayout.CENTER);

        // Barcode Column
        JPanel colBarcode = new JPanel(new BorderLayout(0, 3));
        colBarcode.setOpaque(false);
        JPanel barcodeLabelRow = new JPanel(new BorderLayout());
        barcodeLabelRow.setOpaque(false);
        barcodeLabelRow.add(createFieldLabel("Barcode:"), BorderLayout.WEST);
        btnGenBarcode = createSmallTextButton("⚡ Auto EAN", e -> generateRandomBarcode());
        barcodeLabelRow.add(btnGenBarcode, BorderLayout.EAST);
        colBarcode.add(barcodeLabelRow, BorderLayout.NORTH);
        txtBarcode = new ModernInputField("8901234567890");
        txtBarcode.setFont(new Font("Monospaced", Font.BOLD, 12));
        colBarcode.add(txtBarcode, BorderLayout.CENTER);

        codeBarcodeRow.add(colCode);
        codeBarcodeRow.add(colBarcode);
        formBody.add(codeBarcodeRow);
        formBody.add(Box.createVerticalStrut(6));

        // Row 2: Live Barcode Vector Preview Card
        JPanel barcodePreviewCard = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(248, 252, 249));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        barcodePreviewCard.setOpaque(false);
        barcodePreviewCard.setBorder(new EmptyBorder(6, 8, 6, 8));

        lblBarcodePreview = new JLabel("Barcode Preview", SwingConstants.CENTER);
        lblBarcodePreview.setPreferredSize(new Dimension(280, 52));
        lblBarcodePreview.setHorizontalAlignment(SwingConstants.CENTER);
        barcodePreviewCard.add(lblBarcodePreview, BorderLayout.CENTER);

        JPanel barcodeActionsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 2));
        barcodeActionsRow.setOpaque(false);

        btnViewBarcode = createPillButton("🔍 Fullscreen", new Color(238, 242, 255), new Color(199, 210, 254), new Color(67, 56, 202));
        btnViewBarcode.setFont(AppTheme.font(Font.BOLD, 10));
        btnViewBarcode.setPreferredSize(new Dimension(98, 24));
        btnViewBarcode.addActionListener(e -> viewSelectedBarcode());

        btnPrintBarcode = createPillButton("🖨️ Print Label", new Color(240, 253, 250), new Color(153, 246, 228), new Color(15, 118, 110));
        btnPrintBarcode.setFont(AppTheme.font(Font.BOLD, 10));
        btnPrintBarcode.setPreferredSize(new Dimension(102, 24));
        btnPrintBarcode.addActionListener(e -> printSelectedBarcode());

        barcodeActionsRow.add(btnViewBarcode);
        barcodeActionsRow.add(btnPrintBarcode);
        barcodePreviewCard.add(barcodeActionsRow, BorderLayout.SOUTH);

        formBody.add(barcodePreviewCard);
        formBody.add(Box.createVerticalStrut(6));

        // Row 3: Product Name
        JPanel nameRow = new JPanel(new BorderLayout(0, 3));
        nameRow.setOpaque(false);
        nameRow.add(createFieldLabel("Product Name:"), BorderLayout.NORTH);
        txtName = new ModernInputField("e.g. Amul Pasteurized Butter 500g");
        nameRow.add(txtName, BorderLayout.CENTER);
        formBody.add(nameRow);
        formBody.add(Box.createVerticalStrut(6));

        // Row 4: Category & Quick Add
        JPanel catRow = new JPanel(new BorderLayout(0, 3));
        catRow.setOpaque(false);
        JPanel catHeaderRow = new JPanel(new BorderLayout());
        catHeaderRow.setOpaque(false);
        catHeaderRow.add(createFieldLabel("Category / Department:"), BorderLayout.WEST);
        JButton btnQuickAddCat = createSmallTextButton("+ Manage", e -> manageCategories());
        catHeaderRow.add(btnQuickAddCat, BorderLayout.EAST);
        catRow.add(catHeaderRow, BorderLayout.NORTH);

        cmbCategory = new JComboBox<>();
        cmbCategory.setFont(AppTheme.font(Font.PLAIN, 12));
        cmbCategory.setBackground(Color.WHITE);
        cmbCategory.setPreferredSize(new Dimension(0, 32));
        catRow.add(cmbCategory, BorderLayout.CENTER);
        formBody.add(catRow);
        formBody.add(Box.createVerticalStrut(6));

        // Row 5: Pricing (Buy & Sell) with Live Margin Calculator
        JPanel priceRow = new JPanel(new GridLayout(1, 2, 8, 0));
        priceRow.setOpaque(false);

        JPanel pBuy = new JPanel(new BorderLayout(0, 3));
        pBuy.setOpaque(false);
        pBuy.add(createFieldLabel("Buy Price (₹):"), BorderLayout.NORTH);
        txtPurchasePrice = new ModernInputField("0.00");
        pBuy.add(txtPurchasePrice, BorderLayout.CENTER);

        JPanel pSell = new JPanel(new BorderLayout(0, 3));
        pSell.setOpaque(false);
        pSell.add(createFieldLabel("Sell Price (₹):"), BorderLayout.NORTH);
        txtSellingPrice = new ModernInputField("0.00");
        pSell.add(txtSellingPrice, BorderLayout.CENTER);

        priceRow.add(pBuy);
        priceRow.add(pSell);
        formBody.add(priceRow);

        // Margin Pill Indicator
        lblMarginBadge = new JLabel("Margin: 0.0%") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                int h = getHeight();
                g2.fillRoundRect(0, 0, getWidth(), h, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblMarginBadge.setFont(AppTheme.font(Font.BOLD, 10));
        lblMarginBadge.setOpaque(false);
        lblMarginBadge.setBackground(new Color(236, 253, 245));
        lblMarginBadge.setForeground(new Color(22, 101, 52));
        lblMarginBadge.setBorder(new EmptyBorder(2, 8, 2, 8));
        lblMarginBadge.setVisible(false);

        JPanel marginWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 3));
        marginWrap.setOpaque(false);
        marginWrap.add(lblMarginBadge);
        formBody.add(marginWrap);
        formBody.add(Box.createVerticalStrut(2));

        // Row 6: Stock & Min Alert Level
        JPanel stockRow = new JPanel(new GridLayout(1, 2, 8, 0));
        stockRow.setOpaque(false);

        JPanel pQty = new JPanel(new BorderLayout(0, 3));
        pQty.setOpaque(false);
        pQty.add(createFieldLabel("Stock Qty:"), BorderLayout.NORTH);
        txtQty = new ModernInputField("10");
        pQty.add(txtQty, BorderLayout.CENTER);

        JPanel pMin = new JPanel(new BorderLayout(0, 3));
        pMin.setOpaque(false);
        pMin.add(createFieldLabel("Min Alert Level:"), BorderLayout.NORTH);
        txtMinStock = new ModernInputField("5");
        pMin.add(txtMinStock, BorderLayout.CENTER);

        stockRow.add(pQty);
        stockRow.add(pMin);
        formBody.add(stockRow);
        formBody.add(Box.createVerticalStrut(6));

        // Row 7: Product Image Uploader Card
        JPanel imageSection = new JPanel(new BorderLayout(8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(248, 252, 249));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        imageSection.setOpaque(false);
        imageSection.setBorder(new EmptyBorder(6, 8, 6, 8));

        lblImagePreview = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Shape clip = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setClip(clip);
                super.paintComponent(g2);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        lblImagePreview.setPreferredSize(new Dimension(68, 46));
        lblImagePreview.setOpaque(true);
        lblImagePreview.setBackground(Color.WHITE);
        lblImagePreview.setHorizontalAlignment(SwingConstants.CENTER);
        imageSection.add(lblImagePreview, BorderLayout.WEST);

        JPanel imageBtnStack = new JPanel(new GridLayout(2, 1, 0, 4));
        imageBtnStack.setOpaque(false);

        btnChooseImage = createPillButton("📁 Upload Image...", new Color(240, 250, 244), AppTheme.BORDER_SAGE, AppTheme.FOREST_GREEN);
        btnChooseImage.setFont(AppTheme.font(Font.BOLD, 10));
        btnChooseImage.setPreferredSize(new Dimension(0, 22));
        btnChooseImage.addActionListener(e -> chooseProductImage());

        btnRemoveImage = createPillButton("✖ Remove", new Color(254, 242, 242), new Color(254, 202, 202), AppTheme.STATUS_DANGER);
        btnRemoveImage.setFont(AppTheme.font(Font.BOLD, 10));
        btnRemoveImage.setPreferredSize(new Dimension(0, 22));
        btnRemoveImage.setEnabled(false);
        btnRemoveImage.addActionListener(e -> {
            selectedImagePath = null;
            updateImagePreview();
        });

        imageBtnStack.add(btnChooseImage);
        imageBtnStack.add(btnRemoveImage);
        imageSection.add(imageBtnStack, BorderLayout.CENTER);

        formBody.add(imageSection);
        formBody.add(Box.createVerticalStrut(10));

        // Scroll pane without horizontal scrollbar and with slim vertical scrollbar
        JScrollPane formScroll = new JScrollPane(formBody);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        formScroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
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
                g2.setColor(isThumbRollover() ? AppTheme.FOREST_MID : thumbColor);
                g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 2, thumbBounds.width - 2, thumbBounds.height - 4, 6, 6);
                g2.dispose();
            }
        });
        formCard.add(formScroll, BorderLayout.CENTER);

        // ---------------------------------------------------------------------
        // Form Action CTAs Panel (South)
        // ---------------------------------------------------------------------
        JPanel formActionsContainer = new JPanel(new BorderLayout(0, 6));
        formActionsContainer.setOpaque(false);
        formActionsContainer.setBorder(new EmptyBorder(6, 0, 0, 0));

        // New Mode Button (Add)
        btnAdd = createPillButton("➕ Add Product to Catalog", AppTheme.FOREST_GREEN, AppTheme.FOREST_GREEN, Color.WHITE);
        btnAdd.setFont(AppTheme.font(Font.BOLD, 13));
        btnAdd.setPreferredSize(new Dimension(0, 36));
        btnAdd.addActionListener(e -> addProduct());

        // Edit Mode Buttons (Save / Delete / Cancel)
        editButtonsRow = new JPanel(new GridLayout(1, 3, 6, 0));
        editButtonsRow.setOpaque(false);

        btnUpdate = createPillButton("💾 Save", AppTheme.FOREST_GREEN, AppTheme.FOREST_GREEN, Color.WHITE);
        btnUpdate.setFont(AppTheme.font(Font.BOLD, 12));
        btnUpdate.setPreferredSize(new Dimension(0, 36));
        btnUpdate.addActionListener(e -> updateProduct());

        btnDelete = createPillButton("🗑️ Delete", new Color(254, 242, 242), new Color(254, 202, 202), AppTheme.STATUS_DANGER);
        btnDelete.setFont(AppTheme.font(Font.BOLD, 12));
        btnDelete.setPreferredSize(new Dimension(0, 36));
        btnDelete.addActionListener(e -> deleteProduct());

        btnCancelEdit = createPillButton("Cancel", new Color(240, 250, 244), AppTheme.BORDER_SAGE, AppTheme.TEXT_SECONDARY);
        btnCancelEdit.setFont(AppTheme.font(Font.BOLD, 12));
        btnCancelEdit.setPreferredSize(new Dimension(0, 36));
        btnCancelEdit.addActionListener(e -> clearForm());

        editButtonsRow.add(btnUpdate);
        editButtonsRow.add(btnDelete);
        editButtonsRow.add(btnCancelEdit);
        editButtonsRow.setVisible(false);

        formActionsContainer.add(btnAdd, BorderLayout.NORTH);
        formActionsContainer.add(editButtonsRow, BorderLayout.SOUTH);
        formCard.add(formActionsContainer, BorderLayout.SOUTH);

        centerSplit.add(formCard, BorderLayout.WEST);

        // -------------------------------------------------------------------------
        // RIGHT COLUMN: Elevated Catalog Table & Explorer Card
        // -------------------------------------------------------------------------
        JPanel tableCard = createElevatedCard(20);
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Table Filter & Search Toolbar (North)
        JPanel tableToolbar = new JPanel(new GridLayout(2, 1, 0, 6));
        tableToolbar.setOpaque(false);

        // Toolbar Line 1: Search (West) + Category Filter & Count (East)
        JPanel line1 = new JPanel(new BorderLayout(10, 0));
        line1.setOpaque(false);

        txtSearch = new ModernSearchField("Search product name, code, barcode...");
        txtSearch.setPreferredSize(new Dimension(280, 32));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        line1.add(txtSearch, BorderLayout.WEST);

        JPanel line1Right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        line1Right.setOpaque(false);

        cmbCategoryFilter = new JComboBox<>(new String[]{"All Categories"});
        cmbCategoryFilter.setFont(AppTheme.font(Font.PLAIN, 12));
        cmbCategoryFilter.setBackground(Color.WHITE);
        cmbCategoryFilter.setPreferredSize(new Dimension(145, 30));
        cmbCategoryFilter.addActionListener(e -> applyFilter());
        line1Right.add(cmbCategoryFilter);

        lblResultCount = new JLabel("0 products") {
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
        lblResultCount.setFont(AppTheme.font(Font.BOLD, 10));
        lblResultCount.setOpaque(false);
        lblResultCount.setBackground(new Color(240, 250, 244));
        lblResultCount.setForeground(AppTheme.FOREST_DEEP);
        lblResultCount.setBorder(new EmptyBorder(3, 10, 3, 10));
        line1Right.add(lblResultCount);

        line1.add(line1Right, BorderLayout.EAST);
        tableToolbar.add(line1);

        // Toolbar Line 2: Status Filter Chips
        JPanel line2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        line2.setOpaque(false);

        JLabel lblFilterTag = new JLabel("Filter Status:");
        lblFilterTag.setFont(AppTheme.font(Font.BOLD, 11));
        lblFilterTag.setForeground(AppTheme.TEXT_MUTED);
        line2.add(lblFilterTag);

        btnFilterAll = createFilterChip("All", "ALL");
        btnFilterInStock = createFilterChip("In Stock", "IN_STOCK");
        btnFilterLowStock = createFilterChip("Low Stock", "LOW_STOCK");
        btnFilterOutOfStock = createFilterChip("Out of Stock", "OUT_OF_STOCK");

        line2.add(btnFilterAll);
        line2.add(btnFilterInStock);
        line2.add(btnFilterLowStock);
        line2.add(btnFilterOutOfStock);
        tableToolbar.add(line2);

        tableCard.add(tableToolbar, BorderLayout.NORTH);

        // ---------------------------------------------------------------------
        // Catalog JTable
        // ---------------------------------------------------------------------
        String[] cols = {"#", "Product", "Barcode", "Category", "Buy (₹)", "Sell (₹)", "Stock", "Status", "Actions"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    if (row % 2 == 1) {
                        c.setBackground(new Color(251, 254, 252));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                } else {
                    c.setBackground(new Color(230, 248, 238));
                }
                return c;
            }
        };

        table.setOpaque(false);
        table.setRowHeight(42);
        table.setFont(AppTheme.font(Font.PLAIN, 12));
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(241, 246, 243));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(230, 248, 238));
        table.setSelectionForeground(AppTheme.TEXT_PRIMARY);

        // Header Styling
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setFont(AppTheme.font(Font.BOLD, 12));
                l.setForeground(AppTheme.TEXT_PRIMARY);
                l.setBackground(new Color(240, 250, 244));
                l.setOpaque(true);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_SAGE),
                        new EmptyBorder(0, 10, 0, 10)
                ));
                if (c == 0 || c == 6 || c == 7 || c == 8) {
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (c == 4 || c == 5) {
                    l.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return l;
            }
        });

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(35);  // #
        table.getColumnModel().getColumn(1).setPreferredWidth(230); // Product (Name + SKU)
        table.getColumnModel().getColumn(2).setPreferredWidth(125); // Barcode
        table.getColumnModel().getColumn(3).setPreferredWidth(110); // Category
        table.getColumnModel().getColumn(4).setPreferredWidth(75);  // Buy
        table.getColumnModel().getColumn(5).setPreferredWidth(80);  // Sell
        table.getColumnModel().getColumn(6).setPreferredWidth(60);  // Stock
        table.getColumnModel().getColumn(7).setPreferredWidth(105); // Status
        table.getColumnModel().getColumn(8).setPreferredWidth(80);  // Actions

        // Cell Renderers
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        centerRender.setFont(AppTheme.font(Font.BOLD, 11));
        centerRender.setForeground(AppTheme.TEXT_MUTED);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        // Column 1: Dual-line Product Name & SKU using clean HTML
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                String raw = String.valueOf(val != null ? val : "");
                String[] parts = raw.split("###");
                String name = parts[0];
                String sku = parts.length > 1 ? parts[1] : "";

                l.setText("<html><body style='font-family:Segoe UI;'><b style='color:#111827; font-size:11px;'>" 
                        + escapeHtml(name) + "</b><br><span style='color:#6b7280; font-size:9px;'>SKU: " 
                        + escapeHtml(sku) + "</span></body></html>");
                l.setBorder(new EmptyBorder(2, 10, 2, 6));
                return l;
            }
        });

        // Column 2: Barcode (Forest Mid, Monospaced)
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setFont(new Font("Monospaced", Font.BOLD, 12));
                l.setForeground(AppTheme.FOREST_MID);
                l.setBorder(new EmptyBorder(0, 10, 0, 6));
                return l;
            }
        });

        // Column 3: Category Pill
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setFont(AppTheme.font(Font.PLAIN, 12));
                l.setForeground(AppTheme.TEXT_SECONDARY);
                l.setBorder(new EmptyBorder(0, 8, 0, 8));
                return l;
            }
        });

        // Column 4 & 5: Buy & Sell Prices
        DefaultTableCellRenderer buyRender = new DefaultTableCellRenderer();
        buyRender.setHorizontalAlignment(SwingConstants.RIGHT);
        buyRender.setFont(AppTheme.font(Font.PLAIN, 12));
        buyRender.setForeground(AppTheme.TEXT_MUTED);
        buyRender.setBorder(new EmptyBorder(0, 6, 0, 10));
        table.getColumnModel().getColumn(4).setCellRenderer(buyRender);

        DefaultTableCellRenderer sellRender = new DefaultTableCellRenderer();
        sellRender.setHorizontalAlignment(SwingConstants.RIGHT);
        sellRender.setFont(AppTheme.font(Font.BOLD, 12));
        sellRender.setForeground(AppTheme.FOREST_GREEN);
        sellRender.setBorder(new EmptyBorder(0, 6, 0, 10));
        table.getColumnModel().getColumn(5).setCellRenderer(sellRender);

        // Column 6: Stock Quantity
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(AppTheme.font(Font.BOLD, 12));
                int qty = 0;
                try { qty = Integer.parseInt(String.valueOf(val).trim()); } catch (Exception ignored) {}
                if (qty <= 0) l.setForeground(AppTheme.STATUS_DANGER);
                else if (qty <= 5) l.setForeground(AppTheme.STATUS_WARNING);
                else l.setForeground(AppTheme.TEXT_PRIMARY);
                return l;
            }
        });

        // Column 7: Status Pill Badge (Direct paintComponent on label)
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setText(String.valueOf(val != null ? val : ""));
                return l;
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                String status = getText();
                int badgeW = Math.min(w - 14, 94);
                int badgeH = 22;
                int bx = (w - badgeW) / 2;
                int by = (h - badgeH) / 2;

                if ("OUT OF STOCK".equalsIgnoreCase(status)) {
                    g2.setColor(new Color(254, 242, 242));
                    g2.fillRoundRect(bx, by, badgeW, badgeH, badgeH, badgeH);
                    g2.setColor(new Color(254, 202, 202));
                    g2.drawRoundRect(bx, by, badgeW - 1, badgeH - 1, badgeH, badgeH);
                    g2.setColor(AppTheme.STATUS_DANGER);
                } else if ("LOW STOCK".equalsIgnoreCase(status)) {
                    g2.setColor(new Color(254, 243, 199));
                    g2.fillRoundRect(bx, by, badgeW, badgeH, badgeH, badgeH);
                    g2.setColor(new Color(253, 230, 138));
                    g2.drawRoundRect(bx, by, badgeW - 1, badgeH - 1, badgeH, badgeH);
                    g2.setColor(new Color(180, 83, 9));
                } else {
                    g2.setColor(new Color(236, 253, 245));
                    g2.fillRoundRect(bx, by, badgeW, badgeH, badgeH, badgeH);
                    g2.setColor(new Color(167, 243, 208));
                    g2.drawRoundRect(bx, by, badgeW - 1, badgeH - 1, badgeH, badgeH);
                    g2.setColor(AppTheme.STATUS_SUCCESS);
                }

                g2.setFont(AppTheme.font(Font.BOLD, 9));
                FontMetrics fm = g2.getFontMetrics();
                int tx = bx + (badgeW - fm.stringWidth(status)) / 2;
                int ty = by + ((badgeH - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(status, tx, ty);
                g2.dispose();
            }
        });

        // Column 8: Row Actions (Edit indicator)
        table.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(AppTheme.font(Font.BOLD, 11));
                l.setForeground(AppTheme.FOREST_GREEN);
                l.setText("✏️ Edit");
                return l;
            }
        });

        // Table Selection Listener
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                populateFormFromSelectedRow();
            }
        });

        // Table Viewport with Empty State
        JViewport customViewport = new JViewport() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (tableModel.getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth();
                    int h = getHeight();
                    int cy = Math.max(130, h / 3);

                    SidebarIcon boxIcon = new SidebarIcon("products", 50, new Color(195, 222, 210));
                    boxIcon.paintIcon(this, g2, (w - 50) / 2, cy - 65);

                    g2.setColor(AppTheme.TEXT_PRIMARY);
                    g2.setFont(AppTheme.font(Font.BOLD, 15));
                    FontMetrics fm1 = g2.getFontMetrics();
                    String t1 = "No Products Found";
                    g2.drawString(t1, (w - fm1.stringWidth(t1)) / 2, cy + 16);

                    g2.setColor(AppTheme.TEXT_MUTED);
                    g2.setFont(AppTheme.font(Font.PLAIN, 12));
                    FontMetrics fm2 = g2.getFontMetrics();
                    String t2 = "Try adjusting your search query, status filters, or add a new product";
                    g2.drawString(t2, (w - fm2.stringWidth(t2)) / 2, cy + 38);

                    g2.dispose();
                }
            }
        };
        customViewport.setOpaque(false);
        customViewport.setView(table);

        JScrollPane tableScroll = new JScrollPane();
        tableScroll.setViewport(customViewport);
        tableScroll.setColumnHeaderView(table.getTableHeader());
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.setOpaque(false);
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableScroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        tableScroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
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
                g2.setColor(isThumbRollover() ? AppTheme.FOREST_MID : thumbColor);
                g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 2, thumbBounds.width - 2, thumbBounds.height - 4, 6, 6);
                g2.dispose();
            }
        });

        tableCard.add(tableScroll, BorderLayout.CENTER);
        centerSplit.add(tableCard, BorderLayout.CENTER);

        add(centerSplit, BorderLayout.CENTER);

        // ---------------------------------------------------------------------
        // Dynamic Live Listeners
        // ---------------------------------------------------------------------
        txtBarcode.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateLiveBarcodePreview(); }
            public void removeUpdate(DocumentEvent e) { updateLiveBarcodePreview(); }
            public void changedUpdate(DocumentEvent e) { updateLiveBarcodePreview(); }
        });

        txtCode.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { syncCodeToBarcode(); updateLiveBarcodePreview(); }
            public void removeUpdate(DocumentEvent e) { syncCodeToBarcode(); updateLiveBarcodePreview(); }
            public void changedUpdate(DocumentEvent e) { syncCodeToBarcode(); updateLiveBarcodePreview(); }
        });

        DocumentListener marginListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateMarginDisplay(); }
            public void removeUpdate(DocumentEvent e) { updateMarginDisplay(); }
            public void changedUpdate(DocumentEvent e) { updateMarginDisplay(); }
        };
        txtPurchasePrice.getDocument().addDocumentListener(marginListener);
        txtSellingPrice.getDocument().addDocumentListener(marginListener);

        txtName.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { if (selectedImagePath == null) updateImagePreview(); }
            public void removeUpdate(DocumentEvent e) { if (selectedImagePath == null) updateImagePreview(); }
            public void changedUpdate(DocumentEvent e) { if (selectedImagePath == null) updateImagePreview(); }
        });

        cmbCategory.addActionListener(e -> {
            if (selectedImagePath == null) updateImagePreview();
        });

        updateFilterButtonsUI();
        updateImagePreview();
        updateLiveBarcodePreview();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // -------------------------------------------------------------------------
    // Helper Component Creators
    // -------------------------------------------------------------------------
    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppTheme.font(Font.BOLD, 11));
        l.setForeground(AppTheme.TEXT_SECONDARY);
        return l;
    }

    private JButton createSmallTextButton(String text, java.awt.event.ActionListener l) {
        JButton btn = new JButton(text);
        btn.setFont(AppTheme.font(Font.BOLD, 10));
        btn.setForeground(AppTheme.FOREST_GREEN);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(0, 2, 0, 2));
        btn.addActionListener(l);
        return btn;
    }

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
                        g2.setColor(new Color(Math.max(0, bg.getRed() - 10), Math.max(0, bg.getGreen() - 10), Math.max(0, bg.getBlue() - 10)));
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

    private JButton createFilterChip(String text, String filterKey) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = h;
                boolean active = currentStatusFilter.equals(filterKey);

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
        btn.setFont(AppTheme.font(Font.BOLD, 11));
        btn.setPreferredSize(new Dimension(84, 26));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            currentStatusFilter = filterKey;
            updateFilterButtonsUI();
            applyFilter();
        });
        return btn;
    }

    private void updateFilterButtonsUI() {
        if (btnFilterAll != null) btnFilterAll.repaint();
        if (btnFilterInStock != null) btnFilterInStock.repaint();
        if (btnFilterLowStock != null) btnFilterLowStock.repaint();
        if (btnFilterOutOfStock != null) btnFilterOutOfStock.repaint();
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
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setPreferredSize(new Dimension(160, 74));

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

    // -------------------------------------------------------------------------
    // Modern Input Field Component
    // -------------------------------------------------------------------------
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

            // Magnifying glass icon prefix
            g2.setColor(AppTheme.TEXT_MUTED);
            g2.setFont(AppTheme.font(Font.PLAIN, 12));
            g2.drawString("🔍", 8, h / 2 + 5);

            super.paintComponent(g);

            if (getText().isEmpty() && !focused && placeholder != null) {
                g2.setColor(AppTheme.TEXT_MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(placeholder, 30, ty);
            }
            g2.dispose();
        }
    }

    // -------------------------------------------------------------------------
    // Business Logic & Form Synchronization
    // -------------------------------------------------------------------------
    private void updateMarginDisplay() {
        try {
            String buyStr = txtPurchasePrice.getText().trim();
            String sellStr = txtSellingPrice.getText().trim();
            if (!buyStr.isEmpty() && !sellStr.isEmpty()) {
                double buy = Double.parseDouble(buyStr);
                double sell = Double.parseDouble(sellStr);
                if (buy > 0) {
                    double profit = sell - buy;
                    double marginPct = (profit / buy) * 100.0;
                    if (marginPct >= 0) {
                        lblMarginBadge.setText(String.format("Margin: +%.1f%% (Profit: ₹%.2f)", marginPct, profit));
                        lblMarginBadge.setBackground(new Color(236, 253, 245));
                        lblMarginBadge.setForeground(new Color(22, 101, 52));
                    } else {
                        lblMarginBadge.setText(String.format("Loss: %.1f%% (-₹%.2f)", marginPct, Math.abs(profit)));
                        lblMarginBadge.setBackground(new Color(254, 242, 242));
                        lblMarginBadge.setForeground(AppTheme.STATUS_DANGER);
                    }
                    lblMarginBadge.setVisible(true);
                    return;
                }
            }
        } catch (Exception ignored) {}
        if (lblMarginBadge != null) lblMarginBadge.setVisible(false);
    }

    private void syncCodeToBarcode() {
        if (selectedProductId <= 0 && txtBarcode.getText().trim().isEmpty()) {
            txtBarcode.setText(txtCode.getText().trim());
        }
    }

    private void generateRandomSku() {
        long ts = System.currentTimeMillis() % 100000;
        txtCode.setText("PRD-" + ts);
    }

    private void generateRandomBarcode() {
        long ts = System.currentTimeMillis() % 10000000000L;
        String val = String.format("890%09d", ts % 1000000000L);
        txtBarcode.setText(val);
        updateLiveBarcodePreview();
    }

    private void updateLiveBarcodePreview() {
        String val = txtBarcode != null ? txtBarcode.getText().trim() : "";
        if (val.isEmpty() && txtCode != null) {
            val = txtCode.getText().trim();
        }

        if (val.isEmpty()) {
            lblBarcodePreview.setIcon(null);
            lblBarcodePreview.setText("No Barcode");
            return;
        }

        try {
            BufferedImage img = BarcodeUtil.generateBarcodeImage(val, 280, 52, true);
            lblBarcodePreview.setText("");
            lblBarcodePreview.setIcon(new ImageIcon(img));
        } catch (Exception ex) {
            lblBarcodePreview.setIcon(null);
            lblBarcodePreview.setText("Invalid Barcode");
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
                return productDAO.getProductById(selectedProductId);
            } catch (Exception ignored) {}
        }
        return validateAndBuildProduct();
    }

    public void loadCategories() {
        try {
            cmbCategory.removeAllItems();
            if (cmbCategoryFilter != null) {
                cmbCategoryFilter.removeAllItems();
                cmbCategoryFilter.addItem("All Categories");
            }
            List<Category> categories = categoryDAO.getAllCategories();
            for (Category c : categories) {
                cmbCategory.addItem(c);
                if (cmbCategoryFilter != null) cmbCategoryFilter.addItem(c.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadProductTable() {
        try {
            loadedProducts = productDAO.getAllProducts();
            updateKpiStats();
            applyFilter();
        } catch (Exception e) {
            e.printStackTrace();
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Failed to load products: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateKpiStats() {
        int total = loadedProducts.size();
        int inStock = 0;
        int lowOrOut = 0;
        double totalValuation = 0.0;

        for (Product p : loadedProducts) {
            int q = p.getQuantity();
            int min = p.getMinStockLevel();
            if (q <= min) {
                lowOrOut++;
            } else {
                inStock++;
            }
            totalValuation += (p.getSellingPrice() * q);
        }

        lblKpiTotal.setText(String.valueOf(total));
        lblKpiInStock.setText(String.valueOf(inStock));
        lblKpiLowStock.setText(String.valueOf(lowOrOut));
        lblKpiValuation.setText(AppTheme.formatCurrency(totalValuation));
    }

    private void applyFilter() {
        tableModel.setRowCount(0);
        String search = (txtSearch != null && txtSearch.getText() != null) ? txtSearch.getText().trim().toLowerCase() : "";
        String catFilter = (cmbCategoryFilter != null && cmbCategoryFilter.getSelectedItem() != null) ? cmbCategoryFilter.getSelectedItem().toString() : "All Categories";

        int count = 0;
        int rowIdx = 1;

        for (Product p : loadedProducts) {
            // 1. Status Filter
            boolean isOut = p.isOutOfStock();
            boolean isLow = p.isLowStock() && !isOut;
            boolean isIn = !p.isLowStock() && !p.isOutOfStock();

            if ("IN_STOCK".equals(currentStatusFilter) && !isIn) continue;
            if ("LOW_STOCK".equals(currentStatusFilter) && !isLow && !isOut) continue;
            if ("OUT_OF_STOCK".equals(currentStatusFilter) && !isOut) continue;

            // 2. Category Filter
            String pCat = p.getCategoryName() != null ? p.getCategoryName() : "General";
            if (!"All Categories".equalsIgnoreCase(catFilter) && !catFilter.equalsIgnoreCase(pCat)) {
                continue;
            }

            // 3. Search Filter
            if (!search.isEmpty()) {
                String name = p.getName() != null ? p.getName().toLowerCase() : "";
                String code = p.getCode() != null ? p.getCode().toLowerCase() : "";
                String barcode = p.getBarcode() != null ? p.getBarcode().toLowerCase() : "";
                String catName = pCat.toLowerCase();
                if (!name.contains(search) && !code.contains(search) && !barcode.contains(search) && !catName.contains(search)) {
                    continue;
                }
            }

            String statusStr = isOut ? "OUT OF STOCK" : (isLow ? "LOW STOCK" : "IN STOCK");
            tableModel.addRow(new Object[]{
                    rowIdx++,
                    p.getName() + "###" + p.getCode(),
                    p.getBarcode(),
                    pCat,
                    String.format("₹%.2f", p.getPurchasePrice()),
                    String.format("₹%.2f", p.getSellingPrice()),
                    p.getQuantity(),
                    statusStr,
                    p.getId()
            });
            count++;
        }

        if (lblResultCount != null) {
            lblResultCount.setText(count + (count == 1 ? " product" : " products"));
        }

        if (table != null && table.getParent() != null) {
            table.getParent().repaint();
        }
    }

    private void populateFormFromSelectedRow() {
        int r = table.getSelectedRow();
        if (r < 0 || r >= tableModel.getRowCount()) return;

        Object idObj = tableModel.getValueAt(r, 8);
        selectedProductId = (idObj instanceof Integer) ? (Integer) idObj : Integer.parseInt(idObj.toString());

        Product found = null;
        for (Product p : loadedProducts) {
            if (p.getId() == selectedProductId) {
                found = p;
                break;
            }
        }

        if (found != null) {
            txtCode.setText(found.getCode());
            txtBarcode.setText(found.getBarcode());
            txtName.setText(found.getName());
            txtPurchasePrice.setText(String.format("%.2f", found.getPurchasePrice()));
            txtSellingPrice.setText(String.format("%.2f", found.getSellingPrice()));
            txtQty.setText(String.valueOf(found.getQuantity()));
            txtMinStock.setText(String.valueOf(found.getMinStockLevel()));
            selectedImagePath = found.getImagePath();

            // Set Category
            int catId = found.getCategoryId();
            for (int i = 0; i < cmbCategory.getItemCount(); i++) {
                Category cat = cmbCategory.getItemAt(i);
                if (cat.getId() == catId) {
                    cmbCategory.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Switch to Edit Mode UI
        lblFormTitle.setText("✏️ Edit Product");
        lblFormBadge.setText("ID #" + selectedProductId);
        lblFormBadge.setVisible(true);

        btnAdd.setVisible(false);
        editButtonsRow.setVisible(true);

        updateImagePreview();
        updateLiveBarcodePreview();
        updateMarginDisplay();
    }

    public void clearForm() {
        selectedProductId = -1;
        selectedImagePath = null;
        txtCode.setText("");
        txtBarcode.setText("");
        txtName.setText("");
        txtPurchasePrice.setText("");
        txtSellingPrice.setText("");
        txtQty.setText("10");
        txtMinStock.setText("5");

        lblFormTitle.setText("✨ Add New Product");
        lblFormBadge.setText("");
        lblFormBadge.setVisible(false);

        btnAdd.setVisible(true);
        editButtonsRow.setVisible(false);

        updateImagePreview();
        updateLiveBarcodePreview();
        updateMarginDisplay();

        if (table != null) table.clearSelection();
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
        String catName = cmbCategory != null && cmbCategory.getSelectedItem() != null ? cmbCategory.getSelectedItem().toString() : "";
        String prodName = txtName != null ? txtName.getText().trim() : "";
        ImageIcon icon = ProductImageUtil.getProductIcon(selectedImagePath, catName, prodName, 68, 44);
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
