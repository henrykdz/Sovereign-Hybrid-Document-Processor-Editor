package utils.network.proxy;

import java.util.concurrent.TimeUnit;

import utils.general.StringUtils;

/**
 * Holds and manages performance statistics for a specific proxy. This includes counts for successful and failed attempts, faults directly attributable to the proxy, consecutive
 * proxy faults, total request time, timestamps, and error messages.
 * <p>
 * Instances are typically identified by a unique {@code proxyId}. All public methods that modify or access statistical data are synchronized.
 */
public class ProxyStats {

	private final String proxyId;

	private long successCount           = 0;
	private long failureCount           = 0; // Total number of all failed attempts (includes proxy faults and target issues)
	private long proxyFaultCount        = 0; // Counts only failures directly attributable to the proxy
	private long consecutiveProxyFaults = 0; // Counts consecutive failures directly attributable to the proxy
	private long totalRequestTimeMillis = 0;

	private long   lastUsedTimestamp       = 0;
	private long   lastSuccessTimestamp    = 0;
	private long   lastFailureTimestamp    = 0;    // Timestamp of the last *any* type of failure
	private long   lastProxyFaultTimestamp = 0;    // Timestamp of the last failure directly attributable to the proxy
	private String lastError               = null; // Last recorded error message (any type)
	private String lastProxyFaultError     = null; // Last recorded error message for a direct proxy fault

	public ProxyStats(String proxyId) {
		if (StringUtils.isBlank(proxyId)) {
			throw new IllegalArgumentException("proxyId cannot be blank");
		}
		this.proxyId = proxyId;
	}

	/**
	 * Records a successful attempt for this proxy. Increments success count, adds request time, updates timestamps, and resets the consecutive proxy fault count.
	 *
	 * @param requestTimeMillis Duration of the successful request.
	 */
	public synchronized void recordSuccess(long requestTimeMillis) {
		if (requestTimeMillis < 0) {
			requestTimeMillis = 0;
		}
		successCount++;
		totalRequestTimeMillis += requestTimeMillis;
		lastUsedTimestamp = System.currentTimeMillis();
		lastSuccessTimestamp = lastUsedTimestamp;
		consecutiveProxyFaults = 0; // WICHTIG: Ein Erfolg setzt die Zählung zurück.
	}

	/**
	 * Records a failure that is directly attributable to the proxy itself (e.g., connection refused TO proxy, proxy timeout, proxy auth error). This type of failure increments the
	 * consecutive proxy fault counter.
	 *
	 * @param errorMessage A brief description of the proxy fault.
	 */
	public synchronized void recordProxyFault(String errorMessage) {
		failureCount++; // Zählt als genereller Fehlschlag
		proxyFaultCount++; // Zählt als direkter Proxy-Fehler
		consecutiveProxyFaults++; // Erhöht die Zählung der aufeinanderfolgenden Proxy-Fehler
		lastUsedTimestamp = System.currentTimeMillis();
		lastFailureTimestamp = lastUsedTimestamp;
		lastProxyFaultTimestamp = lastUsedTimestamp;
		this.lastError = StringUtils.truncate(errorMessage, 255);
		this.lastProxyFaultError = this.lastError;
	}

	/**
	 * Records an issue where the proxy successfully communicated with the target, but the target responded with an error (e.g., HTTP 403 from target, SSL error with target,
	 * connection refused BY target). This type of issue increments the total failure count but does NOT increment the consecutive proxy fault counter.
	 *
	 * @param errorMessage A brief description of the target issue.
	 */
	public synchronized void recordTargetIssueViaProxy(String errorMessage) {
		failureCount++; // Zählt als genereller Fehlschlag
		// proxyFaultCount wird NICHT erhöht
		// consecutiveProxyFaults wird NICHT erhöht

		lastUsedTimestamp = System.currentTimeMillis();
		lastFailureTimestamp = lastUsedTimestamp;
		this.lastError = StringUtils.truncate(errorMessage, 255);
		// lastProxyFaultTimestamp und lastProxyFaultError werden NICHT aktualisiert

		// WICHTIG: Ein Ziel-Fehler setzt consecutiveProxyFaults NICHT zurück.
		// Nur ein echter Erfolg (recordSuccess) tut das.
	}

