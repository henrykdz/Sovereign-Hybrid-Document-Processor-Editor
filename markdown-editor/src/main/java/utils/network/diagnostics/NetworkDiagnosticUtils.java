package utils.network.diagnostics;


import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection; // Still needed for ConnectionResult constants
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import utils.general.StringUtils;
import utils.logging.Log;
import utils.network.proxy.ProxyConfig;

/**
 * Provides static utility methods for performing basic network diagnostics. This includes checking host reachability (ICMP/TCP Echo), direct TCP port connectivity, and testing
 * proxy server functionality (both pinging the proxy and testing connectivity through it).
 * <p>
 * This class uses {@link ConnectionResult} to return outcomes for most operations, but specific methods like {@link #checkTcpPortConnection(String, int, int)} and related proxy
 * checks may throw {@link ConnectionCancelledException} if interrupted. It uses {@link Log} for detailed logging.
 * <p>
 * Note: This class primarily uses low-level socket operations. It relies on external classes like {@code ProxyConfig}, {@code ConnectionResult}, {@code Log}, and
 * {@code StringUtils}.
 *
 * @see ConnectionResult
 * @see ConnectionCancelledException
 * @see ProxyConfig
 * @see Log
 */
public final class NetworkDiagnosticUtils {

	// --- Constants ---

	/** Default connection timeout in milliseconds used if no specific timeout is provided or if the provided value is invalid. */
	private static final int DEFAULT_TIMEOUT_MS = 6000; // 6 seconds

	/** Lower bound for timeout values before issuing a warning. */
	private static final int LOW_TIMEOUT_WARNING_MS  = 400;
	/** Upper bound for timeout values before issuing a warning. */
	private static final int HIGH_TIMEOUT_WARNING_MS = 20000;

	/** Default target hosts for checking connectivity *through* a proxy server. */
	public static final List<String> DEFAULT_PROXY_TARGET_HOSTS  = List.of("www.google.com", "www.cloudflare.com", "one.one.one.one");
	/** Default target port (HTTP) used when checking connectivity *through* a proxy server to the default hosts. */
	private static final int         DEFAULT_PROXY_TARGET_PORT   = 80;
	/** Default minimum number of successful connections required when testing proxy connectivity against multiple targets. */
	public static final int          DEFAULT_MIN_PROXY_SUCCESSES = 1;

	/** Internal variable holding the effective default timeout, can be changed via setter. */
	private static volatile int effectiveDefaultTimeout = DEFAULT_TIMEOUT_MS; // Added volatile for visibility if changed concurrently

	/** Private constructor to prevent instantiation of this utility class. */
	private NetworkDiagnosticUtils() {
		throw new IllegalStateException("Utility class cannot be instantiated.");
	}

	// --- Timeout Configuration ---

	/**
	 * Gets the currently configured default timeout in milliseconds for network diagnostic tests.
	 *
	 * @return The default timeout in milliseconds.
	 */
	public static int getDefaultConnectionTimeout() {
		return effectiveDefaultTimeout;
	}

	/**
	 * Sets the default timeout in milliseconds for network diagnostic tests. A warning is logged if the value is unusually low or high. The value is clamped to a minimum of 1ms.
	 *
	 * @param millis The desired default timeout in milliseconds.
	 */
	public static void setDefaultConnectionTimeout(int millis) {
		if (millis <= LOW_TIMEOUT_WARNING_MS || millis >= HIGH_TIMEOUT_WARNING_MS) {
			Log.warn("Setting default network timeout to an unusual value: %dms", millis);
		}
		// Ensure a minimum positive value
		effectiveDefaultTimeout = Math.max(1, millis);
		Log.info("Default network timeout set to: %dms", effectiveDefaultTimeout);
	}

	/**
	 * Resolves the timeout value to use for a diagnostic test. Returns the provided {@code specificTimeoutMillis} if it's positive, otherwise returns the
	 * {@link #getDefaultConnectionTimeout()}. Ensures the returned value is at least 1ms.
	 *
	 * @param specificTimeoutMillis The timeout specified for a particular test, or 0/negative to use the default.
	 * @return The effective timeout in milliseconds (always >= 1).
	 */
	private static int getEffectiveTimeout(int specificTimeoutMillis) {
		int timeout = (specificTimeoutMillis > 0) ? specificTimeoutMillis : getDefaultConnectionTimeout();
		return Math.max(1, timeout); // Ensure minimum 1ms
	}

