package ui;

import dao.ReportDAO;
import config.AppSettings;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardFrame extends JFrame {
    public static final Color APP_BG = AppTheme.BG_CANVAS;

    private User currentUser;
    private JPanel root;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    // View Panels
    private JPanel homeStatsPanel;
    private ProductPanel productPanel;
    private InventoryPanel inventoryPanel;
    private CustomerPanel customerPanel;
    private SupplierPanel supplierPanel;
    private BillingPanel billingPanel;
    private BillHistoryPanel billHistoryPanel;
    private SelfCheckoutPanel selfCheckoutPanel;
    private ReportsPanel reportsPanel;
    private SettingsPanel settingsPanel;

    // Sidebar Navigation Buttons
    private SidebarNavItem btnNavHome;
    private SidebarNavItem btnNavBilling;
    private SidebarNavItem btnNavSelfCheckout;
    private SidebarNavItem btnNavBillHistory;
    private SidebarNavItem btnNavCustomers;
    private SidebarNavItem btnNavProducts;
    private SidebarNavItem btnNavInventory;
    private SidebarNavItem btnNavSuppliers;
    private SidebarNavItem btnNavReports;
    private SidebarNavItem btnNavSettings;
    private final List<SidebarNavItem> navItems = new ArrayList<>();

    // Sidebar Header & Collapse State
    private JPanel sidebarHeader;
    private JLabel lblSidebarHeader;
    private JButton btnSidebarToggle;
    private JPanel navBox;
    private boolean isSidebarCollapsed = false;

    // Header badge for Touch vs Desktop
    private JLabel lblModeBadge;

    // Stat card labels
    private JLabel lblTotalProducts, lblTotalStock, lblTotalCustomers, lblTotalSuppliers;
    private JLabel lblLowStock, lblTodaySales, lblMonthlySales, lblYearlySales, lblTotalRevenue;

    // Graphical Revenue Analytics
    private DailySalesBarChartPanel revenueBarChart;
    private CategoryPieChartPanel revenuePieChart;
    private HorizontalBarChartPanel topProductsChart;
    private ModernDropdown cmbTimelineFilter;
    private ModernDropdown cmbBreakdownFilter;

    // Responsive Dashboard Components & State
    private JPanel sidebar;
    private JPanel sidebarFooter;
    private JButton btnLogout;
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
        util.AppIconUtil.applyToWindow(this);

        // Unified background styling matching login page #e6f0e7
        setBackground(APP_BG);
        getContentPane().setBackground(APP_BG);
        getRootPane().setBackground(APP_BG);

        // macOS Window Header styling: tint title bar to match window background #e6f0e7
        getRootPane().putClientProperty("apple.awt.fullWindowContent", Boolean.TRUE);
        getRootPane().putClientProperty("apple.awt.transparentTitleBar", Boolean.TRUE);
        getRootPane().putClientProperty("apple.awt.windowTitleVisible", Boolean.TRUE);

        initComponents();
        if (currentUser.isAdmin()) {
            loadDashboardStats();
            setActiveNavButton(btnNavHome);
        } else {
            setActiveNavButton(btnNavBilling);
        }
        updateUIVisibilityFromSettings();
    }

    private void initComponents() {
        root = new JPanel(new BorderLayout());
        root.setBackground(APP_BG);
        root.setOpaque(true);

        // 1. Sidebar Navigation (Left Navigation Column with Integrated Brand & User Actions)
        sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(APP_BG);
        sidebar.setPreferredSize(new Dimension(228, 0));
        sidebar.setMinimumSize(new Dimension(228, 0));
        sidebar.setBorder(null);

        // Top Sidebar Header (Brand + Collapse/Expand Toggle)
        sidebarHeader = new JPanel();
        sidebarHeader.setOpaque(false);

        btnSidebarToggle = new JButton(new SidebarIcon("panel_left_close", 18)) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                if (hovered) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(17, 74, 42, 22));
                    g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 10, 10);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        btnSidebarToggle.setToolTipText("Collapse sidebar (icon-only mode)");
        btnSidebarToggle.setFocusPainted(false);
        btnSidebarToggle.setContentAreaFilled(false);
        btnSidebarToggle.setBorderPainted(false);
        btnSidebarToggle.setOpaque(false);
        btnSidebarToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSidebarToggle.setPreferredSize(new Dimension(34, 34));
        btnSidebarToggle.setMaximumSize(new Dimension(34, 34));
        btnSidebarToggle.addActionListener(e -> toggleSidebarCollapse());

        sidebar.add(sidebarHeader, BorderLayout.NORTH);

        // Center Navigation Items List
        navBox = new JPanel();
        navBox.setLayout(new BoxLayout(navBox, BoxLayout.Y_AXIS));
        navBox.setOpaque(false);
        navBox.setBorder(new EmptyBorder(4, 10, 10, 10));

        navItems.clear();
        btnNavHome = new SidebarNavItem("Dashboard", "dashboard");
        btnNavBilling = new SidebarNavItem("New Sale / Billing", "billing");
        btnNavSelfCheckout = new SidebarNavItem("Self Checkout", "self_checkout");
        btnNavBillHistory = new SidebarNavItem("Bill History / Orders", "bill_history");
        btnNavCustomers = new SidebarNavItem("Customers", "customers");
        btnNavProducts = new SidebarNavItem("Products", "products");
        btnNavInventory = new SidebarNavItem("Inventory / Stock", "inventory");
        btnNavSuppliers = new SidebarNavItem("Suppliers", "suppliers");
        btnNavReports = new SidebarNavItem("Reports & P/L", "reports");
        btnNavSettings = new SidebarNavItem("Settings", "settings");

        navItems.add(btnNavHome);
        navItems.add(btnNavBilling);
        navItems.add(btnNavSelfCheckout);
        navItems.add(btnNavBillHistory);
        navItems.add(btnNavCustomers);
        navItems.add(btnNavProducts);
        navItems.add(btnNavInventory);
        navItems.add(btnNavSuppliers);
        navItems.add(btnNavReports);
        navItems.add(btnNavSettings);

        for (SidebarNavItem item : navItems) {
            item.setAlignmentX(Component.CENTER_ALIGNMENT);
            navBox.add(item);
            navBox.add(Box.createVerticalStrut(5));
        }

        JScrollPane navScroll = new JScrollPane(navBox);
        navScroll.setBorder(null);
        navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false);
        navScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

        sidebar.add(navScroll, BorderLayout.CENTER);

        // Bottom Footer (User Profile, Mode Badge, and Logout Button)
        sidebarFooter = new JPanel(new BorderLayout());
        sidebarFooter.setOpaque(false);
        sidebarFooter.setBorder(new EmptyBorder(6, 10, 14, 10));

        lblModeBadge = createModeBadge();

        sidebar.add(sidebarFooter, BorderLayout.SOUTH);

        // Build header & footer elements for initial expanded mode
        updateSidebarHeaderAndFooter(false);

        root.add(sidebar, BorderLayout.WEST);

        // 3. Central Content Cards with top margin so content clears macOS title bar
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(APP_BG);
        contentPanel.setOpaque(true);
        contentPanel.setBorder(new EmptyBorder(38, 0, 0, 0));

        // Views
        initHomeStatsPanel();
        productPanel = new ProductPanel();
        inventoryPanel = new InventoryPanel();
        customerPanel = new CustomerPanel();
        supplierPanel = new SupplierPanel();
        billingPanel = new BillingPanel(currentUser);
        billHistoryPanel = new BillHistoryPanel(currentUser);
        selfCheckoutPanel = new SelfCheckoutPanel(currentUser, () -> {
            showAdminChrome(true);
            setActiveNavButton(btnNavHome);
            cardLayout.show(contentPanel, "HOME");
        });
        reportsPanel = new ReportsPanel();
        settingsPanel = new SettingsPanel(this::updateUIVisibilityFromSettings);

        contentPanel.add(homeStatsPanel, "HOME");
        contentPanel.add(billingPanel, "BILLING");
        contentPanel.add(selfCheckoutPanel, "SELF_CHECKOUT");
        contentPanel.add(billHistoryPanel, "BILL_HISTORY");
        contentPanel.add(productPanel, "PRODUCTS");
        contentPanel.add(inventoryPanel, "INVENTORY");
        contentPanel.add(customerPanel, "CUSTOMERS");
        contentPanel.add(supplierPanel, "SUPPLIERS");
        contentPanel.add(reportsPanel, "REPORTS");
        contentPanel.add(settingsPanel, "SETTINGS");

        root.add(contentPanel, BorderLayout.CENTER);

        // Navigation button actions
        btnNavHome.addActionListener(e -> {
            setActiveNavButton(btnNavHome);
            showAdminChrome(true);
            loadDashboardStats();
            updateUIVisibilityFromSettings();
            cardLayout.show(contentPanel, "HOME");
        });
        btnNavBilling.addActionListener(e -> {
            setActiveNavButton(btnNavBilling);
            showAdminChrome(true);
            billingPanel.resetBillingDesk();
            cardLayout.show(contentPanel, "BILLING");
            billingPanel.focusBarcodeField();
        });
        btnNavSelfCheckout.addActionListener(e -> {
            setActiveNavButton(btnNavSelfCheckout);
            showAdminChrome(false);
            selfCheckoutPanel.loadCatalog();
            selfCheckoutPanel.focusScanInput();
            cardLayout.show(contentPanel, "SELF_CHECKOUT");
        });
        btnNavBillHistory.addActionListener(e -> {
            setActiveNavButton(btnNavBillHistory);
            showAdminChrome(true);
            billHistoryPanel.loadBillHistory();
            cardLayout.show(contentPanel, "BILL_HISTORY");
        });
        btnNavProducts.addActionListener(e -> {
            setActiveNavButton(btnNavProducts);
            showAdminChrome(true);
            productPanel.loadProductTable();
            productPanel.loadCategories();
            cardLayout.show(contentPanel, "PRODUCTS");
        });
        btnNavInventory.addActionListener(e -> {
            setActiveNavButton(btnNavInventory);
            showAdminChrome(true);
            inventoryPanel.loadDropdownData();
            inventoryPanel.refreshData();
            cardLayout.show(contentPanel, "INVENTORY");
        });
        btnNavCustomers.addActionListener(e -> {
            setActiveNavButton(btnNavCustomers);
            showAdminChrome(true);
            customerPanel.loadCustomerTable();
            cardLayout.show(contentPanel, "CUSTOMERS");
        });
        btnNavSuppliers.addActionListener(e -> {
            setActiveNavButton(btnNavSuppliers);
            showAdminChrome(true);
            supplierPanel.loadSupplierTable();
            cardLayout.show(contentPanel, "SUPPLIERS");
        });
        btnNavReports.addActionListener(e -> {
            setActiveNavButton(btnNavReports);
            showAdminChrome(true);
            reportsPanel.loadReports();
            cardLayout.show(contentPanel, "REPORTS");
        });
        btnNavSettings.addActionListener(e -> {
            setActiveNavButton(btnNavSettings);
            showAdminChrome(true);
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

    public void showAdminChrome(boolean visible) {
        if (sidebar != null) sidebar.setVisible(visible);
        revalidate();
        repaint();
    }

    public void showBillHistory() {
        if (billHistoryPanel != null) {
            showAdminChrome(true);
            setActiveNavButton(btnNavBillHistory);
            billHistoryPanel.loadBillHistory();
            cardLayout.show(contentPanel, "BILL_HISTORY");
        }
    }

    public void setActiveNavButton(SidebarNavItem activeBtn) {
        for (SidebarNavItem item : navItems) {
            item.setActive(item == activeBtn);
        }
    }

    public void toggleSidebarCollapse() {
        setSidebarCollapsed(!isSidebarCollapsed);
    }

    public void setSidebarCollapsed(boolean collapsed) {
        this.isSidebarCollapsed = collapsed;
        int targetWidth = collapsed ? 68 : 228;
        sidebar.setPreferredSize(new Dimension(targetWidth, 0));
        sidebar.setMinimumSize(new Dimension(targetWidth, 0));

        btnSidebarToggle.setIcon(new SidebarIcon(collapsed ? "panel_left_open" : "panel_left_close", 18));
        btnSidebarToggle.setToolTipText(collapsed ? "Expand sidebar" : "Collapse sidebar");

        updateSidebarHeaderAndFooter(collapsed);

        for (SidebarNavItem item : navItems) {
            item.setCollapsed(collapsed);
        }

        sidebar.revalidate();
        sidebar.repaint();
        if (root != null) {
            root.revalidate();
            root.repaint();
        }
    }

    private void updateSidebarHeaderAndFooter(boolean collapsed) {
        // 1. Header (Brand mark & Toggle)
        sidebarHeader.removeAll();
        if (collapsed) {
            sidebarHeader.setLayout(new BoxLayout(sidebarHeader, BoxLayout.Y_AXIS));
            sidebarHeader.setBorder(new EmptyBorder(40, 0, 10, 0));

            JLabel icon = new JLabel(util.AppIconUtil.getSquircleImageIcon(28));
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            icon.setToolTipText("SmartBilling Pro");
            btnSidebarToggle.setAlignmentX(Component.CENTER_ALIGNMENT);

            sidebarHeader.add(icon);
            sidebarHeader.add(Box.createVerticalStrut(10));
            sidebarHeader.add(btnSidebarToggle);
            navBox.setBorder(new EmptyBorder(4, 12, 10, 12));
        } else {
            sidebarHeader.setLayout(new BorderLayout());
            sidebarHeader.setBorder(new EmptyBorder(40, 10, 10, 8));

            JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            brand.setOpaque(false);
            JLabel icon = new JLabel(util.AppIconUtil.getSquircleImageIcon(26));
            JLabel title = new JLabel("SmartBilling Pro");
            title.setFont(AppTheme.font(Font.BOLD, 14));
            title.setForeground(AppTheme.TEXT_PRIMARY);
            brand.add(icon);
            brand.add(title);

            sidebarHeader.add(brand, BorderLayout.WEST);
            sidebarHeader.add(btnSidebarToggle, BorderLayout.EAST);
            navBox.setBorder(new EmptyBorder(4, 10, 10, 10));
        }

        // 2. Footer (User Profile & Logout)
        sidebarFooter.removeAll();
        if (collapsed) {
            sidebarFooter.setLayout(new BoxLayout(sidebarFooter, BoxLayout.Y_AXIS));
            sidebarFooter.setBorder(new EmptyBorder(8, 8, 14, 8));

            JComponent avatar = createUserAvatar(34);
            avatar.setAlignmentX(Component.CENTER_ALIGNMENT);
            avatar.setToolTipText(currentUser.getFullName() + " (" + currentUser.getRole() + ")");

            JButton logout = createLogoutButton(true);
            logout.setAlignmentX(Component.CENTER_ALIGNMENT);

            sidebarFooter.add(avatar);
            sidebarFooter.add(Box.createVerticalStrut(8));
            sidebarFooter.add(logout);
        } else {
            sidebarFooter.setLayout(new BorderLayout());
            sidebarFooter.setBorder(new EmptyBorder(6, 10, 14, 10));

            // Floating Profile Card - Borderless, Soft Drop Shadow & Matching High Roundness
            JPanel card = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    int arc = 28; // Matching modern high roundness

                    // Subtle ambient shadow
                    g2.setColor(new Color(0, 50, 30, 7));
                    g2.fillRoundRect(1, 2, w - 2, h - 2, arc, arc);

                    // Card surface - Pure crisp white, NO BORDER!
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                    g2.dispose();
                }
            };
            card.setOpaque(false);
            card.setBorder(new EmptyBorder(12, 12, 12, 12));

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setOpaque(false);

            // Row 1: Profile Info (Avatar + Name & Role/Mode badges)
            JPanel profileRow = new JPanel(new BorderLayout(10, 0));
            profileRow.setOpaque(false);
            profileRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JComponent avatar = createUserAvatar(36);
            profileRow.add(avatar, BorderLayout.WEST);

            JPanel details = new JPanel();
            details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
            details.setOpaque(false);

            String rawName = currentUser.getFullName();
            String dispName = (rawName != null && "System Administrator".equalsIgnoreCase(rawName.trim()))
                    ? "System Admin"
                    : (rawName != null ? rawName.trim() : "User");

            JLabel nameLbl = new JLabel(dispName);
            nameLbl.setFont(AppTheme.font(Font.BOLD, 12));
            nameLbl.setForeground(AppTheme.TEXT_PRIMARY);
            nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            nameLbl.setToolTipText(currentUser.getFullName() + " (@" + currentUser.getUsername() + ")");

            JPanel subRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            subRow.setOpaque(false);
            subRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            String roleStr = currentUser.getRole();
            if ("ADMIN".equalsIgnoreCase(roleStr)) roleStr = "Admin";
            else if ("CASHIER".equalsIgnoreCase(roleStr)) roleStr = "Cashier";
            JLabel roleLbl = new JLabel(roleStr);
            roleLbl.setFont(AppTheme.font(Font.PLAIN, 10));
            roleLbl.setForeground(AppTheme.TEXT_MUTED);

            JLabel dot = new JLabel("•");
            dot.setFont(new Font("SansSerif", Font.PLAIN, 9));
            dot.setForeground(new Color(180, 205, 192));

            subRow.add(roleLbl);
            subRow.add(dot);
            subRow.add(lblModeBadge);

            details.add(nameLbl);
            details.add(Box.createVerticalStrut(2));
            details.add(subRow);

            profileRow.add(details, BorderLayout.CENTER);
            content.add(profileRow);

            // Inner Hairline Divider with comfortable vertical margins
            content.add(Box.createVerticalStrut(10));
            JSeparator sep = new JSeparator() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(210, 234, 198, 120));
                    g2.drawLine(0, 0, getWidth(), 0);
                    g2.dispose();
                }
            };
            sep.setAlignmentX(Component.LEFT_ALIGNMENT);
            sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            sep.setPreferredSize(new Dimension(100, 1));
            content.add(sep);
            content.add(Box.createVerticalStrut(10));

            // Row 2: Left-Aligned Integrated Logout Button
            JButton logout = createLogoutButton(false);
            logout.setAlignmentX(Component.LEFT_ALIGNMENT);
            content.add(logout);

            card.add(content, BorderLayout.CENTER);
            sidebarFooter.add(card, BorderLayout.CENTER);
        }

        sidebarHeader.revalidate();
        sidebarHeader.repaint();
        sidebarFooter.revalidate();
        sidebarFooter.repaint();
    }

    private JLabel createModeBadge() {
        JLabel badge = new JLabel("TOUCH") {
            {
                setFont(AppTheme.font(Font.BOLD, 9));
                setOpaque(false);
                setBorder(new EmptyBorder(2, 6, 2, 6));
                setHorizontalAlignment(SwingConstants.CENTER);
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(getBackground());
                int h = getHeight();
                g2.fillRoundRect(0, 0, getWidth(), h, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setBackground(AppTheme.ELECTRIC_LIME);
        badge.setForeground(AppTheme.FOREST_DEEP);
        return badge;
    }

    private JComponent createUserAvatar(int size) {
        String name = currentUser.getFullName();
        String initial = (name != null && !name.trim().isEmpty()) ? name.trim().substring(0, 1).toUpperCase() : "U";
        return new JComponent() {
            {
                setPreferredSize(new Dimension(size, size));
                setMinimumSize(new Dimension(size, size));
                setMaximumSize(new Dimension(size, size));
                setOpaque(false);
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int sz = Math.min(w, h);
                int x = (w - sz) / 2;
                int y = (h - sz) / 2;

                // Full rounded circle avatar
                GradientPaint gp = new GradientPaint(x, y, AppTheme.FOREST_MID, x + sz, y + sz, AppTheme.FOREST_DEEP);
                g2.setPaint(gp);
                g2.fillOval(x, y, sz, sz);

                g2.setColor(new Color(255, 255, 255, 45));
                g2.setStroke(new BasicStroke(1f));
                g2.drawOval(x, y, sz - 1, sz - 1);

                g2.setColor(Color.WHITE);
                g2.setFont(AppTheme.font(Font.BOLD, Math.round(sz * 0.44f)));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(initial)) / 2;
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(initial, tx, ty);
                g2.dispose();
            }
        };
    }

    private JButton createLogoutButton(boolean collapsed) {
        JButton btn = new JButton(collapsed ? "" : "Log out", new SidebarIcon("logout", collapsed ? 16 : 14, AppTheme.TEXT_SECONDARY)) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        setForeground(AppTheme.STATUS_DANGER);
                        setIcon(new SidebarIcon("logout", collapsed ? 16 : 14, AppTheme.STATUS_DANGER));
                        repaint();
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        hovered = false;
                        setForeground(AppTheme.TEXT_SECONDARY);
                        setIcon(new SidebarIcon("logout", collapsed ? 16 : 14, AppTheme.TEXT_SECONDARY));
                        repaint();
                    }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                if (collapsed) {
                    if (hovered) {
                        g2.setColor(new Color(254, 242, 242));
                        g2.fillOval(1, 1, w - 2, h - 2);
                        g2.setColor(new Color(254, 202, 202));
                        g2.drawOval(1, 1, w - 3, h - 3);
                    } else {
                        g2.setColor(Color.WHITE);
                        g2.fillOval(1, 1, w - 2, h - 2);
                        g2.setColor(AppTheme.BORDER_SAGE);
                        g2.drawOval(1, 1, w - 3, h - 3);
                    }
                } else {
                    int arc = 12;
                    if (hovered) {
                        g2.setColor(new Color(254, 242, 242));
                        g2.fillRoundRect(0, 0, w, h, arc, arc);
                        g2.setColor(new Color(254, 202, 202));
                        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                    } else {
                        g2.setColor(new Color(246, 250, 248));
                        g2.fillRoundRect(0, 0, w, h, arc, arc);
                        g2.setColor(new Color(210, 234, 198, 120));
                        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                    }
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 11));
        btn.setForeground(AppTheme.TEXT_SECONDARY);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setIconTextGap(8);
        if (collapsed) {
            btn.setPreferredSize(new Dimension(36, 36));
            btn.setMaximumSize(new Dimension(36, 36));
            btn.setHorizontalAlignment(SwingConstants.CENTER);
            btn.setToolTipText("Log out of account");
        } else {
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setBorder(new EmptyBorder(7, 10, 7, 10));
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            btn.setPreferredSize(new Dimension(190, 32));
            btn.setToolTipText("Log out of current session");
        }
        btn.addActionListener(e -> logout());
        return btn;
    }

    public void updateUIVisibilityFromSettings() {
        boolean isTouch = AppSettings.isTouchMode();
        if (lblModeBadge != null) {
            if (isTouch) {
                lblModeBadge.setText("TOUCH");
                lblModeBadge.setBackground(AppTheme.ELECTRIC_LIME);
                lblModeBadge.setForeground(AppTheme.FOREST_DEEP);
            } else {
                lblModeBadge.setText("DESKTOP");
                lblModeBadge.setBackground(new Color(230, 244, 236));
                lblModeBadge.setForeground(AppTheme.FOREST_GREEN);
            }
            lblModeBadge.repaint();
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
        if (btnNavSelfCheckout != null) btnNavSelfCheckout.setVisible(showPos || !isAdmin);
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
        homeStatsPanel.setBackground(APP_BG);
        homeStatsPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        // Welcome Header (No background, no border, larger font, circular refresh button)
        JPanel headerPanel = new JPanel(new BorderLayout(16, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(4, 4, 18, 4));

        JLabel welcomeMsg = new JLabel("Welcome back, " + currentUser.getFullName());
        welcomeMsg.setFont(AppTheme.font(Font.BOLD, 26));
        welcomeMsg.setForeground(AppTheme.TEXT_PRIMARY);
        headerPanel.add(welcomeMsg, BorderLayout.WEST);

        // Circular Refresh Button with smooth 180deg single-rotation animation
        JButton btnRefreshStats = new JButton() {
            private boolean hovered = false;
            private double currentAngle = 0.0;
            private javax.swing.Timer animTimer = null;
            private final SidebarIcon refreshIcon = new SidebarIcon("refresh", 17, AppTheme.FOREST_GREEN);
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
                addActionListener(e -> {
                    startRotateAnimation();
                    loadDashboardStats();
                });
            }

            private void startRotateAnimation() {
                if (animTimer != null && animTimer.isRunning()) return;
                final long startTime = System.currentTimeMillis();
                final long duration = 400; // 400ms smooth 180deg turn
                final double startA = currentAngle;

                animTimer = new javax.swing.Timer(16, evt -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed >= duration) {
                        currentAngle = startA + Math.PI;
                        animTimer.stop();
                        repaint();
                    } else {
                        double t = (double) elapsed / duration;
                        // Ease out cubic
                        double ease = 1.0 - Math.pow(1.0 - t, 3);
                        currentAngle = startA + ease * Math.PI;
                        repaint();
                    }
                });
                animTimer.start();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int sz = Math.min(getWidth(), getHeight()) - 2;
                int x = (getWidth() - sz) / 2;
                int y = (getHeight() - sz) / 2;
                g2.setColor(hovered ? AppTheme.BG_CANVAS : Color.WHITE);
                g2.fillOval(x, y, sz, sz);
                g2.setColor(hovered ? AppTheme.FOREST_MID : AppTheme.BORDER_SAGE);
                g2.setStroke(new BasicStroke(hovered ? 1.5f : 1.1f));
                g2.drawOval(x, y, sz, sz);

                // Draw rotated icon at exact center
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                Graphics2D gIcon = (Graphics2D) g2.create();
                gIcon.rotate(currentAngle, cx, cy);
                int iconW = refreshIcon.getIconWidth();
                int iconH = refreshIcon.getIconHeight();
                refreshIcon.paintIcon(this, gIcon, cx - iconW / 2, cy - iconH / 2);
                gIcon.dispose();

                g2.dispose();
            }
        };
        btnRefreshStats.setPreferredSize(new Dimension(38, 38));
        btnRefreshStats.setFocusPainted(false);
        btnRefreshStats.setContentAreaFilled(false);
        btnRefreshStats.setBorderPainted(false);
        btnRefreshStats.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefreshStats.setToolTipText("Refresh Metrics");

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerRight.setOpaque(false);
        headerRight.add(btnRefreshStats);
        headerPanel.add(headerRight, BorderLayout.EAST);

        homeStatsPanel.add(headerPanel, BorderLayout.NORTH);

        // 1. Revenue & Operations Metrics Grid (Gap decreased by half: 6px x 6px)
        statsGrid = new JPanel(new GridLayout(0, 5, 6, 6));
        statsGrid.setBackground(APP_BG);

        lblTodaySales = new JLabel("₹0");
        lblMonthlySales = new JLabel("₹0");
        lblYearlySales = new JLabel("₹0");
        lblTotalRevenue = new JLabel("₹0");
        lblLowStock = new JLabel("0");
        lblTotalProducts = new JLabel("0");
        lblTotalStock = new JLabel("0");
        lblTotalCustomers = new JLabel("0");
        lblTotalSuppliers = new JLabel("0");
        JLabel lblProfitMargin = new JLabel("Live POS");

        statsGrid.add(createMetricCard("Today's Revenue", lblTodaySales, AppTheme.FOREST_GREEN, "zap"));
        statsGrid.add(createMetricCard("Monthly Revenue", lblMonthlySales, AppTheme.FOREST_MID, "calendar"));
        statsGrid.add(createMetricCard("Yearly Revenue", lblYearlySales, AppTheme.STATUS_INFO, "calendar"));
        statsGrid.add(createMetricCard("Total Revenue", lblTotalRevenue, AppTheme.STATUS_SUCCESS, "wallet"));
        statsGrid.add(createMetricCard("Low Stock Alerts", lblLowStock, AppTheme.STATUS_DANGER, "alert_triangle"));

        statsGrid.add(createMetricCard("Unique Products", lblTotalProducts, AppTheme.TEXT_SECONDARY, "products"));
        statsGrid.add(createMetricCard("Total Stock Units", lblTotalStock, AppTheme.FOREST_MID, "boxes"));
        statsGrid.add(createMetricCard("Total Customers", lblTotalCustomers, AppTheme.STATUS_WARNING, "customers"));
        statsGrid.add(createMetricCard("Total Suppliers", lblTotalSuppliers, AppTheme.TEXT_MUTED, "building"));
        statsGrid.add(createMetricCard("POS Billing Status", lblProfitMargin, AppTheme.FOREST_GREEN, "check_circle"));

        // 2. Chart Controls Toolbar
        chartToolbar = new JPanel(new BorderLayout(10, 0));
        chartToolbar.setOpaque(false);
        chartToolbar.setBorder(new EmptyBorder(10, 0, 6, 0));

        lblChartHeader = new JLabel("Interactive Revenue Visual Analytics & Trends");
        lblChartHeader.setIcon(new SidebarIcon("reports", 16, AppTheme.FOREST_GREEN));
        lblChartHeader.setIconTextGap(8);
        lblChartHeader.setFont(AppTheme.font(Font.BOLD, 14));
        lblChartHeader.setForeground(AppTheme.TEXT_PRIMARY);

        filtersPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        filtersPanel.setOpaque(false);

        cmbTimelineFilter = new ModernDropdown("Timeline", "calendar", new String[]{
                "Daily Revenue (Last 7 Days)",
                "Daily Revenue (Last 14 Days)",
                "Daily Revenue (Last 30 Days)",
                "Monthly Revenue (Last 6 Months)",
                "Monthly Revenue (Last 12 Months)",
                "Yearly Revenue (Last 5 Years)"
        });
        cmbTimelineFilter.addActionListener(e -> refreshBarChartData());
        filtersPanel.add(cmbTimelineFilter);

        cmbBreakdownFilter = new ModernDropdown("Breakdown", "pie_chart", new String[]{
                "By Product Category",
                "By Payment Source (Cash/UPI/Card)"
        });
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
        if (availableWidth >= 1050) {
            targetCols = 5;
        } else if (availableWidth >= 850) {
            targetCols = 4;
        } else if (availableWidth >= 650) {
            targetCols = 3;
        } else {
            targetCols = 2;
        }

        if (currentStatsCols != targetCols && statsGrid != null) {
            currentStatsCols = targetCols;
            statsGrid.setLayout(new GridLayout(0, targetCols, 7, 6));
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
        if (availableWidth >= 1080) {
            targetChartsMode = 0; // 3 columns side by side
        } else if (availableWidth >= 740) {
            targetChartsMode = 1; // 2 tiers: Row 1 = 2 charts, Row 2 = 1 wide chart
        } else {
            targetChartsMode = 2; // 1 column stacked
        }

        if (currentChartsLayoutMode != targetChartsMode && chartsWrapper != null) {
            currentChartsLayoutMode = targetChartsMode;
            chartsWrapper.removeAll();

            if (targetChartsMode == 0) {
                // 3 columns side-by-side
                chartsWrapper.setLayout(new GridLayout(1, 3, 7, 6));
                chartsWrapper.setPreferredSize(new Dimension(0, 360));
                chartsWrapper.setMinimumSize(new Dimension(0, 260));
                chartsWrapper.setMaximumSize(null);
                revenueBarChart.setPreferredSize(null);
                revenuePieChart.setPreferredSize(null);
                topProductsChart.setPreferredSize(null);
                chartsWrapper.add(revenueBarChart);
                chartsWrapper.add(revenuePieChart);
                chartsWrapper.add(topProductsChart);
            } else if (targetChartsMode == 1) {
                // 2 tiers
                chartsWrapper.setLayout(new GridLayout(2, 1, 7, 6));
                chartsWrapper.setPreferredSize(new Dimension(0, 540));
                chartsWrapper.setMinimumSize(new Dimension(0, 440));
                chartsWrapper.setMaximumSize(null);

                JPanel topTwo = new JPanel(new GridLayout(1, 2, 7, 6));
                topTwo.setOpaque(false);
                topTwo.add(revenueBarChart);
                topTwo.add(revenuePieChart);

                chartsWrapper.add(topTwo);
                chartsWrapper.add(topProductsChart);
            } else {
                // 1 column vertically stacked
                chartsWrapper.setLayout(new GridLayout(3, 1, 7, 6));
                chartsWrapper.setPreferredSize(new Dimension(0, 780));
                chartsWrapper.setMinimumSize(new Dimension(0, 660));
                chartsWrapper.setMaximumSize(null);

                chartsWrapper.add(revenueBarChart);
                chartsWrapper.add(revenuePieChart);
                chartsWrapper.add(topProductsChart);
            }

            chartsWrapper.revalidate();
            chartsWrapper.repaint();
            if (chartsWrapper.getParent() != null) {
                chartsWrapper.getParent().revalidate();
            }
        }
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accentColor, String iconName) {
        SidebarIcon bgIcon = new SidebarIcon(iconName, 56, accentColor);

        JPanel card = new JPanel(new BorderLayout()) {
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
                int r = 36; // Extra rounded modern widget silhouette
                int w = getWidth();
                int h = getHeight();

                // Subtle ambient drop shadow
                g2.setColor(new Color(0, 50, 30, hovered ? 12 : 5));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);

                // Card surface - Pure crisp white, NO BORDER!
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);

                // Clip to rounded shape for smooth watermark icon rendering
                Shape clip = new RoundRectangle2D.Float(0, 0, w, h, r, r);
                g2.setClip(clip);

                // Big dimmer opacity icon on the right background (no bg box)
                int iconSz = 56;
                int iconX = w - iconSz - 12;
                int iconY = (h - iconSz) / 2;

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hovered ? 0.16f : 0.09f));
                bgIcon.paintIcon(this, g2, iconX, iconY);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        // Increased inner padding
        card.setBorder(new EmptyBorder(16, 22, 16, 22));
        card.setPreferredSize(new Dimension(190, 102));
        card.setMinimumSize(new Dimension(140, 92));

        // Text Column: Value ABOVE, Label BELOW
        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);

        // Value: Larger size (26pt), thinner weight (Font.PLAIN)
        valueLabel.setFont(AppTheme.font(Font.PLAIN, 26));
        valueLabel.setForeground(AppTheme.TEXT_PRIMARY);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Label: Below the value
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(AppTheme.font(Font.BOLD, 11));
        lblTitle.setForeground(AppTheme.TEXT_MUTED);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        textCol.add(valueLabel);
        textCol.add(Box.createVerticalStrut(4));
        textCol.add(lblTitle);

        card.add(textCol, BorderLayout.CENTER);

        return card;
    }

    private String formatAmount(double amount) {
        return AppTheme.formatCurrency(amount);
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

            lblTodaySales.setText(formatAmount(todaySales));
            double yearSales = (double) stats.getOrDefault("yearlySalesAmount", 0.0);
            lblMonthlySales.setText(formatAmount(monthSales));
            if (lblYearlySales != null) lblYearlySales.setText(formatAmount(yearSales));
            lblTotalRevenue.setText(formatAmount(totalRev));
            lblLowStock.setText(String.valueOf(lowStock));
            lblTotalProducts.setText(String.valueOf(totalPrd));
            lblTotalStock.setText(String.format("%,d", totalUnits));
            lblTotalCustomers.setText(String.format("%,d", totalCust));
            lblTotalSuppliers.setText(String.format("%,d", totalSupp));

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
                revenueBarChart.setData(reportDAO.getDailySalesTrend(7), "Daily Revenue Trend (Last 7 Days)");
            } else if (sel == 1) {
                revenueBarChart.setData(reportDAO.getDailySalesTrend(14), "Daily Revenue Trend (Last 14 Days)");
            } else if (sel == 2) {
                revenueBarChart.setData(reportDAO.getDailySalesTrend(30), "Daily Revenue Trend (Last 30 Days)");
            } else if (sel == 3) {
                revenueBarChart.setData(reportDAO.getMonthlyRevenueTrend(6), "Monthly Revenue Trend (Last 6 Months)");
            } else if (sel == 4) {
                revenueBarChart.setData(reportDAO.getMonthlyRevenueTrend(12), "Monthly Revenue Trend (Last 12 Months)");
            } else {
                revenueBarChart.setData(reportDAO.getYearlyRevenueTrend(5), "Yearly Revenue Trend (Last 5 Years)");
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
                revenuePieChart.setData(reportDAO.getCategorySalesBreakdown(), "Revenue by Category Share");
            } else {
                revenuePieChart.setData(reportDAO.getRevenueByPaymentSource(), "Revenue by Payment Source (Cash / UPI / Card)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshTopProductsChartData() {
        if (topProductsChart == null) return;
        try {
            topProductsChart.setData(reportDAO.getTopSellingProducts(6), "Top Selling Products");
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

