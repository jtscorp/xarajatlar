package uz.jtscorp.xarajatlar.utils;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import uz.jtscorp.xarajatlar.R;

/** Renders the saved OFD product list without requesting the receipt again. */
public final class ReceiptItemsView {
    private ReceiptItemsView() {}

    public static void render(LinearLayout container, String itemsJson) {
        container.removeAllViews();
        JSONArray items;
        try {
            items = itemsJson == null ? null : new JSONArray(itemsJson);
        } catch (JSONException error) {
            items = null;
        }
        if (items == null || items.length() == 0) {
            container.setVisibility(View.GONE);
            return;
        }
        container.setVisibility(View.VISIBLE);
        Context context = container.getContext();
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            String name = item.optString("name", "").trim();
            if (name.isEmpty()) name = item.optString("productName", "").trim();
            if (name.isEmpty()) name = item.optString("title", "").trim();
            if (name.isEmpty()) continue;
            double quantity = item.has("amount") ? item.optDouble("amount", 0)
                    : item.optDouble("quantity", 0);
            double price = item.has("price") ? item.optDouble("price", 0)
                    : item.optDouble("unitPrice", 0);
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.topMargin = dp(context, 12);
            container.addView(row, rowParams);

            TextView title = new TextView(context);
            title.setText(name);
            title.setTextSize(16);
            title.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                    container, com.google.android.material.R.attr.colorOnSurface));
            row.addView(title);

            if (Double.isFinite(quantity) && quantity > 0 && Double.isFinite(price) && price >= 0) {
                TextView details = new TextView(context);
                details.setText(context.getString(R.string.receipt_item_details,
                        Formats.number(quantity), Formats.number(price), Formats.number(quantity * price),
                        CurrencyLabel.current(context)));
                details.setTextSize(14);
                details.setTextColor(com.google.android.material.color.MaterialColors.getColor(
                        container, com.google.android.material.R.attr.colorOnSurfaceVariant));
                row.addView(details);
            }
        }
        if (container.getChildCount() == 0) container.setVisibility(View.GONE);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
