package utils.appconfig;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths; // Import Paths
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import utils.logging.Log;
import utils.network.UserAgentMode;
import utils.network.strategy.ConnectionStrategy;
import utils.network.strategy.ProxyUsageStrategy;

/**
 * Manages application configuration options stored in a properties file (im_config.pts) located within a dedicated 'config' subdirectory of the application's execution directory.
 * Provides type-safe access and JavaFX property binding.
 *
 * @see OptionKey Defines the available configuration keys, their types, and defaults.
 */
public class OptionsHandler {

	// --- Constants ---
	public static final String    DIRECTORY_ARCHIVES       = "Archives";             // Relative to execution path
	public static final String    DIRECTORY_CUSTOM_THEMES  = "Custom Themes";        // Relative to execution path
	public static final String    CONFIG_SUBDIRECTORY_NAME = "config";               // Name of the config subfolder
	private static final String   SETTINGS_FILENAME        = "im_config.pts";        // Name of the properties file

	// --- Enums (Keep OptionKey and TargetType as defined before) ---
	public enum TargetType {
		BOOLEAN(Boolean.class),
		INTEGER(Integer.class),
		STRING(String.class);

		private final Class<?> valueClass;

		TargetType(Class<?> valueClass) {
			this.valueClass = valueClass;
		}

		public Class<?> getValueClass() {
			return valueClass;
		}
	}

	public enum OptionKey {
	    // --- App Monetarisierung / Kauf ---

		/**
		 * URL for the external purchase page (e.g., Gumroad or Paddle). Used by the handleRegistration() method. VORLÄUFIGE TEST-URL: Google (als Platzhalter für externe Links)
		 */
		MARKET_PURCHASE_URL(TargetType.STRING, "https://www.google.com/"),
		/** Stores the active percentage discount for marketing campaigns (e.g., 20). */
		MARKETING_DISCOUNT_PERCENTAGE(TargetType.INTEGER, 0), // NEU

		/** Stores the current active promo code string (e.g., "TWITCH20"). */
		MARKETING_PROMO_CODE(TargetType.STRING, null), // NEU
	    // --- Lizenzierung ---
		/** Stores the user's purchased license key string. */
		LICENSE_KEY(TargetType.STRING, null),
	    // --- Application/Window State ---
		WINDOW_X(TargetType.INTEGER, 0), // Used in WebmarksApp/Launcher/Frame
		WINDOW_Y(TargetType.INTEGER, 0), // Used in WebmarksApp/Launcher/Frame
		WINDOW_WIDTH(TargetType.INTEGER, 230), // Used in WebmarksApp/Launcher/Frame
		WINDOW_HEIGHT(TargetType.INTEGER, 600), // Used in WebmarksApp/Launcher/Frame
		SIDEBAR_ACTIVE(TargetType.BOOLEAN, true), // Used in WebmarksApp (SideMenu)
		SIDEBAR_FIXED(TargetType.BOOLEAN, false), // Used in WebmarksApp (SideMenu)
		USER_LOCALE(TargetType.STRING, "en_GB"), // Used early for LangMap, set in Prefs
		THEME(TargetType.STRING, "Alpha (Standard).tme"), // Used for styling, set in Prefs (Appearance)

	    // --- Specific UI States (Sorting, Column Visibility) ---
//		SORTING_CASE_SENSITIVE(TargetType.BOOLEAN, false), // Affects sorting logic
//		SORTING_TABLE(TargetType.STRING, EnumSortOrder.ID.name()), // Affects TableView sorting state
//		SORTING_TABLE_REVERSED(TargetType.BOOLEAN, false),
//		SORTING_TREE(TargetType.STRING, EnumSortOrder.ID.name()), // Affects TreeView sorting state
//		SORTING_TREE_REVERSED(TargetType.BOOLEAN, false),

	    // columnIndex:false,columnAddress:false,columnState:false,columnViews:false,columnChecked:false,columnVisited:false,columnAdded:true
		COLUMN_VISIBILITY(TargetType.STRING, "columnID,columnTitle,columnRating"),

	    // --- Data Handling & Storage ---
		ARCHIVES_DIRECTORY(TargetType.STRING, DIRECTORY_ARCHIVES), // Set in Prefs (General), used by App/Archive
		OPENED_ARCHIVES(TargetType.STRING, null), // Used by App for history
		LAST_ARCHIVE_WAS_OPEN(TargetType.BOOLEAN, false),

