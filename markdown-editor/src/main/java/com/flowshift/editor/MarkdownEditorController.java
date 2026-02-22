/*
 * =============================================================================
 * Project: FlowShift - The Sovereign Content Engine
 * Component: MarkdownLinter
 * 
 * Copyright (c) 2026 FlowShift. All rights reserved.
 * Author: Henryk Daniel Zschuppan
 *
 * This source code is proprietary and confidential. Unauthorized copying 
 * of this file, via any medium, is strictly prohibited.
 *
 * DESIGN PHILOSOPHY: High-performance, context-aware structural validation
 * utilizing a single-pass Oracle-Backtick-Protocol for real-time processing.
 * =============================================================================
 */

package com.flowshift.editor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.kordamp.ikonli.javafx.FontIcon;

import com.flowshift.editor.model.BundleManifest;
import com.flowshift.editor.model.DocumentSettings;
import com.flowshift.editor.model.Placeholder;
import com.flowshift.editor.ui.CodeAreaSearchPopup;
import com.flowshift.editor.ui.ErrorTooltipManager;
import com.flowshift.editor.ui.LinterLineNumberFactory;
import com.flowshift.editor.ui.SovereignPreviewSatellite;
import com.flowshift.editor.ui.dialog.LibraryActions;
import com.flowshift.editor.ui.dialog.ProjectSettingsDialog;
import com.flowshift.editor.ui.dialog.TemplateLibraryWindow;
import com.flowshift.editor.util.BundleService;
import com.flowshift.editor.util.CssThemeManager;
import com.flowshift.editor.util.HeaderTemplateManager;
import com.flowshift.editor.util.MarkdownFormatter;
import com.flowshift.editor.util.MarkdownHighlighter;
import com.flowshift.editor.util.MarkdownLinter;
import com.flowshift.editor.util.MarkdownTemplateManager;
import com.flowshift.editor.util.ProjectSettingsManager;
import com.flowshift.editor.util.SnapshotService;
import com.flowshift.editor.webview.SovereignNavigator;
import com.flowshift.editor.webview.SovereignSourceMapper;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import netscape.javascript.JSObject;
import utils.FileOpeningService;
import utils.logging.Log;

/**
 * Controller für den Markdown Editor. Verwaltet die Sidebar-Logik, den RichTextFX Editor und die Vorschau.
 * 
 */
public class MarkdownEditorController implements Initializable, LibraryActions {

	public class SovereignBridge {
		public void onElementClicked(String tagName, String textContent, String selectorList, String openingTag, String uid, Object occurrence) {
			int occ = 0;
			try {
				occ = Integer.parseInt(occurrence.toString());
			} catch (Exception ignored) {
			}

			// Logge das vollständige Signal
			System.out.println("[BRIDGE-IN] UID: " + uid + " | Tag: " + tagName + " | Text: " + textContent);
			// 1. Navigation im Markdown-Quellcode (Jump to Source)
			MarkdownEditorController.this.smartJumpToSource(tagName, textContent, openingTag, uid, occ);

			// 2. DESIGN-NEXUS DIAGNOSE (Nur im Design-Modus aktiv)
			if (isDesignMode && selectorList != null && !selectorList.isEmpty()) {
				// Inventar aus dem Sanktuarium (StyleEditor) holen
				Set<String> defined = MarkdownLinter.extractDefinedClasses(styleEditor.getText());

				// Selektoren vom JS zerlegen (z.B. "div,#id,.class1,.class2")
				List<String> selectors = Arrays.asList(selectorList.split(","));
				List<String> missingClasses = new ArrayList<>();

				for (String s : selectors) {
					// Wenn es eine Klasse ist (.), prüfen, ob sie im Inventar fehlt
					if (s.startsWith(".") && !defined.contains(s.substring(1))) {
						missingClasses.add(s);
					}
				}

				// Delegiere die visuelle Aufbereitung
				MarkdownEditorController.this.inspectCssRules(selectors, missingClasses);
			}
		}

		public void onElementHovered(String tagName, String textContent) {
		}
	}

	// =============================================================================================
	// 1. KONSTANTEN & PREFERENCES
	// =============================================================================================

//	private static final String      EXPORT_HTML        = "export.html";
	private static final Preferences prefs              = Preferences.userNodeForPackage(MarkdownEditorController.class);
	private static final String      LAST_DIRECTORY_KEY = "lastUsedDirectory";

	private static final int ICON_SIZE = 14;

	// --- CONSTANTS: YAML & DOCUMENT PERSISTENCE ---
	/**
	 * Pattern to extract the entire YAML frontmatter block. Uses \A for start-of-file anchor for max performance. HEILUNG: Erlaubt führende Leerzeichen vor dem Start-Delimiter, um
	 * Konsistenz mit dem Linter herzustellen.
	 */
	private static final Pattern YAML_BLOCK_EXTRACT_PATTERN = Pattern.compile("\\A\\s*---\\s*\\n([\\s\\S]*?)\\n---"); // NEU: \\s* am Anfang

	/** Pattern to extract a single key-value pair from a YAML line. Supports keys with hyphens. */
	private static final Pattern YAML_FIELD_EXTRACT_PATTERN = Pattern.compile("^\\s*([\\w\\-]+):\\s*(.*)");

	/** Pattern to find and replace the entire YAML block in a document. */
	private static final Pattern YAML_REPLACE_PATTERN = Pattern.compile("\\A---[\\s\\S]*?\\n---[\\t ]*\\n?");

	// =============================================================================================
	// 2. FXML FIELDS: ROOT & CENTER
	// =============================================================================================

	// --- ROOT & CENTER ---
	@FXML
	private BorderPane rootPane;
	// --- NEU ---
	@FXML
	private SplitPane mainSplitPane;

	@FXML
	private StackPane editorContainer;  // Der Platzhalter für den programmatischen Editor
	@FXML
	private Pane      diagnosticsStripe;

	@FXML
	private HBox editorUnit; // Wieder da

	@FXML
	private StackPane previewContainer; // Der Container für die WebView
	@FXML
	private Button    undockButton;

	// =============================================================================================
	// FXML FIELDS: SOVEREIGN WORKSPACE (Design & Preview)
	// =============================================================================================
	@FXML
	private SplitPane leftSplitPane;

	@FXML
	private ToggleButton designModeButton;
	@FXML
	private VBox         designPane;
	@FXML
	private StackPane    styleEditorContainer;
	@FXML
	private Button       closeDesignPaneButton;
	@FXML
	private Label        activeContextLabel;
	@FXML
	private Button       suggestRuleButton;
	private String       lastSuggestedSelector = "";
	@FXML
	private WebView      webView;

	// ... (Rest deiner FXML-Felder für Sidebar, Toolbar etc. bleiben)
	// --- TOP Action Menu Bar (Datei Operationen) ---
	@FXML
	private Button    loadButton;
	@FXML
	private Button    newDocumentButton;
	@FXML
	private Button    saveButton;
	@FXML
	private StackPane saveButtonContainer; // <-- NEUER CONTAINER
	@FXML
	private Label     saveSuccessLabel;    // <-- NEUES LABEL
	@FXML
	private Button    saveAsButton;

	@FXML
	private Button projectSettingsButton; // Ersetzt ChoiceBox<DocumentFormat> formatChoiceBox
	@FXML
	private Button injectThemeButton;     // Das FXML-Feld
	@FXML
	private Button exportHtmlButton;
	@FXML
	private Button printButton;

	@FXML
	private Button searchButton;
	// 3. FXML FIELDS: SIDEBAR (Linke Werkzeugleiste)
	// =============================================================================================

	@FXML
	private VBox sideBar;

	@FXML
	private Button templateButton;

	// --- Sektion 1: Verlauf (Undo/Redo) ---
	@FXML
	private Button undoButton;
	@FXML
	private Button redoButton;

	// --- Sektion 2: Formatierung (Grid) ---
	@FXML
	private Button boldButton;
	@FXML
	private Button italicButton;
	@FXML
	private Button strikethroughButton;
	@FXML
	private Button inlineCodeButton;

	// --- Sektion 3: Struktur & Listen ---
	@FXML
	private MenuButton headingMenuButton;
	@FXML
	private Button     bulletListButton;
	@FXML
	private Button     orderedListButton;
	@FXML
	private Button     taskListButton;
	@FXML
	private Button     hrButton;

	// --- Sektion 4: Einfügen ---
	@FXML
	private Button     linkButton;
	@FXML
	private Button     imageButton;
	@FXML
	private Button     quoteButton;
	@FXML
	private Button     codeBlockButton;
	@FXML
	private MenuButton placeholderMenuButton;
	@FXML
	private Button     pageBreakButton;

	// --- Sektion 5: Ansicht (Unten) ---
	@FXML
	private ToggleButton wordWrapToggle;
	private boolean      wordWrapEnabled = false; // Standardmäßig aus

	@FXML
	private ToggleButton viewToggleButton;
	@FXML
	private Button       darkModeButton;

	// =============================================================================================
	// 4. FXML FIELDS: BOTTOM BAR (Statusleiste)
	// =============================================================================================

	@FXML
	private HBox  bottomBar;
	@FXML
	private Label formatStatusLabel;
	@FXML
	private Label errorCountLabel;
	@FXML
	private Label statusLabel;
	@FXML
	private Label wordCountLabel;
	@FXML
	private Label charCountLabel;
	@FXML
	private Label positionLabel;

	// =============================================================================================
	// 5. PROGRAMMATISCHE FELDER (Kein @FXML)
	// =============================================================================================
	// --- VIEWPORT INFRASTRUCTURE ---
	private CodeArea                        editor;
	/** The primary scroll container for the Markdown editor. */
	private VirtualizedScrollPane<CodeArea> codeAreaScrollPane;
	private CodeArea                        styleEditor;
	/** The dedicated scroll container for the CSS Forge. */
	private VirtualizedScrollPane<CodeArea> styleEditorScrollPane;

	// --- Logik Helfer ---
	private MarkdownHighlighter markdownHighlighter;
	private Parser              markdownParser;
	private HtmlRenderer        htmlRenderer;
	private Timeline            debouncerTimeline;  // Für den Haupt-Editor

	// --- Status & State Management ---
	private File    currentFile     = null;
	private boolean saveRequested   = false;
	private boolean isDarkMode      = false;
	private String  initialMarkdown = "";

	// --- SOVEREIGN PREVIEW ARCHITECTURE ---
	/** Dedicated satellite window component for managing the independent live preview stage. */
	private SovereignPreviewSatellite previewSatellite;

	/** Tracking flag to determine if the preview is currently decoupled from the primary editor window. */
	private boolean isUndocked = false;

	/** Persists the last user-defined divider position of the SplitPane to maintain layout consistency. */
	private double persistedPreviewRatio = 0.5;

	/** Logic flag representing the global visibility state of the live preview (Toggle ON/OFF). */
	private boolean isPreviewMode = false;

	/** Stye CSS Design Forge Mode **/
	private boolean isDesignMode = false;

	// Speichert die letzte vertikale Scroll-Position der WebView
	private double  lastWebViewScrollY     = 0;
	private boolean restoreScrollRequested = false; // NEU: Ein Flag, das den Scroll-Wunsch signalisiert
	private boolean forceWebViewReload     = false;
	// In den Member-Variablen eine Spur legen:
	private boolean lastPaginationState = false;

	private ProjectSettingsDialog   projectSettingsDialog;
	private TemplateLibraryWindow   libraryWindow;
	private MarkdownTemplateManager templateManager;
	private CssThemeManager         themeManager;

	/**
	 * Der atomare Datenträger für Design-Informationen (v8.7 Master). Speichert Offsets, den reinen CSS-Inhalt und den vollständigen HTML-Tag.
	 */
	private record StyleRange(int fullStart, // <style...
	        int fullEnd, // .../style>
	        int contentStart, // Start des CSS
	        int contentEnd, // Ende des CSS
	        String content, // Das reine CSS
	        String fullText // Der komplette Block inkl. Tags
	) {
	}

	private static final Pattern STYLE_BLOCK_DETECTOR   = Pattern.compile("<style[^>]*>", Pattern.CASE_INSENSITIVE);
	private static final Pattern STYLE_END_TAG_DETECTOR = Pattern.compile("</style>", Pattern.CASE_INSENSITIVE);

	// Status
	private File lastUsedDirectory = null;

	private ProjectSettingsManager settingsManager;
	private DocumentSettings       projectSettings; // Aktuell geladene Projekt-Defaults

	private LinterLineNumberFactory lineNumberFactory;

	private HeaderTemplateManager headerManager;

	private boolean isEditingTemplate;

	// 1. Instanziere die Brücke als festes Feld im Controller
	private final SovereignBridge       bridge       = new SovereignBridge();
	private final SovereignSourceMapper sourceMapper = new SovereignSourceMapper();
	private SovereignNavigator          navigator;

	// Sektion 5: Programmatische Felder
//	private ErrorTooltipManager errorTooltipManager;

	// Speichert die Editoren, die durch ein 'Cross-Editor-Undo' verlassen wurden
	private final Stack<CodeArea> redoActivityStack = new Stack<>();
	// Das chronologische Gedächtnis der Bearbeitungsorte
	private final Stack<CodeArea> activityStack = new Stack<>();

	// Cache für das kartografierte HTML, um unnötiges Re-Parsing bei CSS-Änderungen zu vermeiden
	private String cachedMappedHtml = null;

	private void ensureHeaderManager() {
		if (headerManager == null) {
			// Wir übergeben null oder einen Dummy, da der Manager nun fest auf user.dir schaut
			headerManager = new HeaderTemplateManager(null);
		}
	}

	private ErrorTooltipManager errorTooltipManager;

	// --- NEUE SUCH-ARCHITEKTUR (Gekapselt) ---
	private CodeAreaSearchPopup searchPopup; // Das visuelle Element

	// ...
	private final List<IndexRange> searchResults        = new ArrayList<>(); // BEHALTEN, da es die Highlighting-Logik braucht
	private int                    currentMatchIndex    = -1;                // BEHALTEN, für die Search Logik
	private List<IndexRange>       currentSearchMatches = new ArrayList<>(); // BEHALTEN, für die Highlighting-Logik

	// =============================================================================================
	// INITIALIZATION LOGIC (The Sovereign Boot Sequence)
	// =============================================================================================

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {

		// --- PHASE 1: FORGING ENGINES & SERVICES ---
		// Initialize core logic, parsers, and management services.
		this.settingsManager = new ProjectSettingsManager();
		this.themeManager = new CssThemeManager();
		this.templateManager = new MarkdownTemplateManager();
		setupFlexmark();

		// Highlighter must exist before the Editor logic attempts to call it.
		this.markdownHighlighter = new MarkdownHighlighter();

		// --- PHASE 2: CONSTRUCTING THE WORKSPACE (UI Components) ---
		// Programmatically build the Editor components to ensure correct initialization order.
		setupCodeEditorProgrammatically(); // Instantiates 'this.editor'
		setupStyleEditorProgrammatically(); // Instantiates 'this.styleEditor'

		// --- PHASE 3: INTER-MODULE HANDSHAKE ---
		// Establish the connection between the UI and the specialized logic modules.
		// CRITICAL: Must happen AFTER Phase 2 to prevent NullPointerExceptions.
		this.navigator = new SovereignNavigator(this.editor, this.sourceMapper);

		// --- PHASE 4: ESTABLISHING CONNECTIONS (Listeners & Firewalls) ---
		// Connect the WebView, event filters, and the Sovereign Security Firewall.
		setupWebView();
		setupListeners();
		setupAccelerators();
		setupSovereignFirewall();

		// --- PHASE 5: BOOTING SYSTEM CONTEXT ---
		// Load project-specific data and prepare the UI environment.
		loadProjectContext();
		setupActionBar();

		setupSearchPopupArchitecture();
		setupPlaceholderMenu();
		setupStructureControls();
		setupSidebarIcons();

		// --- PHASE 6: INITIALIZING CONTENT & PERSISTENCE STATE ---
		// Load initial markdown, perform first render, and mark the clean state.
		editor.replaceText(initialMarkdown);
		editor.moveTo(0);

		applyHighlightingInternal(true);
		renderMarkdownPreview();

		// Set initial "Clean State" for UndoManagers to track changes correctly.
		editor.getUndoManager().mark();
		styleEditor.getUndoManager().mark();

		initializeGUIStates();
		applyInitialThemeStyles();
		setupWindowEventHandlers();

		// --- PHASE 7: FINAL REACTIVITY ---
		// Activate dynamic UI observers like the Diagnostic Stripe responsiveness.
		diagnosticsStripe.heightProperty().addListener((obs, oldH, newH) -> updateDiagnosticsStripe());

		// FINAL STEP: Install diagnostic tooltips
		this.errorTooltipManager = ErrorTooltipManager.install(errorCountLabel, editor, markdownHighlighter, diagnosticsStripe);

		Log.info("Sovereign Initialization: System is ONLINE.");
	}

	private void initializeGUIStates() {
		updateSearchAvailability(false);

		// A. Panel einklappen
		designPane.setVisible(false);
		designPane.setManaged(false);

		// 1. SOUVERÄNER EINGRIFF: Entferne das Design-Panel aus dem Layout
		leftSplitPane.getItems().remove(designPane);

		// HEILUNG: Sicherstellen, dass der Design-Modus beim Start aus ist
		designModeButton.setSelected(false);
		isDesignMode = false;

		if (wordWrapToggle != null) {
			wordWrapToggle.setSelected(false);
			updateWordWrapButton();
		}

		// 2. SOUVERÄNER EINGRIFF: Entferne den previewContainer aus dem Haupt-SplitPane
		mainSplitPane.getItems().remove(previewContainer);

		// Initial-Zustand sicherstellen
		previewContainer.setVisible(false);
		previewContainer.setManaged(false);
		viewToggleButton.setSelected(false);
		isPreviewMode = false;
	}

