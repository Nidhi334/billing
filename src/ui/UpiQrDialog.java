package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class UpiQrDialog extends JDialog {
    public UpiQrDialog(Frame owner, String upiId, String storeName, double amount, String invoiceNo) {
        super(owner, "UPI QR Payment", true);
        setSize(380, 480);
        setLocationRelativeTo(owner);
        setResizable(false);
        initComponents(upiId, storeName, amount, invoiceNo);
    }

    private void initComponents(String upiId, String storeName, double amount, String invoiceNo) {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(Color.WHITE);
        main.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JPanel header = new JPanel(new GridLayout(2, 1, 2, 2));
        header.setBackground(Color.WHITE);
        JLabel title = new JLabel("📱 Scan & Pay with any UPI App", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(new Color(30, 41, 59));
        JLabel subtitle = new JLabel("GPay / PhonePe / Paytm / BHIM", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(100, 116, 139));
        header.add(title);
        header.add(subtitle);
        main.add(header, BorderLayout.NORTH);

        // QR Canvas Simulation
        JPanel qrCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int size = 180;
                int x = (w - size) / 2;
                int y = (h - size) / 2 - 10;

                // Border box
                g2.setColor(new Color(241, 245, 249));
                g2.fillRoundRect(x - 10, y - 10, size + 20, size + 20, 15, 15);
                g2.setColor(new Color(203, 213, 225));
                g2.drawRoundRect(x - 10, y - 10, size + 20, size + 20, 15, 15);

                // Draw mock high-tech QR blocks pattern
                g2.setColor(Color.BLACK);
                // Corner markers (top-left, top-right, bottom-left)
                drawFinderPattern(g2, x + 10, y + 10);
                drawFinderPattern(g2, x + size - 50, y + 10);
                drawFinderPattern(g2, x + 10, y + size - 50);

                // Pixel pattern matrix simulation
                int seed = (int) (amount * 100) + 12345;
                java.util.Random rnd = new java.util.Random(seed);
                int cellSize = 6;
                for (int r = 0; r < 20; r++) {
                    for (int c = 0; c < 20; c++) {
                        // skip corners
                        if ((r < 7 && c < 7) || (r < 7 && c > 12) || (r > 12 && c < 7)) continue;
                        if (rnd.nextBoolean()) {
                            g2.fillRect(x + 30 + (c * cellSize), y + 30 + (r * cellSize), cellSize - 1, cellSize - 1);
                        }
                    }
                }

                // Center logo
                g2.setColor(new Color(124, 58, 237));
                g2.fillRoundRect(w / 2 - 16, y + size / 2 - 16, 32, 32, 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.drawString("UPI", w / 2 - 11, y + size / 2 + 5);

                // Amount text below QR
                g2.setColor(new Color(15, 23, 42));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                String amtStr = String.format("Pay: ₹%.2f", amount);
                int strW = g2.getFontMetrics().stringWidth(amtStr);
                g2.drawString(amtStr, (w - strW) / 2, y + size + 25);
            }

            private void drawFinderPattern(Graphics2D g2, int fx, int fy) {
                g2.fillRect(fx, fy, 40, 40);
                g2.setColor(Color.WHITE);
                g2.fillRect(fx + 6, fy + 6, 28, 28);
                g2.setColor(Color.BLACK);
                g2.fillRect(fx + 12, fy + 12, 16, 16);
            }
        };
        qrCanvas.setBackground(Color.WHITE);
        main.add(qrCanvas, BorderLayout.CENTER);

        // Footer details & Done button
        JPanel footer = new JPanel(new BorderLayout(5, 5));
        footer.setBackground(Color.WHITE);

        JLabel upiLabel = new JLabel("VPA: " + upiId + " | Ref: " + invoiceNo, SwingConstants.CENTER);
        upiLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        upiLabel.setForeground(new Color(71, 85, 105));
        footer.add(upiLabel, BorderLayout.NORTH);

        JButton btnDone = new JButton("✓ Payment Received / Confirm");
        btnDone.setBackground(new Color(16, 185, 129));
        btnDone.setForeground(Color.WHITE);
        btnDone.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDone.setPreferredSize(new Dimension(0, 38));
        btnDone.setFocusPainted(false);
        btnDone.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDone.addActionListener(e -> dispose());
        footer.add(btnDone, BorderLayout.SOUTH);

        main.add(footer, BorderLayout.SOUTH);
        add(main);
    }
}

