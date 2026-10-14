package uz.jtscorp.xarajatlar.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "incomes",
        foreignKeys = {
            @ForeignKey(entity = IncomeCategory.class,
                    parentColumns = "id",
                    childColumns = "incomeCategoryId",
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
            @Index("timestamp"),
            @Index("incomeCategoryId"),
            @Index("familyMemberId"),
            @Index("paymentMethodId")
        })
public class Income {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private double amount;
    private long timestamp;
    private String source;
    private Integer incomeCategoryId;
    private Integer familyMemberId;
    private Integer paymentMethodId;
    private String description;

    public Income() {
        this.timestamp = System.currentTimeMillis();
    }

    public int getId() { return id; }
    public double getAmount() { return amount; }
    public long getTimestamp() { return timestamp; }
    public String getSource() { return source; }
    public Integer getIncomeCategoryId() { return incomeCategoryId; }
    public Integer getFamilyMemberId() { return familyMemberId; }
    public Integer getPaymentMethodId() { return paymentMethodId; }
    public String getDescription() { return description; }

    public void setId(int id) { this.id = id; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setSource(String source) { this.source = source; }
    public void setIncomeCategoryId(Integer incomeCategoryId) { this.incomeCategoryId = incomeCategoryId; }
    public void setFamilyMemberId(Integer familyMemberId) { this.familyMemberId = familyMemberId; }
    public void setPaymentMethodId(Integer paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    public void setDescription(String description) { this.description = description; }
}
