package util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for loading, rendering, and applying macOS-styled app icons across
 * the SmartBilling Pro application (macOS Dock, window frames, taskbar, brand badges).
 */
public final class AppIconUtil {

    private static final String MAC_ICON_PATH = "data/assets/images/app_icon_macos.png";
    private static final String SQUIRCLE_ICON_PATH = "data/assets/images/app_icon_squircle.png";
    private static final String RAW_ICON_PATH = "data/assets/images/Green Storefront Smile Arrow.png";

    private static BufferedImage baseMacIcon = null;
    private static BufferedImage baseSquircleIcon = null;
    private static final Map<Integer, BufferedImage> iconCache = new ConcurrentHashMap<>();
    private static final Map<Integer, ImageIcon> imageIconCache = new ConcurrentHashMap<>();
    private static final Map<Integer, BufferedImage> squircleCache = new ConcurrentHashMap<>();
    private static final Map<Integer, ImageIcon> squircleImageIconCache = new ConcurrentHashMap<>();

    static {
        loadBaseIcons();
    }

    private AppIconUtil() {}

    private static synchronized void loadBaseIcons() {
        try {
            File macFile = new File(MAC_ICON_PATH);
            if (macFile.exists()) {
                baseMacIcon = ImageIO.read(macFile);
            }
        } catch (Exception e) {
            System.err.println("Could not load mac icon file: " + e.getMessage());
        }

        try {
            File sqFile = new File(SQUIRCLE_ICON_PATH);
            if (sqFile.exists()) {
                baseSquircleIcon = ImageIO.read(sqFile);
            }
        } catch (Exception e) {
            System.err.println("Could not load squircle icon file: " + e.getMessage());
        }

        // Fallback: If neither pre-rendered file is found, load raw image and render squircle
        if (baseMacIcon == null || baseSquircleIcon == null) {
            try {
                File rawFile = new File(RAW_ICON_PATH);
                if (rawFile.exists()) {
                    BufferedImage raw = ImageIO.read(rawFile);
                    if (raw != null) {
                        if (baseSquircleIcon == null) {
                            baseSquircleIcon = renderSquircle(raw, 1024);
                        }
                        if (baseMacIcon == null) {
                            baseMacIcon = renderMacDock(raw, 1024);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Could not generate base icons from raw: " + e.getMessage());
            }
        }
    }

    /**
     * Set up the application icon in the macOS Dock or system taskbar.
     * Should be invoked at application startup before opening frames.
     */
    public static void setupTaskbarIcon() {
        // 1. Set macOS application name property
        try {
            System.setProperty("apple.awt.application.name", "SmartBilling Pro");
        } catch (Throwable ignored) {}

        Image icon = getAppIcon(512);
        if (icon == null) return;

        // 2. Modern Java 9+ Taskbar API (macOS Dock & Windows taskbar)
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(icon);
                }
            }
        } catch (Throwable ignored) {}

        // 3. Apple EAWT legacy fallback for macOS
        try {
            Class<?> appClass = Class.forName("com.apple.eawt.Application");
            Object app = appClass.getMethod("getApplication").invoke(null);
            appClass.getMethod("setDockIconImage", Image.class).invoke(app, icon);
        } catch (Throwable ignored) {}
    }

    /**
     * Apply the macOS app icon to any JFrame / JDialog / Window.
     * Provides multi-resolution icon images for crisp scaling on all display densities.
     */
    public static void applyToWindow(Window window) {
        if (window == null) return;
        List<Image> icons = getAppIconImages();
        if (!icons.isEmpty()) {
            window.setIconImages(icons);
        }
    }

    /**
     * Multi-resolution list of icons for window icon sets.
     */
    public static List<Image> getAppIconImages() {
        int[] sizes = {16, 32, 48, 64, 128, 256, 512};
        List<Image> list = new ArrayList<>(sizes.length);
        for (int s : sizes) {
            BufferedImage img = getAppIcon(s);
            if (img != null) {
                list.add(img);
            }
        }
        return list;
    }

    /**
     * Primary app icon (macOS Dock style with shadow) scaled to the requested size.
     */
    public static BufferedImage getAppIcon(int size) {
        return iconCache.computeIfAbsent(size, s -> {
            BufferedImage base = (baseMacIcon != null) ? baseMacIcon : baseSquircleIcon;
            if (base == null) return createEmergencyIcon(s);
            return scaleSmooth(base, s, s);
        });
    }

    /**
     * Returns an ImageIcon for Swing labels/buttons.
     */
    public static ImageIcon getAppImageIcon(int size) {
        return imageIconCache.computeIfAbsent(size, s -> new ImageIcon(getAppIcon(s)));
    }

    /**
     * Returns the full-bleed squircle app icon (without outer drop shadow margin),
     * ideal for embedding within UI badges, cards, or buttons.
     */
    public static BufferedImage getSquircleIcon(int size) {
        return squircleCache.computeIfAbsent(size, s -> {
            BufferedImage base = (baseSquircleIcon != null) ? baseSquircleIcon : baseMacIcon;
            if (base == null) return createEmergencyIcon(s);
            return scaleSmooth(base, s, s);
        });
    }

    /**
     * Returns an ImageIcon of the full-bleed squircle.
     */
    public static ImageIcon getSquircleImageIcon(int size) {
        return squircleImageIconCache.computeIfAbsent(size, s -> new ImageIcon(getSquircleIcon(s)));
    }

