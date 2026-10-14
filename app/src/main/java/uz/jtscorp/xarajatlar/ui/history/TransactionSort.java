package uz.jtscorp.xarajatlar.ui.history;

public enum TransactionSort {
    NEWEST("timestamp DESC, kind ASC, id DESC"),
    OLDEST("timestamp ASC, kind ASC, id ASC"),
    AMOUNT_DESC("amount DESC, timestamp DESC, kind ASC, id DESC"),
    AMOUNT_ASC("amount ASC, timestamp DESC, kind ASC, id DESC");

    private final String orderBy;

    TransactionSort(String orderBy) {
        this.orderBy = orderBy;
    }

    public String orderBy() {
        return orderBy;
    }
}
