package utils.detection;



import java.net.URI;
import java.net.URISyntaxException;

import utils.general.StringUtils;
import utils.logging.Log;
import utils.network.TransferProtocol;

/**
 * Represents the components of a web URL (Uniform Resource Locator). This class stores parts of a URL like protocol, subdomain, domain, path, query, fragment, and port. It
 * provides methods to reconstruct the URL in different formats:
 * <ul>
 * <li>A URI-compliant, percent-encoded string suitable for network requests ({@link #toUriString()}).</li>
 * <li>A human-readable, decoded string suitable for display in a UI ({@link #getDisplayUrl()}).</li>
 * </ul>
 */
public class WebUrl {

	private TransferProtocol protocol;
	private final String     subDomain;  // e.g., "www"
	private final String     mainDomain; // e.g., "example.com" (SLD + TLD)
	private final int        port;       // e.g., 8080, -1 if not specified
	private final String     path;       // e.g., "/folder/page.html" (should be unencoded here)
	private final String     query;      // e.g., "param=value&key=val" (should be unencoded here)
	private final String     fragment;   // e.g., "section1" (should be unencoded here)

	/**
	 * Constructs a new {@code WebUrl} instance with the given components. The path, query, and fragment components are expected to be in their raw, unencoded form (e.g.,
	 * containing spaces or special characters as they are, not percent-encoded). The port is optional and defaults to -1 if not specified.
	 *
	 * @param protocol  The transfer protocol (e.g., HTTP, HTTPS).
	 * @param subdomain The subdomain part (e.g., "www", can be null or empty).
	 * @param domain    The main domain name including TLD (e.g., "example.com").
	 * @param port      The port number (e.g., 8080), or -1 if not specified.
	 * @param path      The path component (e.g., "/path/to/resource", can be null or empty).
	 * @param query     The query string, without the leading '?' (e.g., "name=value", can be null or empty).
	 * @param fragment  The fragment identifier, without the leading '#' (e.g., "section", can be null or empty).
	 */
	public WebUrl(TransferProtocol protocol, String subdomain, String domain, int port, String path, String query, String fragment) {
		this.protocol = protocol;
		this.subDomain = StringUtils.trimToNull(subdomain); // Store null if blank for cleaner URI construction
		this.mainDomain = StringUtils.trimToNull(domain);
		this.port = (port >= 0 && port <= 65535) ? port : -1; // Validate port range, default to -1 if invalid
		this.path = StringUtils.trimToNull(path);
		this.query = StringUtils.trimToNull(query);
		this.fragment = StringUtils.trimToNull(fragment);
	}

	// --- Component Getters ---

	/**
	 * Returns the transfer protocol.
	 * 
	 * @return The protocol, or null if not set.
	 */
	public TransferProtocol getTransferProtocol() {
		return protocol;
	}

	/**
	 * Returns the protocol notation (e.g., "http://").
	 * 
	 * @return The protocol string, or an empty string if no protocol.
	 */
	public String getProtocolNotation() {
		return (protocol != null && protocol.hasNotation()) ? protocol.getNotation() : "";
	}

	/**
	 * Sets or updates the protocol for this WebUrl. Only web-related protocols (HTTP, HTTPS) are considered valid.
	 * 
	 * @param prot The new {@link TransferProtocol}.
	 */
	public void setProtocol(TransferProtocol prot) {
		if (prot == null || !prot.isWeb()) {
			Log.warn("Attempted to set a non-web protocol or null to WebUrl: %s. Protocol not changed.", prot);
			return;
		}
		this.protocol = prot;
	}

	/**
	 * Returns the subdomain (e.g., "www") or an empty string if not set.
	 * 
	 * @return The subdomain or an empty string.
	 */
	public String getSubDomain() {
		return subDomain != null ? subDomain : "";
	}

	/**
	 * Returns the main domain part, including the second-level domain (SLD) and top-level domain (TLD). Example: "example.com", "example.co.uk".
	 * 
	 * @return The domain string, or an empty string if not set.
	 */
	public String getMainDomain() {
		return mainDomain != null ? mainDomain : "";
	}

	/**
	 * Returns the port number, or -1 if not specified.
	 * 
	 * @return The port number or -1.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Returns the raw (potentially unencoded) path component. Example: "/search results/page.html"
	 * 
	 * @return The path string, or an empty string if not set.
	 */
	public String getPath() {
		return path != null ? path : "";
	}

	/**
	 * Returns the raw (potentially unencoded) query component, without the leading '?'. Example: "q=search term&lang=en"
	 * 
	 * @return The query string, or an empty string if not set.
	 */
	public String getQuery() {
		return query != null ? query : "";
	}

	/**
	 * Returns the raw (potentially unencoded) fragment component, without the leading '#'. Example: "section-name"
	 * 
	 * @return The fragment string, or an empty string if not set.
	 */
	public String getFragment() {
		return fragment != null ? fragment : "";
	}

	// --- Boolean Checks ---

	public boolean hasProtocol() {
		return this.protocol != null && this.protocol.hasNotation() && this.protocol != TransferProtocol.NONE;
	}

