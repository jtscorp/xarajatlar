package uz.jtscorp.xarajatlar.ui.debts;

import uz.jtscorp.xarajatlar.utils.Formats;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.Debt;
import uz.jtscorp.xarajatlar.data.entity.DebtEvidence;
import uz.jtscorp.xarajatlar.utils.EvidenceFiles;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

public class DebtDetailActivity extends AppCompatActivity {

    private TextInputEditText personNameInput, phoneInput, addressInput;
    private TextInputEditText amountInput, notesInput;
    private TextInputEditText givenDateInput, dueDateInput;
    private MaterialCardView paymentCard, locationCard, evidenceCard;
    private LinearProgressIndicator paymentProgress;
    private MaterialTextView paymentStatusText, locationText, headerText;
    private MaterialButton addPaymentButton, getLocationButton, addEvidenceButton, closeDebtButton;
    private MaterialButton saveButton;
    private RecyclerView evidenceRecyclerView;
    private DebtEvidenceAdapter evidenceAdapter;
    private AppDatabase database;
    private int debtId = -1;
    private Debt existingDebt;
    private boolean loadingDebt;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final int REQUEST_TAKE_PHOTO = 2001;
    private static final int REQUEST_PICK_IMAGE = 2002;
    private static final int CAMERA_PERMISSION_REQUEST = 2003;
    private String currentPhotoPath;