		SEARCH_LEARN(TargetType.BOOLEAN, true), // Affects search behavior?

	    // --- UI Behavior & Appearance (Preferences Dialog) ---
	    // General Tab
		BYPASS_WRITE_PROTECTION(TargetType.BOOLEAN, false), // General setting, affects saving
		SHOW_TOOLTIPS(TargetType.BOOLEAN, true),
		TREEPANE_ARRANGE_WIDTH(TargetType.INTEGER, 440), // Für SplitPane-Verhalten
		SHOW_ROOT_NODE(TargetType.BOOLEAN, false), // Affects TreeView
		SHOW_TABLE_HEADER(TargetType.BOOLEAN, true), // Affects TableView
		SHOW_TABLE_MENU_BUTTON(TargetType.BOOLEAN, true), // Affects TableView

	    // Behavior/Controls Tab
		BEHAVIOR_OPEN_URL_ON_DOUBLECLICK(TargetType.BOOLEAN, true),
		CLOSE_CONTEXMENU_MOUSEBUTTON_RIGHT(TargetType.BOOLEAN, true),
		HIDE_EMPTY_COREGROUPS(TargetType.BOOLEAN, true), // Affects TreeView display
		CONSUME_AUTO_HIDE_EVENTS(TargetType.BOOLEAN, false), // Affects UI event handling // Für ContextMenu-Verhalten
		ALLOW_SELECTION_ON_RIGHT_CLICK(TargetType.BOOLEAN, true), // Affects UI focus behavior
		EMPTY_TRASH_ON_CLOSE(TargetType.BOOLEAN, false), // Set in Prefs (Behavior?), used by App on exit

		/**
		 * Defines the behavior when closing a modified archive (e.g., via "File -> Close"). Values: "PROMPT" (default), "AUTOSAVE".
		 */
		ARCHIVE_CLOSE_BEHAVIOR(TargetType.STRING, "PROMPT"),

		/**
		 * Defines the behavior when exiting the application with a modified archive. Values: "PROMPT" (default), "AUTOSAVE". This key supersedes EXIT_DIALOG_SAVE_ALWAYS.
		 */
		APP_EXIT_BEHAVIOR(TargetType.STRING, "PROMPT"),

	    // =================================================================================
	    // --- Network & Scraping Settings (Preferences Dialog -> Network Tab) -----------
	    // =================================================================================

	    // --- General Connection Parameters ---
		/** Maximum number of HTTP redirects to follow during a scrape or verification. */
		NETWORK_SCRAPE_MAX_REDIRECTS(TargetType.INTEGER, 5),

		/** Timeout in seconds for a single network request to complete. */
		NETWORK_SCRAPE_TIMEOUT(TargetType.INTEGER, 15),

		/** Delay in milliseconds to wait before initiating a network request. */
		NETWORK_REQUEST_DELAY_MS(TargetType.INTEGER, 250),

	    // --- Scraping Behavior ---
		/** If true, automatically fetches metadata when a new web link is created or verified. */
		NETWORK_AUTOSCRAPE(TargetType.BOOLEAN, true),

		/**
		 * NEU: Intelligent URL Sanitization. If true, tracking parameters and session IDs are automatically stripped from known domains (e.g., Amazon, eBay) to enhance privacy and
		 * archive cleanliness.
		 */
		NETWORK_SANITIZE_URLS(TargetType.BOOLEAN, true), // Default: true (Premium-Feeling)
	    // In enum OptionKey:
		NETWORK_SANITIZE_AMAZON(TargetType.BOOLEAN, true),
		NETWORK_SANITIZE_EBAY(TargetType.BOOLEAN, true),
		NETWORK_SANITIZE_CANONICAL(TargetType.BOOLEAN, true),

		/** If true, the scraper will update the link's URL if a final redirection is found. */
		NETWORK_SCRAPE_UPDATE_URL(TargetType.BOOLEAN, true),
		/** If true, the scraper will update the link's title with fetched metadata. */
		NETWORK_SCRAPE_UPDATE_TITLE(TargetType.BOOLEAN, true),
		/** If true, the scraper will update the link's description. */
		NETWORK_SCRAPE_UPDATE_DESCRIPTION(TargetType.BOOLEAN, true),
		/** If true, the scraper will update the link's keywords/tags. */
		NETWORK_SCRAPE_UPDATE_KEYWORDS(TargetType.BOOLEAN, true),

