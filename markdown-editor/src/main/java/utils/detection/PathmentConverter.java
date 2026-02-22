package utils.detection;


import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import utils.logging.Log;
import utils.network.TransferProtocol;

/**
 * A static utility class for converting standard Java I/O and networking objects into the application-specific {@link Pathment} object. This class acts as an adapter layer.
 */
public final class PathmentConverter {

	// Private constructor to prevent instantiation.
	private PathmentConverter() {
	}

	/**
	 * Converts a {@link java.nio.file.Path} to a {@link Pathment}. This method is the most reliable way to create a file-based Pathment, as it bypasses string parsing entirely.
	 * 
	 * @param path The Path object to convert.
	 * @return A new Pathment object, or an UNSPECIFIED Pathment if the input is null.
	 */
	public static Pathment from(Path path) {
		if (path == null) {
			Log.warn("Cannot convert from null Path.");
			return Pathment.createUnspecified("null_path_input");
		}
		// Use the dedicated FilePath splitter and the Pathment factory.
		// This is the most direct and reliable conversion path.
		FilePath filePath = PathmentExtractor.splitFilePath(path.toString());
		return Pathment.fromFilePath(filePath);
	}

	/**
	 * Converts a {@link java.io.File} to a {@link Pathment}. It internally converts the File to a Path for consistent processing.
	 * 
	 * @param file The File object to convert.
	 * @return A new Pathment object, or an UNSPECIFIED Pathment if the input is null.
	 */
	public static Pathment from(File file) {
		if (file == null) {
			Log.warn("Cannot convert from null File.");
			return Pathment.createUnspecified("null_file_input");
		}
		return from(file.toPath()); // Delegate to the more modern Path-based method.
	}

	/**
	 * Converts a {@link java.net.URI} to a {@link Pathment}. This delegates to the robust string parser in PathmentExtractor.
	 * 
	 * @param uri The URI object to convert.
	 * @return A new Pathment object, or an UNSPECIFIED Pathment if the input is null.
	 */
	public static Pathment from(URI uri) {
		if (uri == null) {
			Log.warn("Cannot convert from null URI.");
			return Pathment.createUnspecified("null_uri_input");
		}
		// The extractor's parser is the single source of truth for string parsing.
		return PathmentExtractor.parse(uri.toString());
	}

	/**
	 * Converts a {@link java.net.URL} to a {@link Pathment}. It internally converts the URL to a URI for consistent processing.
	 * 
	 * @param url The URL object to convert.
	 * @return A new Pathment object, or an UNSPECIFIED Pathment on failure.
	 */
	public static Pathment from(URL url) {
		if (url == null) {
			Log.warn("Cannot convert from null URL.");
			return Pathment.createUnspecified("null_url_input");
		}
		try {
			return from(url.toURI()); // Delegate to the URI-based method.
		} catch (URISyntaxException e) {
			Log.error(e, "Failed to convert URL to URI: " + url);
			// Use the factory that allows specifying a better title
			return Pathment.createUnspecified(url.toString(), "Malformed URL");
		}
	}

	/**
	 * Creates a new Pathment by switching the protocol of an existing web-based Pathment. For example, switching from HTTP to HTTPS.
	 * 
	 * @param original    The original Pathment object. Must be a web URL type.
	 * @param newProtocol The new protocol to apply.
	 * @return A new Pathment with the updated protocol, or the original if conversion is not possible.
	 */
	public static Pathment switchProtocol(Pathment original, TransferProtocol newProtocol) {
		if (original == null || newProtocol == null) {
			Log.warn("Invalid parameters for switching protocol.");
			return original;
		}

		WebUrl originalWebUrl = original.getWebUrl();
		if (originalWebUrl == null || !original.getType().isWebUrl()) {
			Log.warn("Protocol switching is only supported for WebUrl-based Pathments. Current type: %s", original.getType());
			return original;
		}

		// Create a new WebUrl with the new protocol.
		WebUrl newWebUrl = new WebUrl(newProtocol, originalWebUrl.getSubDomain(), originalWebUrl.getMainDomain(), originalWebUrl.getPort(), originalWebUrl.getPath(),
		        originalWebUrl.getQuery(), originalWebUrl.getFragment());

		// Use the Pathment factory to create the new instance.
		return Pathment.fromWebUrl(newWebUrl);
	}
}