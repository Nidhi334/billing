package ui;

import dao.SupplierDAO;
import model.Supplier;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SupplierPanel extends JPanel {
    private SupplierDAO supplierDAO = new SupplierDAO();
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtName, txtCompany, txtPhone, txtEmail, txtAddress;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear;
    private int selectedSupplierId = -1;

    public SupplierPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(248, 250, 252));
        setBorder(new EmptyBorder(15, 15, 15, 15));
        initComponents();
        loadSupplierTable();
    }

    private void initComponents() {
        // Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(248, 250, 252));
        JLabel lblTitle = new JLabel("🛒 Supplier & Vendor Management");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(30, 41, 59));
        topPanel.add(lblTitle, BorderLayout.WEST);
        add(topPanel, BorderLayout.NORTH);

        // Form
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

        g.gridx = 0; g.gridy = 0; formCard.add(new JLabel("Contact Person Name:"), g);
        txtName = new JTextField();
        g.gridy = 1; formCard.add(txtName, g);

        g.gridy = 2; formCard.add(new JLabel("Company / Vendor Name:"), g);
        txtCompany = new JTextField();
        g.gridy = 3; formCard.add(txtCompany, g);

        g.gridy = 4; formCard.add(new JLabel("Phone Number:"), g);
        txtPhone = new JTextField();
        g.gridy = 5; formCard.add(txtPhone, g);

        g.gridy = 6; formCard.add(new JLabel("Email Address:"), g);
        txtEmail = new JTextField();
        g.gridy = 7; formCard.add(txtEmail, g);

        g.gridy = 8; formCard.add(new JLabel("Address:"), g);
        txtAddress = new JTextField();
        g.gridy = 9; formCard.add(txtAddress, g);

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

        g.gridy = 10;
        g.insets = new Insets(15, 6, 6, 6);
        formCard.add(btnPanel, g);

        add(formCard, BorderLayout.WEST);

        // Table
        tableModel = new DefaultTableModel(new String[]{"ID", "Contact Name", "Company", "Phone", "Email", "Address"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(26);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Actions
        btnAdd.addActionListener(e -> addSupplier());
        btnUpdate.addActionListener(e -> updateSupplier());
        btnDelete.addActionListener(e -> deleteSupplier());
        btnClear.addActionListener(e -> clearForm());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int r = table.getSelectedRow();
                selectedSupplierId = Integer.parseInt(tableModel.getValueAt(r, 0).toString());
                txtName.setText(tableModel.getValueAt(r, 1).toString());
                txtCompany.setText(tableModel.getValueAt(r, 2) != null ? tableModel.getValueAt(r, 2).toString() : "");
                txtPhone.setText(tableModel.getValueAt(r, 3) != null ? tableModel.getValueAt(r, 3).toString() : "");
                txtEmail.setText(tableModel.getValueAt(r, 4) != null ? tableModel.getValueAt(r, 4).toString() : "");
                txtAddress.setText(tableModel.getValueAt(r, 5) != null ? tableModel.getValueAt(r, 5).toString() : "");
            }
        });
    }

    public void loadSupplierTable() {
        try {
            tableModel.setRowCount(0);
            List<Supplier> list = supplierDAO.getAllSuppliers();
            for (Supplier s : list) {
                tableModel.addRow(new Object[]{
                        s.getId(), s.getName(), s.getCompanyName(), s.getPhone(), s.getEmail(), s.getAddress()
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addSupplier() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Supplier contact name is required.");
            return;
        }
        try {
            Supplier s = new Supplier(0, name, txtCompany.getText().trim(), txtPhone.getText().trim(), txtEmail.getText().trim(), txtAddress.getText().trim());
            if (supplierDAO.addSupplier(s)) {
                JOptionPane.showMessageDialog(this, "Supplier saved successfully!");
                clearForm();
                loadSupplierTable();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding supplier: " + e.getMessage());
        }
    }

    private void updateSupplier() {
        if (selectedSupplierId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a supplier to update.");
            return;
        }
        try {
            Supplier s = new Supplier(selectedSupplierId, txtName.getText().trim(), txtCompany.getText().trim(), txtPhone.getText().trim(), txtEmail.getText().trim(), txtAddress.getText().trim());
            if (supplierDAO.updateSupplier(s)) {
                JOptionPane.showMessageDialog(this, "Supplier updated successfully!");
                clearForm();
                loadSupplierTable();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void deleteSupplier() {
        if (selectedSupplierId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a supplier to delete.");
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Delete this supplier?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            try {
                if (supplierDAO.deleteSupplier(selectedSupplierId)) {
                    clearForm();
                    loadSupplierTable();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Cannot delete supplier: " + e.getMessage());
            }
        }
    }

    private void clearForm() {
        selectedSupplierId = -1;
        txtName.setText("");
        txtCompany.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtAddress.setText("");
        table.clearSelection();
    }
}

