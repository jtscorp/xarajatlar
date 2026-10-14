package uz.jtscorp.xarajatlar.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "debt_evidences",
        foreignKeys = @ForeignKey(
                entity = Debt.class,
                parentColumns = "id",
                childColumns = "debtId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("debtId")})
public class DebtEvidence {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int debtId;
    private String filePath;
    private String fileType; // "image", "video", "audio"
    private long timestamp;
    private double latitude;
    private double longitude;

    public DebtEvidence() {
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public int getId() { return id; }
    public int getDebtId() { return debtId; }
    public String getFilePath() { return filePath; }
    public String getFileType() { return fileType; }
    public long getTimestamp() { return timestamp; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setDebtId(int debtId) { this.debtId = debtId; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
