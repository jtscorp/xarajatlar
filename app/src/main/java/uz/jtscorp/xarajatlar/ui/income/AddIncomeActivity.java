package uz.jtscorp.xarajatlar.ui.income;

import uz.jtscorp.xarajatlar.utils.Formats;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.FamilyMember;
import uz.jtscorp.xarajatlar.data.entity.Income;
import uz.jtscorp.xarajatlar.data.entity.IncomeCategory;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;
import uz.jtscorp.xarajatlar.ui.common.EditLoadBarrier;
import uz.jtscorp.xarajatlar.ui.common.SubmissionGate;

public class AddIncomeActivity extends AppCompatActivity {

    public static final String EXTRA_INCOME_ID = "extra_income_id";

    private TextInputEditText amountInput;
    private Spinner categorySpinner;
    private ChipGroup familyMemberChips;
    private ChipGroup paymentMethodChips;
    private MaterialButton dateButton;
    private TextInputEditText noteInput;
    private MaterialButton saveButton;

    private AppDatabase database;
    private final DecimalFormat formatter;

    private List<IncomeCategory> incomeCategories = new ArrayList<>();
    private Integer selectedFamilyMemberId = null;
    private Integer selectedPaymentMethodId = null;
    private long selectedTimestamp = System.currentTimeMillis();
    private int editIncomeId = -1;
    private Income existingIncome;
    private boolean loadingExisting;
    private final EditLoadBarrier editLoadBarrier = new EditLoadBarrier();
    private final SubmissionGate submissionGate = new SubmissionGate();

