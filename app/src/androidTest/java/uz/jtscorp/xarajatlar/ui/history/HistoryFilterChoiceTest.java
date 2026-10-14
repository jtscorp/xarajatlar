package uz.jtscorp.xarajatlar.ui.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

import uz.jtscorp.xarajatlar.data.entity.Category;
import uz.jtscorp.xarajatlar.data.entity.FamilyMember;
import uz.jtscorp.xarajatlar.data.entity.PaymentMethod;

@RunWith(AndroidJUnit4.class)
public class HistoryFilterChoiceTest {
    private final Context context =
            InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test
    public void duplicateNamesRemainDistinctAndRestoreExactId() {
        FamilyMember first = familyMember(7, "Ali");
        FamilyMember second = familyMember(8, "Ali");

        List<HistoryFilterBottomSheet.Choice> choices =
                HistoryFilterBottomSheet.relationChoices(context, List.of(first, second));
        HistoryFilterBottomSheet.Choice restored =
                HistoryFilterBottomSheet.findChoice(choices, null, 8);

        assertNotNull(restored);
        assertEquals(Integer.valueOf(8), restored.id);
        assertNotEquals(choices.get(2).label, choices.get(3).label);
    }

    @Test
    public void lookupStatePreservesRelationsAndBlocksApplyUntilChoicesLoad() {
        TransactionFilter current = new TransactionFilter.Builder()
                .category(TransactionFilter.KIND_EXPENSE, 2)
                .familyMemberId(3)
                .paymentMethodId(4)
                .build();
        HistoryFilterBottomSheet.LookupSelection selection =
                new HistoryFilterBottomSheet.LookupSelection(current);

        assertFalse(selection.canApply());
        assertEquals(Integer.valueOf(2), selection.category.id);
        assertEquals(Integer.valueOf(3), selection.family.id);
        assertEquals(Integer.valueOf(4), selection.payment.id);

        selection.complete(context,
                HistoryFilterBottomSheet.categoryChoices(context,
                        List.of(category(2, "Food")), Collections.emptyList()),
                HistoryFilterBottomSheet.relationChoices(context,
                        List.of(familyMember(3, "Ali"))),
                HistoryFilterBottomSheet.paymentChoices(context,
                        List.of(paymentMethod(4, "Cash"))));

        assertTrue(selection.canApply());
        assertEquals(Integer.valueOf(2), selection.category.id);
        assertEquals(Integer.valueOf(3), selection.family.id);
        assertEquals(Integer.valueOf(4), selection.payment.id);

        selection.fail();
        assertFalse(selection.canApply());
        assertTrue(selection.failed);
    }

    @Test
    public void kindIsResolvedByPositionNotByLabelText() {
        assertNull(HistoryFilterBottomSheet.kindAt(0));
        assertEquals(TransactionFilter.KIND_EXPENSE, HistoryFilterBottomSheet.kindAt(1));
        assertEquals(TransactionFilter.KIND_INCOME, HistoryFilterBottomSheet.kindAt(2));
        assertNull(HistoryFilterBottomSheet.kindAt(-1));
    }

    @Test
    public void sortIsResolvedByPositionNotByLabelText() {
        assertEquals(TransactionSort.NEWEST, HistoryFilterBottomSheet.sortAt(0));
        assertEquals(TransactionSort.OLDEST, HistoryFilterBottomSheet.sortAt(1));
        assertEquals(TransactionSort.AMOUNT_DESC, HistoryFilterBottomSheet.sortAt(2));
        assertEquals(TransactionSort.AMOUNT_ASC, HistoryFilterBottomSheet.sortAt(3));
        assertEquals(TransactionSort.NEWEST, HistoryFilterBottomSheet.sortAt(-1));
    }

    @Test
    public void unknownSelectionKeepsItsIdWhenChoicesDoNotContainIt() {
        TransactionFilter current = new TransactionFilter.Builder().familyMemberId(99).build();
        HistoryFilterBottomSheet.LookupSelection selection =
                new HistoryFilterBottomSheet.LookupSelection(current);

        selection.complete(context,
                HistoryFilterBottomSheet.categoryChoices(context,
                        Collections.emptyList(), Collections.emptyList()),
                HistoryFilterBottomSheet.relationChoices(context, Collections.emptyList()),
                HistoryFilterBottomSheet.paymentChoices(context, Collections.emptyList()));

        assertEquals(Integer.valueOf(99), selection.family.id);
        assertTrue(selection.family.label.contains("99"));
    }

    @Test
    public void restoredUnappliedSelectionIsKeptWhenChoicesLoad() {
        HistoryFilterBottomSheet.LookupSelection selection =
                new HistoryFilterBottomSheet.LookupSelection(TransactionFilter.empty());

        selection.restore(
                new HistoryFilterBottomSheet.Choice("", TransactionFilter.KIND_EXPENSE, 2),
                new HistoryFilterBottomSheet.Choice("", null, 3),
                new HistoryFilterBottomSheet.Choice("", null, 4));
        selection.complete(context,
                HistoryFilterBottomSheet.categoryChoices(context,
                        List.of(category(2, "Food")), Collections.emptyList()),
                HistoryFilterBottomSheet.relationChoices(context,
                        List.of(familyMember(3, "Ali"))),
                HistoryFilterBottomSheet.paymentChoices(context,
                        List.of(paymentMethod(4, "Cash"))));

        assertEquals(Integer.valueOf(2), selection.category.id);
        assertEquals(TransactionFilter.KIND_EXPENSE, selection.category.kind);
        assertEquals(Integer.valueOf(3), selection.family.id);
        assertEquals(Integer.valueOf(4), selection.payment.id);
        assertTrue(selection.category.label.contains("Food"));
    }

    @Test
    public void restoreIgnoresMissingValues() {
        TransactionFilter current = new TransactionFilter.Builder().familyMemberId(3).build();
        HistoryFilterBottomSheet.LookupSelection selection =
                new HistoryFilterBottomSheet.LookupSelection(current);

        selection.restore(null, null, null);

        assertEquals(Integer.valueOf(3), selection.family.id);
    }

    private static Category category(int id, String name) {
        Category value = new Category();
        value.setId(id);
        value.setName(name);
        return value;
    }

    private static FamilyMember familyMember(int id, String name) {
        FamilyMember value = new FamilyMember();
        value.setId(id);
        value.setName(name);
        return value;
    }

    private static PaymentMethod paymentMethod(int id, String name) {
        PaymentMethod value = new PaymentMethod();
        value.setId(id);
        value.setName(name);
        return value;
    }
}
