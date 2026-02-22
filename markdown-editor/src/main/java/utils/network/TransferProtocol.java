package utils.network;

import java.io.File;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import utils.general.StringUtils;
import utils.logging.Log;

/**
 * Represents a standardized transfer protocol for classifying and handling various path and address types (URLs, local paths, commands, etc.).
 * <p>
 * This enum acts as the central authority for protocol detection and normalization within the Pathment system. It provides a robust, prioritized detection mechanism and utility
 * methods to handle different address formats consistently.
 */
public enum TransferProtocol {

    // --- Core Protocols ---
	NONE("<?>", null, null, "Not specified or could not be determined"),
	PATH("PATH", null, null, "Local File System Path"),
	UNC("UNC", "\\\\", "unc", "UNC Network Share Path"), // Changed label from "PATH" to "UNC" for clarity

    // --- URL-based Protocols ---
	FILE_URL("FILE-URI", "file://", "file", "URI for a network file resource (e.g., file://server/share)"),

	HTTPS("HTTPS", "https://", "https", "Secure Hypertext Transfer Protocol (SSL/TLS)"), // Changed label
	HTTP("HTTP", "http://", "http", "Hypertext Transfer Protocol (Unencrypted)"), // Changed label
	FTP("FTP", "ftp://", "ftp", "File Transfer Protocol"), // Changed label
	SSH("SSH", "ssh://", "ssh", "Secure Shell Protocol"), // Changed label
	TELNET("TELNET", "telnet://", "telnet", "Telnet Protocol"), // <<< NEUE ZEILE

    // --- Mail-related Protocols ---
	MAILTO("EMAIL", "mailto:", "mailto", "Protocol for Email Addresses"), // Changed label from "mailto:"
	SMTP("SMTP", "smtp://", "smtp", "Simple Mail Transfer Protocol"), // Changed label
	POP3("POP3", "pop3://", "pop3", "Post Office Protocol version 3"), // Changed label
	IMAP("IMAP", "imap://", "imap", "Internet Message Access Protocol"), // Changed label

    // --- Command-related Protocols ---
	COMMAND("CMD", null, "cmd", "Internal or Execution Command"),
	PROMPT("PROMPT", null, "prompt", "Command Prompt Indicator"),

    // --- Edge Case / Error Handling ---
	MALFORMED_FILE_URL("FILE-URI?", "file://", "file", "Malformed file scheme (e.g., file:/path)");

	// =================================================================================
	// Constants and Static Initializer
	// =================================================================================

	private static final Map<String, TransferProtocol> SCHEME_MAP           = new HashMap<>();
	private static final Pattern                       WINDOWS_PATH_PATTERN = Pattern.compile("^[a-zA-Z]:[/\\\\].*");

	static {
		for (TransferProtocol protocol : TransferProtocol.values()) {
			if (protocol.uriScheme != null) {
				SCHEME_MAP.put(protocol.uriScheme.toLowerCase(), protocol);
			}
		}
	}

	private final String label;
	private final String notation;
	private final String uriScheme;
	private final String description;

	TransferProtocol(String label, String notation, String uriScheme, String description) {
		this.label = label;
		this.notation = notation;
		this.uriScheme = uriScheme;
		this.description = description;
	}

	// =================================================================================
	// The Core Detection Logic (Optimized)
	// =================================================================================

