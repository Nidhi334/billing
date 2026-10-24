package ui;

import dao.CategoryDAO;
import dao.ProductDAO;
import dao.PurchaseDAO;
import dao.SupplierDAO;
import model.Category;
import model.Product;
import model.Supplier;
import util.ProductImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Inward Stock Entry Modal Dialog.
 * Simplified UI:
 * - Single intelligent Product input box with floating autocomplete suggestions popup (JPopupMenu)
 * - Auto-detects existing products vs option to "+ Add as new product"
 * - Clean popup suggestions floating below the input without displacing layout or resizing dialog
 * - Minimal dynamic configuration for new products (Category, Selling Price, SKU Code)
 * - Keyra Design System: BG_CANVAS, BORDER_SAGE, FOREST_GREEN accents, zero emojis
 */
public class AddStockDialog extends JDialog {

    private final ProductDAO productDAO = new ProductDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final PurchaseDAO purchaseDAO = new PurchaseDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    private final Runnable onStockAddedCallback;
    private final Product preselectedProduct;

    // Data lists
    private List<Product> allProducts = new ArrayList<>();
    private List<Supplier> allSuppliers = new ArrayList<>();
    private List<Category> allCategories = new ArrayList<>();

    // State
    private Product selectedExistingProduct = null;
    private boolean isNewProduct = false;
    private boolean isUpdatingProgrammatically = false;
    private String newSelectedImagePath = null;

    // UI Components - Product Selection & Suggestions
    private JTextField txtProduct;
    private JPopupMenu suggestionsPopup;
    private JPanel pnlProductStatus;
    private JLabel lblProductStatus;
    private JButton btnClearProduct;

    // UI Components - New Product Minimal Configuration Card
    private JPanel pnlNewProductFields;
    private JComboBox<Category> cmbNewCategory;
    private JTextField txtNewSellPrice;
    private JTextField txtNewCode;
    private JTextField txtNewBarcode;
    private JButton btnChooseImage;

    // UI Components - Shared Inputs
    private JComboBox<Supplier> cmbSuppliers;
    private JButton btnQuickAddSupplier;
    private JTextField txtQty;
    private JTextField txtCostPrice;

    // Live Progression & Math Summary
    private JLabel lblLiveStockProgression;
    private JLabel lblLiveTotalCost;

    public AddStockDialog(Window owner, Product preselectedProduct, Runnable onStockAddedCallback) {
        super(owner, "Inward Stock Entry", ModalityType.APPLICATION_MODAL);
        this.preselectedProduct = preselectedProduct;
        this.onStockAddedCallback = onStockAddedCallback;

        setSize(520, 440);
        setMinimumSize(new Dimension(480, 420));
        setResizable(false);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppTheme.BG_CANVAS);

        initComponents();
        loadAllData();
        setupShortcuts();

