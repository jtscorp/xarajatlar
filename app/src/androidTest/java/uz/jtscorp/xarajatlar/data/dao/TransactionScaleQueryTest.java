package uz.jtscorp.xarajatlar.data.dao;

import static org.junit.Assert.assertEquals;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteStatement;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.List;
import org.junit.Test;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.model.TransactionRow;
import uz.jtscorp.xarajatlar.data.model.TransactionSummary;
import uz.jtscorp.xarajatlar.ui.history.TransactionFilter;
import uz.jtscorp.xarajatlar.ui.history.TransactionSort;

public class TransactionScaleQueryTest {
    @Test public void tenThousandRows_remainPagedAndFullyCounted() {
        AppDatabase database = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), AppDatabase.class).build();
        try {
            database.runInTransaction(() -> {
                SupportSQLiteStatement statement = database.getOpenHelper().getWritableDatabase()
                        .compileStatement("INSERT INTO expenses(amount, timestamp) VALUES (?, ?)");
                for (int i = 0; i < 10_000; i++) {
                    statement.bindDouble(1, i + 1);
                    statement.bindLong(2, i);
                    statement.executeInsert();
                    statement.clearBindings();
                }
            });
            TransactionFilter filter = TransactionFilter.empty();
            List<TransactionRow> firstPage = database.transactionDao().queryRows(
                    TransactionQueryBuilder.page(filter, TransactionSort.NEWEST, 50, 0).asRoomQuery());
            TransactionSummary summary = database.transactionDao().querySummary(
                    TransactionQueryBuilder.summary(filter).asRoomQuery());

            assertEquals(50, firstPage.size());
            assertEquals(10_000, summary.getCount());
            assertEquals(10_000, firstPage.get(0).getAmount(), 0.01);
        } finally {
            database.close();
        }
    }
}
