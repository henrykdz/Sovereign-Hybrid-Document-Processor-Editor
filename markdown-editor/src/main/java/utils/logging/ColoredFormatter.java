package utils.logging;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
// Import necessary for exception stack trace in updated main method
import java.io.PrintWriter;
import java.io.StringWriter;

public class ColoredFormatter extends Formatter {
	private static final String ANSI_RESET        = "\u001B[0m";
	private static final String ANSI_DARK_WHITE   = "\u001B[90m"; // Grey (used for FINE)
	private static final String ANSI_YELLOW       = "\u001B[33m"; // Warning
	private static final String ANSI_RED          = "\u001B[31m"; // Severe
	private static final String ANSI_PASTEL_GREEN = "\u001B[92m"; // Info (Pastel Green)
	// Add constant for standard white
	private static final String ANSI_WHITE = "\u001B[37m"; // Standard White (for FINER/Debug)

	// Fields remain exactly the same
	private boolean showDateTime;
	private boolean showThreadID;
	private boolean showClassName;
	private boolean showMethodName;
	private boolean fullClassName = true;

	// Constructor remains exactly the same
	public ColoredFormatter(boolean showDateTime, boolean showThreadID, boolean showClassName, boolean showMethodName) {
		this.showDateTime = showDateTime;
		this.showThreadID = showThreadID;
		this.showClassName = showClassName;
		this.showMethodName = showMethodName;
	}

	// setFullClassName remains exactly the same
	public void setFullClassName(boolean fullClassName) {
		this.fullClassName = fullClassName;
	}

	// format method remains exactly the same
	@Override
	public String format(LogRecord record) {
		StringBuilder sb = new StringBuilder();
		Level level = record.getLevel();
		String color = getColorForLevel(level); // This will now handle FINER
		String coloredMessage = color + formatMessage(record) + ANSI_RESET;
		String coloredLevel = color + level.getName() + ANSI_RESET;

		if (showDateTime) {
			sb.append(String.format("[%1$tF %1$tT] ", record.getMillis()));
		}
		if (showThreadID) {
			// Use getLongThreadID if available (Java 9+), fallback if needed
			try {
				// Check if method exists (reflection - less ideal but avoids compile error on older JDKs)
				// Or just call it if compiling with JDK 9+
				sb.append(String.format("[Thread-%d] ", record.getLongThreadID()));
			} catch (NoSuchMethodError e) {
				// Fallback for older JDKs (though thread ID might not be long)
				sb.append(String.format("[Thread-%d] ", record.getLongThreadID()));
			}
		}
		sb.append(String.format("[%s] ", coloredLevel));

		if (showClassName) {
			String classNameAbsolute = record.getSourceClassName();
			// Handle null source class name gracefully
			if (classNameAbsolute != null) {
				String className = fullClassName ? classNameAbsolute : classNameAbsolute.substring(classNameAbsolute.lastIndexOf('.') + 1);
				sb.append('(' + className).append(".java");
				int lineNumber = getLineNumber(record);
				sb.append(":").append(lineNumber).append(')');
				if (showMethodName) {
					String methodName = record.getSourceMethodName();
					if (methodName != null) { // Handle null method name
						sb.append(" - ").append(methodName);
					}
				}
			} else {
				sb.append("(Unknown Source)"); // Placeholder if source class is null
			}
		} else if (showMethodName) {
			String methodName = record.getSourceMethodName();
			if (methodName != null) { // Handle null method name
				sb.append(methodName);
			}
		}

		sb.append(": ").append(coloredMessage); // Removed '\n' here, let line separator handle it

		// Use System line separator for consistency
		sb.append(System.lineSeparator());

		// Append exception details if available - improved slightly
		Throwable thrown = record.getThrown();
		if (thrown != null) {
			// Maybe use RED for exception traces regardless of original level? Or stick to level color?
			// Let's stick to original level's color for now.
			String exceptionColor = color; // Or use ANSI_RED always?
			StringWriter sw = new StringWriter();
			try (PrintWriter pw = new PrintWriter(sw)) {
				sb.append(exceptionColor).append("Stack Trace:").append(ANSI_RESET).append(System.lineSeparator());
				thrown.printStackTrace(pw);
			}
			sb.append(sw.toString());
			if (!sw.toString().endsWith(System.lineSeparator())) {
				sb.append(System.lineSeparator());
			}
		}

		return sb.toString();
	}

	// getLineNumber remains exactly the same
	private int getLineNumber(LogRecord record) {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		String sourceClassName = record.getSourceClassName();
		String sourceMethodName = record.getSourceMethodName();
		// Basic null check added for safety
		if (sourceClassName == null || sourceMethodName == null)
			return -1;

		for (StackTraceElement element : stackTrace) {
			if (element.getClassName().equals(sourceClassName) && element.getMethodName().equals(sourceMethodName)) {
				return element.getLineNumber();
			}
		}
		return -1;
	}

	// --- MODIFIED SECTION ---
	private String getColorForLevel(Level level) {
		// Use direct comparison for standard levels, should be safe
		if (level == Level.SEVERE) {
			return ANSI_RED;
		} else if (level == Level.WARNING) {
			return ANSI_YELLOW;
		} else if (level == Level.INFO) {
			return ANSI_PASTEL_GREEN;
		} else if (level == Level.FINER) { // Added check for FINER (Debug)
			return ANSI_WHITE; // Use standard white
		} else if (level == Level.FINE) {
			return ANSI_DARK_WHITE; // Grey
		} else {
			// Default for other levels (FINEST, CONFIG, etc.) - no color or maybe grey?
			// Let's keep it no color as per original 'else'
			return "";
		}
	}
	// --- END MODIFIED SECTION ---

	// Updated main method for testing FINER level
	public static void main(String[] args) {
		java.util.logging.Logger logger = java.util.logging.Logger.getLogger("MyLoggerTest");
		// Prevent duplicate output to root logger's handlers
		logger.setUseParentHandlers(false);
		// Set logger level low enough to process FINER messages
		logger.setLevel(Level.ALL);

		ColoredFormatter formatter = new ColoredFormatter(true, true, true, true);
		formatter.setFullClassName(false); // Use simple class names

		java.util.logging.ConsoleHandler handler = new java.util.logging.ConsoleHandler();
		handler.setFormatter(formatter);
		// Set handler level low enough to output FINER messages
		handler.setLevel(Level.ALL);

		logger.addHandler(handler);

		System.out.println("--- Logging Examples (FINER should be White) ---");
		logger.info("This is an info message (Pastel Green).");
		helperMethod(logger); // Call helper to test FINER from another frame
		logger.fine("This is a fine message (Grey).");
		logger.warning("This is a warning message (Yellow).");
		logger.severe("This is a severe message (Red).");
		logger.finest("This is a finest message (No Color/Default)."); // Test default

		try {
			@SuppressWarnings("unused")
			int i = 1 / 0;
		} catch (ArithmeticException e) {
			System.err.flush();
			System.out.flush(); // Try flushing before logging error
			logger.log(Level.SEVERE, "Caught an expected exception!", e);
		}
		System.out.flush(); // Final flush
		System.out.println("--- End Examples ---");
	}

	private static void helperMethod(java.util.logging.Logger logger) {
		// Log a FINER message from here
		logger.log(Level.FINER, "This is a debug message (FINER - should be White). Value={0}", 123);
	}
}