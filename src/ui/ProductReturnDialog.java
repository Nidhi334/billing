package ui;

import dao.BillingDAO;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import model.Sale;
import model.SaleItem;

public class ProductReturnDialog extends JDialog {
    private JTextField txtInvoiceSearch;
    private JTable itemsTable;
    private DefaultTableModel itemsModel;
    private JSpinner spinReturnQty;
    private JTextField txtReason;
    private JLabel lblRefundAmount;
    private JButton btnProcessReturn;
    private JPopupMenu invoiceSuggestionsPopup;
    private Timer invoiceSuggestionTimer;
    private int invoiceSuggestionRequestId;
    private boolean selectingInvoiceSuggestion;
    private BillingDAO billingDAO = new BillingDAO();
    private Sale currentSale;

    public ProductReturnDialog(Frame owner) {
        super(owner, "🔄 Product Return & Stock Refund", true);
        setSize(720, 520);
        setLocationRelativeTo(owner);
        initComponents();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(15, 15, 15, 15));
        root.setBackground(new Color(248, 250, 252));

        // Top Search Bar for Invoice No
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBackground(Color.WHITE);
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblInv = new JLabel("Invoice Number: ");
        lblInv.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchBar.add(lblInv, BorderLayout.WEST);

        txtInvoiceSearch = new JTextField();
        txtInvoiceSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtInvoiceSearch.setToolTipText("Enter Invoice No (e.g. INV-2026...)");
        searchBar.add(txtInvoiceSearch, BorderLayout.CENTER);

