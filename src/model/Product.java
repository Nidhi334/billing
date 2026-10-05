package model;

public class Product {
    private int id;
    private String code;
    private String barcode;
    private String name;
    private int categoryId;
    private String categoryName;
    private double purchasePrice;
    private double sellingPrice;
    private int quantity;
    private int minStockLevel;
    private String imagePath;

    public Product() {}

    public Product(int id, String code, String barcode, String name, int categoryId, double purchasePrice, double sellingPrice, int quantity, int minStockLevel) {
        this.id = id;
        this.code = code;
        this.barcode = barcode;
        this.name = name;
        this.categoryId = categoryId;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.quantity = quantity;
        this.minStockLevel = minStockLevel;
    }

    public Product(int id, String code, String barcode, String name, int categoryId, double purchasePrice, double sellingPrice, int quantity, int minStockLevel, String imagePath) {
        this(id, code, barcode, name, categoryId, purchasePrice, sellingPrice, quantity, minStockLevel);
        this.imagePath = imagePath;
    }

    public Product(int id, String code, String name, int categoryId, double purchasePrice, double sellingPrice, int quantity, int minStockLevel) {
        this(id, code, code, name, categoryId, purchasePrice, sellingPrice, quantity, minStockLevel);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getBarcode() { return (barcode != null && !barcode.trim().isEmpty()) ? barcode : code; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getMinStockLevel() { return minStockLevel; }
    public void setMinStockLevel(int minStockLevel) { this.minStockLevel = minStockLevel; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public boolean isLowStock() {
        return quantity <= minStockLevel;
    }

    public boolean isOutOfStock() {
        return quantity <= 0;
    }
}

