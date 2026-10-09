package ui;

import dao.BillingDAO;
import dao.CustomerDAO;
import dao.ProductDAO;
import model.Customer;
import model.HeldBill;
import model.Product;
import model.Sale;
import model.SaleItem;
import config.AppSettings;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

public class BillingPanel extends JPanel {
    private ProductDAO productDAO = new ProductDAO();
    private CustomerDAO customerDAO = new CustomerDAO();
    private BillingDAO billingDAO = new BillingDAO();
    private User currentUser;

    // Fast Product Cache for instant barcode & search lookup
    private List<Product> cachedProducts = new ArrayList<>();

    // Multi-bill (Held Bills) Storage
    private List<HeldBill> heldBills = new ArrayList<>();

    // Form fields
    private JTextField txtBarcode;
    private Timer barcodeScanTimer;
    private JComboBox<Customer> cmbCustomer;
    private JTextField txtCustPhone;
    private JTextField txtInvoiceNo;
    private JComboBox<String> cmbPaymentMode;
    private boolean isUpdatingCustomerCombo = false;

    // Cart table
    private DefaultTableModel cartModel;
    private JTable cartTable;
    private List<SaleItem> cartItems = new ArrayList<>();

    // Calculations
    private double subtotal = 0.0;
    private double gstRate = 18.0;
    private double gstAmount = 0.0;
    private double discountValue = 0.0;
    private String discountType = "FLAT"; // FLAT or PERCENT
    private double grandTotal = 0.0;
    private double cashTendered = 0.0;

    // Summary labels
    private JLabel lblSubtotal;
    private JLabel lblGstAmount;
    private JLabel lblDiscount;
    private JLabel lblGrandTotal;
    private JLabel lblChangeDue;
    private JLabel lblItemsCount;
    private JLabel lblTotalQty;
    private JTextField txtCashPaid;

    // Held bill tab selector
    private JComboBox<String> cmbHeldBills;
    private JPanel heldBillsPanel;

    // Configurable UI Containers & Buttons
    private JPanel scanBar;
    private JPanel cartActionsPanel;
    private JPanel discountBar;
    private JButton btnAddDiscount;
    private JButton btnRemoveDiscount;
    private JButton btnOpenUpi;
    private JPanel quickCashPanel;
    private JPanel numpadPanel;

    private JButton btnOpenScanner;
    private JButton btnScanAdd;
    private JButton btnQuickCheckout;

    // Touch vs Non-Touch UI Mode indicator
    private JLabel lblTouchBadge;

