package uz.jtscorp.xarajatlar.data.dao;

import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.ArrayList;
import java.util.List;

import uz.jtscorp.xarajatlar.ui.history.TransactionFilter;
import uz.jtscorp.xarajatlar.ui.history.TransactionSort;

public final class TransactionQueryBuilder {
    private static final String PROJECTION = "kind, id, amount, timestamp, categoryId, "
            + "familyMemberId, paymentMethodId, description, sourceName, receiptUrl, "
            + "categoryName, categoryIcon, categoryColor, familyMemberName, paymentMethodName";

    private static final String UNION = "SELECT 'expense' AS kind, e.id AS id, e.amount AS amount, "
            + "e.timestamp AS timestamp, e.categoryId AS categoryId, e.familyMemberId AS familyMemberId, "
            + "e.paymentMethodId AS paymentMethodId, e.description AS description, e.shopName AS sourceName, "
            + "e.receiptUrl AS receiptUrl, c.name AS categoryName, c.iconName AS categoryIcon, "
            + "COALESCE(c.color, 0) AS categoryColor, f.name AS familyMemberName, p.name AS paymentMethodName "
            + "FROM expenses e LEFT JOIN categories c ON c.id = e.categoryId "
            + "LEFT JOIN family_members f ON f.id = e.familyMemberId "
            + "LEFT JOIN payment_methods p ON p.id = e.paymentMethodId "
            + "UNION ALL "
            + "SELECT 'income' AS kind, i.id AS id, i.amount AS amount, i.timestamp AS timestamp, "
            + "i.incomeCategoryId AS categoryId, i.familyMemberId AS familyMemberId, "
            + "i.paymentMethodId AS paymentMethodId, i.description AS description, i.source AS sourceName, "
            + "NULL AS receiptUrl, ic.name AS categoryName, ic.emoji AS categoryIcon, "
            + "0 AS categoryColor, f.name AS familyMemberName, p.name AS paymentMethodName "
            + "FROM incomes i LEFT JOIN income_categories ic ON ic.id = i.incomeCategoryId "
            + "LEFT JOIN family_members f ON f.id = i.familyMemberId "
            + "LEFT JOIN payment_methods p ON p.id = i.paymentMethodId";

    private TransactionQueryBuilder() {}

    public enum Grouping { CATEGORY, FAMILY, PAYMENT }

    public static BuiltQuery detail(String kind, int id) {
        if (!(TransactionFilter.KIND_EXPENSE.equals(kind)
                || TransactionFilter.KIND_INCOME.equals(kind)) || id <= 0) {
            throw new IllegalArgumentException("Operatsiya noto'g'ri");
        }
        String predicate = " WHERE kind = ? AND id = ?";
        Object[] args = {kind, id};
        return new BuiltQuery("SELECT " + PROJECTION + " FROM (" + UNION + ") transaction_rows"
                + predicate + " LIMIT 1", args, predicate, args);
    }

    public static BuiltQuery groups(TransactionFilter filter, Grouping grouping) {
        if (filter.getKind() == null) throw new IllegalArgumentException("Operatsiya turini tanlang");
        String id, name;
        switch (grouping) {
            case CATEGORY: id = "categoryId"; name = "categoryName"; break;
            case FAMILY: id = "familyMemberId"; name = "familyMemberName"; break;
            case PAYMENT: id = "paymentMethodId"; name = "paymentMethodName"; break;
            default: throw new IllegalArgumentException("Kesim noto'g'ri");
        }
        QueryParts parts = buildParts(filter);
        String sql = "SELECT COALESCE(" + id + ", 0) AS id, "
                + "COALESCE(MAX(NULLIF(" + name + ", '')), 'Belgilanmagan') AS name, "
                + (grouping == Grouping.CATEGORY
                    ? "MAX(categoryIcon) AS icon, MAX(categoryColor) AS color, "
                    : "NULL AS icon, 0 AS color, ")
                + "SUM(amount) AS amount, COUNT(*) AS count" + parts.fromAndPredicate
                + " GROUP BY COALESCE(" + id + ", 0) ORDER BY amount DESC, id ASC";
        return new BuiltQuery(sql, parts.args.toArray(), parts.predicateSql, parts.args.toArray());

    }

    public static BuiltQuery page(TransactionFilter filter, TransactionSort sort, int limit, int offset) {
        if (limit <= 0 || offset < 0) throw new IllegalArgumentException("Noto'g'ri sahifa");
        QueryParts parts = buildParts(filter);
        List<Object> args = new ArrayList<>(parts.args);
        args.add(limit);
        args.add(offset);
        String sql = "SELECT " + PROJECTION + parts.fromAndPredicate
                + " ORDER BY " + sort.orderBy() + " LIMIT ? OFFSET ?";
        return new BuiltQuery(sql, args.toArray(), parts.predicateSql, parts.args.toArray());
    }

