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
import uz.jtscorp.xarajatlar.data.entity.Category;

public class ManageCategoriesActivity extends AppCompatActivity {

    private static final int[] CATEGORY_COLORS = {
            0xFF4CAF50, // Yashil - Oziq-ovqat
            0xFF2196F3, // Ko'k - Transport
            0xFFFF9800, // Sariq - Uy-joy
            0xFF9C27B0, // Binafsha - Kiyim
            0xFFE91E63, // Pushti - Sog'liq
            0xFF00BCD4, // Cyan - Ta'lim
            0xFF795548, // Jigar - Ko'ngilochar
            0xFF607D8B  // Kulrang - Boshqa
    };

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private CategoryAdapter adapter;
    private AppDatabase database;
    private int selectedColorIndex = 0;
    private int selectedEmojiIndex = 0;
    private String selectedEmoji = EmojiPickerAdapter.CATEGORY_EMOJIS[0];

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
        toolbar.setTitle(R.string.ui_kategoriyalar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);

        adapter = new CategoryAdapter(
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
        database.categoryDao().getAll().observe(this, categories -> {
            adapter.submitList(categories);
            emptyState.setVisibility(categories.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(categories.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void showAddDialog() {
        selectedColorIndex = 0;
        selectedEmojiIndex = 0;
        selectedEmoji = EmojiPickerAdapter.CATEGORY_EMOJIS[0];
        showCategoryDialog(null);
    }

    private void showEditDialog(Category category) {
        // Find color index
        for (int i = 0; i < CATEGORY_COLORS.length; i++) {
            if (CATEGORY_COLORS[i] == category.getColor()) {
                selectedColorIndex = i;
                break;
            }
        }
        // Find emoji index
        String iconName = category.getIconName();
        if (iconName != null && !iconName.isEmpty()) {
            selectedEmoji = iconName;
            for (int i = 0; i < EmojiPickerAdapter.CATEGORY_EMOJIS.length; i++) {
                if (EmojiPickerAdapter.CATEGORY_EMOJIS[i].equals(iconName)) {
                    selectedEmojiIndex = i;
                    break;
                }
            }
        } else {
            selectedEmojiIndex = 0;
            selectedEmoji = EmojiPickerAdapter.CATEGORY_EMOJIS[0];
        }
        showCategoryDialog(category);
    }

    private void showCategoryDialog(Category existingCategory) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category, null);
        TextInputLayout inputLayout = dialogView.findViewById(R.id.textInputLayout);
        TextInputEditText editText = dialogView.findViewById(R.id.editText);
        RecyclerView emojiPicker = dialogView.findViewById(R.id.emojiPicker);
        RecyclerView colorPicker = dialogView.findViewById(R.id.colorPicker);

        inputLayout.setHint(R.string.category_name_hint);
        if (existingCategory != null) {
            editText.setText(existingCategory.getName());
        }

        // Setup emoji picker
        EmojiPickerAdapter emojiAdapter = new EmojiPickerAdapter(
                EmojiPickerAdapter.CATEGORY_EMOJIS,
                selectedEmojiIndex,
                (position, emoji) -> {
                    selectedEmojiIndex = position;
                    selectedEmoji = emoji;
                });
        emojiPicker.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        emojiPicker.setAdapter(emojiAdapter);

        // Setup color picker
        ColorPickerAdapter colorAdapter = new ColorPickerAdapter(CATEGORY_COLORS, selectedColorIndex,
                position -> selectedColorIndex = position);
        colorPicker.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        colorPicker.setAdapter(colorAdapter);

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
                Category newCategory = new Category();
                newCategory.setName(name);
                newCategory.setIconName(selectedEmoji);
                newCategory.setColor(CATEGORY_COLORS[selectedColorIndex]);
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    database.categoryDao().insert(newCategory);
                });
            } else {
                existingCategory.setName(name);
                existingCategory.setIconName(selectedEmoji);
                existingCategory.setColor(CATEGORY_COLORS[selectedColorIndex]);
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    database.categoryDao().update(existingCategory);
                });
            }
            dialog.dismiss();
        });
    }

    private void showDeleteConfirmDialog(Category category) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete)
                .setMessage(getString(R.string.delete_named_message, category.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        database.categoryDao().delete(category);
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
