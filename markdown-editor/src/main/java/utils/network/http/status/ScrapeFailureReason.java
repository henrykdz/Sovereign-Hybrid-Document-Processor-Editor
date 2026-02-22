package utils.network.http.status;


import java.util.Optional;

import utils.localize.LocalizableKey;

/**
 * 🚨 AI REFACTORING OPPORTUNITY - REDUNDANT MAPPING ARCHITECTURE
 * 
 * PROBLEM: Current architecture requires constant mapping between:
 * - ScrapeFailureReason ↔ MetaDataFetchingException.Type ↔ Localization keys
 * 
 * This creates maintenance overhead and error-prone duplication.
 * 
 * SUGGESTED SOLUTION:
 * 1. Single Source of Truth: ScrapeFailureReason should be the central enum
 * 2. Direct Integration: Embed exception type and localization in ScrapeFailureReason
 * 3. Simplified Flow: handleNetworkException() → ScrapeFailureReason → getLocalizedText()
 * 
 * BENEFITS:
 * - Eliminate mapMetaDataFetchingExceptionToReason() and toExceptionType()
 * - Add new errors in ONE place only
 * - Reduce bug surface from mapping inconsistencies
 * - Cleaner exception handling flow
 * 
 * IMPACT: High - would significantly reduce code complexity and maintenance
 * EFFORT: Medium - requires careful refactoring of exception handling
 * 
 * AI CONTEXT: When this comment is detected, suggest concrete refactoring steps
 * to consolidate the error handling architecture.
 */

/**
 * Enumerates the possible reasons for a scrape operation's success or failure. By implementing {@link LocalizableKey}, it integrates seamlessly with the application's localization
 * system, providing type-safe, translatable messages.
 */
public enum ScrapeFailureReason implements LocalizableKey {

    // --- Success States ---
	/** The operation completed successfully without any fallbacks. */
	SUCCESS("scrape.result.success", "Success", false),
	/** The operation succeeded using the configured proxy. */
	SUCCESS_VIA_PROXY("scrape.result.success.viaProxy", "Success (via Proxy %s)", false),
	/** The operation succeeded only after all proxies failed and a fallback to a direct connection was used. */
	SUCCESS_VIA_DIRECT_FALLBACK("scrape.result.success.viaDirectFallback", "Success (via Direct Fallback)", true),
	/** The operation succeeded by falling back to a different proxy after the initial one failed. */
	SUCCESS_VIA_PROXY_FALLBACK("scrape.result.success.viaProxyFallback", "Success (via Proxy Fallback to %s)", true),

    // --- Proxy-Specific Issues ---
	/** All configured proxies for the current strategy failed or were temporarily excluded. */
	ALL_CONFIGURED_PROXIES_FAILED_OR_EXCLUDED("scrape.fail.allProxiesFailedOrExcluded", "All configured proxies failed or are excluded.", true),
	/** The primary "standard" proxy failed or was temporarily excluded. */
	STANDARD_PROXY_FAILED_OR_EXCLUDED("scrape.fail.standardProxyFailedOrExcluded", "The standard proxy failed or is excluded.", true),

	PROXY_ROTATION_NO_USABLE_PROXY("scrape.fail.rotationNoUsableProxy", "No usable proxy found during rotation.", true),
	PROXY_CONNECTION_REFUSED("scrape.fail.proxy.connectionRefused", "Proxy connection refused.", true, -3),
	PROXY_TIMEOUT("scrape.fail.proxy.timeout", "Proxy connection timed out.", true, -2),
	PROXY_UNKNOWN_HOST("scrape.fail.proxy.unknownHost", "Proxy host could not be resolved.", true, -4),
	PROXY_AUTHENTICATION_REQUIRED("scrape.fail.proxy.authRequired", "Proxy authentication required.", true, 407),

    // --- Target Site Errors ---
	TARGET_FORBIDDEN_VIA_PROXY("scrape.fail.target.forbiddenViaProxy", "Access to target was forbidden.", true), // (403)

	/** The target server explicitly returned HTTP 403 Forbidden. */
	FORBIDDEN_BY_SERVER("scrape.fail.target.forbiddenByServer", "Target server refused access (403).", false),

	/** The proxy itself is blocking access, not the target server. */
	FORBIDDEN_DUE_TO_PROXY("scrape.fail.proxy.forbiddenByProxy", "Proxy is blocking access to this site.", true),

