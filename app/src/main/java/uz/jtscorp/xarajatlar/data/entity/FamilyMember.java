package uz.jtscorp.xarajatlar.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "family_members")
public class FamilyMember {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String photoPath;
    private String emoji;
    private boolean isActive;

    public FamilyMember() {
        this.isActive = true;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhotoPath() { return photoPath; }
    public String getEmoji() { return emoji; }
    public boolean isActive() { return isActive; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public void setActive(boolean active) { isActive = active; }
}
