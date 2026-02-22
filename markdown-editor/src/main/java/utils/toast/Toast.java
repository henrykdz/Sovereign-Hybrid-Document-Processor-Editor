package utils.toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javafx.animation.Animation.Status;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PopupControl;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.PopupWindow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import utils.logging.Log;

/**
 * A comprehensive toast notification system for JavaFX applications.
 * <p>
 * <b>ARCHITECTURE OVERVIEW:</b>
 * <ol>
 * <li><b>Toast</b> (Builder Pattern) - User-facing API to create and configure toasts</li>
 * <li><b>ToastPopupHandler</b> (Singleton) - Central manager for ALL toast popups</li>
 * <li><b>ToastBar</b> (UI Component) - Visual representation with title, message, progress</li>
 * </ol>
 * 
 * <b>WORKFLOW:</b>
 * <ol>
 * <li>User creates toast: {@code ToastNotifier.build().title("Hello").show()}</li>
 * <li>ToastPopupHandler receives the toast configuration</li>
 * <li>Handler creates Popup + ToastBar UI component</li>
 * <li>Handler calculates position (smart stacking, no overlaps)</li>
 * <li>ToastBar handles countdown and user interactions</li>
 * <li>On close: Handler removes popup and repositions other toasts</li>
 * </ol>
 * 
 * <b>KEY FEATURES:</b>
 * <ul>
 * <li>Multiple simultaneous toasts per window</li>
 * <li>Smart stacking (no visual overlaps)</li>
 * <li>Progress bars for long-running operations</li>
 * <li>Rich text formatting with highlighted parts</li>
 * <li>Cancel buttons for interruptible operations</li>
 * <li>Position-aware (top/bottom/left/right/center)</li>
 * <li>Auto-hide with pause on hover</li>
 * <li>Smooth animations for show/hide</li>
 * <li>Configurable click-to-close behavior</li>
 * <li><b>Custom icons</b> - any JavaFX Node or FontAwesome icons</li>
 * <li><b>Built-in icon styles</b> - success, warning, error, info</li>
 * </ul>
 * 
 * <b>USAGE EXAMPLES:</b>
 * 
 * <pre>
 * // Simple text toast
 * ToastNotifier.build().text("Hello World").show();
 * 
 * // Custom icon with FontAwesome
 * FontIcon customIcon = new FontIcon("fas-rocket");
 * ToastNotifier.build().title("Launch").graphic(customIcon).text("Rocket launched successfully").show();
 * 
 * // Built-in icon styles (auto-sets appropriate icon)
 * ToastNotifier.build().text("Success message").showSuccess();
 * ToastNotifier.build().text("Warning message").showWarning();
 * ToastNotifier.build().text("Error message").showError();
 * ToastNotifier.build().text("Info message").showInformation();
 * 
 * // Image icon
 * ImageView imageIcon = new ImageView(new Image("/icons/status.png"));
 * ToastNotifier.build().title("Custom").graphic(imageIcon).text("With image icon").show();
 * 
 * // Toast with progress tracking
 * Toast toast = ToastNotifier.build().text("Processing...").withProgressBar().show();
 * toast.updateProgress(0.5);
 * 
 * // Toast that cannot be closed by clicking (important messages)
 * ToastNotifier.build().title("Important Notice").text("Please read this carefully").clickToClose(false) // User must use close button
 *         .show();
 * 
 * // Formatted text with highlights  
 * ToastNotifier.build().text("File '%s' saved", "document.txt").show();
 * 
 * // Positioned toast with action buttons
 * ToastNotifier.build().title("Warning").text("File not saved").position(Pos.TOP_CENTER).buttons(saveButton, cancelButton).showWarning();
 * </pre>
 */
public class Toast {

	// --- Builder-Felder ---
	private final StringProperty titleProperty = new SimpleStringProperty("");
//	private Node                      graphic;
	private List<Button>              buttons   = new LinkedList<>();
	private Pos                       position  = Pos.BOTTOM_RIGHT;
	private Duration                  hideAfter = Duration.seconds(7);
	private boolean                   hideCloseButton;
	private EventHandler<ActionEvent> onAction;
	private Window                    owner;
	private Window                    positionAnchorWindow;
	private Node                      positionAnchorNode;

	private List<String> styleClass = new LinkedList<>();

	private PopupControl associatedPopup;

	private boolean clickToClose = true; // ✅ Default: enabled

	// Default ist 0 (keine Mindestbreite)
	public double minWidth = 0;
	public double maxWidth = 440;

	// Startwert -1 bedeutet: Keine Progressbar anzeigen.
	private final DoubleProperty progressProperty    = new SimpleDoubleProperty(-1);
	final StringProperty         messageProperty     = new SimpleStringProperty("");
	final ObjectProperty<Node>   graphicProperty     = new SimpleObjectProperty<>();
	final ObjectProperty<Node>   contentNodeProperty = new SimpleObjectProperty<>();

	/**
	 * Sets the auto-hide duration for this toast and immediately starts or restarts its countdown timer. This is the controlled way to initiate or change the auto-hide behavior
	 * during a toast's lifecycle (e.g., after a progress bar completes).
	 * <p>
	 * If the toast is currently showing, its timer will be stopped, re-initialized with the new duration, and restarted. If the toast is not showing, this method will simply set
	 * the duration, and the timer will be initialized when `show()` is eventually called.
	 *
	 * @param duration The new duration for the countdown.
	 * @return This Toast instance for method chaining.
	 */
	public Toast startHideTimer(Duration duration) {
		this.hideAfter = duration; // Setze die neue Dauer im Toast
		if (associatedPopup != null && associatedPopup.isShowing()) {
			ToastPopupHandler.getInstance().replanToastTimer(this, duration);
		}
		return this;
	}

	private Toast() {
	}

	/** Startet den Builder-Prozess für eine neue Toast-Benachrichtigung. */
	public static Toast create() {
		return new Toast();
	}

	// --- Builder Methode (vorhanden, aber anpassen) ---
	public Toast title(String title) {
		this.titleProperty.set(title);
		return this;
	}

	// --- NEUE METHODEN für Runtime-Updates ---
	public void setTitle(String title) {
		if (Platform.isFxApplicationThread()) {
			this.titleProperty.set(title);
		} else {
			Platform.runLater(() -> this.titleProperty.set(title));
		}
	}

	public StringProperty titleProperty() {
		return titleProperty;
	}

	/**
	 * Setzt einen einfachen, unformatierten Text für die Nachricht.
	 * 
	 * @param text Der anzuzeigende Text.
	 */

	public Toast text(String text) {
		Label label = new Label(text);
		label.getStyleClass().add("text");
		label.setWrapText(true);
		label.setMaxWidth(this.maxWidth);
		this.contentNodeProperty.set(label);
		return this;
	}

