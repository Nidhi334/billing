package ui;

import dao.CategoryDAO;
import model.Category;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CategoryDialog extends JDialog {
    private CategoryDAO categoryDAO = new CategoryDAO();
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtName, txtDesc;
    private JButton btnAdd, btnDelete;

    public CategoryDialog(Frame owner) {
        super(owner, "Manage Product Categories", true);
        setSize(500, 420);
        setLocationRelativeTo(owner);
        initComponents();
        loadCategories();
    }

    private void initComponents() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Form
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Category Name:"));
        txtName = new JTextField();
        form.add(txtName);

        form.add(new JLabel("Description:"));
        txtDesc = new JTextField();
        form.add(txtDesc);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        top.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnAdd = new JButton("Add Category");
        btnAdd.setBackground(new Color(16, 185, 129));
        btnAdd.setForeground(Color.WHITE);

        btnDelete = new JButton("Delete Selected");
        btnDelete.setBackground(new Color(239, 68, 68));
        btnDelete.setForeground(Color.WHITE);

        btnPanel.add(btnAdd);
        btnPanel.add(btnDelete);
        top.add(btnPanel, BorderLayout.SOUTH);

        main.add(top, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Description"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        main.add(new JScrollPane(table), BorderLayout.CENTER);

        add(main);

        btnAdd.addActionListener(e -> addCategory());
        btnDelete.addActionListener(e -> deleteCategory());
    }

    private void loadCategories() {
        try {
            tableModel.setRowCount(0);
            List<Category> list = categoryDAO.getAllCategories();
            for (Category c : list) {
                tableModel.addRow(new Object[]{c.getId(), c.getName(), c.getDescription()});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading categories: " + e.getMessage());
        }
    }

    private void addCategory() {
        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Category name is required.");
            return;
        }
        try {
            if (categoryDAO.addCategory(new Category(0, name, desc))) {
                txtName.setText("");
                txtDesc.setText("");
                loadCategories();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding category: " + e.getMessage());
        }
    }

    private void deleteCategory() {
        int r = table.getSelectedRow();
        if (r == -1) {
            JOptionPane.showMessageDialog(this, "Select a category to delete.");
            return;
        }
        int id = Integer.parseInt(tableModel.getValueAt(r, 0).toString());
        try {
            if (categoryDAO.deleteCategory(id)) {
                loadCategories();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Cannot delete category: " + e.getMessage());
        }
    }
}

