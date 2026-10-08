package ui;

import javax.swing.*;
import java.awt.*;

/**
 * Full-screen Application Frame for SmartBilling Pro Login.
 * Displays centered login card over dynamic billing video background.
 */
public class LoginFrame extends JFrame {
    private final LoginPanel loginPanel;

    public LoginFrame() {
        super("SmartBilling Pro - Enterprise POS & Inventory Suite");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Full Screen Configuration
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1024, 680));
        setSize(1366, 768);
        setLocationRelativeTo(null);

        Color winBg = AppTheme.BG_CANVAS;
        setBackground(winBg);
        getContentPane().setBackground(winBg);
        getRootPane().setBackground(winBg);

        // Apply macOS Application Icon
        util.AppIconUtil.applyToWindow(this);

        // macOS Window Header styling: tint title bar to match window background #e6f0e7
        getRootPane().putClientProperty("apple.awt.fullWindowContent", Boolean.TRUE);
        getRootPane().putClientProperty("apple.awt.transparentTitleBar", Boolean.TRUE);
        getRootPane().putClientProperty("apple.awt.windowTitleVisible", Boolean.TRUE);

        // Embed Login Panel
        loginPanel = new LoginPanel(this);
        setContentPane(loginPanel);
    }

    public LoginPanel getLoginPanel() {
        return loginPanel;
    }
}
