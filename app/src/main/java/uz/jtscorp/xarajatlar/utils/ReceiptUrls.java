package uz.jtscorp.xarajatlar.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Validation policy for Uzbekistan fiscal receipt links. */
public final class ReceiptUrls {
    public static final String TRUSTED_HOST = "ofd.soliq.uz";

    private ReceiptUrls() {}

    public static String normalizeOrNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        try {
            URI uri = new URI(trimmed);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null
                    || !TRUSTED_HOST.equals(host.toLowerCase(Locale.ROOT))
                    || uri.getRawUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                return null;
            }
            return new URI("https", null, TRUSTED_HOST, uri.getPort(), uri.getRawPath(),
                    uri.getRawQuery(), uri.getRawFragment()).toASCIIString();
        } catch (URISyntaxException | IllegalArgumentException error) {
            return null;
        }
    }

    public static String resolveTrustedRedirectOrNull(String currentUrl, String location) {
        String current = normalizeOrNull(currentUrl);
        if (current == null || location == null || location.isBlank()) return null;
        try {
            return normalizeOrNull(new URI(current).resolve(location).toASCIIString());
        } catch (URISyntaxException | IllegalArgumentException error) {
            return null;
        }
    }
}
