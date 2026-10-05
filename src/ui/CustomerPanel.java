package ui;

import dao.CustomerDAO;
import model.Customer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class CustomerPanel extends JPanel {
    private CustomerDAO customerDAO = new CustomerDAO();
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtName, txtPhone, txtEmail, txtAddress, txtSearch;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnSearch, btnReset;
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

        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchPanel.add(lblSearch);

        txtSearch = new JTextField(16);
        txtSearch.setPreferredSize(new Dimension(180, 28));
        searchPanel.add(txtSearch);

        btnSearch = new JButton("Search");
        btnSearch.setBackground(new Color(37, 99, 235));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.setFocusPainted(false);
        searchPanel.add(btnSearch);

        btnReset = new JButton("Reset");
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnReset.setBackground(new Color(205, 99, 75));
        btnReset.setForeground(Color.WHITE);
        btnReset.setFocusPainted(false);
        searchPanel.add(btnReset);

        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // LEFT: Form Card
        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(Color.WHITE);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                new EmptyBorder(14, 14, 14, 14)));
        formCard.setPreferredSize(new Dimension(340, 0));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.weightx = 1.0;

        // 1. Full Name
        g.gridx = 0;
        g.gridy = 0;
        formCard.add(createFieldHeader("Full Name:"), g);
        txtName = new JTextField();
        txtName.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtName.setPreferredSize(new Dimension(0, 30));
        g.gridy = 1;
        formCard.add(txtName, g);

        // 2. Phone Number
        g.gridy = 2;
        formCard.add(createFieldHeader("Phone Number:"), g);
        txtPhone = new JTextField();
        txtPhone.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtPhone.setPreferredSize(new Dimension(0, 30));
        g.gridy = 3;
        formCard.add(txtPhone, g);

        // 3. Email Address
        g.gridy = 4;
        formCard.add(createFieldHeader("Email Address:"), g);
        txtEmail = new JTextField();
        txtEmail.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtEmail.setPreferredSize(new Dimension(0, 30));
        g.gridy = 5;
        formCard.add(txtEmail, g);

        // 4. Billing Address
        g.gridy = 6;
        formCard.add(createFieldHeader("Billing Address:"), g);
        txtAddress = new JTextField();
        txtAddress.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtAddress.setPreferredSize(new Dimension(0, 30));
        g.gridy = 7;
        formCard.add(txtAddress, g);

        // 5. Action Buttons Grid
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        btnPanel.setBackground(Color.WHITE);

        btnAdd = new JButton("Add Customer");
        btnAdd.setBackground(new Color(16, 185, 129));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAdd.setFocusPainted(false);
        btnAdd.setPreferredSize(new Dimension(0, 32));

        btnUpdate = new JButton("Update");
        btnUpdate.setBackground(new Color(37, 99, 235));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnUpdate.setFocusPainted(false);
        btnUpdate.setPreferredSize(new Dimension(0, 32));

        btnDelete = new JButton("Delete");
        btnDelete.setBackground(new Color(239, 68, 68));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDelete.setFocusPainted(false);
        btnDelete.setPreferredSize(new Dimension(0, 32));

        btnClear = new JButton("Clear Form");
        btnClear.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClear.setFocusPainted(false);
        btnClear.setPreferredSize(new Dimension(0, 32));

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);

        g.gridy = 8;
        g.insets = new Insets(12, 4, 6, 4);
        formCard.add(btnPanel, g);

        // Spacer to stick form controls to top
        JPanel verticalSpacer = new JPanel();
        verticalSpacer.setOpaque(false);
        g.gridy = 9;
        g.weighty = 1.0;
        formCard.add(verticalSpacer, g);

        JScrollPane formScroll = new JScrollPane(formCard);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(14);
        add(formScroll, BorderLayout.WEST);

        // CENTER: Table
        String[] cols = { "ID", "Name", "Phone", "Email", "Address", "Registered Date" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(241, 245, 249));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(180);
        table.getColumnModel().getColumn(4).setPreferredWidth(220);
        table.getColumnModel().getColumn(5).setPreferredWidth(140);

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        add(tableScroll, BorderLayout.CENTER);

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

    private JLabel createFieldHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(51, 65, 85));
        return lbl;
    }

    public void loadCustomerTable() {
        try {
            tableModel.setRowCount(0);
            List<Customer> list = customerDAO.getAllCustomers();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (Customer c : list) {
                String regDate = c.getCreatedAt() != null ? sdf.format(c.getCreatedAt()) : "N/A";
                tableModel.addRow(new Object[] {
                        c.getId(), c.getName(), c.getPhone(), c.getEmail(), c.getAddress(), regDate
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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            for (Customer c : list) {
                String regDate = c.getCreatedAt() != null ? sdf.format(c.getCreatedAt()) : "N/A";
                tableModel.addRow(new Object[] {
                        c.getId(), c.getName(), c.getPhone(), c.getEmail(), c.getAddress(), regDate
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
            JOptionPane.showMessageDialog(this, "Customer name is mandatory.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            txtName.requestFocus();
            return;
        }

        try {
            Customer c = new Customer(0, name, phone, email, addr);
            if (customerDAO.addCustomer(c)) {
                JOptionPane.showMessageDialog(this, "Customer added successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadCustomerTable();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding customer: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateCustomer() {
        if (selectedCustomerId <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a customer from table to update.");
            return;
        }
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Customer name cannot be empty.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            txtName.requestFocus();
            return;
        }
        try {
            Customer c = new Customer(selectedCustomerId, name, txtPhone.getText().trim(), txtEmail.getText().trim(),
                    txtAddress.getText().trim());
            if (customerDAO.updateCustomer(c)) {
                JOptionPane.showMessageDialog(this, "Customer updated successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
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
        int c = JOptionPane.showConfirmDialog(this, "Are you sure to delete this customer?", "Confirm",
                JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) {
            try {
                if (customerDAO.deleteCustomer(selectedCustomerId)) {
                    clearForm();
                    loadCustomerTable();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Cannot delete customer (active billing history exists): " + e.getMessage());
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