	/**
	 * Detects the transfer protocol from a raw address string using a prioritized, robust checking mechanism.
	 * <p>
	 * This method avoids simple splitting and instead checks for unambiguous indicators in a specific order (e.g., UNC paths before generic file paths) to ensure the highest
	 * accuracy.
	 *
	 * @param address The raw address string to analyze. Can be null.
	 * @return The detected {@link TransferProtocol}, or {@code NONE} if the address is blank or no specific protocol could be determined.
	 */
	public static TransferProtocol detect(String address) {
		if (StringUtils.isBlank(address)) {
			return NONE;
		}

		String lowerAddr = address.toLowerCase();

		// 1. Check for the most unambiguous structural indicators first.
		if (lowerAddr.startsWith("\\\\"))
			return UNC;
		if (lowerAddr.startsWith("ssh://"))
			return SSH;
		if (lowerAddr.startsWith("$ ") || lowerAddr.startsWith("> "))
			return PROMPT;

		// 2. Check for common URI schemes.
		if (lowerAddr.startsWith("mailto:"))
			return MAILTO;
		if (lowerAddr.startsWith("https://"))
			return HTTPS;
		if (lowerAddr.startsWith("http://"))
			return HTTP;
		if (lowerAddr.startsWith("ftp://"))
			return FTP;
		if (lowerAddr.startsWith("telnet://"))
			return TELNET;
		if (lowerAddr.startsWith("smtp://"))
			return SMTP;
		if (lowerAddr.startsWith("pop3://"))
			return POP3;
		if (lowerAddr.startsWith("imap://"))
			return IMAP;

		// 3. Handle the 'file:' scheme, which can be valid or malformed.
		if (lowerAddr.startsWith("file:")) {
			return lowerAddr.startsWith("file://") ? FILE_URL : MALFORMED_FILE_URL;
		}

		// 4. Check for absolute and relative file system path structures.
		if (WINDOWS_PATH_PATTERN.matcher(address).matches() || address.startsWith("/")) {
			return PATH; // Covers both Windows (e.g., C:\) and Unix-like (e.g., /home/) absolute paths.
		}
		if (address.startsWith("./") || address.startsWith("../") || address.startsWith(".\\") || address.startsWith("..\\")) {
			return PATH; // Covers relative paths.
		}

		// 5. If no other indicators match, it's an unknown type.
		return NONE;
	}

	/**
	 * Detects the protocol from a standard {@link URI} object by delegating to the {@link #fromScheme(String)} method.
	 *
	 * @param uri The URI to analyze.
	 * @return The detected protocol, or {@code NONE} if the URI or its scheme is null.
	 */
	public static TransferProtocol detect(URI uri) {
		if (uri == null) {
			return NONE;
		}
		// Delegate the core logic to fromScheme.
		return fromScheme(uri.getScheme());
	}

	/**
	 * Returns the TransferProtocol enum constant for a given URI scheme string. This is the central, canonical method for scheme-based lookups. It is case-insensitive and uses the
	 * internal scheme map for efficiency.
	 *
	 * @param scheme The scheme string (e.g., "http", "ftp"). Can be null or blank.
	 * @return The corresponding TransferProtocol, or {@code NONE} if not found.
	 */
	public static TransferProtocol fromScheme(String scheme) {
		if (StringUtils.isBlank(scheme)) {
			return NONE;
		}
		return SCHEME_MAP.getOrDefault(scheme.toLowerCase(), NONE);
	}
	// =================================================================================
	// Normalization and Utility Methods (Consolidated)
	// =================================================================================

	/**
	 * Normalizes a given address string by fixing common issues like incorrect slashes or missing protocol separators.
	 *
	 * @param address The raw address string.
	 * @return A normalized version of the address.
	 */
	public static String normalize(String address) {
		if (StringUtils.isBlank(address)) {
			return address;
		}

		String a = address.trim().replace('\\', '/');

		// Fix malformed file URIs (e.g., file:/C... -> file:///C...)
		if (a.matches("^file:/[a-zA-Z]:.*")) {
			return a.replaceFirst("file:/", "file:///");
		}
		if (a.matches("^file:/+[^/].*")) {
			return a.replaceFirst("file:/+", "file://");
		}

		// Add missing "//" for other URL-based protocols
		int colonIndex = a.indexOf(':');
		if (colonIndex > 0 && (colonIndex + 1) < a.length() && a.charAt(colonIndex + 1) != '/') {
			String scheme = a.substring(0, colonIndex);
			TransferProtocol p = SCHEME_MAP.get(scheme);
			if (p != null && p.isUrlBased()) {
				return scheme + "://" + a.substring(colonIndex + 1);
			}
		}

		return a;
	}

