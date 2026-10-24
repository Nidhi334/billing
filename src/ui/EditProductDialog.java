package ui;

import dao.CategoryDAO;
import dao.ProductDAO;
import model.Category;
import model.Product;
import util.ProductImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Modern, Minimalist Modal Dialog for Editing an Existing Catalog Product.
 * Matches Keyra Design System:
 * - Prominent product preview card at the top with large image and quick image actions
 * - Clean two-column inputs with subtle sage borders and modern focus rings
 * - Anti-aliased pill action buttons
 * - "Delete Item" compact button instead of "Delete Product"
 */
public class EditProductDialog extends JDialog {

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    private final Product product;
    private final Runnable onProductUpdated;

    private JTextField txtName;
    private JTextField txtCode;
    private JTextField txtBarcode;
    private JComboBox<Category> cmbCategory;
    private JTextField txtSellingPrice;
    private JTextField txtPurchasePrice;
    private JTextField txtMinStock;

    private String selectedImagePath;
    private JLabel lblImagePreview;
    private JButton btnChangeImage;

    public EditProductDialog(Window owner, Product product, Runnable onProductUpdated) {
        super(owner, "Edit Product - " + (product != null ? product.getName() : ""), ModalityType.APPLICATION_MODAL);
        this.product = product;
        this.onProductUpdated = onProductUpdated;
        this.selectedImagePath = product != null ? product.getImagePath() : null;

        setSize(540, 520);
        setMinimumSize(new Dimension(500, 480));
        setResizable(false);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());
        getContentPane().setBackground(AppTheme.BG_CANVAS);

