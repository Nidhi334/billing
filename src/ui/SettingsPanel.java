package ui;

import config.AppSettings;
import config.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Properties;

public class SettingsPanel extends JPanel {
    private Runnable onSettingsChangedCallback;

    // Screen Mode Selector (Touch vs Non-Touch)
    private JRadioButton rdoTouchScreen;
    private JRadioButton rdoNonTouchScreen;
    private ButtonGroup grpScreenMode;

    // POS & Feature Button Visibility Checkboxes
    private JCheckBox chkPosNav;
    private JCheckBox chkNumpad; // NUPED / Numpad
    private JCheckBox chkHeldBills; // Bed / Table / Multi-bill hold
    private JCheckBox chkQuickCash;
    private JCheckBox chkDiscount;
    private JCheckBox chkUpiQr;
    private JCheckBox chkBarcodeSearch;
    private JCheckBox chkCartActions;

    // Device & Hardware Settings
    private JCheckBox chkBarcodeBeep;
    private JCheckBox chkAutoPrint;
    private JTextField txtPrinterName;

    // Database Connection Credentials (Configurable in Settings)
    private JTextField txtDbHost;
    private JTextField txtDbPort;
    private JTextField txtDbName;
    private JTextField txtDbUser;
    private JPasswordField txtDbPass;

    // Sidebar Navigation Modules
    private JCheckBox chkNavProducts;
    private JCheckBox chkNavInventory;
    private JCheckBox chkNavCustomers;
    private JCheckBox chkNavSuppliers;
    private JCheckBox chkNavReports;

    // Store Details
    private JTextField txtStoreName;
    private JTextField txtStorePhone;
    private JTextField txtStoreGst;

