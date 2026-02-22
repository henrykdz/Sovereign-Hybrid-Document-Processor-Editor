package utils.ui;


import java.util.stream.Stream;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

public class AnimationUtil {

	// --- Constants for the animated Border ---
	// CRITICAL ASSUMPTION: The default border of the target pane (from .window css class)
	// is assumed to be 1.0px wide. These values are calculated based on that assumption.
	final static double BASE_BORDER_WIDTH = 1.0;
	final static double ANIMATED_BORDER_WIDTH = 1.4;
	final static double INSET_COMPENSATION = BASE_BORDER_WIDTH - ANIMATED_BORDER_WIDTH; // Should be -0.4
	
    private AnimationUtil() {}

    /**
     * Creates a timeline animation that applies a continuously changing border to the provided pane.
     * It animates ObjectProperty<Color> instances and uses a listener to update the pane's border
     * by creating new Stop, LinearGradient, BorderStroke, and Border objects when the colors change.
     *
     * @param pane The pane to which the animated border will be applied.
     * @return A timeline object representing the animation.
     */
    public static Timeline getBorderAnimationTimelineWithListener(final Pane pane) {

        final Color[] colors = Stream.of(
                "yellow", "darkorange", "tomato", "deeppink", "blueviolet",
                "cornflowerblue", "lightseagreen", "#6fba82", "chartreuse", "crimson"
        ).map(Color::web).toArray(Color[]::new);

        // --- Properties to hold the animated colors ---
        // These will be the targets of the KeyValues in the Timeline
        ObjectProperty<Color> animatedStop0Color = new SimpleObjectProperty<>(colors[0]);
        ObjectProperty<Color> animatedStop1Color = new SimpleObjectProperty<>(colors[1 % colors.length]); // Handle short arrays

        // --- Constants for the Border ---
        final CornerRadii radii = new CornerRadii(2);
        final BorderWidths widths = new BorderWidths(ANIMATED_BORDER_WIDTH);
        final BorderStrokeStyle strokeStyle = BorderStrokeStyle.SOLID;
        final Insets insets = new Insets(INSET_COMPENSATION);
        
        // --- Listener to update the border when animated colors change ---
        ChangeListener<Color> borderUpdater = (obs, oldColor, newColor) -> {
            // Get current colors from the animated properties
            Color c0 = animatedStop0Color.get();
            Color c1 = animatedStop1Color.get();

            // *** Create NEW Stops, Gradient, Stroke, and Border ***
            Stop stop0 = new Stop(0, c0);
            Stop stop1 = new Stop(1, c1);
            // Use NO_CYCLE or REFLECT based on desired visual effect between stops
            LinearGradient gradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stop0, stop1);
            BorderStroke stroke = new BorderStroke(gradient, strokeStyle, radii, widths, insets);

            Border newBorder = new Border(stroke);

            // Apply the newly created border
            pane.setBorder(newBorder);
        };

        // Attach the listener to both animated color properties
        animatedStop0Color.addListener(borderUpdater);
        animatedStop1Color.addListener(borderUpdater);

        // --- Set Initial Border ---
        // Trigger the listener once manually to set the initial state
        borderUpdater.changed(null, null, null); // Parameters don't matter here

        // --- Build the Timeline to animate the color properties ---
        Timeline timeline = new Timeline();
        // Duration for each color-to-color transition segment
        Duration segmentDuration = Duration.seconds(0.5);
        Duration totalDuration = Duration.ZERO;

        for (int i = 0; i < colors.length; i++) {
            // Target colors for this segment
            Color targetColor0 = colors[(i + 1) % colors.length]; // Next color for stop 0
            Color targetColor1 = colors[(i + 2) % colors.length]; // Next color for stop 1

            // Create KeyValues targeting the *animated properties*
            KeyValue kv0 = new KeyValue(animatedStop0Color, targetColor0, Interpolator.LINEAR);
            KeyValue kv1 = new KeyValue(animatedStop1Color, targetColor1, Interpolator.LINEAR);

            // Add KeyFrame for the end of this segment
            totalDuration = totalDuration.add(segmentDuration);
            timeline.getKeyFrames().add(new KeyFrame(totalDuration, kv0, kv1));
        }

        timeline.setCycleCount(Animation.INDEFINITE);
        return timeline;
    }
}