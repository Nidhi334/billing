package ui;

import config.AppSettings;
import model.Sale;
import model.SaleItem;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.PrinterException;
import java.text.SimpleDateFormat;

public class InvoiceDialog extends JDialog {
    private JTextArea txtInvoice;
    private JComboBox<String> cmbPrintMode;
    private Sale currentSale;

    public InvoiceDialog(Frame owner, Sale sale) {
        super(owner, "Receipt & Invoice Preview", true);
        this.currentSale = sale;
        setSize(440, 680);
        setLocationRelativeTo(owner);
        initComponents();
    }

    private void initComponents() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(12, 14, 12, 14));
        main.setBackground(new Color(248, 250, 252));

        // Top bar for format selection (2-inch, 3-inch, A4)
        JPanel topFormatBar = new JPanel(new BorderLayout(8, 0));
        topFormatBar.setOpaque(false);

        JLabel lblMode = new JLabel("Print Size:");
        lblMode.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblMode.setForeground(new Color(30, 41, 59));
        topFormatBar.add(lblMode, BorderLayout.WEST);

        cmbPrintMode = new JComboBox<>(new String[]{
                "2-Inch Thermal (58mm / 32 Cols) [RECOMMENDED]",
                "3-Inch Thermal (80mm / 44 Cols)",
                "Standard A4 / Full Page Invoice"
        });
        cmbPrintMode.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Load configured default size from AppSettings
        String defaultSize = AppSettings.getString(AppSettings.KEY_RECEIPT_PRINT_SIZE, "2_INCH");
        if ("3_INCH".equalsIgnoreCase(defaultSize)) {
            cmbPrintMode.setSelectedIndex(1);
        } else if ("A4".equalsIgnoreCase(defaultSize)) {
            cmbPrintMode.setSelectedIndex(2);
        } else {
            cmbPrintMode.setSelectedIndex(0); // 2-inch default
        }

        cmbPrintMode.addActionListener(e -> updateReceiptView());
        topFormatBar.add(cmbPrintMode, BorderLayout.CENTER);

        main.add(topFormatBar, BorderLayout.NORTH);

        txtInvoice = new JTextArea();
        txtInvoice.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtInvoice.setEditable(false);
        txtInvoice.setBackground(Color.WHITE);
        txtInvoice.setMargin(new Insets(10, 10, 10, 10));

        updateReceiptView();

        JScrollPane scroll = new JScrollPane(txtInvoice);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225)));
        main.add(scroll, BorderLayout.CENTER);

        // Buttons (Print, Close)
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnPrintThermal = new JButton("Print Receipt / Slip");
        btnPrintThermal.setBackground(new Color(37, 99, 235));
        btnPrintThermal.setForeground(Color.WHITE);
        btnPrintThermal.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JButton btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        btnPanel.add(btnPrintThermal);
        btnPanel.add(btnClose);
        main.add(btnPanel, BorderLayout.SOUTH);

        add(main);

        btnPrintThermal.addActionListener(e -> {
            try {
                boolean complete = txtInvoice.print();
                if (complete) {
                    JOptionPane.showMessageDialog(this, "Receipt Print Job Sent Successfully!", "Print", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Print error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnClose.addActionListener(e -> dispose());
    }

    private void updateReceiptView() {
        int selected = cmbPrintMode.getSelectedIndex();
        if (selected == 0) {
            txtInvoice.setText(generate2InchThermalReceipt(currentSale));
            txtInvoice.setFont(new Font("Monospaced", Font.PLAIN, 11));
        } else if (selected == 1) {
            txtInvoice.setText(generate3InchThermalReceipt(currentSale));
            txtInvoice.setFont(new Font("Monospaced", Font.PLAIN, 11));
        } else {
            txtInvoice.setText(generateStandardInvoice(currentSale));
            txtInvoice.setFont(new Font("Monospaced", Font.PLAIN, 12));
        }
        txtInvoice.setCaretPosition(0);
    }

    private String generate2InchThermalReceipt(Sale sale) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy HH:mm");
        String dateStr = sale.getSaleDate() != null ? sdf.format(sale.getSaleDate()) : sdf.format(new java.util.Date());
        String storeName = AppSettings.getString(AppSettings.KEY_STORE_NAME, "SMARTBILL SHOP").toUpperCase();
        String storePhone = AppSettings.getString(AppSettings.KEY_STORE_PHONE, "9876543210");

        sb.append(centerText(storeName, 32)).append("\n");
        sb.append(centerText("RETAIL POS TERMINAL", 32)).append("\n");
        sb.append(centerText("Ph: " + storePhone + " | GST: 07A", 32)).append("\n");
        sb.append("--------------------------------\n");
        sb.append(String.format("Bill: %-15s %10s\n", sale.getInvoiceNo(), dateStr));
        String cust = sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in";
        if (cust.length() > 24) cust = cust.substring(0, 22) + "..";
        sb.append(String.format("Cust: %-26s\n", cust));
        sb.append(String.format("Pay : %-15s Desk #1\n", sale.getPaymentMode()));
        sb.append("--------------------------------\n");
        sb.append(String.format("%-14s %3s %5s %7s\n", "Item", "Qty", "Rate", "Total"));
        sb.append("--------------------------------\n");

        for (SaleItem item : sale.getItems()) {
            String name = item.getProductName();
            if (name.length() > 14) name = name.substring(0, 12) + "..";
            sb.append(String.format("%-14s %3d %5.0f %7.2f\n",
                    name,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal()));
        }

        sb.append("--------------------------------\n");
        sb.append(String.format("%22s: %8.2f\n", "Subtotal", sale.getSubtotal()));
        if (sale.getDiscountAmount() > 0) {
            sb.append(String.format("%22s: %8.2f\n", "Discount", -sale.getDiscountAmount()));
        }
        sb.append(String.format("%22s: %8.2f\n", "GST(" + (int)sale.getGstRate() + "%)", sale.getGstAmount()));
        sb.append("================================\n");
        sb.append(String.format("NET TOTAL (Rs): %14.2f\n", sale.getTotalAmount()));
        sb.append("================================\n");
        sb.append("\n");
        sb.append(centerText("||| | ||||| || |||| |||| |||", 32)).append("\n");
        sb.append(centerText(sale.getInvoiceNo(), 32)).append("\n");
        sb.append(centerText("*** THANK YOU! VISIT AGAIN ***", 32)).append("\n");
        sb.append(centerText("Powered by SmartBilling POS", 32)).append("\n");
        sb.append("--------------------------------\n");
        return sb.toString();
    }

    private String generate3InchThermalReceipt(Sale sale) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String dateStr = sale.getSaleDate() != null ? sdf.format(sale.getSaleDate()) : sdf.format(new java.util.Date());
        String storeName = AppSettings.getString(AppSettings.KEY_STORE_NAME, "SMARTBILL SHOP").toUpperCase();
        String storePhone = AppSettings.getString(AppSettings.KEY_STORE_PHONE, "9876543210");

        sb.append("============================================\n");
        sb.append(centerText(storeName, 44)).append("\n");
        sb.append(centerText("Tech Plaza, Main Market, New Delhi", 44)).append("\n");
        sb.append(centerText("Ph: " + storePhone + " | GSTIN: 07AAAAA0000A1Z5", 44)).append("\n");
        sb.append("============================================\n");
        sb.append(String.format("Invoice No : %-30s\n", sale.getInvoiceNo()));
        sb.append(String.format("Date & Time: %-30s\n", dateStr));
        sb.append(String.format("Customer   : %-30s\n", (sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in Customer")));
        sb.append(String.format("Cashier    : %-18s Pay: %-10s\n", (sale.getCashierName() != null ? sale.getCashierName() : "Admin"), sale.getPaymentMode()));
        sb.append("--------------------------------------------\n");
        sb.append(String.format("%-18s %4s %9s %9s\n", "Item Description", "Qty", "Price", "Amount"));
        sb.append("--------------------------------------------\n");

        for (SaleItem item : sale.getItems()) {
            String name = item.getProductName();
            if (name.length() > 18) name = name.substring(0, 16) + "..";
            sb.append(String.format("%-18s %4d %9.2f %9.2f\n",
                    name,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getSubtotal()));
        }

        sb.append("--------------------------------------------\n");
        sb.append(String.format("%31s: %10.2f\n", "Subtotal (Rs)", sale.getSubtotal()));
        if (sale.getDiscountAmount() > 0) {
            sb.append(String.format("%31s: %10.2f\n", "Discount (" + sale.getDiscountType() + ")", -sale.getDiscountAmount()));
        }
        sb.append(String.format("%31s: %10.2f\n", "GST (" + (int)sale.getGstRate() + "%)", sale.getGstAmount()));
        sb.append("============================================\n");
        sb.append(String.format("%31s: %10.2f\n", "GRAND TOTAL (Rs)", sale.getTotalAmount()));
        sb.append("============================================\n");
        sb.append("\n");
        sb.append(centerText("||||| ||| |||||| ||||| |||| || ||||", 44)).append("\n");
        sb.append(centerText(sale.getInvoiceNo(), 44)).append("\n");
        sb.append(centerText("Thank you for shopping with us!", 44)).append("\n");
        sb.append(centerText("Exchange within 7 days with this bill.", 44)).append("\n");
        sb.append("============================================\n");
        return sb.toString();
    }

    private String generateStandardInvoice(Sale sale) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String dateStr = sale.getSaleDate() != null ? sdf.format(sale.getSaleDate()) : sdf.format(new java.util.Date());
        String storeName = AppSettings.getString(AppSettings.KEY_STORE_NAME, "SMARTBILL SHOP").toUpperCase();
        String storePhone = AppSettings.getString(AppSettings.KEY_STORE_PHONE, "9876543210");

        sb.append("====================================================\n");
        sb.append(centerText(storeName, 52)).append("\n");
        sb.append("         Tech Plaza, Main Market, New Delhi         \n");
        sb.append("            Phone: +91 " + storePhone + "                  \n");
        sb.append("              GSTIN: 07AAAAA0000A1Z5                \n");
        sb.append("====================================================\n");
        sb.append(String.format("Invoice No : %-20s\n", sale.getInvoiceNo()));
        sb.append(String.format("Date & Time: %-20s\n", dateStr));
        sb.append(String.format("Customer   : %-20s\n", (sale.getCustomerName() != null ? sale.getCustomerName() : "Walk-in Customer")));
        sb.append(String.format("Payment By : %-20s\n", sale.getPaymentMode()));
        sb.append("----------------------------------------------------\n");
        sb.append(String.format("%-20s %5s %11s %12s\n", "Item Name", "Qty", "Price", "Total"));
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
        sb.append(String.format("%38s: %10.2f\n", "Subtotal (Rs)", sale.getSubtotal()));
        if (sale.getDiscountAmount() > 0) {
            sb.append(String.format("%38s: %10.2f\n", "Discount (" + sale.getDiscountType() + ")", -sale.getDiscountAmount()));
        }
        sb.append(String.format("%38s: %10.2f\n", "GST (" + sale.getGstRate() + "%)", sale.getGstAmount()));
        sb.append("====================================================\n");
        sb.append(String.format("%38s: %10.2f\n", "GRAND TOTAL (Rs)", sale.getTotalAmount()));
        sb.append("====================================================\n");
        sb.append("\n");
        sb.append("           Thank you for shopping with us!          \n");
        sb.append("              Goods once sold cannot be             \n");
        sb.append("            returned without original bill.         \n");
        sb.append("====================================================\n");
        return sb.toString();
    }

    private String centerText(String text, int width) {
        if (text == null) return "";
        if (text.length() >= width) return text.substring(0, width);
        int pad = (width - text.length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pad; i++) sb.append(" ");
        sb.append(text);
        while (sb.length() < width) sb.append(" ");
        return sb.toString();
    }
}
