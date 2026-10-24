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
(3, 'Stationery', 'Office supplies, paper, notebooks'),
(4, 'Storage & Networking', 'USB Drives, Adapters, Cables');

-- Suppliers
INSERT IGNORE INTO suppliers (id, name, company_name, phone, email, address) VALUES
(1, 'Tech Wholesale Ltd', 'Tech Distributors Co.', '9876543210', 'sales@techwholesale.com', 'Plot 42, Industrial Area, Delhi'),
(2, 'Global Office Hub', 'Global Supplies Inc', '9811223344', 'contact@globalsupplies.com', 'MG Road, Bangalore');

-- Sample 10 Products with Fixed, Permanent Barcodes and Images
INSERT INTO products (id, code, barcode, name, category_id, purchase_price, selling_price, quantity, min_stock_level, image_path) VALUES
(1, 'PRD-001', '8901030383124', 'Dell Inspiron 15 Laptop', 1, 42000.00, 50000.00, 15, 3, 'data/product_images/PRD-001.jpg'),
(2, 'PRD-002', '8901499028145', 'Logitech B170 Wireless Mouse', 2, 450.00, 799.00, 35, 5, 'data/product_images/PRD-002.jpg'),
(3, 'PRD-003', '8901262010051', 'Mechanical Gaming Keyboard RGB', 2, 1200.00, 1899.00, 20, 4, 'data/product_images/PRD-003.jpg'),
(4, 'PRD-004', '8901058852336', 'Samsung 24-inch FHD Monitor', 1, 7500.00, 9499.00, 12, 3, 'data/product_images/PRD-004.jpg'),
(5, 'PRD-005', '8901030584910', 'Boat Rockerz 450 Bluetooth Headphones', 2, 850.00, 1499.00, 28, 5, 'data/product_images/PRD-005.jpg'),
(6, 'PRD-006', '8901725131013', 'JK Copier A4 Paper Ream (500 Sheets)', 3, 210.00, 340.00, 60, 10, 'data/product_images/PRD-006.jpg'),
(7, 'PRD-007', '8901324021038', 'Parker Classic Gold Ballpoint Pen', 3, 220.00, 350.00, 40, 8, 'data/product_images/PRD-007.jpg'),
(8, 'PRD-008', '8902579100142', 'SanDisk 128GB Ultra USB 3.0 Flash Drive', 4, 480.00, 799.00, 45, 6, 'data/product_images/PRD-008.jpg'),
(9, 'PRD-009', '8901030822197', 'Portronics 6-in-1 USB-C Hub Adapter', 2, 950.00, 1599.00, 18, 4, 'data/product_images/PRD-009.jpg'),
(10, 'PRD-010', '8901058002151', 'Classmate Pulse Spiral Hardbound Notebook', 3, 110.00, 195.00, 50, 10, 'data/product_images/PRD-010.jpg')
ON DUPLICATE KEY UPDATE 
    barcode = VALUES(barcode),
    name = VALUES(name),
    category_id = VALUES(category_id),
    purchase_price = VALUES(purchase_price),
    selling_price = VALUES(selling_price),
    quantity = VALUES(quantity),
    min_stock_level = VALUES(min_stock_level),
    image_path = VALUES(image_path);

-- Sample Customers
INSERT IGNORE INTO customers (id, name, phone, email, address) VALUES
(1, 'Rahul Sharma', '9988776655', 'rahul.s@example.com', 'Sector 14, Gurgaon'),
(2, 'Priya Verma', '9123456780', 'priya.v@example.com', 'Connaught Place, New Delhi');

