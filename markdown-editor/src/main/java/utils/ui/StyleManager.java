package utils.ui;

import java.util.Objects;

import javafx.scene.Node;
import utils.logging.Log;

//Simplified conceptual StyleManager
public class StyleManager {
	private final Node   managedNode;
	private final String highlightClassName = "drag-highlight-border"; // Make configurable?

	public StyleManager(Node node) {
		this.managedNode = Objects.requireNonNull(node);
		// Store original styles if needed, though removing class is often sufficient
	}

	public void highlightBorder() { // Rename might be needed if not always a border
		if (!managedNode.getStyleClass().contains(highlightClassName)) {
			managedNode.getStyleClass().add(highlightClassName);
			Log.fine("Added highlight class to " + managedNode);
		}
	}

	public void restoreDefaultBorderStyle() { // Rename might be needed
		managedNode.getStyleClass().remove(highlightClassName);
		Log.fine("Removed highlight class from " + managedNode);
	}
}