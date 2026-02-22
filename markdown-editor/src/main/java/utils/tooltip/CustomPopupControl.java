package utils.tooltip;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PopupControl;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;
import utils.general.StringUtils;
import utils.localize.LocalizableKey;
import utils.logging.Log;

/**
 * A robust, customizable popup control with rich features.
 * 
 * <h3>Design Note - Future Refactoring:</h3> Current inheritance from {@code PopupControl} causes API limitations:
 * <ul>
 * <li>Cannot override final methods (e.g., {@code maxWidth(double)})</li>
 * <li>Inherited {@code PopupWindow/Window} methods pollute the API</li>
 * <li>False promises (inherited methods don't match our design)</li>
 * <li>No encapsulation of parent behavior</li>
 * </ul>
 * 
 * <b>Proposed Solution:</b> Switch to Composition
 * 
 * <pre>{@code
 * public class CustomPopupControl { // NO extends
 *     private final PopupControl delegate = new PopupControl();
 *     // Clean, focused API
 * }
 * }</pre>
 * 
 * <b>Benefits:</b>
 * <ul>
 * <li>✅ Complete API control - expose only intended methods</li>
 * <li>✅ No inheritance conflicts - design our own fluent API</li>
 * <li>✅ Clean separation - window management vs content presentation</li>
 * <li>✅ Future-proof - easy to adapt as JavaFX evolves</li>
 * <li>✅ Better testing - mock the delegate</li>
 * </ul>
 * 
 * <b>Migration:</b> Priority v2.0.0 | Effort: 2-3 days | Risk: Medium
 * 
 * <hr>
 * 
 * <h3>Component Structure:</h3>
 * 
 * <pre>
 * CustomPopupControl (PopupControl)
 * ├── Scene
 * │   └── shadowWrapper (StackPane)
 * │       └── layoutPane (VBox) - {@link #getLayoutPane()}
 * │           ├── titleBarContent (HBox) - optional title bar
 * │           └── contentPane (StackPane) - {@link #getContentContainer()}
 * │               ├── contentTextFlow (TextFlow) - default text
 * │               └── [custom content] - via {@link #content(Node)}
 * </pre>
 * 
 * <h3>Default Values:</h3>
 * <ul>
 * <li>Show delay: 600ms</li>
 * <li>Total lifetime: 5000ms</li>
 * <li>Animation: {@link AnimationProfile#BALANCED}</li>
 * <li>Position: {@link Position#BELOW}</li>
 * <li>Type: {@link TYPE#INFO}</li>
 * <li>Width mode: {@link WidthMode#FIT_TO_CONTENT}</li>
 * <li>Max width: 400.0</li>
 * <li>Padding: Enabled</li>
 * </ul>
 * 
 * <h3>Hover Behavior:</h3>
 * <ul>
 * <li><b>Tooltip Mode</b> (default): Hides when mouse enters popup</li>
 * <li><b>Interactive Mode</b>: Stays visible on hover</li>
 * </ul>
 * 
 * <h3>Usage Examples:</h3>
 * 
 * <b>Simple tooltip:</b>
 * 
 * <pre>{@code
 * CustomPopupControl.createFor(button).text("Info message").type(TYPE.INFO).show(button, x, y);
 * }</pre>
 * 
 * <b>Interactive popup:</b>
 * 
 * <pre>{@code
 * CustomPopupControl.createFor(textField).unmanaged().content(customNode).addStylesheet("/com/app/custom.css").build();
 * }</pre>
 * 
 * <b>Localized tooltip:</b>
 * 
 * <pre>{@code
 * CustomPopupControl.withLocalizedText(node, PrefLangKey.BUTTON_TOOLTIP).position(Position.ABOVE);
 * }</pre>
 * 
 * <b>Temporary notification:</b>
 * 
 * <pre>{@code
 * CustomPopupControl.showTemporary(owner, "Saved", TYPE.SUCCESS, Position.ABOVE, Duration.seconds(2));
 * }</pre>
 * 
 * <b>External manager usage:</b>
 * 
 * <pre>{@code
 * this.sharedTooltip = CustomPopupControl.createFor(tableView).unmanaged().position(Position.CURSOR_BOTTOM_RIGHT).build();
 * }</pre>
 * 
 * @see Builder
 * @see Position
 * @see TYPE
 * @see AnimationProfile
 * @see WidthMode
 */
public class CustomPopupControl extends PopupControl {
	// --- Static CSS Loading (Pattern replicated from CustomPopupTooltip) ---
	private static final String EMBEDDED_CSS_NAME = "CustomPopupControl.css";
	private static URL          embeddedCssUrl;
	private static boolean      cssLoadAttempted  = false;

	/**
	 * Gets the URL for the embedded stylesheet, loading it only once per application run. This static method provides a robust and efficient way to access the CSS resource
	 * associated with this component, preventing redundant file lookups. It assumes the CSS file resides in the same package as the Java class.
	 *
	 * @return The URL of the CSS resource, or null if it could not be found.
	 */
	private static URL getCssUrl() {
		if (!cssLoadAttempted) {
			try {
				// ARCHITEKTURISCHE HEILUNG: Direkter, relativer Pfad zum Package
				// Wenn die CSS-Datei im selben Package liegt (utils/tooltip/),
				// ist dies der zuverlässigste Weg.
				embeddedCssUrl = CustomPopupControl.class.getResource(EMBEDDED_CSS_NAME);

				if (embeddedCssUrl == null) {
					// Fallback für den Fall, dass es direkt im Wurzelverzeichnis des Classpaths liegt (unwahrscheinlich für dieses Setup)
					embeddedCssUrl = CustomPopupControl.class.getResource("/" + EMBEDDED_CSS_NAME);
				}

				if (embeddedCssUrl == null) {
					Log.error("CRITICAL ERROR: CSS not found. Tried paths relative to package 'utils.tooltip' and classpath root for: " + EMBEDDED_CSS_NAME);
				} else {
					Log.info("CSS successfully loaded from: " + embeddedCssUrl);
				}
			} catch (Exception e) {
				Log.error("CRITICAL ERROR: Failed to load CSS for " + EMBEDDED_CSS_NAME + ": " + e.getMessage());
				e.printStackTrace();
			}
			cssLoadAttempted = true;
		}
		return embeddedCssUrl;
	}

	/**
	 * Predefined animation profiles with coordinated fade-in and fade-out animations. Each profile combines specific durations and interpolators for consistent user experience.
	 * 
	 * <p>
	 * <b>Default:</b> {@link #BALANCED} (150ms fade-in, 120ms fade-out)
	 * </p>
	 * 
	 * <h3>Usage Examples:</h3>
	 * 
	 * <pre>{@code
	 * // Apply during construction
	 * CustomPopupControl popup = CustomPopupControl.createFor(node).withAnimation(AnimationProfile.SHARP).install();
	 * 
	 * // Change dynamically
	 * popup.withAnimation(AnimationProfile.GENTLE);
	 * 
	 * // In programmatic popups
	 * CustomPopupControl.buildFor(node).withType(TYPE.ERROR).withAnimation(AnimationProfile.SHARP).show(Duration.seconds(3));
	 * }</pre>
	 */
	public static enum AnimationProfile {
		/**
		 * Gentle, slow transitions (300ms each).
		 * <ul>
		 * <li><b>Fade-in:</b> 300ms, EASE_OUT</li>
		 * <li><b>Fade-out:</b> 300ms, EASE_IN</li>
		 * </ul>
		 * <p>
		 * <i>Use case:</i> Informational tooltips, subtle notifications
		 * </p>
		 */
		GENTLE(new AnimationStyle(Duration.millis(300), Interpolator.EASE_OUT), new AnimationStyle(Duration.millis(300), Interpolator.EASE_IN)),

		/**
		 * Balanced, professional transitions (150ms in, 120ms out).
		 * <ul>
		 * <li><b>Fade-in:</b> 150ms, EASE_OUT</li>
		 * <li><b>Fade-out:</b> 120ms, EASE_IN</li>
		 * </ul>
		 * <p>
		 * <b>Default profile.</b> Suitable for most use cases.
		 * </p>
		 * <p>
		 * <i>Use case:</i> General tooltips, standard notifications
		 * </p>
		 */
		BALANCED(new AnimationStyle(Duration.millis(150), Interpolator.EASE_OUT), new AnimationStyle(Duration.millis(120), Interpolator.EASE_IN)),

		/**
		 * Fast, snappy transitions (60ms each).
		 * <ul>
		 * <li><b>Fade-in:</b> 60ms, EASE_OUT</li>
		 * <li><b>Fade-out:</b> 60ms, EASE_IN</li>
		 * </ul>
		 * <p>
		 * <i>Use case:</i> Error messages, validation feedback, rapid interactions
		 * </p>
		 */
		SHARP(new AnimationStyle(Duration.millis(60), Interpolator.EASE_OUT), new AnimationStyle(Duration.millis(60), Interpolator.EASE_IN)),

		/**
		 * Playful, asymmetric transitions (250ms in, 150ms out).
		 * <ul>
		 * <li><b>Fade-in:</b> 250ms, EASE_OUT (natural deceleration)</li>
		 * <li><b>Fade-out:</b> 150ms, EASE_IN</li>
		 * </ul>
		 * <p>
		 * <i>Use case:</i> Success notifications, celebratory messages
		 * </p>
		 */
		PLAYFUL(new AnimationStyle(Duration.millis(250), Interpolator.EASE_OUT), new AnimationStyle(Duration.millis(150), Interpolator.EASE_IN)),

		/**
		 * Instant appearance/disappearance (no animation).
		 * <ul>
		 * <li><b>Fade-in:</b> 0ms, LINEAR</li>
		 * <li><b>Fade-out:</b> 0ms, LINEAR</li>
		 * </ul>
		 * <p>
		 * <i>Use case:</i> Debugging, performance-critical scenarios, disabled animations
		 * </p>
		 */
		INSTANT(new AnimationStyle(Duration.ZERO, Interpolator.LINEAR), new AnimationStyle(Duration.ZERO, Interpolator.LINEAR));

		private final AnimationStyle fadeInStyle;
		private final AnimationStyle fadeOutStyle;

		AnimationProfile(AnimationStyle fadeIn, AnimationStyle fadeOut) {
			this.fadeInStyle = fadeIn;
			this.fadeOutStyle = fadeOut;
		}