        if (preselectedProduct != null) {
            SwingUtilities.invokeLater(() -> selectExistingProduct(preselectedProduct));
        }
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BG_CANVAS);

        // =========================================================================
        // 1. CENTER: CLEAN FORM BODY
        // =========================================================================
        JPanel centerContainer = new JPanel();
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));
        centerContainer.setBackground(AppTheme.BG_CANVAS);
        centerContainer.setBorder(new EmptyBorder(16, 22, 12, 22));

        // 1.1 SINGLE PRODUCT INPUT FIELD (With Floating Autocomplete Popup)
        JPanel pnlProductGroup = new JPanel();
        pnlProductGroup.setLayout(new BoxLayout(pnlProductGroup, BoxLayout.Y_AXIS));
        pnlProductGroup.setOpaque(false);
        pnlProductGroup.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblProduct = createFieldLabel("Product *");
        lblProduct.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlProductGroup.add(lblProduct);
        pnlProductGroup.add(Box.createVerticalStrut(4));

        txtProduct = createInputField("Type product name or SKU code to search...");
        txtProduct.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtProduct.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { SwingUtilities.invokeLater(() -> onProductInputChanged()); }
            public void removeUpdate(DocumentEvent e) { SwingUtilities.invokeLater(() -> onProductInputChanged()); }
            public void changedUpdate(DocumentEvent e) { SwingUtilities.invokeLater(() -> onProductInputChanged()); }
        });
        txtProduct.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (suggestionsPopup != null && suggestionsPopup.isVisible()) {
                        suggestionsPopup.setVisible(false);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (suggestionsPopup != null && suggestionsPopup.isVisible() && suggestionsPopup.getComponentCount() > 0) {
                        Component first = suggestionsPopup.getComponent(0);
                        if (first instanceof JMenuItem) {
                            ((JMenuItem) first).doClick();
                            e.consume();
                        }
                    } else if (selectedExistingProduct != null || isNewProduct) {
                        txtQty.requestFocusInWindow();
                    }
                }
            }
        });
        pnlProductGroup.add(txtProduct);
        pnlProductGroup.add(Box.createVerticalStrut(4));

        // Inline status badge shown when product is selected or new product mode is activated
        pnlProductStatus = new JPanel(new BorderLayout(8, 0));
        pnlProductStatus.setOpaque(false);
        pnlProductStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlProductStatus.setVisible(false);

        lblProductStatus = new JLabel("");
        lblProductStatus.setFont(AppTheme.font(Font.PLAIN, 11));
        pnlProductStatus.add(lblProductStatus, BorderLayout.CENTER);

        btnClearProduct = new JButton("Change") {
            {
                setFont(AppTheme.font(Font.BOLD, 10));
                setForeground(AppTheme.FOREST_MID);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setBorder(new EmptyBorder(1, 6, 1, 6));
                addActionListener(e -> clearProductSelection());
            }
        };
        pnlProductStatus.add(btnClearProduct, BorderLayout.EAST);
        pnlProductGroup.add(pnlProductStatus);

        centerContainer.add(pnlProductGroup);
        centerContainer.add(Box.createVerticalStrut(10));

        // 1.2 MINIMAL NEW PRODUCT CONFIGURATION CARD (Only visible when adding as new product)
        pnlNewProductFields = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.dispose();
            }
        };
        pnlNewProductFields.setOpaque(false);
        pnlNewProductFields.setBorder(new EmptyBorder(10, 14, 10, 14));
        pnlNewProductFields.setLayout(new BoxLayout(pnlNewProductFields, BoxLayout.Y_AXIS));
        pnlNewProductFields.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlNewProductFields.setVisible(false);

        // Header inside card
        JPanel pnlCardHeader = new JPanel(new BorderLayout());
        pnlCardHeader.setOpaque(false);
        JLabel lblCardTitle = new JLabel("New Catalog Item Details");
        lblCardTitle.setFont(AppTheme.font(Font.BOLD, 11));
        lblCardTitle.setForeground(AppTheme.FOREST_DEEP);
        pnlCardHeader.add(lblCardTitle, BorderLayout.WEST);

        btnChooseImage = new JButton("+ Image (Optional)") {
            {
                setFont(AppTheme.font(Font.BOLD, 10));
                setForeground(AppTheme.FOREST_MID);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                addActionListener(e -> chooseNewProductImage());
            }
        };
        pnlCardHeader.add(btnChooseImage, BorderLayout.EAST);
        pnlNewProductFields.add(pnlCardHeader);
        pnlNewProductFields.add(Box.createVerticalStrut(8));

        // Row 1: Category & Selling Price
        JPanel newRow1 = new JPanel(new GridLayout(1, 2, 12, 0));
        newRow1.setOpaque(false);
        newRow1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

        JPanel colCat = new JPanel();
        colCat.setLayout(new BoxLayout(colCat, BoxLayout.Y_AXIS));
        colCat.setOpaque(false);
        colCat.add(createFieldLabel("Category *"));
        colCat.add(Box.createVerticalStrut(4));
        cmbNewCategory = new JComboBox<>();
        styleComboBox(cmbNewCategory);
        colCat.add(cmbNewCategory);

        JPanel colSell = new JPanel();
        colSell.setLayout(new BoxLayout(colSell, BoxLayout.Y_AXIS));
        colSell.setOpaque(false);
        colSell.add(createFieldLabel("Retail Selling Price (₹) *"));
        colSell.add(Box.createVerticalStrut(4));
        txtNewSellPrice = createInputField("e.g. 1499.00");
        colSell.add(txtNewSellPrice);

        newRow1.add(colCat);
        newRow1.add(colSell);
        pnlNewProductFields.add(newRow1);
        pnlNewProductFields.add(Box.createVerticalStrut(8));

        // Row 2: SKU Code & Barcode
        JPanel newRow2 = new JPanel(new GridLayout(1, 2, 12, 0));
        newRow2.setOpaque(false);
        newRow2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

        JPanel colCode = new JPanel();
        colCode.setLayout(new BoxLayout(colCode, BoxLayout.Y_AXIS));
        colCode.setOpaque(false);
        colCode.add(createFieldLabel("SKU Code *"));
        colCode.add(Box.createVerticalStrut(4));
        txtNewCode = createInputField("e.g. PRD-102");
        colCode.add(txtNewCode);

        JPanel colBarcode = new JPanel();
        colBarcode.setLayout(new BoxLayout(colBarcode, BoxLayout.Y_AXIS));
        colBarcode.setOpaque(false);
        colBarcode.add(createFieldLabel("Barcode (Optional)"));
        colBarcode.add(Box.createVerticalStrut(4));
        txtNewBarcode = createInputField("e.g. 890123456789");
        colBarcode.add(txtNewBarcode);

        newRow2.add(colCode);
        newRow2.add(colBarcode);
        pnlNewProductFields.add(newRow2);

        centerContainer.add(pnlNewProductFields);
        centerContainer.add(Box.createVerticalStrut(10));

        // 1.3 SHARED INPUTS: Supplier & Qty / Cost Price
        // Supplier / Vendor Row
        JPanel suppHeaderRow = new JPanel(new BorderLayout());
        suppHeaderRow.setOpaque(false);
        suppHeaderRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSupp = createFieldLabel("Supplier / Vendor (Optional)");
        suppHeaderRow.add(lblSupp, BorderLayout.WEST);

        btnQuickAddSupplier = new JButton("+ New Vendor") {
            {
                setFont(AppTheme.font(Font.BOLD, 11));
                setForeground(AppTheme.FOREST_GREEN);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setBorder(new EmptyBorder(0, 4, 0, 0));
                addActionListener(e -> handleQuickAddSupplier());
            }
        };
        suppHeaderRow.add(btnQuickAddSupplier, BorderLayout.EAST);
        centerContainer.add(suppHeaderRow);
        centerContainer.add(Box.createVerticalStrut(4));

        cmbSuppliers = new JComboBox<>();
        styleComboBox(cmbSuppliers);
        cmbSuppliers.setAlignmentX(Component.LEFT_ALIGNMENT);
        cmbSuppliers.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setBorder(new EmptyBorder(6, 10, 6, 10));
                l.setFont(AppTheme.font(Font.PLAIN, 12));
                if (value instanceof Supplier) {
                    Supplier s = (Supplier) value;
                    String comp = (s.getCompanyName() != null && !s.getCompanyName().isEmpty()) ? " (" + s.getCompanyName() + ")" : "";
                    l.setText(s.getName() + comp);
                } else {
                    l.setText("— Direct Purchase / No Vendor —");
                }
                return l;
            }
        });
        centerContainer.add(cmbSuppliers);
        centerContainer.add(Box.createVerticalStrut(10));

        // Dual Inputs: Inward Qty & Unit Cost Price
        JPanel dualRow = new JPanel(new GridLayout(1, 2, 12, 0));
        dualRow.setOpaque(false);
        dualRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        dualRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        JPanel colQty = new JPanel();
        colQty.setLayout(new BoxLayout(colQty, BoxLayout.Y_AXIS));
        colQty.setOpaque(false);
        colQty.add(createFieldLabel("Inward Qty (Units) *"));
        colQty.add(Box.createVerticalStrut(4));
        txtQty = createInputField("e.g. 20");
        txtQty.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateLiveMath(); }
            public void removeUpdate(DocumentEvent e) { updateLiveMath(); }
            public void changedUpdate(DocumentEvent e) { updateLiveMath(); }
        });
        colQty.add(txtQty);

        JPanel colCost = new JPanel();
        colCost.setLayout(new BoxLayout(colCost, BoxLayout.Y_AXIS));
        colCost.setOpaque(false);
        colCost.add(createFieldLabel("Unit Cost Price (₹) *"));
        colCost.add(Box.createVerticalStrut(4));
        txtCostPrice = createInputField("e.g. 850.00");
        txtCostPrice.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateLiveMath(); }
            public void removeUpdate(DocumentEvent e) { updateLiveMath(); }
            public void changedUpdate(DocumentEvent e) { updateLiveMath(); }
        });
        colCost.add(txtCostPrice);

        dualRow.add(colQty);
        dualRow.add(colCost);
        centerContainer.add(dualRow);
        centerContainer.add(Box.createVerticalStrut(8));

        // Live Stock Progression Banner
        lblLiveStockProgression = new JLabel("Stock: Select product and enter quantity");
        lblLiveStockProgression.setFont(AppTheme.font(Font.PLAIN, 11));
        lblLiveStockProgression.setForeground(AppTheme.TEXT_SECONDARY);
        lblLiveStockProgression.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerContainer.add(lblLiveStockProgression);

        root.add(centerContainer, BorderLayout.CENTER);

        // =========================================================================
        // 2. FOOTER: Total Highlight & Action Buttons
        // =========================================================================
        JPanel footer = new JPanel(new BorderLayout(14, 0));
        footer.setBackground(AppTheme.BG_CANVAS);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.BORDER_SAGE),
                new EmptyBorder(10, 22, 14, 22)
        ));

        // Total Inward Investment Highlight
        JPanel totalBox = new JPanel();
        totalBox.setLayout(new BoxLayout(totalBox, BoxLayout.Y_AXIS));
        totalBox.setOpaque(false);

        JLabel lblTotalCap = new JLabel("Total Inward Amount");
        lblTotalCap.setFont(AppTheme.font(Font.BOLD, 10));
        lblTotalCap.setForeground(AppTheme.TEXT_MUTED);

        lblLiveTotalCost = new JLabel("₹0.00");
        lblLiveTotalCost.setFont(AppTheme.font(Font.BOLD, 17));
        lblLiveTotalCost.setForeground(AppTheme.FOREST_DEEP);

        totalBox.add(lblTotalCap);
        totalBox.add(Box.createVerticalStrut(1));
        totalBox.add(lblLiveTotalCost);
        footer.add(totalBox, BorderLayout.WEST);

        // Cancel & Record Inward Stock CTA
        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnBox.setOpaque(false);

        JButton btnCancel = createPillButton("Cancel", Color.WHITE, AppTheme.BORDER_SAGE, AppTheme.TEXT_SECONDARY);
        btnCancel.setPreferredSize(new Dimension(85, 36));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSubmit = createPillButton("Record Inward Stock", AppTheme.FOREST_GREEN, null, Color.WHITE);
        btnSubmit.setIcon(new SidebarIcon("check", 13, Color.WHITE));
        btnSubmit.setPreferredSize(new Dimension(180, 36));
        btnSubmit.addActionListener(e -> handleSaveAndInward());

        btnBox.add(btnCancel);
        btnBox.add(btnSubmit);
        footer.add(btnBox, BorderLayout.EAST);

        root.add(footer, BorderLayout.SOUTH);
        add(root);
    }

    // =========================================================================
    // PRODUCT AUTOCOMPLETE & FLOATING SUGGESTIONS POPUP (JPopupMenu)
    // =========================================================================
    private void onProductInputChanged() {
        if (isUpdatingProgrammatically) return;

        String q = txtProduct.getText().trim();
        if (q.isEmpty()) {
            if (suggestionsPopup != null && suggestionsPopup.isVisible()) {
                suggestionsPopup.setVisible(false);
            }
            selectedExistingProduct = null;
            pnlProductStatus.setVisible(false);
            pnlNewProductFields.setVisible(false);
            setSize(520, 440);
            updateLiveMath();
            return;
        }

        // If an existing product is already selected and user modifies text, reset selection
        if (selectedExistingProduct != null && !q.equalsIgnoreCase(selectedExistingProduct.getName())) {
            selectedExistingProduct = null;
            pnlProductStatus.setVisible(false);
        }

        // Filter matching products
        List<Product> matches = new ArrayList<>();
        String qLower = q.toLowerCase();
        for (Product p : allProducts) {
            if (p.getName().toLowerCase().contains(qLower)
                    || p.getCode().toLowerCase().contains(qLower)
                    || (p.getBarcode() != null && p.getBarcode().toLowerCase().contains(qLower))) {
                matches.add(p);
                if (matches.size() >= 6) break; // Limit to top 6
            }
        }

        showSuggestionsPopup(q, matches);
    }

    private void showSuggestionsPopup(String query, List<Product> matches) {
        if (suggestionsPopup == null) {
            suggestionsPopup = new JPopupMenu();
            suggestionsPopup.setFocusable(false);
            suggestionsPopup.setBackground(Color.WHITE);
            suggestionsPopup.setBorder(BorderFactory.createCompoundBorder(
                    new javax.swing.border.LineBorder(AppTheme.BORDER_SAGE, 1, true),
                    new EmptyBorder(2, 2, 2, 2)
            ));
        }
        suggestionsPopup.removeAll();

        int popWidth = Math.max(txtProduct.getWidth(), 450);

        if (!matches.isEmpty()) {
            for (Product p : matches) {
                suggestionsPopup.add(createProductSuggestionItem(p, popWidth));
            }
            JSeparator sep = new JSeparator();
            sep.setForeground(new Color(230, 240, 235));
            suggestionsPopup.add(sep);
            suggestionsPopup.add(createAddNewSuggestionItem(query, popWidth));
        } else {
            // No matches found: show option to add as new product
            suggestionsPopup.add(createAddNewSuggestionItem(query, popWidth));
        }

        if (txtProduct.isShowing()) {
            suggestionsPopup.pack();
            suggestionsPopup.show(txtProduct, 0, txtProduct.getHeight() + 4);
            txtProduct.requestFocusInWindow();
        }
    }

    private JMenuItem createProductSuggestionItem(Product p, int popWidth) {
        JMenuItem item = new JMenuItem();
        item.setPreferredSize(new Dimension(popWidth, 42));
        item.setBackground(Color.WHITE);
        item.setLayout(new BorderLayout(10, 0));
        item.setBorder(new EmptyBorder(4, 10, 4, 10));

        // Thumbnail
        JLabel lblIcon = new JLabel(ProductImageUtil.getProductIcon(p, 28, 28));
        item.add(lblIcon, BorderLayout.WEST);

        // Name + Code
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        JLabel lblName = new JLabel(p.getName());
        lblName.setFont(AppTheme.font(Font.BOLD, 12));
        lblName.setForeground(AppTheme.TEXT_PRIMARY);

        String cat = p.getCategoryName() != null ? p.getCategoryName() : "General";
        JLabel lblMeta = new JLabel(p.getCode() + " • " + cat);
        lblMeta.setFont(AppTheme.font(Font.PLAIN, 10));
        lblMeta.setForeground(AppTheme.TEXT_MUTED);

        center.add(lblName);
        center.add(Box.createVerticalStrut(1));
        center.add(lblMeta);
        item.add(center, BorderLayout.CENTER);

        // Stock Pill
        int q = p.getQuantity();
        JLabel lblStock = new JLabel(q <= 0 ? "Out of Stock" : "Stock: " + q);
        lblStock.setFont(AppTheme.font(Font.BOLD, 11));
        if (q <= 0) {
            lblStock.setForeground(new Color(220, 38, 38));
        } else if (q <= p.getMinStockLevel()) {
            lblStock.setForeground(new Color(202, 138, 4));
        } else {
            lblStock.setForeground(AppTheme.FOREST_GREEN);
        }
        item.add(lblStock, BorderLayout.EAST);

        item.addActionListener(e -> selectExistingProduct(p));
        return item;
    }

    private JMenuItem createAddNewSuggestionItem(String query, int popWidth) {
        JMenuItem item = new JMenuItem();
        item.setPreferredSize(new Dimension(popWidth, 38));
        item.setBackground(new Color(245, 252, 248));
        item.setLayout(new BorderLayout(8, 0));
        item.setBorder(new EmptyBorder(4, 12, 4, 12));

        JLabel lblText = new JLabel("+ Add \"" + query + "\" as new product");
        lblText.setFont(AppTheme.font(Font.BOLD, 12));
        lblText.setForeground(AppTheme.FOREST_GREEN);
        lblText.setIcon(new SidebarIcon("plus", 13, AppTheme.FOREST_GREEN));
        item.add(lblText, BorderLayout.WEST);

        JLabel lblHint = new JLabel("Configure new item →");
        lblHint.setFont(AppTheme.font(Font.PLAIN, 10));
        lblHint.setForeground(AppTheme.TEXT_MUTED);
        item.add(lblHint, BorderLayout.EAST);

        item.addActionListener(e -> startNewProductMode(query));
        return item;
    }

    private void selectExistingProduct(Product p) {
        this.selectedExistingProduct = p;
        this.isNewProduct = false;

        isUpdatingProgrammatically = true;
        txtProduct.setText(p.getName());
        isUpdatingProgrammatically = false;

        if (suggestionsPopup != null && suggestionsPopup.isVisible()) {
            suggestionsPopup.setVisible(false);
        }

        // Show product status badge
        int q = p.getQuantity();
        String stockStatus = q <= 0 ? "Out of Stock" : (q <= p.getMinStockLevel() ? "Low Stock (" + q + ")" : "In Stock (" + q + " units)");
        String cat = (p.getCategoryName() != null && !p.getCategoryName().isEmpty()) ? p.getCategoryName() : "General";
        lblProductStatus.setText("<html><span style='color:#005a3d; font-weight:bold;'>✓ " + p.getCode() + "</span> &nbsp;•&nbsp; Cat: " + cat + " &nbsp;•&nbsp; <b>" + stockStatus + "</b></html>");
        pnlProductStatus.setVisible(true);

        // Hide new product fields and restore standard compact height
        pnlNewProductFields.setVisible(false);
        setSize(520, 440);

        if (p.getPurchasePrice() > 0) {
            txtCostPrice.setText(String.format("%.2f", p.getPurchasePrice()));
        }

        updateLiveMath();
        txtQty.requestFocusInWindow();
    }

    private void startNewProductMode(String query) {
        this.selectedExistingProduct = null;
        this.isNewProduct = true;

        isUpdatingProgrammatically = true;
        txtProduct.setText(query);
        isUpdatingProgrammatically = false;

        if (suggestionsPopup != null && suggestionsPopup.isVisible()) {
            suggestionsPopup.setVisible(false);
        }

        lblProductStatus.setText("<html><span style='color:#005a3d; font-weight:bold;'>+ New Product Mode</span> &nbsp;•&nbsp; Enter category & pricing below</html>");
        pnlProductStatus.setVisible(true);

        if (txtNewCode.getText().trim().isEmpty()) {
            generateNewSkuCode();
        }

        pnlNewProductFields.setVisible(true);
        setSize(520, 560);

        updateLiveMath();
        txtNewSellPrice.requestFocusInWindow();
    }

    private void clearProductSelection() {
        this.selectedExistingProduct = null;
        this.isNewProduct = false;

        isUpdatingProgrammatically = true;
        txtProduct.setText("");
        isUpdatingProgrammatically = false;

        pnlProductStatus.setVisible(false);
        pnlNewProductFields.setVisible(false);
        setSize(520, 440);

        updateLiveMath();
        txtProduct.requestFocusInWindow();
    }

    private void generateNewSkuCode() {
        long ts = System.currentTimeMillis() % 100000;
        txtNewCode.setText("PRD-" + ts);
    }

    private void chooseNewProductImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Product Image");
        chooser.setFileFilter(new FileNameExtensionFilter("Image Files (*.jpg, *.png, *.webp, *.jpeg)", "jpg", "jpeg", "png", "webp"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String code = txtNewCode.getText().trim();
            if (code.isEmpty()) {
                code = "PRD_" + System.currentTimeMillis();
            }
            String savedPath = ProductImageUtil.saveProductImage(file, code);
            if (savedPath != null) {
                newSelectedImagePath = savedPath;
                if (btnChooseImage != null) btnChooseImage.setText("Image Added ✓");
            }
        }
    }

    private void handleQuickAddSupplier() {
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        JTextField txtName = new JTextField();
        JTextField txtCompany = new JTextField();
        JTextField txtPhone = new JTextField();

        form.add(new JLabel("Contact Name *:"));
        form.add(txtName);
        form.add(new JLabel("Company Name:"));
        form.add(txtCompany);
        form.add(new JLabel("Phone Number:"));
        form.add(txtPhone);

        int opt = JOptionPane.showConfirmDialog(this, form, "Quick Add Supplier / Vendor", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt == JOptionPane.OK_OPTION) {
            String name = txtName.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vendor name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            try {
                Supplier s = new Supplier(0, name, txtCompany.getText().trim(), txtPhone.getText().trim(), "", "");
                if (supplierDAO.addSupplier(s)) {
                    allSuppliers.add(s);
                    cmbSuppliers.addItem(s);
                    cmbSuppliers.setSelectedItem(s);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not add supplier: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // =========================================================================
    // DATA LOADING & LIVE CALCULATION
    // =========================================================================
    private void loadAllData() {
        try {
            // 1. Categories
            cmbNewCategory.removeAllItems();
            allCategories = categoryDAO.getAllCategories();
            if (allCategories != null) {
                for (Category c : allCategories) {
                    cmbNewCategory.addItem(c);
                }
            }

            // 2. Suppliers
            cmbSuppliers.removeAllItems();
            cmbSuppliers.addItem(null); // Direct purchase
            allSuppliers = supplierDAO.getAllSuppliers();
            if (allSuppliers != null) {
                for (Supplier s : allSuppliers) {
                    cmbSuppliers.addItem(s);
                }
            }

            // 3. Products
            allProducts = productDAO.getAllProducts();

        } catch (Exception ex) {
            System.err.println("Notice: Could not load data in AddStockDialog: " + ex.getMessage());
        }
    }

    private void updateLiveMath() {
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

        double total = addQty * cost;
        lblLiveTotalCost.setText(String.format("₹%,.2f", total));

        if (!isNewProduct && selectedExistingProduct != null) {
            int curr = selectedExistingProduct.getQuantity();
            if (addQty > 0) {
                int projected = curr + addQty;
                lblLiveStockProgression.setText("<html>Current Stock: <b>" + curr + "</b> &nbsp;→&nbsp; <span style='color:#005a3d;'>+ " + addQty + " units</span> &nbsp;→&nbsp; Projected: <b style='color:#005a3d;'>" + projected + " units</b></html>");
            } else {
                lblLiveStockProgression.setText("Current Stock: " + curr + " units");
            }
        } else if (isNewProduct) {
            String name = txtProduct.getText().trim();
            String label = name.isEmpty() ? "New Product" : name;
            if (addQty > 0) {
                lblLiveStockProgression.setText("<html>Creating <b>" + label + "</b> &nbsp;→&nbsp; Initial Stock: <b style='color:#005a3d;'>" + addQty + " units</b></html>");
            } else {
                lblLiveStockProgression.setText("Initial Stock: enter inward quantity above");
            }
        } else {
            lblLiveStockProgression.setText("Stock: Select a product to preview stock progression");
        }
    }

    // =========================================================================
    // SAVING & INWARD TRANSACTION
    // =========================================================================
    private void handleSaveAndInward() {
        String qStr = txtQty.getText().trim();
        String cStr = txtCostPrice.getText().trim();

        if (qStr.isEmpty() || cStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter valid inward quantity and unit cost price.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int qty;
        double cost;
        try {
            qty = Integer.parseInt(qStr);
            cost = Double.parseDouble(cStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity must be a positive integer and cost price must be a valid number.", "Validation", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (qty <= 0) {
            JOptionPane.showMessageDialog(this, "Inward quantity must be greater than zero.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cost < 0) {
            JOptionPane.showMessageDialog(this, "Cost price cannot be negative.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Supplier supp = (Supplier) cmbSuppliers.getSelectedItem();
        int supplierId = supp != null ? supp.getId() : 0;

        try {
            int targetProductId = 0;

            if (isNewProduct) {
                String name = txtProduct.getText().trim();
                String code = txtNewCode.getText().trim();
                String sellStr = txtNewSellPrice.getText().trim();

                if (name.isEmpty() || code.isEmpty() || sellStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter product name, SKU code, and retail selling price.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                double sellPrice;
                try {
                    sellPrice = Double.parseDouble(sellStr);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Selling price must be a valid number.", "Validation", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Category cat = (Category) cmbNewCategory.getSelectedItem();
                int catId = cat != null ? cat.getId() : 0;

                String barcode = txtNewBarcode != null ? txtNewBarcode.getText().trim() : "";
                if (barcode.isEmpty()) barcode = code;

                // Create product with initial qty = 0 (the purchase inward transaction will raise the stock atomically)
                Product newProd = new Product(0, code, barcode, name, catId, cost, sellPrice, 0, 5, newSelectedImagePath);
                boolean created = productDAO.addProduct(newProd);
                if (!created) {
                    JOptionPane.showMessageDialog(this, "Failed to create new product in database.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                targetProductId = newProd.getId();

            } else {
                if (selectedExistingProduct == null) {
                    // Try to auto-match if user typed exact code or name
                    String q = txtProduct.getText().trim().toLowerCase();
                    for (Product p : allProducts) {
                        if (p.getName().equalsIgnoreCase(q) || p.getCode().equalsIgnoreCase(q)) {
                            selectedExistingProduct = p;
                            break;
                        }
                    }
                }

                if (selectedExistingProduct == null) {
                    JOptionPane.showMessageDialog(this, "Please select an existing product from suggestions or click '+ Add as new product'.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                targetProductId = selectedExistingProduct.getId();
            }

            // Perform atomic Inward Purchase Transaction
            String invoiceNo = purchaseDAO.generateNextPurchaseInvoiceNo();
            boolean ok = purchaseDAO.recordPurchase(invoiceNo, supplierId, targetProductId, qty, cost);
            if (ok) {
                if (onStockAddedCallback != null) {
                    onStockAddedCallback.run();
                }
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to record inward purchase in database.", "Database Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupShortcuts() {
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        txtQty.addActionListener(e -> {
            if (txtCostPrice.getText().trim().isEmpty()) {
                txtCostPrice.requestFocusInWindow();
            } else {
                handleSaveAndInward();
            }
        });
        txtCostPrice.addActionListener(e -> handleSaveAndInward());
    }

    // =========================================================================
    // UI BUILDER HELPERS
    // =========================================================================
    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppTheme.font(Font.BOLD, 11));
        l.setForeground(AppTheme.TEXT_PRIMARY);
        return l;
    }

    private JTextField createInputField(String placeholder) {
        JTextField tf = new JTextField() {
            private boolean focused = false;
            {
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

                g2.setColor(Color.WHITE);
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
                    g2.drawString(placeholder, 12, ty);
                }
                g2.dispose();
            }
        };
        tf.setFont(AppTheme.font(Font.PLAIN, 12));
        tf.setForeground(AppTheme.TEXT_PRIMARY);
        tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(6, 12, 6, 12));
        tf.setPreferredSize(new Dimension(0, 36));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return tf;
    }

    private void styleComboBox(JComboBox<?> box) {
        box.setFont(AppTheme.font(Font.PLAIN, 12));
        box.setBackground(Color.WHITE);
        box.setForeground(AppTheme.TEXT_PRIMARY);
        box.setPreferredSize(new Dimension(0, 36));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        box.setFocusable(false);
        box.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(AppTheme.BORDER_SAGE, 1, true),
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
                        int cx = w / 2, cy = h / 2;
                        g2.setColor(AppTheme.FOREST_MID);
                        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        Path2D p = new Path2D.Float();
                        p.moveTo(cx - 4, cy - 2);
                        p.lineTo(cx, cy + 2);
                        p.lineTo(cx + 4, cy - 2);
                        g2.draw(p);
                        g2.dispose();
                    }
                };
                btn.setBorder(BorderFactory.createEmptyBorder());
                btn.setContentAreaFilled(false);
                btn.setFocusPainted(false);
                btn.setOpaque(false);
                btn.setPreferredSize(new Dimension(28, 0));
                return btn;
            }
        });
    }

    private JButton createPillButton(String text, Color bg, Color border, Color fg) {
        JButton btn = new JButton(text) {
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

                if (hovered) {
                    if (bg.equals(AppTheme.FOREST_GREEN)) {
                        g2.setColor(AppTheme.FOREST_MID);
                    } else {
                        g2.setColor(new Color(Math.max(0, bg.getRed() - 6), Math.max(0, bg.getGreen() - 6), Math.max(0, bg.getBlue() - 6)));
                    }
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, w, h, h, h);

                if (border != null) {
                    g2.setColor(border);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
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
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setIconTextGap(6);
        return btn;
    }
}
