package uz.jtscorp.xarajatlar.ui.home;

import uz.jtscorp.xarajatlar.utils.Formats;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import uz.jtscorp.xarajatlar.MainActivity;
import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.dao.TransactionQueryBuilder;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.Expense;
import uz.jtscorp.xarajatlar.data.entity.Income;
import uz.jtscorp.xarajatlar.data.model.TransactionRow;
import uz.jtscorp.xarajatlar.ui.camera.OcrScanActivity;
import uz.jtscorp.xarajatlar.ui.camera.QrScanActivity;
import uz.jtscorp.xarajatlar.ui.history.HistoryNavigation;
import uz.jtscorp.xarajatlar.ui.history.TransactionAdapter;
import uz.jtscorp.xarajatlar.ui.history.TransactionDetailActivity;
import uz.jtscorp.xarajatlar.ui.history.TransactionFilter;
import uz.jtscorp.xarajatlar.ui.history.TransactionSort;
import uz.jtscorp.xarajatlar.utils.DateRanges;

public class HomeFragment extends Fragment {
    private AppDatabase database;
    private TransactionAdapter recentAdapter;
    private TextView emptyState, totalExpenseText, totalIncomeText, totalDebtText, differenceText;
    private double monthlyExpense, monthlyIncome;
    private int recentGeneration;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                   @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        database = AppDatabase.getDatabase(requireContext());
        totalExpenseText = view.findViewById(R.id.totalExpenseText);
        totalIncomeText = view.findViewById(R.id.totalIncomeText);
        totalDebtText = view.findViewById(R.id.totalDebtText);
        differenceText = view.findViewById(R.id.periodDifferenceText);
        emptyState = view.findViewById(R.id.emptyState);
        ((TextView) view.findViewById(R.id.periodTitleText)).setText(
                Formats.monthYear(requireContext(), LocalDate.now()));
        // Ma'lumot kelguncha bo'sh ko'rinmasligi uchun boshlang'ich qiymatlar.
        totalExpenseText.setText(Formats.money(requireContext(), 0));
        totalIncomeText.setText(Formats.money(requireContext(), 0));
        totalDebtText.setText(Formats.money(requireContext(), 0));
        differenceText.setText(Formats.money(requireContext(), 0));

        RecyclerView recentList = view.findViewById(R.id.recentRecyclerView);
        recentAdapter = new TransactionAdapter(row -> startActivity(
                TransactionDetailActivity.intent(requireContext(), row)));
        recentList.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentList.setAdapter(recentAdapter);
        attachDeleteGesture(recentList);

        view.findViewById(R.id.homeAddButton).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showAddSheet());
        view.findViewById(R.id.homeScanButton).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.home_scan)
                        .setItems(new String[]{getString(R.string.scan_qr_receipt),
                                        getString(R.string.scan_amount)}, (dialog, choice) ->
                                startActivity(new Intent(requireContext(), choice == 0
                                        ? QrScanActivity.class : OcrScanActivity.class)))
                        .show());
        view.findViewById(R.id.viewAllTransactionsButton).setOnClickListener(v ->
                HistoryNavigation.open(NavHostFragment.findNavController(this),
                        HistoryNavigation.arguments(null, null, null, null, null, null, null)));
        observeData();
    }

    private void observeData() {
        LocalDate today = LocalDate.now();
        DateRanges.Range month = DateRanges.forDays(today.withDayOfMonth(1),
                today.withDayOfMonth(1).plusMonths(1).minusDays(1), ZoneId.systemDefault());
        database.expenseDao().getTotalByDateRange(month.start(), month.endExclusive())
                .observe(getViewLifecycleOwner(), total -> {
                    monthlyExpense = total == null ? 0 : total;
                    totalExpenseText.setText(Formats.money(requireContext(), monthlyExpense));
                    updateDifference();
                });
        database.incomeDao().getTotalByDateRange(month.start(), month.endExclusive())
                .observe(getViewLifecycleOwner(), total -> {
                    monthlyIncome = total == null ? 0 : total;
                    totalIncomeText.setText(Formats.money(requireContext(), monthlyIncome));
                    updateDifference();
                });
        database.debtDao().getTotalActiveDebt().observe(getViewLifecycleOwner(), total ->
                totalDebtText.setText(Formats.money(requireContext(), total == null ? 0 : total)));
        database.expenseDao().getRecent(1).observe(getViewLifecycleOwner(), ignored -> loadRecent());
        database.incomeDao().getRecent(1).observe(getViewLifecycleOwner(), ignored -> loadRecent());
    }

    private void loadRecent() {
        int request = ++recentGeneration;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<TransactionRow> rows = database.transactionDao().queryRows(
                    TransactionQueryBuilder.page(TransactionFilter.empty(),
                            TransactionSort.NEWEST, 6, 0).asRoomQuery());
            FragmentActivity activity = getActivity();
            if (activity == null) return;
            activity.runOnUiThread(() -> {
                if (request != recentGeneration || getView() == null) return;
                recentAdapter.submitList(rows);
                emptyState.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    private void updateDifference() {
        double difference = monthlyIncome - monthlyExpense;
        differenceText.setText(Formats.signedMoney(requireContext(),
                difference >= 0 ? "+" : "−", difference));
        differenceText.setTextColor(requireContext().getColor(
                difference >= 0 ? R.color.history_income : R.color.history_expense));
    }

    private void attachDeleteGesture(RecyclerView list) {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            private final ColorDrawable background = new ColorDrawable(requireContext().getColor(
                    R.color.history_expense));
            @Override public boolean onMove(@NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder from, @NonNull RecyclerView.ViewHolder to) {
                return false;
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
                int position = holder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                TransactionRow row = recentAdapter.rowAt(position);
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.delete_transaction)
                        .setMessage(R.string.delete_transaction_message)
                        .setPositiveButton(R.string.delete, (dialog, which) -> delete(row))
                        .setNegativeButton(R.string.cancel, (dialog, which) ->
                                recentAdapter.notifyItemChanged(position))
                        .setOnCancelListener(dialog -> recentAdapter.notifyItemChanged(position))
                        .show();
            }
            @Override public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder holder, float dx, float dy, int state, boolean active) {
                View item = holder.itemView;
                if (dx < 0) {
                    background.setBounds(item.getRight() + (int) dx, item.getTop(),
                            item.getRight(), item.getBottom());
                    background.draw(canvas);
                }
                super.onChildDraw(canvas, recyclerView, holder, dx, dy, state, active);
            }
        }).attachToRecyclerView(list);
    }

    private void delete(TransactionRow row) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (row.isIncome()) {
                Income income = database.incomeDao().getById(row.getId());
                if (income != null) database.incomeDao().delete(income);
            } else {
                Expense expense = database.expenseDao().getById(row.getId());
                if (expense != null) database.expenseDao().delete(expense);
            }
            FragmentActivity activity = getActivity();
            if (activity != null) activity.runOnUiThread(this::loadRecent);
        });
    }

    @Override public void onResume() {
        super.onResume();
        if (recentAdapter != null) loadRecent();
    }

    @Override public void onDestroyView() {
        recentGeneration++;
        recentAdapter = null;
        super.onDestroyView();
    }
}
