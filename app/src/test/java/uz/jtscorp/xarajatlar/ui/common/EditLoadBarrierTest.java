package uz.jtscorp.xarajatlar.ui.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EditLoadBarrierTest {
    @Test public void completesOnceAfterEveryRequiredPartArrives() {
        EditLoadBarrier barrier = new EditLoadBarrier();

        assertFalse(barrier.markComplete(EditLoadBarrier.ENTITY));
        assertFalse(barrier.markComplete(EditLoadBarrier.PAYMENT_METHODS));
        assertFalse(barrier.markComplete(EditLoadBarrier.ENTITY));
        assertFalse(barrier.markComplete(EditLoadBarrier.CATEGORIES));
        assertTrue(barrier.markComplete(EditLoadBarrier.FAMILY_MEMBERS));
        assertFalse(barrier.markComplete(EditLoadBarrier.FAMILY_MEMBERS));
    }
}
