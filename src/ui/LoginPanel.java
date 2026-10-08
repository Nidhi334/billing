package ui;

import config.DBConnection;
import dao.UserDAO;
import model.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * SmartBilling Pro – Login Screen.
 *
 * Layout (GridLayout 1×2, fills full window):
 *   LEFT  → dark-green inset card with rounded corners + animated features
 *   RIGHT → pure white, minimal login form capped at 380 px wide
 *
 * Design tokens:
 *   - All vector icons drawn via Graphics2D paths (no emoji rendering issues)
 *   - Input fields: filled bg, fully rounded, no visible border line
 *   - Buttons: pill-shaped (cornerRadius = height/2)
 *   - Login button: dark green pill
 *   - Font: Helvetica Neue on macOS (avoids Lucida Grande Bold glyph-cache bug)
 */
public class LoginPanel extends JPanel {

    // ── Color palette ─────────────────────────────────────────────────────────
    static final Color WIN_BG      = AppTheme.BG_CANVAS;         // airy mint canvas #ecfcf0
    static final Color CARD_DARK   = AppTheme.FOREST_DEEP;       // dark pine #003826
    static final Color CARD_MID    = AppTheme.FOREST_GREEN;      // brand forest green #005a3d
    static final Color DARK_GREEN  = AppTheme.FOREST_GREEN;      // primary pill CTA #005a3d
    static final Color MID_GREEN   = AppTheme.FOREST_MID;        // mid forest accent #167a54
    static final Color FIELD_BG    = Color.WHITE;                // crisp white input field bg
    static final Color FIELD_FOC   = new Color(240, 253, 244);   // soft mint focused state
    static final Color TEXT_DARK   = AppTheme.TEXT_PRIMARY;      // high-contrast pine slate #0a2e21
    static final Color TEXT_MED    = AppTheme.TEXT_SECONDARY;    // secondary pine slate #335e4e
    static final Color TEXT_SOFT   = AppTheme.TEXT_MUTED;        // muted sage gray #6e9384
    static final Color TEXT_FAINT  = new Color(130, 160, 146);

    // ── Fields ────────────────────────────────────────────────────────────────
    private final Frame parentFrame;
    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin, btnDbConfig, btnForgotPassword;
    private JButton btnRoleDropdown;
    private RoleDropdownCard roleDropdownCard;
    private boolean isAdmin = true;
    private JCheckBox chkRemember;
    private JButton btnTogglePassword;
    private boolean showPassword = false;
    private LeftPanel leftPanel;
    private final UserDAO userDAO = new UserDAO();

    private final AWTEventListener clickOutsideHandler = event -> {
        if (event instanceof MouseEvent && ((MouseEvent) event).getID() == MouseEvent.MOUSE_PRESSED) {
            MouseEvent me = (MouseEvent) event;
            Component src = me.getComponent();
            if (roleDropdownCard != null && roleDropdownCard.isOpen()) {
                if (src != null && !SwingUtilities.isDescendingFrom(src, roleDropdownCard) && src != btnRoleDropdown) {
                    roleDropdownCard.close();
                }
            }
        }
    };

    // ─────────────────────────────────────────────────────────────────────────
    public LoginPanel(Frame parent) {
        this.parentFrame = parent;
        setLayout(new GridLayout(1, 2, 0, 0));
        setBackground(WIN_BG);    // visible across entire window
        setOpaque(true);
        init();
    }

    /** Helvetica Neue on macOS bypasses the Lucida Grande Bold glyph-cache bug. */
    public static Font appFont(int style, int size) {
        boolean mac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        if (mac) {
            Font f = new Font("Helvetica Neue", style, size);
            return f.getFamily().equalsIgnoreCase("dialog") ? new Font("Arial", style, size) : f;
        }
        return new Font(Font.SANS_SERIF, style, size);
    }

    @Override public void removeNotify() {
        super.removeNotify();
        if (leftPanel != null) leftPanel.stop();
        if (roleDropdownCard != null) roleDropdownCard.close();
    }
    public void stopVideo() { if (leftPanel != null) leftPanel.stop(); }