	    // --- Connection Strategy & Proxies ---
		/**
		 * Defines the primary connection strategy (e.g., DIRECT, USE_STANDARD_PROXY, ROTATION_WITH_FALLBACK). Stored as the enum's name.
		 */
		NETWORK_CONNECTION_STRATEGY(TargetType.STRING, ConnectionStrategy.DIRECT.name()),

		/**
		 * Defines the proxy selection method to use within the ROTATION_WITH_FALLBACK strategy (e.g., SEQUENTIAL, WEIGHTED_RANDOM). Stored as the enum's name.
		 */
		NETWORK_PROXY_USAGE_STRATEGY(TargetType.STRING, ProxyUsageStrategy.WEIGHTED_RANDOM.name()),

		/** Index of the preferred proxy in the main proxy list, used for the USE_STANDARD_PROXY strategy. */
		NETWORK_STANDARD_PROXY(TargetType.INTEGER, -1),

		/** If true, a direct connection will be attempted if all proxies fail during rotation. */
		NETWORK_PROXY_ROTATION_FALLBACK_TO_DIRECT(TargetType.BOOLEAN, false),

	    // --- Proxy Health & Exclusion Thresholds ---
		/**
		 * Maximum number of consecutive proxy-attributable faults before a proxy is considered "hard-blocked" and requires manual reset.
		 */
		NETWORK_PROXY_MAX_CONSECUTIVE_FAILURES(TargetType.INTEGER, 10),

		/**
		 * The cooldown period in seconds that a proxy must wait after any single proxy-attributable fault before it can be used again.
		 */
		NETWORK_PROXY_FAILURE_COOLDOWN_SECONDS(TargetType.INTEGER, 30), // Default: 60 seconds (1 minutes)

	    // --- User-Agent Configuration ---
		/**
		 * Defines the mode for selecting the User-Agent string (e.g., DEFAULT_JAVA, BROWSER_SIMULATION, CUSTOM). Stored as the enum's name.
		 */
		NETWORK_USER_AGENT_MODE(TargetType.STRING, UserAgentMode.DEFAULT_JAVA.name()),

		/** Stores the currently selected custom User-Agent string. This is only used if NETWORK_USER_AGENT_MODE is CUSTOM. */
		NETWORK_USER_AGENT_CUSTOM_SELECTED(TargetType.STRING, null),

	    // --- UI State for Network-Related Dialogs ---
		/**
		 * Stores the last selected sort mode in the Proxy Management dialog. Stored as the enum's name (e.g., "MANUAL", "RELIABILITY_DESC").
		 */
		PROXY_DIALOG_SORT_MODE(TargetType.STRING, "MANUAL"),

	    // =================================================================================
	    // --- Other Settings (For Context) ----------------------------------------------
	    // =================================================================================

	    // --- Browser Settings (Preferences Dialog -> Browser Tab) ---
		/** The path to the default browser executable, or "SYSTEM_DEFAULT" for the OS default. */
		BROWSER_DEFAULT_PATH(TargetType.STRING, "SYSTEM_DEFAULT"),

	    // --- Notification Settings (Preferences Dialog -> Notifications Tab) ---
		/** Defines the default screen position for global toast notifications (e.g., "BOTTOM_RIGHT", "TOP_CENTER"). */
		UI_NOTIFICATION_POSITION(TargetType.STRING, "BOTTOM_RIGHT"),

		/** Default display duration for global toast notifications, in seconds. */
		UI_NOTIFICATION_DURATION_SECONDS(TargetType.INTEGER, 7),

		/** Defines the default owner mode for global notifications (e.g., "DESKTOP", "MAIN_WINDOW"). */
		UI_NOTIFICATION_DEFAULT_OWNER_MODE(TargetType.STRING, "DESKTOP"),

		/** Whether global toast notifications should use a dark style by default. */
		UI_NOTIFICATION_USE_DARK_STYLE(TargetType.BOOLEAN, false);


		private final TargetType targetType;
		private final Object     defaultValue;

		OptionKey(TargetType type, Object value) {
			this.targetType = type;
			this.defaultValue = value;
		}

		public TargetType getTargetType() {
			return targetType;
		}

		public Object getDefaultValue() {
			return defaultValue;
		}
	}

	// --- Fields ---

