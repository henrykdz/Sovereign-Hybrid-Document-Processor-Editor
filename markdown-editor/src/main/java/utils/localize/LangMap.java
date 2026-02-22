package utils.localize;


import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Control;
import javafx.scene.control.Labeled;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import utils.general.StringUtils;
import utils.localize.LanguagePresets.LanguageInfo;
import utils.logging.Log;

/**
 * A centralized, reactive manager for all localization aspects of the application. This singleton-style utility class is the single source of truth for language management and
 * provides robust, thread-safe mechanisms for UI localization.
 *
 * <h3>Core Responsibilities:</h3>
 * <ul>
 * <li><b>Resource Bundle Management:</b> Loads and manages the application's language-specific {@link ResourceBundle} files.</li>
 * <li><b>Reactive UI Binding:</b> Provides {@link StringProperty} instances that automatically update when the application language changes, simplifying UI code.</li>
 * <li><b>Robust Fallback System:</b> Implements a multi-level fallback for missing translations: Active Language -> System Default Language -> English -> Hardcoded Default Text
 * (from {@link LocalizableKey}).</li>
 * <li><b>JVM Locale Control:</b> Manages the JVM's default locale via {@link Locale#setDefault(Locale)}, ensuring that internal JavaFX system components (e.g.,
 * {@code ColorPicker}, {@code FileChooser}) also reflect the selected language.</li>
 * <li><b>Performance Caching:</b> Caches all created {@code StringProperty} objects to ensure performance and stable UI bindings across the application.</li>
 * </ul>
 *
 * <h3>Usage Guide:</h3>
 * <p>
 * <strong>1. Initialization (Crucial):</strong> The entire system MUST be initialized once at application startup by calling {@link #init()}. A failure to do so will result in an
 * {@code IllegalStateException}.
 * </p>
 * <p>
 * <strong>2. UI Binding (Recommended):</strong> The preferred way to localize UI components is by using a {@link LocalizableKey} enum. This provides type-safety and a guaranteed
 * fallback text.
 * </p>
 * 
 * <pre>{@code
 * // In a controller's initialize() method:
 * myLabel.textProperty().bind(PrefLangKey.MENU_GENERAL.property());
 * myTooltip.textProperty().bind(PrefLangKey.LANG_PARTIALLY_SUPPORTED_TIP.property());
 * }</pre>
 * <p>
 * <strong>3. Changing the Language:</strong> To change the language from the UI (e.g., from a settings dialog), call {@link #setUserLanguage(Locale)}. Passing {@code null} will
 * revert the application to the system's default language.
 * </p>
 *
 * @see LocalizableKey for the contract of type-safe localization keys.
 * @see LanguagePresets for the master list of all supported languages.
 */
public final class LangMap {

	// --- Constants ---
	/**
	 * The single source of truth for all languages this application is intended to support.
	 * <p>
	 * A locale code should only be added to this list if a corresponding translation resource bundle (e.g., {@code lang/lang_de-DE.properties}) has been created by the developers
	 * and is considered ready for use.
	 * <p>
	 * During application startup, this master list is filtered against the languages supported by the JavaFX runtime to produce the final, consistent list of languages offered to
	 * the user in the UI.
	 */
	private static final String[] implementedLocaleCodes = { "bs-BA", "de-DE", "da-DK", "en-GB", "en-US", "es-ES", "fi-FI", "fr-FR", "hr-HR", "hu-HU", "it-IT", "pl-PL", "ru-RU",
	        "sv-SE" };

	private static final String MISSING_RESOURCE = "Missing Value";
	private static final String INVALID_KEY      = "InvalidKey";

	// --- State ---
	private static boolean isInitialized = false;

	// Immutable system and fallback bundles
	private static Locale         systemLocale;
	private static ResourceBundle bundleSystem;
	private static ResourceBundle bundleEnglishInternational;

	/**
	 * Represents the language explicitly chosen by the user. This property is the main driver for language changes in the application.
	 * <p>
	 * A {@code null} value signifies that the user wishes to revert to the system's default language detected at startup.
	 * <p>
	 * It is set via the public API method {@link #setUserLanguage(Locale)}. Listeners on this property trigger the loading of the appropriate {@link ResourceBundle} and update the
	 * {@link #activeBundle}.
	 */
	private static final SimpleObjectProperty<Locale> userSelectedLocale = new SimpleObjectProperty<>(null);

	/**
	 * Holds the {@link ResourceBundle} that is currently active for the entire application. This property is derived from the state of {@link #userSelectedLocale} and the
	 * initially detected system locale.
	 * <p>
	 * All localization lookups via {@code getLangString} methods use the bundle contained within this property. It is exposed externally as a {@link ReadOnlyObjectProperty} via
	 * {@link #activeBundleProperty()} to allow other parts of the application to react to language changes without being able to modify the state directly.
	 */
	private static final ReadOnlyObjectWrapper<ResourceBundle> activeBundle = new ReadOnlyObjectWrapper<>();

