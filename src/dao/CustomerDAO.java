package dao;

import config.DBConnection;
import model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    public List<Customer> getAllCustomers() throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Customer c = new Customer();
                c.setId(rs.getInt("id"));
                c.setName(rs.getString("name"));
                c.setPhone(rs.getString("phone"));
                c.setEmail(rs.getString("email"));
                c.setAddress(rs.getString("address"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(c);
            }
        }
        return list;
    }

    public List<Customer> searchCustomers(String keyword) throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customers WHERE name LIKE ? OR phone LIKE ? OR email LIKE ? ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String p = "%" + keyword + "%";
            ps.setString(1, p);
            ps.setString(2, p);
            ps.setString(3, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Customer c = new Customer();
                    c.setId(rs.getInt("id"));
                    c.setName(rs.getString("name"));
                    c.setPhone(rs.getString("phone"));
                    c.setEmail(rs.getString("email"));
                    c.setAddress(rs.getString("address"));
                    c.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(c);
                }
            }
        }
        return list;
    }

    public Customer getCustomerByPhone(String phone) throws SQLException {
        if (phone == null || phone.trim().isEmpty()) return null;
        String sql = "SELECT * FROM customers WHERE phone = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Customer c = new Customer();
                    c.setId(rs.getInt("id"));
                    c.setName(rs.getString("name"));
                    c.setPhone(rs.getString("phone"));
                    c.setEmail(rs.getString("email"));
                    c.setAddress(rs.getString("address"));
                    c.setCreatedAt(rs.getTimestamp("created_at"));
                    return c;
                }
            }
        }
        return null;
    }

    public boolean addCustomer(Customer c) throws SQLException {
        String sql = "INSERT INTO customers (name, phone, email, address) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getAddress());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) c.setId(rs.getInt(1));
                }
                return true;
            }
        }
        return false;
    }

    public boolean updateCustomer(Customer c) throws SQLException {
        String sql = "UPDATE customers SET name = ?, phone = ?, email = ?, address = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getPhone());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getAddress());
            ps.setInt(5, c.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteCustomer(int id) throws SQLException {
        String sql = "DELETE FROM customers WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public double[] getCustomerStats(int customerId) {
        String sql = "SELECT COUNT(*) AS total_orders, COALESCE(SUM(total_amount), 0) AS total_spent FROM sales WHERE customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new double[]{rs.getDouble("total_orders"), rs.getDouble("total_spent")};
                }
            }
        } catch (Exception ignored) {}
        return new double[]{0.0, 0.0};
    }
}

