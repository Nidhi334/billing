package ui;

import dao.CategoryDAO;
import dao.ProductDAO;
import dao.ReportDAO;
import dao.SupplierDAO;
import model.Category;
import model.Product;
import model.Supplier;
import util.ProductImageUtil;

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
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified Modern Products & Stock Management Workspace.
 * Adheres strictly to the Keyra Minimalist Design System:
 * - Minimal, borderless header matching Dashboard style
 * - SVG vector icons for tabs, actions, and status pills (zero emoji clutter)
 * - Single unified workspace for both product catalog management and stock levels
 * - Product image thumbnails displayed directly in each row
 * - Dual inline action buttons per row: [Edit] and [Restock]
 * - Smooth modal dialogs for editing product catalog details and inward restocking
 * - Ambient drop shadow card with arc = 36 matching Dashboard
 */
public class InventoryPanel extends JPanel {

    // DAOs
    private final ProductDAO productDAO = new ProductDAO();
    private final SupplierDAO supplierDAO = new SupplierDAO();
    private final ReportDAO reportDAO = new ReportDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    // Data Caches
    private List<Product> allProducts = new ArrayList<>();
    private List<Product> lowStockProducts = new ArrayList<>();
    private List<Supplier> allSuppliers = new ArrayList<>();
    private List<String[]> stockHistoryList = new ArrayList<>();

    // State
    private String currentTab = "LEDGER"; // Default view: "LEDGER" (All Products & Stock), "ALERTS", "HISTORY"
    private String ledgerFilter = "ALL";  // "ALL", "HEALTHY", "LOW", "OUT"
    private String alertsFilter = "ALL";  // "ALL", "OUT_OF_STOCK", "LOW_STOCK"
    private String historyFilter = "ALL"; // "ALL", "IN", "OUT"
    private String searchQuery = "";
    private JTable hoveredActionTable;
    private int hoveredActionRow = -1;
    private int hoveredAction;

    // Segmented Tab Buttons (Vector SVG icons)
    private JButton btnTabLedger;
    private JButton btnTabAlerts;
    private JButton btnTabHistory;

    // Table Cards & Models
    private CardLayout tableCardLayout;
    private JPanel tableCardContainer;

    private JTable ledgerTable;
    private DefaultTableModel ledgerModel;

    private JTable alertsTable;
    private DefaultTableModel alertsModel;

    private JTable historyTable;
    private DefaultTableModel historyModel;

    // Search & Filter Components
    private ModernSearchField txtSearch;
    private JPanel filterChipsPanel;
    private JLabel lblRecordCount;

    public InventoryPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(AppTheme.BG_CANVAS);
        setBorder(new EmptyBorder(14, 18, 16, 18));