	public boolean hasSubDomain() {
		return StringUtils.isNotBlank(subDomain);
	}

	/**
	 * Checks if a host is present.
	 * <p>
	 * According to the structure of this class, a host is constructed from an optional subdomain and a mandatory main domain (e.g., "example.com"). Therefore, a host can only
	 * exist if the main domain component is present. This method serves as the semantically correct counterpart to {@link #getHost()}.
	 *
	 * @return {@code true} if the main domain component is not null or blank, {@code false} otherwise.
	 */
	public boolean hasHost() {
		return StringUtils.isNotBlank(mainDomain);
	}

	public boolean hasPort() {
		return port != -1;
	}

	public boolean hasPath() {
		return StringUtils.isNotBlank(path);
	}

	public boolean hasQuery() {
		return StringUtils.isNotBlank(query);
	}

	public boolean hasFragment() {
		return StringUtils.isNotBlank(fragment);
	}

	/**
	 * Checks if the path component heuristically looks like a directory path rather than a file path.
	 * <p>
	 * This is a **heuristic** and not a guarantee. It assumes that a path points to a directory if its final segment does not contain a dot ('.'). This can be useful for
	 * client-side logic, such as deciding whether to retry a request with a trailing slash upon receiving a 404 error.
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>"/galleries/funny-boats" -> true</li>
	 * <li>"/assets/style.css" -> false</li>
	 * <li>"/users/profile" -> true (Note: This may be a "clean URL" file, highlighting the heuristic nature)</li>
	 * <li>"/" -> true</li>
	 * </ul>
	 *
	 * @return {@code true} if the path is empty, ends with a slash, or its last segment lacks a dot; {@code false} otherwise.
	 */
	public boolean pathLooksLikeDirectory() {
		String currentPath = getPath(); // Assuming getPath() returns "" if no path exists

		if (currentPath.isEmpty() || currentPath.equals("/")) {
			return true; // Root or empty path is always a directory
		}
		if (currentPath.endsWith("/")) {
			return true; // Explicitly a directory
		}

		// Get the last segment of the path
		int lastSlashIndex = currentPath.lastIndexOf('/');
		String lastSegment = currentPath.substring(lastSlashIndex + 1);

		// If the last segment is empty or does not contain a dot, we assume it's a directory.
		return !lastSegment.contains(".");
	}

	// --- URL String Representations ---

	/**
	 * Assembles and returns a URI-compliant, percent-encoded string representation of this WebUrl. This string is suitable for creating a {@link java.net.URI} object or for use in
	 * network requests. The protocol and port are included if present.
	 *
	 * @return The fully assembled and percent-encoded URI string.
	 * @throws IllegalStateException if the WebUrl components cannot be assembled into a valid URI (wrapping URISyntaxException).
	 */
	public String toUriString() {
		try {
			return assembleUriString(true);
		} catch (URISyntaxException e) {
			String message = String.format("Failed to construct valid URI from WebUrl components. Proto: '%s', Sub: '%s', Dom: '%s', Port: %d, Path: '%s', Query: '%s', Frag: '%s'",
			        protocol, subDomain, mainDomain, port, path, query, fragment);
			Log.error(e, message);
			throw new IllegalStateException(message, e);
		}
	}

	/**
	 * Assembles and returns a URI-compliant, percent-encoded string representation of this WebUrl, excluding the protocol part.
	 *
	 * @return The assembled and percent-encoded URI string without the protocol.
	 * @throws IllegalStateException if the WebUrl components cannot be assembled into a valid URI.
	 */
	public String toUriStringWithoutProtocol() {
		try {
			return assembleUriString(false);
		} catch (URISyntaxException e) {
			String message = String.format(
			        "Failed to construct valid URI (without protocol) from WebUrl components. Sub: '%s', Dom: '%s', Port: %d, Path: '%s', Query: '%s', Frag: '%s'", subDomain,
			        mainDomain, port, path, query, fragment);
			Log.error(e, message);
			throw new IllegalStateException(message, e);
		}
	}

	/**
	 * Returns a human-readable, decoded string representation of the URL, suitable for display in a UI. Includes the protocol and port if present. Special characters (like
	 * umlauts, spaces in path/query/fragment) are shown in their decoded form.
	 *
	 * @return The human-readable URL string with protocol.
	 */
	public String getDisplayUrl() {
		return assembleDisplayUrl(true);
	}

	/**
	 * Returns a human-readable, decoded string representation of the URL, suitable for display in a UI, excluding the protocol.
	 *
	 * @return The human-readable URL string without protocol.
	 */
	public String getDisplayUrlWithoutProtocol() {
		return assembleDisplayUrl(false);
	}

	// --- Private Helper Methods ---

