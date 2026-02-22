package utils.network.http.status;


import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import utils.network.proxy.ProxyConfig;

/**
 * Exception thrown when fetching metadata from a URL fails. It can encapsulate an HTTP status code, a specific error type, the proxy that was being used, and, in some cases, a
 * redirect URL provided within a non-2xx response.
 */
public class MetaDataFetchingException extends Exception {

	private static final long serialVersionUID = 2L; // Increased version due to structural change

	public enum Type {
	    // Network/Connection Errors
	    IO_ERROR,
	    CONNECTION_FAILED,
	    CONNECTION_RESET, 
	    BROKEN_PIPE,
	    NETWORK_UNREACHABLE,
	    TIMEOUT,
	    SSL_ERROR,

	    // Proxy-related Errors
	    PROXY_CONNECTION_ERROR,
	    PROXY_AUTHENTICATION_ERROR,
	    PROXY_GENERIC_ERROR,
	    NO_USABLE_PROXY_FOUND,

	    // HTTP Response Errors
	    TARGET_HTTP_CLIENT_ERROR,
	    TARGET_HTTP_SERVER_ERROR,

	    // Redirect-related
	    /**
	     * Indicates that the server responded with a 3xx redirect status. The application is expected to handle this event as part of a successful session.
	     */
	    REDIRECT_DETECTED, // Formerly UNFOLLOWED_REDIRECT
	    /**
	     * Indicates a failure in the redirect protocol itself, e.g., a 3xx response without a 'Location' header or a malformed redirect URL. This is a session-terminating error.
	     */
	    REDIRECT_PROTOCOL_ERROR,
	    MAX_REDIRECTS_EXCEEDED,

	    // Configuration & Input Errors
	    INVALID_URL_SYNTAX,
	    CONFIGURATION_ERROR,

	    // Operation Flow
	    INTERRUPTED,

	    // Catch-all
	    UNEXPECTED_RUNTIME_ERROR
	}

	private final Type                errorType;
	private final ScrapeFailureReason reason;
	private final Integer             statusCode;
	private final ProxyConfig         attemptedProxy;
	private final String              redirectUrlFromError;

	// =================================================================================
	// Constructors (now private to enforce factory method usage)
	// =================================================================================

	private MetaDataFetchingException(String message, Type errorType, Throwable cause, ScrapeFailureReason reason, Integer statusCode, ProxyConfig attemptedProxy,
	        String redirectUrlFromError) {
		super(message, cause);
		this.errorType = Objects.requireNonNullElse(errorType, Type.UNEXPECTED_RUNTIME_ERROR);
		this.reason = Objects.requireNonNullElse(reason, ScrapeFailureReason.UNEXPECTED_SCRAPING_ERROR);
		this.statusCode = statusCode;
		this.attemptedProxy = attemptedProxy;
		this.redirectUrlFromError = redirectUrlFromError;
	}

	// =================================================================================
	// Static Factory Methods (The new, clean way to create exceptions)
	// =================================================================================
	public static MetaDataFetchingException of(String message, Type errorType) {
		return new MetaDataFetchingException(message, errorType, null, null, null, null, null);
	}

	public static MetaDataFetchingException withCause(String message, Type errorType, Throwable cause) {
		return new MetaDataFetchingException(message, errorType, cause, null, null, null, null);
	}

	public static MetaDataFetchingException withReason(String message, Type errorType, ScrapeFailureReason reason) {
		return new MetaDataFetchingException(message, errorType, null, reason, null, null, null);
	}

	public static MetaDataFetchingException withStatusCode(String message, Type errorType, int statusCode) {
		return new MetaDataFetchingException(message, errorType, null, null, statusCode, null, null);
	}

	public static MetaDataFetchingException withProxy(String message, Type errorType, ProxyConfig attemptedProxy) {
		return new MetaDataFetchingException(message, errorType, null, null, null, attemptedProxy, null);
	}

	public static MetaDataFetchingException withProxy(String message, Type errorType, Throwable cause, ProxyConfig attemptedProxy) {
		return new MetaDataFetchingException(message, errorType, cause, null, null, attemptedProxy, null);
	}

	public static MetaDataFetchingException withStatusCodeAndProxy(String message, Type errorType, int statusCode, ProxyConfig attemptedProxy) {
		return new MetaDataFetchingException(message, errorType, null, null, statusCode, attemptedProxy, null);
	}

	public static MetaDataFetchingException withRedirect(String message, Type errorType, int statusCode, ProxyConfig attemptedProxy, String redirectUrl) {
		return new MetaDataFetchingException(message, errorType, null, null, statusCode, attemptedProxy, redirectUrl);
	}

	public static MetaDataFetchingException wrap(String summaryMessage, MetaDataFetchingException cause, ProxyConfig lastAttemptedProxy) {
		return new MetaDataFetchingException(summaryMessage, cause.getErrorType(), cause, cause.getReason(), cause.getStatusCode().orElse(null),
		        lastAttemptedProxy != null ? lastAttemptedProxy : cause.getAttemptedProxy().orElse(null), cause.getRedirectUrlFromError().orElse(null));
	}

	/**
	 * The most comprehensive factory method, creating an exception with all possible details.
	 */
	public static MetaDataFetchingException withCauseAndDetails(String message, Type errorType, int statusCode, ProxyConfig attemptedProxy, Throwable cause) {
		// Leite an den privaten Konstruktor weiter. Der Reason wird hier nicht direkt gesetzt,
		// da er aus dem errorType abgeleitet werden kann.
		return new MetaDataFetchingException(message, errorType, cause, null, statusCode, attemptedProxy, null);
	}

	// --- Getters ---

	/**
	 * Returns the specific, detailed reason for the failure.
	 * 
	 * @return The {@link ScrapeFailureReason}.
	 */
	public ScrapeFailureReason getReason() {
		return reason;
	}

	public Type getErrorType() {
		return errorType;
	}

	public Optional<Integer> getStatusCode() {
		return Optional.ofNullable(statusCode);
	}

	public Optional<ProxyConfig> getAttemptedProxy() {
		return Optional.ofNullable(attemptedProxy);
	}

	/**
	 * Returns the redirect URL if one was provided in the 'Location' header of a non-2xx (error) response.
	 *
	 * @return An {@link Optional<String>} containing the redirect URL, or an empty Optional.
	 */
	public Optional<String> getRedirectUrlFromError() {
		return Optional.ofNullable(redirectUrlFromError);
	}

	@Override
	public String toString() {
		// StringJoiner(delimiter, prefix, suffix)
		StringJoiner details = new StringJoiner(", ", " [", "]");

		// Füge die immer vorhandenen Felder hinzu
		details.add("Type: " + errorType);
		details.add("Reason: " + reason);

		// Füge die optionalen Felder hinzu, falls sie vorhanden sind
		getStatusCode().ifPresent(sc -> details.add("StatusCode: " + sc));
		getAttemptedProxy().ifPresent(p -> details.add("AttemptedProxy: " + p.getName()));
		getRedirectUrlFromError().ifPresent(url -> details.add("RedirectURL: " + url));

		// Kombiniere die Hauptnachricht mit den Details
		// getClass().getSimpleName() ist oft lesbarer als der volle Klassenname
		return getClass().getSimpleName() + ": " + getMessage() + details.toString();
	}
}