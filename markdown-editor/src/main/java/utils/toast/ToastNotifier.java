package utils.toast;


import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.stage.Window;
import javafx.util.Duration;
import utils.appconfig.OptionsHandler;
import utils.appconfig.OptionsHandler.OptionKey;
import utils.logging.Log;

public class ToastNotifier {

	// --- Globale Standardwerte ---
	private static Duration globalDefaultDuration     = Duration.seconds(7);
	private static Pos      globalDefaultPosition     = Pos.BOTTOM_RIGHT;
	private static boolean  globalDefaultUseDarkStyle = false;
	private static Window   globalAnchorWindow        = null;
	private static Node     globalAnchorNode          = null;

	// --- Neu: Referenz auf den OptionsHandler ---
	private static OptionsHandler optionsHandler;

	public enum NotificationType {
		INFORMATION,
		WARNING,
		ERROR,
		SUCCESS,
		CUSTOM
	}

	/**
	 * Initializes the ToastNotifier with the application's central configuration handler. This MUST be called once at application startup.
	 * 
	 * @param handler The application's OptionsHandler instance.
	 */
	public static void initialize(OptionsHandler handler) {
		if (handler == null) {
			throw new IllegalArgumentException("OptionsHandler cannot be null for ToastNotifier initialization.");
		}
		optionsHandler = handler;
	}

	// --- Getter und Setter für globale Defaults bleiben unverändert ---
	public static Duration getGlobalDefaultDuration() {
		return globalDefaultDuration;
	}

	public static Pos getGlobalDefaultPosition() {
		return globalDefaultPosition;
	}

	public static boolean isGlobalDefaultUseDarkStyle() {
		return globalDefaultUseDarkStyle;
	}

	public static void setGlobalDefaultDuration(Duration duration) {
		globalDefaultDuration = (duration != null && !duration.lessThan(Duration.ZERO)) ? duration : Duration.seconds(7);
	}

	public static void setGlobalDefaultPosition(Pos position) {
		globalDefaultPosition = (position != null) ? position : Pos.BOTTOM_RIGHT;
	}

	public static void setGlobalDefaultUseDarkStyle(boolean useDarkStyle) {
		globalDefaultUseDarkStyle = useDarkStyle;
	}

	public static void setGlobalAnchorWindow(Window window) {
		globalAnchorWindow = window;
		globalAnchorNode = null;
	}

	public static void setGlobalAnchorNode(Node node) {
		globalAnchorNode = node;
		globalAnchorWindow = null;
	}

	// --- Einfache showXxx Methoden bleiben unverändert ---
	public static void showInfo(String title, String message) {
		build().title(title).text(message).showInformation();
	}

	public static void showWarning(String title, String message) {
		build().title(title).text(message).showWarning();
	}

	public static void showError(String title, String message) {
		build().title(title).text(message).showError();
	}

	public static void showSuccess(String title, String message) {
		build().title(title).text(message).showInformation();
	}

	/**
	 * Starts a new Toast builder, intelligently configured with the global default settings. This is the primary entry point for creating toasts.
	 * 
	 * @return A pre-configured Toast builder instance.
	 */
	public static Toast build() {
		Toast toast = Toast.create().position(globalDefaultPosition).hideAfter(globalDefaultDuration);

		if (globalDefaultUseDarkStyle) {
			toast.darkStyle();
		}

		// --- DIE FINALE, KORREKTE REPARATUR ---
		NotificationOwnerMode ownerMode = NotificationOwnerMode.DESKTOP; // Sicherer Fallback
		if (optionsHandler == null) {
			Log.error("ToastNotifier has not been initialized! Toasts will be desktop-relative only.");
		} else {
			ownerMode = NotificationOwnerMode.fromString(optionsHandler.getValueOrDefault(OptionKey.UI_NOTIFICATION_DEFAULT_OWNER_MODE));
		}

		if (ownerMode == NotificationOwnerMode.MAIN_WINDOW) {
			Node anchorNode = globalAnchorNode;
			Window anchorWindow = globalAnchorWindow;

			// Prüfe, ob der Anker existiert UND sein Fenster gerade aktiv angezeigt wird.
			// Das verhindert den Bug in modalen Dialogen.
			if (anchorNode != null && anchorNode.getScene() != null && anchorNode.getScene().getWindow() != null && anchorNode.getScene().getWindow().isShowing()) {
				toast.anchorTo(anchorNode.getScene().getWindow());
			} else if (anchorWindow != null && anchorWindow.isShowing()) {
				toast.anchorTo(anchorWindow);
			}
			// Wenn keine der Bedingungen zutrifft, wird der Toast automatisch zum sicheren Desktop-Toast.
		}
		// Wenn ownerMode == DESKTOP ist, tun wir nichts. Der Toast wird korrekt desktop-relativ sein.

		return toast;
	}

	// --- Test-Methode bleibt unverändert ---
	public static void showTestNotificationWithGlobalDefaults(String testTitle, String testMessage, NotificationType testType) {
		Toast toastBuilder = build().title(testTitle != null ? testTitle : "Test Notification").text(testMessage != null ? testMessage : "This is a test with global settings.");

		switch (testType == null ? NotificationType.INFORMATION : testType) {
		case WARNING:
			toastBuilder.showWarning();
			break;
		case ERROR:
			toastBuilder.showError();
			break;
		default:
			toastBuilder.showInformation();
			break;
		}
	}
}