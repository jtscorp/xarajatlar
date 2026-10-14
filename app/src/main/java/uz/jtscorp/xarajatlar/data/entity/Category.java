package uz.jtscorp.xarajatlar.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String iconName;
    private int color;
    private boolean isActive;

    public Category() {
        this.isActive = true;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getIconName() { return iconName; }
    public int getColor() { return color; }
    public boolean isActive() { return isActive; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setIconName(String iconName) { this.iconName = iconName; }
    public void setColor(int color) { this.color = color; }
    public void setActive(boolean active) { isActive = active; }
}
