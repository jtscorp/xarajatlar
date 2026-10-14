package uz.jtscorp.xarajatlar.ui.history;

import uz.jtscorp.xarajatlar.R;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.room.InvalidationTracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import uz.jtscorp.xarajatlar.data.dao.TransactionQueryBuilder;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.model.TransactionRow;
import uz.jtscorp.xarajatlar.data.model.TransactionSummary;

public class HistoryViewModel extends AndroidViewModel {
    public static final int PAGE_SIZE = 50;
    private static final String KEY_FILTER = "history_filter";
    private static final String KEY_SORT = "history_sort";

    private final AppDatabase database;
    private final Executor queryExecutor;
    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final InvalidationTracker.Observer invalidationObserver;

    private TransactionFilter filter;
    private TransactionSort sort;
    private long generation;
    private boolean initialLoadRunning;
    private boolean pageLoadRunning;
    private Runnable pendingSearch;

    public HistoryViewModel(@NonNull Application application, SavedStateHandle savedStateHandle) {
        this(application, savedStateHandle, AppDatabase.getDatabase(application),
                AppDatabase.databaseWriteExecutor);
    }

    HistoryViewModel(@NonNull Application application, SavedStateHandle savedStateHandle,
                     AppDatabase database, Executor queryExecutor) {
        super(application);
        this.savedStateHandle = savedStateHandle;
        this.database = database;
        this.queryExecutor = queryExecutor;
        TransactionFilter restoredFilter = savedStateHandle.get(KEY_FILTER);
        TransactionSort restoredSort = savedStateHandle.get(KEY_SORT);
        filter = restoredFilter != null ? restoredFilter : TransactionFilter.empty();
        sort = restoredSort != null ? restoredSort : TransactionSort.NEWEST;
        state.setValue(State.loading(filter, sort));

        invalidationObserver = new InvalidationTracker.Observer(
                "expenses", "incomes", "categories", "income_categories",
                "family_members", "payment_methods") {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                mainHandler.post(HistoryViewModel.this::refresh);
            }
        };
        database.getInvalidationTracker().addObserver(invalidationObserver);
        refresh();
    }

    public LiveData<State> getState() {
        return state;
    }

    public TransactionFilter getFilter() {
        return filter;
    }

    public TransactionSort getSort() {
        return sort;
    }

    public void setSearchQuery(String query) {
        cancelPendingSearch();
        pendingSearch = () -> {
            pendingSearch = null;
            applyFilter(filter.buildUpon().query(query).build());
        };
        mainHandler.postDelayed(pendingSearch, 300L);
    }

    public void applyFilter(TransactionFilter newFilter) {
        cancelPendingSearch();
        if (newFilter.equals(filter)) return;
        filter = newFilter;
        savedStateHandle.set(KEY_FILTER, newFilter);
        refresh();
    }

    public void applyFilterAndSort(TransactionFilter newFilter, TransactionSort newSort) {
        cancelPendingSearch();
        boolean changed = !newFilter.equals(filter) || newSort != sort;
        filter = newFilter;
        sort = newSort;
        savedStateHandle.set(KEY_FILTER, newFilter);
        savedStateHandle.set(KEY_SORT, newSort);
        if (changed) refresh();
    }

    public void setSort(TransactionSort newSort) {
        if (newSort == sort) return;
        sort = newSort;
        savedStateHandle.set(KEY_SORT, newSort);
        refresh();
    }

    public void refresh() {
        final long requestGeneration = ++generation;
        initialLoadRunning = true;
        pageLoadRunning = false;
        state.setValue(State.loading(state.getValue(), filter, sort));
        final TransactionFilter requestFilter = filter;
        final TransactionSort requestSort = sort;

        queryExecutor.execute(() -> {
            try {
                InitialResult result = database.runInTransaction(() -> {
                    List<TransactionRow> rows = database.transactionDao().queryRows(
                            TransactionQueryBuilder.page(requestFilter, requestSort, PAGE_SIZE, 0).asRoomQuery());
                    TransactionSummary summary = database.transactionDao().querySummary(
                            TransactionQueryBuilder.summary(requestFilter).asRoomQuery());
                    TransactionSummary databaseSummary = requestFilter.hasActiveFilters()
                            ? database.transactionDao().querySummary(
                                    TransactionQueryBuilder.summary(TransactionFilter.empty()).asRoomQuery())
                            : summary;
                    return new InitialResult(rows, summary, databaseSummary);
                });
                mainHandler.post(() -> acceptInitial(requestGeneration, requestFilter, requestSort, result));
            } catch (Throwable error) {
                mainHandler.post(() -> acceptInitialError(requestGeneration, requestFilter, requestSort, error));
            }
        });
    }

    public void loadNextPage() {
        loadNextPage(false);
    }

    private void loadNextPage(boolean explicitRetry) {
        State current = state.getValue();
        if (current == null || current.isLoading() || !current.canLoadMore()
                || initialLoadRunning || pageLoadRunning
                || (current.getPageError() != null && !explicitRetry)) return;
        pageLoadRunning = true;
        long requestGeneration = generation;
        int offset = current.getRows().size();
        TransactionFilter requestFilter = filter;
        TransactionSort requestSort = sort;
        state.setValue(current.withPageLoading());

        queryExecutor.execute(() -> {
            try {
                List<TransactionRow> page = database.transactionDao().queryRows(
                        TransactionQueryBuilder.page(requestFilter, requestSort, PAGE_SIZE, offset).asRoomQuery());
                mainHandler.post(() -> acceptPage(requestGeneration, page));
            } catch (Throwable error) {
                mainHandler.post(() -> acceptPageError(requestGeneration, error));
            }
        });
    }

    public void retry() {
        State current = state.getValue();
        if (current != null && current.getPageError() != null) loadNextPage(true);
        else refresh();
    }

    private void cancelPendingSearch() {
        if (pendingSearch == null) return;
        mainHandler.removeCallbacks(pendingSearch);
        pendingSearch = null;
    }

    private void acceptInitial(long requestGeneration, TransactionFilter requestFilter,
                               TransactionSort requestSort, InitialResult result) {
        if (requestGeneration != generation) return;
        initialLoadRunning = false;
        int matchedCount = result.summary != null ? result.summary.getCount() : 0;
        int databaseCount = result.databaseSummary != null ? result.databaseSummary.getCount() : 0;
        state.setValue(State.loaded(result.rows, result.summary, requestFilter, requestSort,
                databaseCount == 0, result.rows.size() < matchedCount));
    }

    private void acceptInitialError(long requestGeneration, TransactionFilter requestFilter,
                                    TransactionSort requestSort, Throwable error) {
        if (requestGeneration != generation) return;
        initialLoadRunning = false;
        state.setValue(State.error(requestFilter, requestSort, friendlyMessage(error)));
    }

    private void acceptPage(long requestGeneration, List<TransactionRow> page) {
        if (requestGeneration != generation) return;
        pageLoadRunning = false;
        State current = state.getValue();
        if (current == null) return;
        List<TransactionRow> rows = new ArrayList<>(current.getRows());
        rows.addAll(page);
        int matchedCount = current.getSummary() != null ? current.getSummary().getCount() : rows.size();
        state.setValue(current.withRows(rows, rows.size() < matchedCount));
    }

    private void acceptPageError(long requestGeneration, Throwable error) {
        if (requestGeneration != generation) return;
        pageLoadRunning = false;
        State current = state.getValue();
        if (current != null) state.setValue(current.withPageError(friendlyMessage(error)));
    }

    private String friendlyMessage(Throwable error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? getApplication().getString(R.string.history_load_failed) : message;
    }

    @Override
    protected void onCleared() {
        database.getInvalidationTracker().removeObserver(invalidationObserver);
        cancelPendingSearch();
        super.onCleared();
    }

    private static final class InitialResult {
        final List<TransactionRow> rows;
        final TransactionSummary summary;
        final TransactionSummary databaseSummary;

        InitialResult(List<TransactionRow> rows, TransactionSummary summary,
                      TransactionSummary databaseSummary) {
            this.rows = rows;
            this.summary = summary;
            this.databaseSummary = databaseSummary;
        }
    }

    public static final class State {
        private final List<TransactionRow> rows;
        private final TransactionSummary summary;
        private final TransactionFilter filter;
        private final TransactionSort sort;
        private final boolean loading;
        private final boolean loadingPage;
        private final boolean databaseEmpty;
        private final boolean canLoadMore;
        private final String error;
        private final String pageError;

        private State(List<TransactionRow> rows, TransactionSummary summary,
                      TransactionFilter filter, TransactionSort sort, boolean loading,
                      boolean loadingPage, boolean databaseEmpty, boolean canLoadMore,
                      String error, String pageError) {
            this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
            this.summary = summary;
            this.filter = filter;
            this.sort = sort;
            this.loading = loading;
            this.loadingPage = loadingPage;
            this.databaseEmpty = databaseEmpty;
            this.canLoadMore = canLoadMore;
            this.error = error;
            this.pageError = pageError;
        }

        static State loading(TransactionFilter filter, TransactionSort sort) {
            return loading(null, filter, sort);
        }

        static State loading(State previous, TransactionFilter filter, TransactionSort sort) {
            boolean sameRequest = previous != null && previous.filter.equals(filter)
                    && previous.sort == sort;
            return new State(sameRequest ? previous.rows : Collections.emptyList(),
                    sameRequest ? previous.summary : null, filter, sort,
                    true, false, sameRequest && previous.databaseEmpty,
                    false, null, null);
        }

        static State loaded(List<TransactionRow> rows, TransactionSummary summary,
                            TransactionFilter filter, TransactionSort sort,
                            boolean databaseEmpty, boolean canLoadMore) {
            return new State(rows, summary, filter, sort, false, false,
                    databaseEmpty, canLoadMore, null, null);
        }

        static State error(TransactionFilter filter, TransactionSort sort, String message) {
            return new State(Collections.emptyList(), null, filter, sort,
                    false, false, false, false, message, null);
        }

        State withPageLoading() {
            return new State(rows, summary, filter, sort, false, true,
                    databaseEmpty, canLoadMore, null, null);
        }

        State withRows(List<TransactionRow> newRows, boolean more) {
            return new State(newRows, summary, filter, sort, false, false,
                    databaseEmpty, more, null, null);
        }

        State withPageError(String message) {
            return new State(rows, summary, filter, sort, false, false,
                    databaseEmpty, canLoadMore, null, message);
        }

        public List<TransactionRow> getRows() { return rows; }
        public TransactionSummary getSummary() { return summary; }
        public TransactionFilter getFilter() { return filter; }
        public TransactionSort getSort() { return sort; }
        public boolean isLoading() { return loading; }
        public boolean isLoadingPage() { return loadingPage; }
        public boolean isDatabaseEmpty() { return databaseEmpty; }
        public boolean canLoadMore() { return canLoadMore; }
        public String getError() { return error; }
        public String getPageError() { return pageError; }
    }
}