		/**
		 * Returns the fade-in animation style for this profile.
		 * 
		 * @return The fade-in {@link AnimationStyle}
		 */
		public AnimationStyle getFadeInStyle() {
			return fadeInStyle;
		}

		/**
		 * Returns the fade-out animation style for this profile.
		 * 
		 * @return The fade-out {@link AnimationStyle}
		 */
		public AnimationStyle getFadeOutStyle() {
			return fadeOutStyle;
		}
	}

	/**
	 * Configuration for a single fade animation with duration and interpolator.
	 * <p>
	 * Provides fine-grained control for custom animations beyond predefined profiles.
	 * </p>
	 * 
	 * <h3>Common Interpolators:</h3>
	 * <ul>
	 * <li>{@link Interpolator#LINEAR} - Constant speed</li>
	 * <li>{@link Interpolator#EASE_IN} - Starts slow, accelerates</li>
	 * <li>{@link Interpolator#EASE_OUT} - Starts fast, decelerates</li>
	 * <li>{@link Interpolator#EASE_BOTH} - Slow start and end</li>
	 * </ul>
	 * 
	 * <h3>Usage Examples:</h3>
	 * 
	 * <pre>{@code
	 * // Create custom animation style
	 * AnimationStyle customStyle = AnimationStyle.ofMillis(500, Interpolator.EASE_BOTH);
	 * 
	 * // Apply to popup
	 * popup.withCustomFadeIn(customStyle);
	 * 
	 * // Using factory methods
	 * AnimationStyle.instant(); // 0ms duration
	 * AnimationStyle.fast(); // 100ms duration
	 * AnimationStyle.standard(); // 200ms duration (default)
	 * AnimationStyle.slow(); // 400ms duration
	 * }</pre>
	 * 
	 * @param duration     The animation duration
	 * @param interpolator The interpolator controlling animation easing
	 */
	public static record AnimationStyle(Duration duration, Interpolator interpolator) {

		/**
		 * Creates an AnimationStyle with standard duration (200ms) and EASE_BOTH interpolator.
		 * 
		 * @return Standard animation style
		 */
		public static AnimationStyle standard() {
			return new AnimationStyle(Duration.millis(200), Interpolator.EASE_BOTH);
		}

		/**
		 * Creates an AnimationStyle with fast duration (100ms) and EASE_OUT interpolator.
		 * 
		 * @return Fast animation style
		 */
		public static AnimationStyle fast() {
			return new AnimationStyle(Duration.millis(100), Interpolator.EASE_OUT);
		}

		/**
		 * Creates an AnimationStyle with slow duration (400ms) and EASE_BOTH interpolator.
		 * 
		 * @return Slow animation style
		 */
		public static AnimationStyle slow() {
			return new AnimationStyle(Duration.millis(400), Interpolator.EASE_BOTH);
		}

		/**
		 * Creates an AnimationStyle with instant appearance (0ms duration).
		 * 
		 * @return Instant animation style
		 */
		public static AnimationStyle instant() {
			return new AnimationStyle(Duration.ZERO, Interpolator.LINEAR);
		}

		/**
		 * Creates an AnimationStyle with specified duration and EASE_BOTH interpolator.
		 * 
		 * @param duration The animation duration
		 * @return AnimationStyle with EASE_BOTH interpolator
		 */
		public static AnimationStyle of(Duration duration) {
			return new AnimationStyle(duration, Interpolator.EASE_BOTH);
		}

		/**
		 * Creates an AnimationStyle with specified duration in milliseconds and EASE_BOTH interpolator.
		 * 
		 * @param millis Duration in milliseconds
		 * @return AnimationStyle with EASE_BOTH interpolator
		 */
		public static AnimationStyle ofMillis(long millis) {
			return new AnimationStyle(Duration.millis(millis), Interpolator.EASE_BOTH);
		}

		/**
		 * Creates an AnimationStyle with specified duration and interpolator.
		 * 
		 * @param millis       Duration in milliseconds
		 * @param interpolator The interpolator to use
		 * @return Configured AnimationStyle
		 */
		public static AnimationStyle ofMillis(long millis, Interpolator interpolator) {
			return new AnimationStyle(Duration.millis(millis), interpolator);
		}

		/**
		 * Returns the duration in milliseconds.
		 * 
		 * @return Duration in milliseconds
		 */
		public long toMillis() {
			return (long) duration.toMillis();
		}
	}

	private static final double INITIAL_MAX_WIDTH = 400.0;
//	private static final double INITIAL_MAX_HEIGHT = 600.0;

	// Positioning constants
	private static final double DEFAULT_HORIZONTAL_OFFSET = 10.0;
	private static final double DEFAULT_VERTICAL_OFFSET   = 4.0;
	private static final double DEFAULT_CURSOR_OFFSET_X   = 12.0;
	private static final double DEFAULT_CURSOR_OFFSET_Y   = 16.0;
	private static final double DEFAULT_SCREEN_MARGIN     = 5.0;
	private static final double DEFAULT_TOP_COMPENSATION  = 2.0;
	private static final double DEFAULT_BOTTOM_PADDING    = 6.0;

	/**
	 * Defines the visual style types for the popup control.
	 */
	public static enum TYPE {
		/** Informational style (default) */
		INFO("type-info"),
		/** Success style */
		SUCCESS("type-success"),
		/** Warning style */
		WARNING("type-warning"),
		/** Error style */
		ERROR("type-error");

		private final String styleClass;

		TYPE(String styleClass) {
			this.styleClass = styleClass;
		}

		/**
		 * Gets the CSS style class for this type.
		 * 
		 * @return The style class string
		 */
		public String getStyleClass() {
			return styleClass;
		}
	}

	/**
	 * Defines how the popup's width should be determined.
	 */
	public static enum WidthMode {
		/**
		 * Width adjusts dynamically to fit the content. Ideal for informational tooltips and notifications where content length varies significantly.
		 */
		FIT_TO_CONTENT,

		/**
		 * Width matches the owner component's width. Creates a clean, aligned appearance for dropdown-style popups or when visual consistency with the owner is important.
		 * 
		 * <p>
		 * <b>Requirement:</b> Owner must be a {@link Region} instance.
		 */
		TRACK_OWNER
	}

	/**
	 * Defines positioning strategies for the popup relative to its owner or cursor.
	 */
	public static enum Position {
		/** Positioned above the owner component */
		ABOVE,

		/** Positioned below the owner component (default) */
		BELOW,

		/** Positioned to the left of the owner component */
		LEFT,

		/** Positioned to the right of the owner component */
		RIGHT,

		/** Positioned at bottom right of cursor */
		CURSOR_BOTTOM_RIGHT,

		/** Positioned below component aligned to right edge */
		BELOW_ALIGN_RIGHT
	}

	// === PRIMARY API (BEHALTEN!) ===

	// === ADVANCED API (optional) ===

	/**
	 * Advanced configuration with full control. Use only when the simple install() methods are insufficient.
	 */
	public static Builder createFor(Node owner) {
		return new Builder(owner);
	}

	/**
	 * Convenience shortcut for quick tooltips with localization. Equivalent to: {@code createFor(owner).withKey(key).install()}
	 * 
	 * @param owner The owner node
	 * @param key   LocalizableKey for text content
	 * @return Configured and installed CustomPopupControl
	 */
	public static CustomPopupControl withLocalizedText(Node owner, LocalizableKey key) {
		return createFor(owner).key(key).build();
	}

	// === TEMPORARY POPUP API ===

	/**
	 * Quick API for temporary notifications. Creates, shows, and auto-hides a popup.
	 */
	public static void showTemporary(Node owner, String text, TYPE type, Position position, Duration duration) {
		TemporaryPopup.forNode(owner).withText(text).withType(type).withPosition(position).show(duration);
	}

	// === DEPRECATE THE OLD CONFUSING METHODS ===

	// =========================================================================
	// === FACTORY METHODS (FINALE, VOLLSTÄNDIGE VERSION) =====================
	// =========================================================================
	/**
	 * A fluent Builder for creating and configuring persistent CustomPopupControl instances.
	 */
	public static class Builder {
		// --- Required ---
		private final Node owner;

		private List<String> externalStylesheets = new ArrayList<>(); // NEU

		// --- Content ---
		private LocalizableKey key           = null;
		private String         text          = null;
		private String         title         = null;
		private Node           titleIcon     = null;
		private Node           customContent = null;

		// --- Timing ---
		private Duration showDelay     = Duration.millis(600);
		private Duration totalLifetime = Duration.millis(5000);

		// --- Behavior ---
		private boolean hideOnPopupHover = true;
		private boolean autoBehavior     = true;
//		private boolean interactive      = false;
//		private boolean   autoHide     = true;
//		private boolean   hideOnEscape = true;
		private Position  position  = Position.BELOW;
		private WidthMode widthMode = WidthMode.FIT_TO_CONTENT;

		// --- Dimensions ---
		private Double contentMaxWidth    = null;
		private Double contentMaxHeight   = null;
		private Double contentPrefWidth   = null;
		private Double contentPrefHeight  = null;
		private Double contentFixedWidth  = null;
		private Double contentFixedHeight = null;

		// --- Styling ---
		private TYPE             type             = TYPE.INFO;
		private AnimationProfile animationProfile = AnimationProfile.BALANCED;
		private boolean          paddingEnabled   = true;
		private Insets           customPadding    = null;                     // ✅ Jetzt deklariert!
		private Double           titleBarHeight   = null;
		private String           inlineStyle      = null;
		private String[]         styleClasses     = new String[0];

		/**
		 * Private constructor. Use CustomPopupControl.createFor(node).
		 * 
		 * @param owner The owner node for the popup
		 */
		private Builder(Node owner) {
			this.owner = owner;
		}

		// NEUE METHODE - nach dem gleichen Muster wie getCssUrl()
		public Builder addStylesheet(String path) {
			if (path != null && !path.trim().isEmpty()) {
				this.externalStylesheets.add(path);

				// Optional: Direkt prüfen ob die Resource existiert (wie im getCssUrl Muster)
				URL cssUrl = CustomPopupControl.class.getResource(path);
				if (cssUrl == null) {
					// Fallback mit führendem Slash
					cssUrl = CustomPopupControl.class.getResource("/" + (path.startsWith("/") ? path.substring(1) : path));
				}

				if (cssUrl == null) {
					Log.warn("External CSS not found at: " + path);
				} else {
					Log.info("External CSS found at: " + cssUrl);
				}
			}
			return this;
		}