    private long givenDate = System.currentTimeMillis();
    private long dueDate = 0;
    private double currentLatitude = 0;
    private double currentLongitude = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debt_detail);

        database = AppDatabase.getDatabase(this);
        debtId = getIntent().getIntExtra("debt_id", -1);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupListeners();

        if (debtId > 0) {
            loadingDebt = true;
            saveButton.setEnabled(false);
            loadDebt();
        }
    }

    private void initViews() {
        headerText = findViewById(R.id.headerText);
        personNameInput = findViewById(R.id.personNameInput);
        phoneInput = findViewById(R.id.phoneInput);
        addressInput = findViewById(R.id.addressInput);
        amountInput = findViewById(R.id.amountInput);
        givenDateInput = findViewById(R.id.givenDateInput);
        dueDateInput = findViewById(R.id.dueDateInput);
        notesInput = findViewById(R.id.notesInput);

        paymentCard = findViewById(R.id.paymentCard);
        paymentProgress = findViewById(R.id.paymentProgress);
        paymentStatusText = findViewById(R.id.paymentStatusText);
        addPaymentButton = findViewById(R.id.addPaymentButton);

        locationCard = findViewById(R.id.locationCard);
        locationText = findViewById(R.id.locationText);
        getLocationButton = findViewById(R.id.getLocationButton);

        evidenceCard = findViewById(R.id.evidenceCard);
        addEvidenceButton = findViewById(R.id.addEvidenceButton);
        closeDebtButton = findViewById(R.id.closeDebtButton);
        saveButton = findViewById(R.id.saveButton);

        // Initialize evidence RecyclerView
        evidenceRecyclerView = findViewById(R.id.evidenceRecyclerView);
        evidenceAdapter = new DebtEvidenceAdapter(
                evidence -> viewEvidence(evidence),
                evidence -> deleteEvidence(evidence)
        );
        evidenceRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        evidenceRecyclerView.setAdapter(evidenceAdapter);

        // Set default date
        givenDateInput.setText(Formats.date(givenDate));
    }

    private void setupListeners() {
        givenDateInput.setOnClickListener(v -> showDatePicker(true));
        dueDateInput.setOnClickListener(v -> showDatePicker(false));
        addPaymentButton.setOnClickListener(v -> showAddPaymentDialog());
        getLocationButton.setOnClickListener(v -> getCurrentLocation());
        addEvidenceButton.setOnClickListener(v -> showAddEvidenceDialog());
        closeDebtButton.setOnClickListener(v -> closeDebt());
        saveButton.setOnClickListener(v -> saveDebt());
    }

    private void showDatePicker(boolean isGivenDate) {
        long currentSelection = isGivenDate ? givenDate : (dueDate > 0 ? dueDate : System.currentTimeMillis());

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isGivenDate ? R.string.ui_berilgan_sana : R.string.ui_qaytarish_muddati)
                .setSelection(currentSelection)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (isGivenDate) {
                givenDate = selection;
                givenDateInput.setText(Formats.date(selection));
            } else {
                dueDate = selection;
                dueDateInput.setText(Formats.date(selection));
            }
        });

        picker.show(getSupportFragmentManager(), "date_picker");
    }

    private void showAddPaymentDialog() {
        if (existingDebt == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_payment, null);
        TextInputEditText paymentAmountInput = dialogView.findViewById(R.id.paymentAmountInput);

        double remaining = existingDebt.getAmount() - existingDebt.getPaidAmount();
        paymentAmountInput.setText(String.valueOf(remaining));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.payment_add_title)
                .setView(dialogView)
                .setPositiveButton(R.string.ui_qoshish, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(button -> {
                    String amountStr = paymentAmountInput.getText() != null ? paymentAmountInput.getText().toString() : "";
                    if (amountStr.isEmpty()) {
                        paymentAmountInput.setError(getString(R.string.amount_required));
                        return;
                    }

                    try {
                        double payment = Double.parseDouble(amountStr);
                        if (!Double.isFinite(payment) || payment <= 0 || payment > remaining) {
                            paymentAmountInput.setError(getString(R.string.payment_range_error));
                            return;
                        }
                        double newPaidAmount = existingDebt.getPaidAmount() + payment;
                        existingDebt.setPaidAmount(newPaidAmount);

                        if (newPaidAmount >= existingDebt.getAmount()) {
                            existingDebt.setFullyPaid(true);
                        }

                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            database.debtDao().update(existingDebt);
                            runOnUiThread(() -> {
                                updatePaymentUI();
                                dialog.dismiss();
                            });
                        });
                    } catch (NumberFormatException e) {
                        paymentAmountInput.setError(getString(R.string.invalid_amount));
                    }
                }));
        dialog.show();
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.location_permission_title)
                    .setMessage(R.string.location_permission_message)
                    .setPositiveButton(R.string.allow_location, (dialog, which) ->
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_PERMISSION_REQUEST))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        locationText.setText(String.format(Locale.US, "%.6f, %.6f", currentLatitude, currentLongitude));
                        Toast.makeText(this, R.string.location_obtained, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.location_unavailable, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, R.string.location_denied, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                Toast.makeText(this, R.string.camera_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void closeDebt() {
        if (existingDebt == null || existingDebt.isFullyPaid()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.debt_close_title)
                .setMessage(R.string.debt_close_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    existingDebt.setFullyPaid(true);
                    existingDebt.setPaidAmount(existingDebt.getAmount());
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        database.debtDao().update(existingDebt);
                        runOnUiThread(() -> {
                            Toast.makeText(this, R.string.debt_closed, Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updatePaymentUI() {
        if (existingDebt == null) return;

        double total = existingDebt.getAmount();
        double paid = existingDebt.getPaidAmount();
        int progress = total > 0 ? (int) ((paid / total) * 100) : 0;

        paymentProgress.setProgress(progress);

        paymentStatusText.setText(getString(R.string.debt_payment_status,
                Formats.number(paid), Formats.money(this, total), progress));

        if (existingDebt.isFullyPaid()) {
            closeDebtButton.setVisibility(View.GONE);
            paymentStatusText.setTextColor(getColor(android.R.color.holo_green_dark));
        }
    }

    private void loadDebt() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            existingDebt = database.debtDao().getById(debtId);
            if (existingDebt != null) {
                runOnUiThread(() -> {
                    headerText.setText(R.string.debt_detail_title);
                    loadingDebt = false;
                    saveButton.setEnabled(true);

                    personNameInput.setText(existingDebt.getPersonName());
                    phoneInput.setText(existingDebt.getPhone());
                    addressInput.setText(existingDebt.getAddress());
                    amountInput.setText(String.valueOf(existingDebt.getAmount()));
                    notesInput.setText(existingDebt.getNotes());

                    // Dates
                    givenDate = existingDebt.getGivenDate();
                    givenDateInput.setText(Formats.date(givenDate));

                    if (existingDebt.getDueDate() > 0) {
                        dueDate = existingDebt.getDueDate();
                        dueDateInput.setText(Formats.date(dueDate));
                    }

                    // Location
                    currentLatitude = existingDebt.getLatitude();
                    currentLongitude = existingDebt.getLongitude();
                    if (currentLatitude != 0 || currentLongitude != 0) {
                        locationText.setText(String.format(Locale.US, "%.6f, %.6f", currentLatitude, currentLongitude));
                    }

                    // Show payment card for existing debts
                    paymentCard.setVisibility(View.VISIBLE);
                    updatePaymentUI();

                    // Show evidence card for existing debts
                    evidenceCard.setVisibility(View.VISIBLE);

                    // Show close button if not fully paid
                    if (!existingDebt.isFullyPaid()) {
                        closeDebtButton.setVisibility(View.VISIBLE);
                    }

                    // Load evidence list
                    loadEvidenceList();
                });
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.debt_not_found, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void saveDebt() {
        if (loadingDebt) return;
        String personName = personNameInput.getText() != null ? personNameInput.getText().toString().trim() : "";
        if (personName.isEmpty()) {
            personNameInput.setError(getString(R.string.person_name_required));
            return;
        }

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
        if (!Double.isFinite(amount) || amount <= 0
                || (existingDebt != null && amount < existingDebt.getPaidAmount())) {
            amountInput.setError(getString(R.string.debt_amount_error));
            return;
        }
        if (givenDate <= 0 || (dueDate > 0 && Instant.ofEpochMilli(dueDate)
                .atZone(ZoneId.systemDefault()).toLocalDate().isBefore(
                        Instant.ofEpochMilli(givenDate).atZone(ZoneId.systemDefault()).toLocalDate()))) {
            dueDateInput.setError(getString(R.string.debt_date_error));
            return;
        }

        Debt debt = existingDebt != null ? existingDebt : new Debt();
        debt.setPersonName(personName);
        debt.setPhone(phoneInput.getText() != null ? phoneInput.getText().toString() : "");
        debt.setAddress(addressInput.getText() != null ? addressInput.getText().toString() : "");
        debt.setAmount(amount);
        debt.setNotes(notesInput.getText() != null ? notesInput.getText().toString() : "");
        debt.setGivenDate(givenDate);
        debt.setDueDate(dueDate);
        debt.setLatitude(currentLatitude);
        debt.setLongitude(currentLongitude);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (existingDebt != null) {
                database.debtDao().update(debt);
            } else {
                database.debtDao().insert(debt);
            }
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.debt_saved, Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void showAddEvidenceDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_evidence, null);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.evidence_add_title)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .create();

        dialogView.findViewById(R.id.takePhotoButton).setOnClickListener(v -> {
            dialog.dismiss();
            takePhoto();
        });

        dialogView.findViewById(R.id.pickImageButton).setOnClickListener(v -> {
            dialog.dismiss();
            pickImage();
        });

        dialog.show();
    }

    private void takePhoto() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.camera_permission_title)
                    .setMessage(R.string.debt_photo_permission_message)
                    .setPositiveButton(R.string.allow_camera, (dialog, which) ->
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.CAMERA},
                                    CAMERA_PERMISSION_REQUEST))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "uz.jtscorp.xarajatlar.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "IMG_" + timeStamp;
        File storageDir = new File(getFilesDir(), "debts/" + debtId);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        try {
            File image = File.createTempFile(imageFileName, ".jpg", storageDir);
            currentPhotoPath = image.getAbsolutePath();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadEvidenceList() {
        if (debtId <= 0) return;
        database.debtEvidenceDao().getByDebtId(debtId).observe(this, evidenceList -> {
            evidenceAdapter.setEvidenceList(evidenceList);
        });
    }

    private void viewEvidence(DebtEvidence evidence) {
        if (!EvidenceFiles.isManaged(this, evidence.getFilePath())) {
            Toast.makeText(this, R.string.file_path_unsafe, Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(evidence.getFilePath());
        if (file.exists()) {
            Uri uri = FileProvider.getUriForFile(this, "uz.jtscorp.xarajatlar.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } else {
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteEvidence(DebtEvidence evidence) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.evidence_delete_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        EvidenceFiles.deleteManaged(this, evidence.getFilePath());
                        // Delete from database
                        database.debtEvidenceDao().delete(evidence);
                    });
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_PHOTO && currentPhotoPath != null) {
                saveEvidence(currentPhotoPath, "image");
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    String destPath = copyImageToStorage(selectedImage);
                    if (destPath != null) {
                        saveEvidence(destPath, "image");
                    }
                }
            }
        }
    }

    private String copyImageToStorage(Uri sourceUri) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            File storageDir = new File(getFilesDir(), "debts/" + debtId);
            if (!storageDir.exists()) storageDir.mkdirs();

            File destFile = new File(storageDir, "IMG_" + timeStamp + ".jpg");

            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            OutputStream outputStream = new FileOutputStream(destFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();

            return destFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveEvidence(String filePath, String fileType) {
        DebtEvidence evidence = new DebtEvidence();
        evidence.setDebtId(debtId);
        evidence.setFilePath(filePath);
        evidence.setFileType(fileType);
        evidence.setTimestamp(System.currentTimeMillis());
        evidence.setLatitude(currentLatitude);
        evidence.setLongitude(currentLongitude);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            database.debtEvidenceDao().insert(evidence);
            runOnUiThread(() -> Toast.makeText(this, R.string.evidence_saved, Toast.LENGTH_SHORT).show());
        });
    }
}
