package ui;

import dao.SupplierDAO;
import model.Supplier;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

public class SupplierPanel extends JPanel {
    private SupplierDAO supplierDAO = new SupplierDAO();
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtName, txtCompany, txtPhone, txtEmail, txtAddress, txtSearch;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnSearch, btnReset;
    private int selectedSupplierId = -1;

    public SupplierPanel() {
        setLayout(new BorderLayout(15, 15));
        // setBackground(new Color(248, 250, 252));
        setBackground(Color.LIGHT_GRAY);

        setBorder(new EmptyBorder(15, 15, 15, 15));
        initComponents();
        loadSupplierTable();
    }

    private void initComponents() {
        // ==========================================
        // 1. TOP BAR: Title & Search
        // ==========================================
        JPanel topPanel = new JPanel(new BorderLayout(0, 0));
        topPanel.setBackground(Color.BLUE);
        // setBackground(new Color(248, 250, 252));
        setBackground(Color.LIGHT_GRAY);

        JLabel lblTitle = new JLabel("🏢 Supplier & Vendor Management");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);

        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setBackground(Color.LIGHT_GRAY);
        // searchPanel.setBackground(new Color(248, 250, 252));

        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSearch.setForeground(Color.BLACK);
        searchPanel.add(lblSearch);

        txtSearch = new JTextField(16);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.setPreferredSize(new Dimension(190, 30));
        txtSearch.setBorder(new CompoundBorder(
                new LineBorder(new Color(203, 213, 225)),
                new EmptyBorder(4, 8, 4, 8)));
        searchPanel.add(txtSearch);

        btnSearch = new JButton("Search");
        btnSearch.setBackground(Color.BLUE);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.setFocusPainted(false);
        btnSearch.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSearch.setPreferredSize(new Dimension(80, 30));
        searchPanel.add(btnSearch);

