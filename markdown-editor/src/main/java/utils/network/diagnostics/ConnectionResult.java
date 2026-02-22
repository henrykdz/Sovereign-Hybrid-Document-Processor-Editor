package utils.network.diagnostics;



import java.net.ConnectException; // Import needed for instanceof checks below
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException; // Import needed for instanceof checks below
import java.net.UnknownHostException; // Import needed for instanceof checks below
import java.util.Objects;

/**
 * Represents the outcome of a network diagnostic check (e.g., ping, port connection, proxy test). Includes a status code, a descriptive message, measured latency, and any
 * exception caught. This record is immutable.
 *
 * @param statusCode    An integer code representing the result (e.g., {@link HttpURLConnection#HTTP_OK} (200) for success, -1 for generic error/failure).
 * @param message       A descriptive message about the outcome (e.g., "Connection successful", "Connection timed out"). Never null.
 * @param latencyMillis The time taken for the operation in milliseconds (-1 if not applicable, e.g., failed before timing started or errored out).
 * @param exception     The exception caught during the operation, or null if none occurred or the failure wasn't due to an exception (e.g., logical failure like 'unreachable').
 */
public record ConnectionResult(int statusCode, String message, long latencyMillis, Exception exception) {

	/**
	 * Canonical constructor. Ensures the message is not null. Validates status code conventions (optional but good practice).
	 */
	public ConnectionResult {
		Objects.requireNonNull(message, "Message cannot be null");
		// Optional: Add validation if you have stricter rules for statusCode or latency
		// if (latencyMillis < -1) {
		// throw new IllegalArgumentException("Latency cannot be less than -1");
		// }
	}

	// --- Static Factory Methods (Preferred over public constructors for records) ---

	/**
	 * Creates a result representing a successful operation. Status code is set to {@link HttpURLConnection#HTTP_OK}.
	 *
	 * @param message       The success message (e.g., "Host is reachable"). Must not be null.
	 * @param latencyMillis The measured latency in milliseconds (>= 0).
	 * @return A new ConnectionResult instance representing success.
	 */
	public static ConnectionResult success(String message, long latencyMillis) {
		Objects.requireNonNull(message, "Success message cannot be null");
		if (latencyMillis < 0) {
			// Optional: Log warning or throw if latency must be positive for success
			System.err.println("Warning: Creating success ConnectionResult with negative latency: " + latencyMillis);
			// latencyMillis = 0; // Or clamp it
		}
		return new ConnectionResult(HttpURLConnection.HTTP_OK, message, Math.max(0, latencyMillis), null);
	}

	/**
	 * Creates a result representing a failure that did *not* necessarily involve an exception (e.g., host unreachable within timeout, logical failure). Status code is set to -1
	 * (generic failure).
	 *
	 * @param message       The failure message (e.g., "Host is unreachable"). Must not be null.
	 * @param latencyMillis The measured latency, potentially -1 if timing wasn't relevant or completed.
	 * @return A new ConnectionResult instance representing a non-exception failure.
	 */
	public static ConnectionResult failure(String message, long latencyMillis) {
		Objects.requireNonNull(message, "Failure message cannot be null");
		return new ConnectionResult(-1, message, latencyMillis, null);
	}

	/**
	 * Creates a result representing a failure that did *not* necessarily involve an exception. Status code is set to -1 (generic failure). Latency is set to -1.
	 *
	 * @param message The failure message (e.g., "No target hosts provided"). Must not be null.
	 * @return A new ConnectionResult instance representing a non-exception failure.
	 */
	public static ConnectionResult failure(String message) {
		return failure(message, -1L);
	}

	/**
	 * Creates a result representing an error caused by an exception. Status code is set to -1 (generic error).
	 *
	 * @param message       The error message (often derived from the exception). Must not be null.
	 * @param latencyMillis The latency measured before the exception occurred (-1 if not applicable).
	 * @param exception     The caught exception. Can be null, but typically shouldn't be for this factory.
	 * @return A new ConnectionResult instance representing an error.
	 */
	public static ConnectionResult error(String message, long latencyMillis, Exception exception) {
		Objects.requireNonNull(message, "Error message cannot be null");
		// Exception *could* technically be null, but the method name implies one exists.
		// Objects.requireNonNull(exception, "Exception cannot be null for error result"); // Consider if exception is mandatory here
		return new ConnectionResult(-1, message, latencyMillis, exception);
	}

