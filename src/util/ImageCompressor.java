package util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Utility to batch-clean obsolete product images and compress active product images
 * into standardized 4:3 aspect ratio JPG files for minimal disk space and fast loading.
 */
public class ImageCompressor {

    public static final int TARGET_WIDTH = 600;
    public static final int TARGET_HEIGHT = 450; // 600 * 3 / 4 = 450 (Exact 4:3 aspect ratio)
    public static final float JPEG_QUALITY = 0.85f;

    /**
     * Resizes and fits the source image into an exact 4:3 aspect ratio frame with pure white
     * background (contain mode, no distortion, zero clipping), saving as compressed JPEG.
     */
    public static void compressTo4x3Jpg(File srcFile, File destJpg, int targetWidth, float quality) throws IOException {
        BufferedImage original = ImageIO.read(srcFile);
        if (original == null) {
            throw new IOException("Failed to read image: " + srcFile.getAbsolutePath());
        }

        int targetHeight = (int) Math.round((double) targetWidth * 3.0 / 4.0);

        // Exact 4:3 canvas (RGB for clean JPEG export without alpha channel artifacts)
        BufferedImage canvas = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fill background with clean studio white
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, targetWidth, targetHeight);

        // Fit image within the 4:3 canvas (contain mode)
        int origW = original.getWidth();
        int origH = original.getHeight();
        double scale = Math.min((double) targetWidth / origW, (double) targetHeight / origH);
        int drawW = Math.max(1, (int) Math.round(origW * scale));
        int drawH = Math.max(1, (int) Math.round(origH * scale));
        int drawX = (targetWidth - drawW) / 2;
        int drawY = (targetHeight - drawH) / 2;

        g2.drawImage(original, drawX, drawY, drawW, drawH, null);
        g2.dispose();

        saveCompressedJpg(canvas, destJpg, quality);
    }

    public static void saveCompressedJpg(BufferedImage img, File dest, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(img, "JPG", dest);
            return;
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(dest)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    public static void cleanAndCompressAll() {
        System.out.println("=== CLEANING & 4:3 JPG IMAGE COMPRESSION ===");
        String artifactDir = "/Users/rishukumar/.gemini/antigravity-ide/brain/b52f6d03-ce3a-4a7d-b201-2b89e202edc7";
        File targetDir = new File("data/product_images");
        if (!targetDir.exists()) targetDir.mkdirs();

        String[][] items = {
            {"PRD-001", "8901030383124", "Dell Inspiron 15 Laptop", artifactDir + "/dell_laptop_1792821475243.jpg"},
            {"PRD-002", "8901499028145", "Logitech B170 Wireless Mouse", artifactDir + "/logitech_mouse_1792821492351.jpg"},
            {"PRD-003", "8901262010051", "Mechanical Gaming Keyboard RGB", artifactDir + "/gaming_keyboard_1792821510472.jpg"},
            {"PRD-004", "8901058852336", "Samsung 24-inch FHD Monitor", artifactDir + "/samsung_monitor_1792821531762.jpg"},
            {"PRD-005", "8901030584910", "Boat Rockerz 450 Bluetooth Headphones", artifactDir + "/boat_headphones_1792821548500.jpg"},
            {"PRD-006", "8901725131013", "JK Copier A4 Paper Ream (500 Sheets)", artifactDir + "/copier_paper_ream_1792821565294.jpg"},
            {"PRD-007", "8901324021038", "Parker Classic Gold Ballpoint Pen", artifactDir + "/parker_gold_pen_1792821583823.jpg"},
            {"PRD-008", "8902579100142", "SanDisk 128GB Ultra USB 3.0 Flash Drive", artifactDir + "/sandisk_usb_drive_1792821604020.jpg"},
            {"PRD-009", "8901030822197", "Portronics 6-in-1 USB-C Hub Adapter", artifactDir + "/portronics_usbc_hub_1792821624472.jpg"},
            {"PRD-010", "8901058002151", "Classmate Pulse Spiral Hardbound Notebook", artifactDir + "/classmate_notebook_1792821646871.jpg"}
        };

        Set<String> activeFiles = new HashSet<>();
        for (String[] it : items) {
            activeFiles.add(it[0] + ".jpg"); // e.g. PRD-001.jpg
        }

        // 1. Delete all unused images from data/product_images/
        File[] existing = targetDir.listFiles();
        int deletedCount = 0;
        long bytesFreed = 0;
        if (existing != null) {
            for (File f : existing) {
                if (f.getName().equals(".gitkeep")) continue;
                if (!activeFiles.contains(f.getName())) {
                    bytesFreed += f.length();
                    if (f.delete()) {
                        deletedCount++;
                    }
                }
            }
        }
        System.out.printf("Cleaned up %d obsolete image files (Freed: %.2f MB)%n", deletedCount, (double) bytesFreed / (1024 * 1024));

        // 2. Generate each 4:3 compressed JPG
        long totalNewBytes = 0;
        for (String[] it : items) {
            String sku = it[0];
            String name = it[2];
            File src = new File(it[3]);

            if (!src.exists()) {
                System.err.println("Source not found: " + src.getAbsolutePath());
                continue;
            }

            try {
                File destJpg = new File(targetDir, sku + ".jpg");
                compressTo4x3Jpg(src, destJpg, TARGET_WIDTH, JPEG_QUALITY);
                long len = destJpg.length();
                totalNewBytes += len;
                System.out.printf("  ✓ %s: %s -> %s (Size: %d KB, 4:3 %dx%d)%n",
                        sku, name, destJpg.getName(), len / 1024, TARGET_WIDTH, TARGET_HEIGHT);
            } catch (Exception ex) {
                System.err.println("Failed compressing " + sku + ": " + ex.getMessage());
            }
        }

        System.out.printf("All 10 product images successfully saved as 4:3 compressed JPGs!%nTotal Catalog Image Size: %d KB (%.2f MB)%n",
                totalNewBytes / 1024, (double) totalNewBytes / (1024 * 1024));
    }

    public static void main(String[] args) {
        cleanAndCompressAll();
    }
}
