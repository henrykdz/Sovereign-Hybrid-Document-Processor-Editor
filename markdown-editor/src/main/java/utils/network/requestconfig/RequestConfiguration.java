package utils.network.requestconfig;


import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import utils.network.UrlSanitizationOptions;
import utils.network.proxy.ProxyConfig;
import utils.network.strategy.ConnectionStrategy;
import utils.network.strategy.ProxyUsageStrategy;

/**
 * Holds all configuration parameters needed to perform a network request task. 
 * Effectively immutable after creation.
 */
public class RequestConfiguration {

	public enum HttpRequestMethod {
		GET, HEAD, POST, PUT, DELETE, OPTIONS, TRACE, PATCH
	}

	// --- Factory Methods ---

	public static RequestConfiguration forVerification(RequestConfiguration baseConfig) {
		final int VERIFICATION_TIMEOUT_MS = 7 * 1000;
		final int MAX_REDIRECTS_TO_FOLLOW = 3;
		final int NO_REQUEST_DELAY = 0;

		return new RequestConfiguration(
		        baseConfig.getConnectionStrategy(), baseConfig.getAvailableProxies(), baseConfig.getPreferredProxyIndex(),
		        false, // updateTitle
		        true,  // updateUrlOnRedirection
		        false, // updateDescription
		        false, // updateKeywords
		        VERIFICATION_TIMEOUT_MS, MAX_REDIRECTS_TO_FOLLOW, baseConfig.getUserAgent(), NO_REQUEST_DELAY,
		        baseConfig.isFallbackToDirectOnRotationFailure(), true, 
		        HttpRequestMethod.GET,
		        baseConfig.getProxyUsageStrategy(), baseConfig.getLanguageOverride(), 
		        baseConfig.proxyCooldownSeconds, 
		        baseConfig.getSanitizationOptions() // --- NEU: Vererbung des Sanitization-Flags ---
		);
	}

	public static RequestConfiguration forFullMetadataScrape(RequestConfiguration baseConfig) {
		return new RequestConfiguration(
		        baseConfig.getConnectionStrategy(), baseConfig.getAvailableProxies(), baseConfig.getPreferredProxyIndex(),
		        true,  // updateTitle
		        false, // updateUrlOnRedirection
		        true,  // updateDescription
		        true,  // updateKeywords
		        baseConfig.getTimeoutMillis(), 0, 
		        baseConfig.getUserAgent(), baseConfig.getRequestDelayMillis(),
		        baseConfig.isFallbackToDirectOnRotationFailure(), false, 
		        HttpRequestMethod.GET,
		        baseConfig.getProxyUsageStrategy(), baseConfig.getLanguageOverride(), 
		        baseConfig.proxyCooldownSeconds,
		        baseConfig.getSanitizationOptions() // --- NEU ---
		);
	}

	// --- Copy/Modification Methods ---

	public RequestConfiguration withApplicationLevelRedirectLogic(boolean enableAppLevelRedirectLogic) {
		return new RequestConfiguration(this.connectionStrategy, this.availableProxies, this.preferredProxyIndex,
		        this.updateTitle, enableAppLevelRedirectLogic, this.updateDescription, this.updateKeywords,
		        this.timeoutMillis, enableAppLevelRedirectLogic ? 5 : 0, this.userAgent, this.requestDelayMillis,
		        this.fallbackToDirectOnRotationFailure, false, this.httpMethod, this.proxyUsageStrategy,
		        this.languageOverride, this.proxyCooldownSeconds, 
		        this.sanitizationOptions // --- NEU ---
		);
	}

	public RequestConfiguration withLanguageOverride(Locale language) {
		return new RequestConfiguration(this.connectionStrategy, this.availableProxies, this.preferredProxyIndex,
		        this.updateTitle, this.updateUrlOnRedirection, this.updateDescription, this.updateKeywords,
		        this.timeoutMillis, this.maxRedirectsForManualLoopOrInfo, this.userAgent, this.requestDelayMillis,
		        this.fallbackToDirectOnRotationFailure, this.httpClientShouldFollowRedirects, this.httpMethod,
		        this.proxyUsageStrategy, language, this.proxyCooldownSeconds, 
		        this.sanitizationOptions // --- NEU ---
		);
	}

