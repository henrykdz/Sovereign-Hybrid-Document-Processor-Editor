package com.flowshift.editor.ui.dialog;

import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.kordamp.ikonli.javafx.FontIcon;

import com.flowshift.editor.DocumentFormat;
import com.flowshift.editor.model.DocumentSettings;
import com.flowshift.editor.model.Placeholder;
import com.flowshift.editor.ui.LinterLineNumberFactory;
import com.flowshift.editor.util.HeaderTemplateManager;
import com.flowshift.editor.util.MarkdownHighlighter;
import com.flowshift.editor.util.MarkdownLinter;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import utils.logging.Log;

/**
 * ProjectSettingsDialog v7.7.3 - Final Sovereign Edition. Ein hochpräzises Cockpit für Dokumenten-Geometrie und Branding.
 */
public class ProjectSettingsDialog extends Dialog<DocumentSettings> {

	private static final String GLOBAL_TAGS_FORBIDDEN = "Global tags forbidden!";

	private static final String ERROR_S_DETECTED = " Error(s) detected";

	private static final String STRUCTURE_OK = "Structure: OK";

	private final HeaderTemplateManager headerManager;
	private final MarkdownHighlighter   forgeHighlighter;
	private final Parser                forgeParser;

	private LinterLineNumberFactory lineFactory;

	// UI - Container
	private final VBox identityContent = new VBox(20);

	// Master Controls
	private final CheckBox     persistYamlToggle = new CheckBox("Enable Document Branding (YAML)");
	private final Button       btnApplyDefaults  = new Button("Apply Project Defaults to Fields");
	private final ToggleButton btnWrap           = new ToggleButton();

	// UI - Identity
	private final TextField                 documentTitleField = new TextField(), authorField = new TextField(), deptField = new TextField(), versionField = new TextField(),
	        statusField = new TextField(), companyNameField = new TextField(), logoUrlField = new TextField();
	private final ChoiceBox<String>         themeChoiceBox     = new ChoiceBox<>();
	private final ChoiceBox<DocumentFormat> formatChoiceBox    = new ChoiceBox<>();
	private final ChoiceBox<String>         styleSelector      = new ChoiceBox<>();

	// Bei den anderen Controls
//	private final CheckBox autoPaginationToggle = new CheckBox("Enable Professional Auto-Pagination");
	// Bei den Feldern oben
	private final ToggleButton paginationToggle = new ToggleButton();

	// UI - Margins
	private final TextField marginTopField = new TextField(), marginBottomField = new TextField(), marginLeftField = new TextField(), marginRightField = new TextField();
	// UI - Quick Margin Actions
	private final Button btnMarginStandard = new Button("Print Standard");
	private final Button btnMarginClear    = new Button("Clear All");
	// In ProjectSettingsDialog.java (innerhalb der Klasse)

	// UI - Custom Placeholders (Dynamisch)
	private final VBox                      customMetaContainer = new VBox(8);                 // Container für die dynamischen Zeilen
	private final Button                    btnAddCustomMeta    = new Button("Add Custom Tag");
	private final Map<TextField, TextField> activeCustomFields  = new LinkedHashMap<>();       // Speichert Key -> Value
	// Bei den anderen UI-Feldern
	private final Label customTagCountLabel = new Label("(0)");

	// UI - Forge
	private final TextField newNameField   = new TextField();
	private final CodeArea  headerCodeArea = new CodeArea();
	private final Label     linterStatus   = new Label(STRUCTURE_OK);
	private final Button    btnSaveAs      = new Button("Save as New Style"), btnDeleteStyle = new Button("Delete Style"), btnApplyPreview = new Button("Apply to Preview");
	// DER WÄCHTER-STATUS
	private boolean isInitializing = false;

	private ScrollPane             scrollPaneCustomList;
	private final DocumentSettings projectDefaults;

	private DocumentSettings    originalSettings;
	private Map<String, String> docMeta;

	public ProjectSettingsDialog(Window owner, DocumentSettings globalDefaults, Path projectPath, Consumer<DocumentSettings> previewUpdater) {

		// 1. MODALITY & IDENTITY
		initModality(javafx.stage.Modality.NONE);
		initOwner(owner);
		this.projectDefaults = globalDefaults;
		this.headerManager = new HeaderTemplateManager(projectPath);
		this.forgeHighlighter = new MarkdownHighlighter();

		// Configure local Flexmark parser for the Design Forge
		MutableDataSet options = new MutableDataSet();
		options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create()));
		this.forgeParser = Parser.builder(options).build();

		// 2. STYLING INTEGRATION
		URL cssUrl = getClass().getResource("/com/flowshift/editor/editor-style.css");
		if (cssUrl != null) {
			getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
		}

		// 3. DIALOG SHELL CONFIGURATION
		setTitle("Document Configuration Nexus");
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		getDialogPane().setMinHeight(838);
		getDialogPane().setPrefHeight(838);
		getDialogPane().setPrefWidth(1220);
		getDialogPane().getStyleClass().add("nexus-dialog");

		// 4. MAIN WORKSPACE LAYOUT
		HBox mainLayout = new HBox(30);
		mainLayout.setPadding(new Insets(25, 25, 0, 25));
		mainLayout.setAlignment(Pos.TOP_LEFT);

		VBox leftColumn = buildIdentityColumn();
		VBox rightColumn = buildForgeColumn(previewUpdater);
		Separator vSep = new Separator(javafx.geometry.Orientation.VERTICAL);
		vSep.setPrefHeight(600);
		mainLayout.getChildren().addAll(leftColumn, vSep, rightColumn);

		// 5. FOOTER ARCHITECTURE (Symmetric Anchor)
		VBox nexusContainer = new VBox();
		nexusContainer.setMinHeight(Region.USE_PREF_SIZE); // FLEXIBEL
		Separator footerSeparator = new Separator();

		// We set a generous top margin for the separator to create breathing room,
		// but 0 bottom margin, as the ButtonBar padding will handle the symmetry.
		VBox.setMargin(footerSeparator, new Insets(25, 0, 0, 0));
		footerSeparator.getStyleClass().add("footer-separator"); // For targeted CSS styling
		nexusContainer.getChildren().addAll(mainLayout, footerSeparator);
		getDialogPane().setContent(nexusContainer);

		// 6. DATA HYGIENE & LOGIC BINDING
