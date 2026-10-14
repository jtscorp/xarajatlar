package uz.jtscorp.xarajatlar.ui.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SubmissionGateTest {
    @Test public void onlyOneSubmissionCanRunUntilReleased() {
        SubmissionGate gate = new SubmissionGate();
        assertTrue(gate.tryAcquire());
        assertFalse(gate.tryAcquire());
        gate.release();
        assertTrue(gate.tryAcquire());
    }
}
