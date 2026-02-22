package utils.ui;

import javafx.scene.paint.Color;

public class TextColorHelper {

	/**
	 * Method to determine the text color based on the background color.
	 * 
	 * @param backgroundColor The background color for which to determine the text color.
	 * @return The determined text color (either Color.BLACK or Color.WHITE).
	 */
	public static Color determineTextColor(Color backgroundColor) {
		// Calculate the brightness of the background color (luminance)
		double luminance = (0.2126 * backgroundColor.getRed() + 0.7152 * backgroundColor.getGreen() + 0.0722 * backgroundColor.getBlue());

		if (luminance > 0.5) {
			return Color.BLACK;
		} else {
			return Color.WHITE;
		}
	}

	private TextColorHelper() {
	}
}