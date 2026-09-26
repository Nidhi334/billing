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
    private JLabel lblLowStock, lblTodaySales, lblMonthlySales, lblTotalRevenue;

    private ReportDAO reportDAO = new ReportDAO();

    public DashboardFrame(User user) {
        this.currentUser = user;
        setTitle("SmartBilling Pro - " + (user.isAdmin() ? "Admin Portal" : "Staff Desk") + " [" + user.getFullName() + "]");
        setSize(1200, 780);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initComponents();
        loadDashboardStats();
        updateUIVisibilityFromSettings();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout());

        // 1. Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(15, 23, 42));
        topBar.setPreferredSize(new Dimension(0, 56));
        topBar.setBorder(new EmptyBorder(10, 20, 10, 20));

        JLabel brandLabel = new JLabel("⚡ SmartBilling & Inventory Management");
        lblModeBadge = new JLabel(" [TOUCH MODE] ");
        lblModeBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblModeBadge.setOpaque(true);
        lblModeBadge.setBackground(new Color(16, 185, 129));
        lblModeBadge.setForeground(Color.WHITE);
        lblModeBadge.setBorder(new EmptyBorder(3, 8, 3, 8));

        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandBox.setOpaque(false);
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
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(30, 41, 59));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(new EmptyBorder(15, 10, 15, 10));

        btnNavHome = createSidebarButton("🏠  Dashboard");
        btnNavBilling = createSidebarButton("🧾  New Sale / Billing");
        btnNavProducts = createSidebarButton("📦  Products");
        btnNavInventory = createSidebarButton("📊  Inventory / Stock");
        btnNavCustomers = createSidebarButton("👨‍💼  Customers");
        btnNavSuppliers = createSidebarButton("🛒  Suppliers");
        btnNavReports = createSidebarButton("📈  Reports & P/L");
        btnNavSettings = createSidebarButton("⚙️  Settings");

        sidebar.add(btnNavHome);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavBilling);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavProducts);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavInventory);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavCustomers);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavSuppliers);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavReports);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(btnNavSettings);

        // Role based restriction: If staff, disable reports & settings
        if (!currentUser.isAdmin()) {
            btnNavReports.setEnabled(false);
            btnNavReports.setToolTipText("Admin only");
            btnNavSettings.setEnabled(false);
            btnNavSettings.setToolTipText("Admin only");
        }

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

        if (btnNavBilling != null) btnNavBilling.setVisible(showPos);
        if (btnNavProducts != null) btnNavProducts.setVisible(showPrd);
        if (btnNavInventory != null) btnNavInventory.setVisible(showInv);
        if (btnNavCustomers != null) btnNavCustomers.setVisible(showCust);
        if (btnNavSuppliers != null) btnNavSuppliers.setVisible(showSupp);
        if (btnNavReports != null) btnNavReports.setVisible(showRep && currentUser.isAdmin());

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

    private void initHomeStatsPanel() {
        homeStatsPanel = new JPanel(new BorderLayout(15, 15));
        homeStatsPanel.setBackground(new Color(248, 250, 252));
        homeStatsPanel.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Welcome banner
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(Color.WHITE);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(18, 20, 18, 20)
        ));
        JLabel welcomeMsg = new JLabel("Welcome back, " + currentUser.getFullName() + "! Here is your shop overview:");
        welcomeMsg.setFont(new Font("Segoe UI", Font.BOLD, 18));
        welcomeMsg.setForeground(new Color(30, 41, 59));
        banner.add(welcomeMsg, BorderLayout.WEST);

        JButton btnRefreshStats = new JButton("🔄 Refresh Metrics");
        btnRefreshStats.addActionListener(e -> loadDashboardStats());
        banner.add(btnRefreshStats, BorderLayout.EAST);
        homeStatsPanel.add(banner, BorderLayout.NORTH);

        // Stats Grid: 2 rows x 4 columns
        JPanel statsGrid = new JPanel(new GridLayout(2, 4, 18, 18));
        statsGrid.setBackground(new Color(248, 250, 252));

        lblTodaySales = new JLabel("₹0.00");
        lblMonthlySales = new JLabel("₹0.00");
        lblTotalRevenue = new JLabel("₹0.00");
        lblLowStock = new JLabel("0");
        lblTotalProducts = new JLabel("0");
        lblTotalStock = new JLabel("0");
        lblTotalCustomers = new JLabel("0");
        lblTotalSuppliers = new JLabel("0");

        statsGrid.add(createMetricCard("Today's Sales", lblTodaySales, new Color(37, 99, 235), "🛒"));
        statsGrid.add(createMetricCard("This Month's Sales", lblMonthlySales, new Color(13, 148, 136), "📅"));
        statsGrid.add(createMetricCard("Total Revenue", lblTotalRevenue, new Color(16, 185, 129), "💰"));
        statsGrid.add(createMetricCard("Low Stock Alerts", lblLowStock, new Color(220, 38, 38), "⚠️"));

        statsGrid.add(createMetricCard("Unique Products", lblTotalProducts, new Color(100, 116, 139), "📦"));
        statsGrid.add(createMetricCard("Total Stock Units", lblTotalStock, new Color(124, 58, 237), "🔢"));
        statsGrid.add(createMetricCard("Total Customers", lblTotalCustomers, new Color(217, 119, 6), "👨‍💼"));
        statsGrid.add(createMetricCard("Total Suppliers", lblTotalSuppliers, new Color(71, 85, 105), "🏢"));

        homeStatsPanel.add(statsGrid, BorderLayout.CENTER);
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accentColor, String icon) {
        JPanel card = new JPanel(new BorderLayout(10, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JLabel lblTitle = new JLabel(icon + " " + title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTitle.setForeground(new Color(100, 116, 139));
        card.add(lblTitle, BorderLayout.NORTH);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
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
            lblMonthlySales.setText(String.format("₹%.2f", monthSales));
            lblTotalRevenue.setText(String.format("₹%.2f", totalRev));
            lblLowStock.setText(String.valueOf(lowStock));
            lblTotalProducts.setText(String.valueOf(totalPrd));
            lblTotalStock.setText(String.valueOf(totalUnits));
            lblTotalCustomers.setText(String.valueOf(totalCust));
            lblTotalSuppliers.setText(String.valueOf(totalSupp));
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

