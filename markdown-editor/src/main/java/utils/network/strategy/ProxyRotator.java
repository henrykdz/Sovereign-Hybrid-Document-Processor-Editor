package utils.network.strategy;

import utils.logging.Log;

/**
 * Provides simple round-robin logic for proxy rotation. Maintains a static index to cycle through a list of proxies. This implementation is basic and thread-safe for index access
 * but does not handle complex scenarios like temporarily unavailable proxies.
 */
public final class ProxyRotator {

	private static volatile int nextProxyIndex = 0;            // volatile for visibility across threads
	private static final Object lock           = new Object(); // Simple lock for atomic increment

	/** Private constructor to prevent instantiation. */
	private ProxyRotator() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Gets the next index to use for proxy rotation in a round-robin fashion. This method is thread-safe.
	 *
	 * @param listSize The total number of available proxies in the list.
	 * @return The index (0 to listSize-1) of the next proxy to try. Returns 0 if listSize is non-positive.
	 */
	public static int getNextProxyIndex(int listSize) {
		if (listSize <= 0) {
			return 0; // Or throw exception? Return 0 avoids errors but might hide issues.
		}

		int indexToReturn;
		synchronized (lock) { // Ensure atomic read and update of nextProxyIndex
			indexToReturn = nextProxyIndex;
			nextProxyIndex = (nextProxyIndex + 1) % listSize; // Increment and wrap around
		}
		// Log.fine("ProxyRotator: Next index is " + indexToReturn + " (list size " + listSize + ")");
		return indexToReturn;
	}

	/**
	 * Resets the rotation index back to 0. Might be useful if you want to restart the rotation sequence explicitly.
	 */
	public static void resetRotation() {
		synchronized (lock) {
			Log.info("Resetting proxy rotation index to 0.");
			nextProxyIndex = 0;
		}
	}

}