	/**
	 * A thread-safe cache for reactive {@link StringProperty} instances created via a simple {@code String} key. This cache ensures that for any given string key, only one
	 * {@code StringProperty} object is ever created, allowing for stable UI bindings.
	 * <p>
	 * This map is used by the {@link #getLangProperty(String)} method. For keys originating from a {@link LocalizableKey}, the more robust {@link #locKeyPropertyMap} is used. A
	 * {@link ConcurrentHashMap} is used to ensure thread-safety during lazy initialization.
	 */
	private static final Map<String, StringProperty> langPropertyMap = new ConcurrentHashMap<>(200);

	/**
	 * A thread-safe cache for reactive {@link StringProperty} instances that were created from a {@link LocalizableKey}.
	 * <p>
	 * Unlike {@link #langPropertyMap}, this map stores the entire {@code LocalizableKey} object. This is crucial because it preserves the default text associated with the key. By
	 * retaining the {@code LocalizableKey}, the {@link #updateAllLangProperties()} method can correctly apply the default text as a fallback if a translation is missing after a
	 * language change, preventing the UI from showing "Missing Value".
	 */
	private static final Map<LocalizableKey, StringProperty> locKeyPropertyMap = new ConcurrentHashMap<>(50);

	/**
	 * An observable list containing all {@link LanguageInfo} objects for the languages officially supported and translated by the application.
	 * <p>
	 * This list is populated once during initialization by the {@link #loadSupportedLanguages()} method. It is intended to be used as the data model for UI controls that allow the
	 * user to select a language, such as a {@code ChoiceBox} or {@code ListView}.
	 */
	private static final ObservableList<LanguageInfo> supportedLanguages = FXCollections.observableArrayList();

	/**
	 * A simple set used as a memory to prevent logging the same 'missing key' warning multiple times. Without this, a missing key requested in an update loop or by multiple UI
	 * components would spam the log file, making it difficult to read.
	 * <p>
	 * The {@link #logMissingKeyOnce(String, Locale)} method checks this set before logging.
	 */
	private static final Set<String> alreadyLoggedMissingKeys = new HashSet<>();

	/**
	 * Gets the original system default locale that was detected at application startup. This value does not change during the application's lifecycle. It is useful for UI
	 * components that need to display what the system's default is.
	 *
	 * @return The system's default Locale.
	 */
	public static Locale getSystemDefaultLocale() {
		checkInitialized();
		return systemLocale;
	}

	/**
	 * Returns a read-only property of the currently active ResourceBundle. This is the official way to listen for language changes programmatically. The listener will be notified
	 * whenever the effective language of the application changes.
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>{@code
	 * LangMap.activeBundleProperty().addListener((obs, oldBundle, newBundle) -> {
	 * 	// Code to re-initialize a component, e.g., a ChoiceBox with a new StringConverter
	 * 	reinitializeMyComponent();
	 * });
	 * }</pre>
	 *
	 * @return A read-only property wrapper around the active ResourceBundle.
	 */
	public static ReadOnlyObjectProperty<ResourceBundle> activeBundleProperty() {
		checkInitialized();
		return activeBundle.getReadOnlyProperty();
	}

	/**
	 * Private constructor to prevent instantiation.
	 */
	private LangMap() {
		throw new IllegalStateException("Utility class");
	}

	/**
     * Initializes the entire language system. This method must be called once at application startup (e.g., in the Launcher).
     * This version is robust against missing non-English bundles.
     * @throws IllegalStateException if a critical English fallback bundle cannot be found.
     */
    public static void init() {
        if (isInitialized) {
            Log.warn("LangMap.init() called more than once.");
            return;
        }

        Log.info("Initializing Language System...");

        try {
            // 1. GUARANTEE ENGLISH FALLBACK (ULTIMATE SAFETY NET)
            // This *must* succeed. If it fails, an IllegalStateException is thrown from loadEnglishBundle().
            loadEnglishBundle();

            // 2. Determine System Locale and attempt to load its bundle.
            systemLocale = Locale.getDefault();
            ResourceBundle tempSystemBundle = tryLoadBundle(systemLocale);

            if (tempSystemBundle != null) {
                bundleSystem = tempSystemBundle;
                Log.fine("System Language detected: %s. Using its bundle: %s", systemLocale, bundleSystem.getLocale());
            } else {
                // If system-specific bundle isn't found, default to the English fallback for system operations.
                Log.warn("No specific bundle found for system locale %s. Using English fallback for system defaults.", systemLocale);
                bundleSystem = bundleEnglishInternational;
            }

            // 3. Set up listeners (these should not throw critical errors if bundles are valid)
            setupStateListeners();

            // 4. Load the list of supported languages (this just populates a list, should not fail critically)
            loadSupportedLanguages();

            // 5. Set the initial active bundle (will be either system's or English fallback)
            activeBundle.set(bundleSystem);

            // Mark as initialized *before* logging success, to ensure checkInitialized() passes.
            isInitialized = true;
            Log.info("Language System initialized successfully. Initial active locale: %s", getActiveLocale());

        } catch (IllegalStateException e) {
            // Re-throw critical IllegalStateExceptions (e.g., from loadEnglishBundle)
            throw e;
        } catch (Exception e) {
            // Catch any other unexpected exceptions during setup AFTER English fallback is guaranteed.
            // Log as severe, but attempt to allow the application to start in a degraded localization state.
            Log.severe(e, "CRITICAL: Language System encountered an unexpected error during initialization after English fallback was secured. " +
                    "Application may proceed with limited localization features. Check logs for details.");

            // Ensure activeBundle is set to *something* to prevent later NullPointerExceptions.
            if (activeBundle.get() == null) {
                activeBundle.set(bundleEnglishInternational);
            }
            isInitialized = true; // Mark as initialized to allow the app to run.
        }
    }

