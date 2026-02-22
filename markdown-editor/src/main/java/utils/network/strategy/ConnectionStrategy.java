package utils.network.strategy;

import utils.localize.LangMap;

public enum ConnectionStrategy {
	DIRECT("strategy.direct"), // Direkte Verbindung
	SYSTEM_PROXY("strategy.system"), // System-Proxy verwenden (Optional)
	USE_STANDARD_PROXY("strategy.standard"), // Den unten ausgewählten Standard-Proxy nutzen
//	ROTATION("strategy.rotation"), // Rotation aus Proxy-Liste
	ROTATION_WITH_FALLBACK("strategy.rotationFallback"); // Rotation + Fallback

	private final String langMapKey;

	ConnectionStrategy(String langMapKey) {
		this.langMapKey = langMapKey;
	}

	public String getLangMapKey() {
		return langMapKey;
	}

	@Override
	public String toString() {
		// Damit die ChoiceBox die übersetzten Namen anzeigt
		return LangMap.getLangString(this.langMapKey);
	}
}
