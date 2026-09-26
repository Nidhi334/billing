package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class AppSettings {
    private static final String SETTINGS_FILE = "app_settings.properties";
    private static Properties properties = new Properties();

    // Mode: Touch vs Non-Touch
    public static final String KEY_SCREEN_MODE = "system.screen_mode"; // TOUCH or NON_TOUCH

    // POS & Screen Feature Buttons
    public static final String KEY_SHOW_POS_NAV = "ui.show_pos_nav";
    public static final String KEY_SHOW_NUMPAD = "ui.show_numpad"; // Numpad / NUPED
    public static final String KEY_SHOW_HELD_BILLS = "ui.show_held_bills"; // Bed / Table / Multi-bill hold
    public static final String KEY_SHOW_QUICK_CASH = "ui.show_quick_cash";
    public static final String KEY_SHOW_DISCOUNT = "ui.show_discount";
    public static final String KEY_SHOW_UPI_QR = "ui.show_upi_qr";
    public static final String KEY_SHOW_BARCODE_SEARCH = "ui.show_barcode_search";
    public static final String KEY_SHOW_CART_ACTIONS = "ui.show_cart_actions";

    // Hardware & Device Settings
    public static final String KEY_BARCODE_BEEP = "device.barcode_beep";
    public static final String KEY_AUTO_PRINT = "device.auto_print";
    public static final String KEY_THERMAL_PRINTER_NAME = "device.thermal_printer_name";

    // Navigation Sidebar Modules
    public static final String KEY_NAV_PRODUCTS = "nav.products";
    public static final String KEY_NAV_INVENTORY = "nav.inventory";
    public static final String KEY_NAV_CUSTOMERS = "nav.customers";
    public static final String KEY_NAV_SUPPLIERS = "nav.suppliers";
    public static final String KEY_NAV_REPORTS = "nav.reports";

    // Store details
    public static final String KEY_STORE_NAME = "store.name";
    public static final String KEY_STORE_PHONE = "store.phone";
    public static final String KEY_STORE_GST = "store.gst";

    static {
        loadSettings();
    }

    public static synchronized void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Failed to load app_settings.properties: " + e.getMessage());
            }
        } else {
            applyDefaults();
            saveSettings();
        }
    }

    private static void applyDefaults() {
        properties.setProperty(KEY_SCREEN_MODE, "TOUCH"); // Default to Touch-optimized

        properties.setProperty(KEY_SHOW_POS_NAV, "true");
        properties.setProperty(KEY_SHOW_NUMPAD, "true");
        properties.setProperty(KEY_SHOW_HELD_BILLS, "true");
        properties.setProperty(KEY_SHOW_QUICK_CASH, "true");
        properties.setProperty(KEY_SHOW_DISCOUNT, "true");
        properties.setProperty(KEY_SHOW_UPI_QR, "true");
        properties.setProperty(KEY_SHOW_BARCODE_SEARCH, "true");
        properties.setProperty(KEY_SHOW_CART_ACTIONS, "true");

        properties.setProperty(KEY_BARCODE_BEEP, "true");
        properties.setProperty(KEY_AUTO_PRINT, "true");
        properties.setProperty(KEY_THERMAL_PRINTER_NAME, "Default 80mm POS Printer");

        properties.setProperty(KEY_NAV_PRODUCTS, "true");
        properties.setProperty(KEY_NAV_INVENTORY, "true");
        properties.setProperty(KEY_NAV_CUSTOMERS, "true");
        properties.setProperty(KEY_NAV_SUPPLIERS, "true");
        properties.setProperty(KEY_NAV_REPORTS, "true");

        properties.setProperty(KEY_STORE_NAME, "SmartBilling Supermarket");
        properties.setProperty(KEY_STORE_PHONE, "9876543210");
        properties.setProperty(KEY_STORE_GST, "18.0");
    }

    public static boolean isTouchMode() {
        return "TOUCH".equalsIgnoreCase(getString(KEY_SCREEN_MODE, "TOUCH"));
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = properties.getProperty(key);
        if (val == null) return defaultValue;
        return Boolean.parseBoolean(val.trim());
    }

    public static void setBoolean(String key, boolean value) {
        properties.setProperty(key, String.valueOf(value));
    }

    public static String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static void setString(String key, String value) {
        properties.setProperty(key, value);
    }

    public static synchronized void saveSettings() {
        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            properties.store(fos, "SmartBilling Pro System & UI Visibility Settings");
        } catch (IOException e) {
            System.err.println("Could not save app_settings.properties: " + e.getMessage());
        }
    }
}