		/**
		 * Sets custom padding values.
		 */
		public Builder padding(double top, double right, double bottom, double left) {
			this.customPadding = new Insets(top, right, bottom, left);
			return this;
		}

		// === Content Configuration ===
		public Builder text(String text) {
			this.text = text;
			this.key = null;
			this.customContent = null;
			return this;
		}

		public Builder key(LocalizableKey key) {
			this.key = key;
			this.text = null;
			this.customContent = null;
			return this;
		}

		public Builder content(Node content) {
			this.customContent = content;
			this.text = null;
			this.key = null;
			return this;
		}

		public Builder title(String title) {
			this.title = title;
			return this;
		}

		public Builder title(String title, Node icon) {
			this.title = title;
			this.titleIcon = icon;
			return this;
		}

		// === Timing Configuration ===
		public Builder showDelay(Duration showDelay) {
			this.showDelay = (showDelay != null) ? showDelay : this.showDelay;
			return this;
		}

		public Builder showDelay(long delayInMillis) {
			return showDelay(Duration.millis(delayInMillis));
		}

		public Builder duration(Duration totalLifetime) {
			this.totalLifetime = (totalLifetime != null) ? totalLifetime : this.totalLifetime;
			return this;
		}

		public Builder duration(long durationInMillis) {
			return duration(Duration.millis(durationInMillis));
		}

		// === Behavior Configuration ===
		public Builder position(Position position) {
			this.position = (position != null) ? position : this.position;
			return this;
		}

		public Builder widthMode(WidthMode mode) {
			this.widthMode = (mode != null) ? mode : this.widthMode;
			return this;
		}

		public Builder fixedWidth(double width) {
			if (width > 0) {
				this.contentFixedWidth = width;
			}
			return this;
		}

		public Builder maxWidth(double maxWidth) {
			if (maxWidth > 0) {
				this.contentMaxWidth = maxWidth;
			}
			return this;
		}

		public Builder maxHeight(double maxHeight) {
			if (maxHeight > 0) {
				this.contentMaxHeight = maxHeight;
			}
			return this;
		}

//		public Builder autoHide(boolean autoHide) {
//			this.autoHide = autoHide;
//			return this;
//		}
//
//		public Builder hideOnEscape(boolean hideOnEscape) {
//			this.hideOnEscape = hideOnEscape;
//			return this;
//		}

		// === Styling Configuration ===
		public Builder type(TYPE type) {
			this.type = (type != null) ? type : this.type;
			return this;
		}

		public Builder animation(AnimationProfile profile) {
			this.animationProfile = (profile != null) ? profile : this.animationProfile;
			return this;
		}

		public Builder paddingDefault(boolean enabled) {
			this.paddingEnabled = enabled;
			return this;
		}

		public Builder titleBarHeight(double height) {
			if (height > 0) {
				this.titleBarHeight = height;
			}
			return this;
		}

		public Builder styleClasses(String... styleClasses) {
			this.styleClasses = styleClasses != null ? styleClasses : new String[0];
			return this;
		}

		public Builder style(String inlineStyle) {
			this.inlineStyle = inlineStyle;
			return this;
		}
//
//		public Builder interactive(boolean interactive) {
//			this.interactive = interactive;
//			if (interactive) {
//				this.hideOnPopupHover = false;
//			}
//			return this;
//		}

		/**
		 * Configures the popup to be unmanaged, meaning it will NOT have automatic show/hide behavior on hover. Intended for external managers.
		 * 
		 * @return This Builder instance for chaining
		 */
		public Builder unmanaged() {
			this.autoBehavior = false;
			this.hideOnPopupHover = false;
			return this;
		}

		public Builder hideOnPopupHover(boolean hide) {
			this.hideOnPopupHover = hide;
			return this;
		}

		// === Build Method ===

		public CustomPopupControl build() {
			CustomPopupControl popup = new CustomPopupControl(owner); // installiiert mouse listener mit aktivierten hideOnPopupBehavior

			// Apply ALL configurations systematically:

			// 1. Content configuration
			applyContentConfiguration(popup);

			// 2. Timing configuration
			applyTimingConfiguration(popup);

			// 3. Behavior configuration
			applyBehaviorConfiguration(popup); // Hier geht interactive false rein, was heide on hover aktiviert

			// 4. Dimension configuration
			applyDimensionConfiguration(popup);

			// 5. Styling configuration
			applyStylingConfiguration(popup);

			// 6. Setup behavior
			if (autoBehavior) {
				popup.setupManagedTooltipBehavior();
			}
			popup.hideOnPopupHover(hideOnPopupHover);

			for (String path : externalStylesheets) {
				popup.addExternalStylesheet(path);
			}

			return popup;
		}

		private void applyContentConfiguration(CustomPopupControl popup) {
			if (key != null) {
				popup.textProperty().bind(key.property());
			} else if (text != null) {
				popup.text(text);
			} else if (customContent != null) {
				popup.content(customContent);
			}

			if (title != null) {
				popup.title(title, titleIcon);
			}
		}

		private void applyTimingConfiguration(CustomPopupControl popup) {
			popup.showDelay(showDelay);
			popup.duration(totalLifetime);
		}

		private void applyBehaviorConfiguration(CustomPopupControl popup) {
			popup.position(position);
			popup.widthMode(widthMode);
//			popup.interactive(interactive);
			popup.hideOnPopupHover(hideOnPopupHover);
//			popup.withAutoHide(autoHide);
//			popup.withHideOnEscape(hideOnEscape);
		}

		private void applyDimensionConfiguration(CustomPopupControl popup) {
			if (contentFixedWidth != null) {
				popup.contentFixedWidth(contentFixedWidth);
			} else if (contentMaxWidth != null) {
				popup.contentMaxWidth(contentMaxWidth);
			}

			if (contentFixedHeight != null) {
				popup.contentFixedHeight(contentFixedHeight);
			} else if (contentMaxHeight != null) {
				popup.contentMaxHeight(contentMaxHeight);
			}

			if (contentPrefWidth != null) {
				popup.contentPrefWidth(contentPrefWidth);
			}

			if (contentPrefHeight != null) {
				popup.contentPrefHeight(contentPrefHeight);
			}
		}

		private void applyStylingConfiguration(CustomPopupControl popup) {
			popup.type(type);
			popup.animation(animationProfile);

			// Apply padding
			if (customPadding != null) {
				popup.getContentContainer().setPadding(customPadding);
			} else {
				popup.paddingDefault(paddingEnabled);
			}

			if (titleBarHeight != null) {
				popup.setTitleBarHeight(titleBarHeight);
			}

			if (inlineStyle != null) {
				popup.getLayoutPane().setStyle(inlineStyle);
			}

			for (String styleClass : styleClasses) {
				if (styleClass != null && !styleClass.trim().isEmpty()) {
					popup.getLayoutPane().getStyleClass().add(styleClass.trim());
				}
			}
		}
	}

	// --- 2. API for TEMPORARY, PROGRAMMATIC Popups (Configurator Pattern) ---

	/**
	 * Shows a temporary notification with the most common configurations in a single call.
	 *
	 * @param owner    The Node to which the popup will be anchored.
	 * @param text     The text content to display.
	 * @param type     The visual style of the popup (e.g., TYPE.SUCCESS, TYPE.ERROR).
	 * @param position The position of the popup relative to the owner.
	 * @param duration The total time the popup should be visible before fading out.
	 */
	public static void show(Node owner, String text, TYPE type, Position position, Duration duration) {
		buildFor(owner).withText(text).withType(type).withPosition(position).show(duration);
	}

	/**
	 * Returns a fluent configurator for building and showing temporary popups with full configuration options.
	 *
	 * @param owner The Node to which the popup will be anchored.
	 * @return A new ProgrammaticPopup instance for configuration.
	 */
	public static TemporaryPopup buildFor(Node owner) {
		return new TemporaryPopup(owner);
	}

	// =========================================================================
	// === STATIC METHODS FOR PROGRAMMATIC (TEMPORARY) POPUPS (FINAL API) ====
	// =========================================================================
	/**
	 * A fluent configurator for showing temporary, programmatic popups.
	 */
	public static class TemporaryPopup {
		/**
		 * Creates a temporary popup configurator.
		 */
		public static TemporaryPopup forNode(Node owner) {
			return new TemporaryPopup(owner);
		}

		private final Node     owner;
		private String         text     = null;
		private LocalizableKey key      = null;
		private Position       position = Position.BELOW;
		private TYPE           type     = TYPE.INFO;
		private double         maxWidth = 435.0;         // Default max width

		private TemporaryPopup(Node owner) {
			this.owner = owner;
		}

		/**
		 * Sets the text content.
		 * 
		 * @param text The text to display
		 * @return This ProgrammaticPopup instance for chaining
		 */
		public TemporaryPopup withText(String text) {
			this.text = text;
			this.key = null;
			return this;
		}

		/**
		 * Sets the text content via localization key.
		 * 
		 * @param key The LocalizableKey for the text
		 * @return This ProgrammaticPopup instance for chaining
		 */
		public TemporaryPopup withKey(LocalizableKey key) {
			this.key = key;
			this.text = null;
			return this;
		}

		/**
		 * Sets the position relative to owner.
		 * 
		 * @param position The position strategy (default: BELOW_COMPONENT)
		 * @return This ProgrammaticPopup instance for chaining
		 */
		public TemporaryPopup withPosition(Position position) {
			this.position = position;
			return this;
		}

		/**
		 * Sets the visual type.
		 * 
		 * @param type The visual type (default: INFO)
		 * @return This ProgrammaticPopup instance for chaining
		 */
		public TemporaryPopup withType(TYPE type) {
			this.type = type;
			return this;
		}

		/**
		 * Sets the maximum width.
		 * 
		 * @param maxWidth Maximum width (default: 435.0)
		 * @return This ProgrammaticPopup instance for chaining
		 */
		public TemporaryPopup withMaxWidth(double maxWidth) {
			this.maxWidth = maxWidth;
			return this;
		}

