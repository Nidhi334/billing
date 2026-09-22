package dao;

import config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportDAO {

    public Map<String, Object> getDashboardStats() throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        try (Connection conn = DBConnection.getConnection()) {
            // Total products
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*), COALESCE(SUM(quantity), 0) FROM products");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalProducts", rs.getInt(1));
                    stats.put("totalStockUnits", rs.getInt(2));
                }
            }

            // Total Customers
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM customers");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalCustomers", rs.getInt(1));
                }
            }

            // Total Suppliers
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM suppliers");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalSuppliers", rs.getInt(1));
                }
            }

            // Low Stock count
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM products WHERE quantity <= min_stock_level");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("lowStockCount", rs.getInt(1));
                }
            }

            // Today's Sales
            try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(total_amount), 0), COUNT(*) FROM sales WHERE DATE(sale_date) = CURRENT_DATE()");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("todaySalesAmount", rs.getDouble(1));
                    stats.put("todaySalesCount", rs.getInt(2));
                }
            }

            // Monthly Sales
            try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(total_amount), 0), COUNT(*) FROM sales WHERE MONTH(sale_date) = MONTH(CURRENT_DATE()) AND YEAR(sale_date) = YEAR(CURRENT_DATE())");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("monthlySalesAmount", rs.getDouble(1));
                    stats.put("monthlySalesCount", rs.getInt(2));
                }
            }

            // Total Revenue
            try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(total_amount), 0) FROM sales");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalRevenue", rs.getDouble(1));
                }
            }

            // Total Purchases Cost
            try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(total_amount), 0) FROM purchases");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalPurchases", rs.getDouble(1));
                }
            }
        }

        return stats;
    }

    public List<String[]> getStockHistory(int limit) throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT st.created_at, p.code, p.name, st.type, st.quantity, st.reference_id, st.notes " +
                     "FROM stock_transactions st " +
                     "JOIN products p ON st.product_id = p.id " +
                     "ORDER BY st.id DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new String[]{
                            rs.getString("created_at"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("type"),
                            String.valueOf(rs.getInt("quantity")),
                            rs.getString("reference_id"),
                            rs.getString("notes")
                    });
                }
            }
        }
        return list;
    }

    public List<String[]> getSalesReport(String fromDate, String toDate) throws SQLException {
        List<String[]> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT s.invoice_no, s.sale_date, COALESCE(c.name, 'Walk-in') AS cust, s.subtotal, s.gst_amount, s.total_amount, s.payment_mode " +
                "FROM sales s LEFT JOIN customers c ON s.customer_id = c.id WHERE 1=1 ");

        if (fromDate != null && !fromDate.trim().isEmpty()) {
            sql.append(" AND DATE(s.sale_date) >= '").append(fromDate.trim()).append("'");
        }
        if (toDate != null && !toDate.trim().isEmpty()) {
            sql.append(" AND DATE(s.sale_date) <= '").append(toDate.trim()).append("'");
        }
        sql.append(" ORDER BY s.id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("invoice_no"),
                        rs.getString("sale_date"),
                        rs.getString("cust"),
                        String.format("₹%.2f", rs.getDouble("subtotal")),
                        String.format("₹%.2f", rs.getDouble("gst_amount")),
                        String.format("₹%.2f", rs.getDouble("total_amount")),
                        rs.getString("payment_mode")
                });
            }
        }
        return list;
    }

    public List<String[]> getProfitLossReport() throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT p.code, p.name, " +
                     "COALESCE(SUM(si.quantity), 0) AS total_sold, " +
                     "COALESCE(SUM(si.subtotal), 0) AS sales_revenue, " +
                     "COALESCE(SUM(si.quantity * p.purchase_price), 0) AS cost_of_goods, " +
                     "(COALESCE(SUM(si.subtotal), 0) - COALESCE(SUM(si.quantity * p.purchase_price), 0)) AS profit " +
                     "FROM products p " +
                     "LEFT JOIN sale_items si ON p.id = si.product_id " +
                     "GROUP BY p.id, p.code, p.name, p.purchase_price " +
                     "ORDER BY profit DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("code"),
                        rs.getString("name"),
                        String.valueOf(rs.getInt("total_sold")),
                        String.format("₹%.2f", rs.getDouble("sales_revenue")),
                        String.format("₹%.2f", rs.getDouble("cost_of_goods")),
                        String.format("₹%.2f", rs.getDouble("profit"))
                });
            }
        }
        return list;
    }
}

