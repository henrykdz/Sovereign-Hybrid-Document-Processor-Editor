package utils.detection;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import utils.general.StringUtils;
import utils.logging.Log;
import utils.network.TransferProtocol;

/**
 * A utility class for extracting and parsing potential paths (URLs, file paths, emails, IPs, hostnames, etc.) from raw strings. This class uses a single-pass approach with
 * context-aware validation to improve accuracy and performance.
 */
public final class PathmentExtractor {

	private PathmentExtractor() {
	}

	private static final String PATH_CONTAINER = "{&Path}";

	// --- Optimized Single Regex Pattern ---
	private static final Pattern MASTER_EXTRACTION_PATTERN = Pattern.compile(
	        // PRIORITY 1: Full URLs with a protocol
	        "\\b(?:https?://|ftp://|file://)[^\\s,;<>\"']+" + "|" + // OR
			// PRIORITY 2: E-Mails
	                "\\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,16}\\b" + "|" + // OR
					// PRIORITY 3: Windows and UNC Paths
	                "\\b(?:[A-Za-z]:|\\\\\\\\[^\\\\/\\s]+)" + // Start: C: or \\server
	                "(?:\\\\[^\\\\/:*?\"<>|\\r\\n,;]+)+" + // One or more path segments
	                "(?:\\.[a-zA-Z0-9]{1,10})?(?=\\s|$|[<>\"?:|])" + // Optional extension, stop at space, end, or invalid chars
	                "|" + // OR
					// PRIORITY 4: URLs starting with 'www.'
	                "\\bwww\\.[^\\s,;<>\"']+" + "|" + // OR
					// PRIORITY 5: Unix-style absolute paths
	                "\\b/(?:[^/\\s]+/)*[^/\\s]+\\.[^/\\s.]+" + "|" + // OR
					// PRIORITY 6: IP Addresses and localhost
	                "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b" + "|\\blocalhost\\b" + "|" + // OR
					// PRIORITY 7: Hostnames / Simple Domains
	                "\\b(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,24}\\b",
	        Pattern.CASE_INSENSITIVE);

