package ui;

import dao.CustomerDAO;
import model.Customer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class QuickAddCustomerDialog extends JDialog {
    private JTextField txtName;
    private JTextField txtPhone;
    private JTextField txtEmail;
    private JTextField txtAddress;

    private Customer createdCustomer = null;
    private CustomerDAO customerDAO = new CustomerDAO();

    public QuickAddCustomerDialog(Frame owner, String prefilledPhone) {
        super(owner, "👤 Quick Add Customer (Billing Desk)", true);
        setSize(460, 360);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(248, 250, 252));

        initComponents(prefilledPhone);
        setupShortcuts();
    }

    private void initComponents(String prefilledPhone) {
        // TOP Header
        JPanel top = new JPanel(new GridLayout(2, 1, 2, 2));
        top.setBackground(new Color(30, 41, 59));
        top.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel lblTitle = new JLabel("👤 New Customer Registration");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Register customer right at billing desk for invoice & loyalty tracking.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(new Color(148, 163, 184));
        top.add(lblTitle);
        top.add(lblSub);
        add(top, BorderLayout.NORTH);

        // CENTER Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(10, 16, 10, 16),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(226, 232, 240)),
                        new EmptyBorder(12, 14, 12, 14)
                )
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // 1. Mobile Phone
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel lblPhone = new JLabel("Mobile Number *:");
        lblPhone.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(lblPhone, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtPhone = new JTextField(prefilledPhone != null ? prefilledPhone : "");
        txtPhone.setFont(new Font("Segoe UI", Font.BOLD, 13));
        form.add(txtPhone, gbc);

        // 2. Customer Name
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        JLabel lblName = new JLabel("Customer Name *:");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(lblName, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtName = new JTextField();
        txtName.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        form.add(txtName, gbc);

        // 3. Email (optional)
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        JLabel lblEmail = new JLabel("Email (Optional):");
        lblEmail.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        form.add(lblEmail, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtEmail = new JTextField();
        txtEmail.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        form.add(txtEmail, gbc);

        // 4. Address (optional)
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        JLabel lblAddr = new JLabel("City / Address:");
        lblAddr.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        form.add(lblAddr, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtAddress = new JTextField();
        txtAddress.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        form.add(txtAddress, gbc);

        add(form, BorderLayout.CENTER);

        // BOTTOM Buttons
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottom.setOpaque(false);

        JButton btnSave = new JButton("💾 Save & Attach to Bill ↵");
        btnSave.setBackground(new Color(16, 185, 129));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSave.setFocusPainted(false);

        JButton btnCancel = new JButton("Cancel (Esc)");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        bottom.add(btnSave);
        bottom.add(btnCancel);
        add(bottom, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> saveCustomer());
        btnCancel.addActionListener(e -> dispose());

        // Pressing Enter in name field also triggers save
        txtName.addActionListener(e -> saveCustomer());
        txtPhone.addActionListener(e -> {
            if (txtName.getText().trim().isEmpty()) {
                txtName.requestFocus();
            } else {
                saveCustomer();
            }
        });

        // Set initial focus
        SwingUtilities.invokeLater(() -> {
            if (prefilledPhone != null && !prefilledPhone.trim().isEmpty()) {
                txtName.requestFocusInWindow();
            } else {
                txtPhone.requestFocusInWindow();
            }
        });
    }

    private void setupShortcuts() {
        JRootPane rp = getRootPane();
        InputMap im = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rp.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDialog");
        am.put("closeDialog", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });
    }

    private void saveCustomer() {
        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();
        String address = txtAddress.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter Customer Name.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            txtName.requestFocus();
            return;
        }

        Customer c = new Customer();
        c.setName(name);
        c.setPhone(phone);
        c.setEmail(email);
        c.setAddress(address);

        try {
            boolean ok = customerDAO.addCustomer(c);
            if (ok) {
                this.createdCustomer = c;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save customer.", "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving customer: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public Customer getCreatedCustomer() {
        return createdCustomer;
    }
}
