package utils.network.strategy;

import utils.localize.LangMap;

/**
 * Defines the specific strategies for how a list of proxies is used when a rotation strategy is active.
 */
public enum ProxyUsageStrategy {

	/** (Recommended) Intelligently picks from a pool of the best proxies. */
	WEIGHTED_RANDOM("proxy.strategy.weighted", "Weighted (Recommended)"),

	/** Goes through the list from top to bottom and repeats. */
	SEQUENTIAL("proxy.strategy.sequential", "Sequential"),

	/** Always starts with the best proxy, fails over to the next on error. */
	FAILOVER("proxy.strategy.failover", "Failover"),

	/** (For Testing) Uses the worst-performing proxy first to diagnose issues. */
	TEST_WORST_FIRST("proxy.strategy.testWorstFirst", "Test (Worst First)");

	private final String langKey;
	private final String fallbackText;

	ProxyUsageStrategy(String langKey, String fallbackText) {
		this.langKey = langKey;
		this.fallbackText = fallbackText;
	}

	@Override
	public String toString() {
		return LangMap.getLangString(langKey, fallbackText);
	}
}