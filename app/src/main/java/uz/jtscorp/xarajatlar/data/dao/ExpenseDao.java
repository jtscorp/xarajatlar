package uz.jtscorp.xarajatlar.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;
import uz.jtscorp.xarajatlar.data.entity.Expense;

@Dao
public interface ExpenseDao {
    @Insert
    long insert(Expense expense);

    @Update
    void update(Expense expense);

    @Delete
    void delete(Expense expense);

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    LiveData<List<Expense>> getAll();

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<Expense>> getRecent(int limit);

    @Query("SELECT * FROM expenses WHERE timestamp >= :start AND timestamp < :endExclusive ORDER BY timestamp DESC")
    LiveData<List<Expense>> getByDateRange(long start, long endExclusive);

    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    LiveData<List<Expense>> getByCategory(int categoryId);

    @Query("SELECT * FROM expenses WHERE familyMemberId = :memberId ORDER BY timestamp DESC")
    LiveData<List<Expense>> getByFamilyMember(int memberId);

    @Query("SELECT SUM(amount) FROM expenses WHERE timestamp >= :start AND timestamp < :endExclusive")
    LiveData<Double> getTotalByDateRange(long start, long endExclusive);

    @Query("SELECT SUM(amount) FROM expenses WHERE familyMemberId = :memberId AND timestamp >= :start AND timestamp < :endExclusive")
    LiveData<Double> getTotalByMemberAndDateRange(int memberId, long start, long endExclusive);

    @Query("SELECT * FROM expenses WHERE id = :id")
    Expense getById(int id);

    @Query("SELECT * FROM expenses")
    List<Expense> getAllSync();

    @Query("SELECT shopName FROM expenses WHERE shopName IS NOT NULL AND TRIM(shopName) != '' ORDER BY timestamp DESC")
    List<String> getShopNames();

    @Query("DELETE FROM expenses")
    void deleteAll();
}
