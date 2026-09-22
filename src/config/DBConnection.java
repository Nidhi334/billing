package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static final String CONFIG_FILE = "db_config.properties";
    private static Properties properties = new Properties();

    static {
        loadConfig();
    }

    private static void loadConfig() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Failed to read db_config.properties, using defaults: " + e.getMessage());
            }
        } else {
            // Default configuration
            properties.setProperty("db.host", "localhost");
            properties.setProperty("db.port", "3306");
            properties.setProperty("db.name", "billing_system");
            properties.setProperty("db.user", "root");
            properties.setProperty("db.password", "");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                properties.store(fos, "Database Configuration Settings");
            } catch (IOException e) {
                System.err.println("Could not create default config: " + e.getMessage());
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        String host = properties.getProperty("db.host", "localhost");
        String port = properties.getProperty("db.port", "3306");
        String dbName = properties.getProperty("db.name", "billing_system");
        String user = properties.getProperty("db.user", "root");
        String password = properties.getProperty("db.password", "");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found in classpath!", e);
        }

        // Try connecting directly to the target database first
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            // Error code 1049 is ER_BAD_DB_ERROR ("Unknown database 'billing_system'")
            if (e.getErrorCode() == 1049 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("unknown database"))) {
                // Auto-create database & initialize schema
                initDatabaseAndSchema(host, port, dbName, user, password);
                return DriverManager.getConnection(url, user, password);
            }
            throw e;
        }
    }

    private static void initDatabaseAndSchema(String host, String port, String dbName, String user, String password) throws SQLException {
        String serverUrl = "jdbc:mysql://" + host + ":" + port + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try (Connection conn = DriverManager.getConnection(serverUrl, user, password);
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
        }

        // Run schema tables script
        String dbUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        File schemaFile = new File("database/schema.sql");
        if (schemaFile.exists()) {
            try (Connection conn = DriverManager.getConnection(dbUrl, user, password);
                 java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(schemaFile));
                 java.sql.Statement stmt = conn.createStatement()) {
                StringBuilder sql = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("--") || trimmed.startsWith("/*") || trimmed.isEmpty()) {
                        continue;
                    }
                    if (trimmed.toUpperCase().startsWith("CREATE DATABASE") || trimmed.toUpperCase().startsWith("USE ")) {
                        continue;
                    }
                    sql.append(line).append("\n");
                    if (trimmed.endsWith(";")) {
                        String query = sql.toString().trim();
                        query = query.substring(0, query.length() - 1).trim();
                        if (!query.isEmpty()) {
                            try {
                                stmt.execute(query);
                            } catch (SQLException sqe) {
                                System.err.println("Schema setup query warning: " + sqe.getMessage());
                            }
                        }
                        sql.setLength(0);
                    }
                }
            } catch (Exception ex) {
                System.err.println("Notice: Could not automatically execute schema.sql: " + ex.getMessage());
            }
        }
    }

    public static void saveConfig(String host, String port, String dbName, String user, String password) {
        properties.setProperty("db.host", host);
        properties.setProperty("db.port", port);
        properties.setProperty("db.name", dbName);
        properties.setProperty("db.user", user);
        properties.setProperty("db.password", password);
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "Updated Database Configuration Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Properties getProperties() {
        return properties;
    }
}

