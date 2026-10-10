package ui;

import dao.CustomerDAO;
import model.Customer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern Keyra-Themed Customer Directory & CRM Hub
 */
public class CustomerPanel extends JPanel {
    private final CustomerDAO customerDAO = new CustomerDAO();

    // Table & Models
    private JTable table;
    private DefaultTableModel tableModel;
    private List<Customer> loadedCustomers = new ArrayList<>();

    // Form inputs
    private JTextField txtName, txtPhone, txtEmail, txtAddress;
    private ModernSearchField txtSearch;
    private JLabel lblFormTitle, lblFormBadge;
    private JLabel lblAvatarInitial, lblAvatarName, lblAvatarPhone, lblAvatarStats;
    private JPanel avatarCard;

    // Buttons
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnCancelEdit;
    private JButton btnFilterAll, btnFilterWithPhone, btnFilterWithEmail;

    // KPI Summary Labels
    private JLabel lblKpiTotal, lblKpiWithPhone, lblKpiWithEmail, lblKpiCoverage;
    private JLabel lblResultCount;

    // State
    private int selectedCustomerId = -1;
    private String currentContactFilter = "ALL"; // ALL, WITH_PHONE, WITH_EMAIL
    private JPanel editButtonsRow;

    public CustomerPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(AppTheme.BG_CANVAS);
        setBorder(new EmptyBorder(12, 14, 12, 14));
        initComponents();
        loadCustomerTable();
    }

    private void initComponents() {
        // TOP CONTAINER: Header Strip & KPI Metric Cards
        JPanel topContainer = new JPanel(new BorderLayout(0, 10));
        topContainer.setOpaque(false);

        // 1. Elevated Header Strip
        JPanel headerStrip = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = 18;
                g2.setColor(new Color(0, 50, 30, 5));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        headerStrip.setOpaque(false);
        headerStrip.setBorder(new EmptyBorder(8, 14, 8, 14));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerLeft.setOpaque(false);

        JLabel lblTitle = new JLabel("👥 Customer Directory & Relationship Hub");
        lblTitle.setFont(AppTheme.font(Font.BOLD, 15));
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel lblBadge = new JLabel(" CRM & DIRECTORY ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                int h = getHeight();
                g2.fillRoundRect(0, 0, getWidth(), h, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblBadge.setFont(AppTheme.font(Font.BOLD, 9));
        lblBadge.setOpaque(false);
        lblBadge.setBackground(new Color(230, 245, 236));
        lblBadge.setForeground(AppTheme.FOREST_DEEP);
        lblBadge.setBorder(new EmptyBorder(3, 8, 3, 8));

        JLabel lblSubtitle = new JLabel("Manage client profiles, contact information, purchase history & loyalty tracking");
        lblSubtitle.setFont(AppTheme.font(Font.PLAIN, 11));
        lblSubtitle.setForeground(AppTheme.TEXT_MUTED);

        headerLeft.add(lblTitle);
        headerLeft.add(lblBadge);
        headerLeft.add(lblSubtitle);
        headerStrip.add(headerLeft, BorderLayout.WEST);

        // Header Right Action Buttons
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerRight.setOpaque(false);

        JButton btnNewCustomer = createPillButton("➕ New Customer", AppTheme.FOREST_GREEN, AppTheme.FOREST_GREEN, Color.WHITE);
        btnNewCustomer.setFont(AppTheme.font(Font.BOLD, 11));
        btnNewCustomer.addActionListener(e -> {
            clearForm();
            if (txtName != null) txtName.requestFocusInWindow();
        });

        JButton btnHeaderRefresh = createPillButton("🔄 Refresh", Color.WHITE, AppTheme.BORDER_SAGE, AppTheme.TEXT_SECONDARY);
        btnHeaderRefresh.setFont(AppTheme.font(Font.BOLD, 11));
        btnHeaderRefresh.addActionListener(e -> {
            if (txtSearch != null) txtSearch.setText("");
            currentContactFilter = "ALL";
            updateFilterButtonsUI();
            loadCustomerTable();
        });

        headerRight.add(btnNewCustomer);
        headerRight.add(btnHeaderRefresh);
        headerStrip.add(headerRight, BorderLayout.EAST);

        topContainer.add(headerStrip, BorderLayout.NORTH);

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 10, 0));
        kpiRow.setOpaque(false);
        kpiRow.setPreferredSize(new Dimension(0, 76));

        lblKpiTotal = new JLabel("0");
        lblKpiWithPhone = new JLabel("0");
        lblKpiWithEmail = new JLabel("0");
        lblKpiCoverage = new JLabel("100%");

        JPanel cardTotal = createKpiCard("TOTAL CUSTOMERS", lblKpiTotal, "Registered in directory", AppTheme.FOREST_MID, "customers", () -> {
            currentContactFilter = "ALL";
            updateFilterButtonsUI();
            applyFilter();
        });
        JPanel cardPhone = createKpiCard("PHONE CONTACTS", lblKpiWithPhone, "Mobile billing active", AppTheme.STATUS_SUCCESS, "check_circle", () -> {
            currentContactFilter = "WITH_PHONE";
            updateFilterButtonsUI();
            applyFilter();
        });
        JPanel cardEmail = createKpiCard("EMAIL INVOICES", lblKpiWithEmail, "E-receipt verified", AppTheme.FOREST_GREEN, "zap", () -> {
            currentContactFilter = "WITH_EMAIL";
            updateFilterButtonsUI();
            applyFilter();
        });
        JPanel cardCoverage = createKpiCard("DIRECTORY HEALTH", lblKpiCoverage, "Profile completeness", AppTheme.FOREST_DEEP, "wallet", () -> {
            currentContactFilter = "ALL";
            updateFilterButtonsUI();
            applyFilter();
        });

        kpiRow.add(cardTotal);
        kpiRow.add(cardPhone);
        kpiRow.add(cardEmail);
        kpiRow.add(cardCoverage);

        topContainer.add(kpiRow, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // =========================================================================
        // CENTER: 2-COLUMN WORKSPACE (Left Form Card + Right Customers Table Card)
        // =========================================================================
        JPanel centerSplit = new JPanel(new BorderLayout(12, 0));
        centerSplit.setOpaque(false);

        // -------------------------------------------------------------------------
        // LEFT COLUMN: Elevated Customer Profile & Form Card (370px)
        // -------------------------------------------------------------------------
        JPanel formCard = createElevatedCard(20);
        formCard.setPreferredSize(new Dimension(370, 0));
        formCard.setLayout(new BorderLayout(0, 8));
        formCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Form Card Top Header
        JPanel formHeader = new JPanel(new BorderLayout(8, 0));
        formHeader.setOpaque(false);

        JPanel formTitleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        formTitleRow.setOpaque(false);

        lblFormTitle = new JLabel("✨ Register Customer");
        lblFormTitle.setFont(AppTheme.font(Font.BOLD, 14));
        lblFormTitle.setForeground(AppTheme.TEXT_PRIMARY);

        lblFormBadge = new JLabel("") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                int h = getHeight();
                g2.fillRoundRect(0, 0, getWidth(), h, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblFormBadge.setFont(AppTheme.font(Font.BOLD, 9));
        lblFormBadge.setOpaque(false);
        lblFormBadge.setBackground(new Color(230, 248, 238));
        lblFormBadge.setForeground(AppTheme.FOREST_GREEN);
        lblFormBadge.setBorder(new EmptyBorder(2, 6, 2, 6));
        lblFormBadge.setVisible(false);

        formTitleRow.add(lblFormTitle);
        formTitleRow.add(lblFormBadge);
        formHeader.add(formTitleRow, BorderLayout.WEST);

        btnClear = createPillButton("Clear Form", new Color(248, 250, 252), new Color(226, 232, 240), AppTheme.TEXT_MUTED);
        btnClear.setFont(AppTheme.font(Font.PLAIN, 11));
        btnClear.setPreferredSize(new Dimension(84, 26));
        btnClear.addActionListener(e -> clearForm());
        formHeader.add(btnClear, BorderLayout.EAST);

        formCard.add(formHeader, BorderLayout.NORTH);

        // Form Fields Container
        JPanel formBody = new JPanel();
        formBody.setLayout(new BoxLayout(formBody, BoxLayout.Y_AXIS));
        formBody.setOpaque(false);
        formBody.setBorder(new EmptyBorder(0, 0, 10, 4));

        // Row 1: Profile Avatar Hero Card
        avatarCard = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(248, 252, 249));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatarCard.setOpaque(false);
        avatarCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        lblAvatarInitial = new JLabel("👤", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int sz = Math.min(getWidth(), getHeight());
                int x = (getWidth() - sz) / 2;
                int y = (getHeight() - sz) / 2;

                // Gradient circle
                GradientPaint gp = new GradientPaint(x, y, AppTheme.FOREST_MID, x + sz, y + sz, AppTheme.FOREST_DEEP);
                g2.setPaint(gp);
                g2.fillOval(x, y, sz, sz);

                g2.setColor(new Color(255, 255, 255, 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x, y, sz - 1, sz - 1);

                g2.setColor(Color.WHITE);
                g2.setFont(AppTheme.font(Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                int tx = (getWidth() - fm.stringWidth(t)) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(t, tx, ty);
                g2.dispose();
            }
        };
        lblAvatarInitial.setPreferredSize(new Dimension(48, 48));
        lblAvatarInitial.setOpaque(false);
        avatarCard.add(lblAvatarInitial, BorderLayout.WEST);

        JPanel avatarMeta = new JPanel(new GridLayout(3, 1, 0, 2));
        avatarMeta.setOpaque(false);

        lblAvatarName = new JLabel("Walk-in / New Customer");
        lblAvatarName.setFont(AppTheme.font(Font.BOLD, 13));
        lblAvatarName.setForeground(AppTheme.TEXT_PRIMARY);

        lblAvatarPhone = new JLabel("No mobile number assigned");
        lblAvatarPhone.setFont(AppTheme.font(Font.PLAIN, 11));
        lblAvatarPhone.setForeground(AppTheme.TEXT_MUTED);

        lblAvatarStats = new JLabel("🛍️ Ready for new billing invoice");
        lblAvatarStats.setFont(AppTheme.font(Font.BOLD, 10));
        lblAvatarStats.setForeground(AppTheme.FOREST_MID);

        avatarMeta.add(lblAvatarName);
        avatarMeta.add(lblAvatarPhone);
        avatarMeta.add(lblAvatarStats);
        avatarCard.add(avatarMeta, BorderLayout.CENTER);

        formBody.add(avatarCard);
        formBody.add(Box.createVerticalStrut(10));

        // Row 2: Full Name
        JPanel nameRow = new JPanel(new BorderLayout(0, 3));
        nameRow.setOpaque(false);
        nameRow.add(createFieldLabel("Full Customer Name:"), BorderLayout.NORTH);
        txtName = new ModernInputField("e.g. Rahul Sharma");
        txtName.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { syncAvatarPreview(); }
            public void removeUpdate(DocumentEvent e) { syncAvatarPreview(); }
            public void changedUpdate(DocumentEvent e) { syncAvatarPreview(); }
        });
        nameRow.add(txtName, BorderLayout.CENTER);
        formBody.add(nameRow);
        formBody.add(Box.createVerticalStrut(8));

        // Row 3: Phone Number
        JPanel phoneRow = new JPanel(new BorderLayout(0, 3));
        phoneRow.setOpaque(false);
        phoneRow.add(createFieldLabel("Phone Number (10 Digits):"), BorderLayout.NORTH);
        txtPhone = new ModernInputField("e.g. 9876543210");
        txtPhone.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { syncAvatarPreview(); }
            public void removeUpdate(DocumentEvent e) { syncAvatarPreview(); }
            public void changedUpdate(DocumentEvent e) { syncAvatarPreview(); }
        });
        phoneRow.add(txtPhone, BorderLayout.CENTER);
        formBody.add(phoneRow);
        formBody.add(Box.createVerticalStrut(8));

        // Row 4: Email Address
        JPanel emailRow = new JPanel(new BorderLayout(0, 3));
        emailRow.setOpaque(false);
        emailRow.add(createFieldLabel("Email Address (Optional):"), BorderLayout.NORTH);
        txtEmail = new ModernInputField("e.g. rahul.sharma@gmail.com");
        emailRow.add(txtEmail, BorderLayout.CENTER);
        formBody.add(emailRow);
        formBody.add(Box.createVerticalStrut(8));

        // Row 5: Billing Address
        JPanel addrRow = new JPanel(new BorderLayout(0, 3));
        addrRow.setOpaque(false);
        addrRow.add(createFieldLabel("Billing / Shipping Address:"), BorderLayout.NORTH);
        txtAddress = new ModernInputField("e.g. Flat 402, Green Glen Layout, Bangalore");
        addrRow.add(txtAddress, BorderLayout.CENTER);
        formBody.add(addrRow);
        formBody.add(Box.createVerticalStrut(12));

        // Scroll pane without horizontal scrollbar and with slim vertical scrollbar
        JScrollPane formScroll = new JScrollPane(formBody);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        formScroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(205, 230, 215);
                this.trackColor = new Color(248, 252, 249);
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isThumbRollover() ? AppTheme.FOREST_MID : thumbColor);
                g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 2, thumbBounds.width - 2, thumbBounds.height - 4, 6, 6);
                g2.dispose();
            }
        });
        formCard.add(formScroll, BorderLayout.CENTER);

        // ---------------------------------------------------------------------
        // Form Action CTAs Panel (South)
        // ---------------------------------------------------------------------
        JPanel formActionsContainer = new JPanel(new BorderLayout(0, 6));
        formActionsContainer.setOpaque(false);
        formActionsContainer.setBorder(new EmptyBorder(6, 0, 0, 0));

        // New Mode Button (Add)
        btnAdd = createPillButton("➕ Add Customer to Directory", AppTheme.FOREST_GREEN, AppTheme.FOREST_GREEN, Color.WHITE);
        btnAdd.setFont(AppTheme.font(Font.BOLD, 13));
        btnAdd.setPreferredSize(new Dimension(0, 36));
        btnAdd.addActionListener(e -> addCustomer());

        // Edit Mode Buttons (Save / Delete / Cancel)
        editButtonsRow = new JPanel(new GridLayout(1, 3, 6, 0));
        editButtonsRow.setOpaque(false);

        btnUpdate = createPillButton("💾 Save", AppTheme.FOREST_GREEN, AppTheme.FOREST_GREEN, Color.WHITE);
        btnUpdate.setFont(AppTheme.font(Font.BOLD, 12));
        btnUpdate.setPreferredSize(new Dimension(0, 36));
        btnUpdate.addActionListener(e -> updateCustomer());

        btnDelete = createPillButton("🗑️ Delete", new Color(254, 242, 242), new Color(254, 202, 202), AppTheme.STATUS_DANGER);
        btnDelete.setFont(AppTheme.font(Font.BOLD, 12));
        btnDelete.setPreferredSize(new Dimension(0, 36));
        btnDelete.addActionListener(e -> deleteCustomer());

        btnCancelEdit = createPillButton("Cancel", new Color(240, 250, 244), AppTheme.BORDER_SAGE, AppTheme.TEXT_SECONDARY);
        btnCancelEdit.setFont(AppTheme.font(Font.BOLD, 12));
        btnCancelEdit.setPreferredSize(new Dimension(0, 36));
        btnCancelEdit.addActionListener(e -> clearForm());

        editButtonsRow.add(btnUpdate);
        editButtonsRow.add(btnDelete);
        editButtonsRow.add(btnCancelEdit);
        editButtonsRow.setVisible(false);

        formActionsContainer.add(btnAdd, BorderLayout.NORTH);
        formActionsContainer.add(editButtonsRow, BorderLayout.SOUTH);
        formCard.add(formActionsContainer, BorderLayout.SOUTH);

        centerSplit.add(formCard, BorderLayout.WEST);

        // -------------------------------------------------------------------------
        // RIGHT COLUMN: Elevated Customers Directory Table & Explorer Card
        // -------------------------------------------------------------------------
        JPanel tableCard = createElevatedCard(20);
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Table Filter & Search Toolbar (North)
        JPanel tableToolbar = new JPanel(new GridLayout(2, 1, 0, 6));
        tableToolbar.setOpaque(false);

        // Toolbar Line 1: Search (West) + Result count (East)
        JPanel line1 = new JPanel(new BorderLayout(10, 0));
        line1.setOpaque(false);

        txtSearch = new ModernSearchField("Search by customer name, phone number, or email...");
        txtSearch.setPreferredSize(new Dimension(320, 32));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        line1.add(txtSearch, BorderLayout.WEST);

        lblResultCount = new JLabel("0 customers") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                int h = getHeight();
                g2.fillRoundRect(0, 0, getWidth(), h, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblResultCount.setFont(AppTheme.font(Font.BOLD, 10));
        lblResultCount.setOpaque(false);
        lblResultCount.setBackground(new Color(240, 250, 244));
        lblResultCount.setForeground(AppTheme.FOREST_DEEP);
        lblResultCount.setBorder(new EmptyBorder(3, 10, 3, 10));
        line1.add(lblResultCount, BorderLayout.EAST);
        tableToolbar.add(line1);

        // Toolbar Line 2: Contact Filter Chips
        JPanel line2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        line2.setOpaque(false);

        JLabel lblFilterTag = new JLabel("Filter View:");
        lblFilterTag.setFont(AppTheme.font(Font.BOLD, 11));
        lblFilterTag.setForeground(AppTheme.TEXT_MUTED);
        line2.add(lblFilterTag);

        btnFilterAll = createFilterChip("All Customers", "ALL");
        btnFilterWithPhone = createFilterChip("With Phone", "WITH_PHONE");
        btnFilterWithEmail = createFilterChip("With Email", "WITH_EMAIL");

        line2.add(btnFilterAll);
        line2.add(btnFilterWithPhone);
        line2.add(btnFilterWithEmail);
        tableToolbar.add(line2);

        tableCard.add(tableToolbar, BorderLayout.NORTH);

        // ---------------------------------------------------------------------
        // Customers JTable
        // ---------------------------------------------------------------------
        String[] cols = {"#", "Customer Profile", "Phone", "Email Address", "Address", "Reg. Date", "Action"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    if (row % 2 == 1) {
                        c.setBackground(new Color(251, 254, 252));
                    } else {
                        c.setBackground(Color.WHITE);
                    }
                } else {
                    c.setBackground(new Color(230, 248, 238));
                }
                return c;
            }
        };

        table.setOpaque(false);
        table.setRowHeight(42);
        table.setFont(AppTheme.font(Font.PLAIN, 12));
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(241, 246, 243));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(230, 248, 238));
        table.setSelectionForeground(AppTheme.TEXT_PRIMARY);

        // Header Styling
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setFont(AppTheme.font(Font.BOLD, 12));
                l.setForeground(AppTheme.TEXT_PRIMARY);
                l.setBackground(new Color(240, 250, 244));
                l.setOpaque(true);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_SAGE),
                        new EmptyBorder(0, 10, 0, 10)
                ));
                if (c == 0 || c == 5 || c == 6) {
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return l;
            }
        });

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(35);  // #
        table.getColumnModel().getColumn(1).setPreferredWidth(210); // Customer Profile
        table.getColumnModel().getColumn(2).setPreferredWidth(125); // Phone
        table.getColumnModel().getColumn(3).setPreferredWidth(170); // Email
        table.getColumnModel().getColumn(4).setPreferredWidth(220); // Address
        table.getColumnModel().getColumn(5).setPreferredWidth(120); // Date
        table.getColumnModel().getColumn(6).setPreferredWidth(85);  // Actions

        // Cell Renderers
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        centerRender.setFont(AppTheme.font(Font.BOLD, 11));
        centerRender.setForeground(AppTheme.TEXT_MUTED);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        // Column 1: Customer Profile (Dual line: Bold Name + ID badge)
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                String raw = String.valueOf(val != null ? val : "");
                String[] parts = raw.split("###");
                String name = parts[0];
                String idStr = parts.length > 1 ? parts[1] : "";

                l.setText("<html><body style='font-family:Segoe UI;'><b style='color:#111827; font-size:11px;'>" 
                        + escapeHtml(name) + "</b><br><span style='color:#6b7280; font-size:9px;'>ID: #" 
                        + escapeHtml(idStr) + "</span></body></html>");
                l.setBorder(new EmptyBorder(2, 10, 2, 6));
                return l;
            }
        });

        // Column 2: Phone Number (Forest Mid, Monospaced)
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                String phone = String.valueOf(val != null ? val : "");
                l.setFont(new Font("Monospaced", Font.BOLD, 12));
                l.setForeground(AppTheme.FOREST_MID);
                l.setText(phone.isEmpty() ? "—" : "📞 " + phone);
                l.setBorder(new EmptyBorder(0, 10, 0, 6));
                return l;
            }
        });

        // Column 3: Email Address
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                String email = String.valueOf(val != null ? val : "");
                l.setFont(AppTheme.font(Font.PLAIN, 12));
                l.setForeground(AppTheme.TEXT_SECONDARY);
                l.setText(email.isEmpty() ? "—" : "✉️ " + email);
                l.setBorder(new EmptyBorder(0, 8, 0, 8));
                return l;
            }
        });

        // Column 4: Billing Address
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                String addr = String.valueOf(val != null ? val : "");
                l.setFont(AppTheme.font(Font.PLAIN, 12));
                l.setForeground(AppTheme.TEXT_SECONDARY);
                l.setText(addr.isEmpty() ? "—" : "📍 " + addr);
                l.setBorder(new EmptyBorder(0, 8, 0, 8));
                return l;
            }
        });

        // Column 5: Registration Date
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(AppTheme.font(Font.PLAIN, 11));
                l.setForeground(AppTheme.TEXT_MUTED);
                l.setText(String.valueOf(val != null ? val : "—"));
                return l;
            }
        });

        // Column 6: Actions (Edit profile indicator)
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(AppTheme.font(Font.BOLD, 11));
                l.setForeground(AppTheme.FOREST_GREEN);
                l.setText("✏️ Edit");
                return l;
            }
        });

        // Table Selection Listener
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                populateFormFromSelectedRow();
            }
        });

        // Table Viewport with Empty State
        JViewport customViewport = new JViewport() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (tableModel.getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth();
                    int h = getHeight();
                    int cy = Math.max(130, h / 3);

                    SidebarIcon userIcon = new SidebarIcon("customers", 50, new Color(195, 222, 210));
                    userIcon.paintIcon(this, g2, (w - 50) / 2, cy - 65);

                    g2.setColor(AppTheme.TEXT_PRIMARY);
                    g2.setFont(AppTheme.font(Font.BOLD, 15));
                    FontMetrics fm1 = g2.getFontMetrics();
                    String t1 = "No Customers Found";
                    g2.drawString(t1, (w - fm1.stringWidth(t1)) / 2, cy + 16);

                    g2.setColor(AppTheme.TEXT_MUTED);
                    g2.setFont(AppTheme.font(Font.PLAIN, 12));
                    FontMetrics fm2 = g2.getFontMetrics();
                    String t2 = "Try adjusting your search criteria or register a new client profile";
                    g2.drawString(t2, (w - fm2.stringWidth(t2)) / 2, cy + 38);

                    g2.dispose();
                }
            }
        };
        customViewport.setOpaque(false);
        customViewport.setView(table);

        JScrollPane tableScroll = new JScrollPane();
        tableScroll.setViewport(customViewport);
        tableScroll.setColumnHeaderView(table.getTableHeader());
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.setOpaque(false);
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableScroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        tableScroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(205, 230, 215);
                this.trackColor = new Color(248, 252, 249);
            }
            @Override
            protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
            @Override
            protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isThumbRollover() ? AppTheme.FOREST_MID : thumbColor);
                g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 2, thumbBounds.width - 2, thumbBounds.height - 4, 6, 6);
                g2.dispose();
            }
        });

        tableCard.add(tableScroll, BorderLayout.CENTER);
        centerSplit.add(tableCard, BorderLayout.CENTER);

        add(centerSplit, BorderLayout.CENTER);

        updateFilterButtonsUI();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // -------------------------------------------------------------------------
    // Helper Component Creators
    // -------------------------------------------------------------------------
    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppTheme.font(Font.BOLD, 11));
        l.setForeground(AppTheme.TEXT_SECONDARY);
        return l;
    }

    private JPanel createElevatedCard(int arc) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Ambient drop shadow
                g2.setColor(new Color(0, 50, 30, 6));
                g2.fillRoundRect(1, 2, w - 2, h - 2, arc, arc);
                // Surface
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                // Border
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        return card;
    }

    private JButton createPillButton(String text, Color bg, Color border, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = h;

                if (hovered) {
                    if (bg.equals(AppTheme.FOREST_GREEN)) {
                        g2.setColor(AppTheme.FOREST_MID);
                    } else {
                        g2.setColor(new Color(Math.max(0, bg.getRed() - 10), Math.max(0, bg.getGreen() - 10), Math.max(0, bg.getBlue() - 10)));
                    }
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, w, h, r, r);

                if (border != null) {
                    g2.setColor(border);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                }

                g2.setColor(fg);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createFilterChip(String text, String filterKey) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = h;
                boolean active = currentContactFilter.equals(filterKey);

                if (active) {
                    g2.setColor(AppTheme.FOREST_GREEN);
                    g2.fillRoundRect(0, 0, w, h, r, r);
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, w, h, r, r);
                    g2.setColor(AppTheme.BORDER_SAGE);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                    g2.setColor(AppTheme.TEXT_SECONDARY);
                }

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tx = (w - fm.stringWidth(getText())) / 2;
                int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), tx, ty);
                g2.dispose();
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 11));
        btn.setPreferredSize(new Dimension(96, 26));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            currentContactFilter = filterKey;
            updateFilterButtonsUI();
            applyFilter();
        });
        return btn;
    }

    private void updateFilterButtonsUI() {
        if (btnFilterAll != null) btnFilterAll.repaint();
        if (btnFilterWithPhone != null) btnFilterWithPhone.repaint();
        if (btnFilterWithEmail != null) btnFilterWithEmail.repaint();
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, String subtitle, Color accentColor, String iconName, Runnable onClick) {
        SidebarIcon bgIcon = new SidebarIcon(iconName, 44, accentColor);
        JPanel card = new JPanel(new BorderLayout()) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                    @Override public void mouseClicked(MouseEvent e) { if (onClick != null) onClick.run(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int r = 18;
                int w = getWidth(), h = getHeight();

                g2.setColor(new Color(0, 50, 30, hovered ? 10 : 5));
                g2.fillRoundRect(1, 2, w - 2, h - 2, r, r);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);
                g2.setColor(hovered ? AppTheme.FOREST_MID : AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);

                Shape clip = new RoundRectangle2D.Float(0, 0, w, h, r, r);
                g2.setClip(clip);

                // Watermark icon
                int iconSz = 44;
                int iconX = w - iconSz - 10;
                int iconY = (h - iconSz) / 2;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hovered ? 0.22f : 0.12f));
                bgIcon.paintIcon(this, g2, iconX, iconY);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setBorder(new EmptyBorder(8, 14, 8, 14));
        card.setPreferredSize(new Dimension(160, 76));

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(AppTheme.font(Font.BOLD, 10));
        lblTitle.setForeground(AppTheme.TEXT_MUTED);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(AppTheme.font(Font.BOLD, 18));
        valueLabel.setForeground(AppTheme.TEXT_PRIMARY);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(AppTheme.font(Font.PLAIN, 10));
        lblSub.setForeground(AppTheme.TEXT_SECONDARY);
        lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        textCol.add(lblTitle);
        textCol.add(Box.createVerticalStrut(2));
        textCol.add(valueLabel);
        textCol.add(Box.createVerticalStrut(2));
        textCol.add(lblSub);

        card.add(textCol, BorderLayout.CENTER);
        return card;
    }

    // -------------------------------------------------------------------------
    // Modern Input Field Components
    // -------------------------------------------------------------------------
    private static class ModernInputField extends JTextField {
        private final String placeholder;
        private boolean focused = false;

        public ModernInputField(String placeholder) {
            this.placeholder = placeholder;
            setFont(AppTheme.font(Font.PLAIN, 12));
            setForeground(AppTheme.TEXT_PRIMARY);
            setOpaque(false);
            setBorder(new EmptyBorder(6, 10, 6, 10));
            setPreferredSize(new Dimension(0, 32));

            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int r = 12;

            g2.setColor(new Color(250, 253, 251));
            g2.fillRoundRect(0, 0, w, h, r, r);

            if (focused) {
                g2.setColor(AppTheme.FOREST_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
            super.paintComponent(g);

            if (getText().isEmpty() && !focused && placeholder != null) {
                g2.setColor(AppTheme.TEXT_MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(placeholder, 10, ty);
            }
            g2.dispose();
        }
    }

    private static class ModernSearchField extends JTextField {
        private final String placeholder;
        private boolean focused = false;

        public ModernSearchField(String placeholder) {
            this.placeholder = placeholder;
            setFont(AppTheme.font(Font.PLAIN, 12));
            setForeground(AppTheme.TEXT_PRIMARY);
            setOpaque(false);
            setBorder(new EmptyBorder(6, 30, 6, 10));

            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) { focused = true; repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent e) { focused = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int r = h;

            g2.setColor(new Color(248, 252, 249));
            g2.fillRoundRect(0, 0, w, h, r, r);

            if (focused) {
                g2.setColor(AppTheme.FOREST_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);

            // Magnifying glass icon prefix
            g2.setColor(AppTheme.TEXT_MUTED);
            g2.setFont(AppTheme.font(Font.PLAIN, 12));
            g2.drawString("🔍", 8, h / 2 + 5);

            super.paintComponent(g);

            if (getText().isEmpty() && !focused && placeholder != null) {
                g2.setColor(AppTheme.TEXT_MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(placeholder, 30, ty);
            }
            g2.dispose();
        }
    }

    // -------------------------------------------------------------------------
    // Business Logic & Form Synchronization
    // -------------------------------------------------------------------------
    private void syncAvatarPreview() {
        String name = txtName != null ? txtName.getText().trim() : "";
        String phone = txtPhone != null ? txtPhone.getText().trim() : "";

        if (name.isEmpty()) {
            lblAvatarInitial.setText("👤");
            lblAvatarName.setText("Walk-in / New Customer");
            lblAvatarPhone.setText(phone.isEmpty() ? "No mobile number assigned" : "📞 " + phone);
        } else {
            String initial = getInitials(name);
            lblAvatarInitial.setText(initial);
            lblAvatarName.setText(name);
            lblAvatarPhone.setText(phone.isEmpty() ? "No mobile number assigned" : "📞 " + phone);
        }
        avatarCard.repaint();
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    public void loadCustomerTable() {
        try {
            loadedCustomers = customerDAO.getAllCustomers();
            updateKpiStats();
            applyFilter();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateKpiStats() {
        int total = loadedCustomers.size();
        int withPhone = 0;
        int withEmail = 0;

        for (Customer c : loadedCustomers) {
            if (c.getPhone() != null && !c.getPhone().trim().isEmpty()) withPhone++;
            if (c.getEmail() != null && !c.getEmail().trim().isEmpty()) withEmail++;
        }

        lblKpiTotal.setText(String.valueOf(total));
        lblKpiWithPhone.setText(String.valueOf(withPhone));
        lblKpiWithEmail.setText(String.valueOf(withEmail));
        int pct = total > 0 ? (int) Math.round(((double) withPhone / total) * 100.0) : 100;
        lblKpiCoverage.setText(pct + "%");
    }

    private void applyFilter() {
        tableModel.setRowCount(0);
        String search = (txtSearch != null && txtSearch.getText() != null) ? txtSearch.getText().trim().toLowerCase() : "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        int count = 0;
        int rowIdx = 1;

        for (Customer c : loadedCustomers) {
            boolean hasPhone = c.getPhone() != null && !c.getPhone().trim().isEmpty();
            boolean hasEmail = c.getEmail() != null && !c.getEmail().trim().isEmpty();

            // Contact filter
            if ("WITH_PHONE".equals(currentContactFilter) && !hasPhone) continue;
            if ("WITH_EMAIL".equals(currentContactFilter) && !hasEmail) continue;

            // Search filter
            if (!search.isEmpty()) {
                String name = c.getName() != null ? c.getName().toLowerCase() : "";
                String phone = c.getPhone() != null ? c.getPhone().toLowerCase() : "";
                String email = c.getEmail() != null ? c.getEmail().toLowerCase() : "";
                String addr = c.getAddress() != null ? c.getAddress().toLowerCase() : "";
                if (!name.contains(search) && !phone.contains(search) && !email.contains(search) && !addr.contains(search)) {
                    continue;
                }
            }

            String regDate = c.getCreatedAt() != null ? sdf.format(c.getCreatedAt()) : "—";
            tableModel.addRow(new Object[]{
                    rowIdx++,
                    c.getName() + "###" + c.getId(),
                    c.getPhone() != null ? c.getPhone() : "",
                    c.getEmail() != null ? c.getEmail() : "",
                    c.getAddress() != null ? c.getAddress() : "",
                    regDate,
                    c.getId()
            });
            count++;
        }

        if (lblResultCount != null) {
            lblResultCount.setText(count + (count == 1 ? " customer" : " customers"));
        }

        if (table != null && table.getParent() != null) {
            table.getParent().repaint();
        }
    }

    private void populateFormFromSelectedRow() {
        int r = table.getSelectedRow();
        if (r < 0 || r >= tableModel.getRowCount()) return;

        Object idObj = tableModel.getValueAt(r, 6);
        selectedCustomerId = (idObj instanceof Integer) ? (Integer) idObj : Integer.parseInt(idObj.toString());

        Customer found = null;
        for (Customer c : loadedCustomers) {
            if (c.getId() == selectedCustomerId) {
                found = c;
                break;
            }
        }

        if (found != null) {
            txtName.setText(found.getName());
            txtPhone.setText(found.getPhone() != null ? found.getPhone() : "");
            txtEmail.setText(found.getEmail() != null ? found.getEmail() : "");
            txtAddress.setText(found.getAddress() != null ? found.getAddress() : "");

            // Fetch order stats from DB if available
            try {
                double[] stats = customerDAO.getCustomerStats(found.getId());
                int orders = (int) stats[0];
                double spent = stats[1];
                if (orders > 0) {
                    lblAvatarStats.setText(String.format("🛍️ %d Orders • 💰 %s Total Spent", orders, AppTheme.formatCurrency(spent)));
                } else {
                    lblAvatarStats.setText("🛍️ Registered client • Ready for checkout");
                }
            } catch (Exception ignored) {
                lblAvatarStats.setText("🛍️ Active customer profile");
            }
        }

        // Switch to Edit Mode UI
        lblFormTitle.setText("👤 Customer Profile");
        lblFormBadge.setText("#CUST-" + selectedCustomerId);
        lblFormBadge.setVisible(true);

        btnAdd.setVisible(false);
        editButtonsRow.setVisible(true);

        syncAvatarPreview();
    }

    public void clearForm() {
        selectedCustomerId = -1;
        txtName.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtAddress.setText("");

        lblFormTitle.setText("✨ Register Customer");
        lblFormBadge.setText("");
        lblFormBadge.setVisible(false);

        lblAvatarInitial.setText("👤");
        lblAvatarName.setText("Walk-in / New Customer");
        lblAvatarPhone.setText("No mobile number assigned");
        lblAvatarStats.setText("🛍️ Ready for new billing invoice");

        btnAdd.setVisible(true);
        editButtonsRow.setVisible(false);

        syncAvatarPreview();
        if (table != null) table.clearSelection();
    }

    private void addCustomer() {
        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();
        String addr = txtAddress.getText().trim();

        if (name.isEmpty()) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Customer full name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            }
            txtName.requestFocus();
            return;
        }

        try {
            Customer c = new Customer(0, name, phone, email, addr);
            if (customerDAO.addCustomer(c)) {
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(this, "Customer registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
                clearForm();
                loadCustomerTable();
            } else {
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(this, "Failed to register customer.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Error adding customer: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateCustomer() {
        if (selectedCustomerId <= 0) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Please select a customer from the directory table first.", "Warning", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Customer full name cannot be empty.", "Validation", JOptionPane.WARNING_MESSAGE);
            }
            txtName.requestFocus();
            return;
        }
        try {
            Customer c = new Customer(selectedCustomerId, name, txtPhone.getText().trim(), txtEmail.getText().trim(), txtAddress.getText().trim());
            if (customerDAO.updateCustomer(c)) {
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(this, "Customer profile updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
                clearForm();
                loadCustomerTable();
            } else {
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(this, "Failed to update customer profile.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception e) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Error updating customer: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteCustomer() {
        if (selectedCustomerId <= 0) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this, "Please select a customer from the table to delete.", "Warning", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        int confirm = JOptionPane.YES_OPTION;
        if (!GraphicsEnvironment.isHeadless()) {
            confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this customer profile?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        }
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                if (customerDAO.deleteCustomer(selectedCustomerId)) {
                    if (!GraphicsEnvironment.isHeadless()) {
                        JOptionPane.showMessageDialog(this, "Customer profile removed successfully!", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                    }
                    clearForm();
                    loadCustomerTable();
                } else {
                    if (!GraphicsEnvironment.isHeadless()) {
                        JOptionPane.showMessageDialog(this, "Could not delete customer.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (Exception e) {
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(this, "Delete failed (customer may have billing order history): " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
}
