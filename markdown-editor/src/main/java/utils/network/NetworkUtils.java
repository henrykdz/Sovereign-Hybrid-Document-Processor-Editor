package utils.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import utils.detection.Pathment;
import utils.detection.PathmentExtractor;
import utils.general.StringUtils;
import utils.logging.Log;

/**
 * Provides utility methods for common network-related tasks, including URL validation, header creation, response parsing, and proxy diagnostics. This class contains only static
 * methods and is not intended to be instantiated.
 */
public final class NetworkUtils {

	// --- Constants ---

	// KORREKTUR: Chrome 142 (Realistischer Standard für Ende 2025)
	// Wir entfernen "OPR/...", um als Standard-Chrome zu gelten (beste Tarnung).
	public static final String DEFAULT_USER_AGENT_VALUE = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36";

	public static final String DEFAULT_ACCEPT_LANGUAGE = "en-US,en;q=0.9,de;q=0.8";

	// KORREKTUR: Trailing Slash hinzufügen.
	// Ein echter Browser sendet fast immer den Slash am Ende der Domain als Referer.
	public static final String VALUE_GOOGLE_REFERRER = "https://www.google.com/";

	/** Private constructor to prevent instantiation. */
	private NetworkUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static boolean isActiveInternetConnectionAvailable() {
		// This implementation is a bit simplistic and might not be fully reliable.
		// It only checks if it can open a socket to google.com on port 80.
		try (Socket socket = new Socket()) {
			InetSocketAddress address = new InetSocketAddress("www.google.com", 80);
			socket.connect(address, 3000);
			return true; // Connection successful
		} catch (IOException e) {
			// Log the reason for failure for debugging purposes.
			Log.warn("Internet connectivity check failed: " + e.getMessage());
			return false; // Connection failed
		}
	}

	// --- Header Creation ---

	/**
	 * Creates Accept-Language header string for HTTP requests. Uses quality values to indicate language preferences.
	 * 
	 * @param preferredLocale User's preferred locale, null uses default fallback
	 * @return Formatted Accept-Language header without spaces
	 */
	public static String createAcceptLanguageHeader(Locale preferredLocale) {
		if (preferredLocale == null) {
			return DEFAULT_ACCEPT_LANGUAGE;
		}

		String langTag = preferredLocale.toLanguageTag();
		String langOnly = preferredLocale.getLanguage();
		StringBuilder header = new StringBuilder();

		if (langTag.equalsIgnoreCase(langOnly)) {
			// Case 1: Locale has no country code (e.g. just "de")
			header.append(String.format("%s;q=1.0", langOnly));
		} else {
			// Case 2: Locale has country code (e.g. "bs-BA")
			// Highest priority for specific region, then general language
			header.append(String.format("%s;q=1.0,%s;q=0.9", langTag, langOnly));
		}

		// Add English fallbacks with lower priority if not already main language
		if (!langOnly.equals("en")) {
			header.append(",en-US;q=0.8,en;q=0.7");
		}

		return header.toString();
	}

	// --- Response Processing ---

	public static Charset getConnectionCharset(HttpResponse<?> response) {
		Optional<String> contentTypeOpt = response.headers().firstValue("Content-Type");

		if (contentTypeOpt.isPresent()) {
			String contentType = contentTypeOpt.get().toLowerCase();
			String charsetName = extractCharsetName(contentType);

			if (StringUtils.isNotBlank(charsetName)) {
				charsetName = charsetName.replace("\"", ""); // Remove potential quotes
				try {
					return Charset.forName(charsetName);
				} catch (UnsupportedCharsetException e) {
					Log.warn("Unsupported charset specified in Content-Type: " + charsetName);
				} catch (Exception e) {
					Log.warn(e, "Error parsing charset name: " + charsetName);
				}
			}
		}
		return Charset.defaultCharset();
	}

	private static String extractCharsetName(String contentType) {
		if (contentType == null)
			return null;
		String[] params = contentType.split(";");
		for (String param : params) {
			param = param.trim();
			if (param.startsWith("charset=")) {
				return param.substring("charset=".length()).trim();
			}
		}
		return null;
	}

	// --- String Sanitizing ---

	public static String sanitizeXmlChars(String input) {
		if (StringUtils.isBlank(input)) {
			return "";
		}
		StringBuilder out = new StringBuilder(input.length());
		for (int i = 0; i < input.length(); i++) {
			char current = input.charAt(i);
			if ((current == 0x9) || (current == 0xA) || (current == 0xD) || ((current >= 0x20) && (current <= 0xD7FF)) || ((current >= 0xE000) && (current <= 0xFFFD))
			        || ((current >= 0x10000) && (current <= 0x10FFFF))) {
				out.append(current);
			}
		}
		return out.toString();
	}

	// =========================== NEUE METHODE START ===========================

	public static String cleanKnownUrl(String url, UrlSanitizationOptions options) {
		if (StringUtils.isBlank(url))
			return url;

		// 1. Zuerst das Deep-Decoding (löst %253F etc. auf), damit Pathment sauber parsen kann
		String decodedUrl = Pathment.getDecodedUrl(url);

		// 2. Pathment zur Strukturanalyse nutzen
		Pathment p = PathmentExtractor.parse(decodedUrl);

		// Master-Check: Nur Web-URLs reinigen, wenn die Option aktiv ist
		if (options == null || !options.masterActive() || !p.getType().isWebUrl() || p.getWebUrl() == null) {
			return decodedUrl;
		}

		String host = p.getWebUrl().getHost().toLowerCase();
		String path = p.getWebUrl().getPath(); // Dies ist der Pfad EXAKT bis zum '?'

		// --- 2. EBAY REINIGUNG (ITEM & PRODUCT) ---
		if (options.ebayActive() && host.contains("ebay.")) {
			// Wir prüfen auf beide Typen: /itm/ (Angebote) und /p/ (Katalogprodukte)
			if (path != null && (path.contains("/itm/") || path.contains("/p/"))) {

				// REKONSTRUKTION: Wir bauen die URL NUR aus Protokoll, Host und Pfad zusammen.
				// Das Fragezeichen und der gesamte Tracking-Müll (Query) fallen automatisch weg!
				return p.getProtocol().getNotation() + p.getWebUrl().getHost() + path;
			}
		}

		// --- 3. AMAZON REINIGUNG ---
		if (options.amazonActive() && (host.contains("amazon.") || host.contains("amzn.to"))) {
			return sanitizeAmazonUrl(decodedUrl);
		}

		return decodedUrl;
	}

	/**
	 * Private helper to normalize Amazon URLs to the canonical /dp/ASIN format. Dynamically detects the correct top-level domain (.de, .com, etc.)
	 */
	private static String sanitizeAmazonUrl(String url) {
		// Dieser Regex findet die ASIN in allen Pfadvarianten
		Pattern p = Pattern.compile("(?:dp|gp/product|gp/aw/d|exec/obidos/asin)/([A-Z0-9]{10})", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(url);

		if (m.find()) {
			String asin = m.group(1);

			// Wir extrahieren die Basis-Domain (z.B. amazon.com oder amazon.de)
			// um sicherzustellen, dass wir nicht falsch umleiten.
			String domain = "amazon.de"; // Default
			Pattern domainPattern = Pattern.compile("amazon\\.[a-z\\.]+");
			Matcher domainMatcher = domainPattern.matcher(url.toLowerCase());
			if (domainMatcher.find()) {
				domain = domainMatcher.group();
			}

			return "https://www." + domain + "/dp/" + asin;
		}
		return url;
	}
}