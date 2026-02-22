package utils.detection;

import java.util.Objects;

import utils.general.StringUtils;
import utils.logging.Log;
import utils.network.TransferProtocol;

/**
 * Represents a parsed path or address, encapsulating its type, components, and protocol. This class is immutable at its core (except for the title) and should be instantiated
 * exclusively through its public static factory methods.
 */
public final class Pathment {

	public enum Type {
	    // =========================================================================
	    // General & Fallback Types
	    // =========================================================================

		/**
		 * A catch-all type for strings that could not be confidently classified as any other specific type. This is the default state and indicates a parsing failure or an
		 * unrecognized format.
		 */
		UNSPECIFIED,

	    // =========================================================================
	    // Web & Network Address Types
	    // =========================================================================

		/**
		 * A standard web address, typically using HTTP or HTTPS protocols. Example: "https://www.google.com/search?q=test"
		 */
		URL_ADDRESS,

		/**
		 * An email address, typically associated with the 'mailto:' protocol. Example: "user@example.com"
		 */
		EMAIL,

		/**
		 * A File Transfer Protocol (FTP) address. Example: "ftp://ftp.debian.org/debian/"
		 */
		FTP_ADDRESS,

		/**
		 * A Secure Shell (SSH) address for remote server access. Example: "ssh://user@hostname"
		 */
		SSH_ADDRESS,

		/**
		 * A Telnet address for remote terminal sessions. Example: "telnet://hostname:23"
		 */
		TELNET_ADDRESS,

		/**
		 * A raw IPv4 or IPv6 network address. Example: "192.168.1.1"
		 */
		IP_ADDRESS,

		/**
		 * A standalone network hostname that is not a full URL. Example: "fileserver-01" or "my-local-machine"
		 */
		HOSTNAME,

	    // =========================================================================
	    // File System Path Types
	    // =========================================================================

		/**
		 * A path to a local file using the "file://" URI scheme. Example: "file:///C:/Users/Test/file.txt"
		 */
		FILE_URL,

		/**
		 * A native, absolute path on the local file system, typically starting with a drive letter on Windows or a '/' on Unix-like systems. Example: "C:\Windows\System32" or
		 * "/home/user/documents"
		 */
		LOCAL_PATH,

		/**
		 * A path to a network share using the Universal Naming Convention (UNC). Example: "\\server-name\share\folder"
		 */
		UNC_PATH,

		/**
		 * A path that is relative to a current working directory. Example: "./data/file.csv" or "../images/logo.png"
		 */
		RELATIVE_PATH,

	    // =========================================================================
	    // Command & Execution Types
	    // =========================================================================

		/**
		 * A string that represents a command to be executed, not a resource to be located. Example: "shutdown -s -t 0"
		 */
		EXEC_COMMAND,

		/**
		 * A string that represents a command-line prompt, indicating user input is expected. Example: ">" or "$"
		 */
		PROMPT;

		// --- Helper Methods ---

		/**
		 * Checks if this type is one of the specified target types. This is a flexible and convenient way to check for type membership.
		 * <p>
		 * Example: {@code getType().isOneOf(Type.LOCAL_PATH, Type.UNC_PATH)}
		 *
		 * @param types The varargs array of types to check against.
		 * @return {@code true} if this type is present in the provided types.
		 */
		public boolean isOneOf(Type... types) {
			if (types == null)
				return false;
			for (Type targetType : types) {
				if (this == targetType)
					return true;
			}
			return false;
		}

		/**
		 * Determines if a Pathment of this type can have its status actively verified (e.g., by checking a network connection or file system).
		 * 
		 * @return true if the type is verifiable, false otherwise.
		 */
		public boolean isVerifiable() {
			return isOneOf(URL_ADDRESS, FTP_ADDRESS, SSH_ADDRESS, TELNET_ADDRESS, // Network checks
			        FILE_URL, LOCAL_PATH, UNC_PATH // File system checks
			);
		}

		/** Checks if the type is anything other than UNSPECIFIED. */
		public boolean isSpecified() {
			return this != UNSPECIFIED;
		}

