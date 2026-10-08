package ui;

import dao.ReportDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ReportsPanel extends JPanel {
    private ReportDAO reportDAO = new ReportDAO();

    private JTable salesTable, profitTable;
    private DefaultTableModel salesModel, profitModel;
    private JTextField txtFromDate, txtToDate;
    private JButton btnFilterSales, btnRefreshAll;
    private DailySalesBarChartPanel reportBarChart;
    private CategoryPieChartPanel reportPieChart;

    public ReportsPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(AppTheme.BG_CANVAS);
        setBorder(new EmptyBorder(15, 15, 15, 15));
        initComponents();
        loadReports();
    }

    private void initComponents() {
        // TOP Header
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(AppTheme.BG_CANVAS);

        JLabel lblTitle = new JLabel("📈 Sales & Profit Analysis Reports");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(AppTheme.TEXT_PRIMARY);
        topPanel.add(lblTitle, BorderLayout.WEST);

        btnRefreshAll = new JButton("🔄 Refresh Data");
        topPanel.add(btnRefreshAll, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Tabbed Pane for Sales Report and Profit/Loss
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // TAB 1: Sales Invoices Report
        JPanel salesPanel = new JPanel(new BorderLayout(10, 10));
        salesPanel.setBackground(AppTheme.BG_CANVAS);

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        filterBar.setBackground(Color.WHITE);
        filterBar.setBorder(BorderFactory.createTitledBorder("Filter Invoices by Date (YYYY-MM-DD)"));

        filterBar.add(new JLabel("From Date:"));
        txtFromDate = new JTextField(10);
        filterBar.add(txtFromDate);

        filterBar.add(new JLabel("To Date:"));
        txtToDate = new JTextField(10);
        filterBar.add(txtToDate);

        btnFilterSales = new JButton("Filter");
        btnFilterSales.setBackground(new Color(37, 99, 235));
        btnFilterSales.setForeground(Color.WHITE);
        filterBar.add(btnFilterSales);

        JButton btnResetFilter = new JButton("Show All");
        filterBar.add(btnResetFilter);

        salesPanel.add(filterBar, BorderLayout.NORTH);

        String[] salesCols = {"Invoice No", "Date & Time", "Customer", "Subtotal", "GST", "Total Amount", "Mode"};
        salesModel = new DefaultTableModel(salesCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        salesTable = new JTable(salesModel);
        salesTable.setRowHeight(25);
        salesPanel.add(new JScrollPane(salesTable), BorderLayout.CENTER);

        tabs.addTab("🧾 Sales History & Invoices", salesPanel);

        // TAB 2: Profit & Loss per Product
        JPanel profitPanel = new JPanel(new BorderLayout());
        String[] profitCols = {"Product Code", "Product Name", "Units Sold", "Total Sales (₹)", "Cost of Goods (₹)", "Estimated Profit (₹)"};
        profitModel = new DefaultTableModel(profitCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        profitTable = new JTable(profitModel);
        profitTable.setRowHeight(25);
        profitPanel.add(new JScrollPane(profitTable), BorderLayout.CENTER);

        tabs.addTab("💰 Product Profit & Loss Analysis", profitPanel);

        // TAB 3: Visual Revenue Analytics
        JPanel analyticsPanel = new JPanel(new GridLayout(1, 2, 15, 15));
        analyticsPanel.setBackground(AppTheme.BG_CANVAS);
        analyticsPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        reportBarChart = new DailySalesBarChartPanel();
        reportPieChart = new CategoryPieChartPanel();

        analyticsPanel.add(reportBarChart);
        analyticsPanel.add(reportPieChart);

        tabs.addTab("📊 Revenue Visual Charts & Trends", analyticsPanel);

        add(tabs, BorderLayout.CENTER);

        // Actions
        btnFilterSales.addActionListener(e -> loadSalesReport());
        btnResetFilter.addActionListener(e -> {
            txtFromDate.setText("");
            txtToDate.setText("");
            loadSalesReport();
        });
        btnRefreshAll.addActionListener(e -> loadReports());
    }

    public void loadReports() {
        loadSalesReport();
        loadProfitReport();
        try {
            if (reportBarChart != null) reportBarChart.setData(reportDAO.getDailySalesTrend(7));
            if (reportPieChart != null) reportPieChart.setData(reportDAO.getCategorySalesBreakdown());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadSalesReport() {
        try {
            salesModel.setRowCount(0);
            List<String[]> list = reportDAO.getSalesReport(txtFromDate.getText().trim(), txtToDate.getText().trim());
            for (String[] row : list) {
                salesModel.addRow(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadProfitReport() {
        try {
            profitModel.setRowCount(0);
            List<String[]> list = reportDAO.getProfitLossReport();
            for (String[] row : list) {
                profitModel.addRow(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