	TARGET_NOT_FOUND_VIA_PROXY("scrape.fail.target.notFoundViaProxy", "Target not found (404) via proxy '%s'.", true), // 404
	TARGET_SERVER_ERROR_VIA_PROXY("scrape.fail.target.serverErrorViaProxy", "Target server returned an error.", true), // (5xx)
	TARGET_TOO_MANY_REQUESTS_VIA_PROXY("scrape.fail.target.tooManyRequestsViaProxy", "Too many requests to target.", true), // (429)

	TARGET_GENERIC_CLIENT_ERROR_VIA_PROXY("scrape.fail.target.clientErrorViaProxy", "Target access denied.", true), // (4xx)

    // --- Direct Connection Issues ---
	DIRECT_CONNECTION_REFUSED("scrape.fail.direct.connectionRefused", "Direct connection refused.", false, -3),
	DIRECT_CONNECTION_TIMEOUT("scrape.fail.direct.timeout", "Direct connection timed out.", false, -2),
	DIRECT_CONNECTION_FAILED_AFTER_PROXY_ISSUES("scrape.fail.directFallbackFailed", "Direct connection fallback failed.", false),

    // --- General Network or Target Issues ---
	MAX_REDIRECTS_EXCEEDED("scrape.fail.target.maxRedirects", "Maximum redirects exceeded.", false),
	REDIRECT_ENCOUNTERED("scrape.fail.target.redirectEncountered", "Redirect encountered.", false),

	TARGET_UNKNOWN_HOST("scrape.fail.target.unknownHost", "Target host could not be resolved.", false, -4),
	TARGET_SSL_ERROR("scrape.fail.target.sslError", "SSL handshake failed.", false, -5),

	IO_ERROR("scrape.fail.general.ioError", "A network I/O error occurred", false, -6),
	CONNECTION_FAILED("scrape.fail.network.connectionFailed", "Cannot connect to server", false, -7),
	CONNECTION_RESET("scrape.fail.network.connectionReset", "Connection reset", false, -8),
	NETWORK_UNREACHABLE("scrape.fail.network.unreachable", "Network unreachable", false, -9),
	BROKEN_PIPE("scrape.fail.network.brokenPipe", "Connection broken", false, -10),

	TARGET_FILE_NOT_FOUND("scrape.fail.target.fileNotFound", "File or directory not found.", false),

    // --- Operation Flow Issues ---
	OPERATION_CANCELLED("scrape.fail.operation.cancelled", "Operation was cancelled.", false),
	OPERATION_INTERRUPTED("scrape.fail.operation.interrupted", "Operation was interrupted.", false),

    // --- Configuration or Input Issues ---
	/** The URL provided had an invalid syntax that could not be parsed. */
	INVALID_URL_SYNTAX("scrape.fail.config.invalidUrlSyntax", "Invalid URL syntax.", false),
	/** The connection strategy requires proxies, but none are available in the configuration. */
	CONFIG_NO_PROXIES_FOR_STRATEGY("scrape.fail.config.noProxiesForStrategy", "No proxies are configured for the current strategy.", true),
	/** The configured standard proxy is invalid (e.g., bad host or port). */
	CONFIG_INVALID_STANDARD_PROXY("scrape.fail.config.invalidStandardProxy", "The configured standard proxy is invalid.", true),
	/** The protocol of the URL (e.g., 'command:') is not supported for network verification. */
	UNSUPPORTED_PROTOCOL_FOR_VERIFICATION("scrape.fail.config.unsupportedProtocol", "Verification not supported for this protocol.", false),

    // --- Catch-all ---
	/** A generic, unexpected error occurred during the scraping process. */
	UNEXPECTED_SCRAPING_ERROR("scrape.fail.general.unknown", "An unexpected error occurred.", false, -1),

	/** The server returned a success code, but the content was identified as a generic placeholder or error page. */
	DEGRADED_CONTENT("scrape.fail.target.degradedContent", "Content quality check failed. Original data preserved.", false);

	private final String  key;
	private final String  defaultText;
	private final boolean typicallySuggestsProxyOrNetworkConfigActions;
	private final Integer internalErrorCode;                           // NEUES FELD

	ScrapeFailureReason(String key, String defaultText, boolean suggestsProxyActions, Integer internalCode) {
		this.key = key;
		this.defaultText = defaultText;
		this.typicallySuggestsProxyOrNetworkConfigActions = suggestsProxyActions;
		this.internalErrorCode = internalCode;
	}