	/**
	 * Setzt einen formatierten Text mit hervorgehobenen Teilen. Die Platzhalter '%s' im Format-String werden durch die Argumente ersetzt und fett dargestellt.
	 * 
	 * @param format          Der Format-String (z.B. "Datei '%s' wurde gespeichert.").
	 * @param highlightedArgs Die Argumente, die anstelle von '%s' eingesetzt und hervorgehoben werden.
	 */
	/**
	 * Setzt einen formatierten Text mit hervorgehobenen Teilen.
	 */
	public Toast text(String format, String... highlightedArgs) {
		// --- HIER IST DER FIX ---
		TextFlow textFlow = new TextFlow();
		textFlow.getStyleClass().add("text");
		// setMaxWidth auf TextFlow ist schwierig, der Wrap passiert in den Text-Nodes

		String[] parts = format.split("%s", -1);

		for (int i = 0; i < parts.length; i++) {
			// Normaler Text
			if (!parts[i].isEmpty()) {
				textFlow.getChildren().add(new Text(parts[i]));
			}

			// Hervorgehobenes Argument
			if (i < highlightedArgs.length) {
				Text openingBracket = new Text("[");
				openingBracket.getStyleClass().add("bracket");
				textFlow.getChildren().add(openingBracket);

				Text highlightedText = new Text(highlightedArgs[i]);
				highlightedText.getStyleClass().add("highlight");
				textFlow.getChildren().add(highlightedText);

				Text closingBracket = new Text("]");
				closingBracket.getStyleClass().add("bracket");
				textFlow.getChildren().add(closingBracket);
			}
		}

		// Setze den fertigen TextFlow als Content
		this.contentNodeProperty.set(textFlow);
		return this;
	}

	public Toast content(Node content) {
		this.contentNodeProperty.set(content);
		return this;
	}

	public void updateContent(Node newContent) {
		if (Platform.isFxApplicationThread()) {
			this.contentNodeProperty.set(newContent);
		} else {
			Platform.runLater(() -> this.contentNodeProperty.set(newContent));
		}
	}

	ObjectProperty<Node> contentNodeProperty() {
		return contentNodeProperty;
	}

	/**
	 * Setzt eine Mindestbreite für den Toast, damit er nicht schrumpft, wenn der Text kurz ist.
	 */
	public Toast withMinWidth(double width) {
		this.minWidth = width;
		return this;
	}

	public void updateText(String newText) {
		if (Platform.isFxApplicationThread()) {
			this.messageProperty.set(newText);
		} else {
			Platform.runLater(() -> this.messageProperty.set(newText));
		}
	}

	public StringProperty messageProperty() {
		return messageProperty;
	}

	public void refresh() {
		if (associatedPopup != null && associatedPopup.isShowing()) {
			Platform.runLater(() -> {
				Node root = associatedPopup.getScene().getRoot();
				if (root instanceof Region r) {
					// 1. Bestimme die "Anker-Breite":
					// Wir nehmen das Maximum aus der aktuellen Breite und der gesetzten minWidth.
					double anchorWidth = Math.max(r.getWidth(), this.minWidth);

					// 2. Fixiere die horizontale Größe, aber lasse die vertikale offen (computed)
					r.setMinWidth(anchorWidth);
					r.setPrefWidth(anchorWidth);

					r.setMinHeight(Region.USE_COMPUTED_SIZE);
					r.setPrefHeight(Region.USE_COMPUTED_SIZE);
					r.setMaxHeight(Region.USE_COMPUTED_SIZE);

					// 3. Jetzt die Stage anpassen - sie wird nur in der Höhe wachsen
					associatedPopup.sizeToScene();
				}
			});
		}
	}

	public PopupControl getAssociatedPopup() {
		return associatedPopup;
	}

	/**
	 * Aktiviert die Progressbar für diesen Toast. WICHTIG: Setzt hideAfter automatisch auf INDEFINITE, damit der Toast nicht verschwindet, während er arbeitet.
	 * 
	 * @param initialProgress Startwert (0.0 bis 1.0) oder ProgressBar.INDETERMINATE_PROGRESS (-1.0 funktioniert hier nicht direkt als Startwert für die Logik, nutzen wir 0)
	 */
	public Toast withProgressBar() {
		this.progressProperty.set(0);
		this.hideAfter = Duration.INDEFINITE; // Toast bleibt offen
		return this;
	}

	// Optional: Methode um den Toast manuell zu schließen, wenn fertig
	public void close() {
		// Wir müssen hier einen Weg finden, den Toast zu schließen.
		// Da die Toast-Instanz keine Referenz auf das Popup hat,
		// lösen wir das idealerweise über ein Event oder wir lassen den User
		// einfach ein kurzes "Success" Toast hinterherschicken.
		// Vorerst: Setze Progress auf 1.0 (voll).
		updateProgress(1.0);
	}

	public void setAssociatedPopup(PopupControl popup) {
		this.associatedPopup = popup;
	}

	public boolean isShowing() {
		return associatedPopup != null && associatedPopup.isShowing();
	}

	// Getter für die interne Verwendung in ToastBar
	DoubleProperty progressProperty() {
		return progressProperty;
	}

	/**
	 * Aktualisiert den Fortschritt des Toasts, falls er angezeigt wird.
	 * 
	 * @param value Wert zwischen 0.0 und 1.0
	 */
	public void updateProgress(double value) {
		// UI-Updates müssen im JavaFX Thread passieren
		if (Platform.isFxApplicationThread()) {
			this.progressProperty.set(value);
		} else {
			Platform.runLater(() -> this.progressProperty.set(value));
		}
	}

	// Builder-Methode
	public Toast graphic(Node graphic) {
		this.graphicProperty.set(graphic); // Nur noch Property setzen
		return this;
	}

	// Thread-safe Update-Methode
	public void updateGraphic(Node newGraphic) {
		if (Platform.isFxApplicationThread()) {
			this.graphicProperty.set(newGraphic);
		} else {
			Platform.runLater(() -> this.graphicProperty.set(newGraphic));
		}
	}

	// Getter für View
	public ObjectProperty<Node> graphicProperty() {
		return graphicProperty;
	}

	public Toast position(Pos position) {
		this.position = position;
		return this;
	}

	/**
	 * Sets an anchor window for positioning WITHOUT making it the owner. This prevents focus stealing while allowing correct positioning.
	 */
	public Toast anchorTo(Window window) {
		this.positionAnchorWindow = window;
		this.owner = null; // Wichtig: KEIN Owner setzen!
		return this;
	}

	/**
	 * Sets an anchor node for positioning (even better than window).
	 */
	public Toast anchorTo(Node node) {
		this.positionAnchorNode = node;
		this.owner = null; // Wichtig: KEIN Owner setzen!
		return this;
	}

