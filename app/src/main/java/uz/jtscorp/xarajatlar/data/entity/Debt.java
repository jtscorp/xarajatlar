package uz.jtscorp.xarajatlar.data.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "debts",
        indices = {@Index("isFullyPaid"), @Index("dueDate")})
public class Debt {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String personName;
    private String phone;
    private String address;
    private double amount;
    private double paidAmount;
    private long givenDate;
    private long dueDate;
    private double latitude;
    private double longitude;
    private String notes;
    private boolean isFullyPaid;

    public Debt() {
        this.givenDate = System.currentTimeMillis();
        this.paidAmount = 0;
        this.isFullyPaid = false;
    }

    // Getters
    public int getId() { return id; }
    public String getPersonName() { return personName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public double getAmount() { return amount; }
    public double getPaidAmount() { return paidAmount; }
    public long getGivenDate() { return givenDate; }
    public long getDueDate() { return dueDate; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getNotes() { return notes; }
    public boolean isFullyPaid() { return isFullyPaid; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setPersonName(String personName) { this.personName = personName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }
    public void setGivenDate(long givenDate) { this.givenDate = givenDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setFullyPaid(boolean fullyPaid) { isFullyPaid = fullyPaid; }

    // Helper
    public double getRemainingAmount() {
        return amount - paidAmount;
    }
}
