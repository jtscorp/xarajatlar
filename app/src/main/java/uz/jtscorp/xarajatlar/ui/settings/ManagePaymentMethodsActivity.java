package uz.jtscorp.xarajatlar.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;

public class ManagePaymentMethodsActivity extends AppCompatActivity {

    private static final int[] METHOD_COLORS = {
            0xFF4CAF50, // Yashil - Naqd
            0xFF2196F3, // Ko'k - Karta
            0xFFFF9800, // Sariq
            0xFF9C27B0, // Binafsha
            0xFFE91E63, // Pushti
            0xFF00BCD4, // Cyan
            0xFF795548, // Jigar
            0xFF607D8B  // Kulrang
    };

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private PaymentMethodAdapter adapter;
    private AppDatabase database;
    private int selectedColorIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_list);

        database = AppDatabase.getDatabase(this);

        setupToolbar();
        setupRecyclerView();
        setupFab();
        observeData();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.ui_tolov_usullari);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);

        adapter = new PaymentMethodAdapter(
                method -> showEditDialog(method),
                method -> showDeleteConfirmDialog(method)
        );

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showAddDialog());
    }

    private void observeData() {
        database.paymentMethodDao().getAll().observe(this, methods -> {
            adapter.submitList(methods);
            emptyState.setVisibility(methods.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(methods.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void showAddDialog() {
        selectedColorIndex = 0;
        showMethodDialog(null);
    }

    private void showEditDialog(PaymentMethod method) {
        // Find color index
        for (int i = 0; i < METHOD_COLORS.length; i++) {
            if (METHOD_COLORS[i] == method.getColor()) {
                selectedColorIndex = i;
                break;
            }
        }
        showMethodDialog(method);
    }

    private void showMethodDialog(PaymentMethod existingMethod) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_method, null);
        TextInputLayout nameLayout = dialogView.findViewById(R.id.nameInputLayout);
        TextInputEditText nameEdit = dialogView.findViewById(R.id.nameEditText);
        TextInputLayout digitsLayout = dialogView.findViewById(R.id.digitsInputLayout);
        TextInputEditText digitsEdit = dialogView.findViewById(R.id.digitsEditText);
        RecyclerView colorPicker = dialogView.findViewById(R.id.colorPicker);
        Chip chipCash = dialogView.findViewById(R.id.chipCash);
        Chip chipCard = dialogView.findViewById(R.id.chipCard);

        if (existingMethod != null) {
            nameEdit.setText(existingMethod.getName());
            if (existingMethod.getLastFourDigits() != null) {
                digitsEdit.setText(existingMethod.getLastFourDigits());
            }
            if (existingMethod.isCash()) {
                chipCash.setChecked(true);
            } else {
                chipCard.setChecked(true);
            }
        }

        // Setup color picker
        ColorPickerAdapter colorAdapter = new ColorPickerAdapter(METHOD_COLORS, selectedColorIndex,
                position -> selectedColorIndex = position);
        colorPicker.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        colorPicker.setAdapter(colorAdapter);

        int title = existingMethod == null ? R.string.payment_method_new : R.string.ui_tahrirlash;
        int positiveButton = existingMethod == null ? R.string.ui_qoshish : R.string.ui_saqlash;

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(positiveButton, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String name = nameEdit.getText() != null ? nameEdit.getText().toString().trim() : "";
                    String digits = digitsEdit.getText() != null ? digitsEdit.getText().toString().trim() : "";

                    if (name.isEmpty()) {
                        nameLayout.setError(getString(R.string.name_required));
                        return;
                    }
                    nameLayout.setError(null);
                    if (!digits.isEmpty() && !digits.matches("[0-9]{4}")) {
                        digitsLayout.setError(getString(R.string.card_digits_error));
                        return;
                    }
                    digitsLayout.setError(null);

                    boolean isCash = chipCash.isChecked();

                    if (existingMethod == null) {
                        PaymentMethod newMethod = new PaymentMethod();
                        newMethod.setName(name);
                        newMethod.setLastFourDigits(digits.isEmpty() ? null : digits);
                        newMethod.setColor(METHOD_COLORS[selectedColorIndex]);
                        newMethod.setCash(isCash);
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            database.paymentMethodDao().insert(newMethod);
                        });
                    } else {
                        existingMethod.setName(name);
                        existingMethod.setLastFourDigits(digits.isEmpty() ? null : digits);
                        existingMethod.setColor(METHOD_COLORS[selectedColorIndex]);
                        existingMethod.setCash(isCash);
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            database.paymentMethodDao().update(existingMethod);
                        });
                    }
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void showDeleteConfirmDialog(PaymentMethod method) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete)
                .setMessage(getString(R.string.delete_named_message, method.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        database.paymentMethodDao().delete(method);
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