        invoiceSuggestionTimer = new Timer(250, e -> loadInvoiceSuggestions());
        invoiceSuggestionTimer.setRepeats(false);
        txtInvoiceSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleInvoiceSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleInvoiceSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleInvoiceSuggestions();
            }
        });

        JButton btnSearch = createActionButton("🔍 Find Bill", 10, new Color(37, 99, 235), false);
        searchBar.add(btnSearch, BorderLayout.EAST);

        root.add(searchBar, BorderLayout.NORTH);

        // Center Table of items in this invoice
        String[] cols = {"Item ID", "Product Code", "Product Name", "Sold Qty", "Unit Price (₹)", "Subtotal (₹)"};
        itemsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        itemsTable = new JTable(itemsModel);
        itemsTable.setRowHeight(26);
        itemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setOpaque(false);
        JLabel lblItems = new JLabel("Select Item to Return:");
        lblItems.setFont(new Font("Segoe UI", Font.BOLD, 12));
        centerPanel.add(lblItems, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(itemsTable), BorderLayout.CENTER);

        root.add(centerPanel, BorderLayout.CENTER);

        // Bottom Return Form
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JPanel inputsGrid = new JPanel(new GridLayout(2, 4, 10, 8));
        inputsGrid.setOpaque(false);

        inputsGrid.add(new JLabel("Return Quantity:"));
        spinReturnQty = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        inputsGrid.add(spinReturnQty);

        inputsGrid.add(new JLabel("Calculated Refund:"));
        lblRefundAmount = new JLabel("₹0.00");
        lblRefundAmount.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblRefundAmount.setForeground(new Color(220, 38, 38));
        inputsGrid.add(lblRefundAmount);

        inputsGrid.add(new JLabel("Return Reason:"));
        txtReason = new JTextField("Defective / Customer Exchange");
        inputsGrid.add(txtReason);

        inputsGrid.add(new JLabel("")); // spacer
        inputsGrid.add(new JLabel("")); // spacer

        bottomPanel.add(inputsGrid, BorderLayout.CENTER);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBtns.setOpaque(false);

        btnProcessReturn = createActionButton("Approve Return & Restock", 12, AppTheme.FOREST_GREEN, true);
        btnProcessReturn.setIcon(new SidebarIcon("check", 13, Color.WHITE));
        btnProcessReturn.setIconTextGap(6);
        btnProcessReturn.setPreferredSize(new Dimension(270, 36));
        btnProcessReturn.setEnabled(false);

        JButton btnClose = createSecondaryButton("Close");
        btnClose.setPreferredSize(new Dimension(85, 36));
        btnClose.addActionListener(e -> dispose());

        actionBtns.add(btnProcessReturn);
        actionBtns.add(btnClose);
        bottomPanel.add(actionBtns, BorderLayout.SOUTH);

        root.add(bottomPanel, BorderLayout.SOUTH);

        add(root);

        // Listeners
        btnSearch.addActionListener(e -> searchInvoice());
        txtInvoiceSearch.addActionListener(e -> searchInvoice());

        itemsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                calculateRefund();
            }
        });

        spinReturnQty.addChangeListener(e -> calculateRefund());

        btnProcessReturn.addActionListener(e -> executeReturn());
    }

    private JButton createActionButton(String text, int fontSize) {
        return createActionButton(text, fontSize, AppTheme.FOREST_GREEN, true);
    }

    private JButton createActionButton(String text, int fontSize, Color background, boolean pillShape) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color fill = background;
                if (!isEnabled()) {
                    fill = new Color(100, 116, 139);
                } else if (getModel().isPressed() && getModel().isArmed()) {
                    fill = background.equals(AppTheme.FOREST_GREEN)
                            ? AppTheme.FOREST_DEEP
                            : darken(background, 32);
                } else if (getModel().isRollover()) {
                    fill = background.equals(AppTheme.FOREST_GREEN)
                            ? AppTheme.FOREST_MID
                            : darken(background, 18);
                }
                g2.setColor(fill);
                int arc = pillShape ? getHeight() : 8;
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                g2.dispose();

                super.paintComponent(g);
            }
        };
        button.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        button.setForeground(Color.WHITE);
        button.setMargin(new Insets(8, 14, 8, 14));
        Dimension preferredSize = button.getPreferredSize();
        button.setPreferredSize(new Dimension(preferredSize.width, 36));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text) {
            private boolean hovered;

            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color background = hovered
                        ? new Color(Math.max(0, Color.WHITE.getRed() - 6),
                                Math.max(0, Color.WHITE.getGreen() - 6),
                                Math.max(0, Color.WHITE.getBlue() - 6))
                        : Color.WHITE;
                g2.setColor(background);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(AppTheme.font(Font.BOLD, 12));
        button.setForeground(AppTheme.TEXT_SECONDARY);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static Color darken(Color color, int amount) {
        return new Color(
                Math.max(0, color.getRed() - amount),
                Math.max(0, color.getGreen() - amount),
                Math.max(0, color.getBlue() - amount),
                color.getAlpha());
    }

    private void scheduleInvoiceSuggestions() {
        if (selectingInvoiceSuggestion) {
            return;
        }

        invoiceSuggestionRequestId++;
        if (txtInvoiceSearch.getText().trim().isEmpty()) {
            invoiceSuggestionTimer.stop();
            if (invoiceSuggestionsPopup != null) {
                invoiceSuggestionsPopup.setVisible(false);
            }
            return;
        }
        invoiceSuggestionTimer.restart();
    }

    private void loadInvoiceSuggestions() {
        String query = txtInvoiceSearch.getText().trim();
        if (query.isEmpty()) {
            return;
        }

        int requestId = invoiceSuggestionRequestId;
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return billingDAO.getInvoiceSuggestions(query);
            }

            @Override
            protected void done() {
                if (isCancelled()
                        || requestId != invoiceSuggestionRequestId
                        || !query.equals(txtInvoiceSearch.getText().trim())) {
                    return;
                }

                try {
                    showInvoiceSuggestions(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(ProductReturnDialog.this,
                            "Invoice suggestions were interrupted.", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(ProductReturnDialog.this,
                            "Error loading invoice suggestions: " + cause.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void showInvoiceSuggestions(List<String> invoices) {
        if (invoiceSuggestionsPopup == null) {
            invoiceSuggestionsPopup = new JPopupMenu();
            invoiceSuggestionsPopup.setFocusable(false);
            invoiceSuggestionsPopup.setBackground(Color.WHITE);
            invoiceSuggestionsPopup.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(AppTheme.BORDER_SAGE),
                    new EmptyBorder(2, 2, 2, 2)
            ));
        }

        invoiceSuggestionsPopup.removeAll();
        if (invoices.isEmpty() || !txtInvoiceSearch.isShowing()) {
            invoiceSuggestionsPopup.setVisible(false);
            return;
        }

        for (String invoice : invoices) {
            JMenuItem suggestion = new JMenuItem(invoice);
            suggestion.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            suggestion.setForeground(AppTheme.TEXT_PRIMARY);
            suggestion.setBackground(Color.WHITE);
            suggestion.setPreferredSize(new Dimension(txtInvoiceSearch.getWidth(), 30));
            suggestion.addActionListener(e -> selectInvoiceSuggestion(invoice));
            invoiceSuggestionsPopup.add(suggestion);
        }

        invoiceSuggestionsPopup.show(txtInvoiceSearch, 0, txtInvoiceSearch.getHeight() + 2);
    }

    private void selectInvoiceSuggestion(String invoice) {
        invoiceSuggestionTimer.stop();
        invoiceSuggestionRequestId++;
        invoiceSuggestionsPopup.setVisible(false);
        selectingInvoiceSuggestion = true;
        txtInvoiceSearch.setText(invoice);
        selectingInvoiceSuggestion = false;
        searchInvoice();
    }

    private void searchInvoice() {
        invoiceSuggestionTimer.stop();
        invoiceSuggestionRequestId++;
        if (invoiceSuggestionsPopup != null) {
            invoiceSuggestionsPopup.setVisible(false);
        }

        String inv = txtInvoiceSearch.getText().trim();
        if (inv.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an invoice number to search.", "Input Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            currentSale = billingDAO.getSaleByInvoice(inv);
            itemsModel.setRowCount(0);
            if (currentSale == null) {
                JOptionPane.showMessageDialog(this, "No sale record found for invoice: " + inv, "Not Found", JOptionPane.INFORMATION_MESSAGE);
                btnProcessReturn.setEnabled(false);
                return;
            }

            List<SaleItem> items = currentSale.getItems();
            if (items == null || items.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No items found in this invoice.", "Empty Bill", JOptionPane.INFORMATION_MESSAGE);
                btnProcessReturn.setEnabled(false);
                return;
            }

            for (SaleItem it : items) {
                itemsModel.addRow(new Object[]{
                        it.getProductId(),
                        it.getProductId(),
                        it.getProductName(),
                        it.getQuantity(),
                        it.getUnitPrice(),
                        it.getSubtotal()
                });
            }

            if (itemsTable.getRowCount() > 0) {
                itemsTable.setRowSelectionInterval(0, 0);
            }
            btnProcessReturn.setEnabled(true);
            calculateRefund();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error fetching bill: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calculateRefund() {
        int row = itemsTable.getSelectedRow();
        if (row < 0) {
            lblRefundAmount.setText("₹0.00");
            return;
        }

        int soldQty = (int) itemsModel.getValueAt(row, 3);
        double rate = (double) itemsModel.getValueAt(row, 4);

        int returnQty = (int) spinReturnQty.getValue();
        if (returnQty > soldQty) {
            spinReturnQty.setValue(soldQty);
            returnQty = soldQty;
        }

        double refund = returnQty * rate;
        lblRefundAmount.setText(String.format("₹%.2f", refund));
    }

    private void executeReturn() {
        int row = itemsTable.getSelectedRow();
        if (row < 0 || currentSale == null) {
            JOptionPane.showMessageDialog(this, "Please select an item from the table to return.", "Select Item", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int productId = (int) itemsModel.getValueAt(row, 0);
        String prdName = (String) itemsModel.getValueAt(row, 2);
        int returnQty = (int) spinReturnQty.getValue();
        double rate = (double) itemsModel.getValueAt(row, 4);
        double refund = returnQty * rate;
        String reason = txtReason.getText().trim();

                int confirm = JOptionPane.showConfirmDialog(this,
                "Confirm Return: Product: " + prdName + " | Return Qty: " + returnQty + " | Refund: ₹" + String.format("%.2f", refund) + " | Restock: +" + returnQty,
                "Confirm Return & Refund",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            boolean ok = billingDAO.processReturn(currentSale.getInvoiceNo(), productId, returnQty, refund, reason);
            if (ok) {
                JOptionPane.showMessageDialog(this,
                        "Return Processed Successfully! Restocked +" + returnQty + " units. Refund of ₹" + String.format("%.2f", refund) + " completed.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error processing return: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