	private void setupSovereignFirewall() {
		// 1. KEY_PRESSED: Schutz vor struktureller Zerstörung (Backspace, Delete, Enter, Paste)
		editor.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (!isDesignMode)
				return;

			StyleRange range = findStyleRange(editor.getText());
			if (range == null)
				return;

			int caret = editor.getCaretPosition();
			IndexRange selection = editor.getSelection();
			KeyCode code = e.getCode();

			// --- A. NAVIGATION & KOPIEREN (Immer erlaubt) ---
			if (code.isNavigationKey() || code.isModifierKey() || (code == KeyCode.C && e.isShortcutDown())) {
				return;
			}

			// --- B. DER RICHTUNGS-WÄCHTER (Heilung der Tags) ---

			// BACKSPACE: Blockiere, wenn der Cursor IM Block steht ODER direkt dahinter (Index == fullEnd)
			if (code == KeyCode.BACK_SPACE) {
				if (caret > range.fullStart() && caret <= range.fullEnd()) {
					e.consume();
					return;
				}
			}

			// DELETE: Blockiere, wenn der Cursor IM Block steht ODER direkt davor (Index == fullStart)
			if (code == KeyCode.DELETE) {
				if (caret >= range.fullStart() && caret < range.fullEnd()) {
					e.consume();
					return;
				}
			}

			// --- C. INTERSEKTIONS-SCHUTZ (Paste, Enter, Selektion) ---
			// Blockiere, wenn eine Markierung den Block berührt oder der Cursor darin steht
			boolean isTouching = selection.getStart() < range.fullEnd() && selection.getEnd() > range.fullStart();
			if (isTouching) {
				if (code == KeyCode.ENTER || code == KeyCode.TAB || code == KeyCode.V || code == KeyCode.X) {
					e.consume();
				}
			}
		});

		// 2. KEY_TYPED: Schutz vor Zeicheneingabe (A-Z, 0-9 etc.)
		editor.addEventFilter(KeyEvent.KEY_TYPED, e -> {
			if (!isDesignMode)
				return;
			StyleRange range = findStyleRange(editor.getText());
			if (range == null)
				return;

			int caret = editor.getCaretPosition();
			// Blockiere das Tippen, wenn der Cursor exakt auf oder zwischen den Tags steht
			if (caret >= range.fullStart() && caret < range.fullEnd()) {
				if (!e.isShortcutDown() && !e.isAltDown()) {
					e.consume();
				}
			}
		});
	}

	/**
	 * Prüft, ob die aktuelle Selektion oder der Cursor den Style-Block berührt. Dies ist die Basis für den lückenlosen Schutz (auch bei Paste-Aktionen).
	 */
	private boolean isSelectionInSanctuary() {
		// Nutzt den Live-Scanner aus v8.2
		StyleRange range = findStyleRange(editor.getText());
		if (range == null)
			return false;

		IndexRange selection = editor.getSelection();
		// Logik: Markierung beginnt vor dem Ende UND endet nach dem Start des Blocks.
		// Dies deckt auch den einfachen Cursor ab (Start == End).
		return selection.getStart() < range.fullEnd() && selection.getEnd() > range.fullStart();
	}

	/**
	 * Verknüpft den Schließen-Befehl des Fensters (X-Button) mit unserem zentralen Sicherheitswächter.
	 */
	private void setupWindowEventHandlers() {
		// Wir warten, bis das rootPane einer Szene hinzugefügt wird
		rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
			if (newScene != null) {
				// Wir warten, bis die Szene einem Fenster (Stage) hinzugefügt wird
				newScene.windowProperty().addListener((obsW, oldW, newW) -> {
					if (newW instanceof Stage stage) {
						// JETZT haben wir die Stage und können den Wächter aktivieren
						stage.setOnCloseRequest(event -> {
							if (!canDiscardCurrentDocument()) {
								// Der User will nicht verwerfen/abbrechen -> Schließen stoppen!
								event.consume();
							}
						});

						// Setze die primäre Stage als Standard-Besitzer für alle Dialoge in WindowUtils
						utils.ui.WindowUtils.setDefaultOwner(stage);

						Log.fine("Window Close-Guard and WindowUtils Owner successfully attached to Stage.");
					}
				});
			}
		});
	}

	/**
	 * Lädt den zentralen Projekteinstellungs-Kontext (aus user.dir) und initialisiert den Header-Buffer.
	 */
	private void loadProjectContext() {
		loadLastUsedDirectory(); // Letzte Benutzer-Aktivität (Dateipfad)

		// 1. VAULT muss existieren (nutzt intern user.dir)
		ensureDocumentVaultExists();

		// 2. Settings laden (ruft die neue, parameterlose Methode auf)
		try {
			this.projectSettings = settingsManager.loadSettings();

		} catch (Exception e) {
			Log.warn("Could not load central settings.json. Using defaults.");
			this.projectSettings = new DocumentSettings();
		}

		// 3. Header-Buffer füllen
		ensureHeaderManager();
		if (headerManager != null) {
			String styleName = projectSettings.getActiveHeaderStyle();
			projectSettings.setHeaderHtml(headerManager.loadTemplate(styleName));
		}
	}

	private void applyInitialThemeStyles() {
		if (isDarkMode) {
			rootPane.getStyleClass().add("dark-mode");
		}
		updateDarkModeButtonIcon();
		Platform.runLater(editor::requestFocus);
		updateStatusLabels();
	}

	/**
	 * Aktiviert oder deaktiviert die gesamte Suchfunktionalität.
	 * 
	 * @param isAvailable True, wenn ein Dokument zum Durchsuchen bereit ist.
	 */
	private void updateSearchAvailability(boolean isAvailable) {
		// Der Haupt-Button (Lupe), um die Suche zu öffnen
		searchButton.setDisable(!isAvailable);

		// Wenn die Suche nicht verfügbar ist, stellen wir sicher, dass das Popup
		// auch wirklich geschlossen ist, falls es offen war.
		if (!isAvailable && searchPopup != null && searchPopup.isShowing()) {
			searchPopup.hide();
		}
	}

	/**
	 * Erstellt den Haupt-Editor (v9.3 Master). Integriert Zeilennummern, Kontextmenü mit Sanktuarium-Schutz, und die Diagnose-Leiste in einer stabilen Overlay-Architektur.
	 */
	private void setupCodeEditorProgrammatically() {
		// 1. Instanziierung & Basis-Konfiguration
		editor = new CodeArea();
		editor.setWrapText(true);
		editor.getStyleClass().add("editor");
		editor.setMinWidth(300);

		// 2. Errichtung des Gutters (Zeilennummern & Fehler-Anzeige)
		// HEILUNG: 'false' als dritter Parameter -> Deaktiviert die internen Tooltips
		this.lineNumberFactory = new LinterLineNumberFactory(editor, idx -> LinterLineNumberFactory.findErrorAtLine(editor, markdownHighlighter.getLastErrors(), idx));
		// VERBINDUNG HERSTELLEN
		this.lineNumberFactory.setHoverHandler((node, line) -> {
		    // Wir brauchen Zugriff auf den Manager. Am besten speichern wir ihn als Feld im Controller.
		    if (this.errorTooltipManager != null) {
		        this.errorTooltipManager.showGutterTooltip(node, line);
		    }
		});
		
		editor.setParagraphGraphicFactory(lineNumberFactory);

		// Gutter-Synchronisation bei Cursor-Bewegung
		editor.currentParagraphProperty().addListener((obs, oldPar, newPar) -> {
			lineNumberFactory.updateCurrentLine(newPar.intValue());
		});
		Platform.runLater(() -> {
			lineNumberFactory.updateCurrentLine(editor.getCurrentParagraph());
		});

		// 3. Implementierung des souveränen Kontextmenüs
		ContextMenu contextMenu = new ContextMenu();
		MenuItem cutItem = new MenuItem(UIText.CUT.getLabel());
		cutItem.setOnAction(e -> editor.cut());
		MenuItem copyItem = new MenuItem(UIText.COPY.getLabel());
		copyItem.setOnAction(e -> editor.copy());
		MenuItem pasteItem = new MenuItem(UIText.PASTE.getLabel());
		pasteItem.setOnAction(e -> editor.paste());
		MenuItem selectAllItem = new MenuItem(UIText.SELECT_ALL.getLabel());
		selectAllItem.setOnAction(e -> editor.selectAll());

		contextMenu.getItems().addAll(cutItem, copyItem, pasteItem, new SeparatorMenuItem(), selectAllItem);

		// SANKTUARIUM-WÄCHTER für das Kontextmenü
		contextMenu.setOnShowing(e -> {
			boolean protect = isDesignMode && isSelectionInSanctuary();
			cutItem.setDisable(protect);
			pasteItem.setDisable(protect);
		});

		editor.setContextMenu(contextMenu);

		// 4. SOUVERÄNE GEOMETRIE-HEILUNG (Overlay-Architektur)
		this.codeAreaScrollPane = new VirtualizedScrollPane<>(editor);
		this.codeAreaScrollPane.setMinWidth(300);

		// Wir schaffen rechts 15px Platz für die schwebende Diagnose-Leiste
		codeAreaScrollPane.setStyle("-fx-padding: 0 15px 0 0;");

		// 5. Einbettung in den Container
		// Der ScrollPane (mit Zeilennummern) ist die unterste Schicht.
		// Die Diagnose-Leiste (aus dem FXML) schwebt darüber.
		editorContainer.getChildren().add(0, codeAreaScrollPane);
		editorContainer.setMinWidth(0);
	}

	/**
	 * Erstellt und konfiguriert das 'Souveräne Sanktuarium' (CSS-Editor). Integriert Gutter, Highlighting, Linter und Kontextmenü in einer hochperformanten, virtualisierten
	 * Umgebung.
	 */
	private void setupStyleEditorProgrammatically() {
		// 1. Instanziierung und Basis-Konfiguration
		styleEditor = new CodeArea();
		styleEditor.setMinWidth(300);
		styleEditor.getStyleClass().addAll("editor", "style-editor");

		// 2. Errichtung des Gutters (Zeilennummern & Fehler-Anzeige)
		// Wir binden die Fabrik exakt an diesen Editor und den CSS-Linter.
		LinterLineNumberFactory styleLineFactory = new LinterLineNumberFactory(styleEditor,
		        idx -> LinterLineNumberFactory.findErrorAtLine(styleEditor, MarkdownLinter.lintPureCss(styleEditor.getText()), idx));
		styleEditor.setParagraphGraphicFactory(styleLineFactory);

		// 3. Implementierung des Kontextmenüs (Klassische Editier-Souveränität)
		ContextMenu contextMenu = new ContextMenu();
		MenuItem cutItem = new MenuItem("Cut");
		cutItem.setOnAction(e -> styleEditor.cut());
		MenuItem copyItem = new MenuItem("Copy");
		copyItem.setOnAction(e -> styleEditor.copy());
		MenuItem pasteItem = new MenuItem("Paste");
		pasteItem.setOnAction(e -> styleEditor.paste()); // HEILUNG: Lambda genutzt
		MenuItem selAllItem = new MenuItem("Select All");
		selAllItem.setOnAction(e -> styleEditor.selectAll());

		contextMenu.getItems().addAll(cutItem, copyItem, pasteItem, new SeparatorMenuItem(), selAllItem);
		styleEditor.setContextMenu(contextMenu);

		// 3. REAKTIVES HIGHLIGHTING
		styleEditor.textProperty().addListener((obs, oldText, newText) -> {
			styleEditor.setStyleSpans(0, markdownHighlighter.highlightPureCss(newText, false));

			Platform.runLater(() -> {
				styleEditor.setStyleSpans(0, markdownHighlighter.highlightPureCss(newText, true));
				styleLineFactory.updateCurrentLine(styleEditor.getCurrentParagraph());
			});
		});

		// 5. Fokus-Synchronisation für das Gutter
		styleEditor.currentParagraphProperty().addListener((obs, old, newVal) -> {
			styleLineFactory.updateCurrentLine(newVal.intValue());
		});

		// 6. Virtualisierte Einbettung (Der Performance-Anker)
		this.styleEditorScrollPane = new VirtualizedScrollPane<>(styleEditor);
		this.styleEditorScrollPane.setMinWidth(300);
		this.styleEditorContainer.getChildren().setAll(this.styleEditorScrollPane); // Nutzt setAll für atomaren Austausch

		updateWordWrapButton();
	}

	private void setupListeners() {
		// 1. DER GLOBALE DEBOUNCER (Markdown Linter & Vorschau)

		debouncerTimeline = new Timeline(new KeyFrame(Duration.millis(300), e -> {
			// SOUVERÄNE DATEN-ERNEUERUNG
			String currentText = editor.getText();
			String currentCss = styleEditor.getText(); // Wir holen das aktuelle Design

			CompletableFuture.supplyAsync(() -> {
				java.util.Set<String> defined = MarkdownLinter.extractDefinedClasses(currentCss);
				defined.addAll(themeManager.getSystemClasses()); // SYSTEM-CLASSES HINZUFÜGEN

				Node document = markdownParser.parse(currentText);
				return markdownHighlighter.applyHighlighting(currentText, document, true, isDesignMode, -1, -1, currentSearchMatches, defined);
			}).thenAccept(spans -> {
				Platform.runLater(() -> {
					if (editor.getText().length() == spans.length()) {
						editor.setStyleSpans(0, spans);
						updateStatusLabels();
						refreshGutter();
						updateDiagnosticsStripe(); // Wichtig für die visuellen Cyan-Marker
						if (isPreviewMode)
							renderMarkdownPreview(); // this s the first delayed update and we do not update within applyHighlightInternal
					}
				});
			});
		}));

		debouncerTimeline.setCycleCount(1);

		// --- 2. SOUVERÄNE AKTIONS-KETTE (Chronologie-Management) ---

		// Initialisierung: Der Haupt-Editor ist der erste Anker
		if (activityStack.isEmpty())
			activityStack.push(editor);

		// PRIMARY TEXT LISTENER: Main Markdown Editor
		// Handles real-time reactivity, viewport stability, and state management.
		editor.textProperty().addListener((obs, oldText, newText) -> {
			// Invalidate the mapped HTML cache as the document structure has changed.
			cachedMappedHtml = null;

			// 1. SCROLL ANCHORING: Capture the current vertical scroll position before the update.
			// Utilizing getValue() from the Var<Double> object for RichTextFX 0.11.7 compatibility.
			final double currentEstimatedScrollY = codeAreaScrollPane.estimatedScrollYProperty().getValue();

			// 2. SEARCH INTEGRITY: Invalidate existing results as character offsets have shifted.
			if (!currentSearchMatches.isEmpty()) {
				currentSearchMatches.clear();
				searchResults.clear();
				if (searchPopup != null) {
					searchPopup.updateMatchCount(0, 0);
				}
			}

			// 3. CHRONOLOGY MANAGEMENT: Update the activity stack for global undo/redo tracking.
			if (activityStack.isEmpty() || activityStack.peek() != editor) {
				activityStack.push(editor);
			}
			// Clear the redo stack as a new primary action has occurred.
			redoActivityStack.clear();

			// 4. PERFORMANCE OPTIMIZATION: (Re)start the debouncer for resource-heavy tasks.
			// This orchestrates deep linter scanning and preview rendering once typing stops.
			debouncerTimeline.stop();
			debouncerTimeline.playFromStart();

			// 5. IMMEDIATE UI SYNCHRONIZATION: Update states that must react instantly.
			updateSearchAvailability(newText != null && !newText.isEmpty());
			updateDirtyState();
			updateUndoRedoButtonStates();

			// 6. VIEWPORT RESTORATION & FAST FEEDBACK: Execute on the JavaFX Application Thread.
			Platform.runLater(() -> {
				// Apply immediate syntax highlighting (skipping the expensive linter pass).
				applyHighlightingInternal(false);

				// Restore the scroll position to ensure the viewport remains anchored while typing.
				codeAreaScrollPane.estimatedScrollYProperty().setValue(currentEstimatedScrollY);

				// Ensure the caret remains visible during rapid text changes.
				editor.requestFollowCaret();
			});
		});

		styleEditor.textProperty().addListener((obs, old, newCss) -> {
			// 1. Chronologische Erfassung (wie gehabt)
			if (activityStack.isEmpty() || activityStack.peek() != styleEditor) {
				activityStack.push(styleEditor);
			}
			redoActivityStack.clear();

			// --- DIE SOUVERÄNE REAKTIVITÄTS-BRÜCKE ---
			// Wenn sich das CSS ändert, müssen wir den Haupt-Editor (Markdown)
			// zwingen, seine Klassen-Fehler neu zu prüfen!
			Platform.runLater(() -> applyHighlightingInternal(true));

			// 2. Vorschau und Status (wie gehabt)
			if (isPreviewMode)
				renderMarkdownPreview();
			updateDirtyState();
			updateUndoRedoButtonStates();
		});

		// --- 3. VERFÜGBARKEITS-WÄCHTER (Echtzeit-Buttons) ---

		// Wir lauschen auf beide Editoren. Die Methode 'updateUndoRedoButtonStates'
		// entscheidet dann souverän basierend auf dem Stack-Zustand.
		editor.undoAvailableProperty().addListener((obs, was, is) -> updateUndoRedoButtonStates());
		editor.redoAvailableProperty().addListener((obs, was, is) -> updateUndoRedoButtonStates());

		styleEditor.undoAvailableProperty().addListener((obs, was, is) -> updateUndoRedoButtonStates());
		styleEditor.redoAvailableProperty().addListener((obs, was, is) -> updateUndoRedoButtonStates());

		// --- 4. STATUS & NAVIGATION ---
		editor.caretPositionProperty().addListener((obs, old, val) -> updateStatusLabels());
		editor.selectionProperty().addListener((obs, old, val) -> updateStatusLabels());

		// Dirty State via UndoManager (Markierung wird beim Speichern gesetzt)
		editor.getUndoManager().atMarkedPositionProperty().addListener((obs, was, is) -> updateDirtyState());
		styleEditor.getUndoManager().atMarkedPositionProperty().addListener((obs, was, is) -> updateDirtyState());
	}

	/**
	 * Der souveräne Scanner: Isoliert den ersten <style>-Block aus einem Text-Stream. Ermöglicht die physische Trennung von Inhalt und Form beim Laden.
	 */
	private StyleRange findStyleRange(String text) {
		if (text == null || text.isEmpty())
			return null;

		Matcher startM = STYLE_BLOCK_DETECTOR.matcher(text);
		if (startM.find()) {
			int fullStart = startM.start();
			int contentStart = startM.end();

			Matcher endM = STYLE_END_TAG_DETECTOR.matcher(text);
			if (endM.find(contentStart)) {
				int contentEnd = endM.start();
				int fullEnd = endM.end();

				return new StyleRange(fullStart, fullEnd, contentStart, contentEnd, text.substring(contentStart, contentEnd), // content
				        text.substring(fullStart, fullEnd) // fullText
				);
			}
		}
		return null;
	}

	private void refreshGutter() {
		if (lineNumberFactory != null) {
			// Der harte Reset ist notwendig und hinreichend.
			editor.setParagraphGraphicFactory(null);
			editor.setParagraphGraphicFactory(lineNumberFactory);
		}
	}

	// Vereinfachte applyHighlighting() Methode für "Full-Refresh" (z.B. beim Laden)
	private void applyHighlighting() {
		applyHighlightingInternal(true); // Führt einen vollständigen Scan mit Fehlern aus
	}

	private void applyHighlightingInternal(boolean includeLinterErrors) {
		String text = editor.getText();
		String cssText = styleEditor.getText();

		if (text == null || text.isEmpty()) {
			editor.setStyleSpans(0, createEmptySpans());
			return;
		}

		CompletableFuture.supplyAsync(() -> {
			// 1. NUTZER-INVENTUR
			Set<String> definedClasses = MarkdownLinter.extractDefinedClasses(cssText);

			// 2. SYSTEM-INVENTUR HINZUFÜGEN (Heilung der indirekten Klassen)
			definedClasses.addAll(themeManager.getSystemClasses());

			// 3. DOKUMENT PARSEN
			Node document = markdownParser.parse(text);

			// 4. MASTER-HIGHLIGHTING
			return markdownHighlighter.applyHighlighting(text, document, includeLinterErrors, isDesignMode, -1, -1, currentSearchMatches, definedClasses);

		}).thenAccept(spans -> {
			Platform.runLater(() -> {
				if (editor.getText().length() == spans.length()) {
					editor.setStyleSpans(0, spans);
					updateStatusLabels();
					refreshGutter();
					updateDiagnosticsStripe();
//					if (isPreviewMode && !isDesignMode)
//						renderMarkdownPreview();
				}
			});
		});
	}

	// 1. TOP TOOLBAR
	private void setupActionBar() {
		configureLabelButton(loadButton, "fas-folder-open", UIText.OPEN.getLabel(), UIText.OPEN.getTooltip());
		configureLabelButton(newDocumentButton, "fas-file", UIText.NEW_DOCUMENT.getLabel(), UIText.NEW_DOCUMENT.getTooltip());
		configureLabelButton(saveButton, "fas-save", UIText.SAVE.getLabel(), UIText.SAVE.getTooltip());
		configureLabelButton(saveAsButton, "fas-copy", UIText.SAVE_AS.getLabel(), UIText.SAVE_AS.getTooltip());
		configureLabelButton(projectSettingsButton, "fas-sliders-h", UIText.SETTINGS_BUTTON.getLabel(), UIText.SETTINGS_BUTTON.getTooltip());
		// In setupActionBar()
		configureLabelButton(injectThemeButton, "fas-magic", "Inject Blueprint", "Inject system theme as a starting point");
		configureLabelButton(exportHtmlButton, "fas-file-code", UIText.EXPORT_HTML.getLabel(), UIText.EXPORT_HTML.getTooltip());
		configureIconButton(searchButton, "fas-search", UIText.FIND.getTooltip());
		configureLabelButton(printButton, "fas-print", UIText.PRINT.getLabel(), UIText.PRINT.getTooltip());
		// In setupActionBar()
		configureIconButton(undockButton, "fas-external-link-alt", "Undock Preview to separate window");
	}

	private void setupAccelerators() {
		rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
			if (newScene != null) {
				newScene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::handleOpenDocumentAction);
				newScene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN), () -> handleNewDocumentAction(null));
				newScene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::handleSaveAction);
				newScene.getAccelerators().put(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN), () -> handlePrintAction(null));
				newScene.getAccelerators().put(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN), this::handleUndoAction);
				newScene.getAccelerators().put(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN), this::handleRedoAction);
				// In der Methode, wo du die Accelerators hinzufügst:
				newScene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), () -> handleSearchButtonAction(null) // Erstellt ein Runnable, das
				// die Methode mit null aufruft
				);
				newScene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), this::handleFormatAction);
				newScene.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.CONTROL_DOWN), this::toggleDesignMode);
			}
		});
	}

	// =============================================================================================
	// 7. SETUP SIDEBAR ICONS
	// =============================================================================================

	private void setupSidebarIcons() {
		// --- 1. GRID BUTTONS (Icon-only) ---
		// Diese nutzen weiterhin configureIconButton, da sie KEINEN Text haben sollen
		configureIconButton(undoButton, "fas-undo-alt", UIText.UNDO.getTooltip());
		configureIconButton(redoButton, "fas-redo-alt", UIText.REDO.getTooltip());
		configureIconButton(boldButton, "fas-bold", UIText.BOLD.getTooltip());
		configureIconButton(italicButton, "fas-italic", UIText.ITALIC.getTooltip());
		configureIconButton(strikethroughButton, "fas-strikethrough", UIText.STRIKETHROUGH.getTooltip());
		configureIconButton(inlineCodeButton, "fas-code", UIText.INLINE_CODE.getTooltip());
		configureIconButton(bulletListButton, "fas-list-ul", UIText.BULLET_LIST.getTooltip());
		configureIconButton(orderedListButton, "fas-list-ol", UIText.ORDERED_LIST.getTooltip());
		configureIconButton(taskListButton, "fas-check-square", UIText.TASK_LIST.getTooltip());
		configureIconButton(hrButton, "fas-minus", UIText.HORIZONTAL_RULE.getTooltip());
		configureIconButton(linkButton, "fas-link", UIText.INSERT_LINK.getTooltip());
		configureIconButton(imageButton, "fas-image", UIText.INSERT_IMAGE.getTooltip());
		configureIconButton(quoteButton, "fas-quote-right", UIText.BLOCKQUOTE.getTooltip());
		configureIconButton(codeBlockButton, "fas-file-code", UIText.CODE_BLOCK.getTooltip());
		configureIconButton(pageBreakButton, "fas-file-medical", UIText.INSERT_PAGE_BREAK.getTooltip());

		// SOUVERÄNE ZUWEISUNG: Nutzt die Enum-Konstante statt 'new'
		updateButtonUI(designModeButton, "fas-paint-brush", UIText.DESIGN_TOGGLE);

		// --- 3. WIDE BUTTONS (Icon + Text) using Smart updateButtonUI ---

		// Initiales Setup für statische Icons
		updateButtonUI(templateButton, "fas-magic", UIText.TEMPLATE_BUTTON);

		// Stelle sicher, dass im FXML oder hier die Action auf die neue Methode zeigt:
		templateButton.setOnAction(e -> handleTemplateLibraryAction());

		updateButtonUI(viewToggleButton, "fas-eye", UIText.VIEW_TOGGLE);

		// Initiales Setup für Toggles (Icons werden hier gesetzt)
		updateButtonUI(wordWrapToggle, "fas-text-width", UIText.WORD_WRAP_ON);

		// Aktuellen Zustand der Toggles erzwingen (Text-Updates)
		updateWordWrapButton();
		updateDarkModeButtonIcon();

		// --- 4. ALIGNMENT-HARMONISIERUNG ---
		// Wir zwingen auch die unteren Buttons zur Linksbündigkeit,
		// damit lange Texte ("Design") Platz haben.

		Stream.of(wordWrapToggle, viewToggleButton, designModeButton, darkModeButton).forEach(btn -> {
			btn.setAlignment(Pos.CENTER_LEFT);
			btn.setPadding(new Insets(5, 0, 5, 5)); // Etwas Abstand vom Rand für das Icon
		});
	}

	private void setupStructureControls() {
		if (headingMenuButton == null)
			return;

		FontIcon icon = new FontIcon("fas-heading");
		icon.setIconSize(ICON_SIZE);
		headingMenuButton.setGraphic(icon);
		headingMenuButton.setText(UIText.HEADINGS.getLabel());
		headingMenuButton.setTooltip(new Tooltip(UIText.HEADINGS.getTooltip()));
		headingMenuButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
		headingMenuButton.getStyleClass().add("sidebar-menu-button");
	}

	/**
	 * Sets up the Placeholder MenuButton in the sidebar. Allows stamping dynamic placeholders (like {{date}} or {{randomId}}) directly into the Markdown editor.
	 */
	private void setupPlaceholderMenu() {
		if (placeholderMenuButton == null)
			return;

		FontIcon stampIcon = new FontIcon("fas-stamp");
		stampIcon.setIconSize(ICON_SIZE);
		placeholderMenuButton.setGraphic(stampIcon);
		placeholderMenuButton.setText(UIText.PLACEHOLDER_MENU.getLabel()); // Text aus Enum
		placeholderMenuButton.setTooltip(new Tooltip(UIText.PLACEHOLDER_MENU.getTooltip()));
		placeholderMenuButton.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY); // Oder GRAPHIC_TEXT, je nach gewünschtem Layout
		headingMenuButton.getStyleClass().add("sidebar-menu-button");
		// 2. Dynamisch füllen aus dem Placeholder-Enum
		placeholderMenuButton.getItems().clear();
		for (Placeholder p : Placeholder.values()) {
			MenuItem item = new MenuItem(p.getKey()); // z.B. "documentTitle"
			item.setUserData(p.getTag()); // Speichert "{{documentTitle}}"

			item.setOnAction(e -> {
				// Nutze deine bestehende insertText Methode
				insertText(p.getTag());
				Log.info("Placeholder stamped: " + p.getTag());
			});
			placeholderMenuButton.getItems().add(item);
		}
	}

	/**
	 * Smart Update: Updates text and tooltip. Changes the icon ONLY if a non-null iconCode is provided.
	 */
	private void updateButtonUI(ButtonBase button, String iconCode, UIText ui) {
		if (button == null)
			return;

		// 1. Icon-Logik: Nur bei neuem Code wird das Icon-Objekt getauscht
		if (iconCode != null) {
			FontIcon icon = new FontIcon(iconCode);
			icon.setIconSize(ICON_SIZE);
			button.setGraphic(icon);
		}

		// 2. Text und Tooltip: Werden immer synchronisiert
		button.setText(ui.getLabel());
		button.setTooltip(new Tooltip(ui.getTooltip()));

		// 3. Layout: Linksbündig für Text-Buttons
		button.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
	}

	/**
	 * Icon only - for grid buttons (Undo, Bold, etc.)
	 */
	private void configureIconButton(ButtonBase button, String iconCode, String tooltipText) {
		if (button != null) {
			FontIcon icon = new FontIcon(iconCode);
			icon.setIconSize(ICON_SIZE);
			button.setGraphic(icon);
			button.setText(""); // Remove text
			button.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
			button.getStyleClass().add("sidebar-icon-button");

			if (tooltipText != null)
				button.setTooltip(new Tooltip(tooltipText));
		}
	}

	/**
	 * Icon + Text - for wide buttons (Template, View, Wrap)
	 */
	private void configureLabelButton(ButtonBase button, String iconCode, String labelText, String tooltipText) {
		if (button != null) {
			FontIcon icon = new FontIcon(iconCode);
			icon.setIconSize(ICON_SIZE);

			button.setGraphic(icon);
			button.setText(labelText); // Keep the label text

			button.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

			if (tooltipText != null)
				button.setTooltip(new Tooltip(tooltipText));
		}
	}

	/**
	 * Updates the diagnostics stripe to visually represent detected Linter errors. Each error is drawn as a clickable marker on the stripe, with colors indicating the severity or
	 * type of the error:
	 * <ul>
	 * <li><b>Red:</b> For critical syntax errors that severely impact parsing or rendering.</li>
	 * <li><b>Cyan:</b> Specifically for missing CSS classes, highlighting design-related issues.</li>
	 * <li><b>Gold-Yellow:</b> For general warnings or less severe formatting and structural issues.</li>
	 * </ul>
	 * Clicking a marker will navigate the editor's caret to the beginning of the corresponding error in the source code.
	 */
	private void updateDiagnosticsStripe() {
		diagnosticsStripe.getChildren().clear();

		List<MarkdownLinter.TagError> errors = markdownHighlighter.getLastErrors();
		if (errors == null || errors.isEmpty())
			return;

		double documentLength = editor.getLength();
		double stripeHeight = diagnosticsStripe.getHeight();
		double stripeWidth = diagnosticsStripe.getWidth();

		if (documentLength <= 0 || stripeHeight <= 0)
			return;

		for (MarkdownLinter.TagError error : errors) {
			double yPosition = ((double) error.start / documentLength) * stripeHeight;

			javafx.scene.shape.Rectangle marker = new javafx.scene.shape.Rectangle();
			marker.setX(2);
			marker.setY(yPosition);
			marker.setWidth(stripeWidth - 4);
			marker.setHeight(3);

			// --- SOVEREIGN COLOR DISPATCHER (Tied to your specific ErrorIDs) ---
			javafx.scene.paint.Color color;
			switch (error.id) {
			// CRITICAL ERRORS (Red)
			case UNCLOSED_OPENING: // Unclosed tags are critical for HTML structure
			case MISMATCHED_CLOSING: // Mismatched closing tags lead to significant rendering issues
			case MALFORMED_TAG: // Fundamentally incorrect tag structure
			case ILLEGAL_FRAGMENT_TAG: // Unexpected tags in invalid positions
			case UNCLOSED_CODEBLOCK: // Unclosed fenced code block
			case UNCLOSED_COMMENT: // Unclosed HTML comment
			case MALFORMED_IMAGE_TAG: // Malformed image tags (can break rendering)
			case UNCLOSED_LINK_BRACKET: // Unclosed link square bracket [Text
			case UNCLOSED_LINK_PAREN: // Unclosed link parenthesis [Text](url
			case MISSING_COLON: // Missing colon in YAML or CSS
			case MISSING_YAML_START_DELIMITER: // <-- NEU
			case MISSING_YAML_END_DELIMITER: // <-- NEU
				color = javafx.scene.paint.Color.web("#FF0000"); // Red for critical, structure-breaking errors
				break;

			// MISSING CSS (Cyan - specific design feedback)
			case MISSING_CSS_CLASS:
				color = javafx.scene.paint.Color.web("#20BFDF"); // Cyan for missing CSS class definitions
				break;

			// GENERAL WARNINGS (Gold-Yellow)
			case REDUNDANT_CLOSING: // Redundant closing tags
			case REDUNDANT_BRACE: // Redundant curly braces
			case REDUNDANT_COMMENT_CLOSING: // Redundant HTML comment closing
			case MISSING_SEMICOLON: // Missing semicolon in CSS (often just a warning)
			case UNCLOSED_BRACE: // Unclosed curly brace
			case REDUNDANT_SEMICOLON: // Redundant semicolon
			case MISSING_EQUALS: // Missing '=' in an attribute
			case MISSING_BLANK_LINE_AFTER_HTML: // Missing blank line (often a formatting issue)
			case ILLEGAL_DUPLICATE_STYLE_BLOCK: // Duplicate style blocks
			default: // All other errors not explicitly red or cyan
				color = javafx.scene.paint.Color.web("#FFCA28"); // Gold-Yellow for general warnings
				break;
			}

			marker.setFill(color);

			marker.setEffect(new javafx.scene.effect.DropShadow(3, javafx.scene.paint.Color.BLACK));
			marker.setCursor(javafx.scene.Cursor.HAND);
			marker.setOnMouseClicked(e -> {
				jumpToError(error);
				e.consume();
			});

//			Tooltip.install(marker, new Tooltip(MarkdownLinter.getErrorDescription(error)));

			diagnosticsStripe.getChildren().add(marker);
		}
	}

	/**
	 * Bewegt den Cursor zum Anfang eines Fehlers und scrollt intelligent dorthin. Nutzt die stabile, offizielle API der CodeArea.
	 */
	private void jumpToError(MarkdownLinter.TagError error) {
		// 1. Setze den Cursor exakt an den Fehleranfang
		editor.moveTo(error.start);

		// 2. Intelligentes Scrollen
		int targetLine = editor.offsetToPosition(error.start, CodeArea.Bias.Forward).getMajor();

		// HEILUNG: Wir fragen die CodeArea direkt nach der Anzahl der sichtbaren Paragraphen.
		// Dies ist eine Näherung, aber die stabilste, die wir ohne komplexe Berechnungen bekommen.
		int totalVisibleParagraphs = editor.getVisibleParagraphs().size();

		// Berechne den "Sweet Spot" (oberes Viertel)
		int offset = Math.max(1, totalVisibleParagraphs / 4);

		// Berechne die Ziel-Zeile, die an den oberen Rand gescrollt werden soll
		int scrollToLine = Math.max(0, targetLine - offset);

		// 3. Führe den Sprung aus
		// requestFollowCaret() ist der robusteste Befehl, um den Cursor in den
		// sichtbaren Bereich zu zwingen. showParagraphAtTop() kann manchmal unzuverlässig sein.
		editor.requestFollowCaret();

		// Für die präzise Positionierung:
		Platform.runLater(() -> editor.showParagraphAtTop(scrollToLine));

		editor.requestFocus();
	}

	/**
	 * Synchronizes the word wrap state across all active editors (Markdown and CSS). Ensures visual consistency and updates the sidebar button aesthetics.
	 */
	private void updateWordWrapButton() {
		if (wordWrapToggle == null)
			return;

		// 1. Capture the sovereign state from the UI toggle
		this.wordWrapEnabled = wordWrapToggle.isSelected();

		// 2. Apply the state to the Primary Editor (Markdown)
		if (editor != null) {
			editor.setWrapText(wordWrapEnabled);
		}

		// 3. Apply the state to the Style Sanctuary (CSS Forge)
		// HEILUNG: Auch der CSS-Editor muss dem globalen Befehl gehorchen!
		if (styleEditor != null) {
			styleEditor.setWrapText(wordWrapEnabled);
		}

		// 4. Update UI Button Text & Tooltip (Sovereign Labels)
		UIText wrapText = wordWrapEnabled ? UIText.WORD_WRAP_ON : UIText.WORD_WRAP_OFF;

		// Use Smart Update: keeps the icon, changes the label
		updateButtonUI(wordWrapToggle, null, wrapText);

		Log.fine("Sovereign Wrap: " + (wordWrapEnabled ? "ENABLED" : "DISABLED") + " for all editors.");
	}

	// 3. DARK MODE BUTTON
	private void updateDarkModeButtonIcon() {
		if (darkModeButton == null)
			return;

		UIText text = isDarkMode ? UIText.DARK_MODE_DARK : UIText.DARK_MODE_LIGHT;
		String iconCode = isDarkMode ? "fas-moon" : "fas-sun";

		// ICON ist vorhanden -> Smart Update setzt das neue Icon
		updateButtonUI(darkModeButton, iconCode, text);
	}

	private void updateStatusLabels() {
		// 1. Zählung und Positions-Labels (Unverändert)
		String text = editor.getText();
		long words = Arrays.stream(text.split("\\s+")).filter(w -> !w.isEmpty()).count();

		charCountLabel.setText(UIText.CHARS_PREFIX.getLabel() + text.length());
		wordCountLabel.setText(UIText.WORDS_PREFIX.getLabel() + words);
		positionLabel.setText(UIText.LINE_PREFIX.getLabel() + (editor.getCurrentParagraph() + 1) + UIText.COLUMN_PREFIX.getLabel() + (editor.getCaretColumn() + 1));

		// 2. ERROR REPORTING (Unverändert)
		if (markdownHighlighter != null && errorCountLabel != null) {
			int errorCount = markdownHighlighter.getLastErrors().size();
			errorCountLabel.setText("Errors: " + errorCount);

			if (errorCount > 0) {
				if (!errorCountLabel.getStyleClass().contains("status-label-error")) {
					errorCountLabel.getStyleClass().add("status-label-error");
				}
			} else {
				errorCountLabel.getStyleClass().remove("status-label-error");
			}
		}

		// 3. LOGISCHE HIERARCHIE FÜR DATEINAMEN (Konsolidiert)
		String unsavedIndicator = saveRequested ? UIText.UNSAVED_SUFFIX.getLabel() : "";
		String statusText;

		if (currentFile != null) {
			// Zeige den Namen. Falls es ein Template ist, hänge den Hinweis an.
			statusText = currentFile.getName() + (isEditingTemplate ? " [Template]" : "");
		} else {
			statusText = UIText.UNTITLED.getLabel();
		}

		statusLabel.setText(statusText + unsavedIndicator);
	}

	/**
	 * Gibt die Haupt-Stage der Anwendung zurück. Dient als "Owner" für alle modalen Dialoge.
	 */
	public Stage getPrimaryStage() {
		return (Stage) rootPane.getScene().getWindow();
	}

	public void setInitialMarkdown(String markdown) {
		this.initialMarkdown = (markdown == null) ? "" : markdown;
		if (editor != null) {
			editor.replaceText(this.initialMarkdown);
			renderMarkdownPreview();
			applyHighlighting();
		}
	}

	// --- NEUE HILFSMETHODEN für Preferences ---

	/**
	 * Lädt den Pfad des zuletzt verwendeten Verzeichnisses aus den Preferences.
	 */
	private void loadLastUsedDirectory() {
		// Hole den gespeicherten Pfad. Wenn nichts da ist, gib null zurück.
		String lastPath = prefs.get(LAST_DIRECTORY_KEY, null);
		if (lastPath != null) {
			File dir = new File(lastPath);
			// Überprüfe, ob das Verzeichnis noch existiert.
			if (dir.exists() && dir.isDirectory()) {
				this.lastUsedDirectory = dir;
				System.out.println("Zuletzt verwendeter Ordner geladen: " + lastPath);
			}
		}
	}

	/**
	 * Speichert den aktuellen Wert von 'lastUsedDirectory' in den Preferences.
	 */
	private void saveLastUsedDirectory() {
		if (this.lastUsedDirectory != null) {
			// Speichere den absoluten Pfad unter unserem Schlüssel.
			prefs.put(LAST_DIRECTORY_KEY, this.lastUsedDirectory.getAbsolutePath());
			System.out.println("Neuer Ordner gespeichert: " + this.lastUsedDirectory.getAbsolutePath());
		}
	}

	public String getMarkdownText() {
		return editor.getText();
	}

	public boolean isSaveRequested() {
		return saveRequested;
	}

	/**
	 * Configures and initializes the Flexmark parser and HTML renderer. This method sets up the core engine responsible for converting Markdown text into HTML, enabling essential
	 * features and applying critical architectural fixes for stability and navigation.
	 *
	 * <p>
	 * <b>Key Configurations:</b>
	 * </p>
	 * <ul>
	 * <li><b>GFM Extensions:</b> Enables GitHub Flavored Markdown features like tables, strikethrough, and task lists.</li>
	 * <li><b>YAML Front Matter:</b> Allows parsing of metadata blocks at the beginning of a document.</li>
	 * <li><b>Indented Code Block Fix:</b> Disables the legacy indented code block parser to prevent unwanted rendering of indented HTML as code blocks. This ensures structural
	 * integrity, as fenced code blocks (```) are the preferred standard.</li>
	 * <li><b>Source Position Mapping:</b> Injects a {@code data-line} attribute into the rendered HTML elements. This is the foundational mechanism for the "Sovereign Navigator,"
	 * enabling precise, bidirectional navigation between the source editor and the live preview.</li>
	 * </ul>
	 */
	private void setupFlexmark() {
		MutableDataSet options = new MutableDataSet();

		// 1. EXTENSIONS (AnchorLinkExtension ENTFERNT)
		options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create(), TaskListExtension.create(), YamlFrontMatterExtension.create(),
		        com.vladsch.flexmark.ext.attributes.AttributesExtension.create() // NEU: Erlaubt manuelle IDs
		// AnchorLinkExtension wurde hier gelöscht, um <a> in Headern zu vermeiden
		));

		// 2. ID-GENERIERUNG (Das ist alles, was du für funktionierende TOC-Links brauchst)
		options.set(HtmlRenderer.GENERATE_HEADER_ID, true); // Erzeugt die ID
		options.set(HtmlRenderer.RENDER_HEADER_ID, true); // Schreibt id="..." in den h1-h6 Tag
		options.set(HtmlRenderer.HEADER_ID_GENERATOR_RESOLVE_DUPES, true);
		// Verhindert doppelte Bindestriche (z.B. aus " : " wird "-" statt "---")
		options.set(HtmlRenderer.HEADER_ID_GENERATOR_NO_DUPED_DASHES, true);
		options.set(HtmlRenderer.HEADER_ID_GENERATOR_NON_ASCII_TO_LOWERCASE, true);

		// 3. NAVIGATION & STRUKTUR
		options.set(Parser.INDENTED_CODE_BLOCK_PARSER, false);
		options.set(HtmlRenderer.SOURCE_POSITION_ATTRIBUTE, SovereignSourceMapper.DATA_LINE_ATTRIBUTE);
		options.set(HtmlRenderer.SOURCE_POSITION_PARAGRAPH_LINES, true);

		markdownParser = Parser.builder(options).build();
		htmlRenderer = HtmlRenderer.builder(options).build();
	}

	private void setupWebView() {
		webView.setContextMenuEnabled(false);
		webView.setCache(true);
		webView.setCacheHint(javafx.scene.CacheHint.QUALITY);
		webView.setFontSmoothingType(javafx.scene.text.FontSmoothingType.GRAY);
		webView.getEngine().setJavaScriptEnabled(true);

		webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
			if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {

				JSObject window = (JSObject) webView.getEngine().executeScript("window");
				window.setMember("javaBridge", bridge);

				String injection = """
				        (function() {
				            var style = document.createElement('style');
				            style.innerHTML = '.fs-hover { outline: 2px solid #20BFDF !important; cursor: crosshair !important; }';
				            document.head.appendChild(style);

				            document.onmouseover = function(e) {
				                var target = e.target.closest('[data-fsid]');
				                if (window.last) window.last.classList.remove('fs-hover');
				                if (target) { target.classList.add('fs-hover'); window.last = target; }
				            };

				            document.onclick = function(e) {
				                try {
				                    var target = e.target.closest('[data-fsid]') || e.target;
				                    var uid = target.getAttribute('data-fsid') || '-1';
				                    var tagName = target.tagName.toLowerCase();

				                    var selectors = [];
				                    var curr = target;
				                    while(curr && curr.tagName !== 'HTML') {
				                        selectors.push(curr.tagName.toLowerCase());
				                        if(curr.id) selectors.push('#' + curr.id);
				                        if(curr.className) {
				                            curr.className.split(/\\s+/).forEach(c => {
				                                if(c && c !== 'fs-hover') selectors.push('.' + c);
				                            });
				                        }
				                        curr = curr.parentElement;
				                    }

				                    // HEILUNG: Erhöhe das Limit auf 500 Zeichen, um Satzkürzungen zu verhindern.
				        			var text = target.innerText ? target.innerText.substring(0, 500).replace(/[\\u00A0\\n\\r]+/g, ' ').trim() : '';
				                                // -------------------------------------------------------------------------------------

				                    var sig = (target.outerHTML.match(/^<[^>]+>/) || [''])[0];

				                    var occ = 0;
				                    var all = document.getElementsByTagName(target.tagName);
				                    for(var i=0; i<all.length; i++) {
				                        if(all[i] === target) break;
				                        var s = (all[i].outerHTML.match(/^<[^>]+>/) || [''])[0];
				                        if(s === sig) occ++;
				                    }

				                    javaBridge.onElementClicked(tagName, text, selectors.join(','), sig, uid, occ);
				                } catch(err) { console.error(err); }
				                e.preventDefault(); e.stopPropagation();
				            };
				        })();
				        """;

				webView.getEngine().executeScript(injection);

				// --- Schritt 2: SCROLL-WIEDERHERSTELLUNG (NEUE LOGIK) ---
				// Dieser Block wird JEDES MAL ausgeführt, wenn eine Seite fertig geladen ist.
				// Wir prüfen das Flag, ob wir die Scroll-Position wiederherstellen sollen.

				if (restoreScrollRequested) {
					Platform.runLater(() -> {
						PauseTransition restoreScrollDelay = new PauseTransition(Duration.millis(50));
						restoreScrollDelay.setOnFinished(event -> {
							if (lastWebViewScrollY > 0) {
								webView.getEngine().executeScript("window.scrollTo(0, " + lastWebViewScrollY + ");");
								Log.fine("WebView scrollY restored to: %f", lastWebViewScrollY);
							}

							// --- Gedächtnis löschen ---
							restoreScrollRequested = false; // Flag zurücksetzen
							lastWebViewScrollY = 0; // Wert nullen, damit er nicht beim nächsten Mal "spukt"
						});
						restoreScrollDelay.play();
					});
				}
			}
		});

		Platform.runLater(() -> {
			double screenScale = javafx.stage.Screen.getPrimary().getOutputScaleX();
			webView.setZoom(1.0 / screenScale);
		});
	}

	/**
	 * Standardizes the centering logic for any CodeArea. Ensures the target line is positioned in the middle of the viewport.
	 */
	private void applyCenteredSelection(CodeArea area, int lineIndex) {
		if (area == null || lineIndex < 0)
			return;

//        // Wir erzwingen, dass der Cursor erst einmal physisch dort ist
		area.requestFollowCaret(); // notwendig damit richtig zentriert gecrollt wird

		// Der Trick für die Stabilität: Wir warten einen UI-Puls,
		// damit die CodeArea ihre Dimensionen kennt.
		Platform.runLater(() -> {
			// Wir nehmen eine Standard-Viewport-Größe an, falls die Messung noch scheitert
			int visibleCount = area.getVisibleParagraphs().size();
			if (visibleCount <= 0)
				visibleCount = 5; // Sicherer Schätzwert für 1080p

			int centerOffset = visibleCount / 5;
			int topParagraph = Math.max(0, lineIndex - centerOffset);

			area.showParagraphAtTop(topParagraph);
		});
	}

	// In der smartJumpToSource Methode des Controllers:
	public void smartJumpToSource(String tagName, String textContent, String openingTag, String uidHint, int occurrenceIndex) {
		// Einfache, sichere Delegation
		navigator.smartJumpToSource(tagName, textContent, openingTag, uidHint, occurrenceIndex);
	}

	private void inspectCssRules(List<String> selectors, List<String> missingClasses) {
		if (selectors == null || selectors.isEmpty())
			return;

		String cssText = styleEditor.getText();

		// HEILUNG 1: Wir nehmen den spezifischsten Selektor (das letzte Element der Liste)
		// Die Liste vom JS ist [Tag, ID, Klasse1, Klasse2].
		// Wir wollen die letzte Klasse als 'bestCandidate'.
		String bestCandidate = selectors.get(selectors.size() - 1);

		// 1. STATUS-LABEL & TOOLTIP (Die Aufzählung)
		String statusInfo = bestCandidate;
		if (!missingClasses.isEmpty()) {
			statusInfo += " ⚠️ (" + missingClasses.size() + " missing)";
			activeContextLabel.setStyle("-fx-text-fill: #20BFDF; -fx-font-weight: bold;");

			// HEILUNG 2: Wir installieren einen Tooltip mit der Liste ALLER fehlenden Klassen
			String missingList = String.join("\n", missingClasses);
			Tooltip.install(activeContextLabel, new Tooltip("Missing definitions:\n" + missingList));
		} else {
			activeContextLabel.setStyle(""); // Reset
			Tooltip.uninstall(activeContextLabel, null);
		}
		activeContextLabel.setText(statusInfo);

		// 2. REGEL-SUCHE (Wie gehabt)
		int targetOffset = -1;
		for (String selector : selectors) {
			Matcher m = Pattern.compile("(?i)" + Pattern.quote(selector) + "\\s*\\{").matcher(cssText);
			if (m.find()) {
				targetOffset = m.start();
				break;
			}
		}

		if (targetOffset != -1) {
			suggestRuleButton.setVisible(false);
			suggestRuleButton.setManaged(false);

			final int finalOffset = targetOffset;
			Platform.runLater(() -> {
				styleEditor.moveTo(finalOffset);
				styleEditor.requestFollowCaret();

				// Zeile im Sanktuarium markieren
				int lineIdx = styleEditor.offsetToPosition(finalOffset, org.fxmisc.richtext.model.TwoDimensional.Bias.Forward).getMajor();
				styleEditor.selectRange(finalOffset, finalOffset + styleEditor.getParagraph(lineIdx).length());

				// 3. SOUVERÄNE ZENTRIERUNG (wie im Navigator)
				applyCenteredSelection(styleEditor, lineIdx);

				styleEditor.requestFocus();
			});
		} else {
			// REGEL FEHLT: Erste fehlende Klasse vorschlagen (oder den bestCandidate)
			lastSuggestedSelector = missingClasses.isEmpty() ? bestCandidate : missingClasses.get(0);
			suggestRuleButton.setText("[+] Define Style for " + lastSuggestedSelector);
			suggestRuleButton.setVisible(true);
			suggestRuleButton.setManaged(true);
		}
	}

	/**
	 * Erzeugt ein neues CSS-Skelett im Sanktuarium. Nutzt die automatische Synchronisation, um den Schreibschutz im Haupt-Editor aktuell zu halten.
	 */
	@FXML
	private void handleCreateNewRule() {
		if (lastSuggestedSelector == null || lastSuggestedSelector.isEmpty())
			return;

		// 1. Konstruktion des Skeletts
		String newRule = "\n" + lastSuggestedSelector + " {\n  \n}\n";

		// 2. Injektion in den spezialisierten Editor
		// WICHTIG: Wir ändern NUR den styleEditor. Der dortige Listener (Debouncer)
		// übernimmt den Rest: Er schreibt das CSS in den Haupt-Editor,
		// rekalibriert die Offsets und aktualisiert das Highlighting.
		styleEditor.appendText(newRule);

		// 3. UX-PRÄZISION: Cursor exakt in die leere Zeile setzen
		// Die Position -3 setzt den Cursor genau zwischen die geschweiften Klammern.
		int newCaretPos = styleEditor.getLength() - 3;
		styleEditor.moveTo(newCaretPos);
		styleEditor.requestFocus();

		// 4. UI-Zustand bereinigen
		suggestRuleButton.setVisible(false);
		suggestRuleButton.setManaged(false);

		Log.info("Sovereign Architect: Created and synced new CSS rule for " + lastSuggestedSelector);
	}

	/**
	 * Konfiguriert das Popup-Suchsystem. Ersetzt die alte HBox-Logik. Dies ist die architektonisch korrekte Lösung (Delegation über Setters).
	 */
	private void setupSearchPopupArchitecture() {
		// Wir nutzen den SearchButton als Anker
		if (searchButton == null)
			return;

		// 1. Instanziierung des Popup
		// HINWEIS: Der Konstruktor muss nur den Anker-Button (owner) akzeptieren.
		this.searchPopup = new CodeAreaSearchPopup(searchButton);

		// 2. SETZEN DER AKTIONEN (DELEGATION & ENTKOPPLUNG)

		// Next/Previous/Close: Verwenden die FXML-Handler des Controllers
		this.searchPopup.setOnNextAction(this::handleSearchNextAction);
		this.searchPopup.setOnPreviousAction(this::handleSearchPreviousAction);
		this.searchPopup.setOnCloseAction(this::handleCloseSearchAction);

		// 3. REGISTRIEREN DES HAUPT-LISTENERS (Live-Suche beim Tippen)
		// Die Suche muss starten, sobald sich der Text ändert.
		this.searchPopup.getSearchField().textProperty().addListener((obs, oldVal, newVal) -> findAndHighlightMatches());

		// Führe eine initiale Suche durch, falls bereits Text im Feld ist
		findAndHighlightMatches();

		Log.fine("Search Architecture: Popup initialized and delegated (CP-04 passed).");
	}

	private void findAndHighlightMatches() {
		final String searchText = searchPopup.getSearchField().getText();
		currentSearchMatches.clear();
		searchResults.clear();

		if (searchText != null && !searchText.isEmpty()) {
			String content = editor.getText();
			Pattern pattern = Pattern.compile(Pattern.quote(searchText), Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(content);

			while (matcher.find()) {
				IndexRange range = new IndexRange(matcher.start(), matcher.end());
				currentSearchMatches.add(range);
				searchResults.add(range);
			}
		}

		// UI-Updates delegieren
		applyHighlightingInternal(false);

		// Korrigierter Aufruf des gekapselten UI-Updates im Popup
		searchPopup.updateMatchCount(searchResults.size(), currentMatchIndex + 1);
		searchPopup.updateNavigationButtons(!searchResults.isEmpty()); // Navigation nur aktivieren, wenn Treffer da sind
	}

	/**
	 * Scrolls to the given text range, but only if it's not already visible. If scrolling is needed, it attempts to bring the match into view with context. This is the definitive,
	 * working version using the correct RichTextFX API.
	 *
	 * @param range The IndexRange of the match to scroll to.
	 */
	private void scrollToMatchIntelligently(IndexRange range) {
		// This is the simplest way to check visibility.
		// The Optional will be empty if the paragraph is not currently visible on screen.
		java.util.Optional<javafx.geometry.Bounds> bounds = editor.getCharacterBoundsOnScreen(range.getStart(), range.getEnd());

		// If the bounds are present, it means the text is already visible.
		if (bounds.isPresent()) {
			// The match is visible. Do nothing.
			return;
		}

		// --- If we reach here, the match is NOT visible, so we need to scroll ---

		// The simplest reliable way to scroll with context is to show the paragraph
		// and then request the layout to settle before making further adjustments if needed.
		// For now, showing the paragraph at the top is the most direct approach when it's off-screen.

		// To provide context, we will use a slightly different approach than before,
		// as we can't easily get the viewport height. We will simply scroll a fixed number
		// of lines above the target.

		int matchParagraph = editor.offsetToPosition(range.getStart(), CodeArea.Bias.Forward).getMajor();

		// Scroll to a position 5 lines above the match to provide context.
		// This is a simpler but effective estimation.
		int targetParagraph = matchParagraph - 5;

		// Ensure the target is not negative.
		targetParagraph = Math.max(0, targetParagraph);

		// Use the standard RichTextFX method to scroll.
		editor.showParagraphAtTop(targetParagraph);
	}

	Map<String, String> collectPlaceholders(DocumentSettings settings, Map<String, String> docMetadata) {
		Map<String, String> placeholders = new HashMap<>();

		// 1. System Defaults
		placeholders.put(Placeholder.DATE.getKey(), new SimpleDateFormat("dd.MM.yyyy").format(new Date()));
		placeholders.put(Placeholder.TIME.getKey(), new SimpleDateFormat("HH:mm").format(new Date()));

		// Word count calculation
		String text = (editor != null) ? editor.getText() : "";
		long words = Arrays.stream(text.split("\\s+")).filter(w -> !w.isEmpty()).count();
		placeholders.put(Placeholder.WORD_COUNT.getKey(), String.valueOf(words));
		placeholders.put(Placeholder.READING_TIME.getKey(), Math.max(1, Math.round(words / 200.0)) + " min");
		placeholders.put(Placeholder.RANDOM_ID.getKey(), "ID-" + (System.currentTimeMillis() % 10000));

		// 2. Settings & YAML (Overrides system defaults if keys match)
		placeholders.putAll(settings.asMetaMap()); // Includes custom placeholders
		if (docMetadata != null) {
			placeholders.putAll(docMetadata);
		}

		return placeholders;
	}

	private String applyPlaceholders(String htmlContent, Map<String, String> placeholders) {
		if (htmlContent == null || placeholders.isEmpty())
			return htmlContent;
		String result = htmlContent;

		for (Map.Entry<String, String> entry : placeholders.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();

			// Simpler Null-Check reicht. Leere Strings dürfen ersetzt werden (um Platzhalter zu löschen).
			if (value == null)
				continue;

			// Einfaches String-Replace ist optimiert in der JVM
			result = result.replace("{{" + key + "}}", value);
		}
		return result;
	}

	// --- Action Handlers für die Suche ---

	@FXML
	private void handleSearchButtonAction(ActionEvent event) {
		toggleSearchPopup();
	}

	/**
	 * Toggles the visibility of the search popup and correctly manages focus and state. This replaces the old HBox visibility toggle logic and centralizes search closure.
	 */
	private void toggleSearchPopup() {
		if (searchPopup.isShowing()) {
			// Close the search popup gracefully, which also handles editor cleanup.
			searchPopup.cleanClose(); // <-- Calling the dedicated clean closure method
			editor.requestFocus();

			// Note: handleCloseSearchAction is now implicitly called by searchPopup.cleanClose().
			// We no longer need to call it separately here.

		} else {
			// Open the search popup
			findAndHighlightMatches(); // Initialize search with current text
			searchPopup.show();
			searchPopup.requestFocusOnSearchField(); // Set focus to the popup's text field
		}
	}

	// Wichtig: Diese Methoden sind jetzt private, da sie nur intern via Callback
	// vom CodeAreaSearchPopup gerufen werden. Sie benötigen aber weiterhin den
	// ActionEvent-Parameter für die Methodensignatur.
	private void handleSearchNextAction(ActionEvent event) {
		if (searchResults.isEmpty())
			return;

		currentMatchIndex = (currentMatchIndex + 1) % searchResults.size();
		navigateToMatch(currentMatchIndex);

		// Korrigierter Aufruf des gekapselten UI-Updates
		searchPopup.updateMatchCount(searchResults.size(), currentMatchIndex + 1);
	}

	private void handleSearchPreviousAction(ActionEvent event) {
		if (searchResults.isEmpty())
			return;

		currentMatchIndex--;
		if (currentMatchIndex < 0) {
			currentMatchIndex = searchResults.size() - 1;
		}
		navigateToMatch(currentMatchIndex);

		// Korrigierter Aufruf des gekapselten UI-Updates
		searchPopup.updateMatchCount(searchResults.size(), currentMatchIndex + 1);
	}

	@FXML
	private void handleCloseSearchAction(ActionEvent event) {
		// Reset der internen Listen und des Suchfelds
		currentSearchMatches.clear();
		searchResults.clear();
		currentMatchIndex = -1;

		// Das Suchfeld im Popup muss geleert werden
		if (searchPopup != null) {
			searchPopup.clearSearchField();
			// und die Zähler zurückgesetzt
			searchPopup.updateMatchCount(0, 0);
			searchPopup.updateNavigationButtons(false);
		}

		// Sofortiges Re-Highlighting ohne Suchtreffer
		applyHighlightingInternal(false);
		editor.requestFocus();
		Log.fine("Search state reset: Editor cleared of previous search context.");

		// *** WICHTIG: JETZT DAS POPUP AKTIV VERSTEHEN ***
		// Da der Controller nun die Logik für das Schließen hat,
		// muss ER hier das Popup verstecken.
		if (searchPopup != null) {
			searchPopup.hide(); // <-- DIES IST DIE KORREKTUR, DIE FEHLTE!
		}
	}

	/**
	 * Navigiert (scrollt und selektiert) zu einem bestimmten Match. Nutzt die neue, intelligente Scroll-Methode.
	 * 
	 * @param index Der Index in der searchResults Liste.
	 */
	private void navigateToMatch(int index) {
		if (searchResults.isEmpty() || index < 0 || index >= searchResults.size()) {
			return;
		}

		IndexRange range = searchResults.get(index);

		// 1. Scrolle intelligent zum Match (nur wenn nötig)
		scrollToMatchIntelligently(range);

		// 2. Selektiere das gefundene Wort
		editor.selectRange(range.getStart(), range.getEnd());
	}

	/**
	 * Opens the Configuration Nexus. Implements a persistent companion window pattern to ensure performance and architectural consistency.
	 */
	@FXML
	private void handleProjectSettings(ActionEvent event) {
		// 1. LAZY INITIALIZATION
		if (projectSettingsDialog == null) {
			initializeProjectSettingsDialog();
		}

		// 2. CONTEXT SYNC (Vor dem Anzeigen die aktuellen Daten injizieren)
		// Wir müssen dem bestehenden Dialog die aktuellen Dokument-Daten geben.
		// Dafür braucht der Dialog eine 'refresh' Methode.
		syncCurrentDocumenWithConfigurationDialog();

		// 3. LOCK & SHOW
		setWorkspaceLocked(true);
		projectSettingsDialog.show();
	}

	/**
	 * Forges the persistent ProjectSettingsDialog instance and establishes the permanent event pipeline.
	 */
	private void initializeProjectSettingsDialog() {
		// 1. GATHER STATIC INFRASTRUCTURE
		// These parameters are needed for the initial build and do not change
		// when the user switches documents.
		Path appRootPath = Paths.get(System.getProperty("user.dir"));

		// 2. FORGE THE DIALOG with its permanent dependencies
		projectSettingsDialog = new ProjectSettingsDialog(getPrimaryStage(), // Owner window
		        this.projectSettings, // Global application blueprints (for "Apply Defaults")
		        appRootPath, // Root path for asset management (e.g., headers)
		        preview -> renderMarkdownPreview(preview) // Real-time preview delegate
		);

		// 3. APPLY INITIAL STYLING (Dark Mode)
		if (isDarkMode) {
			projectSettingsDialog.getDialogPane().getStyleClass().addAll("nexus-dialog", "dark-mode");
		}

		// 4. ESTABLISH PERMANENT EVENT PIPELINE
		projectSettingsDialog.setOnHidden(e -> setWorkspaceLocked(false));

		projectSettingsDialog.resultProperty().addListener((obs, old, updated) -> {
			if (updated != null) {
				commitSettingsTransaction(updated);
			} else {
				rollbackSettingsTransaction();
			}
		});

		Log.info("Sovereign Nexus: Permanent dialog infrastructure established.");
	}

	/**
	 * Synchronizes the Nexus with the currently active document's metadata. Also fetches the latest environment data like theme names and formats.
	 */
	private void syncCurrentDocumenWithConfigurationDialog() {
		if (projectSettingsDialog == null)
			return;

		// 1. GATHER ENVIRONMENT DATA (Just-in-Time)
		List<String> themeNames = themeManager.getThemeNames();
		List<DocumentFormat> formatList = Arrays.asList(DocumentFormat.values());

		// 2. GATHER DOCUMENT DATA
		String currentText = editor.getText();
		Map<String, String> currentDocMeta = readYamlMetadata(currentText);

		DocumentSettings documentReality = new DocumentSettings();
		applyYamlOverrides(documentReality, currentDocMeta);

		// 3. INJECT ALL DATA INTO THE PERSISTENT DIALOG
		projectSettingsDialog.updateDocumentData(documentReality, currentDocMeta, themeNames, formatList);
	}

	/**
	 * Commits the user's changes from the Nexus to the application state and the document source.
	 */
	private void commitSettingsTransaction(DocumentSettings updated) {
		this.projectSettings = updated;
		settingsManager.saveSettings(Paths.get(System.getProperty("user.dir")), updated);

		this.forceWebViewReload = true;
		this.cachedMappedHtml = null;

		if (updated.isPersistYaml()) {
			commitMetadataToDocument(updated.asMetaMap());
		} else {
			removeYamlFromDocument();
		}

		// --- HEILUNG: EXPLICIT HIGHLIGHTING REFRESH ---
		// We don't wait for the debouncer here. We force an immediate
		// visual validation to ensure the new YAML block is colored correctly.
		Platform.runLater(() -> {
			applyHighlightingInternal(true); // Full scan including Linter
			Log.info("Nexus: Transaction COMMITTED and visual state synchronized.");
		});
		Log.info("Nexus: Transaction COMMITTED.");
	}

	/**
	 * Discards changes made in the Nexus and reverts the live preview to the document's original state.
	 */
	private void rollbackSettingsTransaction() {
		this.forceWebViewReload = true;
		this.cachedMappedHtml = null;
		Platform.runLater(this::renderMarkdownPreview);
		Log.info("Nexus: Transaction ROLLED BACK.");
	}

	/**
	 * Locks or unlocks the primary workspace to prevent data corruption during non-modal configuration sessions.
	 */
	private void setWorkspaceLocked(boolean locked) {
		// 1. Editor sperren (Schreibschutz)
		editor.setDisable(locked);

		// 2. Sidebar sperren (Verhindert Formatierungen/Speichern)
		sideBar.setDisable(locked);

		// 3. Top-Leiste sperren (außer dem Konfigurator-Button selbst?)
		// Besser: Die ganze rootPane.getTop() Gruppe sperren
		rootPane.getTop().setDisable(locked);

		Log.fine("Workspace Integrity: " + (locked ? "LOCKED" : "UNLOCKED"));
	}

	// --- Action Handlers (FXML Methods) bleiben unverändert ---
	/**
	 * Startet den Prozess, um das System-Theme als Blueprint zu laden. Prüft, ob das Sanktuarium bereits bewohnt ist.
	 */
	@FXML
	private void handleInjectTheme(ActionEvent event) {
		// 1. Wächter: Wir prüfen den Inhalt des dedizierten Style-Editors
		String currentCss = styleEditor.getText().trim();

		if (!currentCss.isEmpty()) {
			// Falls schon CSS da ist -> Sicherheitsabfrage
			Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
			confirm.setTitle("Overwrite Design Blueprint?");
			confirm.setHeaderText("This will replace your current CSS in the Sanctuary.");
			confirm.setContentText("Do you want to proceed with the system default?");
			confirm.getDialogPane().getStyleClass().add("nexus-dialog");
			confirm.initOwner(rootPane.getScene().getWindow());

			if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
				injectSystemTheme(true);
			}
		} else {
			// Sanktuarium ist leer -> Direkt injizieren
			injectSystemTheme(false);
		}
	}

	/**
	 * Injects the default system theme blueprint into the Style Sanctuary (CSS editor). This action replaces any existing content, providing the user with a clean, editable
	 * starting point for theme customization. The method triggers a full UI synchronization cascade upon completion.
	 *
	 * @param isOverwrite A flag (currently informational) confirming that the user has approved overwriting existing styles.
	 */
	private void injectSystemTheme(boolean isOverwrite) {
		// 1. Retrieve the sovereign blueprint from the ThemeManager.
		String themeCss = themeManager.getDefaultThemeBlueprint();

		// 2. Write the blueprint directly into the Style Sanctuary.
		// This automatically triggers highlighting and gutter updates via its listener.
		styleEditor.replaceText(themeCss);

		// 3. Trigger a UI synchronization cascade.
		// Re-renders the live preview to immediately reflect the new default styles.
		renderMarkdownPreview();

		// Mark this state as "clean" in the undo manager.
		styleEditor.getUndoManager().mark();

		Log.info("Sovereign Blueprint successfully injected into Style Sanctuary.");
	}

	@FXML
	private void handleToggleView(ActionEvent event) {
		togglePreviewMode();
	}

	/**
	 * Toggles the preview visibility based on the current UI state. Handles both inline SplitPane view and undocked satellite window view.
	 */
	private void togglePreviewMode() {
		isPreviewMode = viewToggleButton.isSelected();

		if (isPreviewMode) {
			if (isUndocked) {
				previewSatellite.show("FlowShift Live Preview - " + getFileName());
			} else {
				if (!mainSplitPane.getItems().contains(previewContainer)) {
					mainSplitPane.getItems().add(previewContainer);
				}
				previewContainer.setVisible(true);
				previewContainer.setManaged(true);
				Platform.runLater(() -> mainSplitPane.setDividerPositions(persistedPreviewRatio));
			}
			renderMarkdownPreview();
		} else {
			if (isUndocked && previewSatellite != null) {
				previewSatellite.hide();
			} else {
				captureDividerRatio();
				mainSplitPane.getItems().remove(previewContainer);
			}
			editor.requestFocus();
		}
	}

	/**
	 * Captures and persists the current divider position of the main SplitPane. This method ensures that the user's preferred workspace layout is saved before the preview area is
	 * removed or undocked.
	 */
	private void captureDividerRatio() {
		double[] positions = mainSplitPane.getDividerPositions();

		// Ensure at least one divider exists before attempting to read it
		if (positions.length > 0) {
			double currentPos = positions[0];

			// ARCHITECTURAL GUARD:
			// We only persist the ratio if the divider is not already snapped
			// to the extreme right (e.g., > 0.98). This ensures that we don't
			// "save" a state where the preview is effectively invisible.
			if (currentPos < 0.98) {
				this.persistedPreviewRatio = currentPos;
				Log.fine("Sovereign Layout: Divider ratio captured at " + String.format("%.2f", currentPos));
			}
		}
	}

	/**
	 * Transitions the preview from the main SplitPane to the satellite window. Captures the current divider ratio before decoupling to ensure the layout can be perfectly restored
	 * later.
	 */
	@FXML
	private void handleUndockPreview() {
		// 1. GEOMETRY ANCHOR: Capture current ratio before the node disappears
		captureDividerRatio();

		if (previewSatellite == null) {
			previewSatellite = new SovereignPreviewSatellite(getPrimaryStage());
			previewSatellite.getStage().setOnCloseRequest(e -> {
				e.consume(); // Intercept standard close
				redockPreview();
			});
		}

		// 2. NODE TRANSFER: Assign WebView to the satellite
		previewContainer.getChildren().remove(webView);
		previewSatellite.setContent(webView);

		// 3. LAYOUT CLEANUP: Physically remove the container to collapse the gap
		mainSplitPane.getItems().remove(previewContainer);
		isUndocked = true;

		// 4. VISIBILITY SYNC
		if (isPreviewMode) {
			previewSatellite.show("FlowShift Live Preview - " + getFileName());
		}

		Log.info("Sovereign Workspace: Undocked. Layout ratio persisted at " + persistedPreviewRatio);
	}

	/**
	 * Re-integrates the WebView back into the main application window and restores the SplitPane layout. This method executes the structural and geometric changes in a single
	 * synchronous pass to prevent visual flickering.
	 */
	private void redockPreview() {
		if (previewSatellite != null && isUndocked) {
			// 1. EXTRACTION: Remove WebView from the satellite stage's root
			previewSatellite.removeContent(webView);
			previewSatellite.hide();

			// 2. RE-INSERTION: Place WebView back into its original local container
			// Position 0 ensures it remains layered correctly (e.g., under UI buttons)
			previewContainer.getChildren().add(0, webView);
			isUndocked = false;

			// 3. LAYOUT RESTORATION: Re-attach the container to the main SplitPane
			if (isPreviewMode) {
				// We set the target divider position immediately before or during
				// the structural change to ensure the first render pass uses
				// the correct ratio, effectively eliminating the 50/50 "jump".
				mainSplitPane.setDividerPositions(persistedPreviewRatio);

				if (!mainSplitPane.getItems().contains(previewContainer)) {
					mainSplitPane.getItems().add(previewContainer);
				}

				// Final visual reveal within the same UI pulse
				previewContainer.setVisible(true);
				previewContainer.setManaged(true);

				Log.fine(String.format("Sovereign Workspace: Redocked. Viewport anchored at %.2f", persistedPreviewRatio));
			}

			// Return focus to the primary editor for a seamless transition
			editor.requestFocus();
		}
	}

	// Helper for title updates
	private String getFileName() {
		return (currentFile != null) ? currentFile.getName() : "Untitled";
	}

	/**
	 * Orchestrates the Design Mode toggle from UI events. Synchronizes the state between the sidebar toggle button and the internal "Close" (X) button within the design panel
	 * header.
	 * 
	 * @param event The ActionEvent triggered by either the sidebar button or the panel's close button.
	 */
	@FXML
	private void handleToggleDesignMode(ActionEvent event) {
		// HEALING: If the event was triggered by the "X" button inside the panel header,
		// we must manually deselect the sidebar toggle button to maintain UI consistency.
		if (event.getSource() != designModeButton) {
			designModeButton.setSelected(false);
		}

		// Execute the core state transition logic
		toggleDesignMode();
	}

	/**
	 * Switches the Sovereign Design Nexus (CSS Forge) on or off.
	 * 
	 * DESIGN PHILOSOPHY (v9.2 Node-Sovereignty): Instead of just hiding the pane, this method physically manipulates the children of the 'leftSplitPane'. This prevents JavaFX from
	 * rendering empty separators and ensures a stable, distraction-free geometry.
	 */
	private void toggleDesignMode() {
		// Synchronize internal state with the sidebar toggle button
		isDesignMode = designModeButton.isSelected();

		if (isDesignMode) {
			// --- ACTIVATE DESIGN MODE ---

			// [Kassandra Note: Removed automatic preview activation (previously 'Visual Consistency' block).
			// The architect wants full, independent control over preview visibility.
			// The user can manually enable the preview via its dedicated toggle if needed for design.
			// This respects the principle of user autonomy (d01, teaching_12) and avoids overriding user preferences.]

			// 1. SOVEREIGN INTERVENTION: Physically insert the Design Pane
			// Adding at index 0 places it at the top of the vertical SplitPane.
			if (!leftSplitPane.getItems().contains(designPane)) {
				leftSplitPane.getItems().add(designPane); // Fügt es ans Ende (unten) hinzu
			}

			// Ensure visibility and managed state for correct layout pass
			designPane.setVisible(true);
			designPane.setManaged(true);

			// 2. UI UNFOLDING: Set initial divider position and focus the editor
			Platform.runLater(() -> {
				leftSplitPane.setDividerPositions(0.50); // Initial 30% height for CSS Forge
				styleEditor.requestFocus();
			});

			// 3. INSTANT FEEDBACK: Immediate syntax highlighting for the Forge
			styleEditor.setStyleSpans(0, markdownHighlighter.highlightPureCss(styleEditor.getText(), true));

		} else {
			// --- DEACTIVATE DESIGN MODE ---

			// 1. SOUVERÄNE BEREINIGUNG: Physically remove the pane from the layout.
			// This instantly collapses the SplitPane gap and removes the separator.
			leftSplitPane.getItems().remove(designPane);

			designPane.setVisible(false);
			designPane.setManaged(false);

			// 2. Return focus to the primary Markdown editor
			editor.requestFocus();
		}

		// --- SYSTEM INTEGRITY REFRESH ---
		// Re-evaluate the primary editor's highlighting. This is essential to
		// update class-based linter errors (Missing CSS Class) immediately after
		// the CSS Sanctuary has been modified or closed.
		applyHighlightingInternal(true);

		Log.info("Sovereign Design Nexus: Mode " + (isDesignMode ? "ENABLED" : "DISABLED"));
	}

	@FXML
	private void handleBoldAction(ActionEvent event) {
		applyMarkdownFormatting("**", "**");
	}

	@FXML
	private void handleItalicAction(ActionEvent event) {
		applyMarkdownFormatting("*", "*");
	}

	@FXML
	private void handleStrikethroughAction(ActionEvent event) {
		applyMarkdownFormatting("~~", "~~");
	}

	@FXML
	private void handleInlineCodeAction(ActionEvent event) {
		applyMarkdownFormatting("`", "`");
	}

	@FXML
	private void handleBlockquoteAction(ActionEvent event) {
		applyLinePrefix("> ");
	}

	@FXML
	private void handleCodeBlockAction(ActionEvent event) {
//	    IndexRange selection = editor.getSelection();
		String selectedText = editor.getSelectedText();

		// Prüfen, ob die Selektion über mehrere Zeilen geht
		boolean isMultiLine = selectedText.contains("\n");

		if (isMultiLine || selectedText.isEmpty()) {
			// Fall B: Block-Modus
			applyMarkdownFormatting("```\n", "\n```");
		} else {
			// Fall A: Inline-Modus (Das ist der Fix für deine Selektion!)
			applyMarkdownFormatting("`", "`");
		}
	}

	@FXML
	private void handleBulletListAction(ActionEvent event) {
		applyLinePrefix("* ");
	}

	@FXML
	private void handleOrderedListAction(ActionEvent event) {
		applyLinePrefix("1. ");
	}

	@FXML
	private void handleTaskListAction(ActionEvent event) {
		applyLinePrefix("- [ ] ");
	}

	@FXML
	private void handleHrAction(ActionEvent event) {
		insertText("\n\n---\n\n");
	}
	// =============================================================================================
	// SOUVERÄNE ZEITLINIEN-STEUERUNG (v9.1 Master)
	// =============================================================================================

	/**
	 * Führt ein globales Undo aus. Navigiert rückwärts durch die Kausalitätskette beider Editoren.
	 */
	@FXML
	private void handleUndoAction() {
		while (!activityStack.isEmpty()) {
			CodeArea currentTarget = activityStack.peek();

			if (currentTarget.isUndoAvailable()) {
				currentTarget.undo();
				break; // Erfolg: Wir haben die letzte Tat ungeschehen gemacht.
			} else {
				// Dieser Editor ist am Anfang seiner Historie.
				if (activityStack.size() > 1) {
					// Wir schieben ihn in die "Zukunft", damit Redo ihn wiederfindet.
					redoActivityStack.push(activityStack.pop());
				} else {
					break; // Den letzten Anker (Markdown) niemals löschen.
				}
			}
		}
		updateUndoRedoButtonStates();
	}

	/**
	 * Führt ein globales Redo aus. Navigiert vorwärts durch die ungeschehen gemachten Taten beider Editoren.
	 */
	@FXML
	private void handleRedoAction() {
		// 1. PRIORITÄT: Der aktuelle Editor hat noch lokale Redos.
		if (!activityStack.isEmpty() && activityStack.peek().isRedoAvailable()) {
			activityStack.peek().redo();
		}
		// 2. PRIORITÄT: Wir müssen die Grenze in die "Zukunft" überschreiten.
		else if (!redoActivityStack.isEmpty()) {
			CodeArea nextTarget = redoActivityStack.pop();
			activityStack.push(nextTarget); // Zurück in die aktive Gegenwart.
			nextTarget.redo();
		}
		updateUndoRedoButtonStates();
	}

	/**
	 * Synchronisiert die Sidebar-Buttons mit der globalen Zeitlinie. Berücksichtigt sowohl lokale Historien als auch kross-editoriale Sprünge.
	 */
	private void updateUndoRedoButtonStates() {
		// --- UNDO LOGIK ---
		// Wir können rückgängig machen, wenn irgendwo eine Historie existiert
		boolean canUndo = editor.isUndoAvailable() || styleEditor.isUndoAvailable();

		// --- REDO LOGIK ---
		// Wir können vorwärts gehen, wenn:
		// 1. Der aktuelle Editor noch lokale Redos hat
		// 2. ODER wenn wir einen Editor im redoActivityStack geparkt haben
		boolean canRedo = editor.isRedoAvailable() || styleEditor.isRedoAvailable() || !redoActivityStack.isEmpty();

		undoButton.setDisable(!canUndo);
		redoButton.setDisable(!canRedo);
	}

	@FXML
	private void handleLinkAction(ActionEvent event) {
		showLinkDialog(UIText.INSERT_LINK_DIALOG, "[", "](", ")");
	}

	@FXML
	private void handleImageAction(ActionEvent event) {
		showLinkDialog(UIText.INSERT_IMAGE_DIALOG, "![", "](", ")");
	}

	@FXML
	private void handleInsertPageBreak(ActionEvent event) {
		// Ein sauberer Block mit Leerzeilen davor und danach für die Markdown-Lesbarkeit
		String snippet = "\n\n<div class=\"page-break\"></div>\n\n";
		insertText(snippet);
		Log.info("Manual page break inserted at caret position.");
	}

	/**
	 * Helper to show a dialog for inserting links or images.
	 * 
	 * @param title       Window title (e.g. "Insert Link")
	 * @param contentText Label text (e.g. "URL:")
	 * @param prefix      Markdown prefix (e.g. "[" or "![")
	 * @param middle      Markdown middle part (e.g. "](")
	 * @param suffix      Markdown suffix (e.g. ")")
	 */
	private void showLinkDialog(UIText dialogType, String prefix, String middle, String suffix) {
		String selectedText = editor.getSelectedText();

		// Nutze die neue, gestylte TextInputDialog-Methode von WindowUtils
		TextInputDialog dialog = utils.ui.WindowUtils.createTextInputDialog(getPrimaryStage(), dialogType.getTitle(), dialogType.getHeader(), dialogType.getContent(), // Dieser
		                                                                                                                                                               // Text wird
		                                                                                                                                                               // jetzt als
		                                                                                                                                                               // Label
		                                                                                                                                                               // oberhalb
		                                                                                                                                                               // des
		                                                                                                                                                               // Textfeldes
		                                                                                                                                                               // angezeigt
		        "https://" // Initialer Wert für das Textfeld
		);

		dialog.showAndWait().ifPresent(url -> {
			if (!url.isBlank()) {
				String fallbackText = prefix.contains("!") ? "Image" : "Text";
				String linkText = selectedText.isEmpty() ? fallbackText : selectedText;

				String replacement = prefix + linkText + middle + url + suffix;

				editor.replaceSelection(replacement);
				renderMarkdownPreview();
			}
		});

		editor.requestFocus();
	}

	@FXML
	private void handleHeadingAction(ActionEvent event) {
		if (event.getSource() instanceof MenuItem menuItem) {
			try {
				int level = Integer.parseInt(menuItem.getUserData().toString());
				level = Math.max(1, Math.min(level, 6));

				// Neue konsistente Methode verwenden
				applyHeadingToCurrentOrSelectedLines(level, false);

			} catch (NumberFormatException | NullPointerException e) {
				System.err.println("Invalid heading level data: " + e.getMessage());
			}
		}
	}

	@FXML
	private void handleRemoveHeadingAction(ActionEvent event) {
		// Gleiche Methode wie für setzen, nur mit remove=true
		applyHeadingToCurrentOrSelectedLines(0, true);
	}

	@FXML
	private void handleOpenDocumentAction() {
		if (!canDiscardCurrentDocument())
			return;

		File initialDir = getDocumentVaultPath().toFile();

		// Definiere die Filter für Markdown- und Textdateien.
		List<FileChooser.ExtensionFilter> filters = List.of(new FileChooser.ExtensionFilter(UIText.FILE_CHOOSER_FILTER_MD.getLabel(), "*.md", "*.txt"));

		// Nutze WindowUtils, um den Öffnen-Dateidialog anzuzeigen.
		File selectedFile = utils.ui.WindowUtils.openFileChooser(getPrimaryStage(), // Owner Stage
		        UIText.FILE_CHOOSER_OPEN_TITLE.getLabel(), // Titel
		        initialDir, // Initialverzeichnis
		        filters, // Spezifische Markdown/Text Filter
		        true, // "Alle Dateien" Filter hinzufügen
		        true // "Alle kompatiblen Dateien" Filter hinzufügen (kombiniert *.md, *.txt)
		);

		if (selectedFile != null) { // Prüfe, ob der Nutzer eine Datei ausgewählt hat und nicht abgebrochen wurde
			try {
				String raw = Files.readString(selectedFile.toPath(), StandardCharsets.UTF_8);
				loadDocumentToUI(raw, selectedFile, false); // isTemplate = false
			} catch (IOException e) {
				showErrorAlert("Load Error", "Failed to read file.", e, getPrimaryStage());
			}
		}
	}

	@Override
	public void onVaultDocumentOpen(String fileName) {
		if (fileName == null)
			return;
		if (!canDiscardCurrentDocument())
			return;

		Path filePath = getDocumentVaultPath().resolve(fileName);
		try {
			String raw = Files.readString(filePath, StandardCharsets.UTF_8);
			loadDocumentToUI(raw, filePath.toFile(), false); // isTemplate = false
			if (libraryWindow != null)
				libraryWindow.close();
		} catch (IOException e) {
			showErrorAlert("Vault Error", "Failed to load document.", e, getPrimaryStage());
		}
	}

	@Override
	public boolean onTemplateLoad(String name) {
		if (name == null)
			return false;

		if (canDiscardCurrentDocument()) {
			String raw = templateManager.getTemplateContent(name);
			// Wir übergeben ein virtuelles File-Objekt für den Namen
			loadDocumentToUI(raw, new File(name), true); // isTemplate = true
			return true;
		}
		return false;
	}

	// --- PERFORMANCE CONSTANTS: REGEX & MARKERS ---
	private static final Pattern STYLE_TAG_PATTERN = Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>", Pattern.CASE_INSENSITIVE);

	private static final Pattern LEGACY_CSS_MARKER_PATTERN = Pattern.compile("/\\* --- (Start Extracted|End of) Block #\\d+ --- \\*/\\n?");

	private static final String CSS_HEADER_START = "/* --- Start Extracted Block #";
	private static final String CSS_HEADER_END   = " --- */\n";
	private static final String CSS_FOOTER_END   = "/* --- End of Block #";
	private static final String UNTITLED_LABEL   = "Unknown";

	/**
	 * Orchestrates the document loading process with high-performance stream processing. Surgically separates Markdown content from CSS payloads and restores session context.
	 */
	private void loadDocumentToUI(String rawContent, File file, boolean isTemplate) {
		if (rawContent == null)
			return;

		// Pre-allocate buffer to avoid repeated resizing
		StringBuilder markdownBuffer = new StringBuilder(rawContent.length());
		List<String> cssBlocks = new ArrayList<>();

		// 1. STYLE EXTRACTION (Using pre-compiled patterns)
		Matcher styleMatcher = STYLE_TAG_PATTERN.matcher(rawContent);
		int lastReadPos = 0;

		while (styleMatcher.find()) {
			markdownBuffer.append(rawContent, lastReadPos, styleMatcher.start());

			// Extract and clean CSS content
			String css = styleMatcher.group(1).trim();
			css = LEGACY_CSS_MARKER_PATTERN.matcher(css).replaceAll("").trim();

			if (!css.isEmpty()) {
				cssBlocks.add(css);
			}
			lastReadPos = styleMatcher.end();
		}
		markdownBuffer.append(rawContent, lastReadPos, rawContent.length());

		// 2. CSS AGGREGATION
		StringBuilder aggregatedCss = new StringBuilder();
		int blockCount = cssBlocks.size();

		if (blockCount == 1) {
			aggregatedCss.append(cssBlocks.get(0));
		} else if (blockCount > 1) {
			for (int i = 0; i < blockCount; i++) {
				aggregatedCss.append(CSS_HEADER_START).append(i + 1).append(CSS_HEADER_END).append(cssBlocks.get(i)).append("\n").append(CSS_FOOTER_END).append(i + 1)
				        .append(CSS_HEADER_END).append("\n");
			}
		}

		// Update UI components with trimmed content
		editor.replaceText(markdownBuffer.toString().trim());
		styleEditor.replaceText(aggregatedCss.toString().trim());

		// 3. CONTEXT RESTORATION
		this.projectSettings = settingsManager.loadSettings();
		ensureHeaderManager();
		if (headerManager != null) {
			String styleName = projectSettings.getActiveHeaderStyle();
			projectSettings.setHeaderHtml(headerManager.loadTemplate(styleName));
		}

		// 4. STATE SYNCHRONIZATION
		this.currentFile = file;
		this.isEditingTemplate = isTemplate;
		this.saveRequested = isTemplate;

		if (file != null && !isTemplate && file.getParentFile() != null) {
			this.lastUsedDirectory = file.getParentFile();
			saveLastUsedDirectory();
		}

		// --- HEILUNG: WebView Scroll-Position bei neuem Dokument zurücksetzen ---
		this.lastWebViewScrollY = 0; // Setze die letzte Scroll-Position auf 0
		this.restoreScrollRequested = false; // Deaktiviere den Scroll-Wiederherstellungswunsch
		// -------------------------------------------------------------------

		// 5. VIEWPORT & HIGHLIGHTING ORCHESTRATION
		if (markdownHighlighter != null) {
			styleEditor.setStyleSpans(0, markdownHighlighter.highlightPureCss(styleEditor.getText(), true));
		}

		applyHighlightingInternal(true);
		renderMarkdownPreview();
		updateSearchAvailability(true);

		// Final geometry and chronology reset
		resetViewportsToOrigin();
		resetDocumentState();

		Log.info(String.format("Sovereign Load Engine: %s initialized.", file != null ? file.getName() : UNTITLED_LABEL));
	}

	/**
	 * Resets the editor viewports to the absolute origin (Top-Left). Ensures that a newly loaded document starts at Line 1, Column 1.
	 */
	private void resetViewportsToOrigin() {
		Platform.runLater(() -> {
			// 1. Position the Caret at the start of the text
			editor.moveTo(0);
			styleEditor.moveTo(0);

			// 2. Force Vertical and Horizontal scroll to zero
			// We use setValue(0.0) on the estimated properties for 0.11.7 compatibility
			if (codeAreaScrollPane != null) {
				codeAreaScrollPane.estimatedScrollYProperty().setValue(0.0);
				codeAreaScrollPane.estimatedScrollXProperty().setValue(0.0);
			}

			if (styleEditorScrollPane != null) {
				styleEditorScrollPane.estimatedScrollYProperty().setValue(0.0);
				styleEditorScrollPane.estimatedScrollXProperty().setValue(0.0);
			}

			// 3. Ensure the caret remains visible at the top
			editor.requestFollowCaret();

			Log.fine("Sovereign Geometry: Viewports successfully reset to origin.");
		});
	}

	/**
	 * Public entry point for external applications to load a document. This method orchestrates the entire loading process.
	 * 
	 * @param fileToLoad The Markdown file to open.
	 */
	public void loadFile(File fileToLoad) {
		if (fileToLoad == null || !fileToLoad.exists()) {
			showErrorAlert("Load Error", "File not found or is inaccessible.", null);
			return;
		}

		// Use the existing logic to check for unsaved changes before proceeding
		if (!canDiscardCurrentDocument()) {
			return; // User cancelled
		}

		try {
			String rawContent = Files.readString(fileToLoad.toPath(), StandardCharsets.UTF_8);
			// Delegate to your existing, powerful loading method
			loadDocumentToUI(rawContent, fileToLoad, false); // isTemplate = false
		} catch (IOException e) {
			showErrorAlert("Load Error", "Failed to read content from file.", e);
		}
	}

	/**
	 * Führt den "Tabula Rasa"-Protokoll aus. Setzt die globale Zeitlinie und die Undo-Manager für beide Editoren zurück. Dies ist der souveräne Startpunkt für jedes neu geladene
	 * Dokument.
	 */
	private void resetDocumentState() {
		// 1. Zeitlinien löschen und neu initialisieren
		activityStack.clear();
		redoActivityStack.clear();
		activityStack.push(editor); // Markdown ist der neue Startpunkt

		// 2. Historien beider Editoren löschen und einen sauberen Startpunkt setzen
		editor.getUndoManager().forgetHistory();
		editor.getUndoManager().mark();

		styleEditor.getUndoManager().forgetHistory();
		styleEditor.getUndoManager().mark();

		// 3. UI-Buttons auf den neuen, sauberen Zustand synchronisieren
		updateUndoRedoButtonStates();

		Log.fine("Sovereign State Reset: Timelines and UndoManagers cleared for new document.");
	}

	/**
	 * Handles all "Save" actions, whether triggered by button, menu, or shortcut. Orchestrates the saving process and provides visual success feedback. This method serves as the
	 * central entry point for all save operations.
	 */
	@FXML
	private boolean handleSaveAction() { // Von void auf boolean geändert
		boolean success;

		// Bestimme die passende Speicher-Operation
		if (currentFile != null && !isEditingTemplate) {
			// Direktes Speichern
			success = saveContentToFile(currentFile);
		} else {
			// Erstmals Speichern oder Template -> "Speichern unter" erzwingen
			success = handleSaveAsAction();
		}

		// Feedback & Zustands-Synchronisation
		if (success) {
			showSaveSuccessFeedback(true);
		} else {
			updateDirtyState();
		}

		return success; // WICHTIG: Gibt den Erfolg an den Aufrufer zurück
	}

	/**
	 * Executes the "Save As..." process. This method displays the native FileChooser and manages the transition of an unsaved template state to a persistent file state.
	 * 
	 * @return {@code true} if the save operation (including file selection) was successful; {@code false} if the operation failed or was cancelled by the user.
	 */
	@FXML
	private boolean handleSaveAsAction() {
		File initialDir = getDocumentVaultPath().toFile();

		List<FileChooser.ExtensionFilter> filters = List.of(new FileChooser.ExtensionFilter(UIText.FILE_CHOOSER_FILTER_MD.getLabel(), "*.md"));

		File selectedFile = utils.ui.WindowUtils.saveFileChooser(getPrimaryStage(), // 1. Owner Stage
		        UIText.FILE_CHOOSER_SAVE_TITLE.getLabel(), // 2. Titel
		        initialDir, // 3. Initialverzeichnis
		        null, // 4. NEU: initialFileName (hier null, da der Nutzer den Namen wählt)
		        filters, // 5. Spezifische Markdown Filter
		        true, // 6. "Alle Dateien" Filter hinzufügen
		        true // 7. "Alle kompatiblen Dateien" Filter hinzufügen
		);

		if (selectedFile != null) { // Prüfe, ob der Nutzer eine Datei ausgewählt hat
			boolean success = saveContentToFile(selectedFile);
			if (success) {
				isEditingTemplate = false;
				Log.info("File saved as new: %s", selectedFile.getName());
			}
			return success;
		}
		return false; // Nutzer hat abgebrochen
	}

	/**
	 * Returns the {@link Path} to the local document vault directory ({@code user.dir}/documents/). This is the preferred storage location for all active documents. Ensures the
	 * directory exists.
	 * 
	 * @return The absolute path to the document vault.
	 */
	private Path getDocumentVaultPath() {
		Path path = Paths.get(System.getProperty("user.dir"), "documents");
		// Ensure the directory exists before returning its path
		try {
			Files.createDirectories(path);
		} catch (IOException e) {
			Log.error(e, "FATAL: Could not create Document Vault directory.");
			// In a critical failure, we might want to throw a runtime exception or handle it more robustly.
			// For now, logging and returning a path that might not exist is acceptable.
		}
		return path;
	}

	/**
	 * Writes the current editor content to a specified file. Guarantees file integrity by enforcing the {@code .md} extension if missing.
	 * 
	 * @param file The target {@link File} to which content will be written.
	 * @return {@code true} if the file was written successfully; {@code false} otherwise.
	 */
	private boolean saveContentToFile(File file) {
		String fileName = file.getName().toLowerCase();

		// --- SOVEREIGN EXTENSION ENFORCEMENT ---
		// Ensures that the file always has a valid Markdown (.md) or text (.txt) extension.
		if (!fileName.contains(".") || (!fileName.endsWith(".md") && !fileName.endsWith(".txt"))) {
			file = new File(file.getAbsolutePath() + ".md");
			Log.fine("File extension enforced: changed to .md");
		}

		// Combine Markdown and CSS Forge content for persistent storage
		String markdown = editor.getText().trim();
		String css = styleEditor.getText().trim();
		String output = markdown + "\n\n<style>\n" + css + "\n</style>\n";

		try {
			Files.writeString(file.toPath(), output, StandardCharsets.UTF_8);

			this.currentFile = file;
			this.lastUsedDirectory = file.getParentFile();
			saveLastUsedDirectory();

			// Mark both editors' UndoManagers as clean (saved state)
			editor.getUndoManager().mark();
			styleEditor.getUndoManager().mark();

			updateStatusLabels(); // Refresh status bar labels

//			// --- NUR HIER: ERFOLGS-FEEDBACK ---
//			// Das grüne Häkchen kommt nur, wenn wir bis hierhin ohne Exception gekommen sind.
//			showSaveSuccessFeedback(true);

			return true;
		} catch (IOException e) {
			Log.error(e, "Error saving file to: %s", file.getAbsolutePath());
			showErrorAlert("Save Error", "Critical: Could not write file to disk.", e, getPrimaryStage());
			return false; // Signal failure to the calling method
		}
	}

	/**
	 * Provides a professional, non-intrusive visual confirmation of a successful save. Overlays the save button with a temporary checkmark label that is dynamically sized. This
	 * method should ONLY be called if the save operation was truly successful.
	 * 
	 * @param success Indicates whether the save operation completed without errors.
	 */
	private void showSaveSuccessFeedback(boolean success) {
		if (saveButton == null || saveSuccessLabel == null)
			return;

		// If success is false, the error has already been handled by showErrorAlert.
		if (!success) {
			Log.warn("Save success feedback skipped: Operation was not successful.");
			return;
		}

		// 1. Capture original button dimensions for precise overlay sizing
		double buttonWidth = saveButton.getWidth();
		double buttonHeight = saveButton.getHeight();

		// 2. Prepare the success label (text and graphic)
		saveSuccessLabel.setText(UIText.SAVE_SUCCESS_FEEDBACK.getLabel());

		FontIcon checkIcon = new FontIcon("fas-check");
		checkIcon.setIconSize(ICON_SIZE);
		checkIcon.setIconColor(Color.web("#2ECC71"));
		saveSuccessLabel.setGraphic(checkIcon);

		saveSuccessLabel.setStyle("-fx-text-fill: #2ECC71; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-color: transparent;");

		// 3. Dynamically size the label to perfectly match the button
		saveSuccessLabel.setPrefWidth(buttonWidth);
		saveSuccessLabel.setPrefHeight(buttonHeight);
		saveSuccessLabel.setMaxWidth(buttonWidth);
		saveSuccessLabel.setMaxHeight(buttonHeight);

		// 4. Perform visual swap (hide button, show and fade-in label)
		saveButton.setVisible(false);
		saveButton.setManaged(false); // Crucial for layout
		saveSuccessLabel.setVisible(true);
		saveSuccessLabel.setManaged(true); // Crucial for layout
		saveSuccessLabel.setOpacity(0.0); // Start invisible for fade-in

		FadeTransition fadeIn = new FadeTransition(Duration.millis(300), saveSuccessLabel);
		fadeIn.setFromValue(0.0);
		fadeIn.setToValue(1.0);
		fadeIn.play();

		// 5. Schedule fade-out and state restoration
		PauseTransition displayDuration = new PauseTransition(Duration.seconds(1.0));
		displayDuration.setOnFinished(e -> {
			FadeTransition fadeOut = new FadeTransition(Duration.millis(500), saveSuccessLabel);
			fadeOut.setFromValue(1.0);
			fadeOut.setToValue(0.0);
			fadeOut.setOnFinished(f -> {
				// Revert visual swap (hide label, show button)
				saveSuccessLabel.setVisible(false);
				saveSuccessLabel.setManaged(false);
				saveButton.setVisible(true);
				saveButton.setManaged(true);

				// Final state synchronization: Ensure button is correctly disabled if clean
				updateDirtyState();
			});
			fadeOut.play();
		});
		displayDuration.play();
	}

	/**
	 * Erstellt den Vault-Ordner, falls er nicht existiert.
	 */
	private void ensureDocumentVaultExists() {
		try {
			Files.createDirectories(getDocumentVaultPath());
		} catch (IOException e) {
			Log.error(e, "FATAL: Could not create Document Vault directory.");
		}
	}

	/**
	 * Sets the current file and automatically updates lastUsedDirectory
	 */
	private void setCurrentFile(File file) {
		this.currentFile = file;
		if (file != null) {
			this.lastUsedDirectory = file.getParentFile();
			saveLastUsedDirectory();
		}
	}

	private String getExportInitialFileName() {
		String name = (currentFile != null) ? currentFile.getName().replaceFirst("[.][^.]+$", "") : "export";
		return name.replaceAll("[^a-zA-Z0-9.-]", "_") + ".html";
	}

	private String generateCleanHtml(String mdContent) {
		if (mdContent == null || mdContent.isEmpty())
			return "";

		// 1. DATA CASCADE
		Map<String, String> docMetadata = readYamlMetadata(mdContent);
		DocumentSettings tempSettings = new DocumentSettings(projectSettings);
		applyYamlOverrides(tempSettings, docMetadata);

		// 2. RENDERING
		String[] ext = extractAndRemoveCss(mdContent);
		String htmlBody = htmlRenderer.render(markdownParser.parse(preprocessTemplate(ext[0])));
		// SOUVERÄNE TRANSFORMATION
		htmlBody = wrapTopLevelOrphanText(htmlBody);
		htmlBody = applyPlaceholders(escapeUnicodeForHtml(htmlBody), collectPlaceholders(tempSettings, docMetadata));

		// 3. SLICER INJECTION
		String slicerJs = "";
		// HEILUNG: Wir nutzen tempSettings, da dort die YAML-Overrides bereits drin sind
		if (tempSettings.isPaginated()) {
			double pageHeight = tempSettings.getFormat().heightInMm();
			double marginTop = tempSettings.getMarginTop();
			double marginBottom = tempSettings.getMarginBottom();

			// HEILUNG: Aufruf mit allen 3 benötigten Parametern
			slicerJs = "<script>" + themeManager.getSlicerScript(pageHeight, marginTop, marginBottom) + "</script>";
		}

		// 4. FINAL ASSEMBLY
		// Wir übergeben slicerJs als dritten Parameter (scriptBlock)
		return buildCompleteHtml(ext[1], htmlBody, slicerJs, tempSettings, docMetadata, true);
	}

	/**
	 * Analysiert den HTML-Stream und verpackt nur Text in
	 * <p>
	 * , der sich auf der obersten Ebene befindet und nicht bereits von Block-Elementen umschlossen ist.
	 */
	private String wrapTopLevelOrphanText(String html) {
		if (html == null || html.isEmpty())
			return html;

		StringBuilder result = new StringBuilder();
		// Regex erkennt alle HTML-Tags
		Pattern tagPattern = Pattern.compile("<(/?[a-zA-Z0-9]+).*?>");
		Matcher matcher = tagPattern.matcher(html);

		int lastEnd = 0;
		int depth = 0; // Verfolgt die Verschachtelungstiefe

		while (matcher.find()) {
			// 1. Hole den Text ZWISCHEN den Tags
			String textBetween = html.substring(lastEnd, matcher.start());

			// 2. Wenn wir auf Ebene 0 sind und der Text nicht leer ist -> Einwickeln!
			if (depth == 0 && !textBetween.trim().isEmpty()) {
				result.append("<p>").append(textBetween.trim()).append("</p>");
			} else {
				result.append(textBetween);
			}

			String fullTag = matcher.group(0);
			String tagName = matcher.group(1).toLowerCase();

			// 3. Stack-Logik: Tiefe anpassen
			if (fullTag.startsWith("</")) {
				depth = Math.max(0, depth - 1);
			} else if (!fullTag.endsWith("/>") && !isSelfClosingTag(tagName)) {
				// Nur Tags, die keine Self-Closer sind (wie <br> oder <img>), erhöhen die Tiefe
				depth++;
			}

			result.append(fullTag);
			lastEnd = matcher.end();
		}

		// --- ENDE DER WHILE-SCHLEIFE ---

		// 4. Den restlichen Text nach dem letzten Tag verarbeiten
		String tail = html.substring(lastEnd);

		// WICHTIG: Wir prüfen mit trim(), ob echter Text da ist,
		// aber wir hängen das ORIGINAL (inkl. Leerzeilen) an!
		if (depth == 0 && !tail.trim().isEmpty()) {
			result.append("<p>").append(tail).append("</p>");
		} else {
			result.append(tail);
		}

		return result.toString();
	}

	// Hilfsmethode für die Stack-Integrität
	private boolean isSelfClosingTag(String tagName) {
		return List.of("br", "hr", "img", "input", "link", "meta", "base", "area").contains(tagName);
	}

	@FXML
	private void handleExportHtmlAction() {
		File initialDir = getExportsVaultPath().toFile();

		File file = utils.ui.WindowUtils.saveFileChooser(getPrimaryStage(), // Owner Stage
		        UIText.FILE_CHOOSER_EXPORT_TITLE.getLabel(), // Titel
		        initialDir, // Initialverzeichnis
		        getExportInitialFileName(), // NEU: initialName hinzufügen
		        List.of(new FileChooser.ExtensionFilter(UIText.FILE_CHOOSER_FILTER_HTML.getLabel(), "*.html")), // KORREKTUR: Filter in List.of()
		        true, // "Alle Dateien" Filter hinzufügen
		        true // "Alle kompatiblen" Filter hinzufügen
		);

		if (file != null) { // NUR ausführen, wenn der Nutzer eine Datei ausgewählt hat.
			try {
				// 1. DATEN-AKQUISE
				String markdown = editor.getText();
				String shadowCss = styleEditor.getText();
				Map<String, String> docMetadata = readYamlMetadata(markdown);

				DocumentSettings exportSettings = new DocumentSettings(projectSettings);
				applyYamlOverrides(exportSettings, docMetadata);

				// 2. SOUVERÄNE ARCHITEKTUR: CLEAN RENDERER (Ohne data-line Ballast)
				MutableDataSet exportOptions = new MutableDataSet(markdownParser.getOptions());
				exportOptions.set(HtmlRenderer.SOURCE_POSITION_ATTRIBUTE, "");
				exportOptions.set(HtmlRenderer.SOURCE_POSITION_PARAGRAPH_LINES, false);

				HtmlRenderer cleanRenderer = HtmlRenderer.builder(exportOptions).build();
				String htmlBody = cleanRenderer.render(markdownParser.parse(markdown));

				// 3. POST-PROCESSING
				htmlBody = escapeUnicodeForHtml(htmlBody);
				Map<String, String> placeholders = collectPlaceholders(exportSettings, docMetadata);
				String finalHtmlBody = applyPlaceholders(htmlBody, placeholders);

				// 4. SKRIPT-ORCHESTRIERUNG (Slicer & Interaction)
				String slicerJs = "";
				if (exportSettings.isPaginated()) {
					slicerJs = "<script>" + themeManager.getSlicerScript(exportSettings.getFormat().heightInMm(), exportSettings.getMarginTop(), exportSettings.getMarginBottom())
					        + "</script>";
				}
				String exportScripts = themeManager.getInteractionScriptForExport() + slicerJs;

				// 5. FINALE ASSEMBLIERUNG
				String finalHtml = buildCompleteHtml(shadowCss, finalHtmlBody, exportScripts, exportSettings, docMetadata, true);

				// 6. DATEI-OPERATION
				Files.writeString(file.toPath(), finalHtml, StandardCharsets.UTF_8);

				this.lastUsedDirectory = file.getParentFile();
				saveLastUsedDirectory();

				showExportSuccessDialog(file);
				Log.info("Sovereign Export: Clean HTML saved and directory persisted.");

			} catch (IOException e) {
				Log.error(e, "Export Error: Technical failure during clean rendering.");
				showErrorAlert("Export Error", "Failed to create clean HTML export.", e);
			}
		}
	}

	/**
	 * Applies metadata from a YAML block (docMetadata) as overrides to a DocumentSettings object. Uses the Placeholder enum as a sovereign guide for mapping.
	 */
	private void applyYamlOverrides(DocumentSettings settings, Map<String, String> docMetadata) {
		if (docMetadata == null || docMetadata.isEmpty())
			return;

		docMetadata.forEach((key, value) -> {
			Placeholder.fromKey(key).ifPresent(p -> {
				try {
					switch (p) {
					case DOCUMENT_TITLE -> settings.setDocumentTitle(value);
					case COMPANY_NAME   -> settings.setCompanyName(value);
					case LOGO_URL       -> settings.setLogoUrl(value);
					case AUTHOR_NAME    -> settings.setAuthorName(value);
					case DEPARTMENT     -> settings.setDepartment(value);
					case VERSION        -> settings.setVersion(value);
					case STATUS         -> settings.setStatus(value);
					case FORMAT         -> settings.setFormat(DocumentFormat.fromString(value));
					case PAGINATION     -> settings.setPaginated(Boolean.parseBoolean(value.trim().toLowerCase()));

					case HEADER_STYLE -> {
						settings.setActiveHeaderStyle(value);
						if (!"NONE".equals(value)) {
							settings.setHeaderHtml(headerManager.loadTemplate(value));
						}
					}
					// --- DIE HEILUNG: Wir reinigen die numerischen Werte ---
					case MARGIN_TOP, MARGIN_BOTTOM, MARGIN_LEFT, MARGIN_RIGHT -> {
						// Entfernt "mm", "px", Leerzeichen und alles, was keine Zahl oder Punkt ist
						String numericValue = value.replaceAll("[^\\d.]", "");
						double parsedValue = Double.parseDouble(numericValue);

						switch (p) {
						case MARGIN_TOP    -> settings.setMarginTop(parsedValue);
						case MARGIN_BOTTOM -> settings.setMarginBottom(parsedValue);
						case MARGIN_LEFT   -> settings.setMarginLeft(parsedValue);
						case MARGIN_RIGHT  -> settings.setMarginRight(parsedValue);
						default            -> {
						}
						}
					}
					default                                                   -> {
					} // Restliche Keys ignorieren
					}
				} catch (Exception e) {
					Log.warn("Could not apply YAML override for key: " + key);
				}
			});
		});
	}

	@FXML
	private void handlePrintAction(ActionEvent event) {
		// Ruft die intelligente Druck-Logik auf, statt eines fehlenden Dialogs
		smartPrint();
	}

	/**
	 * Smart Printing: Entscheidet basierend auf der aktuellen Ansicht, was gedruckt wird, oder fragt den Benutzer bei Unklarheiten.
	 */
	private void smartPrint() {
		// 1. Wenn die WebView (Vorschau) sichtbar ist, drucken wir diese direkt.
		if (webView.isVisible()) {
			executeHtmlPrint(webView);
			return;
		}

		// 2. Wenn wir im Editor-Modus sind, fragen wir den Benutzer.
		Alert printAlert = new Alert(AlertType.CONFIRMATION);
		printAlert.setTitle(UIText.PRINT.getLabel());
		printAlert.setHeaderText("Druckmodus wählen");
		printAlert.setContentText("Möchten Sie die formatierte Vorschau oder den Markdown-Quellcode drucken?");

		ButtonType btnPreview = new ButtonType("Vorschau (HTML)");
		ButtonType btnCode = new ButtonType("Quellcode (Markdown)");
		ButtonType btnCancel = new ButtonType(UIText.CANCEL.getLabel(), ButtonBar.ButtonData.CANCEL_CLOSE);

		printAlert.getButtonTypes().setAll(btnPreview, btnCode, btnCancel);

		Optional<ButtonType> result = printAlert.showAndWait();

		if (result.isPresent()) {
			if (result.get() == btnPreview) {
				// Wir schalten kurz auf die Vorschau um, rendern und drucken dann
				if (!webView.isVisible()) {
					viewToggleButton.setSelected(true);
					handleToggleView(null);
				}

				// Kurze Pause, damit CSS/Bilder geladen werden können, bevor der Druckdialog kommt
				PauseTransition pause = new PauseTransition(Duration.millis(500));
				pause.setOnFinished(e -> executeHtmlPrint(webView));
				pause.play();

			} else if (result.get() == btnCode) {
				printMarkdownCode();
			}
		}
	}

	/**
	 * Bereitet den rohen Markdown-Quellcode für den Druck vor. Erzeugt ein temporäres, monochromes HTML-Dokument und sendet es nach erfolgreichem Rendering an den
	 * System-Druckdialog.
	 */
	private void printMarkdownCode() {
		final String sourceMarkdown = editor.getText();

		// 1. VALIDIERUNG: Leere Dokumente werden nicht gedruckt
		if (sourceMarkdown == null || sourceMarkdown.trim().isEmpty()) {
			// DER FIX: Wir nutzen die neue, universelle showAlert-Methode
			showAlert(Alert.AlertType.WARNING, "Print Error", "The document is empty", "There is no source code content available to be printed.");
			return;
		}

		// 2. RENDERING-VORBEREITUNG (Offline-WebView)
		// Wir nutzen eine temporäre WebView, um das Dokument im Hintergrund
		// für den Drucker zu setzen.
		final WebView sourcePrintView = new WebView();
		final String styledSourceHtml = createHtmlForSourceCodePrint(sourceMarkdown);
		if (styledSourceHtml == null)
			return; // Abbrechen, wenn das Dokument leer war

		// 3. DRUCK-ORCHESTRIERUNG
		// Wir warten, bis die Engine den Code-Stream vollständig interpretiert hat.
		sourcePrintView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
			if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
				// Der eigentliche Druckbefehl muss zwingend im UI-Thread erfolgen
				javafx.application.Platform.runLater(() -> {
					executeHtmlPrint(sourcePrintView);
					Log.info("Source code print job sent to system.");
				});
			}
		});

		// Start des Silent-Loadings
		sourcePrintView.getEngine().loadContent(styledSourceHtml);
	}

	/**
	 * Executes a "Clean Room" print job, ensuring 100% visual fidelity. This method generates a pristine HTML stream in the background, applies the current document geometry
	 * (format, orientation), and sends it to the system's print dialog. It intentionally ignores the live editor's WebView to strip all workspace aids.
	 *
	 * @param activeViewIgnored The active editor WebView, which is deliberately ignored.
	 */

	private void executeHtmlPrint(WebView activeViewIgnored) {
		// 1. Dokument-DNA und Geometrie laden
		DocumentSettings currentSettings = new DocumentSettings(projectSettings);
		applyYamlOverrides(currentSettings, readYamlMetadata(editor.getText()));

		DocumentFormat format = currentSettings.getFormat();
		javafx.print.Paper paper = format.getJavaFxPaper();
		javafx.print.PageOrientation orientation = format.getJavaFxOrientation();

		// 2. HTML generieren
		String fullDocumentContent = editor.getText() + "\n\n<style>\n" + styleEditor.getText() + "\n</style>\n";
		String cleanHtml = generateCleanHtml(fullDocumentContent);

		// 3. Hintergrund-WebView vorbereiten
		WebView printView = new WebView();
		new Scene(printView); // Wichtig für CSS-Evaluation

		// --- DIE SOUVERÄNE FORMAT-SYNCHRONISATION ---
		printView.setZoom(1.0);

		if (format.isPaper) {
			// FALL A: PAPIER (A4, Letter etc.)
			// Wir zwingen die WebView exakt auf die physikalische Breite des Papiers in Points (1/72 inch).
			// Das verhindert, dass JavaFX die 100% Breite falsch interpretiert.
			double physicalWidth = paper.getWidth();
			double physicalHeight = paper.getHeight();

			printView.setMinWidth(physicalWidth);
			printView.setMaxWidth(physicalWidth);
			printView.setPrefWidth(physicalWidth);

			// Wir lassen die Höhe flexibel für den Slicer, aber die Breite ist Gesetz.
			printView.setPrefHeight(physicalHeight);
		} else {
			// FALL B: WEB
			// Wir nutzen eine Standard-Web-Breite für das Rendering vor dem Druck.
			printView.setPrefWidth(800);
			printView.setPrefHeight(1200);
		}
		// --------------------------------------------

		printView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
			if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {

				// *** SOUVERÄNER RASTERUNGS-OPTIMIERER ***
				// Wir zwingen die WebView, alle Text-Rendering-Artefakte zu entfernen,
				// da diese oft die Schatteneffekte im Druck erzeugen.
				printView.getEngine().executeScript("""
				        // Entfernt alle Text-Schlagschatten, die im WebKit-Druck aktiv sein könnten
				        document.body.style.textShadow = 'none';
				        document.body.style.webkitTextStroke = 'none';

				        // Erzwingt eine aggressive Glättung, die oft bei Druckern hilft
				        document.body.style.webkitFontSmoothing = 'antialiased';

				        // Erzwingt die finale Berechnung des Layouts
				        document.body.style.zoom = '100%';
				        document.body.style.transform = 'none';
				        """);

				// Zeit für den Slicer (1.5s ist sicher)
				javafx.animation.PauseTransition printDelay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(1500));

				printDelay.setOnFinished(e -> {
					Platform.runLater(() -> {
						try {
							PrinterJob job = PrinterJob.createPrinterJob();
							if (job != null) {

								// --- QUALITÄTS-BOOST 1: Druck-Einstellungen konfigurieren ---
								// Wir fordern maximale Qualität vom Druckertreiber an
								job.getJobSettings().setPrintQuality(javafx.print.PrintQuality.HIGH);

								boolean proceed = job.showPrintDialog(rootPane.getScene().getWindow());

								if (proceed) {
									javafx.print.Printer selectedPrinter = job.getPrinter();

									// --- QUALITÄTS-BOOST 2: Hardware-Ränder auf Null ---
									javafx.print.PageLayout precisionLayout = selectedPrinter.createPageLayout(paper, orientation, 0.0, 0.0, 0.0, 0.0);

									job.getJobSettings().setPageLayout(precisionLayout);

									// --- QUALITÄTS-BOOST 3: WebView Rendering-Hints ---
									// Wir deaktivieren das Caching der WebView für den Druckmoment.
									// Das zwingt WebKit dazu, die Vektoren der Schrift direkt in
									// den Druck-Buffer zu zeichnen, anstatt eine Pixel-Kopie zu nutzen.
									printView.setCache(false);

									// Ein spezieller Hint für die Rendering-Präzision
									printView.getEngine().setUserStyleSheetLocation(null);

									// 3. Ausführung
									printView.getEngine().print(job);
									job.endJob();

									Log.info("Sovereign Print: High-Resolution Precision Layout applied.");
								}
							}
						} catch (Exception ex) {
							showErrorAlert("Print Error", "The print job failed.", ex);
						}
					});
				});
				printDelay.play();
			}
		});

		String baseUrl = new File(System.getProperty("user.dir")).toURI().toString();
		printView.getEngine().loadContent(cleanHtml.replace("<head>", "<head><base href=\"" + baseUrl + "/\">"));
	}

	/**
	 * The HTML shell used for printing raw source code. Contains placeholders for CSS, header, footer, and the main content.
	 */
	private static final String HTML_SHELL_FOR_SOURCE_PRINT = """
	        <!DOCTYPE html>
	        <html>
	        <head>
	            <meta charset="UTF-8">
	            <style>%s</style> <!-- CSS Injection -->
	        </head>
	        <body>
	            %s  <!-- Header Injection -->
	            %s  <!-- Footer Injection -->
	            %s  <!-- Content Injection -->
	        </body>
	        </html>
	        """;

	/**
	 * Creates a complete, self-contained HTML document for printing the raw Markdown source code. This method is designed for technical reviews or archival, wrapping the source in
	 * a minimal, monochrome HTML shell optimized for readability and ink-efficiency.
	 *
	 * @param markdown The raw Markdown source text to be printed.
	 * @return A complete HTML string ready to be loaded into a WebView for printing.
	 */
	private String createHtmlForSourceCodePrint(String markdown) {
		// 1. VALIDATION: Prevent printing empty documents.
		if (markdown == null || markdown.trim().isEmpty()) {
			showAlert(Alert.AlertType.WARNING, "Print Blocked", "The document is empty.", "There is no source code to print.");
			return null;
		}

		// 2. DATA PREPARATION
		// Basic escaping for displaying code as text within HTML.
		String escapedContent = markdown.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");

		String timestamp = new java.text.SimpleDateFormat("dd.MM.yyyy ' / ' HH:mm").format(new java.util.Date());
		String filename = (currentFile != null) ? currentFile.getName() : "Untitled Document";

		// Retrieve the specialized CSS for code printing.
		String css = themeManager.getCssForSourceCodePrint();

		// 3. COMPONENT ASSEMBLY
		String headerHtml = String.format("<div class=\"header\"><span>FlowShift Editor - Source Print</span><span>%s</span></div>", timestamp);
		String footerHtml = String.format("<div class=\"footer\"><span>%s</span></div>", filename);
		String contentHtml = String.format("<div style=\"white-space: pre-wrap;\">%s</div>", escapedContent);

		// 4. FINAL INJECTION
		return String.format(HTML_SHELL_FOR_SOURCE_PRINT, css, headerHtml, footerHtml, contentHtml);
	}

	@FXML
	private void handleToggleWordWrap() {
		updateWordWrapButton();

		// SOUVERÄNER FOKUS-REFRESH
		// Wir geben dem Editor den Fokus zurück, der ihn gerade braucht.
		if (isDesignMode && styleEditor.isFocused()) {
			styleEditor.requestFocus();
		} else {
			editor.requestFocus();
		}
	}

	/**
	 * Orchestrates the automatic formatting of the active editor (Markdown or CSS). Utilizes a 2D-coordinate snapshot algorithm (Line/Column) to ensure caret stability and
	 * viewport consistency despite total text replacement.
	 */
	@FXML
	private void handleFormatAction() {
		final CodeArea activeArea = styleEditor.isFocused() ? styleEditor : editor;
		final String currentContent = activeArea.getText();

		if (currentContent == null || currentContent.isEmpty()) {
			return;
		}

		// --- PHASE 1: GEOMETRY SNAPSHOT ---
		// Capture logical position (Line/Column) instead of absolute index for better stability
		final int caretParagraph = activeArea.getCurrentParagraph();
		final int caretColumn = activeArea.getCaretColumn();

		// Calculate vertical offset to preserve the user's scroll position
		final int firstVisible = activeArea.firstVisibleParToAllParIndex();
		final int relativeOffset = caretParagraph - firstVisible;

		// --- PHASE 2: TRANSFORMATION ---
		MarkdownFormatter formatter = new MarkdownFormatter();
		final String formattedText = (activeArea == styleEditor) ? formatter.formatPureCss(currentContent) : formatter.format(currentContent);

		// --- PHASE 3: ATOMIC RECONSTRUCTION ---
		if (!currentContent.equals(formattedText)) {
			activeArea.replaceText(formattedText);

			Platform.runLater(() -> {
				// Restore position using 2D coordinates with safety clamping
				int targetPar = Math.min(caretParagraph, activeArea.getParagraphs().size() - 1);
				int targetCol = Math.min(caretColumn, activeArea.getParagraph(targetPar).length());

				// Precision leap to the original line/column
				activeArea.moveTo(targetPar, targetCol);

				// Synchronize viewport scroll state
				int targetTopPar = Math.max(0, targetPar - relativeOffset);
				activeArea.showParagraphAtTop(targetTopPar);

				activeArea.requestFollowCaret();
				activeArea.requestFocus();

				Log.fine(String.format("Sovereign Caret-Sync: Restored to Line %d, Col %d", targetPar + 1, targetCol + 1));
			});
		}
	}

	@FXML
	private void handleExitAction() {
		// Der Wächter `canDiscardCurrentDocument` enthält nun die gesamte Logik
		// (Dialog anzeigen, Speichern auslösen etc.).
		// Wenn er 'true' zurückgibt, bedeutet das, wir dürfen das Fenster schließen.
		if (canDiscardCurrentDocument()) {
			Stage stage = (Stage) rootPane.getScene().getWindow();
			stage.close();
			Log.info("Editor session ended gracefully.");
		}
	}

	// --- Helper Methods ---

	/**
	 * Synchronizes the UI state with the document's modification status. Disables the save button if the document is clean and manages the window title's dirty marker.
	 */
	private void updateDirtyState() {
		// A document is only "Clean" if BOTH the Markdown editor and the CSS Forge
		// are at their last marked (saved) positions.
		boolean isClean = editor.getUndoManager().isAtMarkedPosition() && styleEditor.getUndoManager().isAtMarkedPosition();

		// Update the internal flag
		saveRequested = !isClean;

		// --- SOVEREIGN SAVE GUARD ---
		// Disable the button if no changes exist to prevent redundant I/O operations.
		if (saveButton != null) {
			saveButton.setDisable(!saveRequested);
		}

		// --- WINDOW TITLE MANAGEMENT ---
		// Update title only if the dirty state actually changed
		Stage stage = (Stage) rootPane.getScene().getWindow();
		if (stage != null) {
			String currentTitle = stage.getTitle();
			String baseTitle = currentTitle.startsWith("* ") ? currentTitle.substring(2) : currentTitle;

			if (saveRequested && !currentTitle.startsWith("* ")) {
				stage.setTitle("* " + baseTitle);
			} else if (!saveRequested && currentTitle.startsWith("* ")) {
				stage.setTitle(baseTitle);
			}
		}
	}

	/**
	 * Applies formatting markdown around the current selection or inserts it at the caret. * @param prefix Markdown prefix (e.g., "**" or "*")
	 * 
	 * @param suffix Markdown suffix (e.g., "**" or "*")
	 */
	private void applyMarkdownFormatting(String prefix, String suffix) {
		IndexRange selection = editor.getSelection();
		String selectedText = editor.getSelectedText();
		String replacement = prefix + selectedText + suffix;
		editor.replaceSelection(replacement);

		if (selection.getLength() == 0) {
			editor.moveTo(selection.getStart() + prefix.length());
		}
		renderMarkdownPreview();
		editor.requestFocus();
	}

	private void insertText(String text) {
		editor.replaceSelection(text);
		renderMarkdownPreview();
		editor.requestFocus();
	}

	/**
	 * Adds a prefix to the beginning of the line(s) containing the selection. * @param prefix The prefix to add (e.g., "> ", "* ").
	 * 
	 * @param enforceIfHeading If true, removes existing heading (#) before applying prefix.
	 */
	private void applyLinePrefix(String prefix, boolean enforceIfHeading) {
		IndexRange selection = editor.getSelection();
		int startParIndex = editor.offsetToPosition(selection.getStart(), CodeArea.Bias.Forward).getMajor();
		int endParIndex = editor.offsetToPosition(selection.getEnd(), CodeArea.Bias.Backward).getMajor();
		int originalCaret = selection.getStart();

		// Calculate total length change for caret adjustment
		int totalLengthChange = 0;
		boolean allHavePrefix = true;

		// 1. Prüfe, ob alle Zeilen den Präfix bereits haben
		for (int i = startParIndex; i <= endParIndex; i++) {
			if (!editor.getParagraph(i).getText().trim().startsWith(prefix.trim())) {
				allHavePrefix = false;
				break;
			}
		}

		// 2. Wende/Entferne Präfix Zeile für Zeile
		for (int i = startParIndex; i <= endParIndex; i++) {
			String currentLine = editor.getParagraph(i).getText();
			int paragraphStart = editor.getAbsolutePosition(i, 0);
			int currentLengthChange = 0;

			if (allHavePrefix) {
				// Entferne Präfix (wenn es am Anfang der Zeile steht)
				if (currentLine.startsWith(prefix)) {
					editor.replaceText(paragraphStart, paragraphStart + prefix.length(), "");
					currentLengthChange = -prefix.length();
				}
			} else {
				// Füge Präfix hinzu
				if (enforceIfHeading && currentLine.trim().startsWith("#")) {
					// Erst vorhandene Headings entfernen
					Matcher m = Pattern.compile("^\\s*#+\\s*").matcher(currentLine);
					if (m.find()) {
						editor.replaceText(paragraphStart, paragraphStart + m.end(), "");
						currentLine = editor.getParagraph(i).getText(); // aktualisiere Zeile
					}
				}

				if (!currentLine.startsWith(prefix)) {
					editor.insertText(paragraphStart, prefix);
					currentLengthChange = prefix.length();
				}
			}
			totalLengthChange += currentLengthChange;
		}

		// 3. Cursor-Anpassung
		if (selection.getLength() == 0) {
			if (allHavePrefix) {
				// Präfix wurde entfernt, Cursor zurücksetzen
				editor.moveTo(Math.max(0, originalCaret + totalLengthChange));
			} else {
				// Präfix wurde hinzugefügt, Cursor nach dem Präfix setzen
				editor.moveTo(originalCaret + prefix.length());
			}
		} else {
			// Auswahl beibehalten
			editor.selectRange(selection.getStart(), selection.getEnd() + totalLengthChange);
		}

		renderMarkdownPreview();
		editor.requestFocus();
	}

	/**
	 * Einheitliche Methode für Headings (setzen UND entfernen)
	 */
	private void applyHeadingToCurrentOrSelectedLines(int level, boolean remove) {
		IndexRange selection = editor.getSelection();

		// Bestimme betroffene Zeilen
		int startPar, endPar;

		if (selection.getLength() == 0) {
			// KEINE Selektion → nur aktuelle Zeile
			int caretPos = Math.min(editor.getCaretPosition(), editor.getLength());
			try {
				int paragraph = editor.offsetToPosition(caretPos, CodeArea.Bias.Forward).getMajor();
				startPar = endPar = paragraph;
			} catch (IndexOutOfBoundsException e) {
				return;
			}
		} else {
			// MIT Selektion → alle markierten Zeilen
			try {
				startPar = editor.offsetToPosition(selection.getStart(), CodeArea.Bias.Forward).getMajor();
				endPar = editor.offsetToPosition(selection.getEnd(), CodeArea.Bias.Backward).getMajor();
			} catch (IndexOutOfBoundsException e) {
				return;
			}
		}

		// Sicherheitschecks
		startPar = Math.max(0, Math.min(startPar, editor.getParagraphs().size() - 1));
		endPar = Math.max(0, Math.min(endPar, editor.getParagraphs().size() - 1));

		for (int i = startPar; i <= endPar; i++) {
			if (i < 0 || i >= editor.getParagraphs().size())
				continue;

			String currentLine = editor.getParagraph(i).getText();
			if (currentLine == null)
				continue;

			int paragraphStart = editor.getAbsolutePosition(i, 0);

			if (remove) {
				// HEADING ENTFERNEN ("Reset to Text")
				String newLine = currentLine.replaceAll("^\\s*#+\\s*", "");
				if (!newLine.equals(currentLine)) {
					try {
						editor.replaceText(paragraphStart, paragraphStart + currentLine.length(), newLine);
					} catch (IndexOutOfBoundsException e) {
						// Ignorieren
					}
				}
			} else {
				// HEADING SETZEN/ÄNDERN (H1, H2, H3)
				String prefix = "#".repeat(level) + " ";

				// 1. Vorher existierenden Header entfernen (wenn vorhanden)
				String cleanedLine = currentLine;
				if (currentLine.trim().startsWith("#")) {
					cleanedLine = currentLine.replaceAll("^\\s*#+\\s*", "");
					try {
						editor.replaceText(paragraphStart, paragraphStart + currentLine.length(), cleanedLine);
					} catch (IndexOutOfBoundsException e) {
						// Ignorieren
					}
				}

				// 2. Neuen Header hinzufügen (wenn nicht schon vorhanden)
				if (!cleanedLine.startsWith(prefix)) {
					try {
						editor.insertText(paragraphStart, prefix);
					} catch (IndexOutOfBoundsException e) {
						// Ignorieren
					}
				}
			}
		}

		renderMarkdownPreview();
		editor.requestFocus();
	}

	// Überladene Methode ohne Enforce-Parameter
	private void applyLinePrefix(String prefix) {
		applyLinePrefix(prefix, false);
	}

	// Diese Methode sollte NUR EINMAL existieren!
	private StyleSpans<Collection<String>> createEmptySpans() {
		StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
		builder.add(Collections.emptyList(), 0);
		return builder.create();
	}

	/**
	 * Escapes all non-ASCII characters (inkl. Emojis) in HTML. Example: 😀 -> &#x1F600;
	 */
	private String escapeUnicodeForHtml(String input) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			int codePoint = input.codePointAt(i);

			if (Character.isSupplementaryCodePoint(codePoint)) {
				i++; // Skip low surrogate
			}

			// Entferne ZWJ und andere Problem-Zeichen für WebView
			if (codePoint == 0x200D)
				continue; // Zero Width Joiner
			if (codePoint == 0xFE0F)
				continue; // Variation Selector 16

			if (codePoint < 128) {
				sb.append((char) codePoint);
			} else {
				sb.append("&#x").append(Integer.toHexString(codePoint)).append(";");
			}
		}
		return sb.toString();
	}

	private String preprocessTemplate(String markdown) {
		// 1. NUR anwenden wenn HTML-Template erkannt
		if (!isHtmlTemplate(markdown)) {
			return markdown;
		}

		// 2. Spezifische Fixes für Template-Probleme
		String fixed = markdown;

		// Fix A: Leerzeilen + Einrückung vor Tags
		fixed = fixed.replaceAll("(?m)^[ \\t]*\\n[ \\t]*(</?[a-zA-Z])", "$1");

		// Fix B: Einrückung am Zeilenanfang für HTML-Tags
		fixed = fixed.replaceAll("(?m)^([ \\t]+)(<[a-zA-Z])", "$2");

		// Fix C: Mehrere Leerzeilen reduzieren auf 1
		fixed = fixed.replaceAll("\\n{3,}", "\n\n");

		return fixed;
	}

	private boolean isHtmlTemplate(String markdown) {
		// Einfache Heuristik für HTML-Templates
		int htmlTagCount = 0;
		String[] tags = { "<div", "<span", "<table", "<style", "<h1", "<h2", "<h3" };

		for (String tag : tags) {
			int count = 0;
			int index = 0;
			while ((index = markdown.indexOf(tag, index)) != -1) {
				count++;
				index += tag.length();
			}
			htmlTagCount += count;
		}

		return htmlTagCount >= 5; // Wenn 5+ HTML-Tags, ist es wahrscheinlich ein Template
	}

	/**
	 * Orchestriert die Live-Vorschau (v8.7 Shadow-CSS Edition). Führt Inhalt (Markdown) und Form (CSS) erst im Moment des Renderings zusammen.
	 */
	private void renderMarkdownPreview() {
		renderMarkdownPreview(null);
	}

	private void renderMarkdownPreview(DocumentSettings previewSettings) {
		if (webView == null)
			return;
		ensureHeaderManager();

		// 1. DATA ACQUISITION
		// We use the raw text to preserve exact offsets for the navigator mapping.
		final String rawMarkdown = editor.getText();
		final String shadowCss = styleEditor.getText();
		Map<String, String> docMetadata = readYamlMetadata(rawMarkdown);

		DocumentSettings ds = (previewSettings != null) ? previewSettings : new DocumentSettings();
		if (previewSettings == null)
			applyYamlOverrides(ds, docMetadata);

		// 2. CONTEXT & CACHE SYNC
		// Prevents flickering by checking if a full reload is required due to layout changes.
		if (ds.isPaginated() != lastPaginationState) {
			this.forceWebViewReload = true;
			this.lastPaginationState = ds.isPaginated();
			this.cachedMappedHtml = null;
		}

		// 3. GENERATION & MAPPING
		// Generates HTML, maps source offsets, and substitutes placeholders in one atomic step.
		String finalBodyContent = getOrRenderMappedHtml(rawMarkdown, ds, docMetadata, previewSettings == null, forceWebViewReload);

		// --- STRUCTURAL HEALING FOR SLICER ---
		// We must wrap top-level orphan text nodes into <p> tags.
		// Rationale: The JS Pagination Slicer relies on block-level elements (children) to move content between pages.
		// If we feed it naked text nodes, it may fail to move them correctly or duplicate them during re-slicing.
		finalBodyContent = wrapTopLevelOrphanText(finalBodyContent);
		// -------------------------------------

		// 4. FINAL PREPARATION
		String finalBody = escapeUnicodeForHtml(finalBodyContent);
		String headerHtml = generateHeaderSection(ds, docMetadata);

		// 5. UPDATE VIEW
		updateWebView(finalBody, headerHtml, shadowCss, ds, docMetadata);

		if (formatStatusLabel != null) {
			formatStatusLabel.setText("Format: " + ds.getFormat().toString());
		}
	}

	/**
	 * Decision engine for WebView updates. Chooses between a flicker-free JavaScript swap or a robust full page reload.
	 */
	private void updateWebView(String finalBody, String headerHtml, String shadowCss, DocumentSettings ds, Map<String, String> meta) {
		boolean isLoaded = webView.getEngine().getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED;

		if (isLoaded && !forceWebViewReload) {
			try {
				executeSovereignSwap(finalBody, headerHtml, shadowCss, ds);
				Log.fine("Sovereign Update: Fast-Swap executed.");
			} catch (Exception ex) {
				Log.warn("Sovereign Swap failed, falling back to full reload.");
				fullReload(shadowCss, finalBody, ds, meta);
			}
		} else {
			fullReload(shadowCss, finalBody, ds, meta);
		}
	}

	/**
	 * Executes a flicker-free content update via JavaScript innerHTML injection. Includes a "Height-Lock" and "Scroll-Anchor" mechanism to prevent viewport jumping.
	 */
	private void executeSovereignSwap(String finalBody, String headerHtml, String shadowCss, DocumentSettings ds) {
		// 1. Build the logical workspace structure
		String workspacePayload = generateTopDocumentInfoHint(ds.getFormat()) + generateContentPayload(ds, headerHtml, finalBody) + generateBottomUsageHint();

		// 2. Prepare data for JavaScript template strings (escaping)
		String jsPayload = workspacePayload.replace("`", "\\`").replace("${", "\\${");
		String jsCss = shadowCss.replace("`", "\\`").replace("${", "\\${");
		String jsLayout = themeManager.getPageLayoutCss(ds).replace("`", "\\`").replace("${", "\\${");

		// 3. The Sovereign Script (with Height-Lock and Scroll-Anchoring)
		String swapScript = """
		        (function() {
		            var y = window.scrollY;
		            var body = document.body;
		            var container = document.getElementById('sovereign-payload');
		            if (!container) return;

		            // --- SOUVERÄNER SCROLL-LOCK ---
		            // Lock current height to prevent scrollbar flickering
		            var currentHeight = body.scrollHeight;
		            body.style.minHeight = currentHeight + 'px';

		            // Atomic DOM update
		            container.innerHTML = `%s`;
		            document.getElementById('layout-style').innerHTML = `%s`;
		            document.getElementById('user-shadow-css').innerHTML = `%s`;

		            // Re-initialize dynamic components
		            if (window.initializeCopyButtons) window.initializeCopyButtons();
		            if (%b && window.reSlice) window.reSlice();

		            // Restore scroll position and release height-lock
		            window.scrollTo(0, y);
		            body.style.minHeight = '';
		        })();
		        """.formatted(jsPayload, jsLayout, jsCss, ds.isPaginated());

		webView.getEngine().executeScript(swapScript);
	}

	private void fullReload(String shadowCss, String finalBody, DocumentSettings settings, Map<String, String> docMetadata) {
		// 1. Zuerst den aktuellen Stand in Java-Variablen sichern
		saveCurrentWebViewScroll();

		// 2. Dann die Seite komplett neu aufbauen
		String fullHTML = buildCompleteHtml(shadowCss, finalBody, themeManager.getInteractionScriptForExport(), settings, docMetadata, false);
		webView.getEngine().loadContent(fullHTML);

		// 3. Paginierung für den neuen Kontext vorbereiten
		triggerPaginationIfEnabled(settings);

		this.forceWebViewReload = false;
		Log.info("Sovereign Reload: Context and Scroll-Anchor rebuilt.");
	}

	private void saveCurrentWebViewScroll() {
		if (webView.getEngine().getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED) {
			try {
				Object scrollYObj = webView.getEngine().executeScript("window.scrollY");
				if (scrollYObj instanceof Number) {
					this.lastWebViewScrollY = ((Number) scrollYObj).doubleValue();
					this.restoreScrollRequested = true; // Signal an den Listener in setupWebView
					Log.fine("Sovereign Anchor: Position %f saved for reload.", lastWebViewScrollY);
				}
			} catch (Exception e) {
				this.lastWebViewScrollY = 0;
				this.restoreScrollRequested = false;
			}
		}
	}

	private void triggerPaginationIfEnabled(DocumentSettings ds) {
		if (ds.isPaginated()) {
			// Wir erstellen einen neuen Listener
			ChangeListener<javafx.concurrent.Worker.State> helper = new ChangeListener<>() {
				@Override
				public void changed(ObservableValue<? extends State> obs, State old, State newState) {
					if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
						// Paginierung ausführen
						String script = themeManager.getSlicerScript(ds.getFormat().heightInMm(), ds.getMarginTop(), ds.getMarginBottom());
						webView.getEngine().executeScript(script);

						// SEHR WICHTIG: Sich selbst wieder entfernen!
						webView.getEngine().getLoadWorker().stateProperty().removeListener(this);
					}
				}
			};
			webView.getEngine().getLoadWorker().stateProperty().addListener(helper);
		}
	}

	/**
	 * Reads metadata from the YAML frontmatter block. HEALING: Now explicitly ignores lines starting with '#' or '<!--' to prevent comments from being parsed as metadata keys.
	 */
	private Map<String, String> readYamlMetadata(String markdownText) {
		Map<String, String> metadata = new HashMap<>();
		Matcher blockMatcher = YAML_BLOCK_EXTRACT_PATTERN.matcher(markdownText);

		if (blockMatcher.find()) {
			String yamlContent = blockMatcher.group(1);
			for (String line : yamlContent.split("\\n")) {
				String trimmed = line.trim();

				// --- HEILUNG: KOMMENTAR-WÄCHTER ---
				// Überspringe Zeilen, die Kommentare sind, bevor der Regex-Matcher läuft.
				if (trimmed.startsWith("#") || trimmed.startsWith("<!--") || trimmed.isEmpty()) {
					continue;
				}

				Matcher fieldMatcher = YAML_FIELD_EXTRACT_PATTERN.matcher(line);
				if (fieldMatcher.find()) {
					String key = fieldMatcher.group(1).trim();
					String value = fieldMatcher.group(2).trim();

					// Quotes entfernen (wie gehabt)
					if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
						value = value.substring(1, value.length() - 1);
					}
					metadata.put(key, value);
				}
			}
		}
		return metadata;
	}

	/**
	 * Intelligently commits metadata to the document. Writes a YAML block if valid data exists, or removes an existing block if no data is provided.
	 * 
	 * @param meta The map of metadata to persist.
	 */
	private void commitMetadataToDocument(Map<String, String> meta) {
		StringBuilder contentBuilder = new StringBuilder();

		// HEILUNG: Wir iterieren über die definierte Reihenfolge im Enum
		for (Placeholder p : Placeholder.values()) {
			String key = p.getKey();
			if (meta.containsKey(key)) {
				String value = meta.get(key);
				if (isValidYamlValue(value)) {
					contentBuilder.append(key).append(": ").append(formatYamlValue(value)).append("\n");
				}
				// Entferne den Key aus der Map, um Duplikate zu vermeiden
				meta.remove(key);
			}
		}

		// Füge eventuelle benutzerdefinierte Keys (die nicht im Enum sind) am Ende hinzu
		meta.forEach((key, value) -> {
			if (isValidYamlValue(value)) {
				contentBuilder.append(key).append(": ").append(formatYamlValue(value)).append("\n");
			}
		});

		if (contentBuilder.length() == 0) {
			removeYamlFromDocument();
			return;
		}

		// Der Block endet bereits mit einem \n durch den letzten append
		String yamlBlock = "---\n" + contentBuilder.toString() + "---\n";

		Matcher m = YAML_REPLACE_PATTERN.matcher(editor.getText());
		if (m.find()) {
			// Ersetzt den alten Block UND die gierig erfassten Leerzeichen danach
			editor.replaceText(m.start(), m.end(), yamlBlock);
		} else {
			// Fügt den neuen Block am Anfang ein, ohne ein ZUSÄTZLICHES \n zu erzwingen
			editor.insertText(0, yamlBlock);
		}
	}

	/**
	 * Hilfsmethode: Prüft, ob ein Wert ins YAML geschrieben werden soll. "INHERIT" ist ein reiner UI-Zustand und wird hier gefiltert.
	 */
	private boolean isValidYamlValue(String val) {
		return val != null && !val.trim().isEmpty() && !"INHERIT".equals(val);
	}

	/**
	 * Helper: Formats the YAML value with quotes if necessary.
	 */
	private String formatYamlValue(String val) {
		if (val.contains(" ") || val.contains(":")) {
			return "\"" + val.replace("\"", "\\\"") + "\"";
		}
		return val;
	}

	/**
	 * Helper: Completely removes the YAML block from the current document.
	 */
	private void removeYamlFromDocument() {
		Matcher m = YAML_REPLACE_PATTERN.matcher(editor.getText());
		if (m.find()) {
			editor.replaceText(m.start(), m.end(), "");
		}
	}

	private String getOrRenderMappedHtml(String rawMarkdown, DocumentSettings ds, Map<String, String> meta, boolean isEditMode, boolean forceReload) {
		if (cachedMappedHtml != null && isEditMode && !forceReload) {
			return cachedMappedHtml;
		}

		// 1. SCHUTZSCHILD (Länge bleibt identisch!)
		// Wir tauschen {{ gegen [[, damit Flexmark sie ignoriert.
		String skeletonMarkdown = rawMarkdown.replace("{{", "[[").replace("}}", "]]");

		// 2. RENDERING (Skelett erzeugen)
		String skeletonHtml = htmlRenderer.render(markdownParser.parse(skeletonMarkdown));

		// 3. RÜCKTAUSCH IM HTML
		// Jetzt stehen im HTML wieder die echten {{stamps}}.
		String htmlWithBraces = skeletonHtml.replace("[[", "{{").replace("]]", "}}");

		// 4. KARTOGRAFIE (Höchste Präzision)
		// Wir mappen das HTML (mit {{stamps}}) gegen das Original im Editor.
		// Da die Längen 1:1 gleich sind, landet der Navigator perfekt.
		String mappedHtml = sourceMapper.mapAndInject(htmlWithBraces, rawMarkdown);

		// 5. SUBSTITUTION (Werte einsetzen)
		// Erst jetzt, wo die fsids fest an den Tags kleben, tauschen wir die Texte aus.
		Map<String, String> placeholders = collectPlaceholders(ds, meta);
		String finalHtml = applyPlaceholders(mappedHtml, placeholders);

		if (isEditMode) {
			cachedMappedHtml = finalHtml;
		}
		return finalHtml;
	}

	private String generateHeaderSection(DocumentSettings settings, Map<String, String> docMetadata) {
		if ("NONE".equalsIgnoreCase(settings.getActiveHeaderStyle())) {
			return "";
		}
		String rawTemplate = settings.getHeaderHtml();
		if (rawTemplate == null || rawTemplate.trim().isEmpty()) {
			return "";
		}
		Map<String, String> placeholders = collectPlaceholders(settings, docMetadata);
		return "<header class='document-header'>" + applyPlaceholders(rawTemplate, placeholders).trim() + "</header>";
	}

	private String generateContentPayload(DocumentSettings settings, String headerSection, String bodyContent) {
		String cleanBody = bodyContent.trim();
		if (settings.isPaginated()) {
			return "<main class='document-content'>" + headerSection + cleanBody + "</main>";
		} else {
			return "<div class='document-page'>" + headerSection + "<main class='document-content'>" + cleanBody + "</main></div>";
		}
	}

	private String buildCompleteHtml(String css, String body, String scripts, DocumentSettings ds, Map<String, String> meta, boolean isExport) {
		// 1. Prepare Header and Content
		String header = generateHeaderSection(ds, meta);
		String payload = generateContentPayload(ds, header, body);

		// 2. Inject Workspace UI (Hints/Tips) only for live view
		if (!isExport) {
			payload = generateTopDocumentInfoHint(ds.getFormat()) + payload + generateBottomUsageHint();
		}

		// 3. Assemble full shell using safe replacement
		return """
		        <!DOCTYPE html>
		        <html>
		        <head>
		            <meta charset="UTF-8">
		            <meta name="viewport" content="width=device-width, initial-scale=1.0">
		            <title>__TITLE__</title>
		            <style id="layout-style">__LAYOUT__</style>
		            __BASE_CSS__
		            <style id="user-shadow-css">__USER_CSS__</style>
		        </head>
		        <body class="__CLASS__">
		            <div id="sovereign-payload">__PAYLOAD__</div>
		            <div id="script-block" class="no-print">__SCRIPTS__</div>
		        </body>
		        </html>""".replace("__TITLE__", ds.getCompanyName()).replace("__LAYOUT__", themeManager.getPageLayoutCss(ds))
		        .replace("__BASE_CSS__", themeManager.getBaseAestheticStyleBlock()).replace("__USER_CSS__", (css != null ? css : ""))
		        .replace("__CLASS__", (isDarkMode ? "dark-mode" : "light-mode") + (isExport ? "" : " workspace-mode")).replace("__PAYLOAD__", payload)
		        .replace("__SCRIPTS__", (scripts != null ? scripts : ""));
	}

	private String generateTopDocumentInfoHint(DocumentFormat format) {
		// HEILUNG: .strip() entfernt alle unsichtbaren Zeichen aus dem Triple-Quote
		return String.format("""
		        <div id="workspace-hint" class="workspace-info no-print">
		        <strong>Editor Mode:</strong> Rendering %s (%s). Use 'Print' for PDF generation.
		        </div>""", format.toString(), format.width).strip();
	}

	private String generateBottomUsageHint() {
		return """
		        <div id="usage-tips" class="workspace-tips no-print">
		        <h4> Document Intelligence</h4>
		        <ul>
		            <li><strong>Dynamic Content:</strong> Use <code>{{documentTitle}}</code> or <code>{{date}}</code>.</li>
		            <li><strong>ID Generation:</strong> <code>{{randomId}}</code> creates a unique number.</li>
		            <li><strong>Auto-Fix:</strong> Press <code>Ctrl+Shift+F</code> to heal structure.</li>
		            <li><strong>Branding:</strong> Select a template in the Settings Nexus.</li>
		        </ul>
		        </div>""".strip();
	}

	/**
	 * Extrahiert CSS aus <style> tags und entfernt sie aus dem Markdown
	 */
	private String[] extractAndRemoveCss(String markdown) {
		StringBuilder cssBuilder = new StringBuilder();
		StringBuilder contentBuilder = new StringBuilder(markdown);

		// Finde alle <style> tags
		Pattern pattern = Pattern.compile("<style[^>]*>([\\s\\S]*?)</style>", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(markdown);

		int offset = 0;
		while (matcher.find()) {
			// CSS extrahieren
			cssBuilder.append(matcher.group(1)).append("\n");

			// <style> tag aus dem Inhalt entfernen
			int start = matcher.start() - offset;
			int end = matcher.end() - offset;
			contentBuilder.delete(start, end);
			offset += (end - start);
		}

		return new String[] { contentBuilder.toString(), // Markdown ohne <style> tags
		        cssBuilder.toString() // Extrahiertes CSS
		};
	}

	public void toggleDarkMode() {
		isDarkMode = !isDarkMode; // Zustand umschalten
//		updatePreview(); // Vorschau neu laden
	}

	/**
	 * Schaltet den Dark Mode der JavaFX-Oberfläche (GUI) um. Dies ist eine reine Editor-Einstellung und verändert NICHT das Dokument-Design.
	 */
	@FXML
	private void handleToggleDarkMode(ActionEvent event) {
		isDarkMode = !isDarkMode;

		// 1. GUI-STYLING (Der Rahmen des Editors)
		if (isDarkMode) {
			rootPane.getStyleClass().add("dark-mode");
		} else {
			rootPane.getStyleClass().remove("dark-mode");
		}

		// 2. VISUELLE KONSISTENZ (Workspace-Hintergrund in der WebView)
		// Wir rendern neu, damit buildCompleteHtml die 'dark-ui' Klasse
		// an den Body der WebView hängen kann (für den Raum um das Papier).
		renderMarkdownPreview();

		// 3. UI-FEEDBACK
		updateDarkModeButtonIcon();

		Log.fine("Editor UI Mode changed. Document DNA remains untouched.");
	}

	@FXML
	private void handleNewDocumentAction(ActionEvent event) {
		if (canDiscardCurrentDocument()) {

			// 1. Reset (Leert den Editor und setzt saveRequested auf FALSE)
			resetDocument();

			// 2. STATE-UPDATE: Dokument ist jetzt "Dirty" und muss gesichert werden.
			this.saveRequested = true;
			updateStatusLabels();

			// 3. Der Nutzer beginnt zu tippen. Die erste Save-Aktion wird SaveAs erzwingen.
			editor.requestFocus();
		}
	}

	private void resetDocument() {
		editor.clear();
		editor.replaceText("");

		// SOUVERÄNER CLEANUP
		currentSearchMatches.clear();
		searchResults.clear();
		// Korrigiert: Nutze die Methode im Popup, um das Textfeld zu leeren.
		if (searchPopup != null) {
			searchPopup.clearSearchField();
		}

		setCurrentFile(null);
		saveRequested = false;

		editor.getUndoManager().forgetHistory();
		editor.getUndoManager().mark();

		this.forceWebViewReload = true;

		renderMarkdownPreview();
		updateStatusLabels();
		updateSearchAvailability(false);
		editor.requestFocus();
	}

	// Edit Menu Actions
	@FXML
	private void handleCutAction(ActionEvent event) {
		editor.cut();
	}

	@FXML
	private void handleCopyAction(ActionEvent event) {
		editor.copy();
	}

	@FXML
	private void handlePasteAction(ActionEvent event) {
		editor.paste();
	}

	@FXML
	private void handleSelectAllAction(ActionEvent event) {
		editor.selectAll();
	}

	// =============================================================================================
	// TEMPLATE & BUNDLE NEXUS (Zentrale Steuerung)
	// =============================================================================================

	/**
	 * Öffnet das Template-Library-Fenster. Nutzt einen Cache, um das Fenster nach dem ersten Aufbau sofort wieder anzuzeigen, anstatt es jedes Mal neu zu erstellen.
	 */
	@FXML
	private void handleTemplateLibraryAction() {
		// 1. SCHON GEBAUT?
		if (libraryWindow != null) {
			// A. INHALT AKTUALISIEREN
			// Wir sagen dem Fenster, es soll seine Listen neu laden,
			// falls sich in der Zwischenzeit etwas geändert hat (z.B. durch Import).
			libraryWindow.refreshContent();

			// B. SICHTBAR MACHEN & FOKUSSIEREN
			libraryWindow.show();
			libraryWindow.toFront();
			return;
		}

		// 2. ERSTER AUFRUF: Fenster bauen und im Feld speichern (cachen)
		libraryWindow = new TemplateLibraryWindow(getPrimaryStage(), templateManager, projectSettings, this::generateCleanHtml, this);

		// Wichtig: Wir müssen sicherstellen, dass die Referenz gelöscht wird,
		// wenn der User das Fenster manuell schließt (z.B. mit dem X-Button).
		libraryWindow.setOnHidden(e -> libraryWindow = null);

		libraryWindow.show();
	}

	@Override
	public void onVaultDocumentDelete(String fileName) {
		if (fileName == null)
			return;

		// Wir umgehen hier die Bestätigungsabfrage, da diese bereits im LibraryWindow war
		try {
			templateManager.deleteDocumentFromVault(fileName);
			Log.info("Document '%s' deleted from Vault.", fileName);
			// Nach dem Löschen muss das Fenster seine Liste selbst aktualisieren
			if (libraryWindow != null) {
				libraryWindow.refreshContent();
			}
		} catch (IOException e) {
			showErrorAlert("Vault Error", "Could not delete file from Vault.", e, libraryWindow);
		}
	}

	@Override
	public boolean onTemplateSave(String name) {
		if (name == null || name.trim().isEmpty())
			return false;

		// 1. SOUVERÄNE ZUSAMMENFÜHRUNG (v8.7)
		String markdown = editor.getText().trim();
		String css = styleEditor.getText().trim();

		if (markdown.isEmpty() && css.isEmpty()) {
			showAlert(Alert.AlertType.WARNING, "Save Blocked", "Cannot save an empty blueprint.", null);
			return false;
		}

		// Wir bauen das vollständige Dokument für den Template-Speicher
		String fullContent = markdown + "\n\n<style>\n" + css + "\n</style>\n";

		try {
			// A. Text als .md sichern
			templateManager.saveCustomTemplate(name, fullContent);

			// B. Asynchronen Snapshot-Prozess starten
			// WICHTIG: buildCompleteHtmlForPreview muss nun beide Teile kennen!
			String html = generateCleanHtml(fullContent);
			Path previewPath = templateManager.getRootPath().resolve("previews").resolve(name + ".png");
			SnapshotService.create(html, previewPath, null);

			Log.info("Template Blueprint '%s' saved with Shadow-CSS.", name);
			templateManager.loadTemplates();
			return true;
		} catch (IOException e) {
			showErrorAlert("Library Error", "Could not save template blueprint.", e);
			return false;
		}
	}

	@Override
	public boolean onTemplateDelete(String name) {
		if (name == null)
			return false;

		// NEU: Die entscheidende Sicherheitsabfrage
		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Permanently remove the blueprint '" + name + "' and its preview image?", ButtonType.OK, ButtonType.CANCEL);
		confirm.setTitle("Delete Blueprint");
		confirm.setHeaderText(null);
		confirm.getDialogPane().getStyleClass().add("nexus-dialog");

		if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
			try {
				// FIX: 'manager' zu 'templateManager' korrigiert
				templateManager.deleteTemplate(name);
				Log.info("Template deleted: %s", name);
				return true; // Erfolg: Der Dialog kann seine Liste aktualisieren.
			} catch (IOException e) {
				showErrorAlert("Delete Error", "Could not delete template files.", e);
				return false;
			}
		}
		return false; // User hat abgebrochen
	}

	@Override
	public void onBundleImport() {
		// 1. Der Service wird gerufen. Er kümmert sich um Caching, Titel, Filter und Anzeige.
		// Wir übergeben das 'libraryWindow' als Besitzer, damit der Dialog davor erscheint.
		File selectedFile = utils.ui.WindowUtils.openFileChooser(libraryWindow, UIText.FILE_CHOOSER_IMPORT_BUNDLE_TITLE.getLabel(), lastUsedDirectory,
		        List.of(new FileChooser.ExtensionFilter("FlowShift Bundle (*.fsb)", "*.fsb")), // KORREKTUR: Filter in List.of()
		        false, true);

		if (selectedFile != null) { // NUR ausführen, wenn der Nutzer eine Datei ausgewählt hat.
			try {
				BundleService service = new BundleService();
				Path appRoot = Paths.get(System.getProperty("user.dir"));
				Path tRoot = appRoot.resolve("templates").resolve("premium");
				Path hRoot = appRoot.resolve("templates").resolve("header_templates");
				Path pRoot = appRoot.resolve("templates").resolve("previews");

				service.importBundle(selectedFile.toPath(), tRoot, hRoot, pRoot);

				templateManager.loadTemplates();
				ensureHeaderManager();

				if (libraryWindow != null) {
					libraryWindow.refreshContent();
				}

			} catch (Exception e) {
				showErrorAlert("Import Failed", "The bundle is corrupt or incomplete.", e);
			}
		}
	}

	@Override
	public void onBundleExport() {
		// 1. QUALITÄTS-CHECK: Nur fehlerfreie Dokumente werden zu Bundles
		if (markdownHighlighter != null && !markdownHighlighter.getLastErrors().isEmpty()) {
			showAlert(Alert.AlertType.WARNING, "Export Blocked", "Structural Integrity Check Failed",
			        "Please fix all Linter errors before creating a professional FlowShift Bundle.", libraryWindow);
			return;
		}

		// Hier könnte man noch einen Check für CSS-Fehler hinzufügen

		// 2. VERSCHMELZUNG FÜR DEN SNAPSHOT
		String markdown = editor.getText().trim();
		String css = styleEditor.getText().trim();
		String fullDocument = markdown + "\n\n<style>\n" + css + "\n</style>\n";

		String htmlForSnapshot = generateCleanHtml(fullDocument);

		// 3. ASYNCHRONER SNAPSHOT-PROZESS
		SnapshotService.create(htmlForSnapshot, null, previewBytes -> {
			if (previewBytes == null) {
				Platform.runLater(() -> showErrorAlert("Export Error", "Snapshot failed.", null, libraryWindow));
				return;
			}

			// 4. SPEICHER-DIALOG
			Platform.runLater(() -> {
				String initialName = projectSettings.getDocumentTitle().replaceAll("[^a-zA-Z0-9.-]", "_") + ".fsb";

				File targetFile = utils.ui.WindowUtils.saveFileChooser(libraryWindow, // Owner Window
				        UIText.FILE_CHOOSER_EXPORT_BUNDLE_TITLE.getLabel(), // Titel
				        lastUsedDirectory, // Initialverzeichnis
				        initialName, // NEU: initialName hinzufügen
				        List.of(new FileChooser.ExtensionFilter("FlowShift Bundle (*.fsb)", "*.fsb")), // KORREKTUR: Filter in List.of()
				        false, // Keine "All Files"
				        true // "Alle kompatiblen"
				);

				if (targetFile != null) { // NUR ausführen, wenn der Nutzer eine Datei ausgewählt hat.
					try {
						BundleManifest manifest = new BundleManifest();
						manifest.id = java.util.UUID.randomUUID().toString();
						manifest.name = projectSettings.getDocumentTitle();
						manifest.author = projectSettings.getAuthorName();
						manifest.version = projectSettings.getVersion();
						manifest.headerStyle = projectSettings.getActiveHeaderStyle();

						new BundleService().createBundle(targetFile.toPath(), manifest, fullDocument, projectSettings.getHeaderHtml(), previewBytes);

						showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Bundle Ready", "Bundle '" + targetFile.getName() + "' created.", libraryWindow);

					} catch (Exception e) {
						showErrorAlert("Export Error", "Packaging failed.", e, libraryWindow);
					}
				}
			});
		});
	}

	/**
	 * Liefert den Pfad zum lokalen Export-Tresor (user.dir/exports/). Erstellt den Ordner automatisch, falls er nicht existiert.
	 */
	private Path getExportsVaultPath() {
		Path path = Paths.get(System.getProperty("user.dir"), "exports");
		try {
			Files.createDirectories(path);
		} catch (IOException e) {
			Log.error(e, "Failed to create exports directory.");
		}
		return path;
	}

	// =============================================================================================
	// DIALOG MANAGEMENT (Sovereign & Hierarchy-Aware)
	// =============================================================================================

	private boolean canDiscardCurrentDocument() {
		if (!saveRequested) {
			return true; // Document is clean, proceed.
		}

		// Use the specialized, context-aware dialog from WindowUtils.
		// We use CLOSING_ARCHIVE as the context for a generic "close document" prompt.
		// The second argument is for formatting the archive name, which we don't need here.
		javafx.util.Pair<ButtonType, Boolean> result = utils.ui.WindowUtils.showSaveConfirmationDialog(getPrimaryStage(), utils.ui.WindowUtils.CloseActionContext.CLOSING_ARCHIVE,
		        getFileName());

		ButtonType userChoice = result.getKey();

		if (userChoice.getButtonData() == ButtonBar.ButtonData.YES) {
			// User chose "Save". Attempt to save and only proceed if successful.
			return handleSaveAction();
		} else if (userChoice.getButtonData() == ButtonBar.ButtonData.NO) {
			// User chose "Don't Save". Proceed with the action.
			return true;
		} else {
			// User chose "Cancel" or closed the dialog.
			return false;
		}
	}

	private void showExportSuccessDialog(File exportedFile) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Export Complete");
		alert.setHeaderText("Your document has been successfully exported.");
		alert.setContentText("Destination: " + exportedFile.getName());
		alert.getDialogPane().getStyleClass().add("nexus-dialog");
		alert.initOwner(getPrimaryStage());

		ButtonType btnOpenFile = new ButtonType("Open File", ButtonBar.ButtonData.YES);
		ButtonType btnOpenFolder = new ButtonType("Open Folder", ButtonBar.ButtonData.OTHER);
		ButtonType btnClose = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

		alert.getButtonTypes().setAll(btnOpenFile, btnOpenFolder, btnClose);

		alert.showAndWait().ifPresent(type -> {
			if (type == btnOpenFolder) {
				FileOpeningService.getInstance().openSystemDefault(exportedFile.getParentFile());
			} else if (type == btnOpenFile) {
				FileOpeningService.getInstance().openSystemDefault(exportedFile);
			}
		});
	}

	private void showAlert(Alert.AlertType type, String title, String header, String content, Window... owner) {
		Window dialogOwner = (owner.length > 0 && owner[0] != null) ? owner[0] : getPrimaryStage();
		utils.ui.WindowUtils.showAlertDialog(type, dialogOwner, title, header, content);
	}

	private void showErrorAlert(String title, String message, Exception e, Window... owner) {
		Window dialogOwner = (owner.length > 0 && owner[0] != null) ? owner[0] : getPrimaryStage();
		utils.ui.WindowUtils.showExceptionDialog(AlertType.ERROR, dialogOwner, title, message, e != null ? e.getMessage() : "No further details.", e);
	}

}