		/**
		 * Shows the temporary popup for specified duration.
		 */
		public void show(Duration duration) {
			if (owner == null)
				return;

			String contentText = (key != null) ? key.get() : text;
			if (StringUtils.isBlank(contentText))
				return;

			// Create unmanaged instance
			CustomPopupControl popup = CustomPopupControl.createFor(owner).unmanaged().build();

			// Configure
			popup.type(type).position(position).text(contentText).contentMaxWidth(maxWidth);

			// Show once with auto-dispose
			popup.showOnce(duration);
		}

	} // class end

	// =========================================================================
	// === LISTENER & EVENT HANDLERS ===========================================
	// =========================================================================

	/** Mouse enter handler - behavior depends on {@link #hideOnPopupHover} */
	private final EventHandler<MouseEvent> popupMouseEnteredHandler;

	/** Mouse exit handler - in interactive mode only hides when owner also left */
	private final EventHandler<MouseEvent> popupMouseExitedHandler;

	/** Focus listener - hides popup when owner loses focus (unless hovering) */
	private final ChangeListener<Boolean> ownerFocusListener;

	/** Window X-move listener - hides popup when parent window moves */
	private ChangeListener<Number> windowXListener;

	/** Window Y-move listener - hides popup when parent window moves */
	private ChangeListener<Number> windowYListener;

	/** Current window for listener management */
	private Window currentWindow;

	// =========================================================================
	// === UI COMPONENTS =======================================================
	// =========================================================================

	/** Owner node this popup is attached to */
	private final Node owner;

	/** Root wrapper with shadow effect */
	private final StackPane shadowWrapper;

	/** Main layout container */
	private final VBox layoutPane;

	/** Container for dynamic content */
	private final StackPane contentPane;

	/** Default text display (when no custom content set) */
	private final TextFlow contentTextFlow;

	/** Optional title bar label */
	private final Label titleLabel;

	/** Optional title bar container */
	private final HBox titleBarContent;

	// =========================================================================
	// === ANIMATION & TIMING ==================================================
	// =========================================================================

	/** Fade-in animation timeline */
	private final Timeline fadeIn;

	/** Fade-out animation timeline */
	private final Timeline fadeOut;

	/** Current fade-in style (from profile) */
	private AnimationStyle fadeInStyle = AnimationProfile.BALANCED.fadeInStyle;

	/** Current fade-out style (from profile) */
	private AnimationStyle fadeOutStyle = AnimationProfile.BALANCED.fadeOutStyle;

	/** Delay before showing (hover mode) */
	private PauseTransition delayTimer = new PauseTransition(Duration.millis(600));

	/** How long popup stays visible */
	private final PauseTransition stayTimer;

	/** Total lifetime duration */
	private Duration totalLifetime = Duration.millis(5000);

	// =========================================================================
	// === STATE & CONFIGURATION ===============================================
	// =========================================================================

	/** Current CSS theme class */
	private String currentThemeStyleClass = TYPE.INFO.getStyleClass();

	/** Tooltip mode (true=hide on popup hover, false=interactive) */
	private boolean hideOnPopupHover = true;

	/** Width behavior strategy */
	private WidthMode widthMode = WidthMode.FIT_TO_CONTENT;

	/** Positioning strategy */
	private Position position = Position.BELOW;

	/** Last mouse X position (for repositioning) */
	private double lastMouseX;

	/** Last mouse Y position (for repositioning) */
	private double lastMouseY;

	// =========================================================================
	// === PROPERTIES ==========================================================
	// =========================================================================

	/** Text content property */
	private final StringProperty textProperty = new SimpleStringProperty();

	public void addExternalStylesheet(String path) {
		if (path == null || path.trim().isEmpty())
			return;

		URL cssUrl = null;

		try {
			// 1. Versuch: Direkter, relativer Pfad zum Package des Aufrufers
			cssUrl = CustomPopupControl.class.getResource(path);

			// 2. Versuch: Mit führendem Slash (vom Root aus)
			if (cssUrl == null) {
				String rootPath = path.startsWith("/") ? path : "/" + path;
				cssUrl = CustomPopupControl.class.getResource(rootPath);
			}

			// 3. Versuch: Über den ClassLoader (funktioniert oft in JARs)
			if (cssUrl == null) {
				String cleanPath = path.startsWith("/") ? path.substring(1) : path;
				cssUrl = Thread.currentThread().getContextClassLoader().getResource(cleanPath);
			}

			if (cssUrl == null) {
				Log.error("External CSS not found after all attempts: " + path);
				return;
			}

			Log.info("External CSS loaded from: " + cssUrl);

			// ✨✨✨ WICHTIG: Stylesheet zum LAYOUT-PANE hinzufügen, nicht zur Scene! ✨✨✨
			final String cssToAdd = cssUrl.toExternalForm();

			if (layoutPane != null) {
				layoutPane.getStylesheets().add(cssToAdd);
				Log.info("External CSS added to layoutPane");
			} else {
				// Fallback: Scene (falls layoutPane nicht verfügbar)
				if (getScene() != null) {
					getScene().getStylesheets().add(cssToAdd);
					Log.info("External CSS added to scene (fallback)");
				}
			}

		} catch (Exception e) {
			Log.error("Failed to load external CSS: " + path, e);
		}
	}

	// Overload für mehrere Stylesheets
	public CustomPopupControl addStylesheets(String... urls) {
		for (String url : urls) {
			addExternalStylesheet(url);
		}
		return this;
	}

	/**
	 * Configures automatic hover behavior for this tooltip instance. Sets up timers and event listeners for show/hide actions based on mouse interactions.
	 * 
	 * <p>
	 * <b>Behavior:</b>
	 * <ul>
	 * <li>Shows after {@link #delayTimer} completes if owner is still hovered</li>
	 * <li>Stays visible for calculated duration based on total lifetime</li>
	 * <li>Hides when mouse exits owner or when stay timer completes</li>
	 * <li>Automatically cleans up when owner's scene changes</li>
	 * </ul>
	 */
	private void setupManagedTooltipBehavior() {

		// Configure delay timer completion
		delayTimer.setOnFinished(event -> {
			if (owner.isHover()) {
				Log.debug("Showing tooltip for owner: %s", owner.getClass().getSimpleName());

				// Calculate screen position from owner bounds
				Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
				show(owner, bounds.getMinX(), bounds.getMaxY());

				// Calculate actual display duration (total lifetime minus fade-out time)
				Duration fadeOutDuration = this.fadeOutStyle.duration();
				Duration stayDuration = this.totalLifetime.subtract(fadeOutDuration);

				// Ensure non-negative stay duration
				if (stayDuration.lessThan(Duration.ZERO)) {
					stayDuration = Duration.ZERO;
					Log.warn("Total lifetime shorter than fade-out duration. Adjusted stay duration to zero.");
				}

				// Start the stay timer with calculated duration
				this.stayTimer.setDuration(stayDuration);
				this.stayTimer.playFromStart();
			}
		});

		// Configure stay timer to trigger fade-out when complete
		stayTimer.setOnFinished(event -> {
			hideFaded();
		});

		// Show tooltip on mouse enter (after delay)
		owner.setOnMouseEntered(event -> {
			delayTimer.playFromStart();
			stayTimer.stop(); // Reset any running stay timer
		});

		// Hide tooltip immediately on mouse exit, except for mouse is hovering this popup, but we check what is allowed via flag so we can control it
		owner.setOnMouseExited(event -> {
			delayTimer.stop();

			// ✅ NEU: Bedingt ausblenden basierend auf Flag
			if (hideOnPopupHover) {
				// Tooltip-Modus: Sofort ausblenden wenn Maus Owner verlässt
				hideFaded();
			} else {
				// Interactive-Modus: Nur ausblenden wenn Maus weder über Owner NOCH Popup ist
				if (!shadowWrapper.isHover()) {
					hideFaded();
				}
				// Wenn Maus über Popup ist, NICHT ausblenden!
			}
		});

		// Clean up when owner's scene changes (e.g., window closed)
		owner.sceneProperty().addListener((obs, oldScene, newScene) -> {
			if (newScene == null) {
				delayTimer.stop();
				hide();
			}
		});
	}

	/**
	 * Configures the width behavior of the popup's content area.
	 * 
	 * <p>
	 * Two width modes are supported:
	 * <ul>
	 * <li><b>{@link WidthMode#FIT_TO_CONTENT}</b> (default): The popup width adjusts dynamically based on its content. This ensures optimal readability without unnecessary
	 * whitespace.</li>
	 * <li><b>{@link WidthMode#TRACK_OWNER}</b>: The popup width matches the owner component's width. This creates a visually aligned appearance when the popup is positioned below
	 * or above the owner. Only supported when the owner is a {@link Region}.</li>
	 * </ul>
	 * 
	 * <p>
	 * <b>Dynamic Updates:</b> If the popup is currently visible when this method is called, it will automatically resize and reposition to reflect the new width mode. This ensures
	 * immediate visual feedback without requiring manual re-display.
	 * </p>
	 * 
	 * <p>
	 * <b>Limitations:</b> {@code TRACK_OWNER} mode requires the owner to be a {@link Region} instance. If applied to other node types, a warning is logged and the mode falls back
	 * to {@code FIT_TO_CONTENT}.
	 * </p>
	 * 
	 * <h3>Usage Examples:</h3>
	 * 
	 * <pre>{@code
	 * // Default behavior - width adjusts to content
	 * popup.widthMode(WidthMode.FIT_TO_CONTENT);
	 * 
	 * // Match owner component width (e.g., for dropdown-like popups)
	 * popup.widthMode(WidthMode.TRACK_OWNER);
	 * }</pre>
	 *
	 * @param mode The desired width behavior mode
	 * @return This instance for method chaining
	 * 
	 * @see WidthMode
	 * @see #isShowing()
	 * @see #sizeToScene()
	 */
	public CustomPopupControl widthMode(WidthMode mode) {
		if (mode == this.widthMode) {
			return this; // No change required
		}

		// Clear previous width bindings
		this.layoutPane.minWidthProperty().unbind();
		this.layoutPane.setMinWidth(Region.USE_COMPUTED_SIZE);

		this.widthMode = mode;

		if (mode == WidthMode.TRACK_OWNER) {
			if (owner instanceof Region) {
				Region ownerRegion = (Region) owner;
				this.layoutPane.minWidthProperty().bind(ownerRegion.widthProperty());
			} else {
				Log.warn("WidthMode.TRACK_OWNER not supported for owner type: {}. Falling back to FIT_TO_CONTENT.", owner.getClass().getSimpleName());
				this.widthMode = WidthMode.FIT_TO_CONTENT;
			}
		}

		// Apply changes immediately if popup is visible
		if (isShowing()) {
			Platform.runLater(() -> {
				if (isShowing()) { // State verification
					sizeToScene();
					reposition(this.lastMouseX, this.lastMouseY);
				}
			});
		}

		return this;
	}

