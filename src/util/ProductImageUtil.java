package util;

import model.Product;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for loading, caching, scaling, and generating high-quality product images.
 */
public class ProductImageUtil {

    private static final String IMAGES_DIR = "data/product_images";
    private static final Map<String, ImageIcon> iconCache = new HashMap<>();

    static {
        File dir = new File(IMAGES_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Get an ImageIcon for a product scaled to target dimensions.
     * If the product has a valid image path, it loads the image.
     * Otherwise, generates an elegant visual placeholder based on category and name.
     */
    public static ImageIcon getProductIcon(Product p, int width, int height) {
        String cacheKey = (p != null ? (p.getId() + "_" + p.getCode() + "_" + p.getImagePath()) : "null") + "_" + width + "x" + height;
        if (iconCache.containsKey(cacheKey)) {
            return iconCache.get(cacheKey);
        }

        ImageIcon icon = null;

        // 1. Try loading from file if path is specified
        if (p != null && p.getImagePath() != null && !p.getImagePath().trim().isEmpty()) {
            File imgFile = resolveImageFile(p.getImagePath());
            if (imgFile != null && imgFile.exists() && imgFile.canRead()) {
                try {
                    BufferedImage bImg = ImageIO.read(imgFile);
                    if (bImg != null) {
                        icon = createScaledIcon(bImg, width, height);
                    }
                } catch (Exception ignored) {}
            }
        }

        // 1b. Fallback: Check if an image named by product code/barcode exists in data/product_images
        if (icon == null && p != null) {
            String[] testNames = {
                (p.getCode() != null) ? p.getCode().replaceAll("[^a-zA-Z0-9_-]", "_") : null,
                (p.getBarcode() != null) ? p.getBarcode().replaceAll("[^a-zA-Z0-9_-]", "_") : null
            };
            for (String tName : testNames) {
                if (tName == null || tName.isEmpty()) continue;
                for (String ext : new String[]{".png", ".jpg", ".jpeg", ".webp"}) {
                    File candidate = new File(IMAGES_DIR, tName + ext);
                    if (candidate.exists() && candidate.canRead()) {
                        try {
                            BufferedImage bImg = ImageIO.read(candidate);
                            if (bImg != null) {
                                icon = createScaledIcon(bImg, width, height);
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (icon != null) break;
            }
        }

        // 2. Fallback: generate high-quality visual placeholder
        if (icon == null) {
            String cat = (p != null && p.getCategoryName() != null) ? p.getCategoryName() : "";
            String name = (p != null && p.getName() != null) ? p.getName() : "Product";
            icon = createPlaceholderIcon(cat, name, width, height);
        }

        iconCache.put(cacheKey, icon);
        return icon;
    }

    /**
     * Overload to get an ImageIcon without needing a full Product entity.
     */
    public static ImageIcon getProductIcon(String imagePath, String category, String productName, int width, int height) {
        Product temp = new Product();
        temp.setImagePath(imagePath);
        temp.setCategoryName(category != null ? category : "");
        temp.setName(productName != null ? productName : "Product");
        return getProductIcon(temp, width, height);
    }

    /**
     * Resolve image file from absolute path or relative to project root.
     */
    public static File resolveImageFile(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        File f = new File(path);
        if (f.exists()) return f;

        // Try relative to project root
        File rel = new File(System.getProperty("user.dir"), path);
        if (rel.exists()) return rel;

        // Try in IMAGES_DIR
        File inDir = new File(IMAGES_DIR, f.getName());
        if (inDir.exists()) return inDir;

        return null;
    }

    /**
     * Copies an uploaded image file into data/product_images and returns the relative path.
     */
    public static String saveProductImage(File sourceFile, String productCode) {
        if (sourceFile == null || !sourceFile.exists()) return null;

        try {
            File dir = new File(IMAGES_DIR);
            if (!dir.exists()) dir.mkdirs();

            String ext = "png";
            String name = sourceFile.getName();
            int dot = name.lastIndexOf('.');
            if (dot > 0 && dot < name.length() - 1) {
                ext = name.substring(dot + 1).toLowerCase();
            }

            String safeCode = (productCode != null && !productCode.trim().isEmpty())
                    ? productCode.replaceAll("[^a-zA-Z0-9_-]", "_")
                    : "prod";
            String newFileName = "prod_" + safeCode + "_" + System.currentTimeMillis() + "." + ext;
            File destFile = new File(dir, newFileName);

            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            clearCache();
            return destFile.getPath();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Clears icon cache when product images are updated.
     */
    public static void clearCache() {
        iconCache.clear();
    }

    /**
     * Scale a BufferedImage into an ImageIcon with rounded corners and high-quality rendering.
     */
    private static ImageIcon createScaledIcon(BufferedImage src, int targetW, int targetH) {
        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Rounded clip
        g2.setClip(new RoundRectangle2D.Float(0, 0, targetW, targetH, 12, 12));

        // Soft background fill
        g2.setColor(new Color(248, 250, 252));
        g2.fillRect(0, 0, targetW, targetH);

        // Aspect-ratio fit
        double srcRatio = (double) src.getWidth() / src.getHeight();
        double targetRatio = (double) targetW / targetH;
        int drawW = targetW;
        int drawH = targetH;
        int drawX = 0;
        int drawY = 0;

        if (srcRatio > targetRatio) {
            drawH = (int) (targetW / srcRatio);
            drawY = (targetH - drawH) / 2;
        } else {
            drawW = (int) (targetH * srcRatio);
            drawX = (targetW - drawW) / 2;
        }

        g2.drawImage(src, drawX, drawY, drawW, drawH, null);

        // Soft border
        g2.setClip(null);
        g2.setColor(new Color(226, 232, 240));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, targetW - 1f, targetH - 1f, 12, 12));
        g2.dispose();

        return new ImageIcon(out);
    }

    /**
     * Generates a modern category-styled visual card placeholder.
     */
    public static ImageIcon createPlaceholderIcon(String category, String productName, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String catLower = (category != null) ? category.toLowerCase().trim() : "";
        String nameLower = (productName != null) ? productName.toLowerCase().trim() : "";

        // Choose theme colors & emoji based on category and name
        Color topColor;
        Color bottomColor;
        Color accentColor;
        String iconEmoji;
        String badgeText;

        if (catLower.contains("dairy") || nameLower.contains("milk") || nameLower.contains("curd") || nameLower.contains("butter") || nameLower.contains("cheese")) {
            topColor = new Color(236, 253, 245);
            bottomColor = new Color(209, 250, 229);
            accentColor = new Color(16, 185, 129);
            iconEmoji = "🥛";
            badgeText = "DAIRY";
        } else if (catLower.contains("fruit") || nameLower.contains("mango") || nameLower.contains("apple") || nameLower.contains("banana") || catLower.contains("veg")) {
            topColor = new Color(254, 243, 199);
            bottomColor = new Color(253, 230, 138);
            accentColor = new Color(245, 158, 11);
            iconEmoji = "🍎";
            badgeText = "FRESH";
        } else if (catLower.contains("electronic") || nameLower.contains("laptop") || nameLower.contains("tv") || nameLower.contains("computer") || nameLower.contains("dell") || nameLower.contains("screen")) {
            topColor = new Color(238, 242, 255);
            bottomColor = new Color(224, 231, 255);
            accentColor = new Color(79, 70, 229);
            iconEmoji = "💻";
            badgeText = "ELEC";
        } else if (catLower.contains("accessories") || nameLower.contains("keyboard") || nameLower.contains("mouse") || nameLower.contains("headphone") || nameLower.contains("mob") || nameLower.contains("cable")) {
            topColor = new Color(240, 249, 255);
            bottomColor = new Color(224, 242, 254);
            accentColor = new Color(14, 165, 233);
            iconEmoji = "⌨️";
            badgeText = "TECH";
        } else if (nameLower.contains("paper") || nameLower.contains("book") || nameLower.contains("pen") || nameLower.contains("stationery") || nameLower.contains("ream")) {
            topColor = new Color(255, 247, 237);
            bottomColor = new Color(254, 237, 213);
            accentColor = new Color(234, 88, 12);
            iconEmoji = "📄";
            badgeText = "STATIONERY";
        } else if (catLower.contains("snack") || catLower.contains("bev") || nameLower.contains("tea") || nameLower.contains("coffee") || nameLower.contains("biscuit") || nameLower.contains("chips")) {
            topColor = new Color(253, 242, 248);
            bottomColor = new Color(252, 231, 243);
            accentColor = new Color(219, 39, 119);
            iconEmoji = "🍪";
            badgeText = "FOOD";
        } else {
            topColor = new Color(241, 245, 249);
            bottomColor = new Color(226, 232, 240);
            accentColor = new Color(100, 116, 139);
            iconEmoji = "📦";
            badgeText = "PRODUCT";
        }

        // Draw rounded gradient background
        g2.setClip(new RoundRectangle2D.Float(0, 0, width, height, 12, 12));
        GradientPaint gp = new GradientPaint(0, 0, topColor, 0, height, bottomColor);
        g2.setPaint(gp);
        g2.fillRect(0, 0, width, height);

        // Center emoji / icon
        int fontSize = Math.max(22, Math.min(width, height) / 2);
        g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, fontSize));
        FontMetrics fm = g2.getFontMetrics();
        int iconX = (width - fm.stringWidth(iconEmoji)) / 2;
        int iconY = (height / 2) + (fm.getAscent() / 3);
        g2.drawString(iconEmoji, iconX, iconY);

        // Top mini badge tag
        if (height >= 50 && width >= 70) {
            int tagFontSize = Math.max(9, Math.min(10, width / 12));
            g2.setFont(new Font("Segoe UI", Font.BOLD, tagFontSize));
            FontMetrics bfm = g2.getFontMetrics();
            int tagW = bfm.stringWidth(badgeText) + 8;
            int tagH = bfm.getHeight();
            int tagX = (width - tagW) / 2;
            int tagY = height - tagH - 4;

            g2.setColor(new Color(255, 255, 255, 200));
            g2.fillRoundRect(tagX, tagY, tagW, tagH, 6, 6);
            g2.setColor(accentColor);
            g2.drawString(badgeText, tagX + 4, tagY + bfm.getAscent());
        }

        // Clean border
        g2.setClip(null);
        g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f, 12, 12));
        g2.dispose();

        return new ImageIcon(img);
    }
}
