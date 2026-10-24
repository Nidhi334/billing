package ui;

import config.AppSettings;
import model.Sale;
import model.SaleItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.text.SimpleDateFormat;

/**
 * Modern Receipt & Invoice Preview Dialog.
 * Redesigned with:
 * - Authentic thermal receipt paper slip simulation with soft drop shadow on AppTheme.BG_CANVAS
 * - Real Code-128B barcode generated via BarcodeUtil with high-DPI rendering
 * - Multi-column clean alignment (no squeezed/overlapping text)
 * - Anti-aliased high-contrast pill action buttons (100% visible text on macOS & all platforms)
 * - Instant switching between 2-Inch (58mm), 3-Inch (80mm), and A4 Invoice formats
 * - Direct vector printing to thermal and standard printers via Java PrinterJob
 */
public class InvoiceDialog extends JDialog {

    private JComboBox<String> cmbPrintMode;
    private JPanel paperWrapper;
    private ReceiptPaperPanel currentReceiptPaper;
    private final Sale currentSale;

    public InvoiceDialog(Window owner, Sale sale) {
        super(owner, "Receipt & Invoice Preview", ModalityType.APPLICATION_MODAL);
        this.currentSale = sale;

        setSize(520, 700);
        setMinimumSize(new Dimension(480, 600));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppTheme.BG_CANVAS);

