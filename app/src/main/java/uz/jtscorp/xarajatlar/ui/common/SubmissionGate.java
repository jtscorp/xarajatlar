package uz.jtscorp.xarajatlar.ui.common;

import java.util.concurrent.atomic.AtomicBoolean;

/** Prevents duplicate writes while an asynchronous save is running. */
public final class SubmissionGate {
    private final AtomicBoolean acquired = new AtomicBoolean();

    public boolean tryAcquire() {
        return acquired.compareAndSet(false, true);
    }

    public void release() {
        acquired.set(false);
    }
}
