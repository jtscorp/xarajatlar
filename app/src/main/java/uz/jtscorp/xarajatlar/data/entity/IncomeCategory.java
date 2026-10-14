package uz.jtscorp.xarajatlar.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "income_categories")
public class IncomeCategory {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String emoji;
    private boolean isActive;

    public IncomeCategory() {
        this.isActive = true;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmoji() { return emoji; }
    public boolean isActive() { return isActive; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public void setActive(boolean active) { isActive = active; }
}
