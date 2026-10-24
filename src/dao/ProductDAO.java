package dao;

import config.DBConnection;
import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {
    private static boolean barcodeColChecked = false;
    private static boolean imageColChecked = false;

    private static boolean seedChecked = false;

    private synchronized void ensureColumns(Connection conn) {
        if (!barcodeColChecked) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE products ADD COLUMN barcode VARCHAR(100) NULL AFTER code");
            } catch (Exception ignored) {
                // Column already exists
            }
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("UPDATE products SET barcode = code WHERE barcode IS NULL OR TRIM(barcode) = ''");
            } catch (Exception ignored) {}
            barcodeColChecked = true;
        }
        if (!imageColChecked) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE products ADD COLUMN image_path VARCHAR(255) NULL");
            } catch (Exception ignored) {
                // Column already exists
            }
            imageColChecked = true;
        }
        ensureDefaultSeedProducts(conn);
    }

    private synchronized void ensureDefaultSeedProducts(Connection conn) {
        if (!seedChecked) {
            try {
                // 1. Ensure categories 1..4 exist
                try (Statement st = conn.createStatement()) {
                    st.execute("INSERT IGNORE INTO categories (id, name, description) VALUES " +
                            "(1, 'Electronics', 'Laptops, Monitors, Gadgets'), " +
                            "(2, 'Accessories', 'Mice, Keyboards, Cables'), " +
                            "(3, 'Stationery', 'Office supplies, paper, notebooks'), " +
                            "(4, 'Storage & Networking', 'USB Drives, Adapters, Cables')");
                } catch (Exception ignored) {}

                // 2. Ensure fixed 10 products with permanent barcodes exist
                String seedSql = "INSERT INTO products (id, code, barcode, name, category_id, purchase_price, selling_price, quantity, min_stock_level, image_path) VALUES " +
                        "(1, 'PRD-001', '8901030383124', 'Dell Inspiron 15 Laptop', 1, 42000.00, 50000.00, 15, 3, 'data/product_images/PRD-001.jpg'), " +
                        "(2, 'PRD-002', '8901499028145', 'Logitech B170 Wireless Mouse', 2, 450.00, 799.00, 35, 5, 'data/product_images/PRD-002.jpg'), " +
                        "(3, 'PRD-003', '8901262010051', 'Mechanical Gaming Keyboard RGB', 2, 1200.00, 1899.00, 20, 4, 'data/product_images/PRD-003.jpg'), " +
                        "(4, 'PRD-004', '8901058852336', 'Samsung 24-inch FHD Monitor', 1, 7500.00, 9499.00, 12, 3, 'data/product_images/PRD-004.jpg'), " +
                        "(5, 'PRD-005', '8901030584910', 'Boat Rockerz 450 Bluetooth Headphones', 2, 850.00, 1499.00, 28, 5, 'data/product_images/PRD-005.jpg'), " +
                        "(6, 'PRD-006', '8901725131013', 'JK Copier A4 Paper Ream (500 Sheets)', 3, 210.00, 340.00, 60, 10, 'data/product_images/PRD-006.jpg'), " +
                        "(7, 'PRD-007', '8901324021038', 'Parker Classic Gold Ballpoint Pen', 3, 220.00, 350.00, 40, 8, 'data/product_images/PRD-007.jpg'), " +
                        "(8, 'PRD-008', '8902579100142', 'SanDisk 128GB Ultra USB 3.0 Flash Drive', 4, 480.00, 799.00, 45, 6, 'data/product_images/PRD-008.jpg'), " +
                        "(9, 'PRD-009', '8901030822197', 'Portronics 6-in-1 USB-C Hub Adapter', 2, 950.00, 1599.00, 18, 4, 'data/product_images/PRD-009.jpg'), " +
                        "(10, 'PRD-010', '8901058002151', 'Classmate Pulse Spiral Hardbound Notebook', 3, 110.00, 195.00, 50, 10, 'data/product_images/PRD-010.jpg') " +
                        "ON DUPLICATE KEY UPDATE barcode = VALUES(barcode), name = VALUES(name), category_id = VALUES(category_id), " +
                        "purchase_price = VALUES(purchase_price), selling_price = VALUES(selling_price), quantity = VALUES(quantity), " +
                        "min_stock_level = VALUES(min_stock_level), image_path = VALUES(image_path)";
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate(seedSql);
                } catch (Exception ex) {
                    System.err.println("Seed products note: " + ex.getMessage());
                }
                seedChecked = true;
            } catch (Exception ignored) {}
        }
    }

    public static String generateUniqueBarcode() {
        long ts = System.currentTimeMillis() % 10000000000L;
        return String.format("890%09d", ts % 1000000000L);
    }

    public List<Product> getAllProducts() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ensureColumns(conn);
        }
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name FROM products p " +
                     "LEFT JOIN categories c ON p.category_id = c.id " +
                     "ORDER BY p.name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapProduct(rs));
            }
        }
        return list;
    }

    public List<Product> searchProducts(String keyword) throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name FROM products p " +
                     "LEFT JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.code LIKE ? OR p.barcode LIKE ? OR p.name LIKE ? OR c.name LIKE ? " +
                     "ORDER BY p.name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapProduct(rs));
                }
            }
        }
        return list;
    }

    public List<Product> getLowStockProducts() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS category_name FROM products p " +
                     "LEFT JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.quantity <= p.min_stock_level " +
                     "ORDER BY p.quantity ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapProduct(rs));
            }
        }
        return list;
    }

    public Product getProductByCode(String code) throws SQLException {
        if (code == null || code.trim().isEmpty()) return null;
        String trimmed = code.trim();
        String sql = "SELECT p.*, c.name AS category_name FROM products p " +
                     "LEFT JOIN categories c ON p.category_id = c.id " +
                     "WHERE LOWER(TRIM(p.code)) = LOWER(?) OR LOWER(TRIM(COALESCE(p.barcode, ''))) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trimmed);
            ps.setString(2, trimmed);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
            }
        }
        return null;
    }

    public Product getProductById(int id) throws SQLException {
        if (id <= 0) return null;
        String sql = "SELECT p.*, c.name AS category_name FROM products p " +
                     "LEFT JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProduct(rs);
                }
            }
        }
        return null;
    }

    public boolean addProduct(Product p) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ensureColumns(conn);
            if (p.getBarcode() == null || p.getBarcode().trim().isEmpty()) {
                p.setBarcode(generateUniqueBarcode());
            }
            String sql = "INSERT INTO products (code, barcode, name, category_id, purchase_price, selling_price, quantity, min_stock_level, image_path) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, p.getCode());
                ps.setString(2, p.getBarcode());
                ps.setString(3, p.getName());
                if (p.getCategoryId() > 0) {
                    ps.setInt(4, p.getCategoryId());
                } else {
                    ps.setNull(4, Types.INTEGER);
                }
                ps.setDouble(5, p.getPurchasePrice());
                ps.setDouble(6, p.getSellingPrice());
                ps.setInt(7, p.getQuantity());
                ps.setInt(8, p.getMinStockLevel());
                ps.setString(9, p.getImagePath());
                int affected = ps.executeUpdate();
                if (affected > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            p.setId(rs.getInt(1));
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean updateProduct(Product p) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ensureColumns(conn);
            String sql = "UPDATE products SET code = ?, barcode = ?, name = ?, category_id = ?, purchase_price = ?, selling_price = ?, " +
                         "quantity = ?, min_stock_level = ?, image_path = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, p.getCode());
                ps.setString(2, p.getBarcode());
                ps.setString(3, p.getName());
                if (p.getCategoryId() > 0) {
                    ps.setInt(4, p.getCategoryId());
                } else {
                    ps.setNull(4, Types.INTEGER);
                }
                ps.setDouble(5, p.getPurchasePrice());
                ps.setDouble(6, p.getSellingPrice());
                ps.setInt(7, p.getQuantity());
                ps.setInt(8, p.getMinStockLevel());
                ps.setString(9, p.getImagePath());
                ps.setInt(10, p.getId());
                return ps.executeUpdate() > 0;
            }
        }
    }

    public boolean updateProductImage(int productId, String imagePath) throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            ensureColumns(conn);
            String sql = "UPDATE products SET image_path = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, imagePath);
                ps.setInt(2, productId);
                return ps.executeUpdate() > 0;
            }
        }
    }

    public boolean deleteProduct(int id) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateStock(int productId, int quantityDiff) throws SQLException {
        String sql = "UPDATE products SET quantity = quantity + ? WHERE id = ? AND (quantity + ?) >= 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantityDiff);
            ps.setInt(2, productId);
            ps.setInt(3, quantityDiff);
            return ps.executeUpdate() > 0;
        }
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getInt("id"));
        p.setCode(rs.getString("code"));
        try {
            String b = rs.getString("barcode");
            p.setBarcode((b != null && !b.trim().isEmpty()) ? b : p.getCode());
        } catch (Exception e) {
            p.setBarcode(p.getCode());
        }
        p.setName(rs.getString("name"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        p.setPurchasePrice(rs.getDouble("purchase_price"));
        p.setSellingPrice(rs.getDouble("selling_price"));
        p.setQuantity(rs.getInt("quantity"));
        p.setMinStockLevel(rs.getInt("min_stock_level"));
        try {
            p.setImagePath(rs.getString("image_path"));
        } catch (Exception ignored) {}
        return p;
    }
}