	/**
     * Versucht, ein ResourceBundle für das gegebene Locale zu laden.
     * Loggt eine feine Nachricht, wenn das Bundle nicht gefunden wird, und gibt null zurück.
     * Im Fehlerfall wird keine Exception geworfen.
     * @param locale Das zu ladende Locale.
     * @return Das geladene ResourceBundle, oder null wenn es nicht gefunden oder geladen werden konnte.
     */
    private static ResourceBundle tryLoadBundle(Locale locale) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(getLanguageBundlePath(), locale);
            Log.fine("Successfully loaded bundle for locale: %s (Bundle Locale: %s)", locale, bundle.getLocale());
            return bundle;
        } catch (MissingResourceException e) {
            Log.fine("Resource bundle for locale %s not found under base name '%s'.", locale, getLanguageBundlePath());
            return null; // Kein Bundle gefunden, kein kritischer Fehler
        } catch (Exception e) {
            Log.warn(e, "Unexpected error loading bundle for locale %s.", locale);
            return null; // Unerwarteter Fehler, kein kritisches Abstürzen
        }
    }
	/**
	 * Sets up the listeners that form the reactive core of the language system.
	 */
	private static void setupStateListeners() {
		// Listener 1: Reacts when the user selects a language.
		userSelectedLocale.addListener((obs, oldLocale, newLocale) -> {

			// =======================================================================
			// HIER DIE ERGÄNZUNG: JVM-Standard-Locale für ColorPicker etc. aktualisieren
			updateJvmDefaultLocale(newLocale);
			// =======================================================================

			if (newLocale == null) {
				// User has chosen "use system default" -> set the system bundle as active.
				Log.info("User reverted to system language.");
				activeBundle.set(bundleSystem);
			} else {
				// User has chosen a specific language -> attempt to load that bundle.
				Log.info("User selected language: %s. Attempting to load bundle.", newLocale);
				try {
					ResourceBundle userBundle = ResourceBundle.getBundle(getLanguageBundlePath(), newLocale);
					if (!userBundle.getLocale().equals(newLocale)) {
						Log.warn("ResourceBundle Fallback: Expected %s, but got %s. Using the provided fallback bundle.", newLocale, userBundle.getLocale());
					}
					activeBundle.set(userBundle);
				} catch (Exception e) {
					Log.error(e, "Failed to load bundle for user selected locale %s. Falling back to English.", newLocale);
					activeBundle.set(bundleEnglishInternational);
				}
			}
		});

		// Listener 2: Reacts when the active bundle changes and updates all cached UI properties.
		activeBundle.addListener((obs, oldBundle, newBundle) -> {
			if (newBundle == null) {
				Log.error("Active language bundle became null! This should not happen.");
				return;
			}
			Log.fine("Active language bundle changed to: %s. Updating all cached UI properties.", newBundle.getLocale());
			updateAllLangProperties();
		});
	}

	/**
     * Lädt das englische ResourceBundle, das als finaler Fallback dient.
     * Diese Methode ist kritisch und wirft eine IllegalStateException, wenn kein englisches Bundle gefunden werden kann.
     * @throws IllegalStateException wenn kein englisches Bundle gefunden werden kann.
     */
    private static void loadEnglishBundle() {
        Log.fine("Attempting to load English fallback bundle (en-GB)...");
        try {
            bundleEnglishInternational = ResourceBundle.getBundle(getLanguageBundlePath(), Locale.forLanguageTag("en-GB"));
        } catch (MissingResourceException e) {
            Log.warn("Could not find en-GB bundle. Attempting to fall back to en-US...");
            try {
                bundleEnglishInternational = ResourceBundle.getBundle(getLanguageBundlePath(), Locale.forLanguageTag("en-US"));
            } catch (MissingResourceException e2) {
                Log.error(e2, "CRITICAL: Could not find ANY English fallback bundle (en-GB, en-US) under base name '%s'. Application cannot run without base localization.", getLanguageBundlePath());
                throw new IllegalStateException("Failed to load English fallback bundles. Check resource path and file existence.", e2);
            } catch (Exception e2) {
                Log.error(e2, "CRITICAL: Unexpected error loading en-US bundle. Application cannot run without base localization.");
                throw new IllegalStateException("Unexpected error during en-US bundle loading.", e2);
            }
        } catch (Exception e) {
            Log.error(e, "CRITICAL: Unexpected error loading en-GB bundle. Application cannot run without base localization.");
            throw new IllegalStateException("Unexpected error during en-GB bundle loading.", e);
        }
        Log.fine("English fallback bundle loaded: %s", bundleEnglishInternational.getLocale());
    }

	/**
	 * Populates the list of languages the application officially supports.
	 */
