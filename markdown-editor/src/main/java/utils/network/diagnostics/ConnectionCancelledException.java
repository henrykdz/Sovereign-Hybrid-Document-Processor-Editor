package utils.network.diagnostics;

import java.io.IOException;

/**
 * Indicates that a network operation (like a connection attempt) was intentionally cancelled, typically due to a thread interrupt resulting from a user action (e.g., pressing a
 * stop button).
 */
public class ConnectionCancelledException extends IOException { // Von IOException erben ist sinnvoll

	private static final long serialVersionUID = 1L; // Standard für serialisierbare Klassen

	/**
	 * Constructs a new ConnectionCancelledException with the specified detail message.
	 *
	 * @param message the detail message.
	 */
	public ConnectionCancelledException(String message) {
		super(message);
	}

	/**
	 * Constructs a new ConnectionCancelledException with the specified detail message and cause.
	 *
	 * @param message the detail message.
	 * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method). (A {@code null} value is permitted, and indicates that the cause is
	 *                nonexistent or unknown.)
	 */
	public ConnectionCancelledException(String message, Throwable cause) {
		super(message, cause);
	}
}