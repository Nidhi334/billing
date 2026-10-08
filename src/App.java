import ui.LoginFrame;

import javax.swing.*;

public class App {
    static {
        // Fix macOS Metal Java2D glyph cache bug (drops vowels & characters on macOS JDK 17)
        System.setProperty("sun.java2d.metal", "false");
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    public static void main(String[] args) {

        // Modern System Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }

        // Initialize macOS Dock & System Taskbar Icon
        util.AppIconUtil.setupTaskbarIcon();

        // Launch Application Login Frame
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}

