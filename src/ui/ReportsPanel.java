package ui;

import dao.ReportDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * ReportsPanel - Executive Financial & Tax Audit Reporting Suite.
 * Rebuilt to match the Keyra Design System (r=20 borderless cards, ambient shadows,
 * stadium pill buttons, vector Lucide icons, and zero emojis).
 */
public class ReportsPanel extends JPanel {
    private final ReportDAO reportDAO = new ReportDAO();

    // Date Range Presets State
    private String activePreset = "THIS_MONTH";
    private String filterStartDate = "";
    private String filterEndDate = "";
    private PillButton btnPresetToday, btnPresetWeek, btnPresetMonth, btnPresetYear, btnPresetCustom;
    private JPanel customDatePanel;
    private ModernInputField txtFromDate, txtToDate;

    // Executive Financial KPI Strip
    private JLabel lblTotalRevenue;
    private JLabel lblCostOfGoods;
    private JLabel lblGrossProfit;
    private JLabel lblProfitMargin;
    private JLabel lblTotalGst;

    // Segmented Tab Switcher
    private CardLayout tableCardLayout;
    private JPanel tableCardContainer;
    private String activeTab = "PROFIT_LOSS";
    private PillButton btnTabProfitLoss;
    private PillButton btnTabTaxAudit;
    private PillButton btnTabCashDrawer;
    private PillButton btnTabSalesRegister;

    // Data Tables & Models
    private JTable profitTable, taxTable, cashTable, salesTable;
    private DefaultTableModel profitModel, taxModel, cashModel, salesModel;
    private final List<String[]> profitData = new ArrayList<>();
    private final List<String[]> taxData = new ArrayList<>();
    private final List<String[]> cashData = new ArrayList<>();
    private final List<String[]> salesData = new ArrayList<>();

    // Search and Summary
    private ModernInputField txtTableSearch;
    private JLabel lblTableSummary;
    private JLabel lblRecordCount;

