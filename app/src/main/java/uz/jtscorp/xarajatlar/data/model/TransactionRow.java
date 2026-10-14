package uz.jtscorp.xarajatlar.data.model;

import java.util.Objects;

public class TransactionRow {
    private String kind;
    private int id;
    private double amount;
    private long timestamp;
    private Integer categoryId;
    private Integer familyMemberId;
    private Integer paymentMethodId;
    private String description;
    private String sourceName;
    private String receiptUrl;
    private String categoryName;
    private String categoryIcon;
    private int categoryColor;
    private String familyMemberName;
    private String paymentMethodName;

    public String stableId() { return kind + ":" + id; }
    public boolean isIncome() { return "income".equals(kind); }

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public Integer getFamilyMemberId() { return familyMemberId; }
    public void setFamilyMemberId(Integer familyMemberId) { this.familyMemberId = familyMemberId; }
    public Integer getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(Integer paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getReceiptUrl() { return receiptUrl; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getCategoryIcon() { return categoryIcon; }
    public void setCategoryIcon(String categoryIcon) { this.categoryIcon = categoryIcon; }
    public int getCategoryColor() { return categoryColor; }
    public void setCategoryColor(int categoryColor) { this.categoryColor = categoryColor; }
    public String getFamilyMemberName() { return familyMemberName; }
    public void setFamilyMemberName(String familyMemberName) { this.familyMemberName = familyMemberName; }
    public String getPaymentMethodName() { return paymentMethodName; }
    public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof TransactionRow)) return false;
        TransactionRow that = (TransactionRow) object;
        return id == that.id && Objects.equals(kind, that.kind);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, id);
    }
}