	// --- Host Reachability (ICMP/Echo) ---

	/**
	 * Tests the reachability of a host using {@link InetAddress#isReachable(int)}. This typically uses an ICMP Echo Request or a TCP Echo, depending on OS and privileges. It
	 * checks if the host *itself* responds, not a specific service port. Requires appropriate system privileges for ICMP on some OS.
	 *
	 * @param host                  The hostname or IP address string to check. Must not be blank.
	 * @param specificTimeoutMillis The timeout in milliseconds for this check. Uses default if non-positive.
	 * @return A {@link ConnectionResult} indicating success ({@link HttpURLConnection#HTTP_OK}) or failure (-1). Note: This method does *not* throw
	 *         {@code ConnectionCancelledException} itself, as interruption of {@code isReachable} typically results in {@code IOException} or premature return.
	 */
	public static ConnectionResult checkHostReachability(String host, int specificTimeoutMillis) {
		if (StringUtils.isBlank(host)) {
			return ConnectionResult.error("Host cannot be blank", -1, new IllegalArgumentException("Host is blank"));
		}

		int timeout = getEffectiveTimeout(specificTimeoutMillis);
		long startTime = System.currentTimeMillis();
		Log.fine("Checking host reachability for: %s (Timeout: %dms)", host, timeout);

		try {
			InetAddress inetAddress = InetAddress.getByName(host);
			Log.debug("Resolved %s to %s", host, inetAddress.getHostAddress());

			// isReachable can be interrupted, often resulting in IOException or returning false quickly.
			// It doesn't reliably throw a specific exception indicating cancellation like Socket.connect does.
			boolean reachable = inetAddress.isReachable(timeout); // Can throw IOException, SecurityException

			long endTime = System.currentTimeMillis();
			long latency = endTime - startTime; // Note: Latency here might be misleading if interrupted

			if (reachable) {
				Log.fine("Host reachable: %s (Latency: %dms)", host, latency);
				return ConnectionResult.success("Host is reachable", latency);
			} else {
				// Could be unreachable or interrupted - cannot easily distinguish reliably here.
				Log.warn("Host determined unreachable: %s (within %dms timeout or possibly interrupted)", host, timeout);
				return ConnectionResult.failure("Host is unreachable or check interrupted", latency);
			}
		} catch (UnknownHostException e) {
			Log.warn("Host resolution failed for: %s", host);
			return ConnectionResult.error("Unknown Host: " + host, -1, e);
		} catch (SecurityException e) {
			Log.warn(e, "Security issue during reachability check for: %s (ICMP might require privileges)", host);
			String message = "Permission denied for reachability check";
			return ConnectionResult.error(message, -1, e);
		} catch (IOException e) {
			// Includes cases where interruption might cause an IOException
			Log.warn(e, "IOException during reachability check for: %s (could be network issue or interrupt)", host);
			String message = "IO Error during reachability check: " + e.getMessage();
			return ConnectionResult.error(message, -1, e);
		} catch (Exception e) { // Catch unexpected errors
			Log.error(e, "Unexpected error during reachability check for: %s", host);
			return ConnectionResult.error("Unexpected error: " + e.getMessage(), -1, e);
		}
	}

	// --- TCP Port Connection Test ---

