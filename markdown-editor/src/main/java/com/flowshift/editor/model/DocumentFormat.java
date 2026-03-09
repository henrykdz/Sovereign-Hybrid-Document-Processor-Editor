package com.flowshift.editor;

/**
 * The Sovereign Registry for Document Formats. This enum defines the physical and behavioral properties of all supported document types. It serves as the single source of truth
 * for geometry, visual aid rules, and pagination presets.
 */
public enum DocumentFormat {

    // --- ISO/DIN A-Series (International) ---
	A0("A0 Poster", "841mm", "1189mm", true, true),
	A1("A1 Poster", "594mm", "841mm", true, true),
	A2("A2 Poster", "420mm", "594mm", true, true),
	A3("A3", "297mm", "420mm", true, true),
	A4_PORTRAIT("A4 Portrait", "210mm", "297mm", true, true),
	A4_LANDSCAPE("A4 Landscape", "297mm", "210mm", true, true),
	A5_PORTRAIT("A5 Portrait", "148mm", "210mm", true, true),
	A5_LANDSCAPE("A5 Landscape", "210mm", "148mm", true, true),
	A6("A6", "105mm", "148mm", true, false),

    // --- US/ANSI Standards (North America) ---
	LETTER_US("US Letter", "215.9mm", "279.4mm", true, true),
	LEGAL_US("US Legal", "215.9mm", "355.6mm", true, true),
	TABLOID_US("US Tabloid", "279.4mm", "431.8mm", true, true),
	EXECUTIVE_US("US Executive", "184.15mm", "266.7mm", true, false),

    // --- Special Formats ---
	BUSINESS_CARD_EU("Business Card (EU)", "85mm", "55mm", true, false),
	BUSINESS_CARD_US("Business Card (US)", "89mm", "51mm", true, false),
	CREDIT_CARD_ID1("Credit Card (ID-1)", "85.6mm", "53.98mm", true, false),
	SQUARE_SOCIAL("Social Media Square", "200mm", "200mm", false, false),

    // --- Web-Native Formats ---
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
		boolean isStandard = baseName.equals("A0") || baseName.equals("A1") || baseName.equals("A2") || 
		                    baseName.equals("A3") || baseName.equals("A4") || baseName.equals("A5") || 
		                    baseName.equals("A6") || baseName.equals("LETTER") || baseName.equals("LEGAL") || 
		                    baseName.equals("TABLOID") || baseName.equals("EXECUTIVE");
		this.pageSize = isStandard ? baseName.toLowerCase() : width + " " + height;
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
		case A0, A1, A2, A3, A4_PORTRAIT, A4_LANDSCAPE, A5_PORTRAIT, A5_LANDSCAPE, A6 -> javafx.print.Paper.A4;
		case LETTER_US   -> javafx.print.Paper.NA_LETTER;
		case LEGAL_US    -> javafx.print.Paper.LEGAL;
		default          -> javafx.print.Paper.A4;
		};
	}

	/**
	 * Determines the correct JavaFX {@link javafx.print.PageOrientation} for this format.
	 */
	public javafx.print.PageOrientation getJavaFxOrientation() {
		if (this.name().contains("LANDSCAPE")) {
			return javafx.print.PageOrientation.LANDSCAPE;
		}
		return javafx.print.PageOrientation.PORTRAIT;
	}
}