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
    private JButton btnAdminRole;
    private JButton btnStaffRole;
    private JPanel leftPanel;
    private CardLayout leftPanelLayout;
    private JPanel forgotPasswordCard;
    private JPanel wallpaperPanel;
    private Timer wallpaperAnimation;
    private JButton btnWallpaperToggle;
    private int wallpaperFrame;
    private UserDAO userDAO = new UserDAO();

    public LoginFrame() {
        setTitle("Login - Billing & Inventory Management System");
        setSize(1100, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);
        initComponents();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        wallpaperAnimation = new Timer(70, e -> {
            wallpaperFrame = (wallpaperFrame + 1) % 1000;
            wallpaperPanel.repaint();
        });
        wallpaperAnimation.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                wallpaperAnimation.stop();
            }
        });
    }

    private void toggleWallpaperAnimation() {
        if (wallpaperAnimation.isRunning()) {
            wallpaperAnimation.stop();
            btnWallpaperToggle.setText("▶  Resume Live Wallpaper");
            btnWallpaperToggle.setToolTipText("Resume the animated billing and inventory wallpaper");
        } else {
            wallpaperAnimation.start();
            btnWallpaperToggle.setText("❚❚  Pause Live Wallpaper");
            btnWallpaperToggle.setToolTipText("Pause the animated billing and inventory wallpaper");
        }
    }

    private void initComponents() {
        wallpaperPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float colorShift = (float) ((Math.sin(wallpaperFrame / 110.0) + 1.0) / 2.0);
                Color topColor = new Color(8, (int) (13 + colorShift * 13), (int) (34 + colorShift * 27));
                Color bottomColor = new Color((int) (24 + colorShift * 17), 13, (int) (58 + colorShift * 38));
                GradientPaint bg = new GradientPaint(0, 0, topColor, getWidth(), getHeight(), bottomColor);
                g2.setPaint(bg);
                g2.fillRect(0, 0, getWidth(), getHeight());

                int width = getWidth();
                int height = getHeight();
                int glowSize = Math.max(240, Math.min(width, height) * 2 / 3);
                int glowX = width / 2 + (int) (width / 3.0 * Math.sin(wallpaperFrame / 45.0));
                int glowY = height / 3 + (int) (height / 5.0 * Math.cos(wallpaperFrame / 55.0));
                g2.setColor(new Color(34, 211, 238, 38));
                g2.fillOval(-width / 10, -height / 5, width / 3, height / 2);
                g2.setColor(new Color(14, 165, 233, 56));
                g2.fillOval(glowX - glowSize / 2, glowY - glowSize / 2, glowSize, glowSize);
                g2.setColor(new Color(168, 85, 247, 52));
                g2.fillOval(width * 3 / 4, height / 10, width / 3, height / 2);
                g2.setColor(new Color(20, 184, 166, 50));
                g2.fillOval(width / 3, height * 3 / 5, width / 3, height / 2);

                g2.setColor(new Color(125, 211, 252, 45));
                int spacing = 42;
                for (int x = spacing; x < width; x += spacing) {
                    for (int y = spacing; y < height; y += spacing) {
                        g2.fillOval(x, y, 2, 2);
                    }
                }

                int safeWidth = Math.max(width, 1);
                int travel = wallpaperFrame % safeWidth;
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(new Color(34, 211, 238, 150));
                g2.drawLine(travel, height / 3, Math.floorMod(travel + 150, safeWidth), height / 3 - 36);
                g2.setColor(new Color(192, 132, 252, 135));
                g2.drawLine(Math.floorMod(travel + width / 2, safeWidth), height * 4 / 5,
                        Math.floorMod(travel + width / 2 + 180, safeWidth), height * 4 / 5 - 48);

                int receiptX = Math.floorMod(width - wallpaperFrame * 2, safeWidth + 100);
                int receiptY = height / 5 + (int) (18 * Math.sin(wallpaperFrame / 18.0));
                g2.setColor(new Color(15, 23, 42, 145));
                g2.fillRoundRect(receiptX + 3, receiptY + 4, 46, 58, 8, 8);
                g2.setColor(new Color(186, 230, 253, 210));
                g2.fillRoundRect(receiptX, receiptY, 46, 58, 8, 8);
                g2.setColor(new Color(14, 165, 233, 205));
                g2.fillRoundRect(receiptX + 8, receiptY + 10, 30, 4, 3, 3);
                g2.setColor(new Color(71, 85, 105, 150));
                g2.fillRoundRect(receiptX + 8, receiptY + 22, 26, 3, 2, 2);
                g2.fillRoundRect(receiptX + 8, receiptY + 31, 20, 3, 2, 2);
                g2.setColor(new Color(13, 148, 136, 200));
                g2.fillRoundRect(receiptX + 8, receiptY + 42, 24, 5, 2, 2);

                int stockX = Math.floorMod(wallpaperFrame * 2 + width / 3, safeWidth + 90) - 45;
                int stockY = height * 3 / 4 + (int) (14 * Math.sin(wallpaperFrame / 22.0));
                g2.setColor(new Color(245, 158, 11, 185));
                g2.fillRoundRect(stockX, stockY, 34, 34, 8, 8);
                g2.setColor(new Color(255, 255, 255, 85));
                g2.drawLine(stockX + 8, stockY + 12, stockX + 26, stockY + 12);
                g2.drawLine(stockX + 8, stockY + 19, stockX + 26, stockY + 19);
                g2.drawLine(stockX + 8, stockY + 26, stockX + 21, stockY + 26);

                int cartX = Math.floorMod(wallpaperFrame * 3 + width / 5, safeWidth + 120) - 60;
                int cartY = height / 2 + (int) (20 * Math.sin(wallpaperFrame / 24.0));
                g2.setColor(new Color(236, 72, 153, 170));
                g2.fillRoundRect(cartX, cartY, 48, 32, 10, 10);
                g2.setColor(new Color(255, 255, 255, 185));
                g2.drawLine(cartX + 10, cartY + 9, cartX + 37, cartY + 9);
                g2.drawLine(cartX + 10, cartY + 16, cartX + 31, cartY + 16);
                g2.fillOval(cartX + 10, cartY + 35, 7, 7);
                g2.fillOval(cartX + 32, cartY + 35, 7, 7);

                g2.dispose();
            }
        };
        wallpaperPanel.setLayout(new BorderLayout());
        wallpaperPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel wallpaperControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        wallpaperControls.setOpaque(false);
        btnWallpaperToggle = new JButton("❚❚  Pause Live Wallpaper");
        btnWallpaperToggle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnWallpaperToggle.setForeground(new Color(224, 242, 254));
        btnWallpaperToggle.setBackground(new Color(15, 23, 42));
        btnWallpaperToggle.setFocusPainted(false);
        btnWallpaperToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnWallpaperToggle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(34, 211, 238), 1),
                new EmptyBorder(9, 14, 9, 14)));
        btnWallpaperToggle.setToolTipText("Pause the animated billing and inventory wallpaper");
        btnWallpaperToggle.addActionListener(e -> toggleWallpaperAnimation());
        wallpaperControls.add(btnWallpaperToggle);
        wallpaperPanel.add(wallpaperControls, BorderLayout.NORTH);

        JPanel shell = new JPanel(new BorderLayout());
        shell.setOpaque(false);
        wallpaperPanel.add(shell, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        shell.add(contentPanel, BorderLayout.CENTER);

        leftPanelLayout = new CardLayout();
        leftPanel = new JPanel(leftPanelLayout);
        leftPanel.setOpaque(false);
        leftPanel.setPreferredSize(new Dimension(380, 430));

        JPanel brandPanel = createBrandPanel();
        forgotPasswordCard = createForgotPasswordCard();
        leftPanel.add(brandPanel, "brand");
        JScrollPane forgotPasswordScroll = new JScrollPane(forgotPasswordCard);
        forgotPasswordScroll.setBorder(null);
        forgotPasswordScroll.setOpaque(false);
        forgotPasswordScroll.getViewport().setOpaque(false);
        forgotPasswordScroll.getVerticalScrollBar().setUnitIncrement(16);
        leftPanel.add(forgotPasswordScroll, "forgot");
        GridBagConstraints leftConstraints = new GridBagConstraints();
        leftConstraints.gridx = 0;
        leftConstraints.gridy = 0;
        leftConstraints.weightx = 0;
        leftConstraints.weighty = 0;
        leftConstraints.fill = GridBagConstraints.VERTICAL;
        leftConstraints.insets = new Insets(0, 0, 0, 20);
        contentPanel.add(leftPanel, leftConstraints);

        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.setOpaque(false);
        formWrap.setBorder(new EmptyBorder(6, 4, 6, 4));
        formWrap.add(createLoginPanel(), BorderLayout.CENTER);
        formWrap.setPreferredSize(new Dimension(380, 430));
        GridBagConstraints formConstraints = new GridBagConstraints();
        formConstraints.gridx = 1;
        formConstraints.gridy = 0;
        formConstraints.weightx = 0;
        formConstraints.weighty = 0;
        formConstraints.fill = GridBagConstraints.VERTICAL;
        contentPanel.add(formWrap, formConstraints);

        add(wallpaperPanel);
        applyRoleSelection("ADMIN");

        btnLogin.addActionListener(e -> performLogin());
        btnForgotPassword.addActionListener(e -> showForgotPasswordPanel());
        btnDbConfig.addActionListener(e -> openDbConfigDialog());
    }

    private JPanel createBrandPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(0, 0, new Color(37, 99, 235), 0, getHeight(),
                        new Color(13, 148, 136));
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 36, 36);

                g2.setColor(new Color(251, 191, 36, 55));
                g2.fillOval(getWidth() - 190, 25, 180, 180);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillOval(-30, 220, 220, 220);
                g2.setColor(new Color(34, 211, 238, 45));
                g2.fillOval(getWidth() - 250, 380, 260, 260);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(20, 20, 20, 20),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel badge = new JLabel("Smart Billing Suite");
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);
        badge.setForeground(new Color(191, 219, 254));
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badge.setOpaque(true);
        badge.setBackground(new Color(255, 255, 255, 38));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(191, 219, 254, 110), 1),
                new EmptyBorder(7, 12, 7, 12)));

        JLabel title = new JLabel("SmartBilling");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 36));

        JLabel subtitle = new JLabel("Fast billing • Smart inventory • Better service");
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setForeground(new Color(191, 219, 254));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel artPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                double scale = Math.min(getWidth() / 400.0, getHeight() / 250.0);
                g2.translate((getWidth() - 400 * scale) / 2.0, (getHeight() - 250 * scale) / 2.0);
                g2.scale(scale, scale);
                g2.setColor(new Color(4, 12, 32, 95));
                g2.fillRoundRect(24, 24, 352, 206, 24, 24);

                g2.setColor(new Color(241, 245, 249));
                g2.fillRoundRect(30, 18, 340, 204, 18, 18);
                g2.setColor(new Color(226, 232, 240));
                g2.fillRoundRect(30, 18, 62, 204, 18, 18);
                g2.fillRect(74, 18, 18, 204);

                g2.setColor(new Color(37, 99, 235));
                g2.fillRoundRect(42, 33, 28, 28, 9, 9);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(49, 48, 55, 42);
                g2.drawLine(55, 42, 63, 51);
                g2.setColor(new Color(148, 163, 184));
                for (int i = 0; i < 4; i++) {
                    int y = 82 + i * 29;
                    g2.fillRoundRect(46, y, 20, 5, 3, 3);
                    g2.fillRoundRect(51, y + 9, 14, 4, 2, 2);
                }

                g2.setColor(new Color(15, 23, 42));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g2.drawString("Sales overview", 108, 43);
                g2.setColor(new Color(100, 116, 139));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                g2.drawString("Your store at a glance", 108, 56);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(106, 68, 118, 53, 10, 10);
                g2.fillRoundRect(232, 68, 121, 53, 10, 10);
                g2.setColor(new Color(100, 116, 139));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                g2.drawString("TODAY'S SALES", 116, 84);
                g2.drawString("ORDERS", 242, 84);
                g2.setColor(new Color(15, 23, 42));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g2.drawString("₹24,850", 116, 105);
                g2.drawString("128", 242, 105);
                g2.setColor(new Color(16, 185, 129));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                g2.drawString("↑ 12.8%", 184, 105);
                g2.drawString("↑ 8.2%", 310, 105);

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(106, 129, 247, 77, 10, 10);
                g2.setColor(new Color(71, 85, 105));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2.drawString("Weekly revenue", 117, 145);
                g2.setColor(new Color(226, 232, 240));
                g2.drawLine(117, 187, 341, 187);
                g2.setColor(new Color(191, 219, 254));
                int[] barHeights = { 19, 29, 24, 39, 31, 48, 42 };
                for (int i = 0; i < barHeights.length; i++) {
                    int x = 128 + i * 29;
                    int barHeight = barHeights[i];
                    g2.fillRoundRect(x, 182 - barHeight, 14, barHeight, 6, 6);
                }
                g2.setColor(new Color(37, 99, 235));
                int highlightHeight = 54 + (int) (4 * Math.sin(wallpaperFrame / 12.0));
                g2.fillRoundRect(302, 182 - highlightHeight, 14, highlightHeight, 6, 6);

                g2.setColor(new Color(255, 255, 255, 238));
                g2.fillRoundRect(300, 4, 82, 48, 12, 12);
                g2.setColor(new Color(16, 185, 129));
                g2.fillRoundRect(309, 13, 20, 20, 6, 6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString("₹", 315, 27);
                g2.setColor(new Color(15, 23, 42));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
                g2.drawString("Payment", 334, 22);
                g2.setColor(new Color(100, 116, 139));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 7));
                g2.drawString("Successful", 334, 33);
                g2.dispose();
            }
        };
        artPanel.setOpaque(false);
        artPanel.setPreferredSize(new Dimension(340, 190));

        JPanel bottomInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bottomInfo.setOpaque(false);
        String[] features = { "FAST CHECKOUT", "LIVE INVENTORY", "SALES INSIGHTS" };
        for (String feature : features) {
            JLabel featureLabel = new JLabel(feature);
            featureLabel.setForeground(new Color(224, 242, 254));
            featureLabel.setFont(new Font("Segoe UI", Font.BOLD, 9));
            featureLabel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(191, 219, 254, 90), 1),
                    new EmptyBorder(5, 8, 5, 8)));
            bottomInfo.add(featureLabel);
        }

        content.add(badge);
        content.add(Box.createVerticalStrut(12));
        content.add(title);
        content.add(Box.createVerticalStrut(6));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(6));
        content.add(artPanel);
        content.add(Box.createVerticalStrut(6));
        content.add(bottomInfo);

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel formCard = new JPanel();
        formCard.setBackground(new Color(239, 246, 255));
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(148, 163, 184, 120), 1),
                new EmptyBorder(16, 20, 16, 20)));
        formCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(3, 8, 3, 8);

        JLabel lblWelcome = new JLabel("Welcome back");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblWelcome.setForeground(new Color(15, 23, 42));
        gbc.gridy = 0;
        formCard.add(lblWelcome, gbc);

        JLabel lblSub = new JLabel("Sign in to continue your smart billing workflow");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(71, 85, 105));
        gbc.gridy = 1;
        formCard.add(lblSub, gbc);

        JPanel roleSwitch = new JPanel(new GridLayout(1, 2, 8, 8));
        roleSwitch.setOpaque(false);
        btnAdminRole = new JButton("Admin");
        btnAdminRole.setBackground(new Color(37, 99, 235));
        btnAdminRole.setForeground(Color.WHITE);
        btnAdminRole.setFocusPainted(false);
        btnAdminRole.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAdminRole.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdminRole.addActionListener(e -> applyRoleSelection("ADMIN"));

        btnStaffRole = new JButton("Staff");
        btnStaffRole.setBackground(new Color(241, 245, 249));
        btnStaffRole.setForeground(new Color(15, 23, 42));
        btnStaffRole.setFocusPainted(false);
        btnStaffRole.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnStaffRole.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnStaffRole.addActionListener(e -> applyRoleSelection("STAFF"));

        roleSwitch.add(btnAdminRole);
        roleSwitch.add(btnStaffRole);
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        formCard.add(roleSwitch, gbc);

        JLabel lblUsername = new JLabel("Username");
        lblUsername.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUsername.setForeground(new Color(51, 65, 85));
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        formCard.add(lblUsername, gbc);

        txtUsername = new JTextField("admin");
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtUsername.setPreferredSize(new Dimension(0, 36));
        txtUsername.setMargin(new Insets(0, 10, 0, 10));
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        formCard.add(txtUsername, gbc);

        JLabel lblPassword = new JLabel("Password");
        lblPassword.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPassword.setForeground(new Color(51, 65, 85));
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        formCard.add(lblPassword, gbc);

        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setPreferredSize(new Dimension(0, 36));
        txtPassword.setMargin(new Insets(0, 10, 0, 10));

        char passwordEchoChar = txtPassword.getEchoChar();
        JButton btnTogglePasswordVisibility = new JButton(new PasswordVisibilityIcon(false));
        btnTogglePasswordVisibility.setPreferredSize(new Dimension(38, 36));
        btnTogglePasswordVisibility.setToolTipText("Show password");
        btnTogglePasswordVisibility.setFocusPainted(false);
        btnTogglePasswordVisibility.setContentAreaFilled(false);
        btnTogglePasswordVisibility.setBorderPainted(false);
        btnTogglePasswordVisibility.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTogglePasswordVisibility.addActionListener(e -> {
            boolean showPassword = txtPassword.getEchoChar() != 0;
            txtPassword.setEchoChar(showPassword ? (char) 0 : passwordEchoChar);
            btnTogglePasswordVisibility.setIcon(new PasswordVisibilityIcon(showPassword));
            btnTogglePasswordVisibility.setToolTipText(showPassword ? "Hide password" : "Show password");
        });

        JPanel passwordFieldPanel = new JPanel(new BorderLayout());
        passwordFieldPanel.setOpaque(false);
        passwordFieldPanel.add(txtPassword, BorderLayout.CENTER);
        passwordFieldPanel.add(btnTogglePasswordVisibility, BorderLayout.EAST);
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        formCard.add(passwordFieldPanel, gbc);

        JPanel lowerRow = new JPanel(new BorderLayout(10, 0));
        lowerRow.setOpaque(false);
        JLabel lblMode = new JLabel("Secure access");
        lblMode.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblMode.setForeground(new Color(100, 116, 139));
        btnForgotPassword = new JButton("Forgot password?");
        btnForgotPassword.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnForgotPassword.setForeground(new Color(37, 99, 235));
        btnForgotPassword.setContentAreaFilled(false);
        btnForgotPassword.setFocusPainted(false);
        btnForgotPassword.setBorderPainted(false);
        btnForgotPassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lowerRow.add(lblMode, BorderLayout.WEST);
        lowerRow.add(btnForgotPassword, BorderLayout.EAST);
        gbc.gridy = 7;
        formCard.add(lowerRow, gbc);

        btnLogin = new JButton("Login");
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setBackground(new Color(37, 99, 235));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setPreferredSize(new Dimension(0, 40));
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        gbc.gridy = 8;
        gbc.insets = new Insets(7, 8, 5, 8);
        formCard.add(btnLogin, gbc);

        btnDbConfig = new JButton("Database Settings");
        btnDbConfig.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnDbConfig.setForeground(new Color(71, 85, 105));
        btnDbConfig.setContentAreaFilled(false);
        btnDbConfig.setBorderPainted(false);
        btnDbConfig.setCursor(new Cursor(Cursor.HAND_CURSOR));
        gbc.gridy = 9;
        formCard.add(btnDbConfig, gbc);

        JLabel footerNote = new JLabel("Admin: admin/admin123   |   Staff: staff/staff123", SwingConstants.CENTER);
        footerNote.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        footerNote.setForeground(new Color(100, 116, 139));
        gbc.gridy = 10;
        formCard.add(footerNote, gbc);

        JButton selfCheckout = new JButton("Customer Self-Checkout");
        selfCheckout.setBackground(new Color(16, 185, 129));
        selfCheckout.setForeground(Color.WHITE);
        selfCheckout.setFocusPainted(false);
        selfCheckout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        selfCheckout.addActionListener(e -> new SelfCheckoutFrame(null).setVisible(true));
        gbc.gridy = 11;
        gbc.insets = new Insets(5, 8, 0, 8);
        formCard.add(selfCheckout, gbc);

        panel.add(formCard, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(380, 430));
        return panel;
    }

    private JPanel createForgotPasswordCard() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel card = new JPanel();
        card.setBackground(new Color(255, 255, 255, 230));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(148, 163, 184, 120), 1),
                new EmptyBorder(24, 22, 24, 22)));
        card.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        JLabel title = new JLabel("Reset your password");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(15, 23, 42));
        gbc.gridy = 0;
        card.add(title, gbc);

        JLabel subtitle = new JLabel("Recover access to your billing account securely.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(71, 85, 105));
        gbc.gridy = 1;
        card.add(subtitle, gbc);

        JLabel labelUser = new JLabel("Username");
        labelUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        labelUser.setForeground(new Color(51, 65, 85));
        gbc.gridy = 2;
        card.add(labelUser, gbc);

        JTextField resetUser = new JTextField();
        resetUser.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        resetUser.setPreferredSize(new Dimension(0, 42));
        gbc.gridy = 3;
        card.add(resetUser, gbc);

        JLabel labelEmail = new JLabel("Security answer");
        labelEmail.setFont(new Font("Segoe UI", Font.BOLD, 12));
        labelEmail.setForeground(new Color(51, 65, 85));
        gbc.gridy = 4;
        card.add(labelEmail, gbc);

        JTextField resetAnswer = new JTextField();
        resetAnswer.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        resetAnswer.setPreferredSize(new Dimension(0, 42));
        gbc.gridy = 5;
        card.add(resetAnswer, gbc);

        JLabel labelNew = new JLabel("New password");
        labelNew.setFont(new Font("Segoe UI", Font.BOLD, 12));
        labelNew.setForeground(new Color(51, 65, 85));
        gbc.gridy = 6;
        card.add(labelNew, gbc);

        JPasswordField resetNew = new JPasswordField();
        resetNew.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        resetNew.setPreferredSize(new Dimension(0, 42));
        gbc.gridy = 7;
        card.add(resetNew, gbc);

        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonRow.setOpaque(false);

        JButton backButton = new JButton("Back to login");
        backButton.setBackground(new Color(241, 245, 249));
        backButton.setForeground(new Color(15, 23, 42));
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> leftPanelLayout.show(leftPanel, "brand"));

        JButton resetButton = new JButton("Reset password");
        resetButton.setBackground(new Color(16, 185, 129));
        resetButton.setForeground(Color.WHITE);
        resetButton.setFocusPainted(false);
        resetButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resetButton.addActionListener(e -> {
            String username = resetUser.getText().trim();
            String answer = resetAnswer.getText().trim();
            String newPass = new String(resetNew.getPassword()).trim();
            if (username.isEmpty() || answer.isEmpty() || newPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter username, security answer and new password.",
                        "Reset Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this,
                    "Password reset request sent for " + username
                            + ". Please confirm the account recovery with your admin.",
                    "Password Reset", JOptionPane.INFORMATION_MESSAGE);
        });

        buttonRow.add(backButton);
        buttonRow.add(resetButton);
        gbc.gridy = 8;
        card.add(buttonRow, gbc);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    private static class PasswordVisibilityIcon implements Icon {
        private final boolean visible;

        private PasswordVisibilityIcon(boolean visible) {
            this.visible = visible;
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(71, 85, 105));
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawOval(x + 1, y + 4, 16, 10);
            g2.fillOval(x + 7, y + 7, 4, 4);
            if (!visible) {
                g2.drawLine(x + 3, y + 15, x + 15, y + 3);
            }
            g2.dispose();
        }
    }

    private void showForgotPasswordPanel() {
        leftPanelLayout.show(leftPanel, "forgot");
    }

    private void applyRoleSelection(String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            btnAdminRole.setBackground(new Color(37, 99, 235));
            btnAdminRole.setForeground(Color.WHITE);
            btnStaffRole.setBackground(new Color(241, 245, 249));
            btnStaffRole.setForeground(new Color(15, 23, 42));
            txtUsername.setText("admin");
        } else {
            btnStaffRole.setBackground(new Color(16, 185, 129));
            btnStaffRole.setForeground(Color.WHITE);
            btnAdminRole.setBackground(new Color(241, 245, 249));
            btnAdminRole.setForeground(new Color(15, 23, 42));
            txtUsername.setText("staff");
        }
        txtPassword.setText("");
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
        String username = txtUsername.getText().trim();
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
