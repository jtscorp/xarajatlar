package uz.jtscorp.xarajatlar.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_settings")
public class AppSettings {
    @PrimaryKey
    private int id = 1; // Singleton - faqat bitta qator

    private int salaryDay = 1;
    private String triggerButton = "power";
    private int triggerCount = 3;
    private int triggerInterval = 500;
    private boolean biometricEnabled = false;
    private int autoLockMinutes = 1;
    private String currencyLabel; // null — tilga qarab standart belgi

    // Getters
    public int getId() { return id; }
    public int getSalaryDay() { return salaryDay; }
    public String getTriggerButton() { return triggerButton; }
    public int getTriggerCount() { return triggerCount; }
    public int getTriggerInterval() { return triggerInterval; }
    public boolean isBiometricEnabled() { return biometricEnabled; }
    public int getAutoLockMinutes() { return autoLockMinutes; }
    public String getCurrencyLabel() { return currencyLabel; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setSalaryDay(int salaryDay) { this.salaryDay = salaryDay; }
    public void setTriggerButton(String triggerButton) { this.triggerButton = triggerButton; }
    public void setTriggerCount(int triggerCount) { this.triggerCount = triggerCount; }
    public void setTriggerInterval(int triggerInterval) { this.triggerInterval = triggerInterval; }
    public void setBiometricEnabled(boolean biometricEnabled) { this.biometricEnabled = biometricEnabled; }
    public void setAutoLockMinutes(int autoLockMinutes) { this.autoLockMinutes = autoLockMinutes; }
    public void setCurrencyLabel(String currencyLabel) { this.currencyLabel = currencyLabel; }
}
