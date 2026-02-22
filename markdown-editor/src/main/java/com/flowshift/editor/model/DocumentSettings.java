package com.flowshift.editor.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.flowshift.editor.DocumentFormat;

/**
 * DocumentSettings v7.7.3 - The Unified Document DNA. Nutzt das Placeholder-Enum als Single Source of Truth für Keys und Reihenfolge.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentSettings {

	public static final double DEFAULT_MARGIN_MM = 15.0; // Der "professionelle" Standard für Papier

	// --- 1. PHYSISCHES LAYOUT (Neutral-Defaults) ---
	private DocumentFormat format       = DocumentFormat.WEB_WIDE; // Startet als Web-Rolle
	public double          marginTop    = 0.0;                     // Keine Ränder
	public double          marginBottom = 0.0;
	public double          marginLeft   = 0.0;
	public double          marginRight  = 0.0;
	public boolean         isPaginated  = false;

	// --- 2. DOKUMENT-IDENTITÄT (Leer) ---
	public String documentTitle = "Untitled Document";
	public String authorName    = "";
	public String department    = "";
	public String version       = "";
	public String status        = "";
	public String legalNote     = "";

	// --- 3. BRANDING & THEME (Deaktiviert) ---
	public String companyName       = "";
	public String logoUrl           = "";
	public String activeHeaderStyle = "NONE";               // Kein Header-Template
	public String activeTheme       = "THEME_DARK_CRIMSON"; // Editor-Thema bleibt App-Sache
	public String profileName       = "Default";

	public Map<String, String> customPlaceholders = new LinkedHashMap<>();

	@JsonIgnore
	private transient String  headerHtml  = "";
	@JsonIgnore
	private transient boolean persistYaml = false;

	public DocumentSettings() {
		this.customPlaceholders = new LinkedHashMap<>();
	}

	public DocumentSettings(DocumentSettings other) {
		if (other != null) {
			this.format = other.format;
			this.isPaginated = other.isPaginated;
			this.marginTop = other.marginTop;
			this.marginBottom = other.marginBottom;
			this.marginLeft = other.marginLeft;
			this.marginRight = other.marginRight;
			this.documentTitle = other.documentTitle;
			this.authorName = other.authorName;
			this.department = other.department;
			this.version = other.version;
			this.status = other.status;
			this.legalNote = other.legalNote;
			this.companyName = other.companyName;
			this.logoUrl = other.logoUrl;
			this.activeHeaderStyle = other.activeHeaderStyle;
			this.activeTheme = other.activeTheme;
			this.profileName = other.profileName;
			this.headerHtml = other.headerHtml;
			this.persistYaml = other.persistYaml;

			this.customPlaceholders = (other.customPlaceholders != null) ? new LinkedHashMap<>(other.customPlaceholders) : new LinkedHashMap<>();
		}

	}

	/**
	 * Generiert eine Map für den YAML-Export. Nutzt die natürliche Reihenfolge des Placeholder-Enums für Konsistenz.
	 */
	public Map<String, String> asMetaMap() {
		Map<String, String> meta = new LinkedHashMap<>();

		// Wir nutzen die strikte Reihenfolge des Enums für den Export
		addToMapIfValid(meta, Placeholder.FORMAT.getKey(), format != null ? format.name() : null);
		addToMapIfValid(meta, Placeholder.PAGINATION.getKey(), String.valueOf(isPaginated));
		addToMapIfValid(meta, Placeholder.DOCUMENT_TITLE.getKey(), documentTitle);
		addToMapIfValid(meta, Placeholder.HEADER_STYLE.getKey(), activeHeaderStyle);
		addToMapIfValid(meta, Placeholder.AUTHOR_NAME.getKey(), authorName);
		addToMapIfValid(meta, Placeholder.VERSION.getKey(), version);
		addToMapIfValid(meta, Placeholder.STATUS.getKey(), status);
		addToMapIfValid(meta, Placeholder.COMPANY_NAME.getKey(), companyName);
		addToMapIfValid(meta, Placeholder.DEPARTMENT.getKey(), department);
		addToMapIfValid(meta, Placeholder.LOGO_URL.getKey(), logoUrl);
		addToMapIfValid(meta, Placeholder.LEGAL_NOTE.getKey(), legalNote);

		// Technische Keys (MARGINS) werden nur gespeichert, wenn sie NICHT dem DEFAULT entsprechen
		// Wir prüfen gegen 0.0 statt gegen DEFAULT_MARGIN_MM ---
		addToMapIfValid(meta, Placeholder.MARGIN_TOP.getKey(), (marginTop == 0.0) ? null : String.valueOf((int) marginTop));
		addToMapIfValid(meta, Placeholder.MARGIN_BOTTOM.getKey(), (marginBottom == 0.0) ? null : String.valueOf((int) marginBottom));
		addToMapIfValid(meta, Placeholder.MARGIN_LEFT.getKey(), (marginLeft == 0.0) ? null : String.valueOf((int) marginLeft));
		addToMapIfValid(meta, Placeholder.MARGIN_RIGHT.getKey(), (marginRight == 0.0) ? null : String.valueOf((int) marginRight));

		// --- NEU: CUSTOM PLACEHOLDERS ZUSAMMENFÜHREN (Am Ende) ---
		if (customPlaceholders != null) {
			customPlaceholders.forEach((k, v) -> {
				// Sicherstellen, dass nur gültige Werte und keine Standard-Keys überschrieben werden
				if (Placeholder.fromKey(k).isEmpty()) {
					addToMapIfValid(meta, k, v);
				}
			});
		}

		return meta;
	}

	private void addToMapIfValid(Map<String, String> map, String key, String value) {
		if (value != null && !value.trim().isEmpty() && !"INHERIT".equals(value)) {
			map.put(key, value);
		}
	}

	// --- GETTER & SETTER ---

	public DocumentFormat getFormat() {
		return format;
	}

	public void setFormat(DocumentFormat format) {
		this.format = (format == null) ? DocumentFormat.A4_PORTRAIT : format;
	}

	public String getDocumentTitle() {
		return documentTitle;
	}

	public void setDocumentTitle(String title) {
		this.documentTitle = title;
	}

	public double getMarginTop() {
		return marginTop;
	}

	public void setMarginTop(double val) {
		this.marginTop = val;
	}

	public double getMarginBottom() {
		return marginBottom;
	}

	public void setMarginBottom(double val) {
		this.marginBottom = val;
	}

	public double getMarginLeft() {
		return marginLeft;
	}

	public void setMarginLeft(double val) {
		this.marginLeft = val;
	}

	public double getMarginRight() {
		return marginRight;
	}

	public void setMarginRight(double val) {
		this.marginRight = val;
	}

	public String getAuthorName() {
		return authorName;
	}

	public void setAuthorName(String name) {
		this.authorName = name;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String dept) {
		this.department = dept;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getLegalNote() {
		return legalNote;
	}

	public void setLegalNote(String note) {
		this.legalNote = note;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String name) {
		this.companyName = name;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public void setLogoUrl(String url) {
		this.logoUrl = url;
	}

	public String getActiveTheme() {
		return activeTheme;
	}

	public void setActiveTheme(String theme) {
		this.activeTheme = theme;
	}

	public String getActiveHeaderStyle() {
		return activeHeaderStyle;
	}

	public void setActiveHeaderStyle(String style) {
		this.activeHeaderStyle = style;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String name) {
		this.profileName = name;
	}

	public String getHeaderHtml() {
		return headerHtml;
	}

	public void setHeaderHtml(String html) {
		this.headerHtml = (html == null) ? "" : html;
	}

	public boolean isPersistYaml() {
		return persistYaml;
	}

	public void setPersistYaml(boolean persist) {
		this.persistYaml = persist;
	}

	public boolean isPaginated() {
		return isPaginated;
	}

	public void setPaginated(boolean enableAutoPagination) {
		this.isPaginated = enableAutoPagination;
	}

	public Map<String, String> getCustomPlaceholders() {
		return customPlaceholders;
	}

	public void setCustomPlaceholders(Map<String, String> customPlaceholders) {
		this.customPlaceholders = (customPlaceholders == null) ? new LinkedHashMap<>() : customPlaceholders;
	}
}