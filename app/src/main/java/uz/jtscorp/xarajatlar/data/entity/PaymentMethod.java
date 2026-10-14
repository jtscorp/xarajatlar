package uz.jtscorp.xarajatlar.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "payment_methods")
public class PaymentMethod {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String lastFourDigits;
    private int color;
    private boolean isCash;

    public PaymentMethod() {
        this.isCash = false;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getLastFourDigits() { return lastFourDigits; }
    public int getColor() { return color; }
    public boolean isCash() { return isCash; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setLastFourDigits(String lastFourDigits) { this.lastFourDigits = lastFourDigits; }
    public void setColor(int color) { this.color = color; }
    public void setCash(boolean cash) { isCash = cash; }
}
