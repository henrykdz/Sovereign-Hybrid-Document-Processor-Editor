package utils.network.http.status;

import java.util.HashMap;
import java.util.Map;

import utils.logging.Log;

/**
 * With the set/getHandleMessage() methods, specific information can be retrieved, like a relocated URL
 **/

public enum HttpResponseCode {
	CONNECTION_FAILED(-1, "Connection failed due to exception", ""),
    // 1xx Informational Response:
    // These status codes indicate a provisional response. The client should be prepared to receive one or
    // more 1xx
    // responses before receiving a regular response.
    // Informational (1xx)
	HTTP_100_CONTINUE(100, "Continue", "The server has received the request header and is continuing the process."),
	HTTP_101_SWITCHING_PROTOCOLS(101, "Switching Protocols", "The server is switching protocols based on client request."),
	HTTP_102_PROCESSING(102, "Processing", "The server is processing the request, but no response is available yet."),
	HTTP_103_EARLY_HINTS(103, "Early Hints", "The server sent preliminary information about the final response."),

    // 2xx Success:
    // This class of status codes indicates the action requested by the client was received, understood,
    // accepted, and
    // processed successfully by the server.
    // Success (2xx)
	HTTP_200_OK(200, "OK", "The request was successful. The server has fulfilled the request and returned a response."),
	HTTP_201_CREATED(201, "Created", "The request has been fulfilled and resulted in a new resource being created."),
	HTTP_202_ACCEPTED(202, "Accepted", "The request has been accepted for processing, but the processing has not been completed."),
	HTTP_203_NON_AUTHORITATIVE_INFORMATION(203, "Non-Authoritative Information", "The returned information may not be the authoritative source."),
	HTTP_204_NO_CONTENT(204, "No Content", "The server has successfully processed the request and is not returning any content."),
	HTTP_205_RESET_CONTENT(205, "Reset Content", "The client should reset the document view."),
	HTTP_206_PARTIAL_CONTENT(206, "Partial Content", "The server is returning partial content, likely due to a range request."),
	HTTP_207_MULTI_STATUS(207, "Multi-Status", "The request has resulted in multiple status codes, each for a separate sub-request."),
	HTTP_208_ALREADY_REPORTED(208, "Already Reported", "The WebDAV server has already issued the requested DAV compliance capabilities."),
	HTTP_226_IM_USED(226, "IM Used", "The server has fulfilled the request and the user might want to update their local view of the resource."),

    // 3xx Redirection:
    // This class of status codes indicates that the client must take additional action to complete the
    // request. These
    // status codes are used to redirect the client to a different resource.
    // Redirection (3xx)
	HTTP_300_MULTIPLE_CHOICES(300, "Multiple Choices", "The server is sending options for the client to choose from (e.g., different languages)."),
	HTTP_301_MOVED_PERMANENTLY(301, "Moved Permanently", "The requested resource has been permanently moved to a new URL. " + "Future requests should be made to the new address."),
	HTTP_302_FOUND(302, "Found (Temporary Redirect)", "The requested resource was found, but currently resides at a different location. "
	        + "This is a temporary redirect, meaning the original address might be valid again in the future."),
	HTTP_303_SEE_OTHER(303, "See Other", "The server is redirecting the client to a different resource."),
	HTTP_304_NOT_MODIFIED(304, "Not Modified", "The resource has not been modified since the last request, allowing the client to use the cached copy."),
	HTTP_305_USE_PROXY(305, "Use Proxy", "The requested resource must be accessed through the provided proxy server."),
	HTTP_306_SWITCH_PROXY(306, "Switch Proxy", "The server is switching proxies (only supported by HTTP/2)."),
	HTTP_307_TEMPORARY_REDIRECT(307, "Temporary Redirect", "The requested resource has been temporarily moved to a new URL. "
	        + "The original address should be used for future requests."),

	HTTP_308_PERMANENT_REDIRECT(308, "Permanent Redirect", "The requested resource has been permanently moved to a new URL. "
	        + "This is the modern equivalent of a 301 redirect and should be treated the same."),

    // Client Error (4xx) This class of status codes is intended for cases in which the client seems to have erred. The client should not repeat the request without modifications.
	HTTP_400_BAD_REQUEST(400, "Bad Request", "The target server rejected the request due to what it considered a syntax error. "
	        + "This can be caused by an overly strict firewall or security system on the target's side."),

