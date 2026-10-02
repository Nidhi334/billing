package ui;

import dao.ReportDAO;
import config.AppSettings;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

public class DashboardFrame extends JFrame {
    private User currentUser;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    // View Panels
    private JPanel homeStatsPanel;
    private ProductPanel productPanel;
    private InventoryPanel inventoryPanel;
    private CustomerPanel customerPanel;
    private SupplierPanel supplierPanel;
    private BillingPanel billingPanel;
    private ReportsPanel reportsPanel;
    private SettingsPanel settingsPanel;

    // Sidebar Navigation Buttons
    private JButton btnNavHome;
    private JButton btnNavBilling;
    private JButton btnNavProducts;
    private JButton btnNavInventory;
    private JButton btnNavCustomers;
    private JButton btnNavSuppliers;
    private JButton btnNavReports;
    private JButton btnNavSettings;

    // Header badge for Touch vs Desktop
    private JLabel lblModeBadge;

    // Stat card labels
    private JLabel lblTotalProducts, lblTotalStock, lblTotalCustomers, lblTotalSuppliers;
    private JLabel lblLowStock, lblTodaySales, lblMonthlySales, lblYearlySales, lblTotalRevenue;

    // Graphical Revenue Analytics
    private DailySalesBarChartPanel revenueBarChart;
    private CategoryPieChartPanel revenuePieChart;
    private HorizontalBarChartPanel topProductsChart;
    private JComboBox<String> cmbTimelineFilter;
    private JComboBox<String> cmbBreakdownFilter;

    // Responsive Dashboard Components & State
    private JPanel sidebar;
    private JButton btnToggleSidebar;
    private JPanel statsGrid;
    private int currentStatsCols = -1;
    private JPanel chartToolbar;
    private JLabel lblChartHeader;
    private JPanel filtersPanel;
    private JPanel chartsWrapper;
    private int currentChartsLayoutMode = -1;

    private ReportDAO reportDAO = new ReportDAO();

