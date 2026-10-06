package ui;

import model.Sale;
import javax.swing.*;
import java.awt.*;

/**
 * Modal Dialog for viewing complete Bill & Order Details.
 * Delegates rendering to BillDetailPanel.
 */
public class BillDetailDialog extends JDialog {
    public BillDetailDialog(Frame owner, Sale sale) {
        super(owner, "Bill Details - " + (sale != null ? sale.getInvoiceNo() : ""), true);
        setSize(820, 640);
        setMinimumSize(new Dimension(740, 560));
        setLocationRelativeTo(owner);
        setContentPane(new BillDetailPanel(this, sale));
    }
}

