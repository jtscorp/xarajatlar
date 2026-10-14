package uz.jtscorp.xarajatlar.ui.camera;

/** Extracts the fields shown on an OFD receipt page. */
public final class ReceiptTextParser {
    private ReceiptTextParser() {}

    public static Result parse(String text) {
        if (text == null || text.isBlank()) return new Result(0, "");
        String[] lines = text.split("\\R");
        double amount = 0;
        String shopName = "";
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.matches("(?iu).*Jami\\s+to['’`]?lov.*")) {
                String number = line.replaceFirst("(?iu)^.*?Jami\\s+to['’`]?lov\\s*", "").trim();
                if (number.isEmpty() && i + 1 < lines.length) number = lines[i + 1].trim();
                amount = parseAmount(number);
            }
            if (line.matches("(?iu)Savdo\\s+cheki\\s*/\\s*Sotuv")) {
                int companyLine = i + 1;
                while (companyLine < lines.length && lines[companyLine].isBlank()) {
                    companyLine++;
                }
                if (companyLine < lines.length) {
                    String company = lines[companyLine].trim();
                    int quoteStart = company.indexOf('"');
                    int quoteEnd = company.indexOf('"', quoteStart + 1);
                    shopName = quoteStart >= 0 && quoteEnd > quoteStart
                            ? company.substring(quoteStart + 1, quoteEnd).trim()
                            : company;
                }
            }
        }
        return new Result(amount, shopName);
    }

    private static double parseAmount(String value) {
        String number = value.replaceAll("[^0-9,.]", "");
        int comma = number.lastIndexOf(',');
        int dot = number.lastIndexOf('.');
        if (comma >= 0 && dot >= 0) {
            number = comma < dot ? number.replace(",", "")
                    : number.replace(".", "").replace(',', '.');
        } else if (comma >= 0) {
            number = number.length() - comma - 1 == 3
                    ? number.replace(",", "") : number.replace(',', '.');
        }
        try {
            double parsed = Double.parseDouble(number);
            return Double.isFinite(parsed) && parsed > 0 ? parsed : 0;
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    public static final class Result {
        public final double amount;
        public final String shopName;

        public Result(double amount, String shopName) {
            this.amount = amount;
            this.shopName = shopName;
        }
    }
}