	/**
	 * Constructs a new CustomPopupControl for the specified owner node.
	 * <p>
	 * <b>Default values:</b>
	 * <ul>
	 * <li>Width mode: FIT_TO_CONTENT</li>
	 * <li>Position: BELOW_COMPONENT</li>
	 * <li>Type: INFO</li>
	 * <li>Animation: BALANCED</li>
	 * <li>Padding: Activated</li>
	 * <li>Max width: 400.0</li>
	 * </ul>
	 * 
	 * @param owner The owner node for this popup control
	 * @throws IllegalArgumentException if owner is null
	 */
	private CustomPopupControl(Node owner) {
		if (owner == null) {
			throw new IllegalArgumentException("Owner node cannot be null.");
		}
		this.owner = owner;

		// Create layout

		this.layoutPane = createLayoutPane();
		this.layoutPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
		this.layoutPane.setMinHeight(Region.USE_COMPUTED_SIZE);
		this.layoutPane.setMaxHeight(Double.MAX_VALUE); // Unendlich, damit es nicht begrenzt wird
		this.layoutPane.setMaxWidth(INITIAL_MAX_WIDTH);

		this.shadowWrapper = new StackPane(this.layoutPane);

		this.shadowWrapper.getStyleClass().add("shadow-wrapper");

		this.shadowWrapper.setFocusTraversable(false);
		this.layoutPane.setFocusTraversable(false);

		getScene().setFill(Color.TRANSPARENT);
		getScene().setRoot(shadowWrapper);

//		setupTooltipBehavior(); // wird nicht explizit hier gesetzt sondern in der frage ob es managed ist oder nicht

		// Load CSS on shadowWrapper (not on Scene)
		// IMPORTANT: Due to a JavaFX limitation in PopupControl, CSS must be loaded
		// on the root node instead of the scene. Unlike Stage, PopupControl does not
		// automatically propagate CSS from scene to root node.
		URL cssUrl = getCssUrl();
		if (cssUrl != null) {
			this.shadowWrapper.getStylesheets().add(cssUrl.toExternalForm());
		} else {
			applyMinimalInlineStyles(this.layoutPane);
		}

		// IMORTANT: do ot activate, otherwise the TableTooltipManagers Popup consumes selection klicks when popup is shown: setAutoHide(true);
		setHideOnEscape(true);

		// 1. Erstellen Sie den dedizierten Inhalts-Container.
		this.contentPane = new StackPane();
		// 2. Setze das Padding standardmäßig auf AKTIV.
		paddingDefault(true);

		// --- KORREKTUR (Level 2: Content-Pane) ---
		this.contentPane.setPrefHeight(Region.USE_COMPUTED_SIZE);

		// 2. Weisen Sie ihm die CSS-Klasse zu, die das Padding definiert.
//		this.contentPane.getStyleClass().add("content-pane");

		// 5. Baue die VBox zusammen
		// Die VBox enthält jetzt optional die titleBarContent und immer das contentPane
		this.layoutPane.getChildren().add(this.contentPane);

		// 4. Erstellen Sie den Standard-Inhalt (den TextFlow).
		this.contentTextFlow = new TextFlow();

		// --- KORREKTUR (Level 3: TextFlow) ---
		// Set Pref/Max width to ensure it respects parent constraints
		this.contentTextFlow.setPrefWidth(Region.USE_COMPUTED_SIZE); // Allow TextFlow to shrink/grow horizontally
		this.contentTextFlow.setPrefHeight(Region.USE_COMPUTED_SIZE); // Already exists
		this.contentTextFlow.setMaxWidth(Double.MAX_VALUE); // Allow it to expand without early constraint
		this.contentTextFlow.setLineSpacing(2.0); // Improves readability for wrapped text

		// CRITICAL: Add listener to TextFlow's width property to ensure Text nodes wrap correctly
		// This listener iterates over children and sets wrappingWidth for Text nodes when TextFlow resizes.
		this.contentTextFlow.widthProperty().addListener((obs, oldWidth, newWidth) -> {
			if (newWidth != null && newWidth.doubleValue() > 0) {
				for (Node child : this.contentTextFlow.getChildren()) {
					if (child instanceof Text textNode) {
						textNode.setWrappingWidth(newWidth.doubleValue());
					}
				}
			}
		});
		// WICHTIG: Dieser bekommt KEINE Klasse mehr.

		// 5. Setzen Sie den Standard-Inhalt als Kind des neuen contentPane.
		this.contentPane.getChildren().add(this.contentTextFlow);
//		this.contentPane.setStyle("-fx-background-color:blue;");

		this.titleLabel = new Label();
		this.titleLabel.getStyleClass().add("title-text");
		this.titleLabel.setWrapText(true);
		HBox.setHgrow(this.titleLabel, Priority.ALWAYS);
		this.titleBarContent = new HBox(10, titleLabel);
		this.titleBarContent.setAlignment(Pos.CENTER_LEFT);

		this.titleBarContent.getStyleClass().add("title-bar");

		this.stayTimer = new PauseTransition(); // Initialisierung ohne Dauer

		this.fadeIn = new Timeline();
		this.fadeOut = new Timeline();
		reconfigureFadeAnimations(); // Konfiguriert die Timelines mit den Standardstilen

		this.fadeIn.setOnFinished(e -> {
			if (isShowing()) {
				shadowWrapper.setOpacity(1.0);
			}
		});

		this.fadeOut.setOnFinished(e -> {
			if (isShowing()) {
				hide();
			}
		});

		// Add listener for dynamic text updates and set the default theme.
		textProperty.addListener((obs, oldText, newText) -> updateTextFlow(this.contentTextFlow, newText));

		type(TYPE.INFO);

		this.ownerFocusListener = (obs, wasFocused, isNowFocused) -> {
			if (!isNowFocused) {
				if (!shadowWrapper.isHover()) {
					hideFaded();
				}
			}
		};

		if (owner != null) {
			owner.focusedProperty().addListener(ownerFocusListener);
		}

		popupMouseEnteredHandler = event -> {
			Log.debug("Mouse entered popup, hideOnPopupHover=" + hideOnPopupHover);

			if (hideOnPopupHover) {
				// Tooltip mode: Hide when mouse enters popup
				hideFaded();
			} else {
				// Interactive mode: Keep visible, stop auto-hide timers
				stayTimer.stop();
				delayTimer.stop();
			}
			event.consume();
		};

		popupMouseExitedHandler = event -> {
			Log.debug("Mouse exited popup, hideOnPopupHover=" + hideOnPopupHover);

			if (!hideOnPopupHover) {
				// Interactive mode: Only hide if mouse also not over owner
				if (!owner.isHover()) {
					hideFaded();
				}
			}
			// Tooltip mode: Already hidden, nothing to do
			event.consume();
		};

		// Wir fügen einen Listener zur "boundsInParentProperty" des Haupt-Layouts hinzu.
		// Diese Eigenschaft ändert sich, wann immer sich die berechnete Größe
		// (Breite oder Höhe) des Layouts ändert – also genau dann, wenn sich der
		// Inhalt ändert (z.B. neue Items hinzukommen oder Text länger wird).
		layoutPane.boundsInParentProperty().addListener((obs, oldBounds, newBounds) -> {
			// Wir rufen die Neupositionierung nur auf, wenn sich die Breite oder Höhe
			// tatsächlich geändert hat, um unnötige Aufrufe zu vermeiden.
			if (newBounds.getWidth() != oldBounds.getWidth() || newBounds.getHeight() != oldBounds.getHeight()) {
				// Wir verwenden die bereits verbesserte, stabile updatePosition-Methode.
				updatePosition();
			}
		});

		textProperty.addListener((obs, oldText, newText) -> {
			if (Platform.isFxApplicationThread()) {
				updateTextFlow(this.contentTextFlow, newText);
			} else {
				Platform.runLater(() -> updateTextFlow(this.contentTextFlow, newText));
			}
		});
	}

	private void enablePopupHoverListeners() {
		shadowWrapper.addEventHandler(MouseEvent.MOUSE_ENTERED, popupMouseEnteredHandler);
		shadowWrapper.addEventHandler(MouseEvent.MOUSE_EXITED, popupMouseExitedHandler);
	}

	private void disablePopupHoverListeners() {
		shadowWrapper.removeEventHandler(MouseEvent.MOUSE_ENTERED, popupMouseEnteredHandler);
		shadowWrapper.removeEventHandler(MouseEvent.MOUSE_EXITED, popupMouseExitedHandler);
	}

	/**
	 * Creates and configures the main layout container for the popup.
	 * 
	 * @return A configured VBox with the "layout-pane" style class applied
	 */
	private VBox createLayoutPane() {
		VBox pane = new VBox();
		pane.getStyleClass().add("layout-pane");
		return pane;
	}

	/**
	 * Applies minimal inline CSS styles as a fallback when no external CSS file is available. These styles ensure basic visual presentation and functionality.
	 * 
	 * @param pane The parent container to apply styles to
	 */
	private void applyMinimalInlineStyles(Parent pane) {
		String minimalStyle = "-fx-background-color: #292829; " + "-fx-border-color: #555555; " + "-fx-border-width: 1px; " + "-fx-background-radius: 5px; "
		        + "-fx-border-radius: 5px; " + "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 10, 0, 0, 2); " + "-fx-padding: 0;";

		pane.setStyle(minimalStyle);
	}

	/**
	 * TODO implement> // interactive > Kann Buttons/Inputs enthalten! Zieht dann aber fokus vom owner Switches the between a passive display mode and an interactive mode. In
	 * interactive mode, the popup can receive and hold focus to process mouse clicks.
	 * <p>
	 * <b>Default:</b> false (passive display)
	 * </p>
	 *
	 * @param interactive true to enable interaction, false for default passive display.
	 * @return This CustomTooltip instance for method chaining.
	 */
//	private CustomPopupControl interactive(boolean interactive) {
	////		return hideOnPopupHover(!interactive);
//		return this;
//	}

