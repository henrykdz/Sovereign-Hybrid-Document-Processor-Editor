package utils.logging;


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.IllegalFormatException;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javafx.application.Platform; // Needed for checking if FX toolkit is available
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Window;
import utils.general.StringUtils;
import utils.localize.LocalizableKey;
import utils.ui.WindowUtils;

/**
 * Provides a static facade for logging using java.util.logging (JUL). Features include: - Standard logging levels (info, warn, error, fine, debug mapped to finer). - Colored
 * console output. - Lazy initialization of an XML file handler for SEVERE messages. - Optional methods to display JavaFX alerts directly from log calls (`...Show`).
 *
 * Note: The `...Show` methods introduce a dependency on JavaFX and `izon.utils.WindowUtils`. Consider separating UI concerns from core logging if this utility needs to run in
 * non-JavaFX environments.
 */
public final class Log { // Make final as it's a utility class

	// --- Constants for Alert Titles (Consider moving if decoupling UI) ---
	private static final String ALERT_TITLE_INFORMATION = "Information";
	private static final String ALERT_TITLE_ERROR       = "Error";
	private static final String ALERT_TITLE_WARNING     = "Warning";

	// --- Constants for File Logging ---
	private static final String LOG_DIR_NAME  = "Debug";
	private static final String LOG_FILE_NAME = "debug.log";

	// --- Constants for Null Checks (Consider removing/moving) ---
//	private static final String PARAMETER_AT_INDEX_D_IS_NULL = "Parameter at index %d is null";
//	public static final String LOG_NULL_PARAMETER_WITH_NAME = "NULL parameter: %s";
//	public static final String LOG_NULL_VARIABLE_WITH_NAME = "NULL variable: %s";
//	private static final String LOG_NULL_PARAMETER = "NULL parameter";
//	private static final String LOG_NULL_VAR = "NULL variable";

	private static final Logger           LOGGER            = Logger.getLogger(Log.class.getName());
	private static FileHandler            handlerFile;
	private static final ColoredFormatter CONSOLE_FORMATTER = new ColoredFormatter(false, false, true, true);
	// Queue for severe messages logged before FileHandler is initialized
	private static final Queue<LogRecord> pendingSevereRecords   = new LinkedList<>();
	private static volatile boolean       fileHandlerInitialized = false;             // Ensure visibility across threads

	static {
		try {
			CONSOLE_FORMATTER.setFullClassName(false);
			LOGGER.setUseParentHandlers(false); // Prevent duplicate logs from root logger

			// --- Console Handler Setup ---
			ConsoleHandler handlerConsole = new ConsoleHandler();
			handlerConsole.setFormatter(CONSOLE_FORMATTER);
			// Console handler will process any record passed to it by the logger
			handlerConsole.setLevel(Level.ALL);
			LOGGER.addHandler(handlerConsole);

			// --- Logger Global Level ---
			// Logger level determines the lowest level message that will be processed AT ALL.
			// Setting to ALL allows fine-grained control via Handler levels or external config.
			LOGGER.setLevel(Level.ALL); // Process all levels, handlers filter

			// --- Lazy File Handler Initializer ---
			// Add a handler that triggers FileHandler creation upon first SEVERE record
			LOGGER.addHandler(new Handler() {
				@Override
				public void publish(LogRecord record) {
					// Check level first for efficiency
					if (record.getLevel().intValue() >= Level.SEVERE.intValue()) {
						// Double-checked locking for thread-safe lazy initialization
						if (!fileHandlerInitialized) {
							synchronized (pendingSevereRecords) {
								// Re-check inside synchronized block
								if (!fileHandlerInitialized) {
									// Add current record *before* init attempt in case init fails
									pendingSevereRecords.add(record);
									initFileHandler(); // Attempt initialization
									fileHandlerInitialized = true; // Mark initialized (even if init failed, prevents retries)
								} else {
									// Another thread initialized it while waiting for lock
									// Ensure this record gets published by the now-existing handler
									// (or add to queue if init failed but flag is set)
									if (handlerFile != null) {
										handlerFile.publish(record); // Publish directly if handler exists
									} else {
										pendingSevereRecords.add(record); // Add to queue if init failed
									}
								}
							}
						} else {
							// Initialized, but need to handle records if init failed before or succeeded now
							if (handlerFile != null) {
								handlerFile.publish(record); // Publish directly
							} else {
								// File handler init was attempted but failed, queue it? Or drop?
								// Current behavior: It won't be logged to file if init failed.
								// Could add to queue here, but queue might grow indefinitely.
								// Let's log a warning that file logging isn't working.
								synchronized (pendingSevereRecords) { // Use same lock for queue access
									if (!pendingSevereRecords.contains(record)) { // Avoid duplicates
										pendingSevereRecords.add(record); // Keep queuing, hoping for later success? Risky.
										LOGGER.log(Level.WARNING, "File handler initialization failed previously. Queuing severe record: " + record.getMessage());
									}
								}
							}
						}
					}
					// This handler *only* triggers file init; it doesn't block other handlers.
				}

				@Override
				public void flush() {
				} // No-op

				@Override
				public void close() throws SecurityException {
				} // No-op
			});

		} catch (Exception e) {
			// Catch unexpected errors during static initialization
			System.err.println("FATAL: Failed to initialize logging system!");
			e.printStackTrace();
		}
	}

