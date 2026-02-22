package com.flowshift.editor;

/**
 * An enum to manage all user-facing strings in the UI. This centralizes all text for easy maintenance and future internationalization.
 */
public enum UIText {
    // Menu Texts
	MENU_FILE("File", null),
	MENU_EDIT("Edit", null),
	MENU_VIEW("View", null),

    // Menu Items
	MENU_NEW("New", "Create new document (Ctrl+N)"),
    // Menü-Items schärfen
	MENU_OPEN("Open...", "Open an existing Markdown file (Ctrl+O)"),
	MENU_FORMAT("Format Document", "Auto-format Markdown, Tables and HTML (Ctrl+Shift+F)"),

	MENU_SAVE("Save", "Save file (Ctrl+S)"),
	MENU_SAVE_AS("Save As...", "Save as new file"),
	MENU_EXPORT_HTML("Export HTML...", "Export as HTML file"),
	MENU_PRINT("Print...", "Print document (Ctrl+P)"),
//	MENU_EXIT("Exit", "Safely close the editor"),

	MENU_UNDO("Undo", "Undo last action (Ctrl+Z)"),
	MENU_REDO("Redo", "Redo last action (Ctrl+Y)"),
	MENU_CUT("Cut", "Cut selection (Ctrl+X)"),
	MENU_COPY("Copy", "Copy selection (Ctrl+C)"),
	MENU_PASTE("Paste", "Paste from clipboard (Ctrl+V)"),
	MENU_SELECT_ALL("Select All", "Select all text (Ctrl+A)"),

	MENU_TOGGLE_PREVIEW("Toggle Preview", "Show/hide preview (Ctrl+E)"),
	MENU_TOGGLE_WORD_WRAP("Toggle Word Wrap", "Toggle word wrapping"),
	MENU_TOGGLE_DARK_MODE("Toggle Dark Mode", "Switch theme (Ctrl+D)"),
    // --- Top Toolbar ---
	OPEN("Open", "Open File (Ctrl+O)"),
	NEW_DOCUMENT("New", "New Document (Ctrl+N)"),
	NEW_DOCUMENT_CONFIRM("New Document", "Unsaved Changes", "Do you want to save the current document?"),
	SAVE("Save", "Save File (Ctrl+S)"),
	SAVE_AS("Save As...", "Save as new file"),
    // Im UIText Enum hinzufügen:
	SETTINGS_BUTTON(" Settings", "Open Project and Document Configuration"),
	EXPORT_HTML("Export HTML", "Export as HTML file"),
    // Im UIText Enum hinzufügen:
	PRINT("Print", "Print document (Ctrl+P)"),
	FIND(null, "Find (Ctrl+F)"),

    // --- Sidebar: Icon-Only Buttons (Tooltip only) ---
	UNDO(null, "Undo (Ctrl+Z)"),
	REDO(null, "Redo (Ctrl+Y)"),
	BOLD(null, "Bold"),
	ITALIC(null, "Italic"),
	STRIKETHROUGH(null, "Strikethrough"),
	INLINE_CODE(null, "Inline Code"),
	BULLET_LIST(null, "Bullet List"),
	ORDERED_LIST(null, "Ordered List"),
	TASK_LIST(null, "Task List"),
	HORIZONTAL_RULE(null, "Horizontal Rule"),
	INSERT_LINK(null, "Insert Link"),
	INSERT_IMAGE(null, "Insert Image"),
	BLOCKQUOTE(null, "Blockquote"),
	CODE_BLOCK(null, "Code Block"),

    // --- Sidebar: Buttons with Text ---
	HEADINGS(" H1-3", "Headings"),
    // In MarkdownEditorController.java -> Enum UIText hinzufügen:
	PLACEHOLDER_MENU(null, "Insert Dynamic Placeholder (Stamp)"),
    // In MarkdownEditorController.java -> Enum UIText
	INSERT_PAGE_BREAK(null, "Insert Page Break (New page in PDF)"),
    // Nur diese beiden Einträge:
	WORD_WRAP_ON(" Wrap", "Word wrap: ON - Click to turn OFF"),
	WORD_WRAP_OFF(" Off", "Word wrap: OFF - Click to turn ON"),
	VIEW_TOGGLE(" View", "Toggle Preview"),
	DARK_MODE_DARK(" Dark", "Switch to Light Mode"),
	DARK_MODE_LIGHT(" Light", "Switch to Dark Mode"),
    // NEU: Der Design-Modus-Schalter
	DESIGN_TOGGLE(" Design", "Open Sovereign Design Nexus (CSS)"),

    // --- Status Bar ---
	UNTITLED("Untitled", null),
	UNSAVED_SUFFIX(" (unsaved)", null),
	WORDS_PREFIX("Words: ", null),
	CHARS_PREFIX("Chars: ", null),
	LINE_PREFIX("Ln ", null),
	COLUMN_PREFIX(", Col ", null),

    // --- Dialogs (Title, Header, Content) ---
	INSERT_LINK_DIALOG("Insert Link", null, "URL:"),
	INSERT_IMAGE_DIALOG("Insert Image", null, "Image URL or Path:"),
	EXIT_CONFIRM_DIALOG("Unsaved Changes", "You have unsaved changes.", "Do you want to save them before closing?"),

