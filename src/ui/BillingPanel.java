package ui;

import dao.BillingDAO;
import dao.CustomerDAO;
import dao.ProductDAO;
import model.Customer;
import model.Product;
import model.Sale;
import model.SaleItem;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BillingPanel extends JPanel {
    private ProductDAO productDAO = new ProductDAO();
    private CustomerDAO customerDAO = new CustomerDAO();
    private BillingDAO billingDAO = new BillingDAO();
    private User currentUser;

    private JComboBox<Customer> cmbCustomer;
    private JComboBox<Product> cmbProduct;
    private JTextField txtQty, txtUnitPrice, txtStockAvail, txtInvoiceNo;
    private JComboBox<String> cmbPaymentMode;
    private JTextField txtGstRate;
    private JLabel lblSubtotal, lblGstAmount, lblGrandTotal;

    private DefaultTableModel cartModel;
    private JTable cartTable;
    private List<SaleItem> cartItems = new ArrayList<>();

    private double subtotal = 0.0;
    private double gstRate = 18.0;
    private double gstAmount = 0.0;
    private double grandTotal = 0.0;

    public BillingPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(248, 250, 252));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        initComponents();
        loadCustomers();
        loadProducts();
        resetBillingDesk();
    }

    private void initComponents() {
        // TOP: Billing Header Info
        JPanel topPanel = new JPanel(new GridLayout(1, 4, 15, 5));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(12, 15, 12, 15)
        ));

        // Invoice No
        JPanel p1 = new JPanel(new BorderLayout(5, 5));
        p1.setBackground(Color.WHITE);
        p1.add(new JLabel("Invoice No:"), BorderLayout.NORTH);
        txtInvoiceNo = new JTextField();
        txtInvoiceNo.setEditable(false);
        txtInvoiceNo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtInvoiceNo.setForeground(new Color(37, 99, 235));
        p1.add(txtInvoiceNo, BorderLayout.CENTER);
        topPanel.add(p1);

        // Customer selection
        JPanel p2 = new JPanel(new BorderLayout(5, 5));
        p2.setBackground(Color.WHITE);
        p2.add(new JLabel("Customer:"), BorderLayout.NORTH);
        cmbCustomer = new JComboBox<>();
        p2.add(cmbCustomer, BorderLayout.CENTER);
        topPanel.add(p2);

        // Payment Mode
        JPanel p3 = new JPanel(new BorderLayout(5, 5));
        p3.setBackground(Color.WHITE);
        p3.add(new JLabel("Payment Mode:"), BorderLayout.NORTH);
        cmbPaymentMode = new JComboBox<>(new String[]{"CASH", "ONLINE", "CARD", "CREDIT"});
        p3.add(cmbPaymentMode, BorderLayout.CENTER);
        topPanel.add(p3);

        // Cashier
        JPanel p4 = new JPanel(new BorderLayout(5, 5));
        p4.setBackground(Color.WHITE);
        p4.add(new JLabel("Cashier / User:"), BorderLayout.NORTH);
        JLabel lblCashier = new JLabel(currentUser != null ? currentUser.getFullName() + " (" + currentUser.getRole() + ")" : "Admin");
        lblCashier.setFont(new Font("Segoe UI", Font.BOLD, 13));
        p4.add(lblCashier, BorderLayout.CENTER);
        topPanel.add(p4);

        add(topPanel, BorderLayout.NORTH);

        // CENTER: Item Entry Form + Cart Table
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(new Color(248, 250, 252));

        // Item Add Toolbar
        JPanel itemBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        itemBar.setBackground(Color.WHITE);
        itemBar.setBorder(BorderFactory.createTitledBorder("Add Item to Bill"));

        itemBar.add(new JLabel("Select Product:"));
        cmbProduct = new JComboBox<>();
        cmbProduct.setPreferredSize(new Dimension(220, 30));
        itemBar.add(cmbProduct);

        itemBar.add(new JLabel("Stock Available:"));
        txtStockAvail = new JTextField(4);
        txtStockAvail.setEditable(false);
        txtStockAvail.setHorizontalAlignment(JTextField.CENTER);
        itemBar.add(txtStockAvail);

        itemBar.add(new JLabel("Unit Price (₹):"));
        txtUnitPrice = new JTextField(7);
        txtUnitPrice.setEditable(false);
        itemBar.add(txtUnitPrice);

        itemBar.add(new JLabel("Quantity:"));
        txtQty = new JTextField("1", 4);
        txtQty.setHorizontalAlignment(JTextField.CENTER);
        itemBar.add(txtQty);

        JButton btnAddItem = new JButton("➕ Add to Cart");
        btnAddItem.setBackground(new Color(16, 185, 129));
        btnAddItem.setForeground(Color.WHITE);
        btnAddItem.setFont(new Font("Segoe UI", Font.BOLD, 12));
        itemBar.add(btnAddItem);

        JButton btnRemoveItem = new JButton("❌ Remove Item");
        btnRemoveItem.setBackground(new Color(239, 68, 68));
        btnRemoveItem.setForeground(Color.WHITE);
        itemBar.add(btnRemoveItem);

        centerPanel.add(itemBar, BorderLayout.NORTH);

        // Cart Table
        String[] cartCols = {"#", "Product Code", "Product Name", "Unit Price (₹)", "Quantity", "Total (₹)"};
        cartModel = new DefaultTableModel(cartCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(26);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        centerPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // RIGHT / SOUTH: Summary Totals & Checkout
        JPanel bottomPanel = new JPanel(new BorderLayout(15, 15));
        bottomPanel.setBackground(new Color(248, 250, 252));

        // Bill Summary Card
        JPanel summaryCard = new JPanel(new GridLayout(4, 2, 10, 8));
        summaryCard.setBackground(Color.WHITE);
        summaryCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)),
                new EmptyBorder(15, 20, 15, 20)
        ));
        summaryCard.setPreferredSize(new Dimension(360, 150));

        summaryCard.add(new JLabel("Subtotal:"));
        lblSubtotal = new JLabel("₹0.00", SwingConstants.RIGHT);
        lblSubtotal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        summaryCard.add(lblSubtotal);

        JPanel gstLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        gstLabelPanel.setBackground(Color.WHITE);
        gstLabelPanel.add(new JLabel("GST Rate (%): "));
        txtGstRate = new JTextField("18.0", 3);
        gstLabelPanel.add(txtGstRate);
        summaryCard.add(gstLabelPanel);

        lblGstAmount = new JLabel("₹0.00", SwingConstants.RIGHT);
        lblGstAmount.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        summaryCard.add(lblGstAmount);

        JLabel lblGrandTitle = new JLabel("GRAND TOTAL:");
        lblGrandTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblGrandTitle.setForeground(new Color(30, 41, 59));
        summaryCard.add(lblGrandTitle);

        lblGrandTotal = new JLabel("₹0.00", SwingConstants.RIGHT);
        lblGrandTotal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblGrandTotal.setForeground(new Color(37, 99, 235));
        summaryCard.add(lblGrandTotal);

        JButton btnCheckout = new JButton("✅ COMPLETE SALE & PRINT BILL");
        btnCheckout.setBackground(new Color(37, 99, 235));
        btnCheckout.setForeground(Color.WHITE);
        btnCheckout.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCheckout.setPreferredSize(new Dimension(0, 42));

        JButton btnCancel = new JButton("Clear Cart / New Bill");

        JPanel checkoutButtons = new JPanel(new GridLayout(1, 2, 10, 0));
        checkoutButtons.setBackground(Color.WHITE);
        checkoutButtons.add(btnCancel);
        checkoutButtons.add(btnCheckout);

        JPanel rightBox = new JPanel(new BorderLayout(8, 8));
        rightBox.setBackground(new Color(248, 250, 252));
        rightBox.add(summaryCard, BorderLayout.CENTER);
        rightBox.add(checkoutButtons, BorderLayout.SOUTH);

        bottomPanel.add(rightBox, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Listeners
        cmbProduct.addActionListener(e -> onProductSelected());
        btnAddItem.addActionListener(e -> addItemToCart());
        btnRemoveItem.addActionListener(e -> removeItemFromCart());
        btnCheckout.addActionListener(e -> completeSale());
        btnCancel.addActionListener(e -> resetBillingDesk());
        txtGstRate.addActionListener(e -> calculateTotals());
    }

    private void onProductSelected() {
        Product p = (Product) cmbProduct.getSelectedItem();
        if (p != null) {
            txtStockAvail.setText(String.valueOf(p.getQuantity()));
            txtUnitPrice.setText(String.format("%.2f", p.getSellingPrice()));
        }
    }

    public void loadCustomers() {
        try {
            cmbCustomer.removeAllItems();
            cmbCustomer.addItem(new Customer(0, "Walk-in Customer", "", "", ""));
            List<Customer> customers = customerDAO.getAllCustomers();
            for (Customer c : customers) {
                cmbCustomer.addItem(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadProducts() {
        try {
            cmbProduct.removeAllItems();
            List<Product> products = productDAO.getAllProducts();
            for (Product p : products) {
                cmbProduct.addItem(p);
            }
            onProductSelected();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resetBillingDesk() {
        try {
            txtInvoiceNo.setText(billingDAO.generateNextInvoiceNo());
        } catch (Exception e) {
            txtInvoiceNo.setText("INV-0001");
        }
        cartItems.clear();
        cartModel.setRowCount(0);
        calculateTotals();
        loadProducts();
    }

    private void addItemToCart() {
        Product p = (Product) cmbProduct.getSelectedItem();
        if (p == null) return;

        int qty;
        try {
            qty = Integer.parseInt(txtQty.getText().trim());
            if (qty <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be greater than 0.", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid quantity.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check stock availability (including what's already in cart)
        int existingInCart = 0;
        for (SaleItem it : cartItems) {
            if (it.getProductId() == p.getId()) {
                existingInCart += it.getQuantity();
            }
        }

        if (existingInCart + qty > p.getQuantity()) {
            JOptionPane.showMessageDialog(this,
                    "Not enough stock! Available in stock: " + p.getQuantity() +
                    ", already in cart: " + existingInCart,
                    "Insufficient Stock", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Add or update existing item in cart
        boolean found = false;
        for (int i = 0; i < cartItems.size(); i++) {
            SaleItem it = cartItems.get(i);
            if (it.getProductId() == p.getId()) {
                it.setQuantity(it.getQuantity() + qty);
                it.setSubtotal(it.getQuantity() * it.getUnitPrice());
                cartModel.setValueAt(it.getQuantity(), i, 4);
                cartModel.setValueAt(String.format("₹%.2f", it.getSubtotal()), i, 5);
                found = true;
                break;
            }
        }

        if (!found) {
            double itemSubtotal = qty * p.getSellingPrice();
            SaleItem item = new SaleItem(p.getId(), p.getCode(), p.getName(), qty, p.getSellingPrice(), itemSubtotal);
            cartItems.add(item);
            cartModel.addRow(new Object[]{
                    cartItems.size(),
                    item.getProductCode(),
                    item.getProductName(),
                    String.format("₹%.2f", item.getUnitPrice()),
                    item.getQuantity(),
                    String.format("₹%.2f", item.getSubtotal())
            });
        }

        txtQty.setText("1");
        calculateTotals();
    }

    private void removeItemFromCart() {
        int r = cartTable.getSelectedRow();
        if (r == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item in the cart table to remove.");
            return;
        }
        cartItems.remove(r);
        cartModel.removeRow(r);
        // re-index
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            cartModel.setValueAt(i + 1, i, 0);
        }
        calculateTotals();
    }

    private void calculateTotals() {
        subtotal = 0.0;
        for (SaleItem item : cartItems) {
            subtotal += item.getSubtotal();
        }

        try {
            gstRate = Double.parseDouble(txtGstRate.getText().trim());
        } catch (Exception e) {
            gstRate = 18.0;
        }

        gstAmount = (subtotal * gstRate) / 100.0;
        grandTotal = subtotal + gstAmount;

        lblSubtotal.setText(String.format("₹%.2f", subtotal));
        lblGstAmount.setText(String.format("₹%.2f", gstAmount));
        lblGrandTotal.setText(String.format("₹%.2f", grandTotal));
    }

    private void completeSale() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty! Add products before checking out.", "Empty Cart", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Customer cust = (Customer) cmbCustomer.getSelectedItem();
        Integer custId = (cust != null && cust.getId() > 0) ? cust.getId() : null;
        String custName = cust != null ? cust.getName() : "Walk-in Customer";

        Sale sale = new Sale();
        sale.setInvoiceNo(txtInvoiceNo.getText().trim());
        sale.setCustomerId(custId);
        sale.setCustomerName(custName);
        sale.setSubtotal(subtotal);
        sale.setGstRate(gstRate);
        sale.setGstAmount(gstAmount);
        sale.setTotalAmount(grandTotal);
        sale.setPaymentMode((String) cmbPaymentMode.getSelectedItem());
        if (currentUser != null) {
            sale.setCreatedBy(currentUser.getId());
            sale.setCashierName(currentUser.getFullName());
        }
        sale.setItems(new ArrayList<>(cartItems));

        try {
            boolean ok = billingDAO.processSale(sale);
            if (ok) {
                // Show Invoice preview
                Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
                InvoiceDialog dialog = new InvoiceDialog(owner, sale);
                dialog.setVisible(true);

                // Reset desk
                resetBillingDesk();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error completing sale: " + ex.getMessage(), "Billing Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}