	// --- Validation Patterns ---
	private static final Pattern EMAIL_PATTERN      = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,16}$");
	private static final Pattern IP_ADDRESS_PATTERN = Pattern
	        .compile("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$" + "|^(?:[0-9a-fA-F]{0,4}:){2,}[0-9a-fA-F]{0,4}$"                                                                                                                                                                      // Simplified
	                                                                                                                                                                                                                                                                                                                                // IPv6
			);
	private static final Pattern HOSTNAME_PATTERN   = Pattern.compile("^([a-zA-Z0-9][a-zA-Z0-9-]{0,61}\\.)+[a-zA-Z]{2,24}$");

	// --- Public API ---

	public static Pathment parse(String textContainingPath) {
		return parseSingle(textContainingPath);
	}

	public static List<Pathment> perform(String raw) {
		ScanResult result = performExtraction(raw);
		return result.validPathments();
	}

	// --- Internal Logic ---

	/**
	 * Parses a single, cleaned string to determine its most likely Pathment type. This is the core classification engine, using a sophisticated, multi-stage pipeline that
	 * leverages the central TransferProtocol.detect() method.
	 *
	 * @param rawText The raw, untrimmed string to parse.
	 * @return A new Pathment instance, which will be of type UNSPECIFIED if no confident classification could be made.
	 */
	private static Pathment parseSingle(String rawText) {
		if (StringUtils.isBlank(rawText)) {
			return Pathment.createUnspecified(rawText != null ? rawText : "");
		}
		String cleanedText = rawText.trim();

		// STAGE 1: PRIMARY CLASSIFICATION using TransferProtocol.detect()
		TransferProtocol detectedProtocol = TransferProtocol.detect(cleanedText);

		// STAGE 2: DELEGATION based on the primary classification
		switch (detectedProtocol) {
		case HTTPS, HTTP, FTP, SSH, TELNET:
			WebUrl url = splitWebUrl(cleanedText);
			if (url != null && validateAndFinalizeWebUrl(url, cleanedText)) {
				return Pathment.fromWebUrl(url);
			}
			break;

		case MAILTO:
			String emailAddress = cleanedText.substring(7);
			if (EMAIL_PATTERN.matcher(emailAddress).matches()) {
				return Pathment.fromEmail(emailAddress);
			}
			break;

		case PATH, UNC, FILE_URL, MALFORMED_FILE_URL:
			FilePath fp = splitFilePath(cleanedText);
			if (fp != null) {
				Pathment p = Pathment.fromFilePath(fp);
				if (p.getType().isSpecified()) {
					return p;
				}
			}
			break;

		case NONE:
			// If even the expert says NONE, we must check for protocol-less patterns.
			// This is the ONLY place where we need heuristics.
			if (cleanedText.contains("@") && EMAIL_PATTERN.matcher(cleanedText).matches()) {
				return Pathment.fromEmail(cleanedText);
			}
			if (IP_ADDRESS_PATTERN.matcher(cleanedText).matches()) {
				return Pathment.fromIpAddress(cleanedText);
			}
			
			// The "www." heuristic can remain as a final, high-confidence guess.
			if (cleanedText.toLowerCase().startsWith("www.") ) {
				WebUrl heuristicUrl = splitWebUrl("https://" + cleanedText);
				if (validateAndFinalizeWebUrl(heuristicUrl, cleanedText)) {
					return Pathment.fromWebUrl(heuristicUrl);
				}
			}
			break; // Fall through to UNSPECIFIED if no heuristic matches


		default:
			Log.debug("debug2 case default: unrecognized");
			// Covers PROMPT etc. Fall through to UNSPECIFIED.
			break;
		}

		// STAGE 3: FINAL FALLBACK
		return Pathment.createUnspecified(cleanedText);
	}
	
	/** 
	 * Performs a full extraction of all potential Pathments from a raw text block.
	 * <p>
	 * This method uses a master regex to find candidate strings, cleans them, and then uses the {@link #parseSingle(String)} engine to classify each one. It intelligently handles
	 * and discards semantic duplicates (e.g., "google.com" and "https://google.com"). The final lists are sorted for a consistent user experience.
	 *
	 * @param rawText The block of text to scan.
	 * @return A {@link ScanResult} record containing sorted lists of validly identified Pathments and potentially interesting, but unspecified, tokens.
	 */
	public static ScanResult performExtraction(String rawText) {
		if (StringUtils.isBlank(rawText)) {
			return new ScanResult(new ArrayList<>(), new ArrayList<>());
		}

		// Use Sets to automatically handle semantic duplicates via the Pathment's equals()/hashCode().
		Set<Pathment> validPathments = new HashSet<>();
		Set<Pathment> unspecifiedTokens = new HashSet<>();

		Matcher matcher = MASTER_EXTRACTION_PATTERN.matcher(rawText);

		while (matcher.find()) {
			String candidate = cleanCandidate(matcher.group(0));

			if (StringUtils.isNotBlank(candidate)) {
				Pathment p = parseSingle(candidate);

				// The .add() method will only add the element if an "equal" one
				// (as defined by Pathment.equals()) is not already present.
				if (p.getType().isSpecified()) {
					validPathments.add(p);
				} else {
					// Only consider unspecified tokens that look somewhat interesting (e.g., contain a dot).
					if (candidate.contains(".")) {
						unspecifiedTokens.add(p);
					}
				}
			}
		}

		// Convert sets to lists and sort them for a predictable order in the UI.
		List<Pathment> sortedValid = new ArrayList<>(validPathments);
		sortedValid.sort(Pathment.createDefaultComparator());

		List<Pathment> sortedUnspecified = new ArrayList<>(unspecifiedTokens);
		sortedUnspecified.sort(Pathment.createDefaultComparator());

		return new ScanResult(sortedValid, sortedUnspecified);
	}

	/**
	 * A private helper to clean up a raw candidate string found by the regex. It trims whitespace and removes common trailing punctuation that is often part of the surrounding
	 * sentence rather than the link itself.
	 *
	 * @param rawCandidate The string matched by the regex.
	 * @return A cleaned, trimmed candidate string.
	 */
	private static String cleanCandidate(String rawCandidate) {
		if (rawCandidate == null)
			return "";

		String candidate = rawCandidate.trim();

		// Split at sentence boundaries (e.g., "google.com, and then...") and take the first part.
		String[] parts = candidate.split("[.,;]\\s+", 2);
		candidate = parts[0];

		// Remove any remaining trailing punctuation.
		return candidate.replaceAll("[.,;\\s]+$", "");
	}

