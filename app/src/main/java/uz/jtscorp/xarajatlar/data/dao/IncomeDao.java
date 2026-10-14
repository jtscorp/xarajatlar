package uz.jtscorp.xarajatlar.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;
import uz.jtscorp.xarajatlar.data.entity.Income;

@Dao
public interface IncomeDao {
    @Insert
    long insert(Income income);

    @Update
    void update(Income income);

    @Delete
    void delete(Income income);

    @Query("SELECT * FROM incomes ORDER BY timestamp DESC")
    LiveData<List<Income>> getAll();

    @Query("SELECT SUM(amount) FROM incomes WHERE timestamp >= :start AND timestamp < :endExclusive")
    LiveData<Double> getTotalByDateRange(long start, long endExclusive);

    @Query("SELECT * FROM incomes WHERE id = :id")
    Income getById(int id);

    @Query("SELECT * FROM incomes")
    List<Income> getAllSync();

    @Query("DELETE FROM incomes")
    void deleteAll();

    @Query("SELECT * FROM incomes ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<Income>> getRecent(int limit);

    @Query("SELECT * FROM incomes WHERE timestamp >= :start AND timestamp < :endExclusive ORDER BY timestamp DESC")
    LiveData<List<Income>> getByDateRange(long start, long endExclusive);

    @Query("SELECT * FROM incomes WHERE incomeCategoryId = :categoryId ORDER BY timestamp DESC")
    LiveData<List<Income>> getByCategory(int categoryId);
}