    public SettingsPanel(Runnable onSettingsChangedCallback) {
        this.onSettingsChangedCallback = onSettingsChangedCallback;
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(248, 250, 252));
        setBorder(new EmptyBorder(15, 18, 15, 18));
        initComponents();
        loadCurrentSettings();
    }

    private void initComponents() {
        // TOP BANNER
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(248, 250, 252));

        JLabel lblTitle = new JLabel("⚙️ System, Hardware & UI Settings");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(30, 41, 59));
        topPanel.add(lblTitle, BorderLayout.WEST);

        JLabel lblSub = new JLabel("Configure Touch vs Non-Touch interface, POS buttons (Numpad, Bed/Hold), Devices, and Database Credentials.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(100, 116, 139));
        topPanel.add(lblSub, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // MAIN SCROLLABLE CONTENT
        JPanel scrollContent = new JPanel();
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBackground(new Color(248, 250, 252));

        // 1. TOUCH VS NON-TOUCH SCREEN MODE
        JPanel modeCard = createSectionCard("🖥️ Screen Mode: Touch-Enabled vs Non-Touch Standard Desktop");
        modeCard.setLayout(new GridLayout(2, 1, 8, 8));

        rdoTouchScreen = new JRadioButton("📱 Touch-Enabled Screen (Optimized: Big touch buttons, on-screen Numpad / NUPED, big Quick-Cash buttons)");
        rdoTouchScreen.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rdoTouchScreen.setBackground(Color.WHITE);
        rdoTouchScreen.setForeground(new Color(16, 185, 129));

        rdoNonTouchScreen = new JRadioButton("💻 Non-Touch Screen / Standard Desktop (Optimized: Keyboard shortcuts F1-F6, hidden Numpad, compact layout)");
        rdoNonTouchScreen.setFont(new Font("Segoe UI", Font.BOLD, 13));
        rdoNonTouchScreen.setBackground(Color.WHITE);
        rdoNonTouchScreen.setForeground(new Color(37, 99, 235));

        grpScreenMode = new ButtonGroup();
        grpScreenMode.add(rdoTouchScreen);
        grpScreenMode.add(rdoNonTouchScreen);

        modeCard.add(rdoTouchScreen);
        modeCard.add(rdoNonTouchScreen);

        // Quick preset handlers
        rdoTouchScreen.addActionListener(e -> {
            chkNumpad.setSelected(true);
            chkQuickCash.setSelected(true);
            chkHeldBills.setSelected(true);
        });
        rdoNonTouchScreen.addActionListener(e -> {
            chkNumpad.setSelected(false); // Keyboard has its own numpad
        });

        scrollContent.add(modeCard);
        scrollContent.add(Box.createVerticalStrut(15));

        // 2. POS BUTTONS & SCREEN VISIBILITY
        JPanel posCard = createSectionCard("⚡ POS Screen Buttons & Feature Visibility");
        posCard.setLayout(new GridLayout(4, 2, 12, 10));

        chkPosNav = new JCheckBox("Show 'New Sale / POS' Navigation Button");
        chkNumpad = new JCheckBox("Show Touchscreen Numpad / NUPED (0-9, ., C)");
        chkHeldBills = new JCheckBox("Show Bed / Table / Multi-Bill Hold Buttons (Hold & Recall)");
        chkQuickCash = new JCheckBox("Show Quick Cash Preset Tender Buttons (₹100, ₹200, ₹500, EXACT)");
        chkDiscount = new JCheckBox("Show Discount Buttons (Apply & Remove Discount)");
        chkUpiQr = new JCheckBox("Show Instant UPI QR Code Button (F6)");
        chkBarcodeSearch = new JCheckBox("Show Barcode / Fast Search Input Bar");
        chkCartActions = new JCheckBox("Show Cart Action Buttons (➕ Qty, ➖ Qty, 🗑 Remove, Clear All)");

        styleCheckbox(chkPosNav);
        styleCheckbox(chkNumpad);
        styleCheckbox(chkHeldBills);
        styleCheckbox(chkQuickCash);
        styleCheckbox(chkDiscount);
        styleCheckbox(chkUpiQr);
        styleCheckbox(chkBarcodeSearch);
        styleCheckbox(chkCartActions);

        posCard.add(chkPosNav);
        posCard.add(chkNumpad);
        posCard.add(chkHeldBills);
        posCard.add(chkQuickCash);
        posCard.add(chkDiscount);
        posCard.add(chkUpiQr);
        posCard.add(chkBarcodeSearch);
        posCard.add(chkCartActions);

        scrollContent.add(posCard);
        scrollContent.add(Box.createVerticalStrut(15));

        // 3. HARDWARE & DEVICE SETTINGS
        JPanel devCard = createSectionCard("🖨️ POS Devices & Hardware Integration");
        devCard.setLayout(new GridLayout(3, 2, 12, 8));

        chkBarcodeBeep = new JCheckBox("Enable Audio Beep on Successful Barcode Scan");
        chkAutoPrint = new JCheckBox("Auto-Print Invoice immediately on Sale Complete");
        styleCheckbox(chkBarcodeBeep);
        styleCheckbox(chkAutoPrint);

        devCard.add(chkBarcodeBeep);
        devCard.add(chkAutoPrint);

        devCard.add(new JLabel("Thermal Printer Model / Name:"));
        txtPrinterName = new JTextField();
        devCard.add(txtPrinterName);

        scrollContent.add(devCard);
        scrollContent.add(Box.createVerticalStrut(15));

        // 4. DATABASE CREDENTIALS CONFIGURATION
        JPanel dbCard = createSectionCard("🗄️ MySQL Database Connection & Credentials");
        dbCard.setLayout(new GridBagLayout());
        GridBagConstraints gdb = new GridBagConstraints();
        gdb.insets = new Insets(5, 8, 5, 8);
        gdb.fill = GridBagConstraints.HORIZONTAL;

        gdb.gridx = 0; gdb.gridy = 0; gdb.weightx = 0;
        dbCard.add(new JLabel("Database Host:"), gdb);
        txtDbHost = new JTextField(15);
        gdb.gridx = 1; gdb.weightx = 1.0;
        dbCard.add(txtDbHost, gdb);

        gdb.gridx = 2; gdb.weightx = 0;
        dbCard.add(new JLabel("Port:"), gdb);
        txtDbPort = new JTextField(6);
        gdb.gridx = 3; gdb.weightx = 0.5;
        dbCard.add(txtDbPort, gdb);

        gdb.gridx = 0; gdb.gridy = 1; gdb.weightx = 0;
        dbCard.add(new JLabel("Database Name:"), gdb);
        txtDbName = new JTextField(15);
        gdb.gridx = 1; gdb.weightx = 1.0;
        dbCard.add(txtDbName, gdb);

        gdb.gridx = 2; gdb.weightx = 0;
        dbCard.add(new JLabel("Username:"), gdb);
        txtDbUser = new JTextField(10);
        gdb.gridx = 3; gdb.weightx = 0.5;
        dbCard.add(txtDbUser, gdb);

        gdb.gridx = 0; gdb.gridy = 2; gdb.weightx = 0;
        dbCard.add(new JLabel("Password:"), gdb);
        txtDbPass = new JPasswordField(15);
        gdb.gridx = 1; gdb.weightx = 1.0;
        dbCard.add(txtDbPass, gdb);

        JButton btnTestConn = new JButton("🔌 Test Connection");
        btnTestConn.setBackground(new Color(241, 245, 249));
        btnTestConn.addActionListener(e -> testDatabaseConnection());
        gdb.gridx = 3; gdb.weightx = 0.5;
        dbCard.add(btnTestConn, gdb);

        scrollContent.add(dbCard);
        scrollContent.add(Box.createVerticalStrut(15));

        // 5. SIDEBAR NAVIGATION BUTTONS
        JPanel navCard = createSectionCard("🧭 Sidebar Menu Visibility");
        navCard.setLayout(new GridLayout(3, 2, 12, 10));

        chkNavProducts = new JCheckBox("Show 'Products' Button");
        chkNavInventory = new JCheckBox("Show 'Inventory / Stock' Button");
        chkNavCustomers = new JCheckBox("Show 'Customers' Button");
        chkNavSuppliers = new JCheckBox("Show 'Suppliers' Button");
        chkNavReports = new JCheckBox("Show 'Reports & P/L' Button");

        styleCheckbox(chkNavProducts);
        styleCheckbox(chkNavInventory);
        styleCheckbox(chkNavCustomers);
        styleCheckbox(chkNavSuppliers);
        styleCheckbox(chkNavReports);

        navCard.add(chkNavProducts);
        navCard.add(chkNavInventory);
        navCard.add(chkNavCustomers);
        navCard.add(chkNavSuppliers);
        navCard.add(chkNavReports);

        scrollContent.add(navCard);
        scrollContent.add(Box.createVerticalStrut(15));

        // 6. STORE / SHOP DETAILS
        JPanel storeCard = createSectionCard("🏢 Shop & Invoice Header Details");
        storeCard.setLayout(new GridLayout(3, 2, 10, 8));

        storeCard.add(new JLabel("Shop / Store Name:"));
        txtStoreName = new JTextField();
        storeCard.add(txtStoreName);

        storeCard.add(new JLabel("Phone / Support No:"));
        txtStorePhone = new JTextField();
        storeCard.add(txtStorePhone);

        storeCard.add(new JLabel("Default GST (%):"));
        txtStoreGst = new JTextField();
        storeCard.add(txtStoreGst);

        scrollContent.add(storeCard);

        JScrollPane scrollPane = new JScrollPane(scrollContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(14);
        add(scrollPane, BorderLayout.CENTER);

        // BOTTOM ACTION BUTTONS
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        bottomBar.setBackground(new Color(248, 250, 252));

        JButton btnResetDefaults = new JButton("Reset All to Default");
        btnResetDefaults.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnResetDefaults.setBackground(new Color(241, 245, 249));
        btnResetDefaults.addActionListener(e -> resetDefaults());

        JButton btnSave = new JButton("💾 Save & Apply All Settings");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setBackground(new Color(16, 185, 129));
        btnSave.setForeground(Color.WHITE);
        btnSave.setPreferredSize(new Dimension(230, 40));
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> saveSettings());

        bottomBar.add(btnResetDefaults);
        bottomBar.add(btnSave);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel createSectionCard(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(
                                BorderFactory.createEmptyBorder(6, 6, 6, 6),
                                title,
                                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                                javax.swing.border.TitledBorder.DEFAULT_POSITION,
                                new Font("Segoe UI", Font.BOLD, 14),
                                new Color(30, 41, 59)
                        ),
                        new EmptyBorder(8, 14, 14, 14)
                )
        ));
        return panel;
    }

    private void styleCheckbox(JCheckBox chk) {
        chk.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chk.setBackground(Color.WHITE);
        chk.setFocusPainted(false);
        chk.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    public void loadCurrentSettings() {
        AppSettings.loadSettings();

        // Screen mode
        boolean isTouch = AppSettings.isTouchMode();
        rdoTouchScreen.setSelected(isTouch);
        rdoNonTouchScreen.setSelected(!isTouch);

        // Buttons
        chkPosNav.setSelected(AppSettings.getBoolean(AppSettings.KEY_SHOW_POS_NAV, true));
        chkNumpad.setSelected(AppSettings.getBoolean(AppSettings.KEY_SHOW_NUMPAD, true));
        chkHeldBills.setSelected(AppSettings.getBoolean(AppSettings.KEY_SHOW_HELD_BILLS, true));
        chkQuickCash.setSelected(AppSettings.getBoolean(AppSettings.KEY_SHOW_QUICK_CASH, true));
        chkDiscount.setSelected(AppSettings.getBoolean(AppSettings.KEY_SHOW_DISCOUNT, true));
        chkUpiQr.setSelected(AppSettings.getBoolean(AppSettings.KEY_SHOW_UPI_QR, true));
        chkBarcodeSearch.setSelected(AppSettings.getBoolean(AppSettings.KEY_SHOW_BARCODE_SEARCH, true));
        chkCartActions.setSelected(AppSettings.getBoolean(AppSettings.KEY_SHOW_CART_ACTIONS, true));

        // Devices
        chkBarcodeBeep.setSelected(AppSettings.getBoolean(AppSettings.KEY_BARCODE_BEEP, true));
        chkAutoPrint.setSelected(AppSettings.getBoolean(AppSettings.KEY_AUTO_PRINT, true));
        txtPrinterName.setText(AppSettings.getString(AppSettings.KEY_THERMAL_PRINTER_NAME, "Default 80mm POS Printer"));

        // DB Credentials
        Properties dbProps = DBConnection.getProperties();
        txtDbHost.setText(dbProps.getProperty("db.host", "localhost"));
        txtDbPort.setText(dbProps.getProperty("db.port", "3306"));
        txtDbName.setText(dbProps.getProperty("db.name", "billing_system"));
        txtDbUser.setText(dbProps.getProperty("db.user", "root"));
        txtDbPass.setText(dbProps.getProperty("db.password", ""));

        // Sidebar
        chkNavProducts.setSelected(AppSettings.getBoolean(AppSettings.KEY_NAV_PRODUCTS, true));
        chkNavInventory.setSelected(AppSettings.getBoolean(AppSettings.KEY_NAV_INVENTORY, true));
        chkNavCustomers.setSelected(AppSettings.getBoolean(AppSettings.KEY_NAV_CUSTOMERS, true));
        chkNavSuppliers.setSelected(AppSettings.getBoolean(AppSettings.KEY_NAV_SUPPLIERS, true));
        chkNavReports.setSelected(AppSettings.getBoolean(AppSettings.KEY_NAV_REPORTS, true));

        // Store
        txtStoreName.setText(AppSettings.getString(AppSettings.KEY_STORE_NAME, "SmartBilling Supermarket"));
        txtStorePhone.setText(AppSettings.getString(AppSettings.KEY_STORE_PHONE, "9876543210"));
        txtStoreGst.setText(AppSettings.getString(AppSettings.KEY_STORE_GST, "18.0"));
    }

    private void saveSettings() {
        AppSettings.setString(AppSettings.KEY_SCREEN_MODE, rdoTouchScreen.isSelected() ? "TOUCH" : "NON_TOUCH");

        AppSettings.setBoolean(AppSettings.KEY_SHOW_POS_NAV, chkPosNav.isSelected());
        AppSettings.setBoolean(AppSettings.KEY_SHOW_NUMPAD, chkNumpad.isSelected());
        AppSettings.setBoolean(AppSettings.KEY_SHOW_HELD_BILLS, chkHeldBills.isSelected());
        AppSettings.setBoolean(AppSettings.KEY_SHOW_QUICK_CASH, chkQuickCash.isSelected());
        AppSettings.setBoolean(AppSettings.KEY_SHOW_DISCOUNT, chkDiscount.isSelected());
        AppSettings.setBoolean(AppSettings.KEY_SHOW_UPI_QR, chkUpiQr.isSelected());
        AppSettings.setBoolean(AppSettings.KEY_SHOW_BARCODE_SEARCH, chkBarcodeSearch.isSelected());
        AppSettings.setBoolean(AppSettings.KEY_SHOW_CART_ACTIONS, chkCartActions.isSelected());

        AppSettings.setBoolean(AppSettings.KEY_BARCODE_BEEP, chkBarcodeBeep.isSelected());
        AppSettings.setBoolean(AppSettings.KEY_AUTO_PRINT, chkAutoPrint.isSelected());
        AppSettings.setString(AppSettings.KEY_THERMAL_PRINTER_NAME, txtPrinterName.getText().trim());

        AppSettings.setBoolean(AppSettings.KEY_NAV_PRODUCTS, chkNavProducts.isSelected());
        AppSettings.setBoolean(AppSettings.KEY_NAV_INVENTORY, chkNavInventory.isSelected());
        AppSettings.setBoolean(AppSettings.KEY_NAV_CUSTOMERS, chkNavCustomers.isSelected());
        AppSettings.setBoolean(AppSettings.KEY_NAV_SUPPLIERS, chkNavSuppliers.isSelected());
        AppSettings.setBoolean(AppSettings.KEY_NAV_REPORTS, chkNavReports.isSelected());

        AppSettings.setString(AppSettings.KEY_STORE_NAME, txtStoreName.getText().trim());
        AppSettings.setString(AppSettings.KEY_STORE_PHONE, txtStorePhone.getText().trim());
        AppSettings.setString(AppSettings.KEY_STORE_GST, txtStoreGst.getText().trim());

        AppSettings.saveSettings();

        // Save DB Credentials securely to db_config.properties
        DBConnection.saveConfig(
                txtDbHost.getText().trim(),
                txtDbPort.getText().trim(),
                txtDbName.getText().trim(),
                txtDbUser.getText().trim(),
                new String(txtDbPass.getPassword())
        );

        JOptionPane.showMessageDialog(this,
                "Settings and Database Configuration saved successfully!\nUI layout has been updated.",
                "Settings Saved", JOptionPane.INFORMATION_MESSAGE);

        if (onSettingsChangedCallback != null) {
            onSettingsChangedCallback.run();
        }
    }

    private void testDatabaseConnection() {
        try {
            java.sql.Connection conn = DBConnection.getConnection();
            if (conn != null && !conn.isClosed()) {
                JOptionPane.showMessageDialog(this,
                        "✓ Database connection successful!\nConnected to: " + txtDbName.getText().trim(),
                        "Connection Test", JOptionPane.INFORMATION_MESSAGE);
                conn.close();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "✗ Connection failed!\nError: " + ex.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetDefaults() {
        int opt = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reset all button visibility & hardware options to default?",
                "Reset Confirmation", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            rdoTouchScreen.setSelected(true);
            chkPosNav.setSelected(true);
            chkNumpad.setSelected(true);
            chkHeldBills.setSelected(true);
            chkQuickCash.setSelected(true);
            chkDiscount.setSelected(true);
            chkUpiQr.setSelected(true);
            chkBarcodeSearch.setSelected(true);
            chkCartActions.setSelected(true);

            chkBarcodeBeep.setSelected(true);
            chkAutoPrint.setSelected(true);

            chkNavProducts.setSelected(true);
            chkNavInventory.setSelected(true);
            chkNavCustomers.setSelected(true);
            chkNavSuppliers.setSelected(true);
            chkNavReports.setSelected(true);

            saveSettings();
        }
    }
}
