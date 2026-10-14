package uz.jtscorp.xarajatlar.ui.stats;

import uz.jtscorp.xarajatlar.utils.Formats;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.InvalidationTracker;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.tabs.TabLayoutMediator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.dao.TransactionQueryBuilder;
import uz.jtscorp.xarajatlar.data.dao.TransactionQueryBuilder.Grouping;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.model.TransactionGroup;
import uz.jtscorp.xarajatlar.data.model.TransactionSummary;
import uz.jtscorp.xarajatlar.databinding.FragmentStatsBinding;
import uz.jtscorp.xarajatlar.ui.history.HistoryNavigation;
import uz.jtscorp.xarajatlar.ui.history.TransactionFilter;
import uz.jtscorp.xarajatlar.utils.DateRanges;

public class StatsFragment extends Fragment {
    private FragmentStatsBinding binding;
    private AppDatabase database;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final StatsCategoryAdapter[] lists = new StatsCategoryAdapter[3];
    private final StatsChartPagerAdapter[] charts = new StatsChartPagerAdapter[3];
    private InvalidationTracker.Observer invalidation;
    private ExecutorService invalidationExecutor;
    private long start, end;
    private boolean allTime, loading;
    private String kind = TransactionFilter.KIND_EXPENSE;
    private int periodId = R.id.monthButton;
    private int generation;
    private static final int[] COLORS = {0xFF267B60, 0xFF477FB3, 0xFFB88035, 0xFF916BA7,
            0xFFB96565, 0xFF438C91, 0xFF80744D, 0xFF6B7F86};

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        database = AppDatabase.getDatabase(requireContext());
        if (state != null) {
            kind = state.getString("statsKind", TransactionFilter.KIND_EXPENSE);
            start = state.getLong("statsStart"); end = state.getLong("statsEnd");
            allTime = state.getBoolean("statsAll"); periodId = state.getInt("statsPeriod", R.id.monthButton);
        }
        if (end == 0) setDays(LocalDate.now().withDayOfMonth(1), LocalDate.now().withDayOfMonth(1).plusMonths(1).minusDays(1));
        var recyclers = List.of(binding.categoryRecyclerView, binding.familyRecyclerView, binding.paymentRecyclerView);
        var pagers = List.of(binding.categoryChartPager, binding.familyChartPager, binding.paymentChartPager);
        var tabs = List.of(binding.categoryChartTabs, binding.familyChartTabs, binding.paymentChartTabs);
        for (int i = 0; i < lists.length; i++) {
            Grouping grouping = Grouping.values()[i];
            lists[i] = new StatsCategoryAdapter();
            lists[i].setOnItemClick(item -> openGroup(grouping, item.id));
            recyclers.get(i).setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclers.get(i).setAdapter(lists[i]);
            charts[i] = new StatsChartPagerAdapter();
            charts[i].setOnGroupSelected(id -> openGroup(grouping, id));
            pagers.get(i).setAdapter(charts[i]);
            new TabLayoutMediator(tabs.get(i), pagers.get(i),
                    (tab, position) -> tab.setText(position == 0 ? R.string.chart_pie : R.string.chart_bar)).attach();
        }
        binding.periodToggleGroup.check(periodId);
        binding.statsKindToggle.check(kind.equals(TransactionFilter.KIND_INCOME) ? R.id.statsIncomeButton : R.id.statsExpenseButton);
        binding.statsKindToggle.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked) return;
            kind = id == R.id.statsIncomeButton ? TransactionFilter.KIND_INCOME : TransactionFilter.KIND_EXPENSE;
            load();
        });
        binding.periodToggleGroup.addOnButtonCheckedListener((group, id, checked) -> {
            if (!checked) return;
            if (id == R.id.rangeButton) return;
            periodId = id; allTime = id == R.id.allTimeButton;
            LocalDate today = LocalDate.now();
            if (id == R.id.weekButton) {
                int delta = (7 + today.getDayOfWeek().getValue()
                        - WeekFields.of(Locale.getDefault()).getFirstDayOfWeek().getValue()) % 7;
                setDays(today.minusDays(delta), today.minusDays(delta).plusDays(6));
            } else if (id == R.id.monthButton) {
                LocalDate first = today.withDayOfMonth(1); setDays(first, first.plusMonths(1).minusDays(1));
            } else if (id == R.id.yearButton) {
                LocalDate first = today.withDayOfYear(1); setDays(first, first.plusYears(1).minusDays(1));
            }
            load();
        });
        binding.rangeButton.setOnClickListener(v -> showRange());
        binding.statsStatus.setOnClickListener(v -> { if (!loading) load(); });
        invalidation = new InvalidationTracker.Observer("expenses", "incomes", "categories",
                "income_categories", "family_members", "payment_methods") {
            @Override public void onInvalidated(@NonNull Set<String> tables) { main.post(StatsFragment.this::load); }
        };
        invalidationExecutor = Executors.newSingleThreadExecutor();
        InvalidationTracker.Observer observer = invalidation;
        invalidationExecutor.execute(
                () -> database.getInvalidationTracker().addObserver(observer));
        load();
    }

    private void setDays(LocalDate first, LocalDate last) {
        DateRanges.Range range = DateRanges.forDays(first, last, ZoneId.systemDefault());
        start = range.start(); end = range.endExclusive(); allTime = false;
    }

    private void showRange() {
        var picker = MaterialDatePicker.Builder.dateRangePicker().setTitleText(R.string.date_range_title)
                .setSelection(new Pair<>(DateRanges.utcPickerSelection(DateRanges.localDateFromInstant(start, ZoneId.systemDefault())),
                        DateRanges.utcPickerSelection(DateRanges.localDateFromInstant(end - 1, ZoneId.systemDefault())))).build();
        picker.addOnPositiveButtonClickListener(value -> {
            if (binding == null) return;
            periodId = R.id.rangeButton;
            setDays(DateRanges.localDateFromUtcPicker(value.first), DateRanges.localDateFromUtcPicker(value.second));
            load();
        });
        picker.addOnNegativeButtonClickListener(v -> restorePeriodButton());
        picker.addOnCancelListener(v -> restorePeriodButton());
        picker.show(getParentFragmentManager(), "statsRange");
    }

    private void restorePeriodButton() {
        if (binding != null) binding.periodToggleGroup.check(periodId);
    }

    private void load() {
        if (binding == null) return;
        final int request = ++generation;
        TransactionFilter period = new TransactionFilter.Builder()
                .startInclusive(allTime ? null : start).endExclusive(allTime ? null : end).build();
        TransactionFilter selected = period.buildUpon().kind(kind).build();
        loading = true;
        binding.statsPeriodText.setText(allTime ? getString(R.string.ui_barcha_vaqt)
                : getString(R.string.date_range,
                        Formats.date(DateRanges.localDateFromInstant(start, ZoneId.systemDefault())),
                        Formats.date(DateRanges.localDateFromInstant(end - 1, ZoneId.systemDefault()))));
        binding.statsStatus.setText(R.string.stats_calculating);
        binding.statIncomeTotal.setText("…"); binding.statExpenseTotal.setText("…"); binding.statBalanceTotal.setText("…");
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Snapshot result = database.runInTransaction(() -> new Snapshot(
                        database.transactionDao().querySummary(TransactionQueryBuilder.summary(period).asRoomQuery()),
                        List.of(database.transactionDao().queryGroups(TransactionQueryBuilder.groups(selected, Grouping.CATEGORY).asRoomQuery()),
                                database.transactionDao().queryGroups(TransactionQueryBuilder.groups(selected, Grouping.FAMILY).asRoomQuery()),
                                database.transactionDao().queryGroups(TransactionQueryBuilder.groups(selected, Grouping.PAYMENT).asRoomQuery()))));
                main.post(() -> {
                    if (binding == null || request != generation) return;
                    loading = false;
                    TransactionSummary total = result.summary;
                    binding.statIncomeTotal.setText(Formats.signedMoney(requireContext(), "+", total.getIncomeTotal()));
                    binding.statExpenseTotal.setText(Formats.signedMoney(requireContext(), "−", total.getExpenseTotal()));
                    double difference = total.getIncomeTotal() - total.getExpenseTotal();
                    binding.statBalanceTotal.setText(Formats.signedMoney(requireContext(), difference >= 0 ? "+" : "−", difference));
                    binding.statBalanceTotal.setTextColor(requireContext().getColor(difference >= 0 ? R.color.history_income : R.color.history_expense));
                    double selectedTotal = kind.equals(TransactionFilter.KIND_INCOME) ? total.getIncomeTotal() : total.getExpenseTotal();
                    binding.statsStatus.setText(selectedTotal == 0 ? R.string.stats_empty_period : R.string.stats_tap_slice);
                    for (int i = 0; i < lists.length; i++) render(i, result.groups.get(i), selectedTotal);
                });
            } catch (RuntimeException error) {
                main.post(() -> {
                    if (binding == null || request != generation) return;
                    loading = false;
                    binding.statsStatus.setText(R.string.stats_failed);
                    for (int i = 0; i < lists.length; i++) { lists[i].setItems(List.of()); charts[i].showEmpty(); }
                });
            }
        });
    }

    private void render(int index, List<TransactionGroup> groups, double total) {
        List<StatsCategoryAdapter.CategoryStat> rows = new ArrayList<>();
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (TransactionGroup group : groups) {
            int color = group.color != 0 ? group.color : COLORS[rows.size() % COLORS.length];
            rows.add(new StatsCategoryAdapter.CategoryStat(group.id, group.name, group.icon, color,
                    group.amount, total > 0 ? (float) (100 * group.amount / total) : 0));
            entries.add(new PieEntry((float) group.amount, group.name, group.id)); colors.add(color);
        }
        lists[index].setItems(rows);
        if (entries.isEmpty()) charts[index].showEmpty(); else charts[index].updateData(entries, colors);
        if (index == 1) {
            binding.familyHeader.setVisibility(View.VISIBLE); binding.familyChartCard.setVisibility(View.VISIBLE); binding.familyCard.setVisibility(View.VISIBLE);
        } else if (index == 2) {
            binding.paymentHeader.setVisibility(View.VISIBLE); binding.paymentChartCard.setVisibility(View.VISIBLE); binding.paymentCard.setVisibility(View.VISIBLE);
        }
    }

    private void openGroup(Grouping grouping, int id) {
        if (loading || binding == null) return;
        HistoryNavigation.open(NavHostFragment.findNavController(this), HistoryNavigation.arguments(kind,
                allTime ? null : start, allTime ? null : end,
                grouping == Grouping.CATEGORY ? kind : null, grouping == Grouping.CATEGORY ? id : null,
                grouping == Grouping.FAMILY ? id : null, grouping == Grouping.PAYMENT ? id : null));
    }


    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("statsKind", kind); state.putLong("statsStart", start); state.putLong("statsEnd", end);
        state.putBoolean("statsAll", allTime); state.putInt("statsPeriod", periodId);
    }

    @Override public void onDestroyView() {
        ++generation;
        if (invalidation != null && invalidationExecutor != null) {
            InvalidationTracker.Observer observer = invalidation;
            invalidationExecutor.execute(
                    () -> database.getInvalidationTracker().removeObserver(observer));
            invalidationExecutor.shutdown();
            invalidationExecutor = null;
        }
        invalidation = null;
        binding = null;
        super.onDestroyView();
    }

    private static class Snapshot {
        final TransactionSummary summary;
        final List<List<TransactionGroup>> groups;
        Snapshot(TransactionSummary summary, List<List<TransactionGroup>> groups) { this.summary = summary; this.groups = groups; }
    }
}