    public AddIncomeActivity() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        formatter = new DecimalFormat("#,###", symbols);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_income);

        database = AppDatabase.getDatabase(this);

        editIncomeId = getIntent().getIntExtra(EXTRA_INCOME_ID, -1);

        initViews();
        if (editIncomeId > -1) {
            loadingExisting = true;
            saveButton.setText(R.string.update);
            setFormEnabled(false);
        }
        setupToolbar();
        setupAmountInput();
        setupDateButton();
        loadCategories();
        loadFamilyMembers();
        loadPaymentMethods();
        setupSaveButton();

        if (editIncomeId > -1) {
            loadExistingIncome();
        }
    }

    private void initViews() {
        amountInput = findViewById(R.id.amountInput);
        ((android.widget.TextView) findViewById(R.id.amountCurrencyText))
                .setText(uz.jtscorp.xarajatlar.utils.CurrencyLabel.current(this));
        categorySpinner = findViewById(R.id.categorySpinner);
        familyMemberChips = findViewById(R.id.familyMemberChips);
        paymentMethodChips = findViewById(R.id.paymentMethodChips);
        dateButton = findViewById(R.id.dateButton);
        noteInput = findViewById(R.id.noteInput);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (editIncomeId > -1) {
            toolbar.setTitle(R.string.income_edit_title);
        }
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

        amountInput.requestFocus();
    }

    private void setupDateButton() {
        updateDateButtonText();
        dateButton.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedTimestamp);

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.setTimeInMillis(selectedTimestamp);
                    selected.set(year, month, dayOfMonth);
                    selectedTimestamp = selected.getTimeInMillis();
                    updateDateButtonText();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void updateDateButtonText() {
        dateButton.setText(Formats.date(selectedTimestamp));
    }

    private void loadCategories() {
        new Thread(() -> {
            List<IncomeCategory> categories = database.incomeCategoryDao().getAllSync();

            if (isFinishing() || isDestroyed()) return;
            runOnUiThread(() -> {
                incomeCategories = categories;

                List<String> displayNames = new ArrayList<>();
                for (IncomeCategory category : categories) {
                    String emoji = category.getEmoji() != null ? category.getEmoji() : "";
                    displayNames.add(emoji + " " + category.getName());
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        displayNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setAdapter(adapter);
                markEditPartLoaded(EditLoadBarrier.CATEGORIES);
            });
        }).start();
    }

    private void loadFamilyMembers() {
        new Thread(() -> {
            List<FamilyMember> members = database.familyMemberDao().getAllSync();

            if (isFinishing() || isDestroyed()) return;
            runOnUiThread(() -> {
                for (FamilyMember member : members) {
                    Chip chip = new Chip(this);
                    chip.setText(member.getName());
                    chip.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
                    chip.setCheckable(true);
                    chip.setTag(member.getId());
                    chip.setEnabled(!loadingExisting);

                    chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            selectedFamilyMemberId = (Integer) buttonView.getTag();
                        }
                    });

                    familyMemberChips.addView(chip);
                }
                markEditPartLoaded(EditLoadBarrier.FAMILY_MEMBERS);
            });
        }).start();
    }

    private void loadPaymentMethods() {
        new Thread(() -> {
            List<PaymentMethod> methods = database.paymentMethodDao().getAllSync();

            if (isFinishing() || isDestroyed()) return;
            runOnUiThread(() -> {
                for (PaymentMethod method : methods) {
                    Chip chip = new Chip(this);
                    String pmEmoji = method.isCash() ? "\uD83D\uDCB5" : "\uD83D\uDCB3";
                    chip.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
                    String pmLabel = pmEmoji + " " + method.getName();
                    if (method.getLastFourDigits() != null && !method.getLastFourDigits().isEmpty()) {
                        pmLabel += " *" + method.getLastFourDigits();
                    }
                    chip.setText(pmLabel);
                    chip.setCheckable(true);
                    chip.setTag(method.getId());
                    chip.setEnabled(!loadingExisting);

                    chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            selectedPaymentMethodId = (Integer) buttonView.getTag();
                        }
                    });

                    paymentMethodChips.addView(chip);
                }
                markEditPartLoaded(EditLoadBarrier.PAYMENT_METHODS);
            });
        }).start();
    }

    private void loadExistingIncome() {
        new Thread(() -> {
            Income income = database.incomeDao().getById(editIncomeId);
            if (income == null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.income_not_found, Toast.LENGTH_SHORT).show();
                    finish();
                });
                return;
            }

            if (isFinishing() || isDestroyed()) return;
            runOnUiThread(() -> {
                existingIncome = income;
                markEditPartLoaded(EditLoadBarrier.ENTITY);
            });
        }).start();
    }

    private void markEditPartLoaded(int part) {
        if (editIncomeId < 0 || !editLoadBarrier.markComplete(part)) return;
        renderExistingIncome();
        loadingExisting = false;
        setFormEnabled(true);
    }

    private void setFormEnabled(boolean enabled) {
        amountInput.setEnabled(enabled);
        categorySpinner.setEnabled(enabled);
        familyMemberChips.setEnabled(enabled);
        paymentMethodChips.setEnabled(enabled);
        dateButton.setEnabled(enabled);
        noteInput.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        setChildrenEnabled(familyMemberChips, enabled);
        setChildrenEnabled(paymentMethodChips, enabled);
    }

    private static void setChildrenEnabled(ChipGroup group, boolean enabled) {
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(enabled);
        }
    }

    private void renderExistingIncome() {
        if (existingIncome == null) return;
        if (existingIncome.getAmount() > 0 && (amountInput.getText() == null
                || amountInput.getText().length() == 0)) {
            amountInput.setText(formatter.format((long) existingIncome.getAmount()));
        }
        Integer categoryId = existingIncome.getIncomeCategoryId();
        if (categoryId != null) {
            for (int i = 0; i < incomeCategories.size(); i++) {
                if (categoryId.equals(incomeCategories.get(i).getId())) {
                    categorySpinner.setSelection(i);
                    break;
                }
            }
        }
        selectedFamilyMemberId = existingIncome.getFamilyMemberId();
        selectedPaymentMethodId = existingIncome.getPaymentMethodId();
        selectChip(familyMemberChips, selectedFamilyMemberId);
        selectChip(paymentMethodChips, selectedPaymentMethodId);
        selectedTimestamp = existingIncome.getTimestamp();
        updateDateButtonText();
        if (noteInput.getText() == null || noteInput.getText().length() == 0) {
            String note = existingIncome.getDescription();
            if (note == null || note.isEmpty()) note = existingIncome.getSource();
            if (note != null) noteInput.setText(note);
        }
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
        saveButton.setOnClickListener(v -> saveIncome());
    }

    private void saveIncome() {
        if (loadingExisting || (editIncomeId > -1 && existingIncome == null)) {
            Toast.makeText(this, R.string.income_loading, Toast.LENGTH_SHORT).show();
            return;
        }
        String amountStr = amountInput.getText() != null ?
                amountInput.getText().toString().replaceAll("[^\\d]", "") : "";

        if (amountStr.isEmpty()) {
            Toast.makeText(this, R.string.amount_required, Toast.LENGTH_SHORT).show();
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

        // Get selected category
        Integer selectedCategoryId = null;
        int spinnerPosition = categorySpinner.getSelectedItemPosition();
        if (spinnerPosition >= 0 && spinnerPosition < incomeCategories.size()) {
            selectedCategoryId = incomeCategories.get(spinnerPosition).getId();
        }
        if (selectedCategoryId == null) {
            Toast.makeText(this, R.string.category_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!submissionGate.tryAcquire()) return;
        saveButton.setEnabled(false);
        Income income = editIncomeId > -1 ? existingIncome : new Income();
        applyEditableFields(income, amount, selectedCategoryId, selectedFamilyMemberId,
                selectedPaymentMethodId, selectedTimestamp, note);
        if (editIncomeId == -1) income.setSource(note);

        final boolean isEdit = editIncomeId > -1;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                if (isEdit) database.incomeDao().update(income);
                else database.incomeDao().insert(income);
                if (!isFinishing() && !isDestroyed()) runOnUiThread(() -> {
                    Toast.makeText(this, isEdit ? R.string.income_updated : R.string.income_saved,
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (RuntimeException error) {
                submissionGate.release();
                if (!isFinishing() && !isDestroyed()) runOnUiThread(() -> {
                    saveButton.setEnabled(true);
                    Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    static void applyEditableFields(Income income, double amount, Integer categoryId,
                                    Integer familyMemberId, Integer paymentMethodId,
                                    long timestamp, String description) {
        income.setAmount(amount);
        income.setIncomeCategoryId(categoryId);
        income.setFamilyMemberId(familyMemberId);
        income.setPaymentMethodId(paymentMethodId);
        income.setTimestamp(timestamp);
        income.setDescription(description);
    }
}
