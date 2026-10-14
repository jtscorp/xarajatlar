package uz.jtscorp.xarajatlar.data.database;

import android.content.Context;
import android.content.res.Resources;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.room.migration.Migration;

import uz.jtscorp.xarajatlar.R;
import uz.jtscorp.xarajatlar.data.dao.*;
import uz.jtscorp.xarajatlar.data.entity.*;

@Database(entities = {
        FamilyMember.class,
        Category.class,
        PaymentMethod.class,
        Expense.class,
        Income.class,
        IncomeCategory.class,
        Debt.class,
        DebtEvidence.class,
        AppSettings.class
}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // DAOs
    public abstract FamilyMemberDao familyMemberDao();
    public abstract CategoryDao categoryDao();
    public abstract PaymentMethodDao paymentMethodDao();
    public abstract ExpenseDao expenseDao();
    public abstract IncomeDao incomeDao();
    public abstract IncomeCategoryDao incomeCategoryDao();
    public abstract DebtDao debtDao();
    public abstract DebtEvidenceDao debtEvidenceDao();
    public abstract AppSettingsDao appSettingsDao();
    public abstract TransactionDao transactionDao();

    // Singleton
    private static volatile AppDatabase INSTANCE;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(4);

    // Migration: Fix category icons from placeholder to emoji
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Update default category icons to emojis
            database.execSQL("UPDATE categories SET iconName = '🍔' WHERE name = 'Oziq-ovqat'");
            database.execSQL("UPDATE categories SET iconName = '🚗' WHERE name = 'Transport'");
            database.execSQL("UPDATE categories SET iconName = '👕' WHERE name = 'Kiyim'");
            database.execSQL("UPDATE categories SET iconName = '🏠' WHERE name = 'Kommunal'");
            database.execSQL("UPDATE categories SET iconName = '💊' WHERE name = 'Salomatlik'");
            database.execSQL("UPDATE categories SET iconName = '📚' WHERE name = \"Ta'lim\"");
            database.execSQL("UPDATE categories SET iconName = '🎮' WHERE name = \"Ko'ngilochar\"");
            database.execSQL("UPDATE categories SET iconName = '📦' WHERE name = 'Boshqa'");
        }
    };

    // Migration: Add emoji field to FamilyMember
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE family_members ADD COLUMN emoji TEXT");
        }
    };

    // Migration: Add income_categories table and extend incomes table
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS income_categories (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT, " +
                    "emoji TEXT, " +
                    "isActive INTEGER NOT NULL DEFAULT 1)");

            database.execSQL("ALTER TABLE incomes ADD COLUMN incomeCategoryId INTEGER");
            database.execSQL("ALTER TABLE incomes ADD COLUMN familyMemberId INTEGER");
            database.execSQL("ALTER TABLE incomes ADD COLUMN paymentMethodId INTEGER");
            database.execSQL("ALTER TABLE incomes ADD COLUMN description TEXT");

            database.execSQL("CREATE INDEX IF NOT EXISTS index_incomes_incomeCategoryId ON incomes(incomeCategoryId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_incomes_familyMemberId ON incomes(familyMemberId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_incomes_paymentMethodId ON incomes(paymentMethodId)");

            database.execSQL("INSERT INTO income_categories (name, emoji, isActive) VALUES ('Oylik', '\uD83D\uDCB0', 1)");
            database.execSQL("INSERT INTO income_categories (name, emoji, isActive) VALUES ('Bonus', '\uD83C\uDF81', 1)");
            database.execSQL("INSERT INTO income_categories (name, emoji, isActive) VALUES ('Freelance', '\uD83D\uDCBB', 1)");
            database.execSQL("INSERT INTO income_categories (name, emoji, isActive) VALUES ('Ijara', '\uD83C\uDFE0', 1)");
            database.execSQL("INSERT INTO income_categories (name, emoji, isActive) VALUES ('Boshqa', '\uD83D\uDCE6', 1)");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE expenses ADD COLUMN receiptItemsJson TEXT");
        }
    };

    // Migration: user-selectable currency label (null = default for the current language)
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE app_settings ADD COLUMN currencyLabel TEXT");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "xarajatlar_database"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            databaseWriteExecutor.execute(() -> {
                                populateDefaultData(context);
                            });
                        }
                    })
                    .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void populateDefaultData(Context context) {
        AppDatabase db = getDatabase(context);
        // Nomlar o'rnatish paytidagi ilova tilida yoziladi; keyin til almashsa o'zgarmaydi.
        Resources resources = context.getResources();

        // Default kategoriyalar
        String[] categories = resources.getStringArray(R.array.default_expense_categories);
        String[] emojis = {"🍔", "🚗", "👕", "🏠", "💊", "📚", "🎮", "📦"};
        int[] colors = {0xFF4CAF50, 0xFF2196F3, 0xFF9C27B0, 0xFFFF9800,
                       0xFFF44336, 0xFF3F51B5, 0xFFE91E63, 0xFF607D8B};

        for (int i = 0; i < categories.length; i++) {
            Category category = new Category();
            category.setName(categories[i]);
            category.setIconName(emojis[i]);
            category.setColor(colors[i]);
            db.categoryDao().insert(category);
        }

        // Default to'lov usuli - Naqd
        PaymentMethod cash = new PaymentMethod();
        cash.setName(resources.getString(R.string.default_cash_name));
        cash.setCash(true);
        cash.setColor(0xFF4CAF50);
        db.paymentMethodDao().insert(cash);

        // Default daromad kategoriyalari
        String[] incomeNames = resources.getStringArray(R.array.default_income_categories);
        String[] incomeEmojis = {"\uD83D\uDCB0", "\uD83C\uDF81", "\uD83D\uDCBB", "\uD83C\uDFE0", "\uD83D\uDCE6"};
        for (int i = 0; i < incomeNames.length; i++) {
            IncomeCategory ic = new IncomeCategory();
            ic.setName(incomeNames[i]);
            ic.setEmoji(incomeEmojis[i]);
            db.incomeCategoryDao().insert(ic);
        }

        // Default sozlamalar
        AppSettings settings = new AppSettings();
        db.appSettingsDao().insert(settings);
    }
}
