package ui;

import model.Sale;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Modern High-Impact Payment Success Modal.
 * Appears upon successful completion of payment (UPI / Cash / Card).
 * Shows amount, user/customer details, payment mode, and "PAID" status.
 */
public class PaymentSuccessDialog extends JDialog {

    private final Sale sale;
    private final String recipientAccount;

    public PaymentSuccessDialog(Frame owner, Sale sale, String recipientAccount) {
        super(owner, "✓ Payment Successful - " + sale.getInvoiceNo(), true);
        this.sale = sale;
        this.recipientAccount = (recipientAccount != null && !recipientAccount.trim().isEmpty()) ? recipientAccount.trim() : "Store Account";

        setSize(420, 520);
        setLocationRelativeTo(owner);
        setResizable(false);
        initComponents();

        // Sound alert for confirmation
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Exception ignored) {}
    }

    private void initComponents() {
        JPanel main = new JPanel(new BorderLayout(0, 14));
        main.setBackground(Color.WHITE);
        main.setBorder(new EmptyBorder(22, 24, 20, 24));

        // 1. TOP: Success Icon & Header
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(Color.WHITE);

        JLabel lblBadge = new JLabel("✓", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(16, 185, 129)); // Emerald Green Circle
                g2.fillOval(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
                g2.dispose();
            }
        };
        lblBadge.setPreferredSize(new Dimension(56, 56));
        lblBadge.setMaximumSize(new Dimension(56, 56));
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 32));
        lblBadge.setForeground(Color.WHITE);
        lblBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitle = new JLabel("PAYMENT RECEIVED SUCCESSFULLY!", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(new Color(6, 95, 70));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Payment verified and recorded in sales history", SwingConstants.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(100, 116, 139));
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(lblBadge);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(lblTitle);
        topPanel.add(Box.createVerticalStrut(3));
        topPanel.add(lblSub);
        main.add(topPanel, BorderLayout.NORTH);

        // 2. CENTER: Amount Card & Transaction Breakdown
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.WHITE);

        // Big Green Amount Box
        JPanel amountCard = new JPanel(new GridLayout(2, 1, 0, 2));
        amountCard.setBackground(new Color(236, 253, 245));
        amountCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(167, 243, 208), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));
        amountCard.setMaximumSize(new Dimension(370, 70));

        JLabel lblAmt = new JLabel(String.format(Locale.US, "₹ %.2f", sale.getTotalAmount()), SwingConstants.CENTER);
        lblAmt.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblAmt.setForeground(new Color(5, 150, 105));

        String modeText = "UPI".equalsIgnoreCase(sale.getPaymentMode())
                ? "Paid via UPI to: " + recipientAccount
                : "Paid via " + sale.getPaymentMode();
        JLabel lblMode = new JLabel(modeText, SwingConstants.CENTER);
        lblMode.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblMode.setForeground(new Color(4, 120, 87));

        amountCard.add(lblAmt);
        amountCard.add(lblMode);
        centerPanel.add(amountCard);
        centerPanel.add(Box.createVerticalStrut(12));

        // Details Grid Card
        JPanel detailsCard = new JPanel(new GridLayout(5, 2, 8, 8));
        detailsCard.setBackground(new Color(248, 250, 252));
        detailsCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));
        detailsCard.setMaximumSize(new Dimension(370, 150));

        addDetailRow(detailsCard, "Invoice Ref:", sale.getInvoiceNo(), true);
        String cust = (sale.getCustomerName() != null && !sale.getCustomerName().trim().isEmpty())
                ? sale.getCustomerName() : "Walk-in Customer";
        addDetailRow(detailsCard, "Customer / User:", cust, true);
        addDetailRow(detailsCard, "Payment Mode:", sale.getPaymentMode(), false);
        addDetailRow(detailsCard, "Payment Status:", "✓ PAID & CONFIRMED", true, new Color(5, 150, 105));
        addDetailRow(detailsCard, "Date & Time:", new SimpleDateFormat("dd MMM yyyy, hh:mm a").format(new Date()), false);

        centerPanel.add(detailsCard);
        main.add(centerPanel, BorderLayout.CENTER);

        // 3. BOTTOM: Action Button
        JPanel bottomPanel = new JPanel(new GridLayout(1, 1, 0, 0));
        bottomPanel.setBackground(Color.WHITE);

        JButton btnOk = new JButton("🖨️ View / Print Invoice & Next Bill (Enter)");
        btnOk.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnOk.setBackground(new Color(16, 185, 129));
        btnOk.setForeground(Color.WHITE);
        btnOk.setPreferredSize(new Dimension(0, 42));
        btnOk.setFocusPainted(false);
        btnOk.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOk.addActionListener(e -> dispose());
        bottomPanel.add(btnOk);

        // Enter key to close
        getRootPane().setDefaultButton(btnOk);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        main.add(bottomPanel, BorderLayout.SOUTH);
        add(main);
    }

    private void addDetailRow(JPanel p, String title, String val, boolean bold) {
        addDetailRow(p, title, val, bold, new Color(30, 41, 59));
    }

    private void addDetailRow(JPanel p, String title, String val, boolean bold, Color valColor) {
        JLabel lblT = new JLabel(title);
        lblT.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblT.setForeground(new Color(100, 116, 139));

        JLabel lblV = new JLabel(val);
        lblV.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, 12));
        lblV.setForeground(valColor);

        p.add(lblT);
        p.add(lblV);
    }
}
