package uz.jtscorp.xarajatlar.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.AppSettings;

import androidx.documentfile.provider.DocumentFile;
import uz.jtscorp.xarajatlar.utils.AppLanguage;
import uz.jtscorp.xarajatlar.utils.AutoBackupState;
import uz.jtscorp.xarajatlar.utils.AutoBackupWorker;
import uz.jtscorp.xarajatlar.utils.BackupFolder;
import uz.jtscorp.xarajatlar.utils.Formats;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.provider.Settings;
import androidx.core.content.FileProvider;
import java.io.File;
import uz.jtscorp.xarajatlar.utils.CurrencyLabel;
import android.text.InputFilter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import uz.jtscorp.xarajatlar.utils.DataExportImport;

public class SettingsFragment extends Fragment {

    private SwitchMaterial biometricSwitch;
    private TextView salaryDayText;
    private MaterialCardView salaryDayCard;
    private MaterialCardView familyMembersCard, categoriesCard, incomeCategoriesCard, paymentMethodsCard;
    private MaterialCardView exportCard, importCard;
    private AppDatabase database;
    private AppSettings currentSettings;
    private DataExportImport dataExportImport;
    private AlertDialog progressDialog;
    private MaterialCardView backupFolderCard;
    private TextView backupFolderText;
    private MaterialCardView languageCard;
    private TextView languageText;
    private MaterialCardView currencyCard;
    private TextView currencyText;
    private SwitchMaterial autoBackupSwitch;
    private TextView autoBackupStatusText;

    private static final String[] CURRENCY_PRESETS = {"$", "€", "₽"};

    private static final String PREFS_NAME = DataExportImport.PREFS_NAME;
    private static final String KEY_BACKUP_FOLDER_URI = DataExportImport.KEY_BACKUP_FOLDER_URI;

    private final ActivityResultLauncher<Intent> folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                        requireContext().getContentResolver().takePersistableUriPermission(uri, flags);
                        requireContext().getSharedPreferences(PREFS_NAME, 0)
                                .edit()
                                .putString(KEY_BACKUP_FOLDER_URI, uri.toString())
                                .apply();
                        updateBackupFolderDisplay();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> importFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        showImportConfirmDialog(uri);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        database = AppDatabase.getDatabase(requireContext());
        dataExportImport = new DataExportImport(requireContext());

        biometricSwitch = view.findViewById(R.id.biometricSwitch);
        salaryDayText = view.findViewById(R.id.salaryDayText);
        salaryDayCard = view.findViewById(R.id.salaryDayCard);
        familyMembersCard = view.findViewById(R.id.familyMembersCard);
        categoriesCard = view.findViewById(R.id.categoriesCard);
        incomeCategoriesCard = view.findViewById(R.id.incomeCategoriesCard);
        paymentMethodsCard = view.findViewById(R.id.paymentMethodsCard);
        exportCard = view.findViewById(R.id.exportCard);
        importCard = view.findViewById(R.id.importCard);
        backupFolderCard = view.findViewById(R.id.backupFolderCard);
        backupFolderText = view.findViewById(R.id.backupFolderText);
        languageCard = view.findViewById(R.id.languageCard);
        languageText = view.findViewById(R.id.languageText);
        String[] languageNames = getResources().getStringArray(R.array.language_names);
        languageText.setText(languageNames[AppLanguage.indexOf(AppLanguage.current(requireContext()))]);
        currencyCard = view.findViewById(R.id.currencyCard);
        currencyText = view.findViewById(R.id.currencyText);
        currencyText.setText(CurrencyLabel.current(requireContext()));
        autoBackupSwitch = view.findViewById(R.id.autoBackupSwitch);
        autoBackupStatusText = view.findViewById(R.id.autoBackupStatusText);
        // Listener boshlang'ich holat qo'yilgandan keyin ulanadi.
        autoBackupSwitch.setChecked(AutoBackupState.isEnabled(requireContext()));
        autoBackupSwitch.setOnCheckedChangeListener((button, checked) -> {
            AutoBackupState.setEnabled(requireContext(), checked);
            AutoBackupWorker.sync(requireContext());
            updateAutoBackupStatus();
            if (checked && needsBackupFolderAccess()) showBackupPermissionDialog();
        });
        view.findViewById(R.id.autoBackupCard).setOnClickListener(v -> {
            if (autoBackupSwitch.isChecked() && needsBackupFolderAccess()) showBackupPermissionDialog();
        });

        TextView appVersionText = view.findViewById(R.id.appVersionText);
        try {
            String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            appVersionText.setText(getString(R.string.app_version, versionName));
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            appVersionText.setVisibility(View.GONE);
        }

