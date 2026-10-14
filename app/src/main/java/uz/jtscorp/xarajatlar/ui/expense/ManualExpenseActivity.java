package uz.jtscorp.xarajatlar.ui.expense;

import uz.jtscorp.xarajatlar.utils.Formats;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;
import uz.jtscorp.xarajatlar.ui.common.SubmissionGate;
import uz.jtscorp.xarajatlar.ui.settings.ManageFamilyMembersActivity;

public class ManualExpenseActivity extends AppCompatActivity {

    public static final String EXTRA_EXPENSE_ID = "expense_id";

    private TextInputEditText amountInput;
    private TextInputEditText noteInput;
    private AutoCompleteTextView shopNameInput;
    private RecyclerView categoryGrid;
    private ChipGroup familyMemberChips;
    private ChipGroup paymentMethodChips;
    private MaterialButton dateButton;
    private MaterialButton saveButton;

    private CategoryAdapter categoryAdapter;
    private AppDatabase database;

    private Integer selectedCategoryId = null;
    private Integer selectedFamilyMemberId = null;
    private Integer selectedPaymentMethodId = null;
    private long selectedTimestamp = System.currentTimeMillis();
    private int editExpenseId = -1;
    private Expense existingExpense;
    private boolean isLoadingExistingExpense;
    private final SubmissionGate submissionGate = new SubmissionGate();

    private final DecimalFormat formatter;

