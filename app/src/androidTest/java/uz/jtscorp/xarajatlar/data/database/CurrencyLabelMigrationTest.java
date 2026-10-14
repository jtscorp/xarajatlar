package uz.jtscorp.xarajatlar.data.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CurrencyLabelMigrationTest {
    @Test public void addsNullCurrencyLabelWithoutChangingExistingSettings() {
        SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
                        .name(null)
                        .callback(new SupportSQLiteOpenHelper.Callback(5) {
                            @Override public void onCreate(SupportSQLiteDatabase db) {
                                db.execSQL("CREATE TABLE app_settings (id INTEGER PRIMARY KEY NOT NULL, "
                                        + "salaryDay INTEGER NOT NULL, biometricEnabled INTEGER NOT NULL)");
                                db.execSQL("INSERT INTO app_settings (id, salaryDay, biometricEnabled) VALUES (1, 15, 1)");
                            }
                            @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {}
                        }).build());
        try {
            SupportSQLiteDatabase db = helper.getWritableDatabase();
            AppDatabase.MIGRATION_5_6.migrate(db);
            try (Cursor cursor = db.query("SELECT id, salaryDay, biometricEnabled, currencyLabel FROM app_settings")) {
                assertTrue(cursor.moveToFirst());
                assertEquals(1, cursor.getInt(0));
                assertEquals(15, cursor.getInt(1));
                assertEquals(1, cursor.getInt(2));
                assertTrue(cursor.isNull(3));
            }
        } finally {
            helper.close();
        }
    }
}