	public Toast owner(Window owner) {
		this.owner = owner;
		// Optional: Warnung loggen dass dies Fokus-Probleme verursachen kann
		System.out.println("WARNING: Using .owner() may cause focus issues. Use .anchorTo() instead.");
		return this;
	}

	/**
	 * Configures the auto-hide duration for this toast. This method is part of the builder pattern and sets the desired duration for when the toast is initially shown or if
	 * `startHideTimer()` is called. It does **not** start or restart the countdown timer itself.
	 *
	 * @param duration The duration after which the toast should auto-hide. Use `Duration.INDEFINITE` to keep it visible indefinitely.
	 * @return This Toast instance for method chaining.
	 */
	public Toast hideAfter(Duration duration) {
		this.hideAfter = duration;
		return this;
	}

	public Toast onAction(EventHandler<ActionEvent> onAction) {
		this.onAction = onAction;
		return this;
	}

	public Toast darkStyle() {
		styleClass.add("dark");
		return this;
	}

	public Toast hideCloseButton() {
		this.hideCloseButton = true;
		return this;
	}

	public Toast buttons(Button... buttons) {
		this.buttons.addAll(Arrays.asList(buttons));
		return this;
	}

	/**
	 * Configures whether the toast can be closed by clicking anywhere on it.
	 * <p>
	 * <b>Default:</b> {@code true} (click-to-close enabled)
	 * 
	 * <b>Usage Scenarios:</b>
	 * <ul>
	 * <li>{@code true} - For informational toasts that can be quickly dismissed</li>
	 * <li>{@code false} - For important toasts that must be explicitly acknowledged, progress toasts that shouldn't be accidentally closed, or toasts with action buttons that need
	 * user interaction</li>
	 * </ul>
	 * 
	 * @param enabled {@code true} to allow closing by click, {@code false} to require using the close button or waiting for auto-hide
	 * @return this Toast instance for method chaining
	 */
	public Toast clickToClose(boolean enabled) {
		this.clickToClose = enabled;
		return this;
	}

	/**
	 * Displays the toast notification and sets its auto-hide duration. This method overrides any previously set `hideAfter` duration, including `Duration.INDEFINITE` if a progress
	 * bar was activated.
	 *
	 * @param duration The duration after which the toast should automatically hide. Use `Duration.INDEFINITE` to keep it visible until manually hidden.
	 * @return This Toast instance for method chaining.
	 */
	public Toast show(Duration duration) {
		this.hideAfter = duration; // Überschreibt die Dauer
		ToastPopupHandler.getInstance().show(this);
		return this;
	}

	// --- Show-Methoden ---
	public Toast show() {
		ToastPopupHandler.getInstance().show(this);
		return this; // Gib die Instanz zurück
	}

	public void hide() {
		if (associatedPopup != null && associatedPopup.isShowing()) {
			Platform.runLater(() -> {
				Node root = associatedPopup.getScene().getRoot();

				// Zugriff über den vollen Pfad der verschachtelten Klassen
				if (root instanceof ToastPopupHandler.ToastBar bar) {
					// Wir nutzen die doHide-Methode der ToastBar,
					// um die Animation und das Nachrücken der anderen Toasts auszulösen.
					bar.doHide(bar.hideCallback);
				} else {
					associatedPopup.hide();
				}
			});
		}
	}

	public Toast showWarning() {
		graphic(getIcon("dialog-warning.png"));
		return show(); // Gibt 'this' durch show() zurück
	}

	public Toast showInformation() {
		graphic(getIcon("dialog-information.png"));
		return show();
	}

	public Toast showError() {
		graphic(getIcon("dialog-error.png"));
		return show();
	}

	public Toast showSuccess() {
		graphic(getIcon("dialog-confirm.png"));
		return show();
	}

	private ImageView getIcon(String iconName) {
		String resourcePath = "/resources/assets/" + iconName;
		try {
			return new ImageView(new Image(getClass().getResource(resourcePath).toExternalForm()));
		} catch (Exception e) {
			System.err.println("WARNUNG: Toast-Icon konnte nicht geladen werden: " + resourcePath);
			return null;
		}
	}

	// =================================================================================
	// INTERNAL TOAST POPUP MANAGER
	// =================================================================================
	/**
	 * Central manager class that handles all toast popup display and lifecycle management.
	 * <p>
	 * <b>RESPONSIBILITIES:</b>
	 * <ul>
	 * <li>Display and hide toast popups with smooth animations</li>
	 * <li>Calculate positions to prevent overlapping of toasts</li>
	 * <li>Maintain stack of toasts per window owner and screen position</li>
	 * <li>Reposition toasts automatically when others are closed</li>
	 * <li>Handle multiple screens and window owners correctly</li>
	 * <li>Manage the complete toast lifecycle</li>
	 * </ul>
	 * 
	 * <b>INTERNAL DATA STRUCTURE:</b>
	 * 
	 * <pre>
	 * Map&lt;Object, Map&lt;Pos, List&lt;Popup&gt;&gt;&gt; ownerStacks
	 * 
	 * Structure:
	 * - Key: Window owner (Object) - identifies which window owns the toasts
	 * - Value: Map of positions for that window
	 *   - Key: Position (Pos) - screen position (BOTTOM_RIGHT, TOP_LEFT, etc.)
	 *   - Value: List of Popups - all toasts at that position, in display order
	 * </pre>
	 * 
	 * <b>EXAMPLE STACK:</b>
	 * 
	 * <pre>
	 * Primary Window →
	 *     BOTTOM_RIGHT → [Popup1, Popup2, Popup3]  (newest at end)
	 *     TOP_LEFT     → [Popup4]
	 * Dialog Window →
	 *     BOTTOM_RIGHT → [Popup5]
	 * </pre>
	 * 
	 * <b>POSITIONING LOGIC:</b>
	 * <ul>
	 * <li>New toasts are added to the end of their position list</li>
	 * <li>Existing toasts are pushed up/down to make space</li>
	 * <li>When a toast closes, all toasts above/below are repositioned</li>
	 * <li>Uses smooth animations for all position changes</li>
	 * </ul>
	 * 
	 * <b>NOTE:</b> This is a singleton class - only one instance manages all toasts application-wide.
	 */
	private static final class ToastPopupHandler {
		private static final ToastPopupHandler                  INSTANCE           = new ToastPopupHandler();
		private static final String                             FINAL_ANCHOR_Y_KEY = "finalAnchorY";
		private static final double                             PADDING            = 15;
		private static final double                             SPACING            = 15;
		private static final Object                             DESKTOP_OWNER_KEY  = new Object();
		private static Stage                                    staticDummyOwner   = null;
		private final Map<Object, Map<Pos, List<PopupControl>>> ownerStacks        = new HashMap<>();
		private final Map<PopupControl, Transition>             activeAnimations   = new HashMap<>();