	private final Path                                       executionPath;                                               // Store the original execution path
	private final Path                                       configurationDirectoryPath;                                  // Path to the 'config' subdirectory
	private final Path                                       configFilePath;                                              // Full path to the properties file within config dir
	private final String                                     description;                                                 // Description for properties file
	private boolean                                          foundConfigFile            = false;                          // Was config file found on last check?
	private boolean                                          successfullyLoadedFromFile = false;                          // Was config loaded successfully?
	private final EnumMap<OptionKey, ObjectProperty<Object>> map                        = new EnumMap<>(OptionKey.class); // Holds the properties

	/**
	 * Constructs an OptionsHandler. Determines the configuration directory path, ensures the directory exists, and initializes default values.
	 *
	 * @param configPath The path to the 'config' subdirectory. Must not be null.
	 * @param comment    A description string for the properties file header. Can be null.
	 * @throws IOException If the configuration directory cannot be created.
	 */
	public OptionsHandler(Path configPath, String comment) throws IOException {
		// Store the provided path directly
		this.configurationDirectoryPath = Objects.requireNonNull(configPath, "Configuration path cannot be null").toAbsolutePath();
		// Derive execution path from config path (optional, if needed elsewhere)
		this.executionPath = this.configurationDirectoryPath.getParent();
		this.description = (comment != null) ? comment : "Application Configuration";

		this.configFilePath = this.configurationDirectoryPath.resolve(SETTINGS_FILENAME);

		Log.info("Execution path determined (parent of config): %s", this.executionPath);
		Log.info("Configuration path set to: %s", this.configurationDirectoryPath);
		Log.info("Configuration file path set to: %s", this.configFilePath);

		// Ensure the configuration directory exists
		ensureConfigDirectoryExists();

		// Initialize map with default properties (now without locale)
		initializeDefaultProperties();
	}

	/** Ensures the configuration directory exists, creating it if necessary. */
	private void ensureConfigDirectoryExists() throws IOException {
		if (!Files.exists(this.configurationDirectoryPath)) {
			try {
				Files.createDirectories(this.configurationDirectoryPath);
				Log.info("Created configuration directory: %s", this.configurationDirectoryPath);
			} catch (IOException e) {
				Log.error(e, "FATAL: Could not create configuration directory: %s", this.configurationDirectoryPath);
				// Re-throw as this is critical for operation
				throw new IOException("Could not create configuration directory: " + this.configurationDirectoryPath, e);
			}
		} else if (!Files.isDirectory(this.configurationDirectoryPath)) {
			// Handle case where path exists but is not a directory
			String errorMsg = "Configuration path exists but is not a directory: " + this.configurationDirectoryPath;
			Log.error(errorMsg);
			throw new IOException(errorMsg);
		}
	}

	/**
	 * Initializes the internal property map with default values from the OptionKey enum.
	 */
	private void initializeDefaultProperties() {
		for (OptionKey key : OptionKey.values()) {
			map.put(key, new SimpleObjectProperty<>(key.getDefaultValue()));
		}
		Log.fine("Initialized OptionsHandler with %d default values.", OptionKey.values().length);
	}

	// --- Property Access (Methods remain the same as before) ---

	@SuppressWarnings("unchecked")
	public <T> ObjectProperty<T> getProperty(OptionKey key) {
		Objects.requireNonNull(key, "OptionKey cannot be null for getProperty");
		ObjectProperty<?> property = map.computeIfAbsent(key, k -> {
			Log.warn("Property for key '%s' was missing, creating with default value: %s", k, k.getDefaultValue());
			return new SimpleObjectProperty<>(k.getDefaultValue());
		});
		return (ObjectProperty<T>) property;
	}

	protected <T> T getValue(OptionKey key) {
		ObjectProperty<T> property = getProperty(key);
		return property.getValue();
	}

