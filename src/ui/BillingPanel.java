package ui;

import config.AppSettings;
import dao.BillingDAO;
import dao.CustomerDAO;
import dao.ProductDAO;
import model.Customer;
import model.HeldBill;
import model.Product;
import model.Sale;
import model.SaleItem;
import model.User;

import util.ProductImageUtil;
import util.QrCodeGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modern 2-Card Sale & Billing Terminal
 * Left Card:  Product Search, Available Product Cards Grid & Selection Station
 * Right Card: Live Bill Items (with inline +/- & delete) + Cost Calculations + Checkout
 * Matches exact Dashboard styling (r=36 border radius, pure white cards, borderless, pill buttons)
 */
public class BillingPanel extends JPanel {
    private final ProductDAO productDAO = new ProductDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final BillingDAO billingDAO = new BillingDAO();
    private final User currentUser;

    // Product Catalog Cards State (Left Card)
    private List<Product> allProducts = new ArrayList<>();
    private final List<Product> filteredProducts = new ArrayList<>();
    private ScrollableGridPanel productsGridPanel;
    private JScrollPane catalogScroll;
    private int gridCols = 3;
    private final List<ProductCardPanel> productCardPanels = new ArrayList<>();
    private JTextField txtProductFilter;
    private Product selectedProduct = null;

    // Camera Barcode Scanner State
    private JPanel cameraCard;
    private CardLayout cameraCardLayout;
    private JPanel cameraCardContainer;
    private JPanel cameraOffView;
    private JPanel cameraOnView;
    private boolean isCameraActive = false;
    private Process cameraProcess = null;
    private BufferedImage cameraFrame = null;
    private boolean showSuccessCheckmark = false;
    private String lastScannedTitle = "";
    private int lastScannedCartQty = 1;
    private long lastScannedTime = 0;
    private String lastScannedBarcode = "";
    private javax.swing.Timer laserScanTimer;
    private float laserScanY = 0.1f;
    private boolean laserDirectionDown = true;
    private javax.swing.Timer checkmarkTimer;

    // Bill Cart State (Right Card)
    private final List<SaleItem> cartItems = new ArrayList<>();
    private DefaultTableModel billModel;
    private JTable billTable;
    private JLabel lblBillCount;
    private JLabel lblSubtotal;
    private JLabel lblGstAmount;
    private JLabel lblGrandTotal;
    private String selectedPaymentMode = "CASH";
    private PaymentModePill btnPayCash, btnPayUpi, btnPayCard;
    private PillButton btnCheckout;

    // Header Controls
    private JLabel lblInvoicePill;
    private JLabel lblCashierPill;
    private String currentInvoiceNo = "";

    // Financial calculations
    private double subtotal = 0.0;
    private final double gstRate = 18.0;
    private double gstAmount = 0.0;
    private double grandTotal = 0.0;