		/** Checks if the type is a standard web URL (HTTP/HTTPS). */
		public boolean isWebUrl() {
			return this == URL_ADDRESS; // URL_ADDRESS covers HTTP and HTTPS by definition in fromWebUrl
		}

		/** Checks if this type represents any form of file-based address. */
		public boolean isFileBased() {
			return isOneOf(LOCAL_PATH, UNC_PATH, FILE_URL, RELATIVE_PATH);
		}

		/**
		 * Checks if the type is a native local or UNC file system path. This is useful for differentiating from file URLs.
		 * 
		 * @return true if the type is LOCAL_PATH or UNC_PATH.
		 */
		public boolean isLocalOrUncPath() {
			return isOneOf(Type.LOCAL_PATH, Type.UNC_PATH);
		}

		/**
		 * Checks if the type is a file URL (e.g., "file:///...").
		 * 
		 * @return true if the type is FILE_URL, false otherwise.
		 */
		public boolean isFileUrl() {
			return this == FILE_URL;
		}

		/**
		 * Checks if the type is a native local file system path (e.g., "C:\...").
		 * 
		 * @return true if the type is LOCAL_PATH, false otherwise.
		 */
		public boolean isLocalPath() {
			return this == LOCAL_PATH;
		}

		/**
		 * Checks if the type is an email address.
		 * 
		 * @return true if the type is EMAIL, false otherwise.
		 */
		public boolean isEmail() {
			return this == EMAIL;
		}

		/**
		 * Checks if the type is an FTP address.
		 * 
		 * @return true if the type is FTP_ADDRESS, false otherwise.
		 */
		public boolean isFTP() {
			return this == FTP_ADDRESS;
		}

		/**
		 * Checks if the type is a relative file path (e.g., "./...").
		 * 
		 * @return true if the type is RELATIVE_PATH, false otherwise.
		 */
		public boolean isRelativePath() {
			return this == RELATIVE_PATH;
		}

		/**
		 * Checks if the type is a raw IP address.
		 * 
		 * @return true if the type is IP_ADDRESS, false otherwise.
		 */
		public boolean isIpAddress() {
			return this == IP_ADDRESS;
		}

		/**
		 * Checks if the type is a standalone hostname.
		 * 
		 * @return true if the type is HOSTNAME, false otherwise.
		 */
		public boolean isHostname() {
			return this == HOSTNAME;
		}

		public boolean isUncPath() {
			return this == UNC_PATH;
		}

		/**
		 * Determines if a Pathment of this type is useful enough to be offered as a direct action from the clipboard (e.g., via the 'From Clipboard' button).
		 */
		public boolean isActionableForClipboard() {
			return isWebUrl() || isFileBased() || isEmail();
		}
	}

	// =================================================================================
	// Fields (Single, Final Declaration)
	// =================================================================================

	private final Type             type;
	private final TransferProtocol protocol;
	private String                 title;                 // Title is the only mutable property
	private final String           addressWithoutProtocol;
	private final WebUrl           webUrl;
	private final FilePath         filePath;

	// =================================================================================
	// Private Constructor (The ONLY Constructor)
	// =================================================================================

	/**
	 * The single, canonical private constructor. All factory methods must lead here. This ensures every Pathment is created in a consistent state.
	 */
	private Pathment(Type type, TransferProtocol protocol, String title, String address, WebUrl webUrl, FilePath filePath) {
		this.type = type;
		this.protocol = protocol;
		this.title = title;
		this.addressWithoutProtocol = address;
		this.webUrl = webUrl;
		this.filePath = filePath;
	}

	// =================================================================================
	// Public Static Factory Methods (The "Front Doors")
	// =================================================================================

	/** Creates a Pathment for an email address. */
	public static Pathment fromEmail(String emailAddress) {
		return new Pathment(Type.EMAIL, TransferProtocol.MAILTO, emailAddress, emailAddress, null, null);
	}

	/** Creates a Pathment for an IP address. */
	public static Pathment fromIpAddress(String ipAddress) {
		return new Pathment(Type.IP_ADDRESS, TransferProtocol.NONE, ipAddress, ipAddress, null, null);
	}

	/** Creates a Pathment for a simple hostname. */
	public static Pathment fromHostname(String hostname) {
		return new Pathment(Type.HOSTNAME, TransferProtocol.NONE, hostname, hostname, null, null);
	}