//		this.isInitializing = true;
//		loadInitialData(themeNames, formatList, docMeta);
//		this.isInitializing = false;
//
//		setupEnhancedLogic(docSettings, previewUpdater);
//		setupResultConverter(docSettings);

		// --- 6. PERMANENT LOGIC BINDING ---
		// Diese Methoden registrieren Listener. Sie werden NUR EINMAL gerufen.
		setupEnhancedLogic(previewUpdater); // Initialisiert Listener
		setupResultConverter(); // Initialisiert OK-Button-Logik

		// 7. FINAL UI POLISHING
		// Apply numeric constraints to geometric fields
		Stream.of(marginTopField, marginBottomField, marginLeftField, marginRightField).forEach(this::makeNumeric);

		// --- 5. SOVEREIGN GEOMETRY & LIFECYCLE LOCK ---
		// We consolidate all layout adjustments into a single UI pulse
		// to ensure mathematical symmetry and prevent race conditions.
		Platform.runLater(() -> {
			Window window = getDialogPane().getScene().getWindow();

			if (window instanceof Stage stage) {
				// A. Window Layering & Sizing
				stage.setAlwaysOnTop(true);
				stage.setMinHeight(880); // Total window height including OS chrome

				// B. Container Flexibility
				// We allow the internal panes to be flexible while the Stage
				// enforces the minimum physical bounds.
				getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
				nexusContainer.setMinHeight(Region.USE_PREF_SIZE);

				// C. Global Lifecycle Link
				// Closing the primary editor automatically cleans up this satellite.
				if (owner instanceof Stage primary) {
					primary.setOnHiding(e -> stage.close());
				}
			}

			// D. Button Bar Symmetery
			// We access the internal ButtonBar and apply identical top and bottom padding.
			Node buttonBarNode = getDialogPane().lookup(".button-bar");
			if (buttonBarNode instanceof Region buttonBar) {
				// SYMMETRY LAW: Balanced 15px padding above and below the buttons.
				// This ensures perfect vertical centering between the separator and border.
				buttonBar.setPadding(new Insets(15, 25, 15, 25));
				buttonBar.setStyle("-fx-background-color: transparent;");
			}
		});

		// Finalizing UI setup
		enforceUniformHeights();
		enableResizing();

		// OK-Button Specialization: Prevent accidental submission via global ENTER key
		Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
		if (okButton != null) {
			okButton.setDefaultButton(false);
			okButton.getStyleClass().add("button-primary");
		}

		Platform.runLater(documentTitleField::requestFocus);

		// Sorge dafür, dass er trotzdem oben bleibt, damit er nicht hinter
		// den Editor rutscht, solange du konfigurierst.
		Window window = getDialogPane().getScene().getWindow();
		if (window instanceof Stage stage) {
			stage.setAlwaysOnTop(true);
		}
	}

	/**
	 * Synchronizes the persistent dialog with the current document's reality. Re-injects metadata and resets UI fields to match the active editor state.
	 */
	public void updateDocumentData(DocumentSettings docSettings, Map<String, String> docMeta, List<String> themeNames, List<DocumentFormat> formatList) {
		this.isInitializing = true; // Block UI triggers during sync

		this.originalSettings = docSettings;
		this.docMeta = docMeta;

		// 1. Refresh Format and Theme Choices
		formatChoiceBox.getItems().setAll((DocumentFormat) null);
		formatChoiceBox.getItems().addAll(formatList);

		themeChoiceBox.getItems().setAll(themeNames);
		themeChoiceBox.setValue(docSettings.getActiveTheme());

		// 2. Load Metadata into Fields
		loadInitialData(themeNames, formatList, docMeta);

		// 3. Sync Header Forge
		updateEditorState(styleSelector.getValue(), docSettings.getHeaderHtml());

		this.isInitializing = false;
		Log.fine("Nexus: Context synchronized with active document.");
	}

	/**
	 * Constructs the left column of the Configuration Nexus.
	 * 
	 * Layout Strategy: 1. Document Identity (Title, Version, Status) --- Separator --- 2. Physical Layout (Format, Pagination) --- Separator --- 3. Geometric Precision (Stacked
	 * Margin Actions & Input Grid)
	 */
	private VBox buildIdentityColumn() {
		VBox col = new VBox(15);
		col.setMinWidth(450);
		col.setPrefWidth(450);

		// Global Master Toggle
		persistYamlToggle.setStyle("-fx-font-weight: bold; -fx-text-fill: #20BFDF; -fx-font-size: 13px;");

		// Project Blueprint Action
		btnApplyDefaults.getStyleClass().add("button-secondary");
		btnApplyDefaults.setMaxWidth(Double.MAX_VALUE);

		// --- SECTION: DOCUMENT CORE SETTINGS ---
		GridPane docGrid = createConfigGrid();
		addSectionHeader(docGrid, "Document Core Settings", "#20BFDF", 0);

		// 1. Group: Identity (Title, Version, Status)
		docGrid.add(new Label("Document Title:"), 0, 1);
		docGrid.add(documentTitleField, 1, 1);

		docGrid.add(new Label("Version / Status:"), 0, 2);
		HBox verBox = new HBox(10, versionField, statusField);
		versionField.setPrefWidth(100);
		statusField.setPrefWidth(200);
		docGrid.add(verBox, 1, 2);

		// HEALING: New Separator to differentiate Identity from Geometry
		docGrid.add(new Separator(), 0, 3, 2, 1);

		// 2. Group: Physical Format
		docGrid.add(new Label("Page Format:"), 0, 4);

		FontIcon pagIcon = new FontIcon("fas-file-alt");
		pagIcon.setIconSize(14);
		paginationToggle.setGraphic(pagIcon);
		paginationToggle.getStyleClass().addAll("pagination-toggle", "button-secondary");

		HBox formatRow = new HBox(10, formatChoiceBox, paginationToggle);
		formatRow.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(formatChoiceBox, Priority.ALWAYS);
		formatChoiceBox.setMaxWidth(Double.MAX_VALUE);
		docGrid.add(formatRow, 1, 4);

		docGrid.add(new Separator(), 0, 5, 2, 1);

		// 3. Group: Margins (Label Stack left, Centered Grid right)
		VBox marginLabelStack = new VBox(8);
		marginLabelStack.setAlignment(Pos.TOP_LEFT);

		Label lbMargins = new Label("Margins (mm):");
		lbMargins.setStyle("-fx-padding: 0 0 5 0;");

		styleMarginActionButton(btnMarginStandard, "fas-print", "#AAB8C7", "Set to 15mm Standard");
		styleMarginActionButton(btnMarginClear, "fas-eraser", "#FF5252", "Reset to 0mm (Neutral)");

		btnMarginStandard.setMaxWidth(110);
		btnMarginClear.setMaxWidth(110);

		marginLabelStack.getChildren().addAll(lbMargins, btnMarginStandard, btnMarginClear);
		docGrid.add(marginLabelStack, 0, 6);

		// Precise Margin Input Grid
		GridPane marginGrid = new GridPane();
		marginGrid.setHgap(15);
		marginGrid.setVgap(8);
		marginGrid.add(createMarginLabel("Top:"), 0, 0);
		marginGrid.add(marginTopField, 1, 0);
		marginGrid.add(createMarginLabel("Bottom:"), 2, 0);
		marginGrid.add(marginBottomField, 3, 0);
		marginGrid.add(createMarginLabel("Left:"), 0, 1);
		marginGrid.add(marginLeftField, 1, 1);
		marginGrid.add(createMarginLabel("Right:"), 2, 1);
		marginGrid.add(marginRightField, 3, 1);

		// HEALING: Vertical centering for the inputs via Wrapper
		VBox marginGridWrapper = new VBox(marginGrid);
		marginGridWrapper.setAlignment(Pos.CENTER);
		docGrid.add(marginGridWrapper, 1, 6);

		// --- SECTION: PROJECT IDENTITY & DEFAULTS ---
		GridPane projGrid = createConfigGrid();
		addSectionHeader(projGrid, "Project Identity & Defaults", "#AAB8C7", 0);
		projGrid.add(new Label("Company Name:"), 0, 1);
		projGrid.add(companyNameField, 1, 1);
		projGrid.add(new Label("Logo URL/Path:"), 0, 2);
		projGrid.add(logoUrlField, 1, 2);
		projGrid.add(new Label("Author / Dept:"), 0, 3);
		HBox authBox = new HBox(10, authorField, new Label("/"), deptField);
		authorField.setPrefWidth(150);
		deptField.setPrefWidth(150);
		projGrid.add(authBox, 1, 3);
		projGrid.add(new Label("Editor Theme:"), 0, 4);
		projGrid.add(themeChoiceBox, 1, 4);

		// Assemble Identity Content
		identityContent.getChildren().clear();
		identityContent.getChildren().addAll(docGrid, new Separator(), projGrid);

		col.getChildren().addAll(persistYamlToggle, new Separator(), btnApplyDefaults, identityContent, new Separator(), createPlaceholderLegend());

		return col;
	}

	/**
	 * Styles the small quick-action buttons for margin management.
	 */
	private void styleMarginActionButton(Button btn, String iconCode, String hexColor, String tooltip) {
		FontIcon icon = new FontIcon(iconCode);
		icon.setIconSize(10);
		icon.setIconColor(javafx.scene.paint.Color.web(hexColor));
		btn.setGraphic(icon);
		btn.setTooltip(new Tooltip(tooltip));
		btn.getStyleClass().add("button-secondary");
		btn.setAlignment(Pos.CENTER_LEFT);
		// Compact font and fixed horizontal padding
		btn.setStyle("-fx-font-size: 10px; -fx-padding: 3 8;");
	}

	/**
	 * Builds the right column of the Nexus, now featuring a split layout: TOP: Custom Metadata & Placeholders BOTTOM: Header Design Forge
	 * 
	 * This ensures equal space distribution and avoids UI obstruction.
	 */
	private VBox buildForgeColumn(Consumer<DocumentSettings> previewUpdater) {
		VBox col = new VBox(25);
		HBox.setHgrow(col, Priority.ALWAYS);

		// 1. TOP SECTION: Custom Metadata
		VBox customMetaSection = buildCustomMetaSection();
		// Wir lassen der Sektion etwas Luft zum Atmen, fixieren aber die Basis
		VBox.setVgrow(customMetaSection, Priority.NEVER);

		// 2. BOTTOM SECTION: Header Design Forge
		VBox forgeWrapper = new VBox(10);
		VBox.setVgrow(forgeWrapper, Priority.ALWAYS); // Die Forge darf den Rest des Platzes nehmen
		forgeWrapper.setStyle("-fx-border-color: #404040; -fx-padding: 15; -fx-border-radius: 5; -fx-background-color: rgba(255,255,255,0.02);");

		// 1. OBERE HBOX: Titel, Style Selector und Wrap Toggle
		Label lbTitle = createHeaderLabel("Header Design Forge", "#D79050");

		FontIcon wrapIcon = new FontIcon("fas-paragraph");
		wrapIcon.setIconSize(14);
		btnWrap.setGraphic(wrapIcon);
		btnWrap.setTooltip(new Tooltip("Toggle Word Wrap"));

		// Style Selector HBOX (Rechts neben dem Titel)
		HBox styleControl = new HBox(5, new Label("Style:"), styleSelector, btnWrap);
		styleControl.setAlignment(Pos.CENTER_LEFT);

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		HBox titleBar = new HBox(15, lbTitle, spacer, styleControl);
		titleBar.setAlignment(Pos.CENTER_LEFT);
		VBox.setMargin(titleBar, new Insets(0, 0, 5, 0)); // Platz nach unten

		// 2. FORGE CODE AREA
		Node forgeComponent = configureForgeEditor();
		VBox.setVgrow(forgeComponent, Priority.ALWAYS);

		// Diagnostics Cluster
		Region spacer1 = new Region();
		Region spacer2 = new Region();
		HBox.setHgrow(spacer1, Priority.ALWAYS);
		HBox.setHgrow(spacer2, Priority.ALWAYS);

		linterStatus.getStyleClass().add("status-badge");
		linterStatus.setPadding(new Insets(2, 8, 2, 8));

		// Actions & SaveAs
		HBox forgeActions = new HBox(10, btnApplyPreview, spacer1, linterStatus, spacer2, btnDeleteStyle);
		forgeActions.setAlignment(Pos.CENTER);

		HBox saveAsBox = new HBox(10, new Label("New Style:"), newNameField, btnSaveAs);
		HBox.setHgrow(newNameField, Priority.ALWAYS);

		// 3. ASSEMBLY DES FORGE WRAPPERS
		forgeWrapper.getChildren().addAll(titleBar, forgeComponent, forgeActions, saveAsBox);

		// Assembly der rechten Spalte MIT dem Separator dazwischen
		col.getChildren().addAll(customMetaSection, forgeWrapper);

		return col;
	}

	/**
	 * Builds the "Custom Metadata & Code Templates" section. Features a refined title bar with aligned elements: Title left, Count center, Add Button right.
	 */
	private VBox buildCustomMetaSection() {
		VBox box = new VBox(10); // Vertical spacing for section elements
		box.setPadding(new Insets(15)); // Padding for the section itself
		box.setStyle("-fx-border-color: #404040; -fx-border-radius: 5; -fx-background-color: rgba(255,255,255,0.02);"); // Border and background

		// --- TITLE BAR ASSEMBLY ---
		HBox titleBar = new HBox(15); // Horizontal spacing for elements within the title bar
		titleBar.setAlignment(Pos.CENTER_LEFT); // Align items to the left
		titleBar.getStyleClass().add("search-popup-container"); // Re-use search popup container for consistent styling

		// Left Aligned: Title
		Label title = createHeaderLabel("Custom Metadata & Code Templates", "#D79050");

		// Center Aligned: Dynamic Counter (with specific font and color)
		customTagCountLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #808080; -fx-font-family: 'Consolas';");
		customTagCountLabel.setPrefWidth(60); // Fixed width for consistency
		customTagCountLabel.setAlignment(Pos.CENTER); // Center the text within its bounds

		// Right Aligned: Add Button
		btnAddCustomMeta.getStyleClass().add("button-secondary");
		btnAddCustomMeta.setGraphic(new FontIcon("fas-plus"));
//		btnAddCustomMeta.setPrefWidth(130); // Standard width for buttons

		// --- SPACER for Right Alignment ---
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS); // Push elements to the left and right

		titleBar.getChildren().addAll(title, spacer, customTagCountLabel, btnAddCustomMeta); // Place them in the HBox
		titleBar.setAlignment(Pos.CENTER_LEFT); // Ensure the HBox itself is aligned left

		// --- Scrollable List for Templates ---
		customMetaContainer.setSpacing(5);
		customMetaContainer.setPadding(new Insets(0, 10, 0, 0));

		this.scrollPaneCustomList = new ScrollPane(customMetaContainer);
		scrollPaneCustomList.setFitToWidth(true);
		scrollPaneCustomList.setPrefHeight(163); // Keep a reasonable default height
		scrollPaneCustomList.getStyleClass().add("sidebar-scroll-pane");
		VBox.setVgrow(scrollPaneCustomList, Priority.ALWAYS); // Allow scroll pane to grow vertically

		// --- ASSEMBLY OF THE SECTION ---
		box.getChildren().addAll(titleBar, scrollPaneCustomList);

		// Button Action
		btnAddCustomMeta.setOnAction(e -> addCustomMetaRow("", ""));

		return box;
	}

	private void addCustomMetaRow(String key, String value) {
		TextField keyField = new TextField(key);
		keyField.setPromptText("Placeholder Key (z.B. SenderName)");
		keyField.setMaxWidth(150);

		TextField valueField = new TextField(value);
		valueField.setPromptText("Value");

		Button removeButton = new Button();
		removeButton.setGraphic(new FontIcon("fas-times"));
		removeButton.getStyleClass().addAll("icon-only-button", "danger");
		removeButton.setPrefWidth(28); // Höhe angleichen
		removeButton.setPrefHeight(28);

		// Visuelle Synchronisation mit den anderen Feldern
		Stream.of(keyField, valueField).forEach(f -> {
			f.setMinHeight(28);
			f.setMaxHeight(28);
			f.textProperty().addListener((obs, o, n) -> {
				if (!isInitializing)
					markActive(f);
				// Live-Update triggern
				if (!isInitializing)
					btnApplyPreview.fire();
			});
		});

		HBox row = new HBox(10, keyField, valueField, removeButton);
		HBox.setHgrow(valueField, Priority.ALWAYS);
		row.setAlignment(Pos.CENTER_LEFT);

		// Assoziation speichern
		activeCustomFields.put(keyField, valueField);
		customMetaContainer.getChildren().add(row);

		// Zähler aktualisieren
		updateCustomTagCounter();

		// Listener für Entfernen
		removeButton.setOnAction(e -> {
			customMetaContainer.getChildren().remove(row);
			activeCustomFields.remove(keyField);
			// Zähler aktualisieren
			updateCustomTagCounter();

			if (!isInitializing)
				btnApplyPreview.fire();
		});

		if (!isInitializing && key.isEmpty()) {
			Platform.runLater(() -> {
				keyField.requestFocus(); // Fokus auf das neue Feld setzen
				scrollToNode(keyField); // Scrollen zum neuen Feld
			});
		}
	}

	/**
	 * Scrolls to the specified node within the custom metadata container. Improved robust version.
	 */
	private void scrollToNode(Node node) {
		if (node == null || scrollPaneCustomList == null || customMetaContainer == null) {
			return;
		}

		Platform.runLater(() -> {
			try {
				// Warte einen Moment, damit das Layout komplett ist
				PauseTransition pause = new PauseTransition(Duration.millis(50));
				pause.setOnFinished(e -> {
					performScrollToNode(node);
				});
				pause.play();
			} catch (Exception ex) {
				Log.warn("Scroll to node failed: " + ex.getMessage());
			}
		});
	}

	private void performScrollToNode(Node node) {
		try {
			// Finde die HBox, die den Node enthält
			Node parent = node.getParent();
			while (parent != null && !(parent instanceof HBox) && parent != customMetaContainer) {
				parent = parent.getParent();
			}

			if (!(parent instanceof HBox hbox)) {
				// Falls keine HBox gefunden, scrolle zum Node selbst
				parent = node;
			} else {
				parent = hbox;
			}

			// Berechne die Position im Container
			int index = customMetaContainer.getChildren().indexOf(parent);
			if (index < 0) {
				return; // Nicht im Container gefunden
			}

			// Berechne kumulative Höhe
			double yPosition = 0;
			for (int i = 0; i < index; i++) {
				Node child = customMetaContainer.getChildren().get(i);
				yPosition += child.getBoundsInParent().getHeight();
				if (i < index - 1) {
					yPosition += customMetaContainer.getSpacing();
				}
			}

			// Berücksichtige Padding
			Insets padding = customMetaContainer.getPadding();
			yPosition += padding.getTop();

			// Berechne VValue
			double totalHeight = scrollPaneCustomList.getContent().getBoundsInLocal().getHeight();
			double viewportHeight = scrollPaneCustomList.getViewportBounds().getHeight();

			if (totalHeight <= viewportHeight) {
				return; // Kein Scrollen nötig
			}

			// Zielposition: Node im oberen Drittel des Viewports
			double targetVValue = (yPosition - (viewportHeight * 0.33)) / (totalHeight - viewportHeight);
			targetVValue = Math.max(0.0, Math.min(1.0, targetVValue));

			// Smooth scroll Animation
			Timeline timeline = new Timeline(new KeyFrame(Duration.millis(200), new KeyValue(scrollPaneCustomList.vvalueProperty(), targetVValue, Interpolator.EASE_BOTH)));
			timeline.play();

			// Fokus setzen
			Platform.runLater(() -> {
				node.requestFocus();

				// Visuelles Highlight (optional)
				if (node instanceof Region region) {
					String originalStyle = region.getStyle();
					region.setStyle(originalStyle + "; -fx-background-color: rgba(32, 191, 223, 0.15);");

					// Highlight nach 800ms entfernen
					PauseTransition removeHighlight = new PauseTransition(Duration.millis(800));
					removeHighlight.setOnFinished(event -> {
						region.setStyle(originalStyle);
					});
					removeHighlight.play();
				}
			});

			Log.fine("Scrolled to node at position: " + yPosition);

		} catch (Exception ex) {
			Log.warn("Error scrolling to node: " + ex.getMessage());
		}
	}

	private void updateCustomTagCounter() {
		int count = activeCustomFields.size();
		customTagCountLabel.setText("(" + count + ")");
	}

	/**
	 * Configures the interactive logic for the dialog. Binds UI fields to live preview updates and defines the "Apply Defaults" behavior.
	 */
	private void setupEnhancedLogic(Consumer<DocumentSettings> previewUpdater) {

		// --- DEFAULT WORKSPACE CONFIGURATION ---
		// Activating Word Wrap by default for a professional "out-of-the-box" experience.
		btnWrap.setSelected(true);
		headerCodeArea.setWrapText(true);

		// 1. AUTO-ACTIVATION & LIVE-UPDATE FOR ALL TEXT FIELDS
		Stream.of(documentTitleField, versionField, statusField, companyNameField, logoUrlField, authorField, deptField, marginTopField, marginBottomField, marginLeftField,
		        marginRightField).forEach(f -> {
			        f.textProperty().addListener((obs, o, n) -> {
				        if (!isInitializing && !n.equals(o)) {
					        markActive(f);
					        // Push current UI state to the main editor's preview
					        previewUpdater.accept(buildSettingsFromUI());
				        }
			        });
		        });

		// 2. PROJECT DEFAULTS ACTION
		// Wenn der Nutzer diesen Button klickt, werden die Werte aus der
		// globalen Vorlage (projectDefaults) in die UI-Felder kopiert.
		btnApplyDefaults.setOnAction(e -> {
			fillFromSettings(this.projectDefaults);
			Log.info("Nexus: Applied application blueprints to dialog fields.");
		});

		// 3. COMBO BOX & TOGGLE ACTIONS
		formatChoiceBox.setOnAction(e -> {
			if (!isInitializing) {
				syncPaginationToggleState();
				markActive(formatChoiceBox);
				previewUpdater.accept(buildSettingsFromUI());
			}
		});

		paginationToggle.setOnAction(e -> {
			if (!isInitializing) {
				if (!formatChoiceBox.getValue().isPaper) {
					paginationToggle.setSelected(false);
					markNeutral(paginationToggle);
					return;
				}
				markActive(paginationToggle);
				previewUpdater.accept(buildSettingsFromUI());
			}
		});

		// PRINT STANDARD (15mm)
		btnMarginStandard.setOnAction(e -> {
			Stream.of(marginTopField, marginBottomField, marginLeftField, marginRightField).forEach(f -> {
				f.setText("15");
				markActive(f);
			});
			previewUpdater.accept(buildSettingsFromUI());
		});

		// CLEAR ALL (0mm)
		btnMarginClear.setOnAction(e -> {
			Stream.of(marginTopField, marginBottomField, marginLeftField, marginRightField).forEach(f -> {
				f.setText("0");
				markNeutral(f);
			});
			previewUpdater.accept(buildSettingsFromUI());
		});

		styleSelector.setOnAction(e -> {
			if (!isInitializing && !"INHERIT".equals(styleSelector.getValue()))
				markActive(styleSelector);
		});

		// 2. MASTER SWITCH
		persistYamlToggle.selectedProperty().addListener((obs, old, isEnabled) -> {
			identityContent.setDisable(!isEnabled);
			btnApplyDefaults.setDisable(!isEnabled);
		});
		identityContent.setDisable(!persistYamlToggle.isSelected());
		btnApplyDefaults.setDisable(!persistYamlToggle.isSelected());

		btnWrap.setOnAction(e -> headerCodeArea.setWrapText(btnWrap.isSelected()));

		headerCodeArea.textProperty().addListener((obs, old, newVal) -> {
			triggerHighlighting();
			validateHeaderCode(newVal);
			headerCodeArea.setParagraphGraphicFactory(null);
			headerCodeArea.setParagraphGraphicFactory(lineFactory);
		});

		headerCodeArea.currentParagraphProperty().addListener((obs, old, newPar) -> {
			if (lineFactory != null)
				lineFactory.updateCurrentLine(newPar.intValue());
		});

		Platform.runLater(() -> {
			if (lineFactory != null)
				lineFactory.updateCurrentLine(headerCodeArea.getCurrentParagraph());
		});

		btnApplyPreview.setOnAction(e -> previewUpdater.accept(buildSettingsFromUI()));

		styleSelector.valueProperty().addListener((obs, old, newVal) -> {
			if (newVal != null)
				updateEditorState(newVal, null);
		});
		btnSaveAs.setOnAction(e -> handleSaveStyle());
		btnDeleteStyle.setOnAction(e -> handleDeleteStyle());

	}

	private DocumentSettings buildSettingsFromUI() {
		DocumentSettings s = new DocumentSettings();
		s.setDocumentTitle(documentTitleField.getText());
		s.setVersion(versionField.getText());
		s.setStatus(statusField.getText());
		s.setCompanyName(companyNameField.getText());
		s.setLogoUrl(logoUrlField.getText());
		s.setAuthorName(authorField.getText());
		s.setDepartment(deptField.getText());
		s.setFormat(formatChoiceBox.getValue());
		s.setPaginated(paginationToggle.isSelected());
		s.setActiveHeaderStyle(styleSelector.getValue());
		s.setHeaderHtml(headerCodeArea.getText());
		s.setActiveTheme(originalSettings.getActiveTheme());
		try {
			s.setMarginTop(Double.parseDouble(marginTopField.getText()));
			s.setMarginBottom(Double.parseDouble(marginBottomField.getText()));
			s.setMarginLeft(Double.parseDouble(marginLeftField.getText()));
			s.setMarginRight(Double.parseDouble(marginRightField.getText()));
		} catch (Exception ignored) {
		}
		return s;
	}

	private void fillFromSettings(DocumentSettings s) {
		this.isInitializing = true; // Blockiert Cyan während des Syncs
		documentTitleField.setText(s.getDocumentTitle());
		companyNameField.setText(s.getCompanyName());
		logoUrlField.setText(s.getLogoUrl());
		authorField.setText(s.getAuthorName());
		deptField.setText(s.getDepartment());
		versionField.setText(s.getVersion());
		statusField.setText(s.getStatus());
		formatChoiceBox.setValue(s.getFormat());
		// Alt: autoPaginationToggle.setSelected(s.enableAutoPagination);
		paginationToggle.setSelected(s.isPaginated());
		styleSelector.setValue(s.getActiveHeaderStyle());
		marginTopField.setText(String.valueOf(s.getMarginTop()));
		marginBottomField.setText(String.valueOf(s.getMarginBottom()));
		marginLeftField.setText(String.valueOf(s.getMarginLeft()));
		marginRightField.setText(String.valueOf(s.getMarginRight()));
		this.isInitializing = false;

		syncPaginationToggleState(); // <-- MANUELLER ANSTOSS

		Stream.of(documentTitleField, companyNameField, logoUrlField, authorField, deptField, versionField, statusField, formatChoiceBox, styleSelector, marginTopField,
		        marginBottomField, marginLeftField, marginRightField).forEach(this::markActive);
	}

	/**
	 * Defines the mapping between the Dialog buttons and the returned DocumentSettings. Ensures that user modifications are captured while preserving non-editable metadata.
	 */
	private void setupResultConverter() {
		setResultConverter(btn -> {
			if (btn == ButtonType.OK) {
				// Start with a copy of the document's original state
				DocumentSettings s = new DocumentSettings(this.originalSettings);

				// 2. OVERWRITE WITH CURRENT UI STATE
				s.setPersistYaml(persistYamlToggle.isSelected());
				s.setActiveTheme(themeChoiceBox.getValue());
				s.setHeaderHtml(headerCodeArea.getText());

				s.setDocumentTitle(documentTitleField.getText());
				s.setAuthorName(authorField.getText());
				s.setDepartment(deptField.getText());
				s.setVersion(versionField.getText());
				s.setStatus(statusField.getText());
				s.setCompanyName(companyNameField.getText());
				s.setLogoUrl(logoUrlField.getText());
				s.setPaginated(paginationToggle.isSelected());
				s.setFormat(formatChoiceBox.getValue());
				s.setActiveHeaderStyle(styleSelector.getValue());

				// Parse numeric margins safely
				try {
					s.setMarginTop(Double.parseDouble(marginTopField.getText()));
					s.setMarginBottom(Double.parseDouble(marginBottomField.getText()));
					s.setMarginLeft(Double.parseDouble(marginLeftField.getText()));
					s.setMarginRight(Double.parseDouble(marginRightField.getText()));
				} catch (NumberFormatException ignored) {
					// Fallback to original values if input is corrupt
				}

				// 3. PROCESS CUSTOM PLACEHOLDERS
				Map<String, String> customMetaMap = new LinkedHashMap<>();
				activeCustomFields.forEach((keyField, valueField) -> {
					String key = keyField.getText().trim();
					String value = valueField.getText().trim();
					if (!key.isEmpty() && Placeholder.fromKey(key).isEmpty()) {
						customMetaMap.put(key, value);
					}
				});
				s.setCustomPlaceholders(customMetaMap);

				return s;
			}
			return null;
		});
	}

	private Node configureForgeEditor() {
		this.lineFactory = new LinterLineNumberFactory(headerCodeArea, this::getErrorForLine);
		headerCodeArea.setParagraphGraphicFactory(lineFactory);
		headerCodeArea.getStyleClass().addAll("editor", "forge-editor");
		headerCodeArea.setMinHeight(50);
		VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(headerCodeArea);
		StackPane container = new StackPane(scrollPane);
		container.getStyleClass().add("forge-editor-container");
		container.setOnScroll(e -> {
			scrollPane.fireEvent(e);
			e.consume();
		});
		return container;
	}

	private void loadInitialData(List<String> themes, List<DocumentFormat> formats, Map<String, String> meta) {
		this.isInitializing = true; // Sicherstellen, dass UI-Trigger blockiert sind

		// 1. Master-Toggle basierend auf der Existenz von YAML setzen
		persistYamlToggle.setSelected(!meta.isEmpty());

		// 2. Text-Felder laden (Standard Keys)
		fillFieldStrict(documentTitleField, Placeholder.DOCUMENT_TITLE.getKey(), meta);
		fillFieldStrict(versionField, Placeholder.VERSION.getKey(), meta);
		fillFieldStrict(statusField, Placeholder.STATUS.getKey(), meta);
		fillFieldStrict(companyNameField, Placeholder.COMPANY_NAME.getKey(), meta);
		fillFieldStrict(logoUrlField, Placeholder.LOGO_URL.getKey(), meta);
		fillFieldStrict(authorField, Placeholder.AUTHOR_NAME.getKey(), meta);
		fillFieldStrict(deptField, Placeholder.DEPARTMENT.getKey(), meta);

		// 3. Physische Ränder laden
		fillMarginField(marginTopField, Placeholder.MARGIN_TOP.getKey(), originalSettings.getMarginTop(), meta);
		fillMarginField(marginBottomField, Placeholder.MARGIN_BOTTOM.getKey(), originalSettings.getMarginBottom(), meta);
		fillMarginField(marginLeftField, Placeholder.MARGIN_LEFT.getKey(), originalSettings.getMarginLeft(), meta);
		fillMarginField(marginRightField, Placeholder.MARGIN_RIGHT.getKey(), originalSettings.getMarginRight(), meta);

		// 4. Format und Paginierung
		formatChoiceBox.getItems().clear();
		formatChoiceBox.getItems().add(null); // Für "Inherit"
		formatChoiceBox.getItems().addAll(formats);

		if (meta.containsKey(Placeholder.FORMAT.getKey())) {
			formatChoiceBox.setValue(DocumentFormat.fromString(meta.get(Placeholder.FORMAT.getKey())));
			markActive(formatChoiceBox);
		} else {
			formatChoiceBox.getSelectionModel().select(0); // wählt 'null'
			markNeutral(formatChoiceBox);
		}

		// 5. Custom Metadata laden (Nutzung des Placeholder Enum zur Filterung)
		activeCustomFields.clear();
		customMetaContainer.getChildren().clear();

		meta.forEach((key, value) -> {
			// NUR Custom Tags laden: Wenn der Key NICHT im Enum definiert ist, ist es Custom
			if (Placeholder.fromKey(key).isEmpty()) {
				addCustomMetaRow(key, value);
			}
		});

		// Sicherstellen, dass beim Start mindestens eine leere Zeile sichtbar ist
		if (activeCustomFields.isEmpty()) {
			addCustomMetaRow("", "");
		}

		// 6. ChoiceBoxes initialisieren
		themeChoiceBox.getItems().setAll(themes);
		themeChoiceBox.setValue(this.originalSettings.getActiveTheme());

		styleSelector.getItems().setAll("INHERIT", "NONE");
		styleSelector.getItems().addAll(headerManager.getAvailableStyles());
		if (meta.containsKey(Placeholder.HEADER_STYLE.getKey())) {
			styleSelector.setValue(meta.get(Placeholder.HEADER_STYLE.getKey()));
			markActive(styleSelector);
		} else {
			styleSelector.setValue("INHERIT");
			markNeutral(styleSelector);
		}

		// 7. FINALE SYNCHRONISATION
		syncPaginationToggleState();

		// 8. Initialer Zustands-Sync für die Forge
		updateEditorState(styleSelector.getValue(), this.originalSettings.getHeaderHtml());
		validateHeaderCode(headerCodeArea.getText());
		triggerHighlighting();

		// Am Ende von loadInitialData, nach der Custom-Tags-Schleife:
		updateCustomTagCounter();

		this.isInitializing = false; // Abschluss der Initialisierung
	}

	private void syncPaginationToggleState() {
		DocumentFormat selected = formatChoiceBox.getValue();

		if (selected != null) {
			if (!selected.isPaper) {
				// HEILUNG 1: Visuell und Logisch auf FALSE setzen
				paginationToggle.setSelected(false);
				paginationToggle.setDisable(true);
				markNeutral(paginationToggle); // Wichtig: Entfernt den 'aktiv' Stil
			} else {
				// Format ist A4/Paper: Normaler Zustand
				paginationToggle.setDisable(false);

				// Den gespeicherten Wert aus YAML oder Default laden
				if (docMeta.containsKey(Placeholder.PAGINATION.getKey())) {
					paginationToggle.setSelected(Boolean.parseBoolean(docMeta.get(Placeholder.PAGINATION.getKey())));
				} else {
					paginationToggle.setSelected(selected.suggestPagination);
				}
				markActive(paginationToggle);
			}
		} else {
			// Kein Format gewählt (Inherit)
			paginationToggle.setDisable(true);
			paginationToggle.setSelected(false);
			markNeutral(paginationToggle);
		}
	}

	/**
	 * Zwingt ein TextField, nur Ganzzahlen (Integer) zu akzeptieren.
	 */
	private void makeNumeric(TextField field) {
		field.setTextFormatter(new TextFormatter<>(change -> {
			// Erlaubt NUR Ziffern. Absolut keine Punkte oder Kommas.
			if (change.getControlNewText().matches("\\d*")) {
				return change;
			}
			return null;
		}));
	}

	/**
	 * Erzeugt ein kleines Margin-Label, das niemals einknickt ("...").
	 */
	private Label createMarginLabel(String text) {
		Label l = new Label(text);
		l.setMinWidth(60); // Feste Breite für perfekte vertikale Flucht
		l.setAlignment(Pos.CENTER_RIGHT);
		l.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #AAB8C7; -fx-font-weight: bold;");
		return l;
	}

	/**
	 * Erzeugt ein Sektions-Label mit festem Branding-Stil. Frei von Container-Zwang.
	 */
	private Label createHeaderLabel(String text, String hexColor) {
		Label label = new Label(text);
		label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: " + hexColor + ";");
		return label;
	}

	private void addSectionHeader(GridPane grid, String title, String color, int row) {
		grid.add(createHeaderLabel(title, color), 0, row, 2, 1);
	}

	private void enforceUniformHeights() {
		Stream.of(documentTitleField, authorField, deptField, versionField, statusField, companyNameField, logoUrlField, themeChoiceBox, formatChoiceBox, styleSelector,
		        newNameField, btnSaveAs, btnDeleteStyle, btnApplyPreview, btnApplyDefaults, btnWrap, marginTopField, marginBottomField, marginLeftField, marginRightField // NEU
		).forEach(node -> {
			node.setMinHeight(28);
			node.setMaxHeight(28);
		});
	}

	private void enableResizing() {
		Platform.runLater(() -> {
			Window window = getDialogPane().getScene().getWindow();
			if (window instanceof Stage stage) {
				stage.setResizable(true);

				// Untergrenze für die Breite (schützt die 450px Spalte + etwas Forge)
				stage.setMinWidth(900);

				// Untergrenze für die Höhe (erlaubt der Forge das Schrumpfen)
				stage.setMinHeight(600);
			}
		});
	}

	private void fillMarginField(TextField f, String key, double currentValue, Map<String, String> meta) {
		// 1. Wenn der Wert explizit im YAML steht -> AKTIV (Cyan)
		if (meta.containsKey(key)) {
			f.setText(meta.get(key).split("\\.")[0]);
			markActive(f);
		}
		// 2. Wenn nicht, zeige den aktuellen Stand des Dokuments (meist 0)
		else {
			f.setText(String.valueOf((int) currentValue));
			// Wenn der Wert 0 ist, ist er NEUTRAL (Grau)
			if (currentValue == 0.0) {
				markNeutral(f);
			} else {
				// Falls das Dokument bereits einen Wert hatte, der nicht 0 ist
				markActive(f);
			}
		}
	}

	// Utility Methods
	private void fillFieldStrict(TextField f, String key, Map<String, String> meta) {
		if (meta.containsKey(key)) {
			f.setText(meta.get(key));
			markActive(f);
		} else {
			f.setText("");
			f.setPromptText("Inherited from Project...");
			markNeutral(f);
		}
	}

	private void markActive(Region n) {
		n.getStyleClass().remove("field-neutral");
		if (!n.getStyleClass().contains("field-active")) {
			n.getStyleClass().add("field-active");
		}
	}

	private void markNeutral(Region n) {
		n.getStyleClass().remove("field-active");
		if (!n.getStyleClass().contains("field-neutral")) {
			n.getStyleClass().add("field-neutral");
		}
	}

