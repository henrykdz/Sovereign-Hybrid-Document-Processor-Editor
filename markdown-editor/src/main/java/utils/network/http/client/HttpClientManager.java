package utils.network.http.client;


import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import utils.general.StringUtils;
import utils.logging.Log;
import utils.network.proxy.CustomProxySelector;
import utils.network.proxy.ProxyConfig;
import utils.network.requestconfig.RequestConfiguration;
import utils.network.strategy.ConnectionStrategy;

/**
 * Manages the creation and reuse of {@link java.net.http.HttpClient} instances.
 * <p>
 * This manager uses a cache for {@link ConnectionStrategy#DIRECT} and specific-proxy connections to enhance performance. For the {@link ConnectionStrategy#SYSTEM_PROXY} strategy,
 * it dynamically creates new clients to ensure they always reflect the latest operating system settings.
 * </p>
 */
public final class HttpClientManager {

	// A thread-safe cache to reuse HttpClient instances for identical, cacheable configurations.
	private static final Map<String, HttpClient> clientCache = new ConcurrentHashMap<>();

	/**
	 * Private constructor to prevent instantiation of this utility class.
	 */
	private HttpClientManager() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Central dispatcher to get the appropriate HttpClient for a given configuration. It intelligently decides whether to return a new, non-cached client (for SYSTEM_PROXY) or a
	 * potentially cached client (for all other strategies).
	 *
	 * @param config The ScrapeConfiguration defining the entire request context.
	 * @return A configured HttpClient instance.
	 */
	public static HttpClient getHttpClient(RequestConfiguration config) {
		Objects.requireNonNull(config, "ScrapeConfiguration cannot be null");

		ConnectionStrategy strategy = config.getConnectionStrategy();

		if (strategy == ConnectionStrategy.SYSTEM_PROXY) {
			return getHttpClientWithSystemProxy(config);
		}

		// Wenn die Strategie explizit DIRECT ist, ignoriere jeden eventuell
		// noch vorhandenen 'preferredProxy' und hole einen direkten Client.
		if (strategy == ConnectionStrategy.DIRECT) {
			return getHttpClient(null, config); // Rufe die Caching-Methode mit proxyConfig = null auf.
		}

		// Für alle anderen Fälle (USE_STANDARD_PROXY, ROTATION)
		// verwende den 'preferredProxy' aus der Konfiguration.
		return getHttpClient(config.getPreferredProxy(), config);
	}

	/**
	 * Gets or creates a cached HttpClient for DIRECT or specific-proxy strategies. This is the primary method for obtaining cacheable clients. It should NOT be used for the
	 * SYSTEM_PROXY strategy.
	 *
	 * @param proxyConfig  The specific {@link ProxyConfig} to use. If null, a client for a DIRECT connection is returned.
	 * @param scrapeConfig The {@link RequestConfiguration}. Must not be null.
	 * @return A configured, potentially cached, HttpClient instance.
	 * @throws IllegalArgumentException if this method is incorrectly called with the SYSTEM_PROXY strategy.
	 */
	public static HttpClient getHttpClient(ProxyConfig proxyConfig, RequestConfiguration scrapeConfig) {
		Objects.requireNonNull(scrapeConfig, "ScrapeConfiguration cannot be null");

		// Safety check: This method is not designed for the SYSTEM_PROXY strategy.
		if (scrapeConfig.getConnectionStrategy() == ConnectionStrategy.SYSTEM_PROXY) {
			throw new IllegalArgumentException("Incorrect usage: Use getHttpClientWithSystemProxy() for the SYSTEM_PROXY strategy.");
		}

		String cacheKey = generateCacheKey(proxyConfig, scrapeConfig);

		// Use computeIfAbsent for thread-safe, efficient creation and caching.
		// The client is only built if its key is not already in the cache.
		return clientCache.computeIfAbsent(cacheKey, key -> {
			Log.fine("Cache miss. Creating new HttpClient for key: " + key);
			return buildCacheableHttpClient(proxyConfig, scrapeConfig);
		});
	}

