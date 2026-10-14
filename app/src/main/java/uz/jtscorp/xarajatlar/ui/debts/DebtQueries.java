package uz.jtscorp.xarajatlar.ui.debts;

import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import java.util.ArrayList;
import java.util.List;
import uz.jtscorp.xarajatlar.data.dao.TransactionQueryBuilder;

public final class DebtQueries {
    public static final int ACTIVE = 0, CLOSED = 1, OVERDUE = 2;
    public static final int DUE_DATE = 0, REMAINING_DESC = 1;
    private DebtQueries() {}

    public static SupportSQLiteQuery build(String search, int status, int sort, long todayStart) {
        List<Object> args = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM debts WHERE isFullyPaid = ?");
        args.add(status == CLOSED ? 1 : 0);
        if (status == OVERDUE) {
            sql.append(" AND dueDate > 0 AND dueDate < ?"); args.add(todayStart);
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (personName GLOB ? OR COALESCE(phone, '') GLOB ?)");
            String pattern = TransactionQueryBuilder.toGlobSubstringPattern(search.trim());
            args.add(pattern); args.add(pattern);
        }
        sql.append(sort == REMAINING_DESC
                ? " ORDER BY (amount - paidAmount) DESC, id DESC"
                : " ORDER BY CASE WHEN dueDate > 0 THEN 0 ELSE 1 END, dueDate ASC, id DESC");
        return new SimpleSQLiteQuery(sql.toString(), args.toArray());
    }
}
