package ui;

import model.Product;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.File;

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
     * Generates a complete retail barcode label card including product name, price, and barcode.
     */
    public static BufferedImage generateProductBarcodeCard(Product p, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Pure white background with subtle border
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        g2.setColor(new Color(226, 235, 230));
        g2.drawRect(0, 0, width - 1, height - 1);

        String bcode = (p.getBarcode() != null && !p.getBarcode().trim().isEmpty()) ? p.getBarcode().trim() : p.getCode();

        // 1. Header: Product Name + Price
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setColor(new Color(15, 23, 42)); // Slate 900
        FontMetrics fm = g2.getFontMetrics();

        String name = p.getName();
        if (fm.stringWidth(name) > width - 120) {
            while (name.length() > 3 && fm.stringWidth(name + "...") > width - 120) {
                name = name.substring(0, name.length() - 1);
            }
            name += "...";
        }
        g2.drawString(name, 14, 22);

        String priceStr = String.format("₹%.2f", p.getSellingPrice());
        g2.setColor(new Color(0, 90, 61)); // Forest Green
        int pw = fm.stringWidth(priceStr);
        g2.drawString(priceStr, width - 14 - pw, 22);

        // Divider
        g2.setColor(new Color(241, 245, 249));
        g2.drawLine(14, 30, width - 14, 30);

        // 2. Barcode Graphic
        int barcodeH = height - 68;
        int barcodeW = width - 28;
        BufferedImage rawBarcode = generateBarcodeImage(bcode, barcodeW, barcodeH, false);
        g2.drawImage(rawBarcode, 14, 34, null);

        // 3. Human-readable Barcode & SKU
        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        g2.setColor(new Color(30, 41, 59));
        FontMetrics fmCode = g2.getFontMetrics();
        int cw = fmCode.stringWidth(bcode);
        g2.drawString(bcode, (width - cw) / 2, height - 16);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g2.setColor(new Color(100, 116, 139));
        g2.drawString("SKU: " + p.getCode(), 14, height - 16);

        g2.dispose();
        return img;
    }

    /**
     * Prompts the user to save a single product's barcode as a PNG image.
     */
    public static File saveBarcodeImage(Component parent, Product p) {
        if (p == null) return null;
        String bcode = (p.getBarcode() != null && !p.getBarcode().trim().isEmpty()) ? p.getBarcode().trim() : p.getCode();
        String defaultFileName = "barcode_" + p.getCode() + "_" + bcode + ".png";

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Product Barcode as PNG");
        chooser.setSelectedFile(new File(defaultFileName));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Images (*.png)", "png"));

        int res = chooser.showSaveDialog(parent);
        if (res == JFileChooser.APPROVE_OPTION) {
            File dest = chooser.getSelectedFile();
            if (!dest.getName().toLowerCase().endsWith(".png")) {
                dest = new File(dest.getParentFile(), dest.getName() + ".png");
            }
            try {
                BufferedImage img = generateProductBarcodeCard(p, 440, 180);
                javax.imageio.ImageIO.write(img, "PNG", dest);

                int opt = JOptionPane.showConfirmDialog(parent,
                        "Barcode successfully saved to:\n" + dest.getAbsolutePath() +
                        "\n\nWould you like to open this barcode image now?",
                        "Barcode Saved", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (opt == JOptionPane.YES_OPTION) {
                    Desktop.getDesktop().open(dest);
                }
                return dest;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Failed to save barcode: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return null;
    }

    /**
     * Exports all product barcode PNG images to a barcodes/ folder and generates a combined sheet.
     */
    public static void exportAllBarcodes(Component parent, java.util.List<Product> products) {
        if (products == null || products.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No products available to export.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        File targetDir = new File("barcodes");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        int count = 0;
        for (Product p : products) {
            try {
                String bcode = (p.getBarcode() != null && !p.getBarcode().trim().isEmpty()) ? p.getBarcode().trim() : p.getCode();
                File dest = new File(targetDir, p.getCode() + "_" + bcode + ".png");
                BufferedImage img = generateProductBarcodeCard(p, 440, 180);
                javax.imageio.ImageIO.write(img, "PNG", dest);
                count++;
            } catch (Exception ignored) {}
        }

        // Generate combined catalog sheet containing all product barcodes
        File sheetFile = new File(targetDir, "00_all_barcodes_sheet.png");
        try {
            generateBarcodeSheet(products, sheetFile);
        } catch (Exception ignored) {}

        if (parent != null) {
            int opt = JOptionPane.showConfirmDialog(parent,
                    "Successfully exported " + count + " barcode images to:\n" + targetDir.getAbsolutePath() +
                    "\n\nIncludes individual PNGs plus '00_all_barcodes_sheet.png' for quick scanning.\n\nOpen barcodes folder now?",
                    "All Barcodes Exported", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (opt == JOptionPane.YES_OPTION) {
                try {
                    Desktop.getDesktop().open(targetDir);
                } catch (Exception ignored) {}
            }
        } else {
            System.out.println("Successfully exported " + count + " barcodes to: " + targetDir.getAbsolutePath());
        }
    }

    /**
     * Generates a combined multi-card sheet with all product barcodes laid out in a grid.
     */
    public static void generateBarcodeSheet(java.util.List<Product> products, File targetFile) throws java.io.IOException {
        if (products == null || products.isEmpty()) return;

        int cols = 2;
        int rows = (int) Math.ceil((double) products.size() / cols);
        int cardW = 380;
        int cardH = 150;
        int padding = 16;
        int headerH = 70;

        int sheetW = (cardW * cols) + (padding * (cols + 1));
        int sheetH = headerH + (cardH * rows) + (padding * (rows + 1));

        BufferedImage sheet = new BufferedImage(sheetW, sheetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = sheet.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background
        g2.setColor(new Color(248, 250, 252));
        g2.fillRect(0, 0, sheetW, sheetH);

        // Header Banner
        g2.setColor(new Color(0, 90, 61)); // Forest Green
        g2.fillRect(0, 0, sheetW, headerH);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2.drawString("Product Barcode Catalog Sheet", padding, 32);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(new Color(220, 240, 230));
        g2.drawString("Point camera scanner at any barcode below to instantly add to bill (" + products.size() + " Products)", padding, 52);

        // Render each card
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            int col = i % cols;
            int row = i / cols;

            int x = padding + col * (cardW + padding);
            int y = headerH + padding + row * (cardH + padding);

            BufferedImage card = generateProductBarcodeCard(p, cardW, cardH);
            g2.drawImage(card, x, y, null);
        }

        g2.dispose();
        javax.imageio.ImageIO.write(sheet, "PNG", targetFile);
    }

    /**
     * Opens an interactive barcode modal dialog for viewing, downloading, and printing.
     */
    public static void showBarcodeDialog(Frame owner, Product p) {
        if (p == null) return;
        String barcodeVal = p.getBarcode();
        if (barcodeVal == null || barcodeVal.trim().isEmpty()) {
            barcodeVal = p.getCode();
        }

        JDialog dlg = new JDialog(owner, "Product Barcode - " + p.getName(), true);
        dlg.setSize(540, 420);
        dlg.setLocationRelativeTo(owner);
        dlg.setResizable(false);

        JPanel content = new JPanel(new BorderLayout(14, 14));
        content.setBackground(new Color(248, 250, 252));
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header info
        JPanel topInfo = new JPanel();
        topInfo.setLayout(new BoxLayout(topInfo, BoxLayout.Y_AXIS));
        topInfo.setOpaque(false);

        JLabel lblName = new JLabel(p.getName(), SwingConstants.CENTER);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblName.setForeground(new Color(15, 23, 42));
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblMeta = new JLabel("SKU: " + p.getCode() + "   •   Price: ₹" + String.format("%.2f", p.getSellingPrice()), SwingConstants.CENTER);
        lblMeta.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblMeta.setForeground(new Color(100, 116, 139));
        lblMeta.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblCodeVal = new JLabel("Barcode: " + barcodeVal, SwingConstants.CENTER);
        lblCodeVal.setFont(new Font("Monospaced", Font.BOLD, 14));
        lblCodeVal.setForeground(new Color(0, 90, 61));
        lblCodeVal.setAlignmentX(Component.CENTER_ALIGNMENT);

        topInfo.add(lblName);
        topInfo.add(Box.createVerticalStrut(4));
        topInfo.add(lblMeta);
        topInfo.add(Box.createVerticalStrut(4));
        topInfo.add(lblCodeVal);
        content.add(topInfo, BorderLayout.NORTH);

        // Barcode Preview Image
        final BufferedImage barcodeImg = generateBarcodeImage(barcodeVal, 480, 140, true);
        JLabel lblBarcodeImg = new JLabel(new ImageIcon(barcodeImg), SwingConstants.CENTER);
        lblBarcodeImg.setBackground(Color.WHITE);
        lblBarcodeImg.setOpaque(true);
        lblBarcodeImg.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 235, 230), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        content.add(lblBarcodeImg, BorderLayout.CENTER);

        // Action Buttons: View Box, Save/Download PNG, Print Label, Close
        JPanel bottomBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomBtns.setOpaque(false);

        JButton btnViewBox = new JButton("📦 View Package Box");
        btnViewBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnViewBox.setBackground(new Color(241, 245, 249));
        btnViewBox.setForeground(new Color(51, 65, 85));
        btnViewBox.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnViewBox.addActionListener(e -> util.PackageBoxGenerator.showBoxDialog(owner, p));

        JButton btnSave = new JButton("⬇ Save PNG");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSave.setBackground(new Color(0, 90, 61));
        btnSave.setForeground(Color.WHITE);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> saveBarcodeImage(dlg, p));

        JButton btnPrint = new JButton("🖨️ Print");
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPrint.setBackground(new Color(240, 248, 244));
        btnPrint.setForeground(new Color(0, 90, 61));
        btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrint.addActionListener(e -> printBarcodeLabel(p, barcodeImg, dlg));

        JButton btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dlg.dispose());

        bottomBtns.add(btnViewBox);
        bottomBtns.add(btnSave);
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

    public static void main(String[] args) {
        System.out.println("Generating barcode images for products...");
        java.util.List<Product> products = new java.util.ArrayList<>();
        try {
            products = new dao.ProductDAO().getAllProducts();
        } catch (Exception ex) {
            System.err.println("Could not load from DB, using default seed list: " + ex.getMessage());
        }

        if (products.isEmpty()) {
            products.add(new Product(1, "PRD-001", "8901030383124", "Dell Inspiron 15 Laptop", 1, 42000.00, 50000.00, 15, 3));
            products.add(new Product(2, "PRD-002", "8901499028145", "Logitech B170 Wireless Mouse", 2, 450.00, 799.00, 35, 5));
            products.add(new Product(3, "PRD-003", "8901262010051", "Mechanical Gaming Keyboard RGB", 2, 1200.00, 1899.00, 20, 4));
            products.add(new Product(4, "PRD-004", "8901058852336", "Samsung 24-inch FHD Monitor", 1, 7500.00, 9499.00, 12, 3));
            products.add(new Product(5, "PRD-005", "8901030584910", "Boat Rockerz 450 Bluetooth Headphones", 2, 850.00, 1499.00, 28, 5));
            products.add(new Product(6, "PRD-006", "8901725131013", "JK Copier A4 Paper Ream (500 Sheets)", 3, 210.00, 340.00, 60, 10));
            products.add(new Product(7, "PRD-007", "8901324021038", "Parker Classic Gold Ballpoint Pen", 3, 220.00, 350.00, 40, 8));
            products.add(new Product(8, "PRD-008", "8902579100142", "SanDisk 128GB Ultra USB 3.0 Flash Drive", 4, 480.00, 799.00, 45, 6));
            products.add(new Product(9, "PRD-009", "8901030822197", "Portronics 6-in-1 USB-C Hub Adapter", 2, 950.00, 1599.00, 18, 4));
            products.add(new Product(10, "PRD-010", "8901058002151", "Classmate Pulse Spiral Hardbound Notebook", 3, 110.00, 195.00, 50, 10));
        }

        exportAllBarcodes(null, products);
        System.out.println("Done! Generated barcodes in barcodes/ directory.");
    }
}
