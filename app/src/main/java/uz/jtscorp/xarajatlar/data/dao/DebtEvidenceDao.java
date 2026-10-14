package uz.jtscorp.xarajatlar.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;
import uz.jtscorp.xarajatlar.data.entity.DebtEvidence;

@Dao
public interface DebtEvidenceDao {
    @Insert
    long insert(DebtEvidence evidence);

    @Delete
    void delete(DebtEvidence evidence);

    @Query("SELECT * FROM debt_evidences WHERE debtId = :debtId ORDER BY timestamp DESC")
    LiveData<List<DebtEvidence>> getByDebtId(int debtId);

    @Query("SELECT * FROM debt_evidences WHERE debtId = 0 ORDER BY timestamp DESC")
    LiveData<List<DebtEvidence>> getUnassigned();

    @Query("UPDATE debt_evidences SET debtId = :debtId WHERE id = :evidenceId")
    void assignToDebt(int evidenceId, int debtId);

    @Query("SELECT * FROM debt_evidences")
    List<DebtEvidence> getAllSync();

    @Query("DELETE FROM debt_evidences")
    void deleteAll();
}
