package utils.ui;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.util.Pair;
import utils.detection.Pathment;
import utils.detection.PathmentExtractor;
import utils.general.StringUtils;
import utils.localize.LangMap;
import utils.localize.LanguagePresets;
import utils.localize.LocalizableKey;
import utils.localize.MainUILangKey;
import utils.logging.Log;

/**
 * Utility class for common JavaFX window and dialog operations.
 * <p>
 * {@code WindowUtils} provides a centralized and consistent API for displaying various types of application dialogs, including alerts, confirmations, exceptions, and file choosers
 * (both open and save). It encapsulates common UI patterns such as:
 * <ul>
 * <li>Dialog owner management and fallback to a default owner.</li>
 * <li>Consistent styling and positioning (centering, bounds correction).</li>
 * <li>Flicker reduction during dialog display transitions.</li>
 * <li>Localization integration for dialog titles and messages.</li>
 * <li>Handling of native OS dialogs (FileChooser) with best-effort foregrounding.</li>
 * </ul>
 * <p>
 * To ensure proper functioning, especially for file choosers and owned alerts, the {@link #setDefaultOwner(Stage)} method **must be called once** at application startup (e.g., in
 * the {@code start()} method of the main {@link javafx.application.Application} class) to register the primary application stage.
 * </p>
 * <p>
 * This class is designed to be a singleton utility; it cannot be instantiated.
 * </p>
 */
public class WindowUtils {

	// =========================================================================
	// === FINAL, WARNING-FREE TEST CODE =======================================
	// =========================================================================

	/**
	 * A self-contained test application for displaying and testing various dialogs provided by the {@link WindowUtils} class. To run it, right-click within this class in your IDE
	 * and select "Run As" -> "Java Application".
	 */
	public static class DialogTestApp extends Application {

		private Stage primaryStage; // Reference to the primary stage for owner

		@Override
		public void start(Stage primaryStage) {
			this.primaryStage = primaryStage;

			// --- Essential Setup for WindowUtils ---
			WindowUtils.setDefaultOwner(primaryStage); // Set the default owner for all dialogs
			LangMap.init(); // Initialize language system for localized dialogs
			LangMap.setUserLanguage(LanguagePresets.getLanguageInfo("en-GB").getLocaleConstruct()); // Set a consistent test language

			System.out.println("Starting WindowUtils Dialog Test App UI...");

			// --- Create Test UI ---
			VBox root = new VBox(10); // Spacing of 10
			root.setPadding(new Insets(20));
			root.setAlignment(Pos.CENTER);

			Label titleLabel = new Label("WindowUtils Dialog Test Cases");
			titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
			root.getChildren().add(titleLabel);

			// Add buttons for each test case
			addButton(root, "Show Alert Dialog", e -> testShowAlertDialog());
			addButton(root, "Show Confirmation Dialog", e -> testShowConfirmationDialog());
			addButton(root, "Show Custom Alert Dialog", e -> testShowCustomAlertDialog());

			// Ersetzt den alten "Exit" Dialog-Button durch zwei spezifische Testfälle
			addButton(root, "Show App Exit Dialog", e -> testShowAppExitDialog());
			addButton(root, "Show Archive Close Dialog", e -> testShowArchiveCloseDialog());

			addButton(root, "Show File Chooser Dialogs", e -> testShowFileChooserDialogs());
			addButton(root, "Exit App", e -> Platform.exit());

			Scene scene = new Scene(root, 400, 550); // Adjust size as needed
			primaryStage.setScene(scene);
			primaryStage.setTitle("WindowUtils Test App");
			primaryStage.setAlwaysOnTop(true); // Keep the test app always on top for easy interaction
			primaryStage.show();

			System.out.println("WindowUtils Test App UI is ready. Click buttons to test dialogs.");
		}

		/** Helper method to add a button to the VBox. */
		private void addButton(VBox parent, String text, EventHandler<ActionEvent> handler) {
			Button button = new Button(text);
			button.setMaxWidth(Double.MAX_VALUE); // Make buttons fill width
			button.setOnAction(handler);
			parent.getChildren().add(button);
		}

		/**
		 * Test case for {@link WindowUtils#showAlertDialog(Alert.AlertType, Window, String, String, String)}.
		 */
		private void testShowAlertDialog() {
			System.out.println("\n--- Test: showAlertDialog (Info) ---");
			WindowUtils.showAlertDialog(Alert.AlertType.INFORMATION, primaryStage, "Info Title", "Info Header", "This is a general information message.");

			System.out.println("\n--- Test: showAlertDialog (Warning) ---");
			WindowUtils.showAlertDialog(Alert.AlertType.WARNING, null, // Test with null owner to use defaultOwner
			        "Warning Title", "Warning Header", "This is a general warning message.");
		}

		/**
		 * Test case for {@link WindowUtils#showConfirmationDialog(Window, String, String, String)}.
		 */
		private void testShowConfirmationDialog() {
			System.out.println("\n--- Test: showConfirmationDialog ---");
			boolean result = WindowUtils.showConfirmationDialog(primaryStage, "Confirm Action", "Are you sure?", "Do you really want to proceed with this action?");
			System.out.println("Confirmation Dialog Result: " + (result ? "YES" : "NO/CANCEL"));
		}

		/**
		 * Test case for {@link WindowUtils#showCustomAlertDialog(Alert.AlertType, Window, String, String, String, List, ButtonType)}.
		 */
		private void testShowCustomAlertDialog() {
			System.out.println("\n--- Test: showCustomAlertDialog ---");
			List<ButtonType> customButtons = Arrays.asList(new ButtonType("Option A"), new ButtonType("Option B"), ButtonType.CANCEL);
			Optional<ButtonType> result = WindowUtils.showCustomAlertDialog(Alert.AlertType.CONFIRMATION, primaryStage, "Custom Dialog", "Choose an option:",
			        "Please select your preferred action from the following:", customButtons, ButtonType.CANCEL);
			result.ifPresent(buttonType -> System.out.println("Custom Dialog Result: " + buttonType.getText()));
		}

		/**
		 * Test case for the APP_EXIT mode of {@link WindowUtils#showSaveConfirmationDialog(Window, izon.utils.ui.WindowUtils.CloseActionContext, String...)}. This tests the dialog
		 * with specific exit-related texts and the "remember choice" checkbox.
		 */
		private void testShowAppExitDialog() {
			System.out.println("\n--- Test: showSaveConfirmationDialog (APP_EXIT mode) ---");

			// Rufe die neue, flexible Methode mit dem korrekten Modus auf.
			// Die Enum liegt jetzt korrekt in WindowUtils.
			Pair<ButtonType, Boolean> result = WindowUtils.showSaveConfirmationDialog(primaryStage, WindowUtils.CloseActionContext.EXITING_APP);

			System.out.println("App Exit Dialog Result: Button=" + result.getKey().getText() + ", RememberCheckbox=" + result.getValue());
		}

		/**
		 * Test case for the ARCHIVE_CLOSE mode of {@link WindowUtils#showSaveConfirmationDialog(Window, izon.utils.ui.WindowUtils.CloseActionContext, String...)}. This tests the
		 * dialog with archive-specific texts and NO "remember choice" checkbox.
		 */
		private void testShowArchiveCloseDialog() {
			System.out.println("\n--- Test: showSaveConfirmationDialog (ARCHIVE_CLOSE mode) ---");

			// Rufe die neue, flexible Methode mit dem anderen Modus auf.
			// Wir übergeben den Archivnamen als Format-Argument für den Header-Text.
			Pair<ButtonType, Boolean> result = WindowUtils.showSaveConfirmationDialog(primaryStage, WindowUtils.CloseActionContext.CLOSING_ARCHIVE, "MyTestArchive.wms");

			// Das Ergebnis der Checkbox wird hier immer 'false' sein, da sie nicht angezeigt wird.
			System.out.println("Archive Close Dialog Result: Button=" + result.getKey().getText() + ", RememberCheckbox=" + result.getValue());
		}

		/**
		 * Test case for {@link WindowUtils#openFileChooser(Stage, String, File, List, boolean, boolean)} and
		 * {@link WindowUtils#saveFileChooser(Stage, String, File, List, boolean, boolean)}.
		 */
		private void testShowFileChooserDialogs() {
			System.out.println("\n--- Test: openFileChooser ---");
			List<FileChooser.ExtensionFilter> openFilters = Arrays.asList(new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"),
			        new FileChooser.ExtensionFilter("All Files (*.*)", "*.*"));
			File openedFile = WindowUtils.openFileChooser(primaryStage, "Open Test File", new File(System.getProperty("user.home")), openFilters, true, true);
			System.out.println("Opened File: " + (openedFile != null ? openedFile.getAbsolutePath() : "Cancelled"));

			System.out.println("\n--- Test: saveFileChooser ---");
			List<FileChooser.ExtensionFilter> saveFilters = Arrays.asList(new FileChooser.ExtensionFilter("Log Files (*.log)", "*.log"));
			// KORREKTUR: 'null' für initialFileName hinzugefügt
			File savedFile = WindowUtils.saveFileChooser(primaryStage, "Save Log File", new File(System.getProperty("user.home")), null, saveFilters, false, true);
			System.out.println("Saved File: " + (savedFile != null ? savedFile.getAbsolutePath() : "Cancelled"));
		}

		@Override
		public void stop() {
			System.out.println("WindowUtils Test App stopped.");
			// Perform any necessary cleanup here, though Platform.exit() handles most.
		}
	}

	/**
	 * The main method that launches our small test application.
	 *
	 * @param args Command line arguments.
	 */
	public static void main(String[] args) {
		Application.launch(DialogTestApp.class, args);
	}

	// =========================================================================
	// === END OF THE TEST CODE ================================================
	// =========================================================================

	/**
	 * Enum for different stage adjustment types.
	 */
	public enum AdjustmentType {
		/**
		 * Adjust the stage's position relative to its owner and the center of the screen.
		 */
		OWNER_TO_SCREEN_CENTER,
		/**
		 * Adjust the stage's position to ensure it stays within the screen bounds.
		 */
		WITHIN_SCREEN_BOUNDS,
		/**
		 * Center the stage on the screen.
		 */
		SCREEN_CENTER;
	}

