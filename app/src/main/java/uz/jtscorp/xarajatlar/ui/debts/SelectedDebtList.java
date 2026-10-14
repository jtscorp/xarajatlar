package uz.jtscorp.xarajatlar.ui.debts;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import java.util.List;
import uz.jtscorp.xarajatlar.data.entity.Debt;

/** Keeps only the currently selected debt query connected to the screen. */
final class SelectedDebtList extends MediatorLiveData<List<Debt>> {
    private LiveData<List<Debt>> source;

    void select(LiveData<List<Debt>> next) {
        if (source == next) return;
        if (source != null) removeSource(source);
        source = next;
        addSource(next, this::setValue);
    }
}
