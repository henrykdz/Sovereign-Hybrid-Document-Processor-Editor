package utils.ui;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.control.ToggleButton;

public final class IconButtonUtils {

	private IconButtonUtils() {
	} // Utility-Klasse

	public static FontIcon configureAsIconButton(ToggleButton button, Ikon iconCode, int size) {
		if (button == null || iconCode == null) {
			return null;
		}
		FontIcon icon = new FontIcon(iconCode);
		icon.setIconSize(size);
		button.setText("");
		button.setGraphic(icon);
		return icon;
	}

	// Weitere Icon-bezogene Hilfsmethoden könnten hier hinzugefügt werden:
	public static FontIcon createStyledIcon(Ikon iconCode, int size, String styleClass) {
		FontIcon icon = new FontIcon(iconCode);
		icon.setIconSize(size);
		if (styleClass != null) {
			icon.getStyleClass().add(styleClass);
		}
		return icon;
	}
}