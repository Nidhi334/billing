package dao;

import config.DBConnection;
import model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {
    private static boolean barcodeColChecked = false;
    private static boolean imageColChecked = false;

    private synchronized void ensureColumns(Connection conn) {
        if (!barcodeColChecked) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE products ADD COLUMN barcode VARCHAR(100) NULL AFTER code");
            } catch (Exception ignored) {
                // Column already exists
            }
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
    }


    public List<Product> getAllProducts() throws SQLException {
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

