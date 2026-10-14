package uz.jtscorp.xarajatlar.ui.history;

import uz.jtscorp.xarajatlar.utils.Formats;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;


import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.model.TransactionRow;
import uz.jtscorp.xarajatlar.data.model.TransactionSummary;

public class HistoryFragment extends Fragment {
    private static final String INITIAL_ARGS_CONSUMED = "history_initial_args_consumed";

    private HistoryViewModel viewModel;
    private TransactionAdapter adapter;
    private RecyclerView recyclerView;
    private TextInputEditText searchInput;
    private TextView resultCount;
    private TextView totals;
    private LinearLayout loadingState;
    private LinearLayout messageState;
    private TextView messageTitle;
    private TextView messageText;
    private MaterialButton retryButton;
    private LinearLayout pageState;
    private View pageProgress;
    private TextView pageError;
    private MaterialButton pageRetry;
    private MaterialButton clearFilters;
    private ChipGroup chipGroup;
    private HorizontalScrollView chipScroll;
    private boolean settingSearchText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        adapter = new TransactionAdapter(this::openTransaction);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recycler, int dx, int dy) {
                RecyclerView.LayoutManager manager = recycler.getLayoutManager();
                if (dy <= 0 || !(manager instanceof LinearLayoutManager)) return;
                LinearLayoutManager linear = (LinearLayoutManager) manager;
                if (linear.findLastVisibleItemPosition() >= adapter.getItemCount() - 6) {
                    viewModel.loadNextPage();
                }
            }
        });

        setupSearch();
        view.findViewById(R.id.historyFilterButton).setOnClickListener(v -> showFilters());
        clearFilters.setOnClickListener(v -> clearAllFilters());
        retryButton.setOnClickListener(v -> viewModel.retry());
        pageRetry.setOnClickListener(v -> viewModel.retry());
        setupFilterResult();
        setupNavigationRequests();
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.historyRecyclerView);
        searchInput = view.findViewById(R.id.historySearchInput);
        resultCount = view.findViewById(R.id.historyResultCount);
        totals = view.findViewById(R.id.historyTotals);
        loadingState = view.findViewById(R.id.historyLoadingState);
        messageState = view.findViewById(R.id.historyMessageState);
        messageTitle = view.findViewById(R.id.historyMessageTitle);
        messageText = view.findViewById(R.id.historyMessageText);
        retryButton = view.findViewById(R.id.historyRetryButton);
        pageState = view.findViewById(R.id.historyPageState);
        pageProgress = view.findViewById(R.id.historyPageProgress);
        pageError = view.findViewById(R.id.historyPageError);
        pageRetry = view.findViewById(R.id.historyPageRetry);
        clearFilters = view.findViewById(R.id.historyClearFilters);
        chipGroup = view.findViewById(R.id.historyChipGroup);
        chipScroll = view.findViewById(R.id.historyChipScroll);
    }

    private void setupSearch() {
        settingSearchText = true;
        searchInput.setText(viewModel.getFilter().getQuery());
        if (searchInput.getText() != null) searchInput.setSelection(searchInput.getText().length());
        settingSearchText = false;
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                if (!settingSearchText) viewModel.setSearchQuery(value.toString());
            }
            @Override public void afterTextChanged(Editable value) {}
        });
    }

    private void setupFilterResult() {
        getParentFragmentManager().setFragmentResultListener(
                HistoryFilterBottomSheet.RESULT_KEY, getViewLifecycleOwner(), (key, result) -> {
                    TransactionFilter filter = result.getSerializable(
                            HistoryFilterBottomSheet.RESULT_FILTER, TransactionFilter.class);
                    TransactionSort sort = result.getSerializable(
                            HistoryFilterBottomSheet.RESULT_SORT, TransactionSort.class);
                    if (filter != null && sort != null) {
                        updateSearchText(filter.getQuery());
                        viewModel.applyFilterAndSort(filter, sort);
                    }
                });
    }

    private void setupNavigationRequests() {
        NavBackStackEntry entry = NavHostFragment.findNavController(this).getCurrentBackStackEntry();
        if (entry == null) return;
        Boolean consumed = entry.getSavedStateHandle().get(INITIAL_ARGS_CONSUMED);
        if (!Boolean.TRUE.equals(consumed)) {
            TransactionFilter initial = HistoryNavigation.filterFrom(getArguments());
            if (initial != null) {
                viewModel.applyFilter(initial);
                updateSearchText(initial.getQuery());
            }
            entry.getSavedStateHandle().set(INITIAL_ARGS_CONSUMED, true);
        }
        entry.getSavedStateHandle().getLiveData(HistoryNavigation.REQUEST_KEY, (Bundle) null)
                .observe(getViewLifecycleOwner(), request -> {
                    if (request == null) return;
                    TransactionFilter filter = HistoryNavigation.filterFrom(request);
                    if (filter != null) {
                        updateSearchText(filter.getQuery());
                        viewModel.applyFilter(filter);
                    }
                    entry.getSavedStateHandle().set(HistoryNavigation.REQUEST_KEY, null);
                });
    }

    private void render(HistoryViewModel.State state) {
        adapter.submitList(state.getRows());
        renderSummary(state.getSummary());
        renderChips(state.getFilter(), state.getSort());
        clearFilters.setVisibility(state.getFilter().hasActiveFilters()
                || state.getSort() != TransactionSort.NEWEST ? View.VISIBLE : View.GONE);

        loadingState.setVisibility(state.isLoading() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(!state.isLoading() && state.getError() == null
                && !state.getRows().isEmpty() ? View.VISIBLE : View.GONE);
        if (state.getError() != null) {
            showMessage(getString(R.string.history_load_failed), state.getError(), true);
        } else if (!state.isLoading() && state.getRows().isEmpty()) {
            if (state.isDatabaseEmpty()) {
                showMessage(getString(R.string.history_empty_title),
                        getString(R.string.history_empty_message), false);
            } else {
                showMessage(getString(R.string.history_no_match_title),
                        getString(R.string.history_no_match_message), false);
            }
        } else {
            messageState.setVisibility(View.GONE);
        }

        boolean showPage = state.isLoadingPage() || state.getPageError() != null;
        pageState.setVisibility(showPage ? View.VISIBLE : View.GONE);
        pageProgress.setVisibility(state.isLoadingPage() ? View.VISIBLE : View.GONE);
        pageError.setVisibility(state.getPageError() != null ? View.VISIBLE : View.GONE);
        pageRetry.setVisibility(state.getPageError() != null ? View.VISIBLE : View.GONE);
        if (state.getPageError() != null) pageError.setText(state.getPageError());
    }

    private void renderSummary(TransactionSummary summary) {
        if (summary == null) {
            resultCount.setText("—");
            totals.setText("");
            return;
        }
        resultCount.setText(getResources().getQuantityString(
                R.plurals.record_count, summary.getCount(), summary.getCount()));
        totals.setText(getString(R.string.history_totals,
                Formats.number(summary.getIncomeTotal()),
                Formats.number(summary.getExpenseTotal()),
                Formats.money(requireContext(), summary.getBalance())));
    }

    private void renderChips(TransactionFilter filter, TransactionSort sort) {
        chipGroup.removeAllViews();
        if (filter.getQuery() != null) addChip(getString(R.string.chip_search, filter.getQuery()),
                () -> changeFilter(filter.buildUpon().query(null).build()));
        if (filter.getKind() != null) addChip(TransactionFilter.KIND_INCOME.equals(filter.getKind())
                        ? getString(R.string.ui_daromad) : getString(R.string.expense_label),
                () -> changeFilter(filter.buildUpon().kind(null).build()));
        if (filter.getStartInclusive() != null || filter.getEndExclusive() != null) {
            String start = formatDate(filter.getStartInclusive());
            String end = filter.getEndExclusive() == null ? "…"
                    : formatDate(filter.getEndExclusive() - 1);
            addChip(getString(R.string.amount_range, start, end), () -> changeFilter(filter.buildUpon()
                    .startInclusive(null).endExclusive(null).build()));
        }
        if (filter.getCategoryId() != null) addChip(relationLabel(getString(R.string.ui_kategoriya), filter.getCategoryId()),
                () -> changeFilter(filter.buildUpon().category(null, null).build()));
        if (filter.getFamilyMemberId() != null) addChip(relationLabel(getString(R.string.chip_family), filter.getFamilyMemberId()),
                () -> changeFilter(filter.buildUpon().familyMemberId(null).build()));
        if (filter.getPaymentMethodId() != null) addChip(relationLabel(getString(R.string.chip_payment), filter.getPaymentMethodId()),
                () -> changeFilter(filter.buildUpon().paymentMethodId(null).build()));
        if (filter.getMinAmount() != null || filter.getMaxAmount() != null) {
            String min = filter.getMinAmount() == null ? "0" : Formats.number(filter.getMinAmount());
            String max = filter.getMaxAmount() == null ? "∞" : Formats.number(filter.getMaxAmount());
            addChip(Formats.withCurrency(requireContext(), getString(R.string.amount_range, min, max)), () -> changeFilter(filter.buildUpon()
                    .minAmount(null).maxAmount(null).build()));
        }
        if (sort != TransactionSort.NEWEST) addChip(sortLabel(sort),
                () -> viewModel.setSort(TransactionSort.NEWEST));
        chipScroll.setVisibility(chipGroup.getChildCount() == 0 ? View.GONE : View.VISIBLE);
    }

    private void addChip(String label, Runnable remove) {
        Chip chip = new Chip(requireContext());
        chip.setText(label);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> remove.run());
        chipGroup.addView(chip);
    }

    private void changeFilter(TransactionFilter filter) {
        updateSearchText(filter.getQuery());
        viewModel.applyFilter(filter);
    }

    private void clearAllFilters() {
        updateSearchText(null);
        viewModel.applyFilterAndSort(TransactionFilter.empty(), TransactionSort.NEWEST);
    }

    private void updateSearchText(String value) {
        String next = value == null ? "" : value;
        if (searchInput.getText() != null && next.contentEquals(searchInput.getText())) return;
        settingSearchText = true;
        searchInput.setText(next);
        searchInput.setSelection(next.length());
        settingSearchText = false;
    }

    private void showFilters() {
        HistoryFilterBottomSheet.newInstance(viewModel.getFilter(), viewModel.getSort())
                .show(getParentFragmentManager(), "historyFilters");
    }

    private void openTransaction(TransactionRow row) {
        startActivity(TransactionDetailActivity.intent(requireContext(), row));
    }

    private void showMessage(String title, String detail, boolean retry) {
        messageState.setVisibility(View.VISIBLE);
        messageTitle.setText(title);
        messageText.setText(detail);
        retryButton.setVisibility(retry ? View.VISIBLE : View.GONE);
    }

    private String relationLabel(String name, int id) {
        return id == TransactionFilter.UNASSIGNED
                ? getString(R.string.labeled_value, name, getString(R.string.unassigned))
                : getString(R.string.chip_relation_id, name, id);
    }

    private static String formatDate(Long timestamp) {
        if (timestamp == null) return "…";
        return Formats.date(timestamp);
    }

    private String sortLabel(TransactionSort sort) {
        return getString(switch (sort) {
            case OLDEST -> R.string.sort_oldest;
            case AMOUNT_DESC -> R.string.sort_amount_desc;
            case AMOUNT_ASC -> R.string.sort_amount_asc;
            default -> R.string.sort_newest;
        });
    }
}
