package uz.jtscorp.xarajatlar.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import uz.jtscorp.xarajatlar.data.entity.AppSettings;

@Dao
public interface AppSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppSettings settings);

    @Update
    void update(AppSettings settings);

    @Query("SELECT * FROM app_settings WHERE id = 1")
    LiveData<AppSettings> getSettings();

    @Query("SELECT * FROM app_settings WHERE id = 1")
    AppSettings getSettingsSync();
}