    public BillingPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout(10, 10));
        setBackground(AppTheme.BG_CANVAS);
        setBorder(new EmptyBorder(10, 12, 10, 12));

        initComponents();
        setupGlobalKeyShortcuts();
        loadCustomers();
        loadProductCache();
        applySettingsVisibility();
        resetBillingDesk();

        addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent event) {
                loadProductCache();
                focusBarcodeField();
            }
            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent event) {}
            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent event) {}
        });
    }


    public void applySettingsVisibility() {
        boolean isTouch = AppSettings.isTouchMode();
        boolean showNumpad = AppSettings.getBoolean(AppSettings.KEY_SHOW_NUMPAD, true);
        boolean showHeldBills = AppSettings.getBoolean(AppSettings.KEY_SHOW_HELD_BILLS, true);
        boolean showQuickCash = AppSettings.getBoolean(AppSettings.KEY_SHOW_QUICK_CASH, true);
        boolean showDiscount = AppSettings.getBoolean(AppSettings.KEY_SHOW_DISCOUNT, true);
        boolean showUpiQr = AppSettings.getBoolean(AppSettings.KEY_SHOW_UPI_QR, true);
        boolean showBarcode = AppSettings.getBoolean(AppSettings.KEY_SHOW_BARCODE_SEARCH, true);
        boolean showCartActions = AppSettings.getBoolean(AppSettings.KEY_SHOW_CART_ACTIONS, true);

        if (lblTouchBadge != null) {
            if (isTouch) {
                lblTouchBadge.setText(" 📱 TOUCH MODE ");
                lblTouchBadge.setBackground(AppTheme.ELECTRIC_LIME);
                lblTouchBadge.setForeground(AppTheme.FOREST_DEEP);
            } else {
                lblTouchBadge.setText(" 💻 KEYBOARD MODE ");
                lblTouchBadge.setBackground(new Color(230, 245, 236));
                lblTouchBadge.setForeground(AppTheme.FOREST_DEEP);
            }
        }

        // On-screen Numpad / NUPED visibility
        if (numpadPanel != null) numpadPanel.setVisible(showNumpad);

        // Bed / Table / Held Bills visibility
        if (heldBillsPanel != null) heldBillsPanel.setVisible(showHeldBills);

        // Quick Cash tendered chips visibility
        if (quickCashPanel != null) quickCashPanel.setVisible(showQuickCash);

        // Barcode / Fast search input visibility
        if (scanBar != null) scanBar.setVisible(showBarcode);

        // Cart Action buttons visibility
        if (cartActionsPanel != null) cartActionsPanel.setVisible(showCartActions);

        // Discount & UPI buttons visibility
        if (btnAddDiscount != null) btnAddDiscount.setVisible(showDiscount);
        if (btnRemoveDiscount != null) btnRemoveDiscount.setVisible(showDiscount);
        if (btnOpenUpi != null) btnOpenUpi.setVisible(showUpiQr);
        if (discountBar != null) discountBar.setVisible(showDiscount || showUpiQr);

        revalidate();
        repaint();
    }

    private void initComponents() {
        // TOP 1: Header Strip & Invoice/Customer Meta Panel
        JPanel topContainer = new JPanel(new BorderLayout(0, 8));
        topContainer.setOpaque(false);

        // Header Strip - Elevated White Card
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

        JLabel lblTerminal = new JLabel("⚡ POS Terminal 01");
        lblTerminal.setFont(AppTheme.font(Font.BOLD, 14));
        lblTerminal.setForeground(AppTheme.TEXT_PRIMARY);

        lblTouchBadge = new JLabel(" TOUCH MODE ") {
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
        lblTouchBadge.setFont(AppTheme.font(Font.BOLD, 9));
        lblTouchBadge.setOpaque(false);
        lblTouchBadge.setBackground(AppTheme.ELECTRIC_LIME);
        lblTouchBadge.setForeground(AppTheme.FOREST_DEEP);
        lblTouchBadge.setBorder(new EmptyBorder(2, 8, 2, 8));

        headerLeft.add(lblTerminal);
        headerLeft.add(lblTouchBadge);

        JLabel lblShortcuts = new JLabel("[ F1 Scan • F2 Hold • F3 Cust • F4 Cash • F6 UPI • Ctrl+↵ Pay ]");
        lblShortcuts.setFont(AppTheme.font(Font.PLAIN, 11));
        lblShortcuts.setForeground(AppTheme.TEXT_MUTED);
        headerLeft.add(lblShortcuts);

        headerStrip.add(headerLeft, BorderLayout.WEST);

        // Held Bills Toolbar
        heldBillsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        heldBillsPanel.setOpaque(false);

        JLabel lblHeld = new JLabel("Held Bills:");
        lblHeld.setForeground(AppTheme.TEXT_SECONDARY);
        lblHeld.setFont(AppTheme.font(Font.BOLD, 11));
        heldBillsPanel.add(lblHeld);

        cmbHeldBills = new JComboBox<>(new String[]{"-- Active Bill --"});
        cmbHeldBills.setFont(AppTheme.font(Font.PLAIN, 11));
        cmbHeldBills.setPreferredSize(new Dimension(130, 28));
        cmbHeldBills.setBackground(Color.WHITE);
        heldBillsPanel.add(cmbHeldBills);

        JButton btnResumeBill = createPillButton("Recall", new Color(239, 246, 255), new Color(191, 219, 254), new Color(29, 78, 216));
        heldBillsPanel.add(btnResumeBill);

        JButton btnHoldBill = createPillButton("Hold (F2)", new Color(254, 243, 199), new Color(253, 230, 138), new Color(180, 83, 9));
        heldBillsPanel.add(btnHoldBill);

        JButton btnHistory = createPillButton("📜 Bill History", new Color(240, 250, 244), AppTheme.BORDER_SAGE, AppTheme.FOREST_GREEN);
        btnHistory.setToolTipText("View Total Orders & Sales History");
        btnHistory.addActionListener(e -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            if (owner instanceof DashboardFrame) {
                ((DashboardFrame) owner).showBillHistory();
            }
        });
        heldBillsPanel.add(btnHistory);

        JButton btnSelfCheckout = createPillButton("🛒 Self Checkout", AppTheme.FOREST_GREEN, AppTheme.FOREST_GREEN, Color.WHITE);
        btnSelfCheckout.setToolTipText("Launch Customer Self-Checkout Express");
        btnSelfCheckout.addActionListener(e -> {
            new SelfCheckoutFrame(currentUser).setVisible(true);
        });
        heldBillsPanel.add(btnSelfCheckout);

        headerStrip.add(heldBillsPanel, BorderLayout.EAST);
        topContainer.add(headerStrip, BorderLayout.NORTH);

        // Sub-header Meta Panel (Invoice No, Customer, Mobile Search, Payment Mode)
        JPanel metaPanel = new JPanel(new GridLayout(1, 4, 12, 0)) {
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
        metaPanel.setOpaque(false);
        metaPanel.setBorder(new EmptyBorder(8, 14, 8, 14));

        // 1. Invoice No
        JPanel pInv = new JPanel(new BorderLayout(0, 4));
        pInv.setOpaque(false);
        JLabel lblInvTitle = new JLabel("INVOICE NUMBER");
        lblInvTitle.setFont(AppTheme.font(Font.BOLD, 10));
        lblInvTitle.setForeground(AppTheme.TEXT_MUTED);
        pInv.add(lblInvTitle, BorderLayout.NORTH);

        txtInvoiceNo = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(240, 250, 244));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        txtInvoiceNo.setOpaque(false);
        txtInvoiceNo.setEditable(false);
        txtInvoiceNo.setFont(AppTheme.font(Font.BOLD, 12));
        txtInvoiceNo.setForeground(AppTheme.FOREST_MID);
        txtInvoiceNo.setBorder(new EmptyBorder(6, 10, 6, 10));
        txtInvoiceNo.setPreferredSize(new Dimension(0, 32));
        pInv.add(txtInvoiceNo, BorderLayout.CENTER);
        metaPanel.add(pInv);

        // 2. Customer
        JPanel pCust = new JPanel(new BorderLayout(0, 4));
        pCust.setOpaque(false);
        JPanel pCustHeader = new JPanel(new BorderLayout(4, 0));
        pCustHeader.setOpaque(false);
        JLabel lblCust = new JLabel("CUSTOMER (F3)");
        lblCust.setFont(AppTheme.font(Font.BOLD, 10));
        lblCust.setForeground(AppTheme.TEXT_MUTED);
        pCustHeader.add(lblCust, BorderLayout.WEST);

        JButton btnQuickAdd = createPillButton("+ New", new Color(240, 250, 244), AppTheme.BORDER_SAGE, AppTheme.FOREST_GREEN);
        btnQuickAdd.setFont(AppTheme.font(Font.BOLD, 10));
        btnQuickAdd.setBorder(new EmptyBorder(1, 8, 1, 8));
        btnQuickAdd.setPreferredSize(new Dimension(54, 20));
        btnQuickAdd.setToolTipText("Quick Add New Customer");
        btnQuickAdd.addActionListener(e -> openQuickAddCustomerDialog(txtCustPhone.getText().trim()));
        pCustHeader.add(btnQuickAdd, BorderLayout.EAST);
        pCust.add(pCustHeader, BorderLayout.NORTH);

        cmbCustomer = new JComboBox<>();
        cmbCustomer.setFont(AppTheme.font(Font.PLAIN, 12));
        cmbCustomer.setBackground(Color.WHITE);
        cmbCustomer.setPreferredSize(new Dimension(0, 32));
        pCust.add(cmbCustomer, BorderLayout.CENTER);
        metaPanel.add(pCust);

        // 3. Customer Mobile Search
        JPanel pPhone = new JPanel(new BorderLayout(0, 4));
        pPhone.setOpaque(false);
        JLabel lblPhoneTitle = new JLabel("CUSTOMER MOBILE (ENTER)");
        lblPhoneTitle.setFont(AppTheme.font(Font.BOLD, 10));
        lblPhoneTitle.setForeground(AppTheme.TEXT_MUTED);
        pPhone.add(lblPhoneTitle, BorderLayout.NORTH);

        txtCustPhone = new ModernSearchField("Mobile or name search...", 12);
        txtCustPhone.setToolTipText("Enter customer mobile to search or register (Press Enter)");
        txtCustPhone.setPreferredSize(new Dimension(0, 32));
        pPhone.add(txtCustPhone, BorderLayout.CENTER);
        metaPanel.add(pPhone);

        // 4. Payment Mode
        JPanel pMode = new JPanel(new BorderLayout(0, 4));
        pMode.setOpaque(false);
        JLabel lblModeTitle = new JLabel("PAYMENT MODE (F4/F6)");
        lblModeTitle.setFont(AppTheme.font(Font.BOLD, 10));
        lblModeTitle.setForeground(AppTheme.TEXT_MUTED);
        pMode.add(lblModeTitle, BorderLayout.NORTH);

        cmbPaymentMode = new JComboBox<>(new String[]{"CASH", "UPI", "CARD", "CREDIT"});
        cmbPaymentMode.setFont(AppTheme.font(Font.BOLD, 12));
        cmbPaymentMode.setBackground(Color.WHITE);
        cmbPaymentMode.setPreferredSize(new Dimension(0, 32));
        pMode.add(cmbPaymentMode, BorderLayout.CENTER);
        metaPanel.add(pMode);

        topContainer.add(metaPanel, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        // CENTER: Left Barcode & Cart Table (POS Desk)
        JPanel leftCenter = new JPanel(new BorderLayout(0, 8));
        leftCenter.setOpaque(false);

        // Barcode / Fast Scanner Card
        scanBar = new JPanel(new BorderLayout(10, 0)) {
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
        scanBar.setOpaque(false);
        scanBar.setBorder(new EmptyBorder(8, 14, 8, 14));

        JLabel lblScanIcon = new JLabel("🏷️ BARCODE / SKU (F1): ");
        lblScanIcon.setFont(AppTheme.font(Font.BOLD, 12));
        lblScanIcon.setForeground(AppTheme.TEXT_PRIMARY);
        scanBar.add(lblScanIcon, BorderLayout.WEST);

        txtBarcode = new JTextField() {
            private boolean focused = false;
            {
                addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                    @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int arc = 14;
                g2.setColor(new Color(248, 252, 249));
                g2.fillRoundRect(0, 0, w, h, arc, arc);

                if (focused) {
                    g2.setColor(AppTheme.FOREST_GREEN);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                } else {
                    g2.setColor(AppTheme.BORDER_SAGE);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                }
                super.paintComponent(g);
                if (getText().isEmpty() && !focused) {
                    g2.setColor(AppTheme.TEXT_MUTED);
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString("Scan barcode or type code/name and press Enter...", 12, ty);
                }
                g2.dispose();
            }
        };
        txtBarcode.setOpaque(false);
        txtBarcode.setFont(AppTheme.font(Font.BOLD, 14));
        txtBarcode.setBorder(new EmptyBorder(6, 12, 6, 12));
        txtBarcode.setPreferredSize(new Dimension(0, 36));

        barcodeScanTimer = new Timer(250, e -> {
            String typed = txtBarcode.getText() == null ? "" : txtBarcode.getText().trim();
            if (!typed.isEmpty() && looksLikeBarcodeScan(typed)) {
                handleBarcodeScan(false);
            }
        });
        barcodeScanTimer.setRepeats(false);
        txtBarcode.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { scheduleBarcodeAutoScan(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { scheduleBarcodeAutoScan(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { scheduleBarcodeAutoScan(); }
        });
        scanBar.add(txtBarcode, BorderLayout.CENTER);

        JPanel scanEastActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        scanEastActions.setOpaque(false);

        btnOpenScanner = createPillButton("📷 Scan Barcode (F7)", new Color(240, 250, 244), AppTheme.BORDER_SAGE, AppTheme.FOREST_GREEN);
        btnOpenScanner.setPreferredSize(new Dimension(150, 36));
        scanEastActions.add(btnOpenScanner);

        btnScanAdd = createPillButton("➕ Add Item ↵", AppTheme.FOREST_GREEN, AppTheme.FOREST_GREEN, Color.WHITE);
        btnScanAdd.setPreferredSize(new Dimension(116, 36));
        scanEastActions.add(btnScanAdd);

        scanBar.add(scanEastActions, BorderLayout.EAST);
        leftCenter.add(scanBar, BorderLayout.NORTH);

        // Cart Table with Empty State & elevated card
        JPanel tableCard = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = 20;
                g2.setColor(new Color(0, 50, 30, 6));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = 20;
                Shape clip = new RoundRectangle2D.Float(0, 0, w, h, r, r);
                g2.clip(clip);
                super.paintChildren(g2);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.dispose();
            }
        };
        tableCard.setOpaque(false);

        String[] cartCols = {"#", "Code / Barcode", "Product Name", "Rate (₹)", "Qty", "Total (₹)", "Action"};
        cartModel = new DefaultTableModel(cartCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 4; // allow direct inline quantity editing
            }
        };

        cartTable = new JTable(cartModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
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
        cartTable.setOpaque(false);
        cartTable.setRowHeight(40);
        cartTable.setFont(AppTheme.font(Font.PLAIN, 12));
        cartTable.setShowVerticalLines(false);
        cartTable.setGridColor(new Color(241, 246, 243));
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cartTable.setSelectionBackground(new Color(230, 248, 238));
        cartTable.setSelectionForeground(AppTheme.TEXT_PRIMARY);

        // Header
        JTableHeader header = cartTable.getTableHeader();
        header.setPreferredSize(new Dimension(0, 40));
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
                if (c == 0 || c == 4 || c == 6) {
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (c == 3 || c == 5) {
                    l.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return l;
            }
        });

        // Column widths
        cartTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(260);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(85);
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(65);
        cartTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        cartTable.getColumnModel().getColumn(6).setPreferredWidth(85);

        // Alignments & Styles
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        centerRender.setFont(AppTheme.font(Font.BOLD, 11));
        centerRender.setForeground(AppTheme.TEXT_MUTED);
        cartTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        DefaultTableCellRenderer codeRender = new DefaultTableCellRenderer();
        codeRender.setFont(AppTheme.font(Font.BOLD, 12));
        codeRender.setForeground(AppTheme.FOREST_MID);
        codeRender.setBorder(new EmptyBorder(0, 10, 0, 6));
        cartTable.getColumnModel().getColumn(1).setCellRenderer(codeRender);

        DefaultTableCellRenderer nameRender = new DefaultTableCellRenderer();
        nameRender.setFont(AppTheme.font(Font.PLAIN, 12));
        nameRender.setForeground(AppTheme.TEXT_PRIMARY);
        nameRender.setBorder(new EmptyBorder(0, 10, 0, 6));
        cartTable.getColumnModel().getColumn(2).setCellRenderer(nameRender);

        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
        rightRender.setFont(AppTheme.font(Font.PLAIN, 12));
        rightRender.setForeground(AppTheme.TEXT_SECONDARY);
        rightRender.setBorder(new EmptyBorder(0, 6, 0, 10));
        cartTable.getColumnModel().getColumn(3).setCellRenderer(rightRender);

        DefaultTableCellRenderer qtyRender = new DefaultTableCellRenderer();
        qtyRender.setHorizontalAlignment(SwingConstants.CENTER);
        qtyRender.setFont(AppTheme.font(Font.BOLD, 12));
        qtyRender.setForeground(AppTheme.TEXT_PRIMARY);
        cartTable.getColumnModel().getColumn(4).setCellRenderer(qtyRender);

        DefaultTableCellRenderer totalRender = new DefaultTableCellRenderer();
        totalRender.setHorizontalAlignment(SwingConstants.RIGHT);
        totalRender.setFont(AppTheme.font(Font.BOLD, 13));
        totalRender.setForeground(AppTheme.TEXT_PRIMARY);
        totalRender.setBorder(new EmptyBorder(0, 6, 0, 12));
        cartTable.getColumnModel().getColumn(5).setCellRenderer(totalRender);

        // Action Column: Sleek pill remove button
        cartTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(AppTheme.font(Font.BOLD, 11));
                l.setForeground(new Color(220, 38, 38));
                l.setText("🗑️ Remove");
                return l;
            }
        });

        // Double click or click on action column removes
        cartTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int r = cartTable.rowAtPoint(e.getPoint());
                int c = cartTable.columnAtPoint(e.getPoint());
                if (r >= 0 && r < cartItems.size()) {
                    if (c == 6) {
                        removeItemFromCart(r);
                    }
                }
            }
        });

        JViewport customViewport = new JViewport() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (cartItems == null || cartItems.isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth();
                    int h = getHeight();
                    int cy = Math.max(140, h / 3);

                    SidebarIcon cartIcon = new SidebarIcon("cart", 52, new Color(195, 222, 210));
                    cartIcon.paintIcon(this, g2, (w - 52) / 2, cy - 65);

                    g2.setColor(AppTheme.TEXT_PRIMARY);
                    g2.setFont(AppTheme.font(Font.BOLD, 15));
                    FontMetrics fm1 = g2.getFontMetrics();
                    String t1 = "Your Billing Cart is Empty";
                    g2.drawString(t1, (w - fm1.stringWidth(t1)) / 2, cy + 16);

                    g2.setColor(AppTheme.TEXT_MUTED);
                    g2.setFont(AppTheme.font(Font.PLAIN, 12));
                    FontMetrics fm2 = g2.getFontMetrics();
                    String t2 = "Scan a product barcode, type code, or press F1 to begin checkout";
                    g2.drawString(t2, (w - fm2.stringWidth(t2)) / 2, cy + 38);

                    g2.dispose();
                }
            }
        };
        customViewport.setOpaque(false);
        customViewport.setView(cartTable);

        JScrollPane tableScroll = new JScrollPane();
        tableScroll.setViewport(customViewport);
        tableScroll.setColumnHeaderView(cartTable.getTableHeader());
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
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }
            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }
            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
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
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(trackColor);
                g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                g2.dispose();
            }
        });

        tableCard.add(tableScroll, BorderLayout.CENTER);

        // Cart status footer
        JPanel cartFooter = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(248, 252, 249));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cartFooter.setOpaque(false);
        cartFooter.setBorder(new EmptyBorder(8, 14, 8, 14));

        JPanel statsLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statsLeft.setOpaque(false);

        lblItemsCount = new JLabel("Items: 0") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(240, 250, 244));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblItemsCount.setFont(AppTheme.font(Font.BOLD, 11));
        lblItemsCount.setForeground(AppTheme.FOREST_MID);
        lblItemsCount.setBorder(new EmptyBorder(3, 8, 3, 8));
        statsLeft.add(lblItemsCount);

        lblTotalQty = new JLabel("Total Qty: 0") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(240, 250, 244));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblTotalQty.setFont(AppTheme.font(Font.BOLD, 11));
        lblTotalQty.setForeground(AppTheme.FOREST_MID);
        lblTotalQty.setBorder(new EmptyBorder(3, 8, 3, 8));
        statsLeft.add(lblTotalQty);
        cartFooter.add(statsLeft, BorderLayout.WEST);

        cartActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        cartActionsPanel.setOpaque(false);

        JButton btnIncQty = createPillButton("➕ Qty +1", new Color(240, 250, 244), AppTheme.BORDER_SAGE, AppTheme.TEXT_PRIMARY);
        cartActionsPanel.add(btnIncQty);

        JButton btnDecQty = createPillButton("➖ Qty -1", new Color(240, 250, 244), AppTheme.BORDER_SAGE, AppTheme.TEXT_PRIMARY);
        cartActionsPanel.add(btnDecQty);

        JButton btnRemove = createPillButton("🗑️ Remove", new Color(254, 242, 242), new Color(254, 202, 202), new Color(220, 38, 38));
        cartActionsPanel.add(btnRemove);

        JButton btnClear = createPillButton("Clear All", new Color(248, 250, 252), new Color(226, 232, 240), AppTheme.TEXT_MUTED);
        cartActionsPanel.add(btnClear);

        btnQuickCheckout = createPillButton("💳 Pay & Print (Ctrl+Enter)", AppTheme.FOREST_GREEN, AppTheme.FOREST_GREEN, Color.WHITE);
        btnQuickCheckout.setFocusPainted(false);
        cartActionsPanel.add(btnQuickCheckout);

        cartFooter.add(cartActionsPanel, BorderLayout.EAST);
        tableCard.add(cartFooter, BorderLayout.SOUTH);

        leftCenter.add(tableCard, BorderLayout.CENTER);
        add(leftCenter, BorderLayout.CENTER);

        // RIGHT: POS Checkout Console Hub
        JPanel rightHub = new JPanel(new BorderLayout(0, 10));
        rightHub.setPreferredSize(new Dimension(360, 0));
        rightHub.setOpaque(false);

        // 1. Total Bill Hero Card - Deep Forest Gradient
        JPanel totalDisplayCard = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = 24;

                g2.setColor(new Color(0, 50, 30, 12));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);

                GradientPaint gp = new GradientPaint(0, 0, AppTheme.FOREST_DEEP, w, h, AppTheme.FOREST_GREEN);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);

                Shape clip = new RoundRectangle2D.Float(0, 0, w, h, r, r);
                g2.setClip(clip);
                SidebarIcon bgIcon = new SidebarIcon("wallet", 58, Color.WHITE);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
                bgIcon.paintIcon(this, g2, w - 68, 14);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        totalDisplayCard.setOpaque(false);
        totalDisplayCard.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel lblPayableTitle = new JLabel("TOTAL AMOUNT DUE");
        lblPayableTitle.setFont(AppTheme.font(Font.BOLD, 10));
        lblPayableTitle.setForeground(new Color(193, 247, 0, 210));
        totalDisplayCard.add(lblPayableTitle, BorderLayout.NORTH);

        lblGrandTotal = new JLabel("₹0.00");
        lblGrandTotal.setFont(AppTheme.font(Font.BOLD, 32));
        lblGrandTotal.setForeground(AppTheme.ELECTRIC_LIME);
        totalDisplayCard.add(lblGrandTotal, BorderLayout.CENTER);

        JPanel subBreakdown = new JPanel(new GridLayout(3, 2, 4, 3));
        subBreakdown.setOpaque(false);
        subBreakdown.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 30)),
                new EmptyBorder(8, 0, 0, 0)
        ));

        subBreakdown.add(createWhiteLabel("Subtotal:"));
        lblSubtotal = createWhiteLabel("₹0.00", SwingConstants.RIGHT);
        subBreakdown.add(lblSubtotal);

        subBreakdown.add(createWhiteLabel("Discount:"));
        lblDiscount = createWhiteLabel("₹0.00", SwingConstants.RIGHT);
        lblDiscount.setForeground(new Color(254, 202, 202));
        subBreakdown.add(lblDiscount);

        subBreakdown.add(createWhiteLabel("GST (18%):"));
        lblGstAmount = createWhiteLabel("₹0.00", SwingConstants.RIGHT);
        subBreakdown.add(lblGstAmount);

        totalDisplayCard.add(subBreakdown, BorderLayout.SOUTH);
        rightHub.add(totalDisplayCard, BorderLayout.NORTH);

        // 2. Middle Panel: Payment Tools, Cash Tendered, Change Due, Numpad
        JPanel midPanel = new JPanel();
        midPanel.setLayout(new BoxLayout(midPanel, BoxLayout.Y_AXIS));
        midPanel.setOpaque(false);

        // Discount & UPI Card
        discountBar = createElevatedCard(18, 10, 12, 10, 12);
        discountBar.setLayout(new BorderLayout(8, 6));

        JLabel lblDiscHeader = new JLabel("PAYMENT TOOLS & DISCOUNT");
        lblDiscHeader.setFont(AppTheme.font(Font.BOLD, 10));
        lblDiscHeader.setForeground(AppTheme.TEXT_MUTED);
        discountBar.add(lblDiscHeader, BorderLayout.NORTH);

        JPanel discBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        discBtnRow.setOpaque(false);

        btnOpenUpi = createPillButton("📱 UPI QR (F6)", new Color(124, 58, 237), new Color(109, 40, 217), Color.WHITE);
        btnAddDiscount = createPillButton("🏷️ Discount (Ctrl+D)", new Color(240, 250, 244), AppTheme.BORDER_SAGE, AppTheme.FOREST_GREEN);
        btnRemoveDiscount = createPillButton("Remove", new Color(248, 250, 252), new Color(226, 232, 240), AppTheme.TEXT_MUTED);

        discBtnRow.add(btnOpenUpi);
        discBtnRow.add(btnAddDiscount);
        discBtnRow.add(btnRemoveDiscount);
        discountBar.add(discBtnRow, BorderLayout.CENTER);

        midPanel.add(discountBar);
        midPanel.add(Box.createVerticalStrut(8));

        // Quick Cash Presets Card
        quickCashPanel = createElevatedCard(18, 10, 12, 10, 12);
        quickCashPanel.setLayout(new BorderLayout(0, 6));

        JLabel lblCashPresetsHeader = new JLabel("💵 QUICK CASH TENDERED");
        lblCashPresetsHeader.setFont(AppTheme.font(Font.BOLD, 10));
        lblCashPresetsHeader.setForeground(AppTheme.TEXT_MUTED);
        quickCashPanel.add(lblCashPresetsHeader, BorderLayout.NORTH);

        JPanel cashChipsGrid = new JPanel(new GridLayout(2, 3, 6, 6));
        cashChipsGrid.setOpaque(false);

        int[] cashPresets = {100, 200, 500, 1000, 2000};
        for (int p : cashPresets) {
            JButton btnChip = createPillButton("₹" + p, new Color(248, 252, 249), AppTheme.BORDER_SAGE, AppTheme.TEXT_PRIMARY);
            btnChip.addActionListener(e -> setTenderedCash(p));
            cashChipsGrid.add(btnChip);
        }
        JButton btnExact = createPillButton("EXACT", new Color(236, 253, 243), new Color(167, 243, 208), new Color(2, 122, 72));
        btnExact.addActionListener(e -> setTenderedCash(grandTotal));
        cashChipsGrid.add(btnExact);

        quickCashPanel.add(cashChipsGrid, BorderLayout.CENTER);
        midPanel.add(quickCashPanel);
        midPanel.add(Box.createVerticalStrut(8));

        // Cash Paid & Change Return Card
        JPanel tenderCard = createElevatedCard(18, 10, 14, 10, 14);
        tenderCard.setLayout(new GridLayout(2, 2, 10, 6));

        JLabel lblCashTitle = new JLabel("Cash Paid (₹):");
        lblCashTitle.setFont(AppTheme.font(Font.BOLD, 12));
        lblCashTitle.setForeground(AppTheme.TEXT_PRIMARY);
        tenderCard.add(lblCashTitle);

        txtCashPaid = new JTextField("0.00") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 252, 249));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        txtCashPaid.setOpaque(false);
        txtCashPaid.setFont(AppTheme.font(Font.BOLD, 15));
        txtCashPaid.setHorizontalAlignment(JTextField.RIGHT);
        txtCashPaid.setBorder(new EmptyBorder(4, 8, 4, 8));
        tenderCard.add(txtCashPaid);

        JLabel lblChangeTitle = new JLabel("Change Return:");
        lblChangeTitle.setFont(AppTheme.font(Font.BOLD, 12));
        lblChangeTitle.setForeground(new Color(220, 38, 38));
        tenderCard.add(lblChangeTitle);

        lblChangeDue = new JLabel("₹0.00", SwingConstants.RIGHT);
        lblChangeDue.setFont(AppTheme.font(Font.BOLD, 17));
        lblChangeDue.setForeground(new Color(0, 135, 90));
        tenderCard.add(lblChangeDue);

        midPanel.add(tenderCard);
        midPanel.add(Box.createVerticalStrut(8));

        // Touch Numpad
        numpadPanel = createElevatedCard(18, 10, 12, 10, 12);
        numpadPanel.setLayout(new BorderLayout(0, 6));

        JLabel lblNumpadTitle = new JLabel("🔢 ON-SCREEN NUMPAD");
        lblNumpadTitle.setFont(AppTheme.font(Font.BOLD, 10));
        lblNumpadTitle.setForeground(AppTheme.TEXT_MUTED);
        numpadPanel.add(lblNumpadTitle, BorderLayout.NORTH);

        JPanel numpadGrid = new JPanel(new GridLayout(4, 3, 5, 5));
        numpadGrid.setOpaque(false);

        String[] keys = {"7", "8", "9", "4", "5", "6", "1", "2", "3", "C", "0", "."};
        for (String k : keys) {
            JButton btnKey = createPillButton(k, new Color(248, 252, 249), AppTheme.BORDER_SAGE, AppTheme.TEXT_PRIMARY);
            btnKey.setFont(AppTheme.font(Font.BOLD, 13));
            btnKey.addActionListener(e -> handleNumpadKey(k));
            numpadGrid.add(btnKey);
        }
        numpadPanel.add(numpadGrid, BorderLayout.CENTER);
        midPanel.add(numpadPanel);

        rightHub.add(midPanel, BorderLayout.CENTER);

        // 3. Bottom Big Checkout CTA
        JPanel bottomCheckout = new JPanel(new GridLayout(1, 1));
        bottomCheckout.setOpaque(false);
        JButton btnPayPrint = createPillButton("💳 PAY & PRINT BILL (Ctrl+Enter)", AppTheme.FOREST_GREEN, AppTheme.FOREST_GREEN, Color.WHITE);
        btnPayPrint.setFont(AppTheme.font(Font.BOLD, 14));
        btnPayPrint.setPreferredSize(new Dimension(0, 50));
        bottomCheckout.add(btnPayPrint);

        rightHub.add(bottomCheckout, BorderLayout.SOUTH);
        add(rightHub, BorderLayout.EAST);

        // LISTENERS
        btnOpenScanner.addActionListener(e -> openBarcodeScannerDialog());
        btnScanAdd.addActionListener(e -> handleBarcodeScan(true));
        btnQuickCheckout.addActionListener(e -> completeSale());

        txtBarcode.addActionListener(e -> {
            String val = txtBarcode.getText() == null ? "" : txtBarcode.getText().trim();
            if (val.isEmpty()) {
                if (!cartItems.isEmpty()) {
                    int opt = JOptionPane.showConfirmDialog(this,
                            "Cart is ready with " + cartItems.size() + " item(s) (Total: " + lblGrandTotal.getText() + ").\nProceed to Pay & Print Bill now?",
                            "Ready for Billing", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (opt == JOptionPane.YES_OPTION) {
                        completeSale();
                    }
                }
            } else {
                handleBarcodeScan(true);
            }
        });

        btnIncQty.addActionListener(e -> adjustSelectedQuantity(1));
        btnDecQty.addActionListener(e -> adjustSelectedQuantity(-1));
        btnRemove.addActionListener(e -> removeSelectedItem());
        btnClear.addActionListener(e -> resetBillingDesk());

        btnAddDiscount.addActionListener(e -> promptDiscountDialog());
        btnRemoveDiscount.addActionListener(e -> {
            discountValue = 0.0;
            calculateTotals();
        });

        btnOpenUpi.addActionListener(e -> openUpiDialog());

        btnHoldBill.addActionListener(e -> holdCurrentBill());
        btnResumeBill.addActionListener(e -> resumeSelectedBill());

        txtCashPaid.addActionListener(e -> updateCashCalculations());
        txtCashPaid.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateCashCalculations();
            }
        });

        btnPayPrint.addActionListener(e -> completeSale());

        // Table inline quantity edit listener
        cartModel.addTableModelListener(e -> {
            if (e.getColumn() == 4) {
                int r = e.getFirstRow();
                try {
                    int qty = Integer.parseInt(cartModel.getValueAt(r, 4).toString().trim());
                    if (qty <= 0) {
                        removeItemFromCart(r);
                    } else {
                        SaleItem it = cartItems.get(r);
                        it.setQuantity(qty);
                        it.setSubtotal(qty * it.getUnitPrice());
                        cartModel.setValueAt(String.format("₹%.2f", it.getSubtotal()), r, 5);
                        calculateTotals();
                    }
                } catch (Exception ignored) {}
            }
        });

        // Customer selection sync
        cmbCustomer.addActionListener(e -> {
            if (isUpdatingCustomerCombo) return;
            Customer c = (Customer) cmbCustomer.getSelectedItem();
            if (c != null && c.getId() > 0) {
                txtCustPhone.setText(c.getPhone() != null ? c.getPhone() : "");
            } else {
                txtCustPhone.setText("");
            }
        });

        // Quick phone search auto-select customer
        txtCustPhone.addActionListener(e -> quickSearchCustomer(txtCustPhone.getText().trim()));

        // Payment mode selection change: reset cash to 0.00 for online / non-cash modes
        cmbPaymentMode.addActionListener(e -> {
            String mode = (String) cmbPaymentMode.getSelectedItem();
            boolean isCash = "CASH".equalsIgnoreCase(mode);
            if (isCash) {
                if (cashTendered <= 0.0 || Math.abs(cashTendered - grandTotal) < 0.01) {
                    cashTendered = grandTotal;
                    txtCashPaid.setText(String.format("%.2f", grandTotal));
                }
            } else {
                cashTendered = 0.0;
                txtCashPaid.setText("0.00");
            }
            updateCashCalculations();
        });
    }

    private JLabel createWhiteLabel(String text) {
        return createWhiteLabel(text, SwingConstants.LEFT);
    }

    private JLabel createWhiteLabel(String text, int align) {
        JLabel lbl = new JLabel(text, align);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(new Color(226, 232, 240));
        return lbl;
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
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 12, 6, 12));
        return btn;
    }

    private JPanel createElevatedCard(int arc, int topPad, int leftPad, int bottomPad, int rightPad) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                g2.setColor(new Color(0, 50, 30, 5));
                g2.fillRoundRect(1, 2, w - 2, h - 2, arc, arc);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(topPad, leftPad, bottomPad, rightPad));
        return card;
    }

    private static class ModernSearchField extends JTextField {
        private final String placeholder;
        private boolean focused = false;

        public ModernSearchField(String placeholder, int columns) {
            super(columns);
            this.placeholder = placeholder;
            setFont(AppTheme.font(Font.PLAIN, 12));
            setOpaque(false);
            setBorder(new EmptyBorder(6, 28, 6, 8));
            setForeground(AppTheme.TEXT_PRIMARY);

            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int arc = 14;

            g2.setColor(new Color(248, 252, 249));
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            if (focused) {
                g2.setColor(AppTheme.FOREST_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            } else {
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }

            g2.setColor(focused ? AppTheme.FOREST_GREEN : AppTheme.TEXT_MUTED);
            g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = 12, cy = h / 2 - 1, r = 4;
            g2.drawOval(cx - r, cy - r, r * 2, r * 2);
            g2.drawLine(cx + 3, cy + 3, cx + 7, cy + 7);

            super.paintComponent(g);

            if (getText().isEmpty() && !focused) {
                g2.setColor(AppTheme.TEXT_MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(placeholder, 28, ty);
            }

            g2.dispose();
        }
    }

    private void setupGlobalKeyShortcuts() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "focusBarcode");
        am.put("focusBarcode", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                txtBarcode.requestFocus();
                txtBarcode.selectAll();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "holdBill");
        am.put("holdBill", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                holdCurrentBill();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "focusCustomer");
        am.put("focusCustomer", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                txtCustPhone.requestFocus();
                txtCustPhone.selectAll();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0), "cashMode");
        am.put("cashMode", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cmbPaymentMode.setSelectedItem("CASH");
                txtCashPaid.requestFocus();
                txtCashPaid.selectAll();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "upiMode");
        am.put("upiMode", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cmbPaymentMode.setSelectedItem("UPI");
                openUpiDialog();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK), "discountDialog");
        am.put("discountDialog", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                promptDiscountDialog();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "payAndPrint");
        am.put("payAndPrint", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                completeSale();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "openScannerDialog");
        am.put("openScannerDialog", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                openBarcodeScannerDialog();
            }
        });
    }

    private void loadProductCache() {
        try {
            cachedProducts = productDAO.getAllProducts();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scheduleBarcodeAutoScan() {
        String typed = txtBarcode.getText() == null ? "" : txtBarcode.getText().trim();
        if (typed.isEmpty()) {
            barcodeScanTimer.stop();
            return;
        }

        if (looksLikeBarcodeScan(typed)) {
            barcodeScanTimer.restart();
        } else {
            barcodeScanTimer.stop();
        }
    }

    private boolean looksLikeBarcodeScan(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        String trimmed = value.trim();
        if (trimmed.length() < 3) return false;
        return trimmed.matches("[A-Za-z0-9_-]+");
    }

    public void loadCustomers() {
        isUpdatingCustomerCombo = true;
        try {
            Customer prev = (Customer) cmbCustomer.getSelectedItem();
            int prevId = prev != null ? prev.getId() : 0;
            cmbCustomer.removeAllItems();
            cmbCustomer.addItem(new Customer(0, "Walk-in Customer", "", "", ""));
            List<Customer> customers = customerDAO.getAllCustomers();
            for (Customer c : customers) {
                cmbCustomer.addItem(c);
                if (c.getId() == prevId) {
                    cmbCustomer.setSelectedItem(c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isUpdatingCustomerCombo = false;
        }
    }

    private void quickSearchCustomer(String query) {
        if (query == null || query.trim().isEmpty()) return;
        String q = query.trim();
        try {
            List<Customer> list = customerDAO.searchCustomers(q);
            if (!list.isEmpty()) {
                Customer matched = list.get(0);
                for (int i = 0; i < cmbCustomer.getItemCount(); i++) {
                    Customer item = cmbCustomer.getItemAt(i);
                    if (item.getId() == matched.getId()) {
                        isUpdatingCustomerCombo = true;
                        cmbCustomer.setSelectedIndex(i);
                        isUpdatingCustomerCombo = false;
                        break;
                    }
                }
                txtCustPhone.setText(matched.getPhone() != null && !matched.getPhone().isEmpty() ? matched.getPhone() : matched.getName());
                focusBarcodeField();
            } else {
                int choice = JOptionPane.showConfirmDialog(
                        this,
                        "Customer '" + q + "' not found.\nWould you like to register this customer now?",
                        "Customer Not Found",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (choice == JOptionPane.YES_OPTION) {
                    openQuickAddCustomerDialog(q);
                } else {
                    txtCustPhone.requestFocus();
                    txtCustPhone.selectAll();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Customer openQuickAddCustomerDialog(String phonePrefill) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, "Quick Add Customer", true);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.setSize(400, 270);
        dlg.setLocationRelativeTo(owner);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(241, 245, 249));
        header.setBorder(new EmptyBorder(10, 16, 10, 16));
        JLabel lblHeader = new JLabel("Register New Customer");
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblHeader.setForeground(new Color(30, 41, 59));
        header.add(lblHeader, BorderLayout.CENTER);
        dlg.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(14, 16, 10, 16));
        form.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblPhone = new JLabel("Mobile No:*");
        lblPhone.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JTextField txtPhone = new JTextField(phonePrefill != null ? phonePrefill : "", 15);
        txtPhone.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel lblName = new JLabel("Full Name:*");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JTextField txtName = new JTextField(15);
        txtName.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel lblAddress = new JLabel("City / Address:");
        lblAddress.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JTextField txtAddress = new JTextField(15);
        txtAddress.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.32;
        form.add(lblPhone, gbc);
        gbc.gridx = 1; gbc.weightx = 0.68;
        form.add(txtPhone, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.32;
        form.add(lblName, gbc);
        gbc.gridx = 1; gbc.weightx = 0.68;
        form.add(txtName, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.32;
        form.add(lblAddress, gbc);
        gbc.gridx = 1; gbc.weightx = 0.68;
        form.add(txtAddress, gbc);

        final Customer[] result = new Customer[1];

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(new Color(248, 250, 252));
        JButton btnCancel = new JButton("Cancel");
        JButton btnSave = new JButton("Save & Select Customer");
        btnSave.setBackground(new Color(37, 99, 235));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSave.setFocusPainted(false);

        btnCancel.addActionListener(e -> dlg.dispose());

        ActionListener saveAction = e -> {
            String p = txtPhone.getText().trim();
            String n = txtName.getText().trim();
            String addr = txtAddress.getText().trim();

            if (n.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Please enter customer name.", "Required Field", JOptionPane.WARNING_MESSAGE);
                txtName.requestFocus();
                return;
            }

            Customer c = new Customer();
            c.setName(n);
            c.setPhone(p);
            c.setAddress(addr);
            c.setEmail("");

            try {
                boolean ok = customerDAO.addCustomer(c);
                if (ok) {
                    result[0] = c;
                    loadCustomers();
                    for (int i = 0; i < cmbCustomer.getItemCount(); i++) {
                        Customer item = cmbCustomer.getItemAt(i);
                        if (item.getId() == c.getId()) {
                            isUpdatingCustomerCombo = true;
                            cmbCustomer.setSelectedIndex(i);
                            isUpdatingCustomerCombo = false;
                            break;
                        }
                    }
                    txtCustPhone.setText(c.getPhone() != null && !c.getPhone().isEmpty() ? c.getPhone() : c.getName());
                    dlg.dispose();
                    focusBarcodeField();
                } else {
                    JOptionPane.showMessageDialog(dlg, "Failed to register customer.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error saving customer: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        btnSave.addActionListener(saveAction);
        txtName.addActionListener(saveAction);
        txtPhone.addActionListener(e -> {
            if (!txtPhone.getText().trim().isEmpty()) {
                txtName.requestFocus();
            }
        });

        footer.add(btnCancel);
        footer.add(btnSave);

        dlg.add(form, BorderLayout.CENTER);
        dlg.add(footer, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(() -> {
            if (phonePrefill != null && !phonePrefill.isEmpty()) {
                txtName.requestFocusInWindow();
            } else {
                txtPhone.requestFocusInWindow();
            }
        });

        dlg.setVisible(true);
        return result[0];
    }

    public void handleBarcodeScan() {
        handleBarcodeScan(true);
    }

    public void handleBarcodeScan(boolean isExplicitSubmit) {
        String raw = txtBarcode.getText() == null ? "" : txtBarcode.getText().trim();
        String code = raw.replaceAll("[\\r\\n\\t]", "").trim();
        if (code.isEmpty()) return;

        // Instant local lookup from cache
        Product match = findProductByCodeOrBarcode(code);

        // If not found in cache, reload cache and try again
        if (match == null) {
            loadProductCache();
            match = findProductByCodeOrBarcode(code);
        }

        // DB Fallback if still not found in cache
        if (match == null) {
            try {
                match = productDAO.getProductByCode(code);
                if (match == null) {
                    List<Product> search = productDAO.searchProducts(code);
                    if (!search.isEmpty()) {
                        match = search.get(0);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (match == null) {
            if (isExplicitSubmit) {
                Toolkit.getDefaultToolkit().beep();
                JOptionPane.showMessageDialog(this,
                        "Product not found for barcode / code: " + code,
                        "Barcode Scan", JOptionPane.WARNING_MESSAGE);
                txtBarcode.selectAll();
            }
            return;
        }

        boolean ok = addProductToCart(match, 1);
        if (ok) {
            txtBarcode.setText("");
            focusBarcodeField();
        }
    }

    public Product findProductByCodeOrBarcode(String query) {
        if (query == null || query.trim().isEmpty()) return null;
        String q = query.trim();
        for (Product p : cachedProducts) {
            String b = p.getBarcode() != null ? p.getBarcode().trim() : "";
            String c = p.getCode() != null ? p.getCode().trim() : "";
            String n = p.getName() != null ? p.getName().trim() : "";
            if (q.equalsIgnoreCase(b) || q.equalsIgnoreCase(c) || q.equalsIgnoreCase(n)) {
                return p;
            }
        }
        return null;
    }

    public boolean addProductToCart(Product p, int qty) {
        if (p == null || qty <= 0) return false;

        // Stock check
        int inCart = 0;
        for (SaleItem it : cartItems) {
            if (it.getProductId() == p.getId()) inCart += it.getQuantity();
        }

        if (inCart + qty > p.getQuantity()) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this,
                    "Insufficient stock for '" + p.getName() + "'!\nAvailable: " + p.getQuantity() + ", in cart: " + inCart,
                    "Stock Warning", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // Update existing or add new
        boolean found = false;
        for (int i = 0; i < cartItems.size(); i++) {
            SaleItem it = cartItems.get(i);
            if (it.getProductId() == p.getId()) {
                it.setQuantity(it.getQuantity() + qty);
                it.setSubtotal(it.getQuantity() * it.getUnitPrice());
                cartModel.setValueAt(it.getQuantity(), i, 4);
                cartModel.setValueAt(String.format("₹%.2f", it.getSubtotal()), i, 5);
                found = true;
                break;
            }
        }

        if (!found) {
            double itemSub = qty * p.getSellingPrice();
            String codeOrBarcode = (p.getBarcode() != null && !p.getBarcode().trim().isEmpty()) ? p.getBarcode() : p.getCode();
            SaleItem it = new SaleItem(p.getId(), codeOrBarcode, p.getName(), qty, p.getSellingPrice(), itemSub);
            cartItems.add(it);
            cartModel.addRow(new Object[]{
                    cartItems.size(),
                    codeOrBarcode,
                    it.getProductName(),
                    String.format("₹%.2f", it.getUnitPrice()),
                    it.getQuantity(),
                    String.format("₹%.2f", it.getSubtotal()),
                    "Delete"
            });
        }

        Toolkit.getDefaultToolkit().beep(); // cashier beep
        calculateTotals();
        return true;
    }

    private void adjustSelectedQuantity(int delta) {
        int r = cartTable.getSelectedRow();
        if (r == -1) {
            if (!cartItems.isEmpty()) r = cartItems.size() - 1; // default to last item
            else return;
        }

        SaleItem it = cartItems.get(r);
        int newQty = it.getQuantity() + delta;
        if (newQty <= 0) {
            removeItemFromCart(r);
        } else {
            it.setQuantity(newQty);
            it.setSubtotal(newQty * it.getUnitPrice());
            cartModel.setValueAt(newQty, r, 4);
            cartModel.setValueAt(String.format("₹%.2f", it.getSubtotal()), r, 5);
            calculateTotals();
        }
    }

    private void removeSelectedItem() {
        int r = cartTable.getSelectedRow();
        if (r == -1) {
            JOptionPane.showMessageDialog(this, "Select an item to remove.");
            return;
        }
        removeItemFromCart(r);
    }

    private void removeItemFromCart(int r) {
        cartItems.remove(r);
        cartModel.removeRow(r);
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            cartModel.setValueAt(i + 1, i, 0);
        }
        calculateTotals();
    }

    private void calculateTotals() {
        subtotal = 0.0;
        int totalUnits = 0;
        for (SaleItem it : cartItems) {
            subtotal += it.getSubtotal();
            totalUnits += it.getQuantity();
        }

        // Compute discount
        double discAmt = 0.0;
        if ("PERCENT".equalsIgnoreCase(discountType)) {
            discAmt = (subtotal * discountValue) / 100.0;
        } else {
            discAmt = Math.min(discountValue, subtotal);
        }

        double discountedSubtotal = Math.max(0, subtotal - discAmt);
        gstAmount = (discountedSubtotal * gstRate) / 100.0;
        grandTotal = discountedSubtotal + gstAmount;

        lblItemsCount.setText("Items: " + cartItems.size());
        lblTotalQty.setText("Total Qty: " + totalUnits);

        lblSubtotal.setText(String.format("₹%.2f", subtotal));
        lblDiscount.setText(String.format("-₹%.2f", discAmt));
        lblGstAmount.setText(String.format("₹%.2f", gstAmount));
        lblGrandTotal.setText(String.format("₹%.2f", grandTotal));

        // Auto-update cash tendered: only for CASH mode, otherwise 0.00
        boolean isCash = "CASH".equalsIgnoreCase((String) cmbPaymentMode.getSelectedItem());
        if (isCash) {
            if (cashTendered <= 0.0 || Math.abs(cashTendered - grandTotal) < 0.01) {
                cashTendered = grandTotal;
                txtCashPaid.setText(String.format("%.2f", grandTotal));
            }
        } else {
            cashTendered = 0.0;
            txtCashPaid.setText("0.00");
        }

        updateCashCalculations();
        if (cartTable != null && cartTable.getParent() != null) {
            cartTable.getParent().repaint();
        }
    }

    private void setTenderedCash(double amt) {
        if (amt > 0) {
            cmbPaymentMode.setSelectedItem("CASH");
        }
        cashTendered = amt;
        txtCashPaid.setText(String.format("%.2f", cashTendered));
        updateCashCalculations();
    }

    private void handleNumpadKey(String key) {
        String cur = txtCashPaid.getText().trim();
        if ("C".equalsIgnoreCase(key)) {
            txtCashPaid.setText("0.00");
        } else {
            if ("0.00".equals(cur) || "0".equals(cur)) cur = "";
            if (".".equals(key) && cur.contains(".")) return;
            cur += key;
            txtCashPaid.setText(cur);
        }
        cmbPaymentMode.setSelectedItem("CASH");
        updateCashCalculations();
    }

    private void updateCashCalculations() {
        boolean isCash = "CASH".equalsIgnoreCase((String) cmbPaymentMode.getSelectedItem());
        if (!isCash) {
            cashTendered = 0.0;
            lblChangeDue.setText("₹0.00");
            return;
        }
        try {
            cashTendered = Double.parseDouble(txtCashPaid.getText().trim());
        } catch (Exception e) {
            cashTendered = 0.0;
        }
        double change = Math.max(0, cashTendered - grandTotal);
        lblChangeDue.setText(String.format("₹%.2f", change));
    }

    private void promptDiscountDialog() {
        JPanel p = new JPanel(new GridLayout(2, 2, 8, 8));
        JComboBox<String> cmbType = new JComboBox<>(new String[]{"Flat Amount (₹)", "Percentage (%)"});
        JTextField txtVal = new JTextField(String.valueOf(discountValue));
        p.add(new JLabel("Discount Type:"));
        p.add(cmbType);
        p.add(new JLabel("Discount Value:"));
        p.add(txtVal);

        int res = JOptionPane.showConfirmDialog(this, p, "Bill Discount (Ctrl+D)", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                discountValue = Double.parseDouble(txtVal.getText().trim());
                discountType = cmbType.getSelectedIndex() == 1 ? "PERCENT" : "FLAT";
                calculateTotals();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid discount value!");
            }
        }
    }

    private void openUpiDialog() {
        cmbPaymentMode.setSelectedItem("UPI");
        cashTendered = 0.0;
        txtCashPaid.setText("0.00");
        updateCashCalculations();

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        UpiQrDialog upiDialog = new UpiQrDialog(owner, "bazaarpoint@upi", "SmartBilling Pro", grandTotal, txtInvoiceNo.getText().trim());
        upiDialog.setVisible(true);
    }

    private void holdCurrentBill() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty, cannot hold bill.");
            return;
        }

        String ref = JOptionPane.showInputDialog(this, "Enter Hold Reference (e.g. Table 2 or Customer Name):", "Hold Bill (F2)", JOptionPane.PLAIN_MESSAGE);
        if (ref == null || ref.trim().isEmpty()) {
            ref = "Bill #" + (heldBills.size() + 1);
        }

        Customer cust = (Customer) cmbCustomer.getSelectedItem();
        HeldBill hb = new HeldBill(
                "HOLD-" + System.currentTimeMillis(),
                ref.trim(),
                cust,
                new ArrayList<>(cartItems),
                discountValue,
                discountType,
                (String) cmbPaymentMode.getSelectedItem()
        );

        heldBills.add(hb);
        updateHeldBillsDropdown();
        resetBillingDesk();
        JOptionPane.showMessageDialog(this, "Bill held successfully as: " + ref);
    }

    private void resumeSelectedBill() {
        int idx = cmbHeldBills.getSelectedIndex();
        if (idx <= 0 || heldBills.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a held bill from dropdown first.");
            return;
        }

        HeldBill hb = heldBills.get(idx - 1);
        resumeHeldBill(hb);
    }

    public boolean resumeHeldBill(HeldBill hb) {
        if (hb == null) return false;
        heldBills.remove(hb);
        updateHeldBillsDropdown();

        // Restore
        cartItems = new ArrayList<>(hb.getItems());
        cartModel.setRowCount(0);
        for (int i = 0; i < cartItems.size(); i++) {
            SaleItem it = cartItems.get(i);
            cartModel.addRow(new Object[]{
                    i + 1,
                    it.getProductCode(),
                    it.getProductName(),
                    String.format("₹%.2f", it.getUnitPrice()),
                    it.getQuantity(),
                    String.format("₹%.2f", it.getSubtotal()),
                    "Delete"
            });
        }

        discountValue = hb.getDiscountValue();
        discountType = hb.getDiscountType();
        cmbPaymentMode.setSelectedItem(hb.getPaymentMode());
        if (hb.getCustomer() != null) {
            for (int i = 0; i < cmbCustomer.getItemCount(); i++) {
                if (cmbCustomer.getItemAt(i).getId() == hb.getCustomer().getId()) {
                    cmbCustomer.setSelectedIndex(i);
                    break;
                }
            }
        }

        calculateTotals();
        JOptionPane.showMessageDialog(this, "Recalled held bill: " + hb.getReference());
        return true;
    }

    public void updateHeldBillsDropdown() {
        cmbHeldBills.removeAllItems();
        cmbHeldBills.addItem("-- Active Bill --");
        for (HeldBill hb : heldBills) {
            cmbHeldBills.addItem(hb.toString());
        }
    }

    public void resetBillingDesk() {
        try {
            txtInvoiceNo.setText(billingDAO.generateNextInvoiceNo());
        } catch (Exception e) {
            txtInvoiceNo.setText("INV-0001");
        }
        cartItems.clear();
        cartModel.setRowCount(0);
        discountValue = 0.0;
        discountType = "FLAT";
        cashTendered = 0.0;
        txtCashPaid.setText("0.00");
        txtBarcode.setText("");
        txtCustPhone.setText("");
        if (cmbPaymentMode != null) {
            cmbPaymentMode.setSelectedItem("CASH");
        }
        if (cmbCustomer != null && cmbCustomer.getItemCount() > 0) {
            isUpdatingCustomerCombo = true;
            cmbCustomer.setSelectedIndex(0);
            isUpdatingCustomerCombo = false;
        }
        calculateTotals();
        loadProductCache();
        focusBarcodeField();
    }

    public void openBarcodeScannerDialog() {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        BarcodeScannerDialog dlg = new BarcodeScannerDialog(owner, this);
        dlg.setVisible(true);
    }

    public void triggerCheckout() {
        completeSale();
    }

    public void focusBarcodeField() {
        SwingUtilities.invokeLater(() -> {
            if (txtBarcode != null) {
                txtBarcode.requestFocusInWindow();
                txtBarcode.selectAll();
            }
        });
    }

    private static class CustomerPromptResult {
        boolean cancelled = false;
        Customer customer = null;
    }

    private CustomerPromptResult promptCustomerDetailsBeforeCheckout(String prefillPhone) {
        CustomerPromptResult res = new CustomerPromptResult();
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, "Customer Details - Billing", true);
        dlg.setSize(440, 310);
        dlg.setLocationRelativeTo(owner);
        dlg.setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(241, 245, 249));
        headerPanel.setBorder(new EmptyBorder(12, 16, 12, 16));
        JLabel lblTitle = new JLabel("Customer Information (Bill #" + txtInvoiceNo.getText() + ")");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(15, 23, 42));
        JLabel lblSubtitle = new JLabel("Enter customer mobile to link bill & records, or Skip for Walk-in");
        lblSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSubtitle.setForeground(new Color(100, 116, 139));
        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSubtitle, BorderLayout.SOUTH);
        dlg.add(headerPanel, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(16, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblPhone = new JLabel("Mobile Number:");
        lblPhone.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JTextField txtPhone = new JTextField(prefillPhone != null ? prefillPhone : "", 15);
        txtPhone.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel lblName = new JLabel("Customer Name:");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JTextField txtName = new JTextField(15);
        txtName.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel lblStatus = new JLabel("Type mobile number to search or add");
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblStatus.setForeground(new Color(100, 116, 139));

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.35;
        form.add(lblPhone, gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;
        form.add(txtPhone, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.35;
        form.add(lblName, gbc);
        gbc.gridx = 1; gbc.weightx = 0.65;
        form.add(txtName, gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.65;
        form.add(lblStatus, gbc);

        dlg.add(form, BorderLayout.CENTER);

        Runnable checkPhone = () -> {
            String p = txtPhone.getText().trim();
            if (p.length() >= 10 || (p.length() >= 4 && !p.isEmpty())) {
                try {
                    Customer exist = customerDAO.getCustomerByPhone(p);
                    if (exist == null) {
                        List<Customer> list = customerDAO.searchCustomers(p);
                        if (!list.isEmpty()) exist = list.get(0);
                    }
                    if (exist != null) {
                        txtName.setText(exist.getName());
                        lblStatus.setText("✓ Existing: " + exist.getName());
                        lblStatus.setForeground(new Color(22, 163, 74));
                    } else {
                        lblStatus.setText("New customer (will be registered)");
                        lblStatus.setForeground(new Color(37, 99, 235));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        txtPhone.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                checkPhone.run();
            }
        });
        if (prefillPhone != null && !prefillPhone.isEmpty()) {
            checkPhone.run();
        }

        JPanel footer = new JPanel(new BorderLayout(8, 8));
        footer.setBackground(new Color(248, 250, 252));
        footer.setBorder(new EmptyBorder(10, 16, 12, 16));

        JButton btnSkip = new JButton("Skip (Walk-in Customer)");
        btnSkip.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSkip.setToolTipText("Proceed as Walk-in Customer without saving details (Esc)");

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtns.setOpaque(false);
        JButton btnCancel = new JButton("Cancel");
        JButton btnContinue = new JButton("Save & Bill (Enter)");
        btnContinue.setBackground(new Color(37, 99, 235));
        btnContinue.setForeground(Color.WHITE);
        btnContinue.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnContinue.setFocusPainted(false);
        rightBtns.add(btnCancel);
        rightBtns.add(btnContinue);

        footer.add(btnSkip, BorderLayout.WEST);
        footer.add(rightBtns, BorderLayout.EAST);
        dlg.add(footer, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> {
            res.cancelled = true;
            dlg.dispose();
        });

        btnSkip.addActionListener(e -> {
            res.cancelled = false;
            res.customer = null;
            dlg.dispose();
        });

        ActionListener proceedAction = e -> {
            String p = txtPhone.getText().trim();
            String n = txtName.getText().trim();

            if (p.isEmpty() && n.isEmpty()) {
                res.cancelled = false;
                res.customer = null;
                dlg.dispose();
                return;
            }

            try {
                Customer existing = null;
                if (!p.isEmpty()) {
                    existing = customerDAO.getCustomerByPhone(p);
                }
                if (existing == null && !n.isEmpty()) {
                    List<Customer> matches = customerDAO.searchCustomers(n);
                    for (Customer mc : matches) {
                        if (mc.getName().equalsIgnoreCase(n)) {
                            existing = mc;
                            break;
                        }
                    }
                }

                if (existing != null) {
                    res.customer = existing;
                } else {
                    Customer newCust = new Customer();
                    newCust.setName(n.isEmpty() ? "Cust " + p : n);
                    newCust.setPhone(p);
                    newCust.setEmail("");
                    newCust.setAddress("");
                    customerDAO.addCustomer(newCust);
                    res.customer = newCust;
                }

                loadCustomers();
                if (res.customer != null) {
                    for (int i = 0; i < cmbCustomer.getItemCount(); i++) {
                        Customer item = cmbCustomer.getItemAt(i);
                        if (item.getId() == res.customer.getId()) {
                            isUpdatingCustomerCombo = true;
                            cmbCustomer.setSelectedIndex(i);
                            isUpdatingCustomerCombo = false;
                            break;
                        }
                    }
                    txtCustPhone.setText(res.customer.getPhone() != null ? res.customer.getPhone() : "");
                }

                res.cancelled = false;
                dlg.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Error saving customer: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        btnContinue.addActionListener(proceedAction);
        txtName.addActionListener(proceedAction);
        txtPhone.addActionListener(e -> {
            if (!txtPhone.getText().trim().isEmpty() && txtName.getText().trim().isEmpty()) {
                checkPhone.run();
                txtName.requestFocus();
            } else {
                proceedAction.actionPerformed(e);
            }
        });

        dlg.getRootPane().setDefaultButton(btnContinue);
        dlg.getRootPane().registerKeyboardAction(
                e -> {
                    res.cancelled = false;
                    res.customer = null;
                    dlg.dispose();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        SwingUtilities.invokeLater(() -> {
            if (prefillPhone != null && !prefillPhone.isEmpty()) {
                txtName.requestFocusInWindow();
            } else {
                txtPhone.requestFocusInWindow();
            }
        });

        dlg.setVisible(true);
        return res;
    }

    public void completeSale() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty! Add products before checking out.", "Empty Cart", JOptionPane.WARNING_MESSAGE);
            focusBarcodeField();
            return;
        }

        Customer cust = (Customer) cmbCustomer.getSelectedItem();
        Integer custId = (cust != null && cust.getId() > 0) ? cust.getId() : null;
        String custName = cust != null ? cust.getName() : "Walk-in Customer";
        String custPhone = (cust != null && cust.getPhone() != null) ? cust.getPhone().trim() : txtCustPhone.getText().trim();

        // If customer is currently Walk-in Customer (custId == null), ask cashier for customer details
        if (custId == null || custId == 0) {
            CustomerPromptResult promptRes = promptCustomerDetailsBeforeCheckout(custPhone);
            if (promptRes.cancelled) {
                return;
            }
            if (promptRes.customer != null) {
                cust = promptRes.customer;
                custId = cust.getId();
                custName = cust.getName();
                custPhone = cust.getPhone() != null ? cust.getPhone() : "";
            } else {
                custName = "Walk-in Customer";
                custId = null;
                custPhone = "";
            }
        }

        Sale sale = new Sale();
        sale.setInvoiceNo(txtInvoiceNo.getText().trim());
        sale.setCustomerId(custId);
        sale.setCustomerName(custName);
        sale.setCustomerPhone(custPhone);
        sale.setSubtotal(subtotal);
        sale.setGstRate(gstRate);
        sale.setGstAmount(gstAmount);
        sale.setDiscountAmount(Math.max(0, subtotal - (grandTotal - gstAmount)));
        sale.setDiscountType(discountType);
        sale.setTotalAmount(grandTotal);
        sale.setPaymentMode((String) cmbPaymentMode.getSelectedItem());
        if (currentUser != null) {
            sale.setCreatedBy(currentUser.getId());
            sale.setCashierName(currentUser.getFullName());
        }
        sale.setItems(new ArrayList<>(cartItems));

        try {
            boolean ok = billingDAO.processSale(sale);
            if (ok) {
                Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
                InvoiceDialog dialog = new InvoiceDialog(owner, sale);
                dialog.setVisible(true);

                resetBillingDesk();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error completing sale: " + ex.getMessage(), "Billing Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