        initComponents();
        loadDropdownData();
        refreshData();
    }

    private void initComponents() {
        // =========================================================================
        // TOP SECTION: Minimal Header (Dashboard style, borderless & clean)
        // =========================================================================
        JPanel headerPanel = new JPanel(new BorderLayout(16, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(4, 4, 16, 4));

        JLabel lblTitle = new JLabel("Products & Stock");
        lblTitle.setFont(AppTheme.font(Font.BOLD, 24));
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        headerPanel.add(lblTitle, BorderLayout.WEST);

        // Header Right: Quick Categories Button, "+ Add Product / Stock" CTA & Smooth Refresh
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerRight.setOpaque(false);

        JButton btnCategories = createSecondaryActionButton("Categories", "tag");
        btnCategories.addActionListener(e -> {
            Window win = SwingUtilities.getWindowAncestor(this);
            Frame frame = (win instanceof Frame) ? (Frame) win : null;
            CategoryDialog dlg = new CategoryDialog(frame);
            dlg.setVisible(true);
            loadDropdownData();
            refreshData();
        });

        JButton btnAddStock = createPrimaryActionButton("Add Product / Stock", "plus");
        btnAddStock.addActionListener(e -> openAddStockDialog(null));

        JButton btnRefresh = createCircularRefreshButton();

        headerRight.add(btnCategories);
        headerRight.add(btnAddStock);
        headerRight.add(btnRefresh);

        headerPanel.add(headerRight, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // =========================================================================
        // CENTER: FULL-WIDTH MULTI-VIEW WORKSPACE CARD (Uncluttered, Spacious)
        // Matching Dashboard card styling: borderless, subtle drop shadow, arc = 36
        // =========================================================================
        JPanel tableCard = createElevatedCard(36);
        tableCard.setLayout(new BorderLayout(0, 10));
        tableCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        // Top Toolbar: Segmented Tabs + Search & Filters
        JPanel tableHeaderBox = new JPanel();
        tableHeaderBox.setLayout(new BoxLayout(tableHeaderBox, BoxLayout.Y_AXIS));
        tableHeaderBox.setOpaque(false);

        // Row 1: Segmented Pill Tab Switcher (SVG Icons, Zero Emojis)
        JPanel tabSwitcherRow = new JPanel(new BorderLayout());
        tabSwitcherRow.setOpaque(false);

        JPanel tabPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabPills.setOpaque(false);

        btnTabLedger = createSvgTabButton("All Products & Stock", "products", "LEDGER");
        btnTabAlerts = createSvgTabButton("Stock Alerts", "alert_triangle", "ALERTS");
        btnTabHistory = createSvgTabButton("Movement History", "bill_history", "HISTORY");

        tabPills.add(btnTabLedger);
        tabPills.add(btnTabAlerts);
        tabPills.add(btnTabHistory);
        tabSwitcherRow.add(tabPills, BorderLayout.WEST);

        lblRecordCount = new JLabel("0 products");
        lblRecordCount.setFont(AppTheme.font(Font.BOLD, 12));
        lblRecordCount.setForeground(AppTheme.TEXT_MUTED);
        lblRecordCount.setBorder(new EmptyBorder(4, 6, 4, 6));
        tabSwitcherRow.add(lblRecordCount, BorderLayout.EAST);

        tableHeaderBox.add(tabSwitcherRow);
        tableHeaderBox.add(Box.createVerticalStrut(12));

        // Row 2: Search Bar + Filter Chips
        JPanel toolbarRow = new JPanel(new BorderLayout(12, 0));
        toolbarRow.setOpaque(false);

        txtSearch = new ModernSearchField("Search products by name, code, category, or barcode...");
        txtSearch.setPreferredSize(new Dimension(380, 34));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { searchQuery = txtSearch.getText().trim(); applyActiveFilters(); }
            public void removeUpdate(DocumentEvent e) { searchQuery = txtSearch.getText().trim(); applyActiveFilters(); }
            public void changedUpdate(DocumentEvent e) { searchQuery = txtSearch.getText().trim(); applyActiveFilters(); }
        });
        toolbarRow.add(txtSearch, BorderLayout.WEST);

        filterChipsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        filterChipsPanel.setOpaque(false);
        toolbarRow.add(filterChipsPanel, BorderLayout.EAST);

        tableHeaderBox.add(toolbarRow);
        tableCard.add(tableHeaderBox, BorderLayout.NORTH);

        // Center: CardLayout containing 3 Views
        tableCardLayout = new CardLayout();
        tableCardContainer = new JPanel(tableCardLayout);
        tableCardContainer.setOpaque(false);

        // View 1: Complete Products Catalog & Stock Ledger Table (Default)
        initLedgerTable();
        tableCardContainer.add(createTableScrollPane(ledgerTable, "ledger"), "LEDGER");

        // View 2: Stock Alerts Table
        initAlertsTable();
        tableCardContainer.add(createTableScrollPane(alertsTable, "alerts"), "ALERTS");

        // View 3: Stock Movement Audit History Table
        initHistoryTable();
        tableCardContainer.add(createTableScrollPane(historyTable, "history"), "HISTORY");

        tableCard.add(tableCardContainer, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        // Default Filter Chips
        rebuildFilterChips();
    }

    // =========================================================================
    // MODAL DIALOG TRIGGERS
    // =========================================================================
    public void openAddStockDialog(Product preselected) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        AddStockDialog dialog = new AddStockDialog(parent, preselected, () -> {
            loadDropdownData();
            refreshData();
        });
        dialog.setVisible(true);
    }

    public void openEditProductDialog(Product product) {
        if (product == null) return;
        Window parent = SwingUtilities.getWindowAncestor(this);
        EditProductDialog dialog = new EditProductDialog(parent, product, () -> {
            loadDropdownData();
            refreshData();
        });
        dialog.setVisible(true);
    }

    // =========================================================================
    // TABLES SETUP & CLEAN RENDERING (Product Thumbnails + Dual Action Buttons)
    // =========================================================================
    private void initLedgerTable() {
        String[] cols = {"#", "Product Details", "Barcode", "Cost Price", "Sell Price", "Stock & Status", "Actions"};
        ledgerModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        ledgerTable = new JTable(ledgerModel);
        setupTableStyle(ledgerTable, 56);

        ledgerTable.getColumnModel().getColumn(0).setPreferredWidth(45);  // #
        ledgerTable.getColumnModel().getColumn(1).setPreferredWidth(320); // Product Details (Thumbnail 4:3 + Info)
        ledgerTable.getColumnModel().getColumn(2).setPreferredWidth(130); // Barcode Button
        ledgerTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Cost Price
        ledgerTable.getColumnModel().getColumn(4).setPreferredWidth(100); // Sell Price
        ledgerTable.getColumnModel().getColumn(5).setPreferredWidth(130); // Stock & Status
        ledgerTable.getColumnModel().getColumn(6).setPreferredWidth(155); // Actions ([Edit] [Restock])

        // Column 0: Center index
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        centerRender.setFont(AppTheme.font(Font.BOLD, 12));
        ledgerTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        // Column 1: Product Thumbnail & Details
        ledgerTable.getColumnModel().getColumn(1).setCellRenderer(new ProductDetailsCellRenderer());

        // Column 2: Dedicated Barcode Pill Button
        ledgerTable.getColumnModel().getColumn(2).setCellRenderer(new BarcodeCellRenderer());

        // Column 3 & 4: Currency Alignments
        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
        rightRender.setFont(AppTheme.font(Font.PLAIN, 12));
        rightRender.setBorder(new EmptyBorder(0, 4, 0, 14));
        ledgerTable.getColumnModel().getColumn(3).setCellRenderer(rightRender);
        ledgerTable.getColumnModel().getColumn(4).setCellRenderer(rightRender);

        // Column 5: Merged Stock Count & Status Pill
        ledgerTable.getColumnModel().getColumn(5).setCellRenderer(new StockStatusCellRenderer());

        // Column 6: Dual Action Buttons ([Edit] and [Restock])
        ledgerTable.getColumnModel().getColumn(6).setCellRenderer(new DualActionCellRenderer());

        setupTableActionMouseListener(ledgerTable, 6, 1, 2);
    }

    private void initAlertsTable() {
        String[] cols = {"#", "Product Details", "Barcode", "Stock & Status", "Min Alert Level", "Unit Sell Price", "Actions"};
        alertsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        alertsTable = new JTable(alertsModel);
        setupTableStyle(alertsTable, 56);

        alertsTable.getColumnModel().getColumn(0).setPreferredWidth(45);  // #
        alertsTable.getColumnModel().getColumn(1).setPreferredWidth(320); // Product Details (Thumbnail 4:3 + Info)
        alertsTable.getColumnModel().getColumn(2).setPreferredWidth(130); // Barcode
        alertsTable.getColumnModel().getColumn(3).setPreferredWidth(130); // Stock & Status
        alertsTable.getColumnModel().getColumn(4).setPreferredWidth(105); // Min Alert
        alertsTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Unit Sell Price
        alertsTable.getColumnModel().getColumn(6).setPreferredWidth(155); // Actions ([Edit] [Restock])

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        centerRender.setFont(AppTheme.font(Font.BOLD, 12));
        alertsTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);

        // Column 1: Product Thumbnail & Details
        alertsTable.getColumnModel().getColumn(1).setCellRenderer(new ProductDetailsCellRenderer());

        // Column 2: Barcode Pill Button
        alertsTable.getColumnModel().getColumn(2).setCellRenderer(new BarcodeCellRenderer());

        // Column 3: Merged Stock Count & Status Pill
        alertsTable.getColumnModel().getColumn(3).setCellRenderer(new StockStatusCellRenderer());

        // Column 4: Min Alert Count
        alertsTable.getColumnModel().getColumn(4).setCellRenderer(centerRender);

        // Column 5: Currency
        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(SwingConstants.RIGHT);
        rightRender.setFont(AppTheme.font(Font.PLAIN, 12));
        rightRender.setBorder(new EmptyBorder(0, 4, 0, 14));
        alertsTable.getColumnModel().getColumn(5).setCellRenderer(rightRender);

        // Column 6: Dual Action Buttons
        alertsTable.getColumnModel().getColumn(6).setCellRenderer(new DualActionCellRenderer());

        setupTableActionMouseListener(alertsTable, 6, 1, 2);
    }

    private void initHistoryTable() {
        String[] cols = {"#", "Date & Time", "Product Details", "Movement", "Quantity", "Ref / Invoice", "Audit Notes"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        historyTable = new JTable(historyModel);
        setupTableStyle(historyTable, 44);

        historyTable.getColumnModel().getColumn(0).setPreferredWidth(45);  // #
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Date
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(260); // Product
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(120); // Movement
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(90);  // Qty
        historyTable.getColumnModel().getColumn(5).setPreferredWidth(150); // Ref
        historyTable.getColumnModel().getColumn(6).setPreferredWidth(200); // Notes

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        historyTable.getColumnModel().getColumn(0).setCellRenderer(centerRender);
        historyTable.getColumnModel().getColumn(1).setCellRenderer(centerRender);
        historyTable.getColumnModel().getColumn(4).setCellRenderer(centerRender);

        // Column 3: Movement Pill (IN vs OUT)
        historyTable.getColumnModel().getColumn(3).setCellRenderer(new MovementPillCellRenderer());

        // Column 5: Reference Monospaced
        historyTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, focus, r, c);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(new Font("Monospaced", Font.BOLD, 11));
                l.setForeground(AppTheme.TEXT_PRIMARY);
                l.setText(String.valueOf(val != null ? val : "—"));
                return l;
            }
        });
    }

    private void setupTableStyle(JTable t, int rowHeight) {
        t.setRowHeight(rowHeight);
        t.setFont(AppTheme.font(Font.PLAIN, 12));
        t.setShowVerticalLines(false);
        t.setGridColor(new Color(241, 246, 243));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setSelectionBackground(new Color(230, 248, 238));
        t.setSelectionForeground(AppTheme.TEXT_PRIMARY);

        JTableHeader header = t.getTableHeader();
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean focus, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, focus, r, c);
                l.setFont(AppTheme.font(Font.BOLD, 11));
                l.setForeground(AppTheme.TEXT_PRIMARY);
                l.setBackground(new Color(245, 250, 247));
                l.setOpaque(true);
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_SAGE),
                        new EmptyBorder(0, 10, 0, 10)
                ));
                if (c == 0 || c == 2 || c == tbl.getColumnCount() - 1 || (tbl == ledgerTable && c == 5) || (tbl == alertsTable && (c == 3 || c == 4))) {
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                } else if ((tbl == ledgerTable && (c == 3 || c == 4)) || (tbl == alertsTable && c == 5)) {
                    l.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return l;
            }
        });
    }

    private void setupTableActionMouseListener(JTable table, int actionColIndex, int productColIndex, int barcodeColIndex) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                updateActionHover(null, -1, 0);
                table.setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0) return;

                Object val = table.getValueAt(row, productColIndex);
                Product target = null;
                if (val instanceof Product) {
                    target = (Product) val;
                } else if (val != null) {
                    String str = val.toString();
                    for (Product p : allProducts) {
                        if (p.getCode().equalsIgnoreCase(str) || p.getName().equalsIgnoreCase(str)) {
                            target = p;
                            break;
                        }
                    }
                }
                if (target == null) return;

                // Click on dedicated Barcode column button
                if (col == barcodeColIndex) {
                    Window parent = SwingUtilities.getWindowAncestor(InventoryPanel.this);
                    Frame frame = (parent instanceof Frame) ? (Frame) parent : null;
                    BarcodeUtil.showBarcodeDialog(frame, target);
                    return;
                }

                if (col != actionColIndex) return;

                Rectangle cellRect = table.getCellRect(row, col, false);
                int relX = e.getX() - cellRect.x;
                int relY = e.getY() - cellRect.y;
                int cellW = cellRect.width;

                int editW = 58;
                int restockW = 76;
                int gap = 8;
                int totalW = editW + gap + restockW;
                int startX = (cellW - totalW) / 2;

                int editX1 = startX;
                int editX2 = startX + editW;
                int restockX1 = startX + editW + gap;
                int restockX2 = startX + totalW;

                int pillH = 26;
                int btnY = (table.getRowHeight() - pillH) / 2;

                if (relY >= btnY - 2 && relY <= btnY + pillH + 2) {
                    if (relX >= editX1 && relX <= editX2) {
                        openEditProductDialog(target);
                    } else if (relX >= restockX1 && relX <= restockX2) {
                        openAddStockDialog(target);
                    }
                }
            }
        });

        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                int action = getActionAt(table, e.getPoint(), actionColIndex);
                updateActionHover(table, row, action);

                // Hover over dedicated Barcode button column
                if (row >= 0 && col == barcodeColIndex) {
                    table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    table.setToolTipText("View, print or download barcode");
                    return;
                }

                if (row >= 0 && col == actionColIndex) {
                    if (action != 0) {
                        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        return;
                    }
                }

                table.setToolTipText(null);
                table.setCursor(Cursor.getDefaultCursor());
            }
        });
    }

    private int getActionAt(JTable table, Point point, int actionColIndex) {
        int row = table.rowAtPoint(point);
        if (row < 0 || table.columnAtPoint(point) != actionColIndex) return 0;

        Rectangle cellRect = table.getCellRect(row, actionColIndex, false);
        int relX = point.x - cellRect.x;
        int relY = point.y - cellRect.y;
        int editW = 58;
        int restockW = 76;
        int gap = 8;
        int startX = (cellRect.width - editW - gap - restockW) / 2;
        int pillY = (table.getRowHeight() - 26) / 2;
        if (relY < pillY || relY >= pillY + 26) return 0;
        if (relX >= startX && relX < startX + editW) return 1;
        if (relX >= startX + editW + gap && relX < startX + editW + gap + restockW) return 2;
        return 0;
    }

    private void updateActionHover(JTable table, int row, int action) {
        if (action == 0) {
            table = null;
            row = -1;
        }
        if (hoveredActionTable == table && hoveredActionRow == row && hoveredAction == action) return;

        if (hoveredActionTable != null && hoveredActionRow >= 0) {
            hoveredActionTable.repaint(hoveredActionTable.getCellRect(hoveredActionRow, 6, false));
        }
        hoveredActionTable = table;
        hoveredActionRow = row;
        hoveredAction = action;
        if (hoveredActionTable != null && hoveredActionRow >= 0) {
            hoveredActionTable.repaint(hoveredActionTable.getCellRect(hoveredActionRow, 6, false));
        }
    }

    private JScrollPane createTableScrollPane(JTable table, String type) {
        JScrollPane scroll = new JScrollPane();

        JViewport customViewport = new JViewport() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (table.getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth(), h = getHeight();
                    String msg;
                    if ("alerts".equals(type)) {
                        msg = "Zero low stock alerts. All warehouse inventory levels are healthy.";
                    } else if ("history".equals(type)) {
                        msg = "No inventory movements recorded in audit history.";
                    } else {
                        msg = "No catalog products found matching your search.";
                    }

                    g2.setFont(AppTheme.font(Font.BOLD, 13));
                    g2.setColor(AppTheme.TEXT_MUTED);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (w - fm.stringWidth(msg)) / 2;
                    int ty = h / 2;
                    g2.drawString(msg, tx, ty);
                    g2.dispose();
                }
            }
        };
        customViewport.setOpaque(false);
        customViewport.setView(table);
        scroll.setViewport(customViewport);
        scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
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
        });
        return scroll;
    }

    // =========================================================================
    // DATA LOADING & FILTERING
    // =========================================================================
    public void loadDropdownData() {
        try {
            List<Product> products = productDAO.getAllProducts();
            if (products != null) {
                this.allProducts = products;
            }
            List<Supplier> suppliers = supplierDAO.getAllSuppliers();
            if (suppliers != null) {
                this.allSuppliers = suppliers;
            }
        } catch (Exception e) {
            System.err.println("Notice: Could not load data: " + e.getMessage());
        }
    }

    public void refreshData() {
        try {
            allProducts = productDAO.getAllProducts();
        } catch (Exception e) {
            System.err.println("Notice: Could not load products: " + e.getMessage());
        }

        try {
            lowStockProducts = productDAO.getLowStockProducts();
        } catch (Exception e) {
            System.err.println("Notice: Could not load low stock: " + e.getMessage());
        }

        try {
            stockHistoryList = reportDAO.getStockHistory(150);
        } catch (Exception e) {
            System.err.println("Notice: Could not load stock history: " + e.getMessage());
        }

        updateCounts();
        applyActiveFilters();
    }

    private void updateCounts() {
        int totalSkus = allProducts != null ? allProducts.size() : 0;
        int lowStock = 0;
        int outOfStock = 0;

        if (allProducts != null) {
            for (Product p : allProducts) {
                int q = p.getQuantity();
                if (q <= 0) {
                    outOfStock++;
                } else if (q <= p.getMinStockLevel()) {
                    lowStock++;
                }
            }
        }

        // Update tab pill labels with clean counts
        if (btnTabLedger != null) {
            btnTabLedger.setText("All Products & Stock (" + totalSkus + ")");
        }
        if (btnTabAlerts != null) {
            btnTabAlerts.setText("Stock Alerts (" + (lowStock + outOfStock) + ")");
        }
        if (btnTabHistory != null) {
            int histCount = stockHistoryList != null ? stockHistoryList.size() : 0;
            btnTabHistory.setText("Movement History (" + histCount + ")");
        }
    }

    private void applyActiveFilters() {
        String q = searchQuery.toLowerCase();

        if ("LEDGER".equals(currentTab)) {
            ledgerModel.setRowCount(0);
            int idx = 1;
            for (Product p : allProducts) {
                boolean isOut = p.getQuantity() <= 0;
                boolean isLow = p.getQuantity() <= p.getMinStockLevel() && !isOut;

                if ("HEALTHY".equals(ledgerFilter) && (isLow || isOut)) continue;
                if ("LOW".equals(ledgerFilter) && !isLow) continue;
                if ("OUT".equals(ledgerFilter) && !isOut) continue;

                if (!q.isEmpty()) {
                    boolean match = p.getName().toLowerCase().contains(q)
                            || p.getCode().toLowerCase().contains(q)
                            || (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(q))
                            || (p.getBarcode() != null && p.getBarcode().toLowerCase().contains(q));
                    if (!match) continue;
                }

                ledgerModel.addRow(new Object[]{
                        idx++,
                        p, // Product object for ProductDetailsCellRenderer
                        p, // Product object for BarcodeCellRenderer
                        String.format("₹%.2f", p.getPurchasePrice()),
                        String.format("₹%.2f", p.getSellingPrice()),
                        p, // Product object for StockStatusCellRenderer (merged Stock & Status)
                        "" // DualActionCellRenderer paints [Edit] and [Restock]
                });
            }
            lblRecordCount.setText(ledgerModel.getRowCount() + " products");

        } else if ("ALERTS".equals(currentTab)) {
            alertsModel.setRowCount(0);
            int idx = 1;
            for (Product p : lowStockProducts) {
                boolean isOut = p.getQuantity() <= 0;
                if ("OUT_OF_STOCK".equals(alertsFilter) && !isOut) continue;
                if ("LOW_STOCK".equals(alertsFilter) && isOut) continue;

                if (!q.isEmpty()) {
                    boolean match = p.getName().toLowerCase().contains(q)
                            || p.getCode().toLowerCase().contains(q)
                            || (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(q))
                            || (p.getBarcode() != null && p.getBarcode().toLowerCase().contains(q));
                    if (!match) continue;
                }

                alertsModel.addRow(new Object[]{
                        idx++,
                        p, // Product object for ProductDetailsCellRenderer
                        p, // Product object for BarcodeCellRenderer
                        p, // Product object for StockStatusCellRenderer (merged Stock & Status)
                        p.getMinStockLevel() + " units",
                        String.format("₹%.2f", p.getSellingPrice()),
                        "" // DualActionCellRenderer paints [Edit] and [Restock]
                });
            }
            lblRecordCount.setText(alertsModel.getRowCount() + " alerts");

        } else if ("HISTORY".equals(currentTab)) {
            historyModel.setRowCount(0);
            int idx = 1;
            for (String[] row : stockHistoryList) {
                if (row.length < 7) continue;

                String type = row[3];
                if ("IN".equals(historyFilter) && !"IN".equalsIgnoreCase(type)) continue;
                if ("OUT".equals(historyFilter) && !"OUT".equalsIgnoreCase(type)) continue;

                if (!q.isEmpty()) {
                    boolean match = row[1].toLowerCase().contains(q)
                            || row[2].toLowerCase().contains(q)
                            || row[5].toLowerCase().contains(q)
                            || row[6].toLowerCase().contains(q);
                    if (!match) continue;
                }

                historyModel.addRow(new Object[]{
                        idx++,
                        row[0],
                        row[2] + " (" + row[1] + ")",
                        type,
                        row[4] + " units",
                        row[5],
                        row[6]
                });
            }
            lblRecordCount.setText(historyModel.getRowCount() + " records");
        }
    }

    private void switchTab(String tabKey) {
        this.currentTab = tabKey;
        tableCardLayout.show(tableCardContainer, tabKey);
        btnTabLedger.repaint();
        btnTabAlerts.repaint();
        btnTabHistory.repaint();
        rebuildFilterChips();
        applyActiveFilters();
    }

    private void rebuildFilterChips() {
        filterChipsPanel.removeAll();

        if ("LEDGER".equals(currentTab)) {
            filterChipsPanel.add(createFilterChip("All Items", "ALL", ledgerFilter, k -> { ledgerFilter = k; applyActiveFilters(); }));
            filterChipsPanel.add(createFilterChip("Healthy", "HEALTHY", ledgerFilter, k -> { ledgerFilter = k; applyActiveFilters(); }));
            filterChipsPanel.add(createFilterChip("Low Stock", "LOW", ledgerFilter, k -> { ledgerFilter = k; applyActiveFilters(); }));
            filterChipsPanel.add(createFilterChip("Out of Stock", "OUT", ledgerFilter, k -> { ledgerFilter = k; applyActiveFilters(); }));
        } else if ("ALERTS".equals(currentTab)) {
            filterChipsPanel.add(createFilterChip("All Alerts", "ALL", alertsFilter, k -> { alertsFilter = k; applyActiveFilters(); }));
            filterChipsPanel.add(createFilterChip("Out of Stock", "OUT_OF_STOCK", alertsFilter, k -> { alertsFilter = k; applyActiveFilters(); }));
            filterChipsPanel.add(createFilterChip("Low Stock", "LOW_STOCK", alertsFilter, k -> { alertsFilter = k; applyActiveFilters(); }));
        } else if ("HISTORY".equals(currentTab)) {
            filterChipsPanel.add(createFilterChip("All Movements", "ALL", historyFilter, k -> { historyFilter = k; applyActiveFilters(); }));
            filterChipsPanel.add(createFilterChip("Stock In (IN)", "IN", historyFilter, k -> { historyFilter = k; applyActiveFilters(); }));
            filterChipsPanel.add(createFilterChip("Stock Out (OUT)", "OUT", historyFilter, k -> { historyFilter = k; applyActiveFilters(); }));
        }

        filterChipsPanel.revalidate();
        filterChipsPanel.repaint();
    }

    // =========================================================================
    // UI BUILDER HELPERS
    // =========================================================================
    private JButton createPrimaryActionButton(String text, String iconName) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                if (iconName != null) {
                    setIcon(new SidebarIcon(iconName, 14, Color.WHITE));
                }
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(hovered ? AppTheme.FOREST_MID : AppTheme.FOREST_GREEN);
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setPreferredSize(new Dimension(175, 36));
        btn.setIconTextGap(8);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createSecondaryActionButton(String text, String iconKey) {
        JButton btn = new JButton(text) {
            private final SidebarIcon icon = (iconKey != null) ? new SidebarIcon(iconKey, 14, new Color(51, 65, 85)) : null;

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = h;

                g2.setColor(getModel().isRollover() ? new Color(241, 245, 249) : Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, r, r);

                g2.setColor(new Color(226, 232, 240));
                g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);

                int contentW = 0;
                if (icon != null) contentW += icon.getIconWidth() + 6;
                g2.setFont(AppTheme.font(Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                contentW += fm.stringWidth(getText());

                int curX = (w - contentW) / 2;
                if (icon != null) {
                    icon.paintIcon(this, g2, curX, (h - icon.getIconHeight()) / 2);
                    curX += icon.getIconWidth() + 6;
                }

                g2.setColor(new Color(51, 65, 85));
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), curX, ty);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(115, 36));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createCircularRefreshButton() {
        JButton btn = new JButton() {
            private boolean hovered = false;
            private double currentAngle = 0.0;
            private javax.swing.Timer animTimer = null;
            private final SidebarIcon refreshIcon = new SidebarIcon("refresh", 16, AppTheme.FOREST_GREEN);
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
                addActionListener(e -> {
                    startRotateAnimation();
                    loadDropdownData();
                    refreshData();
                });
            }

            private void startRotateAnimation() {
                if (animTimer != null && animTimer.isRunning()) return;
                final long startTime = System.currentTimeMillis();
                final long duration = 400;
                final double startA = currentAngle;

                animTimer = new javax.swing.Timer(16, evt -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed >= duration) {
                        currentAngle = startA + Math.PI;
                        animTimer.stop();
                        repaint();
                    } else {
                        double progress = (double) elapsed / duration;
                        currentAngle = startA + (Math.PI * (1.0 - Math.pow(1.0 - progress, 2)));
                        repaint();
                    }
                });
                animTimer.start();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                g2.setColor(hovered ? new Color(230, 248, 238) : Color.WHITE);
                g2.fillOval(0, 0, w - 1, h - 1);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawOval(0, 0, w - 1, h - 1);

                int cx = w / 2;
                int cy = h / 2;
                g2.rotate(currentAngle, cx, cy);
                int sz = refreshIcon.getIconWidth();
                refreshIcon.paintIcon(this, g2, cx - sz / 2, cy - sz / 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(36, 36));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Refresh inventory & products data");
        return btn;
    }

    private JPanel createElevatedCard(int arc) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // Subtle ambient drop shadow matching dashboard
                g2.setColor(new Color(0, 50, 30, 6));
                g2.fillRoundRect(1, 2, w - 2, h - 2, arc, arc);
                // Card surface - Pure crisp white, NO BORDER!
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        return card;
    }

    private JButton createSvgTabButton(String text, String iconName, String tabKey) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            private final SidebarIcon icon = new SidebarIcon(iconName, 14);
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
                addActionListener(e -> switchTab(tabKey));
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = h;
                boolean active = currentTab.equals(tabKey);

                if (active) {
                    g2.setColor(AppTheme.FOREST_GREEN);
                    g2.fillRoundRect(0, 0, w, h, r, r);
                    g2.setColor(Color.WHITE);
                    icon.setOverrideColor(Color.WHITE);
                } else {
                    g2.setColor(hovered ? new Color(240, 252, 244) : Color.WHITE);
                    g2.fillRoundRect(0, 0, w, h, r, r);
                    g2.setColor(AppTheme.BORDER_SAGE);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                    Color fg = hovered ? AppTheme.FOREST_GREEN : AppTheme.TEXT_SECONDARY;
                    g2.setColor(fg);
                    icon.setOverrideColor(fg);
                }

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int iconW = icon.getIconWidth();
                int gap = 8;
                int textW = fm.stringWidth(getText());
                int totalW = iconW + gap + textW;
                int startX = (w - totalW) / 2;

                // Draw SVG icon
                icon.paintIcon(this, g2, startX, (h - icon.getIconHeight()) / 2);

                // Draw Tab text
                int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), startX + iconW + gap, ty);
                g2.dispose();
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 11));
        btn.setPreferredSize(new Dimension(195, 32));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createFilterChip(String text, String chipKey, String activeKey, java.util.function.Consumer<String> onSelect) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = h;
                boolean active = chipKey.equals(activeKey);

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
        int strW = btn.getFontMetrics(btn.getFont()).stringWidth(text);
        btn.setPreferredSize(new Dimension(strW + 24, 28));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            onSelect.accept(chipKey);
            rebuildFilterChips();
        });
        return btn;
    }

    // =========================================================================
    // CUSTOM TABLE CELL RENDERERS (Product Image Thumbnail, Dual Actions, Pills)
    // =========================================================================

    /**
     * Renders a 4:3 aspect-ratio thumbnail of the product image with object-fit: cover,
     * decreased vertical spacing for larger image presence, and product details beside it.
     */
    private static class ProductDetailsCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
            return new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                    int w = getWidth(), h = getHeight();

                    if (sel) {
                        g2.setColor(new Color(230, 248, 238));
                        g2.fillRect(0, 0, w, h);
                    }

                    Product p = null;
                    if (val instanceof Product) {
                        p = (Product) val;
                    }

                    String name = (p != null && p.getName() != null) ? p.getName() : (val != null ? val.toString() : "—");
                    String code = (p != null && p.getCode() != null) ? p.getCode() : "";
                    String cat = (p != null && p.getCategoryName() != null) ? p.getCategoryName() : "General";

                    // 1. Draw 4:3 Aspect-Ratio Thumbnail (clean, uncropped contain scaling, no overlay or border)
                    int thumbH = Math.max(36, h - 8);  // 48px height when h = 56
                    int thumbW = (thumbH * 4) / 3;      // 64px width for exact 4:3 aspect ratio
                    int thumbX = 10;
                    int thumbY = (h - thumbH) / 2;

                    // Clip to rounded 4:3 card
                    RoundRectangle2D thumbClip = new RoundRectangle2D.Float(thumbX, thumbY, thumbW, thumbH, 8, 8);
                    Shape oldClip = g2.getClip();
                    g2.setClip(thumbClip);

                    // Pure white backdrop
                    g2.setColor(Color.WHITE);
                    g2.fillRect(thumbX, thumbY, thumbW, thumbH);

                    // Uncropped contain mode image rendering
                    java.awt.image.BufferedImage rawImg = (p != null) ? ProductImageUtil.getProductRawImage(p) : null;
                    if (rawImg != null && rawImg.getWidth() > 0 && rawImg.getHeight() > 0) {
                        int imgW = rawImg.getWidth();
                        int imgH = rawImg.getHeight();
                        double scale = Math.min((double) thumbW / imgW, (double) thumbH / imgH);
                        int drawW = Math.max(1, (int) Math.round(imgW * scale));
                        int drawH = Math.max(1, (int) Math.round(imgH * scale));
                        int drawX = thumbX + (thumbW - drawW) / 2;
                        int drawY = thumbY + (thumbH - drawH) / 2;
                        g2.drawImage(rawImg, drawX, drawY, drawW, drawH, null);
                    } else {
                        ImageIcon icon = (p != null) ? ProductImageUtil.getProductIcon(p, thumbW, thumbH)
                                                     : ProductImageUtil.getProductIcon(null, cat, name, thumbW, thumbH);
                        if (icon != null && icon.getImage() != null) {
                            int drawW = icon.getIconWidth();
                            int drawH = icon.getIconHeight();
                            int drawX = thumbX + (thumbW - drawW) / 2;
                            int drawY = thumbY + (thumbH - drawH) / 2;
                            g2.drawImage(icon.getImage(), drawX, drawY, drawW, drawH, null);
                        }
                    }
                    g2.setClip(oldClip);

                    // 2. Text layout beside thumbnail (vertically centered in row)
                    int textX = thumbX + thumbW + 12;
                    int midY = h / 2;
                    int nameY = midY - 2;
                    int subY = midY + 14;

                    // Line 1: Product Name
                    g2.setFont(AppTheme.font(Font.BOLD, 12));
                    g2.setColor(AppTheme.TEXT_PRIMARY);
                    FontMetrics fmName = g2.getFontMetrics();

                    int maxTextW = w - textX - 8;
                    String displayName = name;
                    if (fmName.stringWidth(displayName) > maxTextW && maxTextW > 20) {
                        while (displayName.length() > 3 && fmName.stringWidth(displayName + "...") > maxTextW) {
                            displayName = displayName.substring(0, displayName.length() - 1);
                        }
                        displayName = displayName + "...";
                    }
                    g2.drawString(displayName, textX, nameY);

                    // Line 2: Code • Category
                    g2.setFont(AppTheme.font(Font.PLAIN, 11));
                    g2.setColor(AppTheme.TEXT_MUTED);
                    String sub = code.isEmpty() ? cat : (code + "  •  " + cat);
                    g2.drawString(sub, textX, subY);

                    g2.dispose();
                }
            };
        }
    }

    /**
     * Dedicated Barcode Button Cell Renderer:
     * - Renders a sleek, clickable pill button in its own column
     * - Features vector barcode icon + monospaced barcode string
     */
    private static class BarcodeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
            Product p = null;
            if (val instanceof Product) {
                p = (Product) val;
            }
            final String bcode = (p != null && p.getBarcode() != null && !p.getBarcode().trim().isEmpty())
                    ? p.getBarcode().trim()
                    : (p != null && p.getCode() != null ? p.getCode() : (val != null ? val.toString() : "—"));

            return new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth(), h = getHeight();

                    if (sel) {
                        g2.setColor(new Color(230, 248, 238));
                        g2.fillRect(0, 0, w, h);
                    }

                    int pillH = 28;
                    int pillW = Math.min(w - 14, 116);
                    int px = (w - pillW) / 2;
                    int py = (h - pillH) / 2;

                    // Button surface: subtle modern slate/white pill
                    g2.setColor(new Color(248, 250, 252));
                    g2.fillRoundRect(px, py, pillW, pillH, 8, 8);
                    g2.setColor(new Color(226, 232, 240));
                    g2.drawRoundRect(px, py, pillW - 1, pillH - 1, 8, 8);

                    // Mini Barcode Vector Icon (distinct barcode bars)
                    int iconX = px + 9;
                    int iconY = py + 7;
                    int iconH = 14;
                    g2.setColor(new Color(15, 23, 42));
                    int[] bars = {1, 2, 1, 3, 1, 2, 1};
                    int curX = iconX;
                    for (int i = 0; i < bars.length; i++) {
                        g2.fillRect(curX, iconY, bars[i], iconH);
                        curX += bars[i] + 2;
                    }

                    // Barcode Text
                    g2.setFont(new Font("Monospaced", Font.BOLD, 10));
                    g2.setColor(new Color(30, 41, 59));
                    FontMetrics fm = g2.getFontMetrics();

                    String displayStr = bcode;
                    int textStartX = curX + 5;
                    int maxTextW = (px + pillW - 6) - textStartX;
                    if (fm.stringWidth(displayStr) > maxTextW && maxTextW > 10) {
                        while (displayStr.length() > 2 && fm.stringWidth(displayStr + "..") > maxTextW) {
                            displayStr = displayStr.substring(0, displayStr.length() - 1);
                        }
                        displayStr += "..";
                    }

                    int textY = py + ((pillH - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(displayStr, textStartX, textY);

                    g2.dispose();
                }
            };
        }
    }

    /**
     * Merged Stock Level & Status Cell:
     * - Line 1: Bold stock quantity ("X units")
     * - Line 2: Status pill badge (In Stock / Low Stock / Out of Stock)
     */
    private static class StockStatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
            Product p = null;
            if (val instanceof Product) {
                p = (Product) val;
            }

            final int qty = (p != null) ? p.getQuantity() : 0;
            final int minLevel = (p != null) ? p.getMinStockLevel() : 5;
            final boolean isOut = qty <= 0;
            final boolean isLow = !isOut && qty <= minLevel;

            final Color badgeBg;
            final Color badgeBorder;
            final Color dotColor;
            final Color statusFg;
            final String statusText;

            if (isOut) {
                badgeBg = new Color(254, 242, 242);
                badgeBorder = new Color(254, 202, 202);
                dotColor = new Color(220, 38, 38);
                statusFg = new Color(185, 28, 28);
                statusText = "Out of Stock";
            } else if (isLow) {
                badgeBg = new Color(255, 251, 235);
                badgeBorder = new Color(254, 226, 154);
                dotColor = new Color(217, 119, 6);
                statusFg = new Color(180, 83, 9);
                statusText = "Low Stock";
            } else {
                badgeBg = new Color(240, 253, 244);
                badgeBorder = new Color(187, 247, 208);
                dotColor = new Color(22, 163, 74);
                statusFg = new Color(21, 128, 61);
                statusText = "In Stock";
            }

            return new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth(), h = getHeight();

                    if (sel) {
                        g2.setColor(new Color(230, 248, 238));
                        g2.fillRect(0, 0, w, h);
                    }

                    // Line 1: Stock Quantity (Bold 12px)
                    g2.setFont(AppTheme.font(Font.BOLD, 12));
                    g2.setColor(isOut ? new Color(220, 38, 38) : AppTheme.TEXT_PRIMARY);
                    String qtyStr = qty + " units";
                    FontMetrics fmQty = g2.getFontMetrics();
                    int qtyX = (w - fmQty.stringWidth(qtyStr)) / 2;
                    int qtyY = (h / 2) - 4;
                    g2.drawString(qtyStr, qtyX, qtyY);

                    // Line 2: Status Pill Badge
                    int pillH = 18;
                    int pillW = 86;
                    int px = (w - pillW) / 2;
                    int py = (h / 2) + 4;

                    g2.setColor(badgeBg);
                    g2.fillRoundRect(px, py, pillW, pillH, pillH, pillH);
                    g2.setColor(badgeBorder);
                    g2.drawRoundRect(px, py, pillW - 1, pillH - 1, pillH, pillH);

                    // Indicator dot
                    int dotSz = 5;
                    int dotX = px + 8;
                    int dotY = py + (pillH - dotSz) / 2;
                    g2.setColor(dotColor);
                    g2.fillOval(dotX, dotY, dotSz, dotSz);

                    // Status text
                    g2.setFont(AppTheme.font(Font.BOLD, 9));
                    g2.setColor(statusFg);
                    FontMetrics fmStatus = g2.getFontMetrics();
                    int stX = dotX + dotSz + 5;
                    int stY = py + ((pillH - fmStatus.getHeight()) / 2) + fmStatus.getAscent();
                    g2.drawString(statusText, stX, stY);

                    g2.dispose();
                }
            };
        }
    }

    /**
     * Renders two clean, interactive action buttons in the row:
     * - [Edit] pill: subtle slate border & text with pencil vector
     * - [Restock] pill: mint-green background & forest green text with plus icon
     */
    private class DualActionCellRenderer extends DefaultTableCellRenderer {
        private final SidebarIcon plusIcon = new SidebarIcon("plus", 10, AppTheme.FOREST_GREEN);

        @Override
        public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
            return new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth(), h = getHeight();

                    if (sel) {
                        g2.setColor(new Color(230, 248, 238));
                        g2.fillRect(0, 0, w, h);
                    }

                    int editW = 58;
                    int restockW = 76;
                    int gap = 8;
                    int totalW = editW + gap + restockW;
                    int pillH = 26;
                    int py = (h - pillH) / 2;
                    int startX = (w - totalW) / 2;
                    boolean hoveringEdit = hoveredActionTable == t && hoveredActionRow == r && hoveredAction == 1;
                    boolean hoveringRestock = hoveredActionTable == t && hoveredActionRow == r && hoveredAction == 2;

                    // 1. "Edit" Button Pill
                    int editX = startX;
                    Color editBg = new Color(239, 246, 255);
                    Color editBorder = new Color(191, 219, 254);
                    Color editText = new Color(29, 78, 216);
                    g2.setColor(editBg);
                    g2.fillRoundRect(editX, py, editW, pillH, pillH, pillH);
                    g2.setColor(hoveringEdit ? editText : editBorder);
                    g2.drawRoundRect(editX, py, editW - 1, pillH - 1, pillH, pillH);

                    // Edit Icon (small pencil vector)
                    g2.setColor(editText);
                    g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int iconX = editX + 11;
                    int iconY = py + 7;
                    g2.drawLine(iconX, iconY + 8, iconX + 8, iconY);
                    g2.drawLine(iconX - 1, iconY + 11, iconX + 2, iconY + 11);

                    g2.setFont(AppTheme.font(Font.BOLD, 11));
                    g2.setColor(editText);
                    FontMetrics fm = g2.getFontMetrics();
                    int editTy = py + ((pillH - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString("Edit", editX + 24, editTy);

                    // 2. "Restock" Button Pill
                    int restockX = startX + editW + gap;
                    g2.setColor(hoveringRestock ? new Color(220, 244, 230) : new Color(240, 252, 244));
                    g2.fillRoundRect(restockX, py, restockW, pillH, pillH, pillH);
                    g2.setColor(hoveringRestock ? AppTheme.FOREST_MID : AppTheme.BORDER_SAGE);
                    g2.drawRoundRect(restockX, py, restockW - 1, pillH - 1, pillH, pillH);

                    int plusSz = plusIcon.getIconWidth();
                    plusIcon.setOverrideColor(hoveringRestock ? AppTheme.FOREST_DEEP : AppTheme.FOREST_GREEN);
                    plusIcon.paintIcon(this, g2, restockX + 8, py + (pillH - plusSz) / 2);

                    g2.setFont(AppTheme.font(Font.BOLD, 11));
                    g2.setColor(hoveringRestock ? AppTheme.FOREST_DEEP : AppTheme.FOREST_GREEN);
                    fm = g2.getFontMetrics();
                    int restockTy = py + ((pillH - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString("Restock", restockX + 8 + plusSz + 4, restockTy);

                    g2.dispose();
                }
            };
        }
    }

    private static class StatusPillCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
            String status = String.valueOf(val != null ? val : "");
            final Color bg;
            final Color border;
            final Color dotColor;
            final Color fg;
            final String text;

            if (status.contains("OUT")) {
                bg = new Color(254, 242, 242);
                border = new Color(254, 202, 202);
                dotColor = new Color(220, 38, 38);
                fg = new Color(185, 28, 28);
                text = "Out of Stock";
            } else if (status.contains("LOW")) {
                bg = new Color(255, 251, 235);
                border = new Color(254, 226, 154);
                dotColor = new Color(217, 119, 6);
                fg = new Color(180, 83, 9);
                text = "Low Stock";
            } else {
                bg = new Color(240, 253, 244);
                border = new Color(187, 247, 208);
                dotColor = new Color(22, 163, 74);
                fg = new Color(21, 128, 61);
                text = "In Stock";
            }

            return new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth(), h = getHeight();
                    int pillH = 24;
                    int pillW = 100;
                    int px = (w - pillW) / 2;
                    int py = (h - pillH) / 2;

                    // Background pill
                    g2.setColor(bg);
                    g2.fillRoundRect(px, py, pillW, pillH, pillH, pillH);
                    g2.setColor(border);
                    g2.drawRoundRect(px, py, pillW - 1, pillH - 1, pillH, pillH);

                    // Indicator dot
                    int dotSz = 6;
                    int dotX = px + 10;
                    int dotY = py + (pillH - dotSz) / 2;
                    g2.setColor(dotColor);
                    g2.fillOval(dotX, dotY, dotSz, dotSz);

                    // Text
                    g2.setFont(AppTheme.font(Font.BOLD, 10));
                    g2.setColor(fg);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = dotX + dotSz + 6;
                    int ty = py + ((pillH - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(text, tx, ty);

                    g2.dispose();
                }
            };
        }
    }

    private static class MovementPillCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean focus, int r, int c) {
            String move = String.valueOf(val != null ? val : "");
            final boolean isIn = "IN".equalsIgnoreCase(move) || move.contains("IN");
            final Color bg = isIn ? new Color(240, 253, 244) : new Color(248, 250, 252);
            final Color border = isIn ? new Color(187, 247, 208) : new Color(226, 232, 240);
            final Color fg = isIn ? AppTheme.FOREST_GREEN : AppTheme.TEXT_SECONDARY;
            final String text = isIn ? "Stock In" : "Stock Out";

            return new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    int w = getWidth(), h = getHeight();
                    int pillH = 22;
                    int pillW = 80;
                    int px = (w - pillW) / 2;
                    int py = (h - pillH) / 2;

                    g2.setColor(bg);
                    g2.fillRoundRect(px, py, pillW, pillH, pillH, pillH);
                    g2.setColor(border);
                    g2.drawRoundRect(px, py, pillW - 1, pillH - 1, pillH, pillH);

                    g2.setFont(AppTheme.font(Font.BOLD, 10));
                    g2.setColor(fg);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = px + (pillW - fm.stringWidth(text)) / 2;
                    int ty = py + ((pillH - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(text, tx, ty);
                    g2.dispose();
                }
            };
        }
    }

    // =========================================================================
    // MODERN SEARCH FIELD (SVG Vector Magnifying Glass, Crisp Round Pill)
    // =========================================================================
    private static class ModernSearchField extends JTextField {
        private final String placeholder;
        private boolean focused = false;

        public ModernSearchField(String placeholder) {
            this.placeholder = placeholder;
            setFont(AppTheme.font(Font.PLAIN, 12));
            setForeground(AppTheme.TEXT_PRIMARY);
            setOpaque(false);
            setBorder(new EmptyBorder(6, 32, 6, 12));

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

            // Search icon via Java2D vector
            g2.setColor(focused ? AppTheme.FOREST_GREEN : AppTheme.TEXT_MUTED);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cy = h / 2 - 1;
            g2.drawOval(11, cy - 4, 8, 8);
            g2.drawLine(17, cy + 3, 22, cy + 8);

            super.paintComponent(g);

            if (getText().isEmpty() && !focused && placeholder != null) {
                g2.setColor(AppTheme.TEXT_MUTED);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(placeholder, 34, ty);
            }
            g2.dispose();
        }
    }
}