//	private static void loadSupportedLanguagesOld() {
//		String[] localeCodes = { "bs-BA", "de-DE", "da-DK", "en-GB", "en-US", "es-ES", "fi-FI", "fr-FR", "hr-HR", "hu-HU", "it-IT", "pl-PL", "ru-RU", "sv-SE" };
//		supportedLanguages.clear();
//		for (String localeCode : localeCodes) {
//			supportedLanguages.add(LanguagePresets.getLanguageInfo(localeCode));
//		}
//		Log.fine("Loaded %d supported languages.", supportedLanguages.size());
//	}

	/**
	 * Populates the list of languages the application officially supports. It uses the list of all languages for which this application provides translation files. This ensures
	 * that all developer translation work is available to the user, accepting that some system dialogs might fall back to English if not supported by the JVM.
	 */
	private static void loadSupportedLanguages() {
		supportedLanguages.clear();
		Log.info("Loading all %d implemented languages for user selection...", implementedLocaleCodes.length);

		// Iteriere über die Liste IHRER implementierten Sprachen
		for (String localeCode : implementedLocaleCodes) {
			try {
				// Hole die Metadaten für diesen Locale-Code aus Ihren Presets
				LanguageInfo langInfo = LanguagePresets.getLanguageInfo(localeCode);

				// FÜGE JEDE SPRACHE OHNE FILTER HINZU!
				supportedLanguages.add(langInfo);

			} catch (IllegalArgumentException e) {
				Log.error(e, "The locale code '%s' from the implemented list was not found in LanguagePresets.", localeCode);
			}
		}
		Log.info("Initialization complete. %d languages will be offered to the user.", supportedLanguages.size());
	}

	private static void updateAllLangProperties() {
		// --- Teil 1: Update der String-basierten Properties (wie bisher) ---
		if (!langPropertyMap.isEmpty()) {
			Log.fine("Updating %d string-key-based language properties...", langPropertyMap.size());
			for (Map.Entry<String, StringProperty> entry : langPropertyMap.entrySet()) {
				String key = entry.getKey();
				StringProperty property = entry.getValue();
				// Hier ist der Fallback auf "Missing Resource" weiterhin korrekt,
				// da wir keinen besseren Default-Text haben.
				String newValue = getLangString(key);
				property.set(newValue);
			}
		}

		// --- Teil 2: Update der LocalizableKey-basierten Properties (DIE KORREKTUR) ---
		if (!locKeyPropertyMap.isEmpty()) {
			Log.fine("Updating %d LocalizableKey-based language properties...", locKeyPropertyMap.size());
			for (Map.Entry<LocalizableKey, StringProperty> entry : locKeyPropertyMap.entrySet()) {
				LocalizableKey locKey = entry.getKey();
				StringProperty property = entry.getValue();

				// Kein null-Check mehr nötig! getLangString(locKey) gibt garantiert non-null zurück.
				String newValue = getLangString(locKey);
				property.set(newValue);
			}
		}
		Log.fine("Finished updating language properties.");
	}

	// --- Public API ---

	/**
	 * Sets the user's preferred language. This is the main entry point for changing the language.
	 * 
	 * @param locale The locale to set, or null to revert to the system's default language.
	 */
	public static void setUserLanguage(Locale locale) {
		checkInitialized();
		// Setting this property will trigger the listener chain.
		userSelectedLocale.set(locale);
	}

	/**
	 * Gets the currently active, effective locale.
	 * 
	 * @return The locale currently used by the application.
	 */
	public static Locale getActiveLocale() {
		checkInitialized();
		// The active bundle might be null during a brief transition, handle this gracefully.
		ResourceBundle bundle = activeBundle.get();
		return (bundle != null) ? bundle.getLocale() : systemLocale;
	}

	/**
	 * Gets the locale explicitly chosen by the user.
	 * 
	 * @return The chosen locale, or null if the system default is currently active.
	 */
	public static Locale getUserSelectedLocale() {
		checkInitialized();
		return userSelectedLocale.get();
	}

	/**
	 * Gets the observable list of all languages supported by the application.
	 * 
	 * @return A list of LanguageInfo objects.
	 */
	public static ObservableList<LanguageInfo> getSupportedLanguages() {
		checkInitialized();
		return supportedLanguages;
	}

	/**
	 * Retrieves the localized string for a given key. This method provides a robust fallback mechanism (Active -> System -> English).
	 * 
	 * @param fieldKey The key for the desired string (e.g., "word.cancel").
	 * @return The localized string, or a placeholder if not found in any bundle.
	 */
	public static String getLangString(String fieldKey) {
		checkInitialized();
		if (fieldKey == null) {
			Log.warn("getLangString called with null key.");
			return MISSING_RESOURCE;
		}

		ResourceBundle currentActive = activeBundle.get();

		// Attempt 1: Active Bundle
		if (currentActive != null && currentActive.containsKey(fieldKey)) {
			return currentActive.getString(fieldKey);
		}

		// Attempt 2: System Bundle (if it's different from the active one)
		if (bundleSystem != null && bundleSystem != currentActive && bundleSystem.containsKey(fieldKey)) {
			logMissingKeyOnce(fieldKey, (currentActive != null) ? currentActive.getLocale() : systemLocale);
			return bundleSystem.getString(fieldKey);
		}

		// Attempt 3: English Fallback Bundle
		if (bundleEnglishInternational != null && bundleEnglishInternational.containsKey(fieldKey)) {
			logMissingKeyOnce(fieldKey, (currentActive != null) ? currentActive.getLocale() : systemLocale);
			return bundleEnglishInternational.getString(fieldKey);
		}

		// Not found in any bundle
		logMissingKeyOnce(fieldKey, (currentActive != null) ? currentActive.getLocale() : systemLocale);
		return MISSING_RESOURCE;
	}

	/**
	 * Retrieves the localized string for a given key, returning a custom default value ONLY if the key is not found in any resource bundle. This method is null-safe: if the
	 * provided defaultValue is null, it logs a warning and returns an empty string to prevent "null" from appearing in the UI.
	 *
	 * @param fieldKey     The key for the desired string.
	 * @param defaultValue The value to return if the key is not found.
	 * @return The localized string, the provided default value, or an empty string. Never null.
	 */
	public static String getLangString(String fieldKey, String defaultValue) {
		String result = getLangString(fieldKey);

		if (MISSING_RESOURCE.equals(result)) {
			// Der Schlüssel wurde in keiner .properties-Datei gefunden.
			// Jetzt prüfen wir den übergebenen Default-Wert.
			if (defaultValue != null) {
				// Ein gültiger Default-Wert wurde übergeben, also geben wir ihn zurück.
				return defaultValue;
			} else {
				// *** DER "BEST OF BOTH WORLDS"-ANSATZ ***
				// 1. Gib dem Entwickler einen lauten Hinweis im Log.
				Log.warn("No translation found for key '%s' and the provided default value was null. This might indicate a missing default text in a LocalizableKey enum.",
				        fieldKey);
				// 2. Gib dem Endbenutzer eine saubere UI, indem du einen leeren String zurückgibst.
				return "";
			}
		}

		// Der Schlüssel wurde gefunden, also geben wir das Ergebnis zurück (kann auch ein leerer String sein).
		return result;
	}

	/**
	 * Retrieves the localized string for the given LocalizableKey. This is the recommended method for getting static strings, as it robustly uses the key's default text as a final
	 * fallback if no translation is found.
	 * 
	 * @param locKey The LocalizableKey containing the key and default text.
	 * @return The localized string or the key's default text.
	 */
	public static String getLangString(LocalizableKey locKey) {
		if (locKey == null) {
			Log.warn("getLangString called with null LocalizableKey.");
			return MISSING_RESOURCE;
		}
		// Verwendet die robuste Überladung, die einen Default-Wert akzeptiert.
		return getLangString(locKey.getKey(), locKey.getDefaultText());
	}

	/**
	 * Retrieves the localized string for a given key and formats it with the provided arguments. This is a convenience method for `String.format(getLangString(key), args)`. It
	 * uses the key's default text as a fallback for the format string.
	 *
	 * @param locKey The LocalizableKey containing the key and the default format string.
	 * @param args   The arguments to be formatted into the string.
	 * @return The formatted, localized string.
	 */
	public static String getLangString(LocalizableKey locKey, Object... args) {
		if (locKey == null) {
			Log.warn("getLangString(LocalizableKey, args) called with null LocalizableKey.");
			return MISSING_RESOURCE;
		}
		// Hole den Format-String (mit Fallback auf den Default-Text des Keys)
		String formatString = getLangString(locKey);
		try {
			return String.format(formatString, args);
		} catch (java.util.IllegalFormatException e) {
			Log.error(e, "Formatting error for key '%s'. Format string: '%s'. Check placeholder count.", locKey.getKey(), formatString);
			return formatString; // Gib den unformatierten String als Fallback zurück
		}
	}

	/**
	 * Gets a reactive StringProperty for a given key. The property will automatically update its value when the application language changes.
	 * 
	 * @param fieldKey The key for the desired string.
	 * @return A StringProperty that can be bound to UI components.
	 */
	public static StringProperty getLangProperty(String fieldKey) {
		// Calls the extended method without a default value
		return getLangProperty(fieldKey, null);
	}

	/**
	 * Gets a reactive, cached StringProperty for a given {@link LocalizableKey}.
	 * <p>
	 * This is the <strong>recommended method</strong> for binding UI components (like Labels, Buttons, or Tooltips) to localized text. The returned property offers two key
	 * features:
	 * <ol>
	 * <li><b>Reactivity:</b> It automatically updates its value whenever the application's language is changed via {@link #setUserLanguage(Locale)}.</li>
	 * <li><b>Robust Fallback:</b> It uses the default text provided by the {@code LocalizableKey} itself as a fallback. This applies both during initial creation and during
	 * subsequent language updates. This guarantees that the UI will never display a placeholder like "Missing Value" for keys managed via this method.</li>
	 * </ol>
	 * For performance, properties are cached in a dedicated map. Subsequent calls with the same {@code LocalizableKey} instance will return the identical {@code StringProperty}
	 * object, which is essential for stable UI bindings.
	 *
	 * @param locKey The {@code LocalizableKey} containing the string key and the essential default text fallback. Must not be {@code null}.
	 * @return A non-null, reactive {@code StringProperty} that can be safely bound to any JavaFX UI component property.
	 */
	public static StringProperty getLangProperty(LocalizableKey locKey) {
		if (locKey == null) {
			Log.warn("getLangProperty called with a null LocalizableKey.");
			return new SimpleStringProperty(INVALID_KEY);
		}
		return locKeyPropertyMap.computeIfAbsent(locKey, key -> {
			// Kein null-Check mehr nötig! getLangString(key) gibt garantiert non-null zurück.
			String initialValue = getLangString(key);
			return new SimpleStringProperty(initialValue);
		});
	}

	// --- Binding Helper Methods ---

	/**
	 * Binds the given {@link StringProperty} of a UI control to a localized string provided by this localization manager.
	 * <p>
	 * This method is a convenience for binding text properties directly via language keys, typically used in JavaFX controllers.
	 *
	 * @param uiProperty The {@link StringProperty} of the UI control to be bound. Must not be {@code null}.
	 * @param langKey    The language key to retrieve the corresponding localized text. Must not be {@code null} or blank.
	 */
	public static void bindTextProperty(StringProperty uiProperty, String langKey) {
		checkInitialized();
		if (uiProperty == null || StringUtils.isBlank(langKey))
			return;
		uiProperty.bind(getLangProperty(langKey));
	}

	/**
	 * Binds a UI StringProperty to a localized string property, concatenated with a suffix. This is a convenience method for cleaner code in controllers.
	 * 
	 * @param uiProperty The property of the UI control to bind.
	 * @param langKey    The language key for the localized text.
	 * @param suffix     The string to append to the localized text.
	 */
	public static void bindTextProperty(StringProperty uiProperty, String langKey, String suffix) {
		checkInitialized();
		if (uiProperty == null || StringUtils.isBlank(langKey))
			return;

		StringProperty baseProperty = getLangProperty(langKey);
		if (suffix == null || suffix.isEmpty()) {
			uiProperty.bind(baseProperty);
		} else {
			uiProperty.bind(baseProperty.concat(suffix));
		}
	}

	/**
	 * Binds a UI StringProperty to a localized string property from a LocalizableKey, with a suffix.
	 * 
	 * @param uiProperty The property of the UI control to bind.
	 * @param locKey     The LocalizableKey for the localized text.
	 * @param suffix     The string to append to the localized text.
	 */
	public static void bindTextProperty(StringProperty uiProperty, LocalizableKey locKey, String suffix) {
		checkInitialized();
		if (uiProperty == null)
			return;
		if (locKey == null) {
			uiProperty.set(INVALID_KEY);
			return;
		}

		bindTextProperty(uiProperty, locKey.getKey(), suffix);
	}

	/**
	 * Binds a UI property to the concatenation of two other StringProperties. This is a convenience method for complex bindings, e.g., "Jump to: [GroupName]".
	 * 
	 * @param property       The UI property to be bound (e.g., a MenuItem's textProperty).
	 * @param baseProperty   The base localized text (e.g., from LangMap.getLangProperty).
	 * @param concatProperty The property whose value will be appended to the base text.
	 */
	public static void bindTextProperties(StringProperty property, StringProperty baseProperty, StringProperty concatProperty) {
		checkInitialized();
		if (property == null || baseProperty == null || concatProperty == null) {
			Log.warn("bindTextProperties called with a null property. Binding aborted.");
			return;
		}
		property.bind(baseProperty.concat(concatProperty));
	}

	/**
	 * Binds a UI StringProperty to a localized string provided by a LocalizableKey.
	 * 
	 * @param uiProperty The property of the UI control to bind.
	 * @param locKey     The LocalizableKey providing the key and default text.
	 */
	public static void bindTextProperty(StringProperty uiProperty, LocalizableKey locKey) {
		checkInitialized();
		if (uiProperty == null)
			return;
		if (locKey == null) {
			uiProperty.set(INVALID_KEY);
			Log.warn("bindTextProperty called with null LocalizableKey.");
			return;
		}
		uiProperty.bind(getLangProperty(locKey));
	}

	/**
	 * Binds the {@code textProperty} of a {@link Labeled} UI component (e.g., {@link javafx.scene.control.Label}, {@link javafx.scene.control.Button}) to a localized text defined
	 * by the given {@link LocalizableKey}.
	 *
	 * @param component The {@link Labeled} UI component to bind. Must not be {@code null}.
	 * @param locKey    The {@link LocalizableKey} identifying the localized text. Must not be {@code null}.
	 */
	public static void bindText(Labeled component, LocalizableKey locKey) {
		if (component == null || locKey == null)
			return;
		component.textProperty().bind(getLangProperty(locKey));
	}

	/**
	 * Binds the {@code textProperty} of a {@link TextInputControl} (e.g., {@link javafx.scene.control.TextField}, {@link javafx.scene.control.TextArea}) to a localized text
	 * defined by the given {@link LocalizableKey}.
	 *
	 * @param component The {@link TextInputControl} to bind. Must not be {@code null}.
	 * @param locKey    The {@link LocalizableKey} identifying the localized text. Must not be {@code null}.
	 */
	public static void bindText(TextInputControl component, LocalizableKey locKey) {
		if (component == null || locKey == null)
			return;
		component.textProperty().bind(getLangProperty(locKey));
	}

	/**
	 * Binds the {@code textProperty} of a {@link Tooltip} to a localized text defined by the given {@link LocalizableKey}.
	 *
	 * @param tooltip The {@link Tooltip} to bind. Must not be {@code null}.
	 * @param locKey  The {@link LocalizableKey} identifying the localized text. Must not be {@code null}.
	 */
	public static void bindText(Tooltip tooltip, LocalizableKey locKey) {
		if (tooltip == null || locKey == null)
			return;
		tooltip.textProperty().bind(getLangProperty(locKey));
	}

	/**
	 * Binds the {@code textProperty} of a {@link MenuItem} (e.g., used in {@link javafx.scene.control.ContextMenu} or {@link javafx.scene.control.MenuBar}) to a localized text
	 * defined by the given {@link LocalizableKey}.
	 *
	 * @param menuItem The {@link MenuItem} to bind. Must not be {@code null}.
	 * @param locKey   The {@link LocalizableKey} identifying the localized text. Must not be {@code null}.
	 */
	public static void bindText(MenuItem menuItem, LocalizableKey locKey) {
		if (menuItem == null || locKey == null)
			return;
		menuItem.textProperty().bind(getLangProperty(locKey));
	}

	/**
	 * Binds a tooltip with localized text to a JavaFX control. This is a convenience overload for {@link #bindTooltipText(Control, String, Object)}.
	 * 
	 * @param <T>       The type of the JavaFX control.
	 * @param component The control to which the tooltip will be assigned.
	 * @param langKey   The language key for the tooltip text.
	 * @return The created and configured Tooltip object.
	 */
	public static <T extends Control> Tooltip bindTooltipText(T component, String langKey) {
		return bindTooltipText(component, langKey, null);
	}

	/**
	 * Binds a tooltip with localized text to a JavaFX control using a LocalizableKey. This is a convenience overload for {@link #bindTooltipText(Control, LocalizableKey, Object)}.
	 * 
	 * @param <T>       The type of the JavaFX control.
	 * @param component The control to which the tooltip will be assigned.
	 * @param locKey    The LocalizableKey for the tooltip text.
	 * @return The created and configured Tooltip object.
	 */
	public static <T extends Control> Tooltip bindTooltipText(T component, LocalizableKey locKey) {
		return bindTooltipText(component, locKey, null);
	}

	/**
	 * Binds a tooltip with localized text to a JavaFX control using a LocalizableKey, with optional concatenation.
	 * 
	 * @param <T>       The type of the JavaFX control.
	 * @param component The control to which the tooltip will be assigned. Must not be null.
	 * @param locKey    The LocalizableKey for the tooltip text. Must not be null.
	 * @param concat    An optional object to concatenate to the tooltip text.
	 * @return The created and configured Tooltip object.
	 */
	public static <T extends Control> Tooltip bindTooltipText(T component, LocalizableKey locKey, Object concat) {
		if (locKey == null) {
			Log.warn("Cannot bind tooltip: LocalizableKey is null for component: %s", (component != null ? component.getId() : "null"));
			return null;
		}
		// Reuses the base implementation by passing the key from the LocalizableKey
		return bindTooltipText(component, locKey.getKey(), concat);
	}

	/**
	 * Binds a tooltip with localized text to a JavaFX control, with optional concatenation.
	 * 
	 * @param <T>       The type of the JavaFX control.
	 * @param component The control to which the tooltip will be assigned. Must not be null.
	 * @param langKey   The language key for the tooltip text. Must not be blank.
	 * @param concat    An optional object to concatenate to the tooltip text. Can be a {@link String} or a {@link StringProperty}. If null, no concatenation occurs.
	 * @return The created and configured Tooltip object, or null if component or langKey is invalid.
	 */
	public static <T extends Control> Tooltip bindTooltipText(T component, String langKey, Object concat) {
		checkInitialized();
		if (component == null) {
			Log.warn("Cannot bind tooltip: component is null. LangKey: %s", langKey);
			return null;
		}
		if (StringUtils.isBlank(langKey)) {
			Log.warn("Cannot bind tooltip: langKey is blank for component: %s", component.getId());
			return null;
		}

		Tooltip tooltip = new Tooltip();
		tooltip.setShowDuration(Duration.seconds(12));
		tooltip.setWrapText(true);
		tooltip.setMinWidth(160);

		StringProperty langProperty = getLangProperty(langKey);

		if (concat == null) {
			tooltip.textProperty().bind(langProperty);
		} else if (concat instanceof String stringConcat) {
			tooltip.textProperty().bind(langProperty.concat(stringConcat));
		} else if (concat instanceof StringProperty stringPropConcat) {
			tooltip.textProperty().bind(langProperty.concat(stringPropConcat));
		} else {
			Log.warn("bindTooltipText: Unknown concatenation type for key '%s'. Binding base text only.", langKey);
			tooltip.textProperty().bind(langProperty);
		}

		component.setTooltip(tooltip);
		component.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
			if (tooltip.isShowing()) {
				tooltip.hide();
			}
		});

		return tooltip;
	}

	/**
	 * Convenience method to bind a MenuItem's text using a LocalizableKey. This is a shorthand for bindText(MenuItem, LocalizableKey).
	 */
	public static void bindMenuItem(MenuItem menuItem, LocalizableKey locKey) {
		bindText(menuItem, locKey);
	}

	/**
	 * Convenience method to bind a MenuItem's text using a LocalizableKey with a suffix.
	 */
	public static void bindMenuItem(MenuItem menuItem, LocalizableKey locKey, String suffix) {
		if (menuItem == null || locKey == null)
			return;
		bindTextProperty(menuItem.textProperty(), locKey, suffix);
	}

	/**
	 * Convenience method to bind a Labeled component's text.
	 */
	public static void bindLabeled(Labeled component, LocalizableKey locKey) {
		bindText(component, locKey);
	}

	/**
	 * Convenience method to bind a Labeled component's text with a suffix.
	 */
	public static void bindLabeled(Labeled component, LocalizableKey locKey, String suffix) {
		if (component == null || locKey == null)
			return;
		bindTextProperty(component.textProperty(), locKey, suffix);
	}

	/**
	 * Binds the prompt text of a TextInputControl using a LocalizableKey.
	 */
	public static void bindPromptText(TextInputControl component, LocalizableKey locKey) {
		if (component == null || locKey == null)
			return;
		component.promptTextProperty().bind(getLangProperty(locKey));
	}

	// --- Private Helper Methods ---

	/**
	 * The private, core method for creating and caching StringProperties. It ensures that a property is created only once per key and correctly initialized with either a
	 * translation or a provided default value.
	 *
	 * @param fieldKey                The message key; must not be blank.
	 * @param defaultValueForCreation Fallback text used ONLY the first time the key is requested and no translation exists.
	 * @return A non-null, reactive StringProperty with the resolved text.
	 */
	private static StringProperty getLangProperty(String fieldKey, String defaultValueForCreation) {
		checkInitialized();
		if (StringUtils.isBlank(fieldKey)) {
			Log.warn("getLangProperty called with blank key.");
			// Wenn ein Default-Wert da ist, nutze ihn, ansonsten den Fehler-Platzhalter
			return new SimpleStringProperty(defaultValueForCreation != null ? defaultValueForCreation : INVALID_KEY);
		}

		final String key = fieldKey.trim();

		// computeIfAbsent ist thread-sicher und stellt sicher, dass der Code-Block nur einmal pro Schlüssel ausgeführt wird.
		return langPropertyMap.computeIfAbsent(key, k -> {

			// WICHTIG: Wenn die Property ZUM ERSTEN MAL erstellt wird, verwenden wir die getLangString-Methode,
			// die einen Default-Wert akzeptiert. So stellen wir sicher, dass der initiale Wert korrekt ist.
			String initialValue = getLangString(k, defaultValueForCreation);

			// Erstelle die neue, reaktive Eigenschaft mit dem korrekten Startwert.
			return new SimpleStringProperty(initialValue);
		});
	}

	/**
	 * Gets the path to the language resource bundles.
	 * 
	 * @return The base name of the resource bundle.
	 */
