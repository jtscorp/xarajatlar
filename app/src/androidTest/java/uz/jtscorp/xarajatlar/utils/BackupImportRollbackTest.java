package uz.jtscorp.xarajatlar.utils;

import static org.junit.Assert.*;

import android.content.Context;
import android.net.Uri;
import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.AppSettings;
import uz.jtscorp.xarajatlar.data.entity.Expense;

/** Uses the public ZIP import path with a separate, in-memory database. */
public class BackupImportRollbackTest {
    private Context context;
    private AppDatabase database;
    private DataExportImport importer;
    private File zip;

    @Before public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        importer = new DataExportImport(context, database);
        Expense original = new Expense();
        original.setId(99);
        original.setAmount(45000);
        original.setTimestamp(1000);
        database.expenseDao().insert(original);
    }

    @After public void tearDown() {
        CurrencyLabel.cache(context, null);
        importer.shutdown();
        database.close();
        if (zip != null) zip.delete();
    }

    @Test public void malformedSecondRecordPreservesOriginalDatabase() throws Exception {
        JSONObject data = backup();
        data.getJSONArray("expenses").put(new JSONObject().put("id", 2).put("timestamp", 3000));
        assertNotNull(restore(data));
        assertOriginal();
    }

    @Test public void missingRelationPreservesOriginalDatabase() throws Exception {
        JSONObject data = backup();
        data.getJSONArray("expenses").getJSONObject(0).put("categoryId", 789);
        assertNotNull(restore(data));
        assertOriginal();
    }

    @Test public void failedInsertRollsBackEveryDeleteAndInsert() throws Exception {
        database.getOpenHelper().getWritableDatabase().execSQL(
                "CREATE TRIGGER reject_restore BEFORE INSERT ON expenses WHEN NEW.id = 2 " +
                "BEGIN SELECT RAISE(ABORT, 'simulated write failure'); END");
        JSONObject data = backup();
        data.getJSONArray("expenses").put(new JSONObject().put("id", 2)
                .put("amount", 30).put("timestamp", 3000));
        assertNotNull(restore(data));
        assertOriginal();
    }

    @Test public void validBackupReplacesOriginalDatabase() throws Exception {
        assertNull(restore(backup()));
        assertEquals(1, database.expenseDao().getAllSync().size());
        assertEquals(1, database.expenseDao().getAllSync().get(0).getId());
        assertEquals(20, database.expenseDao().getAllSync().get(0).getAmount(), 0);
    }

    @Test public void emptyObjectCannotEraseDatabase() throws Exception {
        assertNotNull(restore(new JSONObject()));
        assertOriginal();
    }

    @Test public void failureAfterMediaCopyRemovesOnlyNewMedia() throws Exception {
        File debts = new File(context.getFilesDir(), "debts");
        debts.mkdirs();
        File existing = File.createTempFile("existing-evidence-", ".jpg", debts);
        byte[] original = new byte[]{1, 2, 3};
        Files.write(existing.toPath(), original);
        Set<String> before = new HashSet<>(Arrays.asList(debts.list()));
        try {
            database.getOpenHelper().getWritableDatabase().execSQL(
                    "CREATE TRIGGER reject_evidence BEFORE INSERT ON debt_evidences " +
                    "BEGIN SELECT RAISE(ABORT, 'simulated media record failure'); END");
            JSONObject data = backup();
            data.getJSONArray("debts").put(new JSONObject().put("id", 1).put("personName", "Sinov")
                    .put("amount", 100).put("givenDate", 1000).put("isFullyPaid", false));
            data.getJSONArray("debt_evidences").put(new JSONObject().put("id", 1)
                    .put("debtId", 1).put("filePath", existing.getAbsolutePath())
                    .put("fileType", "image").put("timestamp", 2000));
            assertNotNull(restore(data, "media/debts/1/" + existing.getName()));
            assertOriginal();
            assertArrayEquals(original, Files.readAllBytes(existing.toPath()));
            assertEquals(before, new HashSet<>(Arrays.asList(debts.list())));
        } finally { existing.delete(); }
    }

    @Test public void archiveTraversalCannotWriteOutsideStagingOrEraseDatabase() throws Exception {
        File outside = new File(context.getCacheDir(), "restore-test-traversal.txt");
        assertFalse(outside.exists());
        assertNotNull(restore(backup(), "../restore-test-traversal.txt"));
        assertFalse(outside.exists());
        assertOriginal();
    }

    @Test public void exportedEvidenceWithoutMediaStillRestoresItsRecord() throws Exception {
        JSONObject data = backup();
        data.getJSONArray("debts").put(new JSONObject().put("id", 1).put("personName", "Sinov")
                .put("amount", 100).put("givenDate", 1000).put("isFullyPaid", false));
        data.getJSONArray("debt_evidences").put(new JSONObject().put("id", 1)
                .put("debtId", 1).put("filePath", "/missing/photo.jpg")
                .put("fileType", "image").put("timestamp", 2000));
        assertNull(restore(data));
        assertEquals(1, database.debtEvidenceDao().getAllSync().size());
        String restoredPath = database.debtEvidenceDao().getAllSync().get(0).getFilePath();
        assertNotEquals("/missing/photo.jpg", restoredPath);
        assertTrue(EvidenceFiles.isManaged(context, restoredPath));
        assertFalse(new File(restoredPath).exists());
    }

    @Test public void evidenceDeleteRefusesFilesOutsideManagedDirectory() throws Exception {
        File protectedFile = File.createTempFile("protected-", ".txt", context.getFilesDir());
        try {
            Files.write(protectedFile.toPath(), new byte[]{4, 5, 6});
            assertNull(EvidenceFiles.managedRegularFile(context, protectedFile.getAbsolutePath()));
            assertFalse(EvidenceFiles.backupPath(context, protectedFile.getAbsolutePath(), 7)
                    .contains(protectedFile.getName()));
            assertFalse(EvidenceFiles.deleteManaged(context, protectedFile.getAbsolutePath()));
            assertTrue(protectedFile.exists());
        } finally {
            protectedFile.delete();
        }
    }

    @Test public void archiveWithTooManyEntriesPreservesOriginalDatabase() throws Exception {
        zip = File.createTempFile("restore-test-", ".zip", context.getCacheDir());
        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(zip))) {
            output.putNextEntry(new ZipEntry("data.json"));
            output.write(backup().toString().getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            for (int i = 1; i <= DataExportImport.MAX_ARCHIVE_ENTRIES; i++) {
                output.putNextEntry(new ZipEntry("empty/" + i));
                output.closeEntry();
            }
        }
        assertNotNull(restoreExistingZip());
        assertOriginal();
    }

    @Test public void oversizedDataJsonPreservesOriginalDatabase() throws Exception {
        zip = File.createTempFile("restore-test-", ".zip", context.getCacheDir());
        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(zip))) {
            output.putNextEntry(new ZipEntry("data.json"));
            byte[] chunk = new byte[8192];
            int remaining = DataExportImport.MAX_DATA_JSON_BYTES + 1;
            while (remaining > 0) {
                int count = Math.min(remaining, chunk.length);
                output.write(chunk, 0, count);
                remaining -= count;
            }
            output.closeEntry();
        }
        assertNotNull(restoreExistingZip());
        assertOriginal();
    }

    @Test public void backupCurrencyLabelIsRestored() throws Exception {
        database.appSettingsDao().insert(new AppSettings());
        JSONObject data = backup().put("settings", settings().put("currencyLabel", "$"));

        assertNull(restore(data));

        assertEquals("$", database.appSettingsDao().getSettingsSync().getCurrencyLabel());
        assertEquals("$", CurrencyLabel.custom(context));
    }

    @Test public void backupWithoutCurrencyLabelRestoresTheDefault() throws Exception {
        AppSettings existing = new AppSettings();
        existing.setCurrencyLabel("€");
        database.appSettingsDao().insert(existing);
        CurrencyLabel.cache(context, "€");

        assertNull(restore(backup().put("settings", settings())));

        assertNull(database.appSettingsDao().getSettingsSync().getCurrencyLabel());
        assertNull(CurrencyLabel.custom(context));
    }

    @Test public void tooLongCurrencyLabelPreservesOriginalDatabase() throws Exception {
        database.appSettingsDao().insert(new AppSettings());
        JSONObject data = backup().put("settings", settings().put("currencyLabel", "123456789"));

        assertNotNull(restore(data));

        assertOriginal();
        assertNull(database.appSettingsDao().getSettingsSync().getCurrencyLabel());
    }

    @Test public void nonTextCurrencyLabelPreservesOriginalDatabase() throws Exception {
        database.appSettingsDao().insert(new AppSettings());
        JSONObject data = backup().put("settings", settings().put("currencyLabel", 5));

        assertNotNull(restore(data));

        assertOriginal();
    }

    private static JSONObject settings() throws Exception {
        return new JSONObject().put("salaryDay", 1).put("biometricEnabled", false);
    }

    private JSONObject backup() throws Exception {
        JSONObject data = new JSONObject();
        for (String table : new String[]{"family_members", "categories", "payment_methods",
                "debts", "expenses", "income_categories", "incomes", "debt_evidences"}) {
            data.put(table, new JSONArray());
        }
        data.getJSONArray("expenses").put(new JSONObject().put("id", 1)
                .put("amount", 20).put("timestamp", 2000));
        return data;
    }

    private void assertOriginal() {
        assertEquals(1, database.expenseDao().getAllSync().size());
        assertEquals(99, database.expenseDao().getAllSync().get(0).getId());
        assertEquals(45000, database.expenseDao().getAllSync().get(0).getAmount(), 0);
    }

    private String restore(JSONObject data) throws Exception {
        return restore(data, null);
    }

    private String restore(JSONObject data, String extraEntry) throws Exception {
        zip = File.createTempFile("restore-test-", ".zip", context.getCacheDir());
        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(zip))) {
            output.putNextEntry(new ZipEntry("data.json"));
            output.write(data.toString().getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            if (extraEntry != null) {
                output.putNextEntry(new ZipEntry(extraEntry));
                output.write(new byte[]{9, 8, 7});
                output.closeEntry();
            }
        }
        return restoreExistingZip();
    }

    private String restoreExistingZip() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>();
        importer.importData(Uri.fromFile(zip), new DataExportImport.ImportCallback() {
            public void onSuccess() { done.countDown(); }
            public void onError(String message) { error.set(message == null ? "error" : message); done.countDown(); }
        });
        assertTrue("Import did not finish", done.await(15, TimeUnit.SECONDS));
        return error.get();
    }
}
