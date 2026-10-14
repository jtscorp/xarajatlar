package uz.jtscorp.xarajatlar.ui.income;

import static org.junit.Assert.assertEquals;

import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import uz.jtscorp.xarajatlar.data.database.AppDatabase;
import uz.jtscorp.xarajatlar.data.entity.Income;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;

public class IncomeEditRegressionTest {
    @Test public void edit_updatesVisibleFields_andPreservesOriginalSource() {
        AppDatabase database = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), AppDatabase.class).build();
        try {
            PaymentMethod payment = new PaymentMethod();
            payment.setId(4);
            payment.setName("Karta");
            database.paymentMethodDao().insert(payment);
            Income income = new Income();
            income.setId(21);
            income.setAmount(1_000);
            income.setSource("Bank importi");
            income.setPaymentMethodId(4);
            income.setTimestamp(100L);
            database.incomeDao().insert(income);

            AddIncomeActivity.applyEditableFields(
                    income, 2_000, null, null, 4, 200L, "Oylik izohi");
            database.incomeDao().update(income);
            Income updated = database.incomeDao().getById(21);

            assertEquals(2_000, updated.getAmount(), 0.01);
            assertEquals(Integer.valueOf(4), updated.getPaymentMethodId());
            assertEquals(200L, updated.getTimestamp());
            assertEquals("Oylik izohi", updated.getDescription());
            assertEquals("Bank importi", updated.getSource());
        } finally {
            database.close();
        }
    }
}
