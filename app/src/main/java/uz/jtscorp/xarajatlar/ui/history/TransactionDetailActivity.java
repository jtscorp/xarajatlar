package uz.jtscorp.xarajatlar.ui.history;

import uz.jtscorp.xarajatlar.utils.Formats;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.dao.TransactionQueryBuilder;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.Expense;
import uz.jtscorp.xarajatlar.data.entity.Income;
import uz.jtscorp.xarajatlar.data.model.TransactionRow;
import uz.jtscorp.xarajatlar.ui.expense.ManualExpenseActivity;
import uz.jtscorp.xarajatlar.ui.income.AddIncomeActivity;
import uz.jtscorp.xarajatlar.utils.ReceiptUrls;
import uz.jtscorp.xarajatlar.utils.ReceiptItemsView;

public class TransactionDetailActivity extends AppCompatActivity {
    public static final String EXTRA_KIND = "transaction_kind";
    public static final String EXTRA_ID = "transaction_id";

    private AppDatabase database;
    private String kind;
    private int id;
    private TransactionRow row;
    private String receiptItemsJson;
    private int generation;

    public static Intent intent(android.content.Context context, TransactionRow row) {
        return intent(context, row.getKind(), row.getId());
    }

    public static Intent intent(android.content.Context context, String kind, int id) {
        return new Intent(context, TransactionDetailActivity.class)
                .putExtra(EXTRA_KIND, kind).putExtra(EXTRA_ID, id);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_transaction_detail);
        database = AppDatabase.getDatabase(this);
        kind = getIntent().getStringExtra(EXTRA_KIND);
        id = getIntent().getIntExtra(EXTRA_ID, -1);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        findViewById(R.id.detailEditButton).setOnClickListener(v -> edit());
        findViewById(R.id.detailDeleteButton).setOnClickListener(v -> confirmDelete());
        findViewById(R.id.detailReceiptButton).setOnClickListener(v -> openReceipt());
        if (id <= 0 || !(TransactionFilter.KIND_EXPENSE.equals(kind)
                || TransactionFilter.KIND_INCOME.equals(kind))) {
            Toast.makeText(this, R.string.transaction_invalid, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (database != null && id > 0) load();
    }

    private void load() {
        int request = ++generation;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            TransactionRow loaded = database.transactionDao().queryOne(
                    TransactionQueryBuilder.detail(kind, id).asRoomQuery());
            Expense expense = loaded != null && TransactionFilter.KIND_EXPENSE.equals(kind)
                    ? database.expenseDao().getById(id) : null;
            String loadedItems = expense == null ? null : expense.getReceiptItemsJson();
            runOnUiThread(() -> {
                if (request != generation || isFinishing() || isDestroyed()) return;
                if (loaded == null) {
                    Toast.makeText(this, R.string.transaction_not_found, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                row = loaded;
                receiptItemsJson = loadedItems;
                render();
            });
        });
    }

    private void render() {
        boolean income = row.isIncome();
        ((MaterialToolbar) findViewById(R.id.toolbar)).setTitle(income ? R.string.income_detail_title : R.string.expense_detail_title);
        text(R.id.detailType, getString(income ? R.string.ui_daromad : R.string.expense_label));
        text(R.id.detailAmount, Formats.signedMoney(this, income ? "+" : "−", row.getAmount()));
        text(R.id.detailDate, Formats.dateTime(row.getTimestamp()));
        text(R.id.detailCategory, value(row.getCategoryName()));
        text(R.id.detailMember, value(row.getFamilyMemberName()));
        text(R.id.detailPayment, value(row.getPaymentMethodName()));
        text(R.id.detailSourceLabel, getString(income ? R.string.source_income : R.string.source_expense));
        text(R.id.detailSource, value(row.getSourceName()));
        text(R.id.detailDescription, value(row.getDescription()));
        ReceiptItemsView.render((LinearLayout) findViewById(R.id.detailReceiptItems), receiptItemsJson);
        findViewById(R.id.detailReceiptItemsCard).setVisibility(
                ((LinearLayout) findViewById(R.id.detailReceiptItems)).getChildCount() > 0
                        ? View.VISIBLE : View.GONE);
        findViewById(R.id.detailReceiptButton).setVisibility(
                ReceiptUrls.normalizeOrNull(row.getReceiptUrl()) != null ? View.VISIBLE : View.GONE);
    }

    private void edit() {
        if (row == null) return;
        Intent intent;
        if (row.isIncome()) {
            intent = new Intent(this, AddIncomeActivity.class)
                    .putExtra(AddIncomeActivity.EXTRA_INCOME_ID, row.getId());
        } else {
            intent = new Intent(this, ManualExpenseActivity.class)
                    .putExtra(ManualExpenseActivity.EXTRA_EXPENSE_ID, row.getId());
        }
        startActivity(intent);
    }

    private void confirmDelete() {
        if (row == null) return;
        new MaterialAlertDialogBuilder(this).setTitle(R.string.delete_transaction)
                .setMessage(R.string.delete_irreversible)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> delete()).show();
    }

    private void delete() {
        findViewById(R.id.detailDeleteButton).setEnabled(false);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (TransactionFilter.KIND_INCOME.equals(kind)) {
                Income income = database.incomeDao().getById(id);
                if (income != null) database.incomeDao().delete(income);
            } else {
                Expense expense = database.expenseDao().getById(id);
                if (expense != null) database.expenseDao().delete(expense);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.transaction_deleted, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void openReceipt() {
        if (row == null) return;
        String url = ReceiptUrls.normalizeOrNull(row.getReceiptUrl());
        if (url == null) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.open_receipt_title)
                .setMessage(getString(R.string.open_receipt_message, ReceiptUrls.TRUSTED_HOST))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.open, (dialog, which) -> {
                    Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(Intent.createChooser(view, getString(R.string.open_receipt_chooser)));
                })
                .show();
    }

    private void text(int id, CharSequence value) { ((TextView) findViewById(id)).setText(value); }
    private String value(String value) { return value == null || value.isBlank() ? getString(R.string.unassigned) : value; }
}