	/**
	 * Core logic to assemble a URI-compliant, percent-encoded string. Uses java.net.URI to handle correct encoding of components.
	 */
	private String assembleUriString(boolean includeProtocol) throws URISyntaxException {
		String schemeComponent = null;
		if (includeProtocol && this.protocol != null && this.protocol.isWeb() && this.protocol != TransferProtocol.NONE) {
			schemeComponent = this.protocol.name().toLowerCase();
		}

		String hostComponent = getHost(); // Assembled from subdomain and domain

		// A scheme requires a host for typical web URLs (http, https)
		if (schemeComponent != null && StringUtils.isBlank(hostComponent)) {
			throw new URISyntaxException(toString(), "Host component is mandatory when a scheme ('" + schemeComponent + "') is present.");
		}

		// Path component for URI constructor:
		// - Must start with "/" if host is present and path is not empty.
		// - Can be null or empty string if no path.
//		String pathComponent = null;
//		if (StringUtils.isNotBlank(this.path)) {
//			pathComponent = this.path.startsWith("/") ? this.path : "/" + this.path;
//		} else if (StringUtils.isNotBlank(hostComponent) || schemeComponent != null) {
//			pathComponent = "";
//		}
		// =========================== KORREKTUR START ===========================
		// This is the crucial fix. We distinguish between an empty path ("/") and an absent path (null).
		String pathComponent;
		if (this.path != null) {
			// If a path string exists (even if empty), ensure it starts with a slash.
			// An empty string "" becomes "/". A string "/foo" remains "/foo".
			pathComponent = this.path.startsWith("/") ? this.path : "/" + this.path;
		} else {
			// If the path field is null, the path component for the URI constructor must also be null.
			pathComponent = null;
		}
		// ============================ KORREKTUR ENDE ============================

		// Query and Fragment components are passed without '?' or '#'
		String queryComponent = this.query;
		String fragmentComponent = this.fragment;

		URI uri = new URI(schemeComponent, null, // userInfo
		        StringUtils.isBlank(hostComponent) ? null : hostComponent, // host can be null if scheme is null
		        hasPort() ? port : -1, // Use port if present, otherwise -1
		        pathComponent, queryComponent, fragmentComponent);
		return uri.toASCIIString(); // Returns the fully qualified, percent-encoded URI string
	}

	/**
	 * www.google.com Core logic to assemble a human-readable display URL string. This version correctly includes the path, query, fragment, and port if present.
	 */
	private String assembleDisplayUrl(boolean includeProtocol) {
		StringBuilder sb = new StringBuilder();
		if (includeProtocol && hasProtocol()) {
			sb.append(getProtocolNotation());
		}

		String hostDisplay = getHost();
		if (StringUtils.isNotBlank(hostDisplay)) {
			sb.append(hostDisplay);
		}

		if (hasPort()) {
			sb.append(":").append(port);
		}

		if (hasPath()) {
			if (StringUtils.isNotBlank(hostDisplay) && !this.path.startsWith("/")) {
				sb.append("/");
			}
			sb.append(this.path);
		}

		if (hasQuery()) {
			sb.append("?").append(this.query);
		}
		if (hasFragment()) {
			sb.append("#").append(this.fragment);
		}

		return sb.toString();
	}

	// --- Other Utility Methods ---

	/**
	 * Returns the full host (e.g., "www.example.com"). It concatenates subdomain (if present) and main domain.
	 * 
	 * @return The full host string, or an empty string if the domain is not set.
	 * 
	 */
	public String getHost() {
		if (!hasHost()) {
			return "";
		}
		if (hasSubDomain()) {
			return subDomain.endsWith(".") ? subDomain + mainDomain : subDomain + "." + mainDomain;
		}
		return mainDomain;
	}

	/**
	 * Extracts the first part of the main domain. NOTE: This is a simple heuristic. For a domain like "bbc.co.uk", it will return "co", not "bbc". For "google.com", it correctly
	 * returns "google".
	 *
	 * @return The first label of the main domain, or null if not applicable.
	 */
	public String domainName() {
		if (!hasHost()) {
			return null;
		}

		// Simple, predictable logic: Split by dot and return the first part.
		String[] domainParts = this.mainDomain.split("\\.");

		if (domainParts.length >= 1) {
			return domainParts[0];
		}

		return null; // Should not be reached if hasHost() is true, but safe.
	}

	/**
	 * Retrieves the top-level domain (TLD) from the domain property. Example: for "example.com", returns "com". For "example.co.uk", returns "uk".
	 * 
	 * @return The TLD, or null if not determinable.
	 */
	public String getTopLevelDomain() {
		if (hasHost()) {
			int lastDot = this.mainDomain.lastIndexOf('.');
			if (lastDot > 0 && lastDot < this.mainDomain.length() - 1) {
				return this.mainDomain.substring(lastDot + 1);
			}
		}
		return null;
	}

	/**
	 * Checks if the host is structurally valid (e.g., contains at least one dot for a domain structure). This is a helper method for debugging or logging, not for validation.
	 *
	 * @return true if the host has a dot, false otherwise.
	 */
	public boolean isHostStructurallyValid() {
		String host = getHost();
		return StringUtils.isNotBlank(host) && host.contains(".");
	}

	@Override
	public String toString() {
		return String.format("WebUrl[protocol=%s, subdomain='%s', domain='%s', port=%d, path='%s', query='%s', fragment='%s']", protocol, subDomain, mainDomain, port, path, query,
		        fragment);
	}
}