    public ReportsPanel() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_CANVAS);
        initDateRangeDefaults();
        initComponents();
        loadReports();
    }

    private void initDateRangeDefaults() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        filterEndDate = sdf.format(cal.getTime());
        // Default to This Month (1st day of current month)
        cal.set(Calendar.DAY_OF_MONTH, 1);
        filterStartDate = sdf.format(cal.getTime());
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 10));
        setBackground(AppTheme.BG_CANVAS);
        setBorder(new EmptyBorder(14, 22, 14, 22));

        // 1. TOP SECTION (Header & Date Presets + Financial KPI Strip)
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setOpaque(false);

        northPanel.add(createTopHeaderBar());
        northPanel.add(Box.createVerticalStrut(10));
        northPanel.add(createFinancialKpiStrip());

        add(northPanel, BorderLayout.NORTH);

        // 2. CENTER SECTION (Main Report Card: Tabs, Search, Table, Summary)
        add(createMainReportCard(), BorderLayout.CENTER);
    }

    // =========================================================================
    // 1. TOP HEADER & DATE CONTROLS BAR
    // =========================================================================
    private JPanel createTopHeaderBar() {
        JPanel header = new JPanel(new BorderLayout(16, 10));
        header.setOpaque(false);

        // Left: Clean title and vector icon
        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleBox.setOpaque(false);
        JLabel icon = new JLabel(new SidebarIcon("reports", 20, AppTheme.FOREST_GREEN));
        JLabel title = new JLabel("Financial Reports & Audit");
        title.setFont(AppTheme.font(Font.BOLD, 22));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        titleBox.add(icon);
        titleBox.add(title);
        header.add(titleBox, BorderLayout.WEST);

        // Right: Date Presets & Export Actions
        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        // Date Presets Segment
        JPanel presetGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        presetGroup.setOpaque(false);

        btnPresetToday = PillButton.preset("Today", "TODAY".equals(activePreset));
        btnPresetWeek  = PillButton.preset("This Week", "THIS_WEEK".equals(activePreset));
        btnPresetMonth = PillButton.preset("This Month", "THIS_MONTH".equals(activePreset));
        btnPresetYear  = PillButton.preset("This Year", "THIS_YEAR".equals(activePreset));
        btnPresetCustom = PillButton.preset("Custom", "CUSTOM".equals(activePreset));

        btnPresetToday.addActionListener(e -> selectPreset("TODAY"));
        btnPresetWeek.addActionListener(e -> selectPreset("THIS_WEEK"));
        btnPresetMonth.addActionListener(e -> selectPreset("THIS_MONTH"));
        btnPresetYear.addActionListener(e -> selectPreset("THIS_YEAR"));
        btnPresetCustom.addActionListener(e -> selectPreset("CUSTOM"));

        presetGroup.add(btnPresetToday);
        presetGroup.add(btnPresetWeek);
        presetGroup.add(btnPresetMonth);
        presetGroup.add(btnPresetYear);
        presetGroup.add(btnPresetCustom);
        rightActions.add(presetGroup);

        // Action Buttons: Export CSV & Print
        PillButton btnExport = PillButton.primary("Export CSV", new SidebarIcon("download", 13, Color.WHITE));
        btnExport.setPreferredSize(new Dimension(122, 34));
        btnExport.addActionListener(e -> exportActiveReportToCSV());
        rightActions.add(btnExport);

        PillButton btnPrint = PillButton.subtle("Print", new SidebarIcon("printer", 13, AppTheme.TEXT_PRIMARY));
        btnPrint.setPreferredSize(new Dimension(84, 34));
        btnPrint.addActionListener(e -> printActiveReport());
        rightActions.add(btnPrint);

        PillButton btnRefresh = PillButton.subtle("", new SidebarIcon("refresh", 13, AppTheme.TEXT_PRIMARY));
        btnRefresh.setPreferredSize(new Dimension(34, 34));
        btnRefresh.setToolTipText("Refresh Data");
        btnRefresh.addActionListener(e -> loadReports());
        rightActions.add(btnRefresh);

        header.add(rightActions, BorderLayout.EAST);

        // Custom Date Range Row (Hidden by default, shown when Custom preset is selected)
        customDatePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        customDatePanel.setOpaque(false);
        customDatePanel.setVisible(false);

        JLabel lblFrom = new JLabel("From:");
        lblFrom.setFont(AppTheme.font(Font.BOLD, 11));
        lblFrom.setForeground(AppTheme.TEXT_SECONDARY);

        txtFromDate = new ModernInputField(filterStartDate);
        txtFromDate.setPreferredSize(new Dimension(105, 30));

        JLabel lblTo = new JLabel("To:");
        lblTo.setFont(AppTheme.font(Font.BOLD, 11));
        lblTo.setForeground(AppTheme.TEXT_SECONDARY);

        txtToDate = new ModernInputField(filterEndDate);
        txtToDate.setPreferredSize(new Dimension(105, 30));

        PillButton btnApply = PillButton.secondary("Apply Filter", new SidebarIcon("check", 11, AppTheme.FOREST_GREEN));
        btnApply.setPreferredSize(new Dimension(105, 30));
        btnApply.addActionListener(e -> {
            filterStartDate = txtFromDate.getText().trim();
            filterEndDate = txtToDate.getText().trim();
            loadReports();
        });

        customDatePanel.add(lblFrom);
        customDatePanel.add(txtFromDate);
        customDatePanel.add(lblTo);
        customDatePanel.add(txtToDate);
        customDatePanel.add(btnApply);

        JPanel combinedHeader = new JPanel(new BorderLayout(0, 4));
        combinedHeader.setOpaque(false);
        combinedHeader.add(header, BorderLayout.NORTH);
        combinedHeader.add(customDatePanel, BorderLayout.SOUTH);

        return combinedHeader;
    }

    private void selectPreset(String preset) {
        this.activePreset = preset;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        filterEndDate = sdf.format(cal.getTime());

        switch (preset) {
            case "TODAY":
                filterStartDate = filterEndDate;
                customDatePanel.setVisible(false);
                break;
            case "THIS_WEEK":
                cal.add(Calendar.DAY_OF_YEAR, -6);
                filterStartDate = sdf.format(cal.getTime());
                customDatePanel.setVisible(false);
                break;
            case "THIS_MONTH":
                cal.set(Calendar.DAY_OF_MONTH, 1);
                filterStartDate = sdf.format(cal.getTime());
                customDatePanel.setVisible(false);
                break;
            case "THIS_YEAR":
                cal.set(Calendar.DAY_OF_YEAR, 1);
                filterStartDate = sdf.format(cal.getTime());
                customDatePanel.setVisible(false);
                break;
            case "CUSTOM":
                customDatePanel.setVisible(true);
                txtFromDate.setText(filterStartDate);
                txtToDate.setText(filterEndDate);
                break;
        }

        updatePresetPills();
        if (!"CUSTOM".equals(preset)) {
            loadReports();
        }
        revalidate();
        repaint();
    }

    private void updatePresetPills() {
        btnPresetToday.setActive("TODAY".equals(activePreset));
        btnPresetWeek.setActive("THIS_WEEK".equals(activePreset));
        btnPresetMonth.setActive("THIS_MONTH".equals(activePreset));
        btnPresetYear.setActive("THIS_YEAR".equals(activePreset));
        btnPresetCustom.setActive("CUSTOM".equals(activePreset));
    }

    // =========================================================================
    // 2. EXECUTIVE FINANCIAL KPI STRIP (4-5 Cards)
    // =========================================================================
    private JPanel createFinancialKpiStrip() {
        JPanel strip = new JPanel(new GridLayout(1, 5, 12, 0));
        strip.setOpaque(false);
        strip.setPreferredSize(new Dimension(0, 94));

        lblTotalRevenue = new JLabel("₹0.00");
        lblCostOfGoods = new JLabel("₹0.00");
        lblGrossProfit = new JLabel("₹0.00");
        lblProfitMargin = new JLabel("0.0%");
        lblTotalGst = new JLabel("₹0.00");

        strip.add(createKpiCard("GROSS SALES REVENUE", lblTotalRevenue, "wallet", AppTheme.FOREST_GREEN, "Total billed volume"));
        strip.add(createKpiCard("COST OF GOODS (COGS)", lblCostOfGoods, "boxes", AppTheme.FOREST_MID, "Wholesale cost base"));
        strip.add(createKpiCard("NET GROSS PROFIT", lblGrossProfit, "trending_up", AppTheme.FOREST_GREEN, "Revenue − Wholesale"));
        strip.add(createKpiCard("PROFIT MARGIN", lblProfitMargin, "pie_chart", new Color(14, 116, 144), "Gross return rate"));
        strip.add(createKpiCard("GST TAX COLLECTED", lblTotalGst, "file_text", new Color(180, 83, 9), "Total tax liability"));

        return strip;
    }

    private JPanel createKpiCard(String title, JLabel valLabel, String iconName, Color accent, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = 18;

                // Subtle ambient drop shadow
                g2.setColor(new Color(0, 50, 30, 6));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);

                // Pure white crisp card surface
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Top row: Title + Icon
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(AppTheme.font(Font.BOLD, 10));
        lblTitle.setForeground(AppTheme.TEXT_MUTED);

        JLabel lblIcon = new JLabel(new SidebarIcon(iconName, 15, accent));
        topRow.add(lblTitle, BorderLayout.WEST);
        topRow.add(lblIcon, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        // Center: Metric Value
        valLabel.setFont(AppTheme.font(Font.BOLD, 19));
        valLabel.setForeground(AppTheme.TEXT_PRIMARY);
        card.add(valLabel, BorderLayout.CENTER);

        // Bottom: Explanatory Subtitle
        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(AppTheme.font(Font.PLAIN, 10));
        lblSub.setForeground(AppTheme.TEXT_SECONDARY);
        card.add(lblSub, BorderLayout.SOUTH);

        return card;
    }

    // =========================================================================
    // 3. MAIN REPORT CARD (Switcher + Search + Tables + Summary)
    // =========================================================================
    private JPanel createMainReportCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = 20;

                // Subtle ambient drop shadow
                g2.setColor(new Color(0, 50, 30, 6));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);

                // Pure white crisp card surface
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 16, 12, 16));

        // Card Top: Tab Switcher (Left) + Search Filter (Right)
        JPanel controlsRow = new JPanel(new BorderLayout(12, 0));
        controlsRow.setOpaque(false);

        // Segmented Tab Switcher Buttons
        JPanel switcher = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        switcher.setOpaque(false);

        btnTabProfitLoss = PillButton.tab("Product Profit & Loss", new SidebarIcon("trending_up", 12, AppTheme.FOREST_GREEN), true);
        btnTabTaxAudit   = PillButton.tab("Tax & GST Audit Ledger", new SidebarIcon("file_text", 12, AppTheme.TEXT_SECONDARY), false);
        btnTabCashDrawer = PillButton.tab("Cash & Drawer Reconciliation", new SidebarIcon("wallet", 12, AppTheme.TEXT_SECONDARY), false);
        btnTabSalesRegister = PillButton.tab("Sales Register / Invoices", new SidebarIcon("billing", 12, AppTheme.TEXT_SECONDARY), false);

        btnTabProfitLoss.addActionListener(e -> switchTab("PROFIT_LOSS"));
        btnTabTaxAudit.addActionListener(e -> switchTab("TAX_AUDIT"));
        btnTabCashDrawer.addActionListener(e -> switchTab("CASH_DRAWER"));
        btnTabSalesRegister.addActionListener(e -> switchTab("SALES_REGISTER"));

        switcher.add(btnTabProfitLoss);
        switcher.add(btnTabTaxAudit);
        switcher.add(btnTabCashDrawer);
        switcher.add(btnTabSalesRegister);
        controlsRow.add(switcher, BorderLayout.WEST);

        // Search Input (Real-time client filtering across active table)
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchBox.setOpaque(false);

        JLabel lblSearchIcon = new JLabel(new SidebarIcon("search", 13, AppTheme.TEXT_MUTED));
        searchBox.add(lblSearchIcon);

        txtTableSearch = new ModernInputField("");
        txtTableSearch.setPreferredSize(new Dimension(210, 32));
        txtTableSearch.setToolTipText("Search records...");
        txtTableSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterActiveTable(); }
            @Override public void removeUpdate(DocumentEvent e) { filterActiveTable(); }
            @Override public void changedUpdate(DocumentEvent e) { filterActiveTable(); }
        });
        searchBox.add(txtTableSearch);
        controlsRow.add(searchBox, BorderLayout.EAST);

        card.add(controlsRow, BorderLayout.NORTH);

        // Card Center: Table CardLayout Container
        tableCardLayout = new CardLayout();
        tableCardContainer = new JPanel(tableCardLayout);
        tableCardContainer.setOpaque(false);

        // Build 4 Tables
        tableCardContainer.add(createProfitLossTableView(), "PROFIT_LOSS");
        tableCardContainer.add(createTaxAuditTableView(), "TAX_AUDIT");
        tableCardContainer.add(createCashDrawerTableView(), "CASH_DRAWER");
        tableCardContainer.add(createSalesRegisterTableView(), "SALES_REGISTER");

        card.add(tableCardContainer, BorderLayout.CENTER);

        // Card Bottom: Summary Strip & Record Count
        JPanel footerRow = new JPanel(new BorderLayout(10, 0));
        footerRow.setOpaque(false);
        footerRow.setBorder(new EmptyBorder(8, 6, 2, 6));

        lblRecordCount = new JLabel("0 records found");
        lblRecordCount.setFont(AppTheme.font(Font.BOLD, 11));
        lblRecordCount.setForeground(AppTheme.TEXT_MUTED);
        footerRow.add(lblRecordCount, BorderLayout.WEST);

        lblTableSummary = new JLabel("Ready for export");
        lblTableSummary.setFont(AppTheme.font(Font.BOLD, 12));
        lblTableSummary.setForeground(AppTheme.FOREST_DEEP);
        footerRow.add(lblTableSummary, BorderLayout.EAST);

        card.add(footerRow, BorderLayout.SOUTH);

        return card;
    }

    private void switchTab(String tabKey) {
        this.activeTab = tabKey;
        tableCardLayout.show(tableCardContainer, tabKey);

        btnTabProfitLoss.setActiveTab("PROFIT_LOSS".equals(tabKey));
        btnTabTaxAudit.setActiveTab("TAX_AUDIT".equals(tabKey));
        btnTabCashDrawer.setActiveTab("CASH_DRAWER".equals(tabKey));
        btnTabSalesRegister.setActiveTab("SALES_REGISTER".equals(tabKey));

        txtTableSearch.setText("");
        filterActiveTable();
    }

    // =========================================================================
    // 4. TABLE BUILDERS
    // =========================================================================
    private JScrollPane createProfitLossTableView() {
        String[] cols = {"Code", "Product Name", "Units Sold", "Sales Revenue", "Wholesale Cost", "Net Profit", "Margin"};
        profitModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        profitTable = styleTable(new JTable(profitModel));

        // Column widths & Alignments
        profitTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        profitTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        profitTable.getColumnModel().getColumn(2).setPreferredWidth(85);
        profitTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        profitTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        profitTable.getColumnModel().getColumn(5).setPreferredWidth(115);
        profitTable.getColumnModel().getColumn(6).setPreferredWidth(80);

        DefaultTableCellRenderer center = createCenterRenderer();
        DefaultTableCellRenderer right = createRightRenderer();

        profitTable.getColumnModel().getColumn(0).setCellRenderer(center);
        profitTable.getColumnModel().getColumn(2).setCellRenderer(center);
        profitTable.getColumnModel().getColumn(3).setCellRenderer(right);
        profitTable.getColumnModel().getColumn(4).setCellRenderer(right);
        profitTable.getColumnModel().getColumn(5).setCellRenderer(new ProfitBadgeRenderer());
        profitTable.getColumnModel().getColumn(6).setCellRenderer(center);

        return createTableScrollPane(profitTable);
    }

    private JScrollPane createTaxAuditTableView() {
        String[] cols = {"Invoice #", "Date & Time", "Customer Name", "Taxable (₹)", "GST %", "CGST (₹)", "SGST (₹)", "Total Tax (₹)", "Invoice Total (₹)"};
        taxModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        taxTable = styleTable(new JTable(taxModel));

        taxTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        taxTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        taxTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        taxTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        taxTable.getColumnModel().getColumn(4).setPreferredWidth(65);
        taxTable.getColumnModel().getColumn(5).setPreferredWidth(85);
        taxTable.getColumnModel().getColumn(6).setPreferredWidth(85);
        taxTable.getColumnModel().getColumn(7).setPreferredWidth(95);
        taxTable.getColumnModel().getColumn(8).setPreferredWidth(115);

        DefaultTableCellRenderer center = createCenterRenderer();
        DefaultTableCellRenderer right = createRightRenderer();

        taxTable.getColumnModel().getColumn(0).setCellRenderer(center);
        taxTable.getColumnModel().getColumn(1).setCellRenderer(center);
        taxTable.getColumnModel().getColumn(3).setCellRenderer(right);
        taxTable.getColumnModel().getColumn(4).setCellRenderer(center);
        taxTable.getColumnModel().getColumn(5).setCellRenderer(right);
        taxTable.getColumnModel().getColumn(6).setCellRenderer(right);
        taxTable.getColumnModel().getColumn(7).setCellRenderer(right);
        taxTable.getColumnModel().getColumn(8).setCellRenderer(right);

        return createTableScrollPane(taxTable);
    }

    private JScrollPane createCashDrawerTableView() {
        String[] cols = {"Payment Channel", "Invoices Count", "Total Revenue Collected", "% Share of Revenue", "Settlement Status"};
        cashModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        cashTable = styleTable(new JTable(cashModel));

        cashTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        cashTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        cashTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        cashTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        cashTable.getColumnModel().getColumn(4).setPreferredWidth(180);

        DefaultTableCellRenderer center = createCenterRenderer();
        DefaultTableCellRenderer right = createRightRenderer();

        cashTable.getColumnModel().getColumn(1).setCellRenderer(center);
        cashTable.getColumnModel().getColumn(2).setCellRenderer(right);
        cashTable.getColumnModel().getColumn(3).setCellRenderer(center);
        cashTable.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer());

        return createTableScrollPane(cashTable);
    }

    private JScrollPane createSalesRegisterTableView() {
        String[] cols = {"Invoice #", "Date & Time", "Customer", "Subtotal (₹)", "GST (₹)", "Total Amount (₹)", "Payment Mode"};
        salesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        salesTable = styleTable(new JTable(salesModel));

        salesTable.getColumnModel().getColumn(0).setPreferredWidth(110);
        salesTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        salesTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        salesTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        salesTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        salesTable.getColumnModel().getColumn(5).setPreferredWidth(120);
        salesTable.getColumnModel().getColumn(6).setPreferredWidth(110);

        DefaultTableCellRenderer center = createCenterRenderer();
        DefaultTableCellRenderer right = createRightRenderer();

        salesTable.getColumnModel().getColumn(0).setCellRenderer(center);
        salesTable.getColumnModel().getColumn(1).setCellRenderer(center);
        salesTable.getColumnModel().getColumn(3).setCellRenderer(right);
        salesTable.getColumnModel().getColumn(4).setCellRenderer(right);
        salesTable.getColumnModel().getColumn(5).setCellRenderer(right);
        salesTable.getColumnModel().getColumn(6).setCellRenderer(center);

        return createTableScrollPane(salesTable);
    }

    private JTable styleTable(JTable t) {
        t.setRowHeight(38);
        t.setFont(AppTheme.font(Font.PLAIN, 12));
        t.setShowGrid(false);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(243, 246, 244)); // Hairline divider only
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setBackground(Color.WHITE);
        t.setSelectionBackground(new Color(0, 90, 61, 14));
        t.setSelectionForeground(AppTheme.TEXT_PRIMARY);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setResizingAllowed(true);
        t.getTableHeader().setPreferredSize(new Dimension(0, 36));

        // Flat clean header renderer
        t.getTableHeader().setDefaultRenderer(new TableCellRenderer() {
            private final JLabel label = new JLabel();
            {
                label.setFont(AppTheme.font(Font.BOLD, 11));
                label.setForeground(AppTheme.TEXT_MUTED);
                label.setBackground(new Color(248, 251, 249));
                label.setOpaque(true);
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 238, 234)),
                        new EmptyBorder(0, 10, 0, 10)
                ));
            }
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFoc, int r, int c) {
                label.setText(val != null ? val.toString().toUpperCase() : "");
                int align = SwingConstants.LEFT;
                if (c == 0 || c == 1 || c == 2 && t == cashTable) align = SwingConstants.CENTER;
                String headerStr = val != null ? val.toString().toLowerCase() : "";
                if (headerStr.contains("revenue") || headerStr.contains("cost") || headerStr.contains("profit")
                        || headerStr.contains("subtotal") || headerStr.contains("gst") || headerStr.contains("total")
                        || headerStr.contains("taxable") || headerStr.contains("cgst") || headerStr.contains("sgst")) {
                    align = SwingConstants.RIGHT;
                }
                label.setHorizontalAlignment(align);
                return label;
            }
        });
        return t;
    }

    private JScrollPane createTableScrollPane(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private DefaultTableCellRenderer createCenterRenderer() {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.CENTER);
        return r;
    }

    private DefaultTableCellRenderer createRightRenderer() {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.RIGHT);
        r.setBorder(new EmptyBorder(0, 8, 0, 10));
        return r;
    }

    // =========================================================================
    // 5. DATA LOADING & FILTERING
    // =========================================================================
    public void loadReports() {
        try {
            // 1. Financial Overview KPIs
            Map<String, Object> fin = reportDAO.getFinancialOverview(filterStartDate, filterEndDate);
            double rev = (Double) fin.getOrDefault("totalRevenue", 0.0);
            double cogs = (Double) fin.getOrDefault("costOfGoods", 0.0);
            double profit = (Double) fin.getOrDefault("grossProfit", 0.0);
            double margin = (Double) fin.getOrDefault("profitMargin", 0.0);
            double gst = (Double) fin.getOrDefault("totalGst", 0.0);

            lblTotalRevenue.setText(String.format("₹%.2f", rev));
            lblCostOfGoods.setText(String.format("₹%.2f", cogs));
            lblGrossProfit.setText(String.format("₹%.2f", profit));
            lblProfitMargin.setText(String.format("%.1f%%", margin));
            lblTotalGst.setText(String.format("₹%.2f", gst));

            // 2. Load Raw Datasets
            profitData.clear();
            profitData.addAll(reportDAO.getProfitLossReport(filterStartDate, filterEndDate));

            taxData.clear();
            taxData.addAll(reportDAO.getTaxReport(filterStartDate, filterEndDate));

            cashData.clear();
            cashData.addAll(reportDAO.getCashReconciliation(filterStartDate, filterEndDate));

            salesData.clear();
            salesData.addAll(reportDAO.getSalesReport(filterStartDate, filterEndDate));

            // 3. Render Active Table
            filterActiveTable();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Failed to load financial reports:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterActiveTable() {
        String query = txtTableSearch != null ? txtTableSearch.getText().trim().toLowerCase() : "";

        switch (activeTab) {
            case "PROFIT_LOSS": {
                profitModel.setRowCount(0);
                double totalSales = 0, totalProfit = 0;
                int rows = 0;
                for (String[] r : profitData) {
                    if (query.isEmpty() || r[0].toLowerCase().contains(query) || r[1].toLowerCase().contains(query)) {
                        profitModel.addRow(r);
                        rows++;
                        try {
                            totalSales += Double.parseDouble(r[3].replaceAll("[^0-9.]", ""));
                            totalProfit += Double.parseDouble(r[5].replaceAll("[^0-9.]", ""));
                        } catch (Exception ignored) {}
                    }
                }
                lblRecordCount.setText(rows + " products listed");
                double avgMargin = totalSales > 0 ? (totalProfit / totalSales) * 100.0 : 0.0;
                lblTableSummary.setText(String.format("Total Sales: ₹%.2f  |  Net Profit: ₹%.2f  |  Average Margin: %.1f%%",
                        totalSales, totalProfit, avgMargin));
                break;
            }
            case "TAX_AUDIT": {
                taxModel.setRowCount(0);
                double totalTaxable = 0, totalGst = 0, totalGross = 0;
                int rows = 0;
                for (String[] r : taxData) {
                    if (query.isEmpty() || r[0].toLowerCase().contains(query) || r[2].toLowerCase().contains(query)) {
                        taxModel.addRow(r);
                        rows++;
                        try {
                            totalTaxable += Double.parseDouble(r[3].replaceAll("[^0-9.]", ""));
                            totalGst += Double.parseDouble(r[7].replaceAll("[^0-9.]", ""));
                            totalGross += Double.parseDouble(r[8].replaceAll("[^0-9.]", ""));
                        } catch (Exception ignored) {}
                    }
                }
                lblRecordCount.setText(rows + " tax invoices");
                lblTableSummary.setText(String.format("Taxable Value: ₹%.2f  |  Total GST: ₹%.2f  |  Gross Sales: ₹%.2f",
                        totalTaxable, totalGst, totalGross));
                break;
            }
            case "CASH_DRAWER": {
                cashModel.setRowCount(0);
                double grandTotal = 0;
                int totalInvoices = 0;
                int rows = 0;
                for (String[] r : cashData) {
                    if (query.isEmpty() || r[0].toLowerCase().contains(query)) {
                        cashModel.addRow(r);
                        rows++;
                        try {
                            totalInvoices += Integer.parseInt(r[1].trim());
                            grandTotal += Double.parseDouble(r[2].replaceAll("[^0-9.]", ""));
                        } catch (Exception ignored) {}
                    }
                }
                lblRecordCount.setText(rows + " payment channels");
                lblTableSummary.setText(String.format("Total Invoices: %d  |  Total Drawer Collections: ₹%.2f",
                        totalInvoices, grandTotal));
                break;
            }
            case "SALES_REGISTER": {
                salesModel.setRowCount(0);
                double totalAmt = 0;
                int rows = 0;
                for (String[] r : salesData) {
                    if (query.isEmpty() || r[0].toLowerCase().contains(query) || r[2].toLowerCase().contains(query) || r[6].toLowerCase().contains(query)) {
                        salesModel.addRow(r);
                        rows++;
                        try {
                            totalAmt += Double.parseDouble(r[5].replaceAll("[^0-9.]", ""));
                        } catch (Exception ignored) {}
                    }
                }
                lblRecordCount.setText(rows + " invoices");
                lblTableSummary.setText(String.format("Total Billed Turnover: ₹%.2f", totalAmt));
                break;
            }
        }
    }

    // =========================================================================
    // 6. 1-CLICK CSV EXPORT & PRINT
    // =========================================================================
    private void exportActiveReportToCSV() {
        JTable currentTable = getActiveTable();
        DefaultTableModel currentModel = (DefaultTableModel) currentTable.getModel();

        if (currentModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "No data available in the current view to export.",
                    "Export Empty", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
        String timeStamp = sdf.format(new Date());
        String defaultName = "SmartBilling_" + activeTab + "_" + timeStamp + ".csv";

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Report to CSV / Excel");
        fileChooser.setSelectedFile(new File(defaultName));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File targetFile = fileChooser.getSelectedFile();
            if (!targetFile.getName().toLowerCase().endsWith(".csv")) {
                targetFile = new File(targetFile.getAbsolutePath() + ".csv");
            }

            try (PrintWriter pw = new PrintWriter(new FileWriter(targetFile))) {
                // Header row
                int colCount = currentModel.getColumnCount();
                for (int c = 0; c < colCount; c++) {
                    pw.print(escapeCSV(currentModel.getColumnName(c)));
                    if (c < colCount - 1) pw.print(",");
                }
                pw.println();

                // Data rows
                int rowCount = currentModel.getRowCount();
                for (int r = 0; r < rowCount; r++) {
                    for (int c = 0; c < colCount; c++) {
                        Object val = currentModel.getValueAt(r, c);
                        pw.print(escapeCSV(val != null ? val.toString() : ""));
                        if (c < colCount - 1) pw.print(",");
                    }
                    pw.println();
                }

                JOptionPane.showMessageDialog(this,
                        "Report exported successfully!\nSaved to: " + targetFile.getAbsolutePath(),
                        "Export Successful", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Failed to export CSV file:\n" + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void printActiveReport() {
        JTable currentTable = getActiveTable();
        if (currentTable.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data available to print.", "Print Empty", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            String title = "SmartBilling Pro - " + activeTab.replace("_", " ") + " (" + filterStartDate + " to " + filterEndDate + ")";
            currentTable.print(JTable.PrintMode.FIT_WIDTH,
                    new java.text.MessageFormat(title),
                    new java.text.MessageFormat("Page {0}"));
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Print failed: " + ex.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JTable getActiveTable() {
        switch (activeTab) {
            case "TAX_AUDIT": return taxTable;
            case "CASH_DRAWER": return cashTable;
            case "SALES_REGISTER": return salesTable;
            default: return profitTable;
        }
    }

    // =========================================================================
    // 7. CUSTOM CELL RENDERERS (Profit Badges, Reconciled Status)
    // =========================================================================
    private static class ProfitBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            setHorizontalAlignment(SwingConstants.RIGHT);
            setBorder(new EmptyBorder(0, 8, 0, 10));
            setFont(AppTheme.font(Font.BOLD, 12));

            String str = value != null ? value.toString() : "0";
            if (str.startsWith("-") || str.contains("−")) {
                setForeground(new Color(220, 38, 38)); // Red for negative profit
            } else {
                setForeground(AppTheme.FOREST_DEEP); // Crisp green for profit
            }
            return c;
        }
    }

    private static class StatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(AppTheme.font(Font.BOLD, 11));
            setForeground(AppTheme.STATUS_SUCCESS);
            return c;
        }
    }

    // =========================================================================
    // 8. CUSTOM UI COMPONENTS (Pill Buttons & Rounded Modern Inputs)
    // =========================================================================
    private static class PillButton extends JButton {
        private Color bg;
        private Color hoverBg;
        private Color fg;
        private Color hoverFg;
        private boolean hasBorder;
        private Color borderColor;
        private boolean hovered = false;
        private boolean active = false;

        public PillButton(String text, Icon icon, Color bg, Color hoverBg, Color fg, Color hoverFg, boolean hasBorder, Color borderColor) {
            super(text, icon);
            this.bg = bg;
            this.hoverBg = hoverBg;
            this.fg = fg;
            this.hoverFg = hoverFg;
            this.hasBorder = hasBorder;
            this.borderColor = borderColor;

            setFont(AppTheme.font(Font.BOLD, 12));
            setForeground(fg);
            setIconTextGap(6);
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

        public void setActive(boolean act) {
            this.active = act;
            if (active) {
                this.bg = AppTheme.FOREST_GREEN;
                this.hoverBg = AppTheme.FOREST_MID;
                this.fg = Color.WHITE;
                this.hoverFg = Color.WHITE;
                this.hasBorder = false;
            } else {
                this.bg = new Color(244, 248, 246);
                this.hoverBg = new Color(230, 240, 234);
                this.fg = AppTheme.TEXT_SECONDARY;
                this.hoverFg = AppTheme.TEXT_PRIMARY;
                this.hasBorder = true;
                this.borderColor = AppTheme.BORDER_SAGE;
            }
            repaint();
        }

        public void setActiveTab(boolean act) {
            this.active = act;
            if (active) {
                this.bg = AppTheme.FOREST_GREEN;
                this.hoverBg = AppTheme.FOREST_MID;
                this.fg = Color.WHITE;
                this.hoverFg = Color.WHITE;
                this.hasBorder = false;
            } else {
                this.bg = new Color(246, 250, 248);
                this.hoverBg = new Color(236, 246, 240);
                this.fg = AppTheme.TEXT_SECONDARY;
                this.hoverFg = AppTheme.FOREST_DEEP;
                this.hasBorder = true;
                this.borderColor = AppTheme.BORDER_SAGE;
            }
            repaint();
        }

        public static PillButton primary(String text, Icon icon) {
            return new PillButton(text, icon, AppTheme.FOREST_GREEN, AppTheme.FOREST_MID, Color.WHITE, Color.WHITE, false, null);
        }

        public static PillButton secondary(String text, Icon icon) {
            return new PillButton(text, icon, new Color(236, 248, 240), new Color(220, 244, 228), AppTheme.FOREST_GREEN, AppTheme.FOREST_MID, true, AppTheme.BORDER_SAGE);
        }

        public static PillButton subtle(String text, Icon icon) {
            return new PillButton(text, icon, new Color(244, 248, 246), new Color(230, 240, 234), AppTheme.TEXT_PRIMARY, AppTheme.FOREST_DEEP, true, AppTheme.BORDER_SAGE);
        }

        public static PillButton preset(String text, boolean active) {
            PillButton btn = new PillButton(text, null,
                    active ? AppTheme.FOREST_GREEN : new Color(244, 248, 246),
                    active ? AppTheme.FOREST_MID : new Color(230, 240, 234),
                    active ? Color.WHITE : AppTheme.TEXT_SECONDARY,
                    active ? Color.WHITE : AppTheme.TEXT_PRIMARY,
                    !active, AppTheme.BORDER_SAGE);
            btn.setFont(AppTheme.font(Font.BOLD, 11));
            btn.active = active;
            return btn;
        }

        public static PillButton tab(String text, Icon icon, boolean active) {
            PillButton btn = new PillButton(text, icon,
                    active ? AppTheme.FOREST_GREEN : new Color(246, 250, 248),
                    active ? AppTheme.FOREST_MID : new Color(236, 246, 240),
                    active ? Color.WHITE : AppTheme.TEXT_SECONDARY,
                    active ? Color.WHITE : AppTheme.FOREST_DEEP,
                    !active, AppTheme.BORDER_SAGE);
            btn.setFont(AppTheme.font(Font.BOLD, 11));
            btn.active = active;
            return btn;
        }

        @Override
        public Dimension getPreferredSize() {
            if (isPreferredSizeSet()) {
                Dimension p = super.getPreferredSize();
                if (p.width > 0) return p;
            }
            Font font = getFont();
            FontMetrics fm = getFontMetrics(font != null ? font : AppTheme.font(Font.BOLD, 11));
            int textW = (getText() != null && !getText().isEmpty()) ? fm.stringWidth(getText()) : 0;
            int iconW = (getIcon() != null) ? getIcon().getIconWidth() : 0;
            int gap = (getIcon() != null && getText() != null && !getText().isEmpty()) ? getIconTextGap() : 0;
            int w = Math.max(64, textW + iconW + gap + 26);
            return new Dimension(w, 32);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int arc = h; // Perfect pill stadium shape

            Color currentBg = hovered ? hoverBg : bg;
            Color currentFg = hovered ? hoverFg : fg;

            g2.setColor(currentBg);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            if (hasBorder) {
                g2.setColor(hovered ? AppTheme.FOREST_MID : borderColor);
                g2.setStroke(new BasicStroke(1.1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            }

            Icon icon = getIcon();
            String text = getText();
            Font font = getFont();
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics(font);

            int textW = (text != null && !text.isEmpty()) ? fm.stringWidth(text) : 0;
            int iconW = (icon != null) ? icon.getIconWidth() : 0;
            int iconH = (icon != null) ? icon.getIconHeight() : 0;
            int gap = (icon != null && text != null && !text.isEmpty()) ? getIconTextGap() : 0;
            int totalW = iconW + gap + textW;
            int startX = (w - totalW) / 2;

            if (icon != null) {
                int iconY = (h - iconH) / 2;
                if (icon instanceof SidebarIcon) {
                    ((SidebarIcon) icon).setOverrideColor(currentFg);
                }
                icon.paintIcon(this, g2, startX, iconY);
            }

            if (text != null && !text.isEmpty()) {
                int textX = startX + iconW + gap;
                int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(currentFg);
                g2.drawString(text, textX, textY);
            }

            g2.dispose();
        }
    }

    private static class ModernInputField extends JTextField {
        private boolean focused = false;

        public ModernInputField(String text) {
            super(text);
            setFont(AppTheme.font(Font.PLAIN, 12));
            setForeground(AppTheme.TEXT_PRIMARY);
            setCaretColor(AppTheme.FOREST_GREEN);
            setOpaque(false);
            setBorder(new EmptyBorder(0, 10, 0, 10));

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
            int r = 10;

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
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
