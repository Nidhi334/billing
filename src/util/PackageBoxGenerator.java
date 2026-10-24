package util;

import dao.ProductDAO;
import model.Product;
import ui.BarcodeUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates realistic retail package outer-box mockups with authentic,
 * high-contrast scannable barcode labels for checkout scanning testing.
 */
public class PackageBoxGenerator {

    public static final String PACKAGES_DIR = "packages";

    public static void generateAllPackageBoxes() {
        File dir = new File(PACKAGES_DIR);
        if (!dir.exists()) dir.mkdirs();

        List<Product> products = new ArrayList<>();
        try {
            products = new ProductDAO().getAllProducts();
        } catch (Exception ex) {
            System.err.println("DB load failed, using seed list: " + ex.getMessage());
        }

        if (products == null || products.isEmpty()) {
            products = getDefaultSeedProducts();
        }

        System.out.println("Generating retail packaging box images for " + products.size() + " products...");

        int idx = 1;
        for (Product p : products) {
            try {
                BufferedImage boxImg = generateSingleBoxImage(p);
                String safeName = p.getName().toLowerCase().replaceAll("[^a-z0-9]", "_").replaceAll("_+", "_");
                if (safeName.length() > 30) safeName = safeName.substring(0, 30);
                File out = new File(dir, String.format("%02d_%s_%s_box.png", idx, p.getCode(), safeName));
                ImageIO.write(boxImg, "PNG", out);
                System.out.println("  ✓ Saved: " + out.getName());
                idx++;
            } catch (Exception ex) {
                System.err.println("Failed generating box for " + p.getName() + ": " + ex.getMessage());
            }
        }

        // Generate combined sheet containing all 10 retail boxes
        try {
            BufferedImage sheet = generateCombinedBoxesSheet(products);
            File sheetFile = new File(dir, "00_all_product_boxes_sheet.png");
            ImageIO.write(sheet, "PNG", sheetFile);
            System.out.println("  ✓ Saved combined catalog sheet: " + sheetFile.getName());
        } catch (Exception ex) {
            System.err.println("Failed generating combined boxes sheet: " + ex.getMessage());
        }

        System.out.println("All retail packaging box mockups generated in " + dir.getAbsolutePath());
    }

