package uz.jtscorp.xarajatlar.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ReceiptUrlsTest {
    @Test public void acceptsOnlyTrustedHttpsReceiptUrls() {
        String valid = "https://ofd.soliq.uz/check?t=123";
        assertEquals(valid, ReceiptUrls.normalizeOrNull("  " + valid + "  "));
        assertEquals(valid, ReceiptUrls.normalizeOrNull("https://OFD.SOLIQ.UZ/check?t=123"));

        assertNull(ReceiptUrls.normalizeOrNull("http://ofd.soliq.uz/check"));
        assertNull(ReceiptUrls.normalizeOrNull("https://ofd.soliq.uz.evil.test/check"));
        assertNull(ReceiptUrls.normalizeOrNull("https://evil.test/?next=ofd.soliq.uz"));
        assertNull(ReceiptUrls.normalizeOrNull("https://user@ofd.soliq.uz/check"));
        assertNull(ReceiptUrls.normalizeOrNull("https:///missing-host"));
        assertNull(ReceiptUrls.normalizeOrNull("javascript:alert(1)"));
        assertNull(ReceiptUrls.normalizeOrNull("https://ofd.soliq.uz:8443/check"));
    }

    @Test public void resolvesOnlyRedirectsThatStayOnTrustedHost() {
        String current = "https://ofd.soliq.uz/check/start";
        assertEquals("https://ofd.soliq.uz/check/next?t=1",
                ReceiptUrls.resolveTrustedRedirectOrNull(current, "next?t=1"));
        assertEquals("https://ofd.soliq.uz/final",
                ReceiptUrls.resolveTrustedRedirectOrNull(current,
                        "https://ofd.soliq.uz/final"));
        assertNull(ReceiptUrls.resolveTrustedRedirectOrNull(current,
                "https://internal.test/private"));
        assertNull(ReceiptUrls.resolveTrustedRedirectOrNull(current,
                "//evil.test/private"));
        assertNull(ReceiptUrls.resolveTrustedRedirectOrNull(current, null));
    }
}
