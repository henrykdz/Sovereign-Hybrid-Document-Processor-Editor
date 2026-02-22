package utils.network.requestconfig;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javafx.collections.ObservableList;
import utils.appconfig.OptionsHandler;
import utils.appconfig.OptionsHandler.OptionKey;
import utils.general.StringUtils;
import utils.localize.LangMap;
import utils.logging.Log;
import utils.network.NetworkUtils;
import utils.network.UrlSanitizationOptions;
import utils.network.UserAgentMode;
import utils.network.proxy.ProxyConfig;
import utils.network.requestconfig.RequestConfiguration.HttpRequestMethod;
import utils.network.strategy.ConnectionStrategy;
import utils.network.strategy.ProxyUsageStrategy;

/**
 * Central factory for creating {@link RequestConfiguration} instances based on application settings. This class provides type-safe creation of configurations for different
 * scraping purposes (general scraping, verification, full metadata) with consistent defaults and user preferences.
 */
public final class RequestConfigurationFactory {

	private static OptionsHandler              optionsHandler;
	private static ObservableList<ProxyConfig> proxyList;

	// Private constructor to prevent instantiation
	private RequestConfigurationFactory() {
		throw new IllegalStateException("Utility class - cannot be instantiated.");
	}

	/**
	 * Initializes the factory with application components. Must be called before using factory methods.
	 *
	 * @param options The application's OptionsHandler instance.
	 * @param proxies The current list of proxy configurations.
	 * @throws IllegalStateException if either parameter is null.
	 */
	public static void initialize(OptionsHandler options, ObservableList<ProxyConfig> proxies) {
		optionsHandler = Objects.requireNonNull(options, "OptionsHandler cannot be null");
		proxyList = Objects.requireNonNull(proxies, "Proxy list cannot be null");
		Log.info("RequestConfigurationFactory initialized with %d proxies", proxies.size());
	}

	/**
	 * Creates a comprehensive {@link RequestConfiguration} for general-purpose web scraping. This configuration reflects all current user preferences from the application
	 * settings.
	 *
	 * @return A new RequestConfiguration for general scraping.
	 * @throws IllegalStateException if factory is not initialized or application components are unavailable.
	 */