	/**
	 * Creates a Pathment from a fully parsed WebUrl object. This factory method correctly maps the transfer protocol (e.g., HTTP, FTP, SSH, TELNET) to the appropriate semantic
	 * Pathment.Type.
	 *
	 * @param webUrl The fully parsed WebUrl object. Must not be null.
	 * @return A new, correctly typed Pathment instance, or an UNSPECIFIED Pathment if the input is null.
	 */
	public static Pathment fromWebUrl(WebUrl webUrl) {
		if (webUrl == null) {
			return createUnspecified("null_weburl_input");
		}

		// Determine the specific semantic type based on the protocol.
		Type urlType = switch (webUrl.getTransferProtocol()) {
		case FTP    -> Type.FTP_ADDRESS;
		case SSH    -> Type.SSH_ADDRESS;
		case TELNET -> Type.TELNET_ADDRESS; // <-- Ergänzung für Telnet
		default     -> Type.URL_ADDRESS; // Covers HTTP, HTTPS, and other web-like protocols.
		};

		return new Pathment(urlType, webUrl.getTransferProtocol(), webUrl.getHost(), // Use the host as the initial title.
		        webUrl.getDisplayUrlWithoutProtocol(), webUrl, null);
	}
	// Ersetzen Sie die fromFilePath-Methode in Pathment.java mit DIESER finalen, robusten Version.

	public static Pathment fromFilePath(FilePath filePath) {
		if (filePath == null) {
			return createUnspecified("null_filepath_input");
		}

		// --- 1. Guard Clause for Malformed URIs (CRITICAL CHECK) ---
		// We check specifically for the malformed case first. A URI is malformed if it
		// had a "file:" scheme but wasn't a valid "file://" URI.
		if (filePath.hasScheme() && TransferProtocol.detect(filePath.getFullPath()) == TransferProtocol.MALFORMED_FILE_URL) {
			// Classify this explicitly and exit early.
			return new Pathment(Type.FILE_URL, // It's semantically a file URL, just a broken one.
			        TransferProtocol.MALFORMED_FILE_URL, // Use the specific error protocol.
			        filePath.getFullName(), filePath.getPathWithoutProtocol(), null, filePath);
		}

		// --- 2. Prioritized Classification for VALID Paths ---
		// If we reach this point, the path is not a malformed URI.

		Type fileType;
		TransferProtocol finalProtocol;

		boolean hasDrive = StringUtils.isNotBlank(filePath.getDrive());
		String fullPath = filePath.getFullPath();
		boolean isUnixRootPath = fullPath.startsWith("/") && !hasDrive;

		if (!hasDrive && (fullPath.startsWith("//") || fullPath.startsWith("\\\\"))) {
			fileType = Type.UNC_PATH;
			finalProtocol = TransferProtocol.UNC;
		} else if (filePath.hasScheme() && !hasDrive) { // wasOriginallyFileUrl is now filePath.hasScheme()
			fileType = Type.FILE_URL;
			finalProtocol = TransferProtocol.FILE_URL;
		} else if (isRelativePath(filePath)) {
			fileType = Type.RELATIVE_PATH;
			finalProtocol = TransferProtocol.PATH;
		} else if (isValidLocalPath(filePath) || isUnixRootPath) {
			fileType = Type.LOCAL_PATH;
			finalProtocol = TransferProtocol.PATH;
		} else {
			fileType = Type.UNSPECIFIED;
			finalProtocol = TransferProtocol.NONE;
		}

		return new Pathment(fileType, finalProtocol, filePath.getFullName(), filePath.getPathWithoutProtocol(), null, filePath);
	}

	/**
	 * Creates a Pathment for an unclassified/fallback string, allowing an explicit title to be provided. This is the preferred method when a meaningful title is already known.
	 *
	 * @param rawText The raw address string for the Pathment.
	 * @param title   The explicit title to use for this Pathment.
	 * @return A new UNSPECIFIED Pathment instance.
	 */
	public static Pathment createUnspecified(String rawText, String title) {
		if (rawText == null)
			rawText = "";
		if (title == null)
			title = ""; // Ensure title is not null

		return new Pathment(Type.UNSPECIFIED, TransferProtocol.NONE, title, rawText, null, null);
	}

