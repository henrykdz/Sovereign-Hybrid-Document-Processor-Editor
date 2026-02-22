package utils.general;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import java.util.StringJoiner;

public final class StringUtils {
	private static final String ELLIPSIS  = "...";
	public static final char    QUOTES    = '\u0022';
	public static final char    SEMICOLON = '\u003B';

	private StringUtils() {
	}

	private static final Charset WINDOWS_1252 = Charset.forName("Windows-1252");

	/**
	 * Converts a string from Windows-1252 encoding to UTF-8 encoding.
	 * 
	 * @param s the string to convert
	 * @return an Optional containing the converted string in UTF-8 encoding, or an empty Optional if the input string is null
	 */
	public static Optional<String> convertFromWindows1252ToUTF8(String s) {
		if (s == null) {
			return Optional.empty();
		}
		return Optional.of(new String(s.getBytes(WINDOWS_1252), StandardCharsets.UTF_8));
	}

	/**
	 * Converts a string from UTF-8 encoding to Windows-1252 encoding.
	 * 
	 * @param s the string to convert
	 * @return an Optional containing the converted string in Windows-1252 encoding, or an empty Optional if the input string is null
	 */
	public static Optional<String> convertFromUTF8ToWindows1252(String s) {
		if (s == null) {
			return Optional.empty();
		}
		return Optional.of(new String(s.getBytes(StandardCharsets.UTF_8), WINDOWS_1252));
	}

	/**
	 * Compares two CharSequences, returning {@code true} if they are equal, handling {@code null} inputs safely.
	 * <p>
	 * Two {@code null} references are considered equal.
	 *
	 * @param cs1 the first CharSequence, may be {@code null}
	 * @param cs2 the second CharSequence, may be {@code null}
	 * @return {@code true} if the CharSequences are equal, case-sensitive, or both {@code null}
	 */
	public static boolean equals(final CharSequence cs1, final CharSequence cs2) {
		if (cs1 == cs2) {
			return true; // Handles two nulls or two identical string instances
		}
		if (cs1 == null || cs2 == null) {
			return false; // One is null, the other is not
		}
		if (cs1.length() != cs2.length()) {
			return false; // Optimization: different lengths cannot be equal
		}
		// CharSequence.toString() is specified to return a String with the same content.
		return cs1.toString().equals(cs2.toString());
	}

	/**
	 * Extracts the file extension from a file path.
	 * 
	 * @param filePath The path of the file.
	 * @return The file extension (e.g., "txt", "jpg") or an empty string if no extension is found.
	 */
	public static String getFileExtension(String filePath) {
		if (filePath == null) {
			return "";
		}
		int dotPos = filePath.lastIndexOf('.');
		return dotPos > 0 ? filePath.substring(dotPos + 1) : "";
	}

