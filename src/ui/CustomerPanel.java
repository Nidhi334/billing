package ui;

import dao.CustomerDAO;
import model.Customer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CustomerPanel extends JPanel {
    private CustomerDAO customerDAO = new CustomerDAO();
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtName, txtPhone, txtEmail, txtAddress, txtSearch;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear;
    private int selectedCustomerId = -1;

    public CustomerPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(248, 250, 252));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        initComponents();
        loadCustomerTable();
    }

    private void initComponents() {
        // TOP: Header & Search
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(new Color(248, 250, 252));

        JLabel lblTitle = new JLabel("👨‍💼 Customer Management");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(30, 41, 59));
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setBackground(new Color(248, 250, 252));
        searchPanel.add(new JLabel("Search:"));
        txtSearch = new JTextField(16);
        searchPanel.add(txtSearch);
        JButton btnSearch = new JButton("Search");
        searchPanel.add(btnSearch);
        JButton btnReset = new JButton("Reset");
        searchPanel.add(btnReset);
        topPanel.add(searchPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // LEFT: Form
        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));
        formCard.setPreferredSize(new Dimension(320, 0));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0; g.gridy = 0; formCard.add(new JLabel("Full Name:"), g);
        txtName = new JTextField();
        g.gridy = 1; formCard.add(txtName, g);

        g.gridy = 2; formCard.add(new JLabel("Phone Number:"), g);
        txtPhone = new JTextField();
        g.gridy = 3; formCard.add(txtPhone, g);

        g.gridy = 4; formCard.add(new JLabel("Email Address:"), g);
        txtEmail = new JTextField();
        g.gridy = 5; formCard.add(txtEmail, g);

        g.gridy = 6; formCard.add(new JLabel("Billing Address:"), g);
        txtAddress = new JTextField();
        g.gridy = 7; formCard.add(txtAddress, g);

        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setBackground(Color.WHITE);
        btnAdd = new JButton("Add");
        btnAdd.setBackground(new Color(16, 185, 129));
        btnAdd.setForeground(Color.WHITE);

        btnUpdate = new JButton("Update");
        btnUpdate.setBackground(new Color(37, 99, 235));
        btnUpdate.setForeground(Color.WHITE);

        btnDelete = new JButton("Delete");
        btnDelete.setBackground(new Color(239, 68, 68));
        btnDelete.setForeground(Color.WHITE);

        btnClear = new JButton("Clear");

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);

        g.gridy = 8;
        g.insets = new Insets(15, 6, 6, 6);
        formCard.add(btnPanel, g);

        add(formCard, BorderLayout.WEST);

        // CENTER: Table
        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Phone", "Email", "Address", "Registered Date"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Actions
        btnAdd.addActionListener(e -> addCustomer());
        btnUpdate.addActionListener(e -> updateCustomer());
        btnDelete.addActionListener(e -> deleteCustomer());
        btnClear.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> searchCustomer());
        txtSearch.addActionListener(e -> searchCustomer());
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            loadCustomerTable();
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int r = table.getSelectedRow();
                selectedCustomerId = Integer.parseInt(tableModel.getValueAt(r, 0).toString());
                txtName.setText(tableModel.getValueAt(r, 1).toString());
                txtPhone.setText(tableModel.getValueAt(r, 2) != null ? tableModel.getValueAt(r, 2).toString() : "");
                txtEmail.setText(tableModel.getValueAt(r, 3) != null ? tableModel.getValueAt(r, 3).toString() : "");
                txtAddress.setText(tableModel.getValueAt(r, 4) != null ? tableModel.getValueAt(r, 4).toString() : "");
            }
        });
    }

    public void loadCustomerTable() {
        try {
            tableModel.setRowCount(0);
            List<Customer> list = customerDAO.getAllCustomers();
            for (Customer c : list) {
                tableModel.addRow(new Object[]{
                        c.getId(), c.getName(), c.getPhone(), c.getEmail(), c.getAddress(), c.getCreatedAt()
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void searchCustomer() {
        String q = txtSearch.getText().trim();
        if (q.isEmpty()) {
            loadCustomerTable();
            return;
        }
        try {
            tableModel.setRowCount(0);
            List<Customer> list = customerDAO.searchCustomers(q);
            for (Customer c : list) {
                tableModel.addRow(new Object[]{
                        c.getId(), c.getName(), c.getPhone(), c.getEmail(), c.getAddress(), c.getCreatedAt()
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addCustomer() {
        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();
        String addr = txtAddress.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Customer name is mandatory.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Customer c = new Customer(0, name, phone, email, addr);
            if (customerDAO.addCustomer(c)) {
                JOptionPane.showMessageDialog(this, "Customer added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadCustomerTable();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding customer: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCustomer() {
        if (selectedCustomerId <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a customer from table to update.");
            return;
        }
        try {
            Customer c = new Customer(selectedCustomerId, txtName.getText().trim(), txtPhone.getText().trim(), txtEmail.getText().trim(), txtAddress.getText().trim());
            if (customerDAO.updateCustomer(c)) {
                JOptionPane.showMessageDialog(this, "Customer updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadCustomerTable();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating customer: " + e.getMessage());
        }
    }

    private void deleteCustomer() {
        if (selectedCustomerId <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a customer to delete.");
            return;
        }
        int c = JOptionPane.showConfirmDialog(this, "Are you sure to delete this customer?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            try {
                if (customerDAO.deleteCustomer(selectedCustomerId)) {
                    clearForm();
                    loadCustomerTable();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Cannot delete customer (active billing history exists): " + e.getMessage());
            }
        }
    }

    private void clearForm() {
        selectedCustomerId = -1;
        txtName.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        table.clearSelection();
    }
}

