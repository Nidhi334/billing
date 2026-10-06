package ui;

import config.DBConnection;
import dao.UserDAO;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Properties;

public class LoginFrame extends JFrame {
    private JComboBox<String> txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnDbConfig;
    private JButton btnForgotPassword;
    private UserDAO userDAO = new UserDAO();

    public LoginFrame() {
        setTitle("Login - Billing & Inventory Management System");
        setSize(480, 640);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        // mainPanel.setBackground(new Color(245, 247, 250));
        mainPanel.setBackground(Color.LIGHT_GRAY);

        // Header Banner
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(30, 41, 59));
        headerPanel.setPreferredSize(new Dimension(480, 110));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("SmartBilling Pro");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Billing & Inventory Management System");
        subLabel.setForeground(new Color(148, 163, 184));
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(subLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form Card
        JPanel formCard = new JPanel();
        formCard.setBackground(Color.LIGHT_GRAY);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(25, 35, 25, 35),
                // BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true)));
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true)));
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblSignIn = new JLabel("Sign In to your account");
        lblSignIn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblSignIn.setForeground(new Color(30, 41, 59));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formCard.add(lblSignIn, gbc);

        // Username
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        formCard.add(new JLabel("Username:"), gbc);

        txtUsername = new JComboBox<>(new String[] { "ADMIN", "STAFF" });

        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtUsername.setPreferredSize(new Dimension(320, 36));
        gbc.gridy = 2;
        formCard.add(txtUsername, gbc);

        // Password
        gbc.gridy = 3;
        formCard.add(new JLabel("Password:"), gbc);

        txtPassword = new JPasswordField("admin123");
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setPreferredSize(new Dimension(320, 36));
        gbc.gridy = 4;
        formCard.add(txtPassword, gbc);

        // Login Button
        btnLogin = new JButton("LOGIN");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(new Color(37, 99, 235));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setPreferredSize(new Dimension(320, 38));
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        gbc.gridy = 5;
        gbc.insets = new Insets(16, 10, 6, 10);
        formCard.add(btnLogin, gbc);

        // Offline / Demo Login Button
        JButton btnDemoLogin = new JButton("⚡ Quick Offline / Demo Login");
        btnDemoLogin.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDemoLogin.setBackground(new Color(200, 75, 75));
        btnDemoLogin.setForeground(Color.WHITE);
        btnDemoLogin.setFocusPainted(false);
        btnDemoLogin.setPreferredSize(new Dimension(320, 34));
        btnDemoLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDemoLogin.setToolTipText("Login as Admin in Offline/Demo mode without waiting for MySQL");
        btnDemoLogin.addActionListener(e -> launchDemoAdmin());
        gbc.gridy = 6;
        gbc.insets = new Insets(4, 10, 6, 10);
        formCard.add(btnDemoLogin, gbc);

        // Secondary Links Row (Forgot Password & DB Settings)
        JPanel linksPanel = new JPanel(new BorderLayout(10, 0));
        linksPanel.setOpaque(false);

        btnForgotPassword = new JButton("Forgot Password?");
        btnForgotPassword.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnForgotPassword.setForeground(new Color(37, 99, 235));
        btnForgotPassword.setContentAreaFilled(false);
        btnForgotPassword.setBorderPainted(false);
        btnForgotPassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        linksPanel.add(btnForgotPassword, BorderLayout.WEST);

        btnDbConfig = new JButton("⚙ DB Settings");
        btnDbConfig.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDbConfig.setForeground(new Color(100, 116, 139));
        btnDbConfig.setContentAreaFilled(false);
        btnDbConfig.setBorderPainted(false);
        btnDbConfig.setCursor(new Cursor(Cursor.HAND_CURSOR));
        linksPanel.add(btnDbConfig, BorderLayout.EAST);

        gbc.gridy = 7;
        gbc.insets = new Insets(6, 10, 4, 10);
        formCard.add(linksPanel, gbc);

        mainPanel.add(formCard, BorderLayout.CENTER);

        // Footer Section
        JPanel footerBox = new JPanel();
        footerBox.setLayout(new BoxLayout(footerBox, BoxLayout.Y_AXIS));
        footerBox.setOpaque(false);
        footerBox.setBorder(new EmptyBorder(6, 15, 12, 15));

        JButton btnSelfCheckoutMode = new JButton("🛒 Open Customer Self-Checkout");
        btnSelfCheckoutMode.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSelfCheckoutMode.setBackground(new Color(16, 185, 129));
        btnSelfCheckoutMode.setForeground(Color.WHITE);
        btnSelfCheckoutMode.setFocusPainted(false);
        btnSelfCheckoutMode.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSelfCheckoutMode.setMaximumSize(new Dimension(360, 36));
        btnSelfCheckoutMode.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSelfCheckoutMode.setToolTipText("Open full-screen express customer self-service checkout");
        btnSelfCheckoutMode.addActionListener(e -> new SelfCheckoutFrame(null).setVisible(true));

        JLabel footerNote = new JLabel("Default logins: admin/admin123 (Admin) | staff/staff123 (Staff)",
                SwingConstants.CENTER);
        footerNote.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        footerNote.setForeground(new Color(100, 116, 139));
        footerNote.setAlignmentX(Component.CENTER_ALIGNMENT);
        footerNote.setBorder(new EmptyBorder(6, 0, 0, 0));

        footerBox.add(btnSelfCheckoutMode);
        footerBox.add(footerNote);
        mainPanel.add(footerBox, BorderLayout.SOUTH);

        add(mainPanel);

        // Actions
        btnLogin.addActionListener(e -> performLogin());
        txtPassword.addActionListener(e -> performLogin());
        btnForgotPassword.addActionListener(e -> openForgotPasswordDialog());
        btnDbConfig.addActionListener(e -> openDbConfigDialog());
    }

    private void launchDemoAdmin() {
        User demoAdmin = new User(1, "admin", "admin123", "System Administrator (Demo/Offline)", "ADMIN");
        launchDashboard(demoAdmin, true);
    }

    private void launchDashboard(User user) {
        launchDashboard(user, false);
    }

    private void launchDashboard(User user, boolean offlineDemo) {
        try {
            DashboardFrame dashboard = new DashboardFrame(user, offlineDemo);
            dashboard.setVisible(true);
            dispose();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Login succeeded, but the dashboard could not be opened.\n\nDetails: " + ex.getMessage(),
                    "Dashboard Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performLogin() {
        String username = String.valueOf(txtUsername.getSelectedItem()).trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.", "Input Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        User user = null;
        try {
            user = userDAO.authenticate(username, password);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "";
            boolean isConnRefused = msg.toLowerCase().contains("connection refused")
                    || msg.toLowerCase().contains("communications link failure")
                    || msg.toLowerCase().contains("can't connect to local mysql server");

            boolean isDefaultAdmin = "admin".equalsIgnoreCase(username) && "admin123".equals(password);
            boolean isDefaultStaff = "staff".equalsIgnoreCase(username) && "staff123".equals(password);

            if (isConnRefused && (isDefaultAdmin || isDefaultStaff)) {
                String notice = "⚠️ MySQL DATABASE IS NOT RUNNING (Connection Refused on port 3306)!\n\n"
                        + "👉 MySQL service start karne ke liye terminal me ye command chalayein:\n"
                        + "   sudo systemctl start mysql\n"
                        + "   (ya: sudo service mysql start)\n\n"
                        + "Kya aap abhi ke liye OFFLINE / DEMO MODE me Login karna chahte hain?";

                int choice = JOptionPane.showConfirmDialog(this, notice,
                        "MySQL Offline - Demo Mode Available", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if (choice == JOptionPane.YES_OPTION) {
                    if (isDefaultAdmin) {
                        launchDashboard(
                                new User(1, "admin", "admin123", "System Administrator (Demo/Offline)", "ADMIN"), true);
                    } else {
                        launchDashboard(new User(2, "staff", "staff123", "Cashier Desk (Demo/Offline)", "STAFF"), true);
                    }
                    return;
                } else {
                    return;
                }
            } else {
                String reason = isConnRefused
                        ? "MySQL Database server band hai (Connection Refused)!\n\n"
                                + "👉 Terminal me MySQL start karein:\n"
                                + "   sudo systemctl start mysql\n\n"
                                + "Ya Default credentials use karein: admin / admin123"
                        : "Database connection error!\n\nDetails: " + msg
                                + "\n\nPlease ensure MySQL is running and credentials in 'DB Settings' are correct.";
                JOptionPane.showMessageDialog(this, reason, "Database Connection Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if (user == null) {
            JOptionPane.showMessageDialog(this, "Invalid username or password!", "Authentication Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        launchDashboard(user);
    }

    private void openForgotPasswordDialog() {
        ForgotPasswordDialog dialog = new ForgotPasswordDialog(this);
        dialog.setVisible(true);
    }

    private void openDbConfigDialog() {
        Properties prop = DBConnection.getProperties();
        JTextField hostField = new JTextField(prop.getProperty("db.host", "localhost"));
        JTextField portField = new JTextField(prop.getProperty("db.port", "3306"));
        JTextField dbField = new JTextField(prop.getProperty("db.name", "billing_system"));
        JTextField userField = new JTextField(prop.getProperty("db.user", "root"));
        JPasswordField passField = new JPasswordField(prop.getProperty("db.password", ""));

        JPanel panel = new JPanel(new GridLayout(6, 2, 8, 8));
        panel.add(new JLabel("MySQL Host:"));
        panel.add(hostField);
        panel.add(new JLabel("MySQL Port:"));
        panel.add(portField);
        panel.add(new JLabel("Database Name:"));
        panel.add(dbField);
        panel.add(new JLabel("Username:"));
        panel.add(userField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);

        JButton btnTestConn = new JButton("🔍 Test Connection");
        panel.add(btnTestConn);
        panel.add(new JLabel("(Click to test)"));

        btnTestConn.addActionListener(e -> {
            String testResult = DBConnection.testConnection(
                    hostField.getText().trim(),
                    portField.getText().trim(),
                    dbField.getText().trim(),
                    userField.getText().trim(),
                    new String(passField.getPassword()));
            JOptionPane.showMessageDialog(this, testResult, "Database Test Result",
                    testResult.startsWith("SUCCESS") || testResult.startsWith("CONNECTED")
                            ? JOptionPane.INFORMATION_MESSAGE
                            : JOptionPane.ERROR_MESSAGE);
        });

        int res = JOptionPane.showConfirmDialog(this, panel, "MySQL Database Settings", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            DBConnection.saveConfig(
                    hostField.getText().trim(),
                    portField.getText().trim(),
                    dbField.getText().trim(),
                    userField.getText().trim(),
                    new String(passField.getPassword()));
            JOptionPane.showMessageDialog(this, "Database settings saved successfully!", "Saved",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
