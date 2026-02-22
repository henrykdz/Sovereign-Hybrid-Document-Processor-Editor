package utils.detection;


import java.util.Objects;
import java.util.Set;

import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;

// Or a UI-related package

import javafx.scene.Node;
import javafx.scene.layout.Border;
import javafx.scene.layout.Pane;
import utils.logging.Log;
import utils.ui.AnimationUtil;
import utils.ui.StyleManager;

/**
 * Interface for providing visual feedback during drag-and-drop operations.
 */
public interface DragDropVisualFeedback {

	/** Attaches necessary listeners to the scene and target node. */
	void attach(Node targetNode); // Changed from Scene to target Node for more local feedback

	/** Detaches listeners when feedback is no longer needed. */
	void detach();

	/** Shows feedback, typically called on DRAG_ENTERED/DRAG_OVER if content is compatible. */
	void showFeedback();

	/** Hides feedback, typically called on DRAG_EXITED/DRAG_DROPPED. */
	void hideFeedback();

	/** Factory method to create a simple highlighting feedback handler. */
	static DragDropVisualFeedback createHighlightFeedback(Node targetNode) {
		return new SimpleHighlightFeedback(targetNode);
	}

	// Factory method now clearly requires Pane
	static DragDropVisualFeedback createAnimationFeedback(Pane targetPane) {
		return new AnimatedBorderFeedback(targetPane);
	}
}

// --- Example Implementations (can be separate files or inner classes) ---

/**
 * Simple highlighting feedback using StyleManager. This implementation relies on an external controller (like LinkCreatorDragboard) to call showFeedback() and hideFeedback() at
 * the appropriate times. It does NOT install its own drag exit event handlers.
 */
class SimpleHighlightFeedback implements DragDropVisualFeedback {
//	private final Node         targetNode;  // Keep targetNode if StyleManager needs it
	private final StyleManager styleManager;
	// REMOVE: exitTargetHandler, exitSceneHandler, sceneChangeListener fields

	public SimpleHighlightFeedback(Node targetNode) {
//		this.targetNode = Objects.requireNonNull(targetNode, "Target Node cannot be null");
		this.styleManager = new StyleManager(targetNode);
		Log.info("SimpleHighlightFeedback created for node: " + targetNode); // Log target node
	}

	@Override
	public void attach(Node ignored) { // Parameter might be ignored if targetNode is set in constructor
		Log.fine("Attaching SimpleHighlightFeedback (no custom handlers installed by feedback impl).");
		// REMOVE: Handler registration logic for exitTargetHandler, exitSceneHandler
		// REMOVE: sceneChangeListener registration logic
	}

	@Override
	public void detach() {
		Log.fine("Detaching SimpleHighlightFeedback.");
		// REMOVE: Handler removal logic for exitTargetHandler, exitSceneHandler
		// REMOVE: sceneChangeListener removal logic

		// Ensure feedback is off on detach, even if the controlling logic missed it somehow.
		// This call is safe as hideFeedback() itself should be idempotent or cheap if already hidden.
		hideFeedback();
	}

	@Override
	public void showFeedback() {
		Log.info("<<<< SimpleHighlightFeedback.showFeedback() CALLED >>>>"); // ADD THIS LOG
		if (styleManager != null) {
			Log.fine("SimpleHighlightFeedback: Applying highlight border.");
			styleManager.highlightBorder();
		} else {
			Log.warn("StyleManager not initialized in SimpleHighlightFeedback during showFeedback.");
		}
		// TEMPORARY TEST (See Point 4 below)
		// if (targetNode != null) {
		// targetNode.setStyle("-fx-border-color: yellow; -fx-border-width: 5px; -fx-border-style: solid;");
		// Log.info("<<<< Applied TEMPORARY yellow border directly >>>>");
		// }
	}

	@Override
	public void hideFeedback() {
		Log.info("<<<< SimpleHighlightFeedback.hideFeedback() CALLED >>>>"); // ADD THIS LOG
		if (styleManager != null) {
			Log.fine("SimpleHighlightFeedback: Restoring default border style.");
			styleManager.restoreDefaultBorderStyle();
		} else {
			Log.warn("StyleManager not initialized in SimpleHighlightFeedback during hideFeedback.");
		}
		// TEMPORARY TEST (See Point 4 below)
		// if (targetNode != null) {
		// targetNode.setStyle(null); // Or restore original style if known
		// Log.info("<<<< Removed TEMPORARY border directly >>>>");
		// }
	}
}

class AnimatedBorderFeedback implements DragDropVisualFeedback {
	private final Pane        targetPane;
	private final Timeline    borderAnimation;
	private final Border      originalBorder;
	private final Set<String> originalStyleClasses;
	// REMOVE: exitTargetHandler, exitSceneHandler, sceneChangeListener

	public AnimatedBorderFeedback(Pane targetPane) {
		this.targetPane = Objects.requireNonNull(targetPane, "Target Pane cannot be null for AnimatedBorderFeedback");
		this.originalBorder = this.targetPane.getBorder();
		this.originalStyleClasses = Set.copyOf(this.targetPane.getStyleClass()); // Good: Immutable copy
		this.borderAnimation = AnimationUtil.getBorderAnimationTimelineWithListener(this.targetPane);
		if (this.borderAnimation == null) {
			throw new IllegalArgumentException("AnimationUtil failed to create border animation for " + targetPane);
		}
		// REMOVE: Handler definitions
	}

	@Override
	public void attach(Node ignored) {
		Log.fine("Attaching AnimatedBorderFeedback (no custom handlers needed).");
		// REMOVE: Handler registration logic
		// sceneChangeListener registration logic removed
	}

	@Override
	public void detach() {
		Log.fine("Detaching AnimatedBorderFeedback.");
		// REMOVE: Handler removal logic
		// sceneChangeListener removal logic removed
		hideFeedback(); // Ensure cleanup on detach
	}

	@Override
	public void showFeedback() {
		// No change needed here, but ensure it only starts if not already running?
		// Optional check: if (borderAnimation.getStatus() != Animation.Status.RUNNING) {
		Log.fine("Playing border animation.");
		borderAnimation.play();
		// }
	}

	@Override
	public void hideFeedback() {
		// Optional check: if (borderAnimation.getStatus() == Animation.Status.RUNNING) {
		Log.fine("Stopping border animation.");
		borderAnimation.stop(); // Stop the timeline first

		// runLater is still good practice here to avoid modifying scene graph during event handling
		Platform.runLater(() -> {
			if (targetPane != null) {
				Log.fine("Restoring original border and style classes.");
				targetPane.setBorder(originalBorder);
				ObservableList<String> currentClasses = targetPane.getStyleClass();
				// Using setAll is fine and efficient
				currentClasses.setAll(originalStyleClasses);
				Log.fine("Restored style classes using setAll with: " + originalStyleClasses);
			} else {
				Log.warn("Target pane was null during deferred restoration.");
			}
		});
	}
}