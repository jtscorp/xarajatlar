package uz.jtscorp.xarajatlar.ui.history;

import android.os.Bundle;

import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;

import uz.jtscorp.xarajatlar.R;

public final class HistoryNavigation {
    public static final String ARG_KIND = "kind";
    public static final String ARG_START_INCLUSIVE = "startInclusive";
    public static final String ARG_END_EXCLUSIVE = "endExclusive";
    public static final String ARG_CATEGORY_KIND = "categoryKind";
    public static final String ARG_CATEGORY_ID = "categoryId";
    public static final String ARG_FAMILY_MEMBER_ID = "familyMemberId";
    public static final String ARG_PAYMENT_METHOD_ID = "paymentMethodId";
    public static final String REQUEST_KEY = "history_filter_request";
    static final String ARG_REPLACE_FILTERS = "replaceHistoryFilters";

    private HistoryNavigation() {}

    public static Bundle arguments(String kind, Long startInclusive, Long endExclusive,
                                   String categoryKind, Integer categoryId,
                                   Integer familyMemberId, Integer paymentMethodId) {
        Bundle values = new Bundle();
        values.putBoolean(ARG_REPLACE_FILTERS, true);
        putNullableString(values, ARG_KIND, kind);
        putNullableLong(values, ARG_START_INCLUSIVE, startInclusive);
        putNullableLong(values, ARG_END_EXCLUSIVE, endExclusive);
        putNullableString(values, ARG_CATEGORY_KIND, categoryKind);
        putNullableInt(values, ARG_CATEGORY_ID, categoryId);
        putNullableInt(values, ARG_FAMILY_MEMBER_ID, familyMemberId);
        putNullableInt(values, ARG_PAYMENT_METHOD_ID, paymentMethodId);
        return values;
    }

    public static void open(NavController controller, Bundle filters) {
        Bundle request = new Bundle(filters);
        request.putBoolean(ARG_REPLACE_FILTERS, true);
        if (controller.getCurrentDestination() != null
                && controller.getCurrentDestination().getId() == R.id.navigation_history) {
            NavBackStackEntry entry = controller.getCurrentBackStackEntry();
            if (entry != null) entry.getSavedStateHandle().set(REQUEST_KEY, request);
            return;
        }
        controller.navigate(R.id.navigation_history, request);
    }

    static TransactionFilter filterFrom(Bundle values) {
        if (values == null || !values.getBoolean(ARG_REPLACE_FILTERS, false)) {
            return null;
        }
        TransactionFilter.Builder builder = new TransactionFilter.Builder();
        if (values.containsKey(ARG_KIND)) builder.kind(values.getString(ARG_KIND));
        if (values.containsKey(ARG_START_INCLUSIVE)) builder.startInclusive(values.getLong(ARG_START_INCLUSIVE));
        if (values.containsKey(ARG_END_EXCLUSIVE)) builder.endExclusive(values.getLong(ARG_END_EXCLUSIVE));
        String categoryKind = values.containsKey(ARG_CATEGORY_KIND)
                ? values.getString(ARG_CATEGORY_KIND) : null;
        Integer categoryId = values.containsKey(ARG_CATEGORY_ID)
                ? values.getInt(ARG_CATEGORY_ID) : null;
        if (categoryKind != null || categoryId != null) builder.category(categoryKind, categoryId);
        if (values.containsKey(ARG_FAMILY_MEMBER_ID)) builder.familyMemberId(values.getInt(ARG_FAMILY_MEMBER_ID));
        if (values.containsKey(ARG_PAYMENT_METHOD_ID)) builder.paymentMethodId(values.getInt(ARG_PAYMENT_METHOD_ID));
        return builder.build();
    }

    private static void putNullableString(Bundle target, String key, String value) {
        if (value != null) target.putString(key, value);
    }

    private static void putNullableLong(Bundle target, String key, Long value) {
        if (value != null) target.putLong(key, value);
    }

    private static void putNullableInt(Bundle target, String key, Integer value) {
        if (value != null) target.putInt(key, value);
    }
}
