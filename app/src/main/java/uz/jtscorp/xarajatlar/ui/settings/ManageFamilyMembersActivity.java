package uz.jtscorp.xarajatlar.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.FamilyMember;

public class ManageFamilyMembersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private FamilyMemberAdapter adapter;
    private AppDatabase database;
    private String selectedEmoji = "👤";

    private static final String[] FAMILY_EMOJIS = {
            "👨", "👩", "👦", "👧", "👴", "👵", "👶", "🧑", "👤"
    };

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
        toolbar.setTitle(R.string.ui_oila_azolari);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);

        adapter = new FamilyMemberAdapter(
                member -> showEditDialog(member),
                member -> showDeleteConfirmDialog(member)
        );

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showAddDialog());
    }

    private void observeData() {
        database.familyMemberDao().getAll().observe(this, members -> {
            adapter.submitList(members);
            emptyState.setVisibility(members.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(members.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void showAddDialog() {
        showMemberDialog(null);
    }

    private void showEditDialog(FamilyMember member) {
        showMemberDialog(member);
    }

    private void showMemberDialog(FamilyMember existingMember) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_family_member, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.nameInput);
        RecyclerView emojiRecyclerView = dialogView.findViewById(R.id.emojiRecyclerView);

        // Set initial emoji
        selectedEmoji = existingMember != null && existingMember.getEmoji() != null
                ? existingMember.getEmoji() : "👤";

        // Find initial position
        int initialPosition = 0;
        for (int i = 0; i < FAMILY_EMOJIS.length; i++) {
            if (FAMILY_EMOJIS[i].equals(selectedEmoji)) {
                initialPosition = i;
                break;
            }
        }

        // Setup emoji picker
        EmojiPickerAdapter emojiAdapter = new EmojiPickerAdapter(FAMILY_EMOJIS, initialPosition, (position, emoji) -> {
            selectedEmoji = emoji;
        });
        emojiRecyclerView.setLayoutManager(new GridLayoutManager(this, 5));
        emojiRecyclerView.setAdapter(emojiAdapter);

        if (existingMember != null) {
            nameInput.setText(existingMember.getName());
        }

        int title = existingMember == null ? R.string.family_member_new : R.string.ui_tahrirlash;
        int positiveButton = existingMember == null ? R.string.ui_qoshish : R.string.ui_saqlash;

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(positiveButton, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
                    if (name.isEmpty()) {
                        nameInput.setError(getString(R.string.name_required));
                        return;
                    }

                    if (existingMember == null) {
                        FamilyMember newMember = new FamilyMember();
                        newMember.setName(name);
                        newMember.setEmoji(selectedEmoji);
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            database.familyMemberDao().insert(newMember);
                        });
                    } else {
                        existingMember.setName(name);
                        existingMember.setEmoji(selectedEmoji);
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            database.familyMemberDao().update(existingMember);
                        });
                    }
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void showDeleteConfirmDialog(FamilyMember member) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete)
                .setMessage(getString(R.string.delete_named_message, member.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        database.familyMemberDao().delete(member);
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
