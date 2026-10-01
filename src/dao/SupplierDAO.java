package dao;

import config.DBConnection;
import model.Supplier;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO {

    public List<Supplier> getAllSuppliers() throws SQLException {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM suppliers ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Supplier s = new Supplier();
                s.setId(rs.getInt("id"));
                s.setName(rs.getString("name"));
                s.setCompanyName(rs.getString("company_name"));
                s.setPhone(rs.getString("phone"));
                s.setEmail(rs.getString("email"));
                s.setAddress(rs.getString("address"));
                s.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(s);
            }
        }
        return list;
    }

    public boolean addSupplier(Supplier s) throws SQLException {
        String sql = "INSERT INTO suppliers (name, company_name, phone, email, address) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getCompanyName());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getEmail());
            ps.setString(5, s.getAddress());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) s.setId(rs.getInt(1));
                }
                return true;
            }
        }
        return false;
    }

    public boolean updateSupplier(Supplier s) throws SQLException {
        String sql = "UPDATE suppliers SET name = ?, company_name = ?, phone = ?, email = ?, address = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getCompanyName());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getEmail());
            ps.setString(5, s.getAddress());
            ps.setInt(6, s.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteSupplier(int id) throws SQLException {
        String sql = "DELETE FROM suppliers WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Supplier> searchSuppliers(String query) throws SQLException {
        List<Supplier> list = new ArrayList<>();
        String sql = "SELECT * FROM suppliers WHERE name LIKE ? OR company_name LIKE ? OR phone LIKE ? OR email LIKE ? ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String p = "%" + query + "%";
            ps.setString(1, p);
            ps.setString(2, p);
            ps.setString(3, p);
            ps.setString(4, p);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Supplier s = new Supplier();
                    s.setId(rs.getInt("id"));
                    s.setName(rs.getString("name"));
                    s.setCompanyName(rs.getString("company_name"));
                    s.setPhone(rs.getString("phone"));
                    s.setEmail(rs.getString("email"));
                    s.setAddress(rs.getString("address"));
                    s.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(s);
                }
            }
        }
        return list;
    }
}