//	private static String getLanguageBundlePath() {
//		return ResMan.getInstance().isResourceFolderUsed() ? ResMan.RESOURCE_DIRECTORY_NAME + ".lang.lang" : "lang.lang";
//	}
	
	private static String getLanguageBundlePath() {
		// HEILUNG: Erzwinge den Standardpfad, um die MissingResourceException zu beheben.
		// Dies umgeht potenzielle Fehlkonfigurationen von ResMan für den Ressourcen-Bundle-Pfad.
		return "lang.lang"; 
	}

	/**
	 * Logs a warning for a missing key, but only once per key and locale combination to avoid log spam.
	 * 
	 * @param fieldKey     The key that was not found.
	 * @param bundleLocale The locale in which the key was expected.
	 */
	private static void logMissingKeyOnce(String fieldKey, Locale bundleLocale) {
		String logKey = bundleLocale.toString() + ":" + fieldKey;
		if (alreadyLoggedMissingKeys.add(logKey)) {
			Log.warn("Missing key: '%s' in bundle for locale: %s (and its fallbacks).", fieldKey, bundleLocale);
		}
	}

	/**
	 * Checks if the class has been initialized, throwing an exception if not. This prevents usage of the class before it's ready.
	 */
	private static void checkInitialized() {
		if (!isInitialized) {
			throw new IllegalStateException("LangMap has not been initialized. Call LangMap.init() at application startup.");
		}
	}

	/**
	 * Updates the default Locale for the entire Java Virtual Machine (JVM). This is crucial for ensuring that internal JavaFX components like ColorPicker, FileChooser etc. also
	 * change their language.
	 *
	 * @param newLocale The new locale selected by the user, or null to revert to the system's default.
	 */
	private static void updateJvmDefaultLocale(Locale newLocale) {
		// Determine the locale to be set for the JVM.
		// If the user selects 'null' (meaning "use system default"), we use the initially detected systemLocale.
		Locale localeForJvm = (newLocale != null) ? newLocale : systemLocale;

		// This is the critical call that changes the language for the ColorPicker.
		Locale.setDefault(localeForJvm);

		Log.info("JVM Default Locale updated to: %s (for system components like ColorPicker)", localeForJvm);
	}
}