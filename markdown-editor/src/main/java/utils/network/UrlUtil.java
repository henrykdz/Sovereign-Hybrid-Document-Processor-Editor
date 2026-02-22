package utils.network;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import utils.logging.Log;

public class UrlUtil {

	/**
	 * Pre-compiled regular expression pattern for validating URLs.
	 */
	private static final Pattern URL_PATTERN = Pattern.compile("^(https?://)?(\\w+\\.\\w+)(:\\d+)?(/\\w+\\/)*([\\w\\.\\?=&]*)?(#.*)?$");

	/**
	 * Checks if a given string is a valid URL format.
	 * 
	 * @param str The string to be checked.
	 * @return true if the string is a valid URL, false otherwise.
	 */
	public static boolean isUrl(String str) {
		return str != null && URL_PATTERN.matcher(str).matches();
	}

	/**
	 * Splits a URL string into its components (protocol, host, port, path, query string, fragment).
	 * 
	 * @param url The URL string to be split.
	 * @return An array of strings containing the URL components, or null if the URL is invalid.
	 */
	public static String[] splitUrl(String url) {
		Matcher matcher = URL_PATTERN.matcher(url);
		if (matcher.matches()) {
			return new String[] { matcher.group(1), // protocol
			        matcher.group(2), // host
			        matcher.group(3), // port (optional)
			        matcher.group(4), // path
			        matcher.group(5), // query string (optional)
			        matcher.group(6) // fragment (optional)
			};
		}
		return new String[0];
	}

	/**
	 * Proves syntax only. "regex" syntax verification
	 * 
	 * @param url
	 * @return
	 */
	public static boolean verifyUrlSyntax(String url) {
		// String urlRegex = "^(?i)(ftp|http|https)://([a-zA-Z0-9]+(?:-[a-zA-Z0-9]+)*\\.)+[a-zA-Z]{2,}(?:[/?#][^ \\t\\n\\r]*[\\w/#]?)*$";
		String urlRegex = "^(?i)(?:www\\.)?(?:[a-zA-Z0-9-_]+\\.)*[a-zA-Z]{2,}(?:[/?#][^ \\t\\n\\r]*[\\w/#]?)*$";

		Pattern pattern = Pattern.compile(urlRegex);
		Matcher m = pattern.matcher(url);
		return m.matches();
	}

	/**
	 * Check if a given text is an web URL. The test is case un-sensitive and check is the text starts with one of the following:
	 * <ul>
	 * <li>http://</li>
	 * <li>https://</li>
	 * <li>www.</li>
	 * </ul>
	 * 
	 * @param text The text to test.
	 * @return {@code true} if the text has an URL form, {@code false} otherwise, including {@code null}.
	 */
	public static boolean hasWebURLAddressPrefix(final String addressText) {
		if (null == addressText)
			return false;
		// low cased
		final String url = addressText.toLowerCase();
		return (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("www."));
	}

	public static boolean startsWithWWWWSuffix(final String addressText) {
		if (null == addressText)
			return false;

		return addressText.toLowerCase().startsWith("www.");
	}

	public static boolean hasFileProtocol(final String addressText) {
		// low cased
		final String url = addressText != null ? addressText.toLowerCase() : null;
		return null != url && (url.startsWith("file:"));
	}

	public static boolean javaNetUrlValidator(String url) {
		try {
			new URI(url);
			return true;
		} catch (URISyntaxException exception) {
			return false;
		}
	}

	public static int getResponseCode(String urlString) throws IOException {
		int code = 0;
		try {
			URI u = new URI(urlString);

			HttpsURLConnection huc = (HttpsURLConnection) u.toURL().openConnection();
			huc.setRequestMethod("GET");
			huc.setRequestProperty("User-Agent", "Chrome");
			huc.connect();
			code = huc.getResponseCode();
		} catch (URISyntaxException e) {
			Log.warn(e, e.getMessage());
		}
		Log.fine("Response code: " + code);
		return code;
	}

	private UrlUtil() {
		super();
	}
}
