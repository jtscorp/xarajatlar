package uz.jtscorp.xarajatlar.ui.expense;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Keeps the newest spelling of each shop while ignoring case and repeated whitespace. */
public final class ShopNameSuggestions {
    private ShopNameSuggestions() {}

    public static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s\\p{Z}]+", " ").trim();
    }

    public static List<String> unique(List<String> names) {
        LinkedHashMap<String, String> shops = new LinkedHashMap<>();
        for (String raw : names) {
            String name = normalize(raw);
            if (!name.isEmpty()) shops.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }
        return new ArrayList<>(shops.values());
    }
}
