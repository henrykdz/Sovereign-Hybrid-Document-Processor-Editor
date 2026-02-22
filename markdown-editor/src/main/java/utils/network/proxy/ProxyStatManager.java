package utils.network.proxy;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

//import izon.flowshift.webmarks.Launcher;
import utils.logging.Log;

public class ProxyStatManager {

	private static final ProxyStatManager INSTANCE       = new ProxyStatManager();
	private static final String           STATS_FILENAME = "proxy_stats.json";

	// --- Configuration settings for proxy exclusion ---
	private int  maxConsecutiveProxyFaultsThreshold = 5;                             // Threshold of consecutive PROXY FAULTS before exclusion.
	private long proxyFaultCooldownMillis           = TimeUnit.SECONDS.toMillis(30); // 🔴 30s statt 5min

	// --- Internal state ---
	private final Map<String, ProxyStats> statsMap      = new ConcurrentHashMap<>();
	private Path                          statsFilePath = null;
	private final Gson                    gson;
	private final AtomicBoolean           dirty         = new AtomicBoolean(false);
	private ScheduledExecutorService      scheduler; // Hinzugefügtes Feld

	private ProxyStatManager() {
		Log.info("ProxyStatManager initializing...");
		gson = new GsonBuilder().setPrettyPrinting().create();
		scheduler = Executors.newSingleThreadScheduledExecutor(); // Initialisierung des Schedulers
		Log.info("ProxyStatManager initialized.");
	}

	public static ProxyStatManager getInstance() {
		return INSTANCE;
	}

	/**
	 * Sets the directory for storing the proxy statistics file and immediately triggers a load of any existing stats from that location. This method is the primary initialization
	 * point for the manager's persistence layer.
	 *
	 * @param configDirectory The path to the application's configuration directory.
	 */
	public synchronized void setStatsDirectory(Path configDirectory) {
		if (configDirectory != null) {
			this.statsFilePath = configDirectory.resolve(STATS_FILENAME);
			Log.info("Proxy stats file path set to: " + this.statsFilePath);
			loadStatsFromFile();

			// Starte das periodische Speichern, sobald wir wissen, wohin wir speichern sollen.
			// Ein Intervall von 5 Minuten ist ein guter Kompromiss.
			startPeriodicPersistence(5, TimeUnit.MINUTES);

		} else {
			Log.warn("Configuration directory is null, cannot set stats file path.");
			this.statsFilePath = null;
		}
	}

	/**
	 * Gets stats for proxy, returns null if not found
	 */
	public ProxyStats getStats(ProxyConfig proxy) {
		if (proxy == null)
			return null;
		return statsMap.get(proxy.toStringSimple());
	}

	public synchronized void setMaxConsecutiveFailuresThreshold(int threshold) {
		if (threshold > 0) {
			this.maxConsecutiveProxyFaultsThreshold = threshold; // Verwendet die korrekte Variable
			Log.info("ProxyStatManager: Max consecutive PROXY FAULTS threshold set to: " + threshold);
		} else {
			Log.warn("ProxyStatManager: Invalid threshold: " + threshold + ". Keeping current: " + this.maxConsecutiveProxyFaultsThreshold);
		}
	}

	public synchronized void setFailureCooldownMillis(long cooldownMillis) {
		if (cooldownMillis >= 0) {
			this.proxyFaultCooldownMillis = cooldownMillis; // Verwendet die korrekte Variable
			Log.info("ProxyStatManager: PROXY FAULT cooldown millis set to: " + cooldownMillis);
		} else {
			Log.warn("ProxyStatManager: Invalid cooldown: " + cooldownMillis + ". Keeping current: " + this.proxyFaultCooldownMillis);
		}
	}

	public ProxyStats getOrCreateStats(ProxyConfig config) {
		Objects.requireNonNull(config, "ProxyConfig cannot be null");
		String proxyId = config.toStringSimple();
		return statsMap.computeIfAbsent(proxyId, id -> {
			Log.fine("Creating new ProxyStats for id: " + id);
			return new ProxyStats(id);
		});
	}

	// --- Differentiated recording methods ---

	/**
	 * Records a successful connection via the specified proxy. This resets the proxy's consecutive failure count and updates its last success timestamp.
	 *
	 * @param config            The proxy that was used successfully.
	 * @param requestTimeMillis The duration of the successful request in milliseconds.
	 */
	public void recordSuccess(ProxyConfig config, long requestTimeMillis) {
		if (config == null)
			return;
		getOrCreateStats(config).recordSuccess(requestTimeMillis);
		dirty.set(true);
	}