    public static BufferedImage generateSingleBoxImage(Product p) {
        int w = 800, h = 800;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // 1. Studio Canvas Background (Light warm gray with subtle vignette)
        g2.setColor(new Color(243, 245, 248));
        g2.fillRect(0, 0, w, h);

        // Subtle gradient top bar
        g2.setPaint(new GradientPaint(0, 0, new Color(230, 236, 242), 0, 80, new Color(243, 245, 248)));
        g2.fillRect(0, 0, w, 80);

        // Top Store Watermark
        g2.setColor(new Color(100, 116, 139));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2.drawString("RETAIL PACKAGING • SMARTBILLING TEST SCANNER COMPATIBLE", 40, 42);

        g2.setColor(new Color(0, 90, 61));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        String catTag = (p.getCategoryName() != null ? p.getCategoryName().toUpperCase() : "GENERAL");
        FontMetrics fmCat = g2.getFontMetrics();
        g2.drawString(catTag, w - 40 - fmCat.stringWidth(catTag), 42);

        // 2. Retail Box Drop Shadow
        int boxX = 60, boxY = 75, boxW = 680, boxH = 680;
        for (int i = 0; i < 18; i++) {
            g2.setColor(new Color(15, 23, 42, 4 + (18 - i)));
            g2.fillRoundRect(boxX - i, boxY + 12 - i, boxW + i * 2, boxH + i * 2, 28 + i, 28 + i);
        }

        // 3. Retail Box Body (Cardboard / Matte Retail Outer Packaging)
        Color boxBg = getBoxColorForProduct(p);
        Color boxAccent = getAccentColorForProduct(p);

        g2.setColor(boxBg);
        g2.fillRoundRect(boxX, boxY, boxW, boxH, 24, 24);

        // Subtle outer border
        g2.setColor(new Color(0, 0, 0, 30));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(boxX, boxY, boxW, boxH, 24, 24);

        // 4. Retail Box Header Strip
        g2.setColor(boxAccent);
        g2.fillRoundRect(boxX, boxY, boxW, 76, 24, 24);
        g2.fillRect(boxX, boxY + 50, boxW, 26); // square bottom corners of header

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g2.drawString(p.getName(), boxX + 28, boxY + 46);

        // 5. Product Image on Box Front
        BufferedImage rawProd = ProductImageUtil.getProductRawImage(p);
        int photoBoxX = boxX + 28;
        int photoBoxY = boxY + 95;
        int photoBoxW = boxW - 56;
        int photoBoxH = 340;

        // Photo white inner frame
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(photoBoxX, photoBoxY, photoBoxW, photoBoxH, 16, 16);
        g2.setColor(new Color(226, 232, 240));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(photoBoxX, photoBoxY, photoBoxW, photoBoxH, 16, 16);

        if (rawProd != null) {
            int pw = rawProd.getWidth(), ph = rawProd.getHeight();
            if (pw > 0 && ph > 0) {
                int pad = 16;
                double scale = Math.min((double) (photoBoxW - pad * 2) / pw, (double) (photoBoxH - pad * 2) / ph);
                int dw = (int) Math.round(pw * scale);
                int dh = (int) Math.round(ph * scale);
                int dx = photoBoxX + (photoBoxW - dw) / 2;
                int dy = photoBoxY + (photoBoxH - dh) / 2;
                g2.drawImage(rawProd, dx, dy, dw, dh, null);
            }
        }

        // 6. Retail Specs & Info Row
        g2.setColor(new Color(71, 85, 105));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        String specs = "SKU: " + p.getCode() + "  •  ORIGINAL SEALED PACKAGING  •  WARRANTY INCLUDED";
        g2.drawString(specs, boxX + 32, boxY + 458);

        // 7. Outer Package Barcode Sticker (Ultra High Contrast, Guaranteed Scannable)
        int stickerX = boxX + 28;
        int stickerY = boxY + 475;
        int stickerW = boxW - 56;
        int stickerH = 180;

        // Sticker White Card with Shadow
        g2.setColor(new Color(0, 0, 0, 15));
        g2.fillRoundRect(stickerX + 2, stickerY + 4, stickerW, stickerH, 14, 14);

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(stickerX, stickerY, stickerW, stickerH, 12, 12);
        g2.setColor(new Color(203, 213, 225));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(stickerX, stickerY, stickerW, stickerH, 12, 12);

        // Sticker Header: Product Title & Price Tag
        g2.setColor(new Color(15, 23, 42));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
        g2.drawString(p.getName(), stickerX + 16, stickerY + 26);

        g2.setColor(new Color(0, 90, 61));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        String priceTag = String.format("M.R.P.: ₹%.2f", p.getSellingPrice());
        FontMetrics fmPrice = g2.getFontMetrics();
        g2.drawString(priceTag, stickerX + stickerW - 16 - fmPrice.stringWidth(priceTag), stickerY + 26);

        // High-contrast clean Code-128 Barcode Image
        String barcodeVal = (p.getBarcode() != null && !p.getBarcode().trim().isEmpty()) ? p.getBarcode().trim() : p.getCode();
        int bcW = stickerW - 40;
        int bcH = 88;
        BufferedImage barcodeImg = BarcodeUtil.generateBarcodeImage(barcodeVal, bcW, bcH, false);
        g2.drawImage(barcodeImg, stickerX + 20, stickerY + 38, null);

        // Human-Readable Digits below Barcode
        g2.setFont(new Font("Monospaced", Font.BOLD, 16));
        g2.setColor(Color.BLACK);
        FontMetrics fmCode = g2.getFontMetrics();
        int cw = fmCode.stringWidth(barcodeVal);
        g2.drawString(barcodeVal, stickerX + (stickerW - cw) / 2, stickerY + 146);

        // Footer helper note on sticker
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(new Color(100, 116, 139));
        g2.drawString("Point camera at barcode above to scan directly into bill", stickerX + 16, stickerY + 168);

        g2.dispose();
        return img;
    }

