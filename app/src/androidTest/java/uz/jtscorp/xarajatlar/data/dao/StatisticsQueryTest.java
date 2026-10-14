package uz.jtscorp.xarajatlar.data.dao;

import static org.junit.Assert.assertEquals;
import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.List;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.Category;
import uz.jtscorp.xarajatlar.data.entity.IncomeCategory;
import uz.jtscorp.xarajatlar.data.entity.Expense;
import uz.jtscorp.xarajatlar.data.entity.Income;
import uz.jtscorp.xarajatlar.data.model.TransactionGroup;
import uz.jtscorp.xarajatlar.data.model.TransactionSummary;
import uz.jtscorp.xarajatlar.ui.history.TransactionFilter;

public class StatisticsQueryTest {
    @Test public void groupsAndDrilldownMatchAllTotalsIncludingUnassigned() {
        AppDatabase db = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), AppDatabase.class).build();
        try {
            Category c = new Category(); c.setId(1); c.setName("Bir xil"); c.setActive(false);
            db.categoryDao().insert(c);
            IncomeCategory ic = new IncomeCategory(); ic.setId(1); ic.setName("Bir xil");
            db.incomeCategoryDao().insert(ic);
            for (int n = 0; n < 2; n++) {
                Expense e = new Expense(); e.setTimestamp(1000); e.setAmount((n + 1) * 10);
                e.setCategoryId(n == 0 ? 1 : null); db.expenseDao().insert(e);
                Income i = new Income(); i.setTimestamp(1000); i.setAmount((n + 2) * 20);
                i.setIncomeCategoryId(n == 0 ? 1 : null); db.incomeDao().insert(i);
            }
            Expense excluded = new Expense(); excluded.setTimestamp(2000); excluded.setAmount(999);
            db.expenseDao().insert(excluded);
            for (String kind : List.of("expense", "income")) {
                TransactionFilter base = new TransactionFilter.Builder().kind(kind)
                        .startInclusive(1000L).endExclusive(2000L).build();
                double expected = kind.equals("expense") ? 30 : 100;
                for (TransactionQueryBuilder.Grouping dimension : TransactionQueryBuilder.Grouping.values()) {
                    List<TransactionGroup> groups = db.transactionDao().queryGroups(
                            TransactionQueryBuilder.groups(base, dimension).asRoomQuery());
                    assertEquals(expected, groups.stream().mapToDouble(g -> g.amount).sum(), 0);
                    for (TransactionGroup group : groups) {
                        TransactionFilter.Builder drilldown = base.buildUpon();
                        switch (dimension) {
                            case CATEGORY: drilldown.category(kind, group.id); break;
                            case FAMILY: drilldown.familyMemberId(group.id); break;
                            case PAYMENT: drilldown.paymentMethodId(group.id); break;
                        }
                        TransactionSummary total = db.transactionDao().querySummary(
                                TransactionQueryBuilder.summary(drilldown.build()).asRoomQuery());
                        assertEquals(group.amount, total.getIncomeTotal() + total.getExpenseTotal(), 0);
                    }
                }
            }
            TransactionFilter empty = new TransactionFilter.Builder().kind("income")
                    .startInclusive(3000L).endExclusive(4000L).build();
            assertEquals(0, db.transactionDao().queryGroups(TransactionQueryBuilder.groups(
                    empty, TransactionQueryBuilder.Grouping.CATEGORY).asRoomQuery()).size());
            assertEquals(0, db.transactionDao().querySummary(
                    TransactionQueryBuilder.summary(empty).asRoomQuery()).getIncomeTotal(), 0);
        } finally { db.close(); }
    }
}
