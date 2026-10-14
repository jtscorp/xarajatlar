package uz.jtscorp.xarajatlar.data.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.Category;
import uz.jtscorp.xarajatlar.data.entity.Expense;
import uz.jtscorp.xarajatlar.data.entity.FamilyMember;
import uz.jtscorp.xarajatlar.data.entity.Income;
import uz.jtscorp.xarajatlar.data.entity.IncomeCategory;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;
import uz.jtscorp.xarajatlar.data.model.TransactionRow;
import uz.jtscorp.xarajatlar.data.model.TransactionSummary;
import uz.jtscorp.xarajatlar.ui.history.TransactionFilter;
import uz.jtscorp.xarajatlar.ui.history.TransactionSort;

@RunWith(AndroidJUnit4.class)
public class TransactionFilterQueryTest {
    private AppDatabase database;
    private TransactionDao dao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = database.transactionDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void fourPagesReturnEveryCollidingIdentityExactlyOnce() {
        seedLargeHistory();
        TransactionFilter all = TransactionFilter.empty();
        Set<String> identities = new HashSet<>();

        for (int offset = 0; offset < 200; offset += 50) {
            List<TransactionRow> page = dao.queryRows(
                    TransactionQueryBuilder.page(all, TransactionSort.NEWEST, 50, offset).asRoomQuery());
            for (TransactionRow row : page) {
                assertTrue("duplicate " + row.stableId(), identities.add(row.stableId()));
            }
        }

        assertEquals(160, identities.size());
        assertTrue(identities.contains("expense:21"));
        assertTrue(identities.contains("income:11"));
    }

    @Test
    public void combinedRelationsDateAmountAndArchivedCategoryFilterUseAndSemantics() {
        seedLookups();
        insertExpense(1, 500, 150, 2, 1, 1, "Mos yozuv", "Do'kon");
        insertExpense(2, 700, 150, 2, null, 1, "Oila yo'q", "Do'kon");
        insertExpense(3, 500, 250, 2, 1, 1, "Sana tashqarida", "Do'kon");
        insertIncome(1, 500, 150, 2, 1, 1, "ID teng", "Manba");

        TransactionFilter filter = new TransactionFilter.Builder()
                .kind(TransactionFilter.KIND_EXPENSE)
                .startInclusive(100L)
                .endExclusive(200L)
                .category(TransactionFilter.KIND_EXPENSE, 2)
                .familyMemberId(1)
                .paymentMethodId(1)
                .minAmount(400.0)
                .maxAmount(600.0)
                .build();

        List<TransactionRow> rows = rows(filter, 0);
        assertEquals(1, rows.size());
        assertEquals("expense:1", rows.get(0).stableId());
        assertEquals("Arxiv", rows.get(0).getCategoryName());
    }

    @Test
    public void unassignedAndCategoryKindKeepEqualCategoryIdsSeparate() {
        seedLookups();
        insertExpense(1, 10, 10, null, null, null, "x", null);
        insertExpense(2, 20, 20, 1, 1, 1, "x", null);
        insertIncome(1, 30, 30, 1, 1, 1, "x", null);

        TransactionFilter unassigned = new TransactionFilter.Builder()
                .familyMemberId(TransactionFilter.UNASSIGNED)
                .paymentMethodId(TransactionFilter.UNASSIGNED)
                .build();
        assertEquals("expense:1", rows(unassigned, 0).get(0).stableId());

        TransactionFilter incomeCategory = new TransactionFilter.Builder()
                .category(TransactionFilter.KIND_INCOME, 1)
                .build();
        List<TransactionRow> rows = rows(incomeCategory, 0);
        assertEquals(1, rows.size());
        assertEquals("income:1", rows.get(0).stableId());
    }

