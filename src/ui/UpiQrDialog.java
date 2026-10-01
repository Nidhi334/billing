package ui;

import config.AppSettings;
import util.QrCodeGenerator;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.util.Locale;

/**
 * Supermarket / Mall-Style Dynamic UPI QR Payment Dialog.
 * Generates an official NPCI UPI QR code linked directly to the store owner's UPI account.
 * 
 * - Money goes 100% directly into the store owner's bank account.
 * - When scanned (PhonePe, GPay, Paytm, BHIM, Cred), the exact bill amount is
 *   automatically prefilled and locked on the customer's screen (just like in shopping malls).
 * - Customer cannot alter the amount; they simply tap Pay and enter PIN.
 */
public class UpiQrDialog extends JDialog {

    private String upiId;
    private String storeName;
    private final double amount;
    private final String invoiceNo;
    private String upiUri;

    private BufferedImage qrImage;
    private boolean paymentConfirmed = false;

    // UI elements to update dynamically
    private JLabel lblStore;
    private JLabel lblVpa;
    private JLabel lblStatusBanner;
    private JPanel statusBannerPanel;
    private JPanel qrCanvas;

    public UpiQrDialog(Frame owner, String upiId, String storeName, double amount, String invoiceNo) {
        super(owner, "Mall-Style Dynamic UPI Payment - " + invoiceNo, true);

        // Load stored settings or prompt if unset
        String savedUpi = AppSettings.getString(AppSettings.KEY_STORE_UPI_ID, "").trim();
        if (!savedUpi.isEmpty()) {
            this.upiId = savedUpi;
        } else if (upiId != null && !upiId.trim().isEmpty() && !"bazaarpoint@upi".equalsIgnoreCase(upiId)) {
            this.upiId = upiId.trim();
        } else {
            this.upiId = "bazaarpoint@upi";
        }

        String savedStore = AppSettings.getString(AppSettings.KEY_STORE_NAME, "SmartBilling Supermarket").trim();
        this.storeName = savedStore.isEmpty() ? ((storeName == null || storeName.trim().isEmpty()) ? "SmartBilling Store" : storeName.trim()) : savedStore;

        this.amount = amount;
        this.invoiceNo = (invoiceNo == null || invoiceNo.trim().isEmpty()) ? "INV-" + System.currentTimeMillis() : invoiceNo.trim();

        // Build NPCI Intent URI & Generate Real QR Code
        regenerateQrData();

        setSize(440, 670);
        setLocationRelativeTo(owner);
        setResizable(false);
        initComponents();

        // If still on placeholder demo ID, prompt merchant right away so payments reach THEIR account!
        if (isDemoId()) {
            SwingUtilities.invokeLater(this::promptChangeUpiDialog);
        }
    }

    private void regenerateQrData() {
        this.upiUri = QrCodeGenerator.buildUpiUri(this.upiId, this.storeName, this.amount, this.invoiceNo);
        this.qrImage = QrCodeGenerator.generateQrImage(this.upiUri, 260);
    }

    public boolean isPaymentConfirmed() {
        return paymentConfirmed;
    }

    public String getUpiUri() {
        return upiUri;
    }

    public String getUpiId() {
        return upiId;
    }

    private boolean isDemoId() {
        return upiId == null || upiId.trim().isEmpty() || "bazaarpoint@upi".equalsIgnoreCase(upiId);
    }

    private void initComponents() {
        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setBackground(Color.WHITE);
        main.setBorder(new EmptyBorder(14, 18, 14, 18));

        // 1. HEADER: Store Name & Mall POS Subtitle
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Color.WHITE);

        lblStore = new JLabel(storeName, SwingConstants.CENTER);
        lblStore.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblStore.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblStore.setForeground(new Color(15, 23, 42));

        JLabel lblScan = new JLabel("📱 Scan with Any UPI App to Pay", SwingConstants.CENTER);
        lblScan.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblScan.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblScan.setForeground(new Color(71, 85, 105));

        headerPanel.add(lblStore);
        headerPanel.add(Box.createVerticalStrut(2));
        headerPanel.add(lblScan);
        headerPanel.add(Box.createVerticalStrut(6));

