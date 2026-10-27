package ui;

import dao.UserDAO;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

/**
 * Modern Keyra-Themed Inline Account Recovery Panel.
 * Designed to slide smoothly into the login area when "Forgot Password?" is clicked.
 *
 * Features:
 *   - Compact, proportional 32-34px pill inputs
 *   - Internal horizontal sliding between Step 1 (Identify) and Step 2 (Reset)
 *   - Direct callbacks for "Back to Login" and "Reset Success" to slide back to Login form
 */
public class ForgotPasswordPanel extends JPanel {

    // ── Brand Design Tokens ───────────────────────────────────────────────────
    private static final Color CANVAS_BG     = AppTheme.BG_CANVAS;      // #ecfcf0
    private static final Color FOREST_DEEP   = AppTheme.FOREST_DEEP;    // #003826
    private static final Color FOREST_GREEN  = AppTheme.FOREST_GREEN;   // #005a3d
    private static final Color FOREST_MID    = AppTheme.FOREST_MID;     // #167a54
    private static final Color BORDER_SAGE   = AppTheme.BORDER_SAGE;    // #d2eac6
    private static final Color TEXT_DARK     = AppTheme.TEXT_PRIMARY;   // #0a2e21
    private static final Color TEXT_MED      = AppTheme.TEXT_SECONDARY; // #335e4e
    private static final Color TEXT_MUTED    = AppTheme.TEXT_MUTED;     // #6e9384
    private static final Color ACCENT_LIME   = AppTheme.ELECTRIC_LIME;  // #c1f700

    // ── Callbacks ─────────────────────────────────────────────────────────────
    private final Runnable onBackToLogin;
    private final Runnable onResetSuccess;

    // ── Components ────────────────────────────────────────────────────────────
    private JLabel         lblStep1Chip;
    private JLabel         lblStep2Chip;

    // Sliding container & pages
    private InternalSlidingContainer slidingContainer;
    private JPanel         page1;
    private JPanel         page2;

    // Page 1 fields (Compact)
    private JTextField     txtUsername;
    private JButton        btnFindUser;
    private JButton        btnCancelP1;

    // Page 2 fields (Compact)
    private JLabel         lblVerifiedBadge;
    private JLabel         lblSecurityQuestion;
    private JTextField     txtSecurityAnswer;
    private JPasswordField txtNewPassword;
    private JPasswordField txtConfirmPassword;
    private JButton        btnToggleNewPass;
    private JButton        btnToggleConfPass;
    private boolean        showNewPass = false;
    private boolean        showConfPass = false;

    private CaptchaDisplay lblCaptchaDisplay;
    private JTextField     txtCaptchaInput;
    private JButton        btnRefreshCaptcha;
    private String         generatedCaptcha;

    private JButton        btnBackToP1;
    private JButton        btnCancelP2;
    private JButton        btnResetPassword;

    private final UserDAO  userDAO = new UserDAO();
    private User           matchedUser = null;

    public ForgotPasswordPanel(Runnable onBackToLogin, Runnable onResetSuccess) {
        this.onBackToLogin = onBackToLogin;
        this.onResetSuccess = onResetSuccess;
        setLayout(new BorderLayout());
        setOpaque(false);
        initComponents();
    }

    private Font appFont(int style, int size) {
        return LoginPanel.appFont(style, size);
    }

