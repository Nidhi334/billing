package ui;

import model.Customer;
import model.HeldBill;
import model.SaleItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class HeldBillsDialog extends JDialog {
    private BillingPanel billingPanel;
    private List<HeldBill> heldBills;

    private DefaultTableModel billsTableModel;
    private JTable billsTable;

    private DefaultTableModel itemsTableModel;
    private JTable itemsTable;

    private JLabel lblSelectedHeader;
    private JLabel lblSelectedTotals;
    private JLabel lblCountBadge;

    public HeldBillsDialog(Frame owner, BillingPanel billingPanel, List<HeldBill> heldBills) {
        super(owner, "📋 Held Bills Manager", true);
        this.billingPanel = billingPanel;
        this.heldBills = heldBills;

        setSize(880, 620);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(241, 245, 249));

        initComponents();
        setupShortcuts();
        refreshBillsTable();
    }

    private void initComponents() {
        // TOP: Header Panel
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.setBackground(new Color(30, 41, 59));
        topPanel.setBorder(new EmptyBorder(14, 18, 14, 18));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 2, 2));
        titleBox.setOpaque(false);
        JLabel lblTitle = new JLabel("📋 Held / Parked Bills Manager");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("View, resume, edit items & quantities, or cancel temporarily parked transactions.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(148, 163, 184));
        titleBox.add(lblTitle);
        titleBox.add(lblSub);
        topPanel.add(titleBox, BorderLayout.WEST);

        lblCountBadge = new JLabel(" 0 Bills on Hold ");
        lblCountBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCountBadge.setOpaque(true);
        lblCountBadge.setBackground(new Color(217, 119, 6));
        lblCountBadge.setForeground(Color.WHITE);
        lblCountBadge.setBorder(new EmptyBorder(4, 10, 4, 10));
        topPanel.add(lblCountBadge, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // CENTER: Split Pane (Master: Held Bills List, Detail: Bill Items)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.50);
        splitPane.setBorder(new EmptyBorder(0, 14, 0, 14));
        splitPane.setOpaque(false);

        // 1. Upper Table: Held Bills List
        JPanel billsCard = new JPanel(new BorderLayout(6, 6));
        billsCard.setBackground(Color.WHITE);
        billsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblListTitle = new JLabel("Active Parked / Held Bills:");
        lblListTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblListTitle.setForeground(new Color(30, 41, 59));
        billsCard.add(lblListTitle, BorderLayout.NORTH);

        String[] billCols = {"#", "Hold Reference", "Customer", "Items", "Units", "Grand Total (₹)", "Payment", "Held At"};
        billsTableModel = new DefaultTableModel(billCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        billsTable = new JTable(billsTableModel);
        billsTable.setRowHeight(28);
        billsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        billsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        billsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        billsTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        billsTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        billsTable.getColumnModel().getColumn(3).setPreferredWidth(55);
        billsTable.getColumnModel().getColumn(3).setCellRenderer(centerRender);
        billsTable.getColumnModel().getColumn(4).setPreferredWidth(55);
        billsTable.getColumnModel().getColumn(4).setCellRenderer(centerRender);
        billsTable.getColumnModel().getColumn(6).setPreferredWidth(70);
        billsTable.getColumnModel().getColumn(6).setCellRenderer(centerRender);
        billsTable.getColumnModel().getColumn(7).setPreferredWidth(80);
        billsTable.getColumnModel().getColumn(7).setCellRenderer(centerRender);

        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
        billsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        billsTable.getColumnModel().getColumn(5).setCellRenderer(rightRender);

        billsCard.add(new JScrollPane(billsTable), BorderLayout.CENTER);
        splitPane.setTopComponent(billsCard);

        // 2. Lower Table: Selected Bill Item Details
        JPanel itemsCard = new JPanel(new BorderLayout(6, 6));
        itemsCard.setBackground(Color.WHITE);
        itemsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        lblSelectedHeader = new JLabel("Select a bill above to view item details.");
        lblSelectedHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSelectedHeader.setForeground(new Color(37, 99, 235));
        itemsCard.add(lblSelectedHeader, BorderLayout.NORTH);

        String[] itemCols = {"#", "Code / Barcode", "Product Name", "Unit Price (₹)", "Quantity", "Total (₹)"};
        itemsTableModel = new DefaultTableModel(itemCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        itemsTable = new JTable(itemsTableModel);
        itemsTable.setRowHeight(26);
        itemsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        itemsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));

        itemsTable.getColumnModel().getColumn(0).setPreferredWidth(35);
        itemsTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        itemsTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        itemsTable.getColumnModel().getColumn(4).setCellRenderer(centerRender);
        itemsTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        itemsTable.getColumnModel().getColumn(3).setCellRenderer(rightRender);
        itemsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        itemsTable.getColumnModel().getColumn(5).setCellRenderer(rightRender);

        itemsCard.add(new JScrollPane(itemsTable), BorderLayout.CENTER);

        lblSelectedTotals = new JLabel("Subtotal: ₹0.00 | Discount: ₹0.00 | GST(18%): ₹0.00 | Net Total: ₹0.00", SwingConstants.RIGHT);
        lblSelectedTotals.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSelectedTotals.setForeground(new Color(15, 23, 42));
        lblSelectedTotals.setBorder(new EmptyBorder(4, 0, 0, 0));
        itemsCard.add(lblSelectedTotals, BorderLayout.SOUTH);

        splitPane.setBottomComponent(itemsCard);
        add(splitPane, BorderLayout.CENTER);

        // BOTTOM: Action Toolbar
        JPanel bottomBar = new JPanel(new BorderLayout(10, 0));
        bottomBar.setBorder(new EmptyBorder(8, 14, 12, 14));
        bottomBar.setOpaque(false);

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftActions.setOpaque(false);

        JButton btnResume = new JButton("▶️ Resume / Recall Bill");
        btnResume.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnResume.setBackground(new Color(16, 185, 129));
        btnResume.setForeground(Color.WHITE);
        btnResume.setFocusPainted(false);

        JButton btnView = new JButton("👁️ View Slip");
        btnView.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnView.setBackground(new Color(37, 99, 235));
        btnView.setForeground(Color.WHITE);
        btnView.setFocusPainted(false);

        JButton btnEdit = new JButton("✏️ Edit Bill");
        btnEdit.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEdit.setBackground(new Color(217, 119, 6));
        btnEdit.setForeground(Color.WHITE);
        btnEdit.setFocusPainted(false);

        JButton btnCancel = new JButton("🗑️ Cancel / Delete");
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCancel.setBackground(new Color(239, 68, 68));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);

        leftActions.add(btnResume);
        leftActions.add(btnView);
        leftActions.add(btnEdit);
        leftActions.add(btnCancel);
        bottomBar.add(leftActions, BorderLayout.WEST);

        JButton btnClose = new JButton("Close (Esc)");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bottomBar.add(btnClose, BorderLayout.EAST);

        add(bottomBar, BorderLayout.SOUTH);

        // Listeners
        billsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSelectedItemDetails();
            }
        });

        billsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    resumeSelectedHeldBill();
                }
            }
        });

        btnResume.addActionListener(e -> resumeSelectedHeldBill());
        btnView.addActionListener(e -> viewSelectedHeldBill());
        btnEdit.addActionListener(e -> editSelectedHeldBill());
        btnCancel.addActionListener(e -> cancelSelectedHeldBill());
        btnClose.addActionListener(e -> dispose());
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

    public void refreshBillsTable() {
        billsTableModel.setRowCount(0);
        for (int i = 0; i < heldBills.size(); i++) {
            HeldBill hb = heldBills.get(i);
            String custName = hb.getCustomer() != null ? hb.getCustomer().getName() : "Walk-in";
            billsTableModel.addRow(new Object[]{
                    i + 1,
                    hb.getReference(),
                    custName,
                    hb.getItems().size(),
                    hb.getTotalUnits(),
                    String.format("%.2f", hb.getGrandTotal()),
                    hb.getPaymentMode(),
                    hb.getFormattedTime()
            });
        }

        lblCountBadge.setText(" " + heldBills.size() + " Bill(s) on Hold ");
        if (heldBills.isEmpty()) {
            lblCountBadge.setBackground(new Color(100, 116, 139));
        } else {
            lblCountBadge.setBackground(new Color(217, 119, 6));
        }

        if (billsTable.getRowCount() > 0) {
            billsTable.setRowSelectionInterval(0, 0);
            updateSelectedItemDetails();
        } else {
            itemsTableModel.setRowCount(0);
            lblSelectedHeader.setText("No bills currently on hold.");
            lblSelectedTotals.setText("Subtotal: ₹0.00 | Discount: ₹0.00 | GST(18%): ₹0.00 | Net Total: ₹0.00");
        }
    }

    private HeldBill getSelectedBill() {
        int r = billsTable.getSelectedRow();
        if (r < 0 || r >= heldBills.size()) return null;
        return heldBills.get(r);
    }

    private void updateSelectedItemDetails() {
        HeldBill hb = getSelectedBill();
        if (hb == null) return;

        String custName = hb.getCustomer() != null ? hb.getCustomer().getName() : "Walk-in Customer";
        lblSelectedHeader.setText("Hold Reference: " + hb.getReference() + "  |  Customer: " + custName + "  |  Payment Mode: " + hb.getPaymentMode());

        itemsTableModel.setRowCount(0);
        for (int i = 0; i < hb.getItems().size(); i++) {
            SaleItem it = hb.getItems().get(i);
            itemsTableModel.addRow(new Object[]{
                    i + 1,
                    it.getProductCode(),
                    it.getProductName(),
                    String.format("%.2f", it.getUnitPrice()),
                    it.getQuantity(),
                    String.format("%.2f", it.getSubtotal())
            });
        }

        double sub = hb.getSubtotal();
        double disc = hb.getDiscountAmount();
        double discounted = Math.max(0, sub - disc);
        double gst = (discounted * 18.0) / 100.0;
        double net = discounted + gst;

        lblSelectedTotals.setText(String.format("Subtotal: ₹%.2f  |  Discount: -₹%.2f  |  GST (18%%): ₹%.2f  |  GRAND TOTAL: ₹%.2f", sub, disc, gst, net));
    }

    private void resumeSelectedHeldBill() {
        HeldBill hb = getSelectedBill();
        if (hb == null) {
            JOptionPane.showMessageDialog(this, "Please select a held bill from the table first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        boolean ok = billingPanel.resumeHeldBill(hb);
        if (ok) {
            dispose();
        }
    }

    private void viewSelectedHeldBill() {
        HeldBill hb = getSelectedBill();
        if (hb == null) {
            JOptionPane.showMessageDialog(this, "Please select a held bill first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("==========================================\n");
        sb.append("          HELD BILL SLIP DETAILS          \n");
        sb.append("==========================================\n");
        sb.append("Reference : ").append(hb.getReference()).append("\n");
        sb.append("Time Held : ").append(hb.getFormattedTime()).append("\n");
        sb.append("Customer  : ").append(hb.getCustomer() != null ? hb.getCustomer().getName() : "Walk-in Customer").append("\n");
        sb.append("Payment   : ").append(hb.getPaymentMode()).append("\n");
        sb.append("------------------------------------------\n");
        sb.append(String.format("%-18s %4s %8s %9s\n", "Item", "Qty", "Rate", "Total"));
        sb.append("------------------------------------------\n");
        for (SaleItem it : hb.getItems()) {
            String name = it.getProductName();
            if (name.length() > 18) name = name.substring(0, 16) + "..";
            sb.append(String.format("%-18s %4d %8.2f %9.2f\n", name, it.getQuantity(), it.getUnitPrice(), it.getSubtotal()));
        }
        sb.append("------------------------------------------\n");
        sb.append(String.format("%30s: %9.2f\n", "Subtotal (Rs)", hb.getSubtotal()));
        if (hb.getDiscountAmount() > 0) {
            sb.append(String.format("%30s: %9.2f\n", "Discount (Rs)", -hb.getDiscountAmount()));
        }
        sb.append(String.format("%30s: %9.2f\n", "GST (18%)", (Math.max(0, hb.getSubtotal() - hb.getDiscountAmount()) * 18.0) / 100.0));
        sb.append("==========================================\n");
        sb.append(String.format("%30s: %9.2f\n", "TOTAL PAYABLE", hb.getGrandTotal()));
        sb.append("==========================================\n");

        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ta.setEditable(false);
        ta.setBackground(Color.WHITE);
        ta.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(420, 420));
        JOptionPane.showMessageDialog(this, sp, "Held Bill - " + hb.getReference(), JOptionPane.PLAIN_MESSAGE);
    }

    private void editSelectedHeldBill() {
        HeldBill hb = getSelectedBill();
        if (hb == null) {
            JOptionPane.showMessageDialog(this, "Please select a held bill first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog editDlg = new JDialog(this, "✏️ Edit Held Bill - " + hb.getReference(), true);
        editDlg.setSize(620, 440);
        editDlg.setLocationRelativeTo(this);
        editDlg.setLayout(new BorderLayout(10, 10));
        ((JPanel) editDlg.getContentPane()).setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel metaRow = new JPanel(new GridLayout(2, 2, 10, 6));
        JTextField txtRef = new JTextField(hb.getReference());
        JComboBox<String> cmbMode = new JComboBox<>(new String[]{"CASH", "UPI", "CARD", "CREDIT"});
        cmbMode.setSelectedItem(hb.getPaymentMode());

        JTextField txtDisc = new JTextField(String.valueOf(hb.getDiscountValue()));
        JComboBox<String> cmbDiscType = new JComboBox<>(new String[]{"FLAT", "PERCENT"});
        cmbDiscType.setSelectedItem(hb.getDiscountType());

        metaRow.add(new JLabel("Hold Reference / Table:"));
        metaRow.add(txtRef);
        metaRow.add(new JLabel("Payment Mode:"));
        metaRow.add(cmbMode);
        editDlg.add(metaRow, BorderLayout.NORTH);

        String[] cols = {"#", "Code", "Product", "Rate (₹)", "Quantity (Editable)"};
        DefaultTableModel editModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return c == 4; }
        };
        for (int i = 0; i < hb.getItems().size(); i++) {
            SaleItem it = hb.getItems().get(i);
            editModel.addRow(new Object[]{i + 1, it.getProductCode(), it.getProductName(), String.format("%.2f", it.getUnitPrice()), it.getQuantity()});
        }
        JTable editTable = new JTable(editModel);
        editTable.setRowHeight(26);
        editDlg.add(new JScrollPane(editTable), BorderLayout.CENTER);

        JPanel editBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnSave = new JButton("Save Changes");
        btnSave.setBackground(new Color(16, 185, 129));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton btnCloseEdit = new JButton("Cancel");
        editBottom.add(btnSave);
        editBottom.add(btnCloseEdit);
        editDlg.add(editBottom, BorderLayout.SOUTH);

        btnSave.addActionListener(e -> {
            if (editTable.isEditing()) editTable.getCellEditor().stopCellEditing();
            String newRef = txtRef.getText().trim();
            if (newRef.isEmpty()) newRef = hb.getReference();

            try {
                double newDisc = Double.parseDouble(txtDisc.getText().trim());
                List<SaleItem> updatedItems = new ArrayList<>();
                for (int r = 0; r < editModel.getRowCount(); r++) {
                    int q = Integer.parseInt(editModel.getValueAt(r, 4).toString().trim());
                    if (q > 0) {
                        SaleItem orig = hb.getItems().get(r);
                        orig.setQuantity(q);
                        orig.setSubtotal(q * orig.getUnitPrice());
                        updatedItems.add(orig);
                    }
                }

                if (updatedItems.isEmpty()) {
                    JOptionPane.showMessageDialog(editDlg, "Bill must contain at least one item!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                hb.setReference(newRef);
                hb.setPaymentMode((String) cmbMode.getSelectedItem());
                hb.setDiscountValue(newDisc);
                hb.setDiscountType((String) cmbDiscType.getSelectedItem());
                hb.setItems(updatedItems);

                billingPanel.updateHeldBillsDropdown();
                refreshBillsTable();
                editDlg.dispose();
                JOptionPane.showMessageDialog(this, "Held bill updated successfully!", "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(editDlg, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCloseEdit.addActionListener(e -> editDlg.dispose());
        editDlg.setVisible(true);
    }

    private void cancelSelectedHeldBill() {
        HeldBill hb = getSelectedBill();
        if (hb == null) {
            JOptionPane.showMessageDialog(this, "Please select a held bill to cancel.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int opt = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to cancel and delete held bill '" + hb.getReference() + "'?",
                "Cancel Held Bill", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            heldBills.remove(hb);
            billingPanel.updateHeldBillsDropdown();
            refreshBillsTable();
            JOptionPane.showMessageDialog(this, "Held bill cancelled successfully.", "Cancelled", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
