package uz.jtscorp.xarajatlar.ui.expense;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import java.util.Arrays;

public class ShopNameSuggestionsTest {
    @Test public void keepsNewestSpellingAndRemovesWhitespaceAndCaseDuplicates() {
        assertEquals(Arrays.asList("Korzinka", "Makro"), ShopNameSuggestions.unique(
                Arrays.asList("  Korzinka  ", "korzinka", "Makro", "  makro  ",
                        " Korzinka   ", "", null)));
    }

    @Test public void collapsesInternalWhitespaceForMatchingAndSaving() {
        assertEquals("Katta do‘kon", ShopNameSuggestions.normalize("  Katta  \t do‘kon  "));
        assertEquals("Katta do‘kon", ShopNameSuggestions.normalize("Katta\u00a0do‘kon"));
        assertEquals(Arrays.asList("Katta do‘kon"), ShopNameSuggestions.unique(
                Arrays.asList("Katta   do‘kon", "katta do‘kon")));
    }
}
