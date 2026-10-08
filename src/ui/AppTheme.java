package ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * AppTheme - Single Source of Truth for Design Tokens and Brand Styling.
 * Extracted directly from the official "Keyra" Brand Identity Guideline.
 * 
 * Brand Colors:
 * - Primary Forest Green : #005a3d (Anchor, active navigation pills, primary CTAs, hero panels)
 * - Electric Lime / Volt : #c1f700 (Vibrant focal accent, tags, highlights, mode indicators)
 * - Airy Mint Canvas     : #ecfcf0 (Fresh, seamless app canvas & window background)
 * - Muted Sage Border    : #d2eac6 (Subtle organic dividers, card outlines, strokes)
 */
public final class AppTheme {

    private AppTheme() {}

    // ── Primary Brand Tokens ──────────────────────────────────────────────────
    /** Dominant brand anchor: Deep Forest Green (#005a3d) */
    public static final Color FOREST_GREEN    = Color.decode("#005a3d"); // rgb(0, 90, 61)
    
    /** High-energy vibrant accent: Electric Lime / Volt (#c1f700) */
    public static final Color ELECTRIC_LIME   = Color.decode("#c1f700"); // rgb(193, 247, 0)
    
    /** Airy, breathable surface & window background (#ecfcf0) */
    public static final Color BG_CANVAS       = Color.decode("#ecfcf0"); // rgb(236, 252, 240)
    
    /** Delicate organic card border & divider stroke (#d2eac6) */
    public static final Color BORDER_SAGE     = Color.decode("#d2eac6"); // rgb(210, 234, 198)

    // ── Secondary Tonal Scale ─────────────────────────────────────────────────
    /** Mid-tone Forest Green for gradients, secondary highlights (#167a54) */
    public static final Color FOREST_MID      = Color.decode("#167a54");
    
    /** Dark Pine Anchor for deep gradients & contrast (#003826) */
    public static final Color FOREST_DEEP     = Color.decode("#003826");
    
    /** Soft Lime highlight for tags and hover effects (#e4fba3) */
    public static final Color LIME_SOFT       = Color.decode("#e4fba3");
    
    /** Whisper Lime tint for subtle card accents (#f4feed) */
    public static final Color LIME_PALE       = Color.decode("#f4feed");

    // ── Typography Colors (Deep Pine & Sage Undertones) ──────────────────────
    /** Primary High-Contrast Text: Deep Forest Slate (#0a2e21) */
    public static final Color TEXT_PRIMARY    = Color.decode("#0a2e21");
    
    /** Secondary / Subtitle Text: Medium Pine Slate (#335e4e) */
    public static final Color TEXT_SECONDARY  = Color.decode("#335e4e");
    
    /** Muted / Caption / Inactive Text: Sage Gray (#6e9384) */
    public static final Color TEXT_MUTED      = Color.decode("#6e9384");
    
    /** Pure White for elevated card text or buttons */
    public static final Color TEXT_WHITE      = Color.WHITE;

    // ── Surfaces & Card Containers ───────────────────────────────────────────
    /** Pure crisp white for elevated cards */
    public static final Color CARD_BG         = Color.WHITE;
    
    /** Gentle translucent forest hover for buttons & list items */
    public static final Color HOVER_SURFACE   = new Color(0, 90, 61, 18);
    
    /** Active selection highlight */
    public static final Color ACTIVE_SURFACE  = new Color(0, 90, 61, 32);

    // ── Semantic Status Colors (Keyra Harmonized) ────────────────────────────
    public static final Color STATUS_SUCCESS  = Color.decode("#00875a"); // Pine Green
    public static final Color STATUS_WARNING  = Color.decode("#d97706"); // Warm Amber
    public static final Color STATUS_DANGER   = Color.decode("#e11d48"); // Rose Crimson
    public static final Color STATUS_INFO     = Color.decode("#0284c7"); // Sky Blue

    // ── Chart Analytics Palette (Cohesive with Forest & Lime) ────────────────
    public static final Color[] CHART_PALETTE = {
        FOREST_GREEN,                 // #005a3d
        Color.decode("#84cc16"),      // Vibrant Lime-Green
        Color.decode("#0d9488"),      // Pine Teal
        Color.decode("#d97706"),      // Warm Amber
        Color.decode("#6366f1"),      // Indigo Sage
        Color.decode("#e11d48"),      // Rose Crimson
        Color.decode("#0284c7"),      // Azure Blue
        Color.decode("#4b5563")       // Slate Gray
    };

    // ── Utility Helper Factories ──────────────────────────────────────────────
    /**
     * Standard subtle border for cards and containers using the brand border tone.
     */
    public static Border createCardBorder() {
        return BorderFactory.createLineBorder(BORDER_SAGE, 1);
    }

    /**
     * Compound border with standard brand line and comfortable inner padding.
     */
    public static Border createCardBorder(int top, int left, int bottom, int right) {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_SAGE, 1),
            new EmptyBorder(top, left, bottom, right)
        );
    }

    /**
     * Cross-platform typography helper.
     * On macOS, uses Helvetica Neue to completely prevent the OpenJDK Lucida Grande Bold glyph gap bug.
     */
    public static Font font(int style, int size) {
        boolean mac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        if (mac) {
            Font f = new Font("Helvetica Neue", style, size);
            if (!f.getFamily().equalsIgnoreCase("dialog")) return f;
            return new Font(Font.SANS_SERIF, style, size);
        }
        return new Font("Segoe UI", style, size);
    }

    /**
     * Formats currency amounts with thousands separators, cleanly dropping '.00'
     * if the amount is a whole number (e.g. ₹14,850 instead of ₹14,850.00), while
     * retaining two decimal places if cents/paise are present (e.g. ₹14,850.50).
     */
    public static String formatCurrency(double amount) {
        if (amount % 1.0 == 0.0) {
            return String.format("₹%,d", (long) amount);
        } else {
            return String.format("₹%,.2f", amount);
        }
    }

    /**
     * Formats numbers with thousands separators, cleanly dropping '.00' if whole.
     */
    public static String formatNumber(double amount) {
        if (amount % 1.0 == 0.0) {
            return String.format("%,d", (long) amount);
        } else {
            return String.format("%,.2f", amount);
        }
    }
}
