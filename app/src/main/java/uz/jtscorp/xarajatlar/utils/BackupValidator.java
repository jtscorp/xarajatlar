package uz.jtscorp.xarajatlar.utils;

import androidx.annotation.StringRes;
import uz.jtscorp.xarajatlar.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Validates the complete restore before any database rows are removed. */
final class BackupValidator {
    private BackupValidator() {}

    static void validate(JSONObject data, File extracted) throws JSONException, IOException {
        Map<String, Set<Integer>> ids = new HashMap<>();
        String[] tables = {"family_members", "categories", "payment_methods", "debts",
                "expenses", "income_categories", "incomes", "debt_evidences"};
        for (String table : tables) {
            // Backups made before the income module legitimately omit both income arrays.
            boolean legacyIncome = !data.has("incomes") && !data.has("income_categories")
                    && (table.equals("incomes") || table.equals("income_categories"));
            JSONArray rows = legacyIncome ? new JSONArray() : data.getJSONArray(table);
            Set<Integer> tableIds = new HashSet<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                long id = integer(row, "id");
                if (id <= 0 || id > Integer.MAX_VALUE || !tableIds.add((int) id)) {
                    throw invalid(R.string.backup_invalid_id, table);
                }
                validateFields(table, row);
            }
            ids.put(table, tableIds);
        }
        for (String table : new String[]{"expenses", "incomes"}) {
            JSONArray rows = data.optJSONArray(table);
            if (rows == null) continue;
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                relation(row, "familyMemberId", ids.get("family_members"));
                relation(row, "paymentMethodId", ids.get("payment_methods"));
                relation(row, table.equals("expenses") ? "categoryId" : "incomeCategoryId",
                        ids.get(table.equals("expenses") ? "categories" : "income_categories"));
            }
        }
        JSONArray evidence = data.getJSONArray("debt_evidences");
        for (int i = 0; i < evidence.length(); i++) {
            JSONObject row = evidence.getJSONObject(i);
            relation(row, "debtId", ids.get("debts"));
            String fileName = new File(row.getString("filePath")).getName();
            File mediaRoot = new File(extracted, "media/debts");
            File source = new File(mediaRoot, row.getInt("debtId") + "/" + fileName);
            // The exporter omits media that was already missing on the source device.
            // Keep the evidence record importable; restore assigns a safe app-owned path.
            if (fileName.isEmpty()
                    || !source.getCanonicalPath().startsWith(mediaRoot.getCanonicalPath() + File.separator)
                    || (source.exists() && !source.isFile())) {
                throw invalid(R.string.backup_invalid_media_file);
            }
        }
        if (data.has("settings")) {
            JSONObject settings = data.getJSONObject("settings");
            long day = integer(settings, "salaryDay");
            if (day < 1 || day > 31) throw invalid(R.string.backup_invalid_salary_day);
            bool(settings, "biometricEnabled");
            if (settings.has("currencyLabel")) {
                Object label = settings.get("currencyLabel");
                if (!(label instanceof String) || CurrencyLabel.normalize((String) label) == null) {
                    throw invalid(R.string.backup_invalid_currency);
                }
            }
        }
    }

    private static void validateFields(String table, JSONObject row) throws JSONException {
        switch (table) {
            case "family_members":
                text(row, "name"); bool(row, "isActive"); break;
            case "categories":
                text(row, "name"); integer(row, "color"); bool(row, "isActive"); break;
            case "payment_methods":
                text(row, "name"); integer(row, "color"); bool(row, "isCash"); break;
            case "income_categories":
                text(row, "name");
                if (row.has("isActive")) bool(row, "isActive");
                break;
            case "expenses":
            case "incomes":
                positiveAmount(row, "amount"); integer(row, "timestamp"); break;
            case "debts":
                text(row, "personName"); positiveAmount(row, "amount");
                integer(row, "givenDate"); bool(row, "isFullyPaid");
                if (row.has("paidAmount") && number(row, "paidAmount") < 0) {
                    throw invalid(R.string.backup_negative_paid);
                }
                if (row.has("dueDate")) integer(row, "dueDate");
                break;
            case "debt_evidences":
                integer(row, "debtId"); integer(row, "timestamp");
                text(row, "filePath"); text(row, "fileType");
                if (!Set.of("image", "video", "audio").contains(row.getString("fileType"))) {
                    throw invalid(R.string.backup_invalid_media_type);
                }
                break;
        }
    }

    private static void relation(JSONObject row, String key, Set<Integer> ids) throws JSONException {
        if (!row.has(key) || row.isNull(key)) return;
        long id = integer(row, key);
        if (id > Integer.MAX_VALUE || id <= 0 || !ids.contains((int) id)) {
            throw invalid(R.string.backup_missing_relation, key);
        }
    }

    private static long integer(JSONObject row, String key) throws JSONException {
        Object value = row.get(key);
        if (!(value instanceof Integer) && !(value instanceof Long)) {
            throw invalid(R.string.backup_not_integer, key);
        }
        return ((Number) value).longValue();
    }

    private static double number(JSONObject row, String key) throws JSONException {
        Object value = row.get(key);
        if (!(value instanceof Number) || !Double.isFinite(((Number) value).doubleValue())) {
            throw invalid(R.string.backup_not_number, key);
        }
        return ((Number) value).doubleValue();
    }

    private static void positiveAmount(JSONObject row, String key) throws JSONException {
        if (number(row, key) <= 0) throw invalid(R.string.backup_amount_not_positive);
    }

    private static void text(JSONObject row, String key) throws JSONException {
        Object value = row.get(key);
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            throw invalid(R.string.backup_text_missing, key);
        }
    }

    private static void bool(JSONObject row, String key) throws JSONException {
        if (!(row.get(key) instanceof Boolean)) throw invalid(R.string.backup_not_boolean, key);
    }

    private static JSONException invalid(@StringRes int messageRes, Object... args) {
        return new BackupFormatException(messageRes, args);
    }
}
