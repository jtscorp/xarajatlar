package uz.jtscorp.xarajatlar.ui.debts;

import uz.jtscorp.xarajatlar.utils.Formats;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Handler;
import android.os.Looper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import java.time.LocalDate;
import java.time.ZoneId;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.Debt;
import java.util.List;

public class DebtsFragment extends Fragment {

    private TextView totalDebtText;
    private TextView filteredDebtText;
    private TextInputEditText searchInput;
    private MaterialButton sortButton;
    private String search = "";
    private int selectedStatus;
    private int selectedSort;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private final Runnable pendingSearch = this::refreshFilters;
    private final BroadcastReceiver dateChanges = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refreshFilters(); }
    };
    private TextView activeDebtCount;
    private TextView closedDebtCount;
    private TabLayout tabLayout;
    private RecyclerView debtRecyclerView;
    private LinearLayout emptyState;
    private FloatingActionButton addDebtFab;
    private DebtAdapter adapter;
    private AppDatabase database;
    private final SelectedDebtList selectedDebtList = new SelectedDebtList();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_debts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = AppDatabase.getDatabase(requireContext());

        totalDebtText = view.findViewById(R.id.totalDebtText);
        activeDebtCount = view.findViewById(R.id.activeDebtCount);
        closedDebtCount = view.findViewById(R.id.closedDebtCount);
        tabLayout = view.findViewById(R.id.tabLayout);
        debtRecyclerView = view.findViewById(R.id.debtRecyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        addDebtFab = view.findViewById(R.id.addDebtFab);

        filteredDebtText = view.findViewById(R.id.filteredDebtText);
        searchInput = view.findViewById(R.id.debtSearchInput);
        sortButton = view.findViewById(R.id.debtSortButton);
        if (savedInstanceState != null) {
            search = savedInstanceState.getString("debtSearch", "");
            selectedStatus = savedInstanceState.getInt("debtStatus", 0);
            selectedSort = savedInstanceState.getInt("debtSort", 0);
        }
        searchInput.setText(search);
        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                search = s.toString();
                searchHandler.removeCallbacks(pendingSearch);
                searchHandler.postDelayed(pendingSearch, 300);
            }
        });
        sortButton.setOnClickListener(v -> {
            selectedSort = selectedSort == DebtQueries.DUE_DATE
                    ? DebtQueries.REMAINING_DESC : DebtQueries.DUE_DATE;
            refreshFilters();
        });
        setupRecyclerView();
        setupTabs();
        setupFab();
        observeTotalDebt();

        selectedDebtList.observe(getViewLifecycleOwner(), items -> {
            boolean empty = items == null || items.isEmpty();
            debtRecyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            adapter.submitList(items);
            double remaining = 0;
            if (items != null) for (Debt item : items) remaining += item.getAmount() - item.getPaidAmount();
            int size = items == null ? 0 : items.size();
            filteredDebtText.setText(getString(R.string.debts_filtered_summary,
                    getResources().getQuantityString(R.plurals.item_count, size, size),
                    Formats.money(requireContext(), remaining)));
        });

        TabLayout.Tab restoredTab = tabLayout.getTabAt(selectedStatus);
        if (restoredTab != null) restoredTab.select();
        refreshFilters();
    }

    private void setupRecyclerView() {
        adapter = new DebtAdapter();
        debtRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        debtRecyclerView.setAdapter(adapter);

        adapter.setOnDebtClickListener(debt -> {
            Intent intent = new Intent(requireContext(), DebtDetailActivity.class);
            intent.putExtra("debt_id", debt.getId());
            startActivity(intent);
        });
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedStatus = tab.getPosition();
                refreshFilters();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupFab() {
        View.OnClickListener addDebt = v -> {
            Intent intent = new Intent(requireContext(), DebtDetailActivity.class);
            startActivity(intent);
        };
        addDebtFab.setOnClickListener(addDebt);
        requireView().findViewById(R.id.addDebtButton).setOnClickListener(addDebt);
    }

    private void observeTotalDebt() {
        // Ma'lumot kelguncha bo'sh ko'rinmasligi uchun boshlang'ich qiymatlar.
        totalDebtText.setText(Formats.money(requireContext(), 0));
        activeDebtCount.setText(getResources().getQuantityString(R.plurals.item_count, 0, 0));
        closedDebtCount.setText(getResources().getQuantityString(R.plurals.item_count, 0, 0));
        database.debtDao().getTotalActiveDebt().observe(getViewLifecycleOwner(), total -> {
            double value = total != null ? total : 0;
            totalDebtText.setText(Formats.money(requireContext(), value));
        });

        // Observe debt counts
        database.debtDao().getActiveDebts().observe(getViewLifecycleOwner(), debts -> {
            int count = debts != null ? debts.size() : 0;
            activeDebtCount.setText(getResources().getQuantityString(R.plurals.item_count, count, count));
        });

        database.debtDao().getClosedDebts().observe(getViewLifecycleOwner(), debts -> {
            int count = debts != null ? debts.size() : 0;
            closedDebtCount.setText(getResources().getQuantityString(R.plurals.item_count, count, count));
        });
    }

    private void refreshFilters() {
        if (getView() == null) return;
        searchHandler.removeCallbacks(pendingSearch);
        sortButton.setText(selectedSort == DebtQueries.DUE_DATE
                ? R.string.ui_muddat_boyicha : R.string.sort_by_remaining);
        long today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        showDebtList(database.debtDao().filter(DebtQueries.build(search, selectedStatus, selectedSort, today)));
    }

    @Override public void onResume() {
        super.onResume();
        refreshFilters();
    }

    @Override public void onStart() {
        super.onStart();
        IntentFilter changes = new IntentFilter(Intent.ACTION_DATE_CHANGED);
        changes.addAction(Intent.ACTION_TIME_CHANGED);
        changes.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        requireContext().registerReceiver(dateChanges, changes, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override public void onStop() {
        requireContext().unregisterReceiver(dateChanges);
        super.onStop();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("debtSearch", search);
        out.putInt("debtStatus", selectedStatus);
        out.putInt("debtSort", selectedSort);
    }

    @Override public void onDestroyView() {
        searchHandler.removeCallbacks(pendingSearch);
        super.onDestroyView();
    }

    private void showDebtList(LiveData<List<Debt>> debts) {
        selectedDebtList.select(debts);
    }
}