	/**
	 * Converts the stack trace of a Throwable to a String.
	 * 
	 * @param throwable The throwable.
	 * @return The stack trace as a String, or an error message if null.
	 */
	public static String getStackTraceString(Throwable throwable) {
		if (throwable == null) {
			return "[No Throwable provided for stack trace]";
		}
		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) { // Use try-with-resources
			throwable.printStackTrace(pw);
		} // pw is flushed and closed automatically
		return sw.toString();
	}

	/**
	 * Returns a string representation of the current call stack.
	 * 
	 * @param maxDepth The maximum number of stack trace elements to include. If 0 or negative, the full stack trace (up to a reasonable limit) is included.
	 * @return A string containing the formatted call stack.
	 */
	public static String getCurrentStackTrace(int maxDepth) {
		StackTraceElement[] stes = Thread.currentThread().getStackTrace();
		StringBuilder sb = new StringBuilder("Call Stack (max depth " + maxDepth + "):\n");
		// Start at index 2 to skip Thread.getStackTrace and this method itself.
		// Some JVMs might have more initial internal frames, adjust 'startIndex' if needed.
		int startIndex = 2;
		// It's good practice to find the actual caller of getCurrentStackTrace if the
		// stack trace structure is very consistent across JVMs or after testing.
		// For simplicity, we use a fixed startIndex.
		for (int i = startIndex; i < stes.length && (maxDepth <= 0 || (i - startIndex) < maxDepth); i++) {
			StackTraceElement ste = stes[i];
			sb.append("\tat ").append(ste.getClassName()).append(".").append(ste.getMethodName()).append("(").append(ste.getFileName()).append(":").append(ste.getLineNumber())
			        .append(")\n");
		}
		if (stes.length == startIndex && maxDepth > 0) { // Handle case where stack is shallower than expected
			sb.append("\t(Stack trace was too shallow to display frames)\n");
		}
		return sb.toString();
	}

	private static synchronized void initFileHandler() {
		if (handlerFile != null) {
			return;
		}

		try {
			File logFile = prepareLogFile(LOG_DIR_NAME, LOG_FILE_NAME);

			// Handler initialisieren
			handlerFile = new FileHandler(logFile.getPath(), false);
			handlerFile.setFormatter(new CustomXMLFormatter());
			handlerFile.setLevel(Level.SEVERE);

			LOGGER.addHandler(handlerFile);
			LOGGER.log(Level.CONFIG, "File logging initialized for SEVERE messages at: " + logFile.getAbsolutePath());

			// Ausstehende Records verarbeiten
			LogRecord record;
			while ((record = pendingSevereRecords.poll()) != null) {
				handlerFile.publish(record);
			}

		} catch (IOException | SecurityException ex) {
			// Fallback zur Konsole, da FileHandler fehlgeschlagen
			LOGGER.log(Level.SEVERE, "Failed to initialize file handler for " + LOG_FILE_NAME, ex);
		}
	}

	/**
	 * Ensures the directory exists and returns the File object for the log file.
	 * 
	 * @param dirName  The name of the directory.
	 * @param fileName The name of the log file.
	 * @return The File object pointing to the log file.
	 * @throws IOException If the directory cannot be created.
	 */
	private static File prepareLogFile(String dirName, String fileName) throws IOException {
		File directory = new File(dirName);
		if (!directory.exists()) {
			boolean created = directory.mkdirs();
			if (!created && !directory.isDirectory()) {
				throw new IOException("Could not create logging directory: " + directory.getAbsolutePath());
			}
		}
		return new File(directory, fileName);
	}

	/**
	 * Gets the underlying JUL Logger instance. Use with caution, prefer the static methods.
	 * 
	 * @return The Logger instance.
	 */
	public static Logger getLogger() {
		return LOGGER;
	}

	// --- Recommendation: Remove or Move Null Check Methods ---
	// These mix validation with logging. Consider a dedicated ValidationUtils class.
	/*
	 * public static boolean isNull(Object obj) { ... } public static boolean isNullParam(Object obj) { ... } public static boolean checkForNull(Object obj, String
	 * logMessageFormat, String varName) { ... } public static boolean isAnyNull(Object... objects) { ... }
	 */

	/**
	 * Safely formats a log message, handling null format strings and formatting exceptions. Passes arguments directly to {@link String#format(String, Object...)}.
	 *
	 * @param formatMessage The format string (potentially null).
	 * @param args          Arguments for the format string (e.g., Integer for %d, String for %s).
	 * @return The formatted string, or an error indicator if formatting fails.
	 */
	private static String format(String formatMessage, Object... args) {
		// Initial checks
		if (formatMessage == null) {
			LOGGER.logp(Level.WARNING, Log.class.getName(), "format", "Formatting attempted with a null formatMessage string.");
			return "[NULL FORMAT MESSAGE]";
		}
		if (args == null || args.length == 0 || !formatMessage.contains("%")) {
			return formatMessage; // No formatting needed
		}

		try {
			// *** THE FIX: Pass original args directly ***
			return String.format(formatMessage, args);
		} catch (IllegalFormatException ife) {
			// Log formatting errors clearly
			LOGGER.logp(Level.SEVERE, Log.class.getName(), "format", "Log message formatting failed. Format: '" + formatMessage + "' Args: " + Arrays.toString(args), ife);
			return "[FORMATTING ERROR] " + formatMessage;
		}
	}
	// --- Standard Log Methods ---

	/** Logs a message at the {@link Level#FINE} level. Use for detailed tracing. */
	public static void fine(String formatMessage, Object... args) {
		log(Level.FINE, formatMessage, null, args);
	}

	/** Logs a message at the {@link Level#FINER} level. Use for debugging during development. */
	public static void debug(String formatMessage, Object... args) {
		log(Level.FINER, formatMessage, null, args);
	}

	/** Logs a message at the {@link Level#INFO} level. Use for application progress/state. */
	public static void info(String formatMessage, Object... args) {
		log(Level.INFO, formatMessage, null, args);
	}

	/** Logs a message at the {@link Level#WARNING} level. Use for potential problems. */
	public static void warn(String formatMessage, Object... args) {
		log(Level.WARNING, formatMessage, null, args);
	}

	/** Logs a message at the {@link Level#WARNING} level with an associated exception. */
	public static void warn(Throwable ex, String formatMessage, Object... args) {
		log(Level.WARNING, formatMessage, ex, args);
	}

	/** Logs an exception at the {@link Level#WARNING} level. */
	public static void warn(Throwable ex) {
		log(Level.WARNING, null, ex); // No additional message
	}

	/** Logs a message at the {@link Level#SEVERE} level. Use for serious errors. */
	public static void error(String formatMessage, Object... args) {
		log(Level.SEVERE, formatMessage, null, args);
	}

	/** Logs a message at the {@link Level#SEVERE} level with an associated exception. */
	public static void error(Throwable ex, String formatMessage, Object... args) {
		log(Level.SEVERE, formatMessage, ex, args);
	}

	/** Logs an exception at the {@link Level#SEVERE} level. */
	public static void error(Throwable ex) {
		log(Level.SEVERE, null, ex); // No additional message
	}
	
	/** Logs an exception and message at the {@link Level#SEVERE} level. */
    public static void severe(Throwable ex, String formatMessage, Object... args) {
        // Leitet einfach an die error-Methode weiter, die bereits Level.SEVERE loggt.
        error(ex, formatMessage, args);
    }

    /** Logs an exception at the {@link Level#SEVERE} level. */
    public static void severe(Throwable ex) {
        // Leitet einfach an die error-Methode weiter.
        error(ex);
    }

	// --- JavaFX Alert Display Methods (Use with caution - UI Coupling) ---

	/** Logs at INFO level and shows a JavaFX Information alert. Requires FX Toolkit. */
	public static void infoShow(String formatMessage, Object... args) {
		infoShow(null, formatMessage, args); // No specific owner window
	}

	/** Logs at WARNING level and shows a JavaFX Warning alert. Requires FX Toolkit. */
	public static void warnShow(String formatMessage, Object... args) {
		warnShow(null, formatMessage, args); // No specific owner window
	}

	/** Logs at SEVERE level and shows a JavaFX Error alert. Requires FX Toolkit. */
	public static void errorShow(String formatMessage, Object... args) {
		errorShow(null, formatMessage, args); // No specific owner window
	}

	/** Logs at INFO level and shows a JavaFX Information alert owned by the given window. Requires FX Toolkit. */
	public static void infoShow(Window owner, String formatMessage, Object... args) {
		final String msg = format(formatMessage, args);
		log(Level.INFO, msg, null); // Log regardless of alert success
		showMessageAlert(AlertType.INFORMATION, owner, ALERT_TITLE_INFORMATION, msg);
	}

	/** Logs at WARNING level and shows a JavaFX Warning alert owned by the given window. Requires FX Toolkit. */
	public static void warnShow(Window owner, String formatMessage, Object... args) {
		final String msg = format(formatMessage, args);
		log(Level.WARNING, msg, null); // Log regardless of alert success
		showMessageAlert(AlertType.WARNING, owner, ALERT_TITLE_WARNING, msg);
	}

	/** Logs at SEVERE level and shows a JavaFX Error alert owned by the given window. Requires FX Toolkit. */
	public static void errorShow(Window owner, String formatMessage, Object... args) {
		final String msg = format(formatMessage, args);
		log(Level.SEVERE, msg, null); // Log regardless of alert success
		showMessageAlert(AlertType.ERROR, owner, ALERT_TITLE_ERROR, msg);
	}

	/** Logs an exception at SEVERE level and shows a JavaFX Error alert with details. Requires FX Toolkit. */
	public static void exceptionShow(Throwable ex, Window owner) {
		// Use the internal log method which finds caller and creates record
		StackTraceElement caller = findCaller(); // Find caller before creating record
		LogRecord record = createLogRecord(Level.SEVERE, null, ex, caller);
		log(record); // Log the prepared record
		showAlert(record, owner); // Show alert using the same record data
	}

	/** Logs an exception and message at SEVERE level, shows a JavaFX Error alert. Requires FX Toolkit. */
	public static void exceptionShow(Throwable ex, String formatMessage, Object... args) {
		StackTraceElement caller = findCaller();
		String formattedMsg = format(formatMessage, args);
		LogRecord record = createLogRecord(Level.SEVERE, formattedMsg, ex, caller);
		log(record); // Log the prepared record
		showAlert(record, null); // Show alert (no owner specified)
	}

	// --- Private Helper Methods ---

	/**
	 * Internal core log method. Formats message, finds caller, creates LogRecord, and logs if level enabled.
	 *
	 * @param level         Level to log at.
	 * @param formatMessage Optional format string for the message.
	 * @param thrown        Optional exception.
	 * @param args          Arguments for the format string.
	 */
	private static void log(Level level, String formatMessage, Throwable thrown, Object... args) {
		// Check level *before* doing any work (formatting, finding caller)
		if (LOGGER.isLoggable(level)) {
			StackTraceElement caller = findCaller(); // Find caller info
			String message = format(formatMessage, args); // Format the message safely
			LogRecord logRecord = createLogRecord(level, message, thrown, caller);
			LOGGER.log(logRecord); // Pass the complete record to the logger
		}
	}

	/** Internal helper to log a pre-constructed LogRecord. */
	private static void log(LogRecord logRecord) {
		// Check level before logging the record
		if (LOGGER.isLoggable(logRecord.getLevel())) {
			LOGGER.log(logRecord);
		}
	}

	/**
	 * Creates a LogRecord, setting source info from the provided caller element. Uses current time as the timestamp.
	 *
	 * @param level   Level of the record.
	 * @param message The log message (already formatted).
	 * @param thrown  Optional associated exception.
	 * @param caller  The stack trace element of the calling method.
	 * @return A configured LogRecord instance.
	 */
	private static LogRecord createLogRecord(Level level, String message, Throwable thrown, StackTraceElement caller) {
		LogRecord logRecord = new LogRecord(level, message); // Sets timestamp implicitly

		logRecord.setLoggerName(LOGGER.getName()); // Set logger name
		logRecord.setThrown(thrown); // Set exception

		// Set source information if available
		if (caller != null) {
			logRecord.setSourceClassName(caller.getClassName());
			logRecord.setSourceMethodName(caller.getMethodName());
		} else {
			// Fallback if caller couldn't be determined
			logRecord.setSourceClassName("UnknownSource");
			logRecord.setSourceMethodName("UnknownMethod");
		}
		// Thread ID and sequence number are handled by JUL internally
		return logRecord;
	}

	/**
	 * Helper to find the first stack trace element *outside* this Log class.
	 *
	 * @return The StackTraceElement of the caller, or null if not found.
	 */
	private static StackTraceElement findCaller() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		String logClassName = Log.class.getName();

		// Iterate stack: Skip getStackTrace, skip findCaller, skip log(...) methods
		for (int i = 1; i < stackTrace.length; i++) {
			StackTraceElement element = stackTrace[i];
			if (!element.getClassName().equals(logClassName)) {
				// This is the first method outside the Log class, likely the original caller
				return element;
			}
		}
		// Should not happen in normal operation
		LOGGER.log(Level.WARNING, "Could not determine log caller source.");
		return null;
	}

	/** Safely shows a simple message alert using WindowUtils, handling potential FX availability issues. */
	private static void showMessageAlert(AlertType alertType, Window owner, String title, String message) {
		if (!isFxToolkitAvailable()) {
			LOGGER.log(Level.CONFIG, "JavaFX Toolkit not available or not on FX thread. Skipping alert: " + title);
			return;
		}
		// Ensure UI operations run on the JavaFX Application Thread
		Platform.runLater(() -> {
			try {
				WindowUtils.showAlertDialog(alertType, owner, title, null, message);
			} catch (Exception ex) {
				LOGGER.log(Level.WARNING, "showMessageAlert(): Failed to show message alert!", ex);
			}
		});
	}

	/** Safely shows a detailed exception alert using WindowUtils, handling potential FX availability issues. */
	private static void showAlert(LogRecord logRecord, Window owner) {
		if (!isFxToolkitAvailable()) {
			LOGGER.log(Level.CONFIG, "JavaFX Toolkit not available or not on FX thread. Skipping detailed alert for: " + logRecord.getMessage());
			return;
		}

		// Die Logik zur Erstellung von Titel, Header und userFriendlyMessage
		// kann hier bleiben oder in buildUserFriendlyErrorSummary verfeinert werden.
		String shortUserMessage = Objects.toString(logRecord.getMessage(), "");
		Throwable exception = logRecord.getThrown();

		String sourceClass = Objects.toString(logRecord.getSourceClassName(), "UnknownClass");
		String sourceMethod = Objects.toString(logRecord.getSourceMethodName(), "UnknownMethod");

		// Benutzerfreundliche Texte erstellen
		String title = "Application " + determineAlertType(logRecord.getLevel()).toString(); // z.B. "Application Error"
		String header = "An issue occurred in " + sourceClass + "." + sourceMethod + "()";
		if (StringUtils.isNotBlank(shortUserMessage)) {
			header = shortUserMessage; // Wenn eine spezifische Nachricht im LogRecord ist, diese als Header nehmen
		} else if (exception != null && StringUtils.isNotBlank(exception.getMessage())) {
			header = exception.getMessage(); // Sonst die Exception Message
		}

		// Der 'content' für showExceptionDialog ist die primäre, kurze Nachricht.
		// Wenn die Exception eine Nachricht hat und diese nicht schon im Header steht, hier anzeigen.
		String mainDialogContent = "Please see details below or check the application logs.";
		if (exception != null && StringUtils.isNotBlank(exception.getMessage()) && !header.equals(exception.getMessage())) {
			mainDialogContent = exception.getMessage();
		} else if (StringUtils.isNotBlank(shortUserMessage)) {
			mainDialogContent = shortUserMessage; // Wenn LogRecord Message vorhanden, diese nutzen
		}

		AlertType type = determineAlertType(logRecord.getLevel());

		// Rufe die NEUE WindowUtils-Methode auf
		WindowUtils.showExceptionDialog(type, owner, title, header, mainDialogContent, exception);
	}

	/** Checks if the JavaFX toolkit is initialized and the current thread is the FX thread. */
	private static boolean isFxToolkitAvailable() {
		try {
			// Try accessing Platform.isFxApplicationThread()
			// This can throw an IllegalStateException if toolkit is not running.
			return Platform.isFxApplicationThread();
		} catch (IllegalStateException e) {
			// Toolkit not running or initialized
			return false;
		} catch (Exception e) {
			// Catch other potential runtime issues just in case
			LOGGER.log(Level.WARNING, "Unexpected error checking FX toolkit availability", e);
			return false;
		}
	}

	/** Determines AlertType based on Log Level */
	private static AlertType determineAlertType(Level level) {
		if (level.intValue() >= Level.SEVERE.intValue()) {
			return AlertType.ERROR;
		} else if (level.intValue() >= Level.WARNING.intValue()) {
			return AlertType.WARNING;
		} else {
			return AlertType.INFORMATION; // Default for INFO and lower
		}
	}

	// --- Alert Content Building Helpers (Refined) ---
	/**
	 * Logs at SEVERE level and shows a localized JavaFX Error alert.
	 * 
	 * @param owner      The owner Window for the alert.
	 * @param titleKey   The localizable key for the alert title.
	 * @param messageKey The localizable key for the alert message.
	 * @param args       Arguments for the message format string.
	 */
	public static void errorShowLocalized(Window owner, LocalizableKey titleKey, LocalizableKey messageKey, Object... args) {
		// 1. Get the translated and formatted strings from your LangKeys.
		final String title = titleKey.get();
		final String message = messageKey.getFormatted(args);

		// 2. Log the untranslated keys for developer clarity and the formatted message.
		log(Level.SEVERE, String.format("Showing localized alert [%s]: %s", messageKey.getKey(), message), null);

		// 3. Call the existing, non-localized show method with the final strings.
		showMessageAlert(AlertType.ERROR, owner, title, message);
	}

	/**
	 * Logs an exception and shows a localized, detailed JavaFX Error alert.
	 * 
	 * @param ex         The exception to display.
	 * @param owner      The owner Window for the alert.
	 * @param titleKey   The localizable key for the alert title.
	 * @param headerKey  The localizable key for the alert's header text.
	 * @param contentKey The localizable key for the main content message.
	 * @param args       Arguments for the content format string.
	 */
	public static void exceptionShowLocalized(Throwable ex, Window owner, LocalizableKey titleKey, LocalizableKey headerKey, LocalizableKey contentKey, Object... args) {
		// 1. Get the translated and formatted strings.
		final String title = titleKey.get();
		final String header = headerKey.getFormatted(args); // Assume header might be formatted
		final String content = contentKey.getFormatted(args); // Assume content is the main formatted message

		// 2. Log the raw information for debugging.
		log(Level.SEVERE, String.format("Showing localized exception alert [%s]: %s", contentKey.getKey(), content), ex);

		// 3. Call the core WindowUtils method directly to show the detailed dialog.
		// We bypass the old showAlert to have more control over the texts.
		if (!isFxToolkitAvailable()) {
			// Log a fallback message if UI is not available
			return;
		}
		Platform.runLater(() -> {
			WindowUtils.showExceptionDialog(AlertType.ERROR, owner, title, header, content, ex);
		});
	}

	/** Private constructor to prevent instantiation of utility class. */
	private Log() {
		throw new IllegalStateException("Utility class");
	}

	// --- Custom XML Formatter (Corrected) ---
	private static class CustomXMLFormatter extends Formatter {
		// Using ISO 8601 format is generally preferred for XML timestamps
		private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"); // ISO 8601 with timezone

		@Override
		public String format(LogRecord record) {
			StringBuilder sb = new StringBuilder(1024);
			sb.append("<record>\n");
			sb.append("  <timestamp>").append(DATE_FORMAT.format(new Date(record.getMillis()))).append("</timestamp>\n");
			sb.append("  <sequence>").append(record.getSequenceNumber()).append("</sequence>\n");
			sb.append("  <level>").append(escapeXml(record.getLevel().getName())).append("</level>\n"); // Escape level name just in case
			sb.append("  <threadID>").append(record.getLongThreadID()).append("</threadID>\n"); // Renamed from <thread>
			// Escape source info safely
			sb.append("  <sourceClass>").append(escapeXml(record.getSourceClassName())).append("</sourceClass>\n"); // Renamed from <class>
			sb.append("  <sourceMethod>").append(escapeXml(record.getSourceMethodName())).append("</sourceMethod>\n");// Renamed from <method>
			// Escape message content
			sb.append("  <message>").append(escapeXml(formatMessage(record))).append("</message>\n");

			Throwable thrown = record.getThrown();
			if (thrown != null) {
				sb.append("  <exception>\n");
				sb.append("    <type>").append(escapeXml(thrown.getClass().getName())).append("</type>\n");
				sb.append("    <message>").append(escapeXml(thrown.getMessage())).append("</message>\n");
				// Wrap stack trace in CDATA section
				sb.append("    <stackTrace><![CDATA[\n");
				StringWriter sw = new StringWriter();
				try (PrintWriter pw = new PrintWriter(sw)) {
					thrown.printStackTrace(pw);
				}
				sb.append(sw.toString()); // Append stack trace content
				sb.append("]]></stackTrace>\n");
				sb.append("  </exception>\n");
			}
			sb.append("</record>\n");
			return sb.toString();
		}

		/**
		 * Escapes characters problematic for XML text content: &, <, >. Also handles quotes for potential attribute use later. Handles null input.
		 */
		private String escapeXml(String text) {
			if (text == null) {
				return "";
			}
			// Perform replacements sequentially
			// NOTE: Ensure you use standard straight quotes " and ' when typing/pasting!
			String result = text;
			result = result.replace("&", "&"); // Ampersand MUST be first
			result = result.replace("<", "<");
			result = result.replace(">", ">");
			result = result.replace("\"", "&quot;");
			result = result.replace("'", "'"); // Escape single quote
			return result;
		}
	}

}