	/**
	 * Resets the counter for consecutive proxy faults to zero. This is typically called by {@link #recordSuccess(long)} or can be called manually if needed (e.g., user action).
	 */
	public synchronized void resetConsecutiveProxyFaults() {
		this.consecutiveProxyFaults = 0;
	}

	// --- Accessor Methods (Getter) ---

	public synchronized String getProxyId() {
		return proxyId;
	}

	public synchronized long getSuccessCount() {
		return successCount;
	}

	/** Gets the total number of all failed attempts (proxy faults + target issues). */
	public synchronized long getFailureCount() {
		return failureCount;
	}

	/** Gets the number of failures directly attributable to the proxy. */
	public synchronized long getProxyFaultCount() {
		return proxyFaultCount;
	}

	/**
	 * Returns the current number of consecutive faults directly attributable to this proxy since the last successful attempt or since these faults were last reset. This is the
	 * primary value used for determining proxy exclusion.
	 */
	public synchronized long getConsecutiveProxyFaults() {
		return consecutiveProxyFaults;
	}

	public synchronized long getTotalRequests() {
		return successCount + failureCount;
	}

	public synchronized double getSuccessRate() {
		long total = getTotalRequests();
		return (total == 0) ? 0.0 : (double) successCount / total;
	}

	public synchronized long getAverageResponseTimeMillis() {
		return (successCount == 0) ? 0 : totalRequestTimeMillis / successCount;
	}

	public synchronized long getLastUsedTimestamp() {
		return lastUsedTimestamp;
	}

	public synchronized long getLastSuccessTimestamp() {
		return lastSuccessTimestamp;
	}

	/** Gets the timestamp of the last failure of any type. */
	public synchronized long getLastFailureTimestamp() {
		return lastFailureTimestamp;
	}

	/** Gets the timestamp of the last failure directly attributable to the proxy. */
	public synchronized long getLastProxyFaultTimestamp() {
		return lastProxyFaultTimestamp;
	}

	/** Gets the last recorded error message of any type. */
	public synchronized String getLastError() {
		return lastError;
	}

	/** Gets the last recorded error message for a direct proxy fault. */
	public synchronized String getLastProxyFaultError() {
		return lastProxyFaultError;
	}

	@Override
	public synchronized String toString() {
		long now = System.currentTimeMillis();
		String lastUsedStr = (lastUsedTimestamp == 0) ? "never" : formatTimeAgo(now - lastUsedTimestamp);
		String lastSuccessStr = (lastSuccessTimestamp == 0) ? "never" : formatTimeAgo(now - lastSuccessTimestamp);
		String lastFailureStr = (lastFailureTimestamp == 0) ? "never" : formatTimeAgo(now - lastFailureTimestamp);
		String lastProxyFaultStr = (lastProxyFaultTimestamp == 0) ? "never" : formatTimeAgo(now - lastProxyFaultTimestamp);

		return String.format("ProxyStats[id=%s, S:%d, F_total:%d, PF_total:%d, CPF:%d, SR:%.1f%%, AvgT:%dms, LU:%s, LS:%s, LF:%s, LPF:%s, LastErr:'%s', LastPFErr:'%s']", proxyId,
		        successCount, failureCount, // Total failures
		        proxyFaultCount, // Direct proxy faults
		        consecutiveProxyFaults, // Consecutive direct proxy faults
		        getSuccessRate() * 100.0, getAverageResponseTimeMillis(), lastUsedStr, lastSuccessStr, lastFailureStr, // Last any failure
		        lastProxyFaultStr, // Last proxy fault
		        StringUtils.truncate(lastError, 50), StringUtils.truncate(lastProxyFaultError, 50));
	}

	private String formatTimeAgo(long millisAgo) {
		if (millisAgo < 0)
			return "in the future?";
		if (millisAgo < 1000)
			return millisAgo + "ms ago";
		long seconds = TimeUnit.MILLISECONDS.toSeconds(millisAgo);
		if (seconds < 60)
			return seconds + "s ago";
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millisAgo);
		if (minutes < 60)
			return minutes + "m ago";
		long hours = TimeUnit.MILLISECONDS.toHours(millisAgo);
		return hours + "h ago";
	}

	public synchronized void setLastProxyFaultTimestamp(long timestamp) {
		this.lastProxyFaultTimestamp = timestamp;
	}
}