	public RequestConfiguration withHttpMethod(HttpRequestMethod newMethod) {
		return new RequestConfiguration(this.connectionStrategy, this.availableProxies, this.preferredProxyIndex,
		        this.updateTitle, this.updateUrlOnRedirection, this.updateDescription, this.updateKeywords,
		        this.timeoutMillis, this.maxRedirectsForManualLoopOrInfo, this.userAgent, this.requestDelayMillis,
		        this.fallbackToDirectOnRotationFailure, this.httpClientShouldFollowRedirects, newMethod,
		        this.proxyUsageStrategy, this.languageOverride, this.proxyCooldownSeconds, 
		        this.sanitizationOptions // --- NEU ---
		);
	}
	
	/**
	 * Creates a copy of this configuration with a modified list of available proxies.
	 * This is used for specialized rotation strategies like "Worst First".
	 */
	public RequestConfiguration withProxyList(List<ProxyConfig> newProxyList) {
		return new RequestConfiguration(
				this.connectionStrategy, 
				newProxyList, // Der neue, modifizierte Proxy-Pool
				this.preferredProxyIndex, 
				this.updateTitle, 
				this.updateUrlOnRedirection, 
				this.updateDescription, 
				this.updateKeywords, 
				this.timeoutMillis, 
				this.maxRedirectsForManualLoopOrInfo, 
				this.userAgent, 
				this.requestDelayMillis, 
				this.fallbackToDirectOnRotationFailure, 
				this.httpClientShouldFollowRedirects, 
				this.httpMethod, 
				this.proxyUsageStrategy, 
				this.languageOverride, 
				this.proxyCooldownSeconds,
				this.sanitizationOptions // --- WICHTIG: Das neue Flag mitnehmen ---
		);
	}
	
	/**
	 * Creates a copy with the specific sanitization preference.
	 */
	public RequestConfiguration withSanitization(boolean sanitize) {
		return new RequestConfiguration(this.connectionStrategy, this.availableProxies, this.preferredProxyIndex,
		        this.updateTitle, this.updateUrlOnRedirection, this.updateDescription, this.updateKeywords,
		        this.timeoutMillis, this.maxRedirectsForManualLoopOrInfo, this.userAgent, this.requestDelayMillis,
		        this.fallbackToDirectOnRotationFailure, this.httpClientShouldFollowRedirects, this.httpMethod,
		        this.proxyUsageStrategy, this.languageOverride, this.proxyCooldownSeconds, 
		        this.sanitizationOptions // --- NEU ---
		);
	}
	
	public RequestConfiguration copyWithDirectStrategy() {
		return new RequestConfiguration(ConnectionStrategy.DIRECT, Collections.emptyList(), -1, this.updateTitle, true,
		        this.updateDescription, this.updateKeywords, this.timeoutMillis, this.maxRedirectsForManualLoopOrInfo,
		        this.userAgent, this.requestDelayMillis, false, true, this.httpMethod,
		        ProxyUsageStrategy.WEIGHTED_RANDOM, this.languageOverride, this.proxyCooldownSeconds, 
		        this.sanitizationOptions // --- NEU ---
		);
	}

	// --- Fields ---
	private final ConnectionStrategy connectionStrategy;
	private final ProxyUsageStrategy proxyUsageStrategy;
	private final List<ProxyConfig>  availableProxies;
	private final int                preferredProxyIndex;
	private final boolean            updateTitle;
	private final boolean            updateUrlOnRedirection;
	private final boolean            updateDescription;
	private final boolean            updateKeywords;
	private final int                timeoutMillis;
	private final int                maxRedirectsForManualLoopOrInfo;
	private final boolean            httpClientShouldFollowRedirects;
	private final String             userAgent;
	private final int                requestDelayMillis;
	private final boolean            fallbackToDirectOnRotationFailure;
	private final HttpRequestMethod  httpMethod;
	private final Locale             languageOverride;
	private final int                proxyCooldownSeconds;
	private final UrlSanitizationOptions sanitizationOptions; // --- NEU ---
	
	/**
	 * Returns the specific ProxyConfig object based on the preferredProxyIndex.
	 * 
	 * @return The preferred ProxyConfig, or null if no valid index is set or the list is empty.
	 */
	public ProxyConfig getPreferredProxy() {
		if (preferredProxyIndex >= 0 && availableProxies != null && preferredProxyIndex < availableProxies.size()) {
			return availableProxies.get(preferredProxyIndex);
		}
		return null;
	}