	/**
	 * Records a critical proxy failure (e.g., connection refused, timeout). This increments the proxy's consecutive failure count and may lead to its temporary exclusion if the
	 * threshold is reached. It also triggers a cooldown period.
	 *
	 * @param config       The proxy that failed to connect.
	 * @param errorMessage A short description of the failure reason.
	 */
	public void recordProxyFault(ProxyConfig config, String errorMessage) {
		if (config == null)
			return;
		getOrCreateStats(config).recordProxyFault(errorMessage);
		dirty.set(true);
		Log.warn("Recorded PROXY FAULT for proxy: %s - Error: %s. This counts towards exclusion.", config.toStringSimple(), errorMessage);
	}

	/**
	 * Records an issue where the proxy itself worked, but the target server responded with an error (e.g., 403 Forbidden, 500 Server Error). This is tracked for statistical
	 * purposes but does **not** count towards the proxy's exclusion threshold.
	 *
	 * @param config       The proxy that was used.
	 * @param errorMessage A short description of the target server's issue.
	 */
	public void recordTargetIssueViaProxy(ProxyConfig config, String errorMessage) {
		if (config == null)
			return;
		getOrCreateStats(config).recordTargetIssueViaProxy(errorMessage);
		dirty.set(true);
		Log.info("Recorded TARGET ISSUE via proxy: %s - Message: %s. This does NOT directly count towards proxy exclusion.", config.toStringSimple(), errorMessage);
	}

	// --- Exclusion and Blocking Logic (uses new stats) ---

	/**
	 * Determines if a proxy should be temporarily excluded from use based on its failure statistics.
	 * <p>
	 * A proxy is excluded if:
	 * <ol>
	 * <li>Its consecutive proxy fault count has reached the configured threshold.</li>
	 * <li>It is currently within the cooldown period following its last recorded proxy fault.</li>
	 * </ol>
	 *
	 * @param config The proxy to check.
	 * @return {@code true} if the proxy should be excluded, {@code false} otherwise.
	 */
	public boolean isExcluded(ProxyConfig config) {
		if (config == null)
			return false;
		ProxyStats stats = getOrCreateStats(config);

		// Kriterium 1: Schwellenwert für aufeinanderfolgende Proxy-Fehler erreicht?
		if (stats.getConsecutiveProxyFaults() >= maxConsecutiveProxyFaultsThreshold) {
			Log.warn("Proxy %s EXCLUDED due to %d consecutive PROXY FAULTS (Threshold: %d)", config.toStringSimple(), stats.getConsecutiveProxyFaults(),
			        maxConsecutiveProxyFaultsThreshold);
			return true;
		}

		// Kriterium 2: Befindet sich der Proxy in der Cooldown-Phase nach dem letzten Proxy-Fehler?
		long lastFault = stats.getLastProxyFaultTimestamp();
		if (lastFault > 0 && lastFault >= stats.getLastSuccessTimestamp()) {
			long timeSinceLastFault = System.currentTimeMillis() - lastFault;
			if (timeSinceLastFault < proxyFaultCooldownMillis) {
				Log.info("Proxy %s EXCLUDED due to cooldown (Last PROXY FAULT %dms ago, Cooldown: %dms)", config.toStringSimple(), timeSinceLastFault, proxyFaultCooldownMillis);
				return true;
			}
		}

		return false;
	}

	public boolean isProxyVisuallyBlocked(ProxyConfig config) {
		if (config == null)
			return false;
		ProxyStats stats = getOrCreateStats(config);
		return stats.getConsecutiveProxyFaults() >= maxConsecutiveProxyFaultsThreshold;
	}

	/**
	 * Checks if proxy is excluded based on SPECIFIC user cooldown
	 */
	public boolean isExcludedByUserCooldown(ProxyConfig config, int userCooldownSeconds) {
		if (config == null || userCooldownSeconds <= 0) {
			return isExcluded(config); // Fallback zu normaler Logik
		}

		ProxyStats stats = getOrCreateStats(config);
		long lastFault = stats.getLastProxyFaultTimestamp();

		if (lastFault == 0)
			return false; // Kein Fehler = kein Cooldown

		long timeSinceFault = System.currentTimeMillis() - lastFault;
		long userCooldownMillis = TimeUnit.SECONDS.toMillis(userCooldownSeconds);

		return timeSinceFault < userCooldownMillis;
	}