        initComponents();
        loadCategories();
        populateFields();
        setupShortcuts();
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BG_CANVAS);

        // =========================================================================
        // 1. FORM BODY (Top Image Hero Card + Clean Form Inputs)
        // =========================================================================
        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setBackground(AppTheme.BG_CANVAS);
        bodyPanel.setBorder(new EmptyBorder(16, 20, 10, 20));

        // 1.1 PROMINENT TOP IMAGE HERO CARD (Clean: Preview + "Change Image" / "Add Image" button only)
        JPanel imageHeroCard = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, w, h, 14, 14);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                g2.dispose();
            }
        };
        imageHeroCard.setOpaque(false);
        imageHeroCard.setBorder(new EmptyBorder(10, 14, 10, 14));
        imageHeroCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        imageHeroCard.setPreferredSize(new Dimension(0, 90));

        // Image Preview Display (88x68 px)
        lblImagePreview = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(new Color(245, 250, 247));
                g2.fillRoundRect(0, 0, w, h, 12, 12);
                g2.setColor(AppTheme.BORDER_SAGE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lblImagePreview.setPreferredSize(new Dimension(88, 68));
        lblImagePreview.setMinimumSize(new Dimension(88, 68));
        lblImagePreview.setHorizontalAlignment(SwingConstants.CENTER);
        imageHeroCard.add(lblImagePreview, BorderLayout.WEST);

        // Action button (Only "Change Image" or "Add Image", vertically centered, zero clutter)
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnWrap.setOpaque(false);
        boolean hasInitialImg = (selectedImagePath != null && !selectedImagePath.isEmpty())
                || (product != null && product.getImagePath() != null && !product.getImagePath().trim().isEmpty());
        btnChangeImage = createPillButton(
                hasInitialImg ? "Change Image" : "Add Image",
                "image", Color.WHITE, AppTheme.BORDER_SAGE, AppTheme.FOREST_GREEN);
        btnChangeImage.setPreferredSize(new Dimension(135, 34));
        btnChangeImage.setFont(AppTheme.font(Font.BOLD, 12));
        btnChangeImage.addActionListener(e -> chooseProductImage());
        btnWrap.add(btnChangeImage);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(btnWrap);
        imageHeroCard.add(centerPanel, BorderLayout.CENTER);

        bodyPanel.add(imageHeroCard);
        bodyPanel.add(Box.createVerticalStrut(12));

        // 1.2 FORM FIELDS CARD
        // Row 1: Product Name
        JPanel pName = createFieldGroup("Product Name *", txtName = createInputField("e.g. Boat Rockerz 450 Bluetooth Headphones"));
        bodyPanel.add(pName);
        bodyPanel.add(Box.createVerticalStrut(8));

        // Row 2: SKU Code & Barcode
        JPanel rowCode = new JPanel(new GridLayout(1, 2, 12, 0));
        rowCode.setOpaque(false);
        rowCode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        rowCode.add(createFieldGroup("SKU Code *", txtCode = createInputField("e.g. PRD-005")));
        rowCode.add(createFieldGroup("Barcode (EAN / Code128)", txtBarcode = createInputField("e.g. 8901030584910")));
        bodyPanel.add(rowCode);
        bodyPanel.add(Box.createVerticalStrut(8));

        // Row 3: Category & Low Stock Alert Level
        JPanel rowCat = new JPanel(new GridLayout(1, 2, 12, 0));
        rowCat.setOpaque(false);
        rowCat.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        cmbCategory = new JComboBox<>();
        styleComboBox(cmbCategory);
        rowCat.add(createFieldGroup("Category *", cmbCategory));
        rowCat.add(createFieldGroup("Low Stock Alert Level", txtMinStock = createInputField("e.g. 5")));
        bodyPanel.add(rowCat);
        bodyPanel.add(Box.createVerticalStrut(8));

        // Row 4: Selling Price (MRP) & Purchase Cost Price
        JPanel rowPrices = new JPanel(new GridLayout(1, 2, 12, 0));
        rowPrices.setOpaque(false);
        rowPrices.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        rowPrices.add(createFieldGroup("Retail Selling Price (₹) *", txtSellingPrice = createInputField("e.g. 1499.00")));
        rowPrices.add(createFieldGroup("Unit Cost / Purchase Price (₹) *", txtPurchasePrice = createInputField("e.g. 850.00")));
        bodyPanel.add(rowPrices);

        root.add(bodyPanel, BorderLayout.CENTER);

        // =========================================================================
        // 2. FOOTER (Delete & Barcode on Left, Cancel & Save on Right)
        // =========================================================================
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(AppTheme.BG_CANVAS);
        footer.setBorder(new EmptyBorder(10, 20, 16, 20));

        // Left Actions: Delete Item + Barcode Print Button
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftButtons.setOpaque(false);

        JButton btnDelete = createPillButton("Delete Item", "trash", new Color(254, 242, 242), new Color(254, 202, 202), new Color(220, 38, 38));
        btnDelete.setPreferredSize(new Dimension(110, 36));
        btnDelete.addActionListener(e -> handleDeleteProduct());
        leftButtons.add(btnDelete);

        JButton btnPrintBarcode = createPillButton("Barcode", "barcode", Color.WHITE, AppTheme.BORDER_SAGE, AppTheme.FOREST_GREEN);
        btnPrintBarcode.setPreferredSize(new Dimension(95, 36));
        btnPrintBarcode.setFont(AppTheme.font(Font.BOLD, 11));
        btnPrintBarcode.addActionListener(e -> printBarcodeLabel());
        leftButtons.add(btnPrintBarcode);

        footer.add(leftButtons, BorderLayout.WEST);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightButtons.setOpaque(false);

        JButton btnCancel = createPillButton("Cancel", null, Color.WHITE, AppTheme.BORDER_SAGE, new Color(71, 85, 105));
        btnCancel.setPreferredSize(new Dimension(85, 36));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = createPillButton("Save Changes", "check", AppTheme.FOREST_GREEN, null, Color.WHITE);
        btnSave.setPreferredSize(new Dimension(140, 36));
        btnSave.addActionListener(e -> handleSaveProduct());

        rightButtons.add(btnCancel);
        rightButtons.add(btnSave);
        footer.add(rightButtons, BorderLayout.EAST);

        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel createFieldGroup(String labelText, JComponent field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(AppTheme.font(Font.BOLD, 11));
        lbl.setForeground(new Color(51, 65, 85));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(lbl);
        p.add(Box.createVerticalStrut(4));
        p.add(field);
        return p;
    }

    private void loadCategories() {
        try {
            List<Category> list = categoryDAO.getAllCategories();
            cmbCategory.removeAllItems();
            for (Category c : list) {
                cmbCategory.addItem(c);
            }
        } catch (Exception ignored) {}
    }

    private void populateFields() {
        if (product == null) return;
        txtName.setText(product.getName());
        txtCode.setText(product.getCode());
        txtBarcode.setText(product.getBarcode() != null ? product.getBarcode() : "");
        txtSellingPrice.setText(String.format("%.2f", product.getSellingPrice()));
        txtPurchasePrice.setText(String.format("%.2f", product.getPurchasePrice()));
        txtMinStock.setText(String.valueOf(product.getMinStockLevel()));

        if (product.getCategoryName() != null) {
            for (int i = 0; i < cmbCategory.getItemCount(); i++) {
                Category c = cmbCategory.getItemAt(i);
                if (c.getName().equalsIgnoreCase(product.getCategoryName())) {
                    cmbCategory.setSelectedIndex(i);
                    break;
                }
            }
        }
        updateImagePreview();
    }

    private void chooseProductImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Product Image");
        chooser.setFileFilter(new FileNameExtensionFilter("Image Files (*.jpg, *.png, *.webp, *.jpeg)", "jpg", "jpeg", "png", "webp"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            String code = txtCode.getText().trim();
            if (code.isEmpty()) {
                code = product != null ? product.getCode() : "PRD";
            }
            String savedPath = ProductImageUtil.saveProductImage(file, code);
            if (savedPath != null) {
                selectedImagePath = savedPath;
                updateImagePreview();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save product image.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateImagePreview() {
        int w = 84, h = 64;
        boolean hasImg = (selectedImagePath != null && !selectedImagePath.trim().isEmpty())
                || (product != null && ProductImageUtil.getProductRawImage(product) != null);
        if (btnChangeImage != null) {
            btnChangeImage.setText(hasImg ? "Change Image" : "Add Image");
        }

        if (selectedImagePath != null && !selectedImagePath.trim().isEmpty()) {
            String catName = cmbCategory != null && cmbCategory.getSelectedItem() != null ? cmbCategory.getSelectedItem().toString() : "";
            String prodName = txtName != null ? txtName.getText().trim() : "";
            lblImagePreview.setIcon(ProductImageUtil.getProductIcon(selectedImagePath, catName, prodName, w, h));
        } else if (product != null) {
            lblImagePreview.setIcon(ProductImageUtil.getProductIcon(product, w, h));
        } else {
            lblImagePreview.setIcon(null);
        }
    }

    private void printBarcodeLabel() {
        try {
            String bCode = txtBarcode.getText().trim();
            if (bCode.isEmpty() && product != null) bCode = product.getCode();
            BufferedImage img = BarcodeUtil.generateBarcodeImage(bCode, 300, 100, true);
            BarcodeUtil.printBarcodeLabel(product, img, this);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not generate barcode label: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSaveProduct() {
        String name = txtName.getText().trim();
        String code = txtCode.getText().trim();
        String barcode = txtBarcode.getText().trim();
        String sellStr = txtSellingPrice.getText().trim();
        String buyStr = txtPurchasePrice.getText().trim();
        String minStr = txtMinStock.getText().trim();

        if (name.isEmpty() || code.isEmpty() || sellStr.isEmpty() || buyStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter product name, code, selling price and purchase price.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double sellPrice, buyPrice;
        int minStock = 5;
        try {
            sellPrice = Double.parseDouble(sellStr);
            buyPrice = Double.parseDouble(buyStr);
            if (!minStr.isEmpty()) minStock = Integer.parseInt(minStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Prices and Alert level must be valid numbers.", "Validation", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (sellPrice < 0 || buyPrice < 0 || minStock < 0) {
            JOptionPane.showMessageDialog(this, "Prices and stock levels cannot be negative.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Category selCat = (Category) cmbCategory.getSelectedItem();
        int catId = selCat != null ? selCat.getId() : 1;
        String catName = selCat != null ? selCat.getName() : "General";

        Product updated = new Product();
        updated.setId(product.getId());
        updated.setName(name);
        updated.setCode(code);
        updated.setBarcode(barcode);
        updated.setCategoryId(catId);
        updated.setCategoryName(catName);
        updated.setSellingPrice(sellPrice);
        updated.setPurchasePrice(buyPrice);
        updated.setQuantity(product.getQuantity());
        updated.setMinStockLevel(minStock);
        updated.setImagePath(selectedImagePath);

        try {
            if (productDAO.updateProduct(updated)) {
                if (onProductUpdated != null) onProductUpdated.run();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update product details in database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteProduct() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete \"" + product.getName() + "\"?\nThis cannot be undone.",
                "Confirm Delete Item",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice == JOptionPane.YES_OPTION) {
            try {
                if (productDAO.deleteProduct(product.getId())) {
                    if (onProductUpdated != null) onProductUpdated.run();
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Could not delete product.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void setupShortcuts() {
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private JTextField createInputField(String placeholder) {
        JTextField tf = new JTextField() {
            private boolean focused = false;
            {
                addFocusListener(new java.awt.event.FocusAdapter() {
                    @Override public void focusGained(java.awt.event.FocusEvent e) { focused = true; repaint(); }
                    @Override public void focusLost(java.awt.event.FocusEvent e) { focused = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();

                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 10, 10));

                Color borderCol = focused ? AppTheme.FOREST_GREEN : AppTheme.BORDER_SAGE;
                g2.setColor(borderCol);
                g2.setStroke(new BasicStroke(focused ? 1.5f : 1.0f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, 10, 10));

                super.paintComponent(g);

                if (getText().isEmpty() && !focused && placeholder != null) {
                    g2.setColor(new Color(156, 163, 175));
                    g2.setFont(getFont());
                    FontMetrics fm = g2.getFontMetrics();
                    int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(placeholder, 12, ty);
                }
                g2.dispose();
            }
        };
        tf.setFont(AppTheme.font(Font.PLAIN, 12));
        tf.setForeground(AppTheme.TEXT_PRIMARY);
        tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(0, 12, 0, 12));
        tf.setPreferredSize(new Dimension(0, 34));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return tf;
    }

    private void styleComboBox(JComboBox<?> box) {
        box.setFont(AppTheme.font(Font.PLAIN, 12));
        box.setBackground(Color.WHITE);
        box.setForeground(AppTheme.TEXT_PRIMARY);
        box.setPreferredSize(new Dimension(0, 34));
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        box.setFocusable(false);
        box.setBorder(BorderFactory.createCompoundBorder(
                new javax.swing.border.LineBorder(AppTheme.BORDER_SAGE, 1, true),
                new EmptyBorder(2, 6, 2, 4)
        ));
        box.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        int w = getWidth(), h = getHeight();
                        int cx = w / 2, cy = h / 2;
                        g2.setColor(AppTheme.FOREST_MID);
                        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        Path2D p = new Path2D.Float();
                        p.moveTo(cx - 4, cy - 2);
                        p.lineTo(cx, cy + 2);
                        p.lineTo(cx + 4, cy - 2);
                        g2.draw(p);
                        g2.dispose();
                    }
                };
                btn.setBorder(BorderFactory.createEmptyBorder());
                btn.setContentAreaFilled(false);
                btn.setFocusPainted(false);
                btn.setOpaque(false);
                btn.setPreferredSize(new Dimension(28, 0));
                return btn;
            }
        });
    }

    private JButton createPillButton(String text, String iconKey, Color bg, Color border, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            private final SidebarIcon icon = (iconKey != null) ? new SidebarIcon(iconKey, 12, fg) : null;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int r = h;

                Color fill = hovered ? (bg.equals(AppTheme.FOREST_GREEN) ? AppTheme.FOREST_MID : (border != null ? new Color(241, 245, 249) : bg.darker())) : bg;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w, h, r, r);

                if (border != null) {
                    g2.setColor(border);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, r, r);
                }

                int iconSz = (icon != null) ? icon.getIconWidth() : 0;
                int gap = (icon != null && !getText().isEmpty()) ? 6 : 0;

                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(getText());
                int contentW = iconSz + gap + textW;

                int curX = (w - contentW) / 2;
                if (icon != null) {
                    icon.paintIcon(this, g2, curX, (h - icon.getIconHeight()) / 2);
                    curX += iconSz + gap;
                }

                g2.setColor(fg);
                int ty = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), curX, ty);

                g2.dispose();
            }
        };
        btn.setFont(AppTheme.font(Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