    public static BufferedImage generateCombinedBoxesSheet(List<Product> products) {
        int cols = 2;
        int rows = (int) Math.ceil((double) products.size() / cols);

        int cardW = 600;
        int cardH = 600;
        int padding = 20;
        int headerH = 90;

        int sheetW = (cardW * cols) + (padding * (cols + 1));
        int sheetH = headerH + (cardH * rows) + (padding * (rows + 1));

        BufferedImage sheet = new BufferedImage(sheetW, sheetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = sheet.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Background
        g2.setColor(new Color(241, 245, 249));
        g2.fillRect(0, 0, sheetW, sheetH);

        // Header Banner
        g2.setColor(new Color(0, 90, 61));
        g2.fillRect(0, 0, sheetW, headerH);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
        g2.drawString("Retail Product Packaging & Barcode Scanner Test Sheet", padding, 40);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        g2.setColor(new Color(220, 245, 235));
        g2.drawString("Hold this sheet or open on phone/monitor. Point camera scanner at any outer box barcode to instantly add to bill.", padding, 68);

        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            int col = i % cols;
            int row = i / cols;

            int x = padding + col * (cardW + padding);
            int y = headerH + padding + row * (cardH + padding);

            BufferedImage singleBox = generateSingleBoxImage(p);
            g2.drawImage(singleBox, x, y, cardW, cardH, null);
        }

        g2.dispose();
        return sheet;
    }

    private static Color getBoxColorForProduct(Product p) {
        String code = p.getCode() != null ? p.getCode().toUpperCase() : "";
        switch (code) {
            case "PRD-001": return new Color(248, 250, 252); // Dell: Sleek Silver Platinum
            case "PRD-002": return new Color(241, 245, 249); // Logitech: Clean White & Gray
            case "PRD-003": return new Color(24, 24, 27);     // Gaming Keyboard: Stealth Carbon
            case "PRD-004": return new Color(248, 250, 252); // Samsung: Premium White Minimalist
            case "PRD-005": return new Color(30, 41, 59);     // Boat: Premium Midnight Black
            case "PRD-006": return new Color(254, 242, 242); // JK Paper: Red / White Carton
            case "PRD-007": return new Color(254, 252, 232); // Parker: Luxury Cream
            case "PRD-008": return new Color(248, 250, 252); // SanDisk: Blister Card
            case "PRD-009": return new Color(241, 245, 249); // Portronics: Space Gray
            case "PRD-010": return new Color(245, 243, 255); // Classmate: Vibrant Stationery
            default: return new Color(248, 250, 252);
        }
    }

    private static Color getAccentColorForProduct(Product p) {
        String code = p.getCode() != null ? p.getCode().toUpperCase() : "";
        switch (code) {
            case "PRD-001": return new Color(0, 118, 206);   // Dell Blue
            case "PRD-002": return new Color(0, 178, 227);   // Logitech Cyan
            case "PRD-003": return new Color(99, 102, 241);  // RGB Indigo
            case "PRD-004": return new Color(14, 165, 233);  // Sky Blue
            case "PRD-005": return new Color(225, 29, 72);   // Boat Crimson
            case "PRD-006": return new Color(220, 38, 38);   // JK Copier Red
            case "PRD-007": return new Color(161, 98, 7);    // Parker Gold
            case "PRD-008": return new Color(220, 38, 38);   // SanDisk Red
            case "PRD-009": return new Color(13, 148, 136);  // Portronics Teal
            case "PRD-010": return new Color(124, 58, 237);  // Classmate Violet
            default: return new Color(0, 90, 61);
        }
    }

    private static List<Product> getDefaultSeedProducts() {
        List<Product> list = new ArrayList<>();
        list.add(new Product(1, "PRD-001", "8901030383124", "Dell Inspiron 15 Laptop", 1, 42000.00, 50000.00, 15, 3, "data/product_images/PRD-001.jpg"));
        list.add(new Product(2, "PRD-002", "8901499028145", "Logitech B170 Wireless Mouse", 2, 450.00, 799.00, 35, 5, "data/product_images/PRD-002.jpg"));
        list.add(new Product(3, "PRD-003", "8901262010051", "Mechanical Gaming Keyboard RGB", 2, 1200.00, 1899.00, 20, 4, "data/product_images/PRD-003.jpg"));
        list.add(new Product(4, "PRD-004", "8901058852336", "Samsung 24-inch FHD Monitor", 1, 7500.00, 9499.00, 12, 3, "data/product_images/PRD-004.jpg"));
        list.add(new Product(5, "PRD-005", "8901030584910", "Boat Rockerz 450 Bluetooth Headphones", 2, 850.00, 1499.00, 28, 5, "data/product_images/PRD-005.jpg"));
        list.add(new Product(6, "PRD-006", "8901725131013", "JK Copier A4 Paper Ream (500 Sheets)", 3, 210.00, 340.00, 60, 10, "data/product_images/PRD-006.jpg"));
        list.add(new Product(7, "PRD-007", "8901324021038", "Parker Classic Gold Ballpoint Pen", 3, 220.00, 350.00, 40, 8, "data/product_images/PRD-007.jpg"));
        list.add(new Product(8, "PRD-008", "8902579100142", "SanDisk 128GB Ultra USB 3.0 Flash Drive", 4, 480.00, 799.00, 45, 6, "data/product_images/PRD-008.jpg"));
        list.add(new Product(9, "PRD-009", "8901030822197", "Portronics 6-in-1 USB-C Hub Adapter", 2, 950.00, 1599.00, 18, 4, "data/product_images/PRD-009.jpg"));
        list.add(new Product(10, "PRD-010", "8901058002151", "Classmate Pulse Spiral Hardbound Notebook", 3, 110.00, 195.00, 50, 10, "data/product_images/PRD-010.jpg"));
        return list;
    }