	/**
	 * Creates a Pathment for an unclassified/fallback string, automatically generating a title by truncating the raw text. Use the overloaded version if a better title is
	 * available.
	 */
	public static Pathment createUnspecified(String rawText) {
		if (rawText == null)
			rawText = "";
		String generatedTitle = StringUtils.truncate(rawText.trim(), 60);

		// Delegate to the more specific factory method
		return createUnspecified(rawText, generatedTitle);
	}

	public static String getDecodedUrl(String url) {
		if (StringUtils.isBlank(url))
			return url;
		String decoded = url;
		try {
			// Wir dekodieren bis zu 3-mal, um Schachtelungen wie %2525 zu entfernen
			for (int i = 0; i < 3; i++) {
				String next = java.net.URLDecoder.decode(decoded, java.nio.charset.StandardCharsets.UTF_8);
				if (next.equals(decoded))
					break;
				decoded = next;
			}
		} catch (Exception e) {
			// Bei Fehlern geben wir den letzten stabilen Stand zurück
		}
		return decoded;
	}

	// =================================================================================
	// Getters and Setters
	// =================================================================================

	public Type getType() {
		return type;
	}

	public TransferProtocol getProtocol() {
		return protocol;
	}

	public String getTitle() {
		return title;
	}

	public WebUrl getWebUrl() {
		return webUrl;
	}

