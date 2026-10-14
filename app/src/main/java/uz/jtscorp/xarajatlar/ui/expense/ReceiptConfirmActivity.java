package uz.jtscorp.xarajatlar.ui.expense;

import uz.jtscorp.xarajatlar.utils.Formats;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.Category;
import uz.jtscorp.xarajatlar.data.entity.Expense;
import uz.jtscorp.xarajatlar.data.entity.FamilyMember;
import uz.jtscorp.xarajatlar.ui.settings.ManageFamilyMembersActivity;
import uz.jtscorp.xarajatlar.utils.ReceiptItemsView;

public class ReceiptConfirmActivity extends AppCompatActivity {

    public static final String EXTRA_AMOUNT = "extra_amount";
    public static final String EXTRA_STORE_NAME = "extra_store_name";
    public static final String EXTRA_DATE = "extra_date";
    public static final String EXTRA_RECEIPT_URL = "extra_receipt_url";
    public static final String EXTRA_RECEIPT_ITEMS = "extra_receipt_items";

    private TextInputEditText amountText;
    private AutoCompleteTextView storeNameText;
    private MaterialButton dateText;
    private TextInputEditText noteInput;
    private RecyclerView categoryGrid;
    private ChipGroup familyMemberChips;
    private MaterialButton saveButton;

    private CategoryAdapter categoryAdapter;
    private AppDatabase database;

    private double amount;
    private String storeName;
    private long date;
    private String receiptUrl;
    private String receiptItemsJson;
    private boolean saving;

    private Integer selectedCategoryId = null;
    private Integer selectedFamilyMemberId = null;

    private final DecimalFormat formatter;

    public ReceiptConfirmActivity() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        formatter = new DecimalFormat("#,###", symbols);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_confirm);

        database = AppDatabase.getDatabase(this);

        // Get data from intent
        amount = getIntent().getDoubleExtra(EXTRA_AMOUNT, 0);
        storeName = getIntent().getStringExtra(EXTRA_STORE_NAME);
        date = getIntent().getLongExtra(EXTRA_DATE, System.currentTimeMillis());
        receiptUrl = getIntent().getStringExtra(EXTRA_RECEIPT_URL);
        receiptItemsJson = getIntent().getStringExtra(EXTRA_RECEIPT_ITEMS);

        if (storeName == null || storeName.isEmpty()) {
            storeName = getString(R.string.unknown);
        }

        initViews();
        setupToolbar();
        displayReceiptInfo();
        ReceiptItemsView.render(findViewById(R.id.receiptItems), receiptItemsJson);
        findViewById(R.id.receiptItemsCard).setVisibility(
                receiptItemsJson == null || receiptItemsJson.isEmpty()
                        ? android.view.View.GONE : android.view.View.VISIBLE);
        loadShopNames();
        loadCategories();
        loadFamilyMembers();
        setupSaveButton();
    }

    private void initViews() {
        amountText = findViewById(R.id.amountText);
        storeNameText = findViewById(R.id.storeNameText);
        dateText = findViewById(R.id.dateText);
        noteInput = findViewById(R.id.noteInput);
        categoryGrid = findViewById(R.id.categoryGrid);
        familyMemberChips = findViewById(R.id.familyMemberChips);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void displayReceiptInfo() {
        amountText.setText(formatter.format(amount));
        storeNameText.setText(storeName);
        dateText.setText(Formats.date(date));
        dateText.setOnClickListener(v -> {
            Calendar selected = Calendar.getInstance();
            selected.setTimeInMillis(date);
            new DatePickerDialog(this, (picker, year, month, dayOfMonth) -> {
                selected.set(year, month, dayOfMonth);
                date = selected.getTimeInMillis();
                dateText.setText(Formats.date(date));
            }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH),
                    selected.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void loadShopNames() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<String> names = ShopNameSuggestions.unique(database.expenseDao().getShopNames());
            if (!isFinishing() && !isDestroyed()) runOnUiThread(() -> {
                storeNameText.setAdapter(new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, names));
                storeNameText.setThreshold(1);
            });
        });
    }

    private void loadCategories() {
        categoryGrid.setLayoutManager(new GridLayoutManager(this, 4));

        new Thread(() -> {
            List<Category> categories = database.categoryDao().getAllSync();

            runOnUiThread(() -> {
                categoryAdapter = new CategoryAdapter(categories, category -> {
                    selectedCategoryId = category.getId();
                });
                categoryGrid.setAdapter(categoryAdapter);
            });
        }).start();
    }

    private void loadFamilyMembers() {
        new Thread(() -> {
            List<FamilyMember> members = database.familyMemberDao().getAllSync();

            runOnUiThread(() -> {
                familyMemberChips.removeAllViews();
                for (FamilyMember member : members) {
                    Chip chip = new Chip(this);
                    chip.setText(member.getName());
                    chip.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
                    chip.setCheckable(true);
                    chip.setTag(member.getId());

                    chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            selectedFamilyMemberId = (Integer) buttonView.getTag();
                        }
                    });

                    familyMemberChips.addView(chip);
                }

                // Add "+" chip for adding new member
                Chip addChip = new Chip(this);
                addChip.setText("+");
                addChip.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
                addChip.setCheckable(false);
                addChip.setContentDescription(getString(R.string.add_family_member));
                addChip.setOnClickListener(v -> startActivity(
                        new Intent(this, ManageFamilyMembersActivity.class)));
                familyMemberChips.addView(addChip);
                for (int i = 0; i < familyMemberChips.getChildCount(); i++) {
                    android.view.View child = familyMemberChips.getChildAt(i);
                    if (child instanceof Chip && selectedFamilyMemberId != null
                            && selectedFamilyMemberId.equals(child.getTag())) {
                        ((Chip) child).setChecked(true);
                        break;
                    }
                }
            });
        }).start();
    }

    @Override protected void onResume() {
        super.onResume();
        if (familyMemberChips != null && familyMemberChips.getChildCount() > 0) loadFamilyMembers();
    }

    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> saveExpense());
    }

    private void saveExpense() {
        if (saving) return;
        String enteredAmount = amountText.getText() == null ? "" : amountText.getText().toString()
                .replace(" ", "").replace("\u00a0", "").trim().replace(',', '.');
        double confirmedAmount;
        try {
            confirmedAmount = Double.parseDouble(enteredAmount);
        } catch (NumberFormatException error) {
            amountText.setError(getString(R.string.invalid_amount));
            return;
        }
        if (!Double.isFinite(confirmedAmount) || confirmedAmount <= 0) {
            amountText.setError(getString(R.string.positive_amount_error));
            return;
        }
        if (date <= 0) {
            Toast.makeText(this, R.string.date_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedCategoryId == null) {
            Toast.makeText(this, R.string.category_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String note = noteInput.getText() != null ? noteInput.getText().toString() : "";

        Expense expense = new Expense();
        expense.setAmount(confirmedAmount);
        expense.setCategoryId(selectedCategoryId);
        expense.setFamilyMemberId(selectedFamilyMemberId);
        String confirmedStore = ShopNameSuggestions.normalize(storeNameText.getText().toString());
        expense.setDescription(note.isEmpty() ? confirmedStore : note);
        expense.setTimestamp(date);
        expense.setReceiptUrl(receiptUrl);
        expense.setShopName(confirmedStore);
        expense.setReceiptItemsJson(receiptItemsJson);

        saving = true;
        saveButton.setEnabled(false);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                database.expenseDao().insert(expense);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.expense_saved, Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (RuntimeException error) {
                runOnUiThread(() -> {
                    saving = false;
                    saveButton.setEnabled(true);
                    Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
