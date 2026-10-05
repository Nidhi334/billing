-- =======================================================
-- Billing & Inventory Management System Database Schema
-- Compatible with MySQL 5.7+ / MySQL 8.0+
-- =======================================================

CREATE DATABASE IF NOT EXISTS billing_system;
USE billing_system;

-- 1. Users table (Admin & Staff)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'STAFF') NOT NULL DEFAULT 'STAFF',
    security_question VARCHAR(255) DEFAULT 'What is your favorite color?',
    security_answer VARCHAR(255) DEFAULT 'blue',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Categories table
CREATE TABLE IF NOT EXISTS categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

-- 3. Suppliers table
CREATE TABLE IF NOT EXISTS suppliers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    company_name VARCHAR(150),
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Products table
CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    barcode VARCHAR(50) NULL,
    name VARCHAR(150) NOT NULL,
    category_id INT,
    purchase_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    selling_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    quantity INT NOT NULL DEFAULT 0,
    min_stock_level INT NOT NULL DEFAULT 5,
    image_path VARCHAR(255) NULL,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- 5. Customers table
CREATE TABLE IF NOT EXISTS customers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(100),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. Purchases table (Stock In)
CREATE TABLE IF NOT EXISTS purchases (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_no VARCHAR(50) UNIQUE NOT NULL,
    supplier_id INT,
    purchase_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL
);

-- 7. Purchase items
CREATE TABLE IF NOT EXISTS purchase_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_cost DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    FOREIGN KEY (purchase_id) REFERENCES purchases(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

-- 8. Sales / Invoices table (Stock Out)
CREATE TABLE IF NOT EXISTS sales (
    id INT AUTO_INCREMENT PRIMARY KEY,
    invoice_no VARCHAR(50) UNIQUE NOT NULL,
    customer_id INT,
    sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    subtotal DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    gst_rate DECIMAL(5, 2) NOT NULL DEFAULT 18.00,
    gst_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    payment_mode VARCHAR(50) DEFAULT 'CASH',
    created_by INT,
    FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- 9. Sale items
CREATE TABLE IF NOT EXISTS sale_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sale_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

-- 10. Inventory / Stock transactions log
CREATE TABLE IF NOT EXISTS stock_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    type ENUM('IN', 'OUT', 'ADJUSTMENT') NOT NULL,
    quantity INT NOT NULL,
    reference_id VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- =======================================================
-- Seed Initial Data
-- =======================================================

-- Default Users (Password: admin123 and staff123)
INSERT IGNORE INTO users (id, username, password, full_name, role) VALUES
(1, 'admin', 'admin123', 'System Administrator', 'ADMIN'),
(2, 'staff', 'staff123', 'Cashier Desk', 'STAFF');

-- Categories
INSERT IGNORE INTO categories (id, name, description) VALUES
(1, 'Electronics', 'Laptops, Monitors, Gadgets'),
(2, 'Accessories', 'Mice, Keyboards, Cables'),
(3, 'Stationery', 'Office supplies, paper, notebooks');
`
-- Suppliers
INSERT IGNORE INTO suppliers (id, name, company_name, phone, email, address) VALUES
(1, 'Tech Wholesale Ltd', 'Tech Distributors Co.', '9876543210', 'sales@techwholesale.com', 'Plot 42, Industrial Area, Delhi'),
(2, 'Global Office Hub', 'Global Supplies Inc', '9811223344', 'contact@globalsupplies.com', 'MG Road, Bangalore');

-- Sample Products
INSERT IGNORE INTO products (id, code, name, category_id, purchase_price, selling_price, quantity, min_stock_level) VALUES
(1, 'PRD-001', 'Dell Inspiron Laptop', 1, 42000.00, 50000.00, 10, 3),
(2, 'PRD-002', 'Wireless Optical Mouse', 2, 350.00, 500.00, 25, 5),
(3, 'PRD-003', 'Mechanical Keyboard', 2, 550.00, 800.00, 15, 4),
(4, 'PRD-004', 'A4 Paper Ream (500 Sheets)', 3, 200.00, 320.00, 4, 10);

-- Sample Customers
INSERT IGNORE INTO customers (id, name, phone, email, address) VALUES
(1, 'Rahul Sharma', '9988776655', 'rahul.s@example.com', 'Sector 14, Gurgaon'),
(2, 'Priya Verma', '9123456780', 'priya.v@example.com', 'Connaught Place, New Delhi');