	/**
	 * Counts the number of occurrences of a specific character in a given string.
	 * 
	 * @param text      The string to count characters in.
	 * @param character The character to count.
	 * @return The number of occurrences of the character found in the string.
	 */
	public static int countChar(String text, char character) {
		if (text == null) {
			return 0;
		}
		int count = 0;
		for (char ch : text.toCharArray()) {
			if (ch == character) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Cleans a string by replacing various invisible characters, control characters (including tabs and newlines), and multiple consecutive spaces with a single space, and then
	 * trims leading/trailing whitespace. Useful for preparing strings (like titles, descriptions) obtained from external sources for display or storage. Returns null if the input
	 * is null.
	 *
	 * @param input The string to clean. Can be null.
	 * @return The cleaned and trimmed string, or null if the input was null.
	 */
	public static String cleanAndTrim(String input) {
		if (input == null) {
			return null;
		}

		// 1. Replace a wide range of whitespace characters (including Unicode spaces like NBSP)
		// and ALL control characters (including \t, \n, \r) with a single space.
		// \p{Z} matches space separators (incl. NBSP U+00A0)
		// \p{C} matches control characters (incl. \t, \n, \r, \u200B etc.)
		String cleaned = input.replaceAll("[\\p{Z}\\p{C}]+", " "); // Replace one or more occurrences with a single space

		// 2. Trim leading and trailing whitespace that might remain or have been introduced.
		return cleaned.trim();
	}

	/**
	 * Trims the given string. Returns an empty string if the input is null.
	 * 
	 * @param str the string to trim, may be null
	 * @return the trimmed string, or an empty string if the input was null
	 */
	public static String trimToEmpty(final String str) {
		return str == null ? "" : str.trim();
	}

	/**
	 * Performs a case-insensitive check if a string starts with a given prefix.
	 * 
	 * @param str    The string to be checked.
	 * @param prefix The prefix to compare with.
	 * @return true if the string starts with the prefix (ignoring case), false otherwise.
	 */
	public static boolean startsWithIgnoreCase(String str, String prefix) {
		if (str == null || prefix == null) {
			return false;
		}
		return str.toLowerCase().startsWith(prefix.toLowerCase());
	}

	/**
	 * Performs a case-insensitive check if a string ends with a given suffix.
	 * 
	 * @param str    The string to be checked.
	 * @param suffix The suffix to compare with.
	 * @return true if the string ends with the suffix (ignoring case), false otherwise.
	 */
	public static boolean endsWithIgnoreCase(String str, String suffix) {
		if (str == null || suffix == null) {
			return false;
		}
		return str.toLowerCase().endsWith(suffix.toLowerCase());
	}

	/**
	 * Efficiently joins multiple strings together with a specified delimiter.
	 * 
	 * @param delimiter The delimiter to insert between strings.
	 * @param strings   The strings to be joined.
	 * @return A single string formed by concatenating the input strings with the delimiter.
	 */
	public static String join(String delimiter, String... strings) {
		if (strings == null || strings.length == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < strings.length; i++) {
			if (i > 0) {
				sb.append(delimiter);
			}
			sb.append(strings[i]);
		}
		return sb.toString();
	}

	/**
	 * Joins a collection of strings together with a specified delimiter.
	 * 
	 * @param delimiter The delimiter to insert between strings.
	 * @param strings   The collection of strings to be joined.
	 * @return A single string formed by concatenating the input strings with the delimiter.
	 */
	public static String join(String delimiter, Collection<String> strings) {
		if (strings == null || strings.isEmpty()) {
			return "";
		}
		StringJoiner joiner = new StringJoiner(delimiter);
		for (String string : strings) {
			joiner.add(string);
		}
		return joiner.toString();
	}

	/**
	 * Checks if a string is null, empty, or contains only whitespace characters. This method leverages the built-in {@link String#isBlank()} for performance and correctness
	 * (requires Java 11+).
	 * 
	 * @param str The string to be checked, may be null.
	 * @return {@code true} if the string is null or blank, {@code false} otherwise.
	 */
	public static boolean isBlank(String str) {
		return str == null || str.isBlank();
	}

	/**
	 * Counts how many times the substring appears in the larger string. This method is null-safe. A null or empty string input returns 0.
	 *
	 * @param str    The CharSequence to check, may be null.
	 * @param subStr The substring to count, may be null.
	 * @return The number of occurrences, 0 if either CharSequence is null or empty.
	 */
	public static int countMatches(final String str, final String subStr) {
		if (isBlank(str) || isBlank(subStr)) {
			return 0;
		}

		int count = 0;
		int idx = 0;
		while ((idx = str.indexOf(subStr, idx)) != -1) {
			count++;
			idx += subStr.length();
		}
		return count;
	}

	/**
	 * Opposite of {@link #isBlank(String)}. Checks if a string is neither null nor empty (excluding only whitespace characters).
	 * 
	 * @param str The string to be checked.
	 * @return true if the string is not blank, false otherwise.
	 */
	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}

	/**
	 * Returns the input string if it is neither null nor blank; otherwise, returns an empty string.
	 * 
	 * @param str The string to be checked.
	 * @return The input string if it is not blank, otherwise an empty string.
	 */
	public static String ifNotBlank(String str) {
		return isNotBlank(str) ? str : "";
	}

	/**
	 * Returns the concatenation of the given string and the concatenation string if the first string is not blank; otherwise, returns an empty string.
	 * 
	 * @param str       The string to be checked.
	 * @param concatStr The string to be concatenated if the first string is not blank.
	 * @return The concatenation of the input strings if the first string is not blank, otherwise an empty string.
	 */
	public static String ifNotBlank(String str, String concatStr) {
		return isNotBlank(str) ? str.concat(concatStr) : "";
	}

	/**
	 * Reverses the given string.
	 * 
	 * @param str The string to be reversed.
	 * @return The reversed string.
	 */
	public static String reverse(String str) {
		return new StringBuilder(str).reverse().toString();
	}

	/**
	 * Trims whitespace from the beginning and end of a String. If the trimmed String is empty ("") or the input was null, returns null. Otherwise, returns the trimmed String.
	 *
	 * <p>
	 * This is useful for converting blank strings (empty or whitespace-only) into null, which can simplify checks later on.
	 * </p>
	 *
	 * <p>
	 * Examples:
	 * </p>
	 * 
	 * <pre>
	 * StringUtils.trimToNull(null)          // = null
	 * StringUtils.trimToNull("")            // = null
	 * StringUtils.trimToNull("   ")         // = null
	 * StringUtils.trimToNull(" abc ")       // = "abc"
	 * StringUtils.trimToNull("abc")         // = "abc"
	 * </pre>
	 *
	 * @param str The String to trim, may be null.
	 * @return The trimmed String, or null if the input String was null, empty, or whitespace-only.
	 */
	public static String trimToNull(final String str) {
		if (str == null) {
			return null;
		}
		final String trimmed = str.trim();
		// Return null if the trimmed result is empty
		return trimmed.isEmpty() ? null : trimmed;
	}

	/**
	 * Repeats the given string a specified number of times.
	 * 
	 * @param str   The string to repeat.
	 * @param count The number of times to repeat the string.
	 * @return The repeated string.
	 */
	public static String repeat(String str, int count) {
		if (str == null) {
			return null;
		}
		if (count <= 0) {
			return "";
		}
		return str.repeat(count);
	}

	/**
	 * Left pads the given string with the specified character to a specified length.
	 * <p>
	 * Usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * String result = leftPad("123", 5, '0'); // Result: "00123"
	 * }</pre>
	 * 
	 * @param str     The string to pad.
	 * @param length  The length of the final string.
	 * @param padChar The character to pad with.
	 * @return The padded string.
	 */
	public static String leftPad(String str, int length, char padChar) {
		if (str == null) {
			return null;
		}
		if (str.length() >= length) {
			return str;
		}
		StringBuilder sb = new StringBuilder(length);
		for (int i = str.length(); i < length; i++) {
			sb.append(padChar);
		}
		sb.append(str);
		return sb.toString();
	}

	/**
	 * Right pads the given string with the specified character to a specified length.
	 * <p>
	 * Usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * String result = rightPad("123", 5, '0'); // Result: "12300"
	 * }</pre>
	 * 
	 * @param str     The string to pad.
	 * @param length  The length of the final string.
	 * @param padChar The character to pad with.
	 * @return The padded string.
	 */
	public static String rightPad(String str, int length, char padChar) {
		if (str == null) {
			return null;
		}
		if (str.length() >= length) {
			return str;
		}
		StringBuilder sb = new StringBuilder(length);
		sb.append(str);
		for (int i = str.length(); i < length; i++) {
			sb.append(padChar);
		}
		return sb.toString();
	}

	/**
	 * Abbreviates a string using ellipses if it exceeds a specified length.
	 * 
	 * @param str       The string to abbreviate.
	 * @param maxLength The maximum allowed length.
	 * @return The abbreviated string.
	 */
	public static String abbreviate(String str, int maxLength) {
		if (str == null) {
			return null;
		}
		if (str.length() <= maxLength) {
			return str;
		}
		return str.substring(0, maxLength - 3) + "...";
	}

	/**
	 * Checks if a string contains another string, ignoring case.
	 * 
	 * @param str       The string to be checked.
	 * @param searchStr The string to search for.
	 * @return true if the string contains the search string (ignoring case), false otherwise.
	 */
	public static boolean containsIgnoreCase(String str, String searchStr) {
		if (str == null || searchStr == null) {
			return false;
		}
		return str.toLowerCase().contains(searchStr.toLowerCase());
	}

	/**
	 * Capitalizes the first letter of the string.
	 * 
	 * @param str The string to be capitalized.
	 * @return The string with the first letter capitalized.
	 */
	public static String capitalizeFirstLetter(String str) {
		return isBlank(str) ? str : str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	/**
	 * Capitalizes the first letter of each word in the given string.
	 * 
	 * @param str The string to capitalize.
	 * @return The string with the first letter of each word capitalized.
	 */
	public static String capitalizeEachWord(String str) {
		if (isBlank(str)) {
			return str;
		}
		String[] words = str.split("\\s+");
		StringBuilder sb = new StringBuilder();
		for (String word : words) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
		}
		return sb.toString();
	}

	/**
	 * Escapes HTML special characters in a string.
	 * 
	 * @param str the input string, may be null
	 * @return the escaped string, or null if the input was null
	 */
	public static String escapeHtml(String str) {
		if (str == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder(str.length() * 2);
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			switch (c) {
			case '<' -> sb.append("&lt;");
			case '>' -> sb.append("&gt;");
			case '&' -> sb.append("&amp;");
			case '"' -> sb.append("&quot;");
			default  -> sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Converts a string from ISO-8859-1 encoding to UTF-8 encoding.
	 * 
	 * @param s the string to convert
	 * @return an Optional containing the converted string in UTF-8 encoding, or an empty Optional if the input string is null
	 */
	public static Optional<String> convertFromISO88591ToUTF8(String s) {
		if (s == null) {
			return Optional.empty();
		}
		return Optional.of(new String(s.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));
	}

	/**
	 * Converts a string from UTF-8 encoding to ISO-8859-1 encoding.
	 * 
	 * @param s the string to convert
	 * @return an Optional containing the converted string in ISO-8859-1 encoding, or an empty Optional if the input string is null
	 */
	public static Optional<String> convertFromUTF8ToISO88591(String s) {
		if (s == null) {
			return Optional.empty();
		}
		return Optional.of(new String(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
	}

	/**
	 * Unescapes common Java escape sequences (like \n, \r, \t, \", \\) and Unicode escape sequences (\\uXXXX) in a string. This method is based on Apache Commons Lang's
	 * StringEscapeUtils.unescapeJava logic.
	 *
	 * @param str The string to unescape.
	 * @return The unescaped string, or null if the input was null.
	 */
	public static String unescapeJava(String str) {
		if (str == null) {
			return null;
		}

		StringBuilder out = new StringBuilder(str.length());
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
			if (ch == '\\') {
				int nextChar = (i == str.length() - 1) ? -1 : str.charAt(i + 1);
				if (nextChar == -1) {
					// Trailing backslash, treat as literal
					out.append('\\');
				} else {
					switch (nextChar) {
					case 'b':
						out.append('\b');
						i++;
						break;
					case 'n':
						out.append('\n');
						i++;
						break;
					case 't':
						out.append('\t');
						i++;
						break;
					case 'f':
						out.append('\f');
						i++;
						break;
					case 'r':
						out.append('\r');
						i++;
						break;
					case '\"':
						out.append('\"');
						i++;
						break;
					case '\'':
						out.append('\'');
						i++;
						break;
					case '\\':
						out.append('\\');
						i++;
						break;
					case 'u': // Unicode escape: \\uXXXX
						if (i + 5 < str.length()) {
							String hex = str.substring(i + 2, i + 6);
							try {
								out.append((char) Integer.parseInt(hex, 16));
								i += 5;
							} catch (NumberFormatException nfe) {
								out.append(ch); // Append original character if malformed unicode
							}
						} else {
							out.append(ch); // Incomplete unicode sequence
						}
						break;
					default:
						out.append(ch); // Unrecognized escape, append literal backslash
						break;
					}
				}
			} else {
				out.append(ch);
			}
		}
		return out.toString();
	}

	/**
	 * Truncates the given string to the specified maximum length, appending an ellipsis ("...") if the string was actually truncated. The total length, including the ellipsis,
	 * will not exceed maxLength.
	 *
	 * @param str       The string to truncate. Can be null.
	 * @param maxLength The maximum desired length including the ellipsis (must be at least 3 for ellipsis to appear).
	 * @return The truncated string with ellipsis, or the original string if truncation was not needed, or null if input was null.
	 * @throws IllegalArgumentException if maxLength is negative.
	 */
	public static String truncate(String str, int maxLength) {
		if (maxLength < 0) {
			throw new IllegalArgumentException("Max length cannot be negative: " + maxLength);
		}
		if (str == null || str.length() <= maxLength) {
			return str;
		}
		// Ensure maxLength is sufficient for ellipsis
		if (maxLength < ELLIPSIS.length()) {
			// Cannot fit ellipsis, just cut at maxLength
			return str.substring(0, maxLength);
		}
		// Truncate and add ellipsis
		return str.substring(0, maxLength - ELLIPSIS.length()) + ELLIPSIS;
	}
}