	HTTP_401_UNAUTHORIZED(401, "Unauthorized", "The request requires user authentication for the target resource. "
	        + "Provide valid credentials (e.g., via login on the website)."),
	HTTP_402_PAYMENT_REQUIRED(402, "Payment Required", "Reserved for future use."),
	HTTP_403_FORBIDDEN(403, "Access Restricted", "The target server understood the request but refuses to authorize it. This is not a proxy issue. The target site may be blocking the proxy's IP address, "
	        + "require a login, or have other access restrictions."),
    // *** Beispiel für 404 ***
	HTTP_404_NOT_FOUND(404, "Not Found", "The target server could not find the requested resource. " + "Verify the URL path is correct."),
	HTTP_405_METHOD_NOT_ALLOWED(405, "Method Not Allowed", "The request method is not supported for this resource. Use a supported method (e.g., GET, POST, PUT, DELETE)."),
	HTTP_406_NOT_ACCEPTABLE(406, "Not Acceptable", "The requested content type is not acceptable by the server."),
	HTTP_407_PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required", "The proxy server requires authentication before it can forward the request. "
	        + "Check your proxy settings and credentials in the application's network preferences."),

	HTTP_408_REQUEST_TIMEOUT(408, "Request Timeout", "The target server timed out waiting for the full request. "
	        + "This can indicate a slow network connection or a heavily loaded server."),

	HTTP_409_CONFLICT(409, "Conflict", "The request could not be completed due to a conflict with the current state of the resource. Try the request again with any necessary modifications."),
	HTTP_410_GONE(410, "Gone", "The requested resource is no longer available and will not be available again. Remove references to this resource from your application."),
	HTTP_411_LENGTH_REQUIRED(411, "Length Required", "The server could not determine the content length of the request. Specify the Content-Length header in the request."),
	HTTP_412_PRECONDITION_FAILED(412, "Precondition Failed", "A precondition specified in one or more request headers evaluated to false as a result of the condition being evaluated on the server. Verify that the preconditions sent in the request headers are still valid."),
	HTTP_413_PAYLOAD_TOO_LARGE(413, "Payload Too Large", "The server refused to process the request because the request entity is larger than the server is willing or able to process. Reduce the size of the request entity or check server configuration limits."),
	HTTP_414_URI_TOO_LONG(414, "URI (Request) Too Long", "The URI provided in the request is too long. Shorten the URI or use a POST request with the data in the request body."),
	HTTP_415_UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type", "The server refuses to accept the request because the entity format is not supported. Use a supported media type in the request."),
	HTTP_416_RANGE_NOT_SATISFIABLE(416, "Range Not Satisfiable", "The client has specified a range of bytes in the request header that cannot be fulfilled by the resource. Modify the range specification in the request header."),
	HTTP_417_EXPECTATION_FAILED(417, "Expectation Failed", "One or more of the expectations specified in the request's Expect header could not be met by the server. Modify or remove the Expectation header in the request."),
	HTTP_418_I_AM_A_TEAPOT(418, "I'm a teapot", "A whimsical response used to indicate that the server refuses to brew coffee because it is a teapot. Typically used for lighthearted humor."),
	HTTP_421_MISDIRECTED_REQUEST(421, "Misdirected Request", "The request was directed at an incorrect server. Ensure the request is sent to the appropriate server."),
	HTTP_422_UNPROCESSABLE_ENTITY(422, "Unprocessable Entity", "The request was well-formed but was unable to be followed due to semantic errors in the request body. Check for errors in the request body, such as invalid fields or missing required data."),
	HTTP_423_LOCKED(423, "Locked", "The resource is currently locked and cannot be modified. Wait until the lock is released or try accessing the resource at a different time."),
	HTTP_424_FAILED_DEPENDENCY(424, "Failed Dependency", "A request could not be completed due to a dependency failing. Investigate the dependency and try again once the dependency is resolved."),
	HTTP_425_TOO_EARLY(425, "Too Early", "The server refused to process the request because the expectation condition in the request prevented the server from fulfilling it. Modify the request to remove the expectation condition or try again later."),
	HTTP_426_UPGRADE_REQUIRED(426, "Upgrade Required", "The server refused to handle the request because the client should upgrade to a newer version of the HTTP protocol. Upgrade the client software to support the requested HTTP version."),
	HTTP_428_PRECONDITION_REQUIRED(428, "Precondition Required", "The server refused to process the request because the request lacks a required precondition. Include the required precondition in the request header."),
	HTTP_429_TOO_MANY_REQUESTS(429, "Too Many Requests", "The target server is rate-limiting requests from this IP address (likely the proxy's). "
	        + "Wait a while before trying again or switch to a different proxy."),
	HTTP_431_REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large", "The server refused to accept the request because the combined length of the request headers exceeds the server's limit. Break down the request into multiple requests or reduce the size of individual header fields."),
	HTTP_443_REQUESTED_HTTPS(443, "Requested HTTPS", "The server requires HTTPS for the requested resource but the client sent an HTTP request. The client should resend the request using HTTPS."),
	HTTP_451_UNAVAILABLE_FOR_LEGAL_REASONS(451, "Unavailable For Legal Reasons", "The server encountered legal obstacles that prevented it from fulfilling the request. Contact the website administrator for more information about the issue."),

    // 5xx Server Error:
    // The server failed to fulfill a valid request. This can be due to a number of reasons, including but
    // not limited
    // to
    // server overload or downtime, network congestion, or a configuration error.

    // Server Error (5xx)

	HTTP_500_INTERNAL_SERVER_ERROR(500, "Internal Server Error", "The target server encountered an unexpected internal error. "
	        + "This is a problem on the target website's end and not related to your connection or proxy."),
	HTTP_501_NOT_IMPLEMENTED(501, "Not Implemented", "The server does not support the functionality required to fulfill the request. Contact the website administrator for information on when this functionality might be supported."),
	HTTP_502_BAD_GATEWAY(502, "Bad Gateway", "The server, while acting as a gateway or proxy, received an invalid response from an upstream server "
	        + "it accessed in attempting to fulfill the request. This can be a temporary issue with the proxy or the target server."),
	HTTP_503_SERVICE_UNAVAILABLE(503, "Service Unavailable", "The target server is currently unavailable (e.g., overloaded or down for maintenance). "
	        + "This is usually a temporary issue with the target site."),
	HTTP_504_GATEWAY_TIMEOUT(504, "Gateway Timeout", "The server, acting as a gateway or proxy, timed out waiting for a response from another server. "
	        + "This is often a temporary issue on the target's infrastructure."),
	HTTP_505_HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported", "The server does not support the HTTP version used in the request. Upgrade your client software to support a compatible HTTP version.");

	/**
	 * Provides a full descriptive string for a given HTTP status code, including the code itself, its short message, and its long description. The long description is
	 * automatically formatted with line breaks after each sentence for better readability in tooltips. Leading whitespace on new lines is removed.
	 *
	 * @param code The HTTP status code to describe.
	 * @return A comprehensive, formatted string suitable for a tooltip or detailed log.
	 */
	public static String getFullDescriptionForCode(int code) {
		HttpResponseCode response = getByCode(code);
		if (response != null) {
			String originalDescription = response.getDescription();

			// Regex, die das Satzende (Punkt, !, ?) UND das folgende Leerzeichen findet.
			// Die Gruppe '$1' fängt das Satzzeichen selbst ein.
			// Wir ersetzen dann "Satzzeichen + Leerzeichen" durch "Satzzeichen + Zeilenumbruch".
			String formattedDescription = originalDescription.replaceAll("([.!?])\\s+", "$1\n");

			// Format: "403 Forbidden:\nThe target server refuses..."
			return String.format("%d %s:\n%s", response.getCode(), response.getMessageShort(false), formattedDescription.trim());
		}
		// Fallback für unbekannte Codes
		return String.format("Unknown or unhandled HTTP status code: %d", code);
	}

	private static final Map<Integer, HttpResponseCode> codeMap;

	static {
		codeMap = new HashMap<>();
		for (HttpResponseCode value : values()) {
			codeMap.put(value.code, value);
		}
	}

	/** return null if not found or code is -1 **/
	public static HttpResponseCode getByCode(int code) {
		return codeMap.get(code);
	}

	private int    code = -1;
	private String shortMessage;
	private String longMessage;

	private HttpResponseCode(int code, String statusMessage, String description) {
		this.code = code;
		this.shortMessage = statusMessage;
		this.longMessage = description;
	}

	public int getCode() {
		return code;
	}

	public String getMessageShort(boolean includeCode) {
		return (includeCode ? " " + code + " " : "") + shortMessage;
	}

	public String getDescription() {
		return longMessage;
	}

	public void printToLog() {
		Log.fine(getMessageShort(true) + " Description: " + getDescription());
	}
}