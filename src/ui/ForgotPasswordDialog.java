package ui;

import dao.UserDAO;
import model.User;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Random;

public class ForgotPasswordDialog extends JDialog {
    private JTextField txtUsername;
    private JLabel lblSecurityQuestion;
    private JTextField txtSecurityAnswer;
    private JPasswordField txtNewPassword;
    private JPasswordField txtConfirmPassword;
    private JTextField txtCaptchaInput;
    private JLabel lblCaptchaDisplay;
    private String generatedCaptcha;

    private JButton btnFindUser;
    private JButton btnResetPassword;
    private JButton btnCancel;
    private JButton btnRefreshCaptcha;

    private JPanel step1Panel;
    private JPanel step2Panel;

    private UserDAO userDAO = new UserDAO();
    private User matchedUser = null;

    public ForgotPasswordDialog(Frame owner) {
        super(owner, "Secure Password Reset", true);
        setSize(480, 560);
        setLocationRelativeTo(owner);
        setResizable(false);
        initComponents();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(248, 250, 252));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 41, 59));
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel lblTitle = new JLabel("🔐 Reset Account Password");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle, BorderLayout.NORTH);

        JLabel lblSub = new JLabel("Verify your identity via username and security answer to set a new password.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(148, 163, 184));
        header.add(lblSub, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // Body Card
        JPanel bodyCard = new JPanel(new GridBagLayout());
        bodyCard.setBackground(Color.WHITE);
        bodyCard.setBorder(new CompoundBorder(
                new EmptyBorder(16, 24, 16, 24),
                new LineBorder(new Color(226, 232, 240), 1, true)
        ));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 12, 5, 12);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.weightx = 1.0;

        // Step 1: Enter Username
        g.gridy = 0;
        JLabel lblStep1 = new JLabel("Step 1: Enter Your Username");
        lblStep1.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStep1.setForeground(new Color(37, 99, 235));
        bodyCard.add(lblStep1, g);

        JPanel pUserRow = new JPanel(new BorderLayout(8, 0));
        pUserRow.setOpaque(false);
        txtUsername = new JTextField();
        txtUsername.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtUsername.setPreferredSize(new Dimension(240, 34));
        pUserRow.add(txtUsername, BorderLayout.CENTER);

        btnFindUser = new JButton("Verify");
        btnFindUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnFindUser.setBackground(new Color(37, 99, 235));
        btnFindUser.setForeground(Color.WHITE);
        btnFindUser.setFocusPainted(false);
        btnFindUser.setPreferredSize(new Dimension(85, 34));
        pUserRow.add(btnFindUser, BorderLayout.EAST);

        g.gridy = 1;
        bodyCard.add(pUserRow, g);

        // Separator
        g.gridy = 2;
        g.insets = new Insets(10, 12, 10, 12);
        JSeparator sep = new JSeparator();
        bodyCard.add(sep, g);

        // Step 2 Container (Initially disabled until user verified)
        step2Panel = new JPanel(new GridBagLayout());
        step2Panel.setOpaque(false);
        GridBagConstraints g2 = new GridBagConstraints();
        g2.insets = new Insets(4, 0, 4, 0);
        g2.fill = GridBagConstraints.HORIZONTAL;
        g2.gridx = 0;
        g2.weightx = 1.0;

        g2.gridy = 0;
        JLabel lblStep2 = new JLabel("Step 2: Answer Security Question & Reset");
        lblStep2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStep2.setForeground(new Color(15, 23, 42));
        step2Panel.add(lblStep2, g2);

        // Security Question Label
        g2.gridy = 1;
        lblSecurityQuestion = new JLabel("Security Question: (Enter username above first)");
        lblSecurityQuestion.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblSecurityQuestion.setForeground(new Color(71, 85, 105));
        step2Panel.add(lblSecurityQuestion, g2);

        // Security Answer
        g2.gridy = 2;
        txtSecurityAnswer = new JTextField();
        txtSecurityAnswer.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSecurityAnswer.setPreferredSize(new Dimension(0, 32));
        txtSecurityAnswer.setEnabled(false);
        step2Panel.add(txtSecurityAnswer, g2);

        // New Password
        g2.gridy = 3;
        JLabel lblNewPass = new JLabel("New Password:");
        lblNewPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        step2Panel.add(lblNewPass, g2);

        g2.gridy = 4;
        txtNewPassword = new JPasswordField();
        txtNewPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtNewPassword.setPreferredSize(new Dimension(0, 32));
        txtNewPassword.setEnabled(false);
        step2Panel.add(txtNewPassword, g2);

        // Confirm New Password
        g2.gridy = 5;
        JLabel lblConfPass = new JLabel("Confirm New Password:");
        lblConfPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        step2Panel.add(lblConfPass, g2);

        g2.gridy = 6;
        txtConfirmPassword = new JPasswordField();
        txtConfirmPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtConfirmPassword.setPreferredSize(new Dimension(0, 32));
        txtConfirmPassword.setEnabled(false);
        step2Panel.add(txtConfirmPassword, g2);

        // Captcha verification
        g2.gridy = 7;
        JPanel pCaptcha = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        pCaptcha.setOpaque(false);
        pCaptcha.add(new JLabel("Captcha:"));

        lblCaptchaDisplay = new JLabel();
        lblCaptchaDisplay.setFont(new Font("Monospaced", Font.BOLD | Font.ITALIC, 16));
        lblCaptchaDisplay.setForeground(new Color(180, 83, 9));
        lblCaptchaDisplay.setOpaque(true);
        lblCaptchaDisplay.setBackground(new Color(254, 243, 199));
        lblCaptchaDisplay.setBorder(new EmptyBorder(3, 8, 3, 8));
        pCaptcha.add(lblCaptchaDisplay);

        btnRefreshCaptcha = new JButton("↻");
        btnRefreshCaptcha.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnRefreshCaptcha.setMargin(new Insets(1, 4, 1, 4));
        btnRefreshCaptcha.addActionListener(e -> generateNewCaptcha());
        pCaptcha.add(btnRefreshCaptcha);

        txtCaptchaInput = new JTextField(6);
        txtCaptchaInput.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtCaptchaInput.setEnabled(false);
        pCaptcha.add(new JLabel("Enter code:"));
        pCaptcha.add(txtCaptchaInput);

        step2Panel.add(pCaptcha, g2);

        g.gridy = 3;
        g.insets = new Insets(0, 12, 10, 12);
        bodyCard.add(step2Panel, g);

        root.add(bodyCard, BorderLayout.CENTER);

        // Bottom Actions
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        bottomBar.setBackground(new Color(241, 245, 249));

        btnCancel = new JButton("Cancel");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnCancel.setPreferredSize(new Dimension(90, 34));
        bottomBar.add(btnCancel);

        btnResetPassword = new JButton("Reset Password");
        btnResetPassword.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnResetPassword.setBackground(new Color(16, 185, 129));
        btnResetPassword.setForeground(Color.WHITE);
        btnResetPassword.setFocusPainted(false);
        btnResetPassword.setPreferredSize(new Dimension(140, 34));
        btnResetPassword.setEnabled(false);
        bottomBar.add(btnResetPassword);

        root.add(bottomBar, BorderLayout.SOUTH);
        add(root);

        // Listeners
        generateNewCaptcha();
        btnFindUser.addActionListener(e -> verifyUsername());
        txtUsername.addActionListener(e -> verifyUsername());
        btnResetPassword.addActionListener(e -> performPasswordReset());
        btnCancel.addActionListener(e -> dispose());
    }

    private void generateNewCaptcha() {
        String chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        Random r = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        generatedCaptcha = sb.toString();
        lblCaptchaDisplay.setText(generatedCaptcha);
    }

    private void verifyUsername() {
        String username = txtUsername.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your username.", "Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User user = userDAO.getUserByUsername(username);
            if (user != null) {
                matchedUser = user;
                String q = user.getSecurityQuestion();
                if (q == null || q.trim().isEmpty()) {
                    q = "What is your favorite color?";
                }
                lblSecurityQuestion.setText("Question: " + q);
                lblSecurityQuestion.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lblSecurityQuestion.setForeground(new Color(30, 41, 59));

                txtSecurityAnswer.setEnabled(true);
                txtNewPassword.setEnabled(true);
                txtConfirmPassword.setEnabled(true);
                txtCaptchaInput.setEnabled(true);
                btnResetPassword.setEnabled(true);
                txtUsername.setEditable(false);
                btnFindUser.setEnabled(false);

                txtSecurityAnswer.requestFocus();
            } else {
                JOptionPane.showMessageDialog(this, "No account found with username '" + username + "'", "Not Found", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performPasswordReset() {
        if (matchedUser == null) return;

        String answer = txtSecurityAnswer.getText().trim();
        String newPass = new String(txtNewPassword.getPassword()).trim();
        String confPass = new String(txtConfirmPassword.getPassword()).trim();
        String captcha = txtCaptchaInput.getText().trim();

        if (answer.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter the answer to the security question.", "Input Error", JOptionPane.WARNING_MESSAGE);
            txtSecurityAnswer.requestFocus();
            return;
        }

        if (newPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a new password.", "Input Error", JOptionPane.WARNING_MESSAGE);
            txtNewPassword.requestFocus();
            return;
        }

        if (newPass.length() < 4) {
            JOptionPane.showMessageDialog(this, "Password should be at least 4 characters long.", "Weak Password", JOptionPane.WARNING_MESSAGE);
            txtNewPassword.requestFocus();
            return;
        }

        if (!newPass.equals(confPass)) {
            JOptionPane.showMessageDialog(this, "New password and confirmation do not match!", "Mismatch", JOptionPane.WARNING_MESSAGE);
            txtConfirmPassword.requestFocus();
            return;
        }

        if (!generatedCaptcha.equalsIgnoreCase(captcha)) {
            JOptionPane.showMessageDialog(this, "Incorrect captcha code. Please re-enter.", "Captcha Failed", JOptionPane.ERROR_MESSAGE);
            generateNewCaptcha();
            txtCaptchaInput.setText("");
            txtCaptchaInput.requestFocus();
            return;
        }

        // Verify security answer match
        String expectedAnswer = matchedUser.getSecurityAnswer();
        if (expectedAnswer == null || expectedAnswer.trim().isEmpty()) {
            expectedAnswer = "blue";
        }

        if (!expectedAnswer.trim().equalsIgnoreCase(answer)) {
            JOptionPane.showMessageDialog(this, "Incorrect security answer. Access denied.", "Security Verification Failed", JOptionPane.ERROR_MESSAGE);
            txtSecurityAnswer.selectAll();
            txtSecurityAnswer.requestFocus();
            return;
        }

        // Proceed to update password
        try {
            boolean success = userDAO.resetPassword(matchedUser.getUsername(), answer, newPass);
            if (success) {
                JOptionPane.showMessageDialog(this, 
                        "Password reset successful!
You can now login with your new password.", 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update password in database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error updating password: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
