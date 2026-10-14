package uz.jtscorp.xarajatlar.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;
import uz.jtscorp.xarajatlar.data.entity.Debt;

@Dao
public interface DebtDao {
    @RawQuery(observedEntities = Debt.class)
    LiveData<List<Debt>> filter(androidx.sqlite.db.SupportSQLiteQuery query);

    @RawQuery
    List<Debt> filterSync(androidx.sqlite.db.SupportSQLiteQuery query);

    @Insert
    long insert(Debt debt);

    @Update
    void update(Debt debt);

    @Delete
    void delete(Debt debt);

    @Query("SELECT * FROM debts WHERE isFullyPaid = 0 ORDER BY dueDate ASC")
    LiveData<List<Debt>> getActiveDebts();

    @Query("SELECT * FROM debts WHERE isFullyPaid = 1 ORDER BY givenDate DESC")
    LiveData<List<Debt>> getClosedDebts();

    @Query("SELECT * FROM debts ORDER BY givenDate DESC")
    LiveData<List<Debt>> getAll();

    @Query("SELECT * FROM debts WHERE id = :id")
    Debt getById(int id);

    @Query("SELECT SUM(amount - paidAmount) FROM debts WHERE isFullyPaid = 0")
    LiveData<Double> getTotalActiveDebt();

    @Query("SELECT * FROM debts")
    List<Debt> getAllSync();

    @Query("DELETE FROM debts")
    void deleteAll();
}
