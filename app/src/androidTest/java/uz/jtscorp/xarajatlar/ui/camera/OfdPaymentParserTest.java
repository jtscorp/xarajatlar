package uz.jtscorp.xarajatlar.ui.camera;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class OfdPaymentParserTest {
    @Test public void usesCashAndCardTotalsFromSignedResponse() {
        assertEquals(199468, OfdPaymentParser.total(
                "{\"data\":{\"cashTotal\":0,\"cardTotal\":199468,\"vatTotal\":21371.56}}"), 0.001);
        assertEquals(100000, OfdPaymentParser.total(
                "{\"data\":{\"cashTotal\":40000,\"cardTotal\":60000}}"), 0.001);
    }

    @Test public void rejectsNonReceiptResponses() {
        assertEquals(0, OfdPaymentParser.total("{\"success\":false,\"data\":null}"), 0.001);
        assertEquals(0, OfdPaymentParser.total("garbage"), 0.001);
    }

    @Test public void keepsReceiptProductsFromSignedResponse() {
        String response = "{\"data\":{\"paymentDetails\":[{\"name\":\"Non\",\"amount\":2,\"price\":5000},"
                + "{\"name\":\"Sut\",\"amount\":1,\"price\":12000}]}}";
        assertEquals("[{\"name\":\"Non\",\"amount\":2,\"price\":5000},{\"name\":\"Sut\",\"amount\":1,\"price\":12000}]",
                OfdPaymentParser.itemsJson(response));
        assertEquals(null, OfdPaymentParser.itemsJson("{\"data\":{}}"));
    }
}