	/**
	 * Retrieves the value associated with the specified OptionKey. If the key is not found in the currently loaded options or the stored value is null, the default value defined
	 * in the OptionKey enum is returned.
	 *
	 * This method also attempts to convert the retrieved value (which might be stored as a String from the properties file) to the target type defined in the OptionKey enum.
	 *
	 * @param <T> The expected type of the value. The method attempts to cast/convert to this type.
	 * @param key The OptionKey to retrieve. Must not be null.
	 * @return The retrieved and potentially converted value, or the OptionKey's default value if the key is not found, the stored value is null, or conversion fails. Returns null
	 *         if the OptionKey's default value is null and no other value can be determined.
	 * @throws NullPointerException if key is null.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getValueOrDefault(OptionKey key) {
		Objects.requireNonNull(key, "OptionKey cannot be null for getValueOrDefault (single-arg)");

		Object storedValueObject = getValue(key); // This already uses OptionKey's default if property was missing in map
		Object defaultValueFromEnum = key.getDefaultValue();
		Class<?> targetType = key.getTargetType().getValueClass();

		Object valueToProcess;

		if (storedValueObject != null) {
			// A value was present (either from file or set programmatically, or default from initial map population)
			valueToProcess = storedValueObject;
		} else {
			// This case should be rare if getValue() always returns the enum default for missing keys.
			// But as a safeguard, explicitly use the enum default.
			Log.fine("Value for key '%s' was null from getValue(). Using default from OptionKey: %s", key, defaultValueFromEnum);
			valueToProcess = defaultValueFromEnum;
		}

		// If valueToProcess is still null (e.g., if enum default was null), return null.
		if (valueToProcess == null) {
			return null;
		}

		// Check if the type already matches the target type defined in OptionKey
		if (targetType.isAssignableFrom(valueToProcess.getClass())) {
			try {
				return (T) valueToProcess; // Direct cast should work
			} catch (ClassCastException e) {
				// Should not happen if isAssignableFrom is true, but defensive.
				Log.warn(e, "Unexpected ClassCastException for key '%s' despite isAssignableFrom check. Value: '%s', TargetType: %s. Returning enum default.", key, valueToProcess,
				        targetType.getSimpleName());
				return (T) defaultValueFromEnum; // Fallback to enum default
			}
		}

		// Type mismatch or value is likely a String (from properties file load). Attempt conversion.
		Log.warn("Value for key '%s' has type (%s) and needs conversion to target type (%s). Value: '%s'", key, valueToProcess.getClass().getSimpleName(),
		        targetType.getSimpleName(), valueToProcess);
		try {
			String stringValue = valueToProcess.toString(); // Convert to string for parsing

			if (targetType == Integer.class) {
				return (T) Integer.valueOf(stringValue);
			} else if (targetType == Boolean.class) {
				String strValLower = stringValue.trim().toLowerCase();
				if ("true".equals(strValLower) || "1".equals(strValLower) || "yes".equals(strValLower))
					return (T) Boolean.TRUE;
				if ("false".equals(strValLower) || "0".equals(strValLower) || "no".equals(strValLower))
					return (T) Boolean.FALSE;
				Log.warn("Cannot convert string '%s' to Boolean for key '%s'. Using enum default.", stringValue, key);
				return (T) defaultValueFromEnum; // Fallback if not a clear boolean string
			} else if (targetType == String.class) {
				return (T) stringValue; // Already a string or convertible to one
			} else if (targetType == Long.class) {
				return (T) Long.valueOf(stringValue);
			} else if (targetType == Double.class) {
				return (T) Double.valueOf(stringValue);
			}
			// Add other type conversions if your OptionKey.TargetType supports them

			Log.warn("Unsupported target type (%s) for conversion for key '%s'. Using enum default.", targetType.getSimpleName(), key);
			return (T) defaultValueFromEnum;

		} catch (NumberFormatException e) {
			Log.warn("Conversion to number failed for key '%s', value '%s' (target type %s). Using enum default. Error: %s", key, valueToProcess, targetType.getSimpleName(),
			        e.getMessage());
			return (T) defaultValueFromEnum;
		} catch (Exception e) { // Catch any other conversion error
			Log.warn(e, "Generic conversion error for key '%s', value '%s' (target type %s). Using enum default.", key, valueToProcess, targetType.getSimpleName());
			return (T) defaultValueFromEnum;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> void setValue(OptionKey key, T value) {
		Objects.requireNonNull(key, "OptionKey cannot be null for setValue");
		Class<?> targetValueClass = key.getTargetType().getValueClass();
		Object originalValue = value; // Keep original for logging if conversion fails

		T processedValue = value; // Start with the original value
//		boolean conversionAttempted = false; // Flag to track if conversion logic was entered

		if (value != null) {
			Class<?> valueActualClass = value.getClass();
			if (!targetValueClass.isAssignableFrom(valueActualClass)) {
				// --- Conversion is needed ---
//				conversionAttempted = true; // Set the flag
//				Log.debug("Type mismatch for key '%s'. Attempting conversion from %s to %s.", key, valueActualClass.getSimpleName(), targetValueClass.getSimpleName());
				try {
					if (targetValueClass == Boolean.class) {
						processedValue = (T) Boolean.valueOf(value.toString());
					} else if (targetValueClass == Integer.class) {
						processedValue = (T) Integer.valueOf(value.toString());
					} else if (targetValueClass == String.class) {
						processedValue = (T) value.toString(); // Technically covered by assignableFrom, but safe fallback
					} else {
						// Log the failure clearly before throwing
						Log.error("Unsupported type conversion for key '%s'. Expected %s, Got %s. Cannot set value.", key, targetValueClass.getName(), valueActualClass.getName());
						throw new IllegalArgumentException("Unsupported value type conversion needed for key '" + key + "'");
					}
//					Log.fine("Successfully converted value for key '%s'.", key); // Log success

				} catch (NumberFormatException nfe) {
					Log.error(nfe, "Invalid number format for key '%s'. Expected %s but got value: '%s'. Cannot set value.", key, targetValueClass.getName(), originalValue);
					throw new IllegalArgumentException("Invalid number format for key '" + key + "'", nfe);
				} catch (Exception e) { // Catch other potential conversion errors
					Log.error(e, "Conversion failed for key '%s'. Value: '%s'.", key, originalValue);
					throw new IllegalArgumentException("Conversion failed for key '" + key + "'", e);
				}
			}
			// else: value is non-null and assignable, no conversion needed.
		}
		// else: value is null, processedValue remains null. No conversion needed.

		// Retrieve the property
		ObjectProperty<Object> property = map.get(key);

		if (property != null) {
			// --- Log the FINAL value being set ---
			// Use the flag to decide which log message to use
//			if (conversionAttempted) {
//				Log.fine("Setting value for key '%s' (type %s, after conversion) to: %s", key, (processedValue != null ? processedValue.getClass().getSimpleName() : "null"), processedValue);
//			} else if (value != null) { // No conversion needed, value was not null
//				Log.fine("Setting value for key '%s' (type %s, no conversion needed) to: %s", key, value.getClass().getSimpleName(), processedValue);
//			} else { // Value was originally null
//				Log.fine("Setting value for key '%s' to: null", key);
//			}
			property.set(processedValue);
		} else {
			Log.error("Cannot set value for key '%s': Key not found in OptionsHandler map.", key);
			throw new IllegalArgumentException("Key '" + key + "' not found in OptionsHandler map.");
		}
	}

	// --- File/Directory Checks and Access ---

	/** Checks whether the specified file path exists and is a regular file. */
	public static boolean fileExists(Path filePath) {
		return filePath != null && Files.exists(filePath) && !Files.isDirectory(filePath);
	}

