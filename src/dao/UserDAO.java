package dao;

import config.DBConnection;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User authenticate(String username, String password) throws SQLException {
        // Ensure default users exist if table is empty
        ensureDefaultUsersExist();

        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setFullName(rs.getString("full_name"));
                    user.setRole(rs.getString("role"));
                    user.setCreatedAt(rs.getTimestamp("created_at"));
                    return user;
                }
            }
        }
        return null;
    }

    private void ensureDefaultUsersExist() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = check.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 0) {
                // Seed default admin and staff
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO users (username, password, full_name, role) VALUES " +
                        "('admin', 'admin123', 'System Administrator', 'ADMIN'), " +
                        "('staff', 'staff123', 'Cashier Desk', 'STAFF')")) {
                    insert.executeUpdate();
                }
            }
        } catch (Exception ex) {
            System.err.println("Could not auto-seed users: " + ex.getMessage());
        }
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, full_name, role, created_at FROM users ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setFullName(rs.getString("full_name"));
                user.setRole(rs.getString("role"));
                user.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(user);
            }
        }
        return list;
    }

    public boolean addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, full_name, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole());
            return ps.executeUpdate() > 0;
        }
    }
}