        btnReset = new JButton("Reset");
        btnReset.setBackground(Color.red);
        // btnReset.setForeground(new Color(200, 75, 75));
        btnReset.setForeground(Color.WHITE);
        btnReset.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnReset.setFocusPainted(false);
        btnReset.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnReset.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(203, 213, 225)),
                new EmptyBorder(4, 10, 4, 10)));
        btnReset.setPreferredSize(new Dimension(70, 30));
        searchPanel.add(btnReset);

        topPanel.add(searchPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // ==========================================
        // 2. LEFT: Form Card
        // ==========================================
        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(Color.LIGHT_GRAY);
        formCard.setBorder(BorderFactory.createCompoundBorder(
                // BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                new EmptyBorder(16, 16, 16, 16)));
        formCard.setPreferredSize(new Dimension(340, 0));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;
        g.weightx = 1.0;

        // Card Header
        JLabel lblFormTitle = new JLabel("📝 Supplier Details");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblFormTitle.setForeground(new Color(30, 41, 59));
        g.gridx = 0;
        g.gridy = 0;
        g.insets = new Insets(2, 4, 10, 4);
        formCard.add(lblFormTitle, g);

        // Reset insets for inputs
        g.insets = new Insets(3, 4, 3, 4);

        // Field 1: Contact Person Name
        g.gridy = 1;
        formCard.add(createFieldHeader("Contact Person Name *"), g);
        txtName = createStyledTextField();
        g.gridy = 2;
        formCard.add(txtName, g);

        // Field 2: Company / Vendor Name
        g.gridy = 3;
        formCard.add(createFieldHeader("Company / Vendor Name *"), g);
        txtCompany = createStyledTextField();
        g.gridy = 4;
        formCard.add(txtCompany, g);

        // Field 3: Phone Number
        g.gridy = 5;
        formCard.add(createFieldHeader("Phone Number"), g);
        txtPhone = createStyledTextField();
        g.gridy = 6;
        formCard.add(txtPhone, g);

        // Field 4: Email Address
        g.gridy = 7;
        formCard.add(createFieldHeader("Email Address"), g);
        txtEmail = createStyledTextField();
        g.gridy = 8;
        formCard.add(txtEmail, g);

        // Field 5: Address
        g.gridy = 9;
        formCard.add(createFieldHeader("Address / Location"), g);
        txtAddress = createStyledTextField();
        g.gridy = 10;
        formCard.add(txtAddress, g);

        // Action Buttons Grid (2x2)
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setBackground(Color.LIGHT_GRAY);

        btnAdd = new JButton("Add Supplier");
        btnAdd.setBackground(new Color(16, 185, 129));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAdd.setFocusPainted(false);
        btnAdd.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAdd.setPreferredSize(new Dimension(0, 34));

        btnUpdate = new JButton("Update");
        btnUpdate.setBackground(new Color(37, 99, 235));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnUpdate.setFocusPainted(false);
        btnUpdate.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnUpdate.setPreferredSize(new Dimension(0, 34));

        btnDelete = new JButton("Delete");
        btnDelete.setBackground(new Color(239, 68, 68));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDelete.setFocusPainted(false);
        btnDelete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDelete.setPreferredSize(new Dimension(0, 34));

        btnClear = new JButton("Clear Form");
        btnClear.setBackground(new Color(241, 245, 249));
        btnClear.setForeground(new Color(51, 65, 85));
        btnClear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClear.setFocusPainted(false);
        btnClear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnClear.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(203, 213, 225)),
                new EmptyBorder(4, 6, 4, 6)));
        btnClear.setPreferredSize(new Dimension(0, 34));

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);

        g.gridy = 11;
        g.insets = new Insets(14, 4, 6, 4);
        formCard.add(btnPanel, g);

        // Vertical spacer to pin fields to top without awkward stretching
        JPanel verticalSpacer = new JPanel();
        verticalSpacer.setOpaque(false);
        g.gridy = 12;
        g.weighty = 1.0;
        formCard.add(verticalSpacer, g);

        JScrollPane formScroll = new JScrollPane(formCard);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.getVerticalScrollBar().setUnitIncrement(14);
        add(formScroll, BorderLayout.WEST);

        // ==========================================
        // 3. CENTER: Table
        // ==========================================
        String[] cols = { "ID", "Contact Person", "Company / Vendor", "Phone", "Email", "Address" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(241, 245, 249));
        table.setSelectionBackground(new Color(224, 231, 255));
        table.setSelectionForeground(new Color(30, 41, 59));

        JTableHeader th = table.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setPreferredSize(new Dimension(0, 32));
        th.setBackground(new Color(241, 245, 249));
        th.setForeground(new Color(30, 41, 59));
        th.setReorderingAllowed(false);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                l.setFont(new Font("Segoe UI", Font.BOLD, 12));
                l.setForeground(new Color(51, 65, 85));
                l.setBackground(new Color(241, 245, 249));
                l.setOpaque(true);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                        new EmptyBorder(6, 10, 6, 10)));
                if (column == 0) {
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return l;
            }
        };
        th.setDefaultRenderer(headerRenderer);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(170);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(170);
        table.getColumnModel().getColumn(5).setPreferredWidth(220);

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        table.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return c;
            }
        };
        for (int i = 1; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));
        tableScroll.getViewport().setBackground(Color.WHITE);
        add(tableScroll, BorderLayout.CENTER);

        // ==========================================
        // 4. Action Listeners
        // ==========================================
        btnAdd.addActionListener(e -> addSupplier());
        btnUpdate.addActionListener(e -> updateSupplier());
        btnDelete.addActionListener(e -> deleteSupplier());
        btnClear.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> searchSupplier());
        txtSearch.addActionListener(e -> searchSupplier());
        btnReset.addActionListener(e -> {
            txtSearch.setText("");
            loadSupplierTable();
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int r = table.getSelectedRow();
                selectedSupplierId = Integer.parseInt(tableModel.getValueAt(r, 0).toString());
                txtName.setText(tableModel.getValueAt(r, 1) != null ? tableModel.getValueAt(r, 1).toString() : "");
                txtCompany.setText(tableModel.getValueAt(r, 2) != null ? tableModel.getValueAt(r, 2).toString() : "");
                txtPhone.setText(tableModel.getValueAt(r, 3) != null ? tableModel.getValueAt(r, 3).toString() : "");
                txtEmail.setText(tableModel.getValueAt(r, 4) != null ? tableModel.getValueAt(r, 4).toString() : "");
                txtAddress.setText(tableModel.getValueAt(r, 5) != null ? tableModel.getValueAt(r, 5).toString() : "");
            }
        });
    }

    private JLabel createFieldHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(new Color(51, 65, 85));
        return lbl;
    }

    private JTextField createStyledTextField() {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setPreferredSize(new Dimension(0, 30));
        tf.setBorder(new CompoundBorder(
                new LineBorder(new Color(203, 213, 225)),
                new EmptyBorder(4, 8, 4, 8)));
        return tf;
    }

    public void loadSupplierTable() {
        try {
            tableModel.setRowCount(0);
            List<Supplier> list = supplierDAO.getAllSuppliers();
            for (Supplier s : list) {
                tableModel.addRow(new Object[] {
                        s.getId(), s.getName(), s.getCompanyName(), s.getPhone(), s.getEmail(), s.getAddress()
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void searchSupplier() {
        String q = txtSearch.getText().trim();
        if (q.isEmpty()) {
            loadSupplierTable();
            return;
        }
        try {
            tableModel.setRowCount(0);
            List<Supplier> list = supplierDAO.searchSuppliers(q);
            for (Supplier s : list) {
                tableModel.addRow(new Object[] {
                        s.getId(), s.getName(), s.getCompanyName(), s.getPhone(), s.getEmail(), s.getAddress()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error searching suppliers: " + e.getMessage(), "Search Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addSupplier() {
        String name = txtName.getText().trim();
        String company = txtCompany.getText().trim();
        if (name.isEmpty() && company.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter at least Contact Person Name or Company Name.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            txtName.requestFocus();
            return;
        }
        try {
            Supplier s = new Supplier(0, name, company, txtPhone.getText().trim(), txtEmail.getText().trim(),
                    txtAddress.getText().trim());
            if (supplierDAO.addSupplier(s)) {
                JOptionPane.showMessageDialog(this, "Supplier added successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadSupplierTable();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding supplier: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSupplier() {
        if (selectedSupplierId <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a supplier from table to update.", "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String name = txtName.getText().trim();
        String company = txtCompany.getText().trim();
        if (name.isEmpty() && company.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Contact Person Name or Company Name cannot be empty.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            txtName.requestFocus();
            return;
        }
        try {
            Supplier s = new Supplier(selectedSupplierId, name, company, txtPhone.getText().trim(),
                    txtEmail.getText().trim(), txtAddress.getText().trim());
            if (supplierDAO.updateSupplier(s)) {
                JOptionPane.showMessageDialog(this, "Supplier updated successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                clearForm();
                loadSupplierTable();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating supplier: " + e.getMessage(), "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSupplier() {
        if (selectedSupplierId <= 0) {
            JOptionPane.showMessageDialog(this, "Please select a supplier from table to delete.", "Selection Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String displayName = !txtName.getText().trim().isEmpty() ? txtName.getText().trim()
                : txtCompany.getText().trim();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete supplier '" + displayName + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (supplierDAO.deleteSupplier(selectedSupplierId)) {
                    JOptionPane.showMessageDialog(this, "Supplier deleted successfully.", "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    clearForm();
                    loadSupplierTable();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Cannot delete supplier: " + e.getMessage(), "Database Error",
                        JOptionPane.ERROR_MESSAGE);
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
