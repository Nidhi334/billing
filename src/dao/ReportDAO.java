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

                        // Yearly Sales
            try (PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(total_amount), 0), COUNT(*) FROM sales WHERE YEAR(sale_date) = YEAR(CURRENT_DATE())");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("yearlySalesAmount", rs.getDouble(1));
                    stats.put("yearlySalesCount", rs.getInt(2));
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
        return getProfitLossReport(null, null);
    }

    public List<String[]> getProfitLossReport(String fromDate, String toDate) throws SQLException {
        List<String[]> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.code, p.name, ")
           .append("COALESCE(SUM(si.quantity), 0) AS total_sold, ")
           .append("COALESCE(SUM(si.subtotal), 0) AS sales_revenue, ")
           .append("COALESCE(SUM(si.quantity * p.purchase_price), 0) AS cost_of_goods, ")
           .append("(COALESCE(SUM(si.subtotal), 0) - COALESCE(SUM(si.quantity * p.purchase_price), 0)) AS profit ")
           .append("FROM products p ")
           .append("LEFT JOIN sale_items si ON p.id = si.product_id ");

        if ((fromDate != null && !fromDate.trim().isEmpty()) || (toDate != null && !toDate.trim().isEmpty())) {
            sql.append("LEFT JOIN sales s ON si.sale_id = s.id WHERE 1=1 ");
            if (fromDate != null && !fromDate.trim().isEmpty()) {
                sql.append(" AND DATE(s.sale_date) >= '").append(fromDate.trim()).append("'");
            }
            if (toDate != null && !toDate.trim().isEmpty()) {
                sql.append(" AND DATE(s.sale_date) <= '").append(toDate.trim()).append("'");
            }
        }

        sql.append(" GROUP BY p.id, p.code, p.name, p.purchase_price ")
           .append("HAVING total_sold > 0 ")
           .append("ORDER BY profit DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                double rev = rs.getDouble("sales_revenue");
                double prof = rs.getDouble("profit");
                double margin = rev > 0 ? (prof / rev) * 100.0 : 0.0;
                list.add(new String[]{
                        rs.getString("code"),
                        rs.getString("name"),
                        String.valueOf(rs.getInt("total_sold")),
                        String.format("₹%.2f", rev),
                        String.format("₹%.2f", rs.getDouble("cost_of_goods")),
                        String.format("₹%.2f", prof),
                        String.format("%.1f%%", margin)
                });
            }
        }
        return list;
    }

    public List<String[]> getTaxReport(String fromDate, String toDate) throws SQLException {
        List<String[]> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT s.invoice_no, DATE_FORMAT(s.sale_date, '%d/%m/%Y %H:%i') as s_date, ")
           .append("COALESCE(c.name, 'Walk-in Customer') AS cust, ")
           .append("s.subtotal, s.gst_rate, s.gst_amount, s.total_amount ")
           .append("FROM sales s ")
           .append("LEFT JOIN customers c ON s.customer_id = c.id WHERE 1=1 ");

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
                double gst = rs.getDouble("gst_amount");
                double halfGst = gst / 2.0;
                list.add(new String[]{
                        rs.getString("invoice_no"),
                        rs.getString("s_date"),
                        rs.getString("cust"),
                        String.format("₹%.2f", rs.getDouble("subtotal")),
                        String.format("%.1f%%", rs.getDouble("gst_rate")),
                        String.format("₹%.2f", halfGst),
                        String.format("₹%.2f", halfGst),
                        String.format("₹%.2f", gst),
                        String.format("₹%.2f", rs.getDouble("total_amount"))
                });
            }
        }
        return list;
    }

    public List<String[]> getCashReconciliation(String fromDate, String toDate) throws SQLException {
        List<String[]> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT UPPER(COALESCE(payment_mode, 'OTHER')) as pmode, ")
           .append("COUNT(*) as bill_count, ")
           .append("COALESCE(SUM(total_amount), 0) as total_amt ")
           .append("FROM sales WHERE 1=1 ");

        if (fromDate != null && !fromDate.trim().isEmpty()) {
            sql.append(" AND DATE(sale_date) >= '").append(fromDate.trim()).append("'");
        }
        if (toDate != null && !toDate.trim().isEmpty()) {
            sql.append(" AND DATE(sale_date) <= '").append(toDate.trim()).append("'");
        }
        sql.append(" GROUP BY UPPER(COALESCE(payment_mode, 'OTHER')) ORDER BY total_amt DESC");

        double grandTotal = 0;
        List<Object[]> temp = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String m = rs.getString("pmode");
                int cnt = rs.getInt("bill_count");
                double amt = rs.getDouble("total_amt");
                grandTotal += amt;
                temp.add(new Object[]{m, cnt, amt});
            }
        }

        for (Object[] r : temp) {
            String m = (String) r[0];
            int cnt = (Integer) r[1];
            double amt = (Double) r[2];
            double pct = grandTotal > 0 ? (amt / grandTotal) * 100.0 : 0.0;
            list.add(new String[]{
                    m,
                    String.valueOf(cnt),
                    String.format("₹%.2f", amt),
                    String.format("%.1f%%", pct),
                    "Reconciled / Settled"
            });
        }
        return list;
    }

    public Map<String, Double> getDailySalesTrend(int days) throws SQLException {
        Map<String, Double> map = new java.util.LinkedHashMap<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        for (int i = days - 1; i >= 0; i--) {
            java.util.Calendar temp = (java.util.Calendar) cal.clone();
            temp.add(java.util.Calendar.DAY_OF_YEAR, -i);
            map.put(sdf.format(temp.getTime()), 0.0);
        }

        String sql = "SELECT DATE_FORMAT(sale_date, '%d/%m') as day_lbl, COALESCE(SUM(total_amount), 0) as amt " +
                     "FROM sales " +
                     "WHERE sale_date >= DATE_SUB(CURRENT_DATE(), INTERVAL ? DAY) " +
                     "GROUP BY DATE_FORMAT(sale_date, '%d/%m') " +
                     "ORDER BY MIN(sale_date) ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("day_lbl"), rs.getDouble("amt"));
                }
            }
        } catch (Exception e) {
            // keep seeded
        }
        return map;
    }

    public Map<String, Double> getMonthlyRevenueTrend(int months) throws SQLException {
        Map<String, Double> map = new java.util.LinkedHashMap<>();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM yy");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        for (int i = months - 1; i >= 0; i--) {
            java.util.Calendar temp = (java.util.Calendar) cal.clone();
            temp.add(java.util.Calendar.MONTH, -i);
            map.put(sdf.format(temp.getTime()), 0.0);
        }

        String sql = "SELECT DATE_FORMAT(sale_date, '%b %y') as mon_lbl, COALESCE(SUM(total_amount), 0) as amt " +
                     "FROM sales " +
                     "WHERE sale_date >= DATE_SUB(CURRENT_DATE(), INTERVAL ? MONTH) " +
                     "GROUP BY DATE_FORMAT(sale_date, '%b %y'), YEAR(sale_date), MONTH(sale_date) " +
                     "ORDER BY YEAR(sale_date) ASC, MONTH(sale_date) ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, months);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("mon_lbl"), rs.getDouble("amt"));
                }
            }
        } catch (Exception e) {
            // keep seeded
        }
        return map;
    }

    public Map<String, Double> getYearlyRevenueTrend(int years) throws SQLException {
        Map<String, Double> map = new java.util.LinkedHashMap<>();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int curYear = cal.get(java.util.Calendar.YEAR);
        for (int i = years - 1; i >= 0; i--) {
            map.put(String.valueOf(curYear - i), 0.0);
        }

        String sql = "SELECT YEAR(sale_date) as yr_lbl, COALESCE(SUM(total_amount), 0) as amt " +
                     "FROM sales " +
                     "WHERE YEAR(sale_date) >= ? " +
                     "GROUP BY YEAR(sale_date) " +
                     "ORDER BY YEAR(sale_date) ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, curYear - years + 1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(String.valueOf(rs.getInt("yr_lbl")), rs.getDouble("amt"));
                }
            }
        } catch (Exception e) {
            // keep seeded
        }
        return map;
    }

    public Map<String, Double> getCategorySalesBreakdown() throws SQLException {
        Map<String, Double> map = new java.util.LinkedHashMap<>();
        String sql = "SELECT COALESCE(c.name, 'General / Other') as cat_name, COALESCE(SUM(si.subtotal), 0) as total " +
                     "FROM sale_items si " +
                     "JOIN products p ON si.product_id = p.id " +
                     "LEFT JOIN categories c ON p.category_id = c.id " +
                     "GROUP BY c.name " +
                     "ORDER BY total DESC LIMIT 6";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("cat_name"), rs.getDouble("total"));
            }
        }
        return map;
    }

    public Map<String, Double> getRevenueByPaymentSource() throws SQLException {
        Map<String, Double> map = new java.util.LinkedHashMap<>();
        String sql = "SELECT COALESCE(payment_mode, 'OTHER') as pmode, COALESCE(SUM(total_amount), 0) as total " +
                     "FROM sales " +
                     "GROUP BY payment_mode " +
                     "ORDER BY total DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("pmode"), rs.getDouble("total"));
            }
        }
        return map;
    }

    public Map<String, Double> getTopSellingProducts(int limit, int days, boolean exactDay) throws SQLException {
        Map<String, Double> map = new java.util.LinkedHashMap<>();
        String condition = exactDay ? "= DATE_SUB(CURRENT_DATE(), INTERVAL ? DAY)" : ">= DATE_SUB(CURRENT_DATE(), INTERVAL ? DAY)";
        String sql = "SELECT p.name, COALESCE(SUM(si.quantity), 0) as total_qty " +
                     "FROM sale_items si " +
                     "JOIN products p ON si.product_id = p.id " +
                     "JOIN sales s ON si.sale_id = s.id " +
                     "WHERE s.sale_date " + condition + " " +
                     "GROUP BY p.id, p.name " +
                     "ORDER BY total_qty DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, days);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("name"), (double) rs.getInt("total_qty"));
                }
            }
        }
        return map;
    }

    public Map<String, Double> getTopSellingProducts(int limit, int days) throws SQLException {
        return getTopSellingProducts(limit, days, false);
    }

    public Map<String, Double> getTopSellingProducts(int limit) throws SQLException {
        return getTopSellingProducts(limit, 30); // default to 30 days
    }

    public List<Map<String, Object>> getRecentSales(int limit) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT s.invoice_no, s.sale_date, COALESCE(c.name, 'Walk-in Customer') AS customer_name, " +
                     "s.total_amount, s.payment_mode " +
                     "FROM sales s " +
                     "LEFT JOIN customers c ON s.customer_id = c.id " +
                     "ORDER BY s.id DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("invoiceNo", rs.getString("invoice_no"));
                    row.put("saleDate", rs.getString("sale_date"));
                    row.put("customer", rs.getString("customer_name"));
                    row.put("totalAmount", rs.getDouble("total_amount"));
                    row.put("paymentMode", rs.getString("payment_mode"));
                    list.add(row);
                }
            }
        }
        return list;
    }

    public List<Map<String, Object>> getLowStockProducts(int limit) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT p.code, p.name, COALESCE(c.name, 'General') as category, p.quantity, p.min_stock_level " +
                     "FROM products p " +
                     "LEFT JOIN categories c ON p.category_id = c.id " +
                     "WHERE p.quantity <= p.min_stock_level " +
                     "ORDER BY p.quantity ASC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("code", rs.getString("code"));
                    row.put("name", rs.getString("name"));
                    row.put("category", rs.getString("category"));
                    row.put("qty", rs.getInt("quantity"));
                    row.put("minStock", rs.getInt("min_stock_level"));
                    list.add(row);
                }
            }
        }
        return list;
    }

    public Map<String, Object> getFinancialOverview() throws SQLException {
        return getFinancialOverview(null, null);
    }

    public Map<String, Object> getFinancialOverview(String fromDate, String toDate) throws SQLException {
        Map<String, Object> fin = new HashMap<>();
        double totalRevenue = 0.0;
        double costOfGoods = 0.0;
        double totalPurchases = 0.0;
        double totalGst = 0.0;
        int totalInvoices = 0;
        double cashRevenue = 0.0;
        double upiRevenue = 0.0;
        double cardRevenue = 0.0;

        StringBuilder dateFilter = new StringBuilder();
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            dateFilter.append(" AND DATE(sale_date) >= '").append(fromDate.trim()).append("'");
        }
        if (toDate != null && !toDate.trim().isEmpty()) {
            dateFilter.append(" AND DATE(sale_date) <= '").append(toDate.trim()).append("'");
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sqlRevenue = "SELECT COALESCE(SUM(total_amount), 0), COALESCE(SUM(gst_amount), 0), COUNT(*) FROM sales WHERE 1=1 " + dateFilter;
            try (PreparedStatement ps = conn.prepareStatement(sqlRevenue);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    totalRevenue = rs.getDouble(1);
                    totalGst = rs.getDouble(2);
                    totalInvoices = rs.getInt(3);
                }
            }

            StringBuilder cogsFilter = new StringBuilder();
            if (fromDate != null && !fromDate.trim().isEmpty()) {
                cogsFilter.append(" AND DATE(s.sale_date) >= '").append(fromDate.trim()).append("'");
            }
            if (toDate != null && !toDate.trim().isEmpty()) {
                cogsFilter.append(" AND DATE(s.sale_date) <= '").append(toDate.trim()).append("'");
            }

            String sqlCogs = "SELECT COALESCE(SUM(si.quantity * p.purchase_price), 0) " +
                            "FROM sale_items si " +
                            "JOIN products p ON si.product_id = p.id " +
                            "JOIN sales s ON si.sale_id = s.id WHERE 1=1 " + cogsFilter;
            try (PreparedStatement ps = conn.prepareStatement(sqlCogs);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) costOfGoods = rs.getDouble(1);
            }

            String sqlPur = "SELECT COALESCE(SUM(total_amount), 0) FROM purchases";
            try (PreparedStatement ps = conn.prepareStatement(sqlPur);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) totalPurchases = rs.getDouble(1);
            }

            String sqlModes = "SELECT UPPER(COALESCE(payment_mode, 'OTHER')) as pmode, COALESCE(SUM(total_amount), 0) as amt " +
                              "FROM sales WHERE 1=1 " + dateFilter + " GROUP BY UPPER(COALESCE(payment_mode, 'OTHER'))";
            try (PreparedStatement ps = conn.prepareStatement(sqlModes);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String m = rs.getString("pmode");
                    double amt = rs.getDouble("amt");
                    if (m.contains("CASH")) cashRevenue += amt;
                    else if (m.contains("UPI")) upiRevenue += amt;
                    else if (m.contains("CARD")) cardRevenue += amt;
                }
            }
        }

        double grossProfit = Math.max(0, totalRevenue - costOfGoods);
        double profitMargin = totalRevenue > 0 ? (grossProfit / totalRevenue) * 100.0 : 0.0;

        fin.put("totalRevenue", totalRevenue);
        fin.put("costOfGoods", costOfGoods);
        fin.put("totalPurchases", totalPurchases);
        fin.put("grossProfit", grossProfit);
        fin.put("profitMargin", profitMargin);
        fin.put("totalGst", totalGst);
        fin.put("totalInvoices", totalInvoices);
        fin.put("cashRevenue", cashRevenue);
        fin.put("upiRevenue", upiRevenue);
        fin.put("cardRevenue", cardRevenue);

        return fin;
    }
}