    // ─────────────────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────────────────
    private void init() {
        leftPanel = new LeftPanel();
        leftPanel.setBackground(WIN_BG);
        leftPanel.setOpaque(true);
        add(leftPanel);

        JPanel right = buildRight();
        right.setBackground(WIN_BG);
        right.setOpaque(true);
        add(right);

        btnLogin.addActionListener          (e -> login());
        txtPassword.addActionListener       (e -> login());
        txtUsername.addActionListener       (e -> txtPassword.requestFocusInWindow());
        btnForgotPassword.addActionListener (e -> forgotPwd());
        btnDbConfig.addActionListener       (e -> dbConfig());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RIGHT PANEL  –  minimal white login, form capped at 380 px
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildRight() {
        JPanel basePanel = new JPanel(new BorderLayout());
        basePanel.setOpaque(false);

        // ── Top-right role switcher (borderless dropdown) ─────────────────────
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 32, 16));
        topBar.setOpaque(false);

        btnRoleDropdown = new JButton("Administrator", new ChevronIcon());
        btnRoleDropdown.setHorizontalTextPosition(SwingConstants.LEFT);
        btnRoleDropdown.setIconTextGap(8);
        btnRoleDropdown.setFont(appFont(Font.BOLD, 13));
        btnRoleDropdown.setForeground(TEXT_MED);
        btnRoleDropdown.setOpaque(false);
        btnRoleDropdown.setContentAreaFilled(false);
        btnRoleDropdown.setBorderPainted(false);
        btnRoleDropdown.setFocusPainted(false);
        btnRoleDropdown.setBorder(new EmptyBorder(6, 10, 6, 6));
        btnRoleDropdown.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRoleDropdown.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btnRoleDropdown.setForeground(DARK_GREEN); }
            @Override public void mouseExited (MouseEvent e) { btnRoleDropdown.setForeground(TEXT_MED); }
        });

        btnRoleDropdown.addActionListener(e -> {
            if (roleDropdownCard.isOpen()) {
                roleDropdownCard.close();
            } else {
                roleDropdownCard.open();
            }
        });

        topBar.add(btnRoleDropdown);
        basePanel.add(topBar, BorderLayout.NORTH);

        /*
         * Form has a fixed preferred width of 380 px centered in the middle
         */
        JPanel form = new JPanel() {
            @Override public Dimension getPreferredSize()  { return new Dimension(380, super.getPreferredSize().height); }
            @Override public Dimension getMaximumSize()    { return new Dimension(380, Integer.MAX_VALUE); }
        };
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        // ── Brand mark (macOS squircle storefront icon badge) ───────────────
        JComponent brandIcon = util.AppIconUtil.createBrandBadge(36);
        brandIcon.setAlignmentY(0.5f);

        JLabel brandName = new JLabel("SmartBilling Pro");
        brandName.setFont(appFont(Font.BOLD, 15));
        brandName.setForeground(TEXT_DARK);
        brandName.setAlignmentY(0.5f);

        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        brand.setOpaque(false);
        brand.setAlignmentX(0f);
        brand.add(brandIcon);
        brand.add(Box.createHorizontalStrut(10));
        brand.add(brandName);
        form.add(brand);
        form.add(Box.createVerticalStrut(28));

        // ── Title ─────────────────────────────────────────────────────────────
        JLabel title = new JLabel("Login to your account");
        title.setFont(appFont(Font.BOLD, 28));
        title.setForeground(TEXT_DARK);
        title.setAlignmentX(0f);
        form.add(title);
        form.add(Box.createVerticalStrut(26));

        // ── Username ──────────────────────────────────────────────────────────
        form.add(fLabel("Username or Staff ID"));
        form.add(Box.createVerticalStrut(5));
        txtUsername = new JTextField("admin");
        txtUsername.setFont(appFont(Font.PLAIN, 14));
        form.add(fWrap(txtUsername, null));
        form.add(Box.createVerticalStrut(16));

        // ── Password ──────────────────────────────────────────────────────────
        form.add(fLabel("Password"));
        form.add(Box.createVerticalStrut(5));
        txtPassword = new JPasswordField("admin123");
        txtPassword.setFont(appFont(Font.PLAIN, 14));
        btnTogglePassword = new JButton("Show");
        btnTogglePassword.setFont(appFont(Font.PLAIN, 12));
        btnTogglePassword.setForeground(TEXT_SOFT);
        btnTogglePassword.setContentAreaFilled(false);
        btnTogglePassword.setBorderPainted(false);
        btnTogglePassword.setFocusPainted(false);
        btnTogglePassword.setBorder(new EmptyBorder(4, 8, 4, 18));
        btnTogglePassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTogglePassword.addActionListener(e -> {
            showPassword = !showPassword;
            txtPassword.setEchoChar(showPassword ? (char) 0 : '•');
            btnTogglePassword.setText(showPassword ? "Hide" : "Show");
        });
        form.add(fWrap(txtPassword, btnTogglePassword));
        form.add(Box.createVerticalStrut(12));

        // ── Remember / Forgot row ─────────────────────────────────────────────
        JPanel optRow = new JPanel(new BorderLayout());
        optRow.setOpaque(false);
        optRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        optRow.setAlignmentX(0f);
        chkRemember = new JCheckBox("Remember this terminal", true);
        chkRemember.setFont(appFont(Font.PLAIN, 13));
        chkRemember.setForeground(TEXT_SOFT);
        chkRemember.setOpaque(false);
        chkRemember.setFocusPainted(false);
        btnForgotPassword = new JButton("Forgot Password?");
        btnForgotPassword.setFont(appFont(Font.PLAIN, 13));
        btnForgotPassword.setForeground(MID_GREEN);
        btnForgotPassword.setContentAreaFilled(false);
        btnForgotPassword.setBorderPainted(false);
        btnForgotPassword.setFocusPainted(false);
        btnForgotPassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        optRow.add(chkRemember,       BorderLayout.WEST);
        optRow.add(btnForgotPassword, BorderLayout.EAST);
        form.add(optRow);
        form.add(Box.createVerticalStrut(24));

        // ── Login button (dark green pill) ────────────────────────────────────
        btnLogin = pillBtn("Login", DARK_GREEN, Color.WHITE);
        form.add(btnLogin);
        form.add(Box.createVerticalStrut(14));

        // ── OR divider ────────────────────────────────────────────────────────
        form.add(orDivider());
        form.add(Box.createVerticalStrut(14));

        // ── Secondary ghost pill buttons ──────────────────────────────────────
        JPanel secRow = new JPanel(new GridLayout(1, 2, 10, 0));
        secRow.setOpaque(false);
        secRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        secRow.setAlignmentX(0f);
        JButton btnDemo  = ghostPill("Quick Demo",    new VecIcon("bolt", 14, TEXT_MED));
        JButton btnKiosk = ghostPill("Self-Checkout", new VecIcon("cart", 14, TEXT_MED));
        btnDemo.addActionListener (e -> launchDemo());
        btnKiosk.addActionListener(e -> { stopVideo(); new SelfCheckoutFrame(null).setVisible(true); });
        secRow.add(btnDemo);
        secRow.add(btnKiosk);
        form.add(secRow);
        form.add(Box.createVerticalStrut(28));

        // ── Footer ────────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        footer.setAlignmentX(0f);
        btnDbConfig = new JButton("MySQL Database Settings");
        btnDbConfig.setFont(appFont(Font.PLAIN, 11));
        btnDbConfig.setForeground(TEXT_FAINT);
        btnDbConfig.setContentAreaFilled(false);
        btnDbConfig.setBorderPainted(false);
        btnDbConfig.setFocusPainted(false);
        btnDbConfig.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JLabel ver = new JLabel("v2.4 Enterprise");
        ver.setFont(appFont(Font.ITALIC, 11));
        ver.setForeground(TEXT_FAINT);
        footer.add(btnDbConfig, BorderLayout.WEST);
        footer.add(ver, BorderLayout.EAST);
        form.add(footer);

        // Center form in remaining space
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill   = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        centerPanel.add(form, gbc);
        basePanel.add(centerPanel, BorderLayout.CENTER);

        roleDropdownCard = new RoleDropdownCard();

        JPanel outer = new JPanel() {
            @Override
            public void doLayout() {
                int w = getWidth(), h = getHeight();
                basePanel.setBounds(0, 0, w, h);
                if (roleDropdownCard != null && btnRoleDropdown != null) {
                    Point pt = SwingUtilities.convertPoint(btnRoleDropdown, 0, btnRoleDropdown.getHeight() + 4, this);
                    int cardW = 196;
                    int cardH = 98;
                    int cardX = pt.x + btnRoleDropdown.getWidth() - cardW;
                    roleDropdownCard.setBounds(cardX, pt.y, cardW, cardH + 16);
                }
            }
        };
        outer.setLayout(null);
        outer.setOpaque(true);
        outer.setBackground(WIN_BG);
        outer.add(roleDropdownCard, 0);
        outer.add(basePanel, 1);
        return outer;
    }

    // ── Component helpers ─────────────────────────────────────────────────────

    private JLabel fLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(appFont(Font.BOLD, 14));
        l.setForeground(TEXT_MED);
        l.setAlignmentX(0f);
        return l;
    }

    /** Filled pill-shaped input wrapper – fully rounded (cornerRadius = height) */
    private JPanel fWrap(JTextField tf, JButton extra) {
        JPanel w = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean foc = tf.hasFocus();
                g2.setColor(foc ? FIELD_FOC : FIELD_BG);
                int r = getHeight();
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        w.setOpaque(false);
        tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(12, 20, 12, extra != null ? 6 : 20));
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { w.repaint(); }
            @Override public void focusLost  (FocusEvent e) { w.repaint(); }
        });
        w.add(tf, BorderLayout.CENTER);
        if (extra != null) { extra.setOpaque(false); w.add(extra, BorderLayout.EAST); }
        w.setPreferredSize(new Dimension(380, 50));
        w.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        w.setMinimumSize(new Dimension(120, 50));
        w.setAlignmentX(0f);
        return w;
    }

    class RoleDropdownCard extends JPanel {
        private float anim = 0f;
        private boolean open = false;
        private Timer animTimer;
        private int hoveredIdx = -1;

        RoleDropdownCard() {
            setOpaque(false);
            setVisible(false);

            animTimer = new Timer(16, e -> {
                if (open) {
                    anim += 0.12f;
                    if (anim >= 1f) { anim = 1f; animTimer.stop(); }
                } else {
                    anim -= 0.16f;
                    if (anim <= 0f) { anim = 0f; setVisible(false); animTimer.stop(); }
                }
                repaint();
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int idx = getItemAt(e.getY());
                    if (idx != hoveredIdx) {
                        hoveredIdx = idx;
                        repaint();
                    }
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    hoveredIdx = -1;
                    repaint();
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    int idx = getItemAt(e.getY());
                    if (idx == 0) {
                        pickRole("admin", "admin123", true);
                        close();
                    } else if (idx == 1) {
                        pickRole("staff", "staff123", false);
                        close();
                    }
                }
            });
        }

        private int getItemAt(int y) {
            int top = 8;
            int h = 38;
            if (y >= top && y < top + h) return 0;
            if (y >= top + h && y < top + h * 2) return 1;
            return -1;
        }

        boolean isOpen() { return open; }

        void open() {
            open = true;
            setVisible(true);
            try {
                Toolkit.getDefaultToolkit().addAWTEventListener(clickOutsideHandler, AWTEvent.MOUSE_EVENT_MASK);
            } catch (Exception ignored) {}
            animTimer.start();
            if (btnRoleDropdown != null) btnRoleDropdown.repaint();
        }

        void close() {
            open = false;
            try {
                Toolkit.getDefaultToolkit().removeAWTEventListener(clickOutsideHandler);
            } catch (Exception ignored) {}
            animTimer.start();
            if (btnRoleDropdown != null) btnRoleDropdown.repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (anim <= 0.001f) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            float p = 1.0f - (float) Math.pow(1.0 - anim, 3.0);
            int slideY = (int) (8 * (1.0f - p));
            int w = getWidth() - 8;
            int h = getHeight() - 14;
            int x = 4;
            int y = 4 + slideY;
            int radius = 16;

            Composite orig = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, anim));

            // Soft multi-layer drop shadow
            g2.setColor(new Color(0, 0, 0, (int)(18 * anim)));
            g2.fillRoundRect(x + 1, y + 4, w - 2, h - 2, radius, radius);
            g2.setColor(new Color(0, 0, 0, (int)(10 * anim)));
            g2.fillRoundRect(x, y + 2, w, h, radius, radius);

            // Card background
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(x, y, w, h, radius, radius);

            // Card border
            g2.setColor(new Color(AppTheme.BORDER_SAGE.getRed(), AppTheme.BORDER_SAGE.getGreen(), AppTheme.BORDER_SAGE.getBlue(), (int)(255 * anim)));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);

            // Render items
            drawItem(g2, x + 6, y + 6,  w - 12, 38, "Administrator", isAdmin,  hoveredIdx == 0);
            drawItem(g2, x + 6, y + 46, w - 12, 38, "Cashier Desk",  !isAdmin, hoveredIdx == 1);

            g2.setComposite(orig);
            g2.dispose();
        }

        private void drawItem(Graphics2D g2, int ix, int iy, int iw, int ih, String text, boolean active, boolean hover) {
            if (hover) {
                g2.setColor(AppTheme.BG_CANVAS);
                g2.fillRoundRect(ix, iy, iw, ih, 10, 10);
            }
            g2.setFont(appFont(active ? Font.BOLD : Font.PLAIN, 13));
            g2.setColor(active ? DARK_GREEN : (hover ? TEXT_DARK : TEXT_MED));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, ix + 12, iy + (ih + fm.getAscent() - fm.getDescent()) / 2);

            if (active) {
                int cx = ix + iw - 18;
                int cy = iy + ih / 2 - 4;
                Path2D check = new Path2D.Float();
                check.moveTo(cx, cy + 4);
                check.lineTo(cx + 3, cy + 7);
                check.lineTo(cx + 9, cy + 1);
                g2.setColor(DARK_GREEN);
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(check);
            }
        }
    }

    private void pickRole(String user, String pass, boolean isAdmin) {
        this.isAdmin = isAdmin;
        txtUsername.setText(user);
        txtPassword.setText(pass);
        if (btnRoleDropdown != null) {
            btnRoleDropdown.setText(isAdmin ? "Administrator" : "Cashier Desk");
            btnRoleDropdown.repaint();
        }
        if (roleDropdownCard != null) {
            roleDropdownCard.repaint();
        }
    }

    /** Fully-rounded (pill) filled button with generous vertical padding */
    private JButton pillBtn(String label, Color bg, Color fg) {
        Color hover = bg.darker();
        Color press = hover.darker();
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? press : getModel().isRollover() ? hover : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(appFont(Font.BOLD, 16));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(14, 24, 14, 24));
        btn.setPreferredSize(new Dimension(380, 52));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        btn.setMinimumSize(new Dimension(120, 52));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(0f);
        return btn;
    }

    /** Ghost pill button with vector icon + label */
    private JButton ghostPill(String text, Icon icon) {
        JButton btn = new JButton(text, icon) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int r = getHeight();
                g2.setColor(getModel().isRollover() ? AppTheme.BG_CANVAS : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, r-1, r-1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(appFont(Font.BOLD, 13));
        btn.setForeground(TEXT_DARK);
        btn.setIconTextGap(6);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JPanel orDivider() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                int mx = getWidth()/2, my = getHeight()/2;
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawLine(0, my, mx-22, my);
                g2.drawLine(mx+22, my, getWidth(), my);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        p.setAlignmentX(0f);
        JLabel or = new JLabel("OR");
        or.setFont(appFont(Font.BOLD, 11));
        or.setForeground(TEXT_FAINT);
        p.add(or);
        return p;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VECTOR ICONS  –  drawn via Graphics2D paths, no emoji / text glyphs
    // ─────────────────────────────────────────────────────────────────────────
    class ChevronIcon implements Icon {
        @Override public int getIconWidth()  { return 10; }
        @Override public int getIconHeight() { return 6; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(TEXT_MED);
            boolean isOpen = (roleDropdownCard != null && roleDropdownCard.isOpen());
            Path2D p = new Path2D.Float();
            if (isOpen) {
                p.moveTo(x + 1, y + 5);
                p.lineTo(x + 5, y + 1);
                p.lineTo(x + 9, y + 5);
            } else {
                p.moveTo(x + 1, y + 1);
                p.lineTo(x + 5, y + 5);
                p.lineTo(x + 9, y + 1);
            }
            g2.draw(p);
            g2.dispose();
        }
    }

    static class VecIcon implements Icon {
        final String type;
        final int    size;
        final Color  color;
        VecIcon(String type, int size, Color color) {
            this.type = type; this.size = size; this.color = color;
        }
        @Override public int getIconWidth()  { return size; }
        @Override public int getIconHeight() { return size; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            drawVecIcon(g2, type, x + size/2, y + size/2, size - 2);
            g2.dispose();
        }
    }

    static void drawVecIcon(Graphics2D g2, String type, int cx, int cy, int sz) {
        int h = Math.max(1, sz / 2);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (type) {
            case "bolt": {
                Path2D p = new Path2D.Float();
                p.moveTo(cx+3, cy-h);    p.lineTo(cx-3, cy+1);
                p.lineTo(cx+1, cy+1);    p.lineTo(cx-3, cy+h);
                p.lineTo(cx+4, cy);      p.lineTo(cx, cy);
                p.closePath();
                g2.fill(p);
                break;
            }
            case "cart": {
                // Basket arc
                g2.drawArc(cx - h, cy - 2, h*2 - 2, h + 2, 0, 180);
                // Wheels
                g2.fillOval(cx - h/2 - 3, cy + h - 4, 5, 5);
                g2.fillOval(cx + h/2 - 2, cy + h - 4, 5, 5);
                // Handle stub
                g2.drawLine(cx - h, cy + 1, cx - h - 3, cy - h/2);
                break;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LEFT PANEL  –  Onboarding slides (bottom-to-top wipe reveal every 2s)
    // ─────────────────────────────────────────────────────────────────────────
    class LeftPanel extends JPanel {
        final List<BufferedImage> slides = new ArrayList<>();
        int     currentIdx = 0;
        int     nextIdx    = 0;
        float   transitionProgress = 1f;
        boolean inTransition       = false;
        Timer   slideTimer;
        Timer   transitionTimer;

        // Card inset (gap around the card, with 42px top space for macOS traffic lights)
        static final int MX        = 22;   // left margin
        static final int MY_TOP    = 42;   // top margin (clearance under traffic lights)
        static final int MY_BOTTOM = 22;   // bottom margin
        static final int MR        = 14;   // right margin (gap between left card and right section)

        LeftPanel() {
            setOpaque(true);
            setBackground(WIN_BG);
            loadSlides();

            // Bottom-to-top curved reveal animation timer (~60 FPS)
            // Starts very fast and decelerates very slowly towards the end (quintic ease-out)
            transitionTimer = new Timer(16, e -> {
                transitionProgress += 0.012f;
                if (transitionProgress >= 1f) {
                    transitionProgress = 1f;
                    currentIdx = nextIdx;
                    inTransition = false;
                    transitionTimer.stop();
                }
                repaint();
            });

            // Slideshow timer: advance every 2.4 seconds
            slideTimer = new Timer(2400, e -> advanceSlide());
            if (!slides.isEmpty()) {
                slideTimer.start();
            }
        }

        private void loadSlides() {
            File dir = new File("data/assets/images/onboarding-slides");
            if (!dir.exists() || !dir.isDirectory()) {
                dir = new File(System.getProperty("user.dir"), "data/assets/images/onboarding-slides");
            }
            if (!dir.exists() || !dir.isDirectory()) {
                dir = new File("/Users/rishukumar/Github/billing/data/assets/images/onboarding-slides");
            }

            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> {
                    String n = name.toLowerCase();
                    return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png");
                });
                if (files != null && files.length > 0) {
                    Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                    for (File f : files) {
                        try {
                            BufferedImage img = ImageIO.read(f);
                            if (img != null) {
                                slides.add(img);
                            }
                        } catch (Exception ex) {
                            System.err.println("Failed to load slide " + f.getName() + ": " + ex.getMessage());
                        }
                    }
                }
            }
        }

        void advanceSlide() {
            if (slides.size() <= 1) return;
            if (inTransition) {
                currentIdx = nextIdx;
            }
            nextIdx = (currentIdx + 1) % slides.size();
            transitionProgress = 0f;
            inTransition = true;
            transitionTimer.start();
        }

        void stop() {
            if (slideTimer != null) slideTimer.stop();
            if (transitionTimer != null) transitionTimer.stop();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int W = getWidth(), H = getHeight();
            if (W <= 0 || H <= 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

            // Card bounds (offset from top for macOS traffic lights)
            int cx = MX, cy = MY_TOP, cw = W - MX - MR, ch = H - MY_TOP - MY_BOTTOM;
            int radius = 32;

            if (cw <= 0 || ch <= 0) {
                g2.dispose();
                return;
            }

            // Soft drop shadow behind card
            g2.setColor(new Color(0, 0, 0, 20));
            g2.fillRoundRect(cx + 4, cy + 4, cw - 4, ch - 2, radius, radius);
            g2.setColor(new Color(0, 0, 0, 10));
            g2.fillRoundRect(cx + 2, cy + 2, cw - 2, ch,     radius, radius);

            if (!slides.isEmpty()) {
                Shape cardShape = new RoundRectangle2D.Float(cx, cy, cw, ch, radius, radius);
                BufferedImage currentImg = slides.get(currentIdx % slides.size());

                if (inTransition && slides.size() > 1) {
                    BufferedImage nextImg = slides.get(nextIdx % slides.size());

                    // Easing: launches very fast and ends much more slowly (quintic ease-out)
                    float inv = 1.0f - Math.min(1.0f, transitionProgress);
                    float t   = 1.0f - (float) Math.pow(inv, 5.0);

                    // Height of the outer ends moving upward from (cy + ch) to cy
                    float ySides = (cy + ch) - (ch * t);

                    // Curvature: starts straight at bottom (dip=0), sags below in the middle,
                    // and straightens out smoothly as either end reaches the very top (dip=0).
                    float maxDip = Math.min(65f, cw * 0.14f);
                    float dip    = (float) (maxDip * Math.sin(Math.PI * t));

                    // Quadratic bezier control point (passes through ySides + dip at center)
                    float ctrlX  = cx + cw / 2.0f;
                    float ctrlY  = ySides + 2.0f * dip;

                    // Region below the curved line where next image is revealed
                    Path2D revealPath = new Path2D.Float();
                    revealPath.moveTo(cx - 4, ySides);
                    revealPath.quadTo(ctrlX, ctrlY, cx + cw + 4, ySides);
                    revealPath.lineTo(cx + cw + 4, cy + ch + 10);
                    revealPath.lineTo(cx - 4, cy + ch + 10);
                    revealPath.closePath();

                    // 1. Draw base current image (static in place)
                    Shape origClip = g2.getClip();
                    g2.setClip(cardShape);
                    drawCoverImage(g2, currentImg, cx, cy, cw, ch);

                    // 2. Reveal next image inside curved region (static in exact same place)
                    g2.clip(revealPath);
                    drawCoverImage(g2, nextImg, cx, cy, cw, ch);
                    g2.setClip(origClip);

                    // 3. Subtle translucent sheen along the curved reveal line
                    if (t > 0.02f && t < 0.98f) {
                        Shape lineClip = g2.getClip();
                        g2.setClip(cardShape);
                        Path2D curveLine = new Path2D.Float();
                        curveLine.moveTo(cx, ySides);
                        curveLine.quadTo(ctrlX, ctrlY, cx + cw, ySides);
                        g2.setColor(new Color(255, 255, 255, (int)(55 * Math.sin(Math.PI * t))));
                        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.draw(curveLine);
                        g2.setClip(lineClip);
                    }
                } else {
                    Shape origClip = g2.getClip();
                    g2.setClip(cardShape);
                    drawCoverImage(g2, currentImg, cx, cy, cw, ch);
                    g2.setClip(origClip);
                }
            } else {
                // Fallback card background if no images found
                g2.setClip(new RoundRectangle2D.Float(cx, cy, cw, ch, radius, radius));
                g2.setPaint(new GradientPaint(cx, cy, CARD_MID, cx, cy + ch, CARD_DARK));
                g2.fillRect(cx, cy, cw, ch);
            }

            g2.dispose();
        }

        private void drawCoverImage(Graphics2D g2, BufferedImage img, int x, int y, int w, int h) {
            if (img == null || w <= 0 || h <= 0) return;
            int imgW = img.getWidth();
            int imgH = img.getHeight();
            if (imgW <= 0 || imgH <= 0) return;

            // Full-cover calculation: fills the frame without distortion or letterboxing
            double scale = Math.max((double) w / imgW, (double) h / imgH);
            int dw = (int) Math.round(imgW * scale);
            int dh = (int) Math.round(imgH * scale);
            int dx = x + (w - dw) / 2;
            int dy = y + (h - dh) / 2;

            g2.drawImage(img, dx, dy, dw, dh, null);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUTH & BUSINESS LOGIC  (functionality unchanged)
    // ─────────────────────────────────────────────────────────────────────────
    private void launchDemo() {
        launchDashboard(new User(1, "admin", "admin123", "System Administrator (Demo/Offline)", "ADMIN"));
    }

    private void launchDashboard(User user) {
        stopVideo();
        try {
            new DashboardFrame(user).setVisible(true);
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w != null) w.dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Dashboard error:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void login() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword()).trim();
        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter username and password.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        User u = null;
        try {
            u = userDAO.authenticate(user, pass);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "";
            boolean conn = msg.toLowerCase().contains("connection refused")
                || msg.toLowerCase().contains("communications link failure")
                || msg.toLowerCase().contains("can't connect");
            boolean defA = "admin".equalsIgnoreCase(user) && "admin123".equals(pass);
            boolean defS = "staff".equalsIgnoreCase(user) && "staff123".equals(pass);
            if (defA || defS) {
                if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this,
                        "MySQL is not running.\nLaunch in OFFLINE / DEMO MODE?",
                        "MySQL Offline", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE))
                    launchDashboard(defA
                        ? new User(1, "admin", "admin123", "System Administrator (Demo)", "ADMIN")
                        : new User(2, "staff", "staff123", "Cashier Desk (Demo)", "STAFF"));
            } else {
                JOptionPane.showMessageDialog(this,
                    conn ? "MySQL is offline.\n\nsudo systemctl start mysql\n\nOr use: admin / admin123"
                         : "Database error:\n" + msg,
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
        if (u == null) { JOptionPane.showMessageDialog(this, "Invalid credentials.", "Login Failed", JOptionPane.ERROR_MESSAGE); return; }
        launchDashboard(u);
    }

    private void forgotPwd() {
        Window anc = SwingUtilities.getWindowAncestor(this);
        new ForgotPasswordDialog(anc instanceof Frame ? (Frame) anc : parentFrame).setVisible(true);
    }

    private void dbConfig() {
        Properties p = DBConnection.getProperties();
        JTextField host = new JTextField(p.getProperty("db.host", "localhost"));
        JTextField port = new JTextField(p.getProperty("db.port", "3306"));
        JTextField db   = new JTextField(p.getProperty("db.name", "billing_system"));
        JTextField usr  = new JTextField(p.getProperty("db.user", "root"));
        JPasswordField pw = new JPasswordField(p.getProperty("db.password", ""));
        JPanel pnl = new JPanel(new GridLayout(6, 2, 8, 8));
        pnl.add(new JLabel("MySQL Host:"));    pnl.add(host);
        pnl.add(new JLabel("MySQL Port:"));    pnl.add(port);
        pnl.add(new JLabel("Database Name:")); pnl.add(db);
        pnl.add(new JLabel("Username:"));      pnl.add(usr);
        pnl.add(new JLabel("Password:"));      pnl.add(pw);
        JButton test = new JButton("Test Connection"); pnl.add(test); pnl.add(new JLabel(""));
        test.addActionListener(e -> {
            String r = DBConnection.testConnection(host.getText().trim(), port.getText().trim(),
                db.getText().trim(), usr.getText().trim(), new String(pw.getPassword()));
            JOptionPane.showMessageDialog(this, r, "Test Result",
                r.startsWith("SUCCESS") || r.startsWith("CONNECTED")
                    ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        });
        if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(this, pnl, "MySQL Database Settings",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
            DBConnection.saveConfig(host.getText().trim(), port.getText().trim(),
                db.getText().trim(), usr.getText().trim(), new String(pw.getPassword()));
            JOptionPane.showMessageDialog(this, "Settings saved!", "Saved", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