	public FilePath getFilePath() {
		return filePath;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	// =================================================================================
	// DISPLAY AND UTILITY METHODS (REVISED AND FINALIZED)
	// =================================================================================

	/**
	 * Returns a concise, user-friendly label representing the high-level classification of this Pathment. This is the definitive method for the **first column** of your UI.
	 *
	 * @return A string like "HTTPS", "PATH", "EMAIL", "IP", etc.
	 */
	public String getDisplayTypeLabel() {
		return switch (type) {
		// For URLs, use the specific protocol label we defined in TransferProtocol
		case URL_ADDRESS, FTP_ADDRESS, SSH_ADDRESS -> protocol.getLabelText();
		// For all other types, use a clear, high-level classification
		case EMAIL      -> "EMAIL";
		case IP_ADDRESS -> "IP";
		case HOSTNAME   -> "HOST";
		// Group all file-based paths under a single, clean label
		case LOCAL_PATH, UNC_PATH, FILE_URL, RELATIVE_PATH -> protocol.getLabelText(); // "PATH", "UNC", "FILE"
		case UNSPECIFIED                                   -> "[TXT]";
		default                                            -> ""; // Should not happen, but safe
		};
	}

	/**
	 * Gets the main, human-readable part of the address, **ALWAYS WITHOUT** any protocol prefix. This is the definitive method for the **second column** of your UI. It provides
	 * just the core content, ensuring consistency across all types.
	 *
	 * @return The address string (e.g., "www.google.com", "C:\...", "user@example.com").
	 */
	public String getAddressForDisplay() {
		return addressWithoutProtocol != null ? addressWithoutProtocol : "";
	}

	/**
	 * Constructs a full, functional URI or address string suitable for actions like "Open Link" or "Copy Full URI". This method **ALWAYS INCLUDES** the necessary protocol/scheme
	 * notation.
	 *
	 * @return A complete URI string (e.g., "https://...", "file:///...", "mailto:...").
	 */
	public String getAddressForUri() {
		// Case 1: WebUrl object exists (handles http, https, ftp, ssh, etc.)
		if (webUrl != null && (type.isWebUrl() || type == Type.FTP_ADDRESS || type == Type.SSH_ADDRESS)) {
			return webUrl.toUriString();
		}

		// Case 2: FilePath object exists (handles local, unc, file url)
		if (filePath != null && type.isFileBased()) {
			try {
				// Let Java's native libraries build the correct, encoded URI
				return new java.io.File(filePath.getFullPath()).toURI().toASCIIString();
			} catch (Exception e) {
				Log.error(e, "Could not convert FilePath to URI string: " + filePath.getFullPath());
				// Fallback to a constructed string if URI conversion fails
				return getProtocol().getNotation() + getAddressForDisplay();
			}
		}

		// Case 3: Handle email addresses specifically
		if (type.isEmail()) {
			return "mailto:" + getAddressForDisplay();
		}

		// Fallback for others like IP, HOSTNAME, or UNSPECIFIED
		return getAddressForDisplay();
	}

	/**
	 * Returns the raw address component without any protocol notation. This is a fundamental getter. `getAddressForDisplay()` is the preferred alias for UI code.
	 *
	 * @return The raw address string.
	 */
	public String getAddressWithoutProtocol() {
		return addressWithoutProtocol != null ? addressWithoutProtocol : "";
	}

	/**
	 * Generates a canonical, normalized string representation of this Pathment's address, specifically designed for robust, logical comparison (e.g., for duplicate detection).
	 * <p>
	 * This method applies different normalization strategies based on the {@link Pathment.Type} to ensure that functionally identical addresses produce the same string output. It
	 * allows optional exclusion of specific URL components.
	 *
	 * @param ignoreProtocol  If true, the protocol (e.g., "https://") is excluded from the normalized web URL string.
	 * @param ignoreSubdomain If true, the subdomain (e.g., "www.") is excluded from the normalized web URL string.
	 * @param ignoreQuery     If true, the query part (e.g., "?name=value") is excluded from the normalized web URL string.
	 * @param ignoreFragment  If true, the fragment part (e.g., "#section") is excluded from the normalized web URL string.
	 * @return A canonical, normalized string suitable for comparison, or an empty string if not applicable.
	 */
	public String getCanonicalAddressForComparison(boolean ignoreProtocol, boolean ignoreSubdomain, boolean ignoreQuery, boolean ignoreFragment) {
		// --- STRATEGY 1: It's a Web URL ---
		if (type.isWebUrl() && webUrl != null) {
			StringBuilder sb = new StringBuilder();

			if (!ignoreProtocol && webUrl.hasProtocol()) {
				sb.append(webUrl.getProtocolNotation().toLowerCase());
			}

			// Use the new, clearly documented and semantically sound hasHost() method
			if (webUrl.hasHost()) {
				if (!ignoreSubdomain) {
					// Use the full host (e.g., "www.example.com")
					sb.append(webUrl.getHost().toLowerCase());
				} else {
					// Use only the main domain (e.g., "example.com")
					sb.append(webUrl.getMainDomain().toLowerCase());
				}
			}

			if (webUrl.hasPath()) {
				sb.append(webUrl.getPath()); // Path is often case-sensitive
			}
			if (!ignoreQuery && webUrl.hasQuery()) {
				sb.append("?").append(webUrl.getQuery());
			}
			if (!ignoreFragment && webUrl.hasFragment()) {
				sb.append("#").append(webUrl.getFragment());
			}
			return sb.toString();
		}

		// --- STRATEGY 2: It's any kind of File-Based Path ---
		if (type.isFileBased() && filePath != null) {
			String path = filePath.getPathWithoutProtocol();
			if (path != null) {
				// Normalize separators and case for robust comparison
				return path.replace('\\', '/').toLowerCase();
			}
			return ""; // Return empty string for null paths
		}

		// --- STRATEGY 3: It's an Email ---
		if (type.isEmail()) {
			return getAddressForDisplay().toLowerCase();
		}

		// --- STRATEGY 4: All other types (IP, Hostname, etc.) ---
		// Use the exact display address as the canonical form.
		return getAddressForDisplay();
	}

	public String getSuggestedTitle() {
		return switch (type) {
		case URL_ADDRESS, FTP_ADDRESS, SSH_ADDRESS         -> (webUrl != null) ? webUrl.getHost() : getAddressWithoutProtocol();
		case LOCAL_PATH, UNC_PATH, FILE_URL, RELATIVE_PATH -> (filePath != null) ? filePath.getFullName() : getAddressWithoutProtocol();
		case IP_ADDRESS, HOSTNAME, EMAIL                   -> getAddressWithoutProtocol();
		default                                            -> getAddressForDisplay();
		};
	}

	// =================================================================================
	// Private Validation Helpers (static, pure functions)
	// =================================================================================

	private static boolean isValidLocalPath(FilePath filePath) {
		if (filePath == null)
			return false;
		return StringUtils.isNotBlank(filePath.getDrive());
	}

	private static boolean isRelativePath(FilePath filePath) {
		if (filePath == null)
			return false;
		String path = filePath.getFullPath();
		return StringUtils.isBlank(filePath.getDrive()) && (path.startsWith("./") || path.startsWith("../") || path.startsWith(".\\") || path.startsWith("..\\"));
	}

	// =================================================================================
	// Boolean Checks
	// =================================================================================

	/**
	 * Checks if the Pathment was created from a malformed file URI string.
	 */
	public boolean isMalformed() {
		return this.protocol == TransferProtocol.MALFORMED_FILE_URL;
	}

	/**
	 * Checks if this Pathment represents a resource whose status can be actively verified (e.g., a web link or a file path).
	 * 
	 * @return true if the status can be checked.
	 */
	public boolean isVerifiable() {
		// Delegates the logic to the Type enum.
		return this.type != null && this.type.isVerifiable();
	}

	/**
	 * Checks if this Pathment has a meaningful protocol notation (e.g., "https://", "mailto:") that would be prepended to its address for display or use.
	 */
	public boolean hasProtocolNotation() {
		return this.protocol != null && this.protocol.hasNotation();
	}

	// Add these methods to your Pathment.java class

	// =================================================================================
	// SORTING LOGIC
	// =================================================================================

	/**
	 * Creates a default comparator for sorting Pathment objects. Sorting is done first by a logical type priority, then alphabetically.
	 *
	 * @return A new Comparator for Pathment objects.
	 */
	public static java.util.Comparator<Pathment> createDefaultComparator() {
		return (p1, p2) -> {
			// Primary sort: by type priority
			int priorityCompare = Integer.compare(getSortPriority(p1), getSortPriority(p2));
			if (priorityCompare != 0) {
				return priorityCompare;
			}

			// Secondary sort: alphabetically by the address display string
			return p1.getAddressForDisplay().compareToIgnoreCase(p2.getAddressForDisplay());
		};
	}

	/**
	 * Helper method to assign a numerical priority to each Pathment type for sorting. Lower numbers have higher priority (will appear first in a sorted list).
	 *
	 * @param pathment The Pathment to evaluate.
	 * @return An integer representing the sort priority.
	 */
	private static int getSortPriority(Pathment pathment) {
		if (pathment == null || pathment.getType() == null) {
			return 999; // Should always be last
		}

		return switch (pathment.getType()) {
		// Highest priority: Web URLs and direct network access protocols
		case URL_ADDRESS, FTP_ADDRESS, SSH_ADDRESS, TELNET_ADDRESS -> 10; // <-- TELNET_ADDRESS hinzugefügt

		// Next: Communication
		case EMAIL -> 20;

		// Next: File system paths (grouped together)
		case FILE_URL, LOCAL_PATH, UNC_PATH, RELATIVE_PATH -> 30;

		// Next: Network identifiers
		case IP_ADDRESS, HOSTNAME -> 40;

		// Lower priority for commands and prompts
		case EXEC_COMMAND, PROMPT -> 50;

		// Unspecified should always be last among the defined types
		case UNSPECIFIED -> 100;
		};
	}

	// =================================================================================
	// EQUALS and HASHCODE IMPLEMENTATION
	// =================================================================================

	// Die Methode getCanonicalAddress() wird ENTFERNT. Sie ist irreführend und
	// ihre Logik wird nun korrekt und explizit in equals() und hashCode() abgebildet.

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Pathment that = (Pathment) o;

		// Two Pathments are equal if their fundamental semantic components are equal.
		// The protocol is a primary part of the identity.
		// The address comparison is case-insensitive for robustness.
		return type == that.type && protocol == that.protocol && Objects.equals(addressWithoutProtocol.toLowerCase(), that.addressWithoutProtocol.toLowerCase());
	}

	@Override
	public int hashCode() {
		// The hash code MUST be based on the same fields used in the equals() method.
		return Objects.hash(type, protocol, addressWithoutProtocol.toLowerCase());
	}

}