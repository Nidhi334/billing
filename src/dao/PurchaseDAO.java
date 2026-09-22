package dao;

import config.DBConnection;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PurchaseDAO {

    public synchronized String generateNextPurchaseInvoiceNo() throws SQLException {
        String prefix = "PUR-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-";
        String sql = "SELECT invoice_no FROM purchases WHERE invoice_no LIKE ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String lastNo = rs.getString("invoice_no");
                    String[] parts = lastNo.split("-");
                    if (parts.length == 3) {
                        int seq = Integer.parseInt(parts[2]) + 1;
                        return String.format("%s%04d", prefix, seq);
                    }
                }
            }
        } catch (Exception e) {
            // fallback
        }
        return prefix + "0001";
    }

    public boolean recordPurchase(String invoiceNo, int supplierId, int productId, int quantity, double unitCost) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            double totalAmount = quantity * unitCost;

            // 1. Insert into purchases
            int purchaseId;
            String insertPurchSql = "INSERT INTO purchases (invoice_no, supplier_id, total_amount) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertPurchSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, invoiceNo);
                if (supplierId > 0) {
                    ps.setInt(2, supplierId);
                } else {
                    ps.setNull(2, Types.INTEGER);
                }
                ps.setDouble(3, totalAmount);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        purchaseId = rs.getInt(1);
                    } else {
                        throw new SQLException("Failed to get purchase id");
                    }
                }
            }

            // 2. Insert item
            String insertItemSql = "INSERT INTO purchase_items (purchase_id, product_id, quantity, unit_cost, subtotal) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertItemSql)) {
                ps.setInt(1, purchaseId);
                ps.setInt(2, productId);
                ps.setInt(3, quantity);
                ps.setDouble(4, unitCost);
                ps.setDouble(5, totalAmount);
                ps.executeUpdate();
            }

            // 3. Update product stock and purchase price
            String updateProductSql = "UPDATE products SET quantity = quantity + ?, purchase_price = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateProductSql)) {
                ps.setInt(1, quantity);
                ps.setDouble(2, unitCost);
                ps.setInt(3, productId);
                ps.executeUpdate();
            }

            // 4. Log stock transaction
            String logSql = "INSERT INTO stock_transactions (product_id, type, quantity, reference_id, notes) VALUES (?, 'IN', ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(logSql)) {
                ps.setInt(1, productId);
                ps.setInt(2, quantity);
                ps.setString(3, invoiceNo);
                ps.setString(4, "Stock In from purchase");
                ps.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException e) { e.printStackTrace(); }
            }
            throw ex;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<String[]> getRecentPurchases(int limit) throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT p.invoice_no, s.name AS supplier_name, pr.name AS product_name, pi.quantity, pi.unit_cost, p.total_amount, p.purchase_date " +
                     "FROM purchases p " +
                     "LEFT JOIN suppliers s ON p.supplier_id = s.id " +
                     "JOIN purchase_items pi ON p.id = pi.purchase_id " +
                     "JOIN products pr ON pi.product_id = pr.id " +
                     "ORDER BY p.id DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[]{
                            rs.getString("invoice_no"),
                            rs.getString("supplier_name") != null ? rs.getString("supplier_name") : "N/A",
                            rs.getString("product_name"),
                            String.valueOf(rs.getInt("quantity")),
                            String.format("₹%.2f", rs.getDouble("unit_cost")),
                            String.format("₹%.2f", rs.getDouble("total_amount")),
                            rs.getString("purchase_date")
                    });
                }
            }
        }
        return list;
    }
}

