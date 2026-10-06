package ui;

import config.DBConnection;
import dao.UserDAO;
import model.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;

/**
 * Modern Full-Screen Centered Login Screen with Dynamic Billing Video Background.
 */
public class LoginPanel extends JPanel {
    private final Frame parentFrame;

    // UI Input Components
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;
    private JButton btnDbConfig;
    private JButton btnForgotPassword;
    private JButton btnRoleAdmin;
    private JButton btnRoleStaff;
    private JCheckBox chkRemember;
    private JButton btnTogglePassword;
    private boolean showPassword = false;

    // Video Background Engine
    private BufferedImage[] videoFrames = null;
    private BufferedImage currentFrame = null;
    private int currentFrameIndex = 0;
    private Timer videoTimer = null;
    private boolean isVideoPlaying = true;
    private JButton btnVideoToggle = null;

    private final UserDAO userDAO = new UserDAO();

    public LoginPanel(Frame parent) {
        this.parentFrame = parent;
        setLayout(new BorderLayout());
        setBackground(new Color(11, 19, 43)); // Deep navy slate background

        initVideoEngine();
        initComponents();
    }

    // =========================================================================
    // 1. VIDEO BACKGROUND ENGINE
    // =========================================================================
    private void initVideoEngine() {
        // Step 1: Load initial frame or fallback hero image immediately for instantaneous 0ms display
        loadFallbackOrFirstFrame();

        // Step 2: Background thread preloads all frames and starts the smooth 24 fps playback loop
        new Thread(() -> {
            try {
                File mp4 = new File("data/assets/login_video.mp4");
                File dir = new File("data/assets/video_frames");
                File sentinel = new File("data/assets/video_frames/.extracted_time");

                // Auto-extract frames if a new MP4 video is provided and frames are missing or older
                if (mp4.exists() && (!dir.exists() || !sentinel.exists() || sentinel.lastModified() < mp4.lastModified())) {
                    dir.mkdirs();
                    try {
                        ProcessBuilder pb = new ProcessBuilder(
                                "ffmpeg", "-y", "-i", mp4.getAbsolutePath(),
                                "-vf", "scale=1280:720,fps=24", "-q:v", "3",
                                new File(dir, "frame_%03d.jpg").getAbsolutePath()
                        );
                        Process proc = pb.start();
                        proc.waitFor();
                        if (!sentinel.exists()) sentinel.createNewFile();
                        sentinel.setLastModified(System.currentTimeMillis());
                    } catch (Exception ignored) {}
                }

                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));
                    if (files != null && files.length > 0) {
                        Arrays.sort(files, Comparator.comparing(File::getName));
                        BufferedImage[] loaded = new BufferedImage[files.length];
                        for (int i = 0; i < files.length; i++) {
                            loaded[i] = ImageIO.read(files[i]);
                        }
                        synchronized (this) {
                            videoFrames = loaded;
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("Note: Video frame preloader notice: " + ex.getMessage());
            }

            // Start video playback timer on the Swing Event Dispatch Thread
            SwingUtilities.invokeLater(() -> {
                if (videoFrames != null && videoFrames.length > 1) {
                    videoTimer = new Timer(41, e -> { // ~24 frames per second
                        if (isVideoPlaying && videoFrames != null && videoFrames.length > 0) {
                            currentFrameIndex = (currentFrameIndex + 1) % videoFrames.length;
                            currentFrame = videoFrames[currentFrameIndex];
                            repaint();
                        }
                    });
                    videoTimer.start();
                }
            });
        }, "BillingVideoBackgroundThread").start();
    }

    private void loadFallbackOrFirstFrame() {
        try {
            File f0 = new File("data/assets/video_frames/frame_000.jpg");
            if (f0.exists()) {
                currentFrame = ImageIO.read(f0);
                return;
            }
            File hero = new File("data/assets/billing_hero.png");
            if (!hero.exists()) {
                hero = new File("data/assets/billing_hero.jpg");
            }
            if (hero.exists()) {
                currentFrame = ImageIO.read(hero);
            }
        } catch (Exception ignored) {}
    }