	/** Checks if the configuration file exists in the config directory. */
	public boolean checkConfigFileExists() {
		this.foundConfigFile = fileExists(this.configFilePath);
		if (!this.foundConfigFile && this.configFilePath != null) { // Check path null safety
			// Log only if file is missing
			Platform.runLater(() -> { // Ensure logging happens on FX thread if needed for UI later
				Log.info("Configuration file does not exist at: %s", this.configFilePath);
				Log.info("Application will use default configuration values.");
			});
		}
		return this.foundConfigFile;
	}

	// logConfigFileMissing removed as logging is now inside checkConfigFileExists

	/**
	 * Prompts the user to locate the configuration file, starting the search in the application's designated configuration directory.
	 *
	 * @param owner The parent stage for the dialog.
	 * @return The Path to the selected configuration file, or null if cancelled.
	 */
	public Path promptUserForConfigFile(Stage owner) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Locate " + SETTINGS_FILENAME);
		// Start in the designated config directory
		try {
			File initialDir = this.configurationDirectoryPath.toFile();
			if (initialDir.isDirectory()) { // Check if directory exists and is valid
				fileChooser.setInitialDirectory(initialDir);
			} else {
				// Fallback to execution path if config dir isn't valid (shouldn't happen)
				Log.warn("Configuration directory '%s' is not valid, starting FileChooser in execution directory '%s'.", this.configurationDirectoryPath, this.executionPath);
				fileChooser.setInitialDirectory(this.executionPath.toFile());
			}
		} catch (Exception e) {
			Log.error(e, "Error setting initial directory for FileChooser. Starting in execution directory.");
			fileChooser.setInitialDirectory(this.executionPath.toFile()); // Fallback
		}
		// Define the expected file extension
		String extension = SETTINGS_FILENAME.substring(SETTINGS_FILENAME.lastIndexOf('.'));
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Configuration File (*" + extension + ")", "*" + extension));
		fileChooser.setInitialFileName(SETTINGS_FILENAME);

