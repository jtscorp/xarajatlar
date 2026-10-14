package uz.jtscorp.xarajatlar.ui.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.os.SystemClock;

import androidx.lifecycle.SavedStateHandle;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.Expense;
import uz.jtscorp.xarajatlar.data.entity.Income;

@RunWith(AndroidJUnit4.class)
public class HistoryViewModelStateTest {
    private AppDatabase database;
    private ManualExecutor executor;
    private Application application;
    private final List<HistoryViewModel> viewModels = new ArrayList<>();

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        application = (Application) context;
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        executor = new ManualExecutor();
        insertExpense(1, 10, 100);
        insertIncome(1, 20, 200);
    }

    @After
    public void tearDown() {
        onMain(() -> viewModels.forEach(HistoryViewModel::onCleared));
        if (database.isOpen()) database.close();
    }

    @Test
    public void rapidFilterChangesIgnoreResponsesFromOlderGenerations() {
        HistoryViewModel viewModel = createViewModel(new SavedStateHandle());
        onMain(() -> {
            viewModel.applyFilter(new TransactionFilter.Builder()
                    .kind(TransactionFilter.KIND_EXPENSE).build());
            viewModel.applyFilter(new TransactionFilter.Builder()
                    .kind(TransactionFilter.KIND_INCOME).build());
        });

        executor.runAt(2);
        waitForMain();
        assertEquals(TransactionFilter.KIND_INCOME,
                viewModel.getState().getValue().getFilter().getKind());
        assertEquals("income:1", viewModel.getState().getValue().getRows().get(0).stableId());

        executor.runAt(0);
        executor.runAt(0);
        waitForMain();
        assertEquals(TransactionFilter.KIND_INCOME,
                viewModel.getState().getValue().getFilter().getKind());
        assertEquals("income:1", viewModel.getState().getValue().getRows().get(0).stableId());
    }

    @Test
    public void savedStateRestoresFilterAndSortAfterViewModelRecreation() {
        SavedStateHandle handle = new SavedStateHandle();
        HistoryViewModel first = createViewModel(handle);
        TransactionFilter expected = new TransactionFilter.Builder()
                .query("ШЎ")
                .kind(TransactionFilter.KIND_EXPENSE)
                .familyMemberId(TransactionFilter.UNASSIGNED)
                .build();
        onMain(() -> first.applyFilterAndSort(expected, TransactionSort.AMOUNT_ASC));
        onMain(first::onCleared);
        viewModels.remove(first);

        HistoryViewModel restored = createViewModel(handle);

        assertEquals(expected, restored.getFilter());
        assertEquals(TransactionSort.AMOUNT_ASC, restored.getSort());
    }

    @Test
    public void sameFilterRefreshKeepsRowsWhileNewGenerationLoads() {
        HistoryViewModel viewModel = createViewModel(new SavedStateHandle());
        executor.runAt(0);
        waitForMain();
        assertEquals(2, viewModel.getState().getValue().getRows().size());

        onMain(viewModel::refresh);

        assertTrue(viewModel.getState().getValue().isLoading());
        assertEquals(2, viewModel.getState().getValue().getRows().size());
    }

    @Test
    public void completeFilterApplyCancelsPendingDebouncedSearch() {
        HistoryViewModel viewModel = createViewModel(new SavedStateHandle());
        onMain(() -> {
            viewModel.setSearchQuery("hidden query");
            viewModel.applyFilterAndSort(TransactionFilter.empty(), TransactionSort.NEWEST);
        });

        SystemClock.sleep(350L);
        waitForMain();

        assertEquals(null, viewModel.getFilter().getQuery());
    }

    @Test
    public void pageErrorRequiresExplicitRetry() {
        for (int id = 2; id <= 51; id++) insertExpense(id, id, 100 + id);
        HistoryViewModel viewModel = createViewModel(new SavedStateHandle());
        executor.runAt(0);
        waitForMain();
        assertTrue(viewModel.getState().getValue().canLoadMore());

        onMain(viewModel::onCleared);
        viewModels.remove(viewModel);
        database.getOpenHelper().getWritableDatabase().execSQL("DROP TABLE expenses");
        onMain(viewModel::loadNextPage);
        executor.runAt(0);
        waitForMain();
        assertTrue(viewModel.getState().getValue().getPageError() != null);

        onMain(viewModel::loadNextPage);
        assertEquals(0, executor.size());

        onMain(viewModel::retry);
        assertEquals(1, executor.size());
    }

    private HistoryViewModel createViewModel(SavedStateHandle handle) {
        HistoryViewModel[] result = new HistoryViewModel[1];
        onMain(() -> result[0] = new HistoryViewModel(application, handle, database, executor));
        viewModels.add(result[0]);
        return result[0];
    }

    private void insertExpense(int id, double amount, long timestamp) {
        Expense expense = new Expense();
        expense.setId(id);
        expense.setAmount(amount);
        expense.setTimestamp(timestamp);
        database.expenseDao().insert(expense);
    }

    private void insertIncome(int id, double amount, long timestamp) {
        Income income = new Income();
        income.setId(id);
        income.setAmount(amount);
        income.setTimestamp(timestamp);
        database.incomeDao().insert(income);
    }

    private static void onMain(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    private static void waitForMain() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static final class ManualExecutor implements Executor {
        private final List<Runnable> tasks = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            tasks.add(command);
        }

        void runAt(int index) {
            tasks.remove(index).run();
        }

        int size() {
            return tasks.size();
        }
    }
}
