package uz.jtscorp.xarajatlar.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;
import uz.jtscorp.xarajatlar.data.entity.IncomeCategory;

@Dao
public interface IncomeCategoryDao {
    @Insert
    long insert(IncomeCategory category);

    @Update
    void update(IncomeCategory category);

    @Delete
    void delete(IncomeCategory category);

    @Query("SELECT * FROM income_categories WHERE isActive = 1 ORDER BY name")
    LiveData<List<IncomeCategory>> getAllActive();

    @Query("SELECT * FROM income_categories ORDER BY name")
    LiveData<List<IncomeCategory>> getAll();

    @Query("SELECT * FROM income_categories WHERE id = :id")
    IncomeCategory getById(int id);

    @Query("SELECT * FROM income_categories")
    List<IncomeCategory> getAllSync();

    @Query("DELETE FROM income_categories")
    void deleteAll();
}
