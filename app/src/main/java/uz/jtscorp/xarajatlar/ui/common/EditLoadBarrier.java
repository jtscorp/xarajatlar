package uz.jtscorp.xarajatlar.ui.common;

/** Opens an edit form exactly once after every asynchronous dependency has loaded. */
public final class EditLoadBarrier {
    public static final int ENTITY = 1;
    public static final int CATEGORIES = 1 << 1;
    public static final int FAMILY_MEMBERS = 1 << 2;
    public static final int PAYMENT_METHODS = 1 << 3;
    private static final int ALL = ENTITY | CATEGORIES | FAMILY_MEMBERS | PAYMENT_METHODS;

    private int completed;
    private boolean delivered;

    public boolean markComplete(int part) {
        completed |= part;
        if (!delivered && completed == ALL) {
            delivered = true;
            return true;
        }
        return false;
    }
}
