package ui;

import javax.swing.*;
import java.awt.*;

/**
 * ForgotPasswordDialog - Lightweight modal wrapper around ForgotPasswordPanel.
 * Reuses the single-source-of-truth ForgotPasswordPanel component to eliminate code duplication.
 */
public class ForgotPasswordDialog extends JDialog {

    public ForgotPasswordDialog(Frame owner) {
        super(owner, "Account Recovery • SmartBilling Pro", true);
        setSize(480, 560);
        setMinimumSize(new Dimension(450, 520));
        setLocationRelativeTo(owner);
        setResizable(false);

        // Embed the unified ForgotPasswordPanel with dialog dispose callbacks
        ForgotPasswordPanel panel = new ForgotPasswordPanel(
                this::dispose, // On Back/Cancel -> close dialog
                this::dispose  // On Reset Success -> close dialog
        );
        setContentPane(panel);
    }
}
