package uz.jtscorp.xarajatlar.ui.camera;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReceiptTextParserTest {
    @Test public void parsesCurrentCheckReceipt() {
        String page = "SOLIQ\nmobile ilovasi\nSavdo cheki/Sotuv\n"
                + "\"ANGLESEY FOOD\" MAS`ULIYATI CHEKLANGAN JAMIYAT XORIJIY KORXONA\n"
                + "г. Ташкент\nChek raqami: 22992\nBank kartalari\t199,468.00\n"
                + "Jami to'lov\t199,468.00\nUmumiy QQS qiymati\t21,371.56";
        ReceiptTextParser.Result result = ReceiptTextParser.parse(page);
        assertEquals(199468.0, result.amount, 0.001);
        assertEquals("ANGLESEY FOOD", result.shopName);
    }

    @Test public void parsesEpiReceipt() {
        String page = "Savdo cheki/Sotuv\n\"PAYNET\" AJ\n"
                + "Мухандислар МФЙ\nChek raqami: 7260467797\n"
                + "Jami to'lov\t100,000.00\nUmumiy QQS qiymati\t0.00";
        ReceiptTextParser.Result result = ReceiptTextParser.parse(page);
        assertEquals(100000.0, result.amount, 0.001);
        assertEquals("PAYNET", result.shopName);
    }

    @Test public void skipsBlankLinesBeforeShopName() {
        String page = "Savdo cheki/Sotuv\n\n\"ANGLESEY FOOD\" MAS`ULIYATI CHEKLANGAN JAMIYAT\n"
                + "Jami to'lov\n199,468.00";
        assertEquals("ANGLESEY FOOD", ReceiptTextParser.parse(page).shopName);
    }

    @Test public void doesNotMistakeLoadingShellForReceipt() {
        ReceiptTextParser.Result result = ReceiptTextParser.parse("SOLIQ mobile ilovasi");
        assertEquals(0, result.amount, 0.001);
        assertEquals("", result.shopName);
    }
}
