package uz.jtscorp.xarajatlar.ui.debts;

import static org.junit.Assert.assertEquals;
import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.List;
import java.util.stream.Collectors;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.Debt;

public class DebtFilterQueryTest {
    @Test public void overdueExcludesClosedAndUndatedAndSortsRemainingAmount() {
        AppDatabase db = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), AppDatabase.class).build();
        try {
            insert(db, 1, "O‘tkir", 900, 100, 70, false);
            insert(db, 2, "Boshqa", 0, 200, 0, false);
            insert(db, 3, "O‘tkir", 800, 100, 100, true);
            insert(db, 4, "O‘tkir", 1000, 100, 10, false);
            assertEquals(List.of(1), ids(db.debtDao().filterSync(DebtQueries.build("", 2, 0, 1000))));
            assertEquals(List.of(2, 4, 1), ids(db.debtDao().filterSync(DebtQueries.build("", 0, 1, 1000))));
            assertEquals(List.of(1, 4), ids(db.debtDao().filterSync(DebtQueries.build("O'TKIR", 0, 0, 1000))));
            assertEquals(List.of(3), ids(db.debtDao().filterSync(DebtQueries.build("998", 1, 0, 1000))));
            assertEquals(List.of(), ids(db.debtDao().filterSync(DebtQueries.build("topilmaydi", 0, 0, 1000))));
        } finally { db.close(); }
    }
    private List<Integer> ids(List<Debt> rows) {
        return rows.stream().map(Debt::getId).collect(Collectors.toList());
    }
    private void insert(AppDatabase db, int id, String name, long due, double amount, double paid, boolean closed) {
        Debt row = new Debt(); row.setId(id); row.setPersonName(name); row.setPhone("998");
        row.setDueDate(due); row.setAmount(amount); row.setPaidAmount(paid); row.setFullyPaid(closed);
        db.debtDao().insert(row);
    }
}