	/**
	 * When true (default), popup hides when mouse enters it (tooltip behavior). When false, popup stays visible on hover (interactive mode).
	 */
	public CustomPopupControl hideOnPopupHover(boolean hide) {
		this.hideOnPopupHover = hide;

		if (hide) {
			enablePopupHoverListeners();
		} else {
			disablePopupHoverListeners();
		}
		// TODO install and deinstall mouse Listener
		return this;
	}

	/**
	 * Activates or deactivates the standard inner padding for the content area. The padding is defined by the "popup-content-padding" CSS style class.
	 * <p>
	 * <b>Default:</b> true (padding activated)
	 * </p>
	 *
	 * @param activated If true, the standard padding is applied. If false, it is removed.
	 * @return This CustomPopupControl instance for method chaining.
	 */
	public CustomPopupControl paddingDefault(boolean activated) {
		final String paddingClass = "popup-content-padding";
		if (activated) {
			if (!this.contentPane.getStyleClass().contains(paddingClass)) {
				this.contentPane.getStyleClass().add(paddingClass);
			}
		} else {
			this.contentPane.getStyleClass().remove(paddingClass);
		}
		return this; // Erlaube Chaining
	}

	/**
	 * Returns the StringProperty that holds the tooltip's text content. This allows for powerful data binding.
	 *
	 * @return The text property of the tooltip.
	 */
	public StringProperty textProperty() {
		return this.textProperty;
	}

	/**
	 * Gets the current text content of the tooltip.
	 * 
	 * @return The current text.
	 */
	public final String getText() {
		return textProperty.get();
	}

	/**
	 * A static helper method that parses a string and populates a TextFlow. It supports simple formatting tags like &lt;b&gt; for bold text and \n for newlines. The {@code Text}
	 * nodes generated will automatically wrap their text based on the {@code TextFlow}'s computed width.
	 *
	 * @param textFlow The TextFlow node to clear and populate.
	 * @param text     The string containing the text and formatting tags. Can be null.
	 */
	private static void updateTextFlow(TextFlow textFlow, String text) {
		// 1. Clear any previous content.
		textFlow.getChildren().clear();

		// 2. Handle null or empty input gracefully.
		if (text == null || text.isEmpty()) {
			return;
		}

		String[] parts = text.split("(?i)(?<=</?b>)|(?=</?b>|\\\\n)");

		boolean isBold = false;
		for (String part : parts) {
			// Check for bold tags
			if (part.equalsIgnoreCase("<b>")) {
				isBold = true;
				continue; // Go to the next part
			}
			if (part.equalsIgnoreCase("</b>")) {
				isBold = false;
				continue;
			}

			// Check for newline tag
			if (part.equals("\\n")) {
				textFlow.getChildren().add(new Text("\n"));
				continue;
			}

			// Process the actual text part
			if (!part.isEmpty()) {
				Text textNode = new Text(part);

				// Apply the base style class for all text within the tooltip.
				textNode.getStyleClass().add("content-text");

				// Apply the bold style class if we are within a <b> tag.
				if (isBold) {
					textNode.getStyleClass().add("text-bold");
				}
				// CRITICAL ADDITION: Set initial wrapping width. Listener on TextFlow will handle dynamic changes.
				// Ensure textNode is immediately set to wrap by the current width if available
				if (textFlow.getWidth() > 0) { // Only apply if TextFlow has a valid width
					textNode.setWrappingWidth(textFlow.getWidth());
				}
				textFlow.getChildren().add(textNode);
			}
		}
	}
	// --- Configuration Methods ---

	// === FLUENT SETTERS (korrigiert) ===

	/**
	 * Sets the text content of the tooltip.
	 * 
	 * @param text The string to display
	 * @return This CustomPopupControl instance for method chaining
	 */
	public CustomPopupControl text(String text) {
		textProperty.set(text);
		return this;
	}

	/**
	 * Sets the positioning strategy.
	 * 
	 * @param position The position strategy
	 * @return This instance for method chaining
	 */
	public CustomPopupControl position(Position position) {
		if (position == null) {
			Log.warn("setPosition(null) called. The position will not be changed. Please provide a valid Position enum.");
			return this;
		}

		if (position == this.position) {
			return this;
		}
		this.position = position;
		reposition(this.lastMouseX, this.lastMouseY);
		return this;
	}

	/**
	 * Recalculates the size and position of the popup. This should be called whenever the content of the popup changes, which might affect its dimensions.
	 *
	 * It uses Platform.runLater to ensure that the resizing and repositioning happens on the next layout pulse, after the JavaFX engine has had time
	 * 
	 * to compute the new preferred size of the content.
	 */
	public void updatePosition() {
		if (!isShowing())
			return;

		Platform.runLater(() -> {
			if (!isShowing())
				return;

			// 1. Größe an den neuen Inhalt anpassen (falls nicht schon passiert)
//	        sizeToScene(); 

			// 2. Position neu berechnen
			reposition(this.lastMouseX, this.lastMouseY);
		});
	}

	/**
	 * Sets the visual type/style.
	 * 
	 * @param type The visual type
	 * @return This instance for method chaining
	 */
	public CustomPopupControl type(TYPE type) {
		if (type != null) {
			String newStyleClass = type.getStyleClass();
			if (!newStyleClass.equals(currentThemeStyleClass)) {
				layoutPane.getStyleClass().remove(currentThemeStyleClass);
				layoutPane.getStyleClass().add(newStyleClass);
				currentThemeStyleClass = newStyleClass;
			}
		}
		return this;
	}

	/**
	 * Sets the show delay for hover-triggered tooltips.
	 * 
	 * @param showDelay The delay duration
	 * @return This instance for method chaining
	 */
	public CustomPopupControl showDelay(Duration showDelay) { // ❌ from withDelay
		if (showDelay == null) {
			Log.warn("setShowDelay(null) ignored");
			return this;
		}
		this.delayTimer.setDuration(showDelay);
		return this;
	}

	/**
	 * Sets the show delay in milliseconds.
	 * 
	 * @param delayInMillis The delay in milliseconds
	 * @return This instance for method chaining
	 */
	public CustomPopupControl showDelay(long delayInMillis) {
		return showDelay(Duration.millis(delayInMillis));
	}

	/**
	 * Sets the total visible duration before auto-hiding.
	 * 
	 * @param totalLifetime The total visible duration
	 * @return This instance for method chaining
	 */
	public CustomPopupControl duration(Duration totalLifetime) { // ❌ from withDuration
		if (totalLifetime == null) {
			Log.warn("setDuration(null) ignored");
			return this;
		}
		this.totalLifetime = totalLifetime;
		return this;
	}

	/**
	 * Sets the total visible duration in milliseconds.
	 * 
	 * @param durationInMillis The duration in milliseconds
	 * @return This instance for method chaining
	 */
	public CustomPopupControl duration(long durationInMillis) {
		return duration(Duration.millis(durationInMillis));
	}

	/**
	 * --- PRIMARY API --- Sets the animation behavior using a predefined, choreographed profile. This is the recommended way to configure animations for a consistent feel.
	 *
	 * @param profile The desired animation profile (e.g., AnimationProfile.GENTLE).
	 * @return This instance for method chaining.
	 */
	public CustomPopupControl animation(AnimationProfile profile) { // ❌ from withAnimation
		if (profile != null) {
			this.fadeInStyle = profile.getFadeInStyle();
			this.fadeOutStyle = profile.getFadeOutStyle();
			reconfigureFadeAnimations();
		}
		return this;
	}

	/**
	 * --- ADVANCED API --- Overrides only the fade-in animation with a custom style. Use this for special cases not covered by the profiles.
	 *
	 * @param customStyle The custom AnimationStyle for the fade-in.
	 * @return This instance for method chaining.
	 */
	public CustomPopupControl fadeIn(AnimationStyle style) { // ❌ from withCustomFadeIn
		if (style != null) {
			this.fadeInStyle = style;
			reconfigureFadeAnimations();
		}
		return this;
	}

	/**
	 * --- ADVANCED API --- Overrides only the fade-out animation with a custom style. Use this for special cases not covered by the profiles.
	 *
	 * @param customStyle The custom AnimationStyle for the fade-out.
	 * @return This instance for method chaining.
	 */
	public CustomPopupControl fadeOut(AnimationStyle style) { // ❌ from withCustomFadeOut
		if (style != null) {
			this.fadeOutStyle = style;
			reconfigureFadeAnimations();
		}
		return this;
	}

	// === GETTERS (für Properties wo sinnvoll) ===

	/**
	 * Gets the current show delay.
	 * 
	 * @return Current show delay duration
	 */
	public Duration getShowDelay() {
		return delayTimer.getDuration();
	}

	/**
	 * Gets the current total visible duration.
	 * 
	 * @return Current total lifetime duration
	 */
	public Duration getDuration() {
		return totalLifetime;
	}

	/**
	 * Gets the current fade-in animation style.
	 * 
	 * @return Current fade-in animation style
	 */
	public AnimationStyle getFadeInStyle() {
		return fadeInStyle;
	}

	/**
	 * Gets the current fade-out animation style.
	 * 
	 * @return Current fade-out animation style
	 */
	public AnimationStyle getFadeOutStyle() {
		return fadeOutStyle;
	}

	/**
	 * Reconfigures the fade-in and fade-out animations based on current animation styles. Creates new KeyFrame animations using the configured durations and interpolators.
	 * 
	 * <p>
	 * This method is called automatically when animation styles are changed via {@link #animation(AnimationProfile)}, {@link #withCustomFadeIn(AnimationStyle)}, or
	 * {@link #withCustomFadeOut(AnimationStyle)}.
	 * </p>
	 */
	private void reconfigureFadeAnimations() {
		// Configure fade-in animation
		KeyValue fadeInValue = new KeyValue(shadowWrapper.opacityProperty(), 1.0, fadeInStyle.interpolator());
		KeyFrame fadeInFrame = new KeyFrame(fadeInStyle.duration(), fadeInValue);
		fadeIn.getKeyFrames().setAll(fadeInFrame);

		// Configure fade-out animation
		KeyValue fadeOutValue = new KeyValue(shadowWrapper.opacityProperty(), 0.0, fadeOutStyle.interpolator());
		KeyFrame fadeOutFrame = new KeyFrame(fadeOutStyle.duration(), fadeOutValue);
		fadeOut.getKeyFrames().setAll(fadeOutFrame);
	}