		public static ToastPopupHandler getInstance() {
			return INSTANCE;
		}

		/**
		 * Reinitializes and restarts the countdown timer for an active toast. This is used when a toast, typically a progress bar, needs to switch its auto-hide duration after
		 * completing its primary task.
		 *
		 * @param toast       The Toast instance whose timer should be reset.
		 * @param newDuration The new duration for the countdown.
		 */
		public void replanToastTimer(Toast toast, Duration newDuration) {
			if (toast == null || toast.associatedPopup == null)
				return;

			ToastBar bar = (ToastBar) toast.associatedPopup.getScene().getRoot();
			if (bar != null) {
				bar.restartCountdown(newDuration); // Ruft die neue Methode in ToastBar auf
			}
		}

		public void show(Toast toast) {
			// ===== 1. Bestimme Positionierungs-Referenz (PRIORITÄT: Anchor > Owner) =====
			final Window positionReference;

			// ✅ ERSTE Priorität: Anchor Node
			if (toast.positionAnchorNode != null && toast.positionAnchorNode.getScene() != null) {
				positionReference = toast.positionAnchorNode.getScene().getWindow();
				System.out.println("DEBUG: Using positionAnchorNode for positioning");
			}
			// ✅ ZWEITE Priorität: Anchor Window
			else if (toast.positionAnchorWindow != null) {
				positionReference = toast.positionAnchorWindow;
				System.out.println("DEBUG: Using positionAnchorWindow for positioning");
			}
			// ❌ DRITTE Priorität: Deprecated Owner (nur für Kompatibilität)
			else if (toast.owner != null) {
				positionReference = toast.owner;
				System.out.println("WARNING: Using deprecated .owner() for positioning");
			}
			// ✅ Standard: Desktop
			else {
				positionReference = null; // Desktop
				System.out.println("DEBUG: Desktop positioning (no anchor/owner)");
			}

			final boolean isDesktopToast = positionReference == null;
			final Window popupOwner; // Für ToastBar Konstruktor
			final Object stackOwnerKey;

			if (staticDummyOwner == null) {
				staticDummyOwner = new Stage(StageStyle.UTILITY);
				staticDummyOwner.setOpacity(0);

				// DER ENTSCHEIDENDE FIX: Gib dem Geist einen Körper.
				// Ohne eine Scene gibt getScene() null zurück und verursacht den Absturz.
				Pane dummyRoot = new Pane();
				dummyRoot.setPrefSize(1, 1);
				staticDummyOwner.setScene(new Scene(dummyRoot));

				staticDummyOwner.setWidth(1);
				staticDummyOwner.setHeight(1);
				// show() kann im Hintergrund bleiben, bis es gebraucht wird.
				// Es ist besser, es sofort anzuzeigen, damit es bereit ist.
				staticDummyOwner.show();
				staticDummyOwner.hide(); // Verstecke es sofort wieder, aber es ist jetzt initialisiert.
				staticDummyOwner.show(); // Halte es im Hintergrund "am Leben".
			}

			if (isDesktopToast) {
				popupOwner = staticDummyOwner; // Dummy für ToastBar
				stackOwnerKey = DESKTOP_OWNER_KEY;
			} else {
				Window realOwner = positionReference;
				while (realOwner instanceof PopupWindow) {
					realOwner = ((PopupWindow) realOwner).getOwnerWindow();
				}
				popupOwner = realOwner; // Für ToastBar
				stackOwnerKey = realOwner;
			}
			final Rectangle2D calculationBounds;
			final Rectangle2D visualBounds;

			// ✅ WICHTIG: Jetzt mit positionReference statt initialOwner arbeiten
			if (isDesktopToast) {
				visualBounds = Screen.getPrimary().getVisualBounds();
				calculationBounds = visualBounds;
			} else {
				visualBounds = Screen.getScreensForRectangle(positionReference.getX(), positionReference.getY(), positionReference.getWidth(), positionReference.getHeight())
				        .stream().findFirst().orElse(Screen.getPrimary()).getVisualBounds();
				calculationBounds = new Rectangle2D(positionReference.getX(), positionReference.getY(), positionReference.getWidth(), positionReference.getHeight());
			}

			// ✅ WICHTIG: PopupControl statt Popup
			final PopupControl popup = new PopupControl();
			toast.setAssociatedPopup(popup);

			// ✅ Konfiguration
			popup.setAutoHide(false);
			popup.setHideOnEscape(false);
			popup.setAutoFix(true); // Wichtig für "always on top"

			final Pos p = toast.position;
			Runnable manualHideCallback = () -> hideAndReorder(stackOwnerKey, popup, p);

			// ToastBar erstellen
			final ToastBar toastBar = new ToastBar(toast, manualHideCallback, positionReference);

			// ✅ Scene setzen (richtig für PopupControl)
			popup.getScene().setRoot(toastBar);
			popup.getScene().setFill(Color.TRANSPARENT);

			// ✅ DEINE BESSERE GRÖSSENBERECHNUNG
			toastBar.applyCss();
			toastBar.layout();
			final double toastWidth = toastBar.prefWidth(-1);
			final double toastHeight = toastBar.prefHeight(-1);

			Point2D nativePosition = calculateNativePosition(calculationBounds, p, toastWidth, toastHeight);
			Point2D correctedPosition = correctPosition(nativePosition, toastWidth, toastHeight, visualBounds);

			// ✅ Position setzen und anzeigen
			popup.setX(correctedPosition.getX()); // Wichtig: setX() nicht setAnchorX()
			popup.setY(correctedPosition.getY()); // setY() nicht setAnchorY()
			popup.getProperties().put(FINAL_ANCHOR_Y_KEY, correctedPosition.getY());

			popup.addEventFilter(KeyEvent.ANY, event -> {
				System.out.println("DEBUG PopupControl Event: " + event.getCode());
				// WICHTIG: KEIN event.consume()!
			});

			Platform.runLater(() -> {
				// ✅ Desktop-Popup mit Dummy-Owner
				popup.show(popupOwner, correctedPosition.getX(), correctedPosition.getY());

				toastBar.doFade();
				addPopupToMap(stackOwnerKey, p, popup);
			});

		}

		private Point2D calculateNativePosition(Rectangle2D bounds, Pos p, double toastWidth, double toastHeight) {
			double x = 0, y = 0;
			switch (p.getHpos()) {
			case LEFT:
				x = bounds.getMinX() + PADDING;
				break;
			case CENTER:
				x = bounds.getMinX() + (bounds.getWidth() - toastWidth) / 2.0;
				break;
			default:
				x = bounds.getMinX() + bounds.getWidth() - toastWidth - PADDING;
				break;
			}
			switch (p.getVpos()) {
			case TOP:
				y = bounds.getMinY() + PADDING;
				break;
			case CENTER:
				y = bounds.getMinY() + (bounds.getHeight() - toastHeight) / 2.0;
				break;
			default:
				y = bounds.getMinY() + bounds.getHeight() - toastHeight - PADDING;
				break;
			}
			return new Point2D(x, y);
		}

