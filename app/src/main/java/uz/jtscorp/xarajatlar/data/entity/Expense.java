package uz.jtscorp.xarajatlar.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses",
        foreignKeys = {
            @ForeignKey(entity = Category.class,
                    parentColumns = "id",
                    childColumns = "categoryId",
                    onDelete = ForeignKey.SET_NULL),
            @ForeignKey(entity = FamilyMember.class,
                    parentColumns = "id",
                    childColumns = "familyMemberId",
                    onDelete = ForeignKey.SET_NULL),
            @ForeignKey(entity = PaymentMethod.class,
                    parentColumns = "id",
                    childColumns = "paymentMethodId",
                    onDelete = ForeignKey.SET_NULL)
        },
        indices = {
            @Index("categoryId"),
            @Index("familyMemberId"),
            @Index("paymentMethodId"),
            @Index("timestamp")
        })
public class Expense {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private double amount;
    private Integer categoryId;
    private Integer familyMemberId;
    private Integer paymentMethodId;
    private String description;
    private String receiptUrl;
    private long timestamp;
    private String shopName;
    private String receiptItemsJson;

    public Expense() {
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public int getId() { return id; }
    public double getAmount() { return amount; }
    public Integer getCategoryId() { return categoryId; }
    public Integer getFamilyMemberId() { return familyMemberId; }
    public Integer getPaymentMethodId() { return paymentMethodId; }
    public String getDescription() { return description; }
    public String getReceiptUrl() { return receiptUrl; }
    public long getTimestamp() { return timestamp; }
    public String getShopName() { return shopName; }
    public String getReceiptItemsJson() { return receiptItemsJson; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public void setFamilyMemberId(Integer familyMemberId) { this.familyMemberId = familyMemberId; }
    public void setPaymentMethodId(Integer paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    public void setDescription(String description) { this.description = description; }
    public void setReceiptUrl(String receiptUrl) { this.receiptUrl = receiptUrl; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public void setReceiptItemsJson(String receiptItemsJson) { this.receiptItemsJson = receiptItemsJson; }
}