	// === OVERRIDE WINDOW GETTER (double) ===

	/**
	 * Sets the maximum content height.
	 * 
	 * @param height Maximum height for content area
	 * @return This instance for method chaining
	 */
	public CustomPopupControl contentMaxHeight(double height) {
		layoutPane.setMaxHeight(height);
//		updatePosition(); // Nur Position updaten
		return this;
	}

	/**
	 * Sets the maximum content width.
	 * 
	 * @param width Maximum width for content area
	 * @return This instance for method chaining
	 */
	public CustomPopupControl contentMaxWidth(double width) {
		layoutPane.setMaxWidth(width);
//		updatePosition(); // Nur Position updaten
		return this;
	}

	/**
	 * Sets the preferred content width.
	 * 
	 * @param width Preferred width for content area
	 * @return This instance for method chaining
	 */
	public CustomPopupControl contentPrefWidth(double width) {
		layoutPane.setPrefWidth(width);
//		updatePosition(); // Nur Position updaten
		return this;
	}

	/**
	 * Sets the preferred content height.
	 * 
	 * @param height Preferred height for content area
	 * @return This instance for method chaining
	 */
	public CustomPopupControl contentPrefHeight(double height) {
		layoutPane.setPrefHeight(height);
//		updatePosition(); // Nur Position updaten
		return this;
	}

	/**
	 * Sets the minimum content width.
	 * 
	 * @param width Minimum width for content area
	 * @return This instance for method chaining
	 */
	public CustomPopupControl contentMinWidth(double width) {
		layoutPane.setMinWidth(width);
//		updatePosition(); // Nur Position updaten
		return this;
	}

	/**
	 * Sets the minimum content height.
	 * 
	 * @param height Minimum height for content area
	 * @return This instance for method chaining
	 */
	public CustomPopupControl contentMinHeight(double height) {
		layoutPane.setMinHeight(height);
		return this;
	}

	/**
	 * Sets a fixed width for the content area (min, pref, max same).
	 * 
	 * @param width Fixed width for content
	 * @return This instance for method chaining
	 */
	public CustomPopupControl contentFixedWidth(double width) {
		if (width > 0) {
			layoutPane.setMinWidth(width);
			layoutPane.setPrefWidth(width);
			layoutPane.setMaxWidth(width);
		}
		return this;
	}

	/**
	 * Sets a fixed height for the content area (min, pref, max same).
	 * 
	 * @param height Fixed height for content
	 * @return This instance for method chaining
	 */
	public CustomPopupControl contentFixedHeight(double height) {
		if (height > 0) {
			layoutPane.setMinHeight(height);
			layoutPane.setPrefHeight(height);
			layoutPane.setMaxHeight(height);
		}
		return this;
	}

	/**
	 * Sets fixed size for the content area.
	 * 
	 * @param width  Fixed width
	 * @param height Fixed height
	 * @return This instance for method chaining
	 */
	public CustomPopupControl contentFixedSize(double width, double height) {
		return contentFixedWidth(width).contentFixedHeight(height);
	}

	/**
	 * Sets the title of the tooltip, optionally with an icon.
	 *
	 * @param title The text for the title. If null or blank, the title bar will be hidden.
	 * @param icon  The icon Node (e.g., an ImageView or a FontIcon) to display. Can be null.
	 * @return This CustomTooltip instance for method chaining.
	 */
	public CustomPopupControl title(String title, Node icon) {
		if (StringUtils.isBlank(title)) {
			// Entferne die Titelzeile aus der VBox
			if (this.layoutPane.getChildren().contains(this.titleBarContent)) {
				this.layoutPane.getChildren().remove(this.titleBarContent);
				updatePosition();
			}
		} else {
			boolean wasHidden = !this.layoutPane.getChildren().contains(this.titleBarContent);
			titleLabel.setText(title);
			titleBarContent.getChildren().clear();
			if (icon != null) {
				titleBarContent.getChildren().add(icon);
			}
			titleBarContent.getChildren().add(titleLabel);

			if (wasHidden) {
				// Füge die Titelzeile an erster Stelle in der VBox ein
				this.layoutPane.getChildren().add(0, this.titleBarContent);
				updatePosition();
			}
		}
		return this;
	}

	public CustomPopupControl title(String title) {
		return title(title, null);
	}

	public CustomPopupControl setTitleBarHeight(double height) {
		this.titleBarContent.setPrefHeight(height);
		this.titleBarContent.setMinHeight(height);
		return this;
	}

	/**
	 * Returns the main layout container (VBox) that holds all popup content.
	 * <p>
	 * <b>Note:</b> This is the root layout pane, not the content pane. Use {@link #content(Node)} to add custom content to the inner content area.
	 * </p>
	 * 
	 * @return The VBox layout pane that serves as the root container
	 * 
	 * @see #content(Node)
	 * @see #paddingDefault(boolean)
	 */
	public VBox getLayoutPane() {
		return layoutPane;
	}

	/**
	 * Returns the inner content container where the main content is placed.
	 * <p>
	 * This is the {@link StackPane} that holds either the default {@link TextFlow} or custom content nodes added via {@link #content(Node)}.
	 * </p>
	 * 
	 * @return The inner content container (StackPane)
	 * 
	 * @see #content(Node)
	 * @see #paddingDefault(boolean)
	 */
	public StackPane getContentContainer() {
		return contentPane;
	}

	/**
	 * Replaces the current content of the popup and explicitly defines its focus behavior.
	 * <p>
	 * The node will be placed inside the inner content container (StackPane), replacing any existing content.
	 * </p>
	 * <p>
	 * <b>Architectural Note:</b> In JavaFX, setting {@code focusTraversable} to {@code true} on a popup's root container can cause it to "steal" focus from the owner control
	 * (e.g., a TextField) immediately upon showing. For "Live-Search" patterns, it is recommended to set this to {@code false} initially and only enable it when explicit user
	 * navigation (like Arrow keys) begins.
	 * </p>
	 * 
	 * @param content          The custom Node to display, or {@code null} to clear content.
	 * @param focusTraversable If {@code true}, the content can receive keyboard focus. If {@code false}, focus remains with the owner control.
	 * @return This CustomPopupControl instance for method chaining.
	 * 
	 * @see #paddingDefault(boolean)
	 */
	public CustomPopupControl content(Node content, boolean focusTraversable) {
		if (content != null) {
			content.setFocusTraversable(focusTraversable);
			this.contentPane.getChildren().setAll(content);
		} else {
			this.contentPane.getChildren().clear();
		}
		return this;
	}

	/**
	 * Overloaded convenience method that sets the content with {@code focusTraversable} set to {@code false}. Ideal for non-interactive or live-search popups.
	 * 
	 * @param content The custom Node to display.
	 * @return This instance for method chaining.
	 */
	public CustomPopupControl content(Node content) {
		return content(content, false);
	}