		private Point2D correctPosition(Point2D naivePosition, double toastWidth, double toastHeight, Rectangle2D visualBounds) {
			double correctedX = naivePosition.getX();
			double correctedY = naivePosition.getY();
			if (correctedX < visualBounds.getMinX())
				correctedX = visualBounds.getMinX();
			if (correctedX + toastWidth > visualBounds.getMaxX())
				correctedX = visualBounds.getMaxX() - toastWidth;
			if (correctedY < visualBounds.getMinY())
				correctedY = visualBounds.getMinY();
			if (correctedY + toastHeight > visualBounds.getMaxY())
				correctedY = visualBounds.getMaxY() - toastHeight;
			return new Point2D(correctedX, correctedY);
		}

		private void addPopupToMap(Object stackOwnerKey, Pos p, PopupControl popup) {
			Map<Pos, List<PopupControl>> posMap = ownerStacks.computeIfAbsent(stackOwnerKey, k -> new HashMap<>());
			List<PopupControl> popups = posMap.computeIfAbsent(p, k -> new LinkedList<>());
			doPushAnimation(stackOwnerKey, p, popup);
			popups.add(popup);
		}

		// (fadeOutAndRemove wurde entfernt, da nicht mehr benötigt)

		private void hideAndReorder(Object stackOwnerKey, PopupControl popup, Pos p) {
			Scene scene = popup.getScene();
			if (scene == null || scene.getRoot() == null)
				return;

			// Root ist das ToastBar
			ToastBar bar = (ToastBar) scene.getRoot();
			final double closedToastHeight = bar.getHeight();

			bar.doHide(() -> {
				popup.hide();
				activeAnimations.remove(popup);
				if (ownerStacks.containsKey(stackOwnerKey)) {
					Map<Pos, List<PopupControl>> posMap = ownerStacks.get(stackOwnerKey);
					if (posMap.containsKey(p)) {
						int closedIndex = posMap.get(p).indexOf(popup);
						if (posMap.get(p).remove(popup) && closedIndex != -1) {
							doSimpleRepositionAnimation(stackOwnerKey, p, closedIndex, closedToastHeight);
						}
					}
				}
			});
		}

		private void doSimpleRepositionAnimation(Object stackOwnerKey, Pos p, int closedIndex, double closedToastHeight) {
			List<PopupControl> popups = ownerStacks.get(stackOwnerKey).get(p);
			if (popups == null || popups.isEmpty())
				return;
			boolean isShowFromTop = p.getVpos() == VPos.TOP || p.getVpos() == VPos.CENTER;
			double shiftAmount = closedToastHeight + SPACING;

			for (int i = 0; i < closedIndex; i++) {
				PopupControl popupToMove = popups.get(i);
				double currentY = popupToMove.getAnchorY();
				double targetY = isShowFromTop ? (currentY - shiftAmount) : (currentY + shiftAmount);
				animatePopupTo(popupToMove, targetY);
			}
		}

		private void animatePopupTo(PopupControl popup, double targetY) {
			Transition existingAnimation = activeAnimations.get(popup);
			if (existingAnimation != null)
				existingAnimation.stop();
			double currentY = popup.getAnchorY();
			CustomTransition animation = new CustomTransition(popup, currentY, targetY);
			activeAnimations.put(popup, animation);
			animation.play();
		}

		private void doPushAnimation(Object stackOwnerKey, Pos p, PopupControl newPopup) { // ✅ PopupControl
			Map<Pos, List<PopupControl>> posMap = ownerStacks.get(stackOwnerKey);
			if (posMap == null)
				return;
			List<PopupControl> existingPopups = posMap.get(p);
			if (existingPopups == null || existingPopups.isEmpty())
				return;

			boolean isShowFromTop = p.getVpos() == VPos.TOP || p.getVpos() == VPos.CENTER;

			// Höhe des neuen Popups aus der Scene root holen
			Scene scene = newPopup.getScene();
			double pushDistance = 0;
			if (scene != null && scene.getRoot() != null) {
				pushDistance = scene.getRoot().getLayoutBounds().getHeight() + SPACING;
			}

			for (PopupControl oldPopup : existingPopups) {
				double currentY = oldPopup.getY(); // ✅ getY()
				double targetY;
				Transition existingAnimation = activeAnimations.get(oldPopup);
				if (existingAnimation != null && existingAnimation.getStatus() == Status.RUNNING) {
					// ✅ TYPECAST zu CustomTransition
					targetY = ((CustomTransition) existingAnimation).getTargetY();
					existingAnimation.stop();
				} else {
					targetY = currentY;
				}
				double newTargetY = isShowFromTop ? (targetY + pushDistance) : (targetY - pushDistance);
				CustomTransition animation = new CustomTransition(oldPopup, currentY, newTargetY);
				activeAnimations.put(oldPopup, animation);
				animation.play();
			}

		}

		// =================================================================================
		// INTERNAL TOAST UI COMPONENT
		// =================================================================================
		/**
		 * The visual UI component that represents a single toast notification.
		 * <p>
		 * <b>UI STRUCTURE:</b>
		 * 
		 * <pre>
		 * ToastBar (VBox)
		 * ├── Title Grid (GridPane)
		 * │   ├── Title Label (left)
		 * │   └── Counter + Close Button (right)
		 * ├── Separator Line (optional)
		 * ├── Body Grid (GridPane)  
		 * │   ├── Icon/Graphic (left)
		 * │   └── Message Text (right) - supports TextFlow for rich formatting
		 * ├── Action Buttons (HBox, optional)
		 * └── Progress Bar (ProgressBar, optional)
		 * </pre>
		 * 
		 * <b>FEATURES:</b>
		 * <ul>
		 * <li>Auto-countdown timer with visual counter</li>
		 * <li>Hover detection - pauses countdown when mouse is over toast</li>
		 * <li>Rich text support with highlighted sections in brackets</li>
		 * <li>Progress bar binding for long operations</li>
		 * <li>Keyboard navigation between buttons</li>
		 * <li>Smooth fade in/out animations</li>
		 * </ul>
		 * 
		 * <b>COUNTDOWN BEHAVIOR:</b>
		 * <ul>
		 * <li>Shows remaining seconds in top-right corner</li>
		 * <li>Pulsing animation when seconds decrement</li>
		 * <li>Pauses when mouse hovers over toast</li>
		 * <li>Automatically closes toast when timer reaches zero</li>
		 * </ul>
		 */
		private static class ToastBar extends StackPane {
			private final DoubleProperty transition     = new SimpleDoubleProperty(0);
			private Timeline             fadeInTimeline;
			private final List<Node>     focusableNodes = new ArrayList<>();