        setupBiometric();
        loadSettings();
        setupCardListeners();
        updateBackupFolderDisplay();
    }

    private void setupBiometric() {
        BiometricManager biometricManager = BiometricManager.from(requireContext());
        boolean canUseBiometric = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS;

        biometricSwitch.setEnabled(canUseBiometric);
        if (!canUseBiometric) {
            biometricSwitch.setChecked(false);
        }

        biometricSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentSettings != null) {
                currentSettings.setBiometricEnabled(isChecked);
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    database.appSettingsDao().update(currentSettings);
                });
            }
        });
    }

    private void loadSettings() {
        database.appSettingsDao().getSettings().observe(getViewLifecycleOwner(), settings -> {
            if (settings != null) {
                currentSettings = settings;
                biometricSwitch.setChecked(settings.isBiometricEnabled());
                salaryDayText.setText(getString(R.string.salary_day_summary, settings.getSalaryDay()));
            }
        });
    }

    private void setupCardListeners() {
        salaryDayCard.setOnClickListener(v -> {
            showSalaryDayDialog();
        });

        familyMembersCard.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ManageFamilyMembersActivity.class));
        });

        categoriesCard.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ManageCategoriesActivity.class));
        });

        incomeCategoriesCard.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ManageIncomeCategoriesActivity.class));
        });

        paymentMethodsCard.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ManagePaymentMethodsActivity.class));
        });

        exportCard.setOnClickListener(v -> {
            showExportConfirmDialog();
        });

        backupFolderCard.setOnClickListener(v -> {
            showBackupFolderDialog();
        });

        importCard.setOnClickListener(v -> {
            openFilePicker();
        });

        languageCard.setOnClickListener(v -> showLanguageDialog());
        currencyCard.setOnClickListener(v -> showCurrencyDialog());
    }

    private void showSalaryDayDialog() {
        String[] days = new String[31];
        for (int i = 0; i < 31; i++) {
            days[i] = String.valueOf(i + 1);
        }

        int currentDay = currentSettings != null ? currentSettings.getSalaryDay() - 1 : 0;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ui_oylik_kuni)
                .setSingleChoiceItems(days, currentDay, (dialog, which) -> {
                    int selectedDay = which + 1;
                    if (currentSettings != null) {
                        currentSettings.setSalaryDay(selectedDay);
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            database.appSettingsDao().update(currentSettings);
                            requireActivity().runOnUiThread(() -> {
                                salaryDayText.setText(getString(R.string.salary_day_summary, selectedDay));
                            });
                        });
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tizim sozlamalaridan qaytganda ruxsat holati o'zgargan bo'lishi mumkin.
        if (autoBackupStatusText != null) updateAutoBackupStatus();
    }

    /** Standart "backups" papkasi ishlatilayotgan va unga ruxsat berilmagan holat. */
    private boolean needsBackupFolderAccess() {
        boolean customFolder = requireContext().getSharedPreferences(PREFS_NAME, 0)
                .getString(KEY_BACKUP_FOLDER_URI, null) != null;
        return !customFolder && !BackupFolder.hasAccess();
    }

    private void updateAutoBackupStatus() {
        Context context = requireContext();
        String status;
        String error = AutoBackupState.lastError(context);
        long lastSuccess = AutoBackupState.lastSuccessTime(context);
        if (!AutoBackupState.isEnabled(context)) {
            status = getString(R.string.auto_backup_off);
        } else if (needsBackupFolderAccess()) {
            status = getString(R.string.auto_backup_permission_needed);
        } else if (error != null) {
            status = getString(R.string.auto_backup_last_error, error);
        } else if (lastSuccess > 0) {
            status = getString(R.string.auto_backup_last_success, Formats.dateTime(lastSuccess));
        } else {
            status = getString(R.string.auto_backup_none_yet);
        }
        autoBackupStatusText.setText(status);
    }

    private void showBackupPermissionDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.backup_permission_title)
                .setMessage(R.string.backup_permission_message)
                .setPositiveButton(R.string.backup_permission_open, (dialog, which) -> openAllFilesAccessSettings())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openAllFilesAccessSettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + requireContext().getPackageName()));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        }
    }

    private void showLanguageDialog() {
        int current = AppLanguage.indexOf(AppLanguage.current(requireContext()));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_language)
                .setSingleChoiceItems(R.array.language_names, current, (dialog, which) -> {
                    dialog.dismiss();
                    if (which != current) AppLanguage.set(AppLanguage.TAGS[which]);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showCurrencyDialog() {
        String custom = CurrencyLabel.custom(requireContext());
        String[] items = new String[CURRENCY_PRESETS.length + 2];
        items[0] = getString(R.string.currency_default, getString(R.string.ui_som));
        System.arraycopy(CURRENCY_PRESETS, 0, items, 1, CURRENCY_PRESETS.length);
        int otherIndex = items.length - 1;
        items[otherIndex] = getString(R.string.currency_other);
        int current = custom == null ? 0 : otherIndex;
        for (int i = 0; i < CURRENCY_PRESETS.length; i++) {
            if (CURRENCY_PRESETS[i].equals(custom)) current = i + 1;
        }
        boolean customIsPreset = current != otherIndex;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_currency)
                .setSingleChoiceItems(items, current, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == otherIndex) {
                        showCustomCurrencyDialog(customIsPreset ? "" : custom);
                    } else {
                        saveCurrency(which == 0 ? null : CURRENCY_PRESETS[which - 1]);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showCustomCurrencyDialog(String initial) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_text, null);
        TextInputLayout inputLayout = view.findViewById(R.id.textInputLayout);
        TextInputEditText input = view.findViewById(R.id.editText);
        inputLayout.setHint(R.string.currency_custom_hint);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(CurrencyLabel.MAX_LENGTH)});
        input.setText(initial);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_currency)
                .setView(view)
                .setPositiveButton(R.string.ui_saqlash, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        // Noto'g'ri qiymatda dialog yopilmasligi uchun tugma ko'rsatilgandan keyin ulanadi.
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String label = CurrencyLabel.normalize(
                            input.getText() == null ? null : input.getText().toString());
                    if (label == null) {
                        inputLayout.setError(getString(R.string.currency_invalid));
                        return;
                    }
                    saveCurrency(label);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void saveCurrency(@Nullable String label) {
        if (currentSettings == null) return;
        currentSettings.setCurrencyLabel(label);
        AppSettings settings = currentSettings;
        AppDatabase.databaseWriteExecutor.execute(() -> database.appSettingsDao().update(settings));
        CurrencyLabel.cache(requireContext(), label);
        currencyText.setText(CurrencyLabel.current(requireContext()));
    }

    private void showExportConfirmDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ui_eksport_qilish)
                .setMessage(R.string.export_confirm_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    exportData();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void exportData() {
        if (needsBackupFolderAccess()) {
            showBackupPermissionDialog();
            return;
        }
        showProgressDialog(getString(R.string.export_progress));

        String uriString = requireContext().getSharedPreferences(PREFS_NAME, 0)
                .getString(KEY_BACKUP_FOLDER_URI, null);

        DataExportImport.ExportCallback callback = new DataExportImport.ExportCallback() {
            @Override
            public void onSuccess(Uri fileUri) {
                hideProgressDialog();
                AutoBackupState.recordSuccess(requireContext(), System.currentTimeMillis());
                updateAutoBackupStatus();
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.export_success_title)
                        .setMessage(getString(R.string.export_success_message, fileUri.getPath()))
                        .setPositiveButton(R.string.close, null)
                        .setNeutralButton(R.string.share, (dialog, which) -> {
                            shareFile(fileUri);
                        })
                        .show();
            }

            @Override
            public void onError(String message) {
                hideProgressDialog();
                Toast.makeText(requireContext(), getString(R.string.error_with_message, message), Toast.LENGTH_LONG).show();
            }
        };

        if (uriString != null) {
            dataExportImport.exportData(Uri.parse(uriString), callback);
        } else {
            dataExportImport.exportData(callback);
        }
    }

    private void shareFile(Uri fileUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/zip");
        // file:// manzilni boshqa ilovaga berib bo'lmaydi: FileProvider orqali ulashiladi.
        Uri shareUri = "file".equals(fileUri.getScheme()) && fileUri.getPath() != null
                ? FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider", new File(fileUri.getPath()))
                : fileUri;
        shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_file)));
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        importFileLauncher.launch(intent);
    }

    private void showImportConfirmDialog(Uri uri) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ui_import_qilish)
                .setMessage(R.string.import_confirm_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    importData(uri);
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void importData(Uri uri) {
        showProgressDialog(getString(R.string.import_progress));

        dataExportImport.importData(uri, new DataExportImport.ImportCallback() {
            @Override
            public void onSuccess() {
                hideProgressDialog();
                Toast.makeText(requireContext(), R.string.import_success, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String message) {
                hideProgressDialog();
                Toast.makeText(requireContext(), getString(R.string.error_with_message, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showProgressDialog(String message) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_progress, null);
        TextView messageText = view.findViewById(R.id.progressMessage);
        messageText.setText(message);

        progressDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .setCancelable(false)
                .create();
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showBackupFolderDialog() {
        String currentUri = requireContext().getSharedPreferences(PREFS_NAME, 0)
                .getString(KEY_BACKUP_FOLDER_URI, null);

        String[] options;
        if (currentUri != null) {
            options = new String[]{getString(R.string.backup_folder_change),
                    getString(R.string.backup_folder_reset)};
        } else {
            options = new String[]{getString(R.string.backup_folder_choose)};
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.ui_backup_papkasi)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openFolderPicker();
                    } else {
                        requireContext().getSharedPreferences(PREFS_NAME, 0)
                                .edit()
                                .remove(KEY_BACKUP_FOLDER_URI)
                                .apply();
                        updateBackupFolderDisplay();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        folderPickerLauncher.launch(intent);
    }

    private void updateBackupFolderDisplay() {
        if (autoBackupStatusText != null) updateAutoBackupStatus();
        String uriString = requireContext().getSharedPreferences(PREFS_NAME, 0)
                .getString(KEY_BACKUP_FOLDER_URI, null);

        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            DocumentFile folder = DocumentFile.fromTreeUri(requireContext(), uri);
            if (folder != null && folder.getName() != null) {
                backupFolderText.setText(folder.getName());
            } else {
                backupFolderText.setText(R.string.backup_folder_selected);
            }
        } else {
            backupFolderText.setText(R.string.ui_standart);
        }
    }
}