	/**
	 * Attempts to establish a direct TCP socket connection to the specified host and port. Does *not* use any proxy. Checks if a specific service is accepting connections.
	 * <p>
	 * Handles success, timeout, connection refused, unknown host, and cancellation via interrupt.
	 *
	 * @param host                  Hostname or IP address. Must not be blank.
	 * @param port                  Port number (1-65535).
	 * @param specificTimeoutMillis Connection timeout (ms). Uses default if non-positive.
	 * @return A {@link ConnectionResult} indicating success or failure (timeout, refused, config error).
	 * @throws ConnectionCancelledException if the connection attempt was cancelled by an interrupt.
	 * @throws IOException                  if a fundamental, unhandled I/O error occurs.
	 */
	public static ConnectionResult checkTcpPortConnection(String host, int port, int specificTimeoutMillis) throws ConnectionCancelledException, IOException {

		// --- Input Validation (Returns ConnectionResult.error) ---
		if (StringUtils.isBlank(host)) {
			return ConnectionResult.error("Host cannot be blank", -1, new IllegalArgumentException("Host is blank"));
		}
		if (port <= 0 || port > 65535) {
			return ConnectionResult.error("Invalid port number: " + port, -1, new IllegalArgumentException("Port number must be between 1 and 65535"));
		}

		int timeout = getEffectiveTimeout(specificTimeoutMillis);
		String target = host + ":" + port;
		Log.fine("Attempting direct TCP connection to: %s (Timeout: %dms)", target, timeout);

		// --- Resolve Hostname (Returns ConnectionResult.error on failure) ---
		InetSocketAddress socketAddress;
		try {
			socketAddress = new InetSocketAddress(host, port);
			if (socketAddress.isUnresolved()) {
				Log.warn("Host resolution failed for: %s", host);
				return ConnectionResult.error("Unknown Host: " + host, -1, new UnknownHostException("Failed to resolve host: " + host));
			}
			if (socketAddress.getAddress() != null)
				Log.debug("Resolved %s for port %d to: %s", host, port, socketAddress.getAddress().getHostAddress());
		} catch (IllegalArgumentException | SecurityException e) {
			Log.warn(e, "Error creating socket address for: %s", target);
			return ConnectionResult.error("Invalid address/port or security restriction: " + e.getMessage(), -1, e);
		}

		// --- Attempt Connection (Can throw ConnectionCancelledException/IOException or return ConnectionResult) ---
		try (Socket socket = new Socket()) {
			long startTime = System.currentTimeMillis();
			socket.connect(socketAddress, timeout); // Blocking call, interruptible
			long endTime = System.currentTimeMillis();
			long latency = endTime - startTime;
			Log.fine("TCP Connection successful to %s (Latency: %dms)", target, latency);
			return ConnectionResult.success("Connection successful", latency);
		} catch (SocketTimeoutException e) {
			Log.warn("TCP Connection timed out for %s after %dms: %s", target, timeout, e.getMessage());
			return ConnectionResult.error("Connection timed out (" + timeout + "ms)", -1, e);

		} catch (ConnectException e) {
			Log.warn("TCP Connection refused for %s: %s", target, e.getMessage());
			// Create a more specific error message
			String connectMsg = "TCP connection refused to " + target;
			if (e.getMessage() != null && !e.getMessage().toLowerCase().contains("refused")) {
				connectMsg += ": " + e.getMessage(); // Add original msg if different
			}
			return ConnectionResult.error(connectMsg, -1, e);

		} catch (SocketException e) {
			boolean looksLikeInterrupt = "Closed by interrupt".equals(e.getMessage()) || e.getCause() instanceof java.nio.channels.ClosedByInterruptException
			        || e.getCause() instanceof java.io.InterruptedIOException;

			if (looksLikeInterrupt && Thread.currentThread().isInterrupted()) { // <<< Zusätzliche Prüfung
				// Thread WAS interrupted, likely by user cancel(true)
				Log.info("Connection attempt to %s cancelled by interrupt.", target /* oder target + config */);
				throw new ConnectionCancelledException("Connection attempt cancelled by interrupt.", e);
			} else {
				// It's a different socket error OR it looked like interrupt but wasn't (e.g., protocol error)
				Log.warn("SocketException during connection to %s (Treating as error, not cancellation): %s", target /* oder target + config */, e.getMessage());
				// Gib einen Fehler zurück, KEINE ConnectionCancelledException werfen
				return ConnectionResult.error("Socket Error: " + e.getMessage(), -1, e);
			}

		} catch (IOException e) {
			Log.warn("IOException during TCP connection attempt to %s: %s", target, e.getMessage());
			// Could be network unreachable, permission denied (OS level), etc.
			return ConnectionResult.error("IOException during connection: " + e.getMessage(), -1, e); // Return error result
		}
		// No generic catch(Exception) - let unexpected runtime exceptions propagate if needed.
	}

	// --- Proxy Server Diagnostics ---

