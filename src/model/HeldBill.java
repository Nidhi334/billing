package model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.discountValue = discountValue;
        this.discountType = discountType != null ? discountType : "FLAT";
        this.paymentMode = paymentMode != null ? paymentMode : "CASH";
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = new ArrayList<>(items); }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public long getTimestamp() { return timestamp; }

    public String getFormattedTime() {
        return new SimpleDateFormat("hh:mm a").format(new Date(timestamp));
    }

    public double getSubtotal() {
        double sub = 0.0;
        for (SaleItem it : items) {
            sub += it.getSubtotal();
        }
        return sub;
    }

    public double getDiscountAmount() {
        double sub = getSubtotal();
        if ("PERCENT".equalsIgnoreCase(discountType)) {
            return (sub * discountValue) / 100.0;
        } else {
            return Math.min(discountValue, sub);
        }
    }

    public double getGrandTotal() {
        double sub = getSubtotal();
        double disc = getDiscountAmount();
        double discounted = Math.max(0, sub - disc);
        double gst = (discounted * 18.0) / 100.0;
        return discounted + gst;
    }

    public int getTotalUnits() {
        int units = 0;
        for (SaleItem it : items) {
            units += it.getQuantity();
        }
        return units;
    }

    @Override
    public String toString() {
        return reference + " [₹" + String.format("%.2f", getGrandTotal()) + " | " + items.size() + " items] (" + getFormattedTime() + ")";
    }
}

