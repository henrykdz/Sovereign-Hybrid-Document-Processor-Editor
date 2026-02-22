package com.flowshift.editor.ui; // Wähle dein UI-Package

import java.net.URL;

import com.flowshift.editor.UIText;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;
import utils.logging.Log;
import utils.tooltip.CustomPopupControl;

/**
 * Eine dedizierte Popup-Ansicht für die Suchfunktionalität des CodeArea Editors. Kapselt das CustomPopupControl und liefert die internen UI-Elemente (TextField, Buttons) über
 * Events an den Controller zurück.
 * 
 * Diese Klasse gewährleistet, dass die Haupt-UI (Action Bar) nicht gestaucht wird, indem sie als frei schwebendes Element implementiert ist.
 */
public class CodeAreaSearchPopup {

	// --- Static CSS Loading (Pattern replicated from CustomPopupTooltip) ---
	private static final String EMBEDDED_CSS_NAME = "/com/flowshift/editor/SearchPopup.css";

	// --- Member-Variablen ---
	private final Node               owner;
	private final CustomPopupControl popup;

	// UI-Elemente des Popups
	private final TextField searchField;
	private final Button    searchNextButton;
	private final Button    searchPreviousButton;
	private final Button    closeButton;
	private final Label     matchCountLabel;

	private EventHandler<ActionEvent> onCloseAction;
	private EventHandler<ActionEvent> onNextAction;
//	private EventHandler<ActionEvent> onPreviousAction;


	public CodeAreaSearchPopup(Button owner) {
		this.owner = owner;

		// --- 1. Instanziierung der UI-Komponenten ---
		this.searchField = new TextField();
		this.searchField.getStyleClass().add("search-field-popup");
		this.searchField.setPromptText(UIText.FIND.getTooltip());
		HBox.setHgrow(searchField, Priority.ALWAYS); 


		this.searchPreviousButton = new Button("<"); // Instanziiert
		this.searchNextButton = new Button(">");
		this.closeButton = new Button("X");
		this.matchCountLabel = new Label("0/0");
		this.matchCountLabel.setMinWidth(50);
		this.matchCountLabel.setPrefWidth(50);
		this.matchCountLabel.setMaxWidth(50);
		this.matchCountLabel.setStyle("-fx-text-fill:white; -fx-alignment: center;");

		// --- 2. UI-Zusammenbau: Erstelle die HBox mit allen Such-Elementen ---
		HBox searchBox = new HBox(5, searchField, searchPreviousButton, searchNextButton, matchCountLabel, closeButton);
		searchBox.setAlignment(Pos.CENTER_LEFT);
		searchBox.getStyleClass().add("search-popup-container");
		searchBox.setPadding(new Insets(8, 12, 8, 12));

		// --- 3. Popup-Erstellung: CustomPopupControl initialisieren ---
		// Hier wird das popup-Objekt instanziiert.
		this.popup = CustomPopupControl.createFor(owner).unmanaged().position(CustomPopupControl.Position.BELOW_ALIGN_RIGHT).content(searchBox) // HBox wird dem popup übergeben
		        .duration(Duration.INDEFINITE).fixedWidth(400.0).paddingDefault(false).styleClasses("floating-toolbar").build();


		loadPopupStylesheets();

		// --- 4. Initiales Styling und Tooltips ---
		// Prompt-Text, Button-Handler etc. bleiben wie besprochen.
		searchField.setPromptText(UIText.FIND.getTooltip());

		searchField.setOnAction(e -> {
			if (onNextAction != null)
				onNextAction.handle(new ActionEvent());
		});

		closeButton.setOnAction(e -> {
			if (onCloseAction != null) {
				onCloseAction.handle(e);
			} else {
				hide();
			}
		});

		// Setze die Handler für die Controller-Delegation
		setOnNextAction(null);
		setOnPreviousAction(null);

		// Anfangszustand der Navigationsbuttons
		updateNavigationButtons(false);
	}