    public DashboardFrame(User user) {
        this.currentUser = user;
        setTitle("SmartBilling Pro - " + (user.isAdmin() ? "Admin Portal" : "Staff Desk") + " [" + user.getFullName() + "]");
        setSize(1280, 800);
        setMinimumSize(new Dimension(850, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        initComponents();
        if (currentUser.isAdmin()) {
            loadDashboardStats();
        }
        updateUIVisibilityFromSettings();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout());

        // 1. Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(15, 23, 42));
        topBar.setPreferredSize(new Dimension(0, 56));
        topBar.setBorder(new EmptyBorder(10, 16, 10, 16));

        btnToggleSidebar = new JButton("☰");
        btnToggleSidebar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnToggleSidebar.setForeground(Color.WHITE);
        btnToggleSidebar.setBackground(new Color(30, 41, 59));
        btnToggleSidebar.setFocusPainted(false);
        btnToggleSidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 85, 105), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        btnToggleSidebar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnToggleSidebar.setToolTipText("Toggle Sidebar (Show / Hide)");
        btnToggleSidebar.addActionListener(e -> {
            sidebar.setVisible(!sidebar.isVisible());
            root.revalidate();
            root.repaint();
        });

        JLabel brandLabel = new JLabel("⚡ SmartBilling & Inventory Management");
        lblModeBadge = new JLabel(" [TOUCH MODE] ");
        lblModeBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblModeBadge.setOpaque(true);
        lblModeBadge.setBackground(new Color(16, 185, 129));
        lblModeBadge.setForeground(Color.WHITE);
        lblModeBadge.setBorder(new EmptyBorder(3, 8, 3, 8));

        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandBox.setOpaque(false);
        brandBox.add(btnToggleSidebar);
        brandBox.add(brandLabel);
        brandBox.add(lblModeBadge);
        brandLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        brandLabel.setForeground(Color.WHITE);
        topBar.add(brandBox, BorderLayout.WEST);

        JPanel userSection = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 2));
        userSection.setOpaque(false);

        JLabel userLabel = new JLabel("👤 " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userLabel.setForeground(new Color(226, 232, 240));

        JButton btnLogout = new JButton("Logout");
        btnLogout.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnLogout.setBackground(new Color(239, 68, 68));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> logout());

        userSection.add(userLabel);
        userSection.add(btnLogout);
        topBar.add(userSection, BorderLayout.EAST);

        root.add(topBar, BorderLayout.NORTH);

        // 2. Sidebar Navigation
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(30, 41, 59));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(new EmptyBorder(15, 10, 15, 10));

        btnNavHome = createSidebarButton("🏠  Dashboard");
        btnNavBilling = createSidebarButton("🧾  New Sale / Billing");
        btnNavCustomers = createSidebarButton("👨‍💼  Customers");
        btnNavProducts = createSidebarButton("📦  Products");
        btnNavInventory = createSidebarButton("📊  Inventory / Stock");
        btnNavSuppliers = createSidebarButton("🛒  Suppliers");
        btnNavReports = createSidebarButton("📈  Reports & P/L");
        btnNavSettings = createSidebarButton("⚙️  Settings");

        sidebar.add(btnNavHome);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavBilling);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavCustomers);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavProducts);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavInventory);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavSuppliers);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavReports);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavSettings);

        root.add(sidebar, BorderLayout.WEST);

        // 3. Central Content Cards
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        // Views
        initHomeStatsPanel();
        productPanel = new ProductPanel();
        inventoryPanel = new InventoryPanel();
        customerPanel = new CustomerPanel();
        supplierPanel = new SupplierPanel();
        billingPanel = new BillingPanel(currentUser);
        reportsPanel = new ReportsPanel();
        settingsPanel = new SettingsPanel(this::updateUIVisibilityFromSettings);

        contentPanel.add(homeStatsPanel, "HOME");
        contentPanel.add(billingPanel, "BILLING");
        contentPanel.add(productPanel, "PRODUCTS");
        contentPanel.add(inventoryPanel, "INVENTORY");
        contentPanel.add(customerPanel, "CUSTOMERS");
        contentPanel.add(supplierPanel, "SUPPLIERS");
        contentPanel.add(reportsPanel, "REPORTS");
        contentPanel.add(settingsPanel, "SETTINGS");

        root.add(contentPanel, BorderLayout.CENTER);

        // Navigation button actions
        btnNavHome.addActionListener(e -> {
            loadDashboardStats();
        updateUIVisibilityFromSettings();
            cardLayout.show(contentPanel, "HOME");
        });
        btnNavBilling.addActionListener(e -> {
            billingPanel.resetBillingDesk();
            cardLayout.show(contentPanel, "BILLING");
            billingPanel.focusBarcodeField();
        });
        btnNavProducts.addActionListener(e -> {
            productPanel.loadProductTable();
            productPanel.loadCategories();
            cardLayout.show(contentPanel, "PRODUCTS");
        });
        btnNavInventory.addActionListener(e -> {
            inventoryPanel.loadDropdownData();
            inventoryPanel.refreshData();
            cardLayout.show(contentPanel, "INVENTORY");
        });
        btnNavCustomers.addActionListener(e -> {
            customerPanel.loadCustomerTable();
            cardLayout.show(contentPanel, "CUSTOMERS");
        });
        btnNavSuppliers.addActionListener(e -> {
            supplierPanel.loadSupplierTable();
            cardLayout.show(contentPanel, "SUPPLIERS");
        });
        btnNavReports.addActionListener(e -> {
            reportsPanel.loadReports();
            cardLayout.show(contentPanel, "REPORTS");
        });
        btnNavSettings.addActionListener(e -> {
            settingsPanel.loadCurrentSettings();
            cardLayout.show(contentPanel, "SETTINGS");
        });

        if (!currentUser.isAdmin()) {
            billingPanel.resetBillingDesk();
            cardLayout.show(contentPanel, "BILLING");
            billingPanel.focusBarcodeField();
        }

        add(root);
    }


    public void updateUIVisibilityFromSettings() {
        boolean isTouch = AppSettings.isTouchMode();
        if (lblModeBadge != null) {
            if (isTouch) {
                lblModeBadge.setText(" 📱 TOUCH SCREEN MODE ");
                lblModeBadge.setBackground(new Color(16, 185, 129));
            } else {
                lblModeBadge.setText(" 💻 NON-TOUCH DESKTOP MODE ");
                lblModeBadge.setBackground(new Color(37, 99, 235));
            }
        }

        boolean showPos = AppSettings.getBoolean(AppSettings.KEY_SHOW_POS_NAV, true);
        boolean showPrd = AppSettings.getBoolean(AppSettings.KEY_NAV_PRODUCTS, true);
        boolean showInv = AppSettings.getBoolean(AppSettings.KEY_NAV_INVENTORY, true);
        boolean showCust = AppSettings.getBoolean(AppSettings.KEY_NAV_CUSTOMERS, true);
        boolean showSupp = AppSettings.getBoolean(AppSettings.KEY_NAV_SUPPLIERS, true);
        boolean showRep = AppSettings.getBoolean(AppSettings.KEY_NAV_REPORTS, true);
        boolean isAdmin = currentUser.isAdmin();

        if (btnNavHome != null) btnNavHome.setVisible(isAdmin);
        if (btnNavBilling != null) btnNavBilling.setVisible(showPos || !isAdmin);
        if (btnNavProducts != null) btnNavProducts.setVisible(isAdmin && showPrd);
        if (btnNavInventory != null) btnNavInventory.setVisible(isAdmin && showInv);
        if (btnNavCustomers != null) btnNavCustomers.setVisible(showCust);
        if (btnNavSuppliers != null) btnNavSuppliers.setVisible(isAdmin && showSupp);
        if (btnNavReports != null) btnNavReports.setVisible(showRep && isAdmin);
        if (btnNavSettings != null) btnNavSettings.setVisible(isAdmin);

        if (billingPanel != null) {
            billingPanel.applySettingsVisibility();
        }

        revalidate();
        repaint();
    }

    private JButton createSidebarButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(new Color(241, 245, 249));
        btn.setBackground(new Color(51, 65, 85));
        btn.setMaximumSize(new Dimension(200, 42));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(10, 15, 10, 15));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // Responsive Scrollable Panel ensuring viewport width tracking
    private static class ResponsiveScrollablePanel extends JPanel implements Scrollable {
        public ResponsiveScrollablePanel() {
            super();
        }
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }
        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }
        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 64;
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

    private void initHomeStatsPanel() {
        homeStatsPanel = new JPanel(new BorderLayout(15, 15));
        homeStatsPanel.setBackground(new Color(248, 250, 252));
        homeStatsPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Welcome banner
        JPanel banner = new JPanel(new BorderLayout(10, 10));
        banner.setBackground(Color.WHITE);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(14, 18, 14, 18)
        ));
        JLabel welcomeMsg = new JLabel("Welcome back, " + currentUser.getFullName() + "! Here is your shop overview:");
        welcomeMsg.setFont(new Font("Segoe UI", Font.BOLD, 17));
        welcomeMsg.setForeground(new Color(30, 41, 59));
        banner.add(welcomeMsg, BorderLayout.WEST);

        JButton btnRefreshStats = new JButton("🔄 Refresh Metrics");
        btnRefreshStats.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefreshStats.setFocusPainted(false);
        btnRefreshStats.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefreshStats.addActionListener(e -> loadDashboardStats());
        banner.add(btnRefreshStats, BorderLayout.EAST);
        homeStatsPanel.add(banner, BorderLayout.NORTH);

        // 1. Revenue & Operations Metrics Grid
        statsGrid = new JPanel(new GridLayout(0, 5, 12, 12));
        statsGrid.setBackground(new Color(248, 250, 252));

        lblTodaySales = new JLabel("₹0.00");
        lblMonthlySales = new JLabel("₹0.00");
        lblYearlySales = new JLabel("₹0.00");
        lblTotalRevenue = new JLabel("₹0.00");
        lblLowStock = new JLabel("0");
        lblTotalProducts = new JLabel("0");
        lblTotalStock = new JLabel("0");
        lblTotalCustomers = new JLabel("0");
        lblTotalSuppliers = new JLabel("0");
        JLabel lblProfitMargin = new JLabel("Live POS");

        statsGrid.add(createMetricCard("Today's Revenue", lblTodaySales, new Color(37, 99, 235), "⚡"));
        statsGrid.add(createMetricCard("Monthly Revenue", lblMonthlySales, new Color(13, 148, 136), "📅"));
        statsGrid.add(createMetricCard("Yearly Revenue", lblYearlySales, new Color(124, 58, 237), "📆"));
        statsGrid.add(createMetricCard("Total Revenue", lblTotalRevenue, new Color(16, 185, 129), "💰"));
        statsGrid.add(createMetricCard("Low Stock Alerts", lblLowStock, new Color(220, 38, 38), "⚠️"));

        statsGrid.add(createMetricCard("Unique Products", lblTotalProducts, new Color(100, 116, 139), "📦"));
        statsGrid.add(createMetricCard("Total Stock Units", lblTotalStock, new Color(79, 70, 229), "🔢"));
        statsGrid.add(createMetricCard("Total Customers", lblTotalCustomers, new Color(217, 119, 6), "👨‍💼"));
        statsGrid.add(createMetricCard("Total Suppliers", lblTotalSuppliers, new Color(71, 85, 105), "🏢"));
        statsGrid.add(createMetricCard("POS Billing Status", lblProfitMargin, new Color(5, 150, 105), "🟢"));

        // 2. Chart Controls Toolbar
        chartToolbar = new JPanel(new BorderLayout(10, 0));
        chartToolbar.setOpaque(false);
        chartToolbar.setBorder(new EmptyBorder(10, 0, 6, 0));

        lblChartHeader = new JLabel("📊 Interactive Revenue Visual Analytics & Trends");
        lblChartHeader.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblChartHeader.setForeground(new Color(30, 41, 59));

        filtersPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filtersPanel.setOpaque(false);

        JLabel lblTimeline = new JLabel("Timeline Trend:");
        lblTimeline.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filtersPanel.add(lblTimeline);

        cmbTimelineFilter = new JComboBox<>(new String[]{
                "📅 Daily Revenue (Last 7 Days)",
                "📅 Daily Revenue (Last 14 Days)",
                "📅 Daily Revenue (Last 30 Days)",
                "📆 Monthly Revenue (Last 6 Months)",
                "📆 Monthly Revenue (Last 12 Months)",
                "📊 Yearly Revenue (Last 5 Years)"
        });
        cmbTimelineFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbTimelineFilter.addActionListener(e -> refreshBarChartData());
        filtersPanel.add(cmbTimelineFilter);

        JLabel lblBreakdown = new JLabel("Breakdown:");
        lblBreakdown.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filtersPanel.add(lblBreakdown);

        cmbBreakdownFilter = new JComboBox<>(new String[]{
                "🍰 By Product Category",
                "💳 By Payment Source (Cash/UPI/Card)"
        });
        cmbBreakdownFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbBreakdownFilter.addActionListener(e -> refreshPieChartData());
        filtersPanel.add(cmbBreakdownFilter);

        chartToolbar.add(lblChartHeader, BorderLayout.WEST);
        chartToolbar.add(filtersPanel, BorderLayout.EAST);

        // 3. Visual Charts Container
        revenueBarChart = new DailySalesBarChartPanel();
        revenuePieChart = new CategoryPieChartPanel();
        topProductsChart = new HorizontalBarChartPanel();

        chartsWrapper = new JPanel();
        chartsWrapper.setOpaque(false);

        // 4. Combine into responsive vertically scrollable body
        ResponsiveScrollablePanel centerContainer = new ResponsiveScrollablePanel();
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));
        centerContainer.setOpaque(false);
        centerContainer.add(statsGrid);
        centerContainer.add(Box.createVerticalStrut(12));
        centerContainer.add(chartToolbar);
        centerContainer.add(Box.createVerticalStrut(8));
        centerContainer.add(chartsWrapper);

        JScrollPane scrollPane = new JScrollPane(centerContainer);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Responsive viewport listener
        scrollPane.getViewport().addChangeListener(e -> {
            int w = scrollPane.getViewport().getWidth();
            updateResponsiveHomeLayout(w);
        });

        homeStatsPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = scrollPane.getViewport().getWidth();
                if (w <= 0) w = homeStatsPanel.getWidth() - 32;
                updateResponsiveHomeLayout(w);
            }
        });

        updateResponsiveHomeLayout(1150);

        homeStatsPanel.add(scrollPane, BorderLayout.CENTER);
    }

    private void updateResponsiveHomeLayout(int availableWidth) {
        if (availableWidth < 100) return;

        // 1. Metric Cards Grid: Dynamic column count
        int targetCols;
        if (availableWidth >= 1200) {
            targetCols = 5;
        } else if (availableWidth >= 960) {
            targetCols = 4;
        } else if (availableWidth >= 700) {
            targetCols = 3;
        } else {
            targetCols = 2;
        }

        if (currentStatsCols != targetCols && statsGrid != null) {
            currentStatsCols = targetCols;
            statsGrid.setLayout(new GridLayout(0, targetCols, 12, 12));
            statsGrid.revalidate();
        }

        // 2. Chart Toolbar: Wrap filters if width is constrained
        if (chartToolbar != null && lblChartHeader != null && filtersPanel != null) {
            if (availableWidth >= 1050) {
                chartToolbar.removeAll();
                chartToolbar.setLayout(new BorderLayout(10, 0));
                chartToolbar.add(lblChartHeader, BorderLayout.WEST);
                chartToolbar.add(filtersPanel, BorderLayout.EAST);
            } else {
                chartToolbar.removeAll();
                chartToolbar.setLayout(new BorderLayout(0, 8));
                chartToolbar.add(lblChartHeader, BorderLayout.NORTH);
                chartToolbar.add(filtersPanel, BorderLayout.WEST);
            }
            chartToolbar.revalidate();
        }

        // 3. Visual Charts: Responsive arrangement
        int targetChartsMode;
        if (availableWidth >= 1250) {
            targetChartsMode = 0; // 3 columns side by side
        } else if (availableWidth >= 780) {
            targetChartsMode = 1; // 2 tiers: Row 1 = 2 charts, Row 2 = 1 wide chart
        } else {
            targetChartsMode = 2; // 1 column stacked
        }

        if (currentChartsLayoutMode != targetChartsMode && chartsWrapper != null) {
            currentChartsLayoutMode = targetChartsMode;
            chartsWrapper.removeAll();

            if (targetChartsMode == 0) {
                // 3 columns side-by-side
                chartsWrapper.setLayout(new GridLayout(1, 3, 14, 0));
                chartsWrapper.setPreferredSize(new Dimension(0, 340));
                chartsWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 340));
                revenueBarChart.setPreferredSize(null);
                revenuePieChart.setPreferredSize(null);
                topProductsChart.setPreferredSize(null);
                chartsWrapper.add(revenueBarChart);
                chartsWrapper.add(revenuePieChart);
                chartsWrapper.add(topProductsChart);
            } else if (targetChartsMode == 1) {
                // 2 tiers
                chartsWrapper.setLayout(new BorderLayout(0, 14));
                chartsWrapper.setPreferredSize(new Dimension(0, 580));
                chartsWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 580));

                JPanel topTwo = new JPanel(new GridLayout(1, 2, 14, 0));
                topTwo.setOpaque(false);
                topTwo.setPreferredSize(new Dimension(0, 310));
                topTwo.add(revenueBarChart);
                topTwo.add(revenuePieChart);

                JPanel bottomOne = new JPanel(new BorderLayout());
                bottomOne.setOpaque(false);
                bottomOne.setPreferredSize(new Dimension(0, 250));
                bottomOne.add(topProductsChart, BorderLayout.CENTER);

                chartsWrapper.add(topTwo, BorderLayout.NORTH);
                chartsWrapper.add(bottomOne, BorderLayout.CENTER);
            } else {
                // 1 column vertically stacked
                chartsWrapper.setLayout(new BoxLayout(chartsWrapper, BoxLayout.Y_AXIS));
                chartsWrapper.setPreferredSize(new Dimension(0, 880));
                chartsWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 880));

                revenueBarChart.setPreferredSize(new Dimension(0, 280));
                revenuePieChart.setPreferredSize(new Dimension(0, 280));
                topProductsChart.setPreferredSize(new Dimension(0, 280));

                chartsWrapper.add(revenueBarChart);
                chartsWrapper.add(Box.createVerticalStrut(14));
                chartsWrapper.add(revenuePieChart);
                chartsWrapper.add(Box.createVerticalStrut(14));
                chartsWrapper.add(topProductsChart);
            }

            chartsWrapper.revalidate();
            chartsWrapper.repaint();
        }
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accentColor, String icon) {
        JPanel card = new JPanel(new BorderLayout(8, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel lblTitle = new JLabel(icon + " " + title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(100, 116, 139));
        card.add(lblTitle, BorderLayout.NORTH);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(accentColor);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private void loadDashboardStats() {
        try {
            Map<String, Object> stats = reportDAO.getDashboardStats();
            double todaySales = (double) stats.getOrDefault("todaySalesAmount", 0.0);
            double monthSales = (double) stats.getOrDefault("monthlySalesAmount", 0.0);
            double totalRev = (double) stats.getOrDefault("totalRevenue", 0.0);
            int lowStock = (int) stats.getOrDefault("lowStockCount", 0);
            int totalPrd = (int) stats.getOrDefault("totalProducts", 0);
            int totalUnits = (int) stats.getOrDefault("totalStockUnits", 0);
            int totalCust = (int) stats.getOrDefault("totalCustomers", 0);
            int totalSupp = (int) stats.getOrDefault("totalSuppliers", 0);

            lblTodaySales.setText(String.format("₹%.2f", todaySales));
            double yearSales = (double) stats.getOrDefault("yearlySalesAmount", 0.0);
            lblMonthlySales.setText(String.format("₹%.2f", monthSales));
            if (lblYearlySales != null) lblYearlySales.setText(String.format("₹%.2f", yearSales));
            lblTotalRevenue.setText(String.format("₹%.2f", totalRev));
            lblLowStock.setText(String.valueOf(lowStock));
            lblTotalProducts.setText(String.valueOf(totalPrd));
            lblTotalStock.setText(String.valueOf(totalUnits));
            lblTotalCustomers.setText(String.valueOf(totalCust));
                        lblTotalSuppliers.setText(String.valueOf(totalSupp));

            refreshBarChartData();
            refreshPieChartData();
            refreshTopProductsChartData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void refreshBarChartData() {
        if (revenueBarChart == null || cmbTimelineFilter == null) return;
        try {
            int sel = cmbTimelineFilter.getSelectedIndex();
            if (sel == 0) {
                revenueBarChart.setData(reportDAO.getDailySalesTrend(7), "📈 Daily Revenue Trend (Last 7 Days)");
            } else if (sel == 1) {
                revenueBarChart.setData(reportDAO.getDailySalesTrend(14), "📈 Daily Revenue Trend (Last 14 Days)");
            } else if (sel == 2) {
                revenueBarChart.setData(reportDAO.getDailySalesTrend(30), "📈 Daily Revenue Trend (Last 30 Days)");
            } else if (sel == 3) {
                revenueBarChart.setData(reportDAO.getMonthlyRevenueTrend(6), "📈 Monthly Revenue Trend (Last 6 Months)");
            } else if (sel == 4) {
                revenueBarChart.setData(reportDAO.getMonthlyRevenueTrend(12), "📈 Monthly Revenue Trend (Last 12 Months)");
            } else {
                revenueBarChart.setData(reportDAO.getYearlyRevenueTrend(5), "📈 Yearly Revenue Trend (Last 5 Years)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshPieChartData() {
        if (revenuePieChart == null || cmbBreakdownFilter == null) return;
        try {
            int sel = cmbBreakdownFilter.getSelectedIndex();
            if (sel == 0) {
                revenuePieChart.setData(reportDAO.getCategorySalesBreakdown(), "🍰 Revenue by Category Share");
            } else {
                revenuePieChart.setData(reportDAO.getRevenueByPaymentSource(), "💳 Revenue by Payment Source (Cash / UPI / Card)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshTopProductsChartData() {
        if (topProductsChart == null) return;
        try {
            topProductsChart.setData(reportDAO.getTopSellingProducts(6), "🏆 Top Selling Products");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logout() {
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to log out?", "Logout", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }
}