	/**
	 * Determines whether the popup has any visible content to display.
	 * 
	 * <p>
	 * Content is considered present if either:
	 * <ul>
	 * <li>The text property contains non-blank content</li>
	 * <li>The content pane contains custom nodes (not just the default empty TextFlow)</li>
	 * </ul>
	 * 
	 * @return {@code true} if the popup has content to show, {@code false} otherwise
	 */
	private boolean hasContent() {
		// Check for non-blank text content
		if (StringUtils.isNotBlank(getText())) {
			return true;
		}

		// Check for custom content nodes in the content pane
		if (contentPane != null && !contentPane.getChildren().isEmpty()) {
			// Custom content is present if either:
			// 1. There are multiple children, or
			// 2. The single child is not the default empty TextFlow
			if (contentPane.getChildren().size() > 1 || contentPane.getChildren().get(0) != this.contentTextFlow) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Displays the tooltip at a specified screen position.
	 */
	public void show(Node anchor, double screenX, double screenY) {
		if (anchor == null || anchor.getScene() == null || !hasContent()) {
			return;
		}

		this.lastMouseX = screenX;
		this.lastMouseY = screenY;

		fadeOut.stop();
		fadeIn.stop();

		if (!isShowing()) {
			shadowWrapper.setOpacity(0.0);

			// Temporär anzeigen bei (0,0)
			show(anchor.getScene().getWindow(), 0, 0);

			// Window-Listener für Verschiebung
			setupWindowListeners(anchor);

			// Positionieren und einblenden
			Platform.runLater(() -> {
				if (isShowing()) {
					sizeToScene();
					reposition(screenX, screenY);
					fadeIn.playFromStart();
				}
			});
		} else {
			reposition(screenX, screenY);
			shadowWrapper.setOpacity(1.0);
		}
	}

	/**
	 * Shows the popup with explicit control over the initial position.
	 */
	public void showAtPosition(Node anchor, double screenX, double screenY) {
		if (anchor == null || anchor.getScene() == null || !hasContent()) {
			return;
		}

		this.lastMouseX = screenX;
		this.lastMouseY = screenY;

		fadeOut.stop();
		fadeIn.stop();

		if (!isShowing()) {
			shadowWrapper.setOpacity(0.0);

			// Größe und Position VOR dem Anzeigen berechnen
			sizeToScene();
			Point2D pos = calculatePosition(screenX, screenY, getWidth(), getHeight());

			// Direkt an korrekter Position anzeigen
			show(anchor.getScene().getWindow(), pos.x(), pos.y());

			// Window-Listener für Verschiebung
			setupWindowListeners(anchor);

			fadeIn.playFromStart();
		} else {
			reposition(screenX, screenY);
			shadowWrapper.setOpacity(1.0);
		}
	}

	private void setupWindowListeners(Node anchor) {
		Window window = anchor.getScene().getWindow();

		// Prüfen ob Listener bereits für dieses Fenster existieren
		if (windowXListener != null && windowYListener != null && currentWindow == window) {
			return;
		}

		// Alte Listener entfernen
		if (currentWindow != null && windowXListener != null) {
			currentWindow.xProperty().removeListener(windowXListener);
			currentWindow.yProperty().removeListener(windowYListener);
		}

		// Neue Listener erstellen
		windowXListener = (obs, oldVal, newVal) -> hide();
		windowYListener = (obs, oldVal, newVal) -> hide();

		window.xProperty().addListener(windowXListener);
		window.yProperty().addListener(windowYListener);
		currentWindow = window;
	}

	@Override
	public void hide() {
		fadeIn.stop();
		// Window-Listener NICHT entfernen - sie bleiben fürs nächste Mal
		// Nur in dispose() entfernen!
		super.hide();
	}

	/**
	 * Initiates a graceful fade-out animation to hide the popup. This method respects the configured fade-out animation style.
	 * 
	 * <p>
	 * <b>Note:</b> The popup will be completely hidden only after the fade-out animation completes.
	 * </p>
	 */
	public void hideFaded() {
		fadeIn.stop();
		if (isShowing()) {
			fadeOut.playFromStart();
		}
	}

	/**
	 * Immediately stops any running fade animations and resets opacity to fully visible. This method is useful for preventing animation race conditions or for immediately
	 * hiding/showing content without animation.
	 */
	public void cancelAnimations() {
		fadeIn.stop();
		fadeOut.stop();
		layoutPane.setOpacity(1.0);
	}

	/**
	 * Displays the popup once with automatic hiding after the specified duration. This method is designed for temporary, one-time notifications.
	 * 
	 * @param duration The total time the popup should remain visible (including fade-out animation time)
	 */
	private void showOnce(Duration duration) {
		if (owner == null || owner.getScene() == null || owner.getScene().getWindow() == null) {
			Log.warn("Cannot show popup: owner or owner's window is not available");
			return;
		}

		if (!hasContent()) {
			Log.debug("Skipping popup display: no content to show");
			return;
		}

		detachHandlers();

		// Reset wrapper opacity for fade-in animation
		shadowWrapper.setOpacity(0.0);

		if (!isShowing()) {
			// Show at temporary position (will be repositioned)
			show(owner.getScene().getWindow(), 0, 0);
		}

		Platform.runLater(() -> {
			if (!isShowing()) {
				return;
			}

			sizeToScene();

			// Calculate final position based on owner bounds
			final Bounds bounds = owner.localToScreen(owner.getBoundsInLocal());
			double finalX = bounds.getMinX();
			double finalY = bounds.getMaxY() + DEFAULT_VERTICAL_OFFSET;

			Point2D adjusted = getPositionWithinScreenBounds(finalX, finalY, getWidth(), getHeight());

			// Adjust position of already-visible popup
			setX(adjusted.x());
			setY(adjusted.y());

			// Start fade-in animation
			fadeIn.playFromStart();

			// Set up automatic hide timer
			PauseTransition hideTimer = new PauseTransition(duration);

			// Ensure proper cleanup after fade-out completes
			fadeOut.setOnFinished(e -> dispose());
			hideTimer.setOnFinished(e -> hideFaded());

			hideTimer.play();
		});
	}

	// --- Private Helper Methods ---

	/**
	 * Represents a 2D point with double precision coordinates. Used for screen position calculations.
	 * 
	 * @param x The X coordinate
	 * @param y The Y coordinate
	 */
	private record Point2D(double x, double y) {
		/**
		 * Returns a string representation of this point.
		 * 
		 * @return String in format "Point2D[x=value, y=value]"
		 */
		@Override
		public String toString() {
			return String.format("Point2D[x=%.1f, y=%.1f]", x, y);
		}
	}

	/**
	 * Adjusts the requested position to ensure the popup stays within screen bounds.
	 * 
	 * <p>
	 * This method prevents the popup from being positioned outside the visible screen area by applying appropriate margins and adjustments.
	 * </p>
	 * 
	 * @param requestedX  The requested X coordinate
	 * @param requestedY  The requested Y coordinate
	 * @param popupWidth  The width of the popup
	 * @param popupHeight The height of the popup
	 * @return A {@link Point2D} with screen-safe coordinates
	 */
	private Point2D getPositionWithinScreenBounds(double requestedX, double requestedY, double popupWidth, double popupHeight) {

		Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
		double adjustedX = requestedX;
		double adjustedY = requestedY;

		// Prevent popup from extending beyond right screen edge
		if (adjustedX + popupWidth > screenBounds.getMaxX()) {
			adjustedX = screenBounds.getMaxX() - popupWidth - DEFAULT_SCREEN_MARGIN;
		}

		// Prevent popup from extending beyond left screen edge
		if (adjustedX < screenBounds.getMinX()) {
			adjustedX = screenBounds.getMinX() + DEFAULT_SCREEN_MARGIN;
		}

		// Prevent popup from extending beyond bottom screen edge
		if (adjustedY + popupHeight > screenBounds.getMaxY()) {
			adjustedY = screenBounds.getMaxY() - popupHeight - DEFAULT_SCREEN_MARGIN;
		}

		// Prevent popup from extending beyond top screen edge
		if (adjustedY < screenBounds.getMinY()) {
			adjustedY = screenBounds.getMinY() + DEFAULT_SCREEN_MARGIN;
		}

		return new Point2D(adjustedX, adjustedY);
	}

	/**
	 * Returns the current positioning strategy for this Popup.
	 * 
	 * @return The current {@link Position} enum value
	 */
	public Position getPosition() {
		return this.position;
	}

	/**
	 * Recalculates AND applies the popup's position on screen. Intended for lightweight repositioning without resizing.
	 */
	public void reposition(double screenX, double screenY) {
		if (!isShowing())
			return;

		// This method now calculates and then immediately applies the position.
		Point2D newPosition = calculatePosition(screenX, screenY, getWidth(), getHeight());
		setX(newPosition.x());
		setY(newPosition.y());
	}

	/**
	 * Calculates the optimal screen position for the popup based on the current positioning strategy.
	 * 
	 * <p>
	 * This method considers:
	 * <ul>
	 * <li>The specified screen coordinates</li>
	 * <li>The owner component's bounds</li>
	 * <li>The current {@link Position} strategy</li>
	 * <li>The popup's target dimensions</li>
	 * </ul>
	 * 
	 * @param screenX      The X-coordinate reference point (screen space)
	 * @param screenY      The Y-coordinate reference point (screen space)
	 * @param targetWidth  The desired width of the popup
	 * @param targetHeight The desired height of the popup
	 * @return A {@link Point2D} containing the calculated (x, y) screen coordinates
	 */
	private Point2D calculatePosition(double screenX, double screenY, double targetWidth, double targetHeight) {
		this.lastMouseX = screenX;
		this.lastMouseY = screenY;

		double finalX, finalY;
		final Bounds ownerBounds = (owner != null) ? owner.localToScreen(owner.getBoundsInLocal()) : new javafx.geometry.BoundingBox(screenX, screenY, 0, 0);

		switch (position) {
		case BELOW:
			finalX = ownerBounds.getMinX() - DEFAULT_HORIZONTAL_OFFSET;
			finalY = ownerBounds.getMaxY() + DEFAULT_VERTICAL_OFFSET - DEFAULT_TOP_COMPENSATION;
			break;

		case ABOVE:
			finalX = ownerBounds.getMinX() - DEFAULT_HORIZONTAL_OFFSET;
			finalY = ownerBounds.getMinY() - targetHeight - DEFAULT_VERTICAL_OFFSET + DEFAULT_BOTTOM_PADDING - 4;
			break;

		case LEFT:
			finalX = ownerBounds.getMinX() - targetWidth - DEFAULT_HORIZONTAL_OFFSET;
			finalY = ownerBounds.getMinY() - DEFAULT_VERTICAL_OFFSET;
			break;

		case RIGHT:
			finalX = ownerBounds.getMaxX() + DEFAULT_HORIZONTAL_OFFSET;
			finalY = ownerBounds.getMinY() - DEFAULT_VERTICAL_OFFSET;
			break;

		case BELOW_ALIGN_RIGHT:
			finalX = ownerBounds.getMaxX() - targetWidth + DEFAULT_HORIZONTAL_OFFSET;
			finalY = ownerBounds.getMaxY() + DEFAULT_VERTICAL_OFFSET - DEFAULT_TOP_COMPENSATION;
			break;

		case CURSOR_BOTTOM_RIGHT:
			finalX = screenX + DEFAULT_CURSOR_OFFSET_X;
			finalY = screenY + DEFAULT_CURSOR_OFFSET_Y;
			break;

		default:
			finalX = screenX;
			finalY = screenY;
			Log.warn("Unknown position strategy: {}. Using direct coordinates.", position);
			break;
		}

		return getPositionWithinScreenBounds(finalX, finalY, targetWidth, targetHeight);
	}

	/**
	 * Removes all mouse event handlers from the owner node. This is typically called when cleaning up temporary or one-time popups.
	 * 
	 * <p>
	 * <b>Note:</b> This method should be used with caution for persistent tooltips as it will disable all hover behavior.
	 * </p>
	 */
	private void detachHandlers() {
		owner.setOnMouseEntered(null);
		owner.setOnMouseExited(null);
		owner.setOnMouseMoved(null);
	}

	/**
	 * Completely disposes this popup, removing all listeners and resources. After disposal, the popup should not be used.
	 */
	public void dispose() {
		Log.debug("Disposing popup");

		// 1. Stop all animations and timers
		delayTimer.stop();
		stayTimer.stop();
		fadeIn.stop();
		fadeOut.stop();

		// 2. Remove window move listeners (prevent memory leaks)
		if (currentWindow != null && windowXListener != null) {
			currentWindow.xProperty().removeListener(windowXListener);
			currentWindow.yProperty().removeListener(windowYListener);
			windowXListener = null;
			windowYListener = null;
			currentWindow = null;
		}

		// 3. Remove focus listener from owner
		if (owner != null && ownerFocusListener != null) {
			owner.focusedProperty().removeListener(ownerFocusListener);
		}

		// 4. Remove all mouse handlers from owner
		if (owner != null) {
			owner.setOnMouseEntered(null);
			owner.setOnMouseExited(null);
			owner.setOnMouseMoved(null);
		}

		// 5. Hide popup if still showing
		if (isShowing()) {
			hide();
		}

		Log.debug("Popup disposed");
	}
}