	/**
	 * Lädt die spezifische CSS-Datei für dieses Popup (`SearchPopup.css`) und wendet sie auf die Szene des Popups an. Dies ist die Methode, die die Stile für die inneren Elemente
	 * des Popups bereitstellt.
	 */
	private void loadPopupStylesheets() {
		// Finde die CSS-Datei relativ zur Klasse CodeAreaSearchPopup
		URL searchPopupCssUrl = CodeAreaSearchPopup.class.getResource(EMBEDDED_CSS_NAME);

		if (popup != null && popup.getScene() != null) {
			Parent popupRoot = popup.getScene().getRoot(); // Der Root-Node des Popups (z.B. der shadowWrapper)
			if (popupRoot != null && searchPopupCssUrl != null) {
				// Füge das spezifische CSS zur Scene des Popups hinzu.
				popupRoot.getStylesheets().add(searchPopupCssUrl.toExternalForm());
				Log.fine("Successfully loaded and applied SearchPopup.css to popup's scene.");
			} else {
				Log.warn("Could not apply SearchPopup.css: Scene, Root, or URL is null. Check paths and existence. Tried CSS: %s", searchPopupCssUrl);
			}
		} else {
			Log.error("Could not access popup's scene or root to apply SearchPopup.css.");
		}
	}

	// =========================================================================
	// 1. SETTER FÜR DELEGATION (Erfüllt die Anforderung des Controllers)
	// =========================================================================

	public void setOnNextAction(EventHandler<ActionEvent> handler) {
		searchNextButton.setOnAction(handler);
		searchField.setOnAction(handler);
	}

	public void setOnPreviousAction(EventHandler<ActionEvent> handler) {
//	    this.onPreviousAction = handler; // Speichern des Handlers
	    // Und die tatsächliche Verbindung zum Button
	    searchPreviousButton.setOnAction(handler); 
	}

	public void setOnCloseAction(EventHandler<ActionEvent> handler) {
		this.onCloseAction = handler;
	}

	/**
	 * Performs a clean and graceful closure of the search popup. This method is the central point for "cleaning up" the search state, encompassing:
	 * <ol>
	 * <li>Hiding the popup window itself.</li>
	 * <li>Clearing the internal search field within the popup.</li>
	 * <li>Resetting UI elements like match counts and navigation buttons to their default state.</li>
	 * <li>Notifying the main controller to reset the search state in the editor (clearing highlights, resetting caret, etc.) via the registered onCloseAction handler.</li>
	 * </ol>
	 * This method ensures that all search-related UI elements are reset to a consistent state when the user explicitly closes the search.
	 */
	public void cleanClose() {
		// 1. Controller benachrichtigen, damit dieser die Editor-Bereinigung durchführt
		if (onCloseAction != null) {
			// Trigger the controller's action. The controller's handleCloseSearchAction will then call hide() on this popup.
			onCloseAction.handle(new ActionEvent());
		} else {
			// Fallback, falls der Handler nicht gesetzt wurde.
			hide();
		}

		// 2. Das interne Suchfeld leeren.
		searchField.clear();

		// 3. Die UI-Elemente im Popup zurücksetzen (Match-Zähler, Buttons).
		updateMatchCount(0, 0);
		updateNavigationButtons(false);
	}

	// =========================================================================
	// 2. PUBLIC API ZUR STEUERUNG DURCH DEN CONTROLLER
	// =========================================================================

	public void show() {
		// Nutzt die reposition Methode des Popups, um es korrekt anzuzeigen
		// Wir übergeben die Position des Anker-Buttons
		Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
		popup.show(owner, bounds.getMinX(), bounds.getMinY());
		// Der CustomPopupControl übernimmt die Berechnung für BELOW_ALIGN_RIGHT
	}

	public void hide() {
		popup.hide();
	}

	public boolean isShowing() {
		return popup.isShowing();
	}

	public TextField getSearchField() {
		return searchField;
	}

	public void requestFocusOnSearchField() {
		searchField.requestFocus();
	}

	public void clearSearchField() {
		searchField.clear();
	}

	// =========================================================================
	// 3. UI UPDATES VOM CONTROLLER
	// =========================================================================

	/**
	 * Aktualisiert das Label, das die Anzahl der Treffer anzeigt (z.B. "3/15").
	 */
	public void updateMatchCount(int total, int current) {
		if (total == 0) {
			matchCountLabel.setText("0/0");
			updateNavigationButtons(false);
		} else {
			// Der aktuelle Index (current) kommt als 1-basiert an (currentMatchIndex + 1)
			matchCountLabel.setText(current + "/" + total);
			updateNavigationButtons(true);
		}
	}

	/**
	 * Aktiviert/Deaktiviert die Navigationsbuttons basierend auf dem Suchergebnis.
	 */
	public void updateNavigationButtons(boolean enable) {
		searchNextButton.setDisable(!enable);
		searchPreviousButton.setDisable(!enable);
	}
}