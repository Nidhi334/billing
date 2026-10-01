package ui;

import config.DBConnection;
import dao.UserDAO;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Properties;

public class LoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnDbConfig;
    private JButton btnForgotPassword;
    private UserDAO userDAO = new UserDAO();

    public LoginFrame() {
        setTitle("Login - Billing & Inventory Management System");
        setSize(480, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(245, 247, 250));

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
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(25, 35, 25, 35),
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true)
        ));
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblSignIn = new JLabel("Sign In to your account");
        lblSignIn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblSignIn.setForeground(new Color(30, 41, 59));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        formCard.add(lblSignIn, gbc);

        // Username
        gbc.gridy = 1; gbc.gridwidth = 2;
        formCard.add(new JLabel("Username:"), gbc);

        txtUsername = new JTextField("admin");
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
        btnLogin.setPreferredSize(new Dimension(320, 40));
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        gbc.gridy = 5;
        gbc.insets = new Insets(18, 10, 8, 10);
        formCard.add(btnLogin, gbc);

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

        gbc.gridy = 6;
        gbc.insets = new Insets(8, 10, 4, 10);
        formCard.add(linksPanel, gbc);

        mainPanel.add(formCard, BorderLayout.CENTER);

        // Footer note
        JLabel footerNote = new JLabel("Default logins: admin/admin123 (Admin) | staff/staff123 (Staff)", SwingConstants.CENTER);
        footerNote.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        footerNote.setForeground(new Color(100, 116, 139));
        footerNote.setBorder(new EmptyBorder(8, 8, 15, 8));
        mainPanel.add(footerNote, BorderLayout.SOUTH);

        add(mainPanel);

        // Actions
        btnLogin.addActionListener(e -> performLogin());
        txtPassword.addActionListener(e -> performLogin());
        btnForgotPassword.addActionListener(e -> openForgotPasswordDialog());
        btnDbConfig.addActionListener(e -> openDbConfigDialog());
    }

    private void performLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User user = userDAO.authenticate(username, password);
            if (user != null) {
                dispose();
                SwingUtilities.invokeLater(() -> {
                    DashboardFrame dashboard = new DashboardFrame(user);
                    dashboard.setVisible(true);
                });
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password!", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Database connection error!\n\nDetails: " + ex.getMessage() +
                    "\n\nPlease ensure MySQL is running and configured correctly via Database Settings button.",
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
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

        JPanel panel = new JPanel(new GridLayout(5, 2, 8, 8));
        panel.add(new JLabel("MySQL Host:")); panel.add(hostField);
        panel.add(new JLabel("MySQL Port:")); panel.add(portField);
        panel.add(new JLabel("Database Name:")); panel.add(dbField);
        panel.add(new JLabel("Username:")); panel.add(userField);
        panel.add(new JLabel("Password:")); panel.add(passField);

        int res = JOptionPane.showConfirmDialog(this, panel, "MySQL Database Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            DBConnection.saveConfig(
                    hostField.getText().trim(),
                    portField.getText().trim(),
                    dbField.getText().trim(),
                    userField.getText().trim(),
                    new String(passField.getPassword())
            );
            JOptionPane.showMessageDialog(this, "Database settings saved successfully!", "Saved", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}

