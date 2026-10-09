package ui;

import model.Product;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;

public class BarcodeUtil {

    // Code 128B patterns (107 patterns, each 11 modules wide, stop pattern is 13 modules wide)
    private static final String[] CODE128_PATTERNS = {
        "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213", // 0-9
        "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132", // 10-19
        "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211", // 20-29
        "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313", // 30-39
        "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331", // 40-49
        "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111", // 50-59
        "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214", // 60-69
        "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111", // 70-79
        "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141", // 80-89
        "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141", // 90-99
        "114131", "311141", "411131", "211412", "211214", "211232", "2331112" // 100-106 (104=Start B: 211214, 106=Stop: 2331112)
    };

    /**
     * Encodes a string into a standard Code-128B barcode image.
     */
    public static BufferedImage generateBarcodeImage(String text, int width, int height, boolean showText) {
        if (text == null || text.trim().isEmpty()) {
            text = "00000000";
        }
        text = text.trim();

        // Calculate Code 128B symbols
        java.util.List<Integer> symbols = new java.util.ArrayList<>();
        symbols.add(104); // Start Code B

        int checksum = 104;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int val = (c >= 32 && c <= 126) ? (c - 32) : 0;
            symbols.add(val);
            checksum += val * (i + 1);
        }

        checksum %= 103;
        symbols.add(checksum);
        symbols.add(106); // Stop symbol

        // Convert symbols to black/white module pattern string
        StringBuilder modules = new StringBuilder();
        for (int sym : symbols) {
            String pattern = CODE128_PATTERNS[sym];
            boolean isBar = true;
            for (int k = 0; k < pattern.length(); k++) {
                int runLen = pattern.charAt(k) - '0';
                for (int r = 0; r < runLen; r++) {
                    modules.append(isBar ? '1' : '0');
                }
                isBar = !isBar;
            }
        }

        // Create Image
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fill background
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        int totalModules = modules.length();
        int quietZone = 12; // left/right margin
        int usableWidth = width - (quietZone * 2);
        double moduleWidth = (double) usableWidth / totalModules;

        int barHeight = showText ? (height - 24) : (height - 10);
        int topMargin = 6;

        g2.setColor(Color.BLACK);
        for (int i = 0; i < totalModules; i++) {
            if (modules.charAt(i) == '1') {
                int x1 = (int) Math.round(quietZone + (i * moduleWidth));
                int x2 = (int) Math.round(quietZone + ((i + 1) * moduleWidth));
                int w = Math.max(1, x2 - x1);
                g2.fillRect(x1, topMargin, w, barHeight);
            }
        }

        // Draw human-readable text at the bottom
        if (showText) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            int strW = fm.stringWidth(text);
            int tx = Math.max(0, (width - strW) / 2);
            int ty = height - 4;
            g2.drawString(text, tx, ty);
        }

        g2.dispose();
        return img;
    }

    /**
     * Opens an interactive barcode modal dialog for viewing and printing.
     */
    public static void showBarcodeDialog(Frame owner, Product p) {
        if (p == null) return;
        String barcodeVal = p.getBarcode();
        if (barcodeVal == null || barcodeVal.trim().isEmpty()) {
            barcodeVal = p.getCode();
        }

        JDialog dlg = new JDialog(owner, "🏷️ Product Barcode - " + p.getName(), true);
        dlg.setSize(440, 360);
        dlg.setLocationRelativeTo(owner);

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBackground(new Color(248, 250, 252));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Header info
        JPanel topInfo = new JPanel(new GridLayout(3, 1, 4, 4));
        topInfo.setOpaque(false);
        JLabel lblName = new JLabel(p.getName(), SwingConstants.CENTER);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblName.setForeground(new Color(30, 41, 59));

        JLabel lblMeta = new JLabel("Code: " + p.getCode() + "  |  Price: ₹" + String.format("%.2f", p.getSellingPrice()), SwingConstants.CENTER);
        lblMeta.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblMeta.setForeground(new Color(100, 116, 139));

        JLabel lblCodeVal = new JLabel("Barcode: " + barcodeVal, SwingConstants.CENTER);
        lblCodeVal.setFont(new Font("Monospaced", Font.BOLD, 14));
        lblCodeVal.setForeground(new Color(37, 99, 235));

        topInfo.add(lblName);
        topInfo.add(lblMeta);
        topInfo.add(lblCodeVal);
        content.add(topInfo, BorderLayout.NORTH);

        // Barcode Preview Image
        final BufferedImage barcodeImg = generateBarcodeImage(barcodeVal, 380, 120, true);
        JLabel lblBarcodeImg = new JLabel(new ImageIcon(barcodeImg), SwingConstants.CENTER);
        lblBarcodeImg.setBackground(Color.WHITE);
        lblBarcodeImg.setOpaque(true);
        lblBarcodeImg.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        content.add(lblBarcodeImg, BorderLayout.CENTER);

        // Print & Close Action Buttons
        JPanel bottomBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBtns.setOpaque(false);

        JButton btnPrint = new JButton("🖨️ Print Barcode Label");
        btnPrint.setBackground(new Color(37, 99, 235));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.addActionListener(e -> dlg.dispose());

        btnPrint.addActionListener(e -> printBarcodeLabel(p, barcodeImg, dlg));

        bottomBtns.add(btnPrint);
        bottomBtns.add(btnClose);
        content.add(bottomBtns, BorderLayout.SOUTH);

        dlg.add(content);
        dlg.setVisible(true);
    }

    /**
     * Standard PrinterJob implementation for 2-inch thermal sticker / label printer.
     */
    public static void printBarcodeLabel(Product p, BufferedImage barcodeImg, Component parent) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
                if (pageIndex > 0) return NO_SUCH_PAGE;

                Graphics2D g2 = (Graphics2D) graphics;
                g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                int cardW = 180;
                // Header
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString(p.getName(), 10, 15);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.drawString("Price: ₹" + String.format("%.2f", p.getSellingPrice()), 10, 28);

                // Draw image
                if (barcodeImg != null) {
                    g2.drawImage(barcodeImg, 10, 32, 160, 55, null);
                }

                g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
                g2.drawString(p.getBarcode(), 10, 96);

                return PAGE_EXISTS;
            }
        });

        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(parent, "Barcode label sent to printer!", "Print Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(parent, "Printing failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
