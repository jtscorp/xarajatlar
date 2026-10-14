package uz.jtscorp.xarajatlar.utils;

import uz.jtscorp.xarajatlar.R;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.documentfile.provider.DocumentFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.AppSettings;
import uz.jtscorp.xarajatlar.data.entity.Category;
import uz.jtscorp.xarajatlar.data.entity.Debt;
import uz.jtscorp.xarajatlar.data.entity.DebtEvidence;
import uz.jtscorp.xarajatlar.data.entity.Expense;
import uz.jtscorp.xarajatlar.data.entity.FamilyMember;
import uz.jtscorp.xarajatlar.data.entity.Income;
import uz.jtscorp.xarajatlar.data.entity.IncomeCategory;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;

public class DataExportImport {

    public static final String PREFS_NAME = "backup_prefs";
    public static final String KEY_BACKUP_FOLDER_URI = "backup_folder_uri";
    static final int MAX_ARCHIVE_ENTRIES = 512;
    static final int MAX_DATA_JSON_BYTES = 8 * 1024 * 1024;
    private static final long MAX_ENTRY_BYTES = 32L * 1024 * 1024;
    private static final long MAX_TOTAL_EXTRACTED_BYTES = 256L * 1024 * 1024;

    public interface ExportCallback {
        void onSuccess(Uri fileUri);
        void onError(String message);
    }

    public interface ImportCallback {
        void onSuccess();
        void onError(String message);
    }

    private final Context context;
    private final AppDatabase database;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public DataExportImport(Context context) {
        this(context, AppDatabase.getDatabase(context));
    }