		// Show the dialog
		File configFile = fileChooser.showOpenDialog(owner);
		return (configFile != null) ? configFile.toPath() : null;
	}

	/**
	 * Returns the absolute path to the application's execution directory.
	 * 
	 * @return The absolute Path object.
	 */
	public Path getExecutionDirectoryPath() {
		return this.executionPath;
	}

	/**
	 * Returns the absolute path to the dedicated configuration directory (e.g., 'config' subdirectory).
	 * 
	 * @return The absolute Path object.
	 */
	public Path getConfigurationDirectoryPath() {
		return this.configurationDirectoryPath;
	}

	/**
	 * Returns the resolved {@link File} object representing the configured archives directory. This path is relative to the execution directory.
	 * 
	 * @return The File object for the archives directory.
	 */
	public File getDirectoryArchives() {
		// Resolve relative to execution path
		String relativePath = getValue(OptionKey.ARCHIVES_DIRECTORY);
		if (relativePath == null)
			relativePath = DIRECTORY_ARCHIVES; // Use default if null
		return this.executionPath.resolve(relativePath).toFile();
	}

	/**
	 * Returns the resolved {@link File} object representing the custom themes directory. This path is relative to the execution directory.
	 * 
	 * @return The File object for the custom themes directory.
	 */
	public File getDirectoryThemesCustom() {
		// Resolve relative to execution path
		return this.executionPath.resolve(DIRECTORY_CUSTOM_THEMES).toFile();
	}

	/** Checks if the specified {@link OptionKey} is managed by this handler. */
	public boolean contains(OptionKey key) {
		return key != null && map.containsKey(key);
	}

	/** Checks if the configuration file was found during the last check/load attempt. */
	public boolean isConfigFileFound() {
		return this.foundConfigFile;
	}

	/** Checks if configuration settings were successfully loaded from file during the last {@link #read()}. */
	public boolean isLoadedFromFile() {
		return this.successfullyLoadedFromFile;
	}

	// --- Load/Save ---

	/**
	 * Reads configuration settings from the properties file in the config directory. Updates internal properties map if the file exists and is readable. Sets the
	 * {@link #isLoadedFromFile()} flag upon successful completion.
	 */
	public void read() {
		Log.info("Attempting to read configuration file: %s", this.configFilePath);
		this.successfullyLoadedFromFile = false; // Reset flag at start

		if (!checkConfigFileExists()) { // This now logs if missing
			Log.info("Config file not found. Using default values.");
			updateArchivesDirectoryPath(); // Ensure default relative path becomes absolute
			return; // Use defaults initialized in constructor
		}
		if (!Files.isReadable(this.configFilePath)) {
			Log.error("Configuration file exists but cannot be read (check permissions): %s. Using default values.", this.configFilePath);
			updateArchivesDirectoryPath(); // Ensure default relative path becomes absolute
			return; // Use defaults
		}

		Properties loadedOptions = new Properties();
		try (BufferedReader reader = Files.newBufferedReader(configFilePath, StandardCharsets.UTF_8)) {
			loadedOptions.load(reader);
			Log.info("Successfully loaded %d properties from %s", loadedOptions.size(), this.configFilePath);
		} catch (IOException | IllegalArgumentException ex) { // Catch potential loading errors
			Log.error(ex, "Failed to read or parse configuration file '%s'. Using default values.", this.configFilePath);
			// Reset to defaults might be safer here, or just keep initialized defaults?
			// For now, we just return and keep the initialized defaults.
			updateArchivesDirectoryPath(); // Ensure default relative path becomes absolute
			return;
		}

		int updatedCount = 0;
		int ignoredCount = 0;
		int errorCount = 0;
		for (String keyName : loadedOptions.stringPropertyNames()) {
			OptionKey enumKey = null;
			try {
				enumKey = OptionKey.valueOf(keyName); // Convert string key to enum key
			} catch (IllegalArgumentException e) {
				Log.warn("Ignoring unknown configuration key '%s' found in %s.", keyName, SETTINGS_FILENAME);
				ignoredCount++;
				continue;
			}

			String stringValue = loadedOptions.getProperty(keyName);
			// Check for null value from properties, although Properties usually stores empty strings instead
			if (stringValue != null) {
				try {
					// Use setValue for type conversion and property update
					setValue(enumKey, stringValue);
					updatedCount++;
				} catch (IllegalArgumentException ex) {
					// Error during conversion/setting for this specific key
					Log.error(ex, "Invalid value format for key '%s' in configuration file ('%s'). Using default value for this key.", enumKey, stringValue);
					// Optionally reset to default explicitly:
					// setValue(enumKey, enumKey.getDefaultValue());
					errorCount++;
				}
			} else {
				Log.warn("Null value encountered for key '%s' in properties file. Behavior might be unexpected.", enumKey);
				// Decide how to handle null - skip, set to default? Skipping for now.
				// setValue(enumKey, enumKey.getDefaultValue()); // Option to set default
				ignoredCount++;
			}
		}
		Log.info("Applied %d configuration values from file. Ignored %d unknown/null keys. Encountered %d value errors.", updatedCount, ignoredCount, errorCount);
		updateArchivesDirectoryPath(); // Ensure path is absolute AFTER loading potential relative path

		// Mark as successfully loaded only if no critical errors occurred during read itself
		this.successfullyLoadedFromFile = true;
	}

	/** Ensures the ARCHIVES_DIRECTORY property holds an absolute path. */
	private void updateArchivesDirectoryPath() {
		try {
			String currentPathValue = getValue(OptionKey.ARCHIVES_DIRECTORY);
			if (currentPathValue == null)
				currentPathValue = DIRECTORY_ARCHIVES; // Use default name if null

			Path path = Paths.get(currentPathValue);
			if (!path.isAbsolute()) {
				path = this.executionPath.resolve(path).toAbsolutePath();
				setValue(OptionKey.ARCHIVES_DIRECTORY, path.toString());
				Log.info("Updated ARCHIVES_DIRECTORY path to absolute: %s", path);
			} else {
				Log.debug("ARCHIVES_DIRECTORY path is already absolute: %s", path);
			}

			// Ensure the directory exists after resolving
			File archiveDir = path.toFile();
			if (!archiveDir.exists()) {
				Log.warn("Archives directory does not exist, attempting to create: %s", path);
				if (!archiveDir.mkdirs()) {
					Log.error("Failed to create archives directory: %s", path);
					// Potentially show error to user?
				}
			} else if (!archiveDir.isDirectory()) {
				Log.error("Archives path exists but is not a directory: %s", path);
				// Potentially show error to user?
			}

		} catch (Exception e) { // Catch potential exceptions during path resolution/setting
			Log.error(e, "Failed to resolve or set absolute path for ARCHIVES_DIRECTORY.");
		}
	}

	/** Writes the current configuration settings to the properties file in the config directory. */
	public void write() {
		// configFilePath is now guaranteed to be set and absolute in the constructor
		Log.info("Writing configuration file: %s", this.configFilePath);
		Properties propertiesToStore = new Properties();

		for (Map.Entry<OptionKey, ObjectProperty<Object>> entry : map.entrySet()) {
			OptionKey key = entry.getKey();
			Object value = entry.getValue().getValue();
			if (value != null) {
				// Store using the OptionKey name and the value's string representation
				propertiesToStore.setProperty(key.name(), value.toString());
			}
			// else: Skip storing null values to keep properties file cleaner
		}

		// Ensure parent directory exists before writing
		try {
			Path parent = this.configFilePath.getParent();
			if (parent != null && !Files.exists(parent)) {
				Files.createDirectories(parent);
				Log.info("Created parent directory for config file: %s", parent);
			}
		} catch (IOException e) {
			Log.error(e, "Failed to create parent directory for config file '%s'. Cannot write settings.", this.configFilePath);
			return; // Abort write
		}

		// Write the properties file
		try (BufferedWriter writer = Files.newBufferedWriter(configFilePath, StandardCharsets.UTF_8)) {
			propertiesToStore.store(writer, this.description); // Use stored description
			Log.info("Successfully wrote %d properties to %s", propertiesToStore.size(), this.configFilePath);
		} catch (IOException e) {
			Log.error(e, "Error writing configuration file to '%s'", configFilePath);
			// Consider notifying the user about the save failure
		}
	}
}