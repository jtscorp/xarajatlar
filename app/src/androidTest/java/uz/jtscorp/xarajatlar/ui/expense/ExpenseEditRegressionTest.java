package uz.jtscorp.xarajatlar.ui.expense;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;

import uz.jtscorp.xarajatlar.data.entity.Expense;

/** Verifies the real edit mutator and Room update preserve non-editable metadata. */
public class ExpenseEditRegressionTest {

    @Test
    public void noteEdit_preservesPaymentAndReceiptMetadata() {
        Expense expense = new Expense();
        expense.setId(42);
        expense.setPaymentMethodId(7);
        expense.setTimestamp(123456L);
        expense.setShopName("Do'kon");
        expense.setReceiptUrl("https://example.test/receipt");

        AppDatabase database = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), AppDatabase.class).build();
        try {
            PaymentMethod payment = new PaymentMethod();
            payment.setId(7);
            payment.setName("Test karta");
            database.paymentMethodDao().insert(payment);
            PaymentMethod nextPayment = new PaymentMethod();
            nextPayment.setId(9);
            nextPayment.setName("Yangi karta");
            database.paymentMethodDao().insert(nextPayment);
            database.expenseDao().insert(expense);
            ManualExpenseActivity.applyEditableFields(
                    expense, 125_000, null, null, 9, 987654L, "Yangi izoh");
            database.expenseDao().update(expense);
            expense = database.expenseDao().getById(42);

        assertEquals(Integer.valueOf(9), expense.getPaymentMethodId());
        assertEquals(987654L, expense.getTimestamp());
        assertEquals("Do'kon", expense.getShopName());
        assertEquals("https://example.test/receipt", expense.getReceiptUrl());
        assertEquals("Yangi izoh", expense.getDescription());
        } finally {
            database.close();
        }
    }
}
