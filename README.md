# SmartBilling Pro - Billing & Inventory Management System

A desktop **Billing & Inventory Management System** built with **Java Swing** and **MySQL**, designed for retail shops, electronics stores, and wholesale businesses.

---

## 🎯 Features

1. **User & Authentication Management**
   - Role-Based Access Control (Admin vs Staff).
   - Staff has restricted access (billing & customer handling), while Admin has complete system & reports access.
   - Built-in Database connection settings dialog to easily configure MySQL credentials without modifying code.

2. **Product & Category Management**
   - Full CRUD (Create, Read, Update, Delete) operations.
   - Real-time search by product code, name, or category.
   - Purchase price, selling price, current stock, and minimum alert thresholds.
   - Automatic visual tags: `IN STOCK`, `LOW STOCK`, and `OUT OF STOCK`.

3. **Inventory & Stock Tracking**
   - Quick **Stock-In** entries when goods arrive from suppliers.
   - Automatic stock reduction on bill checkout.
   - **Audit trail / Stock movement log** recording every transaction (`IN` vs `OUT`).
   - Dedicated Low-Stock & Out-of-Stock alert dashboard.

4. **Point-of-Sale (POS) & Billing Desk**
   - Real-time cart calculation with quantity tracking and live stock checks (prevents overselling).
   - Dynamic GST tax calculations (configurable tax rate).
   - Unique auto-increment invoice generator (`INV-YYYYMMDD-XXXX`).
   - Support for multiple payment methods: `CASH`, `ONLINE`, `CARD`, `CREDIT`.
   - **Printable Thermal/A4 style Invoice Dialog** with print job integration (`PrinterJob`).

5. **Customer & Supplier Management**
   - Track customer profiles, contact info, and addresses.
   - Manage suppliers & vendor companies for wholesale purchases.

6. **Executive Dashboard & Reports**
   - Metric cards: Today's sales, Monthly sales, Total revenue, Low stock alerts, total products, units, customers, and vendors.
   - Filterable Sales Invoice log.
   - **Product-level Profit & Loss Analysis**: Real-time comparison between purchase costs and sales revenues to calculate actual shop profits.

---

## 🏗️ Project Architecture

```
billing/
├── lib/
│   └── mysql-connector-j-26.7.0.jar      # MySQL JDBC Driver
├── database/
│   └── schema.sql                        # Database Schema & Seed Data
├── src/
│   ├── App.java                          # Main launcher
│   ├── config/
│   │   └── DBConnection.java             # Database connection & credentials manager
│   ├── model/
│   │   ├── User.java
│   │   ├── Category.java
│   │   ├── Product.java
│   │   ├── Customer.java
│   │   ├── Supplier.java
│   │   ├── Sale.java
│   │   └── SaleItem.java
│   ├── dao/
│   │   ├── UserDAO.java
│   │   ├── CategoryDAO.java
│   │   ├── ProductDAO.java
│   │   ├── CustomerDAO.java
│   │   ├── SupplierDAO.java
│   │   ├── PurchaseDAO.java
│   │   ├── BillingDAO.java
│   │   └── ReportDAO.java
│   └── ui/
│       ├── LoginFrame.java
│       ├── DashboardFrame.java
│       ├── ProductPanel.java
│       ├── CategoryDialog.java
│       ├── InventoryPanel.java
│       ├── CustomerPanel.java
│       ├── SupplierPanel.java
│       ├── BillingPanel.java
│       ├── InvoiceDialog.java
│       └── ReportsPanel.java
└── run.sh                                # Quick compile and launch script
```

---

## 🚀 Setup & Execution Guide

### 1. Set up MySQL Database
Open your MySQL client / terminal / phpMyAdmin and run the provided SQL script:

```bash
mysql -u root -p < database/schema.sql
```
*(Or import [database/schema.sql](file:///home/nidhi/Projects/billing/database/schema.sql) in MySQL Workbench or phpMyAdmin).*

This creates the `billing_system` database with all tables, constraints, foreign keys, and default sample data.

### 2. Default Login Credentials
- **Admin**:
  - Username: `admin`
  - Password: `admin123`
- **Staff**:
  - Username: `staff`
  - Password: `staff123`

### 3. Database Credentials Configuration
If your MySQL password is not blank or running on a different port/host:
1. Click the **"⚙ Database Settings"** button on the login screen.
2. Enter your Host, Port, Database name, User, and Password, and click OK.
3. Settings are saved in `db_config.properties`.

### 4. Compile & Run

Using the included script:
```bash
./run.sh
```

Or manually:
```bash
# Compile
javac -cp "lib/*:src" -d bin src/config/*.java src/model/*.java src/dao/*.java src/ui/*.java src/App.java

# Run
java -cp "bin:lib/*" App
```