	/**
	 * Starts a user-specific cooldown (resets the fault timestamp)
	 */
	public void triggerUserCooldown(ProxyConfig config, int cooldownSeconds) {
		if (config == null || cooldownSeconds <= 0)
			return;

		ProxyStats stats = getOrCreateStats(config);
		stats.setLastProxyFaultTimestamp(System.currentTimeMillis());
		dirty.set(true);

		Log.info("Triggered %ds user cooldown for proxy %s", cooldownSeconds, config.getName());
	}

	/**
	 * Gets remaining user cooldown time
	 */
	public long getUserCooldownRemainingSeconds(ProxyConfig config, int userCooldownSeconds) {
		if (config == null || userCooldownSeconds <= 0)
			return 0;

		ProxyStats stats = getOrCreateStats(config);
		long lastFault = stats.getLastProxyFaultTimestamp();
		if (lastFault == 0)
			return 0;

		long elapsed = System.currentTimeMillis() - lastFault;
		long total = TimeUnit.SECONDS.toMillis(userCooldownSeconds);
		long remaining = Math.max(0, total - elapsed);

		return TimeUnit.MILLISECONDS.toSeconds(remaining);
	}

	/**
	 * Resets the counter for consecutive proxy faults for a proxy.
	 */
	public synchronized void resetProxyConsecutiveFailures(ProxyConfig config) {
		if (config == null)
			return;
		getOrCreateStats(config).resetConsecutiveProxyFaults();
		dirty.set(true);
		Log.info("Consecutive proxy faults reset for proxy: " + config.toStringSimple());
	}

	public boolean areAllProxiesInListVisuallyBlocked(List<ProxyConfig> proxies) {
		if (proxies == null || proxies.isEmpty())
			return false;
		return proxies.stream().filter(Objects::nonNull).allMatch(this::isProxyVisuallyBlocked);
	}

	public boolean areAllRelevantProxiesExcluded(List<ProxyConfig> relevantProxies) {
		if (relevantProxies == null || relevantProxies.isEmpty())
			return false;
		return relevantProxies.stream().filter(Objects::nonNull).allMatch(this::isExcluded);
	}

	// --- Persistence Methods (Unchanged, but will save new ProxyStats structure) ---

	private synchronized void loadStatsFromFile() {
		if (statsFilePath == null) {
			Log.warn("Cannot load stats: Stats file path is not set.");
			return;
		}
		if (!Files.exists(statsFilePath)) {
			Log.info("Stats file not found at " + statsFilePath + ". Starting with empty statistics.");
			return;
		}
		Log.info("Loading proxy stats from: " + statsFilePath);
		Type mapType = new TypeToken<Map<String, ProxyStats>>() {
		}.getType();
		try (FileReader reader = new FileReader(statsFilePath.toFile())) {
			Map<String, ProxyStats> loadedStats = gson.fromJson(reader, mapType);
			if (loadedStats != null) {
				statsMap.clear();
				statsMap.putAll(loadedStats);
				Log.info("Successfully loaded " + statsMap.size() + " proxy stat entries.");
			} else {
				statsMap.clear();
				Log.info("Stats file was empty or contained null.");
			}
			dirty.set(false);
		} catch (IOException | com.google.gson.JsonSyntaxException e) {
			Log.error(e, "Failed to load proxy stats from file: " + statsFilePath);
			statsMap.clear();
		}
	}

