package uz.jtscorp.xarajatlar.ui.debts;

import static org.junit.Assert.assertSame;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import uz.jtscorp.xarajatlar.data.entity.Debt;

public class DebtTabsTest {
    @Test public void previousTabsCannotReplaceTheSelectedList() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            SelectedDebtList selected = new SelectedDebtList();
            MutableLiveData<List<Debt>> active = new MutableLiveData<>();
            MutableLiveData<List<Debt>> closed = new MutableLiveData<>();
            AtomicReference<List<Debt>> visible = new AtomicReference<>();
            Observer<List<Debt>> observer = visible::set;
            List<Debt> activeRows = Collections.singletonList(new Debt());
            List<Debt> closedRows = Collections.singletonList(new Debt());
            selected.observeForever(observer);
            try {
                for (int i = 0; i < 3; i++) {
                    selected.select(active);
                    active.setValue(activeRows);
                    assertSame(activeRows, visible.get());
                    selected.select(closed);
                    closed.setValue(closedRows);
                    active.setValue(Collections.emptyList());
                    assertSame(closedRows, visible.get());
                }
            } finally {
                selected.removeObserver(observer);
            }
        });
    }
}