        initComponents();
        setupShortcuts();
    }

    public InvoiceDialog(Frame owner, Sale sale) {
        this((Window) owner, sale);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(14, 18, 14, 18));
        root.setBackground(AppTheme.BG_CANVAS);

        // =========================================================================
        // 1. TOP BAR: Modern Format Selector
        // =========================================================================
        JPanel topFormatBar = new JPanel(new BorderLayout(10, 0));
        topFormatBar.setOpaque(false);

        JLabel lblMode = new JLabel("Print Size:");
        lblMode.setFont(AppTheme.font(Font.BOLD, 12));
        lblMode.setForeground(AppTheme.TEXT_PRIMARY);
        topFormatBar.add(lblMode, BorderLayout.WEST);

        cmbPrintMode = new JComboBox<>(new String[]{
                "2-Inch Thermal (58mm / 32 Cols) [RECOMMENDED]",
                "3-Inch Thermal (80mm / 44 Cols)",
                "Standard A4 / Full Page Invoice"
        });
        styleComboBox(cmbPrintMode);

        // Load configured default size from AppSettings
        String defaultSize = AppSettings.getString(AppSettings.KEY_RECEIPT_PRINT_SIZE, "2_INCH");
        if ("3_INCH".equalsIgnoreCase(defaultSize)) {
            cmbPrintMode.setSelectedIndex(1);
        } else if ("A4".equalsIgnoreCase(defaultSize)) {
            cmbPrintMode.setSelectedIndex(2);
        } else {
            cmbPrintMode.setSelectedIndex(0);
        }

        cmbPrintMode.addActionListener(e -> {
            int idx = cmbPrintMode.getSelectedIndex();
            String modeStr = (idx == 1 ? "3_INCH" : (idx == 2 ? "A4" : "2_INCH"));
            AppSettings.setString(AppSettings.KEY_RECEIPT_PRINT_SIZE, modeStr);
            updateReceiptView();
        });

        topFormatBar.add(cmbPrintMode, BorderLayout.CENTER);
        root.add(topFormatBar, BorderLayout.NORTH);

        // =========================================================================
        // 2. CENTER: Authentic Receipt Paper Slip Centered in ScrollPane
        // =========================================================================
        paperWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        paperWrapper.setBackground(AppTheme.BG_CANVAS);

        updateReceiptView();

        JScrollPane scroll = new JScrollPane(paperWrapper);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);

        // =========================================================================
        // 3. FOOTER: High-Contrast Pill Action Buttons (Guaranteed Visibility)
        // =========================================================================
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(6, 0, 0, 0));

        // Format hint badge on left
        JLabel lblHint = new JLabel("Thermal preview scales automatically to printer");
        lblHint.setFont(AppTheme.font(Font.PLAIN, 11));
        lblHint.setForeground(AppTheme.TEXT_MUTED);
        footer.add(lblHint, BorderLayout.WEST);

        // Buttons on right
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnClose = createPillButton("Close", null, Color.WHITE, AppTheme.BORDER_SAGE, AppTheme.TEXT_SECONDARY);
        btnClose.setPreferredSize(new Dimension(85, 36));
        btnClose.addActionListener(e -> dispose());

        JButton btnPrintThermal = createPillButton("Print Receipt / Slip", "printer", AppTheme.FOREST_GREEN, null, Color.WHITE);
        btnPrintThermal.setPreferredSize(new Dimension(175, 36));
        btnPrintThermal.addActionListener(e -> handlePrintReceipt());

        btnPanel.add(btnClose);
        btnPanel.add(btnPrintThermal);
        footer.add(btnPanel, BorderLayout.EAST);

        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void updateReceiptView() {
        int selected = cmbPrintMode.getSelectedIndex();
        paperWrapper.removeAll();
        currentReceiptPaper = buildReceiptPaper(selected);
        paperWrapper.add(currentReceiptPaper);
        paperWrapper.revalidate();
        paperWrapper.repaint();
    }

    // =========================================================================
    // RECEIPT BUILDER (2-Inch, 3-Inch, and A4 Formats)
    // =========================================================================
    private ReceiptPaperPanel buildReceiptPaper(int mode) {
        int paperWidth = (mode == 0 ? 295 : (mode == 1 ? 375 : 460));
        ReceiptPaperPanel paper = new ReceiptPaperPanel(paperWidth);

        String storeName = AppSettings.getString(AppSettings.KEY_STORE_NAME, "SmartBilling Supermarket").toUpperCase();
        String storePhone = AppSettings.getString(AppSettings.KEY_STORE_PHONE, "9876543210");
        String storeGst = AppSettings.getString(AppSettings.KEY_STORE_GST, "07AAAAA0000A1Z5");

        SimpleDateFormat sdf = new SimpleDateFormat(mode == 0 ? "dd/MM/yy HH:mm" : "dd-MM-yyyy HH:mm:ss");
        String dateStr = (currentSale != null && currentSale.getSaleDate() != null)
                ? sdf.format(currentSale.getSaleDate())
                : sdf.format(new java.util.Date());
        String invNo = (currentSale != null && currentSale.getInvoiceNo() != null)
                ? currentSale.getInvoiceNo()
                : "INV-20261024-0001";

        if (mode == 0) {
            // ─────────────────────────────────────────────────────────────────
            // 2-INCH THERMAL RECEIPT (58mm)
            // ─────────────────────────────────────────────────────────────────
            JLabel lblStore = new JLabel(storeName, SwingConstants.CENTER);
            lblStore.setFont(AppTheme.font(Font.BOLD, 14));
            lblStore.setForeground(AppTheme.FOREST_DEEP);
            lblStore.setAlignmentX(Component.CENTER_ALIGNMENT);
            paper.add(lblStore);
            paper.add(Box.createVerticalStrut(2));

            JLabel lblSub = new JLabel("RETAIL POS TERMINAL", SwingConstants.CENTER);
            lblSub.setFont(AppTheme.font(Font.PLAIN, 10));
            lblSub.setForeground(AppTheme.TEXT_SECONDARY);
            lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
            paper.add(lblSub);

            JLabel lblMeta = new JLabel("Ph: " + storePhone + (storeGst.isEmpty() ? "" : " | GST: " + storeGst), SwingConstants.CENTER);
            lblMeta.setFont(AppTheme.font(Font.PLAIN, 10));
            lblMeta.setForeground(AppTheme.TEXT_MUTED);
            lblMeta.setAlignmentX(Component.CENTER_ALIGNMENT);
            paper.add(lblMeta);
            paper.add(Box.createVerticalStrut(6));

            paper.add(createDashedDivider());
            paper.add(Box.createVerticalStrut(6));

            paper.add(createTwoColRow("Bill: " + invNo, dateStr, true, false));
            String cust = (currentSale != null && currentSale.getCustomerName() != null && !currentSale.getCustomerName().isEmpty())
                    ? currentSale.getCustomerName() : "Walk-in";
            if (currentSale != null && currentSale.getCustomerPhone() != null && !currentSale.getCustomerPhone().isEmpty()) {
                cust += " (" + currentSale.getCustomerPhone() + ")";
            }
            paper.add(createTwoColRow("Cust: " + cust, "", false, false));
            String pay = (currentSale != null && currentSale.getPaymentMode() != null) ? currentSale.getPaymentMode() : "CASH";
            paper.add(createTwoColRow("Pay : " + pay, "Desk #1", false, false));
            paper.add(Box.createVerticalStrut(6));

            paper.add(createDashedDivider());
            paper.add(Box.createVerticalStrut(4));

            paper.add(createTwoColRow("ITEM", "TOTAL", true, true));
            paper.add(Box.createVerticalStrut(4));

            if (currentSale != null && currentSale.getItems() != null) {
                for (SaleItem item : currentSale.getItems()) {
                    JPanel itemBlock = new JPanel();
                    itemBlock.setLayout(new BoxLayout(itemBlock, BoxLayout.Y_AXIS));
                    itemBlock.setOpaque(false);
                    itemBlock.setAlignmentX(Component.LEFT_ALIGNMENT);

                    // Row 1: Item Name on Left, Line Total on Right
                    itemBlock.add(createTwoColRow(item.getProductName(), String.format("₹%.2f", item.getSubtotal()), true, true));

                    // Row 2: Qty x Unit Rate
                    JLabel lblCalc = new JLabel(String.format("   %d  ×  ₹%.2f", item.getQuantity(), item.getUnitPrice()));
                    lblCalc.setFont(AppTheme.font(Font.PLAIN, 10));
                    lblCalc.setForeground(AppTheme.TEXT_MUTED);
                    lblCalc.setAlignmentX(Component.LEFT_ALIGNMENT);
                    itemBlock.add(lblCalc);
                    itemBlock.add(Box.createVerticalStrut(4));

                    paper.add(itemBlock);
                }
            }

            paper.add(createDashedDivider());
            paper.add(Box.createVerticalStrut(6));

            // Totals
            double subtotal = currentSale != null ? currentSale.getSubtotal() : 0.0;
            double discount = currentSale != null ? currentSale.getDiscountAmount() : 0.0;
            double gstAmt = currentSale != null ? currentSale.getGstAmount() : 0.0;
            int gstRate = currentSale != null ? (int) currentSale.getGstRate() : 0;
            double total = currentSale != null ? currentSale.getTotalAmount() : 0.0;

            paper.add(createTwoColRow("Subtotal", String.format("₹%.2f", subtotal), false, false));
            if (discount > 0) {
                paper.add(createTwoColRow("Discount", String.format("-₹%.2f", discount), false, false));
            }
            paper.add(createTwoColRow("GST (" + gstRate + "%)", String.format("₹%.2f", gstAmt), false, false));
            paper.add(Box.createVerticalStrut(6));

            paper.add(createDoubleDivider());
            paper.add(Box.createVerticalStrut(4));
            paper.add(createTwoColRow("NET TOTAL", String.format("₹%.2f", total), true, true, 14, AppTheme.FOREST_DEEP));
            paper.add(Box.createVerticalStrut(4));
            paper.add(createDoubleDivider());
            paper.add(Box.createVerticalStrut(10));

            // Real Code-128B Barcode
            BufferedImage barcodeImg = BarcodeUtil.generateBarcodeImage(invNo, 220, 46, true);
            JLabel lblBarcode = new JLabel(new ImageIcon(barcodeImg), SwingConstants.CENTER);
            lblBarcode.setAlignmentX(Component.CENTER_ALIGNMENT);
            paper.add(lblBarcode);
            paper.add(Box.createVerticalStrut(10));

            JLabel lblThanks = new JLabel("*** THANK YOU! VISIT AGAIN ***", SwingConstants.CENTER);
            lblThanks.setFont(AppTheme.font(Font.BOLD, 10));
            lblThanks.setForeground(AppTheme.TEXT_SECONDARY);
            lblThanks.setAlignmentX(Component.CENTER_ALIGNMENT);
            paper.add(lblThanks);

            JLabel lblPower = new JLabel("Powered by SmartBilling POS", SwingConstants.CENTER);
            lblPower.setFont(AppTheme.font(Font.PLAIN, 9));
            lblPower.setForeground(AppTheme.TEXT_MUTED);
            lblPower.setAlignmentX(Component.CENTER_ALIGNMENT);
            paper.add(lblPower);

        } else if (mode == 1) {
            // ─────────────────────────────────────────────────────────────────
            // 3-INCH THERMAL RECEIPT (80mm)
            // ─────────────────────────────────────────────────────────────────
            JLabel lblStore = new JLabel(storeName, SwingConstants.CENTER);
            lblStore.setFont(AppTheme.font(Font.BOLD, 16));
            lblStore.setForeground(AppTheme.FOREST_DEEP);
            lblStore.setAlignmentX(Component.CENTER_ALIGNMENT);
            paper.add(lblStore);
            paper.add(Box.createVerticalStrut(2));

            JLabel lblAddress = new JLabel("Tech Plaza, Main Market, New Delhi", SwingConstants.CENTER);
            lblAddress.setFont(AppTheme.font(Font.PLAIN, 10));
            lblAddress.setForeground(AppTheme.TEXT_SECONDARY);
            lblAddress.setAlignmentX(Component.CENTER_ALIGNMENT);
            paper.add(lblAddress);

            JLabel lblMeta = new JLabel("Ph: " + storePhone + (storeGst.isEmpty() ? "" : "  •  GSTIN: " + storeGst), SwingConstants.CENTER);
            lblMeta.setFont(AppTheme.font(Font.PLAIN, 10));
            lblMeta.setForeground(AppTheme.TEXT_MUTED);
            lblMeta.setAlignmentX(Component.CENTER_ALIGNMENT);
            paper.add(lblMeta);
            paper.add(Box.createVerticalStrut(8));

            paper.add(createDashedDivider());
            paper.add(Box.createVerticalStrut(6));

            paper.add(createTwoColRow("Invoice No : " + invNo, "Date: " + dateStr, true, false));
            String cust = (currentSale != null && currentSale.getCustomerName() != null && !currentSale.getCustomerName().isEmpty())
                    ? currentSale.getCustomerName() : "Walk-in Customer";
            if (currentSale != null && currentSale.getCustomerPhone() != null && !currentSale.getCustomerPhone().isEmpty()) {
                cust += " (" + currentSale.getCustomerPhone() + ")";
            }
            paper.add(createTwoColRow("Customer   : " + cust, "", false, false));
            String cashier = (currentSale != null && currentSale.getCashierName() != null) ? currentSale.getCashierName() : "Admin";
            String pay = (currentSale != null && currentSale.getPaymentMode() != null) ? currentSale.getPaymentMode() : "CASH";
            paper.add(createTwoColRow("Cashier    : " + cashier, "Payment: " + pay, false, false));
            paper.add(Box.createVerticalStrut(6));

            paper.add(createDashedDivider());
            paper.add(Box.createVerticalStrut(4));

            // 4-Column Table Header
            paper.add(createFourColRow("ITEM DESCRIPTION", "QTY", "RATE", "AMOUNT", true));
            paper.add(Box.createVerticalStrut(4));

            if (currentSale != null && currentSale.getItems() != null) {
                for (SaleItem item : currentSale.getItems()) {
                    paper.add(createFourColRow(
                            item.getProductName(),
                            String.valueOf(item.getQuantity()),
                            String.format("%.2f", item.getUnitPrice()),
                            String.format("%.2f", item.getSubtotal()),
                            false
                    ));
                    paper.add(Box.createVerticalStrut(3));
                }
            }

            paper.add(createDashedDivider());
            paper.add(Box.createVerticalStrut(6));

            double subtotal = currentSale != null ? currentSale.getSubtotal() : 0.0;
            double discount = currentSale != null ? currentSale.getDiscountAmount() : 0.0;
            double gstAmt = currentSale != null ? currentSale.getGstAmount() : 0.0;
            int gstRate = currentSale != null ? (int) currentSale.getGstRate() : 0;
            double total = currentSale != null ? currentSale.getTotalAmount() : 0.0;

            paper.add(createTwoColRow("Subtotal (₹)", String.format("₹%.2f", subtotal), false, false));
            if (discount > 0) {
                paper.add(createTwoColRow("Discount", String.format("-₹%.2f", discount), false, false));
            }
            paper.add(createTwoColRow("GST (" + gstRate + "%)", String.format("₹%.2f", gstAmt), false, false));
            paper.add(Box.createVerticalStrut(6));

            paper.add(createDoubleDivider());
            paper.add(Box.createVerticalStrut(4));
            paper.add(createTwoColRow("GRAND TOTAL", String.format("₹%.2f", total), true, true, 15, AppTheme.FOREST_DEEP));
            paper.add(Box.createVerticalStrut(4));
            paper.add(createDoubleDivider());
            paper.add(Box.createVerticalStrut(10));

            // Barcode
            BufferedImage barcodeImg = BarcodeUtil.generateBarcodeImage(invNo, 260, 48, true);
            JLabel lblBarcode = new JLabel(new ImageIcon(barcodeImg), SwingConstants.CENTER);
            lblBarcode.setAlignmentX(Component.CENTER_ALIGNMENT);
            paper.add(lblBarcode);
            paper.add(Box.createVerticalStrut(10));

            JLabel lblThanks = new JLabel("Thank you for shopping with us!", SwingConstants.CENTER);
            lblThanks.setFont(AppTheme.font(Font.BOLD, 11));
            lblThanks.setForeground(AppTheme.TEXT_SECONDARY);
            lblThanks.setAlignmentX(Component.CENTER_ALIGNMENT);
            paper.add(lblThanks);

            JLabel lblPolicy = new JLabel("Exchange within 7 days with original receipt.", SwingConstants.CENTER);
            lblPolicy.setFont(AppTheme.font(Font.PLAIN, 10));
            lblPolicy.setForeground(AppTheme.TEXT_MUTED);
            lblPolicy.setAlignmentX(Component.CENTER_ALIGNMENT);
            paper.add(lblPolicy);

        } else {
            // ─────────────────────────────────────────────────────────────────
            // STANDARD A4 INVOICE
            // ─────────────────────────────────────────────────────────────────
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setOpaque(false);

            JPanel storeBox = new JPanel();
            storeBox.setLayout(new BoxLayout(storeBox, BoxLayout.Y_AXIS));
            storeBox.setOpaque(false);

            JLabel lblStore = new JLabel(storeName);
            lblStore.setFont(AppTheme.font(Font.BOLD, 17));
            lblStore.setForeground(AppTheme.FOREST_DEEP);
            storeBox.add(lblStore);

            JLabel lblAddr = new JLabel("Tech Plaza, Main Market, New Delhi");
            lblAddr.setFont(AppTheme.font(Font.PLAIN, 10));
            lblAddr.setForeground(AppTheme.TEXT_SECONDARY);
            storeBox.add(lblAddr);

            JLabel lblGst = new JLabel("Phone: +91 " + storePhone + " | GSTIN: " + storeGst);
            lblGst.setFont(AppTheme.font(Font.PLAIN, 10));
            lblGst.setForeground(AppTheme.TEXT_MUTED);
            storeBox.add(lblGst);
            headerPanel.add(storeBox, BorderLayout.WEST);

            JPanel invBadgeBox = new JPanel();
            invBadgeBox.setLayout(new BoxLayout(invBadgeBox, BoxLayout.Y_AXIS));
            invBadgeBox.setOpaque(false);

            JLabel lblInvTitle = new JLabel("TAX INVOICE", SwingConstants.RIGHT);
            lblInvTitle.setFont(AppTheme.font(Font.BOLD, 16));
            lblInvTitle.setForeground(AppTheme.FOREST_MID);
            lblInvTitle.setAlignmentX(Component.RIGHT_ALIGNMENT);
            invBadgeBox.add(lblInvTitle);

            JLabel lblInvNum = new JLabel("No: " + invNo, SwingConstants.RIGHT);
            lblInvNum.setFont(AppTheme.font(Font.BOLD, 11));
            lblInvNum.setForeground(AppTheme.TEXT_PRIMARY);
            lblInvNum.setAlignmentX(Component.RIGHT_ALIGNMENT);
            invBadgeBox.add(lblInvNum);

            JLabel lblInvDate = new JLabel("Date: " + dateStr, SwingConstants.RIGHT);
            lblInvDate.setFont(AppTheme.font(Font.PLAIN, 10));
            lblInvDate.setForeground(AppTheme.TEXT_MUTED);
            lblInvDate.setAlignmentX(Component.RIGHT_ALIGNMENT);
            invBadgeBox.add(lblInvDate);

            headerPanel.add(invBadgeBox, BorderLayout.EAST);
            paper.add(headerPanel);
            paper.add(Box.createVerticalStrut(10));

            paper.add(createDashedDivider());
            paper.add(Box.createVerticalStrut(8));

            // Customer Details Card
            String cust = (currentSale != null && currentSale.getCustomerName() != null && !currentSale.getCustomerName().isEmpty())
                    ? currentSale.getCustomerName() : "Walk-in Customer";
            String phone = (currentSale != null && currentSale.getCustomerPhone() != null) ? currentSale.getCustomerPhone() : "";
            String payMode = (currentSale != null && currentSale.getPaymentMode() != null) ? currentSale.getPaymentMode() : "CASH";

            paper.add(createTwoColRow("Billed To: " + cust, "Payment Method: " + payMode, true, false));
            if (!phone.isEmpty()) {
                paper.add(createTwoColRow("Contact  : " + phone, "", false, false));
            }
            paper.add(Box.createVerticalStrut(8));

            // 4-Column Table Header
            paper.add(createFourColRow("PRODUCT DESCRIPTION", "QTY", "UNIT RATE (₹)", "AMOUNT (₹)", true));
            paper.add(Box.createVerticalStrut(4));

            if (currentSale != null && currentSale.getItems() != null) {
                for (SaleItem item : currentSale.getItems()) {
                    paper.add(createFourColRow(
                            item.getProductName(),
                            String.valueOf(item.getQuantity()),
                            String.format("%.2f", item.getUnitPrice()),
                            String.format("%.2f", item.getSubtotal()),
                            false
                    ));
                    paper.add(Box.createVerticalStrut(3));
                }
            }

            paper.add(Box.createVerticalStrut(6));
            paper.add(createDashedDivider());
            paper.add(Box.createVerticalStrut(6));

            double subtotal = currentSale != null ? currentSale.getSubtotal() : 0.0;
            double discount = currentSale != null ? currentSale.getDiscountAmount() : 0.0;
            double gstAmt = currentSale != null ? currentSale.getGstAmount() : 0.0;
            double total = currentSale != null ? currentSale.getTotalAmount() : 0.0;

            paper.add(createTwoColRow("Subtotal", String.format("₹%,.2f", subtotal), false, false));
            if (discount > 0) {
                paper.add(createTwoColRow("Discount (" + currentSale.getDiscountType() + ")", String.format("-₹%,.2f", discount), false, false));
            }
            paper.add(createTwoColRow("Goods & Services Tax (GST)", String.format("₹%,.2f", gstAmt), false, false));
            paper.add(Box.createVerticalStrut(6));

            paper.add(createDoubleDivider());
            paper.add(Box.createVerticalStrut(4));
            paper.add(createTwoColRow("INVOICE TOTAL", String.format("₹%,.2f", total), true, true, 16, AppTheme.FOREST_DEEP));
            paper.add(Box.createVerticalStrut(4));
            paper.add(createDoubleDivider());
            paper.add(Box.createVerticalStrut(12));

            // Barcode & Signature
            JPanel footerCols = new JPanel(new BorderLayout());
            footerCols.setOpaque(false);

            BufferedImage barcodeImg = BarcodeUtil.generateBarcodeImage(invNo, 220, 44, true);
            JLabel lblBarcode = new JLabel(new ImageIcon(barcodeImg), SwingConstants.LEFT);
            footerCols.add(lblBarcode, BorderLayout.WEST);

            JPanel signBox = new JPanel();
            signBox.setLayout(new BoxLayout(signBox, BoxLayout.Y_AXIS));
            signBox.setOpaque(false);
            JLabel lblSign = new JLabel("Authorized Signatory", SwingConstants.RIGHT);
            lblSign.setFont(AppTheme.font(Font.BOLD, 10));
            lblSign.setForeground(AppTheme.TEXT_SECONDARY);
            lblSign.setAlignmentX(Component.RIGHT_ALIGNMENT);
            signBox.add(Box.createVerticalStrut(20));
            signBox.add(lblSign);
            footerCols.add(signBox, BorderLayout.EAST);

            paper.add(footerCols);
            paper.add(Box.createVerticalStrut(10));

            JLabel lblTerms = new JLabel("Thank you for your business! Goods once sold can be exchanged within 7 days.", SwingConstants.CENTER);
            lblTerms.setFont(AppTheme.font(Font.PLAIN, 9));
            lblTerms.setForeground(AppTheme.TEXT_MUTED);
            lblTerms.setAlignmentX(Component.CENTER_ALIGNMENT);
            paper.add(lblTerms);
        }

        // Adjust preferred size to exact paper dimensions
        paper.setPreferredSize(new Dimension(paperWidth, paper.getPreferredSize().height));
        return paper;
    }

    // =========================================================================
    // PRINTING ACTION
    // =========================================================================
    private void handlePrintReceipt() {
        if (currentReceiptPaper == null) return;
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Receipt_" + (currentSale != null ? currentSale.getInvoiceNo() : "POS"));
            job.setPrintable(currentReceiptPaper);

            if (job.printDialog()) {
                job.print();
                JOptionPane.showMessageDialog(this, "Receipt Print Job Sent Successfully!", "Print", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Print error: " + ex.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupShortcuts() {
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        getRootPane().registerKeyboardAction(e -> handlePrintReceipt(),
                KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // =========================================================================
    // UI COMPONENT BUILDERS (Clean Grid Columns & Dashed Lines)
    // =========================================================================
    private JPanel createTwoColRow(String left, String right, boolean leftBold, boolean rightBold) {
        return createTwoColRow(left, right, leftBold, rightBold, 11, AppTheme.TEXT_PRIMARY);
    }

    private JPanel createTwoColRow(String left, String right, boolean leftBold, boolean rightBold, int fontSize, Color color) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, fontSize + 8));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel l = new JLabel(left);
        l.setFont(AppTheme.font(leftBold ? Font.BOLD : Font.PLAIN, fontSize));
        l.setForeground(color);
        p.add(l, BorderLayout.WEST);

        if (right != null && !right.isEmpty()) {
            JLabel r = new JLabel(right);
            r.setFont(AppTheme.font(rightBold ? Font.BOLD : Font.PLAIN, fontSize));
            r.setForeground(color);
            p.add(r, BorderLayout.EAST);
        }
        return p;
    }

    private JPanel createFourColRow(String desc, String qty, String rate, String amt, boolean isHeader) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, isHeader ? 22 : 18));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 2, 0, 2);

        int style = isHeader ? Font.BOLD : Font.PLAIN;
        Color fg = isHeader ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY;

        // Description (Takes remaining width)
        c.gridx = 0;
        c.weightx = 1.0;
        JLabel lblDesc = new JLabel(desc);
        lblDesc.setFont(AppTheme.font(style, 10));
        lblDesc.setForeground(fg);
        p.add(lblDesc, c);

        // Qty
        c.gridx = 1;
        c.weightx = 0.0;
        JLabel lblQty = new JLabel(qty, SwingConstants.CENTER);
        lblQty.setPreferredSize(new Dimension(36, 16));
        lblQty.setFont(AppTheme.font(style, 10));
        lblQty.setForeground(fg);
        p.add(lblQty, c);

        // Rate
        c.gridx = 2;
        JLabel lblRate = new JLabel(rate, SwingConstants.RIGHT);
        lblRate.setPreferredSize(new Dimension(56, 16));
        lblRate.setFont(AppTheme.font(style, 10));
        lblRate.setForeground(fg);
        p.add(lblRate, c);

        // Amount
        c.gridx = 3;
        JLabel lblAmt = new JLabel(amt, SwingConstants.RIGHT);
        lblAmt.setPreferredSize(new Dimension(64, 16));
        lblAmt.setFont(AppTheme.font(isHeader ? Font.BOLD : Font.BOLD, 10));
        lblAmt.setForeground(isHeader ? fg : AppTheme.TEXT_PRIMARY);
        p.add(lblAmt, c);

        return p;
    }

    private JComponent createDashedDivider() {
        return new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(205, 222, 215));
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0f));
                int y = getHeight() / 2;
                g2.drawLine(0, y, getWidth(), y);
                g2.dispose();
            }
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(0, 8);
            }
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, 8);
            }
        };
    }

    private JComponent createDoubleDivider() {
        return new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(80, 115, 98));
                int y = getHeight() / 2;
                g2.drawLine(0, y - 2, getWidth(), y - 2);
                g2.drawLine(0, y + 2, getWidth(), y + 2);
                g2.dispose();
            }
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(0, 10);
            }
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, 10);
            }
        };
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
                new EmptyBorder(2, 8, 2, 6)
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

    private JButton createPillButton(String text, String iconKey, Color bg, Color border, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            private final SidebarIcon icon = (iconKey != null) ? new SidebarIcon(iconKey, 14, fg) : null;
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

                Color fill = hovered ? (bg.equals(AppTheme.FOREST_GREEN) ? AppTheme.FOREST_MID : (border != null ? new Color(241, 245, 249) : bg.darker())) : bg;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w, h, h, h);

                if (border != null) {
                    g2.setColor(border);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                }

                int iconSz = (icon != null) ? icon.getIconWidth() : 0;
                int gap = (icon != null && !getText().isEmpty()) ? 8 : 0;

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(getText());
                int contentW = iconSz + gap + textW;

                int curX = (w - contentW) / 2;
                if (icon != null) {
                    icon.paintIcon(this, g2, curX, (h - icon.getIconHeight()) / 2);
                    curX += iconSz + gap;
                }

                g2.setColor(fg);
                int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), curX, ty);

                g2.dispose();
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // =========================================================================
    // INNER CLASS: RECEIPT PAPER PANEL (Printable Canvas with Paper Slip Drop Shadow)
    // =========================================================================
    private static class ReceiptPaperPanel extends JPanel implements Printable {
        private boolean isPrinting = false;

        public ReceiptPaperPanel(int width) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setBorder(new EmptyBorder(18, 16, 18, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            if (!isPrinting) {
                // Soft realistic paper drop shadow
                g2.setColor(new Color(0, 40, 20, 8));
                g2.fillRoundRect(2, 4, w - 4, h - 2, 8, 8);
                g2.setColor(new Color(0, 40, 20, 14));
                g2.fillRoundRect(1, 2, w - 2, h - 2, 8, 8);
            }

            // Crisp white paper background
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 6, 6);

            if (!isPrinting) {
                // Subtle organic edge border
                g2.setColor(new Color(218, 235, 226));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);
            }

            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex > 0) return NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            double x = pageFormat.getImageableX();
            double y = pageFormat.getImageableY();
            double printableWidth = pageFormat.getImageableWidth();

            double scale = printableWidth / getWidth();
            if (scale > 1.0) scale = 1.0;

            g2.translate(x, y);
            g2.scale(scale, scale);

            isPrinting = true;
            paint(g2);
            isPrinting = false;

            return PAGE_EXISTS;
        }
    }
}
