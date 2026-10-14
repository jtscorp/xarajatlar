package uz.jtscorp.xarajatlar.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.IncomeCategory;

public class ManageIncomeCategoriesActivity extends AppCompatActivity {

    private static final String[] INCOME_EMOJIS = {
            "\uD83D\uDCB0", "\uD83C\uDF81", "\uD83D\uDCBB", "\uD83C\uDFE0",
            "\uD83D\uDCE6", "\uD83D\uDCB3", "\uD83D\uDCBC", "\uD83C\uDFE6",
            "\uD83D\uDCC8", "\uD83D\uDED2", "\uD83C\uDFB5", "\uD83D\uDCF1",
            "✈️", "\uD83D\uDD27", "\uD83C\uDFAC", "⚽"
    };

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private IncomeCategoryAdapter adapter;
    private AppDatabase database;
    private int selectedEmojiIndex = 0;
    private String selectedEmoji = INCOME_EMOJIS[0];

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
        toolbar.setTitle(R.string.ui_daromad_kategoriyalari);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);

        adapter = new IncomeCategoryAdapter(
                category -> showEditDialog(category),
                category -> showDeleteConfirmDialog(category)
        );

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showAddDialog());
    }

    private void observeData() {
        database.incomeCategoryDao().getAll().observe(this, categories -> {
            adapter.submitList(categories);
            emptyState.setVisibility(categories.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(categories.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void showAddDialog() {
        selectedEmojiIndex = 0;
        selectedEmoji = INCOME_EMOJIS[0];
        showCategoryDialog(null);
    }

    private void showEditDialog(IncomeCategory category) {
        // Find emoji index
        String emoji = category.getEmoji();
        if (emoji != null && !emoji.isEmpty()) {
            selectedEmoji = emoji;
            for (int i = 0; i < INCOME_EMOJIS.length; i++) {
                if (INCOME_EMOJIS[i].equals(emoji)) {
                    selectedEmojiIndex = i;
                    break;
                }
            }
        } else {
            selectedEmojiIndex = 0;
            selectedEmoji = INCOME_EMOJIS[0];
        }
        showCategoryDialog(category);
    }

    private void showCategoryDialog(IncomeCategory existingCategory) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category, null);
        TextInputLayout inputLayout = dialogView.findViewById(R.id.textInputLayout);
        TextInputEditText editText = dialogView.findViewById(R.id.editText);
        RecyclerView emojiPicker = dialogView.findViewById(R.id.emojiPicker);
        RecyclerView colorPicker = dialogView.findViewById(R.id.colorPicker);

        inputLayout.setHint(R.string.category_name_hint);
        if (existingCategory != null) {
            editText.setText(existingCategory.getName());
        }

        // Setup emoji picker with income emojis
        EmojiPickerAdapter emojiAdapter = new EmojiPickerAdapter(
                INCOME_EMOJIS,
                selectedEmojiIndex,
                (position, emoji) -> {
                    selectedEmojiIndex = position;
                    selectedEmoji = emoji;
                });
        emojiPicker.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        emojiPicker.setAdapter(emojiAdapter);

        // Hide color picker and its label
        colorPicker.setVisibility(View.GONE);

        dialogView.findViewById(R.id.colorLabel).setVisibility(View.GONE);

        int title = existingCategory == null ? R.string.category_new : R.string.ui_tahrirlash;
        int positiveButton = existingCategory == null ? R.string.ui_qoshish : R.string.ui_saqlash;

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(positiveButton, null)
                .setNegativeButton(R.string.cancel, null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = editText.getText() != null ? editText.getText().toString().trim() : "";
            if (name.isEmpty()) {
                inputLayout.setError(getString(R.string.name_required));
                return;
            }

            if (existingCategory == null) {
                IncomeCategory newCategory = new IncomeCategory();
                newCategory.setName(name);
                newCategory.setEmoji(selectedEmoji);
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    database.incomeCategoryDao().insert(newCategory);
                });
            } else {
                existingCategory.setName(name);
                existingCategory.setEmoji(selectedEmoji);
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    database.incomeCategoryDao().update(existingCategory);
                });
            }
            dialog.dismiss();
        });
    }

    private void showDeleteConfirmDialog(IncomeCategory category) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete)
                .setMessage(getString(R.string.delete_named_message, category.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        database.incomeCategoryDao().delete(category);
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