	/**
	 * Creates a configuration that reflects the current global user settings.
	 */
	public static RequestConfiguration createForGeneralScraping() {
		ensureInitialized();

		try {
			ConnectionStrategy strategy = getEnumValueOrDefault(OptionKey.NETWORK_CONNECTION_STRATEGY, ConnectionStrategy.DIRECT);
			ProxyUsageStrategy usageStrategy = getEnumValueOrDefault(OptionKey.NETWORK_PROXY_USAGE_STRATEGY, ProxyUsageStrategy.WEIGHTED_RANDOM);
			int preferredProxyIndex = optionsHandler.getValueOrDefault(OptionKey.NETWORK_STANDARD_PROXY);

			// Flags
			boolean updateTitle = optionsHandler.getValueOrDefault(OptionKey.NETWORK_SCRAPE_UPDATE_TITLE);
			boolean updateUrlOnRedirection = optionsHandler.getValueOrDefault(OptionKey.NETWORK_SCRAPE_UPDATE_URL);
			boolean updateDescription = optionsHandler.getValueOrDefault(OptionKey.NETWORK_SCRAPE_UPDATE_DESCRIPTION);
			boolean updateKeywords = optionsHandler.getValueOrDefault(OptionKey.NETWORK_SCRAPE_UPDATE_KEYWORDS);

			// --- NEU: Das Premium-Feature Flag auslesen ---
			// Innerhalb des try-Blocks
			UrlSanitizationOptions sanitization = new UrlSanitizationOptions(optionsHandler.getValueOrDefault(OptionKey.NETWORK_SANITIZE_URLS),
			        optionsHandler.getValueOrDefault(OptionKey.NETWORK_SANITIZE_AMAZON), optionsHandler.getValueOrDefault(OptionKey.NETWORK_SANITIZE_EBAY),
			        optionsHandler.getValueOrDefault(OptionKey.NETWORK_SANITIZE_CANONICAL));

			// Network Params
			int timeoutSeconds = optionsHandler.getValueOrDefault(OptionKey.NETWORK_SCRAPE_TIMEOUT);
			int maxRedirects = optionsHandler.getValueOrDefault(OptionKey.NETWORK_SCRAPE_MAX_REDIRECTS);
			int requestDelay = optionsHandler.getValueOrDefault(OptionKey.NETWORK_REQUEST_DELAY_MS);
			boolean fallback = optionsHandler.getValueOrDefault(OptionKey.NETWORK_PROXY_ROTATION_FALLBACK_TO_DIRECT);
			int cooldown = optionsHandler.getValueOrDefault(OptionKey.NETWORK_PROXY_FAILURE_COOLDOWN_SECONDS);

			// UA & Language
			UserAgentMode uaMode = getEnumValueOrDefault(OptionKey.NETWORK_USER_AGENT_MODE, UserAgentMode.DEFAULT_JAVA);
			String userAgent = determineEffectiveUserAgent(uaMode);
			Locale language = determineDefaultLanguage();

			return new RequestConfiguration(strategy, proxyList, preferredProxyIndex, updateTitle, updateUrlOnRedirection, updateDescription, updateKeywords, timeoutSeconds * 1000,
			        maxRedirects, userAgent, requestDelay, fallback, true, HttpRequestMethod.GET, usageStrategy, language, cooldown, sanitization);

		} catch (Exception e) {
			Log.error(e, "Failed to create general scraping configuration");
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Creates a specialized configuration for URL verification (quick connectivity check). Optimized for speed with shorter timeout and limited redirects.
	 *
	 * @return A new RequestConfiguration optimized for verification.
	 */
	public static RequestConfiguration createForVerification() {
		ensureInitialized();
		RequestConfiguration base = createForGeneralScraping();

		// Spezialisierung für schnellen Check
		return new RequestConfiguration(base.getConnectionStrategy(), base.getAvailableProxies(), base.getPreferredProxyIndex(), false, true, false, false, 7000, 3,
		        base.getUserAgent(), 0, base.isFallbackToDirectOnRotationFailure(), true, HttpRequestMethod.GET, base.getProxyUsageStrategy(), base.getLanguageOverride(),
		        base.getProxyCooldownSeconds(), base.getSanitizationOptions() // Vererbung
		);
	}

	/**
	 * Creates a specialized configuration for full metadata scraping (title, description, keywords). Optimized for content extraction with disabled redirect following.
	 *
	 * @return A new RequestConfiguration optimized for full metadata scraping.
	 */

	public static RequestConfiguration createForFullMetadataScrape() {
		ensureInitialized();
		RequestConfiguration base = createForGeneralScraping();

		// Spezialisierung für Content-Extraktion
		return new RequestConfiguration(base.getConnectionStrategy(), base.getAvailableProxies(), base.getPreferredProxyIndex(), true, false, true, true, base.getTimeoutMillis(),
		        0, base.getUserAgent(), base.getRequestDelayMillis(), base.isFallbackToDirectOnRotationFailure(), false, HttpRequestMethod.GET, base.getProxyUsageStrategy(),
		        base.getLanguageOverride(), base.getProxyCooldownSeconds(), base.getSanitizationOptions());
	}

	/**
	 * Creates a custom configuration with specific settings. Useful for one-off operations with non-standard parameters.
	 *
	 * @param strategy        Connection strategy to use
	 * @param cooldownSeconds Custom proxy cooldown in seconds (0 to use default)
	 * @param followRedirects Whether HttpClient should follow redirects
	 * @param httpMethod      HTTP method to use
	 * @return A custom RequestConfiguration
	 */
	public static RequestConfiguration createCustom(ConnectionStrategy strategy, int cooldownSeconds, boolean followRedirects, HttpRequestMethod httpMethod) {

		ensureInitialized();

		// Wir holen die Basis-Konfiguration, um die Benutzerpräferenzen zu erben
		RequestConfiguration baseConfig = createForGeneralScraping();

		// Nutze den übergebenen Cooldown oder falle auf den Standard zurück
		int effectiveCooldown = cooldownSeconds > 0 ? cooldownSeconds : baseConfig.getProxyCooldownSeconds();

		return new RequestConfiguration(strategy, baseConfig.getAvailableProxies(), baseConfig.getPreferredProxyIndex(), baseConfig.isUpdateTitle(),
		        baseConfig.isUpdateUrlOnRedirection(), baseConfig.isUpdateDescription(), baseConfig.isUpdateKeywords(), baseConfig.getTimeoutMillis(),
		        baseConfig.getMaxRedirectsForManualLoopOrInfo(), baseConfig.getUserAgent(), baseConfig.getRequestDelayMillis(), baseConfig.isFallbackToDirectOnRotationFailure(),
		        followRedirects, httpMethod, baseConfig.getProxyUsageStrategy(), baseConfig.getLanguageOverride(), effectiveCooldown, baseConfig.getSanitizationOptions());
	}

	/**
	 * Creates a configuration for direct connection (no proxy). Useful for fallback scenarios or when proxies should be bypassed.
	 *
	 * @return A RequestConfiguration with DIRECT connection strategy
	 */
	public static RequestConfiguration createForDirectConnection() {
		ensureInitialized();
		RequestConfiguration base = createForGeneralScraping();

		return new RequestConfiguration(ConnectionStrategy.DIRECT, List.of(), -1, base.isUpdateTitle(), true, base.isUpdateDescription(), base.isUpdateKeywords(),
		        base.getTimeoutMillis(), base.getMaxRedirectsForManualLoopOrInfo(), base.getUserAgent(), base.getRequestDelayMillis(), false, true, base.getHttpMethod(),
		        ProxyUsageStrategy.WEIGHTED_RANDOM, base.getLanguageOverride(), base.getProxyCooldownSeconds(), base.getSanitizationOptions());
	}

	// --- Helper Methods ---

	private static void ensureInitialized() {
		if (optionsHandler == null || proxyList == null) {
			throw new IllegalStateException("RequestConfigurationFactory not initialized. Call initialize() first.");
		}
	}

	private static <E extends Enum<E>> E getEnumValueOrDefault(OptionKey key, E defaultValue) {

		String savedName = optionsHandler.getValueOrDefault(key);
		if (savedName != null) {
			try {
				return (E) Enum.valueOf(defaultValue.getDeclaringClass(), savedName);
			} catch (IllegalArgumentException | NullPointerException e) {
				Log.warn("Invalid saved value for key %s: '%s'. Using default: %s", key, savedName, defaultValue);
			}
		}
		return defaultValue;
	}

	private static String determineEffectiveUserAgent(UserAgentMode uaMode) {
		String effectiveUserAgent;
		switch (uaMode) {
		case DEFAULT_JAVA:
			effectiveUserAgent = null; // Let HttpClient use its internal default
			break;
		case BROWSER_SIMULATION:
			effectiveUserAgent = NetworkUtils.DEFAULT_USER_AGENT_VALUE;
			break;
		case CUSTOM:
			effectiveUserAgent = optionsHandler.getValueOrDefault(OptionKey.NETWORK_USER_AGENT_CUSTOM_SELECTED);
			break;
		case DISABLED:
			effectiveUserAgent = ""; // Empty string to disable header
			break;
		default:
			Log.warn("Unhandled UserAgentMode: %s. Falling back to HttpClient default.", uaMode);
			effectiveUserAgent = null;
			break;
		}

		Log.fine("Determined User-Agent for mode %s: %s", uaMode,
		        (effectiveUserAgent == null) ? "<HttpClient Default>" : (effectiveUserAgent.isEmpty() ? "<Disabled>" : effectiveUserAgent));

		return effectiveUserAgent;
	}

	private static Locale determineDefaultLanguage() {
		Locale defaultLanguage = null;
		String userLocaleCode = optionsHandler.getValueOrDefault(OptionKey.USER_LOCALE);

		if (StringUtils.isNotBlank(userLocaleCode)) {
			try {
				defaultLanguage = Locale.forLanguageTag(userLocaleCode.replace('_', '-'));
				Log.fine("Using configured USER_LOCALE: %s", userLocaleCode);
			} catch (Exception e) {
				Log.warn(e, "Invalid USER_LOCALE code: '%s'", userLocaleCode);
			}
		}

		if (defaultLanguage == null) {
			defaultLanguage = LangMap.getActiveLocale();
			Log.fine("Using active app language: %s", defaultLanguage.toLanguageTag());
		}

		return defaultLanguage;
	}

	/**
	 * Gets the current proxy cooldown setting from user preferences.
	 *
	 * @return Proxy cooldown in seconds
	 */
	public static int getCurrentProxyCooldownSeconds() {
		ensureInitialized();
		return optionsHandler.getValueOrDefault(OptionKey.NETWORK_PROXY_FAILURE_COOLDOWN_SECONDS);
	}

	/**
	 * Updates the proxy cooldown setting in user preferences. Changes will be reflected in future configurations.
	 *
	 * @param cooldownSeconds New cooldown value in seconds (must be >= 0)
	 */
	public static void updateProxyCooldownSetting(int cooldownSeconds) {
		ensureInitialized();
		if (cooldownSeconds < 0) {
			throw new IllegalArgumentException("Cooldown seconds cannot be negative");
		}

		optionsHandler.setValue(OptionKey.NETWORK_PROXY_FAILURE_COOLDOWN_SECONDS, cooldownSeconds);
		optionsHandler.write();
		Log.info("Updated proxy cooldown setting to %d seconds", cooldownSeconds);
	}

	/**
	 * Checks if the factory has been initialized.
	 *
	 * @return true if factory is ready to create configurations
	 */
	public static boolean isInitialized() {
		return optionsHandler != null && proxyList != null;
	}

}