	/**
	 * Converts a file address (URI or local path) to a normalized Path object. Handles file URIs (file://), Windows paths (C:\...), and Unix paths (/path).
	 * 
	 * @param fileAddress The file address to convert (can be URI or local path)
	 * @return Normalized Path object, or null if conversion fails
	 * @throws IllegalArgumentException if input is null or blank
	 */
	public static Path getLocalizedPath(String fileAddress) {
		Objects.requireNonNull(fileAddress, "fileAddress cannot be null");

		String normalized = fileAddress.replace('\\', '/').trim();
		if (normalized.isBlank()) {
			throw new IllegalArgumentException("fileAddress cannot be blank");
		}

		// Strip file: prefix if present
		if (normalized.toLowerCase().startsWith("file:")) {
			normalized = normalized.replaceFirst("(?i)file:/*", "");
		}

		try {
			return new File(normalized).toPath().normalize();
		} catch (InvalidPathException e) {
			Log.warn("Invalid path syntax: " + fileAddress, e);
			return null;
		} catch (SecurityException e) {
			Log.warn("Security violation while accessing path: " + fileAddress, e);
			return null;
		}
	}

	/**
	 * Removes the protocol notation from the beginning of an address string.
	 *
	 * @param address The address string with a protocol.
	 * @return The address without the protocol notation.
	 */
	public static String removeFrom(String address) {
		TransferProtocol protocol = detect(address);
		if (protocol == NONE || !protocol.hasNotation()) {
			return address;
		}
		return address.substring(protocol.getNotation().length());
	}

	// =================================================================================
	// Getters and Boolean Checks
	// =================================================================================

	public String getLabelText() {
		return label != null ? label : "";
	}

	public String getNotation() {
		return notation != null ? notation : "";
	}

	public String getScheme() {
		return uriScheme;
	}

	public String getDescription() {
		return description;
	}

	public boolean hasNotation() {
		return StringUtils.isNotBlank(notation);
	}

	public boolean isWeb() {
		return this == HTTPS || this == HTTP;
	}

	public boolean isFileUrl() {
		return this == FILE_URL;
	}

	public boolean isLocalOrUncPath() {
		return this == PATH || this == UNC;
	}

	/**
	 * Checks if a given URI scheme string (e.g., "http", "https") corresponds to a web protocol. This is a convenience method for validating raw scheme strings.
	 *
	 * @param scheme The URI scheme to check (case-insensitive).
	 * @return true if the scheme is HTTP or HTTPS; false otherwise.
	 */
	public static boolean isWebScheme(String scheme) {
		if (StringUtils.isBlank(scheme)) {
			return false;
		}
		// Use our robust SCHEME_MAP to find the protocol constant
		TransferProtocol protocol = SCHEME_MAP.get(scheme.toLowerCase());
		// Then, use the reliable instance method
		return protocol != null && protocol.isWeb();
	}

	/**
	 * Checks if this protocol follows a standard URL structure (scheme://host...).
	 * 
	 * @return true for most network-based protocols.
	 */
	public boolean isUrlBased() {
		return switch (this) {
		case HTTPS, HTTP, FTP, SSH, SMTP, POP3, IMAP -> true;
		default                                      -> false;
		};
	}

	/**
	 * Determines if this protocol type supports an active, user-triggered verification check.
	 * 
	 * @return {@code true} if an active check (network, file system) is meaningful.
	 */
//	public boolean isVerifiable() {
//		return switch (this) {
//		case HTTPS, HTTP, FTP, SSH, FILE_URL, PATH, UNC -> true;
//		default                                         -> false;
//		};
//	}
}