	/**
	 * Performs a basic "ping" to the proxy server *itself* by attempting a direct TCP socket connection to its configured host and port. Verifies address resolvability and port
	 * listening status. Does *not* test if the proxy can forward connections.
	 *
	 * @param config                The {@link ProxyConfig} describing the proxy server. Must not be null and contain valid host/port.
	 * @param specificTimeoutMillis Connection timeout (ms). Uses default if non-positive.
	 * @return A {@link ConnectionResult} indicating success or failure of the direct connection.
	 * @throws ConnectionCancelledException if the connection attempt was cancelled by an interrupt.
	 * @throws IOException                  if a fundamental, unhandled I/O error occurs.
	 */
	public static ConnectionResult pingProxyServer(ProxyConfig config, int specificTimeoutMillis) throws ConnectionCancelledException, IOException {
		Objects.requireNonNull(config, "ProxyConfig cannot be null for ping operation.");
		if (StringUtils.isBlank(config.getHost()) || config.getPort() <= 0 || config.getPort() > 65535) {
			return ConnectionResult.error("Invalid host or port in ProxyConfig for ping", -1,
			        new IllegalArgumentException("ProxyConfig has invalid host/port: " + config.getHost() + ":" + config.getPort()));
		}
		int timeout = getEffectiveTimeout(specificTimeoutMillis);
		Log.fine("Pinging proxy server address: %s:%d with timeout: %dms", config.getHost(), config.getPort(), timeout);
		// Delegate to checkTcpPortConnection; exceptions will propagate.
		return checkTcpPortConnection(config.getHost(), config.getPort(), timeout);
	}

	/**
	 * Overload without callback for backward compatibility or when no callback is needed. Calls the detailed checkProxyServiceConnectivity method with a null hostAttemptCallback.
	 *
	 * @param config                The ProxyConfig to test.
	 * @param specificTimeoutMillis Connection timeout (ms) per attempt.
	 * @param targetHosts           List of target hosts.
	 * @param requiredSuccesses     Minimum successful connections needed.
	 * @return A ConnectionResult.
	 * @throws ConnectionCancelledException if interrupted.
	 * @throws IOException                  on unhandled I/O errors.
	 * @throws IllegalArgumentException     if requiredSuccesses < 1.
	 */
	public static ConnectionResult checkProxyServiceConnectivity(ProxyConfig config, int specificTimeoutMillis, List<String> targetHosts, int requiredSuccesses)
	        throws ConnectionCancelledException, IOException, IllegalArgumentException {
		// Rufe die detaillierte Methode auf und übergebe null für den neuen Parameter
		return checkProxyServiceConnectivity(config, specificTimeoutMillis, targetHosts, requiredSuccesses, null /* hostAttemptCallback */);
	}

	/**
	 * Convenience overload using default targets, success count, and no callback. Calls the detailed checkProxyServiceConnectivity method with defaults and a null
	 * hostAttemptCallback.
	 *
	 * @param config                The ProxyConfig to test.
	 * @param specificTimeoutMillis Connection timeout (ms) per attempt.
	 * @return A ConnectionResult.
	 * @throws ConnectionCancelledException if interrupted.
	 * @throws IOException                  on unhandled I/O errors.
	 */
	public static ConnectionResult checkProxyServiceConnectivity(ProxyConfig config, int specificTimeoutMillis) throws ConnectionCancelledException, IOException {
		// Rufe die detaillierte Methode auf und übergebe null für den neuen Parameter
		return checkProxyServiceConnectivity(config, specificTimeoutMillis, DEFAULT_PROXY_TARGET_HOSTS, DEFAULT_MIN_PROXY_SUCCESSES, null /* hostAttemptCallback */);
	}

