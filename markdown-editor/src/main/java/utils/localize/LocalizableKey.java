package utils.localize;


import java.util.Arrays;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Labeled;
import utils.logging.Log;

/**
 * An interface for objects that represent a localizable string key.
 * <p>
 * This provides a contract for any key set (typically an enum) to be used with the application's localization system (e.g., {@link LangMap}). By implementing this interface, a key
 * set gains access to default methods for retrieving and formatting localized strings, ensuring consistent behavior across the application.
 */
public interface LocalizableKey {

	/**
	 * Gets the raw string key used for lookup in a resource bundle. This key should be unique within its context.
	 *
	 * @return The resource bundle key (e.g., "dialog.title.new_link").
	 */
	String getKey();

	/**
	 * Gets the default fallback text, typically in English. This text is used if the key cannot be found in the user's language bundle, or as a fallback if a formatting error
	 * occurs.
	 *
	 * @return The default fallback text.
	 */
	String getDefaultText();

	/**
	 * Retrieves the raw, unformatted localized string for this key from the central {@link LangMap}. If you need to insert arguments into the string, use
	 * {@link #getFormatted(Object...)} instead.
	 *
	 * @return The localized string. If not found, a fallback is typically used by LangMap.
	 */
	default String get() {
		return LangMap.getLangString(this);
	}

	/**
	 * Retrieves the localized string for this key and formats it with the given arguments. This method uses {@link String#format(String, Object...)} to insert the arguments.
	 * <p>
	 * <b>Example:</b>
	 * 
	 * <pre>{@code
	 * // Assuming a key with pattern: "Found %d files."
	 * String message = MyLangKey.FILES_FOUND.getFormatted(5);
	 * // message becomes "Found 5 files." (or its translation)
	 * }</pre>
	 * <p>
	 * <b>Error Handling:</b> If a formatting error occurs, a warning is logged, and the method falls back to formatting the {@link #getDefaultText()} as a safeguard.
	 *
	 * @param args The arguments to be inserted into the localized string pattern.
	 * @return The formatted, localized string.
	 */
	default String getFormatted(Object... args) {
		if (args == null || args.length == 0) {
			return get();
		}
		String pattern = get();
		try {
			return String.format(pattern, args);
		} catch (java.util.IllegalFormatException e) {
			// LÖSUNG: Baue die gewünschten Infos manuell in die Log-Nachricht ein.
			// Wir rufen die Log-Methode OHNE das 'e'-Objekt als erstes Argument auf.
			Log.warn("Formatting failed for LangKey '%s'. Error: %s - \"%s\". Pattern: '%s', Args: %s", getKey(), e.getClass().getSimpleName(), // <-- Der explizite Exception-Name
			        e.getMessage(), // <-- Die kurze Message
			        pattern, Arrays.toString(args));
			// Der Fallback bleibt derselbe, er ist perfekt.
			return String.format(getDefaultText(), args);
		}
	}

	/**
	 * Retrieves a {@link StringProperty} for this key from the central {@link LangMap}. This is useful for data binding in JavaFX, allowing UI components to automatically update
	 * when the application's language changes.
	 *
	 * @return A string property that reflects the current localized value of this key.
	 */
	default StringProperty property() {
		return LangMap.getLangProperty(this);
	}

	/**
	 * Binds the text property of a {@link Labeled} component (like a Button or Label) to the localized string property of this key.
	 *
	 * @param component The Labeled component whose text should be bound.
	 * @return The same component, for method chaining.
	 */
	default <T extends Labeled> T bindTo(T component) {
		if (component != null) {
			component.textProperty().bind(property());
		}
		return component;
	}
}