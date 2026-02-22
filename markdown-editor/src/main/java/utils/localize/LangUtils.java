package utils.localize;

import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;
import javafx.util.Duration;

public final class LangUtils {

	private LangUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Binds a rich-text tooltip to a JavaFX control using a LocalizableKey. The tooltip supports line breaks (\n) and bold tags (<b>...</b>) and updates automatically when the
	 * application language changes.
	 */
	public static <T extends Control> Tooltip bindRichTooltipText(T component, LocalizableKey locKey) {
		if (component == null || locKey == null) {
			// Logik für Fehlerbehandlung hier
			return null;
		}

		// HIER ist die Verbindung: Wir rufen LangMap auf, um die Property zu bekommen.
		StringProperty langProperty = LangMap.getLangProperty(locKey);

		Tooltip tooltip = new Tooltip();
		tooltip.setMinSize(300, 100);
//		tooltip.setAnchorLocation(AnchorLocation.CONTENT_TOP_LEFT);
//		tooltip.setAutoFix(true);
		tooltip.setShowDuration(Duration.seconds(12));

		tooltip.setOnShown(e -> adjustTooltipPosition(tooltip, component));

		langProperty.addListener((obs, oldText, newText) -> {
			tooltip.setGraphic(createRichTextNode(newText));
		});

		tooltip.setGraphic(createRichTextNode(langProperty.get()));

		component.setTooltip(tooltip);

		return tooltip;
	}

	private static void adjustTooltipPosition(Tooltip tooltip, Node component) {
		Bounds bounds = component.localToScreen(component.getBoundsInLocal());
		double x = bounds.getMinX();
		double y = bounds.getMaxY();
		Window window = tooltip.getScene().getWindow();
		/*
		 * .tooltip { -fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.6), 2.0, -2.0, 2.0, 2.0); }
		 */
		// With a nice and smooth shadow on the tooltip window, we need to compensate for its width by 2 now,
		// because the internal process extends the outer stage width of the popup (transparent insets where the shadow is cast).
		// Without this compensation, it would continuously trigger mouse entered events.
		window.setX(x - 2);
		window.setY(y + 2);
	}

	private static Node createRichTextNode(String text) {
		if (text == null || text.isEmpty()) {
			return new TextFlow();
		}

		TextFlow textFlow = new TextFlow();
		textFlow.setFocusTraversable(false);

		textFlow.setMinWidth(50);
		textFlow.setMinHeight(50);
		// Die Regex ist gut, wir behalten sie bei.
		String[] parts = text.split("(?i)(?<=</?b>)|(?=</?b>|\\\\n)");

		boolean isBold = false;
		for (String part : parts) {
			if (part.equalsIgnoreCase("<b>")) {
				isBold = true;
				continue;
			} else if (part.equalsIgnoreCase("</b>")) {
				isBold = false;
				continue;
			} else if (part.equals("\\n")) {
				textFlow.getChildren().add(new Text("\n"));
				continue;
			}

			if (!part.isEmpty()) {
				Text textNode = new Text(part);

				// *** HIER IST DIE ÄNDERUNG: Klassen zuweisen statt Inline-Style ***
				textNode.getStyleClass().add("tooltip-rich-text"); // Basis-Stil für alle
				if (isBold) {
					textNode.getStyleClass().add("tooltip-rich-text-bold"); // Zusätzlicher Stil für fette Teile
				}

				textFlow.getChildren().add(textNode);
			}
		}
		return textFlow;
	}

	// Hier könnten Sie auch Ihre anderen bind...-Methoden aus LangMap einfügen.
}