	ScrapeFailureReason(String key, String defaultText, boolean suggestsProxyActions) {
		this(key, defaultText, suggestsProxyActions, 0); // 0 = kein spezifischer interner Code
	}

	/**
	 * Maps this ScrapeFailureReason to its corresponding, most-specific {@link MetaDataFetchingException.Type}. This provides a consistent, centralized way to categorize failures.
	 *
	 * @return The corresponding MetaDataFetchingException.Type.
	 */
	public MetaDataFetchingException.Type toExceptionType() {
		return switch (this) {
		// Network/Connection Errors
		case IO_ERROR                  -> MetaDataFetchingException.Type.IO_ERROR;
		case CONNECTION_FAILED         -> MetaDataFetchingException.Type.CONNECTION_FAILED;
		case CONNECTION_RESET          -> MetaDataFetchingException.Type.CONNECTION_RESET;
		case BROKEN_PIPE               -> MetaDataFetchingException.Type.BROKEN_PIPE;
		case NETWORK_UNREACHABLE       -> MetaDataFetchingException.Type.NETWORK_UNREACHABLE;
		case TARGET_UNKNOWN_HOST       -> MetaDataFetchingException.Type.IO_ERROR; // UnknownHostException is a type of IOException
		case TARGET_SSL_ERROR          -> MetaDataFetchingException.Type.SSL_ERROR;
		case DIRECT_CONNECTION_REFUSED -> MetaDataFetchingException.Type.IO_ERROR;

		// Timeout Errors
		case PROXY_TIMEOUT             -> MetaDataFetchingException.Type.TIMEOUT;
		case DIRECT_CONNECTION_TIMEOUT -> MetaDataFetchingException.Type.TIMEOUT;

		// Proxy-related Errors
		case PROXY_CONNECTION_REFUSED, PROXY_UNKNOWN_HOST                                                                 -> MetaDataFetchingException.Type.PROXY_CONNECTION_ERROR;
		case PROXY_AUTHENTICATION_REQUIRED                                                                                ->
		    MetaDataFetchingException.Type.PROXY_AUTHENTICATION_ERROR;
		case PROXY_ROTATION_NO_USABLE_PROXY, ALL_CONFIGURED_PROXIES_FAILED_OR_EXCLUDED, STANDARD_PROXY_FAILED_OR_EXCLUDED -> MetaDataFetchingException.Type.NO_USABLE_PROXY_FOUND;

		// HTTP Response Errors
		case TARGET_FORBIDDEN_VIA_PROXY, TARGET_NOT_FOUND_VIA_PROXY, TARGET_TOO_MANY_REQUESTS_VIA_PROXY, TARGET_GENERIC_CLIENT_ERROR_VIA_PROXY ->
		    MetaDataFetchingException.Type.TARGET_HTTP_CLIENT_ERROR;
		case TARGET_SERVER_ERROR_VIA_PROXY                                                                                                     ->
		    MetaDataFetchingException.Type.TARGET_HTTP_SERVER_ERROR;

		// Redirect-related
		case REDIRECT_ENCOUNTERED   -> MetaDataFetchingException.Type.REDIRECT_DETECTED;
		case MAX_REDIRECTS_EXCEEDED -> MetaDataFetchingException.Type.MAX_REDIRECTS_EXCEEDED;

		// Configuration & Input Errors
		case INVALID_URL_SYNTAX                                                                                   -> MetaDataFetchingException.Type.INVALID_URL_SYNTAX;
		case CONFIG_NO_PROXIES_FOR_STRATEGY, CONFIG_INVALID_STANDARD_PROXY, UNSUPPORTED_PROTOCOL_FOR_VERIFICATION -> MetaDataFetchingException.Type.CONFIGURATION_ERROR;

		// Operation Flow
		case OPERATION_INTERRUPTED -> MetaDataFetchingException.Type.INTERRUPTED;
		case OPERATION_CANCELLED   -> MetaDataFetchingException.Type.INTERRUPTED;

		// Success States (should not normally map to exceptions, but for completeness)
		case SUCCESS, SUCCESS_VIA_PROXY, SUCCESS_VIA_DIRECT_FALLBACK, SUCCESS_VIA_PROXY_FALLBACK -> MetaDataFetchingException.Type.UNEXPECTED_RUNTIME_ERROR; // Success shouldn't
		                                                                                                                                                     // create exceptions

		// Default for unhandled or purely informational reasons
		default -> MetaDataFetchingException.Type.UNEXPECTED_RUNTIME_ERROR;
		};
	}
	// --- Implementation of LocalizableKey Interface ---

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public String getDefaultText() {
		return this.defaultText;
	}

