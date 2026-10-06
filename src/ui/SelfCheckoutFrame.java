package ui;

import model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SelfCheckoutFrame extends JFrame {

    private final User checkoutUser;
    private final SelfCheckoutPanel selfCheckoutPanel;

    public SelfCheckoutFrame(User user) {
        super("SmartBilling Pro - Express Self-Checkout Station");
        this.checkoutUser = (user != null) ? user : new User(1, "selfcheckout", "", "Self Checkout Express", "STAFF");

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });

        selfCheckoutPanel = new SelfCheckoutPanel(checkoutUser, this::confirmExit);
        setContentPane(selfCheckoutPanel);
    }

    private void confirmExit() {
        int res = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit Self-Checkout?",
                "Exit Self-Checkout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (res == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {}
            new SelfCheckoutFrame(null).setVisible(true);
        });
    }
}