	/**
	 * Primary constructor updated to use the full options record.
	 */
	public RequestConfiguration(ConnectionStrategy connectionStrategy, List<ProxyConfig> availableProxies,
	        int preferredProxyIndex, boolean updateTitle, boolean updateUrlOnRedirection, boolean updateDescription,
	        boolean updateKeywords, int timeoutMillis, int maxRedirectsInfo, String userAgent, int requestDelayMillis,
	        boolean fallbackToDirectOnRotationFailure, boolean followHttpRedirects, HttpRequestMethod httpMethod,
	        ProxyUsageStrategy proxyUsageStrategy, Locale languageOverride, int proxyCooldownSeconds, 
	        UrlSanitizationOptions sanitizationOptions // --- ÜBERNAHME RECORD ---
	) {

		if (timeoutMillis <= 0) throw new IllegalArgumentException("Timeout must be > 0");
		
		this.connectionStrategy = Objects.requireNonNullElse(connectionStrategy, ConnectionStrategy.DIRECT);
		this.availableProxies = (availableProxies != null) ? List.copyOf(availableProxies) : Collections.emptyList();
		this.preferredProxyIndex = (preferredProxyIndex >= 0 && preferredProxyIndex < this.availableProxies.size()) ? preferredProxyIndex : -1;

		this.updateTitle = updateTitle;
		this.updateUrlOnRedirection = updateUrlOnRedirection;
		this.updateDescription = updateDescription;
		this.updateKeywords = updateKeywords;
		this.timeoutMillis = timeoutMillis;
		this.maxRedirectsForManualLoopOrInfo = maxRedirectsInfo;
		this.userAgent = userAgent;
		this.requestDelayMillis = Math.max(0, requestDelayMillis);
		this.fallbackToDirectOnRotationFailure = fallbackToDirectOnRotationFailure;
		this.httpClientShouldFollowRedirects = followHttpRedirects;
		this.httpMethod = Objects.requireNonNullElse(httpMethod, HttpRequestMethod.GET);
		this.proxyUsageStrategy = Objects.requireNonNullElse(proxyUsageStrategy, ProxyUsageStrategy.WEIGHTED_RANDOM);
		this.languageOverride = languageOverride;
		this.proxyCooldownSeconds = Math.max(0, proxyCooldownSeconds);
		this.sanitizationOptions = Objects.requireNonNullElse(sanitizationOptions, UrlSanitizationOptions.disabled());
	}

	// --- Getters & Wrapper ---

	public Locale getLanguageOverride() { return languageOverride; }
	public ConnectionStrategy getConnectionStrategy() { return connectionStrategy; }
	public ProxyUsageStrategy getProxyUsageStrategy() { return proxyUsageStrategy; }
	public List<ProxyConfig> getAvailableProxies() { return availableProxies; }
	public int getPreferredProxyIndex() { return preferredProxyIndex; }
	public boolean isUpdateTitle() { return updateTitle; }
	public boolean isUpdateUrlOnRedirection() { return updateUrlOnRedirection; }
	public boolean isUpdateDescription() { return updateDescription; }
	public boolean isUpdateKeywords() { return updateKeywords; }
	public int getTimeoutMillis() { return timeoutMillis; }
	public int getMaxRedirectsForManualLoopOrInfo() { return maxRedirectsForManualLoopOrInfo; }
	public boolean isHttpClientShouldFollowRedirects() { return httpClientShouldFollowRedirects; }
	public String getUserAgent() { return userAgent; }
	public int getRequestDelayMillis() { return requestDelayMillis; }
	public int getProxyCooldownSeconds() { return proxyCooldownSeconds; }
	public boolean isFallbackToDirectOnRotationFailure() { return fallbackToDirectOnRotationFailure; }
	public HttpRequestMethod getHttpMethod() { return httpMethod; }
	public UrlSanitizationOptions getSanitizationOptions() { return sanitizationOptions; }
	
	
	/** 
	 * Wrapper for backward compatibility with general "is cleaning active?" checks.
	 */
	public boolean isSanitizeUrls() { return sanitizationOptions.masterActive(); }

	@Override
	public String toString() {
		return "RequestConfiguration{" + "strategy=" + connectionStrategy + ", sanitize=" + sanitizationOptions + ", timeout=" + timeoutMillis + "ms" + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RequestConfiguration that = (RequestConfiguration) o;
		return sanitizationOptions == that.sanitizationOptions && timeoutMillis == that.timeoutMillis && 
		       connectionStrategy == that.connectionStrategy && Objects.equals(userAgent, that.userAgent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectionStrategy, timeoutMillis, userAgent, sanitizationOptions);
	}
}