			// --- Close Counter Variablen ---
			private Timeline  countdownTimeline;
			private int       remainingSeconds;
			private Text      counterText;
			private StackPane counterContainer;
			private boolean   isPaused         = false;
			private boolean   isAnimatingDigit = false;

			final Runnable      hideCallback;    // 'final' oder 'package-private' für den Zugriff
			private final Toast associatedToast; // NEU: Referenz zum Toast speichern

			private HBox topRightBox; // Wir müssen die Box speichern, um später den Counter einzufügen

			public ToastBar(Toast toast, Runnable hideCallback, Window ownerWindow) {
				this.associatedToast = toast; // Referenz speichern
				this.hideCallback = hideCallback; // Callback speichern

				// --- 1. Grund-Setup ---
				VBox mainContent = new VBox();
				mainContent.setFocusTraversable(false);
				mainContent.setSpacing(3);
				mainContent.setPadding(new Insets(1, 2, 5, 2));

				if (toast.minWidth > 0) {
					setMinWidth(toast.minWidth);
					setPrefWidth(toast.minWidth);
				}

				getStyleClass().add("notification-bar");
				if (toast.styleClass.contains("dark")) {
					getStyleClass().add("dark");
				}
				try {
					if (getClass().getResource("/resources/assets/notification.css") != null)
						getStylesheets().add(getClass().getResource("/resources/assets/notification.css").toExternalForm());
				} catch (Exception e) {
					/* ignore */ }

				// --- 2. ZENTRALE COUNTER INITIALISIERUNG (NUR EINMAL!) ---
				Node finalCounterNode = null;
				if (toast.hideAfter != null && !toast.hideAfter.isIndefinite() && !toast.hideAfter.lessThanOrEqualTo(Duration.ZERO)) {
					// Schritt A: Erstelle die UI-Elemente
					finalCounterNode = createCounterNodeInternal();
					finalCounterNode.setFocusTraversable(false);

					// Schritt B: Initialisiere den Timer
					initializeCountdown(toast.hideAfter);
				}

				// --- 3. Header (Title, Counter, Close-Button) ---
				GridPane titleGrid = new GridPane();
				titleGrid.setHgap(10);
				ColumnConstraints col1 = new ColumnConstraints();
				col1.setHgrow(Priority.ALWAYS);
				ColumnConstraints col2 = new ColumnConstraints();
				col2.setHgrow(Priority.NEVER);
				titleGrid.getColumnConstraints().addAll(col1, col2);

				// --- DER FIX: REAKTIVES LABEL ---
				Label titleLabel = new Label();
				titleLabel.getStyleClass().add("title");
				titleLabel.setWrapText(true);
				titleLabel.setMaxWidth(toast.maxWidth - 100);
				GridPane.setMargin(titleLabel, new Insets(1, 0, 0, 14));

				// Wir binden den Text des Labels an die Property des Toasts.
				// Sobald toast.setTitle() gerufen wird, ändert sich das Label im Popup!
				titleLabel.textProperty().bind(toast.titleProperty());

				titleGrid.add(titleLabel, 0, 0);
				// --------------------------------

				// Im Konstruktor (ca. Zeile 1145):
				this.topRightBox = new HBox(5); // Initialisierung der Instanzvariable
				this.topRightBox.setAlignment(Pos.CENTER_RIGHT);

				if (finalCounterNode != null) {
					this.topRightBox.getChildren().add(finalCounterNode);
				}

				if (!toast.hideCloseButton) {
					Button closeBtn = new Button();
					closeBtn.setFocusTraversable(false);
					closeBtn.getStyleClass().add("close-button");
					closeBtn.setMinWidth(24);
					closeBtn.setPrefWidth(24);
					closeBtn.setMinHeight(24);
					closeBtn.setPrefHeight(24);
					StackPane closeGraphic = new StackPane();
					closeGraphic.getStyleClass().add("graphic");
					closeBtn.setGraphic(closeGraphic);
					closeBtn.setOnAction(e -> {
						stopCountdown();
						hideCallback.run();
					});
					topRightBox.getChildren().add(closeBtn);
					focusableNodes.add(closeBtn);
				}
				// --- MARGIN FÜR DIE RECHTE BOX ---
				GridPane.setMargin(topRightBox, new Insets(1, 5, 0, 0));
				titleGrid.add(topRightBox, 1, 0);

				// --- In Toast.java innerhalb der Klasse ToastBar ---

				// 1. Wir definieren ein Binding, das uns sagt, ob ein Titel existiert
				BooleanBinding hasTitle = toast.titleProperty().isNotEmpty();

				// --- 3. Header (Separator Logik) ---
				// Anstatt 'if' nutzen wir jetzt Bindings
				Region separator = new Region();
				separator.getStyleClass().add("separator-line");
				GridPane.setMargin(separator, new Insets(2, 0, 0, 0));

				// Wir fügen den Separator IMMER hinzu, steuern aber Sichtbarkeit und Platzverbrauch
				separator.visibleProperty().bind(hasTitle);
				separator.managedProperty().bind(hasTitle);
				titleGrid.add(separator, 0, 1, 2, 1);

				// --- 4. Body (Dynamisches Padding) ---
				GridPane bodyGrid = new GridPane();
				bodyGrid.setMinHeight(40);
				bodyGrid.setMaxWidth(Double.MAX_VALUE);
				bodyGrid.setHgap(10);

				// REAKTIVES PADDING: Ändert sich automatisch, wenn der Titel gesetzt wird
				bodyGrid.paddingProperty().bind(Bindings.createObjectBinding(() -> {
					// Wenn Titel da -> oben 0 Padding, sonst 5
					double topPadding = hasTitle.get() ? 0 : 5;
					return new Insets(topPadding, 10, 7, 10);
				}, hasTitle));

				// Spalte 0 (Icon) soll nicht wachsen
				ColumnConstraints colIcon = new ColumnConstraints();
				colIcon.setHgrow(Priority.NEVER);

				// Spalte 1 (Content) soll allen verfügbaren Platz fressen
				ColumnConstraints colContent = new ColumnConstraints();
				colContent.setHgrow(Priority.ALWAYS);

				bodyGrid.getColumnConstraints().addAll(colIcon, colContent);

				StackPane iconContainer = new StackPane();
//			iconContainer.setStyle("-fx-background-color:black;");

				iconContainer.setAlignment(Pos.TOP_CENTER);
				iconContainer.getStyleClass().add("icon-container"); // Klasse zuweisen
				// Layout-Properties setzen (sind persistent, auch wenn er rein/raus fliegt)
				GridPane.setValignment(iconContainer, VPos.TOP);

				// Helper: Logik zum Synchronisieren des Grids
				Runnable syncIconContainer = () -> {
					Node currentGraphic = toast.graphicProperty().get();

					// Erstmal den Container leeren (altes Icon wegwerfen)
					iconContainer.getChildren().clear();

					if (currentGraphic != null) {
						// Fall A: Neues Icon vorhanden
						iconContainer.getChildren().add(currentGraphic);

						// Prüfen: Ist der Container physisch im Grid?
						if (!bodyGrid.getChildren().contains(iconContainer)) {
							bodyGrid.add(iconContainer, 0, 0);
						}
					} else {
						// Fall B: Icon ist null (wurde entfernt oder war nie da)
						// Container physisch aus dem Grid entfernen -> Spalte kollabiert -> Text rutscht nach links
						bodyGrid.getChildren().remove(iconContainer);
					}
				};

				syncIconContainer.run();
				toast.graphicProperty().addListener((obs, oldVal, newVal) -> syncIconContainer.run());

//			Node messageNode;
//			if (toast.messageFormat != null && toast.highlightedArgs != null) {
//				// TextFlow mit Fettdruck
//				TextFlow textFlow = new TextFlow();
//				textFlow.getStyleClass().add("text");
//				textFlow.setMaxHeight(Region.USE_PREF_SIZE);
//				textFlow.setMaxWidth(toast.maxWidth);
//
//				String[] parts = toast.messageFormat.split("%s", -1);
//				for (int i = 0; i < parts.length; i++) {
//					if (!parts[i].isEmpty()) {
//						textFlow.getChildren().add(new Text(parts[i]));
//					}
//					if (i < toast.highlightedArgs.length) {
//						Text openingBracket = new Text("[");
//						openingBracket.getStyleClass().add("bracket");
//						textFlow.getChildren().add(openingBracket);
//						Text highlightedText = new Text(toast.highlightedArgs[i]);
//						highlightedText.getStyleClass().add("highlight");
//						textFlow.getChildren().add(highlightedText);
//						Text closingBracket = new Text("]");
//						closingBracket.getStyleClass().add("bracket");
//						textFlow.getChildren().add(closingBracket);
//					}
//				}
//				StackPane textContainer = new StackPane(textFlow);
//				messageNode = textContainer;
//			} else {
//				// Hier wird das einfache Label erstellt
//				Label messageLabel = new Label();
//
//				// --- NEU: Binding an die Property ---
//				// Statt statisch setText aufzurufen, binden wir es:
//				messageLabel.textProperty().bind(toast.messageProperty());
//				// Falls Property leer ist, Fallback auf messageText (für Initialisierung)
//				if (toast.messageProperty().get().isEmpty() && toast.messageText != null) {
//					messageLabel.setText(toast.messageText);
//				}
//				// ------------------------------------
//
//				messageLabel.getStyleClass().add("text");
//				messageLabel.setWrapText(true);
//				messageLabel.setMaxWidth(toast.maxWidth);
//				messageNode = messageLabel;
//			}
//
//			messageNode.setTranslateY(-4.0);
//			bodyGrid.add(messageNode, 1, 0);
//			GridPane.setVgrow(messageNode, Priority.ALWAYS);

				// --- Dynamischer Content Bereich ---
				StackPane contentContainer = new StackPane();
				contentContainer.setMaxWidth(Double.MAX_VALUE);
				contentContainer.setAlignment(Pos.TOP_LEFT);
				if (toast.contentNodeProperty().get() != null) {
					contentContainer.getChildren().add(toast.contentNodeProperty().get());
				}
				toast.contentNodeProperty().addListener((obs, oldVal, newVal) -> {
					contentContainer.getChildren().clear();
					if (newVal != null) {
						contentContainer.getChildren().add(newVal);
					}
				});
				GridPane.setValignment(contentContainer, VPos.TOP);
				bodyGrid.add(contentContainer, 1, 0);
				GridPane.setVgrow(contentContainer, Priority.ALWAYS);

				// --- 5. Action Bar & Progress Bar ---
//				HBox actionButtonBar = new HBox(5);
//				actionButtonBar.setAlignment(Pos.CENTER_RIGHT);
//				actionButtonBar.setPadding(new Insets(0, 10, 5, 10));
//				if (toast.buttons != null && !toast.buttons.isEmpty()) {
//					actionButtonBar.getChildren().addAll(toast.buttons);
//					focusableNodes.addAll(toast.buttons);
//				} else {
//					actionButtonBar.setManaged(false);
//					actionButtonBar.setVisible(false);
//				}

				// --- 5. Action Bar & Progress Bar ---
				HBox actionButtonBar = new HBox(5);
				actionButtonBar.setAlignment(Pos.CENTER_RIGHT);
				actionButtonBar.setPadding(new Insets(0, 10, 5, 10));

				// REAKTIVE LOGIK: Zeige die Bar nur, wenn Buttons da sind
				actionButtonBar.visibleProperty().bind(Bindings.isNotEmpty(actionButtonBar.getChildren()));
				actionButtonBar.managedProperty().bind(actionButtonBar.visibleProperty());

				// Initial befüllen
				if (toast.buttons != null && !toast.buttons.isEmpty()) {
					actionButtonBar.getChildren().addAll(toast.buttons);
				}

				// === PROGRESS BAR ===
				ProgressBar pb = null;
				if (toast.progressProperty().get() > -1.0) {
					pb = new ProgressBar();
					final ProgressBar finalPb = pb;
					finalPb.getStyleClass().add("progress-bar");
					finalPb.progressProperty().bind(toast.progressProperty());
					finalPb.setMaxWidth(Double.MAX_VALUE);
					PseudoClass COMPLETED_PSEUDO_CLASS = PseudoClass.getPseudoClass("completed");
					finalPb.progressProperty().addListener((obs, oldVal, newVal) -> {
						finalPb.pseudoClassStateChanged(COMPLETED_PSEUDO_CLASS, newVal.doubleValue() >= 0.999);
					});
				}

				// --- 6. Final Assembly ---
				BooleanBinding showHeader = toast.titleProperty().isNotEmpty()
				        .or(new javafx.beans.property.SimpleBooleanProperty(finalCounterNode != null || !toast.hideCloseButton));

				// Hier werden die Nodes zum ERSTEN (und einzigen) Mal hinzugefügt:
				mainContent.getChildren().add(titleGrid); // <--- Hier knallte es, weil oben schon ge-add-et wurde!

				titleGrid.visibleProperty().bind(showHeader);
				titleGrid.managedProperty().bind(showHeader);

				mainContent.getChildren().add(bodyGrid);
				mainContent.getChildren().add(actionButtonBar);
				// -----------------------------------------------------

				getChildren().add(mainContent);
				StackPane.setAlignment(mainContent, Pos.TOP_CENTER);
				if (pb != null) {
					getChildren().add(pb);
					StackPane.setAlignment(pb, Pos.BOTTOM_CENTER);
				}

				// --- 7. Event Handlers --
				setOnMouseClicked(e -> {
					if (!toast.clickToClose)
						return;
					if (!(e.getTarget() instanceof Button)) {
						if (toast.onAction != null)
							toast.onAction.handle(new ActionEvent());
						stopCountdown();
						this.hideCallback.run();
					}
				});

				setOnMouseEntered(e -> {
					if (associatedToast.hideAfter != Duration.INDEFINITE) {
						isPaused = true;
						if (counterText != null)
							counterText.setText("⏸");
						if (counterContainer != null)
							counterContainer.pseudoClassStateChanged(PseudoClass.getPseudoClass("paused"), true);
					}
				});
				setOnMouseExited(e -> {
					if (isPaused) {
						isPaused = false;
						if (counterText != null)
							counterText.setText(String.valueOf(remainingSeconds));
						if (counterContainer != null)
							counterContainer.pseudoClassStateChanged(PseudoClass.getPseudoClass("paused"), false);
					}
				});

				this.setOnKeyPressed(event -> {
					if (!isFocused())
						return;
					if (event.getCode() == KeyCode.ESCAPE) {
						stopCountdown();
						this.hideCallback.run();
						event.consume();
					}
				});

				opacityProperty().bind(transition);

				enforceStableLayout();
			}

