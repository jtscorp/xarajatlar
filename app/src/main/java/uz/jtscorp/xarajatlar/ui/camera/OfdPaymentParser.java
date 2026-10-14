package uz.jtscorp.xarajatlar.ui.camera;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

final class OfdPaymentParser {
    private OfdPaymentParser() {}

    static double total(String json) {
        try {
            JSONObject data = new JSONObject(json).optJSONObject("data");
            if (data == null) return 0;
            double cash = data.optDouble("cashTotal", 0);
            double card = data.optDouble("cardTotal", 0);
            double amount = cash + card;
            return Double.isFinite(amount) && amount > 0 ? amount : 0;
        } catch (JSONException | NullPointerException error) {
            return 0;
        }
    }

    static String itemsJson(String json) {
        try {
            JSONObject data = new JSONObject(json).optJSONObject("data");
            JSONArray items = data == null ? null : data.optJSONArray("paymentDetails");
            return items == null || items.length() == 0 ? null : items.toString();
        } catch (JSONException | NullPointerException error) {
            return null;
        }
    }
}