	/**
	 * Creates a NEW, non-cached HttpClient instance that is configured to use the system's default proxy settings. This method MUST be used for the
	 * {@link ConnectionStrategy#SYSTEM_PROXY} strategy to ensure that any changes to the OS-level proxy settings are always respected.
	 *
	 * @param config The {@link RequestConfiguration} providing timeout and redirect policies.
	 * @return A new, non-cached HttpClient instance configured to use system proxies.
	 */
	public static HttpClient getHttpClientWithSystemProxy(RequestConfiguration config) {
		Objects.requireNonNull(config, "ScrapeConfiguration cannot be null");
		Log.fine("Building NEW, non-cached HttpClient with System Proxy Selector.");

		return HttpClient.newBuilder().proxy(ProxySelector.getDefault()) // Use the current system settings.
		        .followRedirects(config.isHttpClientShouldFollowRedirects() ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER)
		        .connectTimeout(Duration.ofMillis(config.getTimeoutMillis())).build();
	}

	/**
	 * Generates a unique string key for caching HttpClient instances. This key is only intended for cacheable strategies (e.g., DIRECT, USE_STANDARD_PROXY).
	 *
	 * @param proxy  The ProxyConfig used, or null for a direct connection.
	 * @param config The ScrapeConfiguration.
	 * @return A unique String key for the cache.
	 */
	private static String generateCacheKey(ProxyConfig proxy, RequestConfiguration config) {
		// The key is based on the proxy (or "DIRECT" if null) combined with timeout and redirect policy.
		String proxyKey = (proxy == null) ? "DIRECT" : proxy.toStringSimple();
		return proxyKey + "_T" + config.getTimeoutMillis() + "_RHttpClientFollows" + config.isHttpClientShouldFollowRedirects();
	}

	/**
	 * Builds a new, cacheable HttpClient instance. This method handles DIRECT and specific proxy configurations. It should not be called directly for SYSTEM_PROXY.
	 *
	 * @param proxyConfig The ProxyConfig to use, or null to force a direct connection.
	 * @param config      The ScrapeConfiguration.
	 * @return A new, configured HttpClient.
	 */
	private static HttpClient buildCacheableHttpClient(ProxyConfig proxyConfig, RequestConfiguration config) {
		HttpClient.Builder builder = HttpClient.newBuilder().followRedirects(config.isHttpClientShouldFollowRedirects() ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER)
		        .connectTimeout(Duration.ofMillis(config.getTimeoutMillis()));

		// Check if a specific, valid proxy configuration is provided.
		if (proxyConfig != null && proxyConfig.getProxy() != null && proxyConfig.getProxy().type() != java.net.Proxy.Type.DIRECT) {
			// --- Configure a specific proxy ---
			Log.fine("Building HttpClient with specific proxy: " + proxyConfig.toStringSimple());
			builder.proxy(new CustomProxySelector(proxyConfig.getProxy()));

			if (proxyConfig.isRequiresAuth() && StringUtils.isNotBlank(proxyConfig.getUsername()) && proxyConfig.getPassword() != null) {
				Log.fine("Adding proxy authenticator for user: " + proxyConfig.getUsername());
				builder.authenticator(new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(proxyConfig.getUsername(), proxyConfig.getPassword().toCharArray());
					}
				});
			}
		} else {
			// --- Force a DIRECT connection ---
			// This block is reached if proxyConfig is null or represents a DIRECT connection.
			// We explicitly set NO_PROXY to ensure that system-level proxies are always bypassed,
			// making the behavior of the DIRECT strategy absolutely predictable.
			Log.fine("Building HttpClient for a direct connection (explicitly bypassing system proxies).");
			builder.proxy(HttpClient.Builder.NO_PROXY);
		}

		return builder.build();
	}

	/**
	 * Clears the entire HttpClient instance cache. This might be useful if global network settings change in a way that would invalidate all clients (e.g., system-wide certificate
	 * changes), or for debugging purposes.
	 */
	public static void clearCache() {
		Log.warn("Clearing all cached HttpClient instances.");
		clientCache.clear();
	}
}