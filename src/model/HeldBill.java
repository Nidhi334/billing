package model;

import java.util.ArrayList;
import java.util.List;

public class HeldBill {
    private String id;
    private String reference;
    private Customer customer;
    private List<SaleItem> items = new ArrayList<>();
    private double discountValue = 0.0;
    private String discountType = "FLAT";
    private String paymentMode = "CASH";
    private long timestamp;

    public HeldBill(String id, String reference, Customer customer, List<SaleItem> items, double discountValue, String discountType, String paymentMode) {
        this.id = id;
        this.reference = reference;
        this.customer = customer;
        this.items = items;
        this.discountValue = discountValue;
        this.discountType = discountType;
        this.paymentMode = paymentMode;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getReference() { return reference; }
    public Customer getCustomer() { return customer; }
    public List<SaleItem> getItems() { return items; }
    public double getDiscountValue() { return discountValue; }
    public String getDiscountType() { return discountType; }
    public String getPaymentMode() { return paymentMode; }
    public long getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return reference + " (" + items.size() + " items)";
    }
}