//	private boolean isNodeActive(Region n) {
//		return n.getStyle().contains("#20BFDF");
//	}

	private GridPane createConfigGrid() {
		GridPane g = new GridPane();
		g.setHgap(15);
		g.setVgap(12);
		ColumnConstraints c1 = new ColumnConstraints(120);
		ColumnConstraints c2 = new ColumnConstraints();
		c2.setHgrow(Priority.ALWAYS);
		g.getColumnConstraints().addAll(c1, c2);
		return g;
	}

	private Label createPlaceholderLegend() {
		StringBuilder sb = new StringBuilder("Available Core Tags: ");
		for (Placeholder p : Placeholder.values())
			sb.append(p.getTag()).append(" ");

		Label leg = new Label(sb.toString());
		leg.setWrapText(true);
		// Styling für eine dezente, aber lesbare Referenz
		leg.setStyle("-fx-font-size: 11px; -fx-text-fill: #808080; -fx-font-family: 'Consolas'; -fx-padding: 10 0 0 0;");
		return leg;
	}

	private void triggerHighlighting() {
		String text = headerCodeArea.getText();

		if (text == null || text.isEmpty()) {
			// Sicherheitsnetz für leeren Text
			headerCodeArea.clearStyle(0, headerCodeArea.getLength());
			return;
		}

		// --- HEILUNG: Der vollständige, souveräne Aufruf ---
		// Wir rufen die neue 5-Parameter-Methode auf und signalisieren,
		// dass der Design-Modus hier irrelevant ist.
		headerCodeArea.setStyleSpans(0, forgeHighlighter.applyHighlighting(text, forgeParser.parse(text), true, // includeLinterErrors
		        false, // isDesignMode (immer false in der Forge)
		        -1, // sanctuaryStart (irrelevant)
		        -1 // sanctuaryEnd (irrelevant)
		));
	}

	private void validateHeaderCode(String html) {
		if (html == null)
			return;

		// 1. Spezial-Check (Gehört nicht zum Standard-Linter, bleibt also hier)
		if (html.toLowerCase().contains("<body") || html.toLowerCase().contains("<html")) {
			updateLinterStatus(GLOBAL_TAGS_FORBIDDEN, false);
			return;
		}

		// 2. SOUVERÄNE EFFIZIENZ:
		// Wir fragen den Highlighter: "Was hast du gerade eben herausgefunden?"
		// Da triggerHighlighting() im Listener ZUERST gerufen wurde, sind diese Daten aktuell.
		List<MarkdownLinter.TagError> errs = forgeHighlighter.getLastErrors();

		// 3. Ergebnis einfach nur visualisieren
		if (errs.isEmpty()) {
			updateLinterStatus(STRUCTURE_OK, true);
		} else {
			updateLinterStatus(errs.size() + ERROR_S_DETECTED, false);
		}
	}

	private String getErrorForLine(int i) {
		return LinterLineNumberFactory.findErrorAtLine(headerCodeArea, forgeHighlighter.getLastErrors(), i);
	}

	private void handleSaveStyle() {
		String name = newNameField.getText().trim();
		if (name.isEmpty() || "NONE".equals(name) || "INHERIT".equals(name))
			return;
		headerManager.saveTemplate(name, headerCodeArea.getText());
		if (!styleSelector.getItems().contains(name))
			styleSelector.getItems().add(name);
		styleSelector.setValue(name);
		markActive(styleSelector);
		newNameField.clear();
	}

	private void handleDeleteStyle() {
		String sel = styleSelector.getValue();
		if (sel == null || "NONE".equals(sel) || "INHERIT".equals(sel))
			return;

		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete style '" + sel + "'?", ButtonType.OK, ButtonType.CANCEL);
		confirm.getDialogPane().getStyleClass().add("nexus-dialog");

		if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
			headerManager.deleteTemplate(sel);
			styleSelector.getItems().remove(sel);
			styleSelector.setValue("INHERIT"); // Fällt auf den Projekt-Default zurück
			markNeutral(styleSelector); // Da es jetzt ein Default ist
		}
	}

	/**
	 * Synchronisiert den Zustand des Forge-Editors mit dem gewählten Stil. Unterscheidet zwischen expliziter Löschung (NONE), Vererbung (INHERIT) und aktivem Design.
	 */
	private void updateEditorState(String styleName, String bufferedHtml) {
		// Wir holen den Namen des geerbten Stils aus dem Original-Modell
		String inheritedStyleName = originalSettings.getActiveHeaderStyle();

		if ("NONE".equals(styleName)) {
			// FALL 1: Explizit KEIN Header (Alles gesperrt und leer)
			headerCodeArea.setDisable(true);
			newNameField.setDisable(true);
			btnSaveAs.setDisable(true);
			btnDeleteStyle.setDisable(true);
			headerCodeArea.replaceText("");
		} else if ("INHERIT".equals(styleName)) {
			// FALL 2: Geerbt (Anzeigen als Vorlage, aber schreibgeschützt)
			headerCodeArea.setDisable(true);
			newNameField.setDisable(false); // Ableiten neuer Stile erlaubt
			btnSaveAs.setDisable(false);
			btnDeleteStyle.setDisable(true); // Projekt-Default darf nicht gelöscht werden
			headerCodeArea.replaceText(headerManager.loadTemplate(inheritedStyleName));
		} else {
			// FALL 3: Aktiver Stil (Vollzugriff)
			headerCodeArea.setDisable(false);
			newNameField.setDisable(false);
			btnSaveAs.setDisable(false);
			btnDeleteStyle.setDisable(false);

			// Nutze Buffer (für ungespeicherte Änderungen) oder lade von Platte
			if (bufferedHtml != null && !bufferedHtml.isEmpty()) {
				headerCodeArea.replaceText(bufferedHtml);
			} else {
				headerCodeArea.replaceText(headerManager.loadTemplate(styleName));
			}
		}

		// Symmetrischer Abschluss: Visuelle Validierung triggern
		triggerHighlighting();
	}

	private void updateLinterStatus(String msg, boolean isOk) {
		linterStatus.setText(msg); // .toUpperCase()
		linterStatus.getStyleClass().removeAll("status-ok", "status-error");

		if (isOk) {
			linterStatus.getStyleClass().add("status-ok");
			// Optisches Feedback: Apply Button ist sicher
			btnApplyPreview.setOpacity(1.0);
		} else {
			linterStatus.getStyleClass().add("status-error");
			// Warn-Feedback: Apply Button ist riskant (aber erlaubt)
			btnApplyPreview.setOpacity(0.7);
		}
	}
}