	/**
	 * Creates a result representing an error caused by an exception, setting latency to -1. Status code is set to -1 (generic error).
	 *
	 * @param message   The error message (often derived from the exception). Must not be null.
	 * @param exception The caught exception. Can be null, but typically shouldn't be for this factory.
	 * @return A new ConnectionResult instance representing an error.
	 */
	public static ConnectionResult error(String message, Exception exception) {
		return error(message, -1L, exception);
	}

	// --- Instance Helper Methods ---

	/**
	 * Checks if the diagnostic attempt was successful. Success is defined as having a status code of {@link HttpURLConnection#HTTP_OK} (200) AND no associated exception.
	 *
	 * @return {@code true} if the operation succeeded, {@code false} otherwise.
	 */
	public boolean isSuccessful() {
		// Primary definition of success: OK status and no exception.
		return statusCode == HttpURLConnection.HTTP_OK && exception == null;
	}

	/**
	 * Checks if the diagnostic attempt represents a failure or error. This is the logical opposite of {@link #isSuccessful()}.
	 *
	 * @return {@code true} if the operation failed or resulted in an error, {@code false} otherwise.
	 */
	public boolean isFailure() {
		return !isSuccessful();
	}

	/**
	 * Checks if an exception was recorded for this result. Note: A failure result might exist without an exception (e.g., logical failure).
	 *
	 * @return {@code true} if an exception is present, {@code false} otherwise.
	 */
	public boolean hasException() {
		return exception != null;
	}

	/**
	 * Checks if the failure was specifically due to a socket timeout.
	 *
	 * @return {@code true} if the recorded exception is a {@link SocketTimeoutException}, {@code false} otherwise.
	 */
	public boolean isTimeout() {
		return exception instanceof SocketTimeoutException;
	}

	/**
	 * Checks if the failure was specifically due to a connection refused error. This typically indicates the host is reachable, but the target port is closed or not listening. It
	 * checks for {@link ConnectException} and often includes a check for "refused" in the message for higher certainty, as ConnectException can have other causes (though less
	 * common).
	 *
	 * @return {@code true} if the result indicates a 'Connection Refused' error, {@code false} otherwise.
	 */
	public boolean isConnectionRefused() {
		// Check for ConnectException first.
		if (!(exception instanceof ConnectException)) {
			return false;
		}
		// Then check the message for higher confidence. This can be locale/OS dependent but works often.
		String exceptionMessage = exception.getMessage();
		return exceptionMessage != null && exceptionMessage.toLowerCase().contains("refused");
		// Alternative (less specific, but avoids message parsing):
		// return exception instanceof java.net.ConnectException;
	}

	/**
	 * Checks if the failure was specifically due to an unknown host error (DNS resolution failure).
	 *
	 * @return {@code true} if the recorded exception is an {@link UnknownHostException}, {@code false} otherwise.
	 */
	public boolean isUnknownHost() {
		return exception instanceof UnknownHostException;
	}

	// --- toString (Customized for Readability) ---

	/**
	 * Provides a concise string representation of the ConnectionResult. Includes status code, message, latency, and the simple name of the exception class if present.
	 *
	 * @return A string representation of this record.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ConnectionResult[");
		sb.append("status=").append(statusCode);
		sb.append(", message='").append(message).append('\'');
		sb.append(", latency=").append(latencyMillis).append("ms");
		sb.append(", exception=");
		sb.append(exception != null ? exception.getClass().getSimpleName() : "null");
		// Optional: Add exception message if useful and not too long
		// if (exception != null && exception.getMessage() != null) {
		// sb.append("(\"").append(exception.getMessage()).append("\")");
		// }
		sb.append(']');
		return sb.toString();
	}
}