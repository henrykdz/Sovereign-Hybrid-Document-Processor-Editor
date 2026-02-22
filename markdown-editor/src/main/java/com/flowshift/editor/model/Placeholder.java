package com.flowshift.editor.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Sovereign Placeholder Registry v7.7.6. Defines all dynamic data points, using highly minimized keys for YAML and user interaction.
 */
public enum Placeholder {
    // --- 1. AUTOMATISIERTE LAUFZEIT-WERTE ---
	DATE("date"),
	TIME("time"),
	RANDOM_ID("randomId"),
	WORD_COUNT("wordCount"),
	READING_TIME("readingTime"),

    // --- 2. IDENTITÄT & BRANDING (Minimal Keys) ---
    // Minimalistische Keys erleichtern die Eingabe im YAML und in der Forge.
	COMPANY_NAME("company"),
	LOGO_URL("logo"),
	AUTHOR_NAME("author"),
	DEPARTMENT("dept"),
	LEGAL_NOTE("legal"),

    // --- 3. DOKUMENT-STRUKTUR (Minimal Keys) ---
	DOCUMENT_TITLE("title"),
	FORMAT("format"),
	PAGINATION("paginate"), // 'paginate' ist kürzer und klarer als 'pagination'
	VERSION("version"),
	STATUS("status"),
	HEADER_STYLE("header"),

    // --- 4. PHYSISCHES LAYOUT (Minimal Keys) ---
    // Vier-Buchstaben-Kürzel sind präzise und kurz.
	MARGIN_TOP("mTop"),
	MARGIN_BOTTOM("mBot"),
	MARGIN_LEFT("mLeft"),
	MARGIN_RIGHT("mRight");

	private final String key;

	Placeholder(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	/**
	 * Gives the placeholder tag in {{key}} format.
	 */
	public String getTag() {
		return "{{" + key + "}}";
	}

	/**
	 * Attempts to convert a raw string key (e.g., from YAML) into an Enum.
	 */
	public static Optional<Placeholder> fromKey(String key) {
		return Arrays.stream(values()).filter(p -> p.key.equalsIgnoreCase(key)).findFirst();
	}
}