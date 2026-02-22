package utils.network.http;

public final class HttpHeader {
	public static final String ACCEPT                      = "Accept";
	public static final String ACCEPT_CHARSET              = "Accept-Charset";
	public static final String ACCEPT_ENCODING             = "Accept-Encoding";
	public static final String ACCEPT_LANGUAGE             = "Accept-Language";
	public static final String ACCEPT_RANGES               = "Accept-Ranges";
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	public static final String AGE                         = "Age";
	public static final String ALLOW                       = "Allow";
	public static final String AUTHORIZATION               = "Authorization";
	public static final String CACHE_CONTROL               = "Cache-Control";
	public static final String CONNECTION                  = "Connection";
	public static final String CONTENT_ENCODING            = "Content-Encoding";
	public static final String CONTENT_LANGUAGE            = "Content-Language";
	public static final String CONTENT_LENGTH              = "Content-Length";
	public static final String CONTENT_LOCATION            = "Content-Location";
	public static final String CONTENT_RANGE               = "Content-Range";
	public static final String CONTENT_TYPE                = "Content-Type";
	public static final String COOKIE                      = "Cookie";
	public static final String DATE                        = "Date";
	public static final String ETAG                        = "ETag";
	public static final String EXPECT                      = "Expect";
	public static final String EXPIRES                     = "Expires";
	public static final String FORWARDED                   = "Forwarded";
	public static final String FROM                        = "From";
	public static final String HOST                        = "Host";
	public static final String IF_MATCH                    = "If-Match";
	public static final String IF_MODIFIED_SINCE           = "If-Modified-Since";
	public static final String IF_NONE_MATCH               = "If-None-Match";
	public static final String IF_RANGE                    = "If-Range";
	public static final String IF_UNMODIFIED_SINCE         = "If-Unmodified-Since";
	public static final String LAST_MODIFIED               = "Last-Modified";
	public static final String LINK                        = "Link";
	public static final String LOCATION                    = "Location";
	public static final String MAX_FORWARDS                = "Max-Forwards";
	public static final String ORIGIN                      = "Origin";
	public static final String PRAGMA                      = "Pragma";
	public static final String PROXY_AUTHENTICATE          = "Proxy-Authenticate";
	public static final String PROXY_AUTHORIZATION         = "Proxy-Authorization";
	public static final String RANGE                       = "Range";
	public static final String REFERER                     = "Referer";
	public static final String RETRY_AFTER                 = "Retry-After";
	public static final String SERVER                      = "Server";
	public static final String SET_COOKIE                  = "Set-Cookie";
	public static final String TE                          = "TE";
	public static final String TRAILER                     = "Trailer";
	public static final String TRANSFER_ENCODING           = "Transfer-Encoding";
	public static final String UPGRADE                     = "Upgrade";
	public static final String USER_AGENT                  = "User-Agent";
	public static final String VARY                        = "Vary";
	public static final String VIA                         = "Via";
	public static final String WARNING                     = "Warning";
	public static final String WWW_AUTHENTICATE            = "WWW-Authenticate";
	// --- Modern Browser Security & Fingerprinting (ESSENZIELL FÜR AMAZON) ---
	/** Used to signal the server that the client prefers an encrypted response. */
	public static final String UPGRADE_INSECURE_REQUESTS = "Upgrade-Insecure-Requests";

	/** Indicates the origin of the fetch request (e.g., 'document', 'image'). */
	public static final String SEC_FETCH_DEST = "Sec-Fetch-Dest";
	/** Indicates the mode of the request (e.g., 'navigate', 'cors'). */
	public static final String SEC_FETCH_MODE = "Sec-Fetch-Mode";
	/** Indicates the relationship between the initiator and the target (e.g., 'cross-site'). */
	public static final String SEC_FETCH_SITE = "Sec-Fetch-Site";
	/** Indicates if the request was triggered by a user gesture. */
	public static final String SEC_FETCH_USER = "Sec-Fetch-User";

	// Optional: Client Hints (Zukunftssicherheit)
	public static final String SEC_CH_UA          = "Sec-CH-UA";
	public static final String SEC_CH_UA_MOBILE   = "Sec-CH-UA-Mobile";
	public static final String SEC_CH_UA_PLATFORM = "Sec-CH-UA-Platform";

	// --- NEU: Security & Misc ---
	/** Common header for identifying AJAX requests or specific client types. */
	public static final String X_REQUESTED_WITH = "X-Requested-With";
	/** Controls how much referrer information is shared. */
	public static final String REFERRER_POLICY  = "Referrer-Policy";

	// Private constructor to prevent instantiation
	private HttpHeader() {
	}
}