//
//	private static boolean isIsolatedToken(String rawText, String candidate, int startIndex) {
//		int endIndex = startIndex + candidate.length();
//		boolean hasPrecedingSpace = startIndex > 0 && rawText.charAt(startIndex - 1) == ' ';
//		boolean hasFollowingSpace = endIndex < rawText.length() && rawText.charAt(endIndex) == ' ';
//		boolean hasPunctuation = candidate.matches(".*[.,;]$");
//
//		return hasPrecedingSpace || hasFollowingSpace || hasPunctuation;
//	}
//
//	private static List<String> extractIsolatedTokens(String rawText) {
//		List<String> tokens = new ArrayList<>();
//		Matcher matcher = Pattern.compile("\\b[^\\s]+\\.(?:zip|txt|docx|sh|conf|exe)\\b").matcher(rawText);
//		while (matcher.find()) {
//			tokens.add(matcher.group(0));
//		}
//		return tokens;
//	}

	public static FilePath splitFilePath(String filePath) {
		if (StringUtils.isBlank(filePath))
			return null;

		String originalPath = filePath.trim();
		String path = originalPath;

		String protocol = "";
		String drive = "";
		String directories = "";
		String fileName = "";
		String extension = "";

		if (path.toLowerCase().startsWith("file:")) {
			protocol = "file";
			path = path.substring(5).replaceAll("^[/\\\\]+", "");
		}

		if (path.matches("^[a-zA-Z]:[/\\\\].*")) {
			drive = path.substring(0, 2);
			path = path.substring(2);
		} else if (path.matches("^[a-zA-Z]:$")) {
			drive = path;
			path = "";
		}

		path = path.replace('\\', '/');

		int lastSlashIndex = path.lastIndexOf('/');
		if (lastSlashIndex != -1) {
			directories = path.substring(0, lastSlashIndex + 1);
			fileName = path.substring(lastSlashIndex + 1);
		} else {
			fileName = path;
		}

		int lastDotIndex = fileName.lastIndexOf('.');
		if (lastDotIndex > 0) {
			extension = fileName.substring(lastDotIndex + 1);
			fileName = fileName.substring(0, lastDotIndex);
		} else if (originalPath.endsWith("/") || originalPath.endsWith("\\")) {
			directories += fileName + "/";
			fileName = "";
		}

		if (StringUtils.isNotBlank(drive) && StringUtils.isBlank(directories) && StringUtils.isBlank(fileName) && StringUtils.isBlank(extension)) {
			directories = "/";
		}
		if (StringUtils.isNotBlank(drive) && directories != null) {
			directories = directories.replace('/', java.io.File.separatorChar);
		}

		boolean schemeFound = !protocol.isEmpty();
		return new FilePath(schemeFound, drive, directories, fileName, extension);
	}

	private static final Pattern WEB_URL_SPLIT_PATTERN = Pattern.compile("^(?:(\\w+):\\/\\/)?" + // Group 1: Protocol (e.g., "https")
	// Group 2: The entire host part (domain, localhost, or IP address)
	        "(" + "(?:(?:[\\w-]+\\.)+[a-zA-Z]{2,24})" + // Option A: A standard domain name
	        "|" + "localhost" + // Option B: The literal word "localhost"
	        "|" + "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)" + // Option C: An IPv4 address
	        ")" + "(?::(\\d+))?" + // Group 3: Port (optional)
	        "(\\/[^?#]*)?" + // Group 4: Path (optional)
	        "(?:\\?([^#]*))?" + // Group 5: Query (optional)
	        "(?:#(.*))?$" // Group 6: Fragment (optional)
	);

	public static WebUrl splitWebUrl(String url) {
		if (StringUtils.isBlank(url)) {
			return null;
		}

		Matcher matcher = WEB_URL_SPLIT_PATTERN.matcher(url);
		if (!matcher.find()) {
			return null; // No match found.
		}

		try {
			// --- 1. Extract components from regex groups ---
			String protocolStr = matcher.group(1);
			String fullHost = matcher.group(2);
			String portStr = matcher.group(3);
			String path = matcher.group(4);
			String query = matcher.group(5);
			String fragment = matcher.group(6);

			// --- 2. Determine protocol using the robust fromScheme method ---
			TransferProtocol protocol = TransferProtocol.fromScheme(protocolStr);

			// --- 3. Parse host into domain and subdomain ---
			String subdomain = null;
			String domain = fullHost; // Default to the full host

			int firstDotIndex = fullHost.indexOf('.');
			int lastDotIndex = fullHost.lastIndexOf('.');

			// A host has a subdomain if it has at least two dots and is not an IP address.
			if (firstDotIndex != -1 && firstDotIndex != lastDotIndex && !IP_ADDRESS_PATTERN.matcher(fullHost).matches()) {
				subdomain = fullHost.substring(0, firstDotIndex);
				domain = fullHost.substring(firstDotIndex + 1);
			}
			// This heuristic correctly handles "localhost" and IP addresses (subdomain remains null).

			// --- 4. Finalize components and create WebUrl object ---
			int port = (portStr != null) ? Integer.parseInt(portStr) : -1;

			return new WebUrl(protocol, subdomain, domain, port, path, query, fragment);

		} catch (Exception e) {
			Log.error(e, "Error while splitting WebUrl from string: %s", url);
			return null; // Return null on any parsing exception.
		}
	}

	/**
	 * Validates a parsed WebUrl object and finalizes it by assigning a default protocol if none was present. This method acts as the final quality gate before a WebUrl is
	 * accepted.
	 *
	 * @param webUrl       The WebUrl object produced by splitWebUrl.
	 * @param rawUrlString The original string, for context in logging.
	 * @return {@code true} if the WebUrl is valid, {@code false} otherwise.
	 */
	private static boolean validateAndFinalizeWebUrl(WebUrl webUrl, String rawUrlString) {
		// --- 1. Basic Object Validation ---
		if (webUrl == null)
			return false;
		String fullHost = webUrl.getHost();
		if (StringUtils.isBlank(fullHost))
			return false;

		// --- 2. Structural Host Validation ---

		// A valid host must not contain underscores.
		if (fullHost.contains("_"))
			return false;

		// A valid port number must be in the correct range.
		if (!(webUrl.getPort() == -1 || (webUrl.getPort() >= 0 && webUrl.getPort() <= 65535))) {
			return false;
		}

		// --- 3. Host Type Specific Validation ---

		boolean isIpOrLocalhost = fullHost.equalsIgnoreCase("localhost") || IP_ADDRESS_PATTERN.matcher(fullHost).matches();

		if (!isIpOrLocalhost) {
			// If it's not an IP or localhost, it must match the stricter hostname pattern.
			if (!HOSTNAME_PATTERN.matcher(fullHost).matches()) {
				Log.fine("[validateAndFinalizeWebUrl] Host '%s' failed hostname regex validation.", fullHost);
				return false;
			}

		}

		// --- 4. Finalization (Side Effect) ---
		// If all checks passed and no protocol was detected, default to HTTPS.
		// This is the only place where the WebUrl object is modified.
//		if (webUrl.getTransferProtocol() == TransferProtocol.NONE) {
//			webUrl.setProtocol(TransferProtocol.HTTPS);
//		}

		return true; // All checks passed.
	}

	public static String normalizePathSeparators(String path) {
		return path.replace('\\', '/');
	}

	public static String constructFileUrlPath(java.nio.file.Path path) {
		String pathString = path.toString().replace("\\", "/");
		return "file://" + pathString;
	}

	public static String removeFilePrefix(String path) {
		path = path.replaceAll("^file:", "");
		path = path.replaceAll("^[/\\\\]+", "");
		return path;
	}

	public static String getTextWithoutExtractedString(String fullText, String extracted, boolean returnTextAfterExtractedStringOnly) {
		if (extracted == null || fullText.length() == extracted.length() && fullText.equals(extracted)) {
			return "";
		}
		if (!returnTextAfterExtractedStringOnly) {
			String text = fullText.replace(extracted, PATH_CONTAINER).trim();
			return text.isEmpty() ? "" : text;
		} else {
			String text = fullText.replace(extracted, PATH_CONTAINER);
			int index = text.indexOf(PATH_CONTAINER) + PATH_CONTAINER.length();
			return index < text.length() ? text.substring(index).trim() : "";
		}
	}

	public record ScanResult(List<Pathment> validPathments, List<Pathment> unspecifiedTokens) {
	}
}