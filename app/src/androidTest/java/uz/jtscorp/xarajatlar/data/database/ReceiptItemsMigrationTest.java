package uz.jtscorp.xarajatlar.data.database;

import static org.junit.Assert.assertEquals;

import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ReceiptItemsMigrationTest {
    @Test public void addsProductsColumnWithoutChangingExistingExpenses() {
        SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(ApplicationProvider.getApplicationContext())
                        .name(null)
                        .callback(new SupportSQLiteOpenHelper.Callback(4) {
                            @Override public void onCreate(SupportSQLiteDatabase db) {
                                db.execSQL("CREATE TABLE expenses (id INTEGER PRIMARY KEY, amount REAL NOT NULL)");
                                db.execSQL("INSERT INTO expenses (id, amount) VALUES (7, 12500)");
                            }
                            @Override public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {}
                        }).build());
        try {
            SupportSQLiteDatabase db = helper.getWritableDatabase();
            AppDatabase.MIGRATION_4_5.migrate(db);
            try (Cursor cursor = db.query("SELECT id, amount, receiptItemsJson FROM expenses")) {
                cursor.moveToFirst();
                assertEquals(7, cursor.getInt(0));
                assertEquals(12500, cursor.getDouble(1), 0.001);
                assertEquals(null, cursor.getString(2));
            }
        } finally {
            helper.close();
        }
    }
}