    /**
     * Creates a self-contained macOS style brand icon component (with squircle curvature,
     * rim highlight, and soft ambient drop shadow) of the given dimension.
     */
    public static JComponent createBrandBadge(int size) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(size, size));
                setMinimumSize(new Dimension(size, size));
                setMaximumSize(new Dimension(size, size));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int w = getWidth();
                int h = getHeight();

                // Compute inner tile dimensions and soft shadow margins
                int shadowPad = Math.max(2, Math.round(w * 0.08f));
                int tileW = w - (shadowPad * 2);
                int tileH = h - (shadowPad * 2);
                int tileX = shadowPad;
                int tileY = Math.max(1, shadowPad - Math.round(w * 0.03f));

                float arc = tileW * 0.448f;

                // 1. Soft drop shadow
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fill(new RoundRectangle2D.Float(tileX, tileY + 3, tileW, tileH, arc, arc));
                g2.setColor(new Color(0, 0, 0, 35));
                g2.fill(new RoundRectangle2D.Float(tileX, tileY + 1.5f, tileW, tileH, arc, arc));

                // 2. Squircle tile
                BufferedImage tile = getSquircleIcon(tileW);
                if (tile != null) {
                    g2.drawImage(tile, tileX, tileY, tileW, tileH, null);
                }

                // 3. Subtle rim stroke
                g2.setColor(new Color(0, 0, 0, 30));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(tileX, tileY, tileW, tileH, arc, arc));

                g2.dispose();
            }
        };
    }

    /**
     * High-quality multi-step progressive halving downscaler to eliminate
     * aliasing and preserve crystal-clear vector sharpness at any size.
     */
    private static BufferedImage scaleSmooth(BufferedImage img, int targetW, int targetH) {
        int w = img.getWidth();
        int h = img.getHeight();

        if (w == targetW && h == targetH) {
            return img;
        }

        BufferedImage ret = img;

        do {
            if (w > targetW) {
                w /= 2;
                if (w < targetW) w = targetW;
            } else if (w < targetW) {
                w = targetW;
            }

            if (h > targetH) {
                h /= 2;
                if (h < targetH) h = targetH;
            } else if (h < targetH) {
                h = targetH;
            }

            BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetW || h != targetH);

        return ret;
    }

    private static BufferedImage renderSquircle(BufferedImage src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        float r = size * 0.2237f;
        Shape squircle = new RoundRectangle2D.Float(0, 0, size, size, r * 2f, r * 2f);

        Graphics2D gClip = (Graphics2D) g2.create();
        gClip.setClip(squircle);
        gClip.drawImage(src, 0, 0, size, size, null);

        // Subtle inner rim stroke
        gClip.setStroke(new BasicStroke(Math.max(1f, size / 256f)));
        gClip.setColor(new Color(255, 255, 255, 45));
        gClip.draw(squircle);
        gClip.dispose();

        // 1px subtle dark outer border
        g2.setStroke(new BasicStroke(Math.max(1f, size / 512f)));
        g2.setColor(new Color(0, 0, 0, 35));
        g2.draw(squircle);

        g2.dispose();
        return out;
    }

    private static BufferedImage renderMacDock(BufferedImage src, int canvasSize) {
        BufferedImage out = new BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        float scale = canvasSize / 1024.0f;
        float tileSize = 824.0f * scale;
        float margin = (canvasSize - tileSize) / 2.0f;
        float cornerRadius = 185.0f * scale;

        float tileX = margin;
        float tileY = margin - (10.0f * scale);

        // Soft drop shadow
        if (canvasSize >= 32) {
            int layers = Math.max(4, Math.round(14 * scale));
            float maxSpread = 28.0f * scale;
            float baseAlpha = 0.032f;
            for (int i = layers; i >= 1; i--) {
                float progress = (float) i / layers;
                float spread = maxSpread * progress;
                float shadowY = tileY + (12.0f * scale) + (8.0f * scale * progress);
                RoundRectangle2D.Float shadowShape = new RoundRectangle2D.Float(
                    tileX - spread * 0.4f,
                    shadowY,
                    tileSize + spread * 0.8f,
                    tileSize + spread * 0.6f,
                    (cornerRadius + spread * 0.4f) * 2f,
                    (cornerRadius + spread * 0.4f) * 2f
                );
                int alpha = Math.min(255, Math.max(1, Math.round(baseAlpha * (1.0f - progress * 0.5f) * 255)));
                g2.setColor(new Color(0, 0, 0, alpha));
                g2.fill(shadowShape);
            }
        }

        Shape squircle = new RoundRectangle2D.Float(tileX, tileY, tileSize, tileSize, cornerRadius * 2f, cornerRadius * 2f);
        Graphics2D gClip = (Graphics2D) g2.create();
        gClip.setClip(squircle);
        gClip.drawImage(src, Math.round(tileX), Math.round(tileY), Math.round(tileSize), Math.round(tileSize), null);

        // Specular sheen
        GradientPaint topSheen = new GradientPaint(
            tileX, tileY, new Color(255, 255, 255, 30),
            tileX, tileY + tileSize * 0.45f, new Color(255, 255, 255, 0)
        );
        gClip.setPaint(topSheen);
        gClip.fill(squircle);

        // Rim stroke
        gClip.setStroke(new BasicStroke(Math.max(1f, 1.5f * scale)));
        GradientPaint rimGrad = new GradientPaint(
            tileX, tileY, new Color(255, 255, 255, 75),
            tileX, tileY + tileSize, new Color(0, 0, 0, 45)
        );
        gClip.setPaint(rimGrad);
        gClip.draw(squircle);
        gClip.dispose();

        g2.setStroke(new BasicStroke(Math.max(1f, 1f * scale)));
        g2.setColor(new Color(0, 0, 0, 40));
        g2.draw(squircle);

        g2.dispose();
        return out;
    }

    private static BufferedImage createEmergencyIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(new Color(17, 74, 42));
        g2.fillRoundRect(0, 0, size, size, Math.round(size * 0.45f), Math.round(size * 0.45f));
        g2.dispose();
        return img;
    }
}
