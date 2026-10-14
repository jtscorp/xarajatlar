package uz.jtscorp.xarajatlar.data.dao;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import uz.jtscorp.xarajatlar.ui.history.TransactionFilter;
import uz.jtscorp.xarajatlar.ui.history.TransactionSort;

public class TransactionQueryBuilderTest {

    @Test
    public void pageAndSummaryUseTheSameBoundPredicate() {
        TransactionFilter filter = new TransactionFilter.Builder()
                .query("O'rik *?[ ШЎ")
                .kind("expense")
                .startInclusive(100L)
                .endExclusive(200L)
                .category("expense", 4)
                .familyMemberId(0)
                .paymentMethodId(8)
                .minAmount(12.5)
                .maxAmount(99.5)
                .build();

        TransactionQueryBuilder.BuiltQuery page = TransactionQueryBuilder.page(
                filter, TransactionSort.AMOUNT_DESC, 50, 100);
        TransactionQueryBuilder.BuiltQuery summary = TransactionQueryBuilder.summary(filter);

        assertEquals(summary.predicateSql(), page.predicateSql());
        assertArrayEquals(summary.predicateArgs(), page.predicateArgs());
        assertTrue(page.sql().endsWith("ORDER BY amount DESC, timestamp DESC, kind ASC, id DESC LIMIT ? OFFSET ?"));
        assertFalse(page.sql().contains("O'rik"));
        assertEquals(11, page.args().length);
        assertEquals(50, page.args()[9]);
        assertEquals(100, page.args()[10]);
    }

    @Test
    public void globPatternHandlesUnicodeApostrophesAndLiteralWildcards() {
        String pattern = TransactionQueryBuilder.toGlobSubstringPattern("O'rik *?[ ШЎ");

        assertEquals("*[oO]['’‘ʻʼ`][rR][iI][kK] [*][?][[] [шШ][ўЎ]*", pattern);
    }

    @Test(expected = IllegalArgumentException.class)
    public void filterRejectsReversedAmountRange() {
        new TransactionFilter.Builder().minAmount(20.0).maxAmount(10.0).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void filterRejectsCategoryIdWithoutCategoryKind() {
        new TransactionFilter.Builder().category(null, 3).build();
    }
}
