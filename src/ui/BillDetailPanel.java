package ui;

import dao.BillingDAO;
import model.Sale;
import model.SaleItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Modern Bill & Order Details Panel.
 * Strictly adheres to the Keyra Minimalist Design System:
 * - Clean elevated cards (pure white surfaces, subtle drop shadows & borderless sage styling)
 * - Zero emoji clutter: SVG vector SidebarIcons for all metrics, headers, and actions
 * - Cohesive summary cards replacing old rainbow boxes
 * - High-DPI itemized order ledger table
 * - Pill-shaped action buttons with anti-aliased Java2D text rendering
 */
public class BillDetailPanel extends JPanel {
    private final Window parentWindow;
    private final Sale sale;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public BillDetailPanel(Window parent, Sale sale) {
        this.parentWindow = parent;
        this.sale = sale;

        // Ensure full item details are loaded
        if (sale != null && (sale.getItems() == null || sale.getItems().isEmpty())) {
            try {
                BillingDAO billingDAO = new BillingDAO();
                Sale fullSale = billingDAO.getSaleByInvoice(sale.getInvoiceNo());
                if (fullSale != null && fullSale.getItems() != null) {
                    sale.setItems(fullSale.getItems());
                }
            } catch (Exception ignored) {}
        }

        setLayout(new BorderLayout(0, 14));
        setBackground(AppTheme.BG_CANVAS);
        setBorder(new EmptyBorder(18, 22, 18, 22));

        initComponents();

        // Escape key to close dialog
        registerKeyboardAction(e -> {
            if (parentWindow != null) parentWindow.dispose();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void initComponents() {
        // =========================================================================
        // 1. TOP SECTION: HEADER & 4 COHESIVE SUMMARY CARDS
        // =========================================================================
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        // --- 1.1 Header Title Row ---
        JPanel titleRow = new JPanel(new BorderLayout(12, 0));
        titleRow.setOpaque(false);
        titleRow.setBorder(new EmptyBorder(0, 2, 8, 2));

        JPanel invInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        invInfo.setOpaque(false);

        JLabel lblInv = new JLabel(sale != null ? sale.getInvoiceNo() : "INV-UNKNOWN");
        lblInv.setFont(AppTheme.font(Font.BOLD, 22));
        lblInv.setForeground(AppTheme.TEXT_PRIMARY);

        // Paid & Settled Pill Badge
        JPanel paidBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(236, 253, 245));
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.setColor(new Color(167, 243, 208));
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        paidBadge.setOpaque(false);
        paidBadge.setBorder(new EmptyBorder(2, 10, 2, 10));

        SidebarIcon checkIco = new SidebarIcon("check", 12, new Color(16, 185, 129));
        JLabel lblCheckIcon = new JLabel(checkIco);
        JLabel lblPaidText = new JLabel("Completed & Paid");
        lblPaidText.setFont(AppTheme.font(Font.BOLD, 11));
        lblPaidText.setForeground(new Color(16, 185, 129));
        paidBadge.add(lblCheckIcon);
        paidBadge.add(lblPaidText);

        invInfo.add(lblInv);
        invInfo.add(paidBadge);

        JLabel lblDateStr = new JLabel(sale != null && sale.getSaleDate() != null ? "Billed on: " + sdf.format(sale.getSaleDate()) : "");
        lblDateStr.setFont(AppTheme.font(Font.PLAIN, 12));
        lblDateStr.setForeground(AppTheme.TEXT_SECONDARY);

        titleRow.add(invInfo, BorderLayout.WEST);
        titleRow.add(lblDateStr, BorderLayout.EAST);
        topContainer.add(titleRow);
        topContainer.add(Box.createVerticalStrut(10));

        // --- 1.2 4 Cohesive Summary Cards Grid ---
        JPanel metaGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        metaGrid.setOpaque(false);

        String custName = (sale != null && sale.getCustomerName() != null && !sale.getCustomerName().isEmpty())
                ? sale.getCustomerName() : "Walk-in Customer";
        String custPhone = (sale != null && sale.getCustomerPhone() != null && !sale.getCustomerPhone().isEmpty())
                ? sale.getCustomerPhone() : "No phone provided";
        String payMode = (sale != null && sale.getPaymentMode() != null) ? sale.getPaymentMode() : "CASH";
        String cashier = (sale != null && sale.getCashierName() != null && !sale.getCashierName().isEmpty())
                ? sale.getCashierName() : "System Administrator";

        int totalItems = (sale != null && sale.getItems() != null) ? sale.getItems().size() : (sale != null ? sale.getItemCount() : 0);
        int totalUnits = 0;
        if (sale != null && sale.getItems() != null) {
            for (SaleItem it : sale.getItems()) totalUnits += it.getQuantity();
        } else if (sale != null) {
            totalUnits = sale.getTotalUnits();
        }

        metaGrid.add(createModernSummaryCard("CUSTOMER", custName, custPhone, "user"));
        metaGrid.add(createModernSummaryCard("PAYMENT METHOD", payMode, "Status: Verified", "wallet"));
        metaGrid.add(createModernSummaryCard("ITEMS PURCHASED", totalItems + " Unique Items", totalUnits + " Total Units", "package"));
        metaGrid.add(createModernSummaryCard("CASHIER / DESK", cashier, "POS Station Active", "shield"));

        topContainer.add(metaGrid);
        add(topContainer, BorderLayout.NORTH);

        // =========================================================================
        // 2. CENTER: ITEMIZED PRODUCTS TABLE CARD
        // =========================================================================
        JPanel tableCard = createElevatedCard(20);
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.setBorder(new EmptyBorder(14, 16, 14, 16));

        // Table Header Label
        JPanel tblTitleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tblTitleRow.setOpaque(false);
        SidebarIcon boxIcon = new SidebarIcon("package", 14, AppTheme.FOREST_GREEN);
        JLabel lblTblIcon = new JLabel(boxIcon);
        JLabel lblItemsTitle = new JLabel("Itemized Order Items");
        lblItemsTitle.setFont(AppTheme.font(Font.BOLD, 13));
        lblItemsTitle.setForeground(AppTheme.TEXT_PRIMARY);
        tblTitleRow.add(lblTblIcon);
        tblTitleRow.add(lblItemsTitle);
        tableCard.add(tblTitleRow, BorderLayout.NORTH);

        String[] cols = {"#", "Product / Item Name", "Item Code / SKU", "Unit Price", "Quantity", "Item Subtotal"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        if (sale != null && sale.getItems() != null) {
            List<SaleItem> items = sale.getItems();
            for (int i = 0; i < items.size(); i++) {
                SaleItem item = items.get(i);
                String code = item.getProductCode() != null && !item.getProductCode().isEmpty() ? item.getProductCode() : "—";
                model.addRow(new Object[]{
                        i + 1,
                        item.getProductName(),
                        code,
                        String.format("₹%,.2f", item.getUnitPrice()),
                        item.getQuantity(),
                        String.format("₹%,.2f", item.getSubtotal())
                });
            }
        }

        JTable table = new JTable(model);
        setupTableStyle(table);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(45);
        table.getColumnModel().getColumn(0).setMaxWidth(55);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(130);

        // Renderers
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        centerRender.setFont(AppTheme.font(Font.BOLD, 12));
        centerRender.setForeground(AppTheme.TEXT_MUTED);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        DefaultTableCellRenderer nameRender = new DefaultTableCellRenderer();
        nameRender.setFont(AppTheme.font(Font.BOLD, 12));
        nameRender.setForeground(AppTheme.TEXT_PRIMARY);
        nameRender.setBorder(new EmptyBorder(0, 10, 0, 10));
        table.getColumnModel().getColumn(1).setCellRenderer(nameRender);

        DefaultTableCellRenderer codeRender = new DefaultTableCellRenderer();
        codeRender.setHorizontalAlignment(SwingConstants.CENTER);
        codeRender.setFont(new Font("Monospaced", Font.PLAIN, 11));
        codeRender.setForeground(AppTheme.TEXT_SECONDARY);
        table.getColumnModel().getColumn(2).setCellRenderer(codeRender);

        DefaultTableCellRenderer priceRender = new DefaultTableCellRenderer();
        priceRender.setHorizontalAlignment(SwingConstants.RIGHT);
        priceRender.setFont(AppTheme.font(Font.PLAIN, 12));
        priceRender.setBorder(new EmptyBorder(0, 4, 0, 14));
        table.getColumnModel().getColumn(3).setCellRenderer(priceRender);

        DefaultTableCellRenderer qtyRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(AppTheme.font(Font.BOLD, 12));
                l.setForeground(AppTheme.TEXT_PRIMARY);
                return l;
            }
        };
        table.getColumnModel().getColumn(4).setCellRenderer(qtyRender);

        DefaultTableCellRenderer subtotalRender = new DefaultTableCellRenderer();
        subtotalRender.setHorizontalAlignment(SwingConstants.RIGHT);
        subtotalRender.setFont(AppTheme.font(Font.BOLD, 12));
        subtotalRender.setForeground(AppTheme.FOREST_GREEN);
        subtotalRender.setBorder(new EmptyBorder(0, 4, 0, 14));
        table.getColumnModel().getColumn(5).setCellRenderer(subtotalRender);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        tableCard.add(scroll, BorderLayout.CENTER);

        add(tableCard, BorderLayout.CENTER);

        // =========================================================================
        // 3. BOTTOM: FINANCIAL BREAKDOWN CARD & PILL ACTION BUTTONS
        // =========================================================================
        JPanel bottomContainer = new JPanel();
        bottomContainer.setLayout(new BoxLayout(bottomContainer, BoxLayout.Y_AXIS));
        bottomContainer.setOpaque(false);

        // --- 3.1 Financial Breakdown Card ---
        JPanel summaryCard = createElevatedCard(16);
        summaryCard.setLayout(new BorderLayout(16, 0));
        summaryCard.setBorder(new EmptyBorder(12, 18, 12, 18));

        // Left Note & Security Verification
        JPanel leftNote = new JPanel(new GridLayout(2, 1, 0, 4));
        leftNote.setOpaque(false);

        JLabel lblGstNote = new JLabel(String.format("Tax Breakdown: GST (%.1f%%)", sale != null ? sale.getGstRate() : 18.0));
        lblGstNote.setFont(AppTheme.font(Font.BOLD, 12));
        lblGstNote.setForeground(AppTheme.TEXT_SECONDARY);

        JPanel verifiedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        verifiedRow.setOpaque(false);
        SidebarIcon shieldIco = new SidebarIcon("shield", 13, AppTheme.FOREST_MID);
        JLabel lblShield = new JLabel(shieldIco);
        JLabel lblVerified = new JLabel("Electronically Generated Tax Invoice • Verified & Settled");
        lblVerified.setFont(AppTheme.font(Font.PLAIN, 11));
        lblVerified.setForeground(AppTheme.TEXT_MUTED);
        verifiedRow.add(lblShield);
        verifiedRow.add(lblVerified);

        leftNote.add(lblGstNote);
        leftNote.add(verifiedRow);
        summaryCard.add(leftNote, BorderLayout.WEST);

        // Right Totals
        JPanel rightTotals = new JPanel(new GridLayout(3, 2, 16, 3));
        rightTotals.setOpaque(false);

        double subtotal = sale != null ? sale.getSubtotal() : 0.0;
        double gstAmt = sale != null ? sale.getGstAmount() : 0.0;
        double grandTotal = sale != null ? sale.getTotalAmount() : 0.0;

        rightTotals.add(createTotalLabel("Subtotal (Taxable):", false, AppTheme.TEXT_SECONDARY));
        rightTotals.add(createTotalValue(String.format("₹%,.2f", subtotal), false, AppTheme.TEXT_PRIMARY));

        rightTotals.add(createTotalLabel("GST Amount:", false, AppTheme.TEXT_SECONDARY));
        rightTotals.add(createTotalValue(String.format("₹%,.2f", gstAmt), false, AppTheme.TEXT_PRIMARY));

        rightTotals.add(createTotalLabel("Grand Total:", true, AppTheme.TEXT_PRIMARY));
        rightTotals.add(createTotalValue(String.format("₹%,.2f", grandTotal), true, AppTheme.FOREST_GREEN));

        summaryCard.add(rightTotals, BorderLayout.EAST);
        bottomContainer.add(summaryCard);
        bottomContainer.add(Box.createVerticalStrut(12));

        // --- 3.2 Action Buttons Row ---
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionRow.setOpaque(false);

        JButton btnClose = createSecondaryPillButton("Close");
        btnClose.addActionListener(e -> {
            if (parentWindow != null) parentWindow.dispose();
        });

        JButton btnPrintSlip = createPrimaryPillButton("Print Receipt Slip", "printer");
        btnPrintSlip.addActionListener(e -> {
            Frame f = parentWindow instanceof Frame ? (Frame) parentWindow : null;
            InvoiceDialog dlg = new InvoiceDialog(f, sale);
            dlg.setVisible(true);
        });

        actionRow.add(btnClose);
        actionRow.add(btnPrintSlip);
        bottomContainer.add(actionRow);

        add(bottomContainer, BorderLayout.SOUTH);
    }

    private JPanel createModernSummaryCard(String title, String mainVal, String subVal, String iconName) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Pure white surface
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);