	/**
	 * Starts a background task that periodically saves proxy statistics to disk if any changes have occurred. This method should only be called once during the application's
	 * lifecycle.
	 *
	 * @param interval The interval between save attempts.
	 * @param unit     The time unit for the interval.
	 */
	public synchronized void startPeriodicPersistence(long interval, TimeUnit unit) {
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.scheduleAtFixedRate(this::saveStatsToFile, interval, interval, unit);
			Log.info("Started periodic proxy stats persistence...");
		} else {
			Log.warn("Could not start periodic persistence, scheduler is unavailable or already shut down.");
		}
	}

	/**
	 * The internal worker method for saving proxy statistics to the configured JSON file. It only performs the write operation if changes have occurred (i.e., if the 'dirty' flag
	 * is set).
	 * <p>
	 * This method is designed to be called exclusively by the class's own lifecycle methods, such as the periodic persistence scheduler or the final shutdown sequence. It is
	 * thread-safe and robust against write failures.
	 *
	 * @see #startPeriodicPersistence(long, TimeUnit)
	 * @see #stopPersistence()
	 */
	private synchronized void saveStatsToFile() {
		if (statsFilePath == null) {
			Log.warn("Cannot save stats: Stats file path is not set.");
			return;
		}

		// Atomically check if the state is dirty and reset the flag.
		// If it wasn't dirty, exit immediately to prevent unnecessary disk I/O.
		if (!dirty.compareAndSet(true, false)) {
			return;
		}

		Map<String, ProxyStats> statsToSave = Map.copyOf(statsMap);
		Log.info("Saving " + statsToSave.size() + " proxy stat entries to: " + statsFilePath);

		try (FileWriter writer = new FileWriter(statsFilePath.toFile())) {
			gson.toJson(statsToSave, writer);
			Log.info("Proxy stats saved successfully.");
		} catch (IOException | RuntimeException e) {
			Log.error(e, "Failed to save proxy stats to file: " + statsFilePath);
			// If saving fails, mark the data as dirty again to ensure a re-attempt.
			dirty.set(true);
		}
	}

	/**
	 * Halts the periodic persistence of proxy statistics.
	 * <p>
	 * This method's primary responsibility is to ensure that any pending, unsaved statistics (marked as "dirty") are written to disk one final time.
	 * <p>
	 * It now shuts down the internal `ScheduledExecutorService` as it is no longer managed globally.
	 *
	 * @see #startPeriodicPersistence(long, TimeUnit)
	 */
	public synchronized void stopPersistence() {
		Log.info("Stopping proxy stats persistence...");

		// Perform one final save to ensure any pending changes are written to disk.
		saveStatsToFile();

		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdown(); // Initiate a graceful shutdown
			try {
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) { // Wait for tasks to complete
					scheduler.shutdownNow(); // Force shutdown if not terminated gracefully
					Log.warn("Forcibly shut down proxy stats scheduler.");
				}
			} catch (InterruptedException e) {
				scheduler.shutdownNow();
				Thread.currentThread().interrupt(); // Preserve interrupt status
				Log.warn("Proxy stats scheduler shutdown interrupted.");
			}
		}

		Log.info("Proxy stats persistence stopped (final save executed).");
	}

	// --- Helper and Management Methods (Unchanged logic, but operate on new stats structure) ---

	public Map<String, ProxyStats> getAllStats() {
		return Collections.unmodifiableMap(statsMap);
	}

	/**
	 * Calculates the number of proxies in the given list that are currently NOT excluded. This is used by the task controllers to detect proxy exhaustion early and prevent "error
	 * floods".
	 *
	 * @param proxies The list of proxies to check.
	 * @return The count of usable (non-excluded) proxies.
	 */
	public int getAvailableProxyCount(List<ProxyConfig> proxies) {
		if (proxies == null || proxies.isEmpty()) {
			return 0;
		}
		return (int) proxies.stream().filter(Objects::nonNull).filter(p -> !isExcluded(p)).count();
	}

	public synchronized void resetStatsForProxy(ProxyConfig config) {
		if (config == null)
			return;
		String proxyId = config.toStringSimple();
		if (statsMap.containsKey(proxyId)) {
			statsMap.put(proxyId, new ProxyStats(proxyId));
			dirty.set(true);
			Log.info("Reset statistics for proxy: " + proxyId);
		}
	}

	public synchronized void resetAllStats() {
		statsMap.clear();
		dirty.set(true);
		Log.info("All proxy statistics have been reset.");
	}

	public synchronized void cleanupOrphanedStats(Iterable<ProxyConfig> currentProxyConfigs) {
		if (currentProxyConfigs == null) {
			Log.warn("Cannot cleanup orphaned stats: currentProxyConfigs is null.");
			return;
		}
		java.util.Set<String> validProxyIds = new java.util.HashSet<>();
		for (ProxyConfig config : currentProxyConfigs) {
			if (config != null) {
				validProxyIds.add(config.toStringSimple());
			}
		}
		int removedCount = 0;
		var iterator = statsMap.keySet().iterator();
		while (iterator.hasNext()) {
			String statsId = iterator.next();
			if (!validProxyIds.contains(statsId)) {
				iterator.remove();
				removedCount++;
				Log.fine("Removed orphaned stats entry for id: " + statsId);
			}
		}
		if (removedCount > 0) {
			dirty.set(true);
			Log.info("Cleaned up " + removedCount + " orphaned proxy stats entries.");
		}
	}
}