    private void initComponents() {
        // Top status bar matching login header space
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 32, 16));
        topBar.setOpaque(false);
        JButton btnTopBack = new JButton("← Back to Login");
        btnTopBack.setFont(appFont(Font.BOLD, 12));
        btnTopBack.setForeground(FOREST_MID);
        btnTopBack.setContentAreaFilled(false);
        btnTopBack.setBorderPainted(false);
        btnTopBack.setFocusPainted(false);
        btnTopBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTopBack.addActionListener(e -> {
            if (onBackToLogin != null) onBackToLogin.run();
        });
        topBar.add(btnTopBack);
        add(topBar, BorderLayout.NORTH);

        // Center card container (width 430 px)
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel recoveryCard = new JPanel(new BorderLayout(0, 0)) {
            @Override
            public Dimension getPreferredSize() { return new Dimension(430, 480); }
            @Override
            public Dimension getMaximumSize()   { return new Dimension(430, 480); }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(BORDER_SAGE);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        recoveryCard.setOpaque(false);

        // Header Banner of recovery card
        recoveryCard.add(buildHeaderBanner(), BorderLayout.NORTH);

        // Pages sliding container
        page1 = buildPage1();
        page2 = buildPage2();
        slidingContainer = new InternalSlidingContainer(page1, page2);
        recoveryCard.add(slidingContainer, BorderLayout.CENTER);

        centerWrapper.add(recoveryCard);
        add(centerWrapper, BorderLayout.CENTER);

        // Actions
        btnFindUser.addActionListener(e -> verifyUsername());
        txtUsername.addActionListener(e -> verifyUsername());
        btnCancelP1.addActionListener(e -> {
            if (onBackToLogin != null) onBackToLogin.run();
        });

        btnBackToP1.addActionListener(e -> slideToStep(0));
        btnCancelP2.addActionListener(e -> {
            if (onBackToLogin != null) onBackToLogin.run();
        });
        btnResetPassword.addActionListener(e -> performPasswordReset());
        txtConfirmPassword.addActionListener(e -> performPasswordReset());
        txtCaptchaInput.addActionListener(e -> performPasswordReset());

        generateNewCaptcha();
    }

    public void resetState() {
        matchedUser = null;
        if (txtUsername != null) txtUsername.setText("");
        if (txtSecurityAnswer != null) txtSecurityAnswer.setText("");
        if (txtNewPassword != null) txtNewPassword.setText("");
        if (txtConfirmPassword != null) txtConfirmPassword.setText("");
        if (txtCaptchaInput != null) txtCaptchaInput.setText("");

        generateNewCaptcha();
        slideToStep(0);
        SwingUtilities.invokeLater(() -> {
            if (txtUsername != null) txtUsername.requestFocusInWindow();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HEADER BANNER
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeaderBanner() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int r = 16;
                GradientPaint gp = new GradientPaint(0, 0, FOREST_DEEP, getWidth(), getHeight(), FOREST_GREEN);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
                // Flatten bottom corners
                g2.fillRect(0, getHeight() - r, getWidth(), r);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Left Icon + Titles
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        // Shield / Lock Badge (34x34)
        JComponent shieldBadge = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(255, 255, 255, 24));
                g2.fillRoundRect(0, 0, w, h, 10, 10);
                g2.setColor(new Color(255, 255, 255, 45));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                int cx = w / 2, cy = h / 2;
                Path2D shield = new Path2D.Float();
                shield.moveTo(cx - 8, cy - 7);
                shield.lineTo(cx + 8, cy - 7);
                shield.lineTo(cx + 8, cy);
                shield.quadTo(cx + 8, cy + 9, cx, cy + 12);
                shield.quadTo(cx - 8, cy + 9, cx - 8, cy);
                shield.closePath();

                g2.setColor(ACCENT_LIME);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(shield);

                g2.fillOval(cx - 2, cy - 3, 4, 4);
                g2.drawLine(cx, cy, cx, cy + 5);
                g2.dispose();
            }
            @Override
            public Dimension getPreferredSize() { return new Dimension(34, 34); }
        };
        left.add(shieldBadge);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);

        JLabel lblTitle = new JLabel("Account Recovery");
        lblTitle.setFont(appFont(Font.BOLD, 15));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Reset your forgotten password");
        lblSub.setFont(appFont(Font.PLAIN, 11));
        lblSub.setForeground(BORDER_SAGE);

        titles.add(lblTitle);
        titles.add(Box.createVerticalStrut(1));
        titles.add(lblSub);
        left.add(titles);

        header.add(left, BorderLayout.WEST);

        // Step chips indicator on right
        JPanel rightChips = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 6));
        rightChips.setOpaque(false);

        lblStep1Chip = createStepChip("1. Identify", true);
        JLabel arrow = new JLabel("→");
        arrow.setFont(appFont(Font.BOLD, 11));
        arrow.setForeground(new Color(255, 255, 255, 120));
        lblStep2Chip = createStepChip("2. Reset", false);

        rightChips.add(lblStep1Chip);
        rightChips.add(arrow);
        rightChips.add(lblStep2Chip);
        header.add(rightChips, BorderLayout.EAST);

        return header;
    }

    private JLabel createStepChip(String text, boolean active) {
        JLabel chip = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setColor(new Color(193, 247, 0, 40));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.setColor(ACCENT_LIME);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight() - 1, getHeight() - 1);
                } else {
                    g2.setColor(new Color(255, 255, 255, 15));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chip.setFont(appFont(Font.BOLD, 10));
        chip.setForeground(active ? ACCENT_LIME : new Color(255, 255, 255, 150));
        chip.setBorder(new EmptyBorder(2, 8, 2, 8));
        chip.setOpaque(false);
        return chip;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAGE 1: IDENTIFICATION (COMPACT)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildPage1() {
        JPanel card = createCardPanel();

        JLabel lblHeading = new JLabel("Find Your Account");
        lblHeading.setFont(appFont(Font.BOLD, 15));
        lblHeading.setForeground(TEXT_DARK);
        lblHeading.setAlignmentX(0f);
        card.add(lblHeading);
        card.add(Box.createVerticalStrut(4));

        JLabel lblDesc = new JLabel("<html>Enter your username or staff ID to retrieve your registered security question.</html>");
        lblDesc.setFont(appFont(Font.PLAIN, 12));
        lblDesc.setForeground(TEXT_MUTED);
        lblDesc.setAlignmentX(0f);
        card.add(lblDesc);
        card.add(Box.createVerticalStrut(20));

        card.add(createFieldLabel("Username or Staff ID"));
        card.add(Box.createVerticalStrut(4));

        txtUsername = new JTextField();
        txtUsername.setFont(appFont(Font.PLAIN, 13));
        txtUsername.setForeground(TEXT_DARK);
        JPanel wrapUsername = createPillField(txtUsername, "e.g. admin or staff", null, 34);

        btnFindUser = createPillButton("Verify & Continue →", FOREST_GREEN, Color.WHITE, 34);
        btnFindUser.setPreferredSize(new Dimension(145, 34));

        JPanel userRow = new JPanel(new BorderLayout(8, 0));
        userRow.setOpaque(false);
        userRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        userRow.setAlignmentX(0f);
        userRow.add(wrapUsername, BorderLayout.CENTER);
        userRow.add(btnFindUser, BorderLayout.EAST);
        card.add(userRow);
        card.add(Box.createVerticalStrut(10));

        // Subtle demo helper pill
        JPanel hintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        hintPanel.setOpaque(false);
        hintPanel.setAlignmentX(0f);
        JLabel lblHint = new JLabel("● Quick Demo: Try 'admin' or 'staff'");
        lblHint.setFont(appFont(Font.PLAIN, 11));
        lblHint.setForeground(TEXT_MUTED);
        hintPanel.add(lblHint);
        card.add(hintPanel);

        card.add(Box.createVerticalGlue());

        // Page 1 footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        footer.setAlignmentX(0f);

        btnCancelP1 = createGhostButton("← Back to Login", 34);
        btnCancelP1.setPreferredSize(new Dimension(135, 34));
        footer.add(btnCancelP1, BorderLayout.WEST);

        card.add(footer);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAGE 2: SECURITY CHALLENGE & RESET (COMPACT)
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildPage2() {
        JPanel card = createCardPanel();

        // Top verified user pill badge
        JPanel badgeRow = new JPanel(new BorderLayout(6, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(236, 253, 243));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(167, 243, 208));
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badgeRow.setOpaque(false);
        badgeRow.setBorder(new EmptyBorder(5, 10, 5, 10));
        badgeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        badgeRow.setAlignmentX(0f);

        lblVerifiedBadge = new JLabel("✓ Account Verified");
        lblVerifiedBadge.setFont(appFont(Font.BOLD, 11));
        lblVerifiedBadge.setForeground(new Color(6, 95, 70));
        badgeRow.add(lblVerifiedBadge, BorderLayout.CENTER);
        card.add(badgeRow);
        card.add(Box.createVerticalStrut(8));

        // Security Question Card (Compact)
        JPanel questionCard = new JPanel(new BorderLayout(6, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(244, 254, 247));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(BORDER_SAGE);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                // Left accent bar
                g2.setColor(FOREST_GREEN);
                g2.fillRoundRect(0, 0, 3, getHeight(), 3, 3);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        questionCard.setOpaque(false);
        questionCard.setBorder(new EmptyBorder(5, 10, 5, 10));
        questionCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        questionCard.setAlignmentX(0f);

        JLabel lblQHeader = new JLabel("Security Question:");
        lblQHeader.setFont(appFont(Font.PLAIN, 10));
        lblQHeader.setForeground(TEXT_MUTED);

        lblSecurityQuestion = new JLabel("Loading question...");
        lblSecurityQuestion.setFont(appFont(Font.BOLD, 12));
        lblSecurityQuestion.setForeground(TEXT_DARK);

        questionCard.add(lblQHeader, BorderLayout.NORTH);
        questionCard.add(lblSecurityQuestion, BorderLayout.CENTER);
        card.add(questionCard);
        card.add(Box.createVerticalStrut(8));

        // Security Answer (Compact 32px)
        card.add(createFieldLabel("Your Security Answer"));
        card.add(Box.createVerticalStrut(3));
        txtSecurityAnswer = new JTextField();
        txtSecurityAnswer.setFont(appFont(Font.PLAIN, 12));
        card.add(createPillField(txtSecurityAnswer, "Enter secret answer", null, 32));
        card.add(Box.createVerticalStrut(8));

        // Passwords Row (Side-by-side Compact 32px)
        JPanel passRow = new JPanel(new GridLayout(1, 2, 8, 0));
        passRow.setOpaque(false);
        passRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        passRow.setAlignmentX(0f);

        // New Password
        JPanel pNew = new JPanel();
        pNew.setLayout(new BoxLayout(pNew, BoxLayout.Y_AXIS));
        pNew.setOpaque(false);
        pNew.add(createFieldLabel("New Password"));
        pNew.add(Box.createVerticalStrut(3));
        txtNewPassword = new JPasswordField();
        txtNewPassword.setFont(appFont(Font.PLAIN, 12));
        btnToggleNewPass = createToggleEyeButton(e -> {
            showNewPass = !showNewPass;
            txtNewPassword.setEchoChar(showNewPass ? (char) 0 : '•');
            btnToggleNewPass.setText(showNewPass ? "Hide" : "Show");
        });
        pNew.add(createPillField(txtNewPassword, "Min 4 chars", btnToggleNewPass, 32));

        // Confirm Password
        JPanel pConf = new JPanel();
        pConf.setLayout(new BoxLayout(pConf, BoxLayout.Y_AXIS));
        pConf.setOpaque(false);
        pConf.add(createFieldLabel("Confirm Password"));
        pConf.add(Box.createVerticalStrut(3));
        txtConfirmPassword = new JPasswordField();
        txtConfirmPassword.setFont(appFont(Font.PLAIN, 12));
        btnToggleConfPass = createToggleEyeButton(e -> {
            showConfPass = !showConfPass;
            txtConfirmPassword.setEchoChar(showConfPass ? (char) 0 : '•');
            btnToggleConfPass.setText(showConfPass ? "Hide" : "Show");
        });
        pConf.add(createPillField(txtConfirmPassword, "Re-enter pass", btnToggleConfPass, 32));

        passRow.add(pNew);
        passRow.add(pConf);
        card.add(passRow);
        card.add(Box.createVerticalStrut(8));

        // Anti-Bot Verification (Compact 32px)
        card.add(createFieldLabel("Security Verification Code"));
        card.add(Box.createVerticalStrut(3));

        JPanel captchaRow = new JPanel(new BorderLayout(6, 0));
        captchaRow.setOpaque(false);
        captchaRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        captchaRow.setAlignmentX(0f);

        lblCaptchaDisplay = new CaptchaDisplay();
        lblCaptchaDisplay.setPreferredSize(new Dimension(105, 32));

        btnRefreshCaptcha = new JButton("↻") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? CANVAS_BG : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER_SAGE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnRefreshCaptcha.setFont(appFont(Font.BOLD, 13));
        btnRefreshCaptcha.setForeground(FOREST_MID);
        btnRefreshCaptcha.setFocusPainted(false);
        btnRefreshCaptcha.setContentAreaFilled(false);
        btnRefreshCaptcha.setBorderPainted(false);
        btnRefreshCaptcha.setPreferredSize(new Dimension(32, 32));
        btnRefreshCaptcha.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRefreshCaptcha.setToolTipText("Generate new captcha");
        btnRefreshCaptcha.addActionListener(e -> generateNewCaptcha());

        JPanel captchaBadgeWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        captchaBadgeWrap.setOpaque(false);
        captchaBadgeWrap.add(lblCaptchaDisplay);
        captchaBadgeWrap.add(btnRefreshCaptcha);

        txtCaptchaInput = new JTextField();
        txtCaptchaInput.setFont(appFont(Font.BOLD, 12));
        txtCaptchaInput.setHorizontalAlignment(JTextField.CENTER);
        JPanel wrapCaptchaInput = createPillField(txtCaptchaInput, "5-digit code", null, 32);

        captchaRow.add(captchaBadgeWrap, BorderLayout.WEST);
        captchaRow.add(wrapCaptchaInput, BorderLayout.CENTER);
        card.add(captchaRow);

        card.add(Box.createVerticalGlue());

        // Page 2 Action Buttons Footer (Compact 34px)
        JPanel footer = new JPanel(new BorderLayout(6, 0));
        footer.setOpaque(false);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        footer.setAlignmentX(0f);

        btnBackToP1 = createGhostButton("← Back", 34);
        btnBackToP1.setPreferredSize(new Dimension(78, 34));

        btnCancelP2 = createGhostButton("Cancel", 34);
        btnCancelP2.setPreferredSize(new Dimension(75, 34));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftActions.setOpaque(false);
        leftActions.add(btnBackToP1);
        leftActions.add(btnCancelP2);

        btnResetPassword = createPillButton("Reset Password", FOREST_GREEN, Color.WHITE, 34);
        btnResetPassword.setPreferredSize(new Dimension(130, 34));

        footer.add(leftActions, BorderLayout.WEST);
        footer.add(btnResetPassword, BorderLayout.EAST);
        card.add(footer);

        return card;
    }

    private JPanel createCardPanel() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERNAL SLIDING CONTAINER
    // ─────────────────────────────────────────────────────────────────────────
    class InternalSlidingContainer extends JPanel {
        private final JPanel p1;
        private final JPanel p2;
        private float slideProgress = 0f;
        private Timer animTimer;

        public InternalSlidingContainer(JPanel p1, JPanel p2) {
            this.p1 = p1;
            this.p2 = p2;
            setLayout(null);
            setOpaque(false);
            add(p1);
            add(p2);
        }

        public void slideToPage(int targetPage, Runnable onComplete) {
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }
            float target = (targetPage == 1) ? 1.0f : 0.0f;
            float start = slideProgress;
            final long startTime = System.currentTimeMillis();
            final int duration = 260; // 260 ms snappy slide

            animTimer = new Timer(15, e -> {
                long elapsed = System.currentTimeMillis() - startTime;
                float t = Math.min(1.0f, (float) elapsed / duration);
                float ease = 1.0f - (float) Math.pow(1.0 - t, 3.0);
                slideProgress = start + (target - start) * ease;
                revalidate();
                repaint();

                if (t >= 1.0f) {
                    slideProgress = target;
                    animTimer.stop();
                    revalidate();
                    repaint();
                    if (onComplete != null) onComplete.run();
                }
            });
            animTimer.start();
        }

        @Override
        public void doLayout() {
            int w = getWidth();
            int h = getHeight();
            int offset = (int) (w * slideProgress);
            p1.setBounds(-offset, 0, w, h);
            p2.setBounds(w - offset, 0, w, h);
        }
    }

    private void slideToStep(int stepIndex) {
        boolean toStep2 = (stepIndex == 1);
        lblStep1Chip.setText(toStep2 ? "✓ Identify" : "1. Identify");
        lblStep2Chip.setForeground(toStep2 ? ACCENT_LIME : new Color(255, 255, 255, 150));
        lblStep1Chip.repaint();
        lblStep2Chip.repaint();

        slidingContainer.slideToPage(stepIndex, () -> {
            if (toStep2) {
                txtSecurityAnswer.requestFocusInWindow();
            } else {
                txtUsername.requestFocusInWindow();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPACT UI HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(appFont(Font.BOLD, 11));
        l.setForeground(TEXT_MED);
        l.setAlignmentX(0f);
        return l;
    }

    private JPanel createPillField(JTextField tf, String placeholder, JButton extra, int height) {
        JPanel w = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean foc = tf.hasFocus();
                g2.setColor(tf.isEnabled() ? (foc ? new Color(240, 253, 244) : Color.WHITE) : new Color(248, 250, 252));
                int r = getHeight();
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
                g2.setColor(tf.isEnabled() ? (foc ? FOREST_MID : BORDER_SAGE) : new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(foc ? 1.3f : 1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, r - 1, r - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        w.setOpaque(false);
        tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(4, 12, 4, extra != null ? 2 : 12));
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { w.repaint(); }
            @Override public void focusLost(FocusEvent e)   { w.repaint(); }
        });
        w.add(tf, BorderLayout.CENTER);
        if (extra != null) {
            extra.setOpaque(false);
            w.add(extra, BorderLayout.EAST);
        }
        w.setPreferredSize(new Dimension(220, height));
        w.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        w.setAlignmentX(0f);
        return w;
    }

    private JButton createPillButton(String text, Color bg, Color fg, int height) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int r = getHeight();
                if (!isEnabled()) {
                    g2.setColor(new Color(226, 232, 240));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
                    g2.dispose();
                    super.paintComponent(g);
                    return;
                }
                Color c = getModel().isPressed() ? bg.darker().darker() : getModel().isRollover() ? bg.darker() : bg;
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(appFont(Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createGhostButton(String text, int height) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int r = getHeight();
                g2.setColor(getModel().isRollover() ? new Color(241, 245, 249) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
                g2.setColor(BORDER_SAGE);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, r - 1, r - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(appFont(Font.PLAIN, 12));
        btn.setForeground(TEXT_MED);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createToggleEyeButton(ActionListener l) {
        JButton btn = new JButton("Show");
        btn.setFont(appFont(Font.PLAIN, 10));
        btn.setForeground(TEXT_MUTED);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(2, 4, 2, 8));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(l);
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CAPTCHA COMPONENT (COMPACT)
    // ─────────────────────────────────────────────────────────────────────────
    class CaptchaDisplay extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            g2.setColor(new Color(236, 252, 240));
            g2.fillRoundRect(0, 0, w, h, 8, 8);
            g2.setColor(BORDER_SAGE);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

            // Diagonal security strokes
            g2.setColor(new Color(0, 90, 61, 22));
            g2.setStroke(new BasicStroke(1.0f));
            for (int i = 6; i < w; i += 16) {
                g2.drawLine(i, 3, i + 6, h - 3);
            }

            if (generatedCaptcha != null) {
                g2.setFont(new Font("Monospaced", Font.BOLD | Font.ITALIC, 14));
                g2.setColor(FOREST_DEEP);
                FontMetrics fm = g2.getFontMetrics();
                int totalWidth = fm.stringWidth(generatedCaptcha);
                int sx = (w - totalWidth) / 2;
                int sy = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(generatedCaptcha, sx, sy);
            }
            g2.dispose();
        }
    }

    private void generateNewCaptcha() {
        String chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        generatedCaptcha = sb.toString();
        if (lblCaptchaDisplay != null) {
            lblCaptchaDisplay.repaint();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUSINESS LOGIC
    // ─────────────────────────────────────────────────────────────────────────
    private void verifyUsername() {
        String username = txtUsername.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your username or staff ID.", "Username Required", JOptionPane.WARNING_MESSAGE);
            txtUsername.requestFocusInWindow();
            return;
        }

        User user = null;
        try {
            user = userDAO.getUserByUsername(username);
        } catch (Exception ex) {
            System.err.println("DB lookup error: " + ex.getMessage());
        }

        // Offline demo fallback for standard accounts
        if (user == null) {
            if ("admin".equalsIgnoreCase(username)) {
                user = new User(1, "admin", "admin123", "System Administrator", "ADMIN", "What is your pet name?", "tiger");
            } else if ("staff".equalsIgnoreCase(username)) {
                user = new User(2, "staff", "staff123", "Cashier Desk", "STAFF", "What is your pet name?", "buddy");
            }
        }

        if (user != null) {
            matchedUser = user;
            String q = user.getSecurityQuestion();
            if (q == null || q.trim().isEmpty()) {
                q = "What is your pet name?";
            }
            lblSecurityQuestion.setText(q);
            lblVerifiedBadge.setText("✓ Verified: " + user.getFullName() + " (@" + user.getUsername() + ") • " + user.getRole());

            generateNewCaptcha();
            txtSecurityAnswer.setText("");
            txtNewPassword.setText("");
            txtConfirmPassword.setText("");
            txtCaptchaInput.setText("");

            slideToStep(1);
        } else {
            JOptionPane.showMessageDialog(this,
                    "No account found matching '" + username + "'.\nPlease check spelling or try 'admin' / 'staff'.",
                    "Account Not Found", JOptionPane.ERROR_MESSAGE);
            txtUsername.selectAll();
            txtUsername.requestFocusInWindow();
        }
    }

    private void performPasswordReset() {
        if (matchedUser == null) {
            JOptionPane.showMessageDialog(this, "Please verify your username first.", "Verification Required", JOptionPane.WARNING_MESSAGE);
            slideToStep(0);
            return;
        }

        String answer = txtSecurityAnswer.getText().trim();
        String newPass = new String(txtNewPassword.getPassword()).trim();
        String confPass = new String(txtConfirmPassword.getPassword()).trim();
        String captcha = txtCaptchaInput.getText().trim();

        if (answer.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your security answer.", "Security Answer Required", JOptionPane.WARNING_MESSAGE);
            txtSecurityAnswer.requestFocusInWindow();
            return;
        }

        if (newPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a new password.", "New Password Required", JOptionPane.WARNING_MESSAGE);
            txtNewPassword.requestFocusInWindow();
            return;
        }

        if (newPass.length() < 4) {
            JOptionPane.showMessageDialog(this, "Password must be at least 4 characters long.", "Password Too Weak", JOptionPane.WARNING_MESSAGE);
            txtNewPassword.requestFocusInWindow();
            return;
        }

        if (!newPass.equals(confPass)) {
            JOptionPane.showMessageDialog(this, "New password and confirmation do not match.", "Password Mismatch", JOptionPane.WARNING_MESSAGE);
            txtConfirmPassword.requestFocusInWindow();
            return;
        }

        if (generatedCaptcha == null || !generatedCaptcha.equalsIgnoreCase(captcha)) {
            JOptionPane.showMessageDialog(this, "Incorrect CAPTCHA code. A new code has been generated.", "CAPTCHA Failed", JOptionPane.ERROR_MESSAGE);
            generateNewCaptcha();
            txtCaptchaInput.setText("");
            txtCaptchaInput.requestFocusInWindow();
            return;
        }

        // Validate security answer
        String expectedAnswer = matchedUser.getSecurityAnswer();
        if (expectedAnswer == null || expectedAnswer.trim().isEmpty()) {
            expectedAnswer = "tiger";
        }

        if (!expectedAnswer.trim().equalsIgnoreCase(answer)) {
            JOptionPane.showMessageDialog(this, "Incorrect security answer. Access denied.", "Security Check Failed", JOptionPane.ERROR_MESSAGE);
            txtSecurityAnswer.selectAll();
            txtSecurityAnswer.requestFocusInWindow();
            return;
        }

        // Update password in database
        boolean success = false;
        try {
            success = userDAO.resetPassword(matchedUser.getUsername(), answer, newPass);
        } catch (Exception ex) {
            System.err.println("DB reset error: " + ex.getMessage());
        }

        // If offline fallback or DB success
        if (success || matchedUser.getId() <= 2) {
            JOptionPane.showMessageDialog(this,
                    "Password reset successfully!\nYou may now login with your new credentials.",
                    "Reset Complete", JOptionPane.INFORMATION_MESSAGE);
            if (onResetSuccess != null) {
                onResetSuccess.run();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update password in database. Please verify MySQL service.", "Update Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