    public ManualExpenseActivity() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        formatter = new DecimalFormat("#,###", symbols);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_expense);

        database = AppDatabase.getDatabase(this);

        initViews();
        setupToolbar();
        setupAmountInput();
        setupDateButton();
        loadCategories();
        loadFamilyMembers();
        loadPaymentMethods();
        loadShopNames();
        setupSaveButton();

        // Check if editing existing expense
        editExpenseId = getIntent().getIntExtra(EXTRA_EXPENSE_ID, -1);
        if (editExpenseId != -1) {
            isLoadingExistingExpense = true;
            saveButton.setEnabled(false);
            loadExistingExpense();
        } else {
            // Accept amount from OCR scanner
            double ocrAmount = getIntent().getDoubleExtra("amount", 0);
            if (ocrAmount > 0) {
                amountInput.setText(String.valueOf((long) ocrAmount));
            }
        }
    }

    private void loadExistingExpense() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.expense_edit_title);
        saveButton.setText(R.string.update);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            Expense expense = database.expenseDao().getById(editExpenseId);
            if (expense == null) {
                if (!isFinishing() && !isDestroyed()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.expense_not_found, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
                return;
            }

            if (!isFinishing() && !isDestroyed()) {
                runOnUiThread(() -> {
                    existingExpense = expense;
                    isLoadingExistingExpense = false;
                    saveButton.setEnabled(true);
                    // Set amount
                    amountInput.setText(String.valueOf((long) expense.getAmount()));

                    // Set note
                    if (expense.getDescription() != null) {
                        noteInput.setText(expense.getDescription());
                    }
                    shopNameInput.setText(expense.getShopName());

                    // Pre-select category
                    selectedCategoryId = expense.getCategoryId();
                    if (selectedCategoryId != null && categoryAdapter != null) {
                        categoryAdapter.setSelectedCategoryId(selectedCategoryId);
                    }

                    // Pre-select family member
                    selectedFamilyMemberId = expense.getFamilyMemberId();
                    selectChip(familyMemberChips, selectedFamilyMemberId);
                    selectedPaymentMethodId = expense.getPaymentMethodId();
                    selectChip(paymentMethodChips, selectedPaymentMethodId);
                    selectedTimestamp = expense.getTimestamp();
                    updateDateButtonText();
                });
            }
        });
    }

    private void initViews() {
        amountInput = findViewById(R.id.amountInput);
        ((android.widget.TextView) findViewById(R.id.amountCurrencyText))
                .setText(uz.jtscorp.xarajatlar.utils.CurrencyLabel.current(this));
        noteInput = findViewById(R.id.noteInput);
        shopNameInput = findViewById(R.id.shopNameInput);
        categoryGrid = findViewById(R.id.categoryGrid);
        familyMemberChips = findViewById(R.id.familyMemberChips);
        paymentMethodChips = findViewById(R.id.paymentMethodChips);
        dateButton = findViewById(R.id.dateButton);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupAmountInput() {
        amountInput.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    amountInput.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[^\\d]", "");

                    if (!cleanString.isEmpty()) {
                        try {
                            long parsed = Long.parseLong(cleanString);
                            String formatted = formatter.format(parsed);
                            current = formatted;
                            amountInput.setText(formatted);
                            amountInput.setSelection(formatted.length());
                        } catch (NumberFormatException error) {
                            current = "";
                            amountInput.setText("");
                            amountInput.setError(getString(R.string.amount_too_large));
                        }
                    } else {
                        current = "";
                    }

                    amountInput.addTextChangedListener(this);
                }
            }
        });

        // Request focus and show keyboard
        amountInput.requestFocus();
    }

    private void setupDateButton() {
        updateDateButtonText();
        dateButton.setOnClickListener(v -> {
            Calendar selected = Calendar.getInstance();
            selected.setTimeInMillis(selectedTimestamp);
            new DatePickerDialog(this, (view, year, month, day) -> {
                Calendar next = Calendar.getInstance();
                next.setTimeInMillis(selectedTimestamp);
                next.set(year, month, day);
                selectedTimestamp = next.getTimeInMillis();
                updateDateButtonText();
            }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH),
                    selected.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void updateDateButtonText() {
        dateButton.setText(Formats.date(selectedTimestamp));
    }

    private void loadCategories() {
        categoryGrid.setLayoutManager(new GridLayoutManager(this, 4));

        new Thread(() -> {
            List<Category> categories = database.categoryDao().getAllSync();

            if (!isFinishing() && !isDestroyed()) {
                runOnUiThread(() -> {
                    categoryAdapter = new CategoryAdapter(categories, category -> {
                        selectedCategoryId = category.getId();
                    });
                    categoryGrid.setAdapter(categoryAdapter);

                    // Pre-select category if editing
                    if (selectedCategoryId != null) {
                        categoryAdapter.setSelectedCategoryId(selectedCategoryId);
                    }
                });
            }
        }).start();
    }

    private void loadFamilyMembers() {
        new Thread(() -> {
            List<FamilyMember> members = database.familyMemberDao().getAllSync();

            if (isFinishing() || isDestroyed()) return;
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
                selectChip(familyMemberChips, selectedFamilyMemberId);
            });
        }).start();
    }

    @Override protected void onResume() {
        super.onResume();
        if (familyMemberChips != null && familyMemberChips.getChildCount() > 0) loadFamilyMembers();
    }

    private void loadPaymentMethods() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<PaymentMethod> methods = database.paymentMethodDao().getAllSync();
            if (isFinishing() || isDestroyed()) return;
            runOnUiThread(() -> {
                for (PaymentMethod method : methods) {
                    Chip chip = new Chip(this);
                    chip.setText(getString(R.string.payment_method_chip,
                            method.isCash() ? "💵" : "💳", method.getName()));
                    chip.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
                    chip.setCheckable(true);
                    chip.setTag(method.getId());
                    chip.setOnCheckedChangeListener((button, checked) -> {
                        if (checked) selectedPaymentMethodId = (Integer) button.getTag();
                    });
                    paymentMethodChips.addView(chip);
                }
                selectChip(paymentMethodChips, selectedPaymentMethodId);
            });
        });
    }

    private void loadShopNames() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<String> shops = ShopNameSuggestions.unique(database.expenseDao().getShopNames());
            if (!isFinishing() && !isDestroyed()) runOnUiThread(() -> {
                ArrayAdapter<String> suggestions = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, shops);
                shopNameInput.setAdapter(suggestions);
                shopNameInput.setThreshold(1);
            });
        });
    }

    private static void selectChip(ChipGroup group, Integer id) {
        if (id == null) return;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip && id.equals(child.getTag())) {
                ((Chip) child).setChecked(true);
                return;
            }
        }
    }

    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> saveExpense());
    }

    private void saveExpense() {
        if (isLoadingExistingExpense || (editExpenseId != -1 && existingExpense == null)) {
            Toast.makeText(this, R.string.expense_loading, Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = amountInput.getText() != null ?
                amountInput.getText().toString().replaceAll("[^\\d]", "") : "";

        if (amountStr.isEmpty()) {
            Toast.makeText(this, R.string.amount_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategoryId == null) {
            Toast.makeText(this, R.string.category_required, Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException error) {
            amountInput.setError(getString(R.string.invalid_amount));
            return;
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            amountInput.setError(getString(R.string.positive_amount_error));
            return;
        }
        String note = noteInput.getText() != null ? noteInput.getText().toString() : "";
        String shopName = ShopNameSuggestions.normalize(shopNameInput.getText().toString());
        if (!submissionGate.tryAcquire()) return;
        saveButton.setEnabled(false);

        if (editExpenseId != -1) {
            applyEditableFields(existingExpense, amount, selectedCategoryId, selectedFamilyMemberId,
                    selectedPaymentMethodId, selectedTimestamp, note);
            existingExpense.setShopName(shopName);

            AppDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    database.expenseDao().update(existingExpense);
                    showSaved(getString(R.string.expense_updated));
                } catch (RuntimeException error) { showSaveError(); }
            });
        } else {
            Expense expense = new Expense();
            expense.setAmount(amount);
            expense.setCategoryId(selectedCategoryId);
            expense.setFamilyMemberId(selectedFamilyMemberId);
            expense.setPaymentMethodId(selectedPaymentMethodId);
            expense.setDescription(note);
            expense.setShopName(shopName);
            expense.setTimestamp(selectedTimestamp);

            AppDatabase.databaseWriteExecutor.execute(() -> {
                try {
                    database.expenseDao().insert(expense);
                    showSaved(getString(R.string.expense_saved));
                } catch (RuntimeException error) { showSaveError(); }
            });
        }
    }

    private void showSaved(String message) {
        if (!isFinishing() && !isDestroyed()) runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void showSaveError() {
        submissionGate.release();
        if (!isFinishing() && !isDestroyed()) runOnUiThread(() -> {
            saveButton.setEnabled(true);
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
        });
    }

    static void applyEditableFields(Expense expense, double amount, Integer categoryId,
                                    Integer familyMemberId, Integer paymentMethodId,
                                    long timestamp, String description) {
        expense.setAmount(amount);
        expense.setCategoryId(categoryId);
        expense.setFamilyMemberId(familyMemberId);
        expense.setPaymentMethodId(paymentMethodId);
        expense.setTimestamp(timestamp);
        expense.setDescription(description);
    }
}