    public static BuiltQuery summary(TransactionFilter filter) {
        QueryParts parts = buildParts(filter);
        String sql = "SELECT COUNT(*) AS count, "
                + "COALESCE(SUM(CASE WHEN kind = 'income' THEN amount ELSE 0 END), 0) AS incomeTotal, "
                + "COALESCE(SUM(CASE WHEN kind = 'expense' THEN amount ELSE 0 END), 0) AS expenseTotal"
                + parts.fromAndPredicate;
        return new BuiltQuery(sql, parts.args.toArray(), parts.predicateSql, parts.args.toArray());
    }

    private static QueryParts buildParts(TransactionFilter filter) {
        List<String> clauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        StringBuilder from = new StringBuilder(" FROM (").append(UNION).append(") transaction_rows");

        if (filter.getQuery() != null) {
            from.append(" CROSS JOIN (SELECT ? AS pattern) search_arg");
            args.add(toGlobSubstringPattern(filter.getQuery()));
        }
        if (filter.getKind() != null) {
            clauses.add("kind = ?"); args.add(filter.getKind());
        }
        if (filter.getStartInclusive() != null) {
            clauses.add("timestamp >= ?"); args.add(filter.getStartInclusive());
        }
        if (filter.getEndExclusive() != null) {
            clauses.add("timestamp < ?"); args.add(filter.getEndExclusive());
        }
        if (filter.getCategoryKind() != null) {
            clauses.add("kind = ?"); args.add(filter.getCategoryKind());
        }
        appendRelation(clauses, args, "categoryId", filter.getCategoryId());
        appendRelation(clauses, args, "familyMemberId", filter.getFamilyMemberId());
        appendRelation(clauses, args, "paymentMethodId", filter.getPaymentMethodId());
        if (filter.getMinAmount() != null) {
            clauses.add("amount >= ?"); args.add(filter.getMinAmount());
        }
        if (filter.getMaxAmount() != null) {
            clauses.add("amount <= ?"); args.add(filter.getMaxAmount());
        }
        if (filter.getQuery() != null) {
            clauses.add("(COALESCE(description, '') GLOB search_arg.pattern"
                    + " OR COALESCE(sourceName, '') GLOB search_arg.pattern"
                    + " OR COALESCE(categoryName, '') GLOB search_arg.pattern"
                    + " OR COALESCE(familyMemberName, '') GLOB search_arg.pattern"
                    + " OR COALESCE(paymentMethodName, '') GLOB search_arg.pattern)");
        }

        String predicate = clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
        from.append(predicate);
        return new QueryParts(from.toString(), predicate, args);
    }

    private static void appendRelation(List<String> clauses, List<Object> args,
                                       String column, Integer value) {
        if (value == null) return;
        if (value == TransactionFilter.UNASSIGNED) {
            clauses.add(column + " IS NULL");
        } else {
            clauses.add(column + " = ?");
            args.add(value);
        }
    }

    public static String toGlobSubstringPattern(String query) {
        StringBuilder result = new StringBuilder("*");
        query.codePoints().forEach(codePoint -> appendGlobCodePoint(result, codePoint));
        return result.append('*').toString();
    }

    private static void appendGlobCodePoint(StringBuilder result, int codePoint) {
        if (isApostrophe(codePoint)) {
            result.append("['’‘ʻʼ`]");
        } else if (codePoint == '*') {
            result.append("[*]");
        } else if (codePoint == '?') {
            result.append("[?]");
        } else if (codePoint == '[') {
            result.append("[[]");
        } else {
            int lower = Character.toLowerCase(codePoint);
            int upper = Character.toUpperCase(codePoint);
            if (lower != upper) {
                result.append('[').appendCodePoint(lower).appendCodePoint(upper).append(']');
            } else {
                result.appendCodePoint(codePoint);
            }
        }
    }

    private static boolean isApostrophe(int value) {
        return value == '\'' || value == '’' || value == '‘' || value == 'ʻ'
                || value == 'ʼ' || value == '`';
    }

    private static final class QueryParts {
        final String fromAndPredicate;
        final String predicateSql;
        final List<Object> args;

        QueryParts(String fromAndPredicate, String predicateSql, List<Object> args) {
            this.fromAndPredicate = fromAndPredicate;
            this.predicateSql = predicateSql;
            this.args = args;
        }
    }

    public static final class BuiltQuery {
        private final String sql;
        private final Object[] args;
        private final String predicateSql;
        private final Object[] predicateArgs;

        BuiltQuery(String sql, Object[] args, String predicateSql, Object[] predicateArgs) {
            this.sql = sql;
            this.args = args;
            this.predicateSql = predicateSql;
            this.predicateArgs = predicateArgs;
        }

        public String sql() { return sql; }
        public Object[] args() { return args.clone(); }
        public String predicateSql() { return predicateSql; }
        public Object[] predicateArgs() { return predicateArgs.clone(); }
        public SupportSQLiteQuery asRoomQuery() { return new SimpleSQLiteQuery(sql, args); }
    }
}
