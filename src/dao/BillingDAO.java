package dao;

import config.DBConnection;
import model.Sale;
import model.SaleItem;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BillingDAO {

    public synchronized String generateNextInvoiceNo() throws SQLException {
        String prefix = "INV-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-";
        String sql = "SELECT invoice_no FROM sales WHERE invoice_no LIKE ? ORDER BY id DESC LIMIT 1";
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

    public boolean processSale(Sale sale) throws SQLException {
        String insertSaleSql = "INSERT INTO sales (invoice_no, customer_id, subtotal, gst_rate, gst_amount, total_amount, payment_mode, created_by) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String insertItemSql = "INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?)";
        String updateStockSql = "UPDATE products SET quantity = quantity - ? WHERE id = ? AND quantity >= ?";
        String logStockSql = "INSERT INTO stock_transactions (product_id, type, quantity, reference_id, notes) VALUES (?, 'OUT', ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int saleId;
            try (PreparedStatement psSale = conn.prepareStatement(insertSaleSql, Statement.RETURN_GENERATED_KEYS)) {
                psSale.setString(1, sale.getInvoiceNo());
                if (sale.getCustomerId() != null && sale.getCustomerId() > 0) {
                    psSale.setInt(2, sale.getCustomerId());
                } else {
                    psSale.setNull(2, Types.INTEGER);
                }
                psSale.setDouble(3, sale.getSubtotal());
                psSale.setDouble(4, sale.getGstRate());
                psSale.setDouble(5, sale.getGstAmount());
                psSale.setDouble(6, sale.getTotalAmount());
                psSale.setString(7, sale.getPaymentMode());
                if (sale.getCreatedBy() != null && sale.getCreatedBy() > 0) {
                    psSale.setInt(8, sale.getCreatedBy());
                } else {
                    psSale.setNull(8, Types.INTEGER);
                }

                psSale.executeUpdate();
                try (ResultSet rs = psSale.getGeneratedKeys()) {
                    if (rs.next()) {
                        saleId = rs.getInt(1);
                        sale.setId(saleId);
                    } else {
                        throw new SQLException("Failed to retrieve generated sale id");
                    }
                }
            }

            try (PreparedStatement psItem = conn.prepareStatement(insertItemSql);
                 PreparedStatement psStock = conn.prepareStatement(updateStockSql);
                 PreparedStatement psLog = conn.prepareStatement(logStockSql)) {

                for (SaleItem item : sale.getItems()) {
                    // 1. Insert item
                    psItem.setInt(1, saleId);
                    psItem.setInt(2, item.getProductId());
                    psItem.setInt(3, item.getQuantity());
                    psItem.setDouble(4, item.getUnitPrice());
                    psItem.setDouble(5, item.getSubtotal());
                    psItem.executeUpdate();

                    // 2. Deduct inventory
                    psStock.setInt(1, item.getQuantity());
                    psStock.setInt(2, item.getProductId());
                    psStock.setInt(3, item.getQuantity());
                    int updated = psStock.executeUpdate();
                    if (updated == 0) {
                        throw new SQLException("Insufficient stock for product: " + item.getProductName());
                    }

                    // 3. Log stock movement
                    psLog.setInt(1, item.getProductId());
                    psLog.setInt(2, item.getQuantity());
                    psLog.setString(3, sale.getInvoiceNo());
                    psLog.setString(4, "Sale to customer: " + (sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in"));
                    psLog.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException rb) { rb.printStackTrace(); }
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

    public List<Sale> getRecentSales(int limit) throws SQLException {
        List<Sale> list = new ArrayList<>();
        String sql = "SELECT s.*, c.name AS customer_name, u.full_name AS cashier_name " +
                     "FROM sales s " +
                     "LEFT JOIN customers c ON s.customer_id = c.id " +
                     "LEFT JOIN users u ON s.created_by = u.id " +
                     "ORDER BY s.id DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Sale s = new Sale();
                    s.setId(rs.getInt("id"));
                    s.setInvoiceNo(rs.getString("invoice_no"));
                    s.setCustomerId(rs.getInt("customer_id"));
                    s.setCustomerName(rs.getString("customer_name"));
                    s.setSaleDate(rs.getTimestamp("sale_date"));
                    s.setSubtotal(rs.getDouble("subtotal"));
                    s.setGstRate(rs.getDouble("gst_rate"));
                    s.setGstAmount(rs.getDouble("gst_amount"));
                    s.setTotalAmount(rs.getDouble("total_amount"));
                    s.setPaymentMode(rs.getString("payment_mode"));
                    s.setCashierName(rs.getString("cashier_name"));
                    list.add(s);
                }
            }
        }
        return list;
    }
}