	SAVE_AND_NEW("Save & New", null),
	DISCARD_AND_NEW("Discard & New", null),

    // --- Dialog Buttons ---
	SAVE_AND_EXIT("Save & Exit", null),
	DONT_SAVE("Don't Save", null),
	CANCEL("Cancel", null),
    // NEU: Konstanten für den FileChooser
	FILE_CHOOSER_SAVE_TITLE("Save Markdown File", null),
	FILE_CHOOSER_OPEN_TITLE("Open Markdown File", null),
	FILE_CHOOSER_EXPORT_TITLE("Export HTML File", null),
	FILE_CHOOSER_FILTER_MD("Markdown Files (*.md, *.txt)", null),
	FILE_CHOOSER_FILTER_HTML("HTML Files (*.html)", null),
	FILE_CHOOSER_FILTER_ALL("All Files (*.*)", null),

	EXPORT_SUCCESS("Export successful", "HTML file exported successfully"),
	EXPORT_ERROR("Export error", "Failed to export HTML file"),

	SAVE_SUCCESS_FEEDBACK("Saved", null),
	SAVE_SUCCESS("Save Successful", "File saved successfully", null),

	SAVE_ERROR("Save Error", "Failed to save file", null),
	LOAD_ERROR("Load Error", "Failed to load file", null),
	DELETE_CONFIRM("Delete File", "Are you sure you want to delete this file?", "This action cannot be undone."),

    // --- Context Menu Items ---
	CUT("Cut\tCtrl+X", "Cut (Ctrl+X)"),
	COPY("Copy\tCtrl+C", "Copy (Ctrl+C)"),
	PASTE("Paste\tCtrl+V", "Paste (Ctrl+V)"),
	SELECT_ALL("Select All\tCtrl+A", "Select All (Ctrl+A)"),

    // Falls du Print Preview machst:
	PRINT_PREVIEW("Print Preview", "Preview before printing"),
    // Im UIText Enum hinzufügen:
	PRINT_EMPTY("Cannot Print", "The document is empty", "There is no content to be printed."), // Aber für einfachen Druck reicht PRINT

    // --- Template Manager ---
	TEMPLATE_BUTTON(" Library", "Open Template Library to load or save blueprints"),
	TEMPLATE_NOT_FOUND_TITLE("No Templates Found", "No templates available", "Please add .md files to the .flowshift/templates folder."),
	TEMPLATE_CHOICE_DIALOG("Select Template", "Choose a Blueprint", "Available Templates:"),

    // Header ist fest, Content ist ein Format-String für .formatted()
	TEMPLATE_CONFLICT_DIALOG("Choose Action", "The current document is not empty.", "How do you want to apply the template '%s'?"),

    // Template Action Buttons
	BTN_TEMPLATE_OVERWRITE("New Document (Overwrite)", "Discards current text and starts fresh from template"),
	BTN_TEMPLATE_INSERT("Insert at Cursor (Snippet)", "Inserts the template text at the current cursor position"),
	// Inside UIText enum
//	BTN_OVERWRITE("New Document", "Replace entire content"),
//	BTN_INSERT_AT_CARET("Insert Snippet", "Insert at current cursor position"),
//	
	FIELD_DEFAULT_FORMAT("Default Document Format:", null),

    // Im Bereich der Dialog-Texte (UIText Enum)

    // Fehler beim Versuch, ohne geladene Datei auf Settings zuzugreifen
	CANNOT_EDIT_SETTINGS("Project Settings", "Cannot edit settings", "Please open a Markdown file first to define the project directory."),

    // Felder für den Project Settings Dialog (die wir jetzt benötigen)
	DIALOG_PROJECT_SETTINGS_TITLE("Project Settings", null),
	DIALOG_PROJECT_SETTINGS_HEADER("Branding Configuration", null),
	FIELD_COMPANY_NAME("Company Name:", null),

	FIELD_LOGO_URL("Logo URL/Path:", null),
	FIELD_APPLY_BRANDING("Apply Branding (Default):", null),
	FIELD_DEFAULT_THEME("Default Editor Theme:", null),
	FIELD_CUSTOM_HEADER_HTML("Default Header HTML (Editable Template):", null),
	// Beispiel: Irgendwo in der Mitte der Enum
	FILE_CHOOSER_IMPORT_BUNDLE_TITLE("Import FlowShift Rich Bundle", null),
	// Beispiel: Irgendwo in der Mitte der Enum
	FILE_CHOOSER_EXPORT_BUNDLE_TITLE("Export FlowShift Rich Bundle", null);

	private final String label;
	private final String tooltip;
	private final String title;
	private final String header;
	private final String content;

	// Constructor for buttons (label and/or tooltip)
	UIText(String label, String tooltip) {
		this.label = label;
		this.tooltip = tooltip;
		this.title = null;
		this.header = null;
		this.content = null;
	}

	// Constructor for dialogs
	UIText(String title, String header, String content) {
		this.label = null;
		this.tooltip = null;
		this.title = title;
		this.header = header;
		this.content = content;
	}

	public String getLabel() {
		return label;
	}

	public String getTooltip() {
		return tooltip;
	}

	public String getTitle() {
		return title;
	}

	public String getHeader() {
		return header;
	}

	public String getContent() {
		return content;
	}
}