    DataExportImport(Context context, AppDatabase database) {
        this.context = context.getApplicationContext();
        this.database = database;
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void shutdown() {
        executor.shutdown();
    }

    public void exportData(ExportCallback callback) {
        executor.execute(() -> {
            try {
                // Create backup directory
                File backupDir = new File(context.getCacheDir(), "backup");
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }

                // Generate filename
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault());
                String timestamp = sdf.format(new Date());
                String zipFilename = "xarajatlar_backup_" + timestamp + ".zip";
                File zipFile = new File(backupDir, zipFilename);

                // Create JSON data
                JSONObject dataJson = createDataJson();

                // Create ZIP file
                try (FileOutputStream fos = new FileOutputStream(zipFile);
                     ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {

                    // Add data.json to ZIP
                    ZipEntry dataEntry = new ZipEntry("data.json");
                    zos.putNextEntry(dataEntry);
                    byte[] jsonBytes = dataJson.toString(2).getBytes(StandardCharsets.UTF_8);
                    zos.write(jsonBytes);
                    zos.closeEntry();

                    // Add media files (debt evidence)
                    addMediaFilesToZip(zos);
                }

                Uri exportUri = saveToBackupFolder(zipFile, zipFilename);

                mainHandler.post(() -> callback.onSuccess(exportUri));

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void exportData(Uri folderUri, ExportCallback callback) {
        executor.execute(() -> {
            try {
                File backupDir = new File(context.getCacheDir(), "backup");
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault());
                String timestamp = sdf.format(new Date());
                String zipFilename = "xarajatlar_backup_" + timestamp + ".zip";
                File tempZipFile = new File(backupDir, zipFilename);

                JSONObject dataJson = createDataJson();

                try (FileOutputStream fos = new FileOutputStream(tempZipFile);
                     ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
                    ZipEntry dataEntry = new ZipEntry("data.json");
                    zos.putNextEntry(dataEntry);
                    byte[] jsonBytes = dataJson.toString(2).getBytes(StandardCharsets.UTF_8);
                    zos.write(jsonBytes);
                    zos.closeEntry();
                    addMediaFilesToZip(zos);
                }

                DocumentFile folder = DocumentFile.fromTreeUri(context, folderUri);
                if (folder == null || !folder.canWrite()) {
                    throw new IOException(context.getString(R.string.backup_folder_not_writable));
                }

                DocumentFile newFile = folder.createFile("application/zip", zipFilename);
                if (newFile == null) {
                    throw new IOException(context.getString(R.string.backup_file_create_failed));
                }

                ContentResolver resolver = context.getContentResolver();
                try (FileInputStream fis = new FileInputStream(tempZipFile);
                     OutputStream os = resolver.openOutputStream(newFile.getUri())) {
                    if (os == null) {
                        throw new IOException(context.getString(R.string.backup_file_write_failed));
                    }
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }

                tempZipFile.delete();

                mainHandler.post(() -> callback.onSuccess(newFile.getUri()));

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private JSONObject createDataJson() throws JSONException {
        JSONObject root = new JSONObject();

        // Family members
        List<FamilyMember> familyMembers = database.familyMemberDao().getAllSync();
        JSONArray familyArray = new JSONArray();
        for (FamilyMember member : familyMembers) {
            JSONObject obj = new JSONObject();
            obj.put("id", member.getId());
            obj.put("name", member.getName());
            obj.put("photoPath", member.getPhotoPath());
            obj.put("isActive", member.isActive());
            familyArray.put(obj);
        }
        root.put("family_members", familyArray);

        // Categories
        List<Category> categories = database.categoryDao().getAllSync();
        JSONArray categoryArray = new JSONArray();
        for (Category cat : categories) {
            JSONObject obj = new JSONObject();
            obj.put("id", cat.getId());
            obj.put("name", cat.getName());
            obj.put("iconName", cat.getIconName());
            obj.put("color", cat.getColor());
            obj.put("isActive", cat.isActive());
            categoryArray.put(obj);
        }
        root.put("categories", categoryArray);

        // Payment methods
        List<PaymentMethod> paymentMethods = database.paymentMethodDao().getAllSync();
        JSONArray paymentArray = new JSONArray();
        for (PaymentMethod pm : paymentMethods) {
            JSONObject obj = new JSONObject();
            obj.put("id", pm.getId());
            obj.put("name", pm.getName());
            obj.put("lastFourDigits", pm.getLastFourDigits());
            obj.put("color", pm.getColor());
            obj.put("isCash", pm.isCash());
            paymentArray.put(obj);
        }
        root.put("payment_methods", paymentArray);

        // Expenses
        List<Expense> expenses = database.expenseDao().getAllSync();
        JSONArray expenseArray = new JSONArray();
        for (Expense exp : expenses) {
            JSONObject obj = new JSONObject();
            obj.put("id", exp.getId());
            obj.put("amount", exp.getAmount());
            obj.put("description", exp.getDescription());
            obj.put("timestamp", exp.getTimestamp());
            obj.put("categoryId", exp.getCategoryId());
            obj.put("familyMemberId", exp.getFamilyMemberId());
            obj.put("paymentMethodId", exp.getPaymentMethodId());
            obj.put("receiptUrl", exp.getReceiptUrl());
            obj.put("shopName", exp.getShopName());
            if (exp.getReceiptItemsJson() != null) {
                obj.put("receiptItems", new JSONArray(exp.getReceiptItemsJson()));
            }
            expenseArray.put(obj);
        }
        root.put("expenses", expenseArray);

        // Incomes
        List<Income> incomes = database.incomeDao().getAllSync();
        JSONArray incomeArray = new JSONArray();
        for (Income inc : incomes) {
            JSONObject obj = new JSONObject();
            obj.put("id", inc.getId());
            obj.put("amount", inc.getAmount());
            obj.put("source", inc.getSource());
            obj.put("timestamp", inc.getTimestamp());
            obj.put("incomeCategoryId", inc.getIncomeCategoryId());
            obj.put("familyMemberId", inc.getFamilyMemberId());
            obj.put("paymentMethodId", inc.getPaymentMethodId());
            obj.put("description", inc.getDescription());
            incomeArray.put(obj);
        }
        root.put("incomes", incomeArray);

        // Debts
        List<Debt> debts = database.debtDao().getAllSync();
        JSONArray debtArray = new JSONArray();
        for (Debt debt : debts) {
            JSONObject obj = new JSONObject();
            obj.put("id", debt.getId());
            obj.put("personName", debt.getPersonName());
            obj.put("phone", debt.getPhone());
            obj.put("address", debt.getAddress());
            obj.put("amount", debt.getAmount());
            obj.put("paidAmount", debt.getPaidAmount());
            obj.put("givenDate", debt.getGivenDate());
            obj.put("dueDate", debt.getDueDate());
            obj.put("latitude", debt.getLatitude());
            obj.put("longitude", debt.getLongitude());
            obj.put("notes", debt.getNotes());
            obj.put("isFullyPaid", debt.isFullyPaid());
            debtArray.put(obj);
        }
        root.put("debts", debtArray);

        // Debt evidences
        List<DebtEvidence> evidences = database.debtEvidenceDao().getAllSync();
        JSONArray evidenceArray = new JSONArray();
        for (DebtEvidence ev : evidences) {
            JSONObject obj = new JSONObject();
            obj.put("id", ev.getId());
            obj.put("debtId", ev.getDebtId());
            obj.put("fileType", ev.getFileType());
            obj.put("filePath", EvidenceFiles.backupPath(context, ev.getFilePath(), ev.getId()));
            obj.put("timestamp", ev.getTimestamp());
            obj.put("latitude", ev.getLatitude());
            obj.put("longitude", ev.getLongitude());
            evidenceArray.put(obj);
        }
        root.put("debt_evidences", evidenceArray);

        // Income categories
        List<IncomeCategory> incomeCategories = database.incomeCategoryDao().getAllSync();
        JSONArray incomeCatArray = new JSONArray();
        for (IncomeCategory ic : incomeCategories) {
            JSONObject obj = new JSONObject();
            obj.put("id", ic.getId());
            obj.put("name", ic.getName());
            obj.put("emoji", ic.getEmoji());
            obj.put("isActive", ic.isActive());
            incomeCatArray.put(obj);
        }
        root.put("income_categories", incomeCatArray);

        // App settings
        AppSettings settings = database.appSettingsDao().getSettingsSync();
        if (settings != null) {
            JSONObject settingsObj = new JSONObject();
            settingsObj.put("salaryDay", settings.getSalaryDay());
            settingsObj.put("biometricEnabled", settings.isBiometricEnabled());
            if (settings.getCurrencyLabel() != null) {
                settingsObj.put("currencyLabel", settings.getCurrencyLabel());
            }
            root.put("settings", settingsObj);
        }

        return root;
    }

    private void addMediaFilesToZip(ZipOutputStream zos) throws IOException {
        List<DebtEvidence> evidences = database.debtEvidenceDao().getAllSync();
        for (DebtEvidence evidence : evidences) {
            File mediaFile = EvidenceFiles.managedRegularFile(context, evidence.getFilePath());
            if (mediaFile != null) {
                    String zipPath = "media/debts/" + evidence.getDebtId() + "/" + mediaFile.getName();
                    ZipEntry entry = new ZipEntry(zipPath);
                    zos.putNextEntry(entry);

                    try (FileInputStream fis = new FileInputStream(mediaFile);
                         BufferedInputStream bis = new BufferedInputStream(fis)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = bis.read(buffer)) != -1) {
                            zos.write(buffer, 0, bytesRead);
                        }
                    }
                    zos.closeEntry();
            }
        }
    }

    /** Standart joy: umumiy "backups" papkasi. Mavjud fayllarga tegilmaydi. */
    private Uri saveToBackupFolder(File sourceFile, String filename) throws IOException {
        if (!BackupFolder.hasAccess()) {
            throw new IOException(context.getString(R.string.backup_permission_required));
        }
        File saved;
        try {
            saved = BackupFolder.save(BackupFolder.directory(), sourceFile, filename);
        } catch (NotDirectoryException e) {
            throw new IOException(context.getString(R.string.backup_folder_is_file));
        } catch (IOException e) {
            throw new IOException(context.getString(R.string.backup_dir_create_failed));
        }
        sourceFile.delete();
        return Uri.fromFile(saved);
    }

    public void importData(Uri zipUri, ImportCallback callback) {
        executor.execute(() -> {
            String restoreId = UUID.randomUUID().toString();
            File tempDir = new File(context.getCacheDir(), "import_" + restoreId);
            // A restore never overwrites media referenced by the existing database.
            File mediaDestination = new File(context.getFilesDir(), "debts/restore_" + restoreId);
            boolean committed = false;
            String failure = null;
            try {
                if (!tempDir.mkdirs()) throw new IOException(context.getString(R.string.backup_temp_dir_failed));

                InputStream opened = context.getContentResolver().openInputStream(zipUri);
                if (opened == null) throw new IOException(context.getString(R.string.backup_open_failed));
                try (InputStream is = opened;
                     ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {

                    ZipEntry entry;
                    int entryCount = 0;
                    long totalBytes = 0;
                    Set<String> extractedPaths = new HashSet<>();
                    while ((entry = zis.getNextEntry()) != null) {
                        if (++entryCount > MAX_ARCHIVE_ENTRIES) {
                            throw new IOException(context.getString(R.string.backup_too_many_files));
                        }
                        File file = new File(tempDir, entry.getName());
                        String canonicalPath = file.getCanonicalPath();
                        if (!canonicalPath.startsWith(tempDir.getCanonicalPath() + File.separator)) {
                            throw new SecurityException(context.getString(R.string.backup_unsafe_path));
                        }
                        if (!extractedPaths.add(canonicalPath)) {
                            throw new IOException(context.getString(R.string.backup_duplicate_file));
                        }
                        if (entry.isDirectory()) {
                            if (!file.isDirectory() && !file.mkdirs()) {
                                throw new IOException(context.getString(R.string.backup_dir_create_failed));
                            }
                        } else {
                            File parent = file.getParentFile();
                            if (!parent.isDirectory() && !parent.mkdirs()) {
                                throw new IOException(context.getString(R.string.backup_dir_create_failed));
                            }
                            try (FileOutputStream fos = new FileOutputStream(file)) {
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                long entryBytes = 0;
                                while ((bytesRead = zis.read(buffer)) != -1) {
                                    entryBytes += bytesRead;
                                    totalBytes += bytesRead;
                                    if (entryBytes > MAX_ENTRY_BYTES) {
                                        throw new IOException(context.getString(R.string.backup_file_too_large));
                                    }
                                    if (totalBytes > MAX_TOTAL_EXTRACTED_BYTES) {
                                        throw new IOException(context.getString(R.string.backup_total_too_large));
                                    }
                                    if ("data.json".equals(entry.getName())
                                            && entryBytes > MAX_DATA_JSON_BYTES) {
                                        throw new IOException(context.getString(R.string.backup_data_too_large));
                                    }
                                    fos.write(buffer, 0, bytesRead);
                                }
                            }
                        }
                        zis.closeEntry();
                    }
                }

                // Read and import data.json
                File dataFile = new File(tempDir, "data.json");
                if (!dataFile.exists()) {
                    throw new IOException(context.getString(R.string.backup_data_missing));
                }

                String jsonContent = readFileAsString(dataFile);
                JSONObject dataJson = new JSONObject(jsonContent);

                BackupValidator.validate(dataJson, tempDir);
                database.beginTransaction();
                try {
                    importFromJson(dataJson, tempDir, mediaDestination);
                    database.setTransactionSuccessful();
                } finally {
                    database.endTransaction();
                }
                committed = true;
                AppSettings restored = database.appSettingsDao().getSettingsSync();
                CurrencyLabel.cache(context, restored == null ? null : restored.getCurrencyLabel());
            } catch (Exception e) {
                if (e instanceof BackupFormatException) {
                    failure = ((BackupFormatException) e).message(context);
                } else {
                    failure = e.getMessage() == null
                            ? context.getString(R.string.backup_restore_failed) : e.getMessage();
                }
            } finally {
                deleteRecursive(tempDir);
                if (!committed) deleteRecursive(mediaDestination);
            }
            if (committed) mainHandler.post(callback::onSuccess);
            else {
                String message = failure;
                mainHandler.post(() -> callback.onError(message));
            }
        });
    }

    private void importFromJson(JSONObject dataJson, File tempDir, File mediaDestination)
            throws JSONException, IOException {
        // Clear all tables (order matters due to foreign keys)
        database.debtEvidenceDao().deleteAll();
        database.expenseDao().deleteAll();
        database.incomeDao().deleteAll();
        database.incomeCategoryDao().deleteAll();
        database.debtDao().deleteAll();
        database.familyMemberDao().deleteAll();
        database.categoryDao().deleteAll();
        database.paymentMethodDao().deleteAll();

        // Import family members
        if (dataJson.has("family_members")) {
            JSONArray familyArray = dataJson.getJSONArray("family_members");
            for (int i = 0; i < familyArray.length(); i++) {
                JSONObject obj = familyArray.getJSONObject(i);
                FamilyMember member = new FamilyMember();
                member.setId(obj.getInt("id"));
                member.setName(obj.getString("name"));
                member.setPhotoPath(obj.optString("photoPath", null));
                member.setActive(obj.getBoolean("isActive"));
                database.familyMemberDao().insert(member);
            }
        }

        // Import categories
        if (dataJson.has("categories")) {
            JSONArray categoryArray = dataJson.getJSONArray("categories");
            for (int i = 0; i < categoryArray.length(); i++) {
                JSONObject obj = categoryArray.getJSONObject(i);
                Category cat = new Category();
                cat.setId(obj.getInt("id"));
                cat.setName(obj.getString("name"));
                cat.setIconName(obj.optString("iconName", null));
                cat.setColor(obj.getInt("color"));
                cat.setActive(obj.getBoolean("isActive"));
                database.categoryDao().insert(cat);
            }
        }

        // Import payment methods
        if (dataJson.has("payment_methods")) {
            JSONArray paymentArray = dataJson.getJSONArray("payment_methods");
            for (int i = 0; i < paymentArray.length(); i++) {
                JSONObject obj = paymentArray.getJSONObject(i);
                PaymentMethod pm = new PaymentMethod();
                pm.setId(obj.getInt("id"));
                pm.setName(obj.getString("name"));
                pm.setLastFourDigits(obj.optString("lastFourDigits", null));
                pm.setColor(obj.getInt("color"));
                pm.setCash(obj.getBoolean("isCash"));
                database.paymentMethodDao().insert(pm);
            }
        }

        // Import debts
        if (dataJson.has("debts")) {
            JSONArray debtArray = dataJson.getJSONArray("debts");
            for (int i = 0; i < debtArray.length(); i++) {
                JSONObject obj = debtArray.getJSONObject(i);
                Debt debt = new Debt();
                debt.setId(obj.getInt("id"));
                debt.setPersonName(obj.getString("personName"));
                debt.setPhone(obj.optString("phone", null));
                debt.setAddress(obj.optString("address", null));
                debt.setAmount(obj.getDouble("amount"));
                debt.setPaidAmount(obj.optDouble("paidAmount", 0));
                debt.setGivenDate(obj.getLong("givenDate"));
                debt.setDueDate(obj.optLong("dueDate", 0));
                debt.setLatitude(obj.optDouble("latitude", 0));
                debt.setLongitude(obj.optDouble("longitude", 0));
                debt.setNotes(obj.optString("notes", null));
                debt.setFullyPaid(obj.getBoolean("isFullyPaid"));
                database.debtDao().insert(debt);
            }
        }

        // Import expenses
        if (dataJson.has("expenses")) {
            JSONArray expenseArray = dataJson.getJSONArray("expenses");
            for (int i = 0; i < expenseArray.length(); i++) {
                JSONObject obj = expenseArray.getJSONObject(i);
                Expense exp = new Expense();
                exp.setId(obj.getInt("id"));
                exp.setAmount(obj.getDouble("amount"));
                exp.setDescription(obj.optString("description", null));
                exp.setTimestamp(obj.getLong("timestamp"));
                if (obj.has("categoryId") && !obj.isNull("categoryId")) {
                    exp.setCategoryId(obj.getInt("categoryId"));
                }
                if (obj.has("familyMemberId") && !obj.isNull("familyMemberId")) {
                    exp.setFamilyMemberId(obj.getInt("familyMemberId"));
                }
                if (obj.has("paymentMethodId") && !obj.isNull("paymentMethodId")) {
                    exp.setPaymentMethodId(obj.getInt("paymentMethodId"));
                }
                exp.setReceiptUrl(ReceiptUrls.normalizeOrNull(obj.optString("receiptUrl", null)));
                exp.setShopName(obj.optString("shopName", null));
                JSONArray receiptItems = obj.optJSONArray("receiptItems");
                if (receiptItems == null) receiptItems = obj.optJSONArray("items");
                if (receiptItems != null) exp.setReceiptItemsJson(receiptItems.toString());
                else if (obj.optString("receiptItemsJson", "").startsWith("[")) {
                    exp.setReceiptItemsJson(new JSONArray(obj.getString("receiptItemsJson")).toString());
                }
                database.expenseDao().insert(exp);
            }
        }

        // Import income categories
        if (dataJson.has("income_categories")) {
            JSONArray incomeCatArray = dataJson.getJSONArray("income_categories");
            for (int i = 0; i < incomeCatArray.length(); i++) {
                JSONObject obj = incomeCatArray.getJSONObject(i);
                IncomeCategory ic = new IncomeCategory();
                ic.setId(obj.getInt("id"));
                ic.setName(obj.getString("name"));
                ic.setEmoji(obj.optString("emoji", null));
                ic.setActive(obj.optBoolean("isActive", true));
                database.incomeCategoryDao().insert(ic);
            }
        }

        // Import incomes
        if (dataJson.has("incomes")) {
            JSONArray incomeArray = dataJson.getJSONArray("incomes");
            for (int i = 0; i < incomeArray.length(); i++) {
                JSONObject obj = incomeArray.getJSONObject(i);
                Income inc = new Income();
                inc.setId(obj.getInt("id"));
                inc.setAmount(obj.getDouble("amount"));
                inc.setSource(obj.optString("source", null));
                inc.setTimestamp(obj.getLong("timestamp"));
                if (obj.has("incomeCategoryId") && !obj.isNull("incomeCategoryId")) {
                    inc.setIncomeCategoryId(obj.getInt("incomeCategoryId"));
                }
                if (obj.has("familyMemberId") && !obj.isNull("familyMemberId")) {
                    inc.setFamilyMemberId(obj.getInt("familyMemberId"));
                }
                if (obj.has("paymentMethodId") && !obj.isNull("paymentMethodId")) {
                    inc.setPaymentMethodId(obj.getInt("paymentMethodId"));
                }
                inc.setDescription(obj.optString("description", null));
                database.incomeDao().insert(inc);
            }
        }

        // Import debt evidences and copy media files
        if (dataJson.has("debt_evidences")) {
            File mediaDir = new File(tempDir, "media/debts");
            JSONArray evidenceArray = dataJson.getJSONArray("debt_evidences");
            for (int i = 0; i < evidenceArray.length(); i++) {
                JSONObject obj = evidenceArray.getJSONObject(i);
                int debtId = obj.getInt("debtId");
                String originalPath = obj.getString("filePath");
                String fileName = new File(originalPath).getName();

                // Copy media file
                File sourceMedia = new File(mediaDir, debtId + "/" + fileName);
                File destDir = new File(mediaDestination, String.valueOf(debtId));
                File destFile = new File(destDir, fileName);
                String newPath = destFile.getCanonicalPath();
                if (sourceMedia.exists()) {
                    if (!destDir.isDirectory() && !destDir.mkdirs()) {
                        throw new IOException(context.getString(R.string.backup_media_dir_failed));
                    }
                    copyFile(sourceMedia, destFile);
                }

                DebtEvidence ev = new DebtEvidence();
                ev.setId(obj.getInt("id"));
                ev.setDebtId(debtId);
                ev.setFileType(obj.getString("fileType"));
                ev.setFilePath(newPath);
                ev.setTimestamp(obj.getLong("timestamp"));
                ev.setLatitude(obj.optDouble("latitude", 0));
                ev.setLongitude(obj.optDouble("longitude", 0));
                database.debtEvidenceDao().insert(ev);
            }
        }

        // Import settings (don't overwrite PIN)
        if (dataJson.has("settings")) {
            JSONObject settingsObj = dataJson.getJSONObject("settings");
            AppSettings settings = database.appSettingsDao().getSettingsSync();
            if (settings != null) {
                settings.setSalaryDay(settingsObj.getInt("salaryDay"));
                settings.setBiometricEnabled(settingsObj.getBoolean("biometricEnabled"));
                // Eski zaxiralarda bu maydon yo'q: standart belgiga qaytadi.
                settings.setCurrencyLabel(CurrencyLabel.normalize(settingsObj.optString("currencyLabel", null)));
                database.appSettingsDao().update(settings);
            }
        }
    }

    private String readFileAsString(File file) throws IOException {
        if (file.length() > MAX_DATA_JSON_BYTES) throw new IOException(context.getString(R.string.backup_data_too_large));
        try (FileInputStream fis = new FileInputStream(file)) {
            return new String(fis.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void copyFile(File source, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(dest)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
}
