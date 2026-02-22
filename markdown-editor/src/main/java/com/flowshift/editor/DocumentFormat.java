package com.flowshift.editor;

/**
 * The Sovereign Registry for Document Formats. This enum defines the physical and behavioral properties of all supported document types. It serves as the single source of truth
 * for geometry, visual aid rules, and pagination presets.
 */
public enum DocumentFormat {

    // --- PRINT-NATIVE FORMATS (Physical paper or PDF) ---
    // LABEL WIDTH HEIGHT IS_PRINT SUGGEST_PAGINATION
	A4_PORTRAIT("A4 Portrait", "210mm", "297mm", true, true),
	A5_PORTRAIT("A5 Portrait", "148mm", "210mm", true, true),
	LETTER_US("US Letter", "215.9mm", "279.4mm", true, true),
	BUSINESS_CARD_EU("Business Card (EU)", "85mm", "55mm", true, false), // Single page
	CREDIT_CARD_ID1("Credit Card (ID-1)", "85.6mm", "53.98mm", true, false), // Single page
	SQUARE_SOCIAL("Social Media Square", "200mm", "200mm", false, false),
    // --- WEB-NATIVE FORMATS (Digital flow) ---
    // LABEL WIDTH HEIGHT IS_PRINT SUGGEST_PAGINATION EXPLICIT_PAGESIZE
	WEB_WIDE("Web (Wide)", "100%", "auto", false, false, "auto"),
	WEB_READING("Web (Reading)", "800px", "auto", false, false, "auto");

	// --- ARCHITECTURAL PROPERTIES ---

	/** A human-readable label for UI components. */
	public final String label;

	/** The CSS width of the document container. */
	public final String width;

	/** The CSS height (or min-height) of the document container. */
	public final String height;

	/**
	 * Is this format primarily intended for physical paper or PDF output? This sovereign flag controls the availability of print-specific UI elements (e.g., pagination toggle) and
	 * visual aids (e.g., page shadows).
	 */
	public final boolean isPaper;

	/**
	 * Should the UI suggest enabling auto-pagination for this format by default? This is a non-binding preset for the user, typically true for multi-page documents.
	 */
	public final boolean suggestPagination;

	/** The technical value for the CSS {@code @page { size: ... }} rule. */
	public final String pageSize;

	// --- CONSTRUCTORS ---

	/**
	 * Standard constructor for print-native formats. Automatically derives the {@code pageSize} from the format's name or dimensions.
	 */
	DocumentFormat(String label, String width, String height, boolean isPaperFormat, boolean suggestPagination) {
		this.label = label;
		this.width = width;
		this.height = height;
		this.isPaper = isPaperFormat;
		this.suggestPagination = suggestPagination;

		// Logic: Standard ISO/US formats use their name (a4, letter), custom ones use dimensions.
		String baseName = this.name().split("_")[0];
		this.pageSize = (baseName.equals("A4") || baseName.equals("A5") || baseName.equals("LETTER")) ? baseName.toLowerCase() : width + " " + height;
	}

	/**
	 * Specialized constructor for web-native or custom formats that require an explicit {@code pageSize}.
	 */
	DocumentFormat(String label, String width, String height, boolean isPrintFormat, boolean suggestPagination, String pageSize) {
		this.label = label;
		this.width = width;
		this.height = height;
		this.isPaper = isPrintFormat;
		this.suggestPagination = suggestPagination;
		this.pageSize = pageSize;
	}

	// --- UTILITY METHODS ---

	@Override
	public String toString() {
		return label;
	}

	/**
	 * Safely converts a string key (e.g., from YAML) into a DocumentFormat enum. Falls back to A4_PORTRAIT if the key is null or invalid.
	 */
	public static DocumentFormat fromString(String text) {
		if (text == null)
			return A4_PORTRAIT;
		for (DocumentFormat b : DocumentFormat.values()) {
			if (b.name().equalsIgnoreCase(text))
				return b;
		}
		return A4_PORTRAIT;
	}

	/**
	 * Extracts the numeric height in millimeters for calculation purposes. Falls back to a safe default for non-numeric heights (e.g., "auto").
	 */
	public double heightInMm() {
		if (height == null || !height.endsWith("mm")) {
			return 297.0; // Safe fallback to A4 height
		}
		try {
			return Double.parseDouble(height.replace("mm", "").trim());
		} catch (NumberFormatException e) {
			return 297.0; // Emergency fallback
		}
	}

	// --- JAVAFX PRINT API BRIDGE ---

	/**
	 * Translates this format into the corresponding JavaFX {@link javafx.print.Paper} object. Falls back to A4 for custom sizes, as the true dimensions are enforced by CSS
	 * {@code @page}.
	 */
	public javafx.print.Paper getJavaFxPaper() {
		return switch (this) {
		case A4_PORTRAIT -> javafx.print.Paper.A4;
		case A5_PORTRAIT -> javafx.print.Paper.A5;
		case LETTER_US   -> javafx.print.Paper.NA_LETTER;
		default          -> javafx.print.Paper.A4;
		};
	}

	/**
	 * Determines the correct JavaFX {@link javafx.print.PageOrientation} for this format.
	 */
	public javafx.print.PageOrientation getJavaFxOrientation() {
		// Future logic can be added here for _LANDSCAPE formats.
		return javafx.print.PageOrientation.PORTRAIT;
	}
}