        // Account status banner indicating whose account gets the money
        statusBannerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 3));
        lblStatusBanner = new JLabel();
        lblStatusBanner.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusBannerPanel.add(lblStatusBanner);
        updateStatusBannerStyle();

        statusBannerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusBannerPanel.setMaximumSize(new Dimension(400, 28));
        headerPanel.add(statusBannerPanel);

        main.add(headerPanel, BorderLayout.NORTH);

        // 2. CENTER PANEL: Dynamic Amount Card & QR Canvas
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.WHITE);

        // Amount Display Pill / Banner
        JPanel amountBanner = new JPanel(new BorderLayout(8, 0));
        amountBanner.setBackground(new Color(240, 253, 244)); // soft emerald
        amountBanner.setBorder(new CompoundBorder(
                new LineBorder(new Color(187, 247, 208), 1, true),
                new EmptyBorder(6, 14, 6, 14)
        ));
        amountBanner.setMaximumSize(new Dimension(390, 48));
        amountBanner.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblPayTitle = new JLabel("Exact Bill Total:");
        lblPayTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblPayTitle.setForeground(new Color(22, 101, 52));

        JLabel lblPayAmount = new JLabel(String.format(Locale.US, "₹ %.2f", amount));
        lblPayAmount.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblPayAmount.setForeground(new Color(21, 128, 61));

        amountBanner.add(lblPayTitle, BorderLayout.WEST);
        amountBanner.add(lblPayAmount, BorderLayout.EAST);
        centerPanel.add(amountBanner);
        centerPanel.add(Box.createVerticalStrut(8));

        // Real QR Code Display Canvas
        qrCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int w = getWidth();
                int h = getHeight();
                int boxSize = 240;
                int bx = (w - boxSize) / 2;
                int by = (h - boxSize) / 2;

                // Outer Card Box with rounded border
                g2.setColor(new Color(248, 250, 252));
                g2.fillRoundRect(bx - 10, by - 10, boxSize + 20, boxSize + 20, 16, 16);
                g2.setColor(new Color(226, 232, 240));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(bx - 10, by - 10, boxSize + 20, boxSize + 20, 16, 16);

                // Render Real QR Code Image
                if (qrImage != null) {
                    g2.drawImage(qrImage, bx, by, boxSize, boxSize, null);
                } else {
                    g2.setColor(Color.RED);
                    g2.drawString("Failed to generate QR Code", bx + 30, by + 120);
                }

                // Center UPI Mini Badge
                int badgeW = 46;
                int badgeH = 24;
                int badgeX = bx + (boxSize - badgeW) / 2;
                int badgeY = by + (boxSize - badgeH) / 2;

                g2.setColor(Color.WHITE);
                g2.fillRoundRect(badgeX - 2, badgeY - 2, badgeW + 4, badgeH + 4, 8, 8);
                g2.setColor(new Color(79, 70, 229)); // Indigo badge
                g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                FontMetrics fm = g2.getFontMetrics();
                int tx = badgeX + (badgeW - fm.stringWidth("UPI")) / 2;
                int ty = badgeY + ((badgeH - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString("UPI", tx, ty);

                g2.dispose();
            }
        };
        qrCanvas.setPreferredSize(new Dimension(390, 260));
        qrCanvas.setMaximumSize(new Dimension(390, 260));
        qrCanvas.setBackground(Color.WHITE);
        qrCanvas.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(qrCanvas);

        // Mall-style locked amount badge
        JPanel dynamicBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));
        dynamicBadge.setBackground(new Color(238, 242, 255));
        dynamicBadge.setBorder(new CompoundBorder(
                new LineBorder(new Color(199, 210, 254), 1, true),
                new EmptyBorder(2, 8, 2, 8)
        ));
        dynamicBadge.setMaximumSize(new Dimension(390, 26));
        dynamicBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDynamic = new JLabel("🔒 Mall POS: Exact bill amount of " + String.format(Locale.US, "₹%.2f", amount) + " is auto-filled & locked.");
        lblDynamic.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblDynamic.setForeground(new Color(67, 56, 202));
        dynamicBadge.add(lblDynamic);

        centerPanel.add(dynamicBadge);
        centerPanel.add(Box.createVerticalStrut(4));

        // Supported Apps
        JLabel lblSupported = new JLabel("GPay • PhonePe • Paytm • BHIM • Cred • Any UPI App", SwingConstants.CENTER);
        lblSupported.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSupported.setForeground(new Color(100, 116, 139));
        lblSupported.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(lblSupported);

        main.add(centerPanel, BorderLayout.CENTER);

        // 3. BOTTOM PANEL: VPA, Change UPI ID button, and Confirmation
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBackground(Color.WHITE);

        // VPA & Invoice detail line with Change UPI ID button
        JPanel detailsPanel = new JPanel(new BorderLayout(6, 2));
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(new EmptyBorder(4, 4, 6, 4));

        JPanel vpaInfoPanel = new JPanel(new GridLayout(2, 1, 0, 1));
        vpaInfoPanel.setBackground(Color.WHITE);

        lblVpa = new JLabel("My UPI: " + upiId);
        lblVpa.setFont(new Font("Monospaced", Font.BOLD, 12));
        lblVpa.setForeground(new Color(30, 41, 59));

        JLabel lblRef = new JLabel("Invoice: " + invoiceNo);
        lblRef.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblRef.setForeground(new Color(100, 116, 139));

        vpaInfoPanel.add(lblVpa);
        vpaInfoPanel.add(lblRef);
        detailsPanel.add(vpaInfoPanel, BorderLayout.CENTER);

        JButton btnChangeUpi = new JButton("✏️ Set My UPI ID");
        btnChangeUpi.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnChangeUpi.setBackground(new Color(238, 242, 255));
        btnChangeUpi.setForeground(new Color(79, 70, 229));
        btnChangeUpi.setFocusPainted(false);
        btnChangeUpi.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnChangeUpi.addActionListener(e -> promptChangeUpiDialog());
        detailsPanel.add(btnChangeUpi, BorderLayout.EAST);

        footer.add(detailsPanel);
        footer.add(Box.createVerticalStrut(4));

        // Buttons Bar
        JPanel btnBar = new JPanel(new GridLayout(1, 2, 8, 0));
        btnBar.setBackground(Color.WHITE);
        btnBar.setMaximumSize(new Dimension(390, 40));

        JButton btnCopy = new JButton("📋 Copy Link");
        btnCopy.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCopy.setBackground(new Color(241, 245, 249));
        btnCopy.setForeground(new Color(51, 65, 85));
        btnCopy.setFocusPainted(false);
        btnCopy.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCopy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(upiUri), null);
            JOptionPane.showMessageDialog(this, "UPI URI copied to clipboard:\n" + upiUri, "Copied", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnDone = new JButton("✓ Payment Received");
        btnDone.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDone.setBackground(new Color(16, 185, 129)); // Emerald green
        btnDone.setForeground(Color.WHITE);
        btnDone.setFocusPainted(false);
        btnDone.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDone.addActionListener(e -> {
            paymentConfirmed = true;
            dispose();
        });

        btnBar.add(btnCopy);
        btnBar.add(btnDone);
        footer.add(btnBar);
        footer.add(Box.createVerticalStrut(2));

        main.add(footer, BorderLayout.SOUTH);
        add(main);
    }

    private void updateStatusBannerStyle() {
        if (isDemoId()) {
            statusBannerPanel.setBackground(new Color(254, 243, 199)); // warm yellow
            statusBannerPanel.setBorder(new CompoundBorder(
                    new LineBorder(new Color(252, 211, 77), 1, true),
                    new EmptyBorder(2, 8, 2, 8)
            ));
            lblStatusBanner.setText("⚠️ Demo ID active. Click [✏️ Set My UPI ID] so money reaches YOUR account.");
            lblStatusBanner.setForeground(new Color(180, 83, 9));
        } else {
            statusBannerPanel.setBackground(new Color(236, 253, 245)); // soft mint green
            statusBannerPanel.setBorder(new CompoundBorder(
                    new LineBorder(new Color(167, 243, 208), 1, true),
                    new EmptyBorder(2, 8, 2, 8)
            ));
            lblStatusBanner.setText("✓ Customer payments will go directly to your account: " + upiId);
            lblStatusBanner.setForeground(new Color(6, 95, 70));
        }
    }

    /**
     * Dialog to configure the merchant's own UPI ID and store name.
     * Changes are saved permanently to app_settings.properties and the QR code is immediately regenerated.
     */
    public void promptChangeUpiDialog() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 4, 6));
        panel.setPreferredSize(new Dimension(360, 150));

        panel.add(new JLabel("Apna UPI ID / VPA daalein (PhonePe/GPay/Paytm number ya ID):"));
        JTextField txtUpi = new JTextField(isDemoId() ? "" : upiId);
        panel.add(txtUpi);

        panel.add(new JLabel("Apni Dukan / Store ka Naam:"));
        JTextField txtName = new JTextField(storeName);
        panel.add(txtName);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "🏬 Link Your Own Bank Account / UPI ID",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String newUpi = txtUpi.getText().trim();
            String newName = txtName.getText().trim();

            if (newUpi.isEmpty()) {
                JOptionPane.showMessageDialog(this, "UPI ID cannot be empty! Please enter your PhonePe/GPay/Paytm UPI ID.", "Empty UPI ID", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!newUpi.contains("@")) {
                JOptionPane.showMessageDialog(this,
                        "Invalid UPI ID format! Must include '@' symbol.\nExamples:\n- 9876543210@paytm\n- 9876543210@ybl (PhonePe)\n- yourname@okaxis (Google Pay)\n- shop@icici",
                        "Invalid UPI ID Format", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (newName.isEmpty()) {
                newName = "SmartBilling Store";
            }

            // Save to persistent app settings permanently
            this.upiId = newUpi;
            this.storeName = newName;
            AppSettings.setString(AppSettings.KEY_STORE_UPI_ID, newUpi);
            AppSettings.setString(AppSettings.KEY_STORE_NAME, newName);
            AppSettings.saveSettings();

            // Regenerate QR data immediately
            regenerateQrData();

            // Refresh UI
            lblStore.setText(this.storeName);
            lblVpa.setText("My UPI: " + this.upiId);
            updateStatusBannerStyle();
            qrCanvas.repaint();

            JOptionPane.showMessageDialog(this,
                    "✓ Account Successfully Linked!\n\n" +
                    "UPI ID: " + newUpi + "\n" +
                    "Store Name: " + newName + "\n\n" +
                    "Ab koi bhi customer scan karega toh paise seedhe aapke is account me aayenge,\n" +
                    "aur customer ke phone par sirf exact bill amount (" + String.format(Locale.US, "₹%.2f", amount) + ") auto-fill rahega (jaise mall me hota hai).",
                    "UPI Account Linked",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }
}