    @Test
    public void unicodeApostropheAndLiteralGlobCharactersSearchThroughRealSqlite() {
        seedLookups();
        insertExpense(1, 10, 10, 1, 1, 1, "O‘RIK *?[ ШЎ", "Bozor");
        insertExpense(2, 20, 20, 1, 1, 1, "orik boshqa", "Bozor");

        TransactionFilter filter = new TransactionFilter.Builder().query("o'rik *?[ шў").build();
        List<TransactionRow> rows = rows(filter, 0);

        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getId());
    }

    @Test
    public void summaryCoversAllMatchesRegardlessOfPageOffset() {
        seedLargeHistory();
        TransactionFilter all = TransactionFilter.empty();

        assertEquals(50, rows(all, 0).size());
        assertEquals(50, rows(all, 100).size());
        TransactionSummary summary = dao.querySummary(TransactionQueryBuilder.summary(all).asRoomQuery());

        assertNotNull(summary);
        assertEquals(160, summary.getCount());
        assertEquals(630.0, summary.getIncomeTotal(), 0.001);
        assertEquals(7875.0, summary.getExpenseTotal(), 0.001);
        assertEquals(-7245.0, summary.getBalance(), 0.001);
    }

    private List<TransactionRow> rows(TransactionFilter filter, int offset) {
        return dao.queryRows(TransactionQueryBuilder.page(
                filter, TransactionSort.NEWEST, 50, offset).asRoomQuery());
    }

    private void seedLargeHistory() {
        seedLookups();
        for (int id = 1; id <= 125; id++) {
            insertExpense(id, id, 1_000 - (id / 3), 1, 1, 1, "Xarajat " + id, "Do'kon");
        }
        for (int id = 1; id <= 35; id++) {
            insertIncome(id, id, 1_000 - (id / 3), 1, 1, 1, "Daromad " + id, "Ish");
        }
    }

    private void seedLookups() {
        Category active = new Category();
        active.setId(1); active.setName("Oziq-ovqat"); active.setIconName("🍔"); active.setColor(0xff00aa00); active.setActive(true);
        database.categoryDao().insert(active);
        Category archived = new Category();
        archived.setId(2); archived.setName("Arxiv"); archived.setIconName("📦"); archived.setColor(0xff777777); archived.setActive(false);
        database.categoryDao().insert(archived);
        IncomeCategory income = new IncomeCategory();
        income.setId(1); income.setName("Oylik"); income.setEmoji("💰"); income.setActive(true);
        database.incomeCategoryDao().insert(income);
        IncomeCategory incomeTwo = new IncomeCategory();
        incomeTwo.setId(2); incomeTwo.setName("Bonus"); incomeTwo.setEmoji("🎁"); incomeTwo.setActive(true);
        database.incomeCategoryDao().insert(incomeTwo);
        FamilyMember member = new FamilyMember();
        member.setId(1); member.setName("Шаҳло"); member.setActive(true);
        database.familyMemberDao().insert(member);
        PaymentMethod payment = new PaymentMethod();
        payment.setId(1); payment.setName("Naqd"); payment.setColor(0xff000000); payment.setCash(true);
        database.paymentMethodDao().insert(payment);
    }

    private void insertExpense(int id, double amount, long timestamp, Integer categoryId,
                               Integer familyId, Integer paymentId, String description, String source) {
        Expense value = new Expense();
        value.setId(id); value.setAmount(amount); value.setTimestamp(timestamp); value.setCategoryId(categoryId);
        value.setFamilyMemberId(familyId); value.setPaymentMethodId(paymentId); value.setDescription(description); value.setShopName(source);
        database.expenseDao().insert(value);
    }

    private void insertIncome(int id, double amount, long timestamp, Integer categoryId,
                              Integer familyId, Integer paymentId, String description, String source) {
        Income value = new Income();
        value.setId(id); value.setAmount(amount); value.setTimestamp(timestamp); value.setIncomeCategoryId(categoryId);
        value.setFamilyMemberId(familyId); value.setPaymentMethodId(paymentId); value.setDescription(description); value.setSource(source);
        database.incomeDao().insert(value);
    }
}
