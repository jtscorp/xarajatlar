package uz.jtscorp.xarajatlar.ui.history;

import java.io.Serializable;
import java.util.Objects;

public final class TransactionFilter implements Serializable {
    public static final String KIND_EXPENSE = "expense";
    public static final String KIND_INCOME = "income";
    public static final int UNASSIGNED = 0;

    private final String query;
    private final String kind;
    private final Long startInclusive;
    private final Long endExclusive;
    private final String categoryKind;
    private final Integer categoryId;
    private final Integer familyMemberId;
    private final Integer paymentMethodId;
    private final Double minAmount;
    private final Double maxAmount;

    private TransactionFilter(Builder builder) {
        query = normalizeQuery(builder.query);
        kind = validateKind(builder.kind, "kind");
        startInclusive = builder.startInclusive;
        endExclusive = builder.endExclusive;
        categoryKind = validateKind(builder.categoryKind, "categoryKind");
        categoryId = builder.categoryId;
        familyMemberId = builder.familyMemberId;
        paymentMethodId = builder.paymentMethodId;
        minAmount = builder.minAmount;
        maxAmount = builder.maxAmount;
        validate();
    }

    public static TransactionFilter empty() {
        return new Builder().build();
    }

    public Builder buildUpon() {
        return new Builder(this);
    }

    private void validate() {
        if (startInclusive != null && endExclusive != null && startInclusive >= endExclusive) {
            throw new IllegalArgumentException("startInclusive must be before endExclusive");
        }
        if (minAmount != null && minAmount < 0 || maxAmount != null && maxAmount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        if (minAmount != null && maxAmount != null && minAmount > maxAmount) {
            throw new IllegalArgumentException("minAmount must not exceed maxAmount");
        }
        if (categoryId != null && categoryKind == null) {
            throw new IllegalArgumentException("categoryKind is required when categoryId is set");
        }
        validateRelation(categoryId, "categoryId");
        validateRelation(familyMemberId, "familyMemberId");
        validateRelation(paymentMethodId, "paymentMethodId");
    }

    private static void validateRelation(Integer value, String name) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }

    private static String validateKind(String value, String name) {
        if (value == null) return null;
        if (!KIND_EXPENSE.equals(value) && !KIND_INCOME.equals(value)) {
            throw new IllegalArgumentException(name + " is invalid");
        }
        return value;
    }

    private static String normalizeQuery(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public boolean hasActiveFilters() {
        return query != null || kind != null || startInclusive != null || endExclusive != null
                || categoryId != null || familyMemberId != null || paymentMethodId != null
                || minAmount != null || maxAmount != null;
    }

    public String getQuery() { return query; }
    public String getKind() { return kind; }
    public Long getStartInclusive() { return startInclusive; }
    public Long getEndExclusive() { return endExclusive; }
    public String getCategoryKind() { return categoryKind; }
    public Integer getCategoryId() { return categoryId; }
    public Integer getFamilyMemberId() { return familyMemberId; }
    public Integer getPaymentMethodId() { return paymentMethodId; }
    public Double getMinAmount() { return minAmount; }
    public Double getMaxAmount() { return maxAmount; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof TransactionFilter)) return false;
        TransactionFilter that = (TransactionFilter) object;
        return Objects.equals(query, that.query) && Objects.equals(kind, that.kind)
                && Objects.equals(startInclusive, that.startInclusive)
                && Objects.equals(endExclusive, that.endExclusive)
                && Objects.equals(categoryKind, that.categoryKind)
                && Objects.equals(categoryId, that.categoryId)
                && Objects.equals(familyMemberId, that.familyMemberId)
                && Objects.equals(paymentMethodId, that.paymentMethodId)
                && Objects.equals(minAmount, that.minAmount)
                && Objects.equals(maxAmount, that.maxAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, kind, startInclusive, endExclusive, categoryKind,
                categoryId, familyMemberId, paymentMethodId, minAmount, maxAmount);
    }

    public static final class Builder {
        private String query;
        private String kind;
        private Long startInclusive;
        private Long endExclusive;
        private String categoryKind;
        private Integer categoryId;
        private Integer familyMemberId;
        private Integer paymentMethodId;
        private Double minAmount;
        private Double maxAmount;

        public Builder() {}

        public Builder(TransactionFilter source) {
            query = source.query;
            kind = source.kind;
            startInclusive = source.startInclusive;
            endExclusive = source.endExclusive;
            categoryKind = source.categoryKind;
            categoryId = source.categoryId;
            familyMemberId = source.familyMemberId;
            paymentMethodId = source.paymentMethodId;
            minAmount = source.minAmount;
            maxAmount = source.maxAmount;
        }

        public Builder query(String value) { query = value; return this; }
        public Builder kind(String value) { kind = value; return this; }
        public Builder startInclusive(Long value) { startInclusive = value; return this; }
        public Builder endExclusive(Long value) { endExclusive = value; return this; }
        public Builder category(String valueKind, Integer valueId) { categoryKind = valueKind; categoryId = valueId; return this; }
        public Builder familyMemberId(Integer value) { familyMemberId = value; return this; }
        public Builder paymentMethodId(Integer value) { paymentMethodId = value; return this; }
        public Builder minAmount(Double value) { minAmount = value; return this; }
        public Builder maxAmount(Double value) { maxAmount = value; return this; }
        public TransactionFilter build() { return new TransactionFilter(this); }
    }
}
