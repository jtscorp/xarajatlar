package uz.jtscorp.xarajatlar.data.dao;

import static org.junit.Assert.assertEquals;

import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.Category;
import uz.jtscorp.xarajatlar.data.entity.Expense;
import uz.jtscorp.xarajatlar.data.entity.FamilyMember;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;
import uz.jtscorp.xarajatlar.data.model.TransactionRow;

public class TransactionDetailQueryTest {
    @Test public void detail_returnsEveryDisplayRelationAndReceipt() {
        AppDatabase database = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), AppDatabase.class).build();
        try {
            Category category = new Category();
            category.setId(2); category.setName("Oziq-ovqat");
            database.categoryDao().insert(category);
            FamilyMember member = new FamilyMember();
            member.setId(3); member.setName("Oila");
            database.familyMemberDao().insert(member);
            PaymentMethod payment = new PaymentMethod();
            payment.setId(4); payment.setName("Karta");
            database.paymentMethodDao().insert(payment);
            Expense expense = new Expense();
            expense.setId(9); expense.setAmount(45_000); expense.setCategoryId(2);
            expense.setFamilyMemberId(3); expense.setPaymentMethodId(4);
            expense.setDescription("Bozor"); expense.setShopName("Do'kon");
            expense.setReceiptUrl("https://example.test/receipt");
            database.expenseDao().insert(expense);

            TransactionRow row = database.transactionDao().queryOne(
                    TransactionQueryBuilder.detail("expense", 9).asRoomQuery());

            assertEquals("Oziq-ovqat", row.getCategoryName());
            assertEquals("Oila", row.getFamilyMemberName());
            assertEquals("Karta", row.getPaymentMethodName());
            assertEquals("Do'kon", row.getSourceName());
            assertEquals("https://example.test/receipt", row.getReceiptUrl());
        } finally {
            database.close();
        }
    }
}