    public void stopVideo() {
        if (videoTimer != null && videoTimer.isRunning()) {
            videoTimer.stop();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        stopVideo();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 1. Draw Active Video Frame (Cover mode: scaled to fill entire panel proportionally)
        if (currentFrame != null) {
            int imgW = currentFrame.getWidth(null);
            int imgH = currentFrame.getHeight(null);
            if (imgW > 0 && imgH > 0) {
                double scale = Math.max((double) w / imgW, (double) h / imgH);
                int drawW = (int) Math.round(imgW * scale);
                int drawH = (int) Math.round(imgH * scale);
                int drawX = (w - drawW) / 2;
                int drawY = (h - drawH) / 2;
                g2.drawImage(currentFrame, drawX, drawY, drawW, drawH, null);
            }
        } else {
            // High-tech dark gradient fallback if no frame available
            GradientPaint gp = new GradientPaint(0, 0, new Color(11, 19, 43), 0, h, new Color(15, 23, 42));
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
        }

        // 2. Cinematic Dark Frosted Overlay (Allows the video to shine through while keeping text 100% legible)
        GradientPaint overlay = new GradientPaint(
                0, 0, new Color(11, 19, 43, 140),
                0, h, new Color(15, 23, 42, 185)
        );
        g2.setPaint(overlay);
        g2.fillRect(0, 0, w, h);

        // Subtle radial vignette in center to focus eye on the login card
        try {
            RadialGradientPaint vignette = new RadialGradientPaint(
                    new Point(w / 2, h / 2),
                    (float) Math.max(w, h) * 0.75f,
                    new float[]{0.0f, 0.6f, 1.0f},
                    new Color[]{
                            new Color(11, 19, 43, 40),
                            new Color(11, 19, 43, 120),
                            new Color(8, 12, 28, 220)
                    }
            );
            g2.setPaint(vignette);
            g2.fillRect(0, 0, w, h);
        } catch (Exception ignored) {}

        g2.dispose();
    }

    // =========================================================================
    // 2. UI LAYOUT & COMPONENTS
    // =========================================================================
    private void initComponents() {
        setOpaque(true);

        // Top Header Bar (Floating over video)
        JPanel topBar = createTopBar();
        add(topBar, BorderLayout.NORTH);

        // Center Area: Perfectly Centered Floating Login Card
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel loginCard = createCenterLoginCard();
        centerWrapper.add(loginCard); // Centered by GridBagLayout default

        add(centerWrapper, BorderLayout.CENTER);

        // Bottom Footer Bar (Trust & Security badge)
        JPanel bottomBar = createBottomBar();
        add(bottomBar, BorderLayout.SOUTH);

        // Bind Action Listeners
        btnLogin.addActionListener(e -> performLogin());
        txtPassword.addActionListener(e -> performLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());
        btnForgotPassword.addActionListener(e -> openForgotPasswordDialog());
        btnDbConfig.addActionListener(e -> openDbConfigDialog());
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(18, 32, 10, 32));

        // Left Brand Header
        JPanel brandLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandLeft.setOpaque(false);

        JLabel lblLogoIcon = new JLabel("⚡", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(14, 116, 144, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(56, 189, 248, 180));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblLogoIcon.setPreferredSize(new Dimension(38, 38));
        lblLogoIcon.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblLogoIcon.setForeground(new Color(56, 189, 248));

        JPanel brandTexts = new JPanel(new GridLayout(2, 1, 0, 1));
        brandTexts.setOpaque(false);
        JLabel lblTitle = new JLabel("SmartBilling Pro");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Enterprise Point of Sale & Smart Inventory Suite");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(new Color(148, 163, 184));
        brandTexts.add(lblTitle);
        brandTexts.add(lblSub);

        brandLeft.add(lblLogoIcon);
        brandLeft.add(brandTexts);

        // Right Video Controls & Edition Badge
        JPanel barRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        barRight.setOpaque(false);

        // Video Play/Pause Toggle Chip
        btnVideoToggle = new JButton("▶ Live Video Background") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 23, 42, 180));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(56, 189, 248, 100));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnVideoToggle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnVideoToggle.setForeground(new Color(56, 189, 248));
        btnVideoToggle.setContentAreaFilled(false);
        btnVideoToggle.setBorderPainted(false);
        btnVideoToggle.setFocusPainted(false);
        btnVideoToggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVideoToggle.setBorder(new EmptyBorder(6, 14, 6, 14));
        btnVideoToggle.addActionListener(e -> toggleVideoPlayback());

        JLabel lblBadge = new JLabel("● v2.4 ENTERPRISE");
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblBadge.setForeground(new Color(52, 211, 153));
        lblBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(16, 185, 129, 120), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));

        barRight.add(btnVideoToggle);
        barRight.add(lblBadge);

        bar.add(brandLeft, BorderLayout.WEST);
        bar.add(barRight, BorderLayout.EAST);
        return bar;
    }

    private void toggleVideoPlayback() {
        isVideoPlaying = !isVideoPlaying;
        if (btnVideoToggle != null) {
            btnVideoToggle.setText(isVideoPlaying ? "▶ Live Video Background" : "⏸ Video Paused");
            btnVideoToggle.setForeground(isVideoPlaying ? new Color(56, 189, 248) : new Color(248, 113, 113));
        }
    }

    private JPanel createBottomBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(4, 16, 14, 16));

        JLabel lblSec = new JLabel("Enterprise Grade Security  •  Offline-First Architecture  •  Instant MySQL Sync");
        lblSec.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSec.setForeground(new Color(148, 163, 184, 210));
        bar.add(lblSec);
        return bar;
    }

    // =========================================================================
    // 3. CENTER FLOATING LOGIN CARD
    // =========================================================================
    private JPanel createCenterLoginCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Subtle Outer Card Shadow
                g2.setColor(new Color(0, 0, 0, 70));
                g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 22, 22);

                // Crisp White Glassmorphism Card
                g2.setColor(new Color(255, 255, 255, 248)); // 97% opaque crisp white
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

                // Card Outline Border
                g2.setColor(new Color(226, 232, 240, 220));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 36, 24, 36));
        card.setPreferredSize(new Dimension(460, 580));
        card.setMaximumSize(new Dimension(470, 600));

        // Card Header: Welcome & Subtitle
        JLabel lblWelcome = new JLabel("Welcome Back 👋");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblWelcome.setForeground(new Color(15, 23, 42));
        lblWelcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel("Sign in to access your billing terminal & POS station");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(100, 116, 139));
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lblWelcome);
        card.add(Box.createVerticalStrut(4));
        card.add(lblSub);
        card.add(Box.createVerticalStrut(16));

        // Quick Role Preset Buttons (Admin / Staff)
        JPanel roleBar = new JPanel(new GridLayout(1, 2, 8, 0));
        roleBar.setOpaque(false);
        roleBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        roleBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnRoleAdmin = createRoleButton("Administrator", true);
        btnRoleStaff = createRoleButton("Cashier / Staff", false);

        btnRoleAdmin.addActionListener(e -> selectRolePreset("admin", "admin123", true));
        btnRoleStaff.addActionListener(e -> selectRolePreset("staff", "staff123", false));

        roleBar.add(btnRoleAdmin);
        roleBar.add(btnRoleStaff);

        card.add(roleBar);
        card.add(Box.createVerticalStrut(16));

        // Username Field
        JLabel lblUser = new JLabel("Username or Staff ID");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUser.setForeground(new Color(51, 65, 85));
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtUsername = new JTextField("admin");
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtUsername.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        txtUsername.setPreferredSize(new Dimension(380, 40));
        txtUsername.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(lblUser);
        card.add(Box.createVerticalStrut(6));
        card.add(txtUsername);
        card.add(Box.createVerticalStrut(14));

        // Password Field with Show/Hide Toggle
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPass.setForeground(new Color(51, 65, 85));
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel passRow = new JPanel(new BorderLayout(0, 0));
        passRow.setOpaque(false);
        passRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        passRow.setPreferredSize(new Dimension(380, 40));
        passRow.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true));
        passRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtPassword = new JPasswordField("admin123");
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtPassword.setBorder(new EmptyBorder(6, 12, 6, 12));

        btnTogglePassword = new JButton("Show");
        btnTogglePassword.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnTogglePassword.setForeground(new Color(71, 85, 105));
        btnTogglePassword.setFocusPainted(false);
        btnTogglePassword.setContentAreaFilled(false);
        btnTogglePassword.setBorder(new EmptyBorder(4, 10, 4, 12));
        btnTogglePassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTogglePassword.setToolTipText("Show / Hide password");
        btnTogglePassword.addActionListener(e -> {
            showPassword = !showPassword;
            txtPassword.setEchoChar(showPassword ? (char) 0 : '•');
            btnTogglePassword.setText(showPassword ? "Hide" : "Show");
        });

        passRow.add(txtPassword, BorderLayout.CENTER);
        passRow.add(btnTogglePassword, BorderLayout.EAST);

        card.add(lblPass);
        card.add(Box.createVerticalStrut(6));
        card.add(passRow);
        card.add(Box.createVerticalStrut(10));

        // Remember Me & Forgot Password Row
        JPanel optionsRow = new JPanel(new BorderLayout());
        optionsRow.setOpaque(false);
        optionsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        optionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        chkRemember = new JCheckBox("Remember this terminal", true);
        chkRemember.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkRemember.setForeground(new Color(100, 116, 139));
        chkRemember.setOpaque(false);
        chkRemember.setFocusPainted(false);

        btnForgotPassword = new JButton("Forgot Password?");
        btnForgotPassword.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnForgotPassword.setForeground(new Color(37, 99, 235));
        btnForgotPassword.setContentAreaFilled(false);
        btnForgotPassword.setBorderPainted(false);
        btnForgotPassword.setCursor(new Cursor(Cursor.HAND_CURSOR));

        optionsRow.add(chkRemember, BorderLayout.WEST);
        optionsRow.add(btnForgotPassword, BorderLayout.EAST);

        card.add(optionsRow);
        card.add(Box.createVerticalStrut(14));

        // Primary Action Button (Sign In)
        btnLogin = new JButton("SIGN IN TO DASHBOARD") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1 = getModel().isPressed() ? new Color(29, 78, 216)
                        : (getModel().isRollover() ? new Color(30, 64, 175) : new Color(37, 99, 235));
                g2.setColor(c1);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setContentAreaFilled(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btnLogin.setPreferredSize(new Dimension(380, 44));
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(btnLogin);
        card.add(Box.createVerticalStrut(10));

        // Quick Offline / Demo Login Button
        JButton btnDemoLogin = new JButton("⚡ Quick Offline / Demo Login (No MySQL wait)");
        btnDemoLogin.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDemoLogin.setBackground(new Color(241, 245, 249));
        btnDemoLogin.setForeground(new Color(30, 41, 59));
        btnDemoLogin.setFocusPainted(false);
        btnDemoLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnDemoLogin.setPreferredSize(new Dimension(380, 36));
        btnDemoLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDemoLogin.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));
        btnDemoLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDemoLogin.addActionListener(e -> launchDemoAdmin());

        card.add(btnDemoLogin);
        card.add(Box.createVerticalStrut(10));

        // Customer Self-Checkout Express Button
        JButton btnSelfCheckoutMode = new JButton("Open Customer Self-Checkout Express") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = getModel().isPressed() ? new Color(4, 120, 87)
                        : (getModel().isRollover() ? new Color(5, 150, 105) : new Color(16, 185, 129));
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnSelfCheckoutMode.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSelfCheckoutMode.setForeground(Color.WHITE);
        btnSelfCheckoutMode.setFocusPainted(false);
        btnSelfCheckoutMode.setContentAreaFilled(false);
        btnSelfCheckoutMode.setBorderPainted(false);
        btnSelfCheckoutMode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnSelfCheckoutMode.setPreferredSize(new Dimension(380, 40));
        btnSelfCheckoutMode.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSelfCheckoutMode.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSelfCheckoutMode.addActionListener(e -> {
            stopVideo();
            new SelfCheckoutFrame(null).setVisible(true);
        });

        card.add(btnSelfCheckoutMode);
        card.add(Box.createVerticalStrut(14));

        // Bottom Utilities: Database Configuration
        JPanel bottomUtils = new JPanel(new BorderLayout());
        bottomUtils.setOpaque(false);
        bottomUtils.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        bottomUtils.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnDbConfig = new JButton("⚙ MySQL Database Settings");
        btnDbConfig.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnDbConfig.setForeground(new Color(100, 116, 139));
        btnDbConfig.setContentAreaFilled(false);
        btnDbConfig.setBorderPainted(false);
        btnDbConfig.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblVer = new JLabel("v2.4 Enterprise");
        lblVer.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblVer.setForeground(new Color(148, 163, 184));

        bottomUtils.add(btnDbConfig, BorderLayout.WEST);
        bottomUtils.add(lblVer, BorderLayout.EAST);

        card.add(bottomUtils);
        return card;
    }

    private JButton createRoleButton(String title, boolean active) {
        JButton btn = new JButton(title);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        updateRoleButtonStyle(btn, active);
        return btn;
    }

    private void updateRoleButtonStyle(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(new Color(239, 246, 255));
            btn.setForeground(new Color(29, 78, 216));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(147, 197, 253), 1, true),
                    new EmptyBorder(6, 12, 6, 12)
            ));
        } else {
            btn.setBackground(new Color(248, 250, 252));
            btn.setForeground(new Color(100, 116, 139));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                    new EmptyBorder(6, 12, 6, 12)
            ));
        }
    }

    private void selectRolePreset(String user, String pass, boolean isAdmin) {
        txtUsername.setText(user);
        txtPassword.setText(pass);
        updateRoleButtonStyle(btnRoleAdmin, isAdmin);
        updateRoleButtonStyle(btnRoleStaff, !isAdmin);
    }

    // =========================================================================
    // 4. AUTHENTICATION & BUSINESS LOGIC
    // =========================================================================
    private void launchDemoAdmin() {
        User demoAdmin = new User(1, "admin", "admin123", "System Administrator (Demo/Offline)", "ADMIN");
        launchDashboard(demoAdmin);
    }

    private void launchDashboard(User user) {
        stopVideo();
        try {
            DashboardFrame dashboard = new DashboardFrame(user);
            dashboard.setVisible(true);
            if (parentFrame != null) {
                parentFrame.dispose();
            } else {
                Window w = SwingUtilities.getWindowAncestor(this);
                if (w != null) w.dispose();
            }
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
            JOptionPane.showMessageDialog(this, "Please enter both username and password.", "Input Required", JOptionPane.WARNING_MESSAGE);
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

            if (isDefaultAdmin || isDefaultStaff) {
                String notice = "⚠️ MySQL DATABASE IS NOT RUNNING (Connection Refused on port 3306)!\n\n"
                        + "👉 MySQL service start karne ke liye terminal me ye command chalayein:\n"
                        + "   sudo systemctl start mysql\n"
                        + "   (ya: sudo service mysql start)\n\n"
                        + "Kya aap abhi ke liye OFFLINE / DEMO MODE me Login karna chahte hain?";

                int choice = JOptionPane.showConfirmDialog(this, notice,
                        "MySQL Offline - Demo Mode Available", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if (choice == JOptionPane.YES_OPTION) {
                    if (isDefaultAdmin) {
                        launchDashboard(new User(1, "admin", "admin123", "System Administrator (Demo/Offline)", "ADMIN"));
                    } else {
                        launchDashboard(new User(2, "staff", "staff123", "Cashier Desk (Demo/Offline)", "STAFF"));
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
            JOptionPane.showMessageDialog(this, "Invalid username or password!", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        launchDashboard(user);
    }

    private void openForgotPasswordDialog() {
        Window ancestor = SwingUtilities.getWindowAncestor(this);
        Frame f = (ancestor instanceof Frame) ? (Frame) ancestor : parentFrame;
        ForgotPasswordDialog dialog = new ForgotPasswordDialog(f);
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
        panel.add(new JLabel("MySQL Host:")); panel.add(hostField);
        panel.add(new JLabel("MySQL Port:")); panel.add(portField);
        panel.add(new JLabel("Database Name:")); panel.add(dbField);
        panel.add(new JLabel("Username:")); panel.add(userField);
        panel.add(new JLabel("Password:")); panel.add(passField);

        JButton btnTestConn = new JButton("🔍 Test Connection");
        panel.add(btnTestConn);
        panel.add(new JLabel("(Click to test)"));

        btnTestConn.addActionListener(e -> {
            String testResult = DBConnection.testConnection(
                    hostField.getText().trim(),
                    portField.getText().trim(),
                    dbField.getText().trim(),
                    userField.getText().trim(),
                    new String(passField.getPassword())
            );
            JOptionPane.showMessageDialog(this, testResult, "Database Test Result",
                    testResult.startsWith("SUCCESS") || testResult.startsWith("CONNECTED")
                            ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        });

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