			private Node createCounterNodeInternal() {
				// Berechne initiale Sekunden
				this.remainingSeconds = (int) Math.ceil(associatedToast.hideAfter.toSeconds());

				counterContainer = new StackPane();
				counterContainer.getStyleClass().add("toast-counter-box");

				counterText = new Text(String.valueOf(remainingSeconds));
				counterText.getStyleClass().add("toast-counter-text");

				counterContainer.getChildren().add(counterText);
				return counterContainer;
			}

			/**
			 * Initializes the internal countdown timer with the specified duration. This method creates and configures the `countdownTimeline` and calculates the initial
			 * `remainingSeconds`. It does **not** start the timer.
			 *
			 * @param duration The duration for the countdown.
			 */
			private void initializeCountdown(Duration duration) {
				this.remainingSeconds = (int) Math.ceil(duration.toSeconds());

				// --- DIE ENTSCHEIDENDE KORREKTUR ---
				if (this.counterText == null) {
					// Wir kommen von einem zeitlosen Toast (ProgressBar) und brauchen jetzt die UI!
					Log.info("Search Scrape finished: Adding counter UI elements dynamically.");
					Node newCounterNode = createCounterNodeInternal();

					if (this.topRightBox != null) {
						// Füge den Counter an der ersten Stelle ein (vor dem Close-Button)
						this.topRightBox.getChildren().add(0, newCounterNode);
					}
				}
				// -----------------------------------

				// Jetzt ist counterText garantiert nicht mehr null.
				countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
					if (isPaused || isAnimatingDigit)
						return;
					remainingSeconds--;
					if (remainingSeconds <= 0) {
						stopCountdown();
						if (this.hideCallback != null)
							this.hideCallback.run();
					} else {
						animatePulsingDigitChange(String.valueOf(remainingSeconds));
					}
				}));
				countdownTimeline.setCycleCount(Timeline.INDEFINITE);
			}

			// --- NEUE animatePulsingDigitChange Methode ---
			private void animatePulsingDigitChange(String newText) {
				// --- WICHTIG: Hier auch prüfen, falls der Kontext zur Laufzeit zerstört wird ---
				if (counterText == null) {
					Log.warn("animatePulsingDigitChange called when counterText is null. Skipping animation.");
					isAnimatingDigit = false;
					return;
				}
				isAnimatingDigit = true;
				Timeline pulseDown = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(counterText.opacityProperty(), 1.0)),
				        new KeyFrame(Duration.millis(200), new KeyValue(counterText.opacityProperty(), 0.6, Interpolator.EASE_BOTH)));
				pulseDown.setOnFinished(e -> {
					counterText.setText(newText);
					Timeline pulseUp = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(counterText.opacityProperty(), 0.6)),
					        new KeyFrame(Duration.millis(200), new KeyValue(counterText.opacityProperty(), 1.0, Interpolator.EASE_BOTH)));
					pulseUp.setOnFinished(ev -> isAnimatingDigit = false);
					pulseUp.play();
				});
				pulseDown.play();
			}

			public void restartCountdown(Duration newDuration) {
				stopCountdown();
				initializeCountdown(newDuration);
				startCountdown();
			}

			public void startCountdown() {
				if (countdownTimeline != null)
					countdownTimeline.playFromStart();
			}

			private void stopCountdown() {
				if (countdownTimeline != null)
					countdownTimeline.stop();
			}

			public void doFade() {
				animate(true, () -> {
					if (countdownTimeline != null)
						countdownTimeline.play();
				});
			}

			public void doHide(Runnable onFinished) {
				stopCountdown();
				animate(false, onFinished);
			}

			private void animate(boolean show, Runnable onFinished) {
				if (fadeInTimeline != null && fadeInTimeline.getStatus() == Status.RUNNING)
					fadeInTimeline.stop();

				fadeInTimeline = new Timeline(new KeyFrame(Duration.millis(350), new KeyValue(transition, show ? 1.0 : 0.0, Interpolator.EASE_OUT)));
				if (onFinished != null)
					fadeInTimeline.setOnFinished(e -> onFinished.run());
				fadeInTimeline.play();
			}

			private void enforceStableLayout() {
				this.sceneProperty().addListener((obs, oldScene, newScene) -> {
					if (newScene != null) {
						newScene.addPostLayoutPulseListener(() -> {
							if (getWidth() > 0 && getHeight() > 0) {
								setMinSize(getWidth(), getHeight());
								setPrefSize(getWidth(), getHeight());
								setMaxSize(getWidth(), getHeight());
							}
						});
					}
				});
			}
		}

		private static class CustomTransition extends Transition {
			private final WeakReference<PopupControl> popupRef;
			private final double                      startY, targetY;

			CustomTransition(PopupControl popup, double startY, double targetY) {
				this.popupRef = new WeakReference<>(popup);
				this.startY = startY;
				this.targetY = targetY;
				setCycleDuration(Duration.millis(350));
				setInterpolator(Interpolator.EASE_OUT);
			}

			@Override
			protected void interpolate(double frac) {
				PopupControl popup = popupRef.get();
				if (popup != null)
					popup.setY(startY + (targetY - startY) * frac);
			}

			// ✅ DIESE METHODE FEHLTE
			public double getTargetY() {
				return targetY;
			}
		}
	}
}