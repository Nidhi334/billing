package dao;

import config.DBConnection;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private static boolean userColumnsChecked = false;

    private synchronized void ensureSecurityColumns(Connection conn) {
        if (userColumnsChecked) return;
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE users ADD COLUMN security_question VARCHAR(255) DEFAULT 'What is your favorite color?' AFTER role");
        } catch (Exception ignored) {}

        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE users ADD COLUMN security_answer VARCHAR(255) DEFAULT 'blue' AFTER security_question");
        } catch (Exception ignored) {}

        userColumnsChecked = true;
    }

    public User authenticate(String username, String password) throws SQLException {
        // Ensure default users exist if table is empty
        ensureDefaultUsersExist();

        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection()) {
            ensureSecurityColumns(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapUser(rs);
                    }
                }
            }
        }
        return null;
    }

    private void ensureDefaultUsersExist() {
        try (Connection conn = DBConnection.getConnection()) {
            ensureSecurityColumns(conn);
            try (PreparedStatement insert = conn.prepareStatement(
                    "INSERT IGNORE INTO users (username, password, full_name, role, security_question, security_answer) VALUES " +
                    "('admin', 'admin123', 'System Administrator', 'ADMIN', 'What is your pet name?', 'tiger'), " +
                    "('staff', 'staff123', 'Cashier Desk', 'STAFF', 'What is your pet name?', 'buddy')")) {
                insert.executeUpdate();
            }
        } catch (Exception ex) {
            System.err.println("Could not auto-seed users: " + ex.getMessage());
        }
    }

    public User getUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection()) {
            ensureSecurityColumns(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapUser(rs);
                    }
                }
            }
        }
        return null;
    }

    public boolean resetPassword(String username, String securityAnswer, String newPassword) throws SQLException {
        String sql = "UPDATE users SET password = ? WHERE username = ? AND LOWER(TRIM(security_answer)) = LOWER(TRIM(?))";
        try (Connection conn = DBConnection.getConnection()) {
            ensureSecurityColumns(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newPassword);
                ps.setString(2, username);
                ps.setString(3, securityAnswer);
                return ps.executeUpdate() > 0;
            }
        }
    }

    public boolean updateSecurityQuestion(int userId, String question, String answer) throws SQLException {
        String sql = "UPDATE users SET security_question = ?, security_answer = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            ensureSecurityColumns(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, question);
                ps.setString(2, answer);
                ps.setInt(3, userId);
                return ps.executeUpdate() > 0;
            }
        }
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection()) {
            ensureSecurityColumns(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapUser(rs));
                }
            }
        }
        return list;
    }

    public boolean addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password, full_name, role, security_question, security_answer) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            ensureSecurityColumns(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPassword());
                ps.setString(3, user.getFullName());
                ps.setString(4, user.getRole());
                ps.setString(5, user.getSecurityQuestion() != null ? user.getSecurityQuestion() : "What is your favorite color?");
                ps.setString(6, user.getSecurityAnswer() != null ? user.getSecurityAnswer() : "blue");
                return ps.executeUpdate() > 0;
            }
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        try {
            user.setSecurityQuestion(rs.getString("security_question"));
            user.setSecurityAnswer(rs.getString("security_answer"));
        } catch (Exception ignored) {}
        return user;
    }
}