	/**
	 * Defines the context for which a save confirmation dialog is shown. This controls the texts, buttons, and options displayed in the dialog.
	 */
	public enum CloseActionContext {
		OPENING_ANOTHER,
		CREATING_NEW,
		CLOSING_ARCHIVE,
		EXITING_APP
	}

	public enum ConfirmationDialogPreset {
		EXISTING_DIRECTORY_TITLE("confirmation_title_existing_directory", "confirmation_header_existing_directory", "confirmation_message_increment_suffix");

		public final String titleKey;
		public final String headerKey;
		final String        messageKey;

		private ConfirmationDialogPreset(String titleKey, String headerKey, String messageKey) {
			this.titleKey = titleKey;
			this.headerKey = headerKey;
			this.messageKey = messageKey;
		}
	}

	public static boolean showConfirmationDialogPreset(ConfirmationDialogPreset preset, Window owner) {
		String dummyName = "TestName";
		String dummyIncrement = "2";
		return showConfirmationDialogPreset(preset, owner, dummyName, dummyIncrement);
	}

	// Default window owner for dialogs (set to primary stage by default)
	private static Stage defaultOwner;

	/**
	 * Set the default owner for all dialogs. By setting up an owner, we can make sure dialogs are always centered related to the owner.
	 * 
	 * @param owner the Stage to be used as the default owner
	 */
	public static void setDefaultOwner(Stage owner) {
		if (owner == null) {
			Log.warn("Attempting to set null as the default owner for dialogs.");
		}
		defaultOwner = owner;
	}

	private WindowUtils() {
	}

	public static Rectangle2D getScreenBounds() {
		return Screen.getPrimary().getVisualBounds();
	}

	/**
	 * Displays a generic alert dialog (INFORMATION, WARNING, ERROR, CONFIRMATION). It ensures the dialog uses {@link #defaultOwner} as its actual owner, is positioned correctly,
	 * reduces flicker, and is set to always-on-top.
	 *
	 * @param alertType The type of alert to be displayed.
	 * @param owner     The conceptual owner window. If null, {@link #defaultOwner} will be used.
	 * @param title     The title of the alert dialog.
	 * @param header    The header text of the alert dialog.
	 * @param message   The main content message of the alert dialog.
	 */
	public static void showAlertDialog(Alert.AlertType alertType, Window owner, String title, String header, String message) {
		Alert alert = createAlertInternal(alertType, owner, title, header, message, true, false);
		alert.showAndWait();
	}

	// === NEUE PUBLIC SCHNITTSSTELLE (Wird von WebmarksApp genutzt) ===
	/**
	 * Creates a standard, fully styled Alert dialog with flicker reduction, using Label for content (non-editable).
	 * 
	 * @param alertType      The type of alert.
	 * @param owner          The dialog owner.
	 * @param title          The dialog title.
	 * @param header         The header text.
	 * @param message        The main content message.
	 * @param setAlwaysOnTop If true, sets the stage always on top.
	 * @return The created Alert dialog.
	 */
	public static Alert createDefaultAlert(Alert.AlertType alertType, Window owner, String title, String header, String message, boolean setAlwaysOnTop) {
		return createAlertInternal(alertType, owner, title, header, message, setAlwaysOnTop, false); // false = use Label
	}

	/**
	 * Creates an Alert dialog designed for displaying technical exceptions, using a TextArea for the main content message (editable=false).
	 * 
	 * @param alertType      The type of alert.
	 * @param owner          The dialog owner.
	 * @param title          The dialog title.
	 * @param header         The header text.
	 * @param message        The main content message (placed in TextArea).
	 * @param setAlwaysOnTop If true, sets the stage always on top.
	 * @return The created Alert dialog.
	 */
	public static Alert createTextAreaAlert(Alert.AlertType alertType, Window owner, String title, String header, String message, boolean setAlwaysOnTop) {
		return createAlertInternal(alertType, owner, title, header, message, setAlwaysOnTop, true); // true = use TextArea
	}

	private static Alert createAlertInternal(Alert.AlertType alertType, Window owner, String title, String header, String message, boolean setAlwaysOnTop, boolean useTextArea) {
		Alert alert = new Alert(alertType);

		if (owner == null)
			owner = defaultOwner;
		alert.initOwner(owner);
		alert.setTitle(title);
		alert.setHeaderText(header);

		// --- 1. DURCHGÄNGIGE TRENNLINIEN & PADDING VIA LOOKUP ---
		Platform.runLater(() -> {
			// Header-Linie (Unten)
			Node headerPanel = alert.getDialogPane().lookup(".header-panel");
			if (headerPanel != null) {
				headerPanel.setStyle("-fx-padding: 15 25 15 25; " + // Viel Raum im Header
				        "-fx-border-color: #D0D0D0; " + // Dezentes Grau
				        "-fx-border-width: 0 0 1 0;" // Nur unten eine Linie
				);
			}

			// ButtonBar-Linie (Oben)
			Node buttonBar = alert.getDialogPane().lookup(".button-bar");
			if (buttonBar != null) {
				buttonBar.setStyle("-fx-min-height: 72px; " + "-fx-padding: 15 25 15 25; " + "-fx-border-color: #D0D0D0; " + "-fx-border-width: 1 0 0 0;" // Nur oben eine Linie
				                                                                                                                                          // (Absolut durchgängig!)
				);
			}
		});

		// --- 2. CONTENT CONTAINER SETUP (Ohne manuelle Separatoren) ---
		VBox contentContainer = new VBox();
		contentContainer.setSpacing(0);
		// Nur seitliches Padding für den Text, oben/unten wird durch Header/ButtonBar geregelt
		contentContainer.setPadding(new Insets(20, 25, 0, 25));

		alert.getDialogPane().setPrefWidth(600);
		alert.getDialogPane().setMaxWidth(Region.USE_COMPUTED_SIZE);
		alert.getDialogPane().setMinSize(Control.USE_PREF_SIZE, Region.USE_COMPUTED_SIZE);
		alert.getDialogPane().setMaxSize(Control.USE_PREF_SIZE, Region.USE_COMPUTED_SIZE);

		if (StringUtils.isNotBlank(message)) {
			if (useTextArea) {
				TextArea messageNode = new TextArea(message);
				messageNode.setEditable(false);
				messageNode.setWrapText(true);
				messageNode.setMinHeight(80);
				messageNode.setPrefHeight(Region.USE_COMPUTED_SIZE);
				contentContainer.getChildren().add(messageNode);
			} else {
				TextFlow textFlow = new TextFlow();
				textFlow.setLineSpacing(3.0);
				updateTextFlow(textFlow, message);

				ScrollPane scrollPane = new ScrollPane(textFlow);
				scrollPane.setFitToWidth(true);
				scrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
				scrollPane.setMinHeight(160);
				scrollPane.setPrefHeight(240);

				scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");

				contentContainer.getChildren().add(scrollPane);
			}
		}

		alert.getDialogPane().setContent(contentContainer);

		// Button-Styling (nur die Breite, Padding wird oben via CSS erledigt)
		ButtonBar buttonBar = (ButtonBar) alert.getDialogPane().lookup(".button-bar");
		if (buttonBar != null) {
			buttonBar.setMinHeight(64);
			buttonBar.setPrefHeight(64);
			buttonBar.setButtonMinWidth(120);
			// Padding oben reduziert, da der Separator bereits Raum schafft
			buttonBar.setStyle("-fx-padding: 20 30 20 10;");
		}

		configureAlertStage(alert, setAlwaysOnTop);

		return alert;
	}