	/**
	 * Checks connectivity *through* a proxy server by attempting TCP connections to a list of target hosts. Requires a minimum number of successful connections. Iterates through
	 * targets, attempting connection via proxy. Handles success, timeout, refusal, unknown target host, config errors, and cancellation.
	 *
	 * @param config                The {@link ProxyConfig} to test. Must not be null and not {@link Proxy.Type#DIRECT}.
	 * @param specificTimeoutMillis Connection timeout (ms) for *each* attempt *through* the proxy. Uses default if non-positive.
	 * @param targetHosts           List of hostnames/IPs to try connecting to via proxy. Uses defaults if null/empty.
	 * @param requiredSuccesses     Minimum number of successful connections needed (>= 1).
	 * @param hostAttemptCallback   A Consumer that accepts the hostname being attempted *before* the connection is tried. Can be null.
	 * @return A {@link ConnectionResult} indicating overall success or failure, detailing the last encountered problem on failure.
	 * @throws ConnectionCancelledException if a connection attempt was cancelled by an interrupt.
	 * @throws IOException                  if a fundamental, unhandled I/O error occurs.
	 * @throws IllegalArgumentException     if {@code requiredSuccesses < 1}.
	 */
	public static ConnectionResult checkProxyServiceConnectivity(ProxyConfig config, int specificTimeoutMillis, List<String> targetHosts, int requiredSuccesses,
	        Consumer<String> hostAttemptCallback // <-- Der neue Parameter
	) throws ConnectionCancelledException, IOException, IllegalArgumentException {

		// --- Precondition and Proxy Validation ---
		Objects.requireNonNull(config, "ProxyConfig cannot be null for service connectivity check.");
		if (requiredSuccesses < 1) {
			Log.error("Required successful proxy checks must be at least 1, but was %d", requiredSuccesses);
			throw new IllegalArgumentException("Required successful checks must be at least 1.");
		}
		Proxy proxy = config.getProxy();
		if (proxy == null || proxy.type() == Proxy.Type.DIRECT) {
			Log.warn("Attempted proxy connectivity check with invalid or DIRECT proxy: %s", config);
			return ConnectionResult.error("Invalid or DIRECT proxy type in config.", -1, new IllegalArgumentException("Proxy type invalid or DIRECT: " + config));
		}
		if (proxy.address() == null || !(proxy.address() instanceof InetSocketAddress)) {
			Log.warn("Proxy address is invalid or not InetSocketAddress: %s", config);
			return ConnectionResult.error("Proxy address is not a valid InetSocketAddress.", -1, new IllegalArgumentException("Invalid proxy address: " + config));
		}
		String proxyAddrStr = proxy.address().toString();

		// --- Setup Targets and Timeout ---
		List<String> hostsToTest = (targetHosts == null || targetHosts.isEmpty()) ? DEFAULT_PROXY_TARGET_HOSTS : targetHosts;
		if (hostsToTest.isEmpty()) {
			Log.warn("No target hosts specified or defaulted for proxy service check for %s", config);
			return ConnectionResult.failure("No target hosts provided for proxy check");
		}
		int timeout = getEffectiveTimeout(specificTimeoutMillis);
		int successCount = 0;
		ConnectionResult lastResult = null; // Store the last significant result
		Log.info("Checking proxy service connectivity: %s targeting up to %d hosts (requires %d successes). Timeout per host: %dms", config, hostsToTest.size(), requiredSuccesses,
		        timeout);

		// --- Loop Through Target Hosts ---
		for (String targetHost : hostsToTest) {
			if (StringUtils.isBlank(targetHost)) {
				Log.warn("Skipping blank target host in list during proxy check for %s", config);
				continue;
			}
			String trimmedTargetHost = targetHost.trim();
			int targetPort = DEFAULT_PROXY_TARGET_PORT; // Assume default port
			String target = trimmedTargetHost + ":" + targetPort;
			InetSocketAddress targetAddress;

			// --- Execute the callback BEFORE trying to resolve/connect ---
			if (hostAttemptCallback != null) {
				try {
					hostAttemptCallback.accept(trimmedTargetHost); // Pass the host name
				} catch (Exception e) {
					Log.error(e, "Error executing hostAttemptCallback for host '%s'", trimmedTargetHost);
					// Decide whether to continue or abort? For now, just log and continue.
				}
			}
			// ------------------------------------------------------------

			// --- Resolve Target Host ---
			try {
				targetAddress = new InetSocketAddress(trimmedTargetHost, targetPort);
				if (targetAddress.isUnresolved()) {
					throw new UnknownHostException("Could not resolve target host: " + trimmedTargetHost);
				}
				if (targetAddress.getAddress() != null) {
					Log.debug("Proxy Check: Resolved target %s to %s", trimmedTargetHost, targetAddress.getAddress().getHostAddress());
				} else {
					// Should not happen if isUnresolved is false, but safety check
					Log.warn("Target address resolved but getAddress() returned null for %s", trimmedTargetHost);
					throw new UnknownHostException("Resolved target address was null for " + trimmedTargetHost);
				}
			} catch (Exception e) { // Catch UnknownHostException and potential SecurityException etc.
				Log.warn("Skipping proxy target %s due to resolution error: %s", target, e.getMessage());
				lastResult = ConnectionResult.error("Target host resolution failed: " + trimmedTargetHost, -1, e);
				continue; // Try next host in the list
			}

			Log.fine("Attempting connection via proxy %s to target: %s", config, target);

			// --- Attempt Connection Via Proxy ---
			try (Socket socket = new Socket(proxy)) { // Use the proxy specified in ProxyConfig
				long startTime = System.currentTimeMillis();
				socket.connect(targetAddress, timeout); // Blocking call, interruptible via thread interrupt
				long endTime = System.currentTimeMillis();
				long latency = endTime - startTime;

				successCount++;
				lastResult = ConnectionResult.success("Proxy connection successful to " + target, latency);
				Log.fine("Successfully reached target via proxy: %s (Success %d/%d, Latency: %dms)", target, successCount, requiredSuccesses, latency);

				// Check if the required number of successful connections has been met
				if (successCount >= requiredSuccesses) {
					Log.info("Proxy service connectivity check successful for %s. Reached required %d targets. Last success: %s", config, requiredSuccesses, target);
					return lastResult; // Early exit on overall success
				}
			} catch (SocketTimeoutException e) {
				Log.warn("Proxy connection timed out (%dms) to %s via %s: %s", timeout, target, config, e.getMessage());
				lastResult = ConnectionResult.error("Proxy connection timed out (" + timeout + "ms) to " + target, -1, e);
				// Continue loop to try the next target host
			} catch (ConnectException e) {
				Log.warn("Proxy connection refused to %s via %s: %s", target, config, e.getMessage());
				String connectMsg = "Proxy connection refused to " + target;
				if (e.getMessage() != null && !e.getMessage().toLowerCase().contains("refused")) {
					connectMsg += ": " + e.getMessage();
				}
				lastResult = ConnectionResult.error(connectMsg, -1, e);
				// Continue loop to try the next target host
			} catch (SocketException e) {
				// Check if this looks like an interruption caused by Task.cancel(true)
				boolean looksLikeInterrupt = "Closed by interrupt".equals(e.getMessage()) || e.getCause() instanceof java.nio.channels.ClosedByInterruptException
				        || e.getCause() instanceof java.io.InterruptedIOException;

				if (looksLikeInterrupt && Thread.currentThread().isInterrupted()) {
					Log.info("Proxy connection attempt to %s via %s cancelled by interrupt.", target, config);
					// Throw the specific exception to signal cancellation properly
					throw new ConnectionCancelledException("Proxy connection attempt cancelled by interrupt.", e);
				} else {
					// Treat other SocketExceptions as connection failures for this target
					Log.warn("SocketException during connection to %s via %s (Treating as Error): %s", target, config, e.getMessage());
					String errorMsg = "Socket Error via Proxy (" + proxyAddrStr + ") to " + target;
					if (e.getMessage() != null)
						errorMsg += ": " + e.getMessage();
					lastResult = ConnectionResult.error(errorMsg, -1, e);
					// Continue loop to try the next target host
				}
			} catch (IOException e) { // Catch other IOExceptions like network unreachable
				Log.warn("IOException during proxy connection to %s via %s: %s", target, config, e.getMessage());
				String errorMsg = "IOException via Proxy to " + target;
				if (e.getMessage() != null)
					errorMsg += ": " + e.getMessage();
				lastResult = ConnectionResult.error(errorMsg, -1, e);
				// Continue loop to try the next target host
			} catch (Exception e) { // Catch unexpected runtime errors for this specific target attempt
				Log.error(e, "Unexpected error during proxy connection to %s via %s", target, config);
				lastResult = ConnectionResult.error("Unexpected error via Proxy to " + target + ": " + e.getMessage(), -1, e);
				// Continue loop to try the next target host
			}
		} // --- End loop through target hosts ---

		// --- Final Result Determination ---
		// If loop finished without reaching required successes
		if (successCount < requiredSuccesses) {
			Log.warn("Proxy service connectivity check failed for %s: Required %d successes, but only achieved %d.", config, requiredSuccesses, successCount);
			// Return the last recorded error/failure result, or a generic failure if none was recorded (e.g., all hosts failed resolution)
			if (lastResult != null && !lastResult.isSuccessful()) {
				return lastResult;
			} else {
				// If lastResult was null or somehow successful despite not meeting requirement
				String finalMsg = String.format("Proxy check failed: Required %d successes, achieved %d. No specific error recorded for last attempt.", requiredSuccesses,
				        successCount);
				return ConnectionResult.failure(finalMsg);
			}
		} else {
			// This part should technically not be reached if the early exit works, but for robustness:
			Log.info("Proxy service check loop finished for %s after meeting requirement (defensive check). Returning last success result.", config);
			return Objects.requireNonNullElse(lastResult, ConnectionResult.success("Required successes met (defensive)", 0));
		}
	} // --- End checkProxyServiceConnectivity (detailed) ---

} // End of NetworkDiagnosticUtils class