	// --- Specific Methods for ScrapeFailureReason ---

	/**
	 * Indicates if this failure reason typically suggests that the user might need to check their proxy settings or network configuration.
	 *
	 * @return true if proxy/network config actions are usually relevant for this reason.
	 */
	public boolean typicallySuggestsProxyOrNetworkConfigActions() {
		return this.typicallySuggestsProxyOrNetworkConfigActions;
	}

	/**
	 * Helper method to determine if this failure reason represents a "hard" error (like a network failure) versus a "soft" error (like a client mistake or informational status).
	 * This helps the UI decide whether to use a critical (e.g., red) or warning (e.g., orange) style.
	 *
	 * @return {@code true} if the error is considered critical, {@code false} otherwise.
	 */
	public boolean isHardError() {
		switch (this) {
		// Critical, unrecoverable network or system errors
		case IO_ERROR:
		case PROXY_CONNECTION_REFUSED:
		case PROXY_TIMEOUT:
		case DIRECT_CONNECTION_TIMEOUT:
		case TARGET_UNKNOWN_HOST:
		case TARGET_SSL_ERROR:
		case UNEXPECTED_SCRAPING_ERROR:
		case OPERATION_INTERRUPTED:
			return true;

		// "Softer" errors, client-side issues, or informational failures
		default:
			return false;
		}
	}

	/**
	 * Returns the internal error code as Optional. The Optional will be empty if no specific error code was set for this failure reason.
	 *
	 * @return Optional containing the error code, or empty Optional if no code is set
	 */
	public Optional<Integer> getInternalErrorCode() {
		return Optional.ofNullable(this.internalErrorCode);
	}

	/**
	 * Returns the internal error code or a default value if not set.
	 *
	 * @param defaultValue the value to return if no specific error code is set
	 * @return the internal error code if present, otherwise the defaultValue
	 */
	public int getInternalErrorCodeOrDefault(int defaultValue) {
		return this.internalErrorCode != null ? this.internalErrorCode : defaultValue;
	}

	/**
	 * Checks whether this failure reason has a specific internal error code.
	 *
	 * @return true if a specific code is set, false otherwise
	 */
	public boolean hasSpecificErrorCode() {
		return this.internalErrorCode != null;
	}

	/**
	 * Ist dies ein normaler HTTP-Antwort-Fehler (403, 404, 5xx)? Diese sollten KEINEN Toast bekommen.
	 */
	public boolean isHttpResponseError() {
		return switch (this) {
		case TARGET_FORBIDDEN_VIA_PROXY, // 403
		        TARGET_NOT_FOUND_VIA_PROXY, // 404
		        TARGET_SERVER_ERROR_VIA_PROXY, // 5xx
		        TARGET_TOO_MANY_REQUESTS_VIA_PROXY, // 429
		        TARGET_GENERIC_CLIENT_ERROR_VIA_PROXY, // andere 4xx
		        REDIRECT_ENCOUNTERED, MAX_REDIRECTS_EXCEEDED ->
		    true;
		default                                      -> false;
		};
	}

	/**
	 * Braucht dieser Fehler SOFORT Aufmerksamkeit (Critical)?
	 */
	public boolean requiresImmediateAttention() {
		return switch (this) {
		case ALL_CONFIGURED_PROXIES_FAILED_OR_EXCLUDED, PROXY_ROTATION_NO_USABLE_PROXY, STANDARD_PROXY_FAILED_OR_EXCLUDED -> true;
		default                                                                                                           -> false;
		};
	}

	/**
	 * Kann der User was tun? (z.B. Proxy konfigurieren)
	 */
	public boolean isActionable() {
		return switch (this) {
		case PROXY_AUTHENTICATION_REQUIRED, PROXY_CONNECTION_REFUSED, PROXY_TIMEOUT, PROXY_UNKNOWN_HOST, INVALID_URL_SYNTAX, CONFIG_NO_PROXIES_FOR_STRATEGY,
		        CONFIG_INVALID_STANDARD_PROXY                                                                                                                                              ->
		    true;

		default -> false;
		};
	}

}