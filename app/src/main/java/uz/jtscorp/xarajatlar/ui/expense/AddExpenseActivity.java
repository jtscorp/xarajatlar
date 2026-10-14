package uz.jtscorp.xarajatlar.ui.expense;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.Category;
import uz.jtscorp.xarajatlar.data.entity.Expense;
import uz.jtscorp.xarajatlar.data.entity.FamilyMember;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;
import uz.jtscorp.xarajatlar.ui.camera.QrScanActivity;
import uz.jtscorp.xarajatlar.ui.camera.OcrScanActivity;
import java.util.ArrayList;
import java.util.List;

public class AddExpenseActivity extends AppCompatActivity {

    private static final int REQUEST_QR_SCAN = 1001;
    private static final int REQUEST_OCR_SCAN = 1002;

    private TextInputEditText amountInput, descriptionInput;
    private AutoCompleteTextView shopNameInput;
    private AutoCompleteTextView categorySpinner, familyMemberSpinner, paymentMethodSpinner;
    private MaterialButton scanQrButton, scanOcrButton, saveButton;
    private MaterialTextView receiptUrlText;

    private AppDatabase database;
    private List<Category> categories = new ArrayList<>();
    private List<FamilyMember> familyMembers = new ArrayList<>();
    private List<PaymentMethod> paymentMethods = new ArrayList<>();

    private Integer selectedCategoryId = null;
    private Integer selectedFamilyMemberId = null;
    private Integer selectedPaymentMethodId = null;
    private String receiptUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        database = AppDatabase.getDatabase(this);

        initViews();
        loadData();
        loadShopNames();
        setupListeners();
    }

    private void initViews() {
        amountInput = findViewById(R.id.amountInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        shopNameInput = findViewById(R.id.shopNameInput);
        categorySpinner = findViewById(R.id.categorySpinner);
        familyMemberSpinner = findViewById(R.id.familyMemberSpinner);
        paymentMethodSpinner = findViewById(R.id.paymentMethodSpinner);
        scanQrButton = findViewById(R.id.scanQrButton);
        scanOcrButton = findViewById(R.id.scanOcrButton);
        saveButton = findViewById(R.id.saveButton);
        receiptUrlText = findViewById(R.id.receiptUrlText);
    }

    private void loadData() {
        // Load categories
        database.categoryDao().getAllActive().observe(this, cats -> {
            categories = cats != null ? cats : new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (Category cat : categories) {
                String emoji = cat.getIconName();
                if (emoji != null && !emoji.isEmpty()) {
                    names.add(emoji + " " + cat.getName());
                } else {
                    names.add(cat.getName());
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, names);
            categorySpinner.setAdapter(adapter);
        });

        // Load family members
        database.familyMemberDao().getAllActive().observe(this, members -> {
            familyMembers = members != null ? members : new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (FamilyMember member : familyMembers) {
                String emoji = member.getEmoji();
                if (emoji != null && !emoji.isEmpty()) {
                    names.add(emoji + " " + member.getName());
                } else {
                    names.add(member.getName());
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, names);
            familyMemberSpinner.setAdapter(adapter);
        });

        // Load payment methods
        database.paymentMethodDao().getAll().observe(this, methods -> {
            paymentMethods = methods != null ? methods : new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (PaymentMethod method : paymentMethods) {
                String pmEmoji = method.isCash() ? "\uD83D\uDCB5" : "\uD83D\uDCB3";
                String pmName = pmEmoji + " " + method.getName();
                if (method.getLastFourDigits() != null && !method.getLastFourDigits().isEmpty()) {
                    pmName += " *" + method.getLastFourDigits();
                }
                names.add(pmName);
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, names);
            paymentMethodSpinner.setAdapter(adapter);
        });
    }

    private void loadShopNames() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<String> names = ShopNameSuggestions.unique(database.expenseDao().getShopNames());
            if (!isFinishing() && !isDestroyed()) runOnUiThread(() -> {
                shopNameInput.setAdapter(new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, names));
                shopNameInput.setThreshold(1);
            });
        });
    }

    private void setupListeners() {
        categorySpinner.setOnItemClickListener((parent, view, position, id) -> {
            if (position < categories.size()) {
                selectedCategoryId = categories.get(position).getId();
            }
        });

        familyMemberSpinner.setOnItemClickListener((parent, view, position, id) -> {
            if (position < familyMembers.size()) {
                selectedFamilyMemberId = familyMembers.get(position).getId();
            }
        });

        paymentMethodSpinner.setOnItemClickListener((parent, view, position, id) -> {
            if (position < paymentMethods.size()) {
                selectedPaymentMethodId = paymentMethods.get(position).getId();
            }
        });

        scanQrButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, QrScanActivity.class);
            startActivityForResult(intent, REQUEST_QR_SCAN);
        });

        scanOcrButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, OcrScanActivity.class);
            startActivityForResult(intent, REQUEST_OCR_SCAN);
        });

        saveButton.setOnClickListener(v -> saveExpense());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_QR_SCAN) {
                receiptUrl = data.getStringExtra("receipt_url");
                double amount = data.getDoubleExtra("amount", 0);
                String shopName = data.getStringExtra("shop_name");

                if (amount > 0) {
                    amountInput.setText(String.valueOf(amount));
                }
                if (shopName != null) {
                    shopNameInput.setText(shopName);
                }
                if (receiptUrl != null) {
                    receiptUrlText.setText(getString(R.string.receipt_link, receiptUrl));
                    receiptUrlText.setVisibility(android.view.View.VISIBLE);
                }
            } else if (requestCode == REQUEST_OCR_SCAN) {
                double amount = data.getDoubleExtra("amount", 0);
                if (amount > 0) {
                    amountInput.setText(String.valueOf((long) amount));
                }
            }
        }
    }

    private void saveExpense() {
        String amountStr = amountInput.getText() != null ? amountInput.getText().toString() : "";
        if (amountStr.isEmpty()) {
            amountInput.setError(getString(R.string.amount_required));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            amountInput.setError(getString(R.string.invalid_amount));
            return;
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            amountInput.setError(getString(R.string.positive_amount_error));
            return;
        }
        if (selectedCategoryId == null) {
            categorySpinner.setError(getString(R.string.category_required));
            return;
        }
        saveButton.setEnabled(false);

        Expense expense = new Expense();
        expense.setAmount(amount);
        expense.setCategoryId(selectedCategoryId);
        expense.setFamilyMemberId(selectedFamilyMemberId);
        expense.setPaymentMethodId(selectedPaymentMethodId);

        String description = descriptionInput.getText() != null ? descriptionInput.getText().toString() : "";
        expense.setDescription(description);

        String shopName = shopNameInput.getText() != null
                ? ShopNameSuggestions.normalize(shopNameInput.getText().toString()) : "";
        expense.setShopName(shopName);

        expense.setReceiptUrl(receiptUrl);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                database.expenseDao().insert(expense);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.expense_saved, Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (RuntimeException error) {
                runOnUiThread(() -> {
                    saveButton.setEnabled(true);
                    Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