    public BillingPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout(0, 16));
        setBackground(AppTheme.BG_CANVAS);
        setOpaque(true);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        initComponents();
        loadProductCache();
        loadInvoiceNo();
        calculateTotals();
        setupKeyboardShortcuts();
    }

    private void initComponents() {
        // ── TOP: Header (Clean title, plain text info, pill-shaped New Bill button) ──
        JPanel headerPanel = new JPanel(new BorderLayout(16, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0, 4, 6, 4));

        JLabel lblTitle = new JLabel("New Sale & Billing");
        lblTitle.setFont(AppTheme.font(Font.BOLD, 22));
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        headerRight.setOpaque(false);

        lblInvoicePill = new JLabel("Invoice: Loading...", new SidebarIcon("file_text", 13, AppTheme.TEXT_SECONDARY), SwingConstants.LEFT);
        lblInvoicePill.setFont(AppTheme.font(Font.BOLD, 12));
        lblInvoicePill.setForeground(AppTheme.TEXT_SECONDARY);
        lblInvoicePill.setIconTextGap(6);
        lblInvoicePill.setOpaque(false);
        headerRight.add(lblInvoicePill);

        String cashierName = currentUser != null ? currentUser.getFullName() : "Staff";
        lblCashierPill = new JLabel("Cashier: " + cashierName, new SidebarIcon("user", 13, AppTheme.TEXT_SECONDARY), SwingConstants.LEFT);
        lblCashierPill.setFont(AppTheme.font(Font.BOLD, 12));
        lblCashierPill.setForeground(AppTheme.TEXT_SECONDARY);
        lblCashierPill.setIconTextGap(6);
        lblCashierPill.setOpaque(false);
        headerRight.add(lblCashierPill);

        // Pill-shaped "New Bill" Button with '+' icon (High visibility Forest Green pill)
        PillButton btnResetDesk = PillButton.primary("New Bill", new SidebarIcon("plus", 12, Color.WHITE));
        btnResetDesk.setPreferredSize(new Dimension(115, 34));
        btnResetDesk.setToolTipText("Start a fresh bill");
        btnResetDesk.addActionListener(e -> resetBillingDesk());
        headerRight.add(btnResetDesk);

        headerPanel.add(headerRight, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // ── CENTER: Left Catalog (flexible/large) + Right Bill (compact) Layout ──
        JPanel centerGrid = new JPanel(new BorderLayout(16, 0));
        centerGrid.setOpaque(false);

        // ====================================================================
        // LEFT CARD: Product Search, Catalog & Selector
        // ====================================================================
        JPanel leftCard = createElevatedCard(new BorderLayout(0, 12));
        leftCard.setBorder(new EmptyBorder(22, 24, 22, 24));

        // 1. Top Search Row
        JPanel catalogHeader = new JPanel(new BorderLayout(10, 8));
        catalogHeader.setOpaque(false);

        JLabel lblCatTitle = new JLabel("Available Products", SwingConstants.LEFT);
        lblCatTitle.setFont(AppTheme.font(Font.BOLD, 14));
        lblCatTitle.setForeground(AppTheme.TEXT_PRIMARY);
        catalogHeader.add(lblCatTitle, BorderLayout.WEST);

        // Pill-shaped Search Input with custom rendered placeholder
        txtProductFilter = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(244, 248, 246));
                g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
                super.paintComponent(g);

                if (getText() == null || getText().isEmpty()) {
                    g2.setFont(AppTheme.font(Font.PLAIN, 12));
                    g2.setColor(new Color(156, 175, 168));
                    FontMetrics fm = g2.getFontMetrics();
                    Insets insets = getInsets();
                    int x = insets != null && insets.left > 0 ? insets.left : 16;
                    int y = ((h - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString("Search products...", x, y);
                }
                g2.dispose();
            }
        };
        txtProductFilter.setOpaque(false);
        txtProductFilter.setBorder(new EmptyBorder(4, 16, 4, 16));
        txtProductFilter.setFont(AppTheme.font(Font.PLAIN, 12));
        txtProductFilter.setToolTipText("Type to search products. Press Enter to add first matching item.");
        txtProductFilter.setPreferredSize(new Dimension(240, 32));
        txtProductFilter.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterCatalog(); }
            public void removeUpdate(DocumentEvent e) { filterCatalog(); }
            public void changedUpdate(DocumentEvent e) { filterCatalog(); }
        });
        txtProductFilter.addActionListener(e -> addFirstMatchedProduct());
        catalogHeader.add(txtProductFilter, BorderLayout.EAST);
        leftCard.add(catalogHeader, BorderLayout.NORTH);

        // 2. Center: Available Products Cards Grid
        productsGridPanel = new ScrollableGridPanel(new GridLayout(0, gridCols, 12, 12));
        productsGridPanel.setOpaque(false);
        productsGridPanel.setBorder(new EmptyBorder(8, 2, 12, 2));

        catalogScroll = new JScrollPane(productsGridPanel);
        catalogScroll.setBorder(BorderFactory.createEmptyBorder());
        catalogScroll.setViewportBorder(null);
        catalogScroll.setOpaque(false);
        catalogScroll.getViewport().setOpaque(false);
        catalogScroll.getViewport().setBorder(null);
        catalogScroll.getVerticalScrollBar().setUnitIncrement(24);

        catalogScroll.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateGridColumns();
            }
        });

        leftCard.add(catalogScroll, BorderLayout.CENTER);

        // Assemble Left Column: Camera Scanner Card on Top + Catalog Card Below
        JPanel leftColumn = new JPanel(new BorderLayout(0, 14));
        leftColumn.setOpaque(false);
        leftColumn.add(createCameraCard(), BorderLayout.NORTH);
        leftColumn.add(leftCard, BorderLayout.CENTER);

        centerGrid.add(leftColumn, BorderLayout.CENTER);

        // ====================================================================
        // RIGHT CARD: Live Bill Items + Real-time Calculations + Checkout
        // ====================================================================
        JPanel rightCard = createElevatedCard(new BorderLayout(0, 10));
        rightCard.setBorder(new EmptyBorder(18, 18, 18, 18));
        rightCard.setPreferredSize(new Dimension(410, 0));

        // 1. Bill Header
        JPanel billHeader = new JPanel(new BorderLayout(10, 0));
        billHeader.setOpaque(false);

        JLabel lblBillTitle = new JLabel("Current Bill", SwingConstants.LEFT);
        lblBillTitle.setFont(AppTheme.font(Font.BOLD, 14));
        lblBillTitle.setForeground(AppTheme.TEXT_PRIMARY);
        billHeader.add(lblBillTitle, BorderLayout.WEST);

        lblBillCount = new JLabel("0 Items (0 Units)");
        lblBillCount.setFont(AppTheme.font(Font.BOLD, 12));
        lblBillCount.setForeground(AppTheme.TEXT_MUTED);
        billHeader.add(lblBillCount, BorderLayout.EAST);
        rightCard.add(billHeader, BorderLayout.NORTH);

        // 2. Live Bill Table (With inline Qty & Direct Delete Column)
        String[] billCols = {"Product Name", "Rate (₹)", "Qty", "Total (₹)", "Del"};
        billModel = new DefaultTableModel(billCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false; // Handled directly via table mouse listener for instant, smooth clicks
            }
        };

        billTable = new JTable(billModel);
        billTable.setRowHeight(38);
        billTable.setFont(AppTheme.font(Font.PLAIN, 12));
        billTable.setShowGrid(false);
        billTable.setShowHorizontalLines(false);
        billTable.setShowVerticalLines(false);
        billTable.setIntercellSpacing(new Dimension(0, 0));
        billTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        billTable.setBackground(Color.WHITE);
        billTable.setSelectionBackground(new Color(0, 90, 61, 16));
        billTable.setSelectionForeground(AppTheme.TEXT_PRIMARY);

        billTable.setBorder(BorderFactory.createEmptyBorder());

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        rightRenderer.setBorder(new EmptyBorder(0, 6, 0, 6));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer nameRenderer = new DefaultTableCellRenderer();
        nameRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        nameRenderer.setBorder(new EmptyBorder(0, 8, 0, 6));

        TableCellRenderer billHeaderRenderer = new TableCellRenderer() {
            private final JLabel label = new JLabel();
            {
                label.setFont(AppTheme.font(Font.BOLD, 11));
                label.setForeground(AppTheme.TEXT_MUTED);
                label.setBackground(Color.WHITE);
                label.setOpaque(true);
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(241, 245, 249)),
                        new EmptyBorder(0, 6, 0, 6)
                ));
            }

            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                label.setText(val != null ? val.toString() : "");
                if (c == 1 || c == 3) {
                    label.setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (c == 2 || c == 4) {
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return label;
            }
        };

        billTable.getTableHeader().setDefaultRenderer(billHeaderRenderer);
        billTable.getTableHeader().setPreferredSize(new Dimension(0, 36));
        billTable.getTableHeader().setReorderingAllowed(false);
        billTable.getTableHeader().setResizingAllowed(false);
        billTable.getTableHeader().setBorder(BorderFactory.createEmptyBorder());

        billTable.getColumnModel().getColumn(0).setPreferredWidth(140); // Product Name
        billTable.getColumnModel().getColumn(1).setPreferredWidth(64);  // Rate (₹)
        billTable.getColumnModel().getColumn(2).setPreferredWidth(76);  // Qty [- 12 +]
        billTable.getColumnModel().getColumn(3).setPreferredWidth(70);  // Total (₹)
        billTable.getColumnModel().getColumn(4).setPreferredWidth(34);  // Del

        billTable.getColumnModel().getColumn(0).setCellRenderer(nameRenderer);
        billTable.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
        billTable.getColumnModel().getColumn(2).setCellRenderer(new QtyStepperRenderer());
        billTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);

        // Delete Button on Column 4 with 16px trash icon and circular hover background
        billTable.getColumnModel().getColumn(4).setCellRenderer(new TableCellRenderer() {
            private final SidebarIcon trashIcon = new SidebarIcon("trash", 16, new Color(220, 38, 38));
            private final JComponent comp = new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    int iconSize = 16;
                    int bgSize = 28;
                    int bx = (w - bgSize) / 2;
                    int by = (h - bgSize) / 2;

                    g2.setColor(new Color(254, 242, 242));
                    g2.fillOval(bx, by, bgSize, bgSize);

                    trashIcon.paintIcon(this, g2, (w - iconSize) / 2, (h - iconSize) / 2);
                    g2.dispose();
                }
            };
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return comp;
            }
        });

        // Instant click handling for inline Qty circular [-] / [+] and Delete buttons
        billTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int r = billTable.rowAtPoint(e.getPoint());
                int c = billTable.columnAtPoint(e.getPoint());
                if (r < 0 || r >= cartItems.size()) return;

                if (c == 2) {
                    // Inline Qty +/- Stepper
                    Rectangle rect = billTable.getCellRect(r, c, false);
                    int mx = e.getX() - rect.x;
                    int w = rect.width;
                    int btnSize = 20;
                    int textW = 24;
                    int gap = 5;
                    int totalW = btnSize * 2 + textW + gap * 2;
                    int startX = (w - totalW) / 2;
                    int plusX = startX + btnSize + textW + gap * 2;

                    if (mx >= startX - 3 && mx <= startX + btnSize + 3) {
                        adjustItemQuantity(r, -1);
                    } else if (mx >= plusX - 3 && mx <= plusX + btnSize + 3) {
                        SaleItem it = cartItems.get(r);
                        Product prod = findProductById(it.getProductId());
                        if (prod != null && it.getQuantity() >= prod.getQuantity()) {
                            Toolkit.getDefaultToolkit().beep();
                            JOptionPane.showMessageDialog(BillingPanel.this,
                                    "Cannot add more. Only " + prod.getQuantity() + " units available in stock for \"" + it.getProductName() + "\".",
                                    "Stock Limit Reached", JOptionPane.WARNING_MESSAGE);
                        } else {
                            adjustItemQuantity(r, 1);
                        }
                    }
                } else if (c == 4) {
                    // Inline Delete button
                    removeItemFromCart(r);
                }
            }
        });

        // Hover cursor over interactive row elements
        billTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int r = billTable.rowAtPoint(e.getPoint());
                int c = billTable.columnAtPoint(e.getPoint());
                if (r >= 0 && r < cartItems.size() && (c == 2 || c == 4)) {
                    if (c == 4) {
                        billTable.setCursor(new Cursor(Cursor.HAND_CURSOR));
                        return;
                    }
                    if (c == 2) {
                        Rectangle rect = billTable.getCellRect(r, c, false);
                        int mx = e.getX() - rect.x;
                        int w = rect.width;
                        int btnSize = 20;
                        int textW = 24;
                        int gap = 5;
                        int totalW = btnSize * 2 + textW + gap * 2;
                        int startX = (w - totalW) / 2;
                        int plusX = startX + btnSize + textW + gap * 2;
                        if (mx >= startX - 3 && mx <= startX + btnSize + 3) {
                            billTable.setCursor(new Cursor(Cursor.HAND_CURSOR));
                            return;
                        }
                        if (mx >= plusX - 3 && mx <= plusX + btnSize + 3) {
                            SaleItem it = cartItems.get(r);
                            Product prod = findProductById(it.getProductId());
                            if (prod != null && it.getQuantity() >= prod.getQuantity()) {
                                billTable.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                            } else {
                                billTable.setCursor(new Cursor(Cursor.HAND_CURSOR));
                            }
                            return;
                        }
                    }
                }
                billTable.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        JScrollPane billScroll = new JScrollPane(billTable);
        billScroll.setBorder(BorderFactory.createEmptyBorder());
        billScroll.setViewportBorder(null);
        billScroll.setOpaque(false);
        billScroll.getViewport().setOpaque(false);
        billScroll.getViewport().setBorder(null);

        rightCard.add(billScroll, BorderLayout.CENTER);

        // 3. Bill Calculation & Checkout Footer
        JPanel billFooter = new JPanel();
        billFooter.setLayout(new BoxLayout(billFooter, BoxLayout.Y_AXIS));
        billFooter.setOpaque(false);
        billFooter.setBorder(new EmptyBorder(8, 0, 0, 0));

        // Subtotal, GST Breakdown
        JPanel calcPanel = new JPanel(new GridLayout(2, 2, 8, 4));
        calcPanel.setOpaque(false);

        JLabel lblSubText = new JLabel("Subtotal (excl. tax):");
        lblSubText.setFont(AppTheme.font(Font.PLAIN, 12));
        lblSubText.setForeground(AppTheme.TEXT_SECONDARY);
        calcPanel.add(lblSubText);

        lblSubtotal = new JLabel("₹0.00", SwingConstants.RIGHT);
        lblSubtotal.setFont(AppTheme.font(Font.BOLD, 12));
        lblSubtotal.setForeground(AppTheme.TEXT_PRIMARY);
        calcPanel.add(lblSubtotal);

        JLabel lblTaxText = new JLabel("GST (18% inclusive):");
        lblTaxText.setFont(AppTheme.font(Font.PLAIN, 12));
        lblTaxText.setForeground(AppTheme.TEXT_SECONDARY);
        calcPanel.add(lblTaxText);

        lblGstAmount = new JLabel("₹0.00", SwingConstants.RIGHT);
        lblGstAmount.setFont(AppTheme.font(Font.BOLD, 12));
        lblGstAmount.setForeground(AppTheme.TEXT_PRIMARY);
        calcPanel.add(lblGstAmount);

        billFooter.add(calcPanel);
        billFooter.add(Box.createVerticalStrut(10));

        // Hairline separator (no 3D groove)
        JSeparator sep = new JSeparator() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(241, 245, 249));
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setPreferredSize(new Dimension(100, 1));
        billFooter.add(sep);
        billFooter.add(Box.createVerticalStrut(10));

        // Total Payable Row
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);

        JLabel lblTotalLabel = new JLabel("TOTAL PAYABLE");
        lblTotalLabel.setFont(AppTheme.font(Font.BOLD, 12));
        lblTotalLabel.setForeground(AppTheme.TEXT_MUTED);
        totalRow.add(lblTotalLabel, BorderLayout.WEST);

        lblGrandTotal = new JLabel("₹0.00");
        lblGrandTotal.setFont(AppTheme.font(Font.BOLD, 28));
        lblGrandTotal.setForeground(AppTheme.FOREST_GREEN);
        totalRow.add(lblGrandTotal, BorderLayout.EAST);
        billFooter.add(totalRow);
        billFooter.add(Box.createVerticalStrut(12));

        // Payment Mode Segmented Pill Selector (Pill cards with active solid bg color, no radio buttons)
        JPanel payRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        payRow.setOpaque(false);
        JLabel lblMode = new JLabel("Payment:");
        lblMode.setFont(AppTheme.font(Font.BOLD, 12));
        lblMode.setForeground(AppTheme.TEXT_SECONDARY);
        payRow.add(lblMode);

        btnPayCash = new PaymentModePill("Cash", "CASH");
        btnPayUpi = new PaymentModePill("UPI", "UPI");
        btnPayCard = new PaymentModePill("Card", "CARD");

        payRow.add(btnPayCash);
        payRow.add(btnPayUpi);
        payRow.add(btnPayCard);
        billFooter.add(payRow);
        billFooter.add(Box.createVerticalStrut(14));

        // Full Width Pill Checkout Button
        btnCheckout = PillButton.primary("Proceed to Checkout", new SidebarIcon("credit_card", 16, Color.WHITE));
        btnCheckout.setFont(AppTheme.font(Font.BOLD, 14));
        btnCheckout.setPreferredSize(new Dimension(0, 48));
        btnCheckout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btnCheckout.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCheckout.addActionListener(e -> completeSale());
        billFooter.add(btnCheckout);

        rightCard.add(billFooter, BorderLayout.SOUTH);
        centerGrid.add(rightCard, BorderLayout.EAST);

        add(centerGrid, BorderLayout.CENTER);
    }

    // =========================================================================
    // CAMERA BARCODE SCANNER CARD (Left Column Header)
    // =========================================================================
    private JPanel createCameraCard() {
        cameraCard = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 23, 42)); // Black / dark modern surface
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.dispose();
            }
        };
        cameraCard.setOpaque(false);
        cameraCard.setBorder(new EmptyBorder(0, 0, 0, 0));
        cameraCard.setPreferredSize(new Dimension(0, 195));
        cameraCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 195));

        cameraCardLayout = new CardLayout();
        cameraCardContainer = new JPanel(cameraCardLayout);
        cameraCardContainer.setOpaque(false);
        cameraCardContainer.setPreferredSize(new Dimension(0, 195));

        // 1. OFF VIEW: Pure minimalist black card with centered "Use Camera" button
        cameraOffView = new JPanel(new GridBagLayout());
        cameraOffView.setOpaque(false);
        cameraOffView.setPreferredSize(new Dimension(0, 195));

        PillButton btnUseCamera = PillButton.primary("Use Camera", null);
        btnUseCamera.setFont(AppTheme.font(Font.BOLD, 13));
        btnUseCamera.setPreferredSize(new Dimension(140, 38));
        btnUseCamera.addActionListener(e -> startCameraScanner());
        cameraOffView.add(btnUseCamera);

        // 2. ON VIEW: Live Camera Preview + Scanning Reticle + Checkmark Overlay + Stop Camera
        cameraOnView = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(0, 195);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Dark rounded background
                g2.setColor(new Color(15, 23, 42));
                g2.fillRoundRect(0, 0, w, h, 28, 28);

                // Clip to rounded bounds for video image
                Shape oldClip = g2.getClip();
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w, h, 28, 28));

                if (cameraFrame != null) {
                    // Center and scale image to fill viewport
                    int fw = cameraFrame.getWidth();
                    int fh = cameraFrame.getHeight();
                    double scale = Math.max((double) w / fw, (double) h / fh);
                    int dw = (int) (fw * scale);
                    int dh = (int) (fh * scale);
                    int dx = (w - dw) / 2;
                    int dy = (h - dh) / 2;
                    // Draw horizontally mirrored (like a real mirror / selfie camera)
                    g2.drawImage(cameraFrame, dx + dw, dy, -dw, dh, null);
                }

                g2.setClip(oldClip);

                // Viewfinder Reticle Frame (180x80)
                int retW = 180;
                int retH = 80;
                int rx = (w - retW) / 2;
                int ry = (h - retH) / 2;

                // Corner brackets in Forest Lime
                g2.setColor(AppTheme.ELECTRIC_LIME);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int len = 16;
                // Top-Left
                g2.drawLine(rx, ry, rx + len, ry);
                g2.drawLine(rx, ry, rx, ry + len);
                // Top-Right
                g2.drawLine(rx + retW, ry, rx + retW - len, ry);
                g2.drawLine(rx + retW, ry, rx + retW, ry + len);
                // Bottom-Left
                g2.drawLine(rx, ry + retH, rx + len, ry + retH);
                g2.drawLine(rx, ry + retH, rx, ry + retH - len);
                // Bottom-Right
                g2.drawLine(rx + retW, ry + retH, rx + retW - len, ry + retH);
                g2.drawLine(rx + retW, ry + retH, rx + retW, ry + retH - len);

                // Animated Green Laser Scan Line
                if (!showSuccessCheckmark) {
                    int lineY = ry + (int) (retH * laserScanY);
                    g2.setColor(new Color(16, 185, 129, 80));
                    g2.fillRect(rx + 2, lineY - 3, retW - 4, 6);
                    g2.setColor(new Color(52, 211, 153));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(rx + 2, lineY, rx + retW - 2, lineY);
                }

                // Checkmark Overlay (flashes for 1.2s when item detected & added)
                if (showSuccessCheckmark) {
                    int badgeW = Math.min(w - 36, 310);
                    int badgeH = 70;
                    int bx = (w - badgeW) / 2;
                    int by = (h - badgeH) / 2;

                    g2.setColor(new Color(0, 90, 61, 235));
                    g2.fillRoundRect(bx, by, badgeW, badgeH, 20, 20);
                    g2.setColor(new Color(52, 211, 153));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(bx, by, badgeW, badgeH, 20, 20);

                    // Checkmark icon
                    SidebarIcon chk = new SidebarIcon("check", 22, Color.WHITE);
                    chk.paintIcon(this, g2, bx + 16, by + (badgeH - chk.getIconHeight()) / 2);

                    // Text
                    int tx = bx + 48;
                    g2.setColor(Color.WHITE);
                    g2.setFont(AppTheme.font(Font.BOLD, 13));
                    FontMetrics fm = g2.getFontMetrics();
                    String title = lastScannedTitle;
                    if (title != null) {
                        if (fm.stringWidth(title) > badgeW - 60) {
                            while (title.length() > 3 && fm.stringWidth(title + "...") > badgeW - 60) {
                                title = title.substring(0, title.length() - 1);
                            }
                            title = title + "...";
                        }
                        g2.drawString(title, tx, by + 28);
                    }

                    g2.setColor(AppTheme.ELECTRIC_LIME);
                    g2.setFont(AppTheme.font(Font.BOLD, 11));
                    String sub = (lastScannedCartQty > 0)
                            ? "✓ Added to bill (" + lastScannedCartQty + " in cart)"
                            : "Product not found";
                    g2.drawString(sub, tx, by + 48);
                }

                g2.dispose();
            }
        };
        cameraOnView.setOpaque(false);

        // Bottom right: Stop Camera pill button
        JPanel onBottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 14));
        onBottomBar.setOpaque(false);

        PillButton btnStopCamera = PillButton.danger("Stop Camera", new SidebarIcon("stop", 11, AppTheme.STATUS_DANGER));
        btnStopCamera.setPreferredSize(new Dimension(116, 32));
        btnStopCamera.addActionListener(e -> stopCameraScanner());
        onBottomBar.add(btnStopCamera);

        cameraOnView.add(onBottomBar, BorderLayout.SOUTH);

        cameraCardContainer.add(cameraOffView, "OFF");
        cameraCardContainer.add(cameraOnView, "ON");

        cameraCard.add(cameraCardContainer, BorderLayout.CENTER);

        // Setup Laser Scan Timer
        laserScanTimer = new javax.swing.Timer(30, e -> {
            if (laserDirectionDown) {
                laserScanY += 0.025f;
                if (laserScanY >= 0.92f) {
                    laserScanY = 0.92f;
                    laserDirectionDown = false;
                }
            } else {
                laserScanY -= 0.025f;
                if (laserScanY <= 0.08f) {
                    laserScanY = 0.08f;
                    laserDirectionDown = true;
                }
            }
            if (isCameraActive && cameraOnView != null) {
                cameraOnView.repaint();
            }
        });

        return cameraCard;
    }

    private void startCameraScanner() {
        isCameraActive = true;
        cameraCardLayout.show(cameraCardContainer, "ON");
        cameraCard.revalidate();
        cameraCard.repaint();
        laserScanTimer.start();
        cameraOnView.repaint();

        // Launch native camera scanner process
        Thread starter = new Thread(() -> {
            File bin = new File("bin/camera_scanner");
            if (!bin.exists()) {
                try {
                    new ProcessBuilder("clang", "-O2", "-framework", "Foundation", "-framework", "AVFoundation",
                            "-framework", "Vision", "-framework", "AppKit", "-framework", "CoreMedia",
                            "-framework", "CoreVideo", "-framework", "CoreImage",
                            "tools/camera_scanner.m", "-o", "bin/camera_scanner").start().waitFor();
                } catch (Exception ignored) {}
            }

            if (bin.exists()) {
                try {
                    ProcessBuilder pb = new ProcessBuilder(bin.getAbsolutePath());
                    cameraProcess = pb.start();

                    InputStream is = cameraProcess.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                    String line;
                    while (isCameraActive && (line = reader.readLine()) != null) {
                        if (line.startsWith("BARCODE:")) {
                            String bcode = line.substring(8).trim();
                            SwingUtilities.invokeLater(() -> handleScannedBarcode(bcode));
                        } else if (line.startsWith("FRAME:")) {
                            String b64 = line.substring(6).trim();
                            try {
                                byte[] bytes = java.util.Base64.getDecoder().decode(b64);
                                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                                if (img != null) {
                                    cameraFrame = img;
                                    SwingUtilities.invokeLater(() -> {
                                        if (cameraOnView != null) cameraOnView.repaint();
                                    });
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Notice: Camera process closed: " + ex.getMessage());
                }
            }
        }, "CameraProcessRunner");
        starter.setDaemon(true);
        starter.start();
    }

    private void stopCameraScanner() {
        isCameraActive = false;
        if (laserScanTimer != null) laserScanTimer.stop();
        if (checkmarkTimer != null) checkmarkTimer.stop();
        showSuccessCheckmark = false;
        cameraFrame = null;

        if (cameraProcess != null) {
            try {
                cameraProcess.getOutputStream().write("STOP\n".getBytes());
                cameraProcess.getOutputStream().flush();
            } catch (Exception ignored) {}
            cameraProcess.destroy();
            cameraProcess = null;
        }

        cameraCardLayout.show(cameraCardContainer, "OFF");
        cameraCard.revalidate();
        cameraCard.repaint();
    }

    public void handleScannedBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) return;
        String val = barcode.trim();

        long now = System.currentTimeMillis();
        // 1.2s debounce on identical barcode
        if (val.equalsIgnoreCase(lastScannedBarcode) && (now - lastScannedTime < 1200)) {
            return;
        }

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

        if (match != null) {
            lastScannedBarcode = val;
            lastScannedTime = now;
            lastScannedTitle = match.getName();

            // Auto add 1 unit (increments if already present)
            boolean added = addProductToCart(match, 1);
            if (!added) {
                // Stock limit or out of stock was reached; addProductToCart already notified user
                return;
            }

            int countInCart = 1;
            for (SaleItem it : cartItems) {
                if (it.getProductId() == match.getId()) {
                    countInCart = it.getQuantity();
                    break;
                }
            }
            lastScannedCartQty = countInCart;

            try { Toolkit.getDefaultToolkit().beep(); } catch (Exception ignored) {}

            showSuccessCheckmark = true;
            if (cameraOnView != null) cameraOnView.repaint();

            if (checkmarkTimer != null) checkmarkTimer.stop();
            checkmarkTimer = new javax.swing.Timer(1200, ev -> {
                showSuccessCheckmark = false;
                if (cameraOnView != null) cameraOnView.repaint();
            });
            checkmarkTimer.setRepeats(false);
            checkmarkTimer.start();
        } else {
            try { Toolkit.getDefaultToolkit().beep(); } catch (Exception ignored) {}
            lastScannedTitle = "Unknown Barcode: " + val;
            lastScannedCartQty = 0;
            showSuccessCheckmark = true;
            if (cameraOnView != null) cameraOnView.repaint();

            if (checkmarkTimer != null) checkmarkTimer.stop();
            checkmarkTimer = new javax.swing.Timer(1200, ev -> {
                showSuccessCheckmark = false;
                if (cameraOnView != null) cameraOnView.repaint();
            });
            checkmarkTimer.setRepeats(false);
            checkmarkTimer.start();
        }
    }

    /**
     * Creates an elevated card matching the exact Dashboard widget silhouette:
     * - setOpaque(false) to prevent rectangular gray clipping on rounded corners
     * - Border radius r = 36
     * - Pure white surface with subtle ambient drop shadow
     * - No harsh borders
     */
    private JPanel createElevatedCard(LayoutManager layout) {
        JPanel card = new JPanel(layout) {
            private final int r = 36; // Exact same border radius as dashboard cards

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Ambient drop shadow matching dashboard
                g2.setColor(new Color(0, 50, 30, 8));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);

                // Pure crisp white card surface, NO BORDER!
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.dispose();
            }

            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                Shape clip = new RoundRectangle2D.Float(0, 0, w, h, r, r);
                g2.clip(clip);
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        card.setOpaque(false); // Vital for rounded corners to not show square background
        return card;
    }

    // ── DATA LOADING & FILTERING ─────────────────────────────────────────────
    public void loadProductCache() {
        try {
            allProducts = productDAO.getAllProducts();
        } catch (Exception e) {
            allProducts = new ArrayList<>();
        }
        filterCatalog();
    }

    private void filterCatalog() {
        String query = txtProductFilter != null && txtProductFilter.getText() != null
                ? txtProductFilter.getText().trim().toLowerCase() : "";

        filteredProducts.clear();
        for (Product p : allProducts) {
            if (query.isEmpty() ||
                (p.getName() != null && p.getName().toLowerCase().contains(query)) ||
                (p.getCode() != null && p.getCode().toLowerCase().contains(query)) ||
                (p.getBarcode() != null && p.getBarcode().toLowerCase().contains(query)) ||
                (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(query))) {
                filteredProducts.add(p);
            }
        }

        populateProductCards();

        if (!filteredProducts.isEmpty()) {
            selectedProduct = filteredProducts.get(0);
        } else {
            selectedProduct = null;
        }
    }

    private void updateGridColumns() {
        if (catalogScroll == null || productsGridPanel == null) return;
        int viewportW = catalogScroll.getViewport().getWidth();
        if (viewportW <= 0) viewportW = catalogScroll.getWidth();
        if (viewportW <= 0) return;

        int cols = Math.max(2, (viewportW - 12) / 185);
        if (cols != gridCols) {
            gridCols = cols;
            productsGridPanel.setLayout(new GridLayout(0, gridCols, 12, 12));
            productsGridPanel.revalidate();
            productsGridPanel.repaint();
        }
    }

    private void populateProductCards() {
        if (productsGridPanel == null) return;
        productsGridPanel.removeAll();
        productCardPanels.clear();

        if (filteredProducts.isEmpty()) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setOpaque(false);
            empty.setPreferredSize(new Dimension(300, 200));
            JLabel lbl = new JLabel("No matching products found");
            lbl.setFont(AppTheme.font(Font.BOLD, 13));
            lbl.setForeground(AppTheme.TEXT_MUTED);
            empty.add(lbl);
            productsGridPanel.add(empty);
        } else {
            for (Product p : filteredProducts) {
                ProductCardPanel card = new ProductCardPanel(p);
                productCardPanels.add(card);
                productsGridPanel.add(card);
            }
        }
        updateGridColumns();
        productsGridPanel.revalidate();
        productsGridPanel.repaint();
    }

    private void refreshProductCards() {
        for (ProductCardPanel c : productCardPanels) {
            c.updateStepperState();
        }
    }

    private void addFirstMatchedProduct() {
        if (!filteredProducts.isEmpty()) {
            selectedProduct = filteredProducts.get(0);
            addProductToCart(selectedProduct, 1);
            txtProductFilter.selectAll();
        }
    }

    private void loadInvoiceNo() {
        try {
            currentInvoiceNo = billingDAO.generateNextInvoiceNo();
        } catch (Exception e) {
            currentInvoiceNo = "INV-" + System.currentTimeMillis();
        }
        lblInvoicePill.setText("Invoice: " + currentInvoiceNo);
    }

    public Product findProductById(int id) {
        for (Product p : allProducts) {
            if (p.getId() == id) return p;
        }
        try {
            Product p = productDAO.getProductById(id);
            if (p != null) allProducts.add(p);
            return p;
        } catch (Exception ignored) {
            return null;
        }
    }

    // ── CART / BILL ACTIONS ──────────────────────────────────────────────────
    public boolean addProductToCart(Product p, int qty) {
        if (p == null || qty <= 0) return false;

        int currentCartQty = 0;
        SaleItem existingItem = null;
        int existingIndex = -1;
        for (int i = 0; i < cartItems.size(); i++) {
            SaleItem it = cartItems.get(i);
            if (it.getProductId() == p.getId()) {
                currentCartQty = it.getQuantity();
                existingItem = it;
                existingIndex = i;
                break;
            }
        }

        int maxStock = p.getQuantity();
        if (maxStock <= 0) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this,
                    "\"" + p.getName() + "\" is out of stock (0 available).",
                    "Out of Stock", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (currentCartQty + qty > maxStock) {
            Toolkit.getDefaultToolkit().beep();
            int remaining = Math.max(0, maxStock - currentCartQty);
            if (remaining <= 0) {
                JOptionPane.showMessageDialog(this,
                        "Cannot add more of \"" + p.getName() + "\".\nMaximum available stock (" + maxStock + " units) is already in the bill.",
                        "Stock Limit Reached", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Cannot add " + qty + " units of \"" + p.getName() + "\".\nOnly " + remaining + " more unit(s) available in stock (Cart has " + currentCartQty + ", Total stock: " + maxStock + ").",
                        "Stock Limit Exceeded", JOptionPane.WARNING_MESSAGE);
            }
            return false;
        }

        if (existingItem != null) {
            int newQty = existingItem.getQuantity() + qty;
            existingItem.setQuantity(newQty);
            existingItem.setSubtotal(newQty * existingItem.getUnitPrice());
            billModel.setValueAt(newQty, existingIndex, 2);
            billModel.setValueAt(String.format("₹%.2f", existingItem.getSubtotal()), existingIndex, 3);
            calculateTotals();
            refreshProductCards();
            return true;
        }

        // New item in bill
        double sub = qty * p.getSellingPrice();
        SaleItem item = new SaleItem(p.getId(), p.getCode(), p.getName(), qty, p.getSellingPrice(), sub);
        cartItems.add(item);

        billModel.addRow(new Object[]{
                p.getName(),
                String.format("₹%.2f", p.getSellingPrice()),
                qty,
                String.format("₹%.2f", sub),
                ""
        });

        calculateTotals();
        refreshProductCards();
        return true;
    }

    public void adjustItemQuantity(int row, int delta) {
        if (row >= 0 && row < cartItems.size()) {
            SaleItem item = cartItems.get(row);
            int newQty = item.getQuantity() + delta;
            if (newQty <= 0) {
                removeItemFromCart(row);
            } else {
                if (delta > 0) {
                    Product prod = findProductById(item.getProductId());
                    int maxStock = prod != null ? prod.getQuantity() : 0;
                    if (prod != null && newQty > maxStock) {
                        Toolkit.getDefaultToolkit().beep();
                        JOptionPane.showMessageDialog(this,
                                "Cannot add more. Only " + maxStock + " units available in stock for \"" + item.getProductName() + "\".",
                                "Stock Limit Reached", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
                item.setQuantity(newQty);
                item.setSubtotal(newQty * item.getUnitPrice());
                billModel.setValueAt(newQty, row, 2);
                billModel.setValueAt(String.format("₹%.2f", item.getSubtotal()), row, 3);
                calculateTotals();
                refreshProductCards();
            }
        }
    }

    private void adjustSelectedQuantity(int delta) {
        int sel = billTable.getSelectedRow();
        if (sel < 0) {
            if (cartItems.size() == 1) sel = 0;
            else {
                JOptionPane.showMessageDialog(this, "Please select an item in the bill to change quantity.");
                return;
            }
        }
        adjustItemQuantity(sel, delta);
    }

    private void removeItemFromCart(int index) {
        if (index >= 0 && index < cartItems.size()) {
            cartItems.remove(index);
            refreshBillTable();
            calculateTotals();
            refreshProductCards();
        }
    }

    private void clearCart() {
        if (cartItems.isEmpty()) return;
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear the current bill?", "Clear Bill", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            cartItems.clear();
            refreshBillTable();
            calculateTotals();
            refreshProductCards();
        }
    }

    private void refreshBillTable() {
        billModel.setRowCount(0);
        for (int i = 0; i < cartItems.size(); i++) {
            SaleItem it = cartItems.get(i);
            billModel.addRow(new Object[]{
                    it.getProductName(),
                    String.format("₹%.2f", it.getUnitPrice()),
                    it.getQuantity(),
                    String.format("₹%.2f", it.getSubtotal()),
                    ""
            });
        }
    }

    private void calculateTotals() {
        subtotal = 0.0;
        int totalUnits = 0;
        for (SaleItem it : cartItems) {
            subtotal += it.getSubtotal();
            totalUnits += it.getQuantity();
        }

        // Inclusive GST
        gstAmount = (subtotal * gstRate) / (100.0 + gstRate);
        grandTotal = subtotal;

        lblSubtotal.setText(String.format("₹%.2f", subtotal - gstAmount));
        lblGstAmount.setText(String.format("₹%.2f", gstAmount));
        lblGrandTotal.setText(String.format("₹%.2f", grandTotal));

        lblBillCount.setText(cartItems.size() + " Item" + (cartItems.size() == 1 ? "" : "s") + " (" + totalUnits + " Units)");
        btnCheckout.setEnabled(!cartItems.isEmpty());
        updatePaymentPillStates();
    }

    public void resetBillingDesk() {
        cartItems.clear();
        selectedPaymentMode = "CASH";
        updatePaymentPillStates();
        refreshBillTable();
        calculateTotals();
        loadInvoiceNo();
        loadProductCache();
        if (txtProductFilter != null) txtProductFilter.setText("");
        refreshProductCards();
    }

    // ── PRODUCT CARDS GRID & STEPPER COMPONENTS ──────────────────────────────
    private static class ScrollableGridPanel extends JPanel implements Scrollable {
        public ScrollableGridPanel(LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 120;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
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
                        // Contain scaling ensures 100% of the product image is visible without cropping
                        double scale = Math.min((double) w / imgW, (double) h / imgH);
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
                    g2.dispose();
                }
            };
            imgContainer.setOpaque(false);
            imgContainer.setPreferredSize(new Dimension(0, 130));

            add(imgContainer, BorderLayout.NORTH);

            // 2. MIDDLE & BOTTOM: Details + Stepper + Add Button
            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setOpaque(false);
            body.setBorder(new EmptyBorder(8, 10, 10, 10));

            // Name
            JLabel lblName = new JLabel(p.getName());
            lblName.setFont(AppTheme.font(Font.BOLD, 12));
            lblName.setForeground(AppTheme.TEXT_PRIMARY);
            lblName.setToolTipText(p.getName());
            lblName.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(lblName);
            body.add(Box.createVerticalStrut(4));

            // Rate (Price)
            JLabel lblPrice = new JLabel(String.format("₹%.2f", p.getSellingPrice()));
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
                    JOptionPane.showMessageDialog(BillingPanel.this,
                            "Cannot add more. Only " + maxAvailable + " units available in stock for \"" + product.getName() + "\".",
                            "Stock Limit Reached", JOptionPane.WARNING_MESSAGE);
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
                    Toolkit.getDefaultToolkit().beep();
                    JOptionPane.showMessageDialog(BillingPanel.this,
                            product.getQuantity() <= 0
                                    ? "\"" + product.getName() + "\" is out of stock (0 available)."
                                    : "\"" + product.getName() + "\" is already at maximum available stock in cart.",
                            "Stock Limit Reached", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (currentQty > remaining) {
                    Toolkit.getDefaultToolkit().beep();
                    JOptionPane.showMessageDialog(BillingPanel.this,
                            "Cannot add " + currentQty + " units. Only " + remaining + " unit(s) available in stock for \"" + product.getName() + "\".",
                            "Stock Limit Exceeded", JOptionPane.WARNING_MESSAGE);
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
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) showCardPopup(e);
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) showCardPopup(e);
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
                            Toolkit.getDefaultToolkit().beep();
                            JOptionPane.showMessageDialog(BillingPanel.this,
                                    product.getQuantity() <= 0
                                            ? "\"" + product.getName() + "\" is out of stock (0 available)."
                                            : "\"" + product.getName() + "\" has reached maximum available stock in the bill (" + product.getQuantity() + " units).",
                                    "Stock Limit Reached", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }
                private void showCardPopup(MouseEvent e) {
                    JPopupMenu menu = new JPopupMenu();
                    menu.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER_SAGE));

                    JMenuItem itemDownload = new JMenuItem("⬇ Download Barcode PNG");
                    itemDownload.setFont(AppTheme.font(Font.PLAIN, 12));
                    itemDownload.addActionListener(ev -> BarcodeUtil.saveBarcodeImage(BillingPanel.this, product));
                    menu.add(itemDownload);

                    JMenuItem itemView = new JMenuItem("🔍 View Barcode Dialog");
                    itemView.setFont(AppTheme.font(Font.PLAIN, 12));
                    itemView.addActionListener(ev -> {
                        Frame owner = (Frame) SwingUtilities.getWindowAncestor(BillingPanel.this);
                        BarcodeUtil.showBarcodeDialog(owner, product);
                    });
                    menu.add(itemView);

                    JMenuItem itemViewBox = new JMenuItem("📦 View Package Box Mockup");
                    itemViewBox.setFont(AppTheme.font(Font.PLAIN, 12));
                    itemViewBox.addActionListener(ev -> {
                        Frame owner = (Frame) SwingUtilities.getWindowAncestor(BillingPanel.this);
                        util.PackageBoxGenerator.showBoxDialog(owner, product);
                    });
                    menu.add(itemViewBox);

                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            });

            updateStepperState();
        }

        private int getRemainingStock() {
            int inCart = 0;
            for (SaleItem it : cartItems) {
                if (it.getProductId() == product.getId()) {
                    inCart = it.getQuantity();
                    break;
                }
            }
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

    // ── CHECKOUT FLOW & POPUP ────────────────────────────────────────────────
    public void completeSale() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "The bill is empty. Please add products before checking out.", "Bill Empty", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String paymentMode = selectedPaymentMode != null ? selectedPaymentMode : "CASH";

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        CheckoutDialog checkoutDlg = new CheckoutDialog(owner, currentInvoiceNo, grandTotal, subtotal, gstAmount, gstRate, paymentMode);
        checkoutDlg.setVisible(true);

        if (checkoutDlg.isSuccess()) {
            Sale sale = checkoutDlg.getCompletedSale();
            if (sale != null) {
                InvoiceDialog invoiceDialog = new InvoiceDialog(owner, sale);
                invoiceDialog.setVisible(true);
                resetBillingDesk();
            }
        }
    }

    public void focusBarcodeField() {
        if (txtProductFilter != null) {
            txtProductFilter.requestFocusInWindow();
        }
    }

    public void updateHeldBillsDropdown() {}

    public boolean resumeHeldBill(HeldBill hb) {
        if (hb == null) return false;
        cartItems.clear();
        if (hb.getItems() != null) cartItems.addAll(hb.getItems());
        refreshBillTable();
        calculateTotals();
        return true;
    }

    private void setupKeyboardShortcuts() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "checkout");
        am.put("checkout", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { completeSale(); }
        });
    }

    // ── INLINE QTY STEPPER RENDERER ──────────────────────────────────────────
    private class QtyStepperRenderer implements TableCellRenderer {
        private final JComponent comp = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                int btnSize = 20;
                int textW = 24;
                int gap = 5;
                int totalW = btnSize * 2 + textW + gap * 2;
                int startX = (w - totalW) / 2;
                int btnY = (h - btnSize) / 2;

                // Circular Minus Button
                g2.setColor(new Color(242, 246, 244));
                g2.fillOval(startX, btnY, btnSize, btnSize);
                g2.setColor(AppTheme.TEXT_PRIMARY);
                g2.setFont(AppTheme.font(Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                String m = "−";
                g2.drawString(m, startX + (btnSize - fm.stringWidth(m)) / 2, btnY + ((btnSize - fm.getHeight()) / 2) + fm.getAscent() - 1);

                // Qty Text
                int qty = (getClientProperty("qty") instanceof Integer) ? (Integer) getClientProperty("qty") : 1;
                String qStr = String.valueOf(qty);
                g2.setColor(AppTheme.TEXT_PRIMARY);
                g2.setFont(AppTheme.font(Font.BOLD, 12));
                fm = g2.getFontMetrics();
                int qx = startX + btnSize + gap + (textW - fm.stringWidth(qStr)) / 2;
                int qy = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(qStr, qx, qy);

                // Circular Plus Button (disabled style if at max stock)
                boolean canInc = Boolean.TRUE.equals(getClientProperty("canInc"));
                int plusX = startX + btnSize + textW + gap * 2;
                g2.setColor(canInc ? new Color(242, 246, 244) : new Color(248, 250, 249));
                g2.fillOval(plusX, btnY, btnSize, btnSize);
                g2.setColor(canInc ? AppTheme.TEXT_PRIMARY : new Color(195, 205, 200));
                g2.setFont(AppTheme.font(Font.BOLD, 13));
                fm = g2.getFontMetrics();
                String p = "+";
                g2.drawString(p, plusX + (btnSize - fm.stringWidth(p)) / 2, btnY + ((btnSize - fm.getHeight()) / 2) + fm.getAscent());

                g2.dispose();
            }
        };

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int q = 1;
            if (value instanceof Number) {
                q = ((Number) value).intValue();
            } else if (value != null) {
                try { q = Integer.parseInt(value.toString().trim()); } catch (Exception ignored) {}
            }
            comp.putClientProperty("qty", q);

            boolean canInc = true;
            if (row >= 0 && row < cartItems.size()) {
                SaleItem it = cartItems.get(row);
                Product prod = findProductById(it.getProductId());
                if (prod != null && q >= prod.getQuantity()) {
                    canInc = false;
                }
            }
            comp.putClientProperty("canInc", canInc);

            return comp;
        }
    }

    // ── SEGMENTED PAYMENT PILL SELECTOR ───────────────────────────────────────
    public class PaymentModePill extends JButton {
        private final String mode;
        private boolean hovered = false;

        public PaymentModePill(String label, String mode) {
            super(label);
            this.mode = mode;
            setFont(AppTheme.font(Font.BOLD, 12));
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(72, 30));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });

            addActionListener(e -> {
                selectedPaymentMode = mode;
                updatePaymentPillStates();
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            boolean isSel = mode.equalsIgnoreCase(selectedPaymentMode);
            int w = getWidth(), h = getHeight();
            int arc = h;

            Color bg;
            Color fg;
            if (isSel) {
                bg = AppTheme.FOREST_GREEN;
                fg = Color.WHITE;
            } else {
                bg = hovered ? new Color(230, 238, 234) : new Color(244, 248, 246);
                fg = hovered ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY;
            }

            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            g2.setFont(getFont());
            g2.setColor(fg);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(getText())) / 2;
            int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
            g2.drawString(getText(), tx, ty);

            g2.dispose();
        }
    }

    private void updatePaymentPillStates() {
        if (btnPayCash != null) btnPayCash.repaint();
        if (btnPayUpi != null) btnPayUpi.repaint();
        if (btnPayCard != null) btnPayCard.repaint();

        if (btnCheckout != null) {
            if ("UPI".equalsIgnoreCase(selectedPaymentMode)) {
                btnCheckout.setText("📱 Scan UPI & Checkout (" + String.format(Locale.US, "₹%.2f", grandTotal) + ")");
                btnCheckout.setIcon(new SidebarIcon("smartphone", 16, Color.WHITE));
            } else if ("CARD".equalsIgnoreCase(selectedPaymentMode)) {
                btnCheckout.setText("💳 Card Checkout (" + String.format(Locale.US, "₹%.2f", grandTotal) + ")");
                btnCheckout.setIcon(new SidebarIcon("credit_card", 16, Color.WHITE));
            } else {
                btnCheckout.setText("Proceed to Checkout (" + String.format(Locale.US, "₹%.2f", grandTotal) + ")");
                btnCheckout.setIcon(new SidebarIcon("wallet", 16, Color.WHITE));
            }
        }
    }

    // ── REUSABLE PILL BUTTON COMPONENT ───────────────────────────────────────
    public static class PillButton extends JButton {
        private final Color bgColor;
        private final Color hoverBg;
        private final Color textColor;
        private final Color hoverText;
        private boolean hovered = false;
        private final boolean hasBorder;
        private final Color borderColor;

        public PillButton(String text, Icon icon, Color bg, Color fg, Color hoverBg, Color hoverFg, boolean hasBorder, Color borderColor) {
            super(text, icon);
            this.bgColor = bg;
            this.textColor = fg;
            this.hoverBg = hoverBg;
            this.hoverText = hoverFg != null ? hoverFg : fg;
            this.hasBorder = hasBorder;
            this.borderColor = borderColor != null ? borderColor : AppTheme.BORDER_SAGE;

            setFont(AppTheme.font(Font.BOLD, 12));
            setForeground(fg);
            setIconTextGap(7);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
            });
        }

        public static PillButton primary(String text, Icon icon) {
            return new PillButton(text, icon, AppTheme.FOREST_GREEN, Color.WHITE, AppTheme.FOREST_MID, Color.WHITE, false, null);
        }

        public static PillButton secondary(String text, Icon icon) {
            return new PillButton(text, icon, new Color(236, 248, 240), AppTheme.FOREST_GREEN, new Color(220, 244, 228), AppTheme.FOREST_MID, false, null);
        }

        public static PillButton subtle(String text, Icon icon) {
            return new PillButton(text, icon, new Color(244, 248, 246), AppTheme.TEXT_PRIMARY, new Color(230, 240, 234), AppTheme.TEXT_PRIMARY, false, null);
        }

        public static PillButton danger(String text, Icon icon) {
            return new PillButton(text, icon, new Color(254, 242, 242), AppTheme.STATUS_DANGER, new Color(254, 226, 226), AppTheme.STATUS_DANGER, false, null);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int arc = h; // Stadium / pill shape

            boolean enabled = isEnabled();
            Color currentBg = !enabled ? new Color(236, 242, 239) : (hovered ? hoverBg : bgColor);
            Color currentFg = !enabled ? new Color(160, 175, 168) : (hovered ? hoverText : textColor);

            // Fill Pill Background
            g2.setColor(currentBg);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            if (hasBorder && enabled) {
                g2.setColor(hovered ? AppTheme.FOREST_MID : borderColor);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }

            // Calculate content (Icon + Text) layout
            Icon icon = getIcon();
            String text = getText();
            Font font = getFont();
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);

            int textW = (text != null && !text.isEmpty()) ? fm.stringWidth(text) : 0;
            int iconW = (icon != null) ? icon.getIconWidth() : 0;
            int iconH = (icon != null) ? icon.getIconHeight() : 0;
            int gap = (icon != null && text != null && !text.isEmpty()) ? getIconTextGap() : 0;
            int contentW = iconW + gap + textW;
            int startX = (w - contentW) / 2;

            if (icon != null) {
                int iconY = (h - iconH) / 2;
                if (icon instanceof SidebarIcon) {
                    ((SidebarIcon) icon).setOverrideColor(currentFg);
                }
                icon.paintIcon(this, g2, startX, iconY);
            }

            if (text != null && !text.isEmpty()) {
                int textX = startX + iconW + gap;
                int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.setColor(currentFg);
                g2.drawString(text, textX, textY);
            }

            g2.dispose();
            // Intentionally omit super.paintComponent(g) to prevent AquaButtonUI rectangular override on macOS
        }
    }

    // ── INNER CLASS: Checkout Dialog (Phone Search & Auto-Registration) ──────
    private class CheckoutDialog extends JDialog {
        private final String invoiceNo;
        private final double totalAmount;
        private final double rawSubtotal;
        private final double taxAmount;
        private final double taxRate;

        private String modalPaymentMode;
        private JButton btnModalCash, btnModalUpi, btnModalCard;

        private String currentStoreUpi;
        private String currentStoreName;
        private String currentUpiUri;
        private BufferedImage currentQrImage;

        private JPanel upiQrSection;
        private JPanel qrCanvas;
        private JLabel lblUpiAmountLocked;
        private JLabel lblUpiPayee;
        private JScrollPane modalScrollPane;
        private JPanel modalBody;

        private JTextField txtPhone;
        private JTextField txtName;
        private JTextField txtEmail;
        private JTextField txtAddress;
        private JLabel lblPhoneStatus;
        private JLabel lblDigitCounter;
        private int phoneValidationState = 0; // 0 = NORMAL, 1 = VALID (10 digits), 2 = ERROR (>10 digits or non-digits)
        private JPanel registrationPanel;

        private boolean isExistingCustomer = false;
        private Customer matchedCustomer = null;
        private boolean saleSuccess = false;
        private Sale createdSale = null;

        public CheckoutDialog(Frame owner, String invoiceNo, double total, double subtotal, double tax, double rate, String mode) {
            super(owner, "Checkout & Payment - " + invoiceNo, true);
            this.invoiceNo = invoiceNo;
            this.totalAmount = total;
            this.rawSubtotal = subtotal;
            this.taxAmount = tax;
            this.taxRate = rate;
            this.modalPaymentMode = (mode != null && !mode.trim().isEmpty()) ? mode.toUpperCase() : "CASH";

            this.currentStoreUpi = AppSettings.getString(AppSettings.KEY_STORE_UPI_ID, "bazaarpoint@upi").trim();
            if (this.currentStoreUpi.isEmpty()) this.currentStoreUpi = "bazaarpoint@upi";
            this.currentStoreName = AppSettings.getString(AppSettings.KEY_STORE_NAME, "SmartBilling Store").trim();
            if (this.currentStoreName.isEmpty()) this.currentStoreName = "SmartBilling Store";

            setSize(540, 720);
            setLocationRelativeTo(owner);
            setResizable(false);
            setLayout(new BorderLayout());
            getContentPane().setBackground(Color.WHITE);

            buildModalUI();
            setModalPaymentMode(this.modalPaymentMode);
        }

        private JTextField createPhoneInputField(String placeholder, int height, int fontSize) {
            JTextField tf = new JTextField() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    boolean focused = hasFocus();

                    Color bg = (phoneValidationState == 2) ? new Color(254, 242, 242) :
                               (phoneValidationState == 1) ? new Color(240, 253, 244) :
                               (focused ? Color.WHITE : new Color(246, 249, 247));

                    Color border = (phoneValidationState == 2) ? new Color(239, 68, 68) :
                                   (phoneValidationState == 1) ? new Color(34, 197, 94) :
                                   (focused ? AppTheme.FOREST_MID : new Color(226, 235, 230));

                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
                    g2.setColor(border);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                    super.paintComponent(g);

                    if (getText() == null || getText().isEmpty()) {
                        g2.setFont(AppTheme.font(Font.PLAIN, fontSize));
                        g2.setColor(new Color(156, 175, 168));
                        FontMetrics fm = g2.getFontMetrics();
                        Insets insets = getInsets();
                        int x = insets != null && insets.left > 0 ? insets.left : 16;
                        int y = ((h - fm.getHeight()) / 2) + fm.getAscent();
                        g2.drawString(placeholder, x, y);
                    }
                    g2.dispose();
                }
            };
            tf.setOpaque(false);
            tf.setBorder(new EmptyBorder(4, 16, 4, 16));
            tf.setFont(AppTheme.font(Font.PLAIN, fontSize));
            tf.setPreferredSize(new Dimension(0, height));
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
            return tf;
        }

        private JTextField createStyledInputField(String placeholder, int height, int fontSize) {
            JTextField tf = new JTextField() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    boolean focused = hasFocus();
                    g2.setColor(focused ? Color.WHITE : new Color(246, 249, 247));
                    g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
                    g2.setColor(focused ? AppTheme.FOREST_MID : new Color(226, 235, 230));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                    super.paintComponent(g);

                    if (getText() == null || getText().isEmpty()) {
                        g2.setFont(AppTheme.font(Font.PLAIN, fontSize));
                        g2.setColor(new Color(156, 175, 168));
                        FontMetrics fm = g2.getFontMetrics();
                        Insets insets = getInsets();
                        int x = insets != null && insets.left > 0 ? insets.left : 16;
                        int y = ((h - fm.getHeight()) / 2) + fm.getAscent();
                        g2.drawString(placeholder, x, y);
                    }
                    g2.dispose();
                }
            };
            tf.setOpaque(false);
            tf.setBorder(new EmptyBorder(4, 16, 4, 16));
            tf.setFont(AppTheme.font(Font.PLAIN, fontSize));
            tf.setPreferredSize(new Dimension(0, height));
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
            return tf;
        }

        private JButton createModalPaymentPill(String label, String mode, String iconName) {
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
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    boolean isSel = mode.equalsIgnoreCase(modalPaymentMode);
                    int w = getWidth(), h = getHeight();
                    int arc = h;

                    Color bg = isSel ? AppTheme.FOREST_GREEN : (hovered ? new Color(234, 242, 238) : new Color(245, 249, 247));
                    Color fg = isSel ? Color.WHITE : (hovered ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY);
                    Color border = isSel ? AppTheme.FOREST_GREEN : (hovered ? AppTheme.FOREST_MID : new Color(226, 235, 230));

                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                    g2.setColor(border);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                    // Draw Icon and Text centered
                    SidebarIcon icon = new SidebarIcon(iconName, 14, fg);
                    g2.setFont(AppTheme.font(Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    int iconW = icon.getIconWidth();
                    int textW = fm.stringWidth(label);
                    int gap = 7;
                    int totalContentW = iconW + gap + textW;
                    int startX = (w - totalContentW) / 2;

                    icon.paintIcon(this, g2, startX, (h - icon.getIconHeight()) / 2);
                    g2.setColor(fg);
                    int textY = ((h - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(label, startX + iconW + gap, textY);

                    g2.dispose();
                }
            };
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(0, 36));
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            btn.addActionListener(e -> setModalPaymentMode(mode));
            return btn;
        }

        private void setModalPaymentMode(String mode) {
            this.modalPaymentMode = (mode != null) ? mode.toUpperCase() : "CASH";
            if (btnModalCash != null) btnModalCash.repaint();
            if (btnModalUpi != null) btnModalUpi.repaint();
            if (btnModalCard != null) btnModalCard.repaint();

            if ("UPI".equalsIgnoreCase(modalPaymentMode)) {
                refreshUpiQr();
                if (upiQrSection != null) {
                    upiQrSection.setVisible(true);
                    SwingUtilities.invokeLater(() -> {
                        if (modalScrollPane != null) {
                            modalScrollPane.validate();
                            JScrollBar bar = modalScrollPane.getVerticalScrollBar();
                            bar.setValue(bar.getMaximum());
                        }
                    });
                }
            } else {
                if (upiQrSection != null) {
                    upiQrSection.setVisible(false);
                }
            }
            if (modalBody != null) {
                modalBody.revalidate();
                modalBody.repaint();
            }
        }

        private void refreshUpiQr() {
            this.currentStoreUpi = AppSettings.getString(AppSettings.KEY_STORE_UPI_ID, "bazaarpoint@upi").trim();
            if (this.currentStoreUpi.isEmpty()) this.currentStoreUpi = "bazaarpoint@upi";
            this.currentStoreName = AppSettings.getString(AppSettings.KEY_STORE_NAME, "SmartBilling Store").trim();
            if (this.currentStoreName.isEmpty()) this.currentStoreName = "SmartBilling Store";

            this.currentUpiUri = QrCodeGenerator.buildUpiUri(this.currentStoreUpi, this.currentStoreName, this.totalAmount, this.invoiceNo);
            this.currentQrImage = QrCodeGenerator.generateQrImage(this.currentUpiUri, 200);

            if (qrCanvas != null) {
                qrCanvas.repaint();
            }
            if (lblUpiAmountLocked != null) {
                lblUpiAmountLocked.setText(String.format(Locale.US, "₹%.2f", totalAmount));
            }
            if (lblUpiPayee != null) {
                lblUpiPayee.setText("Paying to: " + currentStoreName + " (" + currentStoreUpi + ")");
            }
        }

        private void buildModalUI() {
            // ── TOP HEADER / HERO BANNER ──────────────────────────────────────
            JPanel header = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    GradientPaint gp = new GradientPaint(0, 0, new Color(243, 248, 245), 0, h, Color.WHITE);
                    g2.setPaint(gp);
                    g2.fillRect(0, 0, w, h);
                    g2.setColor(new Color(226, 235, 230));
                    g2.drawLine(0, h - 1, w, h - 1);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            header.setBorder(new EmptyBorder(18, 24, 16, 24));

            // Row 1: Invoice Tag + Checkout Badge
            JPanel topRow = new JPanel(new BorderLayout());
            topRow.setOpaque(false);
            topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

            JLabel lblInvoicePill = new JLabel("  " + invoiceNo + "  ") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(234, 246, 239));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.setColor(new Color(196, 228, 210));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lblInvoicePill.setFont(AppTheme.font(Font.BOLD, 11));
            lblInvoicePill.setForeground(AppTheme.FOREST_MID);
            lblInvoicePill.setOpaque(false);
            topRow.add(lblInvoicePill, BorderLayout.WEST);

            JLabel lblCheckoutBadge = new JLabel("SECURE CHECKOUT", new SidebarIcon("check_circle", 13, AppTheme.FOREST_MID), SwingConstants.RIGHT);
            lblCheckoutBadge.setFont(AppTheme.font(Font.BOLD, 10));
            lblCheckoutBadge.setForeground(AppTheme.TEXT_MUTED);
            lblCheckoutBadge.setIconTextGap(5);
            topRow.add(lblCheckoutBadge, BorderLayout.EAST);

            header.add(topRow);
            header.add(Box.createVerticalStrut(10));

            // Row 2: "TOTAL AMOUNT PAYABLE" subtitle
            JLabel lblPayableTitle = new JLabel("TOTAL AMOUNT PAYABLE");
            lblPayableTitle.setFont(AppTheme.font(Font.BOLD, 10));
            lblPayableTitle.setForeground(AppTheme.TEXT_MUTED);
            lblPayableTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.add(lblPayableTitle);
            header.add(Box.createVerticalStrut(2));

            // Row 3: Big Beautiful Payable Amount
            JLabel lblModalTotal = new JLabel(String.format("₹%.2f", totalAmount));
            lblModalTotal.setFont(AppTheme.font(Font.BOLD, 28));
            lblModalTotal.setForeground(AppTheme.FOREST_GREEN);
            lblModalTotal.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.add(lblModalTotal);
            header.add(Box.createVerticalStrut(4));

            // Row 4: Breakdown summary
            String gstPercentStr = String.format("%.0f%%", taxRate * 100);
            JLabel lblBreakdown = new JLabel(String.format("Subtotal: ₹%.2f   •   GST (%s): ₹%.2f", rawSubtotal, gstPercentStr, taxAmount));
            lblBreakdown.setFont(AppTheme.font(Font.PLAIN, 11));
            lblBreakdown.setForeground(AppTheme.TEXT_MUTED);
            lblBreakdown.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.add(lblBreakdown);

            add(header, BorderLayout.NORTH);

            // ── FORM BODY ─────────────────────────────────────────────────────
            JPanel body = new JPanel();
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBackground(Color.WHITE);
            body.setBorder(new EmptyBorder(16, 24, 16, 24));

            // Customer Phone Header Row with Live 10-Digit Counter
            JPanel phoneHeaderRow = new JPanel(new BorderLayout());
            phoneHeaderRow.setOpaque(false);
            phoneHeaderRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            phoneHeaderRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

            JLabel lblPhoneHeader = new JLabel("Customer Mobile Number", new SidebarIcon("user", 13, AppTheme.FOREST_MID), SwingConstants.LEFT);
            lblPhoneHeader.setIconTextGap(6);
            lblPhoneHeader.setFont(AppTheme.font(Font.BOLD, 12));
            lblPhoneHeader.setForeground(AppTheme.TEXT_PRIMARY);
            phoneHeaderRow.add(lblPhoneHeader, BorderLayout.WEST);

            lblDigitCounter = new JLabel("0/10 digits") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Color bg = (phoneValidationState == 2) ? new Color(254, 226, 226) :
                               (phoneValidationState == 1) ? new Color(220, 252, 231) :
                               new Color(241, 245, 249);
                    Color border = (phoneValidationState == 2) ? new Color(248, 113, 113) :
                                   (phoneValidationState == 1) ? new Color(134, 239, 172) :
                                   new Color(226, 232, 240);
                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.setColor(border);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lblDigitCounter.setFont(AppTheme.font(Font.BOLD, 10));
            lblDigitCounter.setForeground(AppTheme.TEXT_MUTED);
            lblDigitCounter.setBorder(new EmptyBorder(2, 8, 2, 8));
            lblDigitCounter.setOpaque(false);
            phoneHeaderRow.add(lblDigitCounter, BorderLayout.EAST);
            body.add(phoneHeaderRow);
            body.add(Box.createVerticalStrut(5));

            txtPhone = createPhoneInputField("Enter 10-digit mobile number...", 38, 14);
            txtPhone.setFont(AppTheme.font(Font.BOLD, 14));
            txtPhone.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(txtPhone);
            body.add(Box.createVerticalStrut(5));

            lblPhoneStatus = new JLabel("Enter 10-digit mobile number to search or register customer (or Skip below)");
            lblPhoneStatus.setFont(AppTheme.font(Font.PLAIN, 11));
            lblPhoneStatus.setForeground(AppTheme.TEXT_MUTED);
            lblPhoneStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(lblPhoneStatus);
            body.add(Box.createVerticalStrut(12));

            // Real-time Phone Lookup & 10-Digit Strict Validation Logic
            Runnable lookupAction = () -> {
                String rawPhone = txtPhone.getText();
                String phone = rawPhone != null ? rawPhone.trim() : "";

                if (phone.isEmpty()) {
                    phoneValidationState = 0;
                    lblDigitCounter.setText("0/10 digits");
                    lblDigitCounter.setForeground(AppTheme.TEXT_MUTED);
                    isExistingCustomer = false;
                    matchedCustomer = null;
                    lblPhoneStatus.setIcon(null);
                    lblPhoneStatus.setText("Enter 10-digit mobile number to search or register customer (or Skip below)");
                    lblPhoneStatus.setForeground(AppTheme.TEXT_MUTED);
                    registrationPanel.setVisible(false);
                } else if (!phone.matches("\\d+")) {
                    phoneValidationState = 2;
                    lblDigitCounter.setText("⚠️ Digits only (0-9)");
                    lblDigitCounter.setForeground(new Color(220, 38, 38));
                    isExistingCustomer = false;
                    matchedCustomer = null;
                    lblPhoneStatus.setIcon(new SidebarIcon("alert_triangle", 13, new Color(220, 38, 38)));
                    lblPhoneStatus.setText("Validation Error: Only digits (0-9) allowed. Please remove letters/symbols.");
                    lblPhoneStatus.setForeground(new Color(220, 38, 38));
                    registrationPanel.setVisible(false);
                } else if (phone.length() > 10) {
                    phoneValidationState = 2;
                    lblDigitCounter.setText("⚠️ " + phone.length() + "/10 (Too Long!)");
                    lblDigitCounter.setForeground(new Color(220, 38, 38));
                    isExistingCustomer = false;
                    matchedCustomer = null;
                    lblPhoneStatus.setIcon(new SidebarIcon("alert_triangle", 13, new Color(220, 38, 38)));
                    lblPhoneStatus.setText("Validation Alert: Mobile number cannot exceed 10 digits! (" + phone.length() + " digits entered)");
                    lblPhoneStatus.setForeground(new Color(220, 38, 38));
                    registrationPanel.setVisible(false);
                } else if (phone.length() < 10) {
                    phoneValidationState = 0;
                    lblDigitCounter.setText(phone.length() + "/10 digits");
                    lblDigitCounter.setForeground(AppTheme.TEXT_SECONDARY);
                    isExistingCustomer = false;
                    matchedCustomer = null;

                    try {
                        Customer found = customerDAO.getCustomerByPhone(phone);
                        if (found == null) {
                            List<Customer> search = customerDAO.searchCustomers(phone);
                            if (!search.isEmpty()) found = search.get(0);
                        }
                        if (found != null && found.getPhone() != null && found.getPhone().equals(phone)) {
                            isExistingCustomer = true;
                            matchedCustomer = found;
                            lblPhoneStatus.setIcon(new SidebarIcon("check_circle", 13, new Color(22, 163, 74)));
                            lblPhoneStatus.setText("Existing Customer: " + found.getName() + " (Auto-linked)");
                            lblPhoneStatus.setForeground(new Color(22, 163, 74));
                            txtName.setText(found.getName());
                            txtEmail.setText(found.getEmail() != null ? found.getEmail() : "");
                            txtAddress.setText(found.getAddress() != null ? found.getAddress() : "");
                            registrationPanel.setVisible(false);
                        } else {
                            lblPhoneStatus.setIcon(null);
                            lblPhoneStatus.setText("Please enter complete 10-digit mobile number (" + phone.length() + "/10 digits entered)");
                            lblPhoneStatus.setForeground(AppTheme.TEXT_MUTED);
                            registrationPanel.setVisible(false);
                        }
                    } catch (Exception ex) {
                        lblPhoneStatus.setIcon(null);
                        lblPhoneStatus.setText("Please enter complete 10-digit mobile number (" + phone.length() + "/10 digits entered)");
                        lblPhoneStatus.setForeground(AppTheme.TEXT_MUTED);
                        registrationPanel.setVisible(false);
                    }
                } else { // Exactly 10 digits!
                    phoneValidationState = 1;
                    lblDigitCounter.setText("✓ 10/10 Digits");
                    lblDigitCounter.setForeground(new Color(22, 163, 74));

                    try {
                        Customer found = customerDAO.getCustomerByPhone(phone);
                        if (found == null) {
                            List<Customer> search = customerDAO.searchCustomers(phone);
                            for (Customer c : search) {
                                if (phone.equals(c.getPhone())) {
                                    found = c;
                                    break;
                                }
                            }
                        }

                        if (found != null) {
                            isExistingCustomer = true;
                            matchedCustomer = found;
                            lblPhoneStatus.setIcon(new SidebarIcon("check_circle", 13, new Color(22, 163, 74)));
                            lblPhoneStatus.setText("Existing Customer: " + found.getName() + " (Auto-linked)");
                            lblPhoneStatus.setForeground(new Color(22, 163, 74));
                            txtName.setText(found.getName());
                            txtEmail.setText(found.getEmail() != null ? found.getEmail() : "");
                            txtAddress.setText(found.getAddress() != null ? found.getAddress() : "");
                            registrationPanel.setVisible(false);
                        } else {
                            isExistingCustomer = false;
                            matchedCustomer = null;
                            lblPhoneStatus.setIcon(new SidebarIcon("user", 13, AppTheme.FOREST_MID));
                            lblPhoneStatus.setText("New Customer (10 digits verified) — Enter details below to register & save");
                            lblPhoneStatus.setForeground(AppTheme.FOREST_MID);
                            registrationPanel.setVisible(true);
                        }
                    } catch (Exception ex) {
                        lblPhoneStatus.setIcon(null);
                        lblPhoneStatus.setText("Status: " + ex.getMessage());
                        registrationPanel.setVisible(false);
                    }
                }
                txtPhone.repaint();
                lblDigitCounter.repaint();
                body.revalidate();
                body.repaint();
            };

            txtPhone.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { lookupAction.run(); }
                public void removeUpdate(DocumentEvent e) { lookupAction.run(); }
                public void changedUpdate(DocumentEvent e) { lookupAction.run(); }
            });

            txtPhone.addActionListener(e -> {
                String p = txtPhone.getText().trim();
                if (p.length() > 10 || (!p.isEmpty() && !p.matches("\\d+"))) {
                    handleConfirmCheckout();
                } else if (p.length() == 10) {
                    if (registrationPanel.isVisible()) {
                        txtName.requestFocusInWindow();
                    } else {
                        handleConfirmCheckout();
                    }
                }
            });

            // New Customer Registration Panel (hidden initially)
            registrationPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    g2.setColor(new Color(248, 251, 249));
                    g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
                    g2.setColor(new Color(226, 235, 230));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            registrationPanel.setOpaque(false);
            registrationPanel.setLayout(new BoxLayout(registrationPanel, BoxLayout.Y_AXIS));
            registrationPanel.setBorder(new EmptyBorder(12, 14, 12, 14));
            registrationPanel.setVisible(false);
            registrationPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel lblRegTitle = new JLabel("NEW CUSTOMER DETAILS");
            lblRegTitle.setFont(AppTheme.font(Font.BOLD, 10));
            lblRegTitle.setForeground(AppTheme.FOREST_MID);
            registrationPanel.add(lblRegTitle);
            registrationPanel.add(Box.createVerticalStrut(8));

            JLabel lblName = new JLabel("Full Name *");
            lblName.setFont(AppTheme.font(Font.BOLD, 11));
            lblName.setForeground(AppTheme.TEXT_PRIMARY);
            txtName = createStyledInputField("e.g. Rahul Sharma", 32, 12);

            JLabel lblEmail = new JLabel("Email (Optional)");
            lblEmail.setFont(AppTheme.font(Font.PLAIN, 11));
            lblEmail.setForeground(AppTheme.TEXT_SECONDARY);
            txtEmail = createStyledInputField("e.g. rahul@example.com", 32, 12);

            JLabel lblAddress = new JLabel("Address (Optional)");
            lblAddress.setFont(AppTheme.font(Font.PLAIN, 11));
            lblAddress.setForeground(AppTheme.TEXT_SECONDARY);
            txtAddress = createStyledInputField("e.g. City or Area", 32, 12);

            registrationPanel.add(lblName);
            registrationPanel.add(Box.createVerticalStrut(3));
            registrationPanel.add(txtName);
            registrationPanel.add(Box.createVerticalStrut(6));
            registrationPanel.add(lblEmail);
            registrationPanel.add(Box.createVerticalStrut(3));
            registrationPanel.add(txtEmail);
            registrationPanel.add(Box.createVerticalStrut(6));
            registrationPanel.add(lblAddress);
            registrationPanel.add(Box.createVerticalStrut(3));
            registrationPanel.add(txtAddress);

            body.add(registrationPanel);
            body.add(Box.createVerticalStrut(14));

            // Payment Mode Confirmation (Segmented Pill Buttons)
            JLabel lblPayMode = new JLabel("Confirm Payment Mode", new SidebarIcon("wallet", 13, AppTheme.FOREST_MID), SwingConstants.LEFT);
            lblPayMode.setIconTextGap(6);
            lblPayMode.setFont(AppTheme.font(Font.BOLD, 12));
            lblPayMode.setForeground(AppTheme.TEXT_PRIMARY);
            lblPayMode.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(lblPayMode);
            body.add(Box.createVerticalStrut(8));

            JPanel modeGrid = new JPanel(new GridLayout(1, 3, 10, 0));
            modeGrid.setOpaque(false);
            modeGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
            modeGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

            btnModalCash = createModalPaymentPill("Cash", "CASH", "wallet");
            btnModalUpi = createModalPaymentPill("UPI", "UPI", "smartphone");
            btnModalCard = createModalPaymentPill("Card", "CARD", "credit_card");

            modeGrid.add(btnModalCash);
            modeGrid.add(btnModalUpi);
            modeGrid.add(btnModalCard);
            body.add(modeGrid);
            body.add(Box.createVerticalStrut(12));

            upiQrSection = createUpiQrSection();
            body.add(upiQrSection);

            modalBody = body;
            modalScrollPane = new JScrollPane(body);
            modalScrollPane.setBorder(null);
            modalScrollPane.setOpaque(false);
            modalScrollPane.getViewport().setOpaque(false);
            modalScrollPane.getVerticalScrollBar().setUnitIncrement(14);
            add(modalScrollPane, BorderLayout.CENTER);

            // ── FOOTER ACTIONS ────────────────────────────────────────────────
            JPanel footer = new JPanel(new BorderLayout(10, 0));
            footer.setBackground(new Color(250, 252, 251));
            footer.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 238, 234)),
                    new EmptyBorder(14, 20, 16, 20)
            ));

            PillButton btnWalkIn = PillButton.subtle("Walk-in (Skip)", new SidebarIcon("user", 13, AppTheme.TEXT_MUTED));
            btnWalkIn.setPreferredSize(new Dimension(135, 36));
            btnWalkIn.addActionListener(e -> executeFinalSale(null, "Walk-in Customer", ""));
            footer.add(btnWalkIn, BorderLayout.WEST);

            JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            rightActions.setOpaque(false);

            PillButton btnCancel = PillButton.subtle("Cancel", null);
            btnCancel.setPreferredSize(new Dimension(80, 36));
            btnCancel.addActionListener(e -> dispose());
            rightActions.add(btnCancel);

            PillButton btnConfirm = PillButton.primary("Complete & Print", new SidebarIcon("printer", 14, Color.WHITE));
            btnConfirm.setPreferredSize(new Dimension(160, 36));
            btnConfirm.addActionListener(e -> handleConfirmCheckout());
            rightActions.add(btnConfirm);

            footer.add(rightActions, BorderLayout.EAST);
            add(footer, BorderLayout.SOUTH);

            SwingUtilities.invokeLater(() -> txtPhone.requestFocusInWindow());
        }

        private JPanel createUpiQrSection() {
            JPanel card = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    int r = 16;
                    g2.setColor(new Color(246, 252, 248));
                    g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);
                    g2.setColor(new Color(205, 235, 220));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            card.setOpaque(false);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(new EmptyBorder(12, 14, 12, 14));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setVisible(false);

            // Row 1: Header (Title, Auto-amount Badge, Change UPI Button)
            JPanel topRow = new JPanel(new BorderLayout(8, 0));
            topRow.setOpaque(false);
            topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

            JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            titleBox.setOpaque(false);

            JLabel lblTitle = new JLabel("Dynamic UPI Payment QR", new SidebarIcon("smartphone", 13, AppTheme.FOREST_DEEP), SwingConstants.LEFT);
            lblTitle.setIconTextGap(5);
            lblTitle.setFont(AppTheme.font(Font.BOLD, 12));
            lblTitle.setForeground(AppTheme.FOREST_DEEP);
            titleBox.add(lblTitle);

            JLabel lblBadge = new JLabel(" AUTO-AMOUNT LOCKED ") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(230, 248, 238));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.setColor(new Color(196, 228, 210));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            lblBadge.setFont(AppTheme.font(Font.BOLD, 9));
            lblBadge.setForeground(AppTheme.FOREST_MID);
            lblBadge.setOpaque(false);
            titleBox.add(lblBadge);
            topRow.add(titleBox, BorderLayout.WEST);

            JButton btnChangeUpi = new JButton("Edit UPI", new SidebarIcon("pencil", 10, AppTheme.FOREST_MID)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                    g2.setColor(new Color(205, 235, 220));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btnChangeUpi.setIconTextGap(4);
            btnChangeUpi.setFont(AppTheme.font(Font.BOLD, 10));
            btnChangeUpi.setForeground(AppTheme.FOREST_MID);
            btnChangeUpi.setOpaque(false);
            btnChangeUpi.setContentAreaFilled(false);
            btnChangeUpi.setBorderPainted(false);
            btnChangeUpi.setFocusPainted(false);
            btnChangeUpi.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnChangeUpi.setPreferredSize(new Dimension(84, 24));
            btnChangeUpi.addActionListener(e -> promptChangeStoreUpi());
            topRow.add(btnChangeUpi, BorderLayout.EAST);

            card.add(topRow);
            card.add(Box.createVerticalStrut(8));

            // Row 2: Exact Amount Banner
            JPanel amtBanner = new JPanel(new BorderLayout(8, 0));
            amtBanner.setBackground(new Color(236, 253, 245));
            amtBanner.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(167, 243, 208), 1, true),
                    new EmptyBorder(5, 12, 5, 12)
            ));
            amtBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

            JLabel lblAmtTitle = new JLabel("Exact Bill Amount:", new SidebarIcon("shield", 12, new Color(22, 101, 52)), SwingConstants.LEFT);
            lblAmtTitle.setIconTextGap(6);
            lblAmtTitle.setFont(AppTheme.font(Font.PLAIN, 12));
            lblAmtTitle.setForeground(new Color(22, 101, 52));
            amtBanner.add(lblAmtTitle, BorderLayout.WEST);

            lblUpiAmountLocked = new JLabel(String.format(Locale.US, "₹%.2f", totalAmount));
            lblUpiAmountLocked.setFont(AppTheme.font(Font.BOLD, 17));
            lblUpiAmountLocked.setForeground(new Color(21, 128, 61));
            amtBanner.add(lblUpiAmountLocked, BorderLayout.EAST);
            card.add(amtBanner);
            card.add(Box.createVerticalStrut(4));

            JLabel lblLockHint = new JLabel("Exact bill amount is auto-encoded in QR. Customer scans & pays without typing.", SwingConstants.CENTER);
            lblLockHint.setFont(AppTheme.font(Font.PLAIN, 10));
            lblLockHint.setForeground(new Color(22, 101, 52));
            lblLockHint.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblLockHint);
            card.add(Box.createVerticalStrut(6));

            // Row 3: QR Code Canvas
            qrCanvas = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                    int w = getWidth();
                    int h = getHeight();
                    int boxSize = 175;
                    int bx = (w - boxSize) / 2;
                    int by = (h - boxSize) / 2;

                    // White surface with soft drop shadow and border
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(bx - 6, by - 6, boxSize + 12, boxSize + 12, 14, 14);
                    g2.setColor(new Color(210, 230, 220));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(bx - 6, by - 6, boxSize + 12, boxSize + 12, 14, 14);

                    if (currentQrImage != null) {
                        g2.drawImage(currentQrImage, bx, by, boxSize, boxSize, null);
                    } else {
                        g2.setColor(Color.RED);
                        g2.drawString("Generating QR Code...", bx + 20, by + 100);
                    }

                    // Center mini UPI badge
                    int badgeW = 42;
                    int badgeH = 20;
                    int badgeX = bx + (boxSize - badgeW) / 2;
                    int badgeY = by + (boxSize - badgeH) / 2;

                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(badgeX - 2, badgeY - 2, badgeW + 4, badgeH + 4, 8, 8);
                    g2.setColor(new Color(79, 70, 229)); // Indigo
                    g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);

                    g2.setColor(Color.WHITE);
                    g2.setFont(AppTheme.font(Font.BOLD, 10));
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = badgeX + (badgeW - fm.stringWidth("UPI")) / 2;
                    int ty = badgeY + ((badgeH - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString("UPI", tx, ty);

                    g2.dispose();
                }
            };
            qrCanvas.setPreferredSize(new Dimension(280, 185));
            qrCanvas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 185));
            qrCanvas.setOpaque(false);
            qrCanvas.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(qrCanvas);
            card.add(Box.createVerticalStrut(5));

            // Row 4: Payee & Supported Apps
            lblUpiPayee = new JLabel("Paying to: " + currentStoreName + " (" + currentStoreUpi + ")", SwingConstants.CENTER);
            lblUpiPayee.setFont(AppTheme.font(Font.BOLD, 11));
            lblUpiPayee.setForeground(AppTheme.TEXT_PRIMARY);
            lblUpiPayee.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblUpiPayee);
            card.add(Box.createVerticalStrut(2));

            JLabel lblSupported = new JLabel("GPay • PhonePe • Paytm • BHIM • Cred • Any Bank UPI App", SwingConstants.CENTER);
            lblSupported.setFont(AppTheme.font(Font.PLAIN, 10));
            lblSupported.setForeground(new Color(100, 116, 139));
            lblSupported.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(lblSupported);
            card.add(Box.createVerticalStrut(8));

            // Row 5: Primary Action inside QR section: "✓ Payment Received — Complete Sale"
            PillButton btnDirectConfirm = PillButton.primary("Payment Received — Complete Sale", new SidebarIcon("check_circle", 14, Color.WHITE));
            btnDirectConfirm.setFont(AppTheme.font(Font.BOLD, 12));
            btnDirectConfirm.setPreferredSize(new Dimension(0, 35));
            btnDirectConfirm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
            btnDirectConfirm.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnDirectConfirm.addActionListener(e -> handleConfirmCheckout());
            card.add(btnDirectConfirm);
            card.add(Box.createVerticalStrut(5));

            // Row 6: Secondary Actions (Fullscreen / Copy Link)
            JPanel subRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
            subRow.setOpaque(false);

            PillButton btnFullscreen = PillButton.subtle("Customer Screen", new SidebarIcon("external_link", 11, AppTheme.TEXT_SECONDARY));
            btnFullscreen.setFont(AppTheme.font(Font.BOLD, 10));
            btnFullscreen.setPreferredSize(new Dimension(145, 26));
            btnFullscreen.addActionListener(e -> openFullscreenUpiDialog());
            subRow.add(btnFullscreen);

            PillButton btnCopyLink = PillButton.subtle("Copy Link", null);
            btnCopyLink.setFont(AppTheme.font(Font.BOLD, 10));
            btnCopyLink.setPreferredSize(new Dimension(95, 26));
            btnCopyLink.addActionListener(e -> {
                if (currentUpiUri != null) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(currentUpiUri), null);
                    JOptionPane.showMessageDialog(CheckoutDialog.this, "UPI URI copied to clipboard!", "Copied", JOptionPane.INFORMATION_MESSAGE);
                }
            });
            subRow.add(btnCopyLink);

            card.add(subRow);
            return card;
        }

        private void promptChangeStoreUpi() {
            JPanel panel = new JPanel(new GridLayout(4, 1, 4, 6));
            panel.setPreferredSize(new Dimension(360, 150));
            panel.add(new JLabel("Apna Store UPI ID daalein (PhonePe/GPay/Paytm):"));
            JTextField txtUpi = new JTextField(currentStoreUpi);
            panel.add(txtUpi);
            panel.add(new JLabel("Store / Dukan ka Naam:"));
            JTextField txtName = new JTextField(currentStoreName);
            panel.add(txtName);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "🏬 Store UPI Configuration",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {
                String newUpi = txtUpi.getText().trim();
                String newName = txtName.getText().trim();
                if (!newUpi.isEmpty()) {
                    AppSettings.setString(AppSettings.KEY_STORE_UPI_ID, newUpi);
                    if (!newName.isEmpty()) {
                        AppSettings.setString(AppSettings.KEY_STORE_NAME, newName);
                    }
                    AppSettings.saveSettings();
                    refreshUpiQr();
                    if (modalBody != null) {
                        modalBody.revalidate();
                        modalBody.repaint();
                    }
                }
            }
        }

        private void openFullscreenUpiDialog() {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            UpiQrDialog dlg = new UpiQrDialog(owner, currentStoreUpi, currentStoreName, totalAmount, invoiceNo);
            dlg.setVisible(true);
            if (dlg.isPaymentConfirmed()) {
                handleConfirmCheckout();
            }
        }

        private void handleConfirmCheckout() {
            String phone = txtPhone.getText().trim();

            if (phone.isEmpty()) {
                executeFinalSale(null, "Walk-in Customer", "");
                return;
            }

            // ── Mobile Number 10-Digit Validation ─────────────────────────────
            if (!phone.matches("\\d+")) {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid Mobile Number!\n\nMobile number must contain numeric digits (0-9) only.\nPlease remove any letters, spaces, or special symbols.",
                        "Validation Error - Numbers Only",
                        JOptionPane.WARNING_MESSAGE
                );
                txtPhone.requestFocusInWindow();
                txtPhone.selectAll();
                return;
            }

            if (phone.length() > 10) {
                JOptionPane.showMessageDialog(
                        this,
                        "Mobile number cannot exceed 10 digits!\n\nYou entered " + phone.length() + " digits:\n\"" + phone + "\"\n\nPlease enter a valid 10-digit mobile number.",
                        "Validation Error - Number Too Long",
                        JOptionPane.WARNING_MESSAGE
                );
                txtPhone.requestFocusInWindow();
                txtPhone.selectAll();
                return;
            }

            if (phone.length() < 10) {
                JOptionPane.showMessageDialog(
                        this,
                        "Mobile number must be exactly 10 digits!\n\nYou entered only " + phone.length() + " digits:\n\"" + phone + "\"\n\nPlease complete the 10-digit number, or leave blank for Walk-in.",
                        "Validation Error - Incomplete Number",
                        JOptionPane.WARNING_MESSAGE
                );
                txtPhone.requestFocusInWindow();
                return;
            }

            Integer customerId = null;
            String customerName = "";
            String customerPhone = phone;

            if (isExistingCustomer && matchedCustomer != null) {
                customerId = matchedCustomer.getId();
                customerName = matchedCustomer.getName();
                customerPhone = matchedCustomer.getPhone();
            } else {
                // New customer registration
                String name = txtName.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter the Customer Name to save their record.", "Name Required", JOptionPane.WARNING_MESSAGE);
                    txtName.requestFocusInWindow();
                    return;
                }

                Customer newCust = new Customer();
                newCust.setName(name);
                newCust.setPhone(phone);
                newCust.setEmail(txtEmail.getText().trim());
                newCust.setAddress(txtAddress.getText().trim());

                try {
                    boolean ok = customerDAO.addCustomer(newCust);
                    if (ok) {
                        customerId = newCust.getId();
                        customerName = newCust.getName();
                    } else {
                        customerName = name;
                    }
                } catch (Exception ex) {
                    customerName = name;
                }
            }

            executeFinalSale(customerId, customerName, customerPhone);
        }

        private void executeFinalSale(Integer custId, String custName, String custPhone) {
            String payMode = modalPaymentMode != null ? modalPaymentMode : "CASH";

            Sale sale = new Sale();
            sale.setInvoiceNo(invoiceNo);
            sale.setCustomerId(custId);
            sale.setCustomerName(custName);
            sale.setCustomerPhone(custPhone);
            sale.setSubtotal(rawSubtotal);
            sale.setGstRate(taxRate);
            sale.setGstAmount(taxAmount);
            sale.setTotalAmount(totalAmount);
            sale.setPaymentMode(payMode);
            if (currentUser != null) {
                sale.setCreatedBy(currentUser.getId());
                sale.setCashierName(currentUser.getFullName());
            }
            sale.setItems(new ArrayList<>(cartItems));

            try {
                boolean ok = billingDAO.processSale(sale);
                if (ok) {
                    this.createdSale = sale;
                    this.saleSuccess = true;
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to record sale in the database.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Database error during sale processing:\n" + ex.getMessage(), "Billing Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        public boolean isSuccess() { return saleSuccess; }
        public Sale getCompletedSale() { return createdSale; }
    }
}