	/**
	 * Configures the stage properties for an alert. It handles opacity, always-on-top behavior, and ensures the window perfectly wraps its content using sizeToScene before
	 * initiating a smooth fade-in.
	 * 
	 * @author Henryk Daniel Zschuppan / Kassandra 2026
	 */
	private static void configureAlertStage(Alert alert, boolean setAlwaysOnTop) {
		Window window = alert.getDialogPane().getScene().getWindow();
		if (window instanceof Stage stage) {
			// Start completely invisible to prevent layout "jumping"
			stage.setOpacity(0);

			if (setAlwaysOnTop) {
				stage.setAlwaysOnTop(true);
			}

			stage.toFront();
			stage.requestFocus();

			// --- THE CRITICAL FIX: WINDOW_SHOWN HANDLER ---
			stage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {
				// 1. Force the stage to recalculate its size based on the
				// actual rendered scene content (heals the cut-off button).
				stage.sizeToScene();

				// 2. Re-center the window now that we have the definitive dimensions.
				// Using our established utility for screen centering.
				WindowUtils.centerStageOnScreen(stage);

				// 3. Once everything is perfectly sized and positioned, fade in.
				Timeline fadeIn = new Timeline(new KeyFrame(Duration.millis(120), new KeyValue(stage.opacityProperty(), 1.0, Interpolator.EASE_OUT)));
				fadeIn.play();
			});
		}
	}

	/**
	 * A path-safe helper method that parses a string and populates a TextFlow. It correctly distinguishes between intentional newlines and file paths.
	 */
	private static void updateTextFlow(TextFlow textFlow, String text) {
		textFlow.getChildren().clear();

		if (text == null || text.isEmpty()) {
			return;
		}

		// --- DER FIX: Wir splitten NUR an echten Newlines und <b> Tags ---
		// Wir nutzen keine manuelle Ersetzung mehr, um Pfade nicht zu korrumpieren.
		String[] parts = text.split("(?i)(?<=</b>)|(?=<b>)|(?<=<b>)|(?=</b>)|(?<=\n)|(?=\n)");

		boolean isBold = false;
		for (String part : parts) {
			// 1. Check for bold tags
			if (part.equalsIgnoreCase("<b>")) {
				isBold = true;
				continue;
			}
			if (part.equalsIgnoreCase("</b>")) {
				isBold = false;
				continue;
			}

			// 2. Check for ACTUAL newline character
			if (part.equals("\n")) {
				textFlow.getChildren().add(new Text("\n"));
				continue;
			}

			// 3. Process text nodes
			if (!part.isEmpty()) {
				Text textNode = new Text(part);
				textNode.getStyleClass().add("content-text");

				// Schriftfarbe für Lesbarkeit auf weißem Grund
				textNode.setFill(Color.web("#333333"));

				if (isBold) {
					textNode.setStyle("-fx-font-weight: bold;");
				}
				textFlow.getChildren().add(textNode);
			}
		}
	}

	/**
	 * Creates a fully styled TextInputDialog, applying custom ButtonBar padding and vertical content layout to match the visual style and alignment of WindowUtils Alerts.
	 *
	 * @param owner        The window that owns this dialog.
	 * @param title        The dialog title.
	 * @param header       The header text.
	 * @param content      The prompt text (Label above the TextField).
	 * @param initialValue The initial value in the text field.
	 * @return The created TextInputDialog instance.
	 */
	public static TextInputDialog createTextInputDialog(Window owner, String title, String header, String content, String initialValue) {
		// 1. Create standard dialog instance and set core properties
		TextInputDialog dialog = new TextInputDialog(initialValue);
		if (owner == null) {
			owner = defaultOwner;
		}
		dialog.initOwner(owner);
		dialog.getDialogPane().setPadding(new Insets(0, 0, 10, 0));
		dialog.setTitle(title);
		dialog.setHeaderText(header);
		dialog.setContentText(null); // Clear default content label
		// --- NEU HINZUGEFÜGT ---
		dialog.getDialogPane().setPrefWidth(500); // Setzt eine sinnvolle Standardbreite
		dialog.getDialogPane().setMinWidth(400); // Sorgt dafür, dass er nicht zu schmal wird
		// ------------------------
		// --- 2. Build Custom Vertical Content Layout ---

		// Find internal TextField (must exist)
		TextField textField = (TextField) dialog.getDialogPane().lookup(".text-input");

		// Create custom Label for prompt text (above the field)
		Label promptLabel = new Label(content);
		promptLabel.setWrapText(true);
		promptLabel.setMaxWidth(Double.MAX_VALUE);
		promptLabel.setPadding(new Insets(0)); // No internal padding

		// VBox to arrange Label over TextField (5px spacing)
		VBox verticalContent = new VBox(5, promptLabel, textField);

		// Apply empirically best padding to align content with Header (25px top margin)
		verticalContent.setPadding(new Insets(25, 20, 10, 20));

		// Replace the internal content with the new vertical VBox
		dialog.getDialogPane().setContent(verticalContent);

		// --- 3. ButtonBar Fixes (Padding and MinWidth) ---
		ButtonBar buttonBar = (ButtonBar) dialog.getDialogPane().lookup(".button-bar");

		if (buttonBar != null) {
			final double MIN_BUTTON_WIDTH = 70.0;
			// The validated, empirically best padding for the ButtonBar
			final String BUTTON_BAR_PADDING = "-fx-padding: 25 10 20 10;";

			buttonBar.setButtonMinWidth(MIN_BUTTON_WIDTH);
			buttonBar.setStyle(BUTTON_BAR_PADDING);
		}

		// --- 4. Stage Setup (Positioning, Visibility, Focus) ---
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		if (stage != null) {
			stage.setOpacity(0);
			stage.setAlwaysOnTop(true);

			attachStageAdjustmentHandler(stage, AdjustmentType.OWNER_TO_SCREEN_CENTER, dialog.getDialogPane().getPrefWidth(), dialog.getDialogPane().getPrefHeight());

			stage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {
				Timeline fadeIn = new Timeline(new KeyFrame(Duration.millis(100), new KeyValue(stage.opacityProperty(), 1.0)));
				fadeIn.play();
			});
		}

		return dialog;
	}

	/**
	 * Displays a flexible confirmation/prompt dialog with custom button types. This method centralizes the creation and display of JavaFX Alert dialogs with custom button sets,
	 * ensuring consistent styling and owner handling.
	 *
	 * @param alertType           The type of alert (e.g., INFORMATION, WARNING, CONFIRMATION).
	 * @param owner               The conceptual owner window. If null, {@link #defaultOwner} is used.
	 * @param title               The title of the dialog.
	 * @param header              The header text.
	 * @param message             The main content message.
	 * @param customButtonTypes   An array of {@link ButtonType}s to display in the dialog. The order matters.
	 * @param defaultResultButton The {@link ButtonType} that should be considered the default result if the dialog is closed without explicit user action (e.g., ESC key).
	 * @return An Optional containing the {@link ButtonType} chosen by the user, or empty if the dialog was closed without a selection.
	 */
	public static Optional<ButtonType> showCustomAlertDialog(AlertType alertType, Window owner, String title, String header, String message, List<ButtonType> customButtonTypes,
	        ButtonType defaultResultButton) {
		// HIER IST DIE ANPASSUNG: true für setAlwaysOnTop
		// createAlert wird nun das Padding und die Flicker-Reduzierung handhaben.
		Alert alert = createDefaultAlert(alertType, owner, title, header, message, true);

		// Set content message if it was set via setContentText in createAlert
		// If content is already set by createAlert's setContentText, we need to decide if this message should override or append.
		// Given how createAlert works, setContentText is called there. If you want this message *instead* of what createAlert sets,
		// you would do alert.setContentText(message); here, but it's redundant.
		// It's assumed createAlert has set the base message.

		// Set custom buttons
		if (customButtonTypes != null && !customButtonTypes.isEmpty()) {
			alert.getButtonTypes().setAll(customButtonTypes);
		} else {
			alert.getButtonTypes().setAll(ButtonType.OK);
		}

		// Set default button
		alert.setResultConverter(dialogButton -> {
			if (dialogButton == null)
				return defaultResultButton;
			return dialogButton;
		});

		return alert.showAndWait();
	}

	/**
	 * Displays an alert dialog specifically for showing an exception. It includes a user-friendly message in the main content area and the full stack trace of the throwable in an
	 * expandable section. This method is safe to be called from any thread.
	 *
	 * @param alertType        The type of alert (typically AlertType.ERROR).
	 * @param owner            The conceptual owner window for this dialog (can be null). If null, {@link #defaultOwner} is used.
	 * @param title            The title of the dialog window (e.g., "Application Error").
	 * @param header           A brief header summarizing the error (e.g., "Failed to load file.").
	 * @param shortUserMessage A user-friendly message explaining the error in simple terms.
	 * @param throwable        The {@link Throwable} instance whose stack trace is to be displayed.
	 */
	public static void showExceptionDialog(Alert.AlertType alertType, Window owner, String title, String header, String shortUserMessage, Throwable throwable) {
		// Ensure execution on the JavaFX Application Thread for UI operations.
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(() -> showExceptionDialog(alertType, owner, title, header, shortUserMessage, throwable));
			return;
		}

		try {
			// Use createAlert to handle common alert setup (owner, title, header, message, styling, positioning, flicker reduction, alwaysOnTop).
			// Exception dialogs are critical, so they should be always on top.
			Alert alert = createAlertInternal(alertType, owner, StringUtils.isNotBlank(title) ? title : "Exception Occurred", // Use provided title or fallback
			        StringUtils.isNotBlank(header) ? header : (throwable != null ? throwable.getClass().getSimpleName() : "Error"), // Use provided header or fallback
			        StringUtils.isNotBlank(shortUserMessage) ? shortUserMessage
			                : (throwable != null && throwable.getMessage() != null ? throwable.getMessage() : "An unexpected error occurred."), // Use provided message or fallback
			        true // Set alert always on top for critical exceptions.
			        , true);

			// Add expandable stack trace content if a throwable is provided.
			if (throwable != null) {
				java.io.StringWriter sw = new java.io.StringWriter();
				java.io.PrintWriter pw = new java.io.PrintWriter(sw);
				throwable.printStackTrace(pw);
				String stackTraceStr = sw.toString();

				TextArea textAreaStackTrace = new TextArea(stackTraceStr);
				textAreaStackTrace.setEditable(false);
				textAreaStackTrace.setWrapText(true);
				textAreaStackTrace.setPrefRowCount(15); // Default number of rows
				textAreaStackTrace.setMaxWidth(Double.MAX_VALUE);
				textAreaStackTrace.setMaxHeight(Double.MAX_VALUE);
				GridPane.setVgrow(textAreaStackTrace, Priority.ALWAYS);
				GridPane.setHgrow(textAreaStackTrace, Priority.ALWAYS);

				GridPane expandableContent = new GridPane();
				expandableContent.setMaxWidth(Double.MAX_VALUE);
				expandableContent.add(new Label("Technical Details (Stack Trace):"), 0, 0); // Localize this label if needed
				expandableContent.add(textAreaStackTrace, 0, 1);

				alert.getDialogPane().setExpandableContent(expandableContent); // Set expandable content
			}

			// Ensure a standard OK button exists and add a custom "Copy to Clipboard" button.
			alert.getButtonTypes().setAll(ButtonType.OK);
			Button copyButton = new Button("Copy to Clipboard"); // Localize this button text
			final String completeErrorText = createCompleteErrorText(alert.getHeaderText(), alert.getContentText(), throwable); // Use actual header/content for consistency

			copyButton.setOnAction(event -> {
				final Clipboard clipboard = Clipboard.getSystemClipboard();
				final ClipboardContent content = new ClipboardContent();
				content.putString(completeErrorText);
				clipboard.setContent(content);

				// Provide visual feedback for the copy action.
				copyButton.setText("Copied!"); // Localize this feedback text
				PauseTransition pause = new PauseTransition(Duration.seconds(2));
				pause.setOnFinished(e -> copyButton.setText("Copy to Clipboard")); // Restore original text
				pause.play();
			});

			// Attempt to add the copy button to the dialog's button bar.
			ButtonBar buttonBar = (ButtonBar) alert.getDialogPane().lookup(".button-bar");
			if (buttonBar != null) {
				buttonBar.getButtons().add(0, copyButton); // Add button to the far left
			} else {
				Log.warn("showExceptionDialog: Could not find the ButtonBar in the dialog to add the copy button.");
			}

			// The stage manipulation (sizing, stylesheets, flicker reduction, focus)
			// is now entirely handled by the createAlert method.
			// No direct stage access needed here anymore.

			// Show the dialog and wait for user interaction.
			alert.showAndWait();

		} catch (Exception e) {
			// Log critical errors if displaying the dialog itself fails.
			Log.error(e, "CRITICAL: Failed to display the exception dialog. The original error was:");
			if (throwable != null) {
				Log.error(throwable);
			}
		}
	}

	/**
	 * NEUE HILFSMETHODE: Erstellt einen formatierten String mit allen Fehlerinformationen.
	 *
	 * @param header  The header text.
	 * @param message The user-friendly message.
	 * @param t       The throwable.
	 * @return A formatted string for logging or copying.
	 */
	private static String createCompleteErrorText(String header, String message, Throwable t) {
		StringBuilder sb = new StringBuilder();
		sb.append("Error Summary\n");
		sb.append("=============\n");
		sb.append("Header: ").append(header).append("\n");
		sb.append("Message: ").append(message).append("\n\n");

		if (t != null) {
			sb.append("Stack Trace\n");
			sb.append("===========\n");
			java.io.StringWriter sw = new java.io.StringWriter();
			java.io.PrintWriter pw = new java.io.PrintWriter(sw);
			t.printStackTrace(pw);
			sb.append(sw.toString());
		}
		return sb.toString();
	}

	/**
	 * Displays a confirmation dialog using a predefined set of messages from the specified preset. The message format is localized and formatted with the provided arguments.
	 *
	 * @param preset The preset configuration containing the title, header, and message keys.
	 * @param owner  The window that owns this confirmation dialog.
	 * @param args   The arguments used to format the header and message content.
	 * @return {@code true} if the user clicked "OK", {@code false} if the user clicked "Cancel".
	 */
	public static boolean showConfirmationDialogPreset(ConfirmationDialogPreset preset, Window owner, Object... args) {
		// Check for null preset to avoid NullPointerException
		if (preset == null) {
			Log.error("showConfirmationDialogPreset called with a null preset.");
			// Optionally show a generic error dialog or just return false
			return false;
		}

		MessageFormat formatter = new MessageFormat("");
		// Use the new, unambiguous method to get the currently active locale
		formatter.setLocale(LangMap.getActiveLocale());

		String titleText = LangMap.getLangString(preset.titleKey);

		// Format header and message using the loaded strings
		String headerTemplate = LangMap.getLangString(preset.headerKey);
		String messageTemplate = LangMap.getLangString(preset.messageKey);

		// It's safer to format using the formatter instance to ensure the locale is respected
		formatter.applyPattern(headerTemplate);
		String headerText = formatter.format(args);

		formatter.applyPattern(messageTemplate);
		String messageText = formatter.format(args);

		// Call the underlying dialog display method
		return showConfirmationDialog(owner, titleText, headerText, messageText);
	}

	/**
	 * Displays a confirmation dialog with localized content for the title, header, and message. If use Translation, pass the language keys as parameters.
	 * 
	 * @param owner      The window that owns this confirmation dialog.
	 * @param keyTitle   The key used to fetch the localized title from the language map.
	 * @param keyHeader  The key used to fetch the localized header from the language map.
	 * @param keyMessage The key used to fetch the localized message from the language map.
	 * @return {@code true} if the user clicked "OK", {@code false} if the user clicked "Cancel".
	 */
	public static boolean showConfirmationDialog(Window owner, String title, String header, String message, boolean useTranslation) {
		if (useTranslation) {
			title = LangMap.getLangString(title);
			header = LangMap.getLangString(header);
			message = LangMap.getLangString(message);
		}
		return showConfirmationDialog(owner, title, header, message);
	}

	/**
	 * Displays a confirmation dialog with the specified title, header, and message content. The dialog includes "OK" and "Cancel" buttons, and the default action is bound to the
	 * "OK" button.
	 *
	 * @param ownerWindow The window that conceptually owns this confirmation dialog. If null, {@link #defaultOwner} will be used.
	 * @param title       The title of the confirmation dialog.
	 * @param header      The header text of the confirmation dialog.
	 * @param message     The message content of the confirmation dialog.
	 * @return {@code true} if the user clicked "OK", {@code false} if the user clicked "Cancel".
	 */
	public static boolean showConfirmationDialog(Window owner, String title, String header, String message) {
		Alert alert = createDefaultAlert(Alert.AlertType.CONFIRMATION, owner, title, header, message, true);

		ButtonType defaultButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
		alert.getButtonTypes().setAll(defaultButton, ButtonType.CANCEL);

		Optional<ButtonType> option = alert.showAndWait();
		return option.map(buttonType -> buttonType == defaultButton).orElse(false);
	}

	/**
	 * Displays a highly configurable confirmation dialog for saving changes, adapted to the given context. This is the central, intelligent method for all save prompts during
	 * application or archive closure.
	 *
	 * @param ownerWindow The window that owns this dialog.
	 * @param context     The {@link CloseActionContext} which determines the dialog's content, buttons, and options.
	 * @param formatArgs  Optional arguments for formatting strings (e.g., the archive name for the header).
	 * @return A Pair containing the user's selected ButtonType and the state of the "remember" checkbox.
	 */
	/**
	 * Displays a highly configurable confirmation dialog for saving changes. Optimized for English structure and consistent 45px Nexus-Symmetry.
	 */
	public static Pair<ButtonType, Boolean> showSaveConfirmationDialog(Window ownerWindow, CloseActionContext context, String... formatArgs) {
		String title, header, message;
		boolean showCheckbox;
		List<ButtonType> buttons;

		// 1. Configure the dialog based on the provided context
		switch (context) {
		case EXITING_APP:
			title = MainUILangKey.LK_EXIT_DIALOG_TITLE.get();
			header = MainUILangKey.LK_EXIT_DIALOG_HEADER.get();
			message = MainUILangKey.LK_EXIT_DIALOG_MESSAGE.get();
			showCheckbox = true; // Show the "remember choice" checkbox for app exit
			buttons = List.of(new ButtonType(MainUILangKey.LK_BUTTON_SAVE_AND_EXIT.get(), ButtonBar.ButtonData.FINISH),
			        new ButtonType(MainUILangKey.LK_BUTTON_EXIT_WITHOUT_SAVING.get(), ButtonBar.ButtonData.NO),
			        new ButtonType(MainUILangKey.LK_BUTTON_CANCEL.get(), ButtonBar.ButtonData.CANCEL_CLOSE));
			break;

		case OPENING_ANOTHER:
		case CREATING_NEW:
		case CLOSING_ARCHIVE:
		default:
			String archiveName = (formatArgs.length > 0) ? formatArgs[0] : "";
			title = MainUILangKey.LK_DIALOG_UNSAVED_CHANGES_TITLE.get();

			LocalizableKey headerKey;
			switch (context) {
			case OPENING_ANOTHER -> headerKey = MainUILangKey.LK_F_HEADER_UNSAVED_CHANGES_OPENING;
			case CREATING_NEW    -> headerKey = MainUILangKey.LK_F_HEADER_UNSAVED_CHANGES_CREATING;
			default              -> headerKey = MainUILangKey.LK_F_HEADER_UNSAVED_CHANGES_DEFAULT; // For CLOSING_ARCHIVE
			}
			header = headerKey.getFormatted(archiveName);

			message = MainUILangKey.LK_DIALOG_UNSAVED_CHANGES_MESSAGE.get();

			// The "remember choice" checkbox is explicitly hidden for the CREATING_NEW context
			showCheckbox = (context != CloseActionContext.CREATING_NEW);

			buttons = List.of(new ButtonType(MainUILangKey.LK_BUTTON_SAVE.get(), ButtonBar.ButtonData.YES),
			        new ButtonType(MainUILangKey.LK_BUTTON_DONT_SAVE.get(), ButtonBar.ButtonData.NO),
			        new ButtonType(MainUILangKey.LK_BUTTON_CANCEL.get(), ButtonBar.ButtonData.CANCEL_CLOSE));
			break;
		}

		// 2. Build and show the dialog
		Alert alert = createDefaultAlert(AlertType.CONFIRMATION, ownerWindow, title, header, "", true);

		Label messageLabel = new Label(message);

		// Create the VBox with the message label
		VBox alertContent = new VBox(messageLabel);
		alertContent.setSpacing(15);
		alertContent.setPadding(new Insets(10, 10, 10, 30));
		alertContent.setStyle("-fx-font-size: 13px;"); // Apply font style to the entire content VBox

		CheckBox rememberCheckbox = null;
		if (showCheckbox) {
			rememberCheckbox = new CheckBox(MainUILangKey.LK_DIALOG_CHECKBOX_REMEMBER_AUTOSAVE.get());
			rememberCheckbox.setWrapText(true);
			rememberCheckbox.setGraphicTextGap(8);
			alertContent.getChildren().add(rememberCheckbox);
		}

		alert.getDialogPane().setContent(alertContent);
		alert.getButtonTypes().setAll(buttons);

		ButtonType cancelBtn = buttons.stream().filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE).findFirst().get();
		alert.setResultConverter(dialogButton -> dialogButton == null ? cancelBtn : dialogButton);

		Optional<ButtonType> result = alert.showAndWait();
		boolean wasCheckboxSelected = (rememberCheckbox != null) && rememberCheckbox.isSelected();

		return new Pair<>(result.orElse(cancelBtn), wasCheckboxSelected);
	}

	/**
	 * Konvertiert eine Liste von einfachen Dateierweiterungs-Strings (z.B. "txt", "pdf") in eine Liste von {@link FileChooser.ExtensionFilter} Objekten.
	 *
	 * @param simpleExtensions Eine Liste von Dateierweiterungen ohne den Punkt (z.B. "txt").
	 * @return Eine Liste von ExtensionFilter-Objekten.
	 */
	private static List<FileChooser.ExtensionFilter> createFiltersFromSimpleExtensions(List<String> simpleExtensions) {
		if (simpleExtensions == null || simpleExtensions.isEmpty()) {
			return Collections.emptyList();
		}
		return simpleExtensions.stream().map(ext -> new FileChooser.ExtensionFilter(ext.toUpperCase() + " Files (*." + ext.toLowerCase() + ")", "*." + ext.toLowerCase())).toList();
	}

	/**
	 * Configures a {@link FileChooser} with the specified settings, including title, initial directory, and a list of {@link FileChooser.ExtensionFilter} objects. This is the
	 * **primary** configuration method for FileChooser in WindowUtils. All other configureFileChooser methods should delegate to this one.
	 *
	 * @param fileChooser                The {@link FileChooser} instance to configure.
	 * @param title                      The title of the file chooser window.
	 * @param initialDirectory           The initial directory to open in the file chooser. If null or invalid, the user's home directory will be used.
	 * @param filters                    A list of {@link FileChooser.ExtensionFilter} objects to add. Can be null or empty.
	 * @param addShowAllFilesFilter      A boolean flag indicating whether to add a generic "All Files" (*.*) filter.
	 * @param addShowAllCompatibleFilter A boolean flag indicating whether to add an "All Compatible Files" filter, which combines all extensions from the {@code filters} list.
	 */
	public static void configureFileChooser(FileChooser fileChooser, String title, File initialDirectory, List<FileChooser.ExtensionFilter> filters, boolean addShowAllFilesFilter,
	        boolean addShowAllCompatibleFilter) {
		fileChooser.setTitle(title);
		fileChooser.getExtensionFilters().clear(); // Important: Clear previous filters to avoid duplicates

		// Add "All Compatible Files" filter if requested and filters are provided
		if (addShowAllCompatibleFilter && filters != null && !filters.isEmpty()) {
			List<String> allExtensions = filters.stream().flatMap(filter -> filter.getExtensions().stream()).distinct().toList();
			if (!allExtensions.isEmpty()) {
				fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(LangMap.getLangString("common_all_compatible_files", "All Compatible Files"), allExtensions));
			}
		}

		// Add individual filters
		if (filters != null && !filters.isEmpty()) {
			fileChooser.getExtensionFilters().addAll(filters);
		}

		// Add "All Files" filter if requested
		if (addShowAllFilesFilter) {
			fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(LangMap.getLangString("common_all_files", "All Files"), "*.*"));
		}

		// Set initial directory
		if (initialDirectory != null && initialDirectory.exists() && initialDirectory.isDirectory()) {
			fileChooser.setInitialDirectory(initialDirectory);
		} else {
			fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		}
	}

	// --- NEW Convenience method for configuring FileChooser with simple extension strings ---
	/**
	 * Configures a {@link FileChooser} with the specified settings, including title, initial directory, and a list of simple file extension strings. This is a convenience method
	 * that converts the strings into {@link FileChooser.ExtensionFilter} objects. This method has a distinct name to avoid type erasure conflicts.
	 *
	 * @param fileChooser                The {@link FileChooser} instance to configure.
	 * @param title                      The title of the file chooser window.
	 * @param initialDirectory           The initial directory for the file chooser.
	 * @param simpleExtensions           A list of file extensions (e.g., "txt", "pdf").
	 * @param addShowAllFilesFilter      Flag indicating whether to add an "All Files" filter.
	 * @param addShowAllCompatibleFilter Flag indicating whether to add an "All Compatible Files" filter.
	 */
	public static void configureFileChooserBySimpleExtensions(FileChooser fileChooser, String title, File initialDirectory, List<String> simpleExtensions,
	        boolean addShowAllFilesFilter, boolean addShowAllCompatibleFilter) {
		List<FileChooser.ExtensionFilter> generatedFilters = createFiltersFromSimpleExtensions(simpleExtensions);
		configureFileChooser(fileChooser, title, initialDirectory, generatedFilters, addShowAllFilesFilter, addShowAllCompatibleFilter);
	}

	// --- openFileChooser Methods (Overloads) ---

	/**
	 * Opens a file chooser dialog for file selection. This is the **primary method** for open dialogs, directly accepting {@link FileChooser.ExtensionFilter} objects. It uses the
	 * provided `ownerStage` as the actual owner for the native dialog. If `ownerStage` is null, it falls back to {@link #defaultOwner}. Note: {@link #defaultOwner} must be set via
	 * {@link #setDefaultOwner(Stage)} for reliable fallback behavior. It brings the *actual* owner to front and requests focus before showing the native dialog.
	 * 
	 * @param ownerStage             The conceptual owner stage for this dialog. If null, {@link #defaultOwner} will be used as the actual owner.
	 * @param title                  The dialog title.
	 * @param initialDirectory       The initial directory.
	 * @param filters                A list of {@link FileChooser.ExtensionFilter} objects.
	 * @param addAllFilesFilter      Flag to add "All Files" filter.
	 * @param addAllCompatibleFilter Flag to add "All Compatible Files" filter.
	 * @return The selected file, or null.
	 */
	public static File openFileChooser(Stage ownerStage, String title, File initialDirectory, List<FileChooser.ExtensionFilter> filters, boolean addAllFilesFilter,
	        boolean addAllCompatibleFilter) {
		FileChooser fileChooser = new FileChooser();
		configureFileChooser(fileChooser, title, initialDirectory, filters, addAllFilesFilter, addAllCompatibleFilter);

		// Determine the actual owner for the native dialog
		Stage actualDialogOwner = (ownerStage != null) ? ownerStage : defaultOwner;

		// Log warnings if no suitable owner is found
		if (actualDialogOwner == null) {
			Log.error(
			        "WindowUtils.openFileChooser: No owner provided and defaultOwner is null. FileChooser will be shown without owner, which can cause issues (e.g., being hidden).");
		} else if (!actualDialogOwner.isShowing()) {
			Log.warn("WindowUtils.openFileChooser: Actual owner is not showing. FileChooser might not appear correctly.");
		}

		// Bring the actual owner to front and request focus before opening native dialog
		// No alwaysOnTop manipulation needed based on observed behavior.
		if (actualDialogOwner != null && actualDialogOwner.isShowing()) {
			actualDialogOwner.toFront();
			actualDialogOwner.requestFocus();
		}

		File selectedFile = null;
		selectedFile = fileChooser.showOpenDialog(actualDialogOwner); // Call with the determined actual owner

		return (selectedFile != null && selectedFile.exists()) ? selectedFile : null;
	}

	/**
	 * Opens a file chooser dialog for file selection. This is a **convenience method** that uses {@link #defaultOwner} as its conceptual owner. This delegates to
	 * `openFileChooser(Stage, String, File, List<FileChooser.ExtensionFilter>, boolean, boolean)`. Note: {@link #defaultOwner} must be set via {@link #setDefaultOwner(Stage)}.
	 *
	 * @param title                  The dialog title.
	 * @param initialDirectory       The initial directory.
	 * @param filters                A list of {@link FileChooser.ExtensionFilter} objects.
	 * @param addAllFilesFilter      Flag to add "All Files" filter.
	 * @param addAllCompatibleFilter Flag to add "All Compatible Files" filter.
	 * @return The selected file, or null.
	 */
	public static File openFileChooser(String title, File initialDirectory, List<FileChooser.ExtensionFilter> filters, boolean addAllFilesFilter, boolean addAllCompatibleFilter) {
		// Delegates to the primary method, explicitly passing defaultOwner as conceptual owner
		return openFileChooser(defaultOwner, title, initialDirectory, filters, addAllFilesFilter, addAllCompatibleFilter);
	}

	/**
	 * Opens a file chooser dialog for file selection. This is a **convenience method** that uses a conceptual owner stage and accepts a list of simple file extension strings
	 * (e.g., "txt", "pdf"). By default, "All Files" and "All Compatible" filters are added. This method delegates to `openFileChooser(Stage, String, File,
	 * List<FileChooser.ExtensionFilter>, boolean, boolean)`. Note: {@link #defaultOwner} must be set via {@link #setDefaultOwner(Stage)}.
	 *
	 * @param conceptualOwnerStage The conceptual owner stage. If null, {@link #defaultOwner} will be used.
	 * @param title                The dialog title.
	 * @param initialDirectory     The initial directory.
	 * @param simpleExtensions     A list of file extensions (e.g., "txt", "pdf").
	 * @return The selected file, or null.
	 */
	public static File openFileChooser(Stage conceptualOwnerStage, String title, File initialDirectory, List<String> simpleExtensions) {
		List<FileChooser.ExtensionFilter> filters = createFiltersFromSimpleExtensions(simpleExtensions);
		// Delegates to the primary method, passing the conceptualOwnerStage and default flags
		return openFileChooser(conceptualOwnerStage, title, initialDirectory, filters, true, true);
	}

	/**
	 * Opens a file chooser dialog for file selection. This is a **convenience method** that uses {@link #defaultOwner} as its conceptual owner and accepts a list of simple file
	 * extension strings (e.g., "txt", "pdf"). By default, "All Files" and "All Compatible" filters are added. This method delegates to `openFileChooser(String, File,
	 * List<String>)`. Note: {@link #defaultOwner} must be set via {@link #setDefaultOwner(Stage)}.
	 *
	 * @param title            The dialog title.
	 * @param initialDirectory The initial directory.
	 * @param simpleExtensions A list of file extensions (e.g., "txt", "pdf").
	 * @return The selected file, or null.
	 */
	public static File openFileChooser(String title, File initialDirectory, List<String> simpleExtensions) {
		// Delegates to the convenience method with explicit conceptualOwnerStage (which uses defaultOwner implicitly)
		return openFileChooser(defaultOwner, title, initialDirectory, simpleExtensions);
	}

	/**
	 * Opens a file chooser dialog for file selection (varargs convenience method). This uses a conceptual owner stage, but the actual native dialog will be owned by
	 * {@link #defaultOwner}. This method is a convenience for `openFileChooser(Stage, String, File, List<FileChooser.ExtensionFilter>, boolean, boolean)`. Note:
	 * {@link #defaultOwner} must be set via {@link #setDefaultOwner(Stage)}.
	 *
	 * @param conceptualOwnerStage The conceptual owner stage. If null, {@link #defaultOwner} will be used.
	 * @param title                The dialog title.
	 * @param initialDirectory     The initial directory.
	 * @param simpleExtensions     One or more simple file extension strings.
	 * @return The selected file, or null.
	 */
	public static File openFileChooser(Stage conceptualOwnerStage, String title, File initialDirectory, String... simpleExtensions) {
		// Converts varargs to List<String> and then calls the List<String> convenience method (which uses conceptualOwnerStage)
		return openFileChooser(conceptualOwnerStage, title, initialDirectory, Arrays.asList(simpleExtensions));
	}

	/**
	 * Opens a file chooser dialog for file selection (varargs convenience method). This uses {@link #defaultOwner} as its conceptual owner. This method delegates to
	 * `openFileChooser(String, File, String...)`. Note: {@link #defaultOwner} must be set via {@link #setDefaultOwner(Stage)}.
	 *
	 * @param title            The dialog title.
	 * @param initialDirectory The initial directory.
	 * @param simpleExtensions One or more simple file extension strings.
	 * @return The selected file, or null.
	 */
	public static File openFileChooser(String title, File initialDirectory, String... simpleExtensions) {
		return openFileChooser(defaultOwner, title, initialDirectory, simpleExtensions);
	}

	/**
	 * Opens a file chooser dialog for file selection (varargs convenience method). This uses a conceptual owner stage, but the actual native dialog will be owned by
	 * {@link #defaultOwner}. This method is a convenience for `openFileChooser(Stage, String, File, List<FileChooser.ExtensionFilter>, boolean, boolean)`. Note:
	 * {@link #defaultOwner} must be set via {@link #setDefaultOwner(Stage)}.
	 *
	 * @param conceptualOwnerStage The conceptual owner stage. If null, {@link #defaultOwner} will be used.
	 * @param title                The dialog title.
	 * @param initialDirectory     The initial directory.
	 * @param filters              One or more {@link FileChooser.ExtensionFilter} objects.
	 * @return The selected file, or null.
	 */
	public static File openFileChooser(Stage conceptualOwnerStage, String title, File initialDirectory, FileChooser.ExtensionFilter... filters) {
		// Delegates to the primary method, passing the conceptualOwnerStage and default flags
		return openFileChooser(conceptualOwnerStage, title, initialDirectory, Arrays.asList(filters), true, true);
	}

	/**
	 * Opens a file chooser dialog for file selection (varargs convenience method). This uses {@link #defaultOwner} as its conceptual owner. This method delegates to
	 * `openFileChooser(String, File, FileChooser.ExtensionFilter...)`. Note: {@link #defaultOwner} must be set via {@link #setDefaultOwner(Stage)}.
	 *
	 * @param title            The dialog title.
	 * @param initialDirectory The initial directory.
	 * @param filters          One or more {@link FileChooser.ExtensionFilter} objects.
	 * @return The selected file, or null.
	 */
	public static File openFileChooser(String title, File initialDirectory, FileChooser.ExtensionFilter... filters) {
		return openFileChooser(defaultOwner, title, initialDirectory, filters);
	}

	// --- saveFileChooser Methods (Overloads) ---

	/**
	 * Opens a file chooser dialog for saving a file. This is the **primary method** for save dialogs, directly accepting {@link FileChooser.ExtensionFilter} objects. It uses the
	 * provided `ownerStage` as the actual owner for the native dialog. If `ownerStage` is null, it falls back to {@link #defaultOwner}. Note: {@link #defaultOwner} must be set via
	 * {@link #setDefaultOwner(Stage)} for reliable behavior. It brings the *actual* owner to front and requests focus before showing the native dialog.
	 *
	 * @param ownerStage             The conceptual owner stage for this dialog. If null, {@link #defaultOwner} will be used as the actual owner.
	 * @param title                  The dialog title.
	 * @param initialDirectory       The initial directory.
	 * @param filters                A list of {@link FileChooser.ExtensionFilter} objects. The first extension from the first filter will be used for appending if needed.
	 * @param addAllFilesFilter      Flag to add "All Files" filter.
	 * @param addAllCompatibleFilter Flag to add "All Compatible Files" filter.
	 * @return The selected file path (as a File object), with the extension appended if necessary. Null if cancelled.
	 */
	// NEU: Parameter 'initialFileName' hinzugefügt
	public static File saveFileChooser(Stage ownerStage, String title, File initialDirectory, String initialFileName, List<FileChooser.ExtensionFilter> filters,
	        boolean addAllFilesFilter, boolean addAllCompatibleFilter) {
		FileChooser fileChooser = new FileChooser();
		configureFileChooser(fileChooser, title, initialDirectory, filters, addAllFilesFilter, addAllCompatibleFilter);

		// NEU: Initialen Dateinamen setzen, wenn vorhanden
		if (initialFileName != null && !initialFileName.isEmpty()) {
			fileChooser.setInitialFileName(initialFileName);
		}

		// Determine the actual owner for the native dialog
		Stage actualDialogOwner = (ownerStage != null) ? ownerStage : defaultOwner;

		// Log warnings if no suitable owner is found
		if (actualDialogOwner == null) {
			Log.error(
			        "WindowUtils.saveFileChooser: No owner provided and defaultOwner is null. FileChooser will be shown without owner, which can cause issues (e.g., being hidden).");
		} else if (!actualDialogOwner.isShowing()) {
			Log.warn("WindowUtils.saveFileChooser: Actual owner is not showing. FileChooser might not appear correctly.");
		}

		// Bring the actual owner to front and request focus before opening native dialog
		// No alwaysOnTop manipulation needed based on observed behavior.
		if (actualDialogOwner != null && actualDialogOwner.isShowing()) {
			actualDialogOwner.toFront();
			actualDialogOwner.requestFocus();
		}

		File selectedFile = null;
		selectedFile = fileChooser.showSaveDialog(actualDialogOwner); // Call with the determined actual owner

		if (selectedFile != null && filters != null && !filters.isEmpty()) {
			boolean hasCorrectExtension = false;
			String fileNameLower = selectedFile.getName().toLowerCase();

			List<String> allAllowedSimpleExtensions = filters.stream().flatMap(filter -> filter.getExtensions().stream())
			        .map(extPattern -> extPattern.startsWith("*.") ? extPattern.substring(2) : extPattern).distinct().toList();

			for (String simpleExt : allAllowedSimpleExtensions) {
				if (fileNameLower.endsWith("." + simpleExt.toLowerCase())) {
					hasCorrectExtension = true;
					break;
				}
			}

			FileChooser.ExtensionFilter selectedFilter = fileChooser.getSelectedExtensionFilter();
			String defaultExtToAppend = null;

			if (selectedFilter != null && !selectedFilter.getExtensions().isEmpty()) {
				String firstPattern = selectedFilter.getExtensions().get(0);
				defaultExtToAppend = firstPattern.startsWith("*.") ? firstPattern.substring(2) : firstPattern;
			} else if (!filters.isEmpty() && !filters.get(0).getExtensions().isEmpty()) {
				String firstPattern = filters.get(0).getExtensions().get(0);
				defaultExtToAppend = firstPattern.startsWith("*.") ? firstPattern.substring(2) : firstPattern;
			}

			if (!hasCorrectExtension && defaultExtToAppend != null) {
				selectedFile = new File(selectedFile.getAbsolutePath() + "." + defaultExtToAppend);
			}
		}
		return selectedFile;
	}

	/**
	 * Opens a file chooser dialog for saving a file. This is a **convenience method** that uses {@link #defaultOwner} as its conceptual owner. This delegates to
	 * `saveFileChooser(Stage, String, File, List<FileChooser.ExtensionFilter>, boolean, boolean)`. Note: {@link #defaultOwner} must be set via {@link #setDefaultOwner(Stage)}.
	 *
	 * @param title                  The dialog title.
	 * @param initialDirectory       The initial directory.
	 * @param filters                A list of {@link FileChooser.ExtensionFilter} objects. The first extension from the first filter will be used for appending if needed.
	 * @param addAllFilesFilter      Flag to add "All Files" filter.
	 * @param addAllCompatibleFilter Flag to add "All Compatible Files" filter.
	 * @return The selected file path (as a File object), with the extension appended if necessary. Null if cancelled.
	 */
	public static File saveFileChooser(String title, File initialDirectory, List<FileChooser.ExtensionFilter> filters, boolean addAllFilesFilter, boolean addAllCompatibleFilter) {
		// NEU: 'null' für initialFileName hinzugefügt
		return saveFileChooser(defaultOwner, title, initialDirectory, null, filters, addAllFilesFilter, addAllCompatibleFilter);
	}

	/**
	 * Opens a file chooser dialog for saving a file. This is a **convenience method** that uses a conceptual owner stage and accepts a list of simple file extension strings (e.g.,
	 * "txt", "pdf"). By default, "All Compatible" filters are added, but no "All Files" filter. This method delegates to `saveFileChooser(Stage, String, File,
	 * List<FileChooser.ExtensionFilter>, boolean, boolean)`. Note: {@link #defaultOwner} must be set via {@link #setDefaultOwner(Stage)}.
	 *
	 * @param conceptualOwnerStage The conceptual owner stage. If null, {@link #defaultOwner} will be used.
	 * @param title                The dialog title.
	 * @param initialDirectory     The initial directory.
	 * @param simpleExtensions     A list of file extensions (e.g., "txt", "pdf").
	 * @return The selected file path (as a File object), with the extension appended if necessary. Null if cancelled.
	 */
	// NEU: initialFileName Parameter hinzugefügt (von aufrufender Methode)
	public static File saveFileChooser(Stage conceptualOwnerStage, String title, File initialDirectory, String initialFileName, List<String> simpleExtensions) {
		List<FileChooser.ExtensionFilter> filters = createFiltersFromSimpleExtensions(simpleExtensions);
		// NEU: initialFileName übergeben
		return saveFileChooser(conceptualOwnerStage, title, initialDirectory, initialFileName, filters, false, true);
	}

	// Falls diese existiert und der Aufruf von oben nicht ausreicht:
	public static File saveFileChooser(Stage conceptualOwnerStage, String title, File initialDirectory, List<String> simpleExtensions) {
		// Dieser ist für Fälle, wo kein initialFileName gegeben ist
		return saveFileChooser(conceptualOwnerStage, title, initialDirectory, null, simpleExtensions); // NEU: null für initialFileName
	}

	/**
	 * Opens a file chooser dialog for saving a file. This is a **convenience method** that uses {@link #defaultOwner} as its conceptual owner and accepts a list of simple file
	 * extension strings (e.g., "txt", "pdf"). By default, "All Compatible" filters are added, but no "All Files" filter. This method delegates to `saveFileChooser(String, File,
	 * List<String>)`. Note: {@link #defaultOwner} must be set via {@link #setDefaultOwner(Stage)}.
	 *
	 * @param title            The dialog title.
	 * @param initialDirectory The initial directory.
	 * @param simpleExtensions A list of file extensions (e.g., "txt", "pdf").
	 * @return The selected file path (as a File object), with the extension appended if necessary. Null if cancelled.
	 */
	public static File saveFileChooser(String title, File initialDirectory, List<String> simpleExtensions) {
		return saveFileChooser(defaultOwner, title, initialDirectory, simpleExtensions); // Delegates to conceptual owner overload
	}

	/**
	 * Opens a file chooser dialog for saving a file (varargs convenience method). This uses a conceptual owner stage, but the actual native dialog will be owned by
	 * {@link #defaultOwner}. This method is a convenience for `saveFileChooser(Stage, String, File, List<FileChooser.ExtensionFilter>, boolean, boolean)`. Note:
	 * {@link #defaultOwner} must be set via {@link #setDefaultOwner(Stage)}.
	 *
	 * @param conceptualOwnerStage The conceptual owner stage. If null, {@link #defaultOwner} will be used.
	 * @param title                The dialog title.
	 * @param initialDirectory     The initial directory.
	 * @param simpleExtensions     One or more simple file extension strings.
	 * @return The selected file path (as a File object), with the extension appended if necessary. Null if cancelled.
	 */
	public static File saveFileChooser(Stage conceptualOwnerStage, String title, File initialDirectory, String... simpleExtensions) {
		return saveFileChooser(conceptualOwnerStage, title, initialDirectory, Arrays.asList(simpleExtensions)); // Delegates to List<String> overload
	}

	/**
	 * Opens a file chooser dialog for saving a file (varargs convenience method). This uses {@link #defaultOwner} as its conceptual owner. This method delegates to
	 * `saveFileChooser(String, File, String...)`. Note: {@link #defaultOwner} must be set via {@link #setDefaultOwner(Stage)}.
	 *
	 * @param title            The dialog title.
	 * @param initialDirectory The initial directory.
	 * @param simpleExtensions One or more simple file extension strings.
	 * @return The selected file path (as a File object), with the extension appended if necessary. Null if cancelled.
	 */
	public static File saveFileChooser(String title, File initialDirectory, String... simpleExtensions) {
		return saveFileChooser(defaultOwner, title, initialDirectory, simpleExtensions); // Delegates to conceptual owner overload
	}

	/**
	 * Opens a file chooser dialog for saving a file (varargs convenience method). This uses a conceptual owner stage, but the actual native dialog will be owned by
	 * {@link #defaultOwner}. This method is a convenience for `saveFileChooser(Stage, String, File, List<FileChooser.ExtensionFilter>, boolean, boolean)`. Note:
	 * {@link #defaultOwner} must be set via {@link #setDefaultOwner(Stage)}.
	 *
	 * @param conceptualOwnerStage The conceptual owner stage. If null, {@link #defaultOwner} will be used.
	 * @param title                The dialog title.
	 * @param initialDirectory     The initial directory.
	 * @param filters              One or more {@link FileChooser.ExtensionFilter} objects.
	 * @return The selected file path (as a File object), with the extension appended if necessary. Null if cancelled.
	 */
	public static File saveFileChooser(Stage conceptualOwnerStage, String title, File initialDirectory, FileChooser.ExtensionFilter... filters) {
		// KORREKTUR: 'null' für initialFileName hinzugefügt
		return saveFileChooser(conceptualOwnerStage, title, initialDirectory, null, Arrays.asList(filters), false, true); // Delegates to List<ExtensionFilter> overload
	}

	/**
	 * Opens a file chooser dialog for saving a file (varargs convenience method). This uses {@link #defaultOwner} as its conceptual owner. This method delegates to
	 * `saveFileChooser(String, File, FileChooser.ExtensionFilter...)`. Note: {@link #defaultOwner} must be set via {@link #setDefaultOwner(Stage)}.
	 *
	 * @param title            The dialog title.
	 * @param initialDirectory The initial directory.
	 * @param filters          One or more {@link FileChooser.ExtensionFilter} objects.
	 * @return The selected file path (as a File object), with the extension appended if necessary. Null if cancelled.
	 */
	public static File saveFileChooser(String title, File initialDirectory, FileChooser.ExtensionFilter... filters) {
		return saveFileChooser(defaultOwner, title, initialDirectory, filters); // Delegates to conceptual owner overload
	}

	/**
	 * Checks if the file is compatible with the supported extensions.
	 * 
	 * @param file                The file to check.
	 * @param supportedExtensions A list of supported file extensions.
	 * @return True if the file is compatible; otherwise, false.
	 */
	public static boolean isCompatibleFile(File file, List<String> supportedExtensions) {
		// Ensure the file is not null and there are supported extensions
		if (file == null || supportedExtensions == null || supportedExtensions.isEmpty()) {
			return false;
		}

		// Check if the file extension matches any of the supported extensions
		String fileName = file.getName().toLowerCase();
		return supportedExtensions.stream().anyMatch(ext -> fileName.endsWith("." + ext.toLowerCase()));
	}

	public static Optional<Pathment> showTextScanDialog(Window owner, String title, String header, String initialText) {
		// 1. Create a custom Dialog<Pathment>. This allows us to return a Pathment object.
		Dialog<Pathment> dialog = new Dialog<>();
		dialog.setTitle(title);
		dialog.setHeaderText(header);

		// Set the owner and apply initial styling/positioning from your utility method if possible.
		// This part is a simplified version of what your 'createAlert' does.
		if (owner == null)
			owner = defaultOwner;
		dialog.initOwner(owner);
		Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
		stage.setOpacity(0);
		attachStageAdjustmentHandler(stage, AdjustmentType.OWNER_TO_SCREEN_CENTER, 600, 400); // Beispielgröße
		stage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> {
			Timeline fadeIn = new Timeline(new KeyFrame(Duration.millis(100), new KeyValue(stage.opacityProperty(), 1.0)));
			fadeIn.play();
		});

		// 2. Create the main layout components.
		BorderPane borderPane = new BorderPane();
		TextArea textArea = new TextArea(initialText != null ? initialText : "");
		textArea.setPromptText(LangMap.getLangString("dialog.scan.prompt", "Paste text here..."));
		textArea.setWrapText(true);
		textArea.setPrefRowCount(10);

		ListView<Pathment> resultListView = new ListView<>();
		resultListView.setVisible(false);
		resultListView.setPlaceholder(new Label(LangMap.getLangString("dialog.scan.no_results", "No valid links found.")));

		resultListView.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
			@Override
			protected void updateItem(Pathment item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? null : item.getAddressForDisplay());
			}
		});

		VBox contentBox = new VBox(10, textArea, resultListView);
		VBox.setVgrow(resultListView, Priority.ALWAYS);
		borderPane.setCenter(contentBox);

		dialog.getDialogPane().setContent(borderPane);

		// 3. Define the dialog buttons.
		ButtonType scanButtonType = new ButtonType(LangMap.getLangString("button.scan", "Scan"), ButtonBar.ButtonData.OK_DONE);
		ButtonType selectButtonType = new ButtonType(LangMap.getLangString("button.select", "Select"), ButtonBar.ButtonData.APPLY);
		dialog.getDialogPane().getButtonTypes().addAll(scanButtonType, ButtonType.CANCEL);

		// Get the actual button nodes for manipulation.
		Node scanButtonNode = dialog.getDialogPane().lookupButton(scanButtonType);

		// The "Select" button will be enabled/disabled based on the ListView selection.
		Node selectButtonNode = dialog.getDialogPane().lookupButton(selectButtonType);
		if (selectButtonNode != null)
			selectButtonNode.setDisable(true); // Initially hide or disable

		resultListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
			Node currentSelectButton = dialog.getDialogPane().lookupButton(selectButtonType);
			if (currentSelectButton != null) {
				currentSelectButton.setDisable(newSelection == null);
			}
		});

		// 4. Define the logic for the "Scan" button.
		scanButtonNode.addEventFilter(ActionEvent.ACTION, event -> {
			event.consume(); // Prevent the dialog from closing.

			String textToScan = textArea.getText();
			List<Pathment> findings = PathmentExtractor.perform(textToScan);

			resultListView.getItems().setAll(findings);
			resultListView.setVisible(true);

			// Switch the button set from "Scan/Cancel" to "Select/Cancel".
			dialog.getDialogPane().getButtonTypes().setAll(selectButtonType, ButtonType.CANCEL);

			// After switching, the new "Select" button needs its disable state to be set.
			Node newSelectButton = dialog.getDialogPane().lookupButton(selectButtonType);
			if (newSelectButton != null) {
				newSelectButton.setDisable(resultListView.getSelectionModel().getSelectedItem() == null);
			}
		});

		// 5. Set the result converter. This is where the magic happens.
		// It converts the clicked ButtonType into the Pathment object we want to return.
		dialog.setResultConverter(dialogButton -> {
			if (dialogButton == selectButtonType) {
				// If the "Select" button was clicked, return the selected item from the list.
				return resultListView.getSelectionModel().getSelectedItem();
			}
			// For any other button (e.g., Cancel) or if the dialog is closed, return null.
			return null;
		});

		// 6. Show the dialog and wait for the result.
		// Because dialog is of type Dialog<Pathment>, showAndWait() now correctly returns Optional<Pathment>.
		Optional<Pathment> result = dialog.showAndWait();
		return result;
	}

	/**
	 * Displays a specialized 'About' dialog. It separates the main content (legal text, version info) from the prominent license status display, using the robust, centralized
	 * Alert setup.
	 *
	 * @param owner         The parent window.
	 * @param appTitle      The application title (for window bar).
	 * @param appHeader     The primary header (e.g., Copyright).
	 * @param mainContent   The main textual content (e.g., version, author, homepage).
	 * @param licenseStatus The prominent, final status string (e.g., "Status: LICENSED").
	 */
	public static void showAboutDialog(Window owner, String appTitle, String appVersionHeader, String copyrightText, String mainContent, String licenseStatus) {
		// 1. Create the base alert, which handles positioning, flicker reduction, and alwaysOnTop.
		// Wir verwenden appVersionHeader als Alert Header, damit "Webmarks v1.0" groß angezeigt wird.
		Alert alert = createDefaultAlert(AlertType.INFORMATION, owner, appTitle, appVersionHeader, "", true);

		// 2. Create the content container.
		// NOTE: createAlertInternal already sets a VBox as Content, wir greifen darauf zu.
		Node contentNode = alert.getDialogPane().getContent();
		VBox contentVBox;

		if (contentNode instanceof VBox) {
			contentVBox = (VBox) contentNode;
			contentVBox.getChildren().clear(); // Den initialen, leeren Label-Platzhalter entfernen
		} else {
			// Sollte nicht passieren, aber sicherer Fallback
			contentVBox = new VBox(10);
			alert.getDialogPane().setContent(contentVBox);
		}

		// --- 3. FÜGE DAS COPYRIGHT-LABEL HINZU (Prominenter Subheader) ---
		Label copyrightLabel = new Label(copyrightText);
		copyrightLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 1.1em; -fx-padding: 0 0 5px 0;");

		contentVBox.getChildren().add(copyrightLabel);
		// -----------------------------------------------------------------

		// 4. Add the main, scrolling content (long legal/info text)
		TextArea contentArea = new TextArea(mainContent); // <- Enthält jetzt den juristischen Text und Kontaktdaten
		contentArea.setEditable(false);
		contentArea.setWrapText(true);
		contentArea.setPrefRowCount(10);
		contentArea.setMinHeight(260);
		contentArea.setMaxWidth(Double.MAX_VALUE);
		VBox.setVgrow(contentArea, Priority.ALWAYS);
		contentVBox.getChildren().add(contentArea);

		// 5. Add the prominent, non-editable license status label (Footer)
		Label statusLabel = new Label(licenseStatus);
		statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 1.0em;");
		statusLabel.setPadding(new Insets(10, 0, 0, 0));
		contentVBox.getChildren().add(statusLabel);

		// 6. Final UI adjustments (Match the requested size/look)
		contentVBox.setSpacing(10);
		contentVBox.setPadding(new Insets(10)); // Setze Padding für die VBox selbst

		alert.getDialogPane().setPrefSize(480, 480);
		alert.getDialogPane().setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

		alert.getButtonTypes().setAll(ButtonType.OK);
		alert.showAndWait();
	}

	/**
	 * Attaches a stage adjustment handler based on the specified adjustment type.
	 * 
	 * @param stage          the stage to adjust
	 * @param adjustmentType the type of adjustment to apply
	 */
	public static void attachStageAdjustmentHandler(Stage stage, AdjustmentType adjustmentType, double width, double height) {
		// Null checks for parameters
		if (stage == null) {
			throw new IllegalArgumentException("Stage cannot be null");
		}
		if (adjustmentType == null) {
			throw new IllegalArgumentException("AdjustmentType cannot be null");
		}

		processAdjustment(adjustmentType, stage, width, height);
		// Define the event handler to adjust the stage position when shown
		EventHandler<WindowEvent> handler = ignore -> {
			processAdjustment(adjustmentType, stage, width, height);
		};

		// Attach the event handler to the WINDOW_SHOWN event
		stage.addEventHandler(WindowEvent.WINDOW_SHOWN, handler);
	}

	public static void processAdjustment(AdjustmentType adj, Stage stage, double width, double height) {
		switch (adj) {
		case OWNER_TO_SCREEN_CENTER:
			adjustStagePositionBetweenScreenCenterAndOwner(stage, width, height);
			correctStagePositionWithinBounds(stage);
			break;
		case WITHIN_SCREEN_BOUNDS:
			correctStagePositionWithinBounds(stage);
			break;

		case SCREEN_CENTER:
			centerStageOnScreen(stage, width, height);
			break;
		}
	}

	public static void attachStageOnShownPositionWithinScreenBoundsHandler(Stage stage) {
		// Define the event handler to adjust the stage position when shown
		EventHandler<WindowEvent> handler = ignore -> correctStagePositionWithinBounds(stage);
		// Attach the event handler to the WINDOW_SHOWN event
		stage.addEventHandler(WindowEvent.WINDOW_SHOWN, handler);
	}

	public static void attachStageOnShownPositionWithinScreenBoundsHandler(Stage stage, Insets insets) {
		// Define the event handler to adjust the stage position when shown
		EventHandler<WindowEvent> handler = ignore -> ensureStagePositionWithinBounds(stage, insets);
		// Attach the event handler to the WINDOW_SHOWN event
		stage.addEventHandler(WindowEvent.WINDOW_SHOWN, handler);
	}

	/**
	 * Adjusts the position of the given stage to be relative to the screen center.
	 * 
	 * @param stage     the Stage object whose position needs to be adjusted.
	 * @param isOnShown flag indicating if the stage is being shown.
	 */
	public static void adjustStagePositionBetweenScreenCenterAndOwner(Stage stage, double width, double height) {
		final Window owner = stage.getOwner();

		if (owner == null) {
			Log.warn("Window owner variable is null.");
			return;
		}
		final Scene ownerScene = owner.getScene();

		// Get the dimensions of the dialog
		double stageWidth = stage.getWidth() > 0 ? stage.getWidth() : width;
		double stageHeight = stage.getHeight() > 0 ? stage.getHeight() : height;

		// Center the stage over its owner (parent window)
		double x = owner.getX() + (ownerScene.getWidth() / 2.0) - (stageWidth / 2.0);
		double y = owner.getY() - 25 + (ownerScene.getHeight() / 2.0) - (stageHeight / 2.0);

		// Round to the nearest integer
		x = Math.round(x);
		y = Math.round(y);

		// Further adjust position to center the stage relative to the screen
		final Rectangle2D screenBounds = getScreenBounds();
		double screenCenterX = screenBounds.getMaxX() / 2;
		double screenCenterY = screenBounds.getMaxY() / 2;

		double dialogCenterX = (x + (stageWidth / 2));
		double dialogCenterY = (y + (stageHeight / 2));

		double centerXDifferenceHalf = (screenCenterX - dialogCenterX) / 2;
		double centerYDifferenceHalf = (screenCenterY - dialogCenterY) / 2;

		x = x + centerXDifferenceHalf;
		y = y + centerYDifferenceHalf;

		stage.setX(x);
		stage.setY(y);

	}

	/**
	 * Corrects the position of the given stage to ensure it remains within the screen bounds. This is necessary on Windows and Linux where app windows can extend beyond the screen
	 * on the top and right edges.
	 * 
	 * @param stage the stage to be corrected
	 */
	public static void correctStagePositionWithinBounds(Stage stage) {
		Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
		double width = stage.getWidth();
		double height = stage.getHeight();

		if (stage.getX() + width >= screenBounds.getWidth()) {
			stage.setX(screenBounds.getWidth() - width);
		}

		if (stage.getX() < 0.0D) {
			stage.setX(0.0D);
		}

		if (stage.getY() + height >= screenBounds.getHeight()) {
			stage.setY(screenBounds.getHeight() - height);
		}

		if (stage.getY() < 0.0D) {
			stage.setY(0.0D);
		}
	}

	public static void ensureStagePositionWithinBounds(Stage stage, Insets padding) {
		Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
		double width = stage.getWidth();
		double height = stage.getHeight();

		if (stage.getX() + width - padding.getRight() >= screenBounds.getWidth()) {
			stage.setX(screenBounds.getWidth() - width + padding.getRight());
		}

		if (stage.getX() + padding.getLeft() < 0.0D) {
			stage.setX(0.0D - padding.getLeft());
		}

		if (stage.getY() + height - padding.getBottom() >= screenBounds.getHeight()) {
			stage.setY(screenBounds.getHeight() - height + padding.getBottom());
		}

		if (stage.getY() + padding.getTop() < 0.0D) {
			stage.setY(0.0D - padding.getTop());
		}
	}

	/**
	 * Centers the given stage (window) on the screen. If the stage already has a valid width and height, those values are used. If not, the provided dimensions are used as
	 * fallback. If only one dimension is given, it will be applied to both the width and height, assuming a cubic size. If the stage size is still invalid, default dimensions
	 * (800x600) will be used.
	 *
	 * @param stage      The stage (window) to be centered on the screen.
	 * @param dimensions Optional dimensions for the stage (width and height). If only one dimension is provided, it will be used for both width and height, assuming a cubic size.
	 */
	public static void centerStageOnScreen(Stage stage, double... dimensions) {
		// Default to the stage's width and height if valid; otherwise, fall back to provided dimensions
		double stageWidth = (stage.getWidth() > 0) ? stage.getWidth() : (dimensions.length > 0 && dimensions[0] > 0) ? dimensions[0] : 0;

		double stageHeight = (stage.getHeight() > 0) ? stage.getHeight()
		        : (dimensions.length > 1 && dimensions[1] > 0) ? dimensions[1] : (dimensions.length == 1 && dimensions[0] > 0) ? dimensions[0] : 0;

		// Ensure valid dimensions for width and height
		if (stageWidth <= 0 || stageHeight <= 0) {
			// If both width and height are invalid, fall back to default values (e.g., 800x600)
			stageWidth = 800;
			stageHeight = 600;
		}

		// Get the primary screen's visual bounds
		Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

		// Calculate the new position to center the stage
		double x = (screenBounds.getWidth() - stageWidth) / 2.0;
		double y = (screenBounds.getHeight() - stageHeight) / 2.0;

		// Set the stage's new position
		stage.setX(x);
		stage.setY(y);
	}

}
