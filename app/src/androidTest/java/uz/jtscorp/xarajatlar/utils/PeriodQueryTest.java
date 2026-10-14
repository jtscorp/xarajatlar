package uz.jtscorp.xarajatlar.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.Expense;
import uz.jtscorp.xarajatlar.data.entity.Income;

/** Exercises the actual Room queries against an isolated in-memory database. */
public class PeriodQueryTest {
    private AppDatabase database;

    @Before public void createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), AppDatabase.class).build();
    }

    @After public void closeDatabase() { database.close(); }

    @Test public void expenseRowsAndTotalUseTheSameHalfOpenPeriod() throws Exception {
        for (long timestamp : new long[]{999, 1000, 1999, 2000}) {
            Expense row = new Expense();
            row.setTimestamp(timestamp);
            row.setAmount(timestamp == 1000 ? 10 : timestamp == 1999 ? 20 : 100);
            database.expenseDao().insert(row);
        }
        List<Expense> rows = await(database.expenseDao().getByDateRange(1000, 2000));
        assertEquals(2, rows.size());
        assertEquals(1999, rows.get(0).getTimestamp());
        assertEquals(1000, rows.get(1).getTimestamp());
        assertEquals(30, await(database.expenseDao().getTotalByDateRange(1000, 2000)), 0);
    }

    @Test public void incomeRowsAndTotalExcludeTheFollowingPeriodStart() throws Exception {
        for (long timestamp : new long[]{999, 1000, 1999, 2000}) {
            Income row = new Income();
            row.setTimestamp(timestamp);
            row.setAmount(timestamp == 1000 ? 40 : timestamp == 1999 ? 60 : 500);
            database.incomeDao().insert(row);
        }
        List<Income> rows = await(database.incomeDao().getByDateRange(1000, 2000));
        assertEquals(2, rows.size());
        assertEquals(1999, rows.get(0).getTimestamp());
        assertEquals(1000, rows.get(1).getTimestamp());
        assertEquals(100, await(database.incomeDao().getTotalByDateRange(1000, 2000)), 0);
    }

    private <T> T await(LiveData<T> data) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        Observer<T> observer = value -> { result.set(value); latch.countDown(); };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> data.observeForever(observer));
        try {
            assertTrue("Room query did not emit", latch.await(5, TimeUnit.SECONDS));
            return result.get();
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> data.removeObserver(observer));
        }
    }
}
