package utils.resources;

import java.util.Arrays;
import java.util.List;

import utils.logging.Log;

/**
 * This enum class defines keys for different resource types, their corresponding subdirectories, and supported file formats.
 */
public enum ResType {

	LAYOUT("fxml", "fxml"),
	STYLE("styles", "css"),
	LANG("lang", "properties"),
	ICON("icons", "jpg", "png", "gif", "bmp", "tiff", "raw", "psd", "heif", "webp", "indd"),
	SVG("SVG", "svg"),
	FLAGS("lang/flags", "jpg", "png", "gif", "bmp"), // Simplified list
	FONT("fonts", "ttf", "otf"),
	TEXT("text", "txt"),
	AUDIO("audio", "aif", "wav", "m4a", "mp3", "ogg", "flac"),
	THEME("themes", "tme"),
	NATIVES("natives", "dll", "exe"),
    // Aktiviert: Mapping für HTML-Hilfedateien
    // Sucht in: src/main/resources/resources/helpfiles/
	HELP("helpfiles", "html", "htm"),
	UNKNOWN_TYPE_FORMAT(null, "");

	private final String       directoryName;
	private final List<String> formats;

	/**
	 * Constructor for a resource type.
	 * 
	 * @param folderName       The subdirectory name within the resources folder.
	 * @param supportedFormats A varargs array of supported file extensions (without the dot).
	 */
	ResType(String folderName, String... supportedFormats) {
		this.directoryName = folderName;
		this.formats = Arrays.asList(supportedFormats);
	}

	public String getDirectoryName() {
		return directoryName;
	}

	/**
	 * Checks if this resource type supports the given format (extension).
	 * 
	 * @param format The file extension to check (e.g., "png").
	 * @return true if the format is supported, false otherwise.
	 */
	public boolean supportsFormat(String format) {
		if (format == null || format.isEmpty()) {
			return false;
		}
		// Case-insensitive check
		return formats.stream().anyMatch(f -> f.equalsIgnoreCase(format));
	}

	public static ResType getTypeOf(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			return UNKNOWN_TYPE_FORMAT;
		}
		ResKey<?> key = new ResKey<>(fileName);
		return getTypeOf(key);
	}

	public static ResType getTypeOf(ResKey<?> key) {
		if (key == null) {
			return UNKNOWN_TYPE_FORMAT;
		}
		String format = key.getFormat();
		return getTypeByFormat(format);
	}

	/**
	 * Finds the ResType that corresponds to a given file format (extension).
	 *
	 * @param format The file extension (e.g., "html", "fxml") without the leading dot.
	 * @return The matching ResType, or UNKNOWN_TYPE_FORMAT if no corresponding type is found.
	 */
	public static ResType getTypeByFormat(String format) {
		if (format == null || format.isBlank()) {
			return UNKNOWN_TYPE_FORMAT;
		}

		// Iterate through all defined ResType values.
		for (ResType type : ResType.values()) {
			// Check if the current type supports the given format.
			if (type.supportsFormat(format)) {
				return type; // Match found, return the type.
			}
		}

		// If the loop completes without finding a match, log a helpful warning.
		Log.warn("ResType not found for format '%s'. " + "SOLUTION: Add the format to the appropriate enum constant in ResType.java. "
		        + "Example: HELP(\"helpfiles\", \"html\", \"htm\")", format);

		return UNKNOWN_TYPE_FORMAT;
	}
}