                // Subtle sage border
                g2.setColor(new Color(230, 238, 234));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Title row: SVG Icon + Uppercase Category
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        SidebarIcon ico = new SidebarIcon(iconName, 12, AppTheme.FOREST_MID);
        JLabel lblIcon = new JLabel(ico);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(AppTheme.font(Font.BOLD, 10));
        lblTitle.setForeground(AppTheme.TEXT_MUTED);
        titleRow.add(lblIcon);
        titleRow.add(lblTitle);

        card.add(titleRow);
        card.add(Box.createVerticalStrut(4));

        // Main value
        JLabel lblMain = new JLabel(mainVal);
        lblMain.setFont(AppTheme.font(Font.BOLD, 13));
        lblMain.setForeground(AppTheme.TEXT_PRIMARY);
        lblMain.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblMain);
        card.add(Box.createVerticalStrut(2));

        // Subtitle
        JLabel lblSub = new JLabel(subVal);
        lblSub.setFont(AppTheme.font(Font.PLAIN, 11));
        lblSub.setForeground(AppTheme.TEXT_SECONDARY);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblSub);

        return card;
    }

    private void setupTableStyle(JTable t) {
        t.setRowHeight(38);
        t.setFont(AppTheme.font(Font.PLAIN, 12));
        t.setShowVerticalLines(false);
        t.setGridColor(new Color(241, 246, 243));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setSelectionBackground(new Color(230, 248, 238));
        t.setSelectionForeground(AppTheme.TEXT_PRIMARY);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        t.setFillsViewportHeight(true);

        JTableHeader header = t.getTableHeader();
        header.setPreferredSize(new Dimension(0, 34));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, focus, r, c);
                l.setFont(AppTheme.font(Font.BOLD, 11));
                l.setForeground(AppTheme.TEXT_PRIMARY);
                l.setBackground(new Color(245, 250, 247));
                l.setOpaque(true);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_SAGE),
                        new EmptyBorder(0, 10, 0, 10)
                ));
                if (c == 0 || c == 2 || c == 4) {
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (c == 3 || c == 5) {
                    l.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return l;
            }
        });
    }

    private JPanel createElevatedCard(int arc) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                // Drop shadow
                g2.setColor(new Color(0, 50, 30, 6));
                g2.fillRoundRect(1, 2, w - 2, h - 2, arc, arc);

                // Pure white surface
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        return card;
    }

    private JButton createPrimaryPillButton(String text, String iconName) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                if (iconName != null) {
                    setIcon(new SidebarIcon(iconName, 14, Color.WHITE));
                }
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

                g2.setColor(hovered ? AppTheme.FOREST_MID : AppTheme.FOREST_GREEN);
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(170, 36));
        btn.setIconTextGap(8);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createSecondaryPillButton(String text) {
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
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                g2.setColor(hovered ? new Color(240, 246, 243) : Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 12));
        btn.setForeground(AppTheme.TEXT_PRIMARY);
        btn.setPreferredSize(new Dimension(90, 36));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel createTotalLabel(String text, boolean bold, Color color) {
        JLabel l = new JLabel(text, SwingConstants.RIGHT);
        l.setFont(AppTheme.font(bold ? Font.BOLD : Font.PLAIN, bold ? 13 : 12));
        l.setForeground(color);
        return l;
    }

    private JLabel createTotalValue(String text, boolean bold, Color color) {
        JLabel l = new JLabel(text, SwingConstants.RIGHT);
        l.setFont(AppTheme.font(bold ? Font.BOLD : Font.PLAIN, bold ? 16 : 12));
        l.setForeground(color);
        return l;
    }
}