    public static File getBoxFileForProduct(Product p) {
        if (p == null || p.getCode() == null) return null;
        File dir = new File(PACKAGES_DIR);
        if (!dir.exists()) return null;
        File[] matches = dir.listFiles((d, name) -> name.contains(p.getCode()) && name.endsWith(".png"));
        if (matches != null && matches.length > 0) return matches[0];
        return null;
    }

    public static void openPackagesFolder(Component parent) {
        try {
            File dir = new File(PACKAGES_DIR);
            if (!dir.exists()) dir.mkdirs();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(dir);
            } else {
                JOptionPane.showMessageDialog(parent, "Packaging boxes are located in:\n" + dir.getAbsolutePath(),
                        "Packages Folder", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Could not open folder: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void showBoxDialog(Frame owner, Product p) {
        if (p == null) return;
        BufferedImage boxImg = null;
        File boxFile = getBoxFileForProduct(p);
        if (boxFile != null && boxFile.exists()) {
            try {
                boxImg = ImageIO.read(boxFile);
            } catch (Exception ignored) {}
        }
        if (boxImg == null) {
            boxImg = generateSingleBoxImage(p);
        }

        final BufferedImage finalBoxImg = boxImg;
        JDialog dlg = new JDialog(owner, "Retail Package Box - " + p.getName(), true);
        dlg.setSize(580, 680);
        dlg.setLocationRelativeTo(owner);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(new Color(248, 250, 252));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Top info
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        JLabel lblTitle = new JLabel("📦 Retail Package Mockup (Scannable Barcode Label)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(15, 23, 42));
        JLabel lblSub = new JLabel(p.getName() + "   •   SKU: " + p.getCode() + "   •   Barcode: " + (p.getBarcode() != null ? p.getBarcode() : p.getCode()));
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(100, 116, 139));
        top.add(lblTitle, BorderLayout.NORTH);
        top.add(lblSub, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        // Center Box Image preview
        JPanel imgPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (finalBoxImg == null) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int w = getWidth(), h = getHeight();
                int bw = finalBoxImg.getWidth(), bh = finalBoxImg.getHeight();
                double scale = Math.min((double) w / bw, (double) h / bh);
                int dw = (int) Math.round(bw * scale);
                int dh = (int) Math.round(bh * scale);
                int dx = (w - dw) / 2;
                int dy = (h - dh) / 2;
                g2.drawImage(finalBoxImg, dx, dy, dw, dh, null);
                g2.dispose();
            }
        };
        imgPanel.setOpaque(false);
        imgPanel.setPreferredSize(new Dimension(520, 520));
        root.add(imgPanel, BorderLayout.CENTER);

        // Bottom Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton btnOpenDir = new JButton("📂 Open Folder");
        btnOpenDir.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnOpenDir.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnOpenDir.addActionListener(e -> openPackagesFolder(dlg));

        JButton btnSave = new JButton("⬇ Save Box PNG");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSave.setBackground(new Color(0, 90, 61));
        btnSave.setForeground(Color.WHITE);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(p.getCode() + "_package_box.png"));
            if (fc.showSaveDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                try {
                    File target = fc.getSelectedFile();
                    if (!target.getName().toLowerCase().endsWith(".png")) {
                        target = new File(target.getParentFile(), target.getName() + ".png");
                    }
                    ImageIO.write(finalBoxImg, "PNG", target);
                    JOptionPane.showMessageDialog(dlg, "Saved package box image to:\n" + target.getAbsolutePath(),
                            "Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "Save error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dlg.dispose());

        actions.add(btnOpenDir);
        actions.add(btnSave);
        actions.add(btnClose);
        root.add(actions, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    public static void main(String[] args) {
        generateAllPackageBoxes();
    }
}
