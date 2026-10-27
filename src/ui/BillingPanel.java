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
import util.ProductImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
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
    private boolean isUpdatingPaymentMode = false;

    // Live Auto-Suggestion Popup for Barcode / Product Search
    private JPopupMenu suggestionsPopup;
    private JList<Product> lstSuggestions;
    private DefaultListModel<Product> suggestionsModel;
    private JLabel lblSuggestionsHeader;
    private JLabel lblSuggestionsFooter;
    private boolean isProgrammaticSearchUpdate = false;

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
                lblTouchBadge.setText(" 📱 TOUCH SCREEN MODE ");
                lblTouchBadge.setBackground(AppTheme.ELECTRIC_LIME);
                lblTouchBadge.setForeground(AppTheme.FOREST_DEEP);
            } else {
                lblTouchBadge.setText(" 💻 NON-TOUCH KEYBOARD MODE (F1-F6) ");
                lblTouchBadge.setBackground(AppTheme.FOREST_GREEN);
                lblTouchBadge.setForeground(Color.WHITE);
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
        // TOP 1: Mall POS Header Bar (Store info, shortcuts banner, held bill bar)
        JPanel topContainer = new JPanel(new BorderLayout(8, 8));
        topContainer.setOpaque(false);

        // Header Strip
        JPanel headerStrip = new JPanel(new BorderLayout(10, 5));
        headerStrip.setBackground(AppTheme.FOREST_DEEP);
        headerStrip.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerLeft.setOpaque(false);
        JLabel lblTerminal = new JLabel("⚡ POS TERMINAL - DESK #1");
        lblTerminal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTerminal.setForeground(Color.WHITE);
        headerLeft.add(lblTerminal);

        lblTouchBadge = new JLabel(" 📱 TOUCH MODE ");
        lblTouchBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTouchBadge.setOpaque(true);
        lblTouchBadge.setBackground(AppTheme.ELECTRIC_LIME);
        lblTouchBadge.setForeground(AppTheme.FOREST_DEEP);
        lblTouchBadge.setBorder(new EmptyBorder(2, 6, 2, 6));
        headerLeft.add(lblTouchBadge);
        headerStrip.add(headerLeft, BorderLayout.WEST);

        JLabel lblShortcuts = new JLabel("[ F1: Search/Barcode | F7: Camera Scan | F2: Hold | F3: Customer | F4: Cash | F6: UPI | Ctrl+Enter: Pay ]");
        lblShortcuts.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblShortcuts.setForeground(new Color(148, 163, 184));
        lblShortcuts.setHorizontalAlignment(SwingConstants.CENTER);
        headerStrip.add(lblShortcuts, BorderLayout.CENTER);

        // Held Bills Toolbar
        heldBillsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        heldBillsPanel.setOpaque(false);
        JLabel lblHeld = new JLabel("Held Bills (Bed/Table):");
        lblHeld.setForeground(new Color(203, 213, 225));
        lblHeld.setFont(new Font("Segoe UI", Font.BOLD, 11));
        heldBillsPanel.add(lblHeld);
        // set style
        // set style

        cmbHeldBills = new JComboBox<>(new String[]{"-- Active Bill --"});
        cmbHeldBills.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cmbHeldBills.setPreferredSize(new Dimension(170, 26));
        heldBillsPanel.add(cmbHeldBills);

        JButton btnResumeBill = new JButton("Recall");
        btnResumeBill.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnResumeBill.setBackground(new Color(37, 99, 235));
        btnResumeBill.setForeground(Color.WHITE);
        btnResumeBill.setMargin(new Insets(2, 6, 2, 6));
        heldBillsPanel.add(btnResumeBill);

        JButton btnHoldBill = new JButton("Hold (F2)");
        btnHoldBill.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnHoldBill.setBackground(new Color(217, 119, 6));
        btnHoldBill.setForeground(Color.WHITE);
        btnHoldBill.setMargin(new Insets(2, 6, 2, 6));
        heldBillsPanel.add(btnHoldBill);

        JButton btnHistory = new JButton("📜 Bill History");
        btnHistory.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnHistory.setBackground(new Color(241, 245, 249));
        btnHistory.setForeground(new Color(30, 41, 59));
        btnHistory.setMargin(new Insets(2, 8, 2, 8));
        btnHistory.setFocusable(false);
        btnHistory.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnHistory.setToolTipText("View Total Orders & Sales History");
        btnHistory.addActionListener(e -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            if (owner instanceof DashboardFrame) {
                ((DashboardFrame) owner).showBillHistory();
            }
        });
        heldBillsPanel.add(btnHistory);

        JButton btnSelfCheckout = new JButton("🛒 Self Checkout");
        btnSelfCheckout.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnSelfCheckout.setBackground(new Color(16, 185, 129));
        btnSelfCheckout.setForeground(Color.WHITE);
        btnSelfCheckout.setMargin(new Insets(2, 8, 2, 8));
        btnSelfCheckout.setFocusable(false);
        btnSelfCheckout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSelfCheckout.setToolTipText("Launch Customer Self-Checkout Express");
        btnSelfCheckout.addActionListener(e -> {
            new SelfCheckoutFrame(currentUser).setVisible(true);
        });
        heldBillsPanel.add(btnSelfCheckout);

        headerStrip.add(heldBillsPanel, BorderLayout.EAST);
        topContainer.add(headerStrip, BorderLayout.NORTH);

        // Sub-header controls (Invoice #, Customer, Payment Mode, Cashier)
        JPanel metaPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        metaPanel.setBackground(Color.WHITE);
        metaPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(8, 10, 8, 10)
        ));

        // 1. Invoice No
        JPanel pInv = new JPanel(new BorderLayout(4, 2));
        pInv.setBackground(Color.WHITE);
        pInv.add(new JLabel("Invoice No:"), BorderLayout.NORTH);
        txtInvoiceNo = new JTextField();
        txtInvoiceNo.setEditable(false);
        txtInvoiceNo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtInvoiceNo.setForeground(new Color(37, 99, 235));
        pInv.add(txtInvoiceNo, BorderLayout.CENTER);
        metaPanel.add(pInv);

        // 2. Customer
        JPanel pCust = new JPanel(new BorderLayout(4, 2));
        pCust.setBackground(Color.WHITE);
        JPanel pCustHeader = new JPanel(new BorderLayout(4, 0));
        pCustHeader.setOpaque(false);
        JLabel lblCust = new JLabel("Customer (F3):");
        lblCust.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pCustHeader.add(lblCust, BorderLayout.WEST);

        JButton btnQuickAdd = new JButton("+ New");
        btnQuickAdd.setFont(new Font("Segoe UI", Font.BOLD, 10));
        btnQuickAdd.setForeground(new Color(37, 99, 235));
        btnQuickAdd.setBackground(new Color(239, 246, 255));
        btnQuickAdd.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(191, 219, 254), 1),
                new EmptyBorder(1, 6, 1, 6)
        ));
        btnQuickAdd.setFocusable(false);
        btnQuickAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnQuickAdd.setToolTipText("Quick Add New Customer");
        btnQuickAdd.addActionListener(e -> openQuickAddCustomerDialog(txtCustPhone.getText().trim()));
        pCustHeader.add(btnQuickAdd, BorderLayout.EAST);
        pCust.add(pCustHeader, BorderLayout.NORTH);

        cmbCustomer = new JComboBox<>();
        pCust.add(cmbCustomer, BorderLayout.CENTER);
        metaPanel.add(pCust);

        // 3. Customer Mobile Quick Search
        JPanel pPhone = new JPanel(new BorderLayout(4, 2));
        pPhone.setBackground(Color.WHITE);
        pPhone.add(new JLabel("Mobile / Search (Enter):"), BorderLayout.NORTH);
        txtCustPhone = new JTextField();
        txtCustPhone.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtCustPhone.setToolTipText("Enter customer mobile to search or register (Press Enter)");
        pPhone.add(txtCustPhone, BorderLayout.CENTER);
        metaPanel.add(pPhone);

        // 4. Payment Mode
        JPanel pMode = new JPanel(new BorderLayout(4, 2));
        pMode.setBackground(Color.WHITE);
        pMode.add(new JLabel("Payment Mode:"), BorderLayout.NORTH);
        cmbPaymentMode = new JComboBox<>(new String[]{"CASH", "UPI", "CARD", "CREDIT"});
        pMode.add(cmbPaymentMode, BorderLayout.CENTER);
        metaPanel.add(pMode);

        topContainer.add(metaPanel, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        // CENTER: Left Barcode & Cart Table (POS Desk)
        JPanel leftCenter = new JPanel(new BorderLayout(8, 8));
        leftCenter.setOpaque(false);

        // Fast Barcode Scanner input box
        scanBar = new JPanel(new BorderLayout(8, 0));
        scanBar.setBackground(Color.WHITE);
        scanBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(37, 99, 235), 2, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblScanIcon = new JLabel("🏷️ BARCODE / SEARCH (F1): ");
        lblScanIcon.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblScanIcon.setForeground(new Color(30, 41, 59));
        scanBar.add(lblScanIcon, BorderLayout.WEST);

        txtBarcode = new JTextField();
        txtBarcode.setFont(new Font("Segoe UI", Font.BOLD, 15));
        txtBarcode.setToolTipText("Type product name, code or scan barcode (Live suggestions popup enabled)");

        initSuggestionsPopup();

        barcodeScanTimer = new Timer(250, e -> {
            String typed = txtBarcode.getText() == null ? "" : txtBarcode.getText().trim();
            if (!typed.isEmpty() && typed.matches("^\\d{8,14}$")) {
                handleBarcodeScan(false);
            }
        });
        barcodeScanTimer.setRepeats(false);

        txtBarcode.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                scheduleBarcodeAutoScan();
                onBarcodeSearchInputChanged();
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                scheduleBarcodeAutoScan();
                onBarcodeSearchInputChanged();
            }
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                scheduleBarcodeAutoScan();
                onBarcodeSearchInputChanged();
            }
        });

        txtBarcode.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (suggestionsPopup != null && suggestionsPopup.isVisible() && lstSuggestions.isVisible()) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        int cur = lstSuggestions.getSelectedIndex();
                        int next = Math.min(suggestionsModel.size() - 1, cur + 1);
                        lstSuggestions.setSelectedIndex(next);
                        lstSuggestions.ensureIndexIsVisible(next);
                        e.consume();
                        return;
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        int cur = lstSuggestions.getSelectedIndex();
                        int prev = Math.max(0, cur - 1);
                        lstSuggestions.setSelectedIndex(prev);
                        lstSuggestions.ensureIndexIsVisible(prev);
                        e.consume();
                        return;
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        hideSuggestionsPopup();
                        e.consume();
                        return;
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        if (lstSuggestions.getSelectedIndex() >= 0) {
                            selectAndAddHighlightedProduct();
                            e.consume();
                            return;
                        }
                    }
                }

                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    hideSuggestionsPopup();
                    handleBarcodeScan(true);
                    e.consume();
                }
            }
        });

        txtBarcode.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                Timer t = new Timer(220, evt -> {
                    if (suggestionsPopup != null && suggestionsPopup.isVisible()) {
                        Window active = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
                        if (active != null && !active.isAncestorOf(txtBarcode)) {
                            hideSuggestionsPopup();
                        }
                    }
                });
                t.setRepeats(false);
                t.start();
            }
        });

        scanBar.add(txtBarcode, BorderLayout.CENTER);

        JPanel scanEastActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        scanEastActions.setOpaque(false);

        btnOpenScanner = new JButton("📷 Scan Barcode (F7)");
        btnOpenScanner.setBackground(new Color(37, 99, 235));
        btnOpenScanner.setForeground(Color.WHITE);
        btnOpenScanner.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnOpenScanner.setFocusPainted(false);
        scanEastActions.add(btnOpenScanner);

        btnScanAdd = new JButton("➕ Add Item ↵");
        btnScanAdd.setBackground(new Color(16, 185, 129));
        btnScanAdd.setForeground(Color.WHITE);
        btnScanAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnScanAdd.setFocusPainted(false);
        scanEastActions.add(btnScanAdd);

        scanBar.add(scanEastActions, BorderLayout.EAST);

        leftCenter.add(scanBar, BorderLayout.NORTH);

        // Cart Table
        String[] cartCols = {"#", "Code / Barcode", "Product Name", "Rate (₹)", "Qty", "Total (₹)", "Action"};
        cartModel = new DefaultTableModel(cartCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 4; // allow direct inline quantity editing
            }
        };
        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(32);
        cartTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cartTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Column widths
        cartTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(260);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        cartTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        cartTable.getColumnModel().getColumn(6).setPreferredWidth(70);

        // Center align Qty and Price
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        cartTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        cartTable.getColumnModel().getColumn(4).setCellRenderer(centerRender);

        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
        cartTable.getColumnModel().getColumn(3).setCellRenderer(rightRender);
        cartTable.getColumnModel().getColumn(5).setCellRenderer(rightRender);

        JScrollPane tableScroll = new JScrollPane(cartTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        leftCenter.add(tableScroll, BorderLayout.CENTER);

        // Cart status footer (Total items, units, delete button)
        JPanel cartFooter = new JPanel(new BorderLayout(10, 0));
        cartFooter.setBackground(Color.WHITE);
        cartFooter.setBorder(new EmptyBorder(6, 12, 6, 12));

        JPanel statsLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        statsLeft.setOpaque(false);
        lblItemsCount = new JLabel("Items: 0");
        lblItemsCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblItemsCount.setForeground(new Color(71, 85, 105));
        statsLeft.add(lblItemsCount);

        lblTotalQty = new JLabel("Total Qty: 0");
        lblTotalQty.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTotalQty.setForeground(new Color(71, 85, 105));
        statsLeft.add(lblTotalQty);
        cartFooter.add(statsLeft, BorderLayout.WEST);

        cartActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cartActionsPanel.setOpaque(false);

        JButton btnIncQty = new JButton("➕ Qty +1");
        btnIncQty.setFont(new Font("Segoe UI", Font.BOLD, 11));
        cartActionsPanel.add(btnIncQty);

        JButton btnDecQty = new JButton("➖ Qty -1");
        btnDecQty.setFont(new Font("Segoe UI", Font.BOLD, 11));
        cartActionsPanel.add(btnDecQty);

        JButton btnRemove = new JButton("🗑 Remove");
        btnRemove.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnRemove.setBackground(new Color(239, 68, 68));
        btnRemove.setForeground(Color.WHITE);
        cartActionsPanel.add(btnRemove);

        JButton btnClear = new JButton("Clear All");
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cartActionsPanel.add(btnClear);

        btnQuickCheckout = new JButton("💳 Pay & Print (Ctrl+Enter)");
        btnQuickCheckout.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnQuickCheckout.setBackground(AppTheme.FOREST_GREEN);
        btnQuickCheckout.setForeground(Color.WHITE);
        btnQuickCheckout.setFocusPainted(false);
        cartActionsPanel.add(btnQuickCheckout);

        cartFooter.add(cartActionsPanel, BorderLayout.EAST);
        leftCenter.add(cartFooter, BorderLayout.SOUTH);

        add(leftCenter, BorderLayout.CENTER);

        // RIGHT: Mall POS Checkout Hub (Totals, Discount, Numpad, Quick Cash, UPI, Checkout)
        JPanel rightHub = new JPanel(new BorderLayout(10, 10));
        rightHub.setPreferredSize(new Dimension(360, 0));
        rightHub.setOpaque(false);

        // 1. Total Bill Display Card
        JPanel totalDisplayCard = new JPanel(new BorderLayout(5, 5));
        totalDisplayCard.setBackground(AppTheme.FOREST_DEEP);
        totalDisplayCard.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel lblPayableTitle = new JLabel("TOTAL PAYABLE");
        lblPayableTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblPayableTitle.setForeground(AppTheme.BORDER_SAGE);
        totalDisplayCard.add(lblPayableTitle, BorderLayout.NORTH);

        lblGrandTotal = new JLabel("₹0.00");
        lblGrandTotal.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblGrandTotal.setForeground(AppTheme.ELECTRIC_LIME);
        totalDisplayCard.add(lblGrandTotal, BorderLayout.CENTER);

        JPanel subBreakdown = new JPanel(new GridLayout(3, 2, 4, 2));
        subBreakdown.setOpaque(false);

        subBreakdown.add(createWhiteLabel("Subtotal:"));
        lblSubtotal = createWhiteLabel("₹0.00", SwingConstants.RIGHT);
        subBreakdown.add(lblSubtotal);

        subBreakdown.add(createWhiteLabel("Discount:"));
        lblDiscount = createWhiteLabel("₹0.00", SwingConstants.RIGHT);
        lblDiscount.setForeground(new Color(248, 113, 113));
        subBreakdown.add(lblDiscount);

        subBreakdown.add(createWhiteLabel("GST (18%):"));
        lblGstAmount = createWhiteLabel("₹0.00", SwingConstants.RIGHT);
        subBreakdown.add(lblGstAmount);

        totalDisplayCard.add(subBreakdown, BorderLayout.SOUTH);
        rightHub.add(totalDisplayCard, BorderLayout.NORTH);

        // 2. Middle: Quick Cash / Tendered & Discount Controls
        JPanel midPanel = new JPanel();
        midPanel.setLayout(new BoxLayout(midPanel, BoxLayout.Y_AXIS));
        midPanel.setOpaque(false);

        // Discount Toolbar
        discountBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        discountBar.setBackground(Color.WHITE);
        discountBar.setBorder(BorderFactory.createTitledBorder("🏷️ Bill Discount (Ctrl+D)"));

        btnAddDiscount = new JButton("Apply Discount");
        btnAddDiscount.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnAddDiscount.setBackground(new Color(241, 245, 249));
        discountBar.add(btnAddDiscount);

        btnRemoveDiscount = new JButton("Remove");
        btnRemoveDiscount.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        discountBar.add(btnRemoveDiscount);

        btnOpenUpi = new JButton("📱 UPI QR (F6)");
        btnOpenUpi.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnOpenUpi.setBackground(new Color(139, 92, 246));
        btnOpenUpi.setForeground(Color.WHITE);
        discountBar.add(btnOpenUpi);

        midPanel.add(discountBar);
        midPanel.add(Box.createVerticalStrut(6));

        // Quick Cash Chips (₹100, ₹200, ₹500, ₹2000, Exact)
        quickCashPanel = new JPanel(new GridLayout(2, 3, 5, 5));
        quickCashPanel.setBackground(Color.WHITE);
        quickCashPanel.setBorder(BorderFactory.createTitledBorder("💵 Quick Cash Tendered"));

        int[] cashPresets = {100, 200, 500, 1000, 2000};
        for (int p : cashPresets) {
            JButton btnChip = new JButton("₹" + p);
            btnChip.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnChip.setBackground(new Color(248, 250, 252));
            btnChip.addActionListener(e -> setTenderedCash(p));
            quickCashPanel.add(btnChip);
        }
        JButton btnExact = new JButton("EXACT");
        btnExact.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnExact.setBackground(new Color(220, 252, 231));
        btnExact.setForeground(new Color(22, 101, 52));
        btnExact.addActionListener(e -> setTenderedCash(grandTotal));
        quickCashPanel.add(btnExact);

        midPanel.add(quickCashPanel);
        midPanel.add(Box.createVerticalStrut(6));

        // Cash Paid & Change Due Banner
        JPanel tenderCard = new JPanel(new GridLayout(2, 2, 8, 4));
        tenderCard.setBackground(Color.WHITE);
        tenderCard.setBorder(new EmptyBorder(8, 10, 8, 10));

        JLabel lblCashTitle = new JLabel("Cash Paid (₹):");
        lblCashTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tenderCard.add(lblCashTitle);

        txtCashPaid = new JTextField("0.00");
        txtCashPaid.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtCashPaid.setHorizontalAlignment(JTextField.RIGHT);
        tenderCard.add(txtCashPaid);

        JLabel lblChangeTitle = new JLabel("Change Return:");
        lblChangeTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblChangeTitle.setForeground(new Color(220, 38, 38));
        tenderCard.add(lblChangeTitle);

        lblChangeDue = new JLabel("₹0.00", SwingConstants.RIGHT);
        lblChangeDue.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblChangeDue.setForeground(new Color(22, 163, 74));
        tenderCard.add(lblChangeDue);

        midPanel.add(tenderCard);
        midPanel.add(Box.createVerticalStrut(6));

        // Touch Numpad (7-8-9, 4-5-6, 1-2-3, 0-.-C)
        numpadPanel = new JPanel(new GridLayout(4, 3, 4, 4));
        numpadPanel.setBackground(Color.WHITE);
        numpadPanel.setBorder(BorderFactory.createTitledBorder("🔢 POS Numpad / NUPED Buttons"));

        String[] keys = {"7", "8", "9", "4", "5", "6", "1", "2", "3", "C", "0", "."};
        for (String k : keys) {
            JButton btnKey = new JButton(k);
            btnKey.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnKey.setBackground(new Color(248, 250, 252));
            btnKey.setFocusPainted(false);
            btnKey.addActionListener(e -> handleNumpadKey(k));
            numpadPanel.add(btnKey);
        }
        midPanel.add(numpadPanel);

        rightHub.add(midPanel, BorderLayout.CENTER);

        // 3. Bottom Big Checkout Button
        JPanel bottomCheckout = new JPanel(new GridLayout(1, 1));
        bottomCheckout.setOpaque(false);
        JButton btnPayPrint = new JButton("💳 PAY & PRINT BILL (Ctrl+Enter)");
        btnPayPrint.setBackground(AppTheme.FOREST_GREEN);
        btnPayPrint.setForeground(Color.WHITE);
        btnPayPrint.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnPayPrint.setPreferredSize(new Dimension(0, 48));
        btnPayPrint.setFocusPainted(false);
        btnPayPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        bottomCheckout.add(btnPayPrint);

        rightHub.add(bottomCheckout, BorderLayout.SOUTH);
        add(rightHub, BorderLayout.EAST);

        // LISTENERS
        btnOpenScanner.addActionListener(e -> openBarcodeScannerDialog());
        btnScanAdd.addActionListener(e -> {
            if (suggestionsPopup != null && suggestionsPopup.isVisible() && lstSuggestions.isVisible() && lstSuggestions.getSelectedIndex() >= 0) {
                selectAndAddHighlightedProduct();
            } else {
                handleBarcodeScan(true);
            }
        });
        btnQuickCheckout.addActionListener(e -> completeSale());

        txtBarcode.addActionListener(e -> {
            if (suggestionsPopup != null && suggestionsPopup.isVisible() && lstSuggestions.isVisible() && lstSuggestions.getSelectedIndex() >= 0) {
                selectAndAddHighlightedProduct();
                return;
            }
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
                hideSuggestionsPopup();
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
            if (isUpdatingPaymentMode) return;
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

            if ("UPI".equalsIgnoreCase(mode) && !cartItems.isEmpty()) {
                openUpiDialog();
            }
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

    // =========================================================================
    // LIVE AUTO-SUGGESTION POPUP FOR BARCODE / PRODUCT SEARCH
    // =========================================================================
    private void initSuggestionsPopup() {
        suggestionsPopup = new JPopupMenu();
        suggestionsPopup.setLayout(new BorderLayout());
        suggestionsPopup.setFocusable(false);
        suggestionsPopup.setBackground(Color.WHITE);
        suggestionsPopup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(4, 4, 4, 4)
        ));

        JPanel popupContent = new JPanel(new BorderLayout(0, 4));
        popupContent.setOpaque(false);

        // Header
        lblSuggestionsHeader = new JLabel("  🔍 MATCHING PRODUCTS");
        lblSuggestionsHeader.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblSuggestionsHeader.setForeground(new Color(71, 85, 105));
        lblSuggestionsHeader.setPreferredSize(new Dimension(0, 22));
        popupContent.add(lblSuggestionsHeader, BorderLayout.NORTH);

        // JList with custom renderer
        suggestionsModel = new DefaultListModel<>();
        lstSuggestions = new JList<>(suggestionsModel);
        lstSuggestions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lstSuggestions.setFocusable(false);
        lstSuggestions.setFixedCellHeight(52);
        lstSuggestions.setCellRenderer(new ProductSuggestionCellRenderer());

        // Mouse hover selection
        lstSuggestions.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int idx = lstSuggestions.locationToIndex(e.getPoint());
                if (idx >= 0 && idx != lstSuggestions.getSelectedIndex()) {
                    lstSuggestions.setSelectedIndex(idx);
                }
            }
        });

        // Mouse click to add
        lstSuggestions.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectAndAddHighlightedProduct();
            }
        });

        JScrollPane scroll = new JScrollPane(lstSuggestions);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(241, 245, 249), 1));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        popupContent.add(scroll, BorderLayout.CENTER);

        // Footer
        lblSuggestionsFooter = new JLabel("  💡 Use ↑ / ↓ arrow keys to select, press Enter to Add, Esc to close");
        lblSuggestionsFooter.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblSuggestionsFooter.setForeground(new Color(100, 116, 139));
        lblSuggestionsFooter.setPreferredSize(new Dimension(0, 22));
        popupContent.add(lblSuggestionsFooter, BorderLayout.SOUTH);

        suggestionsPopup.add(popupContent, BorderLayout.CENTER);
    }

    private void onBarcodeSearchInputChanged() {
        if (isProgrammaticSearchUpdate) return;

        String raw = txtBarcode.getText();
        if (raw == null || raw.trim().isEmpty()) {
            hideSuggestionsPopup();
            return;
        }

        String query = raw.trim();
        if (query.isEmpty()) {
            hideSuggestionsPopup();
            return;
        }

        if (cachedProducts.isEmpty()) {
            loadProductCache();
        }

        List<Product> matches = findMatchingProducts(query);
        if (matches.isEmpty()) {
            try {
                List<Product> dbMatches = productDAO.searchProducts(query);
                if (!dbMatches.isEmpty()) {
                    matches = dbMatches;
                }
            } catch (Exception ignored) {}
        }

        showSuggestionsPopup(query, matches);
    }

    public List<Product> findMatchingProducts(String query) {
        List<Product> result = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return result;

        String qLower = query.trim().toLowerCase();
        String[] tokens = qLower.split("\\s+");

        for (Product p : cachedProducts) {
            String nameLower = p.getName() != null ? p.getName().toLowerCase() : "";
            String codeLower = p.getCode() != null ? p.getCode().toLowerCase() : "";
            String barLower = p.getBarcode() != null ? p.getBarcode().toLowerCase() : "";
            String catLower = p.getCategoryName() != null ? p.getCategoryName().toLowerCase() : "";

            boolean exactOrPrefix = codeLower.startsWith(qLower) || barLower.startsWith(qLower) || nameLower.startsWith(qLower);

            boolean tokensMatch = true;
            for (String tok : tokens) {
                if (!nameLower.contains(tok) && !codeLower.contains(tok) && !barLower.contains(tok) && !catLower.contains(tok)) {
                    tokensMatch = false;
                    break;
                }
            }

            if (exactOrPrefix || tokensMatch) {
                result.add(p);
                if (result.size() >= 12) break;
            }
        }
        return result;
    }

    private void showSuggestionsPopup(String query, List<Product> matches) {
        if (suggestionsPopup == null) {
            initSuggestionsPopup();
        }

        suggestionsModel.clear();
        for (Product p : matches) {
            suggestionsModel.addElement(p);
        }

        if (matches.isEmpty()) {
            lblSuggestionsHeader.setText("  🔍 NO PRODUCTS FOUND FOR: \"" + query + "\"");
            lblSuggestionsHeader.setForeground(new Color(220, 38, 38));
            lblSuggestionsFooter.setText("  ⚠️ Check spelling or press Enter to scan barcode code directly");
            lstSuggestions.setVisible(false);
            suggestionsPopup.setPreferredSize(new Dimension(Math.max(txtBarcode.getWidth(), 520), 75));
        } else {
            lblSuggestionsHeader.setText("  🔍 MATCHING PRODUCTS (" + matches.size() + " FOUND)");
            lblSuggestionsHeader.setForeground(new Color(30, 41, 59));
            lblSuggestionsFooter.setText("  💡 Use ↑ / ↓ arrow keys to select, press Enter to Add, Esc to close");
            lstSuggestions.setVisible(true);
            lstSuggestions.setSelectedIndex(0);

            int visibleRows = Math.min(matches.size(), 5);
            int height = 52 + (visibleRows * 54);
            int width = Math.max(txtBarcode.getWidth(), 560);
            suggestionsPopup.setPreferredSize(new Dimension(width, height));
        }

        if (txtBarcode.isShowing()) {
            suggestionsPopup.pack();
            suggestionsPopup.show(txtBarcode, 0, txtBarcode.getHeight() + 3);
            txtBarcode.requestFocusInWindow();
        }
    }

    private void hideSuggestionsPopup() {
        if (suggestionsPopup != null && suggestionsPopup.isVisible()) {
            suggestionsPopup.setVisible(false);
        }
    }

    private void selectAndAddHighlightedProduct() {
        if (lstSuggestions != null && lstSuggestions.getSelectedIndex() >= 0 && lstSuggestions.isVisible()) {
            Product selected = lstSuggestions.getSelectedValue();
            if (selected != null) {
                hideSuggestionsPopup();
                addProductToCart(selected, 1);
                isProgrammaticSearchUpdate = true;
                txtBarcode.setText("");
                isProgrammaticSearchUpdate = false;
                focusBarcodeField();
            }
        }
    }

    private class ProductSuggestionCellRenderer extends JPanel implements ListCellRenderer<Product> {
        private JLabel lblIcon = new JLabel();
        private JLabel lblName = new JLabel();
        private JLabel lblMeta = new JLabel();
        private JLabel lblPrice = new JLabel();
        private JLabel lblStock = new JLabel();
        private JLabel lblActionPill = new JLabel();

        public ProductSuggestionCellRenderer() {
            setLayout(new BorderLayout(10, 0));
            setBorder(new EmptyBorder(6, 10, 6, 10));
            setOpaque(true);

            lblIcon.setPreferredSize(new Dimension(36, 36));
            add(lblIcon, BorderLayout.WEST);

            JPanel pnlCenter = new JPanel();
            pnlCenter.setLayout(new BoxLayout(pnlCenter, BoxLayout.Y_AXIS));
            pnlCenter.setOpaque(false);

            lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblMeta.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            pnlCenter.add(lblName);
            pnlCenter.add(Box.createVerticalStrut(2));
            pnlCenter.add(lblMeta);
            add(pnlCenter, BorderLayout.CENTER);

            JPanel pnlRight = new JPanel(new GridLayout(2, 1, 2, 2));
            pnlRight.setOpaque(false);

            JPanel rowTop = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            rowTop.setOpaque(false);
            lblPrice.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblPrice.setForeground(new Color(22, 101, 52));
            lblActionPill.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lblActionPill.setOpaque(true);
            lblActionPill.setBorder(new EmptyBorder(2, 8, 2, 8));
            rowTop.add(lblPrice);
            rowTop.add(lblActionPill);
            pnlRight.add(rowTop);

            JPanel rowBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            rowBottom.setOpaque(false);
            lblStock.setFont(new Font("Segoe UI", Font.BOLD, 11));
            rowBottom.add(lblStock);
            pnlRight.add(rowBottom);

            add(pnlRight, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Product> list, Product p, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            if (p == null) return this;

            lblIcon.setIcon(ProductImageUtil.getProductIcon(p, 34, 34));

            lblName.setText(p.getName());
            String cat = p.getCategoryName() != null && !p.getCategoryName().isEmpty() ? p.getCategoryName() : "General";
            String bCode = p.getBarcode() != null && !p.getBarcode().isEmpty() ? p.getBarcode() : p.getCode();
            lblMeta.setText(p.getCode() + " • Barcode: " + bCode + " • " + cat);

            lblPrice.setText(String.format("₹%.2f", p.getSellingPrice()));

            int qty = p.getQuantity();
            if (qty <= 0) {
                lblStock.setText("⚠️ Out of Stock (0)");
                lblStock.setForeground(new Color(220, 38, 38));
            } else if (qty <= p.getMinStockLevel()) {
                lblStock.setText("⚠️ Low Stock: " + qty + " left");
                lblStock.setForeground(new Color(217, 119, 6));
            } else {
                lblStock.setText("✓ " + qty + " in stock");
                lblStock.setForeground(new Color(22, 101, 52));
            }

            if (isSelected) {
                setBackground(new Color(239, 246, 255));
                lblName.setForeground(new Color(30, 58, 138));
                lblMeta.setForeground(new Color(59, 130, 246));
                lblActionPill.setText("↵ Enter to Add");
                lblActionPill.setBackground(new Color(37, 99, 235));
                lblActionPill.setForeground(Color.WHITE);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 4, 0, 0, new Color(37, 99, 235)),
                        new EmptyBorder(6, 6, 6, 10)
                ));
            } else {
                setBackground(index % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                lblName.setForeground(new Color(15, 23, 42));
                lblMeta.setForeground(new Color(100, 116, 139));
                lblActionPill.setText("+ Add");
                lblActionPill.setBackground(new Color(241, 245, 249));
                lblActionPill.setForeground(new Color(71, 85, 105));
                setBorder(new EmptyBorder(6, 10, 6, 10));
            }

            return this;
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

        // Phone number validation if input is digits
        String digits = q.replaceAll("[^0-9]", "");
        if (!digits.isEmpty() && q.matches("^[0-9+ -]+$")) {
            if (!q.matches("\\d+")) {
                JOptionPane.showMessageDialog(this,
                        "Phone number must contain numeric digits (0-9) only.\nPlease remove any letters, symbols or spaces.",
                        "Validation Error - Numbers Only",
                        JOptionPane.WARNING_MESSAGE);
                txtCustPhone.requestFocus();
                txtCustPhone.selectAll();
                return;
            }
            if (digits.charAt(0) < '6' || digits.charAt(0) > '9') {
                JOptionPane.showMessageDialog(this,
                        "Mobile number must start with 6, 7, 8, or 9.\n(You entered a number starting with '" + digits.charAt(0) + "').",
                        "Validation Error - Invalid Starting Digit",
                        JOptionPane.WARNING_MESSAGE);
                txtCustPhone.requestFocus();
                txtCustPhone.selectAll();
                return;
            }
            if (digits.length() > 10) {
                JOptionPane.showMessageDialog(this,
                        "Mobile number cannot exceed 10 digits!\n(You entered " + digits.length() + " digits). Please enter only 10 digits.",
                        "Validation Error - Number Too Long",
                        JOptionPane.WARNING_MESSAGE);
                txtCustPhone.requestFocus();
                txtCustPhone.selectAll();
                return;
            }
            if (digits.length() < 10) {
                JOptionPane.showMessageDialog(this,
                        "Mobile number must be exactly 10 digits!\n(You entered " + digits.length() + " digits).",
                        "Validation Error - Incomplete Number",
                        JOptionPane.WARNING_MESSAGE);
                txtCustPhone.requestFocus();
                txtCustPhone.selectAll();
                return;
            }
        }

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
        QuickAddCustomerDialog dlg = new QuickAddCustomerDialog(owner, phonePrefill);
        dlg.setVisible(true);

        Customer c = dlg.getCreatedCustomer();
        if (c != null) {
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
            focusBarcodeField();
        }
        return c;
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
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty! Add products before generating UPI QR.", "Empty Cart", JOptionPane.WARNING_MESSAGE);
            return;
        }

        isUpdatingPaymentMode = true;
        cmbPaymentMode.setSelectedItem("UPI");
        isUpdatingPaymentMode = false;

        cashTendered = 0.0;
        txtCashPaid.setText("0.00");
        updateCashCalculations();

        String storeUpi = AppSettings.getString(AppSettings.KEY_STORE_UPI_ID, "bazaarpoint@upi").trim();
        String storeName = AppSettings.getString(AppSettings.KEY_STORE_NAME, "SmartBilling Pro").trim();

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        UpiQrDialog upiDialog = new UpiQrDialog(owner, storeUpi, storeName, grandTotal, txtInvoiceNo.getText().trim());
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
            if (p.isEmpty()) {
                lblStatus.setText("Type 10-digit mobile number to search or add");
                lblStatus.setForeground(new Color(100, 116, 139));
                return;
            }
            if (!p.matches("\\d+")) {
                lblStatus.setText("⚠️ Only digits (0-9) allowed");
                lblStatus.setForeground(new Color(220, 38, 38));
                return;
            }
            if (p.charAt(0) < '6' || p.charAt(0) > '9') {
                lblStatus.setText("⚠️ Mobile number must start with 6, 7, 8, or 9");
                lblStatus.setForeground(new Color(220, 38, 38));
                return;
            }
            if (p.length() > 10) {
                lblStatus.setText("⚠️ Mobile number exceeds 10 digits (" + p.length() + "/10)");
                lblStatus.setForeground(new Color(220, 38, 38));
                return;
            }
            if (p.length() < 10) {
                lblStatus.setText("Entering mobile number: " + p.length() + "/10 digits");
                lblStatus.setForeground(new Color(100, 116, 139));
                return;
            }

            try {
                Customer exist = customerDAO.getCustomerByPhone(p);
                if (exist != null) {
                    txtName.setText(exist.getName());
                    lblStatus.setText("✓ Existing Customer: " + exist.getName());
                    lblStatus.setForeground(new Color(22, 163, 74));
                } else {
                    lblStatus.setText("✓ Valid 10-digit mobile (New Customer)");
                    lblStatus.setForeground(new Color(37, 99, 235));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };

        txtPhone.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { checkPhone.run(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { checkPhone.run(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { checkPhone.run(); }
        });

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

            if (!p.isEmpty()) {
                if (!p.matches("\\d+")) {
                    JOptionPane.showMessageDialog(dlg,
                            "Mobile number must contain numeric digits (0-9) only.\nPlease remove any letters, symbols or spaces.",
                            "Validation Error - Numbers Only",
                            JOptionPane.WARNING_MESSAGE);
                    txtPhone.requestFocus();
                    return;
                }
                if (p.charAt(0) < '6' || p.charAt(0) > '9') {
                    JOptionPane.showMessageDialog(dlg,
                            "Mobile number must start with 6, 7, 8, or 9.\n(You entered a number starting with '" + p.charAt(0) + "').",
                            "Validation Error - Invalid Starting Digit",
                            JOptionPane.WARNING_MESSAGE);
                    txtPhone.requestFocus();
                    return;
                }
                if (p.length() > 10) {
                    JOptionPane.showMessageDialog(dlg,
                            "Mobile number cannot exceed 10 digits!\n(You entered " + p.length() + " digits). Please enter only 10 digits.",
                            "Validation Error - Number Too Long",
                            JOptionPane.WARNING_MESSAGE);
                    txtPhone.requestFocus();
                    return;
                }
                if (p.length() < 10) {
                    JOptionPane.showMessageDialog(dlg,
                            "Mobile number must be exactly 10 digits!\n(You entered " + p.length() + " digits).",
                            "Validation Error - Incomplete Number",
                            JOptionPane.WARNING_MESSAGE);
                    txtPhone.requestFocus();
                    return;
                }
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
