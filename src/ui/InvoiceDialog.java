package ui;

import model.Sale;
import model.SaleItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.PrinterException;
import java.text.SimpleDateFormat;

public class InvoiceDialog extends JDialog {
    private JTextArea txtInvoice;

    public InvoiceDialog(Frame owner, Sale sale) {
        super(owner, "Invoice Preview & Print", true);
        setSize(520, 620);
        setLocationRelativeTo(owner);
        initComponents(sale);
    }

    private void initComponents(Sale sale) {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(15, 15, 15, 15));

        txtInvoice = new JTextArea();
        txtInvoice.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtInvoice.setEditable(false);
        txtInvoice.setBackground(Color.WHITE);

        generateInvoiceText(sale);

        JScrollPane scroll = new JScrollPane(txtInvoice);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225)));
        main.add(scroll, BorderLayout.CENTER);

        // Buttons (Print, Close)
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnPrint = new JButton("🖨️ Print Invoice");
        btnPrint.setBackground(new Color(37, 99, 235));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JButton btnClose = new JButton("Close");

        btnPanel.add(btnPrint);
        btnPanel.add(btnClose);
        main.add(btnPanel, BorderLayout.SOUTH);

        add(main);

        btnPrint.addActionListener(e -> {
            try {
                boolean complete = txtInvoice.print();
                if (complete) {
                    JOptionPane.showMessageDialog(this, "Print Job Completed!", "Print", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Print error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnClose.addActionListener(e -> dispose());
    }

    private void generateInvoiceText(Sale sale) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String dateStr = sale.getSaleDate() != null ? sdf.format(sale.getSaleDate()) : sdf.format(new java.util.Date());

        sb.append("====================================================\n");
        sb.append("                   SMARTBILL SHOP                   \n");
        sb.append("         Tech Plaza, Main Market, New Delhi         \n");
        sb.append("            Phone: +91 98765 43210                  \n");
        sb.append("              GSTIN: 07AAAAA0000A1Z5                \n");
        sb.append("====================================================\n");
        sb.append(String.format("Invoice No : %-20s\n", sale.getInvoiceNo()));
        sb.append(String.format("Date & Time: %-20s\n", dateStr));
        sb.append(String.format("Customer   : %-20s\n", (sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in Customer")));
        sb.append(String.format("Payment By : %-20s\n", sale.getPaymentMode()));
        sb.append("----------------------------------------------------\n");
        sb.append(String.format("%-20s %5s %11s %12s\n", "Item Name", "Qty", "Price(₹)", "Total(₹)"));
        sb.append("----------------------------------------------------\n");

        for (SaleItem item : sale.getItems()) {
            String name = item.getProductName();
            if (name.length() > 20) name = name.substring(0, 18) + "..";
            sb.append(String.format("%-20s %5d %11.2f %12.2f\n",
                    name,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal()));
        }

        sb.append("----------------------------------------------------\n");
        sb.append(String.format("%38s: %10.2f\n", "Subtotal (₹)", sale.getSubtotal()));
        sb.append(String.format("%38s: %10.2f\n", "GST (" + sale.getGstRate() + "%)", sale.getGstAmount()));
        sb.append("====================================================\n");
        sb.append(String.format("%38s: %10.2f\n", "GRAND TOTAL (₹)", sale.getTotalAmount()));
        sb.append("====================================================\n");
        sb.append("\n");
        sb.append("           Thank you for shopping with us!          \n");
        sb.append("              Goods once sold cannot be             \n");
        sb.append("            returned without original bill.         \n");
        sb.append("====================================================\n");

        txtInvoice.